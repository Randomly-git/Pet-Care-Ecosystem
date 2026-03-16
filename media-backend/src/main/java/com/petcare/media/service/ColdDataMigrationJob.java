// service/ColdDataMigrationJob.java
package com.petcare.media.service;

import com.petcare.media.entity.MediaFile;
import com.petcare.media.entity.RelatedType;
import com.petcare.media.repository.MediaRepository;
import com.petcare.media.service.CosStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 冷热数据分离定时任务
 * 
 * 归档策略：
 * 1. 用户私人数据（活动记录/状态记录）：超过30天自动归档
 * 2. 社区动态：超过7天未访问自动归档
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ColdDataMigrationJob {

    private final MediaRepository mediaRepository;
    private final CosStorageService cosStorageService;

    // 用户私人数据归档天数（默认30天）
    @Value("${cold.storage.private.days:30}")
    private int privateDataArchiveDays;

    // 社区数据归档天数（默认7天）
    @Value("${cold.storage.community.days:7}")
    private int communityDataArchiveDays;

    // 归档恢复等待时间（分钟）
    @Value("${cold.storage.restore.wait-minutes:3}")
    private int restoreWaitMinutes;

    /**
     * 每天北京时间 2:00 执行冷数据归档任务
     * 
     * cron表达式说明：
     * - 秒(0) 分(0) 时(2) 日(*) 月(*) 周(?)
     * - 北京时间2:00 = UTC 18:00
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Shanghai")
    @Transactional
    public void migrateToColdStorage() {
        log.info("================= 冷数据归档任务开始 =================");
        
        int totalArchived = 0;
        
        // 1. 归档用户私人数据（活动记录/状态记录）
        totalArchived += archivePrivateData();
        
        // 2. 归档社区动态数据
        totalArchived += archiveCommunityData();
        
        log.info("================= 冷数据归档任务完成，共归档 {} 个文件 =================", totalArchived);
    }

    /**
     * 归档用户私人数据（活动记录/状态记录）
     * 归档条件：上传时间超过 privateDataArchiveDays 天
     */
    private int archivePrivateData() {
        List<RelatedType> privateTypes = Arrays.asList(RelatedType.ACTIVITY, RelatedType.STATUS);
        LocalDateTime threshold = LocalDateTime.now().minusDays(privateDataArchiveDays);

        List<MediaFile> filesToArchive = mediaRepository.findPrivateMediaToArchive(privateTypes, threshold);

        if (filesToArchive.isEmpty()) {
            log.info("无用户私人数据需要归档");
            return 0;
        }

        log.info("发现 {} 个用户私人数据需要归档（超过{}天）", filesToArchive.size(), privateDataArchiveDays);

        int successCount = 0;
        for (MediaFile file : filesToArchive) {
            try {
                // 1. 设置COS标签，触发生命周期规则（Status=Cold 一天后归档）
                cosStorageService.setFileTagging(file.getFileUrl(), "Status", "Cold");

                // 2. 更新数据库状态
                file.setStatus("Cold");
                mediaRepository.save(file);

                successCount++;
                log.debug("文件归档成功: mediaId={}, fileUrl={}", file.getMediaId(), file.getFileUrl());

            } catch (Exception e) {
                log.error("文件归档失败: mediaId={}, error={}", file.getMediaId(), e.getMessage());
            }
        }

        log.info("用户私人数据归档完成：成功 {} 个，失败 {} 个", successCount, filesToArchive.size() - successCount);
        return successCount;
    }

    /**
     * 归档社区动态数据
     * 归档条件：最后访问时间超过 communityDataArchiveDays 天
     */
    private int archiveCommunityData() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(communityDataArchiveDays);

        List<MediaFile> filesToArchive = mediaRepository.findCommunityMediaToArchive(threshold);

        if (filesToArchive.isEmpty()) {
            log.info("无社区数据需要归档");
            return 0;
        }

        log.info("发现 {} 个社区数据需要归档（超过{}天未访问）", filesToArchive.size(), communityDataArchiveDays);

        int successCount = 0;
        for (MediaFile file : filesToArchive) {
            try {
                // 1. 设置COS标签，触发生命周期规则（Status=Cold 一天后归档）
                cosStorageService.setFileTagging(file.getFileUrl(), "Status", "Cold");

                // 2. 更新数据库状态
                file.setStatus("Cold");
                mediaRepository.save(file);

                successCount++;
                log.debug("社区文件归档成功: mediaId={}, fileUrl={}", file.getMediaId(), file.getFileUrl());

            } catch (Exception e) {
                log.error("社区文件归档失败: mediaId={}, error={}", file.getMediaId(), e.getMessage());
            }
        }

        log.info("社区数据归档完成：成功 {} 个，失败 {} 个", successCount, filesToArchive.size() - successCount);
        return successCount;
    }

    /**
     * 手动触发归档任务（用于测试或手动执行）
     */
    public int manualArchive() {
        log.info("手动触发冷数据归档任务");
        migrateToColdStorage();
        return 0;
    }
}
