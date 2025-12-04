package petcare.example.community_backend.controller;

import petcare.example.community_backend.dto.CommentCreateRequestDTO;
import petcare.example.community_backend.dto.CommentResponseDTO;
import petcare.example.community_backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 评论 API 控制器
 */
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * POST /api/v1/comments
     * 创建评论或回复
     * @param requestDTO 评论请求 DTO
     * @return 创建成功的评论/回复的 DTO
     */
    @PostMapping
    public ResponseEntity<CommentResponseDTO> createComment(@Valid @RequestBody CommentCreateRequestDTO requestDTO) {
        try {
            CommentResponseDTO savedComment = commentService.createComment(requestDTO);
            return new ResponseEntity<>(savedComment, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            // 业务校验失败
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            System.err.println("创建评论失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    /**
     * GET /api/v1/comments/moment/{momentId}
     * 获取某动态下的所有评论 (返回包含嵌套回复的 DTO 列表)
     * @param momentId 动态ID
     * @return 评论列表 (包含嵌套回复)
     */
    @GetMapping("/moment/{momentId}")
    public List<CommentResponseDTO> getCommentsByMomentId(@PathVariable Long momentId) {
        return commentService.getCommentsByMomentId(momentId);
    }

    /**
     * DELETE /api/v1/comments/{commentId}
     * 删除指定评论 (及其所有回复和点赞)
     * @param commentId 评论ID
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable Long commentId) {
        try {
            commentService.deleteComment(commentId);
            return ResponseEntity.ok("评论删除成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("评论删除失败: " + e.getMessage());
        }
    }
}