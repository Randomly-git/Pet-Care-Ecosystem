package petcare.example.community_backend.service;

import petcare.example.community_backend.client.UserServiceFacade;
import petcare.example.community_backend.client.dto.MediaResponse;
import petcare.example.community_backend.client.dto.UserResponseDTO;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.ColdArchiveData;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.repository.PetMomentRepository;
import petcare.example.community_backend.mapper.MomentMapper;
import petcare.example.community_backend.model.TargetType;
import petcare.example.community_backend.client.MediaServiceFacade;
import petcare.example.community_backend.dto.HBaseArchiveRecord;
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
     *
     * 注意：由于扁平化设计，HBase 查询需要前端传入 userId
     * 当前方法主要用于获取当前用户自己的动态列表
     */
    public List<MomentResponseDTO> getMomentsByUserId(Long userId) {
        List<MomentResponseDTO> result = new ArrayList<>();

        // 1. 查询 MySQL（热数据）
        List<PetMoment> mysqlMoments = momentRepository.findByUserIdAndAuditStatusOrderByCreatedAtDesc(userId, "APPROVED");
        log.debug("查询MySQL动态: userId={}, count={}", userId, mysqlMoments.size());

        if (mysqlMoments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 更新最后访问时间
        Set<Long> mysqlMomentIds = new HashSet<>();
        for (PetMoment moment : mysqlMoments) {
            mysqlMomentIds.add(moment.getId());
            updateLastAccessTime(moment);
        }

        // 3. 收集所需 ID
        Set<Long> userIds = mysqlMoments.stream().map(PetMoment::getUserId).collect(Collectors.toSet());

        // 4. 批量聚合数据
        Map<Long, List<MediaResponse>> mediaMap = mediaServiceFacade.batchGetMediaMap("MOMENT", mysqlMomentIds);
        Map<Long, Long> likeCounts = likeService.countLikesByTargetIds(TargetType.MOMENT, mysqlMomentIds);
        Map<Long, Long> commentCounts = commentService.countCommentsByMomentIds(mysqlMomentIds);
        Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(userIds);

        // 5. 组装 DTO 列表
        for (PetMoment moment : mysqlMoments) {
            MomentResponseDTO dto = momentMapper.toResponseDTO(moment);
            Long currentMomentId = moment.getId();

            // 检查 MIGRATING 状态
            if ("MIGRATING".equals(moment.getMigrationStatus())) {
                log.debug("动态正在迁移中: momentId={}", currentMomentId);
                continue; // 跳过迁移中的动态
            }

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

        // 6. 按创建时间倒序排序
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
     * 冷热分离策略：
     * - MySQL 存储所有可见数据（热数据 + 已恢复的冷数据）
     * - HBase 存储待恢复的冷数据，通过 MQ 异步恢复
     * - 当 MySQL 返回数不足 page_size 时，自动从 HBase 补齐
     *
     * 补齐流程：
     * 1. 查询 MySQL 获取分页动态
     * 2. 如果 MySQL 数量 < page_size，计算需要补齐的数量
     * 3. 扫描 HBase 获取更早的冷数据
     * 4. 发送 MQ 事件异步恢复冷数据到 MySQL
     * 5. 返回当前已有的数据（冷数据会在下次请求时出现）
     */
    public List<MomentResponseDTO> getAllMomentsWithPagination(Pageable pageable) {
        // 1. 查询数据库获取分页的动态实体
        Page<PetMoment> momentPage = momentRepository.findAllByAuditStatusOrderByCreatedAtDesc("APPROVED", pageable);
        List<PetMoment> moments = momentPage.getContent();

        // 2. 如果 MySQL 数量 < page_size，尝试从 HBase 补齐
        int pageSize = pageable.getPageSize();
        if (moments.size() < pageSize) {
            int missing = pageSize - moments.size();
            log.info("【冷热分离】MySQL 动态数量不足，需要补齐: current={}, required={}, missing={}",
                    moments.size(), pageSize, missing);

            // 收集已返回的动态 ID，避免重复
            Set<Long> existingMomentIds = moments.stream().map(PetMoment::getId).collect(Collectors.toSet());

            // 计算时间阈值：基于最后一条动态的时间
            LocalDateTime timeThreshold = null;
            if (!moments.isEmpty()) {
                PetMoment lastMoment = moments.get(moments.size() - 1);
                timeThreshold = lastMoment.getCreatedAt();
            }

            // 从 HBase 扫描更早的冷数据
            List<HBaseArchiveRecord> coldArchives = hBaseService.scanArchivesOlderThan(
                    timeThreshold, existingMomentIds, missing);

            if (!coldArchives.isEmpty()) {
                log.info("【冷热分离】找到 {} 条冷数据需要恢复", coldArchives.size());

                // 发送 MQ 事件异步恢复冷数据
                for (HBaseArchiveRecord archive : coldArchives) {
                    eventPublisher.publishRestoreFromColdEvent(archive.getMomentId(), archive.getUserId());
                }

                log.info("【冷热分离】已发送 {} 条冷数据恢复事件", coldArchives.size());
            } else {
                log.info("【冷热分离】HBase 中无更多冷数据可补齐");
            }
        }

        // 3. 返回已聚合的动态列表（可能不足 page_size）
        if (!moments.isEmpty()) {
            return buildMomentDTOList(moments, true);
        }

        // 4. MySQL 完全无数据，返回空列表
        log.info("【冷热分离】MySQL 暂无数据，冷数据恢复中，请稍后刷新");
        return Collections.emptyList();
    }

    /**
     * 构建 MomentResponseDTO 列表（通用方法）
     * @param updateAccess 是否更新最后访问时间（管理员操作应传 false）
     */
    private List<MomentResponseDTO> buildMomentDTOList(List<PetMoment> moments, boolean updateAccess) {
        if (moments.isEmpty()) {
            return Collections.emptyList();
        }

        // 更新最后访问时间
        if (updateAccess) {
            for (PetMoment moment : moments) {
                updateLastAccessTime(moment);
            }
        }

        // 收集所需 ID
        Set<Long> momentIds = moments.stream().map(PetMoment::getId).collect(Collectors.toSet());
        Set<Long> userIds = moments.stream().map(PetMoment::getUserId).collect(Collectors.toSet());

        // 批量聚合数据
        Map<Long, List<MediaResponse>> mediaMap = mediaServiceFacade.batchGetMediaMap("MOMENT", momentIds);
        Map<Long, Long> likeCounts = likeService.countLikesByTargetIds(TargetType.MOMENT, momentIds);
        Map<Long, Long> commentCounts = commentService.countCommentsByMomentIds(momentIds);
        Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(userIds);

        // 组装 DTO 列表
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
    /*@Transactional(rollbackFor = Exception.class)
    public PetMoment createMoment(PetMoment moment, List<Long> mediaIds) {
        // 1. 保存动态主体，获取真实 ID
        PetMoment savedMoment = momentRepository.save(moment);
        Long momentId = savedMoment.getId();

        // 2. 批量关联媒体文件
        if (mediaIds != null && !mediaIds.isEmpty()) {
            mediaServiceFacade.batchUpdateRelatedId(mediaIds, "MOMENT", momentId);
        }

        return savedMoment;
    }*/
    @Transactional(rollbackFor = Exception.class)
    public PetMoment createMoment(PetMoment moment, List<Long> mediaIds) {
        // 【一致性保障】强制初始状态为审核中
        moment.setAuditStatus("PENDING"); 
        
        PetMoment savedMoment = momentRepository.save(moment);
        Long momentId = savedMoment.getId();

        if (mediaIds != null && !mediaIds.isEmpty()) {
            mediaServiceFacade.batchUpdateRelatedId(mediaIds, "MOMENT", momentId);
        }
        return savedMoment;
    }

    /**
     * 修改动态
     * 规则：
     * 1. 正在迁移 (MIGRATING) 的动态禁止修改
     * 2. 修改后 audit_status 必须重置为 PENDING 重新审核
     * 3. 更新 last_access_time
     */
    @Transactional(rollbackFor = Exception.class)
    public MomentResponseDTO updateMoment(Long momentId, Long userId, String content, List<Long> mediaIds) {
        PetMoment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new IllegalArgumentException("动态不存在"));

        // 1. 权限校验
        if (!moment.getUserId().equals(userId)) {
            throw new SecurityException("无权修改他人的动态");
        }

        // 2. 冷迁移保护：正在搬运的数据禁止修改，以防 MySQL 和 HBase 数据不一致
        if ("MIGRATING".equals(moment.getMigrationStatus())) {
            log.warn("【冷迁移保护】拒绝修改正在迁移的动态: momentId={}", momentId);
            throw new IllegalStateException("动态正在数据整理中，请稍后再试");
        }

        // 3. 业务逻辑处理
        moment.setContent(content);
        // 核心：修改后必须重新审核
        moment.setAuditStatus("PENDING");
        // 更新访问时间，确保它不会立即被当成冷数据搬走
        moment.setLastAccessTime(LocalDateTime.now());

        PetMoment saved = momentRepository.save(moment);

        // 4. 更新媒体关联（如果是异步关联，这里可以发送 MQ）
        if (mediaIds != null && !mediaIds.isEmpty()) {
            mediaServiceFacade.batchUpdateRelatedId(mediaIds, "MOMENT", momentId);
        }

        return buildMomentDTO(saved);
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
     * 根据ID获取单个动态
     *
     * @param momentId 动态ID
     * @param userId   用户ID（可选，推荐传入，用于定位冷数据）
     *
     * 冷热分离：
     * 1. 先查询 MySQL
     * 2. 如果 MySQL 不存在，说明数据可能被迁移到 HBase，发送 MQ 恢复事件
     * 3. 返回空，前端会提示用户稍后刷新
     * 
     * 注意：前端不会直接看到冷数据，必须等 MQ 恢复完成并写入 MySQL 后才能看到
     */
    public Optional<MomentResponseDTO> getMomentById(Long momentId, Long userId) {
        // 1. 查询 MySQL
        Optional<PetMoment> momentOpt = momentRepository.findById(momentId);

        if (momentOpt.isPresent()) {
            PetMoment moment = momentOpt.get();

            // 检查迁移状态
            if ("MIGRATING".equals(moment.getMigrationStatus())) {
                // 正在迁移中，发送恢复请求
                log.info("动态正在迁移中，发送恢复请求: momentId={}", momentId);
                eventPublisher.publishRestoreFromColdEvent(momentId, moment.getUserId());
                return Optional.empty();
            }

            // 更新最后访问时间
            updateLastAccessTime(moment);

            // 从 MySQL 返回
            return Optional.of(buildMomentDTO(moment));
        }

        // 2. MySQL 不存在，发送 MQ 恢复事件
        if (userId != null) {
            log.info("MySQL中无记录，发送冷数据恢复请求: momentId={}, userId={}", momentId, userId);
            eventPublisher.publishRestoreFromColdEvent(momentId, userId);
        } else {
            log.warn("MySQL中无记录，且未提供userId，无法发送恢复请求: momentId={}", momentId);
        }

        // 返回空，前端提示用户稍后刷新
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
     * 查询冷数据（扁平化版本）
     *
     * @param momentId 动态ID
     * @param userId   用户ID（必须传入，用于拼装 RowKey）
     *
     * 扁平化设计优势：
     * - 只需 1 次 HBase Get 操作
     * - 评论和点赞已包含在 full_data 中，无需额外查询
     */
    private Optional<MomentResponseDTO> queryColdMomentById(Long momentId, Long userId) {
        if (userId == null) {
            log.error("【冷数据查询】缺少 userId，无法定位冷数据: momentId={}", momentId);
            return Optional.empty();
        }

        // 直接通过 userId 拼装 RowKey 查询
        var archiveRecordOpt = hBaseService.getArchive(userId, momentId);
        if (archiveRecordOpt.isEmpty()) {
            log.debug("【冷数据查询】HBase 中未找到记录: momentId={}, userId={}", momentId, userId);
            return Optional.empty();
        }

        var record = archiveRecordOpt.get();

        // 从 full_data 获取完整数据
        ColdArchiveData archiveData = hBaseService.getArchiveData(userId, momentId);

        // 构建 MomentResponseDTO
        MomentResponseDTO dto = new MomentResponseDTO();
        dto.setId(record.getMomentId());
        dto.setUserId(record.getUserId());
        dto.setContent(record.getContent());
        dto.setCreatedAt(record.getCreatedAt());

        // 从 UserService 获取作者信息
        UserResponseDTO author = userServiceFacade.batchGetUsers(Set.of(record.getUserId()))
                .get(record.getUserId());
        if (author != null) {
            dto.setAuthorName(author.getNickname() != null ? author.getNickname() : "宠物爱好者");
            dto.setAuthorAvatar(author.getAvatarUrl());
        }

        // 从 full_data 的元数据获取计数
        if (archiveData != null && archiveData.getMetadata() != null) {
            dto.setLikeCount(archiveData.getMetadata().getLikeCount());
            dto.setCommentCount(archiveData.getMetadata().getCommentCount());
        } else {
            dto.setLikeCount(0);
            dto.setCommentCount(0);
        }

        log.info("【冷数据查询】从HBase读取成功: momentId={}, userId={}", momentId, userId);
        return Optional.of(dto);
    }

    /**
     * 更新动态的最后访问时间
     * 如果状态为 MIGRATING，不更新（数据正在被迁移，访问时间无意义）
     */
    private void updateLastAccessTime(PetMoment moment) {
        // 【冷迁移保护】如果正在迁移中，不更新访问时间
        if ("MIGRATING".equals(moment.getMigrationStatus())) {
            log.debug("【冷迁移保护】动态正在迁移中，跳过更新访问时间: momentId={}", moment.getId());
            return;
        }
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

    // ==================== 审核流状态机操作 ====================

    /**
     * 审核通过
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean approveMoment(Long momentId) {
        log.info("【内容审核】尝试通过动态: momentId={}", momentId);
        // 使用 CAS 乐观锁：只允许从 PENDING 改为 APPROVED
        int updated = momentRepository.updateAuditStatus(momentId, "PENDING", "APPROVED");
        
        if (updated == 0) {
            // 幂等性拦截：如果返回 0，说明要么帖子不存在，要么已经被别的管理员点过通过了
            log.warn("【内容审核】动态已被处理或不存在，幂等拦截: momentId={}", momentId);
            throw new IllegalStateException("该动态已被处理，请勿重复操作");
        }
        return true;
    }

    /**
     * 审核拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectMoment(Long momentId) {
        log.info("【内容审核】尝试拒绝动态: momentId={}", momentId);
        int updated = momentRepository.updateAuditStatus(momentId, "PENDING", "REJECTED");
        
        if (updated == 0) {
            throw new IllegalStateException("该动态已被处理，请勿重复操作");
        }
        return true;
    }

    /**
     * 获取待审核列表（给管理员用）
     */
    public List<MomentResponseDTO> getPendingMoments(Pageable pageable) {
        Page<PetMoment> pendingPage = momentRepository.findAllByAuditStatusOrderByCreatedAtDesc("PENDING", pageable);
        return buildMomentDTOList(pendingPage.getContent(), false);
    }
}