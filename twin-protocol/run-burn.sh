#!/bin/bash
# run-burn.sh — 简单守护脚本（备用方案，推荐用 PM2）

echo "================================================"
echo "  PDA Atomic Burn Daemon"
echo "  按 Ctrl+C 停止"
echo "================================================"

while true; do
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 启动 atomic-burn-daemon.js ..."
    node scripts/atomic-burn-daemon1.js

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 进程退出，5 秒后自动重启..."
    sleep 5
done
