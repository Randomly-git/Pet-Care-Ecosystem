package petcare.example.community_backend.service;

import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.dto.CommentCreateRequestDTO;
// 您可能还需要一个 CommentResponseDTO 来返回更丰富的数据
import petcare.example.community_backend.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    /**
     * 创建评论
     */
    public Comment createComment(CommentCreateRequestDTO requestDTO) {
        // TODO: 校验 momentId 和 parentId (如果存在) 是否有效

        Comment comment = new Comment();
        comment.setUserId(requestDTO.getUserId());
        comment.setMomentId(requestDTO.getMomentId());
        comment.setContent(requestDTO.getContent());
        comment.setParentId(requestDTO.getParentId()); // 处理回复逻辑

        return commentRepository.save(comment);
    }

    /**
     * 获取某一动态下的所有评论 (注意：返回 List<Comment>，如果需要前端友好，应创建 CommentResponseDTO)
     */
    public List<Comment> getCommentsByMomentId(Long momentId) {
        // 按创建时间升序，方便前端构建层级结构
        return commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId);
    }
}