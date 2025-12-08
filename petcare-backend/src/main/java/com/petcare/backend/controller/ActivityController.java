package com.petcare.backend.controller;

import com.petcare.backend.client.MediaServiceClient;
import com.petcare.backend.dto.response.MediaResponse;
import com.petcare.backend.dto.request.CreateActivityDTO;
import com.petcare.backend.dto.request.UpdateActivityDTO;
import com.petcare.backend.dto.response.ActivityDTO;
import com.petcare.backend.dto.response.ActivityKindDTO;
import com.petcare.backend.dto.response.ActivityRecordDTO;
import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.ActivityRecord;
import com.petcare.backend.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/activities")
@Slf4j
public class ActivityController {

    private final ActivityService activityService;
    private final MediaServiceClient mediaServiceClient;

    public ActivityController(ActivityService activityService, MediaServiceClient mediaServiceClient) {
        this.activityService = activityService;
        this.mediaServiceClient = mediaServiceClient;
    }

    /**
     * 根据用户ID和活动种类ID获取活动列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ActivityDTO>> getActivitiesByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) Long activityKindId) {

        log.info("获取用户ID: {} 的活动列表，活动种类ID: {}", userId, activityKindId);

        List<ActivityDTO> activities;
        if (activityKindId != null) {
            activities = activityService.getActivitiesByUserId(userId, activityKindId);
        } else {
            activities = activityService.getActivitiesByUserId(userId);
        }

        return ResponseEntity.ok(activities);
    }

    /**
     * 根据活动ID获取活动详情
     */
    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityDTO> getActivityById(@PathVariable Long activityId) {
        log.info("获取活动ID: {} 的详情", activityId);

        Optional<ActivityDTO> activity = activityService.getActivityById(activityId);
        return activity.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建新活动
     */
    @PostMapping
    public ResponseEntity<Activity> createActivity(@RequestBody CreateActivityDTO createActivityDTO) {
        log.info("创建新活动: {}", createActivityDTO);

        Activity createdActivity = activityService.createActivity(createActivityDTO);
        return ResponseEntity.ok(createdActivity);
    }

    /**
     * 更新活动信息
     */
    @PutMapping
    public ResponseEntity<Activity> updateActivity(@RequestBody UpdateActivityDTO updateActivityDTO) {
        log.info("更新活动: {}", updateActivityDTO);

        Activity updatedActivity = activityService.updateActivity(updateActivityDTO);
        return ResponseEntity.ok(updatedActivity);
    }

    /**
     * 软删除活动
     */
    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long activityId) {
        log.info("软删除活动ID: {}", activityId);

        boolean isDeleted = activityService.deleteActivity(activityId);
        if (isDeleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 彻底删除活动（包括相关记录）
     */
    @DeleteMapping("/{activityId}/complete")
    public ResponseEntity<Void> deleteActivityCompletely(@PathVariable Long activityId) {
        log.info("彻底删除活动ID: {}", activityId);

        activityService.deleteActivityCompletely(activityId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取所有活动种类
     */
    @GetMapping("/kinds")
    public ResponseEntity<List<ActivityKindDTO>> getAllActivityKinds() {
        log.info("获取所有活动种类");

        List<ActivityKindDTO> activityKinds = activityService.getAllActivityKinds();
        return ResponseEntity.ok(activityKinds);
    }

    /**
     * 搜索活动记录（包含媒体文件信息）
     */
    @GetMapping("/records/pet/{petId}")
    public ResponseEntity<List<ActivityRecordDTO>> searchActivityRecords(
            @PathVariable Long petId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long activityKindId) {

        log.info("搜索宠物ID: {} 的活动记录，开始时间: {}, 结束时间: {}, 活动种类ID: {}",
                petId, startDate, endDate, activityKindId);

        List<ActivityRecordDTO> records = activityService.searchActivityRecords(
                petId, startDate, endDate, activityKindId);

        // 为每个记录获取关联的媒体文件
        if (records != null) {
            for (ActivityRecordDTO record : records) {
                try {
                    List<MediaResponse> mediaFiles = mediaServiceClient.getRelatedFiles("ACTIVITY", record.getActivityRecordId());
                    record.setMediaFiles(mediaFiles);
                } catch (Exception e) {
                    log.warn("获取活动记录 {} 的媒体文件失败: {}", record.getActivityRecordId(), e.getMessage());
                    // 不抛出异常，继续处理其他记录
                }
            }
        }

        return ResponseEntity.ok(records);
    }

    /**
     * 创建活动记录（支持可选文件上传）
     * 返回类型保持不变：ResponseEntity<ActivityRecord>
     */
    @PostMapping("/records/pet/{petId}")
    public ResponseEntity<ActivityRecord> createActivityRecord(
            @PathVariable Long petId,
            @RequestParam Long activityId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam Long userId) {

        log.info("创建活动记录，宠物ID: {}, 活动ID: {}, 用户ID: {}, 文件: {}",
                petId, activityId, userId, file != null ? file.getOriginalFilename() : "无");

        // 1. 先创建活动记录
        ActivityRecord record = activityService.createActivityRecord(
                petId, activityId, description, date);

        // 2. 如果有文件，异步上传到媒体服务（不阻塞主流程）
        if (file != null && !file.isEmpty()) {
            try {
                // 使用异步方式上传文件，避免影响主流程
                new Thread(() -> {
                    try {
                        MediaResponse mediaResponse = mediaServiceClient.uploadFile(
                                file, userId, "ACTIVITY", record.getActivityRecordId());
                        log.info("活动记录 {} 的文件上传成功: {}", record.getActivityRecordId(), mediaResponse.getFileName());
                    } catch (Exception e) {
                        log.error("活动记录 {} 的文件上传失败: {}", record.getActivityRecordId(), e.getMessage());
                    }
                }).start();
            } catch (Exception e) {
                // 文件上传失败不影响活动记录的创建
                log.warn("活动记录 {} 的文件上传失败，但记录已成功创建", record.getActivityRecordId(), e);
            }
        }

        return ResponseEntity.ok(record);
    }

    /**
     * 更新活动记录（支持可选文件更新）
     * 返回类型保持不变：ResponseEntity<ActivityRecord>
     */
    @PutMapping("/records/{recordId}")
    public ResponseEntity<ActivityRecord> updateActivityRecord(
            @PathVariable Long recordId,
            @RequestParam(required = false) Long newActivityId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) Long userId) {

        log.info("更新活动记录ID: {}, 新活动ID: {}, 描述: {}, 日期: {}, 文件: {}, 用户ID: {}",
                recordId, newActivityId, description, date,
                file != null ? file.getOriginalFilename() : "无", userId);

        // 1. 先更新活动记录
        ActivityRecord record = activityService.updateActivityRecord(
                recordId, newActivityId, description, date);

        // 2. 如果有新文件上传，更新媒体文件
        if (file != null && !file.isEmpty() && userId != null) {
            try {
                // 使用异步方式处理文件更新
                new Thread(() -> {
                    try {
                        // 先删除旧的媒体文件
                        mediaServiceClient.deleteRelatedFiles("ACTIVITY", recordId);

                        // 上传新文件
                        MediaResponse mediaResponse = mediaServiceClient.uploadFile(
                                file, userId, "ACTIVITY", recordId);
                        log.info("活动记录 {} 的文件更新成功: {}", recordId, mediaResponse.getFileName());
                    } catch (Exception e) {
                        log.error("活动记录 {} 的文件更新失败: {}", recordId, e.getMessage());
                    }
                }).start();
            } catch (Exception e) {
                // 文件更新失败不影响活动记录的更新
                log.warn("活动记录 {} 的文件更新失败，但记录已成功更新", recordId, e);
            }
        }

        return ResponseEntity.ok(record);
    }

    /**
     * 删除活动记录（同时删除关联的媒体文件）
     * 返回类型保持不变：ResponseEntity<Void>
     */
    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<Void> deleteActivityRecord(@PathVariable Long recordId) {
        log.info("删除活动记录ID: {}", recordId);

        // 1. 先删除关联的媒体文件（同步执行，确保媒体文件被删除）
        try {
            mediaServiceClient.deleteRelatedFiles("ACTIVITY", recordId);
            log.info("活动记录 {} 的关联媒体文件已删除", recordId);
        } catch (Exception e) {
            log.error("删除活动记录 {} 的关联媒体文件失败: {}", recordId, e.getMessage());
            // 即使媒体删除失败，也继续删除活动记录
        }

        // 2. 删除活动记录
        activityService.deleteActivityRecord(recordId);

        return ResponseEntity.ok().build();
    }

    /**
     * 为活动记录上传媒体文件（新增独立接口）
     * 这样不破坏原有接口的返回格式
     */
    @PostMapping("/records/{recordId}/media")
    public ResponseEntity<Void> uploadRecordMedia(
            @PathVariable Long recordId,
            @RequestParam MultipartFile file,
            @RequestParam Long userId) {

        log.info("为活动记录 {} 上传媒体文件: {}, 用户ID: {}",
                recordId, file.getOriginalFilename(), userId);

        try {
            // 上传文件到媒体服务
            MediaResponse mediaResponse = mediaServiceClient.uploadFile(
                    file, userId, "ACTIVITY", recordId);
            log.info("活动记录 {} 的媒体文件上传成功: {}", recordId, mediaResponse.getFileName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("活动记录 {} 的媒体文件上传失败", recordId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量获取活动记录（用于统计）
     */
    @PostMapping("/records/batch")
    public ResponseEntity<List<ActivityRecordDTO>> getActivityRecordsByPetIds(@RequestBody List<Long> petIds) {
        log.info("批量获取宠物ID: {} 的活动记录", petIds);

        try {
            List<ActivityRecordDTO> records = activityService.getActivityRecordsByPetIds(petIds);

            // 为每个记录获取关联的媒体文件
            if (records != null) {
                for (ActivityRecordDTO record : records) {
                    try {
                        List<MediaResponse> mediaFiles = mediaServiceClient.getRelatedFiles("ACTIVITY", record.getActivityRecordId());
                        record.setMediaFiles(mediaFiles);
                    } catch (Exception e) {
                        log.warn("获取活动记录 {} 的媒体文件失败: {}", record.getActivityRecordId(), e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error("批量获取活动记录失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}