import { HardhatRuntimeEnvironment } from 'hardhat/types';
import { DeployFunction } from 'hardhat-deploy/types';
import { ethers, upgrades } from 'hardhat';
import { ContractFactory } from 'ethers';
import * as fs from 'fs';
import * as path from 'path';

const func: DeployFunction = async function (hre: HardhatRuntimeEnvironment) {
  const { deployments, getNamedAccounts, network } = hre;
  const { deployer } = await getNamedAccounts();
  
  console.log('\n========================================');
  console.log('Deploying Trade  Contract (UUPS)');
  console.log('========================================\n');

  try {
    // 获取已部署的合约地址
    const dividend = await deployments.get('Dividend');
    const tip = await deployments.get('TIP');
    const signner = '0x0AB1DCa339783f09cE4D8d05A1F37afbf7631C61';
    const feeRecipient = '0x0AB1DCa339783f09cE4D8d05A1F37afbf7631C61';

    // 获取合约工厂
    const Trade = (await ethers.getContractFactory('Trade'))as unknown as ContractFactory;

    // 检查是否已经部署
    let trade;
    let proxyAddress;
    
    try {
      const existingDeployment = await deployments.get('Trade');
      proxyAddress = existingDeployment.address;
      
      console.log('   Existing proxy found, importing...');
      // 使用 forceImport 导入已存在的代理
      await upgrades.forceImport(proxyAddress, Trade, { kind: 'uups' });
      trade = await ethers.getContractAt('Trade', proxyAddress);
      console.log('   ✅ Proxy imported successfully');
      console.log(`   Proxy Address: ${proxyAddress}`);
      
      // 如果只是导入，直接返回
      return true;
    } catch (e) {
      // 如果不存在，则部署新的代理
      console.log('   No existing proxy found, deploying new proxy...');
      trade = await upgrades.deployProxy(
          Trade,
        [
          tip.address,
          signner,
          dividend.address,
          feeRecipient
        ],
        {
          kind: 'uups',
          initializer: 'initialize',
        }
      );
    }

    // 只有在新部署时才处理交易回执
    let receipt: any = undefined;
    const tx = trade.deploymentTransaction();
    if (tx) {
      const txReceipt = await tx.wait();
      if (txReceipt) {
        receipt = {
          transactionHash: txReceipt.hash,
          transactionIndex: txReceipt.index,
          blockHash: txReceipt.blockHash,
          blockNumber: txReceipt.blockNumber,
          gasUsed: txReceipt.gasUsed,
          from: tx.from,
          cumulativeGasUsed: txReceipt.gasUsed,
        };
      }
    }

    await trade.waitForDeployment();
    proxyAddress = await trade.getAddress();
    const implementation = await upgrades.erc1967.getImplementationAddress(proxyAddress);

    console.log('✅ Trade deployment completed!');
    console.log(`   Proxy Address: ${proxyAddress}`);
    console.log(`   Implementation Address: ${implementation}`);

    // 保存为 hardhat-deploy deployment 文件
    const artifact = await deployments.getArtifact('Trade');

    await deployments.save('Trade', {
      abi: artifact.abi,
      address: proxyAddress,
      receipt,
      bytecode: artifact.bytecode,
      deployedBytecode: artifact.deployedBytecode,
      implementation,
    });

    // 导出 solc input
    try {
      const buildInfoDir = path.join(__dirname, '..', 'artifacts', 'build-info');
      const outputDir = path.join(__dirname, '..', 'deployments', 'solcInputs');

      if (fs.existsSync(buildInfoDir)) {
        const files = fs.readdirSync(buildInfoDir).filter((f) => f.endsWith('.json'));
        let chosen: any | null = null;

        for (const file of files) {
          const fullPath = path.join(buildInfoDir, file);
          const raw = fs.readFileSync(fullPath, 'utf8');
          try {
            const json = JSON.parse(raw);
            const input = json.input;
            if (input && input.sources && input.sources['contracts/Trade.sol']) {
              chosen = input;
              break;
            }
          } catch {
            // ignore parse error
          }
        }

        if (chosen) {
          if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
          }
          const outPath = path.join(outputDir, 'Trade.json');
          fs.writeFileSync(outPath, JSON.stringify(chosen, null, 2));
          console.log(`   🧾 Solc input exported to: ${outPath}`);
        } else {
          console.warn('   ⚠️  Could not find build-info, skip solcInputs export.');
        }
      } else {
        console.warn('   ⚠️  build-info directory not found, skip solcInputs export.');
      }
    } catch (e) {
      console.warn('   ⚠️  Failed to export solcInputs:', e);
    }
  } catch (error: any) {
    console.error('❌ 部署失败:', error);
    throw error;
  }
};

func.tags = ['Trade'];

export default func;
