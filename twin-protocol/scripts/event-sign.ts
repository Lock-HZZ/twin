import { ethers } from 'hardhat';
import dotenv from 'dotenv';
dotenv.config();

async function main() {
    console.log('🚀 Event sign...\n');

    const tokenAddress =  "0x3FA025684B507Ea84E7C45B8b7202bcE7c7A0385";

    const token = await ethers.getContractAt('TIP', tokenAddress);
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error('\n❌ Error:', error);
        process.exit(1);
    });
