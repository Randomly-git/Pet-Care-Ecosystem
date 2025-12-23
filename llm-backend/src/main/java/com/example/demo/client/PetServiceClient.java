package com.example.demo.client;

import com.example.demo.dto.response.StatusRecordDTO;
import org.springframework.beans.factory.annotation.Qualifier; // 必须导入
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
public class PetServiceClient {

    private final RestTemplate restTemplate;
    private final String petServiceBaseUrl = "http://petcare-backend";

    // 修改点：添加 @Qualifier 指定使用带负载均衡的 Bean
    public PetServiceClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取宠物状态记录
     */
    public List<StatusRecordDTO> getPetStatusRecords(Long petId) {
        try {
            // 因为使用了 @LoadBalanced 的 restTemplate，这里可以使用服务名 petcare-backend
            String url = petServiceBaseUrl + "/api/status/records/pet/" + petId;

            System.out.println("调用宠物服务URL: " + url);

            ResponseEntity<List<StatusRecordDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<StatusRecordDTO>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return Collections.emptyList();

        } catch (Exception e) {
            System.err.println("调用宠物服务异常: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}