@echo off
title PDA Atomic Burn Daemon
color 0A

echo ================================================
echo   PDA Atomic Burn Daemon - 自动守护进程
echo   按 Ctrl+C 停止
echo ================================================
echo.

:loop
echo [%date% %time%] 启动 atomic-burn-daemon1.js ...
node scripts/atomic-burn-daemon1.js

echo.
echo [%date% %time%] 进程退出，5 秒后自动重启...
timeout /t 5 /nobreak > nul
goto loop
