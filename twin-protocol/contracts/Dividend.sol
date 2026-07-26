// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "./interfaces/IERC20.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/ReentrancyGuardUpgradeable.sol";

contract Dividend is
    Initializable,
    OwnableUpgradeable,
    UUPSUpgradeable,
    ReentrancyGuardUpgradeable
{
    IERC20 public constant USDC = IERC20(0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359);
    IERC20 public tip;

    address public operator;
    address public feeDividendContract;
    uint256 public usdcWithdrawFeeRate;

    uint256 public constant BASE = 10000;

    mapping(address => mapping(uint8 => uint256)) public balances;

    /// @notice 已处理的批次ID，用于幂等去重（防止同一批分红被重复发放）
    mapping(bytes32 => bool) public usedBatch;

    event RewardAdded(address indexed user, uint8 indexed rewardType, uint8 indexed assetType, uint256 amount);
    event BatchProcessed(bytes32 indexed batchId, uint256 count);
    event Withdraw(address indexed user, uint8 indexed rewardType, uint8 indexed assetType, uint256 amount, uint256 fee);
    event OperatorUpdated(address operator);
    event FeeDividendContractUpdated(address feeDividendContract);
    event UsdcWithdrawFeeRateUpdated(uint256 rate);

    modifier onlyOperator() {
        require(msg.sender == operator, "Only operator");
        _;
    }

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(address _tip, address _operator, address _feeDividendContract) external initializer {
        require(_tip != address(0), "Invalid tip");
        require(_operator != address(0), "Invalid operator");
        require(_feeDividendContract != address(0), "Invalid fee dividend contract");
        tip = IERC20(_tip);
        operator = _operator;
        feeDividendContract = _feeDividendContract;
        usdcWithdrawFeeRate = 1000;
        __Ownable_init(_msgSender());
        __UUPSUpgradeable_init();
        __ReentrancyGuard_init();
    }

    function _authorizeUpgrade(address newImplementation) internal override onlyOwner {}


    /**
     * @notice 发放单条奖励（适用于实时、少量奖励，无幂等保证）
     */
    function addReward(
        address user,
        uint8 rewardType,
        uint8 assetType,
        uint256 amount
    ) external onlyOperator {
        require(amount > 0, "Zero amount");
        require(assetType <= 1, "Invalid asset type");
        balances[user][assetType] += amount;
        emit RewardAdded(user, rewardType, assetType, amount);
    }

    function addRewards(
        address[] calldata users,
        uint8[] calldata rewardTypes,
        uint8[] calldata assetTypes,
        uint256[] calldata amounts
    ) external onlyOperator {
        uint256 len = users.length;
        require(len == rewardTypes.length && len == assetTypes.length && len == amounts.length, "Length mismatch");

        for (uint256 i = 0; i < len; i++) {
            require(amounts[i] > 0, "Zero amount");
            require(assetTypes[i] <= 1, "Invalid asset type");

            balances[users[i]][assetTypes[i]] += amounts[i];
            emit RewardAdded(users[i], rewardTypes[i], assetTypes[i], amounts[i]);
        }
    }

    /**
     * @notice 幂等批量发放奖励。同一 batchId 只能成功执行一次，
     *         后端可安全重发（超时/失败重试），链上保证不会重复入账。
     * @param batchId 批次唯一标识（后端生成，全局唯一）
     */
    function addRewardsBatch(
        bytes32 batchId,
        address[] calldata users,
        uint8[] calldata rewardTypes,
        uint8[] calldata assetTypes,
        uint256[] calldata amounts
    ) external onlyOperator {
        require(!usedBatch[batchId], "Batch already processed");

        uint256 len = users.length;
        require(len == rewardTypes.length && len == assetTypes.length && len == amounts.length, "Length mismatch");
        require(len > 0, "Empty batch");

        // 先置位，防重入/重放
        usedBatch[batchId] = true;

        for (uint256 i = 0; i < len; i++) {
            require(amounts[i] > 0, "Zero amount");
            require(assetTypes[i] <= 1, "Invalid asset type");

            balances[users[i]][assetTypes[i]] += amounts[i];
            emit RewardAdded(users[i], rewardTypes[i], assetTypes[i], amounts[i]);
        }

        emit BatchProcessed(batchId, len);
    }

    /**
     * @notice 查询某批次是否已处理
     */
    function isBatchProcessed(bytes32 batchId) external view returns (bool) {
        return usedBatch[batchId];
    }

    function withdrawTIP(uint256 amount) external nonReentrant {
        require(amount > 0, "Zero amount");
        require(balances[msg.sender][1] >= amount, "Insufficient balance");

        balances[msg.sender][1] -= amount;

        require(tip.transfer(msg.sender, amount), "TIP transfer failed");

        emit Withdraw(msg.sender, 0, 1, amount, 0);
    }

    function withdrawUSDC(uint256 amount) external nonReentrant {
        require(amount > 0, "Zero amount");
        require(balances[msg.sender][0] >= amount, "Insufficient balance");
        require(feeDividendContract != address(0), "Fee dividend contract not set");

        balances[msg.sender][0] -= amount;

        uint256 fee = amount * usdcWithdrawFeeRate / BASE;
        uint256 toUser = amount - fee;

        if (toUser > 0) {
            require(USDC.transfer(msg.sender, toUser), "USDC to user failed");
        }

        if (fee > 0) {
            require(USDC.transfer(feeDividendContract, fee), "USDC fee failed");
        }

        emit Withdraw(msg.sender, 0, 0, toUser, fee);
    }

    function getTotalBalance(address user, uint8 assetType) public view returns (uint256) {
        return balances[user][assetType];
    }

    function setOperator(address _operator) external onlyOwner {
        require(_operator != address(0), "Invalid operator");
        operator = _operator;
        emit OperatorUpdated(_operator);
    }

    function setFeeDividendContract(address _contract) external onlyOwner {
        require(_contract != address(0), "Invalid contract");
        feeDividendContract = _contract;
        emit FeeDividendContractUpdated(_contract);
    }

    function setUsdcWithdrawFeeRate(uint256 _rate) external onlyOwner {
        require(_rate <= BASE, "Rate exceeds BASE");
        usdcWithdrawFeeRate = _rate;
        emit UsdcWithdrawFeeRateUpdated(_rate);
    }

    function rescueToken(address token, address to, uint256 amount) external onlyOwner {
        require(to != address(0), "Invalid to");
        require(IERC20(token).transfer(to, amount), "Rescue failed");
    }
}
