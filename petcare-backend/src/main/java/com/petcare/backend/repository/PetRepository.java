package com.petcare.backend.repository;

import com.petcare.backend.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    // 根据宠物名称查找
    List<Pet> findByName(String name);

    // 根据宠物名称模糊搜索
    List<Pet> findByNameContaining(String name);

    // 根据物种查找
    List<Pet> findBySpecies(String species);

    // 根据品种查找
    List<Pet> findByBreed(String breed);

    // 根据物种和品种查找
    List<Pet> findBySpeciesAndBreed(String species, String breed);

    // 使用自定义查询查找最近创建的宠物
    @Query("SELECT p FROM Pet p ORDER BY p.createdAt DESC")
    List<Pet> findRecentPets();

    // 统计某种物种的宠物数量
    @Query("SELECT COUNT(p) FROM Pet p WHERE p.species = :species")
    Long countBySpecies(@Param("species") String species);


    @Query("SELECT COUNT(s) FROM Status s WHERE s.pet.petId = :petId")
    Long countStatusByPetId(@Param("petId") Long petId);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.pet.petId = :petId")
    Long countActivityByPetId(@Param("petId") Long petId);
}