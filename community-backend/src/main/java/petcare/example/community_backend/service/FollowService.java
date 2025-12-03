package petcare.example.community_backend.service;

import petcare.example.community_backend.model.Follow;
import petcare.example.community_backend.dto.FollowRequestDTO;
import petcare.example.community_backend.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;

    @Transactional
    public boolean toggleFollow(FollowRequestDTO requestDTO) {
        Long followerId = requestDTO.getFollowerId();
        Long followedId = requestDTO.getFollowedId();

        // 校验：用户不能关注自己
        if (followerId.equals(followedId)) {
            throw new IllegalArgumentException("用户不能关注自己");
        }

        Optional<Follow> existingFollow = followRepository.findByFollowerIdAndFollowedId(followerId, followedId);

        if (existingFollow.isPresent()) {
            // 已关注，执行取消关注 (删除)
            followRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);
            return false; // 已取消关注
        } else {
            // 未关注，执行关注 (创建)
            Follow newFollow = new Follow();
            newFollow.setFollowerId(followerId);
            newFollow.setFollowedId(followedId);
            followRepository.save(newFollow);
            return true; // 已关注
        }
    }

    public long getFollowerCount(Long userId) {
        // 统计粉丝数
        return followRepository.countByFollowedId(userId);
    }

    public long getFollowingCount(Long userId) {
        // 统计关注数
        return followRepository.countByFollowerId(userId);
    }
}