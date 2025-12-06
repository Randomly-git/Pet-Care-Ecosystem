package com.petcare.backend.service;

import com.petcare.backend.dto.request.CreateStatusRecordDTO;
import com.petcare.backend.dto.request.UpdateStatusRecordDTO;
import com.petcare.backend.dto.response.StatusRecordDTO;
import com.petcare.backend.entity.Status;
import com.petcare.backend.entity.StatusRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StatusService {

    // 1️⃣ 根据用户ID获取所有有效状态
    List<Status> getValidStatusesByUserId(Long userId);

    // 2️⃣ 根据状态ID软删除状态（修改state=0）
    void softDeleteStatus(Long statusId);

    // 3️⃣ 根据状态ID修改状态名称
    Status updateStatusName(Long statusId, String newName);

    // 4️⃣ 新增状态
    Status createStatus(Long userId, String statusName);

    // 查找某一天未结束的状态记录
    List<StatusRecordDTO> getActiveStatusRecordsByPetIdAndDate(Long PetId, LocalDate targetDate);

    // 新增：获取某个宠物的所有状态记录
    List<StatusRecordDTO> getAllStatusRecordsByPetId(Long petId);

    // 创建状态记录（只插入start_date）
    StatusRecord createStatusRecord(CreateStatusRecordDTO createStatusRecordDTO);

    // 停止状态记录（插入end_date）
    StatusRecord stopStatusRecord(Long statusRecordId, LocalDate endDate);

    // 删除状态记录
    void deleteStatusRecord(Long statusRecordId);

    // 删除状态及其所有相关记录
    void deleteStatusAndRecords(Long statusId);

    // 修改状态记录
    StatusRecord updateStatusRecord(UpdateStatusRecordDTO updateStatusRecordDTO);

    // 新增：根据状态ID获取所有状态记录
    List<StatusRecordDTO> getStatusRecordsByStatusId(Long statusId);
}