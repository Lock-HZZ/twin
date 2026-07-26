import { ethers } from 'hardhat';
import dotenv from 'dotenv';
dotenv.config();

async function main() {
    console.log('🚀 approve...\n');

    const tokenAddress =  "0x3FA025684B507Ea84E7C45B8b7202bcE7c7A0385";
    const contractAddress = "0x3A035657911f96B78f6CdDdf6ACbD5Dd20C2eb6b";

    const [signer] = await ethers.getSigners();
    console.log(`   Signer: ${signer.address}`);
    console.log(`   tokenAddress: ${tokenAddress}`);

    const token = await ethers.getContractAt('TIP', tokenAddress);


    const tx = await token.approve(contractAddress, BigInt(10 ** 25));
    const receipt = await (tx as any).wait();
    console.log(`   Transaction hash: ${receipt?.hash || receipt?.transactionHash}`);
    console.log(`   Block number: ${receipt?.blockNumber}`);
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error('\n❌ Error:', error);
        process.exit(1);
    });
