package petcare.example.community_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.service.MomentService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@Slf4j
public class MomentAuditController {

    private final MomentService momentService;

    /**
     * 1. 获取所有待审核（PENDING）的动态
     */
    @GetMapping("/pending")
    public ResponseEntity<List<MomentResponseDTO>> getPendingMoments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<MomentResponseDTO> pendingList = momentService.getPendingMoments(PageRequest.of(page, size));
        return ResponseEntity.ok(pendingList);
    }

    /**
     * 2. 审核通过接口
     */
    @PostMapping("/{momentId}/approve")
    public ResponseEntity<Map<String, Object>> approveMoment(@PathVariable Long momentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            momentService.approveMoment(momentId);
            result.put("success", true);
            result.put("message", "审核已通过，帖子已对外界可见");
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            // 捕获 CAS 锁抛出的幂等异常
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 3. 审核拒绝接口
     */
    @PostMapping("/{momentId}/reject")
    public ResponseEntity<Map<String, Object>> rejectMoment(@PathVariable Long momentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            momentService.rejectMoment(momentId);
            result.put("success", true);
            result.put("message", "审核已拒绝");
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}