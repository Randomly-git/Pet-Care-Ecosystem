package petcare.example.community_backend.controller;

import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentCreateRequestDTO;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.service.MomentService;
import petcare.example.community_backend.service.MediaEventPublisher;
import petcare.example.community_backend.repository.PetMomentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/moments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@Tag(name = "社区动态接口", description = "用于发布、查询和删除宠物社区动态")
public class MomentController {

    private final MomentService momentService;
    private final PetMomentRepository momentRepository;
    private final MediaEventPublisher mediaEventPublisher;

    /**
     * GET /api/v1/moments/{momentId}
     * 获取单条动态详情
     *
     * @param momentId 动态ID
     * @param userId   用户ID（可选但推荐传入，用于快速定位冷数据）
     *                 传入 userId 时：
     *                 1. 热数据：从 MySQL 直接返回
     *                 2. 冷数据：从 HBase 快速定位并返回
     *                 未传入 userId 时：
     *                 1. 热数据：从 MySQL 返回
     *                 2. 冷数据：需要额外查询定位，可能较慢
     */
    @GetMapping("/{momentId}")
    @Operation(summary = "获取动态详情", description = "根据动态ID获取单条动态详情，支持冷热数据分离存储。" +
            "推荐传入 userId 参数以提升冷数据查询性能。")
    public ResponseEntity<MomentResponseDTO> getMomentById(
            @Parameter(description = "动态ID", required = true) @PathVariable Long momentId,
            @Parameter(description = "用户ID（推荐传入，用于快速定位冷数据）", required = false)
            @RequestParam(required = false) Long userId) {

        log.debug("【API】获取动态详情: momentId={}, userId={}", momentId, userId);

        Optional<MomentResponseDTO> momentOpt = momentService.getMomentById(momentId, userId);

        if (momentOpt.isPresent()) {
            return ResponseEntity.ok(momentOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

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

        // 记录接口开始处理时间
        long startTime = System.currentTimeMillis();
        log.info("【性能监控】开始创建社区动态，用户ID: {}, mediaIds: {}",
                requestDTO.getUserId(), requestDTO.getMediaIds());

        try {
            // 统一委托给 Service 处理，确保 auditStatus 和媒体关联逻辑一致
            PetMoment momentEntity = new PetMoment();
            momentEntity.setUserId(requestDTO.getUserId());
            momentEntity.setContent(requestDTO.getContent());
            
            PetMoment saved = momentService.createMoment(momentEntity, requestDTO.getMediaIds());
            
            // 转换返回对象
            MomentResponseDTO responseDTO = momentService.getMomentById(saved.getId(), saved.getUserId())
                    .orElseThrow(() -> new RuntimeException("创建成功但查询失败"));

            long endTime = System.currentTimeMillis();
            log.info("【性能监控】创建社区动态总耗时: {}ms", (endTime - startTime));

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("创建动态失败: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/v1/moments/{momentId}
     * 修改动态内容
     */
    @PutMapping("/{momentId}")
    @Operation(summary = "修改动态", description = "修改已发布的动态内容，修改后将重新进入审核状态。")
    public ResponseEntity<MomentResponseDTO> updateMoment(
            @PathVariable Long momentId,
            @Valid @RequestBody MomentCreateRequestDTO requestDTO) {
        
        log.info("【API】修改动态请求: momentId={}, userId={}", momentId, requestDTO.getUserId());
        
        MomentResponseDTO updated = momentService.updateMoment(
                momentId, 
                requestDTO.getUserId(), 
                requestDTO.getContent(), 
                requestDTO.getMediaIds());
        
        return ResponseEntity.ok(updated);
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
        try {
            Pageable pageable = PageRequest.of(page, size);
            return momentService.getAllMomentsWithPagination(pageable);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * DELETE /api/v1/moments/{momentId}?userId={userId}
     * 删除动态（需要用户ID以验证权限并处理冷库数据）
     *
     * @param momentId 动态ID
     * @param userId   用户ID（必须传入，用于权限验证和冷库删除）
     */
    @DeleteMapping("/{momentId}")
    @Operation(summary = "删除动态", description = "根据动态ID删除指定的动态内容，需要提供用户ID进行权限验证")
    public ResponseEntity<String> deleteMoment(
            @Parameter(description = "动态ID", required = true) @PathVariable Long momentId,
            @Parameter(description = "用户ID（必须传入）", required = true) @RequestParam Long userId) {

        log.info("【API】删除动态请求: momentId={}, userId={}", momentId, userId);

        try {
            boolean deleted = momentService.deleteMoment(momentId, userId);

            if (deleted) {
                log.info("【API】删除动态成功: momentId={}, userId={}", momentId, userId);
                return ResponseEntity.ok("删除成功");
            } else {
                log.warn("【API】删除动态失败，动态不存在: momentId={}", momentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("动态不存在");
            }
        } catch (SecurityException e) {
            log.warn("【API】删除动态权限不足: momentId={}, userId={}, error={}", momentId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权删除他人的动态");
        } catch (Exception e) {
            log.error("【API】删除动态异常: momentId={}, userId={}, error={}", momentId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除失败: " + e.getMessage());
        }
    }
}