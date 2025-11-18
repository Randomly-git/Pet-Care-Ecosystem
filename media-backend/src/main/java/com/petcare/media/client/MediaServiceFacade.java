// src/main/java/com/petcare/media/client/MediaServiceFacade.java
package com.petcare.media.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.media.dto.ApiResponse;
import com.petcare.media.dto.MediaResponse;
import com.petcare.media.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MediaServiceFacade {

    private final RestTemplate restTemplate;
    private final FileUploadUtil fileUploadUtil;
    private final ObjectMapper objectMapper;

    @Value("${app.media-service.url:http://localhost:8082}")
    private String mediaServiceUrl;

    /**
     * 上传文件（MultipartFile）
     */
    public MediaResponse uploadFile(MultipartFile file, Long petId, String relatedType, Long relatedId) {
        try {
            // 创建请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));
            body.add("petId", petId);
            body.add("relatedType", relatedType);
            body.add("relatedId", relatedId);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 发送请求
            String uploadUrl = mediaServiceUrl + "/api/media/upload";
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ApiResponse<MediaResponse> apiResponse = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<ApiResponse<MediaResponse>>() {}
                );

                if (apiResponse.isSuccess()) {
                    log.info("✅ 文件上传成功: {}", file.getOriginalFilename());
                    return apiResponse.getData();
                } else {
                    throw new RuntimeException("文件上传失败: " + apiResponse.getMessage());
                }
            } else {
                throw new RuntimeException("文件上传失败，HTTP状态: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ 文件上传失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件信息
     */
    public MediaResponse getFileInfo(Long mediaId) {
        try {
            String url = mediaServiceUrl + "/api/media/" + mediaId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ApiResponse<MediaResponse> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<MediaResponse>>() {}
            );

            if (apiResponse.isSuccess()) {
                return apiResponse.getData();
            } else {
                throw new RuntimeException("获取文件信息失败: " + apiResponse.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("获取文件信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(Long mediaId) {
        try {
            String url = mediaServiceUrl + "/api/media/" + mediaId;
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, null, String.class);

            ApiResponse<Void> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<Void>>() {}
            );

            if (!apiResponse.isSuccess()) {
                throw new RuntimeException("文件删除失败: " + apiResponse.getMessage());
            }

            log.info("✅ 文件删除成功: {}", mediaId);
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取宠物的所有文件
     */
    public List<MediaResponse> getPetFiles(Long petId) {
        try {
            String url = mediaServiceUrl + "/api/media/pet/" + petId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ApiResponse<List<MediaResponse>> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<List<MediaResponse>>>() {}
            );

            if (apiResponse.isSuccess()) {
                return apiResponse.getData();
            } else {
                throw new RuntimeException("获取宠物文件失败: " + apiResponse.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("获取宠物文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除关联业务的所有文件
     */
    public void deleteRelatedFiles(String relatedType, Long relatedId) {
        try {
            String url = mediaServiceUrl + "/api/media/related/" + relatedType + "/" + relatedId;
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, null, String.class);

            ApiResponse<Void> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<Void>>() {}
            );

            if (!apiResponse.isSuccess()) {
                throw new RuntimeException("删除关联文件失败: " + apiResponse.getMessage());
            }

            log.info("✅ 关联文件删除成功: {}/{}", relatedType, relatedId);
        } catch (Exception e) {
            throw new RuntimeException("删除关联文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取有效的关联类型
     */
    public List<String> getValidRelatedTypes() {
        try {
            String url = mediaServiceUrl + "/api/media/types";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ApiResponse<List<String>> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<List<String>>>() {}
            );

            if (apiResponse.isSuccess()) {
                return apiResponse.getData();
            } else {
                throw new RuntimeException("获取关联类型失败: " + apiResponse.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("获取关联类型失败: " + e.getMessage(), e);
        }
    }

    // ========== 本地文件上传的方法 ==========

    /**
     * 上传本地文件
     */
    public MediaResponse uploadLocalFile(String filePath, Long petId, String relatedType, Long relatedId) {
        String result = fileUploadUtil.uploadLocalFile(filePath, petId, relatedType, relatedId, mediaServiceUrl);
        try {
            // 解析返回的JSON
            ApiResponse<MediaResponse> apiResponse = objectMapper.readValue(
                    result,
                    new TypeReference<ApiResponse<MediaResponse>>() {}
            );
            return apiResponse.getData();
        } catch (Exception e) {
            throw new RuntimeException("解析上传结果失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从字节数组上传文件
     */
    public MediaResponse uploadFileFromBytes(byte[] fileBytes, String fileName, Long petId, String relatedType, Long relatedId) {
        String result = fileUploadUtil.uploadFileFromBytes(fileBytes, fileName, petId, relatedType, relatedId, mediaServiceUrl);
        try {
            // 解析返回的JSON
            ApiResponse<MediaResponse> apiResponse = objectMapper.readValue(
                    result,
                    new TypeReference<ApiResponse<MediaResponse>>() {}
            );
            return apiResponse.getData();
        } catch (Exception e) {
            throw new RuntimeException("解析上传结果失败: " + e.getMessage(), e);
        }
    }

    /**
     * 自定义资源类用于文件上传
     */
    private static class MultipartFileResource extends ByteArrayResource {
        private final String filename;

        public MultipartFileResource(MultipartFile multipartFile) throws Exception {
            super(multipartFile.getBytes());
            this.filename = multipartFile.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    public List<MediaResponse> getRelatedFiles(String relatedType, Long relatedId) {
        try {
            String url = mediaServiceUrl + "/api/media/related/" + relatedType + "/" + relatedId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ApiResponse<List<MediaResponse>> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<List<MediaResponse>>>() {}
            );

            if (apiResponse.isSuccess()) {
                return apiResponse.getData();
            } else {
                throw new RuntimeException("获取关联文件失败: " + apiResponse.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("获取关联文件失败: " + e.getMessage(), e);
        }
    }

}