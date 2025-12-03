// src/main/java/com/petcare/media/client/MediaServiceFacade.java
package com.petcare.media.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.media.dto.ApiResponse;
import com.petcare.media.dto.MediaResponse;
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
    private final ObjectMapper objectMapper;

    // 在客户端代码中定义了 MediaClientException，用于封装调用媒体服务失败的异常。
    private static class MediaClientException extends RuntimeException {
        public MediaClientException(String message) {
            super(message);
        }
        public MediaClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    @Value("${app.media-service.url:http://localhost:8082}")
    private String mediaServiceUrl;

    /**
     * 上传文件（MultipartFile）
     */
    public MediaResponse uploadFile(MultipartFile file, Long petId, String relatedType, Long relatedId) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));
            body.add("petId", petId);
            body.add("relatedType", relatedType);
            body.add("relatedId", relatedId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String uploadUrl = mediaServiceUrl + "/api/media/upload";
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ApiResponse<MediaResponse> apiResponse = parseResponse(response.getBody(), new TypeReference<ApiResponse<MediaResponse>>() {});

                // ⚡️ 核心修改：检查 code 是否为 20000
                if (apiResponse.getCode() == 20000) {
                    log.info("✅ 文件上传成功: {}", file.getOriginalFilename());
                    return apiResponse.getData();
                } else {
                    // 抛出客户端异常，包含后端返回的 code 和 message
                    throw new MediaClientException(
                            String.format("文件上传失败 (Code: %d): %s", apiResponse.getCode(), apiResponse.getMessage()));
                }
            } else {
                throw new MediaClientException("文件上传失败，HTTP状态: " + response.getStatusCode());
            }

        } catch (MediaClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ 文件上传失败: {}", file.getOriginalFilename(), e);
            throw new MediaClientException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件信息
     */
    public MediaResponse getFileInfo(Long mediaId) {
        try {
            String url = mediaServiceUrl + "/api/media/" + mediaId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ApiResponse<MediaResponse> apiResponse = parseResponse(response.getBody(), new TypeReference<ApiResponse<MediaResponse>>() {});

            // ⚡️ 核心修改：检查 code 是否为 20000
            if (apiResponse.getCode() == 20000) {
                return apiResponse.getData();
            } else {
                throw new MediaClientException(
                        String.format("获取文件信息失败 (Code: %d): %s", apiResponse.getCode(), apiResponse.getMessage()));
            }
        } catch (MediaClientException e) {
            throw e;
        } catch (Exception e) {
            throw new MediaClientException("获取文件信息失败: " + e.getMessage(), e);
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

            ApiResponse<Void> apiResponse = parseResponse(response.getBody(), new TypeReference<ApiResponse<Void>>() {});

            // ⚡️ 核心修改：检查 code 是否为 20000
            if (apiResponse.getCode() != 20000) {
                throw new MediaClientException(
                        String.format("文件删除失败 (Code: %d): %s", apiResponse.getCode(), apiResponse.getMessage()));
            }

            log.info("✅ 文件删除成功: {}", mediaId);
        } catch (MediaClientException e) {
            throw e;
        } catch (Exception e) {
            throw new MediaClientException("文件删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取宠物的所有文件
     */
    public List<MediaResponse> getPetFiles(Long petId) {
        try {
            String url = mediaServiceUrl + "/api/media/pet/" + petId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ApiResponse<List<MediaResponse>> apiResponse = parseResponse(response.getBody(), new TypeReference<ApiResponse<List<MediaResponse>>>() {});

            // ⚡️ 核心修改：检查 code 是否为 20000
            if (apiResponse.getCode() == 20000) {
                return apiResponse.getData();
            } else {
                throw new MediaClientException(
                        String.format("获取宠物文件失败 (Code: %d): %s", apiResponse.getCode(), apiResponse.getMessage()));
            }
        } catch (MediaClientException e) {
            throw e;
        } catch (Exception e) {
            throw new MediaClientException("获取宠物文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取关联业务的所有文件
     */
    public List<MediaResponse> getRelatedFiles(String relatedType, Long relatedId) {
        try {
            String url = mediaServiceUrl + "/api/media/related/" + relatedType + "/" + relatedId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ApiResponse<List<MediaResponse>> apiResponse = parseResponse(response.getBody(), new TypeReference<ApiResponse<List<MediaResponse>>>() {});

            // ⚡️ 核心修改：检查 code 是否为 20000
            if (apiResponse.getCode() == 20000) {
                return apiResponse.getData();
            } else {
                throw new MediaClientException(
                        String.format("获取关联文件失败 (Code: %d): %s", apiResponse.getCode(), apiResponse.getMessage()));
            }
        } catch (MediaClientException e) {
            throw e;
        } catch (Exception e) {
            throw new MediaClientException("获取关联文件失败: " + e.getMessage(), e);
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

            ApiResponse<Void> apiResponse = parseResponse(response.getBody(), new TypeReference<ApiResponse<Void>>() {});

            // ⚡️ 核心修改：检查 code 是否为 20000
            if (apiResponse.getCode() != 20000) {
                throw new MediaClientException(
                        String.format("删除关联文件失败 (Code: %d): %s", apiResponse.getCode(), apiResponse.getMessage()));
            }

            log.info("✅ 关联文件删除成功: {}/{}", relatedType, relatedId);
        } catch (MediaClientException e) {
            throw e;
        } catch (Exception e) {
            throw new MediaClientException("删除关联文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取有效的关联类型
     */
    public List<String> getValidRelatedTypes() {
        try {
            String url = mediaServiceUrl + "/api/media/types";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ApiResponse<List<String>> apiResponse = parseResponse(response.getBody(), new TypeReference<ApiResponse<List<String>>>() {});

            // ⚡️ 核心修改：检查 code 是否为 20000
            if (apiResponse.getCode() == 20000) {
                return apiResponse.getData();
            } else {
                throw new MediaClientException(
                        String.format("获取关联类型失败 (Code: %d): %s", apiResponse.getCode(), apiResponse.getMessage()));
            }
        } catch (MediaClientException e) {
            throw e;
        } catch (Exception e) {
            throw new MediaClientException("获取关联类型失败: " + e.getMessage(), e);
        }
    }

    // 💡 泛型解析辅助方法
    private <T> T parseResponse(String json, TypeReference<T> typeRef) throws Exception {
        if (json == null) {
            throw new MediaClientException("API响应体为空");
        }
        return objectMapper.readValue(json, typeRef);
    }

    /**
     * 自定义资源类用于文件上传 (保留)
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
}