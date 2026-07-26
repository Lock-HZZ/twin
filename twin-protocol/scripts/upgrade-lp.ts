import { ethers, upgrades } from 'hardhat';

async function main() {
  console.log('\n========================================');
  console.log('Upgrading Defi Contract');
  console.log('========================================\n');

  // 获取当前代理地址（从部署记录中获取）
  const PROXY_ADDRESS = process.env.GAME_PROXY_ADDRESS || '0xAEc7EA41A1cB5F73983cF4574A3de91101a6A6Db';
  
  if (!PROXY_ADDRESS) {
    console.error('❌ Please set PDA_LP_PROXY_ADDRESS environment variable');
    process.exit(1);
  }

  console.log(`Proxy Address: ${PROXY_ADDRESS}`);
  console.log('');

  // 获取当前实现地址
  const currentImplementation = await upgrades.erc1967.getImplementationAddress(PROXY_ADDRESS);
  console.log(`Current Implementation: ${currentImplementation}`);
  console.log('');

  // 获取新的合约工厂
  const lpManager = await ethers.getContractFactory("PDALiquidityManager");

  // 首先尝试导入现有的代理（如果未注册）
  console.log('Importing existing proxy...');
  try {
    await upgrades.forceImport(PROXY_ADDRESS, lpManager as any, { kind: 'uups' });
    console.log('✅ Proxy imported successfully');
  } catch (error: any) {
    console.log('⚠️  Proxy already imported or error:', error.message);
  }
  console.log('');

  // 验证升级兼容性
  console.log('Validating upgrade...');
  try {
    await upgrades.validateUpgrade(PROXY_ADDRESS, lpManager as any, { kind: 'uups' });
    console.log('✅ Upgrade validation passed');
  } catch (error: any) {
    console.error('❌ Upgrade validation failed:', error.message);
    console.error('Full error:', error);
    process.exit(1);
  }
  console.log('');

  // 执行升级
  console.log('Upgrading contract...');
  const upgraded = await upgrades.upgradeProxy(PROXY_ADDRESS, lpManager as any, {
    kind: 'uups',
    redeployImplementation: 'always'  // 强制重新部署实现合约
  });
  await upgraded.waitForDeployment();

  const newImplementation = await upgrades.erc1967.getImplementationAddress(PROXY_ADDRESS);
  
  console.log('✅ Upgrade completed!');
  console.log(`   Proxy Address: ${PROXY_ADDRESS}`);
  console.log(`   Old Implementation: ${currentImplementation}`);
  console.log(`   New Implementation: ${newImplementation}`);
  
  // 检查实现地址是否真的改变了
  if (currentImplementation === newImplementation) {
    console.log('   ⚠️  WARNING: Implementation address did not change!');
    console.log('   This might indicate that the upgrade did not deploy a new implementation.');
  } else {
    console.log('   ✅ Implementation address changed successfully!');
  }
  console.log('');

  // 验证升级后的合约
  console.log('Verifying upgraded contract...');
  const pdaLiquidityManager = await ethers.getContractAt('PDALiquidityManager', PROXY_ADDRESS);
  
  try {
    const owner = await pdaLiquidityManager.owner();
    console.log(`   Owner: ${owner}`);
    
    const USDT = await pdaLiquidityManager.USDT()
    console.log(`   USDT: ${USDT}`);

    
    console.log('✅ Contract verification passed');
  } catch (error) {
    console.error('❌ Contract verification failed:', error);
  }
  console.log('');

  console.log('========================================');
  console.log('Upgrade Summary');
  console.log('========================================');
  console.log(`Proxy:              ${PROXY_ADDRESS}`);
  console.log(`Old Implementation: ${currentImplementation}`);
  console.log(`New Implementation: ${newImplementation}`);
  console.log('========================================\n');

}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
