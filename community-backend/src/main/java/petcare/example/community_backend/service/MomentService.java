// MomentService.java
package petcare.example.community_backend.service;

import petcare.example.community_backend.client.UserServiceFacade;
import petcare.example.community_backend.client.dto.MediaResponse;
import petcare.example.community_backend.client.dto.UserResponseDTO;
import petcare.example.community_backend.dto.MomentCreateRequestDTO;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.repository.PetMomentRepository;
import petcare.example.community_backend.mapper.MomentMapper;
import petcare.example.community_backend.model.TargetType;
import petcare.example.community_backend.client.MediaServiceFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomentService {

    private final PetMomentRepository momentRepository;
    private final MomentMapper momentMapper;
    private final CommentService commentService;
    private final LikeService likeService;
    private final MediaServiceFacade mediaServiceFacade;
    private final UserServiceFacade userServiceFacade;

    /**
     * 获取用户动态列表 (已优化 N+1)
     */
    public List<MomentResponseDTO> getMomentsByUserId(Long userId) {
        // 1. 查询数据库获取所有动态实体
        List<PetMoment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (moments.isEmpty()) return Collections.emptyList();

        // 2. 收集 ID
        List<Long> momentIds = moments.stream().map(PetMoment::getId).collect(Collectors.toList());
        Set<Long> userIds = moments.stream().map(PetMoment::getUserId).collect(Collectors.toSet());

        // 3. 批量聚合数据 (全部采用单次请求/单次查询)

        // a. 调用优化后的批量媒体接口
        Map<Long, List<MediaResponse>> mediaMap = mediaServiceFacade.getMediaFilesBatch("MOMENT", momentIds);

        // b. 批量获取点赞数
        Map<Long, Long> likeCounts = likeService.countLikesByTargetIds(TargetType.MOMENT, new HashSet<>(momentIds));

        // c. 批量获取评论数
        Map<Long, Long> commentCounts = commentService.countCommentsByMomentIds(new HashSet<>(momentIds));

        // d. 批量获取作者信息
        Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(userIds);

        // 4. 组装 DTO
        return moments.stream().map(moment -> {
            MomentResponseDTO dto = momentMapper.toResponseDTO(moment);
            Long mid = moment.getId();

            // 设置图片
            List<String> urls = mediaMap.getOrDefault(mid, Collections.emptyList()).stream()
                    .map(MediaResponse::getFileUrl)
                    .collect(Collectors.toList());
            dto.setMediaUrls(urls);

            // 设置统计
            dto.setLikeCount(likeCounts.getOrDefault(mid, 0L).intValue());
            dto.setCommentCount(commentCounts.getOrDefault(mid, 0L).intValue());

            // 设置作者 (如果 DTO 后续添加了这些字段)
            UserResponseDTO author = userMap.get(moment.getUserId());
            if (author != null) {
                // dto.setAuthorName(author.getNickname());
                // dto.setAuthorAvatar(author.getAvatarUrl());
            }

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 创建动态 (保持原有事务一致性)
     */
    @Transactional(rollbackFor = Exception.class)
    public PetMoment createNewMoment(MomentCreateRequestDTO requestDTO) {
        PetMoment momentEntity = new PetMoment();
        momentEntity.setUserId(requestDTO.getUserId());
        momentEntity.setContent(requestDTO.getContent());
        PetMoment savedMoment = momentRepository.save(momentEntity);

        if (requestDTO.getMediaIds() != null && !requestDTO.getMediaIds().isEmpty()) {
            mediaServiceFacade.batchUpdateRelatedId(
                    requestDTO.getMediaIds(), "MOMENT", savedMoment.getId());
        }
        return savedMoment;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMoment(Long momentId) {
        Optional<PetMoment> momentOpt = momentRepository.findById(momentId);
        if (momentOpt.isPresent()) {
            commentService.deleteCommentsByMomentId(momentId);
            likeService.deleteLikesByTargetTypeAndTargetIds(TargetType.MOMENT, Set.of(momentId));
            mediaServiceFacade.deleteRelatedFiles("MOMENT", momentId);
            momentRepository.deleteById(momentId);
            return true;
        }
        return false;
    }

    public Optional<PetMoment> getMomentById(Long momentId) {
        return momentRepository.findById(momentId);
    }
}