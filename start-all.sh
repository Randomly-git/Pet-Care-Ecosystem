#!/bin/bash

echo "========================================"
echo "启动宠物关爱生态系统 - 所有服务"
echo "========================================"
echo

# 检查Java
if ! command -v java &> /dev/null; then
    echo "[错误] 未找到Java，请先安装Java 17或更高版本"
    exit 1
fi

# 检查Node.js
if ! command -v node &> /dev/null; then
    echo "[错误] 未找到Node.js，请先安装Node.js"
    exit 1
fi

# 创建日志目录
mkdir -p logs

echo "[1/4] 启动后端微服务..."

# 启动PetCare后端 (端口8082)
echo ""
echo "[PetCare-Backend] 启动中..."
cd petcare-backend
mvn spring-boot:run > ../logs/petcare-backend.log 2>&1 &
PETCARE_PID=$!
echo "PetCare-Backend PID: $PETCARE_PID"
cd ..

# 等待5秒
sleep 5

# 启动媒体后端 (端口8081)
echo ""
echo "[Media-Backend] 启动中..."
cd media-backend
mvn spring-boot:run > ../logs/media-backend.log 2>&1 &
MEDIA_PID=$!
echo "Media-Backend PID: $MEDIA_PID"
cd ..

# 等待5秒
sleep 5

# 启动社区后端 (端口8083)
echo ""
echo "[Community-Backend] 启动中..."
cd community-backend
mvn spring-boot:run > ../logs/community-backend.log 2>&1 &
COMMUNITY_PID=$!
echo "Community-Backend PID: $COMMUNITY_PID"
cd ..

# 等待10秒让后端服务完全启动
echo ""
echo "[Backend] 等待后端服务启动..."
sleep 10

# 保存PID到文件
echo "$PETCARE_PID" > logs/petcare-backend.pid
echo "$MEDIA_PID" > logs/media-backend.pid
echo "$COMMUNITY_PID" > logs/community-backend.pid

echo ""
echo "[2/4] 检查后端服务状态..."
echo ""
echo "测试PetCare服务 (8082):"
curl -s http://localhost:8082/api/auth/login || echo "[PetCare] 服务未就绪"
echo ""
echo "测试媒体服务 (8081):"
curl -s http://localhost:8081/api/v1/media || echo "[Media] 服务未就绪"
echo ""
echo "测试社区服务 (8083):"
curl -s http://localhost:8083/api/v1/moments || echo "[Community] 服务未就绪"
echo ""

echo "[3/4] 启动前端服务..."

# 启动前端
echo ""
echo "[Frontend] 启动中..."
cd frontend
npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "Frontend PID: $FRONTEND_PID"
cd ..

# 保存前端PID
echo "$FRONTEND_PID" > logs/frontend.pid

echo ""
echo "[4/4] 所有服务启动完成！"
echo ""
echo "服务地址："
echo "- 前端应用: http://localhost:5173"
echo "- PetCare API: http://localhost:8082"
echo "- 媒体 API: http://localhost:8081"
echo "- 社区 API: http://localhost:8083"
echo ""
echo "日志文件："
echo "- PetCare: logs/petcare-backend.log"
echo "- Media: logs/media-backend.log"
echo "- Community: logs/community-backend.log"
echo "- Frontend: logs/frontend.log"
echo ""
echo "停止所有服务请运行: ./stop-all.sh"
echo ""

# 等待用户输入
echo "按 Ctrl+C 停止监控（服务继续运行）"
tail -f logs/petcare-backend.log logs/media-backend.log logs/community-backend.log logs/frontend.log