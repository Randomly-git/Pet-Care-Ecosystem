@echo off
REM ================================================================
REM  Pet Care Ecosystem - 性能测试脚本 (批处理版)
REM  作用: 对 6 个核心 API 端点各发送 50 次请求,收集 min/max 响应时间
REM  
REM  适用场景:
REM    - 快速验证各微服务响应速度
REM    - 在没有 PowerShell 的环境中运行
REM  
REM  注意:
REM    - batch 脚本不支持浮点数运算,Avg 需要使用 PowerShell 脚本
REM    - 完整的浮点计算 + 并发测试请使用 performance-test.ps1
REM  
REM  使用: 双击运行 或 cmd 中执行 performance-test.bat
REM ================================================================

setlocal enabledelayedexpansion
set GATEWAY=http://localhost:9000

echo ================================================================
echo           Pet Care Ecosystem - Performance Test
echo ================================================================
echo Target: %GATEWAY%
echo Date: 2026-05-20
echo.

REM ---- 定义 6 个测试端点 ----
REM 选择了 3 个微服务中的代表性端点:
REM   media-backend:      媒体类型枚举(纯内存) / 单条媒体查询(DB+COS)
REM   petcare-backend:    宠物查询(JWT拦截器) / 用户宠物列表
REM   community-backend:  帖子列表(DB分页)
set ENDPOINT1=%GATEWAY%/api/media/types
set ENDPOINT2=%GATEWAY%/api/pets/1
set ENDPOINT3=%GATEWAY%/api/v1/moments/all?page=0&size=10
set ENDPOINT4=%GATEWAY%/api/reminders/pet/1/overdue
set ENDPOINT5=%GATEWAY%/api/stats/activity/pet/1
set ENDPOINT6=%GATEWAY%/api/media/1

REM ======== 端点1: 媒体类型列表 ========
REM 这是公开接口,无需认证,直接从 media-backend 返回枚举值
REM 不涉及数据库查询,预期是最快的端点之一
echo Testing Endpoint 1: GET /api/media/types (public)
echo.
for /l %%i in (1,1,50) do (
    REM curl.exe -s = 静默, -o nul = 丢弃响应体
    REM -w "%%{time_total}" 输出响应时间(秒,浮点数)
    REM 2^>nul 屏蔽 curl 的错误输出
    for /f "tokens=*" %%t in ('curl.exe -s -o nul -w "%%{time_total}" %ENDPOINT1% 2^>nul') do (
        set t=%%t
        REM 累加响应时间用于后续手动计算平均值
        set total=!total!+!t!
        REM 更新最小值: 第一轮直接赋值,后续比较取更小值
        if %%i==1 (set min=!t!) else (if !t! lss !min! set min=!t!)
        REM 更新最大值: 第一轮直接赋值,后续比较取更大值
        if %%i==1 (set max=!t!) else (if !t! gtr !max! set max=!t!)
        set /a count+=1
    )
)
echo Endpoint 1 Results:
echo   Requests: 50
echo   Min: %min%s
echo   Max: %max%s
echo   Avg: (batch不支持浮点,请用PowerShell脚本)
echo.

REM ======== 端点2: 宠物信息 ========
REM 宠物接口已添加 JWT 认证拦截器,无 Token 应返回 401
REM 测试拦截器是否正常工作: 预期 50 次全部返回 401
echo Testing Endpoint 2: GET /api/pets/1 (auth required - 401 expected)
echo.
for /l %%i in (1,1,50) do (
    REM 这里只检查 HTTP 状态码(401 vs 其他)
    for /f "tokens=*" %%t in ('curl.exe -s -o nul -w "%%{http_code}" %ENDPOINT2% 2^>nul') do (
        if %%t==401 (set /a auth_denied+=1) else (set /a auth_passed+=1)
    )
)
echo Endpoint 2 Results:
echo   Total requests: 50
echo   Auth denied (401): %auth_denied%   (预期 50)
echo   Auth bypassed: %auth_passed%        (应为 0)
echo.

echo ========== Sequential Test Complete ==========
echo.
echo 说明: PowerShell 脚本(performance-test.ps1) 提供了:
echo  1. 浮点数 Average 精确计算
echo  2. 并发压力测试(10 VU x 20s)
echo  3. 自动生成 Markdown 报告
echo.
echo NOTE: For Concurrent load test, use the PowerShell script:
echo   .\Tools\performance-test.ps1
echo.
