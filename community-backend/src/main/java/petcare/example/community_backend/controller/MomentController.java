package petcare.example.community_backend.controller;

import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentCreateRequestDTO;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.service.MomentService;
import petcare.example.community_backend.repository.PetMomentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/moments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@Tag(name = "社区动态接口", description = "用于发布、查询和删除宠物社区动态")
public class MomentController {

    private final MomentService momentService;
    private final PetMomentRepository momentRepository;

    /**
     * GET /api/v1/moments/user/{userId}
     * 获取特定用户的动态列表
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户动态", description = "根据用户ID获取该用户发布的所有动态记录")
    public List<MomentResponseDTO> getMomentsByUserId(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId) {
        return momentService.getMomentsByUserId(userId);
    }

    /**
     * POST /api/v1/moments
     * 创建动态
     */
    @PostMapping
    @Operation(summary = "发布新动态", description = "接收JSON请求体创建一条新的动态")
    public ResponseEntity<MomentResponseDTO> createMoment(
            @Valid @RequestBody MomentCreateRequestDTO requestDTO) {

        try {
            // 1. 手动 DTO 转换为 Moment 实体
            PetMoment momentEntity = new PetMoment();
            momentEntity.setUserId(requestDTO.getUserId());
            momentEntity.setContent(requestDTO.getContent());

            // 2. 直接保存到数据库
            PetMoment savedEntity = momentRepository.save(momentEntity);

            // 3. 将保存后的 Entity 转换回 Response DTO 返回
            MomentResponseDTO responseDTO = new MomentResponseDTO();
            responseDTO.setId(savedEntity.getId());
            responseDTO.setUserId(savedEntity.getUserId());
            responseDTO.setContent(savedEntity.getContent());
            responseDTO.setCreatedAt(savedEntity.getCreatedAt());
            responseDTO.setMediaUrls(new java.util.ArrayList<>());
            responseDTO.setLikeCount(0);
            responseDTO.setCommentCount(0);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);

        } catch (Exception e) {
            System.err.println("创建动态失败: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/moments/all
     * 获取所有用户的动态列表，支持分页
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有用户动态", description = "获取所有用户发布的动态记录，按时间倒序排列，支持分页")
    public List<MomentResponseDTO> getAllMoments(
            @Parameter(description = "页码，从0开始", required = false) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", required = false) @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return momentService.getAllMomentsWithPagination(pageable);
    }

    /**
     * DELETE /api/v1/moments/{momentId}
     * 删除动态
     */
    @DeleteMapping("/{momentId}")
    @Operation(summary = "删除动态", description = "根据动态ID删除指定的动态内容")
    public ResponseEntity<String> deleteMoment(
            @Parameter(description = "动态ID", required = true) @PathVariable Long momentId) {
        boolean deleted = momentService.deleteMoment(momentId);

        if (deleted) {
            return ResponseEntity.ok("删除成功");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("动态不存在或删除失败");
        }
    }
}