package petcare.example.community_backend.controller;

import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.dto.CommentCreateRequestDTO;
import petcare.example.community_backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * POST /api/v1/comments
     * 创建评论或回复
     */
    @PostMapping
    public ResponseEntity<Comment> createComment(@Valid @RequestBody CommentCreateRequestDTO requestDTO) {
        try {
            Comment savedComment = commentService.createComment(requestDTO);
            return new ResponseEntity<>(savedComment, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    /**
     * GET /api/v1/comments/moment/{momentId}
     * 获取某动态下的所有评论
     */
    @GetMapping("/moment/{momentId}")
    public List<Comment> getCommentsByMomentId(@PathVariable Long momentId) {
        return commentService.getCommentsByMomentId(momentId);
    }
}