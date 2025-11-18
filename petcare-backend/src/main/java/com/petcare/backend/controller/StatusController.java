package com.petcare.backend.controller;

import com.petcare.backend.dto.request.CreateStatusRecordDTO;
import com.petcare.backend.dto.request.UpdateStatusRecordDTO;
import com.petcare.backend.dto.response.StatusRecordDTO;
import com.petcare.backend.entity.Status;
import com.petcare.backend.entity.StatusRecord;
import com.petcare.backend.service.StatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/status")
@Slf4j
public class StatusController {

    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    // 1️⃣ 根据用户ID获取所有有效状态
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Status>> getValidStatusesByUserId(@PathVariable Long userId) {
        log.debug("开始获取用户ID为 {} 的状态列表", userId);

        try {
            log.debug("调用状态服务获取用户 {} 的有效状态", userId);
            List<Status> statuses = statusService.getValidStatusesByUserId(userId);

            log.debug("成功获取到用户 {} 的状态列表，共 {} 条记录", userId, statuses.size());

            if (statuses.isEmpty()) {
                log.info("用户ID为 {} 的状态列表为空", userId);
            } else {
                log.debug("状态列表详情: {}", statuses);
                // 如果Status对象较大，可以只记录关键信息
                // log.debug("状态ID列表: {}", statuses.stream().map(Status::getId).collect(Collectors.toList()));
            }

            return ResponseEntity.ok(statuses);
        } catch (Exception e) {
            log.error("获取用户ID为 {} 的状态列表失败", userId, e);
            log.debug("异常详情 - 用户ID: {}, 异常类型: {}, 异常消息: {}",
                    userId, e.getClass().getSimpleName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            log.debug("完成用户ID为 {} 的状态列表获取请求", userId);
        }
    }

    // 2️⃣ 根据状态ID软删除状态
    @DeleteMapping("/{statusId}")
    public ResponseEntity<Void> softDeleteStatus(@PathVariable Long statusId) {
        try {
            statusService.softDeleteStatus(statusId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("软删除状态ID为 {} 的状态失败", statusId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 3️⃣ 根据状态ID修改状态名称
    @PutMapping("/{statusId}/name")
    public ResponseEntity<Status> updateStatusName(@PathVariable Long statusId,
                                                   @RequestParam String newName) {
        try {
            Status updatedStatus = statusService.updateStatusName(statusId, newName);
            return ResponseEntity.ok(updatedStatus);
        } catch (Exception e) {
            log.error("更新状态ID为 {} 的名称失败", statusId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 4️⃣ 新增状态
    @PostMapping
    public ResponseEntity<Status> createStatus(@RequestParam Long userId,
                                               @RequestParam String statusName) {
        try {
            Status createdStatus = statusService.createStatus(userId, statusName);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStatus);
        } catch (Exception e) {
            log.error("为用户ID {} 创建状态 {} 失败", userId, statusName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 5️⃣ 获取某一天未结束的状态记录（基于宠物）
    @GetMapping("/records/active")
    public ResponseEntity<List<StatusRecordDTO>> getActiveStatusRecordsByPetIdAndDate(
            @RequestParam Long petId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        try {
            List<StatusRecordDTO> records = statusService.getActiveStatusRecordsByPetIdAndDate(petId, targetDate);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error("获取宠物ID为 {} 在日期 {} 的活跃状态记录失败", petId, targetDate, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 新增：5.1️⃣ 获取某个宠物的所有状态记录
    @GetMapping("/records/pet/{petId}")
    public ResponseEntity<List<StatusRecordDTO>> getAllStatusRecordsByPetId(@PathVariable Long petId) {
        try {
            List<StatusRecordDTO> records = statusService.getAllStatusRecordsByPetId(petId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error("获取宠物ID为 {} 的所有状态记录失败", petId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 6️⃣ 创建状态记录（只插入start_date，基于宠物）
    @PostMapping("/records")
    public ResponseEntity<StatusRecord> createStatusRecord(@RequestBody CreateStatusRecordDTO createStatusRecordDTO) {
        try {
            StatusRecord createdRecord = statusService.createStatusRecord(createStatusRecordDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRecord);
        } catch (Exception e) {
            log.error("创建状态记录失败: {}", createStatusRecordDTO, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 7️⃣ 停止状态记录（插入end_date）
    @PutMapping("/records/{statusRecordId}/stop")
    public ResponseEntity<StatusRecord> stopStatusRecord(@PathVariable Long statusRecordId,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            StatusRecord stoppedRecord = statusService.stopStatusRecord(statusRecordId, endDate);
            return ResponseEntity.ok(stoppedRecord);
        } catch (Exception e) {
            log.error("停止状态记录ID为 {} 失败", statusRecordId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 8️⃣ 删除状态记录
    @DeleteMapping("/records/{statusRecordId}")
    public ResponseEntity<Void> deleteStatusRecord(@PathVariable Long statusRecordId) {
        try {
            statusService.deleteStatusRecord(statusRecordId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("删除状态记录ID为 {} 失败", statusRecordId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 9️⃣ 删除状态及其所有相关记录
    @DeleteMapping("/{statusId}/with-records")
    public ResponseEntity<Void> deleteStatusAndRecords(@PathVariable Long statusId) {
        try {
            statusService.deleteStatusAndRecords(statusId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("删除状态ID为 {} 及其所有记录失败", statusId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 🔟 修改状态记录
    @PutMapping("/records")
    public ResponseEntity<StatusRecord> updateStatusRecord(@RequestBody UpdateStatusRecordDTO updateStatusRecordDTO) {
        try {
            StatusRecord updatedRecord = statusService.updateStatusRecord(updateStatusRecordDTO);
            return ResponseEntity.ok(updatedRecord);
        } catch (Exception e) {
            log.error("更新状态记录失败: {}", updateStatusRecordDTO, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}