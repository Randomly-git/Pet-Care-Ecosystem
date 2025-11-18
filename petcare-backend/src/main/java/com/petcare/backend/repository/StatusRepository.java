package com.petcare.backend.repository;

import com.petcare.backend.entity.Status;
import com.petcare.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {

    // 根据用户ID查找状态
    List<Status> findByUserUserId(Long userId);

    // 根据状态名称查找
    List<Status> findByStatusName(String statusName);

    // 根据用户ID和状态名称查找
    List<Status> findByUserUserIdAndStatusName(Long userId, String statusName);

    // 查找特定用户的所有状态名称
    @Query("SELECT DISTINCT s.statusName FROM Status s WHERE s.user.userId = :userId")
    List<String> findDistinctStatusNamesByUserId(@Param("userId") Long userId);

    // 根据用户和状态名称查找具体状态
    Optional<Status> findByUserAndStatusName(User user, String statusName);

    // 检查用户是否已有特定状态
    boolean existsByUserUserIdAndStatusName(Long userId, String statusName);


}