package com.petcare.backend.controller;

import com.petcare.backend.client.MediaServiceClient;
import com.petcare.backend.controller.ApiResponse;
import com.petcare.backend.dto.response.TimelineDTO;
import com.petcare.backend.dto.request.CreateStatusRecordDTO;
import com.petcare.backend.entity.Status;
import com.petcare.backend.service.StatusService;
import com.petcare.backend.service.PetStatusHBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
    private final PetStatusHBaseService petStatusHBaseService;

    public StatusController(StatusService statusService, MediaServiceClient mediaServiceClient, PetStatusHBaseService petStatusHBaseService) {
        this.statusService = statusService;
        this.mediaServiceClient = mediaServiceClient;
        this.petStatusHBaseService = petStatusHBaseService;
    }

    @Operation(summary = "获取宠物的有效状态列表", description = "获取该宠物定义的所有未被软删除的状态类型")
    @GetMapping("/pet/{petId}")
    public ApiResponse<List<Status>> getValidStatusesByPetId(
            @Parameter(description = "宠物ID") @PathVariable Long petId) {
        log.debug("开始获取宠物ID为 {} 的状态列表", petId);

        try {
            List<Status> statuses = statusService.getValidStatusesByPetId(petId);
            return ApiResponse.success(statuses);
        } catch (Exception e) {
            log.error("获取宠物ID为 {} 的状态列表失败", petId, e);
            return ApiResponse.error("获取状态列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "软删除状态", description = "逻辑删除某种状态类型")
    @DeleteMapping("/{statusId}")
    public ApiResponse<Void> softDeleteStatus(
            @Parameter(description = "状态ID") @PathVariable Long statusId) {
        try {
            statusService.softDeleteStatus(statusId);
            return ApiResponse.success(null, "删除成功");
        } catch (Exception e) {
            log.error("软删除状态ID为 {} 的状态失败", statusId, e);
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "修改状态名称", description = "更新已有状态类型的显示名称")
    @PutMapping("/{statusId}/name")
    public ApiResponse<Status> updateStatusName(
            @Parameter(description = "状态ID") @PathVariable Long statusId,
            @Parameter(description = "新的状态名称") @RequestParam String newName) {
        try {
            Status updatedStatus = statusService.updateStatusName(statusId, newName);
            return ApiResponse.success(updatedStatus, "修改成功");
        } catch (Exception e) {
            log.error("更新状态ID为 {} 的名称失败", statusId, e);
            return ApiResponse.error("修改失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新状态当前值", description = "更新状态的当前值（如：健康、生病中、怀孕中等）")
    @PutMapping("/{statusId}/value")
    public ApiResponse<Status> updateStatusValue(
            @Parameter(description = "状态ID") @PathVariable Long statusId,
            @Parameter(description = "新的状态值") @RequestParam String statusValue) {
        try {
            Status updatedStatus = statusService.updateStatusValue(statusId, statusValue);
            return ApiResponse.success(updatedStatus, "保存成功");
        } catch (Exception e) {
            log.error("更新状态ID为 {} 的状态值失败", statusId, e);
            return ApiResponse.error("保存失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取宠物状态变更时间线", description = "获取宠物的状态变更历史时间线，支持按状态ID过滤，分页加载")
    @GetMapping("/timeline/{petId}")
    public ApiResponse<List<TimelineDTO>> getStatusTimeline(
            @Parameter(description = "宠物ID") @PathVariable Long petId,
            @Parameter(description = "状态ID过滤，可选") @RequestParam(required = false) Long statusId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小，默认50") @RequestParam(defaultValue = "50") int size) {
        log.debug("开始获取宠物ID为 {} 的状态时间线, statusId={}, page={}, size={}", petId, statusId, page, size);

        try {
            List<TimelineDTO> timeline = petStatusHBaseService.getStatusTimeline(petId, statusId, page, size);
            return ApiResponse.success(timeline);
        } catch (Exception e) {
            log.error("获取宠物ID为 {} 的状态时间线失败", petId, e);
            return ApiResponse.error("获取状态时间线失败: " + e.getMessage());
        }
    }
}