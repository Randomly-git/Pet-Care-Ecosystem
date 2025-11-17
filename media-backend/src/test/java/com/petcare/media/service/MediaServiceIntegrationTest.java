// test/java/com/petcare/media/service/MediaServiceIntegrationTest.java
package com.petcare.media.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MediaServiceIntegrationTest {

    @Autowired
    private MediaService mediaService;

    @MockBean  // 添加这一行
    private CosStorageService cosStorageService;

    @Test
    void contextLoads() {
        assertNotNull(mediaService);
    }
}