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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
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

    /**
     * 获取特定用户ID的所有动态，并转换为 DTO 列表。
     * 核心：负责从多个服务（用户、媒体、点赞、评论）聚合数据。
     */
    public List<MomentResponseDTO> getMomentsByUserId(Long userId) {
        // 1. 查询数据库获取所有动态实体
        List<PetMoment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(userId);

        if (moments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 收集所需 ID
        Set<Long> momentIds = moments.stream().map(PetMoment::getId).collect(Collectors.toSet());
        // 收集所有动态的作者 ID
        Set<Long> userIds = moments.stream().map(PetMoment::getUserId).collect(Collectors.toSet());

        // 3. 批量聚合数据
        // a. 批量获取媒体文件
        Map<Long, List<MediaResponse>> mediaMap = mediaServiceFacade.batchGetMediaMap("MOMENT", momentIds);

        // b. 批量获取动态点赞数 (调用 LikeService)
        Map<Long, Long> likeCounts = likeService.countLikesByTargetIds(TargetType.MOMENT, momentIds);

        // c. 批量获取动态评论数 (调用 CommentService)
        Map<Long, Long> commentCounts = commentService.countCommentsByMomentIds(momentIds);

        // d. 批量获取作者信息 (调用 UserServiceFacade)
        Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(userIds); // 【修改点 1】: 调用 UserServiceFacade

        // 4. 组装 DTO 列表
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
     * 获取所有用户的动态，支持分页。
     * 核心：负责从多个服务（用户、媒体、点赞、评论）聚合数据，支持分页加载。
     */
    public List<MomentResponseDTO> getAllMomentsWithPagination(Pageable pageable) {
        // 1. 查询数据库获取分页的动态实体
        Page<PetMoment> momentPage = momentRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<PetMoment> moments = momentPage.getContent();

        if (moments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 收集所需 ID
        Set<Long> momentIds = moments.stream().map(PetMoment::getId).collect(Collectors.toSet());
        // 收集所有动态的作者 ID
        Set<Long> userIds = moments.stream().map(PetMoment::getUserId).collect(Collectors.toSet());

        // 3. 批量聚合数据
        // a. 批量获取媒体文件
        Map<Long, List<MediaResponse>> mediaMap = mediaServiceFacade.batchGetMediaMap("MOMENT", momentIds);

        // b. 批量获取动态点赞数 (调用 LikeService)
        Map<Long, Long> likeCounts = likeService.countLikesByTargetIds(TargetType.MOMENT, momentIds);

        // c. 批量获取动态评论数 (调用 CommentService)
        Map<Long, Long> commentCounts = commentService.countCommentsByMomentIds(momentIds);

        // d. 批量获取作者信息 (调用 UserServiceFacade)
        Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(userIds);

        // 4. 组装 DTO 列表
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
     * **已修改：** 不再接收文件，而是接收媒体ID列表，并调用媒体服务进行批量关联。
     */
    @Transactional(rollbackFor = Exception.class) // 确保媒体关联失败时，动态主体回滚
    public PetMoment createMoment(PetMoment moment, List<Long> mediaIds) { // <-- 移除 files, 接收 mediaIds
        // 1. 保存动态主体，获取真实 ID
        PetMoment savedMoment = momentRepository.save(moment);
        Long momentId = savedMoment.getId();

        // 2. 批量关联媒体文件
        // 只有当 mediaIds 不为空时才调用媒体服务进行关联
        if (mediaIds != null && !mediaIds.isEmpty()) {
            // 调用 MediaServiceFacade 批量更新 relatedId
            // 如果此步骤失败（如媒体服务宕机或业务失败），将抛出异常，触发事务回滚。
            mediaServiceFacade.batchUpdateRelatedId(
                    mediaIds,
                    "MOMENT",
                    momentId
            );
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
            // 使用 LikeService 提供的批量删除方法
            likeService.deleteLikesByTargetTypeAndTargetIds(TargetType.MOMENT, Set.of(momentId));

            // 3. 删除媒体文件 (调用 MediaServiceFacade)
            // 使用 MediaServiceFacade 提供的 deleteRelatedFiles 方法
            mediaServiceFacade.deleteRelatedFiles("MOMENT", momentId);

            // 4. 删除动态主体
            momentRepository.deleteById(momentId);

            return true;
        }
        return false;
    }

    /**
     * 根据ID获取单个动态
     */
    public Optional<PetMoment> getMomentById(Long momentId) {
        return momentRepository.findById(momentId);
    }
}