// service/MediaService.java - 使用本地存储版本
package com.petcare.media.service;

import com.petcare.media.entity.MediaFile;
import com.petcare.media.entity.RelatedType;
import com.petcare.media.exception.MediaServiceException;
import com.petcare.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MediaServiceLocal {

    private final MediaRepository mediaRepository;
    private final LocalStorageService localStorageService;

    // --- 核心业务逻辑方法 ---

    /**
     * 上传媒体文件 (主体方法，接收 RelatedType 枚举)
     */
    @Transactional(rollbackFor = Exception.class)
    public MediaFile uploadMediaFile(MultipartFile file, Long userId,
                                     RelatedType relatedType, Long relatedId) {
        // 1. 验证文件
        if (file.isEmpty()) {
            throw new MediaServiceException("文件不能为空");
        }

        try {
            log.info("📁 文件信息 - 文件名: {}, 大小: {}, 类型: {}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            // 2. 确定存储路径
            String filePath = generateFilePath(relatedType, userId, relatedId);
            log.info("📍 生成的文件路径: {}", filePath);

            // 3. 上传到本地存储
            String fileUrl = localStorageService.uploadFile(file, filePath);

            // 4. 创建媒体文件记录
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFileName(file.getOriginalFilename());
            mediaFile.setFileUrl(fileUrl);

            String fileType = file.getContentType();
            if (fileType == null || fileType.isEmpty()) {
                fileType = determineFileType(file.getOriginalFilename());
            }
            mediaFile.setFileType(fileType);
            mediaFile.setFileSize(file.getSize());
            mediaFile.setUserId(userId);
            mediaFile.setRelatedType(relatedType);
            mediaFile.setRelatedId(relatedId);

            // 5. 保存到数据库
            MediaFile savedMediaFile = mediaRepository.save(mediaFile);
            log.info("✅ 媒体文件上传成功: ID={}, URL={}", savedMediaFile.getMediaId(), fileUrl);

            return savedMediaFile;

        } catch (MediaServiceException e) {
            // 重新抛出业务异常 (例如：文件不存在或上传失败)
            log.error("文件上传失败: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            // 捕获 上传服务 中抛出的 IOException，转换为 MediaServiceException
            log.error("文件上传 IO 异常: {}", e.getMessage(), e);
            throw new MediaServiceException("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            // 捕获其他所有异常，记录详细日志
            log.error("文件上传过程中发生未知异常: {}", e.getMessage(), e);
            throw new MediaServiceException("系统异常，文件上传失败");
        }
    }

    /**
     * 接收字符串类型的 Type，转换后调用主体方法
     */
    public MediaFile uploadMediaFile(MultipartFile file, Long userId,
                                     String type, Long relatedId) {
        RelatedType relatedType;
        try {
            relatedType = RelatedType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            relatedType = RelatedType.MOMENT; // 默认为 MOMENT 类型
        }
        return uploadMediaFile(file, userId, relatedType, relatedId);
    }

    /**
     * 根据 ID 获取媒体文件信息
     */
    public MediaFile getMediaFileById(Long id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new MediaServiceException("媒体文件不存在"));
    }

    /**
     * 获取用户的所有媒体文件
     */
    public List<MediaFile> getMediaFilesByUserId(Long userId) {
        return mediaRepository.findByUserIdOrderByUploadTimeDesc(userId);
    }

    /**
     * 获取指定关联的所有媒体文件
     */
    public List<MediaFile> getMediaFilesByRelatedId(RelatedType relatedType, Long relatedId) {
        return mediaRepository.findByRelatedTypeAndRelatedId(relatedType, relatedId);
    }

    /**
     * 批量获取指定关联的媒体文件
     */
    public List<MediaFile> getMediaFilesByRelatedIds(RelatedType relatedType, List<Long> relatedIds) {
        return mediaRepository.findByRelatedTypeAndRelatedIdIn(relatedType, relatedIds);
    }

    /**
     * 删除媒体文件
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMediaFile(Long id) {
        MediaFile mediaFile = getMediaFileById(id);
        try {
            // 1. 删除本地存储文件
            localStorageService.deleteFile(mediaFile.getFileUrl());

            // 2. 删除数据库记录
            mediaRepository.delete(mediaFile);
            log.info("✅ 媒体文件删除成功: ID={}, URL={}", id, mediaFile.getFileUrl());
            return true;

        } catch (Exception e) {
            log.error("❌ 删除媒体文件失败: ID={}, 错误: {}", id, e.getMessage(), e);
            return false;
        }
    }

    // --- 私有辅助方法 ---

    /**
     * 生成文件存储路径
     */
    private String generateFilePath(RelatedType relatedType, Long userId, Long relatedId) {
        if (relatedId != null) {
            return String.format("%s/%d/%d", relatedType.name().toLowerCase(), userId, relatedId);
        } else {
            return String.format("%s/%d", relatedType.name().toLowerCase(), userId);
        }
    }

    /**
     * 根据文件名确定文件类型
     */
    private String determineFileType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String extension = fileName.toLowerCase();
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg") || extension.endsWith(".png")) {
            return "image/jpeg";
        } else if (extension.endsWith(".gif")) {
            return "image/gif";
        } else if (extension.endsWith(".mp4")) {
            return "video/mp4";
        } else if (extension.endsWith(".pdf")) {
            return "application/pdf";
        } else {
            return "application/octet-stream";
        }
    }

    // 缺失的方法，为Controller提供支持

    /**
     * 根据关联类型和关联ID获取媒体文件（字符串参数版本）
     */
    public List<MediaFile> getMediaFilesByRelated(String relatedType, Long relatedId) {
        try {
            RelatedType type = RelatedType.valueOf(relatedType.toUpperCase());
            return getMediaFilesByRelatedId(type, relatedId);
        } catch (IllegalArgumentException e) {
            log.error("无效的RelatedType: {}", relatedType);
            return List.of();
        }
    }

    /**
     * 根据关联类型和关联ID删除媒体文件
     */
    public boolean deleteMediaFilesByRelated(String relatedType, Long relatedId) {
        try {
            RelatedType type = RelatedType.valueOf(relatedType.toUpperCase());
            List<MediaFile> mediaFiles = mediaRepository.findByRelatedTypeAndRelatedId(type, relatedId);

            for (MediaFile mediaFile : mediaFiles) {
                deleteMediaFile(mediaFile.getMediaId());
            }

            return true;
        } catch (IllegalArgumentException e) {
            log.error("无效的RelatedType: {}", relatedType);
            return false;
        }
    }

    /**
     * 获取所有有效的关联类型
     */
    public List<String> getValidRelatedTypes() {
        return Arrays.stream(RelatedType.values())
                .map(RelatedType::name)
                .collect(Collectors.toList());
    }

    /**
     * 批量更新关联ID
     */
    public int batchUpdateRelatedId(List<Long> mediaIds, String relatedType, Long relatedId) {
        try {
            RelatedType type = RelatedType.valueOf(relatedType.toUpperCase());
            int updatedCount = 0;

            for (Long mediaId : mediaIds) {
                MediaFile mediaFile = mediaRepository.findById(mediaId).orElse(null);
                if (mediaFile != null) {
                    mediaFile.setRelatedType(type);
                    mediaFile.setRelatedId(relatedId);
                    mediaRepository.save(mediaFile);
                    updatedCount++;
                }
            }

            log.info("✅ 批量更新关联ID完成: 更新了{}个媒体文件", updatedCount);
            return updatedCount;
        } catch (IllegalArgumentException e) {
            log.error("无效的RelatedType: {}", relatedType);
            return 0;
        }
    }
}