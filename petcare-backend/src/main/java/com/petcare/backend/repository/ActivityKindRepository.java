package com.petcare.backend.repository;

import com.petcare.backend.entity.ActivityKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityKindRepository extends JpaRepository<ActivityKind, Long> {

    // 根据活动种类名称查找
    Optional<ActivityKind> findByActivityKindName(String activityKindName);

    // 根据活动种类名称模糊搜索
    List<ActivityKind> findByActivityKindNameContaining(String activityKindName);

    // 查找所有活动种类名称
    @Query("SELECT ak.activityKindName FROM ActivityKind ak")
    List<String> findAllActivityKindNames();

    // 检查活动种类名称是否存在
    boolean existsByActivityKindName(String activityKindName);
}