// SPDX-License-Identifier: MIT
pragma solidity ^0.8.22;

import "./IERC20.sol";

interface ITIP is IERC20 {
    function pair() external view returns (address);
    function destroyFromLP(uint256 amount) external;
}