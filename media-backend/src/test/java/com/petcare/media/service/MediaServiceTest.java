// test/java/com/petcare/media/service/MediaServiceTest.java
package com.petcare.media.service;

import com.petcare.media.entity.MediaFile;
import com.petcare.media.entity.RelatedType;
import com.petcare.media.repository.MediaRepository;
import com.petcare.media.util.TestFileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private CosStorageService cosStorageService;

    @Mock
    private MediaRepository mediaRepository;

    @InjectMocks
    private MediaService mediaService;

    private MultipartFile testImageFile;
    private MediaFile sampleMediaFile;

    @BeforeEach
    void setUp() throws IOException {
        testImageFile = TestFileUtils.createTestImageFile();

        sampleMediaFile = new MediaFile();
        sampleMediaFile.setId(1L);
        sampleMediaFile.setFileName("test-image.jpg");
        sampleMediaFile.setFileUrl("https://test-bucket.cos.ap-test.myqcloud.com/test/path/test-image.jpg");
        sampleMediaFile.setFileType("image/jpeg");
        sampleMediaFile.setFileSize(1024L);
        sampleMediaFile.setPetId(1L);
        sampleMediaFile.setRelatedType(RelatedType.ACTIVITY);
        sampleMediaFile.setRelatedId(100L);
    }

    @Test
    void uploadMediaFile_Success() throws IOException {
        // 准备
        when(cosStorageService.uploadFile(any(MultipartFile.class), anyString()))
                .thenReturn("https://test-bucket.cos.ap-test.myqcloud.com/test/path/test-image.jpg");
        when(mediaRepository.save(any(MediaFile.class))).thenReturn(sampleMediaFile);

        // 执行
        MediaFile result = mediaService.uploadMediaFile(testImageFile, 1L, RelatedType.ACTIVITY, 100L);

        // 验证
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test-image.jpg", result.getFileName());
        assertEquals(RelatedType.ACTIVITY, result.getRelatedType());
        assertEquals(100L, result.getRelatedId());

        verify(cosStorageService, times(1)).uploadFile(any(MultipartFile.class), anyString());
        verify(mediaRepository, times(1)).save(any(MediaFile.class));
    }

    @Test
    void uploadMediaFile_WithStringType_Success() throws IOException {
        // 准备
        when(cosStorageService.uploadFile(any(MultipartFile.class), anyString()))
                .thenReturn("https://test-bucket.cos.ap-test.myqcloud.com/test/path/test-image.jpg");
        when(mediaRepository.save(any(MediaFile.class))).thenReturn(sampleMediaFile);

        // 执行
        MediaFile result = mediaService.uploadMediaFile(testImageFile, 1L, "ACTIVITY", 100L);

        // 验证
        assertNotNull(result);
        assertEquals(RelatedType.ACTIVITY, result.getRelatedType());
    }

    @Test
    void uploadMediaFile_InvalidRelatedType_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(RuntimeException.class, () -> {
            mediaService.uploadMediaFile(testImageFile, 1L, "INVALID_TYPE", 100L);
        });

        assertTrue(exception.getMessage().contains("无效的关联类型"));
    }

    @Test
    void uploadMediaFile_EmptyFile_ThrowsException() throws IOException {
        // 准备
        MultipartFile emptyFile = TestFileUtils.createEmptyFile();

        // 执行 & 验证
        Exception exception = assertThrows(RuntimeException.class, () -> {
            mediaService.uploadMediaFile(emptyFile, 1L, RelatedType.ACTIVITY, 100L);
        });

        assertTrue(exception.getMessage().contains("文件不能为空"));
    }

    @Test
    void getMediaFileById_Success() {
        // 准备
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(sampleMediaFile));

        // 执行
        MediaFile result = mediaService.getMediaFileById(1L);

        // 验证
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(mediaRepository, times(1)).findById(1L);
    }

    @Test
    void getMediaFileById_NotFound_ThrowsException() {
        // 准备
        when(mediaRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行 & 验证
        Exception exception = assertThrows(RuntimeException.class, () -> {
            mediaService.getMediaFileById(999L);
        });

        assertTrue(exception.getMessage().contains("媒体文件不存在"));
        verify(mediaRepository, times(1)).findById(999L);
    }

    @Test
    void getMediaFilesByPetId_Success() {
        // 准备
        List<MediaFile> expectedFiles = Arrays.asList(sampleMediaFile);
        when(mediaRepository.findByPetId(1L)).thenReturn(expectedFiles);

        // 执行
        List<MediaFile> result = mediaService.getMediaFilesByPetId(1L);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(mediaRepository, times(1)).findByPetId(1L);
    }

    @Test
    void getMediaFilesByRelated_Success() {
        // 准备
        List<MediaFile> expectedFiles = Arrays.asList(sampleMediaFile);
        when(mediaRepository.findByRelatedTypeAndRelatedId(RelatedType.ACTIVITY, 100L))
                .thenReturn(expectedFiles);

        // 执行
        List<MediaFile> result = mediaService.getMediaFilesByRelated(RelatedType.ACTIVITY, 100L);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(RelatedType.ACTIVITY, result.get(0).getRelatedType());
        verify(mediaRepository, times(1)).findByRelatedTypeAndRelatedId(RelatedType.ACTIVITY, 100L);
    }

    @Test
    void deleteMediaFile_Success() {
        // 准备
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(sampleMediaFile));
        doNothing().when(cosStorageService).deleteFile(anyString());
        doNothing().when(mediaRepository).deleteById(1L);

        // 执行
        mediaService.deleteMediaFile(1L);

        // 验证
        verify(cosStorageService, times(1)).deleteFile(sampleMediaFile.getFileUrl());
        verify(mediaRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteMediaFilesByRelated_Success() {
        // 准备
        List<MediaFile> mediaFiles = Arrays.asList(sampleMediaFile);
        when(mediaRepository.findByRelatedTypeAndRelatedId(RelatedType.ACTIVITY, 100L))
                .thenReturn(mediaFiles);
        when(mediaRepository.deleteByRelatedTypeAndRelatedId(RelatedType.ACTIVITY, 100L))
                .thenReturn(1);

        // 执行
        mediaService.deleteMediaFilesByRelated(RelatedType.ACTIVITY, 100L);

        // 验证
        verify(cosStorageService, times(1)).deleteFile(sampleMediaFile.getFileUrl());
        verify(mediaRepository, times(1)).deleteByRelatedTypeAndRelatedId(RelatedType.ACTIVITY, 100L);
    }

    @Test
    void updateMediaFileInfo_Success() {
        // 准备
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(sampleMediaFile));
        when(mediaRepository.save(any(MediaFile.class))).thenReturn(sampleMediaFile);

        // 执行
        MediaFile result = mediaService.updateMediaFileInfo(1L, "new-filename.jpg");

        // 验证
        assertNotNull(result);
        assertEquals("new-filename.jpg", result.getFileName());
        verify(mediaRepository, times(1)).save(any(MediaFile.class));
    }

    @Test
    void countMediaFilesByPetId_Success() {
        // 准备
        when(mediaRepository.countByPetId(1L)).thenReturn(5L);

        // 执行
        Long count = mediaService.countMediaFilesByPetId(1L);

        // 验证
        assertEquals(5L, count);
        verify(mediaRepository, times(1)).countByPetId(1L);
    }

    @Test
    void getValidRelatedTypes_Success() {
        // 执行
        List<String> types = mediaService.getValidRelatedTypes();

        // 验证
        assertNotNull(types);
        assertEquals(4, types.size());
        assertTrue(types.contains("ACTIVITY"));
        assertTrue(types.contains("STATUS"));
        assertTrue(types.contains("MOMENT"));
        assertTrue(types.contains("PET_AVATAR"));
    }
}