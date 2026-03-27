package com.petcare.backend.controller;

import com.petcare.backend.service.ActivityColdDataMigrationJob;
import com.petcare.backend.service.ActivityService;
import com.petcare.backend.service.HBaseColdStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 冷数据管理 Admin API
 * 用于手动触发迁移任务、查询统计信息等测试操作
 */
@RestController
@RequestMapping("/api/admin/cold-storage")
@Slf4j
@Tag(name = "冷数据管理", description = "冷数据迁移和查询的管理接口（仅用于测试）")
@RequiredArgsConstructor
public class ColdStorageAdminController {

    private final ActivityColdDataMigrationJob migrationJob;
    private final ActivityService activityService;
    private final HBaseColdStorageService hBaseColdStorageService;

    /**
     * 手动触发冷数据迁移任务
     * POST /api/admin/cold-storage/migration/trigger
     */
    @PostMapping("/migration/trigger")
    @Operation(summary = "手动触发迁移", description = "手动触发 ActivityRecord 冷数据迁移任务")
    public ResponseEntity<Map<String, Object>> triggerMigration() {
        log.info("【Admin API】手动触发冷数据迁移任务");

        try {
            migrationJob.manualMigrate();
            var stats = migrationJob.getMigrationStats();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "迁移任务已触发执行");
            result.put("stats", stats);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("【Admin API】迁移任务执行失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "迁移任务执行失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 获取迁移统计信息
     * GET /api/admin/cold-storage/migration/stats
     */
    @GetMapping("/migration/stats")
    @Operation(summary = "获取迁移统计", description = "获取冷数据迁移的统计信息")
    public ResponseEntity<?> getMigrationStats() {
        log.info("【Admin API】获取迁移统计信息");
        return ResponseEntity.ok(migrationJob.getMigrationStats());
    }

    /**
     * 测试查询冷数据（指定宠物和时间范围）
     * GET /api/admin/cold-storage/query
     */
    @GetMapping("/query")
    @Operation(summary = "测试查询冷数据", description = "按宠物ID和时间范围查询活动记录（热数据+冷数据）")
    public ResponseEntity<?> queryActivityRecords(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("【Admin API】查询活动记录: petId={}, startDate={}, endDate={}", petId, startDate, endDate);

        var records = activityService.queryActivityRecords(petId, startDate, endDate);

        Map<String, Object> result = new HashMap<>();
        result.put("petId", petId);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("totalCount", records.size());
        result.put("records", records);

        return ResponseEntity.ok(result);
    }

    /**
     * 测试从 HBase 读取单条冷数据
     * GET /api/admin/cold-storage/hbase/get
     */
    @GetMapping("/hbase/get")
    @Operation(summary = "测试HBase单条读取", description = "根据 RowKey 从 HBase 读取单条冷数据")
    public ResponseEntity<?> getFromHBase(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "活动记录ID") @RequestParam Long activityRecordId,
            @Parameter(description = "活动日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime activityDate) {

        log.info("【Admin API】从HBase读取冷数据: petId={}, recordId={}, date={}",
                petId, activityRecordId, activityDate);

        String rowKey = hBaseColdStorageService.generateRowKey(petId, activityDate, activityRecordId);
        log.info("【Admin API】生成的RowKey: {}", rowKey);

        var record = hBaseColdStorageService.getFromColdStorage(rowKey);

        Map<String, Object> result = new HashMap<>();
        result.put("rowKey", rowKey);
        result.put("found", record.isPresent());
        record.ifPresent(r -> result.put("record", r));

        return ResponseEntity.ok(result);
    }

    /**
     * 生成 RowKey（用于调试）
     * GET /api/admin/cold-storage/rowkey/generate
     */
    @GetMapping("/rowkey/generate")
    @Operation(summary = "生成RowKey", description = "根据参数生成 HBase RowKey")
    public ResponseEntity<?> generateRowKey(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "活动日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime activityDate,
            @Parameter(description = "活动记录ID") @RequestParam Long activityRecordId) {

        String rowKey = hBaseColdStorageService.generateRowKey(petId, activityDate, activityRecordId);

        Map<String, Object> result = new HashMap<>();
        result.put("petId", petId);
        result.put("activityDate", activityDate);
        result.put("activityRecordId", activityRecordId);
        result.put("rowKey", rowKey);

        return ResponseEntity.ok(result);
    }
}
