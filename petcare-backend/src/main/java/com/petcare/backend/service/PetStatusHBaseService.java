package com.petcare.backend.service;

import com.petcare.backend.config.HBaseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
    private static final String CF_INFO = "info";

    // 列名常量
    private static final byte[] COL_STATUS_ID = Bytes.toBytes("status_id");
    private static final byte[] COL_STATUS_NAME = Bytes.toBytes("status_name");
    private static final byte[] COL_STATUS_VALUE = Bytes.toBytes("status_value");
    private static final byte[] COL_OP_TYPE = Bytes.toBytes("op_type");

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

            put.addColumn(Bytes.toBytes(CF_INFO), COL_STATUS_ID, Bytes.toBytes(String.valueOf(statusId)));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_STATUS_NAME, Bytes.toBytes(statusName != null ? statusName : ""));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_STATUS_VALUE, Bytes.toBytes(statusValue != null ? statusValue : ""));
            put.addColumn(Bytes.toBytes(CF_INFO), COL_OP_TYPE, Bytes.toBytes(opType));

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
}