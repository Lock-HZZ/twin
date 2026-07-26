import { ethers } from 'hardhat';
import dotenv from 'dotenv';
dotenv.config();

async function main() {
    console.log('🚀 trade...\n');

    const tradeAddres =  "0x3A035657911f96B78f6CdDdf6ACbD5Dd20C2eb6b";

    const trade = await ethers.getContractAt('Trade', tradeAddres);

    const deadline = Math.floor(Date.now() / 1000) + 60 * 5; // 20 minutes from the current Unix time
    const tx = await trade.sell(BigInt(10 ** 20), deadline);
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
