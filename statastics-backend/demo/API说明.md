# 宠物活动统计服务 API 文档

## 📋 服务概览
**服务名称**: 宠物活动统计服务  
**服务地址**: `http://localhost:9000` (通过网关)  
**基础路径**: `/api/stats/activity`  
**功能描述**: 提供宠物活动数据的统计分析，包括月度、周度统计和活动类型分析

---

## 🔗 API 接口列表

### 1. 健康检查
**检查服务是否正常运行**

- **URL**: `/health`
- **Method**: GET
- **参数**: 无
- **响应**: 纯文本

**示例请求**:
```bash
curl http://localhost:9000/api/stats/activity/health
```

**示例响应**:
```
Activity Stats Service is running!
```

---

### 2. 获取宠物活动统计数据（默认最近3个月）
**获取指定宠物的活动统计，默认返回最近3个月的月度统计数据**

- **URL**: `/pet/{petId}`
- **Method**: GET
- **路径参数**:
  | 参数名 | 类型 | 必需 | 说明 |
  |--------|------|------|------|
  | petId | Long | 是 | 宠物ID |

- **查询参数**:
  | 参数名 | 类型 | 默认值 | 必需 | 说明 |
  |--------|------|--------|------|------|
  | period | String | MONTHLY | 否 | 统计周期：`MONTHLY`(月度) 或 `WEEKLY`(周度) |

**示例请求**:
```bash
# 获取宠物1的月度统计（默认）
curl "http://localhost:9000/api/stats/activity/pet/1"

# 获取宠物1的周度统计
curl "http://localhost:9000/api/stats/activity/pet/1?period=WEEKLY"
```

**示例响应（月度统计）**:
```json
{
  "petId": 1,
  "period": "MONTHLY",
  "totalActivities": 397,
  "uniqueActivityTypes": 6,
  "uniqueActivityKinds": 4,
  "monthlyStats": [
    {
      "yearMonth": "2025-12",
      "totalActivities": 64,
      "activityTypeCounts": {
        "吃主粮": 26,
        "毛发护理": 2,
        "跑跳/玩耍": 6,
        "与主人互动": 6,
        "喝水": 12,
        "清理居所": 12
      },
      "activityKindCounts": {
        "清洁": 14,
        "喂养": 38,
        "运动": 6,
        "互动": 6
      }
    }
  ],
  "weeklyStats": null,
  "mostFrequentActivity": "吃主粮",
  "mostFrequentCount": 155,
  "mostFrequentKind": "喂养",
  "mostFrequentKindCount": 232
}
```

**示例响应（周度统计）**:
```json
{
  "petId": 1,
  "period": "WEEKLY",
  "totalActivities": 397,
  "uniqueActivityTypes": 6,
  "uniqueActivityKinds": 4,
  "monthlyStats": null,
  "weeklyStats": [
    {
      "yearWeek": "2025-W52",
      "weekNumber": 52,
      "weekRange": "12月22日 - 12月22日",
      "totalActivities": 2,
      "activityTypeCounts": {
        "吃主粮": 2
      },
      "activityKindCounts": {
        "喂养": 2
      }
    }
  ],
  "mostFrequentActivity": "吃主粮",
  "mostFrequentCount": 155,
  "mostFrequentKind": "喂养",
  "mostFrequentKindCount": 232
}
```

---

### 3. 获取指定时间段的统计数据
**获取指定时间范围内的活动统计数据**

- **URL**: `/pet/{petId}/range`
- **Method**: GET
- **路径参数**:
  | 参数名 | 类型 | 必需 | 说明 |
  |--------|------|------|------|
  | petId | Long | 是 | 宠物ID |

- **查询参数**:
  | 参数名 | 类型 | 必需 | 说明 |
  |--------|------|------|------|
  | startDate | DateTime | 是 | 开始时间（ISO格式） |
  | endDate | DateTime | 是 | 结束时间（ISO格式） |
  | period | String | 是 | 统计周期：`MONTHLY` 或 `WEEKLY` |

**示例请求**:
```bash
curl "http://localhost:9000/api/stats/activity/pet/1/range?startDate=2024-12-01T00:00:00&endDate=2024-12-31T23:59:59&period=MONTHLY"
```

**示例响应**:
```json
{
  "petId": 1,
  "period": "MONTHLY",
  "totalActivities": null,
  "uniqueActivityTypes": null,
  "uniqueActivityKinds": null,
  "monthlyStats": [],
  "weeklyStats": null,
  "mostFrequentActivity": null,
  "mostFrequentCount": null,
  "mostFrequentKind": null,
  "mostFrequentKindCount": null
}
```

---

### 4. POST方式获取统计数据（完整参数）
**通过POST请求体传递完整参数**

- **URL**: `/`
- **Method**: POST
- **请求头**: `Content-Type: application/json`
- **请求体**:
  ```json
  {
    "petId": 1,
    "startDate": "2024-12-01T00:00:00",
    "endDate": "2024-12-31T23:59:59",
    "period": "MONTHLY"
  }
  ```

**示例请求**:
```bash
curl -X POST "http://localhost:9000/api/stats/activity" \
  -H "Content-Type: application/json" \
  -d '{
    "petId": 1,
    "startDate": "2024-12-01T00:00:00",
    "endDate": "2024-12-31T23:59:59",
    "period": "MONTHLY"
  }'
```

---

## 🏗️ 数据结构说明

### ActivityStatsResponse（统计响应）
| 字段 | 类型 | 说明 |
|------|------|------|
| petId | Long | 宠物ID |
| period | String | 统计周期：MONTHLY/WEEKLY |
| totalActivities | Integer | 总活动数量 |
| uniqueActivityTypes | Integer | 唯一活动类型数量 |
| uniqueActivityKinds | Integer | 唯一活动大类数量 |
| monthlyStats | Array[MonthlyStat] | 月度统计数据（period=MONTHLY时有效） |
| weeklyStats | Array[WeeklyStat] | 周度统计数据（period=WEEKLY时有效） |
| mostFrequentActivity | String | 最频繁的活动类型 |
| mostFrequentCount | Integer | 最频繁活动的次数 |
| mostFrequentKind | String | 最频繁的活动大类 |
| mostFrequentKindCount | Integer | 最频繁活动大类的次数 |

### MonthlyStat（月度统计）
| 字段 | 类型 | 说明 |
|------|------|------|
| yearMonth | String | 年月格式：YYYY-MM |
| totalActivities | Integer | 该月总活动数 |
| activityTypeCounts | Object | 各活动类型的次数统计 |
| activityKindCounts | Object | 各活动大类的次数统计 |

### WeeklyStat（周度统计）
| 字段 | 类型 | 说明 |
|------|------|------|
| yearWeek | String | 年周格式：YYYY-Www |
| weekNumber | Integer | 周数 |
| weekRange | String | 周期范围（中文显示） |
| totalActivities | Integer | 该周总活动数 |
| activityTypeCounts | Object | 各活动类型的次数统计 |
| activityKindCounts | Object | 各活动大类的次数统计 |

---
