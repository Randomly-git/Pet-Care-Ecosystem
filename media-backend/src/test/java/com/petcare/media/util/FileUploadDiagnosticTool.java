// src/test/java/com/petcare/media/util/FileUploadDiagnosticTool.java
package com.petcare.media.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StopWatch;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;

@Slf4j
public class FileUploadDiagnosticTool {

    /**
     * 诊断文件上传问题
     */
    public static DiagnosticResult diagnoseUpload(MockMultipartFile file) {
        StopWatch stopWatch = new StopWatch("文件上传诊断");
        DiagnosticResult result = new DiagnosticResult();

        try {
            stopWatch.start("文件基本信息检查");
            result.setFileName(file.getOriginalFilename());
            result.setContentType(file.getContentType());
            result.setFileSize(file.getSize());
            result.setEmpty(file.isEmpty());
            stopWatch.stop();

            stopWatch.start("文件内容读取");
            byte[] content;
            try (InputStream is = file.getInputStream()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }
                content = buffer.toByteArray();
            }
            result.setActualSize(content.length);
            result.setHash(calculateHash(content));
            stopWatch.stop();

            stopWatch.start("内容分析");
            result.setFirst100Bytes(extractFirstBytes(content, 100));
            result.setSignature(identifyFileSignature(content));
            stopWatch.stop();

        } catch (Exception e) {
            result.setError(e.getMessage());
        }

        result.setExecutionTime(stopWatch.getTotalTimeMillis());
        log.info("📊 文件诊断完成: {}", result);

        return result;
    }

    private static String calculateHash(byte[] data) {
        return Integer.toHexString(java.util.Arrays.hashCode(data));
    }

    private static String extractFirstBytes(byte[] data, int count) {
        int length = Math.min(data.length, count);
        byte[] firstBytes = new byte[length];
        System.arraycopy(data, 0, firstBytes, 0, length);
        return Base64.getEncoder().encodeToString(firstBytes);
    }

    private static String identifyFileSignature(byte[] data) {
        if (data.length < 4) return "文件太小";

        // 检查常见文件签名
        if (data[0] == (byte)0xFF && data[1] == (byte)0xD8) return "JPEG";
        if (data[0] == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) return "PNG";
        if (data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46) return "GIF";
        if (data[0] == 0x25 && data[1] == 0x50 && data[2] == 0x44 && data[3] == 0x46) return "PDF";

        return "未知格式";
    }

    @lombok.Data
    public static class DiagnosticResult {
        private String fileName;
        private String contentType;
        private long fileSize;
        private long actualSize;
        private boolean empty;
        private String hash;
        private String first100Bytes;
        private String signature;
        private String error;
        private long executionTime;

        public boolean isConsistent() {
            return fileSize == actualSize && !empty;
        }

        @Override
        public String toString() {
            return String.format(
                    "File[name=%s, size=%d/%d, type=%s, hash=%s, signature=%s, consistent=%s]",
                    fileName, fileSize, actualSize, contentType, hash, signature, isConsistent()
            );
        }
    }
}
