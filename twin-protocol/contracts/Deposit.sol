// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "./interfaces/IERC20.sol";
import "./interfaces/IRouter02.sol";
import "./interfaces/ITrade.sol";
import "@openzeppelin/contracts/utils/cryptography/ECDSA.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/ReentrancyGuardUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/cryptography/EIP712Upgradeable.sol";


contract Deposit is  Initializable,
    OwnableUpgradeable,
    UUPSUpgradeable,
    ReentrancyGuardUpgradeable,
    EIP712Upgradeable  {
    IERC20    public constant USDC   = IERC20(0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359);
    IRouter02 public constant ROUTER = IRouter02(0xedf6066a2b290C185783862C7F4776A2C8077AD1);

    ITrade public trade;
    address public tip;

    address public signer;
    address public dividendContract;
    address public dustReceiver;

    mapping(uint256 => bool) public usedNonces;

    uint8 public constant FUNC_DEPOSIT = 2;

    uint256 public constant BASE = 10000;
    uint256 public slippageTolerance;

    bytes32 public constant DEPOSIT_TYPEHASH = keccak256(
        "Deposit(address user,uint256 amount,uint256 nonce,uint256 deadline,uint8 functionType)"
    );

    event DepositExecuted(
        address indexed user,
        uint256 usdcAmount,
        uint256 toDividend,
        uint256 tipBought,
        uint256 liquidity,
        uint256 dustUSDC,
        uint256 dustTIP
    );
    event SignerUpdated(address signer);
    event DividendContractUpdated(address dividendContract);
    event DustReceiverUpdated(address dustReceiver);
    event SlippageToleranceUpdated(uint256 slippageTolerance);

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(address _trade, address _signer, address _dustReceiver, address _dividendContract) external initializer {
        require(_trade != address(0), "_trade is zero");
        require(_signer != address(0), "_signer is zero");
        require(_dustReceiver != address(0), "_dustReceiver is zero");
        require(_dividendContract != address(0), "_dividendContract is zero");

        trade = ITrade(_trade);
        tip = address(trade.tip());
        signer = _signer;
        dustReceiver = _dustReceiver;
        dividendContract = _dividendContract;
        slippageTolerance = 100;
        __Ownable_init(_msgSender());
        __UUPSUpgradeable_init();
        __ReentrancyGuard_init();
        __EIP712_init("TwinProtocolDeposit", "1");
    }

    function _authorizeUpgrade(address newImplementation) internal override onlyOwner {}


    function depositWithSig(
        address user,
        uint256 usdcAmount,
        uint256 nonce,
        uint256 deadline,
        uint8 functionType,
        bytes calldata signature
    ) external nonReentrant returns (uint256 liquidity) {
        require(block.timestamp <= deadline, "Signature expired");
        require(functionType == FUNC_DEPOSIT, "Bad function type");
        require(usdcAmount > 0, "Zero amount");
        require(dividendContract != address(0), "Dividend contract not set");
        require(dustReceiver != address(0), "Dust receiver not set");
        require(!usedNonces[nonce], "Nonce used");

        bytes32 structHash = keccak256(abi.encode(
            DEPOSIT_TYPEHASH, user, usdcAmount, nonce, deadline, functionType
        ));
        bytes32 digest = _hashTypedDataV4(structHash);
        require(ECDSA.recover(digest, signature) == signer, "Invalid signature");

        usedNonces[nonce] = true;

        require(USDC.transferFrom(user, address(this), usdcAmount), "USDC transferFrom failed");

        uint256 toDividend = usdcAmount / 2;
        require(USDC.transfer(dividendContract, toDividend), "USDC to dividend failed");

        uint256 toBuyTip = usdcAmount / 4;
        USDC.approve(address(ROUTER), toBuyTip);

        address[] memory buyPath = new address[](2);
        buyPath[0] = address(USDC);
        buyPath[1] = tip;

        uint256[] memory amountsOut = ROUTER.getAmountsOut(toBuyTip, buyPath);
        uint256 minTipOut = amountsOut[1] * (BASE - slippageTolerance) / BASE;

        uint256 tipBefore = IERC20(tip).balanceOf(address(this));
        ROUTER.swapExactTokensForTokens(toBuyTip, minTipOut, buyPath, address(this), deadline);
        uint256 tipReceived = IERC20(tip).balanceOf(address(this)) - tipBefore;

        uint256 toLiquidity = usdcAmount - toDividend - toBuyTip;

        USDC.approve(address(ROUTER), toLiquidity);
        IERC20(tip).approve(address(ROUTER), tipReceived);

        (uint256 usedUSDC, uint256 usedTIP, uint256 lpAmount) = ROUTER.addLiquidity(
            address(USDC),
            tip,
            toLiquidity,
            tipReceived,
            0,
            0,
            address(trade),
            deadline
        );

        uint256 dustUSDC = toLiquidity - usedUSDC;
        uint256 dustTIP = tipReceived - usedTIP;
        if (dustUSDC > 0) USDC.transfer(dustReceiver, dustUSDC);
        if (dustTIP > 0) IERC20(tip).transfer(dustReceiver, dustTIP);

        emit DepositExecuted(user, usdcAmount, toDividend, tipReceived, lpAmount, dustUSDC, dustTIP);
        return lpAmount;
    }

    function setSigner(address _signer) external onlyOwner {
        require(_signer != address(0), "Invalid signer");
        signer = _signer;
        emit SignerUpdated(_signer);
    }

    function setDividendContract(address _dividendContract) external onlyOwner {
        require(_dividendContract != address(0), "Invalid dividend contract");
        dividendContract = _dividendContract;
        emit DividendContractUpdated(_dividendContract);
    }

    function setDustReceiver(address _dustReceiver) external onlyOwner {
        require(_dustReceiver != address(0), "Invalid dust receiver");
        dustReceiver = _dustReceiver;
        emit DustReceiverUpdated(_dustReceiver);
    }

    function setSlippageTolerance(uint256 _slippageTolerance) external onlyOwner {
        require(_slippageTolerance <= BASE, "Slippage exceeds BASE");
        slippageTolerance = _slippageTolerance;
        emit SlippageToleranceUpdated(_slippageTolerance);
    }

    function rescueToken(address token, address to, uint256 amount) external onlyOwner {
        require(to != address(0), "Invalid to");
        require(IERC20(token).transfer(to, amount), "Rescue failed");
    }
}
