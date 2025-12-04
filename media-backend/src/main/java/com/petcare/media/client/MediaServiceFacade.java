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

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MediaServiceFacade {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 在客户端代码中定义了 MediaClientException，用于封装调用媒体服务失败的异常。
    public static class MediaClientException extends RuntimeException {
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
     * 上传文件（MultipartFile）- 修复版本
     */
    public MediaResponse uploadFile(MultipartFile file, Long userId, String relatedType, Long relatedId) {
        try {
            log.info("📤 开始上传文件: 文件名={}, 大小={}字节, 类型={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            // 🎯 关键修复1：验证文件大小
            if (file.isEmpty()) {
                throw new MediaClientException("文件不能为空");
            }

            if (file.getSize() < 100) {
                log.warn("⚠️ 警告：文件大小异常小，仅 {} 字节", file.getSize());
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // 🎯 关键修复2：使用修复的 MultipartFileResource
            body.add("file", new FixedMultipartFileResource(file));

            // 🎯 关键修复3：确保参数是字符串类型（非常重要！）
            body.add("userId", String.valueOf(userId));
            body.add("relatedType", relatedType);
            body.add("relatedId", String.valueOf(relatedId));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String uploadUrl = mediaServiceUrl + "/api/media/upload";

            log.info("📡 发送请求到: {}, 文件大小: {}字节", uploadUrl, file.getSize());

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ApiResponse<MediaResponse> apiResponse = parseResponse(response.getBody(), new TypeReference<ApiResponse<MediaResponse>>() {});

                // ⚡️ 核心修改：检查 code 是否为 20000
                if (apiResponse.getCode() == 20000) {
                    log.info("✅ 文件上传成功: {} ({}字节)",
                            file.getOriginalFilename(), file.getSize());
                    return apiResponse.getData();
                } else {
                    // 抛出客户端异常，包含后端返回的 code 和 message
                    throw new MediaClientException(
                            String.format("文件上传失败 (Code: %d): %s",
                                    apiResponse.getCode(), apiResponse.getMessage()));
                }
            } else {
                throw new MediaClientException("文件上传失败，HTTP状态: " + response.getStatusCode());
            }

        } catch (MediaClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ 文件上传失败详情 - 文件名: {}, 大小: {}字节, 错误: {}",
                    file.getOriginalFilename(), file.getSize(), e.getMessage(), e);
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
     * 获取用户的所有文件
     */
    public List<MediaResponse> getUserFiles(Long userId) {
        try {
            String url = mediaServiceUrl + "/api/media/user/" + userId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ApiResponse<List<MediaResponse>> apiResponse = parseResponse(response.getBody(), new TypeReference<ApiResponse<List<MediaResponse>>>() {});

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
     * 🎯 修复的 MultipartFileResource - 解决文件截断问题
     */
    private static class FixedMultipartFileResource extends ByteArrayResource {
        private final String filename;
        private final long fileSize;

        public FixedMultipartFileResource(MultipartFile multipartFile) {
            super(readBytesSafely(multipartFile));
            this.filename = multipartFile.getOriginalFilename();
            this.fileSize = multipartFile.getSize();

            log.debug("🔄 FixedMultipartFileResource 创建 - 文件名: {}, 大小: {}字节",
                    filename, fileSize);
        }

        /**
         * 🎯 关键修复：安全读取文件字节，避免截断
         */
        private static byte[] readBytesSafely(MultipartFile file) {
            try {
                log.debug("正在读取文件字节: {}, 报告大小: {}字节",
                        file.getOriginalFilename(), file.getSize());

                // 方法1：直接使用 getBytes() - 对于大多数情况都有效
                byte[] bytes = file.getBytes();

                log.debug("文件读取完成: 实际读取 {} 字节", bytes.length);

                // 验证文件大小是否匹配
                if (file.getSize() != bytes.length) {
                    log.warn("⚠️ 文件大小不匹配! 报告大小: {}字节, 实际读取: {}字节",
                            file.getSize(), bytes.length);
                }

                return bytes;
            } catch (IOException e) {
                log.error("❌ 读取文件失败: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
            }
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public long contentLength() {
            return fileSize;
        }
    }
}