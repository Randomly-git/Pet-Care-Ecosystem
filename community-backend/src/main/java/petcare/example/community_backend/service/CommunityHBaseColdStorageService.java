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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 社区模块 HBase 冷数据存储服务
 * 负责 Moments、Comments、Likes 冷数据的读写操作
 *
 * 新表结构（单列族 d）：
 * - community_moments: RowKey = {userId前4位}_{momentId}
 * - community_comments: RowKey = {userId前4位}_{commentId}
 * - community_likes: RowKey = {userId前4位}_{likeId}
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

    // ==================== 统一的列族常量 ====================
    private static final byte[] CF_D = Bytes.toBytes("d");

    // ==================== 列名常量（统一使用 d 列族） ====================
    // 通用
    private static final byte[] COL_ID = Bytes.toBytes("id");
    private static final byte[] COL_USER_ID = Bytes.toBytes("user_id");
    private static final byte[] COL_CREATED_AT = Bytes.toBytes("created_at");

    // Moments
    private static final byte[] COL_MOMENT_ID = Bytes.toBytes("moment_id");
    private static final byte[] COL_CONTENT = Bytes.toBytes("content");

    // Comments
    private static final byte[] COL_COMMENT_ID = Bytes.toBytes("comment_id");
    private static final byte[] COL_MOMENT_ID_REF = Bytes.toBytes("moment_id");
    private static final byte[] COL_PARENT_ID = Bytes.toBytes("parent_id");

    // Likes
    private static final byte[] COL_LIKE_ID = Bytes.toBytes("like_id");
    private static final byte[] COL_TARGET_TYPE = Bytes.toBytes("target_type");
    private static final byte[] COL_TARGET_ID = Bytes.toBytes("target_id");

    // user_info (通用)
    private static final byte[] COL_USER_NAME = Bytes.toBytes("user_name");
    private static final byte[] COL_USER_AVATAR = Bytes.toBytes("user_avatar");

    // ==================== RowKey 生成方法 ====================

    /**
     * 生成 Moments 表 RowKey
     * 格式: {user_id前4位(盐值)}_{moment_id}
     *
     * 示例: 0001_12345
     */
    public String generateMomentRowKey(Long userId, Long momentId) {
        String userPrefix = String.format("%04d", userId % 10000);
        return userPrefix + "_" + momentId;
    }

    /**
     * 生成 Comments 表 RowKey
     * 格式: {user_id前4位(盐值)}_{comment_id}
     */
    public String generateCommentRowKey(Long userId, Long commentId) {
        String userPrefix = String.format("%04d", userId % 10000);
        return userPrefix + "_" + commentId;
    }

    /**
     * 生成 Likes 表 RowKey
     * 格式: {user_id前4位(盐值)}_{like_id}
     */
    public String generateLikeRowKey(Long userId, Long likeId) {
        String userPrefix = String.format("%04d", userId % 10000);
        return userPrefix + "_" + likeId;
    }

    /**
     * 扫描 HBase 查找指定 momentId 对应的 RowKey（兜底方案，性能较差）
     *
     * 仅在无法获取 userId 时使用
     * 由于 RowKey 设计改变，此方法暂时返回空
     * 建议前端在调用 API 时传入 userId 参数
     *
     * @param momentId 动态ID
     * @return RowKey，如果未找到则返回空
     */
    public Optional<String> scanForMomentRowKey(Long momentId) {
        log.warn("【HBase扫描】scanForMomentRowKey 暂未实现，请传入 userId 参数以直接定位: momentId={}", momentId);
        // 由于新 RowKey 格式无法通过 momentId 直接扫描，必须依赖 userId
        return Optional.empty();
    }

    // ==================== Moments 操作 ====================

    /**
     * 保存动态到冷库
     */
    public void saveMomentToColdStorage(MomentResponseDTO dto, String userName, String userAvatar) {
        String rowKey = generateMomentRowKey(dto.getUserId(), dto.getId());

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_MOMENTS)))) {
            Put put = new Put(Bytes.toBytes(rowKey));

            // 所有字段写入 d 列族
            put.addColumn(CF_D, COL_MOMENT_ID, Bytes.toBytes(String.valueOf(dto.getId())));
            put.addColumn(CF_D, COL_USER_ID, Bytes.toBytes(String.valueOf(dto.getUserId())));
            put.addColumn(CF_D, COL_CONTENT, Bytes.toBytes(dto.getContent() != null ? dto.getContent() : ""));
            put.addColumn(CF_D, COL_USER_NAME, Bytes.toBytes(userName != null ? userName : ""));
            put.addColumn(CF_D, COL_USER_AVATAR, Bytes.toBytes(userAvatar != null ? userAvatar : ""));
            put.addColumn(CF_D, COL_CREATED_AT, Bytes.toBytes(
                    dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : LocalDateTime.now().toString()));

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
    public Optional<MomentResponseDTO> getMomentFromColdStorage(Long userId, Long momentId) {
        String rowKey = generateMomentRowKey(userId, momentId);
        return getMomentFromColdStorageByRowKey(rowKey);
    }

    /**
     * 从冷库读取动态（通过 RowKey）
     */
    public Optional<MomentResponseDTO> getMomentFromColdStorageByRowKey(String rowKey) {
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_MOMENTS)))) {
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addFamily(CF_D);

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
     * 删除冷库中的动态
     */
    public void deleteMomentFromColdStorage(Long userId, Long momentId) {
        String rowKey = generateMomentRowKey(userId, momentId);
        deleteFromColdStorage(getFullTableName(TABLE_MOMENTS), rowKey);
    }

    /**
     * 按用户ID查询该用户的所有动态
     * 用于 getMomentsByUserId 时查询 HBase 中的冷数据
     */
    public List<MomentResponseDTO> queryMomentsByUserId(Long userId) {
        List<MomentResponseDTO> results = new ArrayList<>();
        String userPrefix = String.format("%04d", userId % 10000);

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_MOMENTS)))) {
            String startRow = userPrefix + "_";
            String endRow = userPrefix + "_~"; // ~ 在 ASCII 中比数字大

            Scan scan = new Scan();
            scan.withStartRow(Bytes.toBytes(startRow));
            scan.withStopRow(Bytes.toBytes(endRow));
            scan.addFamily(CF_D);

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
                String rowKey = generateCommentRowKey(comment.getUserId(), comment.getCommentId());
                Put put = new Put(Bytes.toBytes(rowKey));

                put.addColumn(CF_D, COL_COMMENT_ID, Bytes.toBytes(String.valueOf(comment.getCommentId())));
                put.addColumn(CF_D, COL_MOMENT_ID_REF, Bytes.toBytes(String.valueOf(comment.getMomentId())));
                put.addColumn(CF_D, COL_USER_ID, Bytes.toBytes(String.valueOf(comment.getUserId())));
                put.addColumn(CF_D, COL_CONTENT, Bytes.toBytes(comment.getContent() != null ? comment.getContent() : ""));
                put.addColumn(CF_D, COL_PARENT_ID, Bytes.toBytes(
                        comment.getParentId() != null ? String.valueOf(comment.getParentId()) : ""));
                put.addColumn(CF_D, COL_USER_NAME, Bytes.toBytes(comment.getUserName() != null ? comment.getUserName() : ""));
                put.addColumn(CF_D, COL_USER_AVATAR, Bytes.toBytes(comment.getUserAvatar() != null ? comment.getUserAvatar() : ""));
                put.addColumn(CF_D, COL_CREATED_AT, Bytes.toBytes(comment.getCreatedAt().toString()));

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
     * 按评论ID列表查询评论
     * 用于恢复时从 HBase 获取指定评论
     */
    public List<CommentColdData> getCommentsByIds(List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<CommentColdData> results = new ArrayList<>();

        // 由于评论的 RowKey 是 {userId前4位}_{commentId}，我们需要知道 userId
        // 但在恢复场景中，评论的 userId 已经在 CommentColdData 中了
        // 这里按 commentId 扫描所有评论（性能较差，仅用于恢复）

        // TODO: 优化方案 - 在恢复流程中，先获取 userId，再按 userId 前缀扫描
        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_COMMENTS)))) {
            Scan scan = new Scan();
            scan.addFamily(CF_D);
            scan.setFilter(new org.apache.hadoop.hbase.filter.ColumnPaginationFilter(1, 0));

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    byte[] commentIdVal = result.getValue(CF_D, COL_COMMENT_ID);
                    if (commentIdVal != null) {
                        Long commentId = Long.parseLong(Bytes.toString(commentIdVal));
                        if (commentIds.contains(commentId)) {
                            CommentColdData comment = parseCommentResult(result);
                            if (comment != null) {
                                results.add(comment);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("【HBase】查询评论失败: error={}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * 按用户ID查询该用户的所有评论
     * 用于恢复时获取指定用户的所有评论
     */
    public List<CommentColdData> queryCommentsByUserId(Long userId) {
        List<CommentColdData> results = new ArrayList<>();
        String userPrefix = String.format("%04d", userId % 10000);

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_COMMENTS)))) {
            String startRow = userPrefix + "_";
            String endRow = userPrefix + "_~"; // ~ 在 ASCII 中比数字大

            Scan scan = new Scan();
            scan.withStartRow(Bytes.toBytes(startRow));
            scan.withStopRow(Bytes.toBytes(endRow));
            scan.addFamily(CF_D);

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    CommentColdData comment = parseCommentResult(result);
                    if (comment != null) {
                        results.add(comment);
                    }
                }
            }
        } catch (IOException e) {
            log.error("【HBase】查询用户评论失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("查询用户评论失败", e);
        }

        return results;
    }

    /**
     * 删除冷库中的评论（按评论ID列表批量删除）
     */
    public void deleteCommentsFromColdStorage(List<CommentColdData> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_COMMENTS)))) {
            List<Delete> deletes = new ArrayList<>();

            for (CommentColdData comment : comments) {
                String rowKey = generateCommentRowKey(comment.getUserId(), comment.getCommentId());
                deletes.add(new Delete(Bytes.toBytes(rowKey)));
            }

            table.delete(deletes);
            log.info("【HBase】批量删除评论成功: count={}", comments.size());

        } catch (IOException e) {
            log.error("【HBase】批量删除评论失败: error={}", e.getMessage(), e);
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
                String rowKey = generateLikeRowKey(like.getUserId(), like.getLikeId());
                Put put = new Put(Bytes.toBytes(rowKey));

                put.addColumn(CF_D, COL_LIKE_ID, Bytes.toBytes(String.valueOf(like.getLikeId())));
                put.addColumn(CF_D, COL_USER_ID, Bytes.toBytes(String.valueOf(like.getUserId())));
                put.addColumn(CF_D, COL_TARGET_TYPE, Bytes.toBytes(like.getTargetType().name()));
                put.addColumn(CF_D, COL_TARGET_ID, Bytes.toBytes(String.valueOf(like.getTargetId())));
                put.addColumn(CF_D, COL_CREATED_AT, Bytes.toBytes(like.getCreatedAt().toString()));
                put.addColumn(CF_D, COL_USER_NAME, Bytes.toBytes(like.getUserName() != null ? like.getUserName() : ""));
                put.addColumn(CF_D, COL_USER_AVATAR, Bytes.toBytes(like.getUserAvatar() != null ? like.getUserAvatar() : ""));

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
     * 按用户ID查询该用户的所有点赞
     * 用于恢复时获取指定用户的所有点赞
     */
    public List<LikeColdData> queryLikesByUserId(Long userId) {
        List<LikeColdData> results = new ArrayList<>();
        String userPrefix = String.format("%04d", userId % 10000);

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_LIKES)))) {
            String startRow = userPrefix + "_";
            String endRow = userPrefix + "_~";

            Scan scan = new Scan();
            scan.withStartRow(Bytes.toBytes(startRow));
            scan.withStopRow(Bytes.toBytes(endRow));
            scan.addFamily(CF_D);

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    LikeColdData like = parseLikeResult(result);
                    if (like != null) {
                        results.add(like);
                    }
                }
            }
        } catch (IOException e) {
            log.error("【HBase】查询用户点赞失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("查询用户点赞失败", e);
        }

        return results;
    }

    /**
     * 删除冷库中的点赞（按点赞列表批量删除）
     */
    public void deleteLikesFromColdStorage(List<LikeColdData> likes) {
        if (likes == null || likes.isEmpty()) {
            return;
        }

        try (Table table = hbaseConnection.getTable(TableName.valueOf(getFullTableName(TABLE_LIKES)))) {
            List<Delete> deletes = new ArrayList<>();

            for (LikeColdData like : likes) {
                String rowKey = generateLikeRowKey(like.getUserId(), like.getLikeId());
                deletes.add(new Delete(Bytes.toBytes(rowKey)));
            }

            table.delete(deletes);
            log.info("【HBase】批量删除点赞成功: count={}", likes.size());

        } catch (IOException e) {
            log.error("【HBase】批量删除点赞失败: error={}", e.getMessage(), e);
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

            byte[] momentId = result.getValue(CF_D, COL_MOMENT_ID);
            if (momentId != null) dto.setId(Long.parseLong(Bytes.toString(momentId)));

            byte[] userId = result.getValue(CF_D, COL_USER_ID);
            if (userId != null) dto.setUserId(Long.parseLong(Bytes.toString(userId)));

            byte[] content = result.getValue(CF_D, COL_CONTENT);
            if (content != null) dto.setContent(Bytes.toString(content));

            byte[] userName = result.getValue(CF_D, COL_USER_NAME);
            if (userName != null) dto.setAuthorName(Bytes.toString(userName));

            byte[] userAvatar = result.getValue(CF_D, COL_USER_AVATAR);
            if (userAvatar != null) dto.setAuthorAvatar(Bytes.toString(userAvatar));

            byte[] createdAt = result.getValue(CF_D, COL_CREATED_AT);
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

            byte[] commentId = result.getValue(CF_D, COL_COMMENT_ID);
            if (commentId != null) comment.setCommentId(Long.parseLong(Bytes.toString(commentId)));

            byte[] momentId = result.getValue(CF_D, COL_MOMENT_ID_REF);
            if (momentId != null) comment.setMomentId(Long.parseLong(Bytes.toString(momentId)));

            byte[] userId = result.getValue(CF_D, COL_USER_ID);
            if (userId != null) comment.setUserId(Long.parseLong(Bytes.toString(userId)));

            byte[] content = result.getValue(CF_D, COL_CONTENT);
            if (content != null) comment.setContent(Bytes.toString(content));

            byte[] parentId = result.getValue(CF_D, COL_PARENT_ID);
            if (parentId != null && !Bytes.toString(parentId).isEmpty()) {
                comment.setParentId(Long.parseLong(Bytes.toString(parentId)));
            }

            byte[] userName = result.getValue(CF_D, COL_USER_NAME);
            if (userName != null) comment.setUserName(Bytes.toString(userName));

            byte[] userAvatar = result.getValue(CF_D, COL_USER_AVATAR);
            if (userAvatar != null) comment.setUserAvatar(Bytes.toString(userAvatar));

            byte[] createdAt = result.getValue(CF_D, COL_CREATED_AT);
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

            byte[] likeId = result.getValue(CF_D, COL_LIKE_ID);
            if (likeId != null) like.setLikeId(Long.parseLong(Bytes.toString(likeId)));

            byte[] userId = result.getValue(CF_D, COL_USER_ID);
            if (userId != null) like.setUserId(Long.parseLong(Bytes.toString(userId)));

            byte[] targetType = result.getValue(CF_D, COL_TARGET_TYPE);
            if (targetType != null) like.setTargetType(LikeColdData.LikeTargetType.valueOf(Bytes.toString(targetType)));

            byte[] targetId = result.getValue(CF_D, COL_TARGET_ID);
            if (targetId != null) like.setTargetId(Long.parseLong(Bytes.toString(targetId)));

            byte[] createdAt = result.getValue(CF_D, COL_CREATED_AT);
            if (createdAt != null) like.setCreatedAt(LocalDateTime.parse(Bytes.toString(createdAt)));

            byte[] userName = result.getValue(CF_D, COL_USER_NAME);
            if (userName != null) like.setUserName(Bytes.toString(userName));

            byte[] userAvatar = result.getValue(CF_D, COL_USER_AVATAR);
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
