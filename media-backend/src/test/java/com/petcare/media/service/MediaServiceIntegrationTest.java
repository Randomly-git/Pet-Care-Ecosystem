// src/test/java/com/petcare/media/service/MediaServiceIntegrationTest.java
package com.petcare.media.service;

import com.petcare.media.entity.MediaFile;
import com.petcare.media.entity.RelatedType;
import com.petcare.media.util.MediaTestUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MediaServiceIntegrationTest {

    @Autowired
    private MediaService mediaService;

    private static Long testMediaId;
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_RELATED_ID = 100L;
    private static final String TEST_UPLOAD_DIR = "src/test/resources/test-uploads";

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(Paths.get(TEST_UPLOAD_DIR));
        String testImagePath = TEST_UPLOAD_DIR + "/test-image.jpg";
        MediaTestUtils.createTestImageFile(testImagePath);
    }

    @AfterAll
    static void cleanup() {
        // 清理测试文件
        MediaTestUtils.cleanupTestFiles(TEST_UPLOAD_DIR + "/test-image.jpg");
    }

    @Test
    @Order(1)
    void testUploadUserAvatar() throws IOException {
        // 使用真实图片文件创建 MultipartFile
        MultipartFile file = MediaTestUtils.createMultipartFileFromLocal(
                TEST_UPLOAD_DIR + "/test-image.jpg",
                "user-avatar.jpg"
        );

        // 执行真实上传到腾讯云COS
        MediaFile mediaFile = mediaService.uploadMediaFile(
                file, TEST_USER_ID, "USER_AVATAR", TEST_RELATED_ID
        );

        // 验证结果
        assertNotNull(mediaFile);
        assertNotNull(mediaFile.getMediaId());
        assertEquals("user-avatar.jpg", mediaFile.getFileName());
        assertEquals(RelatedType.USER_AVATAR, mediaFile.getRelatedType());
        assertNotNull(mediaFile.getFileUrl());
        assertTrue(mediaFile.getFileUrl().contains("myqcloud.com")); // 验证是腾讯云URL

        testMediaId = mediaFile.getMediaId();
        System.out.println("✅ 真实上传宠物头像成功，mediaId: " + testMediaId);
        System.out.println("✅ 文件URL: " + mediaFile.getFileUrl());
    }

    @Test
    @Order(2)
    void testUploadActivityImage() throws IOException {
        MultipartFile file = MediaTestUtils.createMultipartFileFromLocal(
                TEST_UPLOAD_DIR + "/test-image.jpg",
                "activity-image.jpg"
        );

        MediaFile mediaFile = mediaService.uploadMediaFile(
                file, TEST_USER_ID, "ACTIVITY", 200L
        );

        assertNotNull(mediaFile);
        assertEquals("activity-image.jpg", mediaFile.getFileName());
        assertEquals(RelatedType.ACTIVITY, mediaFile.getRelatedType());
        assertTrue(mediaFile.getFileUrl().contains("myqcloud.com"));

        System.out.println("✅ 真实上传活动图片成功，mediaId: " + mediaFile.getMediaId());
        System.out.println("✅ 文件URL: " + mediaFile.getFileUrl());
    }

    @Test
    @Order(3)
    void testGetMediaFileById() {
        MediaFile mediaFile = mediaService.getMediaFileById(testMediaId);

        assertNotNull(mediaFile);
        assertEquals(testMediaId, mediaFile.getMediaId());
        assertEquals("user-avatar.jpg", mediaFile.getFileName());
        assertTrue(mediaFile.getFileUrl().contains("myqcloud.com"));

        System.out.println("✅ 查询文件信息成功: " + mediaFile.getFileName());
        System.out.println("✅ 文件URL: " + mediaFile.getFileUrl());
    }

    @Test
    @Order(4)
    void testGetUserFiles() {
        List<MediaFile> mediaFiles = mediaService.getMediaFilesByUserId(TEST_USER_ID);

        assertNotNull(mediaFiles);
        assertTrue(mediaFiles.size() >= 1);

        // 验证每个文件都有腾讯云URL
        for (MediaFile file : mediaFiles) {
            assertTrue(file.getFileUrl().contains("myqcloud.com"));
        }

        System.out.println("✅ 查询用户文件成功，数量: " + mediaFiles.size());
    }

    @Test
    @Order(5)
    void testDeleteMediaFile() {
        // 先获取文件信息用于验证
        MediaFile mediaFile = mediaService.getMediaFileById(testMediaId);
        String fileUrl = mediaFile.getFileUrl();
        assertNotNull(fileUrl);

        // 执行真实删除（从腾讯云COS和数据库）
        mediaService.deleteMediaFile(testMediaId);

        // 验证删除
        assertThrows(RuntimeException.class, () -> {
            mediaService.getMediaFileById(testMediaId);
        });

        System.out.println("✅ 真实删除文件成功，mediaId: " + testMediaId);
        System.out.println("✅ 已从腾讯云COS删除: " + fileUrl);
    }

    @Test
    @Order(6)
    void testUploadDifferentFileTypes() throws IOException {
        // 测试上传不同类型的文件
        MultipartFile imageFile = new MockMultipartFile(
                "file", "test.png", "image/png", "fake png content".getBytes()
        );

        MultipartFile videoFile = new MockMultipartFile(
                "file", "test.mp4", "video/mp4", "fake video content".getBytes()
        );

        // 测试图片上传
        MediaFile imageMedia = mediaService.uploadMediaFile(
                imageFile, TEST_USER_ID, "MOMENT", 300L
        );
        assertNotNull(imageMedia);
        assertEquals("test.png", imageMedia.getFileName());
        assertEquals("image/png", imageMedia.getFileType());

        // 测试视频上传
        MediaFile videoMedia = mediaService.uploadMediaFile(
                videoFile, TEST_USER_ID, "STATUS", 400L
        );
        assertNotNull(videoMedia);
        assertEquals("test.mp4", videoMedia.getFileName());
        assertEquals("video/mp4", videoMedia.getFileType());

        System.out.println("✅ 多文件类型上传测试成功");
    }
}