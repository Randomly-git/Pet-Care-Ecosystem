package com.petcare.backend.repository;

import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.ActivityKind;
import com.petcare.backend.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    // 根据宠物ID查找有效活动
    List<Activity> findByPetPetIdAndState(Long petId, Integer state);

    // 根据活动种类查找有效活动
    List<Activity> findByActivityKindActivityKindIdAndState(Long activityKindId, Integer state);

    // 根据活动名称查找有效活动
    List<Activity> findByActivityNameAndState(String activityName, Integer state);

    // 根据宠物ID和活动种类查找有效活动
    List<Activity> findByPetPetIdAndActivityKindActivityKindIdAndState(Long petId, Long activityKindId, Integer state);

    // 查找特定宠物的所有有效活动名称
    @Query("SELECT DISTINCT a.activityName FROM Activity a WHERE a.pet.petId = :petId AND a.state = 1")
    List<String> findDistinctActivityNamesByPetId(@Param("petId") Long petId);


    // 查找所有有效活动
    List<Activity> findByState(Integer state);

    // 根据ID和状态查找活动
    Optional<Activity> findByActivityIdAndState(Long activityId, Integer state);

    // 软删除活动（更新状态为0）
    @Modifying
    @Query("UPDATE Activity a SET a.state = 0 WHERE a.activityId = :activityId")
    int softDelete(@Param("activityId") Long activityId);

    // 统计有效活动记录数量
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.pet.petId = :petId AND a.state = 1")
    Long countByPetPetIdAndState(@Param("petId") Long petId);

    @Query("SELECT COUNT(ar) FROM ActivityRecord ar WHERE ar.pet.petId = :petId")
    Long countByPetPetId(@Param("petId") Long petId);
}