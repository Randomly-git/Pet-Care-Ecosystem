package com.petcare.backend.repository;

import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.Status;
import com.petcare.backend.entity.StatusRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StatusRecordRepository extends JpaRepository<StatusRecord, Long> {

    // 根据宠物ID查找状态记录
    List<StatusRecord> findByPetPetId(Long petId);

    // 根据状态ID查找状态记录
    List<StatusRecord> findByStatusStatusId(Long statusId);

    // 根据宠物ID和状态ID查找
    List<StatusRecord> findByPetPetIdAndStatusStatusId(Long petId, Long statusId);

    // 查找特定日期范围内的状态记录
    List<StatusRecord> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    // 查找正在进行的状态记录（end_date为null或未来日期）
    @Query("SELECT sr FROM StatusRecord sr WHERE sr.pet.petId = :petId AND (sr.endDate IS NULL OR sr.endDate >= CURRENT_DATE)")
    List<StatusRecord> findCurrentStatusRecordsByPetId(@Param("petId") Long petId);

    // 查找特定宠物在特定日期的状态记录
    @Query("SELECT sr FROM StatusRecord sr WHERE sr.pet.petId = :petId AND sr.startDate <= :date AND (sr.endDate IS NULL OR sr.endDate >= :date)")
    List<StatusRecord> findStatusRecordsByPetIdAndDate(@Param("petId") Long petId, @Param("date") LocalDate date);

    // 根据宠物和状态查找记录
    List<StatusRecord> findByPetAndStatus(Pet pet, Status status);

    // 根据描述内容模糊搜索
    List<StatusRecord> findByStatusDescriptionContaining(String keyword);

    // 查找特定状态和描述的状态记录
    List<StatusRecord> findByStatusAndStatusDescriptionContaining(Status status, String keyword);

    // 查找没有描述的状态记录
    List<StatusRecord> findByStatusDescriptionIsNull();

    // 查找有描述的状态记录
    List<StatusRecord> findByStatusDescriptionIsNotNull();

    // 根据宠物ID和描述查找
    List<StatusRecord> findByPetPetIdAndStatusDescriptionContaining(Long petId, String keyword);

    // 查找最新的状态记录（按开始日期倒序）
    @Query("SELECT sr FROM StatusRecord sr WHERE sr.pet.petId = :petId ORDER BY sr.startDate DESC, sr.statusRecordId DESC")
    List<StatusRecord> findRecentStatusRecordsByPetId(@Param("petId") Long petId);
}