// service/CosStorageService.java
package com.petcare.media.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
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

    @Value("${tencent.cos.static-website-url}")
    private String staticWebsiteUrl;

    // 归档恢复的等待时间（分钟）
    @Value("${cold.storage.restore.wait-minutes:3}")
    private int restoreWaitMinutes;

    // 归档文件临时访问时间（分钟）
    @Value("${cold.storage.restore.expiry-minutes:10}")
    private int restoreExpiryMinutes;

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

            String fileUrl = staticWebsiteUrl + "/" + key;
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
            String cleanedStaticWebsiteUrl = staticWebsiteUrl.endsWith("/")
                    ? staticWebsiteUrl.substring(0, staticWebsiteUrl.length() - 1)
                    : staticWebsiteUrl;
            String prefix = cleanedStaticWebsiteUrl + "/";
            String key = fileUrl.replace(prefix, "");

            cosClient.deleteObject(bucketName, key);
            log.info("文件从COS删除成功: {}", key);

        } catch (CosClientException e) {
            log.error("腾讯云COS删除失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件从云存储删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置对象标签（用于触发COS生命周期规则）
     * 设置对象标签（用于触发COS生命周期规则）
     * @param fileUrl 文件URL
     * @param tagKey 标签键
     * @param tagValue 标签值
     */
    public void setFileTagging(String fileUrl, String tagKey, String tagValue) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            // 使用COS SDK v5 通过复制对象的方式设置标签
            // 构造带标签的元数据
            ObjectMetadata metadata = new ObjectMetadata();
            // 设置x-cos-tagging头，格式为 key1=value1&key2=value2
            metadata.setHeader("x-cos-tagging", tagKey + "=" + tagValue);

            // 使用copyObject来保留原文件并添加标签
            CopyObjectRequest copyRequest = new CopyObjectRequest(bucketName, key, bucketName, key);
            copyRequest.setNewObjectMetadata(metadata);

            cosClient.copyObject(copyRequest);

            log.info("COS文件标签设置成功: key={}, {}={}", key, tagKey, tagValue);

        } catch (CosClientException e) {
            log.error("设置COS文件标签失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 批量设置对象标签
     * @param fileUrls 文件URL列表
     * @param tagKey 标签键
     * @param tagValue 标签值
     */
    public void batchSetFileTagging(List<String> fileUrls, String tagKey, String tagValue) {
        for (String fileUrl : fileUrls) {
            try {
                setFileTagging(fileUrl, tagKey, tagValue);
            } catch (Exception e) {
                log.error("批量设置标签失败: {}", fileUrl, e);
            }
        }
    }

    /**
     * 检查文件是否为归档存储
     * @param fileUrl 文件URL
     * @return true if archived
     */
    public boolean isArchived(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            ObjectMetadata metadata = cosClient.getObjectMetadata(bucketName, key);

            String storageClass = metadata.getStorageClass();
            // 归档存储类型包括：ARCHIVE, DEEP_ARCHIVE, STANDARD_IA, GLACIER
            return storageClass != null && (
                    storageClass.contains("ARCHIVE") ||
                    storageClass.contains("GLACIER") ||
                    storageClass.equals("STANDARD_IA")
            );
        } catch (CosClientException e) {
            log.error("检查文件存储类型失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 恢复归档文件（临时访问）
     * @param fileUrl 文件URL
     * @return 恢复结果信息
     */
    public String restoreArchivedFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            // 检查当前文件状态
            ObjectMetadata metadata = cosClient.getObjectMetadata(bucketName, key);
            String storageClass = metadata.getStorageClass();

            // 如果不是归档存储，直接返回
            if (storageClass == null || (!storageClass.contains("ARCHIVE") && !storageClass.contains("GLACIER"))) {
                log.info("文件未处于归档状态，无需恢复: {}", key);
                return "File is not archived";
            }

            // 检查是否已有恢复中的临时文件
            Boolean ongoingRestore = metadata.getOngoingRestore();
            if (ongoingRestore != null && ongoingRestore) {
                log.info("文件正在恢复中: {}", key);
                return "File is being restored";
            }

            // 发起恢复请求 - 使用COS SDK v5的RestoreObjectRequest
            RestoreObjectRequest restoreRequest = new RestoreObjectRequest(bucketName, key, restoreExpiryMinutes);
            cosClient.restoreObject(restoreRequest);

            log.info("归档文件恢复请求已提交: key={}, 有效期={}天", key, restoreExpiryMinutes);

            return "Restore initiated, please wait " + restoreWaitMinutes + " minutes";

        } catch (CosClientException e) {
            log.error("恢复归档文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("恢复归档文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 强制恢复归档文件（无论当前状态如何）
     * @param fileUrl 文件URL
     * @param waitMinutes 等待恢复完成的分钟数
     * @return 恢复后的输入流（如果需要立即获取）
     */
    public boolean forceRestoreArchivedFile(String fileUrl, int waitMinutes) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            // 检查当前状态
            ObjectMetadata metadata = cosClient.getObjectMetadata(bucketName, key);
            String storageClass = metadata.getStorageClass();

            // 非归档文件直接返回
            if (storageClass == null || (!storageClass.contains("ARCHIVE") && !storageClass.contains("GLACIER"))) {
                return false;
            }

            // 如果已有恢复中的任务，不重复发起
            Boolean ongoingRestore = metadata.getOngoingRestore();
            if (ongoingRestore != null && ongoingRestore) {
                log.info("文件正在恢复中，等待完成: {}", key);
                // 等待恢复完成
                try {
                    Thread.sleep(waitMinutes * 60 * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }

            // 发起新的恢复请求 - 使用COS SDK v5的RestoreObjectRequest
            RestoreObjectRequest restoreRequest = new RestoreObjectRequest(bucketName, key, restoreExpiryMinutes);
            cosClient.restoreObject(restoreRequest);

            log.info("归档文件恢复请求已提交: key={}", key);

            // 等待恢复完成
            try {
                log.info("等待文件恢复完成...");
                Thread.sleep(waitMinutes * 60 * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            return true;

        } catch (CosClientException e) {
            log.error("强制恢复归档文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("强制恢复归档文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件的存储类型
     * @param fileUrl 文件URL
     * @return 存储类型字符串
     */
    public String getStorageClass(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            ObjectMetadata metadata = cosClient.getObjectMetadata(bucketName, key);
            return metadata.getStorageClass();
        } catch (CosClientException e) {
            log.error("获取文件存储类型失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从URL中提取COS对象键
     */
    private String extractKeyFromUrl(String fileUrl) {
        String cleanedStaticWebsiteUrl = staticWebsiteUrl.endsWith("/")
                ? staticWebsiteUrl.substring(0, staticWebsiteUrl.length() - 1)
                : staticWebsiteUrl;
        String prefix = cleanedStaticWebsiteUrl + "/";
        return fileUrl.replace(prefix, "");
    }
}