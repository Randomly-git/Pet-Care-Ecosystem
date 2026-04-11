package com.petcare.backend.service;

import com.petcare.backend.config.HBaseProperties;
import com.petcare.backend.dto.response.TimelineDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 宠物状态历史 HBase 存储服务
 * 负责宠物状态变更历史的读写操作
 *
 * 表结构：
 * - 表名: <namespace>:pet_status_history
 * - 列族: info
 * - RowKey: {pet_id左补零至10位}_{Long.MAX_VALUE - timestamp}
 *
 * 列名：
 * - info:status_id
 * - info:status_name
 * - info:status_value
 * - info:op_type (UPDATE 或 INSERT)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PetStatusHBaseService {

    private final Connection hbaseConnection;
    private final HBaseProperties hBaseProperties;

    // 表名
    private static final String TABLE_NAME = "pet_status_history";
    // 列族
    private static final byte[] CF_INFO = Bytes.toBytes("info");

    // 列名常量
    private static final byte[] COL_STATUS_ID = Bytes.toBytes("status_id");
    private static final byte[] COL_STATUS_NAME = Bytes.toBytes("status_name");
    private static final byte[] COL_STATUS_VALUE = Bytes.toBytes("status_value");
    private static final byte[] COL_OP_TYPE = Bytes.toBytes("op_type");

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成 HBase RowKey
     * 格式: {pet_id左补零至10位}_{Long.MAX_VALUE - timestamp}
     */
    public String generateRowKey(Long petId, long timestamp) {
        String petIdStr = String.format("%010d", petId);
        long reversedTimestamp = Long.MAX_VALUE - timestamp;
        return petIdStr + "_" + String.format("%019d", reversedTimestamp);
    }

    /**
     * 保存宠物状态变更记录到 HBase
     *
     * @param petId 宠物ID
     * @param statusId 状态ID
     * @param statusName 状态名称
     * @param statusValue 状态值
     * @param timestamp 变更时间戳
     * @param opType 操作类型 (UPDATE 或 INSERT)
     */
    public void saveStatusChange(Long petId, Long statusId, String statusName, String statusValue, long timestamp, String opType) {
        String rowKey = generateRowKey(petId, timestamp);

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Put put = new Put(Bytes.toBytes(rowKey));

            put.addColumn(CF_INFO, COL_STATUS_ID, Bytes.toBytes(String.valueOf(statusId)));
            put.addColumn(CF_INFO, COL_STATUS_NAME, Bytes.toBytes(statusName != null ? statusName : ""));
            put.addColumn(CF_INFO, COL_STATUS_VALUE, Bytes.toBytes(statusValue != null ? statusValue : ""));
            put.addColumn(CF_INFO, COL_OP_TYPE, Bytes.toBytes(opType));

            table.put(put);
            log.info("宠物状态变更记录写入成功: petId={}, statusId={}, opType={}, rowKey={}",
                    petId, statusId, opType, rowKey);

        } catch (IOException e) {
            log.error("宠物状态变更记录写入失败: petId={}, statusId={}, error={}",
                    petId, statusId, e.getMessage(), e);
            throw new RuntimeException("宠物状态变更记录写入失败", e);
        }
    }

    /**
     * 获取完整的表名
     */
    private String getFullTableName() {
        return hBaseProperties.getNamespace() + ":" + TABLE_NAME;
    }

    /**
     * 获取宠物状态变更时间线
     *
     * @param petId 宠物ID
     * @param statusId 状态ID过滤，可为空
     * @param page 页码，从0开始
     * @param size 每页大小，默认50
     * @return 时间线列表
     */
    public List<TimelineDTO> getStatusTimeline(Long petId, Long statusId, int page, int size) {
        String petIdStr = String.format("%010d", petId);
        String minRow = petIdStr + "_"; // 该 petId 的最小 rowKey
        String maxRow = String.format("%010d", petId + 1); // 下一个 petId 的最小 rowKey

        List<TimelineDTO> timeline = new ArrayList<>();

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Scan scan = new Scan();
            // 反向扫描时，startRow 应该是较大值，stopRow 应该是较小值
            scan.setStartRow(Bytes.toBytes(maxRow)); // 从"0000000005"开始
            scan.setStopRow(Bytes.toBytes(minRow)); // 扫到"0000000004_"（不包含）
            scan.setReversed(true); // 倒序扫描，最近的先来
            scan.setCaching(100); // 缓存大小

            // 如果有状态ID过滤
            if (statusId != null) {
                SingleColumnValueFilter filter = new SingleColumnValueFilter(
                    CF_INFO,
                    COL_STATUS_ID,
                    CompareFilter.CompareOp.EQUAL,
                    Bytes.toBytes(String.valueOf(statusId))
                );
                scan.setFilter(filter);
            }

            try (ResultScanner scanner = table.getScanner(scan)) {
                int count = 0;
                int offset = page * size;
                for (Result result : scanner) {
                    if (count >= offset + size) {
                        break; // 超过当前页
                    }
                    if (count >= offset) {
                        TimelineDTO dto = convertResultToTimelineDTO(result);
                        if (dto != null) {
                            timeline.add(dto);
                        }
                    }
                    count++;
                }
            }

        } catch (IOException e) {
            log.error("查询宠物状态时间线失败: petId={}, statusId={}, error={}",
                    petId, statusId, e.getMessage(), e);
            throw new RuntimeException("查询宠物状态时间线失败", e);
        }

        return timeline;
    }

    /**
     * 将 HBase Result 转换为 TimelineDTO
     */
    private TimelineDTO convertResultToTimelineDTO(Result result) {
        if (result.isEmpty()) {
            return null;
        }

        try {
            String rowKey = Bytes.toString(result.getRow());
            String[] parts = rowKey.split("_");
            if (parts.length != 2) {
                return null;
            }

            long reversedTimestamp = Long.parseLong(parts[1]);
            long timestamp = Long.MAX_VALUE - reversedTimestamp;
            LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp / 1000, (int)((timestamp % 1000) * 1_000_000), java.time.ZoneOffset.ofHours(8)); // 假设北京时间

            String statusName = Bytes.toString(result.getValue(CF_INFO, COL_STATUS_NAME));
            String statusValue = Bytes.toString(result.getValue(CF_INFO, COL_STATUS_VALUE));

            return new TimelineDTO(statusName, dateTime.format(FORMATTER), statusValue);

        } catch (Exception e) {
            log.warn("转换 Result 到 TimelineDTO 失败: {}", e.getMessage());
            return null;
        }
    }
}