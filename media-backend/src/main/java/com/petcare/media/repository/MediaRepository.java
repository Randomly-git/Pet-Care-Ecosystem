// repository/MediaRepository.java
package com.petcare.media.repository;

import com.petcare.media.entity.MediaFile;
import com.petcare.media.entity.RelatedType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<MediaFile, Long> {

    // 根据宠物ID查找所有媒体文件
    List<MediaFile> findByUserId(Long UserId);

    // 根据业务类型和业务ID查找媒体文件
    List<MediaFile> findByRelatedTypeAndRelatedId(RelatedType relatedType, Long relatedId);

    // 根据用户ID和业务类型查找
    List<MediaFile> findByUserIdAndRelatedType(Long UserId, RelatedType relatedType);

    // 根据文件URL查找
    Optional<MediaFile> findByFileUrl(String fileUrl);

    // 根据文件类型前缀查找（如图片、视频）
    List<MediaFile> findByFileTypeStartingWith(String fileTypePrefix);

    // 根据业务类型、业务ID和文件类型查找
    @Query("SELECT m FROM MediaFile m WHERE m.relatedType = :relatedType AND m.relatedId = :relatedId AND m.fileType LIKE :fileTypePattern")
    List<MediaFile> findByRelatedTypeAndRelatedIdAndFileTypeLike(
            @Param("relatedType") RelatedType relatedType,
            @Param("relatedId") Long relatedId,
            @Param("fileTypePattern") String fileTypePattern);

    // 删除特定业务记录的所有媒体文件
    @Modifying
    @Transactional
    @Query("DELETE FROM MediaFile m WHERE m.relatedType = :relatedType AND m.relatedId = :relatedId")
    int deleteByRelatedTypeAndRelatedId(@Param("relatedType") RelatedType relatedType,
                                        @Param("relatedId") Long relatedId);

    // 统计有用户多少媒体文件
    @Query("SELECT COUNT(m) FROM MediaFile m WHERE m.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    // 获取宠物最新的媒体文件
    List<MediaFile> findByUserIdOrderByUploadTimeDesc(Long UserId);

    // 根据多个业务ID查找
    @Query("SELECT m FROM MediaFile m WHERE m.relatedType = :relatedType AND m.relatedId IN :relatedIds")
    List<MediaFile> findByRelatedTypeAndRelatedIdIn(@Param("relatedType") RelatedType relatedType,
                                                    @Param("relatedIds") List<Long> relatedIds);

    // ==================== 冷热分离查询 ====================

    // 查询用户私人数据（活动/状态记录）中超过N天未访问的热数据
    @Query("SELECT m FROM MediaFile m WHERE m.status = 'Hot' AND m.relatedType IN :privateTypes " +
           "AND m.uploadTime < :threshold AND m.relatedType != 'USER_AVATAR'")
    List<MediaFile> findPrivateMediaToArchive(
            @Param("privateTypes") List<RelatedType> privateTypes,
            @Param("threshold") LocalDateTime threshold);

    // 查询社区动态中超过N天未访问的热数据
    @Query("SELECT m FROM MediaFile m WHERE m.status = 'Hot' AND m.relatedType = 'MOMENT' " +
           "AND m.lastAccessTime < :threshold")
    List<MediaFile> findCommunityMediaToArchive(@Param("threshold") LocalDateTime threshold);

    // 查询所有热数据（排除头像）
    @Query("SELECT m FROM MediaFile m WHERE m.status = 'Hot' AND m.relatedType != 'USER_AVATAR'")
    List<MediaFile> findAllHotMedia();

    // 更新最后访问时间
    @Modifying
    @Query("UPDATE MediaFile m SET m.lastAccessTime = :lastAccessTime WHERE m.mediaId = :mediaId")
    void updateLastAccessTime(@Param("mediaId") Long mediaId, @Param("lastAccessTime") LocalDateTime lastAccessTime);

    // 批量更新状态
    @Modifying
    @Query("UPDATE MediaFile m SET m.status = :status WHERE m.mediaId IN :mediaIds")
    void batchUpdateStatus(@Param("mediaIds") List<Long> mediaIds, @Param("status") String status);

    // 批量更新关联ID（用于MQ消息处理）
    @Modifying
    @Query("UPDATE MediaFile m SET m.relatedId = :newRelatedId WHERE m.mediaId IN :mediaIds AND m.relatedType = :relatedType")
    int batchUpdateRelatedId(@Param("mediaIds") List<Long> mediaIds,
                            @Param("relatedType") String relatedType,
                            @Param("newRelatedId") Long newRelatedId);
}