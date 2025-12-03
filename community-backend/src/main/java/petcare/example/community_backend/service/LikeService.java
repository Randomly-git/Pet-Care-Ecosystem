package petcare.example.community_backend.service;

import petcare.example.community_backend.model.Like;
import petcare.example.community_backend.model.TargetType;
import petcare.example.community_backend.dto.LikeRequestDTO;
import petcare.example.community_backend.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

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
}