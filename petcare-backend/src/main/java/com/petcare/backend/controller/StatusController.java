package com.petcare.backend.controller;

import com.petcare.backend.client.MediaServiceClient;
import com.petcare.backend.dto.response.MediaResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/status")
@Slf4j
public class StatusController {

    private final StatusService statusService;
    private final MediaServiceClient mediaServiceClient;

    public StatusController(StatusService statusService, MediaServiceClient mediaServiceClient) {
        this.statusService = statusService;
        this.mediaServiceClient = mediaServiceClient;
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

            // 为每个记录获取关联的媒体文件
            if (records != null) {
                for (StatusRecordDTO record : records) {
                    try {
                        List<MediaResponse> mediaFiles = mediaServiceClient.getRelatedFiles("STATUS", record.getStatusRecordId());
                        // 如果需要，可以将媒体文件信息设置到DTO中
                        record.setMediaFiles(mediaFiles);
                    } catch (Exception e) {
                        log.warn("获取状态记录 {} 的媒体文件失败: {}", record.getStatusRecordId(), e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error("获取宠物ID为 {} 在日期 {} 的活跃状态记录失败", petId, targetDate, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 新增：5.1️⃣ 获取某个宠物的所有状态记录（带媒体文件信息）
    @GetMapping("/records/pet/{petId}")
    public ResponseEntity<List<StatusRecordDTO>> getAllStatusRecordsByPetId(@PathVariable Long petId) {
        try {
            List<StatusRecordDTO> records = statusService.getAllStatusRecordsByPetId(petId);

            // 为每个记录获取关联的媒体文件
            if (records != null) {
                for (StatusRecordDTO record : records) {
                    try {
                        List<MediaResponse> mediaFiles = mediaServiceClient.getRelatedFiles("STATUS", record.getStatusRecordId());
                        // 如果需要，可以将媒体文件信息设置到DTO中
                        record.setMediaFiles(mediaFiles);
                    } catch (Exception e) {
                        log.warn("获取状态记录 {} 的媒体文件失败: {}", record.getStatusRecordId(), e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error("获取宠物ID为 {} 的所有状态记录失败", petId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 6️⃣ 创建状态记录（支持文件上传）
    @PostMapping("/records")
    public ResponseEntity<StatusRecord> createStatusRecord(
            @RequestParam Long statusId,
            @RequestParam Long petId,
            @RequestParam(required = false) String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam Long userId) {

        log.info("创建状态记录，状态ID: {}, 宠物ID: {}, 用户ID: {}, 文件: {}",
                statusId, petId, userId, file != null ? file.getOriginalFilename() : "无");

        try {
            // 1. 创建DTO对象
            CreateStatusRecordDTO createStatusRecordDTO = new CreateStatusRecordDTO();
            createStatusRecordDTO.setStatusId(statusId);
            createStatusRecordDTO.setPetId(petId);
            createStatusRecordDTO.setStatusDescription(description);
            createStatusRecordDTO.setStartDate(startDate);

            // 2. 创建状态记录
            StatusRecord createdRecord = statusService.createStatusRecord(createStatusRecordDTO);

            // 3. 如果有文件，异步上传到媒体服务
            if (file != null && !file.isEmpty()) {
                new Thread(() -> {
                    try {
                        MediaResponse mediaResponse = mediaServiceClient.uploadFile(
                                file, userId, "STATUS", createdRecord.getStatusRecordId());
                        log.info("状态记录 {} 的文件上传成功: {}", createdRecord.getStatusRecordId(), mediaResponse.getFileName());
                    } catch (Exception e) {
                        log.error("状态记录 {} 的文件上传失败: {}", createdRecord.getStatusRecordId(), e.getMessage());
                    }
                }).start();
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(createdRecord);
        } catch (Exception e) {
            log.error("创建状态记录失败: statusId={}, petId={}, userId={}", statusId, petId, userId, e);
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

    // 8️⃣ 删除状态记录（同时删除关联的媒体文件）
    @DeleteMapping("/records/{statusRecordId}")
    public ResponseEntity<Void> deleteStatusRecord(@PathVariable Long statusRecordId) {
        log.info("删除状态记录ID: {}", statusRecordId);

        try {
            // 1. 先删除关联的媒体文件
            mediaServiceClient.deleteRelatedFiles("STATUS", statusRecordId);
            log.info("状态记录 {} 的关联媒体文件已删除", statusRecordId);
        } catch (Exception e) {
            log.error("删除状态记录 {} 的关联媒体文件失败: {}", statusRecordId, e.getMessage());
            // 即使媒体删除失败，也继续删除状态记录
        }

        // 2. 删除状态记录
        statusService.deleteStatusRecord(statusRecordId);

        return ResponseEntity.noContent().build();
    }

    // 9️⃣ 删除状态及其所有相关记录
    @DeleteMapping("/{statusId}/with-records")
    public ResponseEntity<Void> deleteStatusAndRecords(@PathVariable Long statusId) {
        try {
            // 先获取该状态的所有记录，删除关联的媒体文件
            List<StatusRecordDTO> records = statusService.getStatusRecordsByStatusId(statusId);
            if (records != null) {
                for (StatusRecordDTO record : records) {
                    try {
                        mediaServiceClient.deleteRelatedFiles("STATUS", record.getStatusRecordId());
                    } catch (Exception e) {
                        log.warn("删除状态记录 {} 的媒体文件失败，继续处理", record.getStatusRecordId(), e);
                    }
                }
            }

            // 删除状态及其记录
            statusService.deleteStatusAndRecords(statusId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("删除状态ID为 {} 及其所有记录失败", statusId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 🔟 修改状态记录（支持文件更新）
    @PutMapping("/records/{statusRecordId}")
    public ResponseEntity<StatusRecord> updateStatusRecord(
            @PathVariable Long statusRecordId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) Long userId) {

        log.info("更新状态记录ID: {}, 描述: {}, 开始日期: {}, 结束日期: {}, 文件: {}, 用户ID: {}",
                statusRecordId, description, startDate, endDate,
                file != null ? file.getOriginalFilename() : "无", userId);

        try {
            // 1. 创建DTO对象
            UpdateStatusRecordDTO updateDTO = new UpdateStatusRecordDTO();
            updateDTO.setStatusRecordId(statusRecordId);
            updateDTO.setStatusDescription(description);
            updateDTO.setStartDate(startDate);
            updateDTO.setEndDate(endDate);

            // 2. 更新状态记录
            StatusRecord updatedRecord = statusService.updateStatusRecord(updateDTO);

            // 3. 如果有新文件上传，更新媒体文件
            if (file != null && !file.isEmpty() && userId != null) {
                new Thread(() -> {
                    try {
                        // 先删除旧的媒体文件
                        mediaServiceClient.deleteRelatedFiles("STATUS", statusRecordId);

                        // 上传新文件
                        MediaResponse mediaResponse = mediaServiceClient.uploadFile(
                                file, userId, "STATUS", statusRecordId);
                        log.info("状态记录 {} 的文件更新成功: {}", statusRecordId, mediaResponse.getFileName());
                    } catch (Exception e) {
                        log.error("状态记录 {} 的文件更新失败: {}", statusRecordId, e.getMessage());
                    }
                }).start();
            }

            return ResponseEntity.ok(updatedRecord);
        } catch (Exception e) {
            log.error("更新状态记录失败: {}", statusRecordId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 🔟+1️⃣ 为状态记录上传媒体文件（新增独立接口）
    @PostMapping("/records/{statusRecordId}/media")
    public ResponseEntity<Void> uploadStatusRecordMedia(
            @PathVariable Long statusRecordId,
            @RequestParam MultipartFile file,
            @RequestParam Long userId) {

        log.info("为状态记录 {} 上传媒体文件: {}, 用户ID: {}",
                statusRecordId, file.getOriginalFilename(), userId);

        try {
            MediaResponse mediaResponse = mediaServiceClient.uploadFile(
                    file, userId, "STATUS", statusRecordId);
            log.info("状态记录 {} 的媒体文件上传成功: {}", statusRecordId, mediaResponse.getFileName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("状态记录 {} 的媒体文件上传失败", statusRecordId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}