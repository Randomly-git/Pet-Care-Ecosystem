// controller/MediaController.java
package com.petcare.media.controller;

import com.petcare.media.dto.ApiResponse;
import com.petcare.media.dto.MediaResponse;
import com.petcare.media.entity.MediaFile;
import com.petcare.media.service.MediaServiceLocal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;
import com.petcare.media.dto.MediaBatchUpdateRequest;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/media") // 统一使用 /api/v1 前缀
@RequiredArgsConstructor
public class MediaController {

    private final MediaServiceLocal mediaService;

    // POST /api/media/upload
    @PostMapping("/upload")
    public ApiResponse<MediaResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam("relatedType") String relatedType,
            @RequestParam("relatedId") Long relatedId) {

        MediaFile mediaFile = mediaService.uploadMediaFile(file, userId, relatedType, relatedId);
        MediaResponse response = MediaResponse.fromEntity(mediaFile);

        // 直接返回 ApiResponse.success
        return ApiResponse.success("文件上传成功", response);
    }

    // GET /api/media/{mediaId}
    @GetMapping("/{mediaId}")
    public ApiResponse<MediaResponse> getFileInfo(@PathVariable Long mediaId) {
        MediaFile mediaFile = mediaService.getMediaFileById(mediaId);
        MediaResponse response = MediaResponse.fromEntity(mediaFile);
        return ApiResponse.success(response);
    }

    // GET /api/media/user/{userId}
    @GetMapping("/user/{userId}")
    public ApiResponse<List<MediaResponse>> getUserFiles(@PathVariable Long userId) {
        List<MediaFile> mediaFiles = mediaService.getMediaFilesByUserId(userId);
        List<MediaResponse> responses = mediaFiles.stream()
                .map(MediaResponse::fromEntity)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    // GET /api/media/related/{relatedType}/{relatedId}
    @GetMapping("/related/{relatedType}/{relatedId}")
    public ApiResponse<List<MediaResponse>> getRelatedFiles(
            @PathVariable String relatedType,
            @PathVariable Long relatedId) {
        List<MediaFile> mediaFiles = mediaService.getMediaFilesByRelated(relatedType, relatedId);
        List<MediaResponse> responses = mediaFiles.stream()
                .map(MediaResponse::fromEntity)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    // DELETE /api/media/{mediaId}
    @DeleteMapping("/{mediaId}")
    public ApiResponse<Void> deleteFile(@PathVariable Long mediaId) {
        mediaService.deleteMediaFile(mediaId);
        return ApiResponse.success("文件删除成功", null);
    }

    // DELETE /api/media/related/{relatedType}/{relatedId}
    @DeleteMapping("/related/{relatedType}/{relatedId}")
    public ApiResponse<Void> deleteRelatedFiles(
            @PathVariable String relatedType,
            @PathVariable Long relatedId) {
        mediaService.deleteMediaFilesByRelated(relatedType, relatedId);
        return ApiResponse.success("关联文件删除成功", null);
    }

    // GET /api/media/types
    @GetMapping("/types")
    public ApiResponse<List<String>> getValidRelatedTypes() {
        List<String> types = mediaService.getValidRelatedTypes();
        return ApiResponse.success(types);
    }

    /**
     * PATCH /media/batch/related
     * API: 批量更新媒体文件的 relatedId，完成文件与业务实体的关联。
     * 使用 @Valid 触发 MediaBatchUpdateRequest 的校验规则。
     */
    @PatchMapping("/batch/related")
    public ResponseEntity<ApiResponse<Integer>> batchUpdateRelatedId(@Valid @RequestBody MediaBatchUpdateRequest request) {
        try {
            int updatedCount = mediaService.batchUpdateRelatedId(
                    request.getMediaIds(),
                    request.getRelatedType(),
                    request.getNewRelatedId()
            );
            return ResponseEntity.ok(ApiResponse.success("批量关联成功", updatedCount));
        } catch (IllegalArgumentException e) {
            // 业务错误：使用 40001 作为业务状态码，并传入错误信息
            return ResponseEntity.badRequest().body(ApiResponse.error(40001, e.getMessage()));
        } catch (Exception e) {
            // 系统错误：使用 50000 作为业务状态码，并传入错误信息
            return ResponseEntity.internalServerError().body(ApiResponse.error(50000, "批量关联失败: " + e.getMessage()));
        }
    }
}