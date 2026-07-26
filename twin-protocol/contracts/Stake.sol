// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "./interfaces/IERC20.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/ReentrancyGuardUpgradeable.sol";

contract Stake is Initializable, OwnableUpgradeable, UUPSUpgradeable, ReentrancyGuardUpgradeable {

    IERC20 public tip;
    address public dividendContract;

    struct StakeInfo {
        address user;
        uint256 amount;
        uint256 plan;           // 30, 90, 180, 360 (天数)
        uint256 startTime;
        uint256 endTime;
        bool withdrawn;
    }

    mapping(uint256 => StakeInfo) public stakes;
    uint256 public nextStakeId;

    mapping(address => uint256[]) public userStakes;

    event Staked(
        uint256 indexed stakeId,
        address indexed user,
        uint256 amount,
        uint256 plan,
        uint256 startTime,
        uint256 endTime
    );

    event Withdrawn(
        uint256 indexed stakeId,
        address indexed user,
        uint256 amount
    );

    event DividendContractUpdated(address dividendContract);

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(address _tip, address _dividendContract) external initializer {
        require(_tip != address(0), "_tip is zero");
        require(_dividendContract != address(0), "_dividendContract is zero");

        tip = IERC20(_tip);
        dividendContract = _dividendContract;

        __Ownable_init(_msgSender());
        __UUPSUpgradeable_init();
        __ReentrancyGuard_init();
    }

    function _authorizeUpgrade(address newImplementation) internal override onlyOwner {}

    /**
     * @notice 质押TIP代币
     * @param amount 质押数量
     * @param plan 质押套餐：30/90/180/360 天
     */
    function stake(uint256 amount, uint256 plan) external nonReentrant returns (uint256 stakeId) {
        require(amount > 0, "Amount must be greater than 0");
        require(plan == 30 || plan == 90 || plan == 180 || plan == 360, "Invalid plan");

        require(tip.transferFrom(msg.sender, address(this), amount), "Transfer failed");

        stakeId = nextStakeId++;
        uint256 duration = plan * 1 days;
        uint256 endTime = block.timestamp + duration;

        stakes[stakeId] = StakeInfo({
            user: msg.sender,
            amount: amount,
            plan: plan,
            startTime: block.timestamp,
            endTime: endTime,
            withdrawn: false
        });

        userStakes[msg.sender].push(stakeId);

        emit Staked(stakeId, msg.sender, amount, plan, block.timestamp, endTime);
        return stakeId;
    }

    /**
     * @notice 赎回质押（到期后）
     * @param stakeId 质押ID
     */
    function withdraw(uint256 stakeId) external nonReentrant {
        StakeInfo storage stakeInfo = stakes[stakeId];
        require(stakeInfo.user == msg.sender, "Not stake owner");
        require(!stakeInfo.withdrawn, "Already withdrawn");
        require(block.timestamp >= stakeInfo.endTime, "Not yet matured");

        stakeInfo.withdrawn = true;

        require(tip.transfer(msg.sender, stakeInfo.amount), "Transfer failed");

        emit Withdrawn(stakeId, msg.sender, stakeInfo.amount);
    }

    /**
     * @notice 查询用户所有质押
     */
    function getUserStakes(address user) external view returns (uint256[] memory) {
        return userStakes[user];
    }

    /**
     * @notice 查询质押详情
     */
    function getStakeInfo(uint256 stakeId) external view returns (
        address user,
        uint256 amount,
        uint256 plan,
        uint256 startTime,
        uint256 endTime,
        bool withdrawn
    ) {
        StakeInfo memory stakeInfo = stakes[stakeId];
        return (
            stakeInfo.user,
            stakeInfo.amount,
            stakeInfo.plan,
            stakeInfo.startTime,
            stakeInfo.endTime,
            stakeInfo.withdrawn
        );
    }

    /**
     * @notice 更新分红合约地址
     */
    function setDividendContract(address _dividendContract) external onlyOwner {
        require(_dividendContract != address(0), "Invalid dividend contract");
        dividendContract = _dividendContract;
        emit DividendContractUpdated(_dividendContract);
    }

    /**
     * @notice 紧急提取（仅owner）
     */
    function rescueToken(address token, address to, uint256 amount) external onlyOwner {
        require(to != address(0), "Invalid to");
        require(IERC20(token).transfer(to, amount), "Rescue failed");
    }
}
