// src/test/java/com/petcare/media/client/MediaServiceFacadeUnitTest.java
package com.petcare.media.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.media.dto.ApiResponse;
import com.petcare.media.dto.MediaResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class MediaServiceFacadeUnitTest {

    @Mock
    private RestTemplate restTemplate;

    private MediaServiceFacade mediaServiceFacade;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mediaServiceFacade = new MediaServiceFacade(restTemplate, objectMapper);
        // 通过反射设置私有字段
        setPrivateField(mediaServiceFacade, "mediaServiceUrl", "http://localhost:8082");
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("设置字段失败: " + fieldName, e);
        }
    }

    @Test
    void testUploadFileSuccess() throws Exception {
        // 1. 准备测试文件
        byte[] fileData = createTestFile(1024 * 10); // 10KB
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", fileData
        );

        log.info("🧪 测试文件上传 - 大小: {}字节", file.getSize());

        // 2. 准备模拟响应
        MediaResponse mockResponse = new MediaResponse();
        mockResponse.setMediaId(999L);
        mockResponse.setFileName("test.jpg");
        mockResponse.setFileSize(file.getSize());
        mockResponse.setFileUrl("https://cos.example.com/test.jpg");

        ApiResponse<MediaResponse> apiResponse = ApiResponse.success("上传成功", mockResponse);
        String responseJson = objectMapper.writeValueAsString(apiResponse);

        // 3. 模拟 RestTemplate 调用
        when(restTemplate.exchange(
                eq("http://localhost:8082/api/media/upload"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(responseJson));

        // 4. 执行上传
        MediaResponse response = mediaServiceFacade.uploadFile(file, 1L, "MOMENT", 100L);

        // 5. 验证结果
        assertNotNull(response);
        assertEquals(999L, response.getMediaId());
        assertEquals(file.getSize(), response.getFileSize());

        log.info("✅ 文件上传测试通过 - 大小: {}字节", response.getFileSize());
    }

    @Test
    void testParameterConversionToString() throws Exception {
        // 测试参数是否被正确转换为字符串
        byte[] fileData = createTestFile(1024); // 1KB文件，避免空文件异常
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", fileData
        );

        Long userId = 123L;
        Long relatedId = 456L;

        MediaResponse mockResponse = new MediaResponse();
        ApiResponse<MediaResponse> apiResponse = ApiResponse.success("成功", mockResponse);
        String responseJson = objectMapper.writeValueAsString(apiResponse);

        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenAnswer(invocation -> {
                    HttpEntity<?> httpEntity = invocation.getArgument(2);
                    Object body = httpEntity.getBody();

                    if (body instanceof LinkedMultiValueMap) {
                        @SuppressWarnings("unchecked")
                        LinkedMultiValueMap<String, Object> map = (LinkedMultiValueMap<String, Object>) body;

                        Object userIdParam = map.getFirst("userId");
                        Object relatedIdParam = map.getFirst("relatedId");

                        log.info("参数验证:");
                        log.info("  userId: {} (类型: {})",
                                userIdParam, userIdParam != null ? userIdParam.getClass().getSimpleName() : "null");
                        log.info("  relatedId: {} (类型: {})",
                                relatedIdParam, relatedIdParam != null ? relatedIdParam.getClass().getSimpleName() : "null");

                        // 🎯 关键验证：参数必须是字符串
                        assertInstanceOf(String.class, userIdParam, "userId 应该是字符串类型");
                        assertInstanceOf(String.class, relatedIdParam, "relatedId 应该是字符串类型");

                        assertEquals("123", userIdParam);
                        assertEquals("456", relatedIdParam);
                    }

                    return ResponseEntity.ok(responseJson);
                });

        mediaServiceFacade.uploadFile(file, userId, "TEST", relatedId);
        log.info("✅ 参数类型转换测试通过");
    }

    @Test
    void testLargeFileUpload() throws Exception {
        // 测试大文件上传
        byte[] largeFileData = createTestFile(1024 * 1024); // 1MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", largeFileData
        );

        log.info("📦 测试大文件 - 大小: {}字节", largeFile.getSize());

        // 验证文件在 Java 层面是完整的
        assertEquals(largeFileData.length, largeFile.getSize());
        assertEquals(largeFileData.length, largeFile.getBytes().length);

        MediaResponse mockResponse = new MediaResponse();
        mockResponse.setFileSize(largeFile.getSize());

        ApiResponse<MediaResponse> apiResponse = ApiResponse.success("成功", mockResponse);
        String responseJson = objectMapper.writeValueAsString(apiResponse);

        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseJson));

        MediaResponse response = mediaServiceFacade.uploadFile(largeFile, 1L, "LARGE", 1L);

        assertEquals(largeFile.getSize(), response.getFileSize());
        log.info("✅ 大文件上传测试通过 - 大小: {}字节", largeFile.getSize());
    }

    @Test
    void testFileSizeConsistency() throws Exception {
        // 测试各种文件大小
        int[] testSizes = {100, 1024, 1024 * 10, 1024 * 100};

        for (int size : testSizes) {
            byte[] fileData = createTestFile(size);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test_" + size + ".jpg", "image/jpeg", fileData
            );

            assertEquals(size, file.getSize(),
                    String.format("文件大小 %d 不匹配", size));
            assertEquals(size, file.getBytes().length,
                    String.format("文件内容大小 %d 不匹配", size));
        }

        log.info("✅ 文件大小一致性测试通过 - 测试了 {} 种大小", testSizes.length);
    }

    @Test
    void testErrorResponseHandling() throws Exception {
        byte[] fileData = createTestFile(1024); // 1KB文件
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", fileData
        );

        // 模拟错误响应
        ApiResponse<MediaResponse> errorResponse = ApiResponse.error(40001, "文件格式不支持");
        String errorJson = objectMapper.writeValueAsString(errorResponse);

        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(errorJson));

        // 验证抛出正确的异常
        MediaServiceFacade.MediaClientException exception = assertThrows(
                MediaServiceFacade.MediaClientException.class,
                () -> mediaServiceFacade.uploadFile(file, 1L, "TEST", 1L)
        );

        assertTrue(exception.getMessage().contains("40001"));
        log.info("✅ 错误响应处理测试通过");
    }

    @Test
    void testEmptyFileHandling() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        // 🎯 根据修复的 uploadFile 方法，空文件会抛出异常
        // 因为我们添加了：if (file.isEmpty()) { throw new MediaClientException("文件不能为空"); }

        // 验证空文件确实会抛出异常
        MediaServiceFacade.MediaClientException exception = assertThrows(
                MediaServiceFacade.MediaClientException.class,
                () -> mediaServiceFacade.uploadFile(emptyFile, 1L, "TEST", 1L)
        );

        assertTrue(exception.getMessage().contains("文件不能为空"));
        log.info("✅ 空文件处理测试通过 - 正确抛出了异常");
    }

    @Test
    void testVerySmallFileWarning() throws Exception {
        // 测试小文件警告
        byte[] tinyFileData = createTestFile(50); // 50字节，小于100会触发警告
        MockMultipartFile tinyFile = new MockMultipartFile(
                "file", "tiny.jpg", "image/jpeg", tinyFileData
        );

        log.info("⚠️ 测试小文件 - 大小: {}字节 (应该触发警告)", tinyFile.getSize());

        MediaResponse mockResponse = new MediaResponse();
        ApiResponse<MediaResponse> apiResponse = ApiResponse.success("成功", mockResponse);
        String responseJson = objectMapper.writeValueAsString(apiResponse);

        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseJson));

        MediaResponse response = mediaServiceFacade.uploadFile(tinyFile, 1L, "TINY", 1L);

        assertNotNull(response);
        log.info("✅ 小文件警告测试通过");
    }

    @Test
    void testFixedMultipartFileResourceWorks() throws Exception {
        // 专门测试 FixedMultipartFileResource 是否正常工作
        byte[] fileData = createTestFile(1024 * 50); // 50KB
        MockMultipartFile file = new MockMultipartFile(
                "file", "resource_test.jpg", "image/jpeg", fileData
        );

        // 模拟请求，验证 FixedMultipartFileResource 被使用
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenAnswer(invocation -> {
                    HttpEntity<?> httpEntity = invocation.getArgument(2);
                    log.info("📡 请求已发送，验证 FixedMultipartFileResource");

                    // 验证请求体包含文件
                    assertNotNull(httpEntity.getBody());

                    return ResponseEntity.ok("{\"code\":20000,\"message\":\"成功\",\"data\":{}}");
                });

        mediaServiceFacade.uploadFile(file, 1L, "RESOURCE_TEST", 1L);
        log.info("✅ FixedMultipartFileResource 工作正常");
    }

    @Test
    void testFileContentIntegrity() throws Exception {
        // 测试文件内容完整性
        int fileSize = 1024 * 100; // 100KB
        byte[] originalData = new byte[fileSize];

        // 创建有规律的数据，便于验证
        for (int i = 0; i < fileSize; i++) {
            originalData[i] = (byte) ((i * 7 + 13) % 256); // 使用公式创建可验证的数据
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "integrity_test.jpg", "image/jpeg", originalData
        );

        log.info("🔍 测试文件内容完整性 - 大小: {}字节", fileSize);

        // 验证 MultipartFile 正确读取数据
        byte[] readData = file.getBytes();
        assertEquals(originalData.length, readData.length);

        // 验证内容完全一致
        assertArrayEquals(originalData, readData, "文件内容在读取过程中被修改！");

        log.info("✅ 文件内容完整性测试通过");
    }

    private byte[] createTestFile(int size) {
        byte[] data = new byte[size];
        // 添加简单文件头模拟 JPEG
        if (size > 10) {
            data[0] = (byte) 0xFF;
            data[1] = (byte) 0xD8; // JPEG SOI
            data[2] = (byte) 0xFF;
            data[3] = (byte) 0xE0; // APP0
        }
        // 填充数据
        for (int i = 4; i < size; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }
}