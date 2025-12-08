package petcare.example.community_backend.controller;

import petcare.example.community_backend.dto.FollowRequestDTO;
import petcare.example.community_backend.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class FollowController {

    private final FollowService followService;

    /**
     * POST /api/v1/follows
     * API: 关注/取消关注作者
     * Body: { "followerId": 123, "followedId": 456 }
     */
    @PostMapping
    public ResponseEntity<String> toggleFollow(@Valid @RequestBody FollowRequestDTO requestDTO) {
        try {
            boolean isFollowing = followService.toggleFollow(requestDTO);

            if (isFollowing) {
                return ResponseEntity.ok("关注成功");
            } else {
                return ResponseEntity.ok("取消关注成功");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("操作失败");
        }
    }

    /**
     * GET /api/v1/follows/followers/count/{userId}
     * API: 获取用户的粉丝数
     */
    @GetMapping("/followers/count/{userId}")
    public ResponseEntity<Long> getFollowerCount(@PathVariable Long userId) {
        long count = followService.getFollowerCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * GET /api/v1/follows/following/count/{userId}
     * API: 获取用户的关注数
     */
    @GetMapping("/following/count/{userId}")
    public ResponseEntity<Long> getFollowingCount(@PathVariable Long userId) {
        long count = followService.getFollowingCount(userId);
        return ResponseEntity.ok(count);
    }
}