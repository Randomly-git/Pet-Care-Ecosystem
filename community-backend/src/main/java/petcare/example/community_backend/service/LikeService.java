package petcare.example.community_backend.service;

import petcare.example.community_backend.model.Like;
import petcare.example.community_backend.model.TargetType;
import petcare.example.community_backend.dto.LikeRequestDTO;
import petcare.example.community_backend.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;

    @Transactional
    public boolean toggleLike(LikeRequestDTO requestDTO) {
        Long userId = requestDTO.getUserId();
        TargetType targetType = requestDTO.getTargetType();
        Long targetId = requestDTO.getTargetId();

        Optional<Like> existingLike = likeRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);

        if (existingLike.isPresent()) {
            // 已点赞，执行取消点赞 (删除)
            likeRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
            return false; // 返回 false 表示已取消
        } else {
            // 未点赞，执行点赞 (创建)
            Like newLike = new Like();
            newLike.setUserId(userId);
            newLike.setTargetType(targetType);
            newLike.setTargetId(targetId);
            likeRepository.save(newLike);
            return true; // 返回 true 表示已点赞
        }
    }

    /**
     * 批量统计指定目标类型和目标ID集合的点赞数。
     * @param targetType 目标类型 (MOMENT 或 COMMENT)
     * @param targetIds 目标ID集合
     * @return Map<TargetId, Count>
     */
    public Map<Long, Long> countLikesByTargetIds(TargetType targetType, Collection<Long> targetIds) {
        // 调用 Repository 批量查询，并将 List<Object[]> 结果转换为 Map<Long, Long>
        return likeRepository.countLikesByTargetTypeAndTargetIdIn(targetType, targetIds).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],  // TargetId 是 Long 类型
                        arr -> (Long) arr[1]   // Count 是 Long 类型
                ));
    }

    /**
     * 批量删除指定目标类型和目标ID集合的点赞记录 (用于级联删除)
     * @param targetType 目标类型 (MOMENT 或 COMMENT)
     * @param targetIds 目标ID集合
     */
    @Transactional
    public void deleteLikesByTargetTypeAndTargetIds(TargetType targetType, Collection<Long> targetIds) {
        if (targetIds.isEmpty()) return;
        likeRepository.deleteByTargetTypeAndTargetIdIn(targetType, targetIds);
    }
}