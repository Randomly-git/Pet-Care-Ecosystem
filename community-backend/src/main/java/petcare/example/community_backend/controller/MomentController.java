package petcare.example.community_backend.controller;

import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentCreateRequestDTO;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.service.MomentService;
import petcare.example.community_backend.repository.PetMomentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/moments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class MomentController {

    private final MomentService momentService;
    private final PetMomentRepository momentRepository;

    /**
     * GET /api/v1/moments/user/{userId}
     * 获取特定用户的动态列表 (返回 DTO)
     */
    @GetMapping("/user/{userId}")
    public List<MomentResponseDTO> getMomentsByUserId(@PathVariable Long userId) {
        return momentService.getMomentsByUserId(userId);
    }

    /**
     * POST /api/v1/moments
     * 创建动态 (接收 DTO，使用 application/json)
     * **已修改：** 改为接收 JSON 请求体，其中包含预上传的 mediaIds。
     */
    @PostMapping
    public ResponseEntity<MomentResponseDTO> createMoment(
            @Valid @RequestBody MomentCreateRequestDTO requestDTO) {

        try {
            // 1. 手动 DTO 转换为 Moment 实体
            PetMoment momentEntity = new PetMoment();
            momentEntity.setUserId(requestDTO.getUserId());
            momentEntity.setContent(requestDTO.getContent());

            // 2. 直接保存到数据库，暂时忽略mediaIds
            PetMoment savedEntity = momentRepository.save(momentEntity);

            // 3. 手动将保存后的 Entity 转换回 Response DTO 返回
            MomentResponseDTO responseDTO = new MomentResponseDTO();
            responseDTO.setId(savedEntity.getId());
            responseDTO.setUserId(savedEntity.getUserId());
            responseDTO.setContent(savedEntity.getContent());
            responseDTO.setCreatedAt(savedEntity.getCreatedAt());
            // 设置默认值
            responseDTO.setMediaUrls(new java.util.ArrayList<>());
            responseDTO.setLikeCount(0);
            responseDTO.setCommentCount(0);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);

        } catch (Exception e) {
            System.err.println("创建动态失败: " + e.getMessage());
            e.printStackTrace();
            // 如果是业务异常，这里可以返回更精确的状态码
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /api/v1/moments/{momentId}
     * 删除动态
     */
    @DeleteMapping("/{momentId}")
    public ResponseEntity<String> deleteMoment(@PathVariable Long momentId) {
        boolean deleted = momentService.deleteMoment(momentId);

        if (deleted) {
            return ResponseEntity.ok("删除成功");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("动态不存在或删除失败");
        }
    }
}