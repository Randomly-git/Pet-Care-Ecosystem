package com.petcare.backend.controller;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/activities")
@Slf4j
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
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
     * 搜索活动记录
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

        return ResponseEntity.ok(records);
    }

    /**
     * 创建活动记录
     */
    @PostMapping("/records/pet/{petId}")
    public ResponseEntity<ActivityRecord> createActivityRecord(
            @PathVariable Long petId,
            @RequestParam Long activityId,
            @RequestParam(required = false) String description, // 修改：允许描述为空
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {

        log.info("为宠物ID: {} 创建活动记录，活动ID: {}, 描述: {}, 日期: {}",
                petId, activityId, description, date);

        ActivityRecord record = activityService.createActivityRecord(petId, activityId, description, date);
        return ResponseEntity.ok(record);
    }

    /**
     * 更新活动记录
     */
    @PutMapping("/records/{recordId}")
    public ResponseEntity<ActivityRecord> updateActivityRecord(
            @PathVariable Long recordId,
            @RequestParam(required = false) Long newActivityId, // 修改：允许参数为空
            @RequestParam(required = false) String description, // 修改：允许描述为空
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {

        log.info("更新活动记录ID: {}, 新活动ID: {}, 描述: {}, 日期: {}",
                recordId, newActivityId, description, date);

        ActivityRecord record = activityService.updateActivityRecord(recordId, newActivityId, description, date);
        return ResponseEntity.ok(record);
    }

    /**
     * 删除活动记录
     */
    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<Void> deleteActivityRecord(@PathVariable Long recordId) {
        log.info("删除活动记录ID: {}", recordId);

        activityService.deleteActivityRecord(recordId);
        return ResponseEntity.ok().build();
    }
}