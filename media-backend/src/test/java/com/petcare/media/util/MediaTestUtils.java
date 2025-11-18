// src/test/java/com/petcare/media/util/MediaTestUtils.java
package com.petcare.media.util;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MediaTestUtils {

    public static MultipartFile createMultipartFileFromLocal(String filePath, String originalFileName) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }

        try (FileInputStream input = new FileInputStream(file)) {
            String contentType = Files.probeContentType(Paths.get(filePath));
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return new MockMultipartFile(
                    "file",
                    originalFileName != null ? originalFileName : file.getName(),
                    contentType,
                    input
            );
        }
    }

    public static void createTestImageFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, "fake image data for testing".getBytes());
    }

    public static void cleanupTestFiles(String... filePaths) {
        for (String filePath : filePaths) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                System.err.println("删除测试文件失败: " + filePath);
            }
        }
    }
}