package petcare.example.community_backend.service;

import petcare.example.community_backend.client.UserServiceFacade;
import petcare.example.community_backend.client.dto.MediaResponse;
import petcare.example.community_backend.client.dto.UserResponseDTO;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.repository.PetMomentRepository;
import petcare.example.community_backend.mapper.MomentMapper;
import petcare.example.community_backend.model.TargetType;
import petcare.example.community_backend.client.MediaServiceFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MomentService {

    private final PetMomentRepository momentRepository;
    private final MomentMapper momentMapper;
    private final CommentService commentService;
    private final LikeService likeService;
    private final MediaServiceFacade mediaServiceFacade;
    private final UserServiceFacade userServiceFacade;
    private final CommunityHBaseColdStorageService hBaseService;
    private final CommunityColdStorageEventPublisher eventPublisher;

    /**
     * 获取特定用户ID的所有动态，并转换为 DTO 列表。
     * 核心：负责从多个服务（用户、媒体、点赞、评论）聚合数据。
     * 
     * 冷热分离：同时查询 MySQL（热数据）和 HBase（冷数据）
     */
    public List<MomentResponseDTO> getMomentsByUserId(Long userId) {
        List<MomentResponseDTO> result = new ArrayList<>();

        // 1. 查询 MySQL（热数据）
        List<PetMoment> mysqlMoments = momentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        log.debug("查询MySQL动态: userId={}, count={}", userId, mysqlMoments.size());

        // 2. 查询 HBase（冷数据）
        List<MomentResponseDTO> hbaseMoments = hBaseService.queryMomentsByUserId(userId);
        log.debug("查询HBase动态: userId={}, count={}", userId, hbaseMoments.size());

        // 3. 合并结果
        Set<Long> allMomentIds = new HashSet<>();

        // 处理 MySQL 数据
        for (PetMoment moment : mysqlMoments) {
            allMomentIds.add(moment.getId());
            // 更新最后访问时间
            updateLastAccessTime(moment);
        }

        // 处理 HBase 数据
        for (MomentResponseDTO hbaseMoment : hbaseMoments) {
            if (!allMomentIds.contains(hbaseMoment.getId())) {
                allMomentIds.add(hbaseMoment.getId());
                result.add(hbaseMoment);
            }
        }

        if (allMomentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 批量聚合数据（排除 HBase 数据中已有的）
        Set<Long> mysqlMomentIds = mysqlMoments.stream().map(PetMoment::getId).collect(Collectors.toSet());
        Set<Long> userIds = mysqlMoments.stream().map(PetMoment::getUserId).collect(Collectors.toSet());

        // 5. 批量聚合数据
        Map<Long, List<MediaResponse>> mediaMap = mediaServiceFacade.batchGetMediaMap("MOMENT", mysqlMomentIds);
        Map<Long, Long> likeCounts = likeService.countLikesByTargetIds(TargetType.MOMENT, mysqlMomentIds);
        Map<Long, Long> commentCounts = commentService.countCommentsByMomentIds(mysqlMomentIds);
        Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(userIds);

        // 6. 组装 MySQL 的 DTO 列表
        for (PetMoment moment : mysqlMoments) {
            MomentResponseDTO dto = momentMapper.toResponseDTO(moment);
            Long currentMomentId = moment.getId();

            // 聚合媒体 URLs
            List<String> mediaUrls = mediaMap.getOrDefault(currentMomentId, Collections.emptyList()).stream()
                    .map(MediaResponse::getFileUrl)
                    .collect(Collectors.toList());
            dto.setMediaUrls(mediaUrls);

            // 聚合计数
            dto.setLikeCount(likeCounts.getOrDefault(currentMomentId, 0L).intValue());
            dto.setCommentCount(commentCounts.getOrDefault(currentMomentId, 0L).intValue());

            // 聚合作者信息
            UserResponseDTO author = userMap.get(moment.getUserId());
            if (author != null) {
                dto.setAuthorName(author.getNickname() != null ? author.getNickname() : "宠物爱好者");
                dto.setAuthorAvatar(author.getAvatarUrl());
            }

            result.add(dto);
        }

        // 7. 为 HBase 数据补充聚合信息
        for (MomentResponseDTO dto : hbaseMoments) {
            List<String> mediaUrls = mediaMap.getOrDefault(dto.getId(), Collections.emptyList()).stream()
                    .map(MediaResponse::getFileUrl)
                    .collect(Collectors.toList());
            dto.setMediaUrls(mediaUrls);

            // HBase 中已有点赞数和评论数，不需要额外查询
        }

        // 8. 按创建时间倒序排序
        result.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        log.info("查询动态完成: userId={}, totalCount={}", userId, result.size());
        return result;
    }

    /**
     * 获取所有用户的动态，支持分页。
     * 核心：负责从多个服务（用户、媒体、点赞、评论）聚合数据，支持分页加载。
     * 
     * 注意：分页场景下只查询 MySQL（热数据），HBase 冷数据通过单独接口访问
     */
    public List<MomentResponseDTO> getAllMomentsWithPagination(Pageable pageable) {
        // 1. 查询数据库获取分页的动态实体
        Page<PetMoment> momentPage = momentRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<PetMoment> moments = momentPage.getContent();

        if (moments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 更新最后访问时间
        for (PetMoment moment : moments) {
            updateLastAccessTime(moment);
        }

        // 3. 收集所需 ID
        Set<Long> momentIds = moments.stream().map(PetMoment::getId).collect(Collectors.toSet());
        Set<Long> userIds = moments.stream().map(PetMoment::getUserId).collect(Collectors.toSet());

        // 4. 批量聚合数据
        Map<Long, List<MediaResponse>> mediaMap = mediaServiceFacade.batchGetMediaMap("MOMENT", momentIds);
        Map<Long, Long> likeCounts = likeService.countLikesByTargetIds(TargetType.MOMENT, momentIds);
        Map<Long, Long> commentCounts = commentService.countCommentsByMomentIds(momentIds);
        Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(userIds);

        // 5. 组装 DTO 列表
        return moments.stream().map(moment -> {
            MomentResponseDTO dto = momentMapper.toResponseDTO(moment);
            Long currentMomentId = moment.getId();

            // 聚合媒体 URLs
            List<String> mediaUrls = mediaMap.getOrDefault(currentMomentId, Collections.emptyList()).stream()
                    .map(MediaResponse::getFileUrl)
                    .collect(Collectors.toList());
            dto.setMediaUrls(mediaUrls);

            // 聚合计数
            dto.setLikeCount(likeCounts.getOrDefault(currentMomentId, 0L).intValue());
            dto.setCommentCount(commentCounts.getOrDefault(currentMomentId, 0L).intValue());

            // 聚合作者信息
            UserResponseDTO author = userMap.get(moment.getUserId());
            if (author != null) {
                dto.setAuthorName(author.getNickname() != null ? author.getNickname() : "宠物爱好者");
                dto.setAuthorAvatar(author.getAvatarUrl());
            }

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 创建动态
     * 不再接收文件，而是接收媒体ID列表，并调用媒体服务进行批量关联。
     */
    @Transactional(rollbackFor = Exception.class)
    public PetMoment createMoment(PetMoment moment, List<Long> mediaIds) {
        // 1. 保存动态主体，获取真实 ID
        PetMoment savedMoment = momentRepository.save(moment);
        Long momentId = savedMoment.getId();

        // 2. 批量关联媒体文件
        if (mediaIds != null && !mediaIds.isEmpty()) {
            mediaServiceFacade.batchUpdateRelatedId(mediaIds, "MOMENT", momentId);
        }

        return savedMoment;
    }

    /**
     * 删除动态（应确保事务一致性，同时删除评论、点赞和媒体文件）
     *
     * @param momentId 动态ID
     * @param userId   用户ID（用于权限验证和冷库删除）
     * @return true 删除成功
     * @throws SecurityException 如果用户不是动态的作者
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMoment(Long momentId, Long userId) {
        if (userId == null) {
            log.error("删除动态失败：userId 不能为空");
            throw new IllegalArgumentException("userId 不能为空");
        }

        Optional<PetMoment> momentOpt = momentRepository.findById(momentId);

        if (momentOpt.isPresent()) {
            PetMoment moment = momentOpt.get();

            // 【权限验证】确保只有动态作者才能删除
            if (!moment.getUserId().equals(userId)) {
                log.warn("【权限校验】用户 {} 无权删除动态 {}，该动态属于用户 {}",
                        userId, momentId, moment.getUserId());
                throw new SecurityException("无权删除他人的动态");
            }

            // 1. 级联删除评论及其点赞 (调用 CommentService)
            commentService.deleteCommentsByMomentId(momentId);

            // 2. 删除动态的点赞 (调用 LikeService)
            likeService.deleteLikesByTargetTypeAndTargetIds(TargetType.MOMENT, Set.of(momentId));

            // 3. 删除媒体文件 (调用 MediaServiceFacade)
            mediaServiceFacade.deleteRelatedFiles("MOMENT", momentId);

            // 4. 删除动态主体
            momentRepository.deleteById(momentId);

            // 5. 【冷库清理】如果动态已被迁移到冷库，需要发送 MQ 事件删除冷库数据
            // 注意：正常情况下，动态应该在 MySQL 中被找到并删除
            // 但如果 MySQL 中没有记录（已被迁移后删除），则需要从冷库删除
            // 这种情况通常发生在：迁移成功 -> MySQL 删除 -> 再次删除请求
            log.info("【冷库清理】动态已从 MySQL 删除，发送冷库清理事件: momentId={}, userId={}",
                    momentId, userId);
            eventPublisher.publishDeleteFromColdEvent(momentId, userId);

            return true;
        }

        // MySQL 中没有记录，可能是冷数据（已被迁移）
        // 需要从冷库中删除
        log.info("【冷库清理】MySQL 中无记录，检查冷库: momentId={}, userId={}", momentId, userId);

        // 直接发送冷库删除事件（幂等操作，已删除则无影响）
        eventPublisher.publishDeleteFromColdEvent(momentId, userId);

        return true;
    }

    /**
     * 根据ID获取单个动态（推荐版本）
     *
     * @param momentId 动态ID
     * @param userId   用户ID（可选，推荐传入）
     *                 传入 userId 时可直接拼装 HBase RowKey 快速定位冷数据
     *
     * 冷热分离：
     * 1. 先查询 MySQL
     * 2. 如果 MySQL 存在且状态为 MIGRATING，说明正在迁移中，触发恢复
     * 3. 如果 MySQL 不存在，根据 userId 查询 HBase
     * 4. 如果 HBase 存在，说明是冷数据，触发恢复并写回 MySQL
     */
    public Optional<MomentResponseDTO> getMomentById(Long momentId, Long userId) {
        // 1. 查询 MySQL
        Optional<PetMoment> momentOpt = momentRepository.findById(momentId);

        if (momentOpt.isPresent()) {
            PetMoment moment = momentOpt.get();

            // 检查迁移状态
            if ("MIGRATING".equals(moment.getMigrationStatus())) {
                // 正在迁移中，等待完成后再返回
                log.info("动态正在迁移中，发送恢复请求: momentId={}", momentId);
                eventPublisher.publishRestoreFromColdEvent(momentId, moment.getUserId());
                return Optional.empty();
            }

            // 更新最后访问时间
            updateLastAccessTime(moment);

            // 从 MySQL 返回
            return Optional.of(buildMomentDTO(moment));
        }

        // 2. MySQL 不存在，从 HBase 查询
        log.info("MySQL中无记录，尝试从HBase查询: momentId={}, userId={}", momentId, userId);

        Optional<MomentResponseDTO> coldMomentOpt = queryColdMomentById(momentId, userId);

        if (coldMomentOpt.isPresent()) {
            MomentResponseDTO coldMoment = coldMomentOpt.get();

            // 发送恢复事件
            log.info("HBase中存在冷数据，发送恢复请求: momentId={}", momentId);
            eventPublisher.publishRestoreFromColdEvent(momentId, userId);

            return Optional.of(coldMoment);
        }

        // 3. 都不存在
        log.warn("动态不存在: momentId={}", momentId);
        return Optional.empty();
    }

    /**
     * 根据ID获取单个动态（兼容版本，不推荐）
     *
     * @param momentId 动态ID
     * @deprecated 请使用 {@link #getMomentById(Long, Long)} 并传入 userId
     */
    @Deprecated
    public Optional<MomentResponseDTO> getMomentById(Long momentId) {
        return getMomentById(momentId, null);
    }

    /**
     * 查询冷数据
     *
     * @param momentId 动态ID
     * @param userId   用户ID（可选，推荐传入）
     *                 传入 userId 时直接拼装 RowKey 查询
     *                 未传入 userId 时尝试从 HBase 扫描（性能较差）
     */
    private Optional<MomentResponseDTO> queryColdMomentById(Long momentId, Long userId) {
        String rowKey;

        if (userId != null) {
            // 方案A：直接通过 userId 拼装 RowKey（推荐）
            rowKey = hBaseService.generateMomentRowKey(userId, momentId);
            log.debug("【冷数据查询】通过 userId 直接生成 RowKey: momentId={}, userId={}, rowKey={}",
                    momentId, userId, rowKey);
        } else {
            // 方案B：扫描 HBase 查找匹配的 momentId（兜底，性能差）
            log.warn("【冷数据查询】未传入 userId，将进行全表扫描，性能较差: momentId={}", momentId);
            Optional<String> rowKeyOpt = hBaseService.scanForMomentRowKey(momentId);
            if (rowKeyOpt.isEmpty()) {
                log.debug("【冷数据查询】HBase 中未找到记录: momentId={}", momentId);
                return Optional.empty();
            }
            rowKey = rowKeyOpt.get();
        }

        // 通过 RowKey 查询 HBase
        return hBaseService.getMomentFromColdStorageByRowKey(rowKey);
    }

    /**
     * 更新动态的最后访问时间
     */
    private void updateLastAccessTime(PetMoment moment) {
        LocalDateTime now = LocalDateTime.now();
        moment.setLastAccessTime(now);
        momentRepository.save(moment);
    }

    /**
     * 构建 MomentResponseDTO
     */
    private MomentResponseDTO buildMomentDTO(PetMoment moment) {
        MomentResponseDTO dto = momentMapper.toResponseDTO(moment);

        // 补充聚合信息
        List<MediaResponse> mediaList = mediaServiceFacade.batchGetMediaMap("MOMENT", Set.of(moment.getId()))
                .getOrDefault(moment.getId(), Collections.emptyList());
        dto.setMediaUrls(mediaList.stream().map(MediaResponse::getFileUrl).collect(Collectors.toList()));

        dto.setLikeCount(likeService.countLikesByTargetIds(TargetType.MOMENT, Set.of(moment.getId()))
                .getOrDefault(moment.getId(), 0L).intValue());
        dto.setCommentCount(commentService.countCommentsByMomentIds(Set.of(moment.getId()))
                .getOrDefault(moment.getId(), 0L).intValue());

        UserResponseDTO author = userServiceFacade.batchGetUsers(Set.of(moment.getUserId()))
                .get(moment.getUserId());
        if (author != null) {
            dto.setAuthorName(author.getNickname() != null ? author.getNickname() : "宠物爱好者");
            dto.setAuthorAvatar(author.getAvatarUrl());
        }

        return dto;
    }
}