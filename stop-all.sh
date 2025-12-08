#!/bin/bash

echo "停止所有服务..."

# 停止后端服务
if [ -f logs/petcare-backend.pid ]; then
    PID=$(cat logs/petcare-backend.pid)
    kill -9 $PID 2>/dev/null && echo "已停止 PetCare-Backend (PID: $PID)"
fi

if [ -f logs/media-backend.pid ]; then
    PID=$(cat logs/media-backend.pid)
    kill -9 $PID 2>/dev/null && echo "已停止 Media-Backend (PID: $PID)"
fi

if [ -f logs/community-backend.pid ]; then
    PID=$(cat logs/community-backend.pid)
    kill -9 $PID 2>/dev/null && echo "已停止 Community-Backend (PID: $PID)"
fi

if [ -f logs/frontend.pid ]; then
    PID=$(cat logs/frontend.pid)
    kill -9 $PID 2>/dev/null && echo "已停止 Frontend (PID: $PID)"
fi

# 清理PID文件
rm -f logs/*.pid

echo "所有服务已停止"