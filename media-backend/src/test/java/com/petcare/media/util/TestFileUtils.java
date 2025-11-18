// test/java/com/petcare/media/util/TestFileUtils.java
package com.petcare.media.util;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestFileUtils {

    public static MultipartFile createTestImageFile() throws IOException {
        // 创建虚拟图片文件内容
        byte[] imageData = new byte[1024]; // 1KB的虚拟数据
        for (int i = 0; i < imageData.length; i++) {
            imageData[i] = (byte) (i % 256);
        }

        return new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                imageData
        );
    }

    public static MultipartFile createTestTextFile() throws IOException {
        String textContent = "这是一个测试文本文件内容";

        return new MockMultipartFile(
                "file",
                "test-document.txt",
                "text/plain",
                textContent.getBytes()
        );
    }

    public static MultipartFile createTestPdfFile() throws IOException {
        // 创建虚拟PDF文件内容（简单的PDF头）
        byte[] pdfData = "%PDF-1.4\n%测试PDF文件".getBytes();

        return new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                pdfData
        );
    }

    public static MultipartFile createEmptyFile() {
        return new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );
    }
}