package com.petcare.backend.repository;

import com.petcare.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 根据用户名查找用户
    Optional<User> findByName(String name);

    // 检查用户名是否存在
    boolean existsByName(String name);

    // 根据用户名模糊搜索
    List<User> findByNameContaining(String name);

    // 自定义查询：查找创建时间在指定日期之后的用户
    @Query("SELECT u FROM User u WHERE u.createdAt >= :startDate")
    List<User> findUsersCreatedAfter(@Param("startDate") java.time.LocalDateTime startDate);

    // 检查用户是否存在
    boolean existsById(Long userId);

    // 统计用户数量
    long count();
}