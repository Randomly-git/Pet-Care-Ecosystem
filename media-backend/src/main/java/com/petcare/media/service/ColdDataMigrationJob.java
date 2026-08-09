// service/ColdDataMigrationJob.java
package com.petcare.media.service;

import com.petcare.media.entity.MediaFile;
import com.petcare.media.entity.RelatedType;
import com.petcare.media.repository.MediaRepository;
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
 *
 * 使用 MQ 异步处理 COS 操作，优势：
 * 1. 批量处理：定时任务一次性发送所有归档消息，消费者按需处理
 * 2. 削峰填谷：避免瞬间大量 COS API 调用
 * 3. 重试机制：失败自动重试，确保最终成功
 * 4. 解耦：主流程不受 COS API 响应时间影响
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ColdDataMigrationJob {

    private final MediaRepository mediaRepository;
    private final MediaOperationPublisher mediaOperationPublisher;

    // 用户私人数据归档天数（默认30天）
    @Value("${cold.storage.private.days:30}")
    private int privateDataArchiveDays;

    // 社区数据归档天数（默认7天）
    @Value("${cold.storage.community.days:7}")
    private int communityDataArchiveDays;

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
        // MediaBackendApplication 上的 @EnableScheduling 使本方法每天北京时间 02:00 被调用。
        // 本方法只负责查询候选文件和发布 MQ，不直接调用 COS，避免定时线程被远程 API 阻塞。
        log.info("================= 冷数据归档任务开始 =================");

        int totalArchived = 0;

        // 私人媒体按上传时间超过 30 天归档，和社区媒体使用不同业务策略。
        totalArchived += archivePrivateData();

        // 社区媒体按最后访问时间超过 7 天归档，体现访问热度而非创建时间。
        totalArchived += archiveCommunityData();

        log.info("================= 冷数据归档任务完成，共发送 {} 个归档任务到MQ =================", totalArchived);
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

        log.info("发现 {} 个用户私人数据需要归档（超过{}天），发送到MQ异步处理",
                filesToArchive.size(), privateDataArchiveDays);

        int sendCount = 0;
        for (MediaFile file : filesToArchive) {
            try {
                // 一条文件一条 MQ 消息：应用层可以批量扫描，但消费者仍按文件独立重试。
                mediaOperationPublisher.publishSetStorageClassEvent(
                        file.getMediaId(),
                        file.getFileUrl(),
                        "ARCHIVE"
                );

                // 先标记 Archiving，表示消息已发出但 COS 最终状态尚未确认。
                file.setStatus("Archiving");
                mediaRepository.save(file);

                sendCount++;
                log.debug("归档任务已发送: mediaId={}, fileUrl={}", file.getMediaId(), file.getFileUrl());

            } catch (Exception e) {
                log.error("发送归档任务失败: mediaId={}, error={}", file.getMediaId(), e.getMessage());
            }
        }

        log.info("用户私人数据归档任务发送完成：成功 {} 个，失败 {} 个",
                sendCount, filesToArchive.size() - sendCount);
        return sendCount;
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

        log.info("发现 {} 个社区数据需要归档（超过{}天未访问），发送到MQ异步处理",
                filesToArchive.size(), communityDataArchiveDays);

        int sendCount = 0;
        for (MediaFile file : filesToArchive) {
            try {
                // 发送 MQ 消息，异步设置 COS 标签
                mediaOperationPublisher.publishSetStorageClassEvent(
                        file.getMediaId(),
                        file.getFileUrl(),
                        "ARCHIVE"
                );

                // 更新数据库状态为 ARCHIVING（归档中）
                file.setStatus("Archiving");
                mediaRepository.save(file);

                sendCount++;
                log.debug("社区归档任务已发送: mediaId={}, fileUrl={}", file.getMediaId(), file.getFileUrl());

            } catch (Exception e) {
                log.error("发送社区归档任务失败: mediaId={}, error={}", file.getMediaId(), e.getMessage());
            }
        }

        log.info("社区数据归档任务发送完成：成功 {} 个，失败 {} 个",
                sendCount, filesToArchive.size() - sendCount);
        return sendCount;
    }

    /**
     * 手动触发归档任务（用于测试或手动执行）
     */
    public int manualArchive() {
        // 手动入口复用定时任务逻辑，便于测试和运维临时补跑。
        log.info("手动触发冷数据归档任务");
        migrateToColdStorage();
        // 当前实现没有接收 migrateToColdStorage 的统计值，因此该返回值固定为 0；调用方不能据此判断实际数量。
        return 0;
    }
}
