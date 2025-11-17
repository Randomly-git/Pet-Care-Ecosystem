// service/MediaService.java
package com.petcare.media.service;

import com.petcare.media.entity.MediaFile;
import com.petcare.media.entity.RelatedType;
import com.petcare.media.repository.MediaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MediaService {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private CosStorageService cosStorageService;

    /**
     * 上传媒体文件
     */
    public MediaFile uploadMediaFile(MultipartFile file, Long petId,
                                     RelatedType relatedType, Long relatedId) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                throw new RuntimeException("文件不能为空");
            }

            // 确定存储路径
            String filePath = generateFilePath(relatedType, petId, relatedId);

            // 上传到腾讯云COS
            String fileUrl = cosStorageService.uploadFile(file, filePath);

            // 创建媒体文件记录
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFileName(file.getOriginalFilename());
            mediaFile.setFileUrl(fileUrl);
            mediaFile.setFileType(file.getContentType());
            mediaFile.setFileSize(file.getSize());
            mediaFile.setPetId(petId);
            mediaFile.setRelatedType(relatedType);
            mediaFile.setRelatedId(relatedId);

            MediaFile savedFile = mediaRepository.save(mediaFile);
            log.info("媒体文件上传成功: mediaId={}, fileUrl={}", savedFile.getId(), fileUrl);

            return savedFile;

        } catch (IOException e) {
            log.error("文件上传IO异常: {}", e.getMessage(), e);
            throw new RuntimeException("文件处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传媒体文件（字符串类型参数）
     */
    public MediaFile uploadMediaFile(MultipartFile file, Long petId,
                                     String relatedTypeStr, Long relatedId) {
        try {
            RelatedType relatedType = RelatedType.valueOf(relatedTypeStr.toUpperCase());
            return uploadMediaFile(file, petId, relatedType, relatedId);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的关联类型: " + relatedTypeStr +
                    "，有效值: ACTIVITY, STATUS, MOMENT, PET_AVATAR");
        }
    }

    /**
     * 根据ID获取媒体文件信息
     */
    public MediaFile getMediaFileById(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("媒体文件不存在, ID: " + mediaId));
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
            throw new RuntimeException("无效的关联类型: " + relatedTypeStr);
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
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除特定业务记录的所有媒体文件
     */
    public void deleteMediaFilesByRelated(RelatedType relatedType, Long relatedId) {
        List<MediaFile> mediaFiles = getMediaFilesByRelated(relatedType, relatedId);

        for (MediaFile mediaFile : mediaFiles) {
            try {
                // 从腾讯云COS删除文件
                cosStorageService.deleteFile(mediaFile.getFileUrl());
            } catch (Exception e) {
                log.warn("删除云存储文件失败: {}", mediaFile.getFileUrl(), e);
                // 继续删除其他文件，不中断
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
            throw new RuntimeException("无效的关联类型: " + relatedTypeStr);
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
        return List.of(
                RelatedType.ACTIVITY.name(),
                RelatedType.STATUS.name(),
                RelatedType.MOMENT.name(),
                RelatedType.PET_AVATAR.name()
        );
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