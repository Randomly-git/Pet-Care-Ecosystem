package com.petcare.backend.service;

import com.petcare.backend.dto.request.CreatePetRequest;
import com.petcare.backend.dto.request.LoginRequest;
import com.petcare.backend.dto.request.RegisterRequest;
import com.petcare.backend.dto.response.LoginResponse;
import com.petcare.backend.dto.response.PetResponse;
import com.petcare.backend.dto.response.RegisterResponse;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /**
     * 根据用户ID获取宠物信息
     * @param userId 用户ID
     * @return 宠物响应DTO
     */
    List<PetResponse> getPetsByUserId(Long userId);

    // 用户认证相关方法
    RegisterResponse register(RegisterRequest registerRequest);

    LoginResponse login(LoginRequest loginRequest);

    User getUserEntityById(Long userId);

    /**
     * 批量获取用户信息
     * @param userIds 用户ID集合
     * @return 用户列表
     */
    List<User> batchGetUsersByIds(Set<Long> userIds);

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息的Optional包装
     */
    Optional<User> getUserById(Long userId);
}