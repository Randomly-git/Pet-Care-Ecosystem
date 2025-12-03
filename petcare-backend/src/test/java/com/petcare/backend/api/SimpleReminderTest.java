package com.petcare.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SimpleReminderTest {

    private static final String BASE_URL = "http://localhost:8080/api/reminders";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
    }

    public static void main(String[] args) throws Exception {
        // 统一配置参数
        Long petId = 393L;
        Long reminderId = 27L;
        String description = "已完成活动";

        System.out.println("=== 提醒功能API测试 ===\n");

        // 1. 获取逾期提醒
        System.out.println("1. 获取逾期提醒");
        String overdueResponse = sendRequest("GET", BASE_URL + "/pet/" + petId + "/overdue", null);
        printJson("逾期提醒", overdueResponse);

        // 2. 延期提醒
        System.out.println("2. 延期提醒");
        //String postponeResponse = sendRequest("PUT", BASE_URL + "/" + reminderId + "/postpone", null);
        //System.out.println("延期结果: " + (postponeResponse.isEmpty() ? "成功" : postponeResponse) + "\n");

        // 3. 确认提醒（无描述）
        System.out.println("3. 确认提醒（无描述）");
        //String confirmResponse = sendRequest("PUT", BASE_URL + "/" + reminderId + "/confirm", null);
        //System.out.println("确认结果: " + (confirmResponse.isEmpty() ? "成功" : confirmResponse) + "\n");

        // 4. 确认提醒（带描述）
        System.out.println("4. 确认提醒（带描述）");
        String confirmWithDescResponse = sendRequest("PUT", BASE_URL + "/" + reminderId + "/confirm-with-description",
                mapper.writeValueAsString(description));
        System.out.println("带描述确认结果: " + (confirmWithDescResponse.isEmpty() ? "成功" : confirmWithDescResponse) + "\n");
    }

    private static String sendRequest(String method, String url, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json");

        switch (method.toUpperCase()) {
            case "PUT":
                builder.PUT(body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody());
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