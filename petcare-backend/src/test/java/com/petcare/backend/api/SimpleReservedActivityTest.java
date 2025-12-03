package com.petcare.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petcare.backend.dto.request.CreateReservedActivityDTO;
import com.petcare.backend.dto.request.UpdateReservedActivityDTO;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.Duration;

public class SimpleReservedActivityTest {

    private static final String BASE_URL = "http://localhost:8080/api/reserved-activities";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
    }

    public static void main(String[] args) throws Exception {
        // 测试参数配置
        Long petId = 393L;
        Long activityId = 400L;
        LocalDate date = LocalDate.now().plusDays(3);

        System.out.println("=== 预约活动API测试 ===\n");

        // 1. 创建
        System.out.println("1. 创建预约活动");
        CreateReservedActivityDTO createDTO = new CreateReservedActivityDTO();
        createDTO.setActivityId(activityId);
        createDTO.setPetId(petId);
        createDTO.setReminderDate(date);

        String createJson = mapper.writeValueAsString(createDTO);
        String createResponse = sendRequest("POST", BASE_URL, createJson);
        printJson("创建响应", createResponse);

        // 2. 查询
        System.out.println("2. 查询预约活动");
        String queryResponse = sendRequest("GET", BASE_URL + "/pet/" + petId, null);
        printJson("查询响应", queryResponse);

        // 3. 修改 (需要实际的activityReminderId)
        System.out.println("3. 修改预约活动日期");
        Long reminderId = 69L; // 从创建响应中获取实际ID，这里先用默认值
        LocalDate newDate = LocalDate.now().plusDays(5);

        UpdateReservedActivityDTO updateDTO = new UpdateReservedActivityDTO();
        updateDTO.setActivityReminderId(reminderId);
        updateDTO.setReminderDate(newDate);

        String updateJson = mapper.writeValueAsString(updateDTO);
        //String updateResponse = sendRequest("PUT", BASE_URL + "/date", updateJson);
        //printJson("修改响应", updateResponse);

        // 4. 删除
        System.out.println("4. 删除预约活动");
        //String deleteResponse = sendRequest("DELETE", BASE_URL + "/" + reminderId, null);
        //System.out.println("删除状态: " + (deleteResponse.isEmpty() ? "204 No Content" : deleteResponse));
    }

    private static String sendRequest(String method, String url, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json");

        switch (method.toUpperCase()) {
            case "POST":
                builder.POST(body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody());
                break;
            case "PUT":
                builder.PUT(body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody());
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                builder.GET();
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static void printJson(String title, String json) {
        try {
            Object jsonObj = mapper.readValue(json, Object.class);
            String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
            System.out.println(title + ":\n" + pretty + "\n");
        } catch (Exception e) {
            System.out.println(title + ": " + json + "\n");
        }
    }
}