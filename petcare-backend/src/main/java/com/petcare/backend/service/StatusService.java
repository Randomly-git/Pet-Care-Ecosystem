package com.petcare.backend.service;

import com.petcare.backend.dto.request.CreateStatusRecordDTO;
import com.petcare.backend.entity.Status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StatusService {

    // 1️⃣ 根据宠物ID获取所有有效状态
    List<Status> getValidStatusesByPetId(Long petId);

    // 1.5️⃣ 根据宠物ID获取状态数量
    Long getStatusCountByPetId(Long petId);

    // 2️⃣ 根据状态ID软删除状态（修改state=0）
    void softDeleteStatus(Long statusId);

    // 3️⃣ 根据状态ID修改状态名称
    Status updateStatusName(Long statusId, String newName);

    // 3.5️⃣ 根据状态ID修改状态当前值
    Status updateStatusValue(Long statusId, String statusValue);

    // 4️⃣ 新增状态（为宠物创建）
    Status createStatus(Long petId, String statusName);

}