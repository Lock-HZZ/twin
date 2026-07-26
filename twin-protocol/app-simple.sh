#!/bin/bash

# 简化版Java应用管理脚本
# 配置参数（根据你的实际情况修改）
APP_NAME="your-app"
JAR_FILE="your-app.jar"
APP_PORT=8080
PID_FILE="/tmp/${APP_NAME}.pid"
LOG_FILE="/var/log/${APP_NAME}.log"

# 获取PID
get_pid() {
    if [ -f "$PID_FILE" ]; then
        cat "$PID_FILE"
    fi
}

# 检查是否运行
is_running() {
    local pid=$(get_pid)
    [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null
}

# 启动应用
start() {
    if is_running; then
        echo "应用已经在运行，PID: $(get_pid)"
        return 1
    fi
    
    echo "启动应用..."
    nohup java -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    
    sleep 3
    if is_running; then
        echo "启动成功，PID: $(get_pid)"
    else
        echo "启动失败"
        rm -f "$PID_FILE"
    fi
}

# 停止应用
stop() {
    local pid=$(get_pid)
    if [ -z "$pid" ] || ! is_running; then
        echo "应用未运行"
        rm -f "$PID_FILE"
        return 0
    fi
    
    echo "停止应用，PID: $pid"
    kill "$pid"
    
    # 等待停止
    for i in {1..30}; do
        if ! is_running; then
            echo "停止成功"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
    done
    
    # 强制停止
    echo "强制停止"
    kill -9 "$pid" 2>/dev/null
    rm -f "$PID_FILE"
}

# 查看状态
status() {
    if is_running; then
        echo "应用正在运行，PID: $(get_pid)"
        echo "端口: $APP_PORT"
    else
        echo "应用未运行"
    fi
}

# 查看日志
logs() {
    if [ -f "$LOG_FILE" ]; then
        tail -f "$LOG_FILE"
    else
        echo "日志文件不存在"
    fi
}

# 主程序
case "$1" in
    start)   start ;;
    stop)    stop ;;
    restart) stop; sleep 2; start ;;
    status)  status ;;
    log)     logs ;;
    *)       echo "用法: $0 {start|stop|restart|status|log}" ;;
esac 