// service/MediaService.java
package com.petcare.media.service;

import com.petcare.media.entity.MediaFile;
import com.petcare.media.entity.RelatedType;
import com.petcare.media.exception.MediaServiceException;
import com.petcare.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor; // 使用 RequiredArgsConstructor 替代 @Autowired
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor // 推荐使用构造器注入，配合 final 字段
public class MediaService {

    private final MediaRepository mediaRepository; // 使用 final 字段
    private final CosStorageService cosStorageService; // 使用 final 字段

    // --- 核心业务逻辑方法 ---

    /**
     * 上传媒体文件 (主体方法，接收 RelatedType 枚举)
     */
    @Transactional(rollbackFor = Exception.class)
    public MediaFile uploadMediaFile(MultipartFile file, Long petId,
                                     RelatedType relatedType, Long relatedId) {
        // 1. 验证文件
        if (file.isEmpty()) {
            throw new MediaServiceException("文件不能为空");
        }

        try {
            log.info("📁 文件信息 - 文件名: {}, 大小: {}, 类型: {}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            // 2. 确定存储路径
            String filePath = generateFilePath(relatedType, petId, relatedId);
            log.info("📍 生成的文件路径: {}", filePath);

            // 3. 上传到腾讯云COS
            String fileUrl = cosStorageService.uploadFile(file, filePath);

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
            mediaFile.setPetId(petId);
            mediaFile.setRelatedType(relatedType);
            mediaFile.setRelatedId(relatedId);

            MediaFile savedFile = mediaRepository.save(mediaFile);
            log.info("媒体文件上传成功: mediaId={}, fileUrl={}", savedFile.getMediaId(), fileUrl);

            return savedFile;

        } catch (IOException e) {
            log.error("文件读写错误: {}", e.getMessage(), e);
            // 抛出 500xx 系统错误码
            throw new MediaServiceException(50001, "文件处理失败，请稍后重试");
        } catch (MediaServiceException e) {
            // 重新抛出业务异常 (例如：文件不存在或COS上传失败，如果CosStorageService抛出的是MediaServiceException)
            throw e;
        } catch (Exception e) {
            log.error("文件上传未知错误: {}", e.getMessage(), e);
            // 捕获 CosStorageService 中抛出的 RuntimeException，转换为 MediaServiceException
            throw new MediaServiceException(50000, "文件上传失败，请联系管理员");
        }
    }


    /**
     * 上传媒体文件（Controller 调用方法，接收 String 类型参数）
     */
    public MediaFile uploadMediaFile(MultipartFile file, Long petId,
                                     String relatedTypeStr, Long relatedId) {
        try {
            String cleanedType = relatedTypeStr.toUpperCase().trim();
            // 解决局部变量冲突，将 RelatedType.valueOf 结果赋给临时变量
            RelatedType type = RelatedType.valueOf(cleanedType);

            // 正确调用主体方法
            return uploadMediaFile(file, petId, type, relatedId);

        } catch (IllegalArgumentException e) {
            log.error("❌ 枚举转换失败 - 输入: '{}', 错误: {}", relatedTypeStr, e.getMessage());
            throw new MediaServiceException("无效的关联类型: " + relatedTypeStr +
                    "，有效值: " + String.join(", ", getValidRelatedTypes()));
        }
    }

    /**
     * 根据ID获取媒体文件信息
     */
    public MediaFile getMediaFileById(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaServiceException("媒体文件不存在, ID: " + mediaId));
    }

    /**
     * 获取宠物的所有媒体文件
     */
    public List<MediaFile> getMediaFilesByPetId(Long petId) {
        return mediaRepository.findByPetId(petId);
    }

    /**
     * 获取特定业务记录的媒体文件
     */
    public List<MediaFile> getMediaFilesByRelated(RelatedType relatedType, Long relatedId) {
        return mediaRepository.findByRelatedTypeAndRelatedId(relatedType, relatedId);
    }

    /**
     * 获取特定业务记录的媒体文件（字符串类型参数）
     */
    public List<MediaFile> getMediaFilesByRelated(String relatedTypeStr, Long relatedId) {
        try {
            RelatedType relatedType = RelatedType.valueOf(relatedTypeStr.toUpperCase());
            return getMediaFilesByRelated(relatedType, relatedId);
        } catch (IllegalArgumentException e) {
            throw new MediaServiceException("无效的关联类型: " + relatedTypeStr);
        }
    }

    /**
     * 获取特定业务记录的图片文件
     */
    public List<MediaFile> getImageFilesByRelated(RelatedType relatedType, Long relatedId) {
        return mediaRepository.findByRelatedTypeAndRelatedIdAndFileTypeLike(
                relatedType, relatedId, "image/%");
    }

    /**
     * 获取特定业务记录的视频文件
     */
    public List<MediaFile> getVideoFilesByRelated(RelatedType relatedType, Long relatedId) {
        return mediaRepository.findByRelatedTypeAndRelatedIdAndFileTypeLike(
                relatedType, relatedId, "video/%");
    }

    /**
     * 删除单个媒体文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMediaFile(Long mediaId) {
        MediaFile mediaFile = getMediaFileById(mediaId);

        try {
            // 从腾讯云COS删除文件
            cosStorageService.deleteFile(mediaFile.getFileUrl());

            // 从数据库删除记录
            mediaRepository.deleteById(mediaId);

            log.info("媒体文件删除成功: mediaId={}", mediaId);

        } catch (Exception e) {
            log.error("媒体文件删除失败: mediaId={}", mediaId, e);
            // 统一抛出 MediaServiceException
            throw new MediaServiceException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除特定业务记录的所有媒体文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMediaFilesByRelated(RelatedType relatedType, Long relatedId) {
        List<MediaFile> mediaFiles = getMediaFilesByRelated(relatedType, relatedId);

        for (MediaFile mediaFile : mediaFiles) {
            try {
                // 从腾讯云COS删除文件
                cosStorageService.deleteFile(mediaFile.getFileUrl());
            } catch (Exception e) {
                log.warn("删除云存储文件失败，事务回滚: {}", mediaFile.getFileUrl(), e);
                // 统一抛出 MediaServiceException，确保事务能正确回滚
                throw new MediaServiceException(50002, "删除云存储文件失败，事务已回滚: " + e.getMessage());
            }
        }

        // 从数据库批量删除记录
        int deletedCount = mediaRepository.deleteByRelatedTypeAndRelatedId(relatedType, relatedId);
        log.info("删除业务记录关联的媒体文件成功: relatedType={}, relatedId={}, deletedCount={}",
                relatedType, relatedId, deletedCount);
    }

    /**
     * 删除特定业务记录的所有媒体文件（字符串类型参数）
     */
    public void deleteMediaFilesByRelated(String relatedTypeStr, Long relatedId) {
        try {
            RelatedType relatedType = RelatedType.valueOf(relatedTypeStr.toUpperCase());
            deleteMediaFilesByRelated(relatedType, relatedId);
        } catch (IllegalArgumentException e) {
            throw new MediaServiceException("无效的关联类型: " + relatedTypeStr); // ⚡️ 修正为 MediaServiceException
        }
    }

    /**
     * 更新媒体文件信息（如文件名）
     */
    public MediaFile updateMediaFileInfo(Long mediaId, String newFileName) {
        MediaFile mediaFile = getMediaFileById(mediaId);
        mediaFile.setFileName(newFileName);
        return mediaRepository.save(mediaFile);
    }

    /**
     * 统计宠物的媒体文件数量
     */
    public Long countMediaFilesByPetId(Long petId) {
        return mediaRepository.countByPetId(petId);
    }

    /**
     * 获取所有有效的关联类型
     */
    public List<String> getValidRelatedTypes() {
        return Arrays.stream(RelatedType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    // --- 内部工具方法 ---

    /**
     * 根据文件名推断文件类型
     */
    private String determineFileType(String fileName) {
        if (fileName == null) return "application/octet-stream";

        String lowerFileName = fileName.toLowerCase();
        // 简化判断逻辑
        if (lowerFileName.endsWith(".png")) return "image/png";
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerFileName.endsWith(".gif")) return "image/gif";
        if (lowerFileName.endsWith(".webp")) return "image/webp";

        if (lowerFileName.endsWith(".mp4")) return "video/mp4";
        if (lowerFileName.endsWith(".mov")) return "video/quicktime";
        if (lowerFileName.endsWith(".avi")) return "video/avi";

        if (lowerFileName.endsWith(".pdf")) return "application/pdf";
        if (lowerFileName.endsWith(".doc") || lowerFileName.endsWith(".docx")) return "application/msword";

        return "application/octet-stream"; // 默认类型
    }

    /**
     * 生成文件存储路径
     */
    private String generateFilePath(RelatedType relatedType, Long petId, Long relatedId) {
        return String.format("%s/pet_%d/%s_%d",
                relatedType.name().toLowerCase(),
                petId,
                relatedType.name().toLowerCase(),
                relatedId);
    }
}