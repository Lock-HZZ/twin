import { HardhatRuntimeEnvironment } from 'hardhat/types';
import { DeployFunction } from 'hardhat-deploy/types';
import { ethers, upgrades } from 'hardhat';
import { ContractFactory } from 'ethers';
import * as fs from 'fs';
import * as path from 'path';

const func: DeployFunction = async function (hre: HardhatRuntimeEnvironment) {
  const { deployments, getNamedAccounts, network } = hre;

  console.log('\n========================================');
  console.log('Deploying FeeDividend Contract (UUPS)');
  console.log('========================================\n');

  try {
    // 获取合约工厂
    const FeeDividend = (await ethers.getContractFactory('FeeDividend'))as unknown as ContractFactory;

    // 检查是否已经部署
    let feeDividend;
    let proxyAddress;

    try {
      const existingDeployment = await deployments.get('FeeDividend');
      if (existingDeployment) {
        feeDividend = existingDeployment.address;
        return;
      }
    } catch (e) {
      // 如果不存在，则部署新的代理
      console.log('   No existing proxy found, deploying new proxy...');
      feeDividend = await upgrades.deployProxy(
          FeeDividend,
          [
            "0x0AB1DCa339783f09cE4D8d05A1F37afbf7631C61"
          ],
          {
            kind: 'uups',
            initializer: 'initialize',
          }
      );
    }

    // 只有在新部署时才处理交易回执
    let receipt: any = undefined;
    // @ts-ignore
    const tx = feeDividend.deploymentTransaction();
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

    // @ts-ignore
    await feeDividend.waitForDeployment();
    // @ts-ignore
    proxyAddress = await feeDividend.getAddress();
    const implementation = await upgrades.erc1967.getImplementationAddress(proxyAddress);

    console.log('✅ FeeDividend deployment completed!');
    console.log(`   Proxy Address: ${proxyAddress}`);
    console.log(`   Implementation Address: ${implementation}`);

    // 保存为 hardhat-deploy deployment 文件
    const artifact = await deployments.getArtifact('FeeDividend');

    await deployments.save('FeeDividend', {
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
            if (input && input.sources && input.sources['contracts/FeeDividend.sol']) {
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
          const outPath = path.join(outputDir, 'FeeDividend.json');
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

func.tags = ['FeeDividend'];

export default func;
