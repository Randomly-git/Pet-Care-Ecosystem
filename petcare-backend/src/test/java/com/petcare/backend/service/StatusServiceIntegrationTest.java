package com.petcare.backend.service;

import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.Status;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.StatusRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev") // ✅ 使用 application-dev.yml 配置的数据库
@Transactional // 每个测试方法执行后自动回滚数据库，避免污染数据
class StatusServiceIntegrationTest {

    @Autowired
    private StatusService statusService;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private StatusRepository statusRepository;

    private Pet pet;

    @BeforeEach
    void initData() {
        // 初始化宠物数据
        pet = new Pet();
        pet.setName("TestPet");
        pet.setSpecies("狗");
        pet.setBreed("金毛");
        petRepository.save(pet);

        // 初始化两个状态
        Status s1 = new Status();
        s1.setPet(pet);
        s1.setStatusName("Running");
        s1.setState(1);

        Status s2 = new Status();
        s2.setPet(pet);
        s2.setStatusName("Sleeping");
        s2.setState(1);

        statusRepository.saveAll(List.of(s1, s2));
    }

    /**
     * 1️⃣ 测试：查询有效状态
     */
    @Test
    void testGetValidStatusesByPetId() {
        List<Status> statuses = statusService.getValidStatusesByPetId(pet.getPetId());
        assertEquals(2, statuses.size());
        assertTrue(statuses.stream().allMatch(s -> s.getState() == 1));
    }

    /**
     * 2️⃣ 测试：新增状态
     */
    @Test
    void testCreateStatus() {
        Status created = statusService.createStatus(pet.getPetId(), "Eating");

        assertNotNull(created.getStatusId());
        assertEquals("Eating", created.getStatusName());
        assertEquals(1, created.getState());
        assertEquals(pet.getPetId(), created.getPet().getPetId());

        List<Status> all = statusRepository.findByPetPetId(pet.getPetId());
        assertEquals(3, all.size());
    }

    /**
     * 3️⃣ 测试：修改状态名称
     */
    @Test
    void testUpdateStatusName() {
        Status status = statusRepository.findByPetPetIdAndStatusName(pet.getPetId(), "Running").get(0);

        Status updated = statusService.updateStatusName(status.getStatusId(), "Walking");
        assertEquals("Walking", updated.getStatusName());

        Status dbStatus = statusRepository.findById(status.getStatusId()).orElseThrow();
        assertEquals("Walking", dbStatus.getStatusName());
    }

    /**
     * 4️⃣ 测试：软删除状态
     */
    @Test
    void testSoftDeleteStatus() {
        Status status = statusRepository.findByPetPetIdAndStatusName(pet.getPetId(), "Sleeping").get(0);

        statusService.softDeleteStatus(status.getStatusId());

        Status deleted = statusRepository.findById(status.getStatusId()).orElseThrow();
        assertEquals(0, deleted.getState());
    }

    /**
     * 5️⃣ 测试：重复创建状态应抛异常
     */
    @Test
    void testCreateDuplicateStatus() {
        assertThrows(RuntimeException.class, () ->
                statusService.createStatus(pet.getPetId(), "Running"));
    }
}
