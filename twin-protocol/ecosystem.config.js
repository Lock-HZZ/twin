// ecosystem.config.js — PM2 配置文件
// 
// 安装 PM2: npm install -g pm2
// 启动:     pm2 start ecosystem.config.js
// 查看日志: pm2 logs pda-burn
// 查看状态: pm2 status
// 停止:     pm2 stop pda-burn
// 开机自启: pm2 startup && pm2 save

module.exports = {
    apps: [{
        name: 'pda-burn',
        script: 'scripts/atomic-burn-daemon1.js',
        cwd: '/path/to/pda',        // ← 改成你的实际路径，例如 /root/pda 或 /home/user/pda

        // 崩溃自动重启
        autorestart: true,
        restart_delay: 5000,        // 崩溃后 5 秒重启
        max_restarts: 999999,       // 无限重启
        min_uptime: '10s',          // 至少运行 10 秒才算"稳定"

        // 日志
        log_date_format: 'YYYY-MM-DD HH:mm:ss',
        out_file: './logs/burn-out.log',   // 标准输出
        error_file: './logs/burn-err.log', // 错误输出
        merge_logs: true,

        // 内存过高自动重启（防内存泄漏）
        max_memory_restart: '256M',
    }],
};
