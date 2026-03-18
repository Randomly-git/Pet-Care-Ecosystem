package com.petcare.backend.service;

import com.petcare.backend.entity.Status;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.User;
import com.petcare.backend.repository.StatusRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;


@SpringBootTest
@ActiveProfiles("dev")
//@Transactional
class StatusServiceIntegrationTest2 {

    @Autowired
    private StatusService statusService;


    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    private Pet testPet;
    private Status testStatus;

    @BeforeEach
    void setUp() {
        // 获取或创建测试用户
        List<User> users = userRepository.findAll();
        User testUser;
        if (users.isEmpty()) {
            testUser = new User();
            testUser.setName("测试用户");
            testUser.setPasswordHash("testpassword");
            testUser = userRepository.save(testUser);
        } else {
            testUser = users.get(0);
        }

        // 获取或创建测试宠物
        List<Pet> pets = petRepository.findAll();
        if (pets.isEmpty()) {
            testPet = new Pet();
            testPet.setName("测试宠物");
            testPet.setSpecies("狗");
            testPet = petRepository.save(testPet);
        } else {
            testPet = pets.get(0);
        }

        // 创建测试状态（关联到宠物）
        testStatus = new Status();
        testStatus.setStatusName("健康状态");
        testStatus.setPet(testPet); // 关联到宠物
        testStatus = statusRepository.save(testStatus);

    }
}