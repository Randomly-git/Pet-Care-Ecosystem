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
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor // 推荐使用构造器注入，配合 final 字段
@CacheConfig(cacheNames = "media") // 统一缓存前缀
public class MediaService {

    private final MediaRepository mediaRepository; // 使用 final 字段
    private final CosStorageService cosStorageService; // 使用 final 字段
    private final MediaOperationPublisher mediaOperationPublisher; // MQ 发布者

    // --- 核心业务逻辑方法 ---

    /**
     * 上传媒体文件 - 成功后清除用户和业务相关的列表缓存
     */
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(key = "'user:' + #userId"),
            @CacheEvict(key = "'related:' + #relatedType + ':' + #relatedId")
    })
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
            mediaFile.setUserId(userId);
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
     * 根据ID获取媒体文件信息
     */
    @Cacheable(key = "'id:' + #mediaId", unless = "#result == null")
    public MediaFile getMediaFileById(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaServiceException("媒体文件不存在, ID: " + mediaId));
    }

    /**
     * 获取宠物的所有媒体文件
     */
    @Cacheable(key = "'user:' + #userId")
    public List<MediaFile> getMediaFilesByUserId(Long userId) {

        return mediaRepository.findByUserId(userId);
    }

    /**
     * 获取特定业务记录的媒体文件
     * 对于社区动态（MOMENT），自动更新最后访问时间以重置7天倒计时
     */
    public List<MediaFile> getMediaFilesByRelated(RelatedType relatedType, Long relatedId) {
        List<MediaFile> mediaFiles = mediaRepository.findByRelatedTypeAndRelatedId(relatedType, relatedId);

        // 对于社区动态（MOMENT），更新所有文件的最后访问时间
        if (relatedType == RelatedType.MOMENT && !mediaFiles.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (MediaFile mediaFile : mediaFiles) {
                // 避免重复更新（只在超过1分钟后才更新）
                if (mediaFile.getLastAccessTime() == null ||
                    mediaFile.getLastAccessTime().isBefore(now.minusMinutes(1))) {
                    mediaFile.setLastAccessTime(now);
                    mediaRepository.save(mediaFile);
                }
            }
            log.debug("重置社区动态媒体文件访问时间: relatedType={}, relatedId={}, count={}",
                     relatedType, relatedId, mediaFiles.size());
        }

        return mediaFiles;
    }

    // ==================== 冷热数据访问逻辑 ====================

    /**
     * 访问媒体文件时更新最后访问时间
     * 用于社区动态的7天倒计时
     * 
     * @param mediaId 媒体文件ID
     */
    @Transactional
    public void updateLastAccessTime(Long mediaId) {
        MediaFile mediaFile = getMediaFileById(mediaId);
        
        // 头像不参与冷热分离
        if (mediaFile.isExemptFromColdStorage()) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        mediaFile.setLastAccessTime(now);
        
        // 如果之前是冷数据，访问时需要恢复
        if ("Cold".equals(mediaFile.getStatus())) {
            handleColdDataAccess(mediaFile);
        }
        
        mediaRepository.save(mediaFile);
        log.debug("更新媒体文件最后访问时间: mediaId={}, lastAccessTime={}", mediaId, now);
    }

    /**
     * 批量更新最后访问时间
     * @param mediaIds 媒体文件ID列表
     */
    @Transactional
    public void batchUpdateLastAccessTime(List<Long> mediaIds) {
        for (Long mediaId : mediaIds) {
            updateLastAccessTime(mediaId);
        }
    }

    /**
     * 处理冷数据访问
     * 根据数据类型采取不同的恢复策略：
     * 1. 私人数据（活动/状态记录）：恢复后保持10分钟临时访问
     * 2. 社区动态：立即恢复，重新开始7天倒计时
     * 使用 MQ 异步处理恢复操作
     *
     * @param mediaFile 媒体文件
     */
    private void handleColdDataAccess(MediaFile mediaFile) {
        RelatedType relatedType = mediaFile.getRelatedType();
        String fileUrl = mediaFile.getFileUrl();

        try {
            if (relatedType == RelatedType.ACTIVITY || relatedType == RelatedType.STATUS) {
                // 私人数据：恢复归档文件（10分钟临时访问）
                log.info("访问私人冷数据，准备恢复: mediaId={}, type={}", mediaFile.getMediaId(), relatedType);

                // 发送 MQ 消息异步恢复（不等待结果）
                mediaOperationPublisher.publishRestoreArchivedEvent(
                        mediaFile.getMediaId(),
                        fileUrl
                );
                // 注意：私人数据访问后不改变状态，保持Cold

            } else if (relatedType == RelatedType.MOMENT) {
                // 社区动态：恢复并重新激活
                log.info("访问社区冷数据，恢复激活: mediaId={}", mediaFile.getMediaId());

                // 发送 MQ 消息异步恢复
                mediaOperationPublisher.publishRestoreArchivedEvent(
                        mediaFile.getMediaId(),
                        fileUrl
                );

                // 更新状态为 RESTORING（恢复中），等待消费者处理
                mediaFile.setStatus("Restoring");
                // 移除COS标签（可选，让文件重新开始生命周期倒计时）
                // cosStorageService.setFileTagging(fileUrl, "Status", "Hot");

                log.info("社区冷数据已发送恢复任务，重新开始7天倒计时: mediaId={}", mediaFile.getMediaId());
            }

        } catch (Exception e) {
            log.error("发送冷数据恢复任务失败: mediaId={}, error={}", mediaFile.getMediaId(), e.getMessage());
            // 不抛出异常，允许用户尝试获取文件
        }
    }

    /**
     * 获取媒体文件的访问URL（处理冷数据恢复）
     * 
     * @param mediaId 媒体文件ID
     * @return 文件访问URL
     */
    public String getMediaAccessUrl(Long mediaId) {
        MediaFile mediaFile = getMediaFileById(mediaId);
        
        // 更新最后访问时间
        updateLastAccessTime(mediaId);
        
        return mediaFile.getFileUrl();
    }

    /**
     * 检查媒体文件是否为冷数据
     * 
     * @param mediaId 媒体文件ID
     * @return true if cold
     */
    public boolean isColdData(Long mediaId) {
        MediaFile mediaFile = getMediaFileById(mediaId);
        return "Cold".equals(mediaFile.getStatus());
    }

    /**
     * 手动恢复冷数据
     * 使用 MQ 异步处理恢复操作
     *
     * @param mediaId 媒体文件ID
     * @return 恢复结果信息
     */
    @Transactional
    public String restoreColdData(Long mediaId) {
        MediaFile mediaFile = getMediaFileById(mediaId);

        if (!"Cold".equals(mediaFile.getStatus())) {
            return "文件不是冷数据，无需恢复";
        }

        try {
            // 发送 MQ 消息，异步恢复归档文件
            mediaOperationPublisher.publishRestoreArchivedEvent(
                    mediaFile.getMediaId(),
                    mediaFile.getFileUrl()
            );

            // 更新数据库状态为 RESTORING（恢复中）
            mediaFile.setStatus("Restoring");
            mediaRepository.save(mediaFile);

            log.info("冷数据恢复任务已发送: mediaId={}", mediaId);
            return "恢复任务已发送，请稍候...";

        } catch (Exception e) {
            log.error("发送冷数据恢复任务失败: mediaId={}, error={}", mediaId, e.getMessage());
            return "恢复失败: " + e.getMessage();
        }
    }

    /**
     * 获取特定业务记录的媒体文件（字符串类型参数）
     */
    @Cacheable(key = "'related:' + #relatedTypeStr.toUpperCase() + ':' + #relatedId")
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
     * 删除顺序：先删数据库记录，后MQ异步删除COS文件
     * 这样设计确保：
     * 1. 用户能快速得到删除成功的响应
     * 2. COS删除失败不影响本地数据一致性（通过MQ重试机制）
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(allEntries = true)
    public void deleteMediaFile(Long mediaId) {
        MediaFile mediaFile = getMediaFileById(mediaId);
        String fileUrl = mediaFile.getFileUrl();

        try {
            // 1. 先从数据库删除记录
            mediaRepository.deleteById(mediaId);
            log.info("媒体文件记录从数据库删除成功: mediaId={}", mediaId);

            // 2. 通过MQ异步删除COS文件
            // 使用MQ的好处：失败可重试，解耦主流程
            mediaOperationPublisher.publishDeleteFileEvent(mediaId, fileUrl);
            log.info("媒体文件COS删除任务已提交MQ: mediaId={}, fileUrl={}", mediaId, fileUrl);

        } catch (Exception e) {
            log.error("媒体文件删除失败: mediaId={}, error={}", mediaId, e.getMessage(), e);
            throw new MediaServiceException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除特定业务记录的所有媒体文件
     * 删除顺序：先删数据库记录，后MQ异步删除COS文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMediaFilesByRelated(RelatedType relatedType, Long relatedId) {
        List<MediaFile> mediaFiles = getMediaFilesByRelated(relatedType, relatedId);

        if (mediaFiles.isEmpty()) {
            log.info("没有找到关联的媒体文件: relatedType={}, relatedId={}", relatedType, relatedId);
            return;
        }

        // 1. 先从数据库批量删除记录
        int deletedCount = mediaRepository.deleteByRelatedTypeAndRelatedId(relatedType, relatedId);
        log.info("媒体文件记录从数据库删除成功: relatedType={}, relatedId={}, deletedCount={}",
                relatedType, relatedId, deletedCount);

        // 2. 通过MQ异步批量删除COS文件
        for (MediaFile mediaFile : mediaFiles) {
            try {
                mediaOperationPublisher.publishDeleteFileEvent(mediaFile.getMediaId(), mediaFile.getFileUrl());
                log.debug("COS删除任务已提交MQ: mediaId={}, fileUrl={}", mediaFile.getMediaId(), mediaFile.getFileUrl());
            } catch (Exception e) {
                log.warn("提交COS删除任务失败，继续处理下一个文件: mediaId={}, error={}",
                        mediaFile.getMediaId(), e.getMessage());
            }
        }

        log.info("批量媒体文件删除任务已提交MQ: relatedType={}, relatedId={}, totalCount={}",
                relatedType, relatedId, mediaFiles.size());
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
    public Long countMediaFilesByUserId(Long userId) {
        return mediaRepository.countByUserId(userId);
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
    private String generateFilePath(RelatedType relatedType, Long userId, Long relatedId) {
        return String.format("%s/user_%d/%s_%d",
                relatedType.name().toLowerCase(),
                userId,
                relatedType.name().toLowerCase(),
                relatedId);
    }

    /**
     * 批量更新媒体文件的 relatedId 和 relatedType。
     * @param mediaIds 媒体文件 ID 列表
     * @param relatedTypeStr 关联类型（字符串，如 "MOMENT"）
     * @param newRelatedId 新的关联 ID（如 Moment ID）
     * @return 更新成功的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(allEntries = true)
    public int batchUpdateRelatedId(List<Long> mediaIds, String relatedTypeStr, Long newRelatedId) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return 0;
        }

        RelatedType relatedType;
        try {
            // 校验并转换 RelatedType
            relatedType = RelatedType.valueOf(relatedTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("无效的关联类型: {}", relatedTypeStr);
            throw new IllegalArgumentException("无效的关联类型: " + relatedTypeStr);
        }

        // 1. 批量查询待更新的 MediaFile 实体
        List<MediaFile> filesToUpdate = mediaRepository.findAllById(mediaIds);

        // 2. 批量更新实体属性
        for (MediaFile mediaFile : filesToUpdate) {
            // 确保更新后的文件类型与请求的类型一致，避免错误关联
            if (mediaFile.getRelatedType() == relatedType) {
                mediaFile.setRelatedId(newRelatedId);
                // relatedType 理论上在上传时已设定，这里不重复设置，保持数据一致性
            } else {
                // 可选：如果上传时的 relatedType 与本次关联的类型不匹配，可以跳过或抛出异常
                log.warn("媒体文件ID: {} 的上传类型({}) 与本次关联类型({}) 不匹配，已跳过。",
                        mediaFile.getMediaId(), mediaFile.getRelatedType().name(), relatedTypeStr);
            }
        }

        // 3. 批量保存更新后的实体
        mediaRepository.saveAll(filesToUpdate);

        log.info("✅ 成功将 {} 个媒体文件关联到 RelatedType: {}, RelatedId: {}",
                filesToUpdate.size(), relatedTypeStr, newRelatedId);

        return filesToUpdate.size();
    }

    /**
     * 上传媒体文件（Controller 调用方法，接收 String 类型参数）
     */
    public MediaFile uploadMediaFile(MultipartFile file, Long userId,
                                     String relatedTypeStr, Long relatedId) {
        try {
            String cleanedType = relatedTypeStr.toUpperCase().trim();
            // 解决局部变量冲突，将 RelatedType.valueOf 结果赋给临时变量
            RelatedType type = RelatedType.valueOf(cleanedType);

            // 正确调用主体方法
            return uploadMediaFile(file, userId, type, relatedId);

        } catch (IllegalArgumentException e) {
            log.error("❌ 枚举转换失败 - 输入: '{}', 错误: {}", relatedTypeStr, e.getMessage());
            throw new MediaServiceException("无效的关联类型: " + relatedTypeStr +
                    "，有效值: " + String.join(", ", getValidRelatedTypes()));
        }
    }
}