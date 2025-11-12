package com.petcare.backend.repository;

import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.ActivityKind;
import com.petcare.backend.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    // 根据宠物ID查找活动
    List<Activity> findByPetPetId(Long petId);

    // 根据活动种类查找
    List<Activity> findByActivityKindActivityKindId(Long activityKindId);

    // 根据活动名称查找
    List<Activity> findByActivityName(String activityName);

    // 根据宠物ID和活动种类查找
    List<Activity> findByPetPetIdAndActivityKindActivityKindId(Long petId, Long activityKindId);

    // 查找特定宠物的所有活动名称
    @Query("SELECT DISTINCT a.activityName FROM Activity a WHERE a.pet.petId = :petId")
    List<String> findDistinctActivityNamesByPetId(@Param("petId") Long petId);

    // 根据宠物和活动种类查找活动
    List<Activity> findByPetAndActivityKind(Pet pet, ActivityKind activityKind);
}