import { ethers } from 'hardhat';
import dotenv from 'dotenv';
dotenv.config();

async function main() {
    console.log('🚀 add white...\n');

    const tokenAddress =  "0x3FA025684B507Ea84E7C45B8b7202bcE7c7A0385";

    const token = await ethers.getContractAt('TIP', tokenAddress);


    const tx = await token.removeWhiteList("0x0AB1DCa339783f09cE4D8d05A1F37afbf7631C61");
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
