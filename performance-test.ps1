<#
.SYNOPSIS
  Pet Care Ecosystem - 性能测试脚本 (PowerShell 版,功能完整)
.DESCRIPTION
  本脚本分两部分:
  第一部分 - API 基准测试: 对 6 个核心端点各发送 50 次请求,统计最小/平均/最大响应时间
  第二部分 - 并发压力测试: 启动 10 个后台作业(模拟 10 个虚拟用户),同时对 /api/media/types
              持续施压 20 秒,统计总请求数、错误数、吞吐量和平均响应时间
  
  运行方式: powershell -ExecutionPolicy Bypass -File performance-test.ps1
  前置条件: 后端微服务已启动,网关端口为 9000
  输出: 自动生成 Documents\性能测试报告.md
.NOTES
  作者: Pet Care Ecosystem Team
  日期: 2026-05-20
#>

# ===== 配置区 =====
# 网关地址（所有微服务的统一入口,端口固定为 9000）
$gateway = "http://localhost:9000"

# 6 个核心测试端点 —— 覆盖 3 个已启动的微服务
#   媒体服务(media-backend):    端点1/3/4/6
#   宠物日常记录(petcare-backend): 端点2/5
$endpoints = @(
    @{name = "1-媒体类型 (public)";          path = "/api/media/types"},
    @{name = "2-宠物信息 (auth)";            path = "/api/pets/1"},
    @{name = "3-媒体详情 (auth)";            path = "/api/media/1"},
    @{name = "4-关联媒体列表 (auth)";        path = "/api/media/related/MOMENT/1"},
    @{name = "5-用户宠物列表 (auth)";        path = "/api/pets/user/2"},
    @{name = "6-不存在媒体 (error场景)";     path = "/api/media/99999"}
)

Write-Host "========================================================" -f Cyan
Write-Host "   Pet Care Ecosystem - 性能测试报告" -f Yellow
Write-Host "   日期: 2026-05-20  目标: $gateway" -f Yellow
Write-Host "========================================================" -f Cyan
Write-Host ""

# ==================== 第一部分：基准测试 ====================
<#
  基准测试逻辑:
  1. 对每个端点依次执行 50 次 HTTP GET 请求
  2. 每次请求使用 curl.exe -w "%{time_total}" 获取精确到微秒的响应时间(秒)
  3. 同时记录每次的 HTTP 状态码,用于确认请求是否被正确处理
  4. 50 次完成后,计算 min/avg/max 并显示
#>
Write-Host "--- 第一部分：API 基准测试 (每个端点 50 次请求) ---" -f Green
Write-Host ""

$globalResults = @()  # 存储所有端点的结果,供结尾汇总表格使用

foreach ($ep in $endpoints) {
    $url = $gateway + $ep.path
    Write-Host "测试: $($ep.name)" -f White
    Write-Host "  URL: $url"
    
    # $times: 存储 50 次响应时间(单位:秒)
    # $statusCodes: 哈希表,记录每种 HTTP 状态码出现次数,例如 {"200"=48, "401"=2}
    $times = @()
    $statusCodes = @{}
    
    for ($i = 0; $i -lt 50; $i++) {
        try {
            # curl.exe -s = 静默模式(不显示进度), -o nul = 丢弃响应体(只在乎响应时间)
            # -w = 自定义输出格式: "%{http_code}:%{time_total}"
            #    http_code = 例如 200/401/400
            #    time_total = 从开始到完成的总秒数(浮点数),精确到微秒
            # 2>$null 屏蔽 curl 的 stderr 输出
            $result = & curl.exe -s -o nul -w "%{http_code}:%{time_total}" $url 2>$null
            $parts = $result -split ':'
            $code = $parts[0]      # HTTP 状态码,如 200, 401, 400
            $sec = [double]$parts[1]  # 响应时间(秒),如 0.007123
            
            $times += $sec
            
            # 更新状态码计数
            if (-not $statusCodes.ContainsKey($code)) {
                $statusCodes[$code] = 0
            }
            $statusCodes[$code]++
        } catch {
            # 请求失败(如连接被拒绝),忽略不计
        }
    }
    
    # 将响应时间从秒转为毫秒,保留 2 位小数
    $avgMs = [math]::Round(($times | Measure-Object -Average).Average * 1000, 2)
    $minMs = [math]::Round(($times | Measure-Object -Minimum).Minimum * 1000, 2)
    $maxMs = [math]::Round(($times | Measure-Object -Maximum).Maximum * 1000, 2)
    
    # 将状态码哈希拼接为可读字符串,如 "200=50"
    $codeStr = ($statusCodes.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join ", "
    
    Write-Host "  > Avg=$avgMs ms | Min=$minMs ms | Max=$maxMs ms | Codes: $codeStr" -f Yellow
    
    # 存入结果列表,供结尾汇总表格使用
    $globalResults += [PSCustomObject]@{
        Endpoint = $ep.name
        Path = $ep.path
        AvgMs = $avgMs
        MinMs = $minMs
        MaxMs = $maxMs
        StatusCodes = $codeStr
    }
    
    Write-Host ""
}

# ==================== 第二部分：并发测试 ====================
<#
  并发压力测试逻辑:
  1. 启动 10 个 PowerShell 后台作业(Start-Job),每个作业模拟一个"虚拟用户"
  2. 每个作业在 20 秒内循环不断发送 GET 请求到 /api/media/types
  3. 每个作业独立记录: 成功请求数、错误数、每次的响应时间
  4. 所有作业完成后,汇总计算: 总请求数、错误总数、平均响应时间、吞吐量(req/s)
  5. 测试结果可反映系统在并发压力下的稳定性、响应时间变化趋势
#>
Write-Host "--- 第二部分：并发测试 (10 并发 × 20 秒, /api/media/types) ---" -f Green
Write-Host ""

$concurrentUrl = $gateway + "/api/media/types"
$jobCount = 10    # 并发虚拟用户数 (VUs),模拟 10 个用户同时访问
$duration = 20    # 测试持续时间(秒)

$jobs = @()
$startTime = Get-Date

for ($j = 0; $j -lt $jobCount; $j++) {
    # Start-Job 在后台启动独立进程,彼此互不影响,模拟多用户真实并发
    # -ScriptBlock: 定义每个 worker 的执行逻辑
    # -ArgumentList: 传给 ScriptBlock 的参数(url, 持续时间)
    $jobs += Start-Job -Name "Worker$j" -ScriptBlock {
        # param() 接收外部传入的参数
        param($url, $durationSec)
        $count = 0      # 本 worker 成功请求计数
        $errors = 0     # 本 worker 错误请求计数
        $times = @()    # 本 worker 每次请求的响应时间集合
        $endTime = (Get-Date).AddSeconds($durationSec)  # 计算结束时间
        
        # 循环发送请求,直到持续时间结束
        while ((Get-Date) -lt $endTime) {
            try {
                # 发送请求同时获取状态码和响应时间(秒)
                $result = & curl.exe -s -o nul -w "%{http_code}:%{time_total}" $url 2>$null
                $parts = $result -split ':'
                $code = $parts[0]       # HTTP 状态码
                $sec = [double]$parts[1] # 响应时间(秒)
                $count++
                $times += $sec
                # HTTP 5xx 视为服务器端错误(网关超时、服务不可用等)
                if ($code -ge 500) { $errors++ }
            } catch {
                $errors++
            }
        }
        
        # 返回本 worker 的统计结果给父进程
        $avgT = if ($times.Count -gt 0) { ($times | Measure-Object -Average).Average * 1000 } else { 0 }
        return @{total=$count; err=$errors; avg=$avgT}
    } -ArgumentList $concurrentUrl, $duration
}

Write-Host "  等待 $duration 秒，$jobCount 个并发作业运行中..." -f Gray
# 多等 5 秒确保最后一个作业已完整完成
Start-Sleep -Seconds ($duration + 5)

# 等待所有后台作业完成并接收返回结果
$concurrentResults = $jobs | Wait-Job | Receive-Job

# ===== 汇总所有 worker 的数据 =====
# 总请求数 = 所有 worker 的 count 之和
$totalReqs = ($concurrentResults | ForEach-Object { $_['total'] } | Measure-Object -Sum).Sum
# 总错误数 = 所有 worker 的 err 之和
$totalErrors = ($concurrentResults | ForEach-Object { $_['err'] } | Measure-Object -Sum).Sum
# 收集所有 worker 的平均响应时间,再求全局平均
$allAvgs = $concurrentResults | ForEach-Object { $_['avg'] }
$conAvg = if ($allAvgs.Count -gt 0) { [math]::Round(($allAvgs | Measure-Object -Average).Average, 2) } else { 0 }
# 吞吐量 = 总请求数 / 测试持续时间(秒)
$throughput = [math]::Round($totalReqs / $duration, 2)

Write-Host "  总请求数: $totalReqs" -f Yellow
Write-Host "  错误数: $totalErrors" -f $(if ($totalErrors -gt 0) { "Red" } else { "Green" })
Write-Host "  平均响应时间(各worker): $conAvg ms" -f Yellow
Write-Host "  吞吐量: $throughput req/s" -f Green
Write-Host ""

# ==================== 总结与报告输出 ====================
Write-Host "--- 性能测试总结 ---" -f Cyan
Write-Host ""
# Format-Table 以表格形式打印所有端点的基准测试结果
$globalResults | Format-Table Endpoint, AvgMs, MinMs, MaxMs, StatusCodes -AutoSize
Write-Host ""
Write-Host "并发测试 (10VU x 20s): 总请求=$totalReqs, 错误=$totalErrors, 吞吐=$throughput req/s, 平均=$conAvg ms" -f White
Write-Host "========================================================" -f Cyan

# ===== 自动生成 Markdown 报告文件(位于 Documents/ 目录下) =====
$reportPath = "Documents\性能测试报告.md"
$lines = @()
$lines += "# 性能测试报告"
$lines += ""
$lines += "> **测试日期**：2026-05-20  **测试工具**：PowerShell + curl.exe"
$lines += ""
$lines += "## 1. API 基准测试 (50 次/端点)"
$lines += ""
$lines += "| # | 端点 | 平均 (ms) | 最小 (ms) | 最大 (ms) | 状态码分布 |"
$lines += "|---|------|-----------|-----------|-----------|-----------|"
foreach ($r in $globalResults) {
    $lines += "| $(($globalResults.IndexOf($r))+1) | $($r.Endpoint) | $($r.AvgMs) | $($r.MinMs) | $($r.MaxMs) | $($r.StatusCodes) |"
}
$lines += ""
$lines += "## 2. 并发压力测试"
$lines += ""
$lines += "| 参数 | 值 |"
$lines += "|------|-----|"
$lines += "| 并发虚拟用户数 | $jobCount |"
$lines += "| 持续时间 | ${duration}s |"
$lines += "| 测试端点 | /api/media/types |"
$lines += "| 总请求数 | $totalReqs |"
$lines += "| 错误数 | $totalErrors |"
$lines += "| 平均响应时间 | $conAvg ms |"
$lines += "| 吞吐量 | $throughput req/s |"
$lines += ""
$lines += "## 3. 截图素材"
$lines += ""
$lines += "| 截图编号 | 内容 | 位置 |"
$lines += "|----------|------|------|"
$lines += "| PS-01 | 基准测试终端输出 (6端点的 Avg/Min/Max) | 终端输出第一部分 |"
$lines += "| PS-02 | 并发测试结果 (10VU x 20s 吞吐量) | 终端输出第二部分 |"

$lines -join "`r`n" | Out-File -FilePath $reportPath -Encoding utf8
Write-Host "报告已保存到 $reportPath" -f Cyan
