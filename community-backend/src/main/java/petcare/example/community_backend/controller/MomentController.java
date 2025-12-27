package petcare.example.community_backend.controller;

import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentCreateRequestDTO;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.service.MomentService;
import petcare.example.community_backend.mapper.MomentMapper; // 确保导入了 Mapper
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/moments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MomentController {

    private final MomentService momentService;
    private final MomentMapper momentMapper; // 必须注入 Mapper

    @GetMapping("/user/{userId}")
    public List<MomentResponseDTO> getMomentsByUserId(@PathVariable Long userId) {
        return momentService.getMomentsByUserId(userId);
    }

    @PostMapping
    public ResponseEntity<MomentResponseDTO> createMoment(@Valid @RequestBody MomentCreateRequestDTO requestDTO) {
        try {
            // 1. 创建动态并关联媒体
            PetMoment savedMoment = momentService.createNewMoment(requestDTO);

            // 2. 转换并手动填充 mediaUrls (因为刚刚关联成功，数据库里已经有了)
            // 或者直接从 requestDTO.getMediaIds 获取（如果你能拿到 URL 的话）
            // 最稳妥的方法是重新查一次该动态的 DTO，这样数据最准确
            MomentResponseDTO responseDTO = momentMapper.toResponseDTO(savedMoment);

            // 暂时由于是刚创建，如果是预上传，mediaUrls 可以根据业务逻辑填充或让前端重新刷新
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("创建动态失败", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{momentId}")
    public ResponseEntity<String> deleteMoment(@PathVariable Long momentId) {
        boolean deleted = momentService.deleteMoment(momentId);
        return deleted ? ResponseEntity.ok("删除成功") : ResponseEntity.notFound().build();
    }
}