// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "./interfaces/IERC20.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/ReentrancyGuardUpgradeable.sol";

/**
 * @title TipStaking
 * @notice TIP代币质押合约，支持四档期限：30天、90天、180天、360天
 * @dev 后端负责每日分红发放到分红合约
 */
contract TipStaking is
    Initializable,
    OwnableUpgradeable,
    UUPSUpgradeable,
    ReentrancyGuardUpgradeable
{
    IERC20 public tip;
    address public dividendContract;

    // 质押套餐期限（天）
    uint256 public constant PLAN_30_DAYS = 30;
    uint256 public constant PLAN_90_DAYS = 90;
    uint256 public constant PLAN_180_DAYS = 180;
    uint256 public constant PLAN_360_DAYS = 360;

    // 质押记录
    struct Stake {
        uint256 amount;          // 质押金额
        uint256 plan;            // 质押期限（天）
        uint256 startTime;       // 质押开始时间
        uint256 endTime;         // 质押到期时间
        bool withdrawn;          // 是否已赎回
    }

    // 用户质押记录：user => stakeId => Stake
    mapping(address => mapping(uint256 => Stake)) public stakes;

    // 用户质押记录数量
    mapping(address => uint256) public stakeCount;

    // 全局质押统计（按套餐）
    mapping(uint256 => uint256) public totalStakedByPlan;

    event Staked(
        address indexed user,
        uint256 indexed stakeId,
        uint256 amount,
        uint256 plan,
        uint256 startTime,
        uint256 endTime
    );

    event Withdrawn(
        address indexed user,
        uint256 indexed stakeId,
        uint256 amount
    );

    event DividendContractUpdated(address dividendContract);

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(address _tip, address _dividendContract) external initializer {
        require(_tip != address(0), "TIP is zero");
        require(_dividendContract != address(0), "Dividend contract is zero");

        tip = IERC20(_tip);
        dividendContract = _dividendContract;

        __Ownable_init(_msgSender());
        __UUPSUpgradeable_init();
        __ReentrancyGuard_init();
    }

    function _authorizeUpgrade(address newImplementation) internal override onlyOwner {}

    /**
     * @notice 质押TIP代币
     * @param amount 质押金额
     * @param plan 质押期限（30/90/180/360天）
     */
    function stake(uint256 amount, uint256 plan) external nonReentrant {
        require(amount > 0, "Amount must be greater than 0");
        require(
            plan == PLAN_30_DAYS ||
            plan == PLAN_90_DAYS ||
            plan == PLAN_180_DAYS ||
            plan == PLAN_360_DAYS,
            "Invalid plan"
        );

        // 转入TIP代币
        require(tip.transferFrom(_msgSender(), address(this), amount), "Transfer failed");

        uint256 stakeId = stakeCount[_msgSender()]++;
        uint256 startTime = block.timestamp;
        uint256 endTime = startTime + (plan * 1 days);

        stakes[_msgSender()][stakeId] = Stake({
            amount: amount,
            plan: plan,
            startTime: startTime,
            endTime: endTime,
            withdrawn: false
        });

        totalStakedByPlan[plan] += amount;

        emit Staked(_msgSender(), stakeId, amount, plan, startTime, endTime);
    }

    /**
     * @notice 赎回质押（到期后）
     * @param stakeId 质押记录ID
     */
    function withdraw(uint256 stakeId) external nonReentrant {
        Stake storage stakeInfo = stakes[_msgSender()][stakeId];

        require(stakeInfo.amount > 0, "Stake not found");
        require(!stakeInfo.withdrawn, "Already withdrawn");
        require(block.timestamp >= stakeInfo.endTime, "Not yet matured");

        stakeInfo.withdrawn = true;
        totalStakedByPlan[stakeInfo.plan] -= stakeInfo.amount;

        require(tip.transfer(_msgSender(), stakeInfo.amount), "Transfer failed");

        emit Withdrawn(_msgSender(), stakeId, stakeInfo.amount);
    }

    /**
     * @notice 查询用户的质押记录
     * @param user 用户地址
     * @param stakeId 质押记录ID
     */
    function getStake(address user, uint256 stakeId) external view returns (Stake memory) {
        return stakes[user][stakeId];
    }

    /**
     * @notice 查询某个套餐的总质押量
     * @param plan 质押期限
     */
    function getTotalStaked(uint256 plan) external view returns (uint256) {
        return totalStakedByPlan[plan];
    }

    /**
     * @notice 设置分红合约地址
     * @param _dividendContract 分红合约地址
     */
    function setDividendContract(address _dividendContract) external onlyOwner {
        require(_dividendContract != address(0), "Invalid dividend contract");
        dividendContract = _dividendContract;
        emit DividendContractUpdated(_dividendContract);
    }

    /**
     * @notice 紧急提取代币（仅owner）
     * @param token 代币地址
     * @param to 接收地址
     * @param amount 金额
     */
    function rescueToken(address token, address to, uint256 amount) external onlyOwner {
        require(to != address(0), "Invalid to");
        require(IERC20(token).transfer(to, amount), "Rescue failed");
    }
}
