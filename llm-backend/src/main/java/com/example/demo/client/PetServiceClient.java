package com.example.demo.client;

import com.example.demo.dto.response.StatusRecordDTO;
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
    private final String petServiceBaseUrl = "http://localhost:8082"; // 宠物服务地址

    public PetServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取宠物状态记录
     */
    public List<StatusRecordDTO> getPetStatusRecords(Long petId) {
        try {
            String url = petServiceBaseUrl + "/api/status/records/pet/" + petId;

            System.out.println("调用宠物服务URL: " + url);

            ResponseEntity<List<StatusRecordDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<StatusRecordDTO>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("成功获取到 " + response.getBody().size() + " 条状态记录");
                return response.getBody();
            } else {
                System.out.println("获取状态记录失败，状态码: " + response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            System.out.println("调用宠物服务异常: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}