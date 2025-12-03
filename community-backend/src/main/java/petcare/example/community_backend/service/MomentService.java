package petcare.example.community_backend.service;

import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.repository.PetMomentRepository;
import petcare.example.community_backend.mapper.MomentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import petcare.example.community_backend.repository.CommentRepository; // 新增
import petcare.example.community_backend.repository.LikeRepository;    // 新增
import petcare.example.community_backend.model.TargetType;

@Service
@RequiredArgsConstructor
public class MomentService {

    private final PetMomentRepository momentRepository;
    private final MomentMapper momentMapper;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;

    /**
     * 获取特定用户ID的所有动态，并转换为 DTO 列表。
     */
    public List<MomentResponseDTO> getMomentsByUserId(Long userId) {
        List<PetMoment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<MomentResponseDTO> dtos = momentMapper.toResponseDTOList(moments);

        for (MomentResponseDTO dto : dtos) {
            Long momentId = dto.getId();

            // 1. 获取评论数
            long commentCount = commentRepository.countByMomentId(momentId);
            dto.setCommentCount((int) commentCount);

            // 2. 获取点赞数
            long likeCount = likeRepository.countByTargetTypeAndTargetId(TargetType.MOMENT, momentId);
            dto.setLikeCount((int) likeCount);
        }

        // TODO: 媒体URL的填充逻辑不变

        return dtos;
    }

    /**
     * 创建动态
     */
    public PetMoment createMoment(PetMoment moment) {
        // 业务校验已移至 Controller/DTO 的 @Valid 或 Service 逻辑
        return momentRepository.save(moment);
    }

    /**
     * 删除动态
     */
    public boolean deleteMoment(Long momentId) {
        Optional<PetMoment> moment = momentRepository.findById(momentId);
        if (moment.isPresent()) {
            momentRepository.delete(moment.get());
            return true;
        }
        return false;
    }
}