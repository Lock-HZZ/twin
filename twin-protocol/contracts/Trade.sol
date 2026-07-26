// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "./interfaces/IERC20.sol";
import "./interfaces/IPair.sol";
import "./interfaces/IRouter02.sol";
import "./interfaces/ITIP.sol";
import "@openzeppelin/contracts/utils/math/Math.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/ReentrancyGuardUpgradeable.sol";

contract Trade is
    Initializable,
    OwnableUpgradeable,
    UUPSUpgradeable,
    ReentrancyGuardUpgradeable
{
    IERC20    public constant USDC   = IERC20(0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359);
    IRouter02 public constant ROUTER = IRouter02(0xedf6066a2b290C185783862C7F4776A2C8077AD1);

    ITIP    public tip;
    address public pair;

    address public feeReceiver;
    address public dividendContract;
    address public operator;

    modifier onlyOperator() {
        require(msg.sender == operator, "Only operator");
        _;
    }

    uint256 public constant BASE = 10000;
    uint256 public slippageTolerance;

    uint256 public buyTaxRate;

    uint256 public sellTaxLow;
    uint256 public sellTaxMid;
    uint256 public sellTaxHigh;
    uint256 public dropMid;
    uint256 public dropHigh;

    uint256 public constant BEIJING_OFFSET = 8 hours;

    uint256 public dailyBasePrice;
    uint256 public lastPrice;
    uint256 public lastTradeDay;

    event Buy(address indexed user, uint256 usdcIn, uint256 fee, uint256 tipOut);
    event Sell(address indexed user, uint256 tipIn, uint256 fee, uint256 usdcOut, uint256 burned);
    event RemoveLiquidity(address indexed user, uint256 liquidity, uint256 usdcOut, uint256 tipToDividend);
    event DailyBasePriceUpdated(uint256 day, uint256 basePrice);
    event FeeReceiverUpdated(address feeReceiver);
    event DividendContractUpdated(address dividendContract);
    event OperatorUpdated(address operator);
    event BuyTaxRateUpdated(uint256 rate);
    event SlippageToleranceUpdated(uint256 slippageTolerance);
    event SellTaxConfigUpdated(uint256 sellTaxLow, uint256 sellTaxMid, uint256 sellTaxHigh, uint256 dropMid, uint256 dropHigh);

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(address _tip, address _operator, address _dividendContract, address _feeReceiver) external initializer {
        require(_tip != address(0), "Invalid tip");
        require(_operator != address(0), "Invalid operator");
        require(_dividendContract != address(0), "Invalid dividend contract");
        require(_feeReceiver != address(0), "Invalid fee receiver");
        tip = ITIP(_tip);
        pair = ITIP(_tip).pair();
        operator = _operator;
        dividendContract = _dividendContract;
        feeReceiver = _feeReceiver;
        slippageTolerance = 100;
        sellTaxLow = 600;
        sellTaxMid = 2000;
        sellTaxHigh = 5000;
        dropMid = 500;
        dropHigh = 1000;
        __Ownable_init(_msgSender());
        __UUPSUpgradeable_init();
        __ReentrancyGuard_init();
    }

    function _authorizeUpgrade(address newImplementation) internal override onlyOwner {}

    function currentPrice() public view returns (uint256) {
        (uint112 r0, uint112 r1, ) = IPair(pair).getReserves();
        address token0 = IPair(pair).token0();

        uint256 usdcReserve;
        uint256 tipReserve;
        if (token0 == address(USDC)) {
            usdcReserve = uint256(r0);
            tipReserve  = uint256(r1);
        } else {
            usdcReserve = uint256(r1);
            tipReserve  = uint256(r0);
        }
        if (tipReserve == 0) {
            return 0;
        }
        return usdcReserve * 1e18 / tipReserve;
    }

    function beijingDay() public view returns (uint256) {
        return (block.timestamp + BEIJING_OFFSET) / 1 days;
    }

    function currentSellTaxRate() public view returns (uint256) {
        uint256 basePrice = (beijingDay() > lastTradeDay && lastPrice != 0)
            ? lastPrice
            : dailyBasePrice;
        uint256 cur = currentPrice();

        if (basePrice == 0 || cur >= basePrice) {
            return sellTaxLow;
        }

        uint256 dropBps = (basePrice - cur) * BASE / basePrice;
        if (dropBps >= dropHigh) {
            return sellTaxHigh;
        }
        if (dropBps >= dropMid) {
            return sellTaxMid;
        }
        return sellTaxLow;
    }

    function _rollDay() internal {
        uint256 today = beijingDay();
        if (today > lastTradeDay) {
            if (lastPrice != 0) {
                dailyBasePrice = lastPrice;
                emit DailyBasePriceUpdated(today, dailyBasePrice);
            }
            lastTradeDay = today;
        }
    }

    function _recordPrice() internal {
        lastPrice = currentPrice();
    }

    function buy(uint256 usdcAmount, uint256 deadline) external nonReentrant returns (uint256 tipOut) {
        require(usdcAmount > 0, "Zero amount");

        _rollDay();

        require(USDC.transferFrom(msg.sender, address(this), usdcAmount), "USDC transferFrom failed");
        USDC.approve(address(ROUTER), usdcAmount);

        address[] memory path = new address[](2);
        path[0] = address(USDC);
        path[1] = address(tip);

        uint256[] memory amountsOut = ROUTER.getAmountsOut(usdcAmount, path);
        uint256 minTipOut = amountsOut[1] * (BASE - slippageTolerance) / BASE;

        uint256 tipBefore = tip.balanceOf(address(this));
        ROUTER.swapExactTokensForTokens(usdcAmount, minTipOut, path, address(this), deadline);
        uint256 tipReceived = tip.balanceOf(address(this)) - tipBefore;

        uint256 fee = tipReceived * buyTaxRate / BASE;
        if (fee > 0) {
            tip.transfer(feeReceiver, fee);
        }

        tipOut = tipReceived - fee;
        require(tip.transfer(msg.sender, tipOut), "TIP transfer failed");

        _recordPrice();
        emit Buy(msg.sender, usdcAmount, fee, tipOut);
    }

    function sell(uint256 tipAmount, uint256 deadline) external nonReentrant returns (uint256 usdcOut) {
        require(tipAmount > 0, "Zero amount");

        _rollDay();

        require(tip.transferFrom(msg.sender, address(this), tipAmount), "TIP transferFrom failed");

        uint256 taxRate = currentSellTaxRate();
        uint256 fee = tipAmount * taxRate / BASE;
        uint256 amountToSwap = tipAmount - fee;

        if (fee > 0) {
            tip.transfer(feeReceiver, fee);
        }

        address[] memory path = new address[](2);
        path[0] = address(tip);
        path[1] = address(USDC);
        uint256[] memory amountsOut = ROUTER.getAmountsOut(amountToSwap, path);
        uint256 minUsdcOut = amountsOut[1] * (BASE - slippageTolerance) / BASE;

        tip.approve(address(ROUTER), amountToSwap);

        uint256 usdcBefore = USDC.balanceOf(msg.sender);
        ROUTER.swapExactTokensForTokens(amountToSwap, minUsdcOut, path, msg.sender, deadline);
        usdcOut = USDC.balanceOf(msg.sender) - usdcBefore;

        // 额外通缩：从 LP 再销毁 min(amountToSwap, LP的TIP余额)
        uint256 pairTip = tip.balanceOf(pair);
        uint256 burnAmt = Math.min(amountToSwap, pairTip);
        if (burnAmt > 0) {
            tip.destroyFromLP(burnAmt);
        }

        _recordPrice();
        emit Sell(msg.sender, tipAmount, fee, usdcOut, burnAmt);
    }

    function removeLiquidity(
        address user,
        uint256 amount,
        uint256 deadline
    ) external onlyOperator nonReentrant returns (uint256 usdcOut, uint256 tipToDividend) {
        require(amount > 0, "Zero liquidity");
        require(dividendContract != address(0), "Dividend contract not set");

        uint256 totalSupply = IERC20(pair).totalSupply();
        uint256 tipBalance = tip.balanceOf(pair);
        uint256 usdcBalance = USDC.balanceOf(pair);

        uint256 expectedTIP = (amount * tipBalance) / totalSupply;
        uint256 expectedUSDC = (amount * usdcBalance) / totalSupply;

        uint256 amountTIPMin = expectedTIP * (BASE - slippageTolerance) / BASE;
        uint256 amountUSDCMin = expectedUSDC * (BASE - slippageTolerance) / BASE;

        IERC20(pair).approve(address(ROUTER), amount);
        (uint256 amountTIP, uint256 amountUSDC) = ROUTER.removeLiquidity(
            address(tip),
            address(USDC),
            amount,
            amountTIPMin,
            amountUSDCMin,
            address(this),
            deadline
        );

        usdcOut = amountUSDC;
        if (usdcOut > 0) {
            require(USDC.transfer(user, usdcOut), "USDC transfer failed");
        }

        tipToDividend = amountTIP;
        if (tipToDividend > 0) {
            require(tip.transfer(dividendContract, tipToDividend), "TIP transfer failed");
        }

        emit RemoveLiquidity(user, amount, usdcOut, tipToDividend);
    }

    function setFeeReceiver(address _feeReceiver) external onlyOwner {
        require(_feeReceiver != address(0), "Invalid feeReceiver");
        feeReceiver = _feeReceiver;
        emit FeeReceiverUpdated(_feeReceiver);
    }

    function setDividendContract(address _dividendContract) external onlyOwner {
        require(_dividendContract != address(0), "Invalid dividend contract");
        dividendContract = _dividendContract;
        emit DividendContractUpdated(_dividendContract);
    }

    function setOperator(address _operator) external onlyOwner {
        require(_operator != address(0), "Invalid operator");
        operator = _operator;
        emit OperatorUpdated(_operator);
    }

    function setBuyTaxRate(uint256 _rate) external onlyOwner {
        require(_rate <= BASE, "Rate exceeds BASE");
        buyTaxRate = _rate;
        emit BuyTaxRateUpdated(_rate);
    }

    function setSlippageTolerance(uint256 _slippageTolerance) external onlyOwner {
        require(_slippageTolerance <= BASE, "Slippage exceeds BASE");
        slippageTolerance = _slippageTolerance;
        emit SlippageToleranceUpdated(_slippageTolerance);
    }

    function setSellTaxConfig(
        uint256 _sellTaxLow,
        uint256 _sellTaxMid,
        uint256 _sellTaxHigh,
        uint256 _dropMid,
        uint256 _dropHigh
    ) external onlyOwner {
        require(_sellTaxLow <= BASE, "sellTaxLow exceeds BASE");
        require(_sellTaxMid <= BASE, "sellTaxMid exceeds BASE");
        require(_sellTaxHigh <= BASE, "sellTaxHigh exceeds BASE");
        require(_dropMid <= BASE, "dropMid exceeds BASE");
        require(_dropHigh <= BASE, "dropHigh exceeds BASE");
        require(_dropMid < _dropHigh, "dropMid must be less than dropHigh");

        sellTaxLow = _sellTaxLow;
        sellTaxMid = _sellTaxMid;
        sellTaxHigh = _sellTaxHigh;
        dropMid = _dropMid;
        dropHigh = _dropHigh;

        emit SellTaxConfigUpdated(_sellTaxLow, _sellTaxMid, _sellTaxHigh, _dropMid, _dropHigh);
    }

    function rescueToken(address token, address to, uint256 amount) external onlyOwner {
        require(to != address(0), "Invalid to");
        require(IERC20(token).transfer(to, amount), "Rescue transfer failed");
    }
}
