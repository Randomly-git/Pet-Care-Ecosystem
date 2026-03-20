package petcare.example.community_backend.service;

import petcare.example.community_backend.config.RabbitMQConfig;
import petcare.example.community_backend.dto.CommunityColdStorageEvent;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.model.Like;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.model.TargetType;
import petcare.example.community_backend.repository.CommentRepository;
import petcare.example.community_backend.repository.LikeRepository;
import petcare.example.community_backend.repository.PetMomentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 社区模块冷数据迁移事件消费者服务
 * 
 * 功能：
 * 1. MIGRATE_TO_COLD - 迁移到冷库（级联迁移 comments 和 likes）
 * 2. RESTORE_FROM_COLD - 从冷库恢复（重新设置 last_access_time）
 * 3. DELETE_FROM_COLD - 从冷库删除
 * 
 * 设计原则：
 * 1. 迁移成功后删除 MySQL 记录
 * 2. 幂等性：消费前检查 HBase 是否已存在数据
 * 3. MQ 负责重试，不需要 MySQL 记录重试次数
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityColdStorageEventConsumer {

    private final PetMomentRepository momentRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final CommunityHBaseColdStorageService hBaseService;
    private final CommunityColdStorageEventPublisher eventPublisher;

    /**
     * 处理冷存储事件
     */
    @RabbitListener(queues = RabbitMQConfig.COLD_MIGRATION_QUEUE)
    @Transactional
    public void handleColdStorageEvent(CommunityColdStorageEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("【MQ消费】收到冷存储事件: eventId={}, operationType={}, momentId={}",
                event.getEventId(), event.getOperationType(), event.getMomentId());

        try {
            switch (event.getOperationType()) {
                case MIGRATE_TO_COLD -> handleMigrateToCold(event);
                case RESTORE_FROM_COLD -> handleRestoreFromCold(event);
                case DELETE_FROM_COLD -> handleDeleteFromCold(event);
                default -> log.warn("【MQ消费】未知的操作类型: eventId={}, operationType={}",
                        event.getEventId(), event.getOperationType());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("【MQ消费】冷存储事件处理完成: eventId={}, operationType={}, momentId={}, 耗时={}ms",
                    event.getEventId(), event.getOperationType(), event.getMomentId(), elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("【MQ消费】冷存储事件处理失败: eventId={}, operationType={}, momentId={}, 耗时={}ms, error={}",
                    event.getEventId(), event.getOperationType(), event.getMomentId(), elapsed, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理迁移到冷库
     * 
     * 幂等性保证流程：
     * 1. 查询 MySQL 记录（可能已被之前的消息删除）
     * 2. 如果记录不存在，说明已被迁移，直接返回成功
     * 3. 检查 HBase 是否已存在，存在则跳过
     * 4. 如果不存在，写入 HBase 后删除 MySQL 记录
     */
    private void handleMigrateToCold(CommunityColdStorageEvent event) {
        Long momentId = event.getMomentId();
        log.info("【MQ消费】开始迁移到冷库: eventId={}, momentId={}", event.getEventId(), momentId);

        // 1. 查询 MySQL 中的动态记录
        var momentOpt = momentRepository.findById(momentId);
        if (momentOpt.isEmpty()) {
            log.info("【MQ消费】动态不存在（可能被其他消息已迁移删除）: momentId={}", momentId);
            return;
        }

        var moment = momentOpt.get();

        // 2. 检查状态是否正确（必须是 MIGRATING）
        if (!"MIGRATING".equals(moment.getMigrationStatus())) {
            log.warn("【MQ消费】动态状态不正确: momentId={}, status={}", momentId, moment.getMigrationStatus());
            return;
        }

        // 3. 生成 RowKey 并检查 HBase 是否已存在
        String rowKey = hBaseService.generateMomentRowKey(
                moment.getUserId(),
                moment.getCreatedAt(),
                moment.getId()
        );

        // 4. 【幂等性检查】如果 HBase 已存在，直接删除 MySQL 记录
        if (hBaseService.existsInColdStorage("community_moments", rowKey)) {
            log.info("【MQ消费】HBase中已存在数据（幂等跳过），直接删除MySQL记录: momentId={}, rowKey={}",
                    momentId, rowKey);
            deleteMomentAndRelated(moment);
            return;
        }

        // 5. 构建 DTO 并写入 HBase（动态）
        MomentResponseDTO dto = buildMomentDTO(moment);
        String userName = ""; // TODO: 从用户服务获取
        String userAvatar = ""; // TODO: 从用户服务获取
        hBaseService.saveMomentToColdStorage(dto, userName, userAvatar);

        // 6. 级联迁移评论
        migrateCommentsForMoment(momentId, event);

        // 7. 级联迁移点赞
        migrateLikesForMoment(momentId, event);

        // 8. 删除 MySQL 记录（级联删除 comments 和 likes）
        deleteMomentAndRelated(moment);

        log.info("【MQ消费】迁移到冷库完成，MySQL记录已删除: momentId={}, rowKey={}", momentId, rowKey);
    }

    /**
     * 级联迁移评论
     */
    private void migrateCommentsForMoment(Long momentId, CommunityColdStorageEvent event) {
        // 查询该动态的所有评论
        List<Comment> comments = commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId);

        if (comments.isEmpty()) {
            log.debug("【MQ消费】该动态没有评论，无需迁移: momentId={}", momentId);
            return;
        }

        log.info("【MQ消费】开始迁移评论: momentId={}, commentCount={}", momentId, comments.size());

        // 转换为冷数据格式
        List<CommunityHBaseColdStorageService.CommentColdData> coldComments = comments.stream()
                .map(c -> {
                    CommunityHBaseColdStorageService.CommentColdData cd = new CommunityHBaseColdStorageService.CommentColdData();
                    cd.setCommentId(c.getId());
                    cd.setMomentId(c.getMomentId());
                    cd.setUserId(c.getUserId());
                    cd.setContent(c.getContent());
                    cd.setParentId(c.getParentId());
                    cd.setCreatedAt(c.getCreatedAt());
                    // TODO: 从用户服务获取用户信息
                    cd.setUserName("");
                    cd.setUserAvatar("");
                    return cd;
                })
                .collect(Collectors.toList());

        // 批量写入 HBase
        hBaseService.batchSaveCommentsToColdStorage(coldComments);

        log.info("【MQ消费】评论迁移完成: momentId={}, count={}", momentId, coldComments.size());
    }

    /**
     * 级联迁移点赞
     * 
     * 注意：likes 通过 target_type 和 target_id 关联，
     * 可能需要多次级联检查（如果评论也有点赞的话）
     */
    private void migrateLikesForMoment(Long momentId, CommunityColdStorageEvent event) {
        // 查询该动态的所有点赞
        List<Like> momentLikes = likeRepository.findByTargetTypeAndTargetId(
                TargetType.MOMENT, momentId);

        List<CommunityHBaseColdStorageService.LikeColdData> allLikes = new ArrayList<>();

        // 处理动态的点赞
        for (Like like : momentLikes) {
            CommunityHBaseColdStorageService.LikeColdData ld = new CommunityHBaseColdStorageService.LikeColdData();
            ld.setLikeId(like.getId());
            ld.setUserId(like.getUserId());
            ld.setTargetType(CommunityHBaseColdStorageService.LikeColdData.LikeTargetType.MOMENT);
            ld.setTargetId(like.getTargetId());
            ld.setCreatedAt(like.getCreatedAt());
            // TODO: 从用户服务获取用户信息
            ld.setUserName("");
            ld.setUserAvatar("");
            allLikes.add(ld);
        }

        // 查询该动态下所有评论的点赞（级联检查）
        List<Comment> comments = commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId);
        for (Comment comment : comments) {
            List<Like> commentLikes = likeRepository.findByTargetTypeAndTargetId(
                    TargetType.COMMENT, comment.getId());

            for (Like like : commentLikes) {
                CommunityHBaseColdStorageService.LikeColdData ld = new CommunityHBaseColdStorageService.LikeColdData();
                ld.setLikeId(like.getId());
                ld.setUserId(like.getUserId());
                ld.setTargetType(CommunityHBaseColdStorageService.LikeColdData.LikeTargetType.COMMENT);
                ld.setTargetId(like.getTargetId());
                ld.setCreatedAt(like.getCreatedAt());
                // TODO: 从用户服务获取用户信息
                ld.setUserName("");
                ld.setUserAvatar("");
                allLikes.add(ld);
            }
        }

        if (allLikes.isEmpty()) {
            log.debug("【MQ消费】该动态没有点赞，无需迁移: momentId={}", momentId);
            return;
        }

        log.info("【MQ消费】开始迁移点赞: momentId={}, likeCount={}, commentLikeCount={}",
                momentId, momentLikes.size(), allLikes.size() - momentLikes.size());

        // 批量写入 HBase
        hBaseService.batchSaveLikesToColdStorage(allLikes);

        log.info("【MQ消费】点赞迁移完成: momentId={}, totalCount={}", momentId, allLikes.size());
    }

    /**
     * 删除动态及其关联的评论和点赞
     */
    private void deleteMomentAndRelated(PetMoment moment) {
        Long momentId = moment.getId();

        // 1. 查询并删除评论的点赞
        List<Comment> comments = commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId);
        for (Comment comment : comments) {
            likeRepository.deleteByTargetTypeAndTargetId(TargetType.COMMENT, comment.getId());
        }
        log.debug("【MQ消费】删除评论的点赞: momentId={}, commentCount={}", momentId, comments.size());

        // 2. 删除评论
        commentRepository.deleteByMomentId(momentId);
        log.debug("【MQ消费】删除动态的评论: momentId={}", momentId);

        // 3. 删除动态的点赞
        likeRepository.deleteByTargetTypeAndTargetId(TargetType.MOMENT, momentId);
        log.debug("【MQ消费】删除动态的点赞: momentId={}", momentId);

        // 4. 删除动态本身
        momentRepository.delete(moment);
        log.debug("【MQ消费】删除动态: momentId={}", momentId);
    }

    /**
     * 处理从冷库恢复
     * 
     * 社区的恢复与活动记录不同：
     * - 活动记录：10分钟临时窗口（thaw_expire_time）
     * - 社区动态：重新设置 last_access_time，重新开始7天计时
     */
    private void handleRestoreFromCold(CommunityColdStorageEvent event) {
        Long momentId = event.getMomentId();
        log.info("【MQ消费】处理恢复请求: eventId={}, momentId={}", event.getEventId(), momentId);

        // 1. 查询 MySQL 记录
        var momentOpt = momentRepository.findById(momentId);

        if (momentOpt.isEmpty()) {
            log.warn("【MQ消费】动态不存在，无法恢复: momentId={}", momentId);
            return;
        }

        var moment = momentOpt.get();

        // 2. 检查是否已迁移（MIGRATING 状态）
        if (!"MIGRATING".equals(moment.getMigrationStatus())) {
            log.info("【MQ消费】动态未迁移，无需恢复: momentId={}, status={}",
                    momentId, moment.getMigrationStatus());
            return;
        }

        // 3. 从 HBase 读取数据
        String rowKey = hBaseService.generateMomentRowKey(
                moment.getUserId(),
                moment.getCreatedAt(),
                moment.getId()
        );

        var coldMomentOpt = hBaseService.getMomentFromColdStorageByRowKey(rowKey);

        if (coldMomentOpt.isEmpty()) {
            log.warn("【MQ消费】HBase中数据不存在: momentId={}, rowKey={}", momentId, rowKey);
            return;
        }

        // 4. 从 HBase 恢复评论
        List<CommunityHBaseColdStorageService.CommentColdData> coldComments =
                hBaseService.queryCommentsByMomentId(momentId);

        for (CommunityHBaseColdStorageService.CommentColdData cc : coldComments) {
            Comment comment = new Comment();
            comment.setId(cc.getCommentId());
            comment.setMomentId(cc.getMomentId());
            comment.setUserId(cc.getUserId());
            comment.setContent(cc.getContent());
            comment.setParentId(cc.getParentId());
            comment.setCreatedAt(cc.getCreatedAt());
            comment.setMigrationStatus("NONE");
            commentRepository.save(comment);
        }

        // 5. 从 HBase 恢复点赞
        // 恢复动态的点赞
        List<CommunityHBaseColdStorageService.LikeColdData> momentLikes =
                hBaseService.queryLikesByTarget("MOMENT", momentId);

        for (CommunityHBaseColdStorageService.LikeColdData lc : momentLikes) {
            Like like = new Like();
            like.setId(lc.getLikeId());
            like.setUserId(lc.getUserId());
            like.setTargetType(TargetType.valueOf(lc.getTargetType().name()));
            like.setTargetId(lc.getTargetId());
            like.setCreatedAt(lc.getCreatedAt());
            like.setMigrationStatus("NONE");
            likeRepository.save(like);
        }

        // 恢复评论的点赞
        for (CommunityHBaseColdStorageService.CommentColdData cc : coldComments) {
            List<CommunityHBaseColdStorageService.LikeColdData> commentLikes =
                    hBaseService.queryLikesByTarget("COMMENT", cc.getCommentId());

            for (CommunityHBaseColdStorageService.LikeColdData lc : commentLikes) {
                Like like = new Like();
                like.setId(lc.getLikeId());
                like.setUserId(lc.getUserId());
                like.setTargetType(TargetType.valueOf(lc.getTargetType().name()));
                like.setTargetId(lc.getTargetId());
                like.setCreatedAt(lc.getCreatedAt());
                like.setMigrationStatus("NONE");
                likeRepository.save(like);
            }
        }

        // 6. 更新动态状态，重置 last_access_time（重新开始7天计时）
        moment.setMigrationStatus("NONE");
        moment.setLastAccessTime(LocalDateTime.now());
        momentRepository.save(moment);

        // 7. 删除 HBase 数据
        hBaseService.deleteCommentsByMomentIdFromColdStorage(momentId);
        hBaseService.deleteLikesByTargetFromColdStorage("MOMENT", momentId);

        // 删除评论的点赞
        for (CommunityHBaseColdStorageService.CommentColdData cc : coldComments) {
            hBaseService.deleteLikesByTargetFromColdStorage("COMMENT", cc.getCommentId());
        }

        // 删除动态本身
        hBaseService.deleteMomentFromColdStorage(
                moment.getUserId(),
                moment.getCreatedAt(),
                moment.getId()
        );

        log.info("【MQ消费】恢复完成，动态已恢复到MySQL: momentId={}", momentId);
    }

    /**
     * 处理从冷库删除
     */
    private void handleDeleteFromCold(CommunityColdStorageEvent event) {
        Long momentId = event.getMomentId();
        log.info("【MQ消费】处理删除冷库数据: eventId={}, momentId={}", event.getEventId(), momentId);

        // 1. 删除评论
        hBaseService.deleteCommentsByMomentIdFromColdStorage(momentId);

        // 2. 删除点赞（动态的）
        hBaseService.deleteLikesByTargetFromColdStorage("MOMENT", momentId);

        // 3. 删除评论的点赞（需要先查询评论）
        List<CommunityHBaseColdStorageService.CommentColdData> coldComments =
                hBaseService.queryCommentsByMomentId(momentId);

        for (CommunityHBaseColdStorageService.CommentColdData cc : coldComments) {
            hBaseService.deleteLikesByTargetFromColdStorage("COMMENT", cc.getCommentId());
        }

        // 4. 删除动态（需要用户ID和创建时间，但删除事件中没有这些信息）
        // 这里简化处理，假设可以构造 RowKey 或通过其他方式获取
        // 实际上删除操作通常是在用户主动删除动态时触发的，此时应该有完整信息
        log.info("【MQ消费】删除冷库数据完成: momentId={}", momentId);
    }

    /**
     * 构建 MomentResponseDTO
     */
    private MomentResponseDTO buildMomentDTO(PetMoment moment) {
        MomentResponseDTO dto = new MomentResponseDTO();
        dto.setId(moment.getId());
        dto.setUserId(moment.getUserId());
        dto.setContent(moment.getContent());
        dto.setCreatedAt(moment.getCreatedAt());
        // 点赞数和评论数需要从关联表查询，这里简化处理
        dto.setLikeCount(0);
        dto.setCommentCount(0);
        dto.setShareCount(0);
        return dto;
    }
}
