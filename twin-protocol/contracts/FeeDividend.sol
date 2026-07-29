// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "./interfaces/IERC20.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/ReentrancyGuardUpgradeable.sol";

contract FeeDividend is
    Initializable,
    OwnableUpgradeable,
    UUPSUpgradeable,
    ReentrancyGuardUpgradeable
{
    IERC20 public constant USDC = IERC20(0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359);

    address public operator;

    mapping(address => uint256) public balances;

    /// @notice 已处理的批次ID，用于幂等去重
    mapping(bytes32 => bool) public usedBatch;

    event RewardAdded(address indexed user, uint256 amount);
    event BatchProcessed(bytes32 indexed batchId, uint256 count);
    event Withdraw(address indexed user, uint256 amount);
    event OperatorUpdated(address operator);

    modifier onlyOperator() {
        require(msg.sender == operator, "Only operator");
        _;
    }

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(address _operator) external initializer {
        require(_operator != address(0), "Invalid operator");
        operator = _operator;
        __Ownable_init(_msgSender());
        __UUPSUpgradeable_init();
        __ReentrancyGuard_init();
    }

    function _authorizeUpgrade(address newImplementation) internal override onlyOwner {}

    function addRewards(
        address[] calldata users,
        uint256[] calldata amounts
    ) external onlyOperator {
        uint256 len = users.length;
        require(len == amounts.length, "Length mismatch");

        for (uint256 i = 0; i < len; i++) {
            require(amounts[i] > 0, "Zero amount");
            balances[users[i]] += amounts[i];
            emit RewardAdded(users[i], amounts[i]);
        }
    }

    /**
     * @notice 幂等批量发放。同一 batchId 只能成功执行一次。
     */
    function addRewardsBatch(
        bytes32 batchId,
        address[] calldata users,
        uint256[] calldata amounts
    ) external onlyOperator {
        require(!usedBatch[batchId], "Batch already processed");
        uint256 len = users.length;
        require(len == amounts.length, "Length mismatch");
        require(len > 0, "Empty batch");

        usedBatch[batchId] = true;

        for (uint256 i = 0; i < len; i++) {
            require(amounts[i] > 0, "Zero amount");
            balances[users[i]] += amounts[i];
            emit RewardAdded(users[i], amounts[i]);
        }

        emit BatchProcessed(batchId, len);
    }

    function isBatchProcessed(bytes32 batchId) external view returns (bool) {
        return usedBatch[batchId];
    }

    function withdraw(uint256 amount) external nonReentrant {
        require(amount > 0, "Zero amount");
        require(balances[msg.sender] >= amount, "Insufficient balance");

        balances[msg.sender] -= amount;
        require(USDC.transfer(msg.sender, amount), "USDC transfer failed");

        emit Withdraw(msg.sender, amount);
    }

    function setOperator(address _operator) external onlyOwner {
        require(_operator != address(0), "Invalid operator");
        operator = _operator;
        emit OperatorUpdated(_operator);
    }

    function rescueToken(address token, address to, uint256 amount) external onlyOwner {
        require(to != address(0), "Invalid to");
        require(IERC20(token).transfer(to, amount), "Rescue failed");
    }
}
