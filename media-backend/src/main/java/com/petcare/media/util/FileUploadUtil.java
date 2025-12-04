// src/main/java/com/petcare/media/util/FileUploadUtil.java
package com.petcare.media.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class FileUploadUtil {

    private final RestTemplate restTemplate;

    public FileUploadUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 上传本地文件到媒体服务
     */
    public String uploadLocalFile(String filePath, Long userId, String relatedType, Long relatedId, String mediaServiceUrl) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("文件不存在: " + filePath);
            }

            // 创建 MultipartFile
            Resource resource = new FileSystemResource(file);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);
            body.add("userId", userId);
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
                log.info("✅ 文件上传成功: {}", filePath);
                return response.getBody();
            } else {
                throw new RuntimeException("文件上传失败: " + response.getBody());
            }

        } catch (Exception e) {
            log.error("❌ 文件上传失败: {}", filePath, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从字节数组上传文件
     */
    public String uploadFileFromBytes(byte[] fileBytes, String fileName, Long userId, String relatedType, Long relatedId, String mediaServiceUrl) {
        try {
            // 创建临时文件
            Path tempFile = Files.createTempFile("upload_", "_" + fileName);
            Files.write(tempFile, fileBytes);

            String result = uploadLocalFile(tempFile.toString(), userId, relatedType, relatedId, mediaServiceUrl);

            // 删除临时文件
            Files.deleteIfExists(tempFile);

            return result;

        } catch (IOException e) {
            throw new RuntimeException("创建临时文件失败: " + e.getMessage(), e);
        }
    }
}