package petcare.example.community_backend.service;

import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.repository.PetMomentRepository;
import petcare.example.community_backend.mapper.MomentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MomentService {

    private final PetMomentRepository momentRepository;
    private final MomentMapper momentMapper;

    /**
     * 获取特定用户ID的所有动态，并转换为 DTO 列表。
     */
    public List<MomentResponseDTO> getMomentsByUserId(Long userId) {
        List<PetMoment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // 直接返回 Mapper 转换结果
        // TODO: 在这里是微服务组合的关键！
        //    应该调用 Media Service 和 Comment Service 的 API，
        //    获取 mediaUrls, commentCount, likeCount，并填充到 DTOs 中。
        return momentMapper.toResponseDTOList(moments);
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