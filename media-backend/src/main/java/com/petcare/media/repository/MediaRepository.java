// repository/MediaRepository.java
package com.petcare.media.repository;

import com.petcare.media.entity.MediaFile;
import com.petcare.media.entity.RelatedType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}