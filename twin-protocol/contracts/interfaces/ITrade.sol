// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "./ITIP.sol";

interface ITrade {
    function tip() external view returns (ITIP);
    function pair() external view returns (address);
    function buy(uint256 usdcAmount, uint256 deadline) external returns (uint256 tipOut);
}
