package com.example.demo.agent.service;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ToolRegistry {

    public List<Map<String, Object>> getAllTools() {
        return List.of(
            tool("get_pet_info", "查看宠物基本信息（名称、品种、物种、性别、生日）", params(req("petId", "string", "宠物ID"))),
            tool("get_activity_records", "查询宠物的活动打卡记录，可按日期范围筛选", params(
                req("petId", "string", "宠物ID"),
                opt("startDate", "string", "开始日期 yyyy-MM-dd，默认30天前"),
                opt("endDate", "string", "结束日期 yyyy-MM-dd，默认今天"),
                opt("page", "string", "页码默认0"),
                opt("size", "string", "每页条数默认20"))),
            tool("create_activity_record", "为宠物创建一条新的活动打卡记录，比如喂食、散步、洗澡等", params(
                req("petId", "string", "宠物ID"),
                req("activityName", "string", "活动名称，如散步、喂食主粮、洗澡"),
                opt("description", "string", "活动描述，如'在小区散步30分钟'"),
                opt("date", "string", "活动时间 yyyy-MM-dd HH:mm，默认当前")))
        );
    }

    private Map<String, Object> tool(String name, String desc, Map<String, Object> params) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name); f.put("description", desc); f.put("parameters", params);
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("type", "function"); t.put("function", f);
        return t;
    }

    private Map<String, Object> params(Map.Entry<String, Object>... entries) {
        Map<String, Object> props = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (var e : entries) {
            @SuppressWarnings("unchecked") Map<String, Object> prop = (Map<String, Object>) e.getValue();
            props.put(e.getKey(), prop);
            if (Boolean.TRUE.equals(prop.remove("_req_"))) required.add(e.getKey());
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", "object"); r.put("properties", props); r.put("required", required);
        return r;
    }

    private Map.Entry<String, Object> req(String name, String type, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type); m.put("description", desc); m.put("_req_", true);
        return new AbstractMap.SimpleEntry<>(name, m);
    }

    private Map.Entry<String, Object> opt(String name, String type, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type); m.put("description", desc);
        return new AbstractMap.SimpleEntry<>(name, m);
    }
}
