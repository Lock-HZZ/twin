import {HardhatRuntimeEnvironment} from 'hardhat/types';
import {DeployFunction} from 'hardhat-deploy/types';
import {deployContract} from './001_deploy_utils';
import dotenv from 'dotenv';
import {ContractTransaction} from "ethers";

dotenv.config();

const func: DeployFunction = async function (hre: HardhatRuntimeEnvironment) {
     console.log('🚀 Deploying TIP.sol...');
     const { deployments, getNamedAccounts } = hre;
     const { deployer } = await getNamedAccounts();

     const burnAddress= '0x0AB1DCa339783f09cE4D8d05A1F37afbf7631C61';

     const tokenDeployment = await deployContract(
          hre,
          'TIP',
          [burnAddress]
     );

     console.log('✅ TIP.sol.sol deployment completed!');
     console.log(`   TIP Address: ${tokenDeployment.address}`);
};

func.tags = ['TIP'];
export default func;
