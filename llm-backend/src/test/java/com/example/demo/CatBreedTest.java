package com.example.demo;

import com.example.demo.breed.dto.BreedResult;
import com.example.demo.breed.resolver.BreedResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CatBreedTest {

    @Autowired
    private BreedResolver breedResolver;

    @Test
    void testIdentifyCatBreed() {
        System.out.println("\n========== 测试: 猫品种识别 ==========");

        // 使用一张公开的猫图
        String imageUrl = "https://cdn2.thecatapi.com/images/MTYwODA4MQ.jpg";

        BreedResult result = breedResolver.identifyCatBreed(imageUrl);

        System.out.println("识别结果: " + result.getIdentifiedBreed());
        System.out.println("信心指数: " + result.getConfidence());
        System.out.println("品种描述: " + result.getDescription());
        System.out.println("品种特征: " + result.getCharacteristics());

        assertNotNull(result.getIdentifiedBreed());
        assertFalse(result.getIdentifiedBreed().isEmpty(), "品种名称不应为空");
        System.out.println("✅ 猫品种识别测试通过");
    }

    @Test
    void testIdentifyNonCat() {
        System.out.println("\n========== 测试: 非猫图片识别 ==========");

        String dogUrl = "https://images.dog.ceo/breeds/retriever-golden/n02099601_100.jpg";

        BreedResult result = breedResolver.identifyCatBreed(dogUrl);

        System.out.println("识别结果: " + result.getIdentifiedBreed());
        System.out.println("信心指数: " + result.getConfidence());

        assertNotNull(result.getIdentifiedBreed());
        System.out.println("✅ 非猫图片识别测试通过");
    }
}
