package petcare.example.community_backend.service;

import petcare.example.community_backend.client.UserServiceFacade;
import petcare.example.community_backend.client.dto.MediaResponse;
import petcare.example.community_backend.client.dto.UserResponseDTO;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.repository.PetMomentRepository;
import petcare.example.community_backend.mapper.MomentMapper;
import petcare.example.community_backend.repository.CommentRepository;
import petcare.example.community_backend.repository.LikeRepository;
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
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
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
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMoment(Long momentId) {
        Optional<PetMoment> momentOpt = momentRepository.findById(momentId);

        if (momentOpt.isPresent()) {
            // 1. 级联删除评论及其点赞 (调用 CommentService)
            commentService.deleteCommentsByMomentId(momentId);

            // 2. 删除动态的点赞 (调用 LikeService)
            likeService.deleteLikesByTargetTypeAndTargetIds(TargetType.MOMENT, Set.of(momentId));

            // 3. 删除媒体文件 (调用 MediaServiceFacade)
            mediaServiceFacade.deleteRelatedFiles("MOMENT", momentId);

            // 4. 删除动态主体
            momentRepository.deleteById(momentId);

            return true;
        }
        return false;
    }

    /**
     * 根据ID获取单个动态
     * 
     * 冷热分离：
     * 1. 先查询 MySQL
     * 2. 如果 MySQL 存在且状态为 MIGRATING，说明正在迁移中，触发恢复
     * 3. 如果 MySQL 不存在，从 HBase 查询
     * 4. 如果 HBase 存在，说明是冷数据，触发恢复并写回 MySQL
     */
    public Optional<MomentResponseDTO> getMomentById(Long momentId) {
        // 1. 查询 MySQL
        Optional<PetMoment> momentOpt = momentRepository.findById(momentId);

        if (momentOpt.isPresent()) {
            PetMoment moment = momentOpt.get();

            // 检查迁移状态
            if ("MIGRATING".equals(moment.getMigrationStatus())) {
                // 正在迁移中，等待完成后再返回
                log.info("动态正在迁移中，发送恢复请求: momentId={}", momentId);
                eventPublisher.publishRestoreFromColdEvent(momentId);
                return Optional.empty();
            }

            // 更新最后访问时间
            updateLastAccessTime(moment);

            // 从 MySQL 返回
            return Optional.of(buildMomentDTO(moment));
        }

        // 2. MySQL 不存在，从 HBase 查询
        log.info("MySQL中无记录，尝试从HBase查询: momentId={}", momentId);

        // 由于 RowKey 需要 userId 和 createdAt，这里简化处理
        // 实际场景中应该通过其他索引或缓存获取这些信息
        // 或者在消息中携带这些信息
        Optional<MomentResponseDTO> coldMomentOpt = queryColdMomentById(momentId);

        if (coldMomentOpt.isPresent()) {
            MomentResponseDTO coldMoment = coldMomentOpt.get();

            // 发送恢复事件
            log.info("HBase中存在冷数据，发送恢复请求: momentId={}", momentId);
            eventPublisher.publishRestoreFromColdEvent(momentId);

            return Optional.of(coldMoment);
        }

        // 3. 都不存在
        log.warn("动态不存在: momentId={}", momentId);
        return Optional.empty();
    }

    /**
     * 查询冷数据
     * 由于 RowKey 格式需要 userId 和 createdAt，这里需要特殊处理
     * 简化方案：扫描 HBase 查找指定 ID 的记录
     */
    private Optional<MomentResponseDTO> queryColdMomentById(Long momentId) {
        // TODO: 实现根据 momentId 扫描 HBase 的逻辑
        // 实际生产中可以通过维护一个 MySQL 索引表来快速定位
        log.debug("查询冷数据: momentId={} (需要实现索引定位)", momentId);
        return Optional.empty();
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