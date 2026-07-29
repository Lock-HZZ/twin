// SPDX-License-Identifier: MIT
pragma solidity ^0.8.22;

import "./interfaces/IERC20.sol";
import "./interfaces/IFactory.sol";
import "./interfaces/IPair.sol";
import "./interfaces/IRouter02.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/math/Math.sol";

contract TIP is IERC20, Ownable {
    mapping(address => uint256) private _balances;
    mapping(address => mapping(address => uint256)) private _allowances;

    string private _name = "TIP";
    string private _symbol = "TIP";
    uint8 private _decimals = 18;
    uint256 private _totalSupply = 100000000000 * 10**uint256(_decimals);

    address public immutable pair;

    address public destroyer;
    address public burner;
    address public dividendAddress;

    address public constant USDC = 0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359;

    address public constant ROUTER = 0xedf6066a2b290C185783862C7F4776A2C8077AD1;
    address public constant DEAD_ADDRESS = 0x000000000000000000000000000000000000dEaD;

    bool public buyStatus = false;
    bool public sellStatus = false;

    uint256 public DAILY_BURN_RATE = 13;
    uint256 public BURN_INTERVAL = 1 days;
    uint256 public constant BASE = 1000;
    uint256 public lastBurnTime;

    mapping(address => bool) public whiteList;

    event burnPoolTokens(uint256 burnAmount, uint256 toBurn, uint256 toDividend);
    event mint(address indexed to, uint256 amount);
    event destroyFromLP(uint256 amount);
    
    constructor(address _burner) Ownable(msg.sender) {
        require (_burner != address(0), "Invalid burner address");
        burner = _burner;
        IRouter02 router = IRouter02(ROUTER);
        IFactory factory = IFactory(router.factory());

        pair = factory.createPair(address(this), USDC);
        _balances[msg.sender] = _totalSupply;
        whiteList[msg.sender] = true;
        emit Transfer(address(0), msg.sender, _totalSupply);
    }

    function name() public view returns (string memory) {
        return _name;
    }

    function symbol() public view returns (string memory) {
        return _symbol;
    }

    function decimals() public view returns (uint8) {
        return _decimals;
    }

    function totalSupply() public view override returns (uint256) {
        return _totalSupply;
    }


    function balanceOf(address account) public view override returns (uint256) {
        return _balances[account];
    }

    function transfer(address recipient, uint256 amount) public override returns (bool) {
        _transfer(msg.sender, recipient, amount);
        return true;
    }

    function allowance(address owner, address spender) public view override returns (uint256) {
        return _allowances[owner][spender];
    }

    function approve(address spender, uint256 amount) public override returns (bool) {
        _approve(msg.sender, spender, amount);
        return true;
    }

    function transferFrom(address sender, address recipient, uint256 amount) public override returns (bool) {
        uint256 currentAllowance = _allowances[sender][msg.sender];
        require(currentAllowance >= amount, "ERC20: transfer amount exceeds allowance");
        _transfer(sender, recipient, amount);
        _approve(sender, msg.sender, currentAllowance - amount);
        return true;
    }

    function mint(address to, uint256 amount) external onlyOwner {
        require(to != address(0), "ERC20: mint to the zero address");
        require(amount > 0, "Amount must be greater than zero");

        _totalSupply += amount;
        _balances[to] += amount;

        emit Transfer(address(0), to, amount);
        emit mint(to, amount);
    }

    function addWhiteList(address user) external onlyOwner {
        require(user != address(0), "Invalid address");
        whiteList[user] = true;
    }

    function removeWhiteList(address user) external onlyOwner {
        whiteList[user] = false;
    }

    function isWhiteListed(address user) external view returns (bool) {
        return whiteList[user];
    }

    function setBuyStatus(bool _status) external onlyOwner {
        buyStatus = _status;
    }

    function setSellStatus(bool _status) external onlyOwner {
        sellStatus = _status;
    }

    function burnPoolTokens(uint256 deadRatio) external returns (uint256 burnAmount) {
        require(msg.sender == burner, "Only burner can call this function");

        if (lastBurnTime != 0) {
            uint256 lastBurnDay = lastBurnTime / BURN_INTERVAL;
            uint256 currentDay = block.timestamp / BURN_INTERVAL;
            require(currentDay > lastBurnDay, "Already burned today");
        }

        require(deadRatio <= BASE, "deadRatio exceeds BASE");

        uint256 pairBalance = _balances[pair];
        require(pairBalance > 0, "No tokens in pool");

        burnAmount = pairBalance * DAILY_BURN_RATE / BASE;
        require(burnAmount > 0, "Burn amount too small");

        uint256 toBurn = burnAmount * deadRatio / BASE;
        uint256 toDividend = burnAmount - toBurn;

        _balances[pair] -= burnAmount;

        if (toBurn > 0) {
            _balances[DEAD_ADDRESS] += toBurn;
            emit Transfer(pair, DEAD_ADDRESS, toBurn);
        }

        if (toDividend > 0) {
            require(dividendAddress != address(0), "Dividend address not set");
            _balances[dividendAddress] += toDividend;
            emit Transfer(pair, dividendAddress, toDividend);
        }

        lastBurnTime = block.timestamp;
        IPair(pair).sync();
        emit burnPoolTokens(burnAmount, toBurn, toDividend);
    }

    function destroyFromLP(uint256 amount)  external {
        require(msg.sender == destroyer, "Only destroyer can call this function");
        require(amount > 0, "Amount must be greater than zero");
        require(_balances[pair] >= amount, "Not enough tokens in LP");

        _balances[pair] -= amount;
        _balances[DEAD_ADDRESS] += amount;

        emit Transfer(pair, DEAD_ADDRESS, amount);
        IPair(pair).sync();
        emit destroyFromLP(amount);
    }

    function setDestroyer(address _destroyer) external onlyOwner {
        require(_destroyer != address(0), "Invalid destroyer address");
        destroyer = _destroyer;
    }

    function setBurner(address _burner) external onlyOwner {
        require(_burner != address(0), "Invalid burner address");
        burner = _burner;
    }

    function setDividendAddress(address _dividendAddress) external onlyOwner {
        require(_dividendAddress != address(0), "Invalid dividend address");
        dividendAddress = _dividendAddress;
    }

    function setDailyBurnRate(uint256 _dailyBurnRate) external onlyOwner {
        require(_dailyBurnRate <= BASE, "Daily burn rate exceeds BASE");
        DAILY_BURN_RATE = _dailyBurnRate;
    }

    function setBurnInterval(uint256 _burnInterval) external onlyOwner {
        require(_burnInterval > 0, "Burn interval must be greater than zero");
        BURN_INTERVAL = _burnInterval;
    }

    function _transfer(address sender, address recipient, uint256 amount) internal {
        require(sender != address(0), "ERC20: transfer from the zero address");
        require(recipient != address(0), "ERC20: transfer to the zero address");
        require(_balances[sender] >= amount, "ERC20: transfer amount exceeds balance");

        _balances[sender] -= amount;

        uint256 transferAmount = amount;

        if (sender == pair) {
            if (!whiteList[recipient]) {
                require(buyStatus, "Access denied: can't buy TIP");
            }
        }

        if (recipient == pair) {
            if (!whiteList[sender]) {
                require(sellStatus, "Access denied: can't sell TIP");
            }
        }
        
        _balances[recipient] += transferAmount;
        emit Transfer(sender, recipient, transferAmount);
    }

    function _approve(address owner, address spender, uint256 amount) internal {
        require(owner != address(0), "ERC20: approve from the zero address");
        require(spender != address(0), "ERC20: approve to the zero address");
        _allowances[owner][spender] = amount;
        emit Approval(owner, spender, amount);
    }

}