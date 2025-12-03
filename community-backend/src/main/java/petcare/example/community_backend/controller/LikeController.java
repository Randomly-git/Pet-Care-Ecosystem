package petcare.example.community_backend.controller;

import petcare.example.community_backend.dto.LikeRequestDTO;
import petcare.example.community_backend.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * POST /api/v1/likes
     * 切换点赞状态 (点赞/取消点赞)
     */
    @PostMapping
    public ResponseEntity<String> toggleLike(@Valid @RequestBody LikeRequestDTO requestDTO) {
        try {
            boolean isLiked = likeService.toggleLike(requestDTO);

            if (isLiked) {
                return ResponseEntity.ok("点赞成功");
            } else {
                return ResponseEntity.ok("取消点赞成功");
            }
        } catch (Exception e) {
            // 建议使用 @ControllerAdvice 统一处理
            return ResponseEntity.internalServerError().body("操作失败: " + e.getMessage());
        }
    }

    // 可以添加 GET /api/v1/likes/count?targetType=...&targetId=... 来单独获取点赞数
}