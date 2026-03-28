package com.petcare.backend.service;

import com.petcare.backend.config.HBaseProperties;
import com.petcare.backend.dto.response.ActivityRecordDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HBase 冷数据存储服务
 * 负责 ActivityRecord 冷数据的读写操作
 *
 * 表结构（单列族精简设计）：
 * - 表名: <namespace>:activity_record
 * - 列族: d
 * - RowKey: {padding_pet_id}_{yyyyMMdd}_{padding_record_id}
 *
 * 列名 (Qualifier) - 仅存储静态业务数据：
 * - d:record_id     <- activity_record_id (主键)
 * - d:activity_id   <- activity_id (活动类型外键)
 * - d:pet_id        <- pet_id (宠物外键)
 * - d:desc          <- activity_description (活动描述)
 * - d:date          <- activity_date (活动日期)
 *
 * 注意：以下字段不存入 HBase，留在 MySQL 或通过关联查询获取：
 * - user_id, activity_name, activity_kind_id, activity_kind_name, pet_name
 * - thaw_expire_time, migration_status (动态状态锁)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HBaseColdStorageService {

    private final Connection hbaseConnection;
    private final HBaseProperties hBaseProperties;

    // 表名
    private static final String TABLE_NAME = "activity_record";
    // 列族（单列族设计）
    private static final String CF_D = "d";
    // RowKey 格式: {pet_id}_{date}_{record_id} (date只用于分区，不含时间)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    // 日期时间格式：存储完整的时间信息，用于读取时还原
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    // 列名常量 - 精简后的核心字段
    private static final byte[] COL_RECORD_ID = Bytes.toBytes("record_id");
    private static final byte[] COL_ACTIVITY_ID = Bytes.toBytes("activity_id");
    private static final byte[] COL_PET_ID = Bytes.toBytes("pet_id");
    private static final byte[] COL_DESC = Bytes.toBytes("desc");
    private static final byte[] COL_DATE = Bytes.toBytes("date");

    /**
     * 生成 HBase RowKey
     * 格式: {pet_id}_{date}_{activity_record_id}
     */
    public String generateRowKey(Long petId, LocalDateTime activityDate, Long activityRecordId) {
        String dateStr = activityDate.format(DATE_FORMATTER);
        return String.format("%015d_%s_%015d", petId, dateStr, activityRecordId);
    }

    /**
     * 保存活动记录到冷库
     *
     * @param dto 活动记录DTO
     * @return 生成的RowKey
     */
    public String saveToColdStorage(ActivityRecordDTO dto) {
        String rowKey = generateRowKey(dto.getPetId(), dto.getActivityDate(), dto.getActivityRecordId());

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            // 先检查是否已存在
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addFamily(Bytes.toBytes(CF_D));
            Result existingResult = table.get(get);

            if (existingResult != null && !existingResult.isEmpty()) {
                log.info("冷库数据已存在，跳过写入: rowKey={}, recordId={}",
                        rowKey, dto.getActivityRecordId());
                return rowKey;
            }

            // 构建 Put - 只存储核心业务字段
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(CF_D), COL_RECORD_ID,
                    Bytes.toBytes(String.valueOf(dto.getActivityRecordId())));
            put.addColumn(Bytes.toBytes(CF_D), COL_ACTIVITY_ID,
                    Bytes.toBytes(String.valueOf(dto.getActivityId() != null ? dto.getActivityId() : 0)));
            put.addColumn(Bytes.toBytes(CF_D), COL_PET_ID,
                    Bytes.toBytes(String.valueOf(dto.getPetId())));
            put.addColumn(Bytes.toBytes(CF_D), COL_DESC,
                    Bytes.toBytes(dto.getActivityDescription() != null ? dto.getActivityDescription() : ""));
            // 使用完整时间格式存储日期时间，保留小时和分钟信息
            put.addColumn(Bytes.toBytes(CF_D), COL_DATE,
                    Bytes.toBytes(dto.getActivityDate().format(DATETIME_FORMATTER)));

            // 写入
            table.put(put);
            log.info("冷库写入成功: rowKey={}, recordId={}", rowKey, dto.getActivityRecordId());

        } catch (IOException e) {
            log.error("冷库写入失败: recordId={}, error={}", dto.getActivityRecordId(), e.getMessage(), e);
            throw new RuntimeException("冷库写入失败: " + e.getMessage(), e);
        }

        return rowKey;
    }

    /**
     * 批量保存活动记录到冷库
     */
    public List<String> batchSaveToColdStorage(List<ActivityRecordDTO> dtos) {
        List<String> rowKeys = new ArrayList<>();

        if (dtos == null || dtos.isEmpty()) {
            return rowKeys;
        }

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            List<Put> puts = new ArrayList<>();

            for (ActivityRecordDTO dto : dtos) {
                String rowKey = generateRowKey(dto.getPetId(), dto.getActivityDate(), dto.getActivityRecordId());
                rowKeys.add(rowKey);

                // 构建 Put - 只存储核心业务字段
                Put put = new Put(Bytes.toBytes(rowKey));
                put.addColumn(Bytes.toBytes(CF_D), COL_RECORD_ID,
                        Bytes.toBytes(String.valueOf(dto.getActivityRecordId())));
                put.addColumn(Bytes.toBytes(CF_D), COL_ACTIVITY_ID,
                        Bytes.toBytes(String.valueOf(dto.getActivityId() != null ? dto.getActivityId() : 0)));
                put.addColumn(Bytes.toBytes(CF_D), COL_PET_ID,
                        Bytes.toBytes(String.valueOf(dto.getPetId())));
                put.addColumn(Bytes.toBytes(CF_D), COL_DESC,
                        Bytes.toBytes(dto.getActivityDescription() != null ? dto.getActivityDescription() : ""));
                // 使用完整时间格式存储日期时间，保留小时和分钟信息
                put.addColumn(Bytes.toBytes(CF_D), COL_DATE,
                        Bytes.toBytes(dto.getActivityDate().format(DATETIME_FORMATTER)));

                puts.add(put);
            }

            table.put(puts);
            log.info("批量写入冷库成功: count={}", dtos.size());

        } catch (IOException e) {
            log.error("批量写入冷库失败: error={}", e.getMessage(), e);
            throw new RuntimeException("批量写入冷库失败: " + e.getMessage(), e);
        }

        return rowKeys;
    }

    /**
     * 从冷库读取单条记录
     */
    public Optional<ActivityRecordDTO> getFromColdStorage(String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addFamily(Bytes.toBytes(CF_D));

            Result result = table.get(get);
            if (result == null || result.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(parseResult(result));
        } catch (IOException e) {
            log.error("读取冷库数据失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
            throw new RuntimeException("读取冷库数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按宠物ID和时间范围查询冷数据
     *
     * 注意：由于 RowKey 格式为 {pet_id}_{date}_{activity_record_id}，
     * 我们需要使用正确的范围查询。startRow 包含，stopRow 不包含。
     *
     * @param petId    宠物ID
     * @param startDate 起始日期（可选，为null时从最早开始）
     * @param endDate   结束日期（可选，为null时到当前日期）
     */
    public List<ActivityRecordDTO> queryByPetIdAndDateRange(Long petId, LocalDateTime startDate, LocalDateTime endDate) {
        List<ActivityRecordDTO> results = new ArrayList<>();

        // 处理 null 日期，设置默认值
        LocalDateTime effectiveStartDate = startDate;
        LocalDateTime effectiveEndDate = endDate;

        if (effectiveStartDate == null) {
            // 默认从一年前开始
            effectiveStartDate = LocalDateTime.now().minusYears(1).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
        }
        if (effectiveEndDate == null) {
            // 默认到当前时间
            effectiveEndDate = LocalDateTime.now();
        }

        // startRow: 从起始日期的第一条记录开始
        String startRow = generateRowKey(petId, effectiveStartDate, 0L);

        // endRow: 使用结束日期的下一天，确保包含结束日期的所有记录
        LocalDateTime nextDay = effectiveEndDate.plusDays(1);
        String endRow = generateRowKey(petId, nextDay, 0L);

        log.debug("【HBase查询】petId={}, startRow={}, endRow={}", petId, startRow, endRow);

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Scan scan = new Scan();
            scan.withStartRow(Bytes.toBytes(startRow), true);
            scan.withStopRow(Bytes.toBytes(endRow), true);
            scan.addFamily(Bytes.toBytes(CF_D));

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    ActivityRecordDTO dto = parseResult(result);
                    if (dto != null) {
                        results.add(dto);
                    }
                }
            }
        } catch (IOException e) {
            log.error("查询冷库数据失败: petId={}, error={}", petId, e.getMessage(), e);
            throw new RuntimeException("查询冷库数据失败: " + e.getMessage(), e);
        }

        log.debug("【HBase查询】完成: petId={}, 找到 {} 条记录", petId, results.size());
        return results;
    }

    /**
     * 检查冷库数据是否存在
     */
    public boolean existsInColdStorage(String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addFamily(Bytes.toBytes(CF_D));
            return table.exists(get);
        } catch (IOException e) {
            log.error("检查冷库数据失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从冷库删除记录
     */
    public void deleteFromColdStorage(String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
            log.info("从冷库删除记录: rowKey={}", rowKey);
        } catch (IOException e) {
            log.error("从冷库删除记录失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
            throw new RuntimeException("从冷库删除记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 HBase Result 为 DTO（精简版）
     * 注意：只解析 HBase 中存储的核心字段，其他字段需要从 MySQL 关联获取
     */
    private ActivityRecordDTO parseResult(Result result) {
        if (result == null || result.isEmpty()) {
            return null;
        }

        try {
            ActivityRecordDTO dto = new ActivityRecordDTO();

            // 解析核心字段
            byte[] recordId = result.getValue(Bytes.toBytes(CF_D), COL_RECORD_ID);
            if (recordId != null) {
                dto.setActivityRecordId(Long.parseLong(Bytes.toString(recordId)));
            }

            byte[] activityId = result.getValue(Bytes.toBytes(CF_D), COL_ACTIVITY_ID);
            if (activityId != null) {
                dto.setActivityId(Long.parseLong(Bytes.toString(activityId)));
            }

            byte[] petId = result.getValue(Bytes.toBytes(CF_D), COL_PET_ID);
            if (petId != null) {
                dto.setPetId(Long.parseLong(Bytes.toString(petId)));
            }

            byte[] desc = result.getValue(Bytes.toBytes(CF_D), COL_DESC);
            if (desc != null) {
                dto.setActivityDescription(Bytes.toString(desc));
            }

            byte[] date = result.getValue(Bytes.toBytes(CF_D), COL_DATE);
            if (date != null) {
                String dateTimeStr = Bytes.toString(date);
                // 尝试使用完整时间格式解析（yyyyMMddHHmm），如果失败则使用日期格式（yyyyMMdd）兼容旧数据
                try {
                    LocalDateTime activityDateTime = LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
                    dto.setActivityDate(activityDateTime);
                } catch (Exception e) {
                    // 兼容旧数据：使用日期格式解析
                    LocalDate localDate = LocalDate.parse(dateTimeStr, DATE_FORMATTER);
                    dto.setActivityDate(localDate.atStartOfDay());
                    log.debug("【兼容旧数据】使用日期格式解析: rowKey={}",
                            Bytes.toString(result.getRow()));
                }
            }

            return dto;
        } catch (Exception e) {
            log.error("解析冷库数据失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取完整的表名（包含命名空间）
     */
    private String getFullTableName() {
        return hBaseProperties.getNamespace() + ":" + TABLE_NAME;
    }
}
