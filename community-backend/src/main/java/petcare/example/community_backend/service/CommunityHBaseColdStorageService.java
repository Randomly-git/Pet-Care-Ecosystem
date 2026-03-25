package petcare.example.community_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Service;
import petcare.example.community_backend.config.HBaseProperties;
import petcare.example.community_backend.dto.ColdArchiveData;
import petcare.example.community_backend.dto.HBaseArchiveRecord;
import petcare.example.community_backend.util.GzipUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 社区模块 HBase 冷数据存储服务（扁平化单表设计）
 *
 * 设计原则：
 * 1. 单表存储：所有数据存入 community_moments_archive 表
 * 2. 扁平化归档：评论和点赞序列化到 full_data 列
 * 3. GZIP 压缩：节省存储空间
 *
 * 表结构：
 * - community_moments_archive: RowKey = {userId前4位}_{momentId}
 *
 * 列结构：
 * d:moment_id      -> 动态ID
 * d:user_id        -> 作者用户ID
 * d:content        -> 动态内容
 * d:created_at     -> 创建时间
 * d:full_data      -> GZIP压缩的JSON（二进制）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityHBaseColdStorageService {

    private final Connection hbaseConnection;
    private final HBaseProperties hBaseProperties;
    private final ObjectMapper objectMapper;

    // ==================== 表名常量 ====================
    private static final String TABLE_ARCHIVE = "community_moments_archive";

    // ==================== 列族常量 ====================
    private static final byte[] CF_D = Bytes.toBytes("d");

    // ==================== 列名常量 ====================
    private static final byte[] COL_MOMENT_ID = Bytes.toBytes("moment_id");
    private static final byte[] COL_USER_ID = Bytes.toBytes("user_id");
    private static final byte[] COL_CONTENT = Bytes.toBytes("content");
    private static final byte[] COL_CREATED_AT = Bytes.toBytes("created_at");
    private static final byte[] COL_FULL_DATA = Bytes.toBytes("full_data");

    // ==================== RowKey 生成方法 ====================

    /**
     * 生成归档表 RowKey
     * 格式: {user_id前4位(盐值)}_{moment_id}
     *
     * 示例: 0027_10086 (userId=27, momentId=10086)
     */
    public String generateRowKey(Long userId, Long momentId) {
        String userPrefix = String.format("%04d", userId % 10000);
        return userPrefix + "_" + momentId;
    }

    /**
     * 检查数据是否存在（幂等性保证）
     */
    public boolean exists(Long userId, Long momentId) {
        String rowKey = generateRowKey(userId, momentId);
        return existsByRowKey(rowKey);
    }

    /**
     * 检查数据是否存在（通过 RowKey）
     */
    public boolean existsByRowKey(String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Get get = new Get(Bytes.toBytes(rowKey));
            return table.exists(get);
        } catch (IOException e) {
            log.error("【HBase】检查数据存在性失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
            return false;
        }
    }

    // ==================== 保存归档记录 ====================

    /**
     * 保存归档记录到 HBase（扁平化单行存储）
     *
     * @param record 归档记录
     * @param archiveData 归档数据（评论+点赞）
     */
    public void saveArchive(HBaseArchiveRecord record, ColdArchiveData archiveData) {
        String rowKey = generateRowKey(record.getUserId(), record.getMomentId());

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Put put = new Put(Bytes.toBytes(rowKey));

            // 核心字段
            put.addColumn(CF_D, COL_MOMENT_ID, Bytes.toBytes(String.valueOf(record.getMomentId())));
            put.addColumn(CF_D, COL_USER_ID, Bytes.toBytes(String.valueOf(record.getUserId())));
            put.addColumn(CF_D, COL_CONTENT, Bytes.toBytes(record.getContent() != null ? record.getContent() : ""));
            put.addColumn(CF_D, COL_CREATED_AT, Bytes.toBytes(
                    record.getCreatedAt() != null ? record.getCreatedAt().toString() : LocalDateTime.now().toString()));

            // 扁平化归档数据（评论+点赞）- GZIP 压缩
            if (archiveData != null) {
                String json = objectMapper.writeValueAsString(archiveData);
                byte[] compressedData = GzipUtils.compressIfNeeded(json);
                put.addColumn(CF_D, COL_FULL_DATA, compressedData);

                log.info("【HBase】归档数据已序列化: momentId={}, 评论数={}, 点赞数={}, JSON大小={} bytes, 压缩后={} bytes",
                        record.getMomentId(),
                        archiveData.getComments() != null ? archiveData.getComments().size() : 0,
                        archiveData.getLikes() != null ? archiveData.getLikes().size() : 0,
                        json.getBytes().length,
                        compressedData.length);
            }

            table.put(put);
            log.info("【HBase】归档记录写入成功: momentId={}, userId={}, rowKey={}", record.getMomentId(), record.getUserId(), rowKey);

        } catch (IOException e) {
            log.error("【HBase】归档记录写入失败: momentId={}, error={}", record.getMomentId(), e.getMessage(), e);
            throw new RuntimeException("归档记录写入失败", e);
        }
    }

    // ==================== 读取归档记录 ====================

    /**
     * 读取归档记录（包含反序列化 full_data）
     *
     * @param userId 用户ID
     * @param momentId 动态ID
     * @return 归档记录（包含扁平化数据）
     */
    public Optional<HBaseArchiveRecord> getArchive(Long userId, Long momentId) {
        String rowKey = generateRowKey(userId, momentId);
        return getArchiveByRowKey(rowKey);
    }

    /**
     * 读取归档记录（通过 RowKey）
     */
    public Optional<HBaseArchiveRecord> getArchiveByRowKey(String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addFamily(CF_D);

            Result result = table.get(get);
            if (result == null || result.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(parseArchiveRecord(result, rowKey));

        } catch (IOException e) {
            log.error("【HBase】读取归档记录失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
            throw new RuntimeException("读取归档记录失败", e);
        }
    }

    /**
     * 解析归档记录
     */
    private HBaseArchiveRecord parseArchiveRecord(Result result, String rowKey) {
        try {
            HBaseArchiveRecord.HBaseArchiveRecordBuilder builder = HBaseArchiveRecord.builder();

            // 核心字段
            byte[] momentId = result.getValue(CF_D, COL_MOMENT_ID);
            if (momentId != null) builder.momentId(Long.parseLong(Bytes.toString(momentId)));

            byte[] userId = result.getValue(CF_D, COL_USER_ID);
            if (userId != null) builder.userId(Long.parseLong(Bytes.toString(userId)));

            byte[] content = result.getValue(CF_D, COL_CONTENT);
            if (content != null) builder.content(Bytes.toString(content));

            byte[] createdAt = result.getValue(CF_D, COL_CREATED_AT);
            if (createdAt != null) builder.createdAt(LocalDateTime.parse(Bytes.toString(createdAt)));

            // full_data - GZIP 解压并反序列化
            byte[] fullData = result.getValue(CF_D, COL_FULL_DATA);
            if (fullData != null && fullData.length > 0) {
                String json = GzipUtils.decompressIfNeeded(fullData);
                ColdArchiveData archiveData = objectMapper.readValue(json, ColdArchiveData.class);
                builder.fullData(fullData);  // 保留原始数据
                log.debug("【HBase】归档数据已解析: rowKey={}, 评论数={}, 点赞数={}",
                        rowKey,
                        archiveData.getComments() != null ? archiveData.getComments().size() : 0,
                        archiveData.getLikes() != null ? archiveData.getLikes().size() : 0);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("【HBase】解析归档记录失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取归档记录的反序列化数据（评论+点赞）
     */
    public ColdArchiveData getArchiveData(Long userId, Long momentId) {
        Optional<HBaseArchiveRecord> recordOpt = getArchive(userId, momentId);
        if (recordOpt.isEmpty()) {
            return null;
        }

        HBaseArchiveRecord record = recordOpt.get();
        byte[] fullData = record.getFullData();
        if (fullData == null || fullData.length == 0) {
            return ColdArchiveData.builder()
                    .comments(java.util.Collections.emptyList())
                    .likes(java.util.Collections.emptyList())
                    .metadata(ColdArchiveData.ArchiveMetadata.builder()
                            .commentCount(0)
                            .likeCount(0)
                            .snapshotTime(record.getCreatedAt())
                            .build())
                    .build();
        }

        String json = GzipUtils.decompressIfNeeded(fullData);
        try {
            return objectMapper.readValue(json, ColdArchiveData.class);
        } catch (IOException e) {
            log.error("【HBase】反序列化归档数据失败: momentId={}, error={}", momentId, e.getMessage(), e);
            throw new RuntimeException("反序列化归档数据失败", e);
        }
    }

    // ==================== 删除归档记录 ====================

    /**
     * 删除归档记录
     */
    public void deleteArchive(Long userId, Long momentId) {
        String rowKey = generateRowKey(userId, momentId);
        deleteArchiveByRowKey(rowKey);
    }

    /**
     * 删除归档记录（通过 RowKey）
     */
    public void deleteArchiveByRowKey(String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName()))) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
            log.info("【HBase】归档记录已删除: rowKey={}", rowKey);
        } catch (IOException e) {
            log.error("【HBase】删除归档记录失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
            throw new RuntimeException("删除归档记录失败", e);
        }
    }

    // ==================== 工具方法 ====================

    private String getFullTableName() {
        return hBaseProperties.getNamespace() + ":" + TABLE_ARCHIVE;
    }
}
