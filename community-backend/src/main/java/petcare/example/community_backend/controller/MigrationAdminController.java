package petcare.example.community_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petcare.example.community_backend.service.CommunityColdDataMigrationJob;
import petcare.example.community_backend.config.HBaseProperties;
import petcare.example.community_backend.service.CommunityHBaseColdStorageService;
import petcare.example.community_backend.repository.PetMomentRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
   
/**
 * 社区冷数据迁移管理接口
 * 用于手动触发和管理冷数据迁移任务
 */
@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "冷数据迁移管理接口", description = "用于手动触发和管理社区冷数据迁移任务")
public class MigrationAdminController {

    private final CommunityColdDataMigrationJob migrationJob;
    private final CommunityHBaseColdStorageService hBaseService;
    private final PetMomentRepository momentRepository;
    private final HBaseProperties hBaseProperties;

    /**
     * POST /api/admin/migration/trigger
     * 手动触发冷数据迁移扫描任务
     */
    @PostMapping("/trigger")
    @Operation(summary = "手动触发迁移任务", description = "手动触发冷数据迁移扫描任务，立即执行迁移逻辑")
    public ResponseEntity<Map<String, Object>> triggerMigration() {
        long startTime = System.currentTimeMillis();
        log.info("【管理接口】手动触发冷数据迁移任务");

        try {
            // 使用和定时任务相同的 TransactionTemplate 确保事务一致性
            CommunityColdDataMigrationJob.MigrationStats stats = migrationJob.executeMigrationWithTransaction();
            long elapsed = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "冷数据迁移任务执行完成");
            result.put("stats", Map.of(
                    "success", stats.getSuccess(),
                    "skipped", stats.getSkipped(),
                    "failed", stats.getFailed(),
                    "elapsedMs", stats.getElapsedMs()
            ));
            result.put("triggeredAt", LocalDateTime.now().toString());
            result.put("totalTimeMs", elapsed);

            log.info("【管理接口】冷数据迁移任务执行完成: success={}, skipped={}, failed={}, 耗时={}ms",
                    stats.getSuccess(), stats.getSkipped(), stats.getFailed(), elapsed);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("【管理接口】冷数据迁移任务执行失败: error={}", e.getMessage(), e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "冷数据迁移任务执行失败: " + e.getMessage());
            result.put("triggeredAt", LocalDateTime.now().toString());

            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * GET /api/admin/migration/status
     * 查询迁移状态和待迁移数据统计
     */
    @GetMapping("/status")
    @Operation(summary = "查询迁移状态", description = "查询当前迁移任务状态和待迁移数据统计")
    public ResponseEntity<Map<String, Object>> getMigrationStatus() {
        log.info("【管理接口】查询迁移状态");

        HBaseProperties.ColdData.Community config = hBaseProperties.getColdData().getCommunity();
        LocalDateTime approvedThreshold = LocalDateTime.now().minusDays(config.getApprovedDaysThreshold());
        LocalDateTime rejectedThreshold = LocalDateTime.now().minusDays(config.getRejectedDaysThreshold());

        long pendingCount = momentRepository.countPendingMigrationRecords(approvedThreshold, rejectedThreshold);
        long migratingCount = momentRepository.countMigratingRecords();
        long totalCount = momentRepository.count();

        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("approvedThresholdDays", config.getApprovedDaysThreshold());
        result.put("rejectedThresholdDays", config.getRejectedDaysThreshold());
        result.put("statistics", Map.of(
                "totalMoments", totalCount,
                "pendingMigration", pendingCount,
                "migrating", migratingCount
        ));

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/admin/migration/enable
     * 启用迁移任务
     */
    @PostMapping("/enable")
    @Operation(summary = "启用迁移任务", description = "启用定时迁移任务（定时任务仍按 cron 表达式执行）")
    public ResponseEntity<Map<String, Object>> enableMigration() {
        log.info("【管理接口】启用迁移任务");
        migrationJob.setMigrationEnabled(true);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "迁移任务已启用");
        result.put("enabled", true);
        result.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/admin/migration/disable
     * 禁用迁移任务
     */
    @PostMapping("/disable")
    @Operation(summary = "禁用迁移任务", description = "禁用定时迁移任务（定时任务将跳过执行）")
    public ResponseEntity<Map<String, Object>> disableMigration() {
        log.info("【管理接口】禁用迁移任务");
        migrationJob.setMigrationEnabled(false);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "迁移任务已禁用");
        result.put("enabled", false);
        result.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/migration/hbase/check
     * 检查 HBase 连接状态
     */
    @GetMapping("/hbase/check")
    @Operation(summary = "检查HBase连接", description = "检查 HBase 冷存储服务连接状态")
    public ResponseEntity<Map<String, Object>> checkHBaseConnection() {
        log.info("【管理接口】检查 HBase 连接状态");

        try {
            boolean connected = hBaseService.isConnected();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("connected", connected);
            result.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("【管理接口】HBase 连接检查失败: error={}", e.getMessage());

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("connected", false);
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.internalServerError().body(result);
        }
    }
}
