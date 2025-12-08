@echo off
echo ========================================
echo 启动宠物关爱生态系统 - 所有服务
echo ========================================
echo.

:: 检查Java是否安装
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Java，请先安装Java 17或更高版本
    pause
    exit /b 1
)

:: 检查Node.js是否安装
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Node.js，请先安装Node.js
    pause
    exit /b 1
)

echo [1/4] 启动后端微服务...

:: 启动PetCare后端 (端口8082)
echo.
echo [PetCare-Backend] 启动中...
start "PetCare-Backend" cmd /k "cd /d %~dp0petcare-backend && mvn spring-boot:run"

:: 等待5秒让PetCare后端先启动
timeout /t 5 /nobreak >nul

:: 启动媒体后端 (端口8081)
echo.
echo [Media-Backend] 启动中...
start "Media-Backend" cmd /k "cd /d %~dp0media-backend && mvn spring-boot:run"

:: 等待5秒
timeout /t 5 /nobreak >nul

:: 启动社区后端 (端口8083)
echo.
echo [Community-Backend] 启动中...
start "Community-Backend" cmd /k "cd /d %~dp0community-backend && mvn spring-boot:run"

:: 等待10秒让后端服务完全启动
echo.
echo [Backend] 等待后端服务启动...
timeout /t 10 /nobreak >nul

echo [2/4] 检查后端服务状态...
echo.
echo 测试PetCare服务 (8082):
curl -s http://localhost:8082/api/auth/login || echo [PetCare] 服务未就绪
echo.
echo 测试媒体服务 (8081):
curl -s http://localhost:8081/api/v1/media || echo [Media] 服务未就绪
echo.
echo 测试社区服务 (8083):
curl -s http://localhost:8083/api/v1/moments || echo [Community] 服务未就绪
echo.

echo [3/4] 启动前端服务...

:: 启动前端
echo.
echo [Frontend] 启动中...
start "Frontend" cmd /k "cd /d %~dp0frontend && npm run dev"

echo.
echo [4/4] 所有服务启动完成！
echo.
echo 服务地址：
echo - 前端应用: http://localhost:5173
echo - PetCare API: http://localhost:8082
echo - 媒体 API: http://localhost:8081
echo - 社区 API: http://localhost:8083
echo.
echo 提示：所有服务都在独立的命令行窗口中运行
echo 关闭此窗口不会停止服务，需要逐个关闭服务窗口
echo.
pause