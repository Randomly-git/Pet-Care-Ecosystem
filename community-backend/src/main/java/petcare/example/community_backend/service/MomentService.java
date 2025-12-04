package petcare.example.community_backend.service;

import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.repository.PetMomentRepository;
import petcare.example.community_backend.mapper.MomentMapper;
import petcare.example.community_backend.repository.CommentRepository;
import petcare.example.community_backend.repository.LikeRepository;
import petcare.example.community_backend.model.TargetType;
import petcare.example.community_backend.client.MediaServiceFacade;
// 移除 import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MomentService {

    private final PetMomentRepository momentRepository;
    private final MomentMapper momentMapper;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final MediaServiceFacade mediaServiceFacade;

    /**
     * 获取特定用户ID的所有动态，并转换为 DTO 列表。
     * 核心：负责从多个服务（点赞、评论、媒体）聚合数据。
     */
    public List<MomentResponseDTO> getMomentsByUserId(Long userId) {
        // 1. 查询数据库获取所有动态实体
        List<PetMoment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // 2. 使用 Mapper 将实体转换为 DTO 列表
        List<MomentResponseDTO> dtos = momentMapper.toResponseDTOList(moments);

        // 3. 循环填充跨服务/聚合数据
        for (MomentResponseDTO dto : dtos) {
            Long momentId = dto.getId();

            // 3.1. 获取评论数
            long commentCount = commentRepository.countByMomentId(momentId);
            dto.setCommentCount((int) commentCount);

            // 3.2. 获取点赞数
            long likeCount = likeRepository.countByTargetTypeAndTargetId(TargetType.MOMENT, momentId);
            dto.setLikeCount((int) likeCount);

            // 3.3. 【新增逻辑】调用媒体服务获取媒体 URL 列表
            List<String> mediaUrls = mediaServiceFacade.getMediaUrlsByRelated(
                    "MOMENT", // 关联类型（字符串，与媒体服务保持一致）
                    momentId
            );
            dto.setMediaUrls(mediaUrls); // 填充媒体 URL
        }

        return dtos;
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
            // ... (评论和点赞删除逻辑)

            // 2. 【新增】删除媒体文件
            mediaServiceFacade.deleteRelatedFiles("MOMENT", momentId);

            // 3. 删除动态主体
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