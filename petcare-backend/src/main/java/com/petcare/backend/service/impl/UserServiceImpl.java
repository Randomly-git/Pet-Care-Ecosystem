package com.petcare.backend.service.impl;

import com.petcare.backend.dto.request.CreatePetRequest;
import com.petcare.backend.dto.response.PetResponse;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.exception.PetNotFoundException;
import com.petcare.backend.repository.*;
import com.petcare.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final PetRepository petRepository;
    private final StatusRepository statusRepository;
    private final ActivityRepository activityRepository;
    private final StatusRecordRepository statusRecordRepository;
    private final ActivityRecordRepository activityRecordRepository;

    public UserServiceImpl(PetRepository petRepository,
                           StatusRepository statusRepository,
                           ActivityRepository activityRepository,
                           StatusRecordRepository statusRecordRepository,
                           ActivityRecordRepository activityRecordRepository) {
        this.petRepository = petRepository;
        this.statusRepository = statusRepository;
        this.activityRepository = activityRepository;
        this.statusRecordRepository = statusRecordRepository;
        this.activityRecordRepository = activityRecordRepository;
    }

    @Override
    public Pet createPet(CreatePetRequest createPetRequest) {
        log.info("创建新宠物: {}", createPetRequest.getName());

        Pet pet = new Pet();
        BeanUtils.copyProperties(createPetRequest, pet);

        Pet savedPet = petRepository.save(pet);
        log.info("宠物创建成功, ID: {}", savedPet.getPetId());

        return savedPet;
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponse getPetById(Long petId) {
        log.info("查询宠物信息, ID: {}", petId);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException(petId));

        return convertToPetResponse(pet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> getAllPets() {
        log.info("查询所有宠物信息");

        List<Pet> pets = petRepository.findAll();

        return pets.stream()
                .map(this::convertToPetResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Pet getPetEntityById(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException(petId));
    }

    /**
     * 将 Pet 实体转换为 PetResponse DTO
     */
    private PetResponse convertToPetResponse(Pet pet) {
        PetResponse response = new PetResponse();
        BeanUtils.copyProperties(pet, response);

        // 添加统计信息
        Long statusCount = statusRepository.countByPetPetId(pet.getPetId());
        Long activityCount = activityRepository.countByPetPetId(pet.getPetId());
        Long statusRecordCount = statusRecordRepository.countByPetPetId(pet.getPetId());
        Long activityRecordCount = activityRecordRepository.countByPetPetId(pet.getPetId());

        response.setStatusCount(statusCount);
        response.setActivityCount(activityCount);
        response.setStatusRecordCount(statusRecordCount);
        response.setActivityRecordCount(activityRecordCount);

        return response;
    }
}