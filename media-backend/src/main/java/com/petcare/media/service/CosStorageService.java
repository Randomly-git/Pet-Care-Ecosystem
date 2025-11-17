// service/CosStorageService.java
package com.petcare.media.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class CosStorageService {

    @Autowired
    private COSClient cosClient;

    @Value("${tencent.cos.bucket-name}")
    private String bucketName;

    @Value("${tencent.cos.region}")
    private String region;

    public String uploadFile(MultipartFile file, String filePath) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;
        String key = filePath + "/" + fileName;

        File tempFile = File.createTempFile("cos_upload", fileExtension);
        file.transferTo(tempFile);

        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, tempFile);
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

            String fileUrl = "https://" + bucketName + ".cos." + region + ".myqcloud.com/" + key;
            log.info("文件上传成功: {}, ETag: {}", fileUrl, putObjectResult.getETag());

            return fileUrl;

        } catch (CosClientException e) {
            log.error("腾讯云COS上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传到云存储失败: " + e.getMessage(), e);
        } finally {
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String prefix = "https://" + bucketName + ".cos." + region + ".myqcloud.com/";
            String key = fileUrl.replace(prefix, "");

            cosClient.deleteObject(bucketName, key);
            log.info("文件从COS删除成功: {}", key);

        } catch (CosClientException e) {
            log.error("腾讯云COS删除失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件从云存储删除失败: " + e.getMessage(), e);
        }
    }
}