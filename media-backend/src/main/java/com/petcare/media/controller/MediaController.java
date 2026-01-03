// controller/MediaController.java
package com.petcare.media.controller;

import com.petcare.media.dto.ApiResponse;
import com.petcare.media.dto.MediaResponse;
import com.petcare.media.entity.MediaFile;
import com.petcare.media.service.MediaService;
import com.petcare.media.dto.MediaBatchUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "媒体管理接口", description = "负责系统中所有图片、视频的上传、查询、批量关联及删除操作")
public class MediaController {

    private final MediaService mediaService;

    /**
     * POST /api/media/upload
     * 上传并关联媒体文件
     */
    @Operation(summary = "上传媒体文件", description = "上传单个文件（图片/视频）并立即关联到指定的业务实体（如 MOMENT 或 ACTIVITY）")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaResponse> uploadFile(
            @Parameter(description = "文件流", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传用户ID") @RequestParam("userId") Long userId,
            @Parameter(description = "关联业务类型 (例如: MOMENT, ACTIVITY, STATUS)", example = "MOMENT")
            @RequestParam("relatedType") String relatedType,
            @Parameter(description = "关联业务实体的ID") @RequestParam("relatedId") Long relatedId) {

        MediaFile mediaFile = mediaService.uploadMediaFile(file, userId, relatedType, relatedId);
        MediaResponse response = MediaResponse.fromEntity(mediaFile);

        return ApiResponse.success("文件上传成功", response);
    }

    /**
     * GET /api/media/{mediaId}
     * 获取单个媒体文件信息
     */
    @Operation(summary = "获取媒体详情", description = "根据媒体ID获取文件的元数据及访问URL")
    @GetMapping("/{mediaId}")
    public ApiResponse<MediaResponse> getFileInfo(
            @Parameter(description = "媒体ID") @PathVariable Long mediaId) {
        MediaFile mediaFile = mediaService.getMediaFileById(mediaId);
        return ApiResponse.success(MediaResponse.fromEntity(mediaFile));
    }

    /**
     * GET /api/media/related/{relatedType}/{relatedId}
     * 获取关联的媒体列表
     */
    @Operation(summary = "查询关联媒体", description = "获取某个业务实体（如某条动态）下所有的媒体文件列表")
    @GetMapping("/related/{relatedType}/{relatedId}")
    public ApiResponse<List<MediaResponse>> getRelatedFiles(
            @Parameter(description = "关联类型") @PathVariable String relatedType,
            @Parameter(description = "关联ID") @PathVariable Long relatedId) {
        List<MediaFile> files = mediaService.getMediaFilesByRelated(relatedType, relatedId);
        List<MediaResponse> response = files.stream()
                .map(MediaResponse::fromEntity)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    /**
     * DELETE /api/media/{mediaId}
     * 删除单个媒体文件
     */
    @Operation(summary = "删除媒体文件", description = "根据ID彻底删除媒体文件及磁盘上的物理资源")
    @DeleteMapping("/{mediaId}")
    public ApiResponse<Void> deleteFile(
            @Parameter(description = "媒体ID") @PathVariable Long mediaId) {
        mediaService.deleteMediaFile(mediaId);
        return ApiResponse.success("文件删除成功", null);
    }

    /**
     * DELETE /api/media/related/{relatedType}/{relatedId}
     * 批量删除关联的媒体
     */
    @Operation(summary = "批量删除关联媒体", description = "删除指定业务实体下的所有关联媒体文件")
    @DeleteMapping("/related/{relatedType}/{relatedId}")
    public ApiResponse<Void> deleteRelatedFiles(
            @Parameter(description = "关联类型") @PathVariable String relatedType,
            @Parameter(description = "关联ID") @PathVariable Long relatedId) {
        mediaService.deleteMediaFilesByRelated(relatedType, relatedId);
        return ApiResponse.success("关联文件删除成功", null);
    }

    /**
     * GET /api/media/types
     * 获取有效的关联类型
     */
    @Operation(summary = "获取有效关联类型", description = "返回系统中所有允许的业务关联枚举值")
    @GetMapping("/types")
    public ApiResponse<List<String>> getValidRelatedTypes() {
        List<String> types = mediaService.getValidRelatedTypes();
        return ApiResponse.success(types);
    }

    /**
     * PATCH /api/media/batch/related
     * 批量更新媒体文件的 relatedId
     */
    @Operation(summary = "批量关联媒体", description = "将多个已上传但未绑定的媒体文件批量关联到具体的业务实体ID上")
    @PatchMapping("/batch/related")
    public ResponseEntity<ApiResponse<Integer>> batchUpdateRelatedId(
            @Valid @RequestBody MediaBatchUpdateRequest request) {
        try {
            int updatedCount = mediaService.batchUpdateRelatedId(
                    request.getMediaIds(),
                    request.getRelatedType(),
                    request.getNewRelatedId()
            );
            return ResponseEntity.ok(ApiResponse.success("批量关联成功", updatedCount));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(40001, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(50000, "系统错误: " + e.getMessage()));
        }
    }
}