package com.petcare.backend.repository;

import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {

    // 根据宠物ID查找状态
    List<Status> findByPetPetId(Long petId);

    // 根据状态名称查找
    List<Status> findByStatusName(String statusName);

    // 根据宠物ID和状态名称查找
    List<Status> findByPetPetIdAndStatusName(Long petId, String statusName);

    // 查找特定宠物的所有状态名称
    @Query("SELECT DISTINCT s.statusName FROM Status s WHERE s.pet.petId = :petId")
    List<String> findDistinctStatusNamesByPetId(@Param("petId") Long petId);

    // 根据宠物和状态名称查找具体状态
    Optional<Status> findByPetAndStatusName(Pet pet, String statusName);

    // 检查宠物是否已有特定状态
    boolean existsByPetPetIdAndStatusName(Long petId, String statusName);

    @Query("SELECT COUNT(sr) FROM StatusRecord sr WHERE sr.pet.petId = :petId")
    Long countByPetPetId(@Param("petId") Long petId);
}