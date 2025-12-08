package com.petcare.media.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class LocalStorageService {

    @Value("${app.storage.local.path:/tmp/uploads}")
    private String storagePath;

    public String uploadFile(MultipartFile file, String filePath) throws IOException {
        // 创建存储目录
        Path uploadPath = Paths.get(storagePath, filePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 获取原始文件名和扩展名
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // 生成唯一的文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = timestamp + "_" + UUID.randomUUID().toString() + fileExtension;

        // 保存文件
        Path targetPath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath);

        // 返回文件访问路径（实际应用中应该返回URL）
        String fileUrl = "/uploads/" + filePath + "/" + fileName;
        log.info("文件上传成功到本地: {}", targetPath.toAbsolutePath());

        return fileUrl;
    }

    public void deleteFile(String fileUrl) {
        try {
            // 从URL中提取文件路径
            if (fileUrl.startsWith("/uploads/")) {
                String relativePath = fileUrl.substring(9); // 移除 "/uploads/" 前缀
                Path filePath = Paths.get(storagePath, relativePath);
                Files.deleteIfExists(filePath);
                log.info("文件删除成功: {}", filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("删除文件失败: {}", fileUrl, e);
        }
    }
}