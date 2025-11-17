// controller/MediaController.java
package com.petcare.media.controller;

import com.petcare.media.dto.ApiResponse;
import com.petcare.media.dto.MediaResponse;
import com.petcare.media.entity.MediaFile;
import com.petcare.media.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/media")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("petId") Long petId,
            @RequestParam("relatedType") String relatedType,
            @RequestParam("relatedId") Long relatedId) {

        try {
            MediaFile mediaFile = mediaService.uploadMediaFile(file, petId, relatedType, relatedId);
            MediaResponse response = MediaResponse.fromEntity(mediaFile);
            return ResponseEntity.ok(ApiResponse.success("文件上传成功", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<MediaResponse>> getFileInfo(@PathVariable Long mediaId) {
        try {
            MediaFile mediaFile = mediaService.getMediaFileById(mediaId);
            MediaResponse response = MediaResponse.fromEntity(mediaFile);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/pet/{petId}")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getPetFiles(@PathVariable Long petId) {
        try {
            List<MediaFile> mediaFiles = mediaService.getMediaFilesByPetId(petId);
            List<MediaResponse> responses = mediaFiles.stream()
                    .map(MediaResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(responses));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/related/{relatedType}/{relatedId}")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getRelatedFiles(
            @PathVariable String relatedType,
            @PathVariable Long relatedId) {
        try {
            List<MediaFile> mediaFiles = mediaService.getMediaFilesByRelated(relatedType, relatedId);
            List<MediaResponse> responses = mediaFiles.stream()
                    .map(MediaResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(responses));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long mediaId) {
        try {
            mediaService.deleteMediaFile(mediaId);
            return ResponseEntity.ok(ApiResponse.success("文件删除成功", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/related/{relatedType}/{relatedId}")
    public ResponseEntity<ApiResponse<Void>> deleteRelatedFiles(
            @PathVariable String relatedType,
            @PathVariable Long relatedId) {
        try {
            mediaService.deleteMediaFilesByRelated(relatedType, relatedId);
            return ResponseEntity.ok(ApiResponse.success("关联文件删除成功", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<String>>> getValidRelatedTypes() {
        try {
            List<String> types = mediaService.getValidRelatedTypes();
            return ResponseEntity.ok(ApiResponse.success(types));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}