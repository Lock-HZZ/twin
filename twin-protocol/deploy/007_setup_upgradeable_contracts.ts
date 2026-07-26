import { HardhatRuntimeEnvironment } from 'hardhat/types';
import { DeployFunction } from 'hardhat-deploy/types';
import { ethers } from 'hardhat';

const func: DeployFunction = async function (hre: HardhatRuntimeEnvironment) {
  const { deployments } = hre;
  
  console.log('\n========================================');
  console.log('Setting Up Upgradeable Contracts');
  console.log('========================================\n');

  // 获取已部署的合约
  const tipDeployment = await deployments.get('TIP');
  const tradeDeployment = await deployments.get('Trade');
  const depositDeployment = await deployments.get('Deposit');
  const dividendDeployment = await deployments.get('Dividend');

  const tip = await ethers.getContractAt('TIP', tipDeployment.address);


  // 1. tip加白Deposit
  console.log('1. Adding DepositUpgradeable to TIP whitelist...');
  try {
    const tx1 = await tip.addWhiteList(depositDeployment.address);
    console.log('   ✅ DepositUpgradeable added to TIP whitelist');
  } catch (error: any) {
    console.log('   ⚠️  Already whitelisted or error:', error.message);
  }

  // 2. tip加白Trade
  console.log('2. Adding TradeUpgradeable to TIP whitelist...');
  try {
    const tx2 = await tip.addWhiteList(tradeDeployment.address);
    console.log('   ✅ TradeUpgradeable added to TIP whitelist');
  } catch (error: any) {
    console.log('   ⚠️  Already whitelisted or error:', error.message);
  }

  // 3. tip设置分红合约
  console.log('3. Setting Dividend contract in TIP...');
  try {
    const tx3 = await tip.setDividendAddress(dividendDeployment.address);
    console.log('   ✅ Dividend contract set in TIP');
  } catch (error: any) {
    console.log('   ⚠️  Already set or error:', error.message);
  }

  //4. tip设置destroyer
  console.log('4. Setting Destroyer in TIP...');
  try {
    const tx4 = await tip.setDestroyer(tradeDeployment.address);
    console.log('   ✅ Destroyer set in TIP');
  } catch (error: any) {
    console.log('   ⚠️  Already set or error:', error.message);
  }

  console.log('\n✅ All upgradeable contracts set up successfully!');

};

func.tags = ['setup-upgradeable'];

export default func;
