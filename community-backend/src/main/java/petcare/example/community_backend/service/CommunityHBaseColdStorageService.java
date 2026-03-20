package petcare.example.community_backend.service;

import petcare.example.community_backend.config.HBaseProperties;
import petcare.example.community_backend.dto.MomentResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 社区模块 HBase 冷数据存储服务
 * 负责 Moments、Comments、Likes 冷数据的读写操作
 * 
 * 表结构：
 * - community_moments: content, media, engagement, user_info, metadata
 * - community_comments: content, user_info, engagement, thread, metadata
 * - community_likes: info, user_info, metadata
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityHBaseColdStorageService {

    private final Connection hbaseConnection;
    private final HBaseProperties hBaseProperties;

    // ==================== 表名常量 ====================
    private static final String TABLE_MOMENTS = "community_moments";
    private static final String TABLE_COMMENTS = "community_comments";
    private static final String TABLE_LIKES = "community_likes";

    // ==================== Moments 列族常量 ====================
    private static final byte[] CF_M_CONTENT = Bytes.toBytes("content");
    private static final byte[] CF_M_MEDIA = Bytes.toBytes("media");
    private static final byte[] CF_M_ENGAGEMENT = Bytes.toBytes("engagement");
    private static final byte[] CF_M_USER_INFO = Bytes.toBytes("user_info");
    private static final byte[] CF_M_METADATA = Bytes.toBytes("metadata");

    // ==================== Comments 列族常量 ====================
    private static final byte[] CF_C_CONTENT = Bytes.toBytes("content");
    private static final byte[] CF_C_USER_INFO = Bytes.toBytes("user_info");
    private static final byte[] CF_C_ENGAGEMENT = Bytes.toBytes("engagement");
    private static final byte[] CF_C_THREAD = Bytes.toBytes("thread");
    private static final byte[] CF_C_METADATA = Bytes.toBytes("metadata");

    // ==================== Likes 列族常量 ====================
    private static final byte[] CF_L_INFO = Bytes.toBytes("info");
    private static final byte[] CF_L_USER_INFO = Bytes.toBytes("user_info");
    private static final byte[] CF_L_METADATA = Bytes.toBytes("metadata");

    // ==================== 列名常量 ====================
    // 通用
    private static final byte[] COL_ID = Bytes.toBytes("id");
    private static final byte[] COL_USER_ID = Bytes.toBytes("user_id");
    private static final byte[] COL_CREATED_AT = Bytes.toBytes("created_at");
    
    // Moments content
    private static final byte[] COL_MOMENT_ID = Bytes.toBytes("moment_id");
    private static final byte[] COL_CONTENT = Bytes.toBytes("content");
    private static final byte[] COL_LOCATION = Bytes.toBytes("location");
    private static final byte[] COL_TAGS = Bytes.toBytes("tags");
    
    // Moments media
    private static final byte[] COL_MEDIA_IDS = Bytes.toBytes("media_ids");
    private static final byte[] COL_MEDIA_COUNT = Bytes.toBytes("media_count");
    private static final byte[] COL_COVER_URL = Bytes.toBytes("cover_url");
    
    // Moments/comments engagement
    private static final byte[] COL_LIKE_COUNT = Bytes.toBytes("like_count");
    private static final byte[] COL_COMMENT_COUNT = Bytes.toBytes("comment_count");
    private static final byte[] COL_SHARE_COUNT = Bytes.toBytes("share_count");
    
    // Comments content
    private static final byte[] COL_COMMENT_ID = Bytes.toBytes("comment_id");
    private static final byte[] COL_MOMENT_ID_REF = Bytes.toBytes("moment_id");
    private static final byte[] COL_PARENT_ID = Bytes.toBytes("parent_id");
    
    // Comments thread
    private static final byte[] COL_REPLY_COUNT = Bytes.toBytes("reply_count");
    private static final byte[] COL_ROOT_COMMENT_ID = Bytes.toBytes("root_comment_id");
    private static final byte[] COL_THREAD_DEPTH = Bytes.toBytes("thread_depth");
    private static final byte[] COL_THREAD_PATH = Bytes.toBytes("thread_path");
    
    // Likes info
    private static final byte[] COL_LIKE_ID = Bytes.toBytes("like_id");
    private static final byte[] COL_TARGET_TYPE = Bytes.toBytes("target_type");
    private static final byte[] COL_TARGET_ID = Bytes.toBytes("target_id");
    
    // user_info (通用)
    private static final byte[] COL_USER_NAME = Bytes.toBytes("user_name");
    private static final byte[] COL_USER_AVATAR = Bytes.toBytes("user_avatar");
    
    // metadata (通用)
    private static final byte[] COL_UPDATED_AT = Bytes.toBytes("updated_at");
    private static final byte[] COL_LAST_ACCESS_TIME = Bytes.toBytes("last_access_time");
    private static final byte[] COL_IS_PUBLIC = Bytes.toBytes("is_public");
    private static final byte[] COL_IS_DELETED = Bytes.toBytes("is_deleted");
    private static final byte[] COL_IS_VALID = Bytes.toBytes("is_valid");

    // ==================== RowKey 生成方法 ====================

    /**
     * 生成 Moments 表 RowKey
     * 格式: {user_id}_{reverse_timestamp}_{moment_id}
     */
    public String generateMomentRowKey(Long userId, LocalDateTime createdAt, Long momentId) {
        long reverseTimestamp = getReverseTimestamp(createdAt);
        return String.format("%015d_%015d_%015d", userId, reverseTimestamp, momentId);
    }

    /**
     * 生成 Comments 表 RowKey
     * 格式: C_{moment_id}_{reverse_timestamp}_{comment_id}
     */
    public String generateCommentRowKey(Long momentId, LocalDateTime createdAt, Long commentId) {
        long reverseTimestamp = getReverseTimestamp(createdAt);
        return String.format("C_%015d_%015d_%015d", momentId, reverseTimestamp, commentId);
    }

    /**
     * 生成 Likes 表 RowKey
     * 格式: L_{target_type}_{target_id}_{user_id}_{like_id}
     */
    public String generateLikeRowKey(String targetType, Long targetId, Long userId, Long likeId) {
        return String.format("L_%s_%015d_%015d_%015d", targetType, targetId, userId, likeId);
    }

    private long getReverseTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return Long.MAX_VALUE;
        }
        long timestamp = dateTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
        return Long.MAX_VALUE - timestamp;
    }

    // ==================== Moments 操作 ====================

    /**
     * 保存动态到冷库
     */
    public void saveMomentToColdStorage(MomentResponseDTO dto, String userName, String userAvatar) {
        String rowKey = generateMomentRowKey(dto.getUserId(),
                dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now(),
                dto.getId());

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_MOMENTS)))) {
            Put put = new Put(Bytes.toBytes(rowKey));

            // content 列族
            put.addColumn(CF_M_CONTENT, COL_MOMENT_ID, Bytes.toBytes(String.valueOf(dto.getId())));
            put.addColumn(CF_M_CONTENT, COL_USER_ID, Bytes.toBytes(String.valueOf(dto.getUserId())));
            put.addColumn(CF_M_CONTENT, COL_CONTENT, Bytes.toBytes(dto.getContent() != null ? dto.getContent() : ""));
            put.addColumn(CF_M_CONTENT, COL_LOCATION, Bytes.toBytes(dto.getLocation() != null ? dto.getLocation() : ""));
            put.addColumn(CF_M_CONTENT, COL_TAGS, Bytes.toBytes(dto.getTags() != null ? dto.getTags() : ""));

            // media 列族
            put.addColumn(CF_M_MEDIA, COL_MEDIA_COUNT, Bytes.toBytes(String.valueOf(dto.getMediaUrls() != null ? dto.getMediaUrls().size() : 0)));

            // engagement 列族
            put.addColumn(CF_M_ENGAGEMENT, COL_LIKE_COUNT, Bytes.toBytes(String.valueOf(dto.getLikeCount() != null ? dto.getLikeCount() : 0)));
            put.addColumn(CF_M_ENGAGEMENT, COL_COMMENT_COUNT, Bytes.toBytes(String.valueOf(dto.getCommentCount() != null ? dto.getCommentCount() : 0)));
            put.addColumn(CF_M_ENGAGEMENT, COL_SHARE_COUNT, Bytes.toBytes(String.valueOf(dto.getShareCount() != null ? dto.getShareCount() : 0)));

            // user_info 列族
            put.addColumn(CF_M_USER_INFO, COL_USER_NAME, Bytes.toBytes(userName != null ? userName : ""));
            put.addColumn(CF_M_USER_INFO, COL_USER_AVATAR, Bytes.toBytes(userAvatar != null ? userAvatar : ""));

            // metadata 列族
            LocalDateTime now = LocalDateTime.now();
            put.addColumn(CF_M_METADATA, COL_CREATED_AT, Bytes.toBytes(dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : now.toString()));
            put.addColumn(CF_M_METADATA, COL_UPDATED_AT, Bytes.toBytes(now.toString()));
            put.addColumn(CF_M_METADATA, COL_LAST_ACCESS_TIME, Bytes.toBytes(now.toString()));
            put.addColumn(CF_M_METADATA, COL_IS_PUBLIC, Bytes.toBytes("true"));
            put.addColumn(CF_M_METADATA, COL_IS_DELETED, Bytes.toBytes("false"));

            table.put(put);
            log.info("【HBase】动态写入冷库成功: momentId={}, rowKey={}", dto.getId(), rowKey);

        } catch (IOException e) {
            log.error("【HBase】动态写入冷库失败: momentId={}, error={}", dto.getId(), e.getMessage(), e);
            throw new RuntimeException("动态写入冷库失败", e);
        }
    }

    /**
     * 从冷库读取动态
     */
    public Optional<MomentResponseDTO> getMomentFromColdStorage(Long userId, LocalDateTime createdAt, Long momentId) {
        String rowKey = generateMomentRowKey(userId, createdAt, momentId);
        return getMomentFromColdStorageByRowKey(rowKey);
    }

    /**
     * 从冷库读取动态（通过 RowKey）
     */
    public Optional<MomentResponseDTO> getMomentFromColdStorageByRowKey(String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_MOMENTS)))) {
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addFamily(CF_M_CONTENT);
            get.addFamily(CF_M_MEDIA);
            get.addFamily(CF_M_ENGAGEMENT);
            get.addFamily(CF_M_USER_INFO);
            get.addFamily(CF_M_METADATA);

            Result result = table.get(get);
            if (result == null || result.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(parseMomentResult(result, rowKey));
        } catch (IOException e) {
            log.error("【HBase】读取动态失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
            throw new RuntimeException("读取动态失败", e);
        }
    }

    /**
     * 按用户ID查询历史动态
     */
    public List<MomentResponseDTO> queryMomentsByUserId(Long userId) {
        List<MomentResponseDTO> results = new ArrayList<>();

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_MOMENTS)))) {
            String startRow = String.format("%015d_", userId);
            String endRow = String.format("%015d`", userId);

            Scan scan = new Scan();
            scan.withStartRow(Bytes.toBytes(startRow), true);
            scan.withStopRow(Bytes.toBytes(endRow), true);
            scan.addFamily(CF_M_CONTENT);
            scan.addFamily(CF_M_MEDIA);
            scan.addFamily(CF_M_ENGAGEMENT);
            scan.addFamily(CF_M_USER_INFO);
            scan.addFamily(CF_M_METADATA);

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    String rowKey = Bytes.toString(result.getRow());
                    MomentResponseDTO dto = parseMomentResult(result, rowKey);
                    if (dto != null) {
                        results.add(dto);
                    }
                }
            }
        } catch (IOException e) {
            log.error("【HBase】查询用户动态失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("查询用户动态失败", e);
        }

        return results;
    }

    /**
     * 删除冷库中的动态
     */
    public void deleteMomentFromColdStorage(Long userId, LocalDateTime createdAt, Long momentId) {
        String rowKey = generateMomentRowKey(userId, createdAt, momentId);
        deleteFromColdStorage(getFullTableName(TABLE_MOMENTS), rowKey);
    }

    /**
     * 更新冷库中动态的最后访问时间
     */
    public void updateMomentLastAccessTime(Long userId, LocalDateTime createdAt, Long momentId) {
        String rowKey = generateMomentRowKey(userId, createdAt, momentId);

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_MOMENTS)))) {
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(CF_M_METADATA, COL_LAST_ACCESS_TIME, Bytes.toBytes(LocalDateTime.now().toString()));
            table.put(put);
            log.debug("【HBase】更新动态访问时间: rowKey={}", rowKey);
        } catch (IOException e) {
            log.error("【HBase】更新动态访问时间失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
        }
    }

    // ==================== Comments 操作 ====================

    /**
     * 批量保存评论到冷库
     */
    public void batchSaveCommentsToColdStorage(List<CommentColdData> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_COMMENTS)))) {
            List<Put> puts = new ArrayList<>();

            for (CommentColdData comment : comments) {
                String rowKey = generateCommentRowKey(comment.getMomentId(), comment.getCreatedAt(), comment.getCommentId());
                Put put = new Put(Bytes.toBytes(rowKey));

                // content 列族
                put.addColumn(CF_C_CONTENT, COL_COMMENT_ID, Bytes.toBytes(String.valueOf(comment.getCommentId())));
                put.addColumn(CF_C_CONTENT, COL_MOMENT_ID_REF, Bytes.toBytes(String.valueOf(comment.getMomentId())));
                put.addColumn(CF_C_CONTENT, COL_USER_ID, Bytes.toBytes(String.valueOf(comment.getUserId())));
                put.addColumn(CF_C_CONTENT, COL_CONTENT, Bytes.toBytes(comment.getContent() != null ? comment.getContent() : ""));
                put.addColumn(CF_C_CONTENT, COL_PARENT_ID, Bytes.toBytes(comment.getParentId() != null ? String.valueOf(comment.getParentId()) : ""));

                // user_info 列族
                put.addColumn(CF_C_USER_INFO, COL_USER_NAME, Bytes.toBytes(comment.getUserName() != null ? comment.getUserName() : ""));
                put.addColumn(CF_C_USER_INFO, COL_USER_AVATAR, Bytes.toBytes(comment.getUserAvatar() != null ? comment.getUserAvatar() : ""));

                // metadata 列族
                put.addColumn(CF_C_METADATA, COL_CREATED_AT, Bytes.toBytes(comment.getCreatedAt().toString()));
                put.addColumn(CF_C_METADATA, COL_IS_DELETED, Bytes.toBytes("false"));

                puts.add(put);
            }

            table.put(puts);
            log.info("【HBase】批量写入评论成功: count={}", comments.size());

        } catch (IOException e) {
            log.error("【HBase】批量写入评论失败: error={}", e.getMessage(), e);
            throw new RuntimeException("批量写入评论失败", e);
        }
    }

    /**
     * 按动态ID查询所有评论
     */
    public List<CommentColdData> queryCommentsByMomentId(Long momentId) {
        List<CommentColdData> results = new ArrayList<>();

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_COMMENTS)))) {
            String startRow = String.format("C_%015d_", momentId);
            String endRow = String.format("C_%015d`", momentId);

            Scan scan = new Scan();
            scan.withStartRow(Bytes.toBytes(startRow), true);
            scan.withStopRow(Bytes.toBytes(endRow), true);
            scan.addFamily(CF_C_CONTENT);
            scan.addFamily(CF_C_USER_INFO);
            scan.addFamily(CF_C_METADATA);

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    CommentColdData comment = parseCommentResult(result);
                    if (comment != null) {
                        results.add(comment);
                    }
                }
            }
        } catch (IOException e) {
            log.error("【HBase】查询评论失败: momentId={}, error={}", momentId, e.getMessage(), e);
            throw new RuntimeException("查询评论失败", e);
        }

        return results;
    }

    /**
     * 删除冷库中的评论（按动态ID批量）
     */
    public void deleteCommentsByMomentIdFromColdStorage(Long momentId) {
        List<CommentColdData> comments = queryCommentsByMomentId(momentId);

        if (comments.isEmpty()) {
            return;
        }

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_COMMENTS)))) {
            List<Delete> deletes = new ArrayList<>();

            for (CommentColdData comment : comments) {
                String rowKey = generateCommentRowKey(comment.getMomentId(), comment.getCreatedAt(), comment.getCommentId());
                deletes.add(new Delete(Bytes.toBytes(rowKey)));
            }

            table.delete(deletes);
            log.info("【HBase】批量删除评论成功: momentId={}, count={}", momentId, comments.size());

        } catch (IOException e) {
            log.error("【HBase】批量删除评论失败: momentId={}, error={}", momentId, e.getMessage(), e);
            throw new RuntimeException("批量删除评论失败", e);
        }
    }

    // ==================== Likes 操作 ====================

    /**
     * 批量保存点赞到冷库
     */
    public void batchSaveLikesToColdStorage(List<LikeColdData> likes) {
        if (likes == null || likes.isEmpty()) {
            return;
        }

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_LIKES)))) {
            List<Put> puts = new ArrayList<>();

            for (LikeColdData like : likes) {
                String rowKey = generateLikeRowKey(like.getTargetType().name(),
                        like.getTargetId(), like.getUserId(), like.getLikeId());
                Put put = new Put(Bytes.toBytes(rowKey));

                // info 列族
                put.addColumn(CF_L_INFO, COL_LIKE_ID, Bytes.toBytes(String.valueOf(like.getLikeId())));
                put.addColumn(CF_L_INFO, COL_USER_ID, Bytes.toBytes(String.valueOf(like.getUserId())));
                put.addColumn(CF_L_INFO, COL_TARGET_TYPE, Bytes.toBytes(like.getTargetType().name()));
                put.addColumn(CF_L_INFO, COL_TARGET_ID, Bytes.toBytes(String.valueOf(like.getTargetId())));
                put.addColumn(CF_L_INFO, COL_CREATED_AT, Bytes.toBytes(like.getCreatedAt().toString()));

                // user_info 列族
                put.addColumn(CF_L_USER_INFO, COL_USER_NAME, Bytes.toBytes(like.getUserName() != null ? like.getUserName() : ""));
                put.addColumn(CF_L_USER_INFO, COL_USER_AVATAR, Bytes.toBytes(like.getUserAvatar() != null ? like.getUserAvatar() : ""));

                // metadata 列族
                put.addColumn(CF_L_METADATA, COL_IS_VALID, Bytes.toBytes("true"));

                puts.add(put);
            }

            table.put(puts);
            log.info("【HBase】批量写入点赞成功: count={}", likes.size());

        } catch (IOException e) {
            log.error("【HBase】批量写入点赞失败: error={}", e.getMessage(), e);
            throw new RuntimeException("批量写入点赞失败", e);
        }
    }

    /**
     * 按目标ID查询所有点赞
     */
    public List<LikeColdData> queryLikesByTarget(String targetType, Long targetId) {
        List<LikeColdData> results = new ArrayList<>();

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_LIKES)))) {
            String prefix = String.format("L_%s_%015d_", targetType, targetId);
            byte[] startBytes = Bytes.toBytes(prefix);
            byte[] endBytes = Bytes.add(startBytes, new byte[]{0});

            Scan scan = new Scan();
            scan.withStartRow(startBytes, true);
            scan.withStopRow(endBytes, true);
            scan.addFamily(CF_L_INFO);
            scan.addFamily(CF_L_USER_INFO);
            scan.addFamily(CF_L_METADATA);

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    LikeColdData like = parseLikeResult(result);
                    if (like != null) {
                        results.add(like);
                    }
                }
            }
        } catch (IOException e) {
            log.error("【HBase】查询点赞失败: targetType={}, targetId={}, error={}", targetType, targetId, e.getMessage(), e);
            throw new RuntimeException("查询点赞失败", e);
        }

        return results;
    }

    /**
     * 删除冷库中的点赞（按目标ID批量）
     */
    public void deleteLikesByTargetFromColdStorage(String targetType, Long targetId) {
        List<LikeColdData> likes = queryLikesByTarget(targetType, targetId);

        if (likes.isEmpty()) {
            return;
        }

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_LIKES)))) {
            List<Delete> deletes = new ArrayList<>();

            for (LikeColdData like : likes) {
                String rowKey = generateLikeRowKey(like.getTargetType().name(),
                        like.getTargetId(), like.getUserId(), like.getLikeId());
                deletes.add(new Delete(Bytes.toBytes(rowKey)));
            }

            table.delete(deletes);
            log.info("【HBase】批量删除点赞成功: targetType={}, targetId={}, count={}", targetType, targetId, likes.size());

        } catch (IOException e) {
            log.error("【HBase】批量删除点赞失败: targetType={}, targetId={}, error={}", targetType, targetId, e.getMessage(), e);
            throw new RuntimeException("批量删除点赞失败", e);
        }
    }

    // ==================== 通用方法 ====================

    /**
     * 检查数据是否存在
     */
    public boolean existsInColdStorage(String tableName, String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(tableName)))) {
            Get get = new Get(Bytes.toBytes(rowKey));
            return table.exists(get);
        } catch (IOException e) {
            log.error("【HBase】检查数据存在性失败: table={}, rowKey={}, error={}", tableName, rowKey, e.getMessage(), e);
            return false;
        }
    }

    private void deleteFromColdStorage(String tableName, String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(tableName))) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
            log.info("【HBase】从冷库删除记录: table={}, rowKey={}", tableName, rowKey);
        } catch (IOException e) {
            log.error("【HBase】从冷库删除记录失败: table={}, rowKey={}, error={}", tableName, rowKey, e.getMessage(), e);
            throw new RuntimeException("从冷库删除记录失败", e);
        }
    }

    private String getFullTableName(String tableName) {
        return hBaseProperties.getNamespace() + ":" + tableName;
    }

    // ==================== 解析方法 ====================

    private MomentResponseDTO parseMomentResult(Result result, String rowKey) {
        try {
            MomentResponseDTO dto = new MomentResponseDTO();

            byte[] momentId = result.getValue(CF_M_CONTENT, COL_MOMENT_ID);
            if (momentId != null) dto.setId(Long.parseLong(Bytes.toString(momentId)));

            byte[] userId = result.getValue(CF_M_CONTENT, COL_USER_ID);
            if (userId != null) dto.setUserId(Long.parseLong(Bytes.toString(userId)));

            byte[] content = result.getValue(CF_M_CONTENT, COL_CONTENT);
            if (content != null) dto.setContent(Bytes.toString(content));

            byte[] location = result.getValue(CF_M_CONTENT, COL_LOCATION);
            if (location != null) dto.setLocation(Bytes.toString(location));

            byte[] tags = result.getValue(CF_M_CONTENT, COL_TAGS);
            if (tags != null) dto.setTags(Bytes.toString(tags));

            byte[] userName = result.getValue(CF_M_USER_INFO, COL_USER_NAME);
            if (userName != null) dto.setAuthorName(Bytes.toString(userName));

            byte[] userAvatar = result.getValue(CF_M_USER_INFO, COL_USER_AVATAR);
            if (userAvatar != null) dto.setAuthorAvatar(Bytes.toString(userAvatar));

            byte[] likeCount = result.getValue(CF_M_ENGAGEMENT, COL_LIKE_COUNT);
            if (likeCount != null) dto.setLikeCount(Integer.parseInt(Bytes.toString(likeCount)));

            byte[] commentCount = result.getValue(CF_M_ENGAGEMENT, COL_COMMENT_COUNT);
            if (commentCount != null) dto.setCommentCount(Integer.parseInt(Bytes.toString(commentCount)));

            byte[] shareCount = result.getValue(CF_M_ENGAGEMENT, COL_SHARE_COUNT);
            if (shareCount != null) dto.setShareCount(Integer.parseInt(Bytes.toString(shareCount)));

            byte[] createdAt = result.getValue(CF_M_METADATA, COL_CREATED_AT);
            if (createdAt != null) dto.setCreatedAt(LocalDateTime.parse(Bytes.toString(createdAt)));

            return dto;
        } catch (Exception e) {
            log.error("【HBase】解析动态数据失败: rowKey={}, error={}", rowKey, e.getMessage(), e);
            return null;
        }
    }

    private CommentColdData parseCommentResult(Result result) {
        try {
            CommentColdData comment = new CommentColdData();

            byte[] commentId = result.getValue(CF_C_CONTENT, COL_COMMENT_ID);
            if (commentId != null) comment.setCommentId(Long.parseLong(Bytes.toString(commentId)));

            byte[] momentId = result.getValue(CF_C_CONTENT, COL_MOMENT_ID_REF);
            if (momentId != null) comment.setMomentId(Long.parseLong(Bytes.toString(momentId)));

            byte[] userId = result.getValue(CF_C_CONTENT, COL_USER_ID);
            if (userId != null) comment.setUserId(Long.parseLong(Bytes.toString(userId)));

            byte[] content = result.getValue(CF_C_CONTENT, COL_CONTENT);
            if (content != null) comment.setContent(Bytes.toString(content));

            byte[] parentId = result.getValue(CF_C_CONTENT, COL_PARENT_ID);
            if (parentId != null && !Bytes.toString(parentId).isEmpty()) {
                comment.setParentId(Long.parseLong(Bytes.toString(parentId)));
            }

            byte[] userName = result.getValue(CF_C_USER_INFO, COL_USER_NAME);
            if (userName != null) comment.setUserName(Bytes.toString(userName));

            byte[] userAvatar = result.getValue(CF_C_USER_INFO, COL_USER_AVATAR);
            if (userAvatar != null) comment.setUserAvatar(Bytes.toString(userAvatar));

            byte[] createdAt = result.getValue(CF_C_METADATA, COL_CREATED_AT);
            if (createdAt != null) comment.setCreatedAt(LocalDateTime.parse(Bytes.toString(createdAt)));

            return comment;
        } catch (Exception e) {
            log.error("【HBase】解析评论数据失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    private LikeColdData parseLikeResult(Result result) {
        try {
            LikeColdData like = new LikeColdData();

            byte[] likeId = result.getValue(CF_L_INFO, COL_LIKE_ID);
            if (likeId != null) like.setLikeId(Long.parseLong(Bytes.toString(likeId)));

            byte[] userId = result.getValue(CF_L_INFO, COL_USER_ID);
            if (userId != null) like.setUserId(Long.parseLong(Bytes.toString(userId)));

            byte[] targetType = result.getValue(CF_L_INFO, COL_TARGET_TYPE);
            if (targetType != null) like.setTargetType(LikeColdData.LikeTargetType.valueOf(Bytes.toString(targetType)));

            byte[] targetId = result.getValue(CF_L_INFO, COL_TARGET_ID);
            if (targetId != null) like.setTargetId(Long.parseLong(Bytes.toString(targetId)));

            byte[] createdAt = result.getValue(CF_L_INFO, COL_CREATED_AT);
            if (createdAt != null) like.setCreatedAt(LocalDateTime.parse(Bytes.toString(createdAt)));

            byte[] userName = result.getValue(CF_L_USER_INFO, COL_USER_NAME);
            if (userName != null) like.setUserName(Bytes.toString(userName));

            byte[] userAvatar = result.getValue(CF_L_USER_INFO, COL_USER_AVATAR);
            if (userAvatar != null) like.setUserAvatar(Bytes.toString(userAvatar));

            return like;
        } catch (Exception e) {
            log.error("【HBase】解析点赞数据失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 内部类 ====================

    @lombok.Data
    public static class CommentColdData {
        private Long commentId;
        private Long momentId;
        private Long userId;
        private String content;
        private Long parentId;
        private String userName;
        private String userAvatar;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    public static class LikeColdData {
        private Long likeId;
        private Long userId;
        private LikeTargetType targetType;
        private Long targetId;
        private String userName;
        private String userAvatar;
        private LocalDateTime createdAt;

        public enum LikeTargetType {
            MOMENT, COMMENT
        }
    }
}
