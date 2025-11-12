package com.petcare.backend.service;

import com.petcare.backend.dto.request.CreatePetRequest;
import com.petcare.backend.dto.response.PetResponse;
import com.petcare.backend.entity.Pet;

import java.util.List;

public interface UserService {

    /**
     * 创建新宠物
     * @param createPetRequest 创建宠物请求DTO
     * @return 创建的宠物实体
     */
    Pet createPet(CreatePetRequest createPetRequest);

    /**
     * 根据宠物ID获取宠物信息
     * @param petId 宠物ID
     * @return 宠物响应DTO
     */
    PetResponse getPetById(Long petId);

    /**
     * 获取所有宠物信息
     * @return 所有宠物的响应DTO列表
     */
    List<PetResponse> getAllPets();

    /**
     * 根据宠物ID获取宠物实体（内部使用）
     * @param petId 宠物ID
     * @return 宠物实体
     */
    Pet getPetEntityById(Long petId);
}