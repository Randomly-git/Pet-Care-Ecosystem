// controller/MediaController.java
package com.petcare.media.controller;

import com.petcare.media.dto.ApiResponse;
import com.petcare.media.dto.MediaResponse;
import com.petcare.media.entity.MediaFile;
import com.petcare.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/media") // 统一使用 /api 前缀
@RequiredArgsConstructor // 替代 @Autowired
public class MediaController {

    private final MediaService mediaService;

    // POST /api/media/upload
    @PostMapping("/upload")
    public ApiResponse<MediaResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("petId") Long petId,
            @RequestParam("relatedType") String relatedType,
            @RequestParam("relatedId") Long relatedId) {

        MediaFile mediaFile = mediaService.uploadMediaFile(file, petId, relatedType, relatedId);
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

    // GET /api/media/pet/{petId}
    @GetMapping("/pet/{petId}")
    public ApiResponse<List<MediaResponse>> getPetFiles(@PathVariable Long petId) {
        List<MediaFile> mediaFiles = mediaService.getMediaFilesByPetId(petId);
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
}