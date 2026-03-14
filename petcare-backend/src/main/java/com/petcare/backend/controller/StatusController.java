package com.petcare.backend.controller;

import com.petcare.backend.client.MediaServiceClient;
import com.petcare.backend.dto.response.MediaResponse;
import com.petcare.backend.dto.request.CreateStatusRecordDTO;
import com.petcare.backend.dto.request.UpdateStatusRecordDTO;
import com.petcare.backend.dto.response.StatusRecordDTO;
import com.petcare.backend.entity.Status;
import com.petcare.backend.entity.StatusRecord;
import com.petcare.backend.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/status")
@Slf4j
@Tag(name = "宠物状态管理", description = "处理宠物长期状态（如生病、怀孕、发情等）的定义及记录维护")
public class StatusController {

    private final StatusService statusService;
    private final MediaServiceClient mediaServiceClient;

    public StatusController(StatusService statusService, MediaServiceClient mediaServiceClient) {
        this.statusService = statusService;
        this.mediaServiceClient = mediaServiceClient;
    }

    @Operation(summary = "获取宠物的有效状态列表", description = "获取该宠物定义的所有未被软删除的状态类型")
    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<Status>> getValidStatusesByPetId(
            @Parameter(description = "宠物ID") @PathVariable Long petId) {
        log.debug("开始获取宠物ID为 {} 的状态列表", petId);

        try {
            List<Status> statuses = statusService.getValidStatusesByPetId(petId);
            return ResponseEntity.ok(statuses);
        } catch (Exception e) {
            log.error("获取宠物ID为 {} 的状态列表失败", petId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "软删除状态", description = "逻辑删除某种状态类型")
    @DeleteMapping("/{statusId}")
    public ResponseEntity<Void> softDeleteStatus(
            @Parameter(description = "状态ID") @PathVariable Long statusId) {
        try {
            statusService.softDeleteStatus(statusId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("软删除状态ID为 {} 的状态失败", statusId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "修改状态名称", description = "更新已有状态类型的显示名称")
    @PutMapping("/{statusId}/name")
    public ResponseEntity<Status> updateStatusName(
            @Parameter(description = "状态ID") @PathVariable Long statusId,
            @Parameter(description = "新的状态名称") @RequestParam String newName) {
        try {
            Status updatedStatus = statusService.updateStatusName(statusId, newName);
            return ResponseEntity.ok(updatedStatus);
        } catch (Exception e) {
            log.error("更新状态ID为 {} 的名称失败", statusId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "更新状态当前值", description = "更新状态的当前值（如：健康、生病中、怀孕中等）")
    @PutMapping("/{statusId}/value")
    public ResponseEntity<Status> updateStatusValue(
            @Parameter(description = "状态ID") @PathVariable Long statusId,
            @Parameter(description = "新的状态值") @RequestParam String statusValue) {
        try {
            Status updatedStatus = statusService.updateStatusValue(statusId, statusValue);
            return ResponseEntity.ok(updatedStatus);
        } catch (Exception e) {
            log.error("更新状态ID为 {} 的状态值失败", statusId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "新增状态类型", description = "为宠物创建一种新的宠物状态（如：过敏中）")
    @PostMapping
    public ResponseEntity<Status> createStatus(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "状态名称") @RequestParam String statusName) {
        try {
            Status createdStatus = statusService.createStatus(petId, statusName);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStatus);
        } catch (Exception e) {
            log.error("为宠物ID {} 创建状态 {} 失败", petId, statusName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "获取指定日期的活跃记录", description = "获取某宠物在特定日期处于“未结束”状态的所有记录")
    @GetMapping("/records/active")
    public ResponseEntity<List<StatusRecordDTO>> getActiveStatusRecordsByPetIdAndDate(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "目标日期 (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        try {
            List<StatusRecordDTO> records = statusService.getActiveStatusRecordsByPetIdAndDate(petId, targetDate);

            if (records != null) {
                for (StatusRecordDTO record : records) {
                    try {
                        List<MediaResponse> mediaFiles = mediaServiceClient.getRelatedFiles("STATUS", record.getStatusRecordId());
                        record.setMediaFiles(mediaFiles);
                    } catch (Exception e) {
                        log.warn("获取状态记录 {} 的媒体文件失败", record.getStatusRecordId());
                        record.setMediaFiles(List.of());
                    }
                }
            }

            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error("获取记录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "获取宠物所有状态记录", description = "获取某宠物的历史及当前所有状态记录，包含图片")
    @GetMapping("/records/pet/{petId}")
    public ResponseEntity<List<StatusRecordDTO>> getAllStatusRecordsByPetId(
            @Parameter(description = "宠物ID") @PathVariable Long petId) {
        try {
            List<StatusRecordDTO> records = statusService.getAllStatusRecordsByPetId(petId);

            if (records != null) {
                for (StatusRecordDTO record : records) {
                    try {
                        List<MediaResponse> mediaFiles = mediaServiceClient.getRelatedFiles("STATUS", record.getStatusRecordId());
                        record.setMediaFiles(mediaFiles);
                    } catch (Exception e) {
                        log.warn("获取媒体失败", record.getStatusRecordId());
                        record.setMediaFiles(List.of());
                    }
                }
            }

            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "开始一项状态记录", description = "为宠物开启一段状态计时，支持上传证明图片")
    @PostMapping(value = "/records", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StatusRecord> createStatusRecord(
            @Parameter(description = "状态类型ID") @RequestParam Long statusId,
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "描述备注") @RequestParam(required = false) String description,
            @Parameter(description = "开始日期 (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "媒体文件", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) @RequestParam(required = false) MultipartFile file,
            @Parameter(description = "操作人ID") @RequestParam Long userId) {

        log.info("创建状态记录，状态ID: {}, 宠物ID: {}", statusId, petId);

        try {
            CreateStatusRecordDTO createStatusRecordDTO = new CreateStatusRecordDTO();
            createStatusRecordDTO.setStatusId(statusId);
            createStatusRecordDTO.setPetId(petId);
            createStatusRecordDTO.setStatusDescription(description);
            createStatusRecordDTO.setStartDate(startDate);

            StatusRecord createdRecord = statusService.createStatusRecord(createStatusRecordDTO);

            if (file != null && !file.isEmpty()) {
                new Thread(() -> {
                    try {
                        mediaServiceClient.uploadFile(file, userId, "STATUS", createdRecord.getStatusRecordId());
                    } catch (Exception e) {
                        log.error("文件上传失败", e);
                    }
                }).start();
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(createdRecord);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "停止状态记录", description = "为正在进行的状态记录设置结束日期")
    @PutMapping("/records/{statusRecordId}/stop")
    public ResponseEntity<StatusRecord> stopStatusRecord(
            @Parameter(description = "记录ID") @PathVariable Long statusRecordId,
            @Parameter(description = "结束日期 (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            StatusRecord stoppedRecord = statusService.stopStatusRecord(statusRecordId, endDate);
            return ResponseEntity.ok(stoppedRecord);
        } catch (Exception e) {
            log.error("停止记录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "删除状态记录", description = "删除单条状态记录及其关联的所有媒体文件")
    @DeleteMapping("/records/{statusRecordId}")
    public ResponseEntity<Void> deleteStatusRecord(
            @Parameter(description = "记录ID") @PathVariable Long statusRecordId) {
        log.info("删除状态记录ID: {}", statusRecordId);

        try {
            mediaServiceClient.deleteRelatedFiles("STATUS", statusRecordId);
        } catch (Exception e) {
            log.error("媒体删除失败", e);
        }

        statusService.deleteStatusRecord(statusRecordId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "彻底删除状态分类及记录", description = "物理删除整个状态分类，以及该分类下所有的历史记录和媒体文件")
    @DeleteMapping("/{statusId}/with-records")
    public ResponseEntity<Void> deleteStatusAndRecords(
            @Parameter(description = "状态ID") @PathVariable Long statusId) {
        try {
            List<StatusRecordDTO> records = statusService.getStatusRecordsByStatusId(statusId);
            if (records != null) {
                for (StatusRecordDTO record : records) {
                    try {
                        mediaServiceClient.deleteRelatedFiles("STATUS", record.getStatusRecordId());
                    } catch (Exception e) {
                        log.warn("批量删除媒体失败");
                    }
                }
            }

            statusService.deleteStatusAndRecords(statusId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "修改状态记录详情", description = "更新记录的描述、起止日期或更换图片")
    @PutMapping(value = "/records/{statusRecordId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StatusRecord> updateStatusRecord(
            @Parameter(description = "记录ID") @PathVariable Long statusRecordId,
            @Parameter(description = "新描述") @RequestParam(required = false) String description,
            @Parameter(description = "新开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "新结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "更新图片文件") @RequestParam(required = false) MultipartFile file,
            @Parameter(description = "操作人ID") @RequestParam(required = false) Long userId) {

        try {
            UpdateStatusRecordDTO updateDTO = new UpdateStatusRecordDTO();
            updateDTO.setStatusRecordId(statusRecordId);
            updateDTO.setStatusDescription(description);
            updateDTO.setStartDate(startDate);
            updateDTO.setEndDate(endDate);

            StatusRecord updatedRecord = statusService.updateStatusRecord(updateDTO);

            if (file != null && !file.isEmpty() && userId != null) {
                new Thread(() -> {
                    try {
                        mediaServiceClient.deleteRelatedFiles("STATUS", statusRecordId);
                        mediaServiceClient.uploadFile(file, userId, "STATUS", statusRecordId);
                    } catch (Exception e) {
                        log.error("更新文件失败", e);
                    }
                }).start();
            }

            return ResponseEntity.ok(updatedRecord);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "为状态记录补充媒体文件", description = "独立上传接口，不影响现有记录数据")
    @PostMapping(value = "/records/{statusRecordId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadStatusRecordMedia(
            @Parameter(description = "记录ID") @PathVariable Long statusRecordId,
            @Parameter(description = "文件") @RequestParam MultipartFile file,
            @Parameter(description = "用户ID") @RequestParam Long userId) {

        try {
            mediaServiceClient.uploadFile(file, userId, "STATUS", statusRecordId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}