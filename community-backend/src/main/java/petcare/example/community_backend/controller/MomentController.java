package petcare.example.community_backend.controller;

import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentCreateRequestDTO;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.service.MomentService;
import petcare.example.community_backend.mapper.MomentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/moments")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;
    private final MomentMapper momentMapper; // 注入 Mapper

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
     * 创建动态 (接收 Request DTO, 返回 Response DTO)
     * 使用 @Valid 触发 MomentCreateRequestDTO 中的校验规则
     */
    @PostMapping
    public ResponseEntity<MomentResponseDTO> createMoment(@Valid @RequestBody MomentCreateRequestDTO requestDTO) {

        try {
            // 1. 将 DTO 转换为 Entity
            PetMoment momentEntity = momentMapper.toEntity(requestDTO);

            // 2. 保存 Entity
            PetMoment savedEntity = momentService.createMoment(momentEntity);

            // 3. 将保存后的 Entity 转换回 Response DTO 返回
            MomentResponseDTO responseDTO = momentMapper.toResponseDTO(savedEntity);

            // TODO: 在这里可以填充 mediaUrls 等跨服务数据

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);

        } catch (Exception e) {
            // 建议使用 @ControllerAdvice 进行统一异常处理，这里简化处理
            System.err.println("创建动态失败: " + e.getMessage());
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
            return new ResponseEntity<>("删除成功", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("动态不存在或删除失败", HttpStatus.NOT_FOUND);
        }
    }
}