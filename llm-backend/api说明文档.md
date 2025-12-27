# PetCare LLM 微服务 API 文档

## 📋 服务概览

**服务名称**: 宠物健康分析 LLM 微服务  
**服务地址**: `http://localhost:8088` (或 `http://192.168.71.20:8088`)  
**技术栈**: Spring Boot + GraphQL + Qwen AI  
**数据源**:
- 宠物基本信息服务 (localhost:9000)
- Qwen 3.5 AI 模型

---

## 🔗 GraphQL 端点

### 基础信息
- **GraphQL 端点**: `POST /graphql`
- **GraphiQL 界面**: `GET /graphiql` (用于测试)
- **请求格式**: `application/json`

### 请求示例
```http
POST http://localhost:8088/graphql
Content-Type: application/json

{
  "query": "query { petHealthAnalysis(petId: \"1\") { name breed healthAdvice } }"
}
```

---

## 📊 API 接口

### 1. 宠物健康分析查询
获取宠物的综合健康分析报告，基于宠物信息和历史状态记录生成AI建议。

**GraphQL Query:**
```graphql
petHealthAnalysis(petId: ID!): PetHealthAnalysis
```

**参数:**
| 参数名 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| petId | ID! | 是 | 宠物ID (数字字符串) |

**返回类型: PetHealthAnalysis**
| 字段 | 类型 | 说明 |
|------|------|------|
| petId | ID | 宠物ID |
| name | String | 宠物名称 |
| breed | String | 品种 |
| species | String | 物种 |
| healthAdvice | String! | AI生成的健康建议 |
| statusRecords | [StatusRecord] | 历史健康状态记录 |

---