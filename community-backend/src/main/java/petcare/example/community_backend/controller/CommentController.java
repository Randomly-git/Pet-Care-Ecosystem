package petcare.example.community_backend.controller;

import petcare.example.community_backend.dto.CommentCreateRequestDTO;
import petcare.example.community_backend.dto.CommentResponseDTO;
import petcare.example.community_backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@CrossOrigin(origins = "*", maxAge = 3600, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@Tag(name = "社区评论接口", description = "用于对动态进行评论、回复以及删除评论")
public class CommentController {

    private final CommentService commentService;

    /**
     * POST /api/v1/comments
     * 创建评论或回复
     */
    @PostMapping
    @Operation(summary = "发表评论/回复", description = "对动态进行评论或对已有的评论进行二级回复")
    public ResponseEntity<CommentResponseDTO> createComment(
            @Valid @RequestBody CommentCreateRequestDTO requestDTO) {
        try {
            CommentResponseDTO savedComment = commentService.createComment(requestDTO);
            return new ResponseEntity<>(savedComment, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            System.err.println("创建评论失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    /**
     * GET /api/v1/comments/moment/{momentId}
     * 获取某动态下的所有评论
     */
    @GetMapping("/moment/{momentId}")
    @Operation(summary = "获取动态下的评论", description = "根据动态ID获取其下所有包含嵌套回复的评论列表")
    public List<CommentResponseDTO> getCommentsByMomentId(
            @Parameter(description = "动态ID", required = true) @PathVariable Long momentId) {
        return commentService.getCommentsByMomentId(momentId);
    }

    /**
     * DELETE /api/v1/comments/{commentId}
     * 删除指定评论
     */
    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论", description = "删除指定评论及其关联的所有回复")
    public ResponseEntity<String> deleteComment(
            @Parameter(description = "评论ID", required = true) @PathVariable Long commentId) {
        try {
            commentService.deleteComment(commentId);
            return ResponseEntity.ok("评论删除成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("评论删除失败: " + e.getMessage());
        }
    }
}