// client/MediaServiceClient.java
package com.petcare.backend.client;

import com.petcare.backend.dto.response.ApiResponse;
import com.petcare.backend.dto.response.MediaResponse;
import com.petcare.backend.dto.request.MediaBatchUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class MediaServiceClient {

    private final RestTemplate restTemplate;

    @Value("${media.service.url:http://localhost:8082}")
    private String mediaServiceUrl;

    public MediaServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 上传文件到媒体服务
     */
    public MediaResponse uploadFile(MultipartFile file, Long userId, String relatedType, Long relatedId) {
        try {
            String url = mediaServiceUrl + "/api/media/upload";

            // 构建请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // 添加文件
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", resource);

            // 添加其他参数
            body.add("userId", userId);
            body.add("relatedType", relatedType);
            body.add("relatedId", relatedId);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 发送请求
            ResponseEntity<ApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    ApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                if (apiResponse.getCode() == 20000) {
                    // 使用LinkedHashMap转换
                    Object data = apiResponse.getData();
                    MediaResponse mediaResponse = convertToMediaResponse(data);
                    log.info("文件上传成功: {}", mediaResponse.getFileName());
                    return mediaResponse;
                } else {
                    throw new RuntimeException("媒体服务返回错误: " + apiResponse.getMessage());
                }
            }

            throw new RuntimeException("上传文件失败，状态码: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("调用媒体服务上传文件失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取关联的文件列表
     */
    public List<MediaResponse> getRelatedFiles(String relatedType, Long relatedId) {
        try {
            String url = mediaServiceUrl + "/api/media/related/" + relatedType + "/" + relatedId;

            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(url, ApiResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                if (apiResponse.getCode() == 20000) {
                    List<?> dataList = (List<?>) apiResponse.getData();
                    return dataList.stream()
                            .map(this::convertToMediaResponse)
                            .toList();
                }
            }

            log.warn("获取关联文件失败: {}", response.getStatusCode());
            return List.of();

        } catch (Exception e) {
            log.error("调用媒体服务获取关联文件失败", e);
            return List.of();
        }
    }

    /**
     * 删除关联的文件
     */
    public void deleteRelatedFiles(String relatedType, Long relatedId) {
        try {
            String url = mediaServiceUrl + "/api/media/related/" + relatedType + "/" + relatedId;

            ResponseEntity<ApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    ApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                if (apiResponse.getCode() == 20000) {
                    log.info("删除关联文件成功: {}/{}", relatedType, relatedId);
                } else {
                    log.warn("删除关联文件失败: {}", apiResponse.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("调用媒体服务删除关联文件失败", e);
            // 根据业务需求决定是否抛出异常
        }
    }

    /**
     * 删除单个文件
     */
    public void deleteFile(Long mediaId) {
        try {
            String url = mediaServiceUrl + "/api/media/" + mediaId;

            ResponseEntity<ApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    ApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("删除文件成功: {}", mediaId);
            }

        } catch (Exception e) {
            log.error("调用媒体服务删除文件失败", e);
        }
    }

    /**
     * 批量更新关联ID
     */
    public int batchUpdateRelatedId(List<Long> mediaIds, String relatedType, Long newRelatedId) {
        try {
            String url = mediaServiceUrl + "/api/media/batch/related";

            // 构建请求体
            MediaBatchUpdateRequest request = new MediaBatchUpdateRequest();
            request.setMediaIds(mediaIds);
            request.setRelatedType(relatedType);
            request.setNewRelatedId(newRelatedId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MediaBatchUpdateRequest> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    requestEntity,
                    ApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                if (apiResponse.getCode() == 20000) {
                    Integer count = (Integer) apiResponse.getData();
                    log.info("批量更新关联ID成功，更新数量: {}", count);
                    return count != null ? count : 0;
                } else {
                    throw new RuntimeException("批量更新失败: " + apiResponse.getMessage());
                }
            }

            throw new RuntimeException("批量更新失败，状态码: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("调用媒体服务批量更新关联ID失败", e);
            throw new RuntimeException("批量更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转换Object到MediaResponse
     */
    private MediaResponse convertToMediaResponse(Object obj) {
        if (obj instanceof java.util.LinkedHashMap) {
            java.util.LinkedHashMap<?, ?> map = (java.util.LinkedHashMap<?, ?>) obj;
            MediaResponse response = new MediaResponse();

            response.setMediaId(((Number) map.get("mediaId")).longValue());
            response.setFileName((String) map.get("fileName"));
            response.setFileUrl((String) map.get("fileUrl"));
            response.setFileType((String) map.get("fileType"));
            response.setFileSize(((Number) map.get("fileSize")).longValue());
            response.setUploadTime(map.get("uploadTime") != null ?
                    java.time.LocalDateTime.parse(map.get("uploadTime").toString()) : null);
            response.setUserId(((Number) map.get("userId")).longValue());
            response.setRelatedType((String) map.get("relatedType"));
            response.setRelatedTypeDesc((String) map.get("relatedTypeDesc"));
            response.setRelatedId(((Number) map.get("relatedId")).longValue());

            return response;
        }

        // 如果已经是MediaResponse类型，直接返回
        if (obj instanceof MediaResponse) {
            return (MediaResponse) obj;
        }

        throw new IllegalArgumentException("无法转换对象到MediaResponse: " + obj.getClass());
    }
}