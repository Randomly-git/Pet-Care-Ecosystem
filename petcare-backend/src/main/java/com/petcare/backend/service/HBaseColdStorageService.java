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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HBase 冷数据存储服务
 * 负责 ActivityRecord 冷数据的读写操作
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HBaseColdStorageService {

    private final Connection hbaseConnection;
    private final HBaseProperties hBaseProperties;

    // 表名
    private static final String TABLE_NAME = "activity_record";
    // 列族
    private static final String CF_INFO = "info";
    private static final String CF_METADATA = "metadata";
    // RowKey 格式: {pet_id}_{date}_{activity_record_id}
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 列名 - info
    private static final byte[] COL_ACTIVITY_RECORD_ID = Bytes.toBytes("activity_record_id");
    private static final byte[] COL_ACTIVITY_ID = Bytes.toBytes("activity_id");
    private static final byte[] COL_ACTIVITY_NAME = Bytes.toBytes("activity_name");
    private static final byte[] COL_ACTIVITY_DESCRIPTION = Bytes.toBytes("activity_description");
    private static final byte[] COL_ACTIVITY_DATE = Bytes.toBytes("activity_date");
    private static final byte[] COL_PET_ID = Bytes.toBytes("pet_id");
    private static final byte[] COL_USER_ID = Bytes.toBytes("user_id");
    private static final byte[] COL_ACTIVITY_KIND_ID = Bytes.toBytes("activity_kind_id");
    private static final byte[] COL_ACTIVITY_KIND_NAME = Bytes.toBytes("activity_kind_name");
    private static final byte[] COL_PET_NAME = Bytes.toBytes("pet_name");

    // 列名 - metadata
    private static final byte[] COL_CREATED_AT = Bytes.toBytes("created_at");
    private static final byte[] COL_LAST_ACCESS_TIME = Bytes.toBytes("last_access_time");
    private static final byte[] COL_MIGRATED_AT = Bytes.toBytes("migrated_at");

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
            get.addFamily(Bytes.toBytes(CF_INFO));
            Result existingResult = table.get(get);
            
            if (existingResult != null && !existingResult.isEmpty()) {
                log.info("冷库数据已存在，跳过写入: rowKey={}, activityRecordId={}", 
                        rowKey, dto.getActivityRecordId());
                return rowKey;
            }

            // 构建 Put
            Put put = new Put(Bytes.toBytes(rowKey));

            // info 列族
            put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_RECORD_ID, 
                    Bytes.toBytes(String.valueOf(dto.getActivityRecordId())));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_ID, 
                    Bytes.toBytes(String.valueOf(dto.getActivityId())));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_NAME, 
                    Bytes.toBytes(dto.getActivityName() != null ? dto.getActivityName() : ""));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_DESCRIPTION, 
                    Bytes.toBytes(dto.getActivityDescription() != null ? dto.getActivityDescription() : ""));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_DATE, 
                    Bytes.toBytes(dto.getActivityDate().toString()));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_PET_ID, 
                    Bytes.toBytes(String.valueOf(dto.getPetId())));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_USER_ID, 
                    Bytes.toBytes(String.valueOf(dto.getUserId() != null ? dto.getUserId() : 0)));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_KIND_ID, 
                    Bytes.toBytes(String.valueOf(dto.getActivityKindId() != null ? dto.getActivityKindId() : 0)));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_KIND_NAME, 
                    Bytes.toBytes(dto.getActivityKindName() != null ? dto.getActivityKindName() : ""));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_PET_NAME, 
                    Bytes.toBytes(dto.getPetName() != null ? dto.getPetName() : ""));

            // metadata 列族
            put.addColumn(Bytes.toBytes(CF_METADATA), COL_CREATED_AT, 
                    Bytes.toBytes(LocalDateTime.now().toString()));
            put.addColumn(Bytes.toBytes(CF_METADATA), COL_LAST_ACCESS_TIME, 
                    Bytes.toBytes(LocalDateTime.now().toString()));

            // 写入
            table.put(put);
            log.info("冷库写入成功: rowKey={}, activityRecordId={}", rowKey, dto.getActivityRecordId());
            
        } catch (IOException e) {
            log.error("冷库写入失败: activityRecordId={}, error={}", dto.getActivityRecordId(), e.getMessage(), e);
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

                // 构建 Put
                Put put = new Put(Bytes.toBytes(rowKey));

                // info 列族
                put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_RECORD_ID, 
                        Bytes.toBytes(String.valueOf(dto.getActivityRecordId())));
                put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_ID, 
                        Bytes.toBytes(String.valueOf(dto.getActivityId())));
                put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_NAME, 
                        Bytes.toBytes(dto.getActivityName() != null ? dto.getActivityName() : ""));
                put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_DESCRIPTION, 
                        Bytes.toBytes(dto.getActivityDescription() != null ? dto.getActivityDescription() : ""));
                put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_DATE, 
                        Bytes.toBytes(dto.getActivityDate().toString()));
                put.addColumn(Bytes.toBytes(CF_INFO), COL_PET_ID, 
                        Bytes.toBytes(String.valueOf(dto.getPetId())));
                put.addColumn(Bytes.toBytes(CF_INFO), COL_USER_ID, 
                        Bytes.toBytes(String.valueOf(dto.getUserId() != null ? dto.getUserId() : 0)));
                put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_KIND_ID, 
                        Bytes.toBytes(String.valueOf(dto.getActivityKindId() != null ? dto.getActivityKindId() : 0)));
                put.addColumn(Bytes.toBytes(CF_INFO), COL_ACTIVITY_KIND_NAME, 
                        Bytes.toBytes(dto.getActivityKindName() != null ? dto.getActivityKindName() : ""));
                put.addColumn(Bytes.toBytes(CF_INFO), COL_PET_NAME, 
                        Bytes.toBytes(dto.getPetName() != null ? dto.getPetName() : ""));

                // metadata 列族
                put.addColumn(Bytes.toBytes(CF_METADATA), COL_CREATED_AT, 
                        Bytes.toBytes(LocalDateTime.now().toString()));
                put.addColumn(Bytes.toBytes(CF_METADATA), COL_LAST_ACCESS_TIME, 
                        Bytes.toBytes(LocalDateTime.now().toString()));

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
            get.addFamily(Bytes.toBytes(CF_INFO));
            get.addFamily(Bytes.toBytes(CF_METADATA));

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
     */
    public List<ActivityRecordDTO> queryByPetIdAndDateRange(Long petId, LocalDateTime startDate, LocalDateTime endDate) {
        List<ActivityRecordDTO> results = new ArrayList<>();
        
        // startRow: 从起始日期的第一条记录开始
        String startRow = generateRowKey(petId, startDate, 0L);
        
        // endRow: 使用结束日期的下一天，确保包含结束日期的所有记录
        LocalDateTime nextDay = endDate.plusDays(1);
        String endRow = generateRowKey(petId, nextDay, 0L);

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Scan scan = new Scan();
            scan.withStartRow(Bytes.toBytes(startRow), true);
            scan.withStopRow(Bytes.toBytes(endRow), true);
            scan.addFamily(Bytes.toBytes(CF_INFO));
            scan.addFamily(Bytes.toBytes(CF_METADATA));

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

        return results;
    }

    /**
     * 检查冷库数据是否存在
     */
    public boolean existsInColdStorage(String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addFamily(Bytes.toBytes(CF_INFO));
            return table.exists(get);
        } catch (IOException e) {
            log.error("检查冷库数据失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新冷库中的最后访问时间
     */
    public void updateLastAccessTime(String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(CF_METADATA), COL_LAST_ACCESS_TIME, 
                    Bytes.toBytes(LocalDateTime.now().toString()));
            table.put(put);
            log.debug("更新冷库最后访问时间: rowKey={}", rowKey);
        } catch (IOException e) {
            log.error("更新冷库最后访问时间失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
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
     * 解析 HBase Result 为 DTO
     */
    private ActivityRecordDTO parseResult(Result result) {
        if (result == null || result.isEmpty()) {
            return null;
        }

        try {
            ActivityRecordDTO dto = new ActivityRecordDTO();
            
            // info 列族
            byte[] activityRecordId = result.getValue(Bytes.toBytes(CF_INFO), COL_ACTIVITY_RECORD_ID);
            if (activityRecordId != null) {
                dto.setActivityRecordId(Long.parseLong(Bytes.toString(activityRecordId)));
            }

            byte[] activityId = result.getValue(Bytes.toBytes(CF_INFO), COL_ACTIVITY_ID);
            if (activityId != null) {
                dto.setActivityId(Long.parseLong(Bytes.toString(activityId)));
            }

            byte[] activityName = result.getValue(Bytes.toBytes(CF_INFO), COL_ACTIVITY_NAME);
            if (activityName != null) {
                dto.setActivityName(Bytes.toString(activityName));
            }

            byte[] activityDescription = result.getValue(Bytes.toBytes(CF_INFO), COL_ACTIVITY_DESCRIPTION);
            if (activityDescription != null) {
                dto.setActivityDescription(Bytes.toString(activityDescription));
            }

            byte[] activityDate = result.getValue(Bytes.toBytes(CF_INFO), COL_ACTIVITY_DATE);
            if (activityDate != null) {
                dto.setActivityDate(LocalDateTime.parse(Bytes.toString(activityDate)));
            }

            byte[] petId = result.getValue(Bytes.toBytes(CF_INFO), COL_PET_ID);
            if (petId != null) {
                dto.setPetId(Long.parseLong(Bytes.toString(petId)));
            }

            byte[] userId = result.getValue(Bytes.toBytes(CF_INFO), COL_USER_ID);
            if (userId != null) {
                dto.setUserId(Long.parseLong(Bytes.toString(userId)));
            }

            byte[] activityKindId = result.getValue(Bytes.toBytes(CF_INFO), COL_ACTIVITY_KIND_ID);
            if (activityKindId != null) {
                dto.setActivityKindId(Long.parseLong(Bytes.toString(activityKindId)));
            }

            byte[] activityKindName = result.getValue(Bytes.toBytes(CF_INFO), COL_ACTIVITY_KIND_NAME);
            if (activityKindName != null) {
                dto.setActivityKindName(Bytes.toString(activityKindName));
            }

            byte[] petName = result.getValue(Bytes.toBytes(CF_INFO), COL_PET_NAME);
            if (petName != null) {
                dto.setPetName(Bytes.toString(petName));
            }

            // metadata 列族 - 注意：简化设计后不再需要 lastAccessTime

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
