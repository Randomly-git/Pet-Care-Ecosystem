package com.petcare.backend.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petcare.backend.dto.request.CreateFixedActivityDTO;
import com.petcare.backend.dto.request.UpdateFixedActivityDTO;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SimpleFixedActivityApiTest {

    private static final String BASE_URL = "http://localhost:8080/api/fixed-activities";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public static void main(String[] args) {
        // 统一控制参数
        Long petId = 393L;
        Long activityId = 410L;
        Integer gapTime = 7;

        System.out.println("=== 定时活动API测试 ===\n");

        try {
            // 1. 创建定时活动
            System.out.println("1. 创建定时活动:");
            CreateFixedActivityDTO createDTO = new CreateFixedActivityDTO();
            createDTO.setPetId(petId);
            createDTO.setActivityId(activityId);
            createDTO.setGapTime(gapTime);

           //String createResponse = sendPostRequest(BASE_URL, createDTO);
            //printJson("创建响应", createResponse);

            // 2. 查看宠物所有定时活动
            System.out.println("2. 查看宠物所有定时活动:");
            //String getResponse = sendGetRequest(BASE_URL + "/pet/" + petId);
            //printJson("查询响应", getResponse);

            // 3. 修改定时活动的间隔时间 (这里需要先获取创建的fixedActivityId，实际使用时需要解析响应)
            System.out.println("3. 修改定时活动的间隔时间:");
            Long fixedActivityId = 33L; // 这里需要从创建响应中获取实际ID
            UpdateFixedActivityDTO updateDTO = new UpdateFixedActivityDTO();
            updateDTO.setFixedActivityId(fixedActivityId);
            updateDTO.setGapTime(14);

            //String updateResponse = sendPutRequest(BASE_URL + "/" + fixedActivityId, updateDTO);
            //printJson("修改响应", updateResponse);

            // 4. 删除定时活动
            System.out.println("4. 删除定时活动:");
            sendDeleteRequest(BASE_URL + "/" + fixedActivityId);
            System.out.println("删除请求已发送");

        } catch (Exception e) {
            System.err.println("测试错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String sendPostRequest(String url, Object body) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static String sendGetRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static String sendPutRequest(String url, Object body) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static void sendDeleteRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void printJson(String title, String json) {
        try {
            Object jsonObject = objectMapper.readValue(json, Object.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            System.out.println(title + ":");
            System.out.println(prettyJson);
            System.out.println();
        } catch (Exception e) {
            System.out.println(title + ": " + json);
            System.out.println();
        }
    }
}