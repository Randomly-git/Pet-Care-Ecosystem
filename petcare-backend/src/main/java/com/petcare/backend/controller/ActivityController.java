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
import com.petcare.backend.service.MediaEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/activities")
@Slf4j
@Tag(name = "宠物活动管理", description = "处理宠物活动类型的定义及具体的活动打卡记录")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final MediaServiceClient mediaServiceClient;
    private final MediaEventPublisher mediaEventPublisher;

    @Operation(summary = "获取用户的活动列表", description = "根据用户ID获取其创建的活动，可选按活动种类过滤")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ActivityDTO>> getActivitiesByUserId(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "活动种类ID（可选）") @RequestParam(required = false) Long activityKindId) {

        log.info("获取用户ID: {} 的活动列表，活动种类ID: {}", userId, activityKindId);

        List<ActivityDTO> activities;
        if (activityKindId != null) {
            activities = activityService.getActivitiesByUserId(userId, activityKindId);
        } else {
            activities = activityService.getActivitiesByUserId(userId);
        }

        return ResponseEntity.ok(activities);
    }

    @Operation(summary = "获取活动详情", description = "根据活动定义ID获取详细信息")
    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityDTO> getActivityById(@Parameter(description = "活动ID") @PathVariable Long activityId) {
        log.info("获取活动ID: {} 的详情", activityId);

        Optional<ActivityDTO> activity = activityService.getActivityById(activityId);
        return activity.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "创建新活动", description = "定义一种新的宠物活动类型")
    @PostMapping
    public ResponseEntity<Activity> createActivity(@RequestBody CreateActivityDTO createActivityDTO) {
        log.info("创建新活动: {}", createActivityDTO);

        Activity createdActivity = activityService.createActivity(createActivityDTO);
        return ResponseEntity.ok(createdActivity);
    }

    @Operation(summary = "更新活动信息", description = "修改已定义的活动信息")
    @PutMapping
    public ResponseEntity<Activity> updateActivity(@RequestBody UpdateActivityDTO updateActivityDTO) {
        log.info("更新活动: {}", updateActivityDTO);

        Activity updatedActivity = activityService.updateActivity(updateActivityDTO);
        return ResponseEntity.ok(updatedActivity);
    }

    @Operation(summary = "软删除活动", description = "逻辑删除活动定义，不会立即物理删除数据")
    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(@Parameter(description = "活动ID") @PathVariable Long activityId) {
        log.info("软删除活动ID: {}", activityId);

        boolean isDeleted = activityService.deleteActivity(activityId);
        if (isDeleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "彻底删除活动", description = "物理删除活动及其所有关联的打卡记录")
    @DeleteMapping("/{activityId}/complete")
    public ResponseEntity<Void> deleteActivityCompletely(@Parameter(description = "活动ID") @PathVariable Long activityId) {
        log.info("彻底删除活动ID: {}", activityId);

        activityService.deleteActivityCompletely(activityId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "获取所有活动种类", description = "获取系统预设或自定义的所有活动大类（如：饮食、运动、医疗等）")
    @GetMapping("/kinds")
    public ResponseEntity<List<ActivityKindDTO>> getAllActivityKinds() {
        log.info("获取所有活动种类");

        List<ActivityKindDTO> activityKinds = activityService.getAllActivityKinds();
        return ResponseEntity.ok(activityKinds);
    }

    @Operation(summary = "搜索活动记录", description = "根据宠物、时间范围和种类筛选打卡记录，结果包含媒体文件链接")
    @GetMapping("/records/pet/{petId}")
    public ResponseEntity<List<ActivityRecordDTO>> searchActivityRecords(
            @Parameter(description = "宠物ID") @PathVariable Long petId,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "活动种类ID") @RequestParam(required = false) Long activityKindId) {

        log.info("搜索宠物ID: {} 的活动记录，开始时间: {}, 结束时间: {}, 活动种类ID: {}",
                petId, startDate, endDate, activityKindId);

        List<ActivityRecordDTO> records = activityService.searchActivityRecords(
                petId, startDate, endDate, activityKindId);

        if (records != null) {
            for (ActivityRecordDTO record : records) {
                try {
                    List<MediaResponse> mediaFiles = mediaServiceClient.getRelatedFiles("ACTIVITY", record.getActivityRecordId());
                    record.setMediaFiles(mediaFiles);
                } catch (Exception e) {
                    log.warn("获取活动记录 {} 的媒体文件失败: {}", record.getActivityRecordId(), e.getMessage());
                    // 媒体获取失败不影响主数据返回
                    record.setMediaFiles(List.of());
                }
            }
        }

        return ResponseEntity.ok(records);
    }

    @Operation(summary = "创建活动记录", description = "为宠物添加一次活动记录（如：今天喂食了），支持上传照片")
    @PostMapping("/records/pet/{petId}")
    public ResponseEntity<ActivityRecord> createActivityRecord(
            @Parameter(description = "宠物ID") @PathVariable Long petId,
            @Parameter(description = "活动ID") @RequestParam Long activityId,
            @Parameter(description = "描述备注") @RequestParam(required = false) String description,
            @Parameter(description = "记录日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            @Parameter(description = "上传图片/视频文件") @RequestParam(required = false) MultipartFile file,
            @Parameter(description = "执行人ID") @RequestParam Long userId) {

        log.info("创建活动记录，宠物ID: {}, 活动ID: {}, 用户ID: {}, 文件: {}",
                petId, activityId, userId, file != null ? file.getOriginalFilename() : "无");

        // 记录接口开始处理时间
        long startTime = System.currentTimeMillis();

        ActivityRecord record = activityService.createActivityRecord(petId, activityId, description, date);

        if (file != null && !file.isEmpty()) {
            try {
                // 同步上传文件，获取媒体ID
                long uploadStartTime = System.currentTimeMillis();
                MediaResponse mediaResponse = mediaServiceClient.uploadFile(file, userId, "ACTIVITY", record.getActivityRecordId());
                long uploadEndTime = System.currentTimeMillis();

                log.info("【性能监控】活动记录 {} 文件上传耗时: {}ms",
                        record.getActivityRecordId(), (uploadEndTime - uploadStartTime));

                // 发送 MQ 消息，异步更新关联（用于耗时监控演示）
                // 注意：这里 mediaId 已经通过 uploadFile 关联好了，MQ 主要是演示和监控目的
                if (mediaResponse != null && mediaResponse.getMediaId() != null) {
                    mediaEventPublisher.publishActivityMediaBindEvent(
                            record.getActivityRecordId(),
                            Collections.singletonList(mediaResponse.getMediaId()),
                            userId
                    );
                }
            } catch (Exception e) {
                log.error("活动记录 {} 的文件上传失败", record.getActivityRecordId(), e);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("【性能监控】创建活动记录总耗时: {}ms", (endTime - startTime));

        return ResponseEntity.ok(record);
    }

    @Operation(summary = "更新活动记录", description = "修改已存在的活动打卡记录，可更换图片")
    @PutMapping("/records/{recordId}")
    public ResponseEntity<ActivityRecord> updateActivityRecord(
            @Parameter(description = "记录ID") @PathVariable Long recordId,
            @Parameter(description = "新活动ID") @RequestParam(required = false) Long newActivityId,
            @Parameter(description = "描述备注") @RequestParam(required = false) String description,
            @Parameter(description = "记录日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            @Parameter(description = "更新图片文件") @RequestParam(required = false) MultipartFile file,
            @Parameter(description = "执行人ID") @RequestParam(required = false) Long userId) {

        log.info("更新活动记录ID: {}, 文件: {}", recordId, file != null ? file.getOriginalFilename() : "无");

        // 记录接口开始处理时间
        long startTime = System.currentTimeMillis();

        ActivityRecord record = activityService.updateActivityRecord(recordId, newActivityId, description, date);

        if (file != null && !file.isEmpty() && userId != null) {
            try {
                long fileOpStartTime = System.currentTimeMillis();

                // 先删除旧文件
                mediaServiceClient.deleteRelatedFiles("ACTIVITY", recordId);
                // 上传新文件
                MediaResponse mediaResponse = mediaServiceClient.uploadFile(file, userId, "ACTIVITY", recordId);

                long fileOpEndTime = System.currentTimeMillis();

                log.info("【性能监控】更新活动记录 {} 文件操作耗时: {}ms",
                        recordId, (fileOpEndTime - fileOpStartTime));

                // 发送 MQ 消息（用于耗时监控演示）
                if (mediaResponse != null && mediaResponse.getMediaId() != null) {
                    mediaEventPublisher.publishActivityMediaBindEvent(
                            recordId,
                            Collections.singletonList(mediaResponse.getMediaId()),
                            userId
                    );
                }
            } catch (Exception e) {
                log.error("活动记录 {} 的文件更新失败", recordId, e);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("【性能监控】更新活动记录总耗时: {}ms", (endTime - startTime));

        return ResponseEntity.ok(record);
    }

    @Operation(summary = "删除活动记录", description = "物理删除一条活动记录及其关联的媒体文件")
    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<Void> deleteActivityRecord(@Parameter(description = "记录ID") @PathVariable Long recordId) {
        log.info("删除活动记录ID: {}", recordId);

        try {
            mediaServiceClient.deleteRelatedFiles("ACTIVITY", recordId);
        } catch (Exception e) {
            log.error("删除活动记录 {} 的媒体失败: {}", recordId, e.getMessage());
        }

        activityService.deleteActivityRecord(recordId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "上传记录媒体", description = "专门为某条活动记录上传/补充媒体文件")
    @PostMapping("/records/{recordId}/media")
    public ResponseEntity<Void> uploadRecordMedia(
            @Parameter(description = "记录ID") @PathVariable Long recordId,
            @Parameter(description = "文件") @RequestParam MultipartFile file,
            @Parameter(description = "用户ID") @RequestParam Long userId) {

        log.info("为活动记录 {} 上传媒体文件", recordId);

        try {
            mediaServiceClient.uploadFile(file, userId, "ACTIVITY", recordId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("媒体文件上传失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "批量获取记录", description = "通过一组宠物ID批量查询它们的活动记录，常用于首页或汇总统计")
    @PostMapping("/records/batch")
    public ResponseEntity<List<ActivityRecordDTO>> getActivityRecordsByPetIds(
            @Parameter(description = "宠物ID列表") @RequestBody List<Long> petIds) {
        log.info("批量获取宠物记录: {}", petIds);

        try {
            List<ActivityRecordDTO> records = activityService.getActivityRecordsByPetIds(petIds);
            if (records != null) {
                for (ActivityRecordDTO record : records) {
                    try {
                        List<MediaResponse> mediaFiles = mediaServiceClient.getRelatedFiles("ACTIVITY", record.getActivityRecordId());
                        record.setMediaFiles(mediaFiles);
                    } catch (Exception e) {
                        log.warn("获取媒体文件失败: {}", record.getActivityRecordId());
                    }
                }
            }
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}