# PetCare LLM 微服务 API 文档

## 服务概览

**服务地址**: `POST /graphql` (通过网关 `http://localhost:9000/graphql`)  
**请求格式**: `application/json`  
**技术栈**: Spring Boot + GraphQL + Qwen AI (qwen3-max/qwen-vl-max) + ONNX 嵌入

---

## 接口列表

### 1. activityHealthAnalysis — 活动健康分析

基于宠物的历史活动记录，结合 RAG 品种知识库，生成个性化健康分析。

```graphql
activityHealthAnalysis(petId: ID!, days: Int, userRequirement: String): AnalysisResult
```

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| petId | ID! | 是 | 宠物ID |
| days | Int | 否 | 分析天数，默认30 |
| userRequirement | String | 否 | 用户特别关心的问题 |

**返回 AnalysisResult:**

| 字段 | 类型 | 说明 |
|------|------|------|
| petId | ID! | 宠物ID |
| petName | String! | 宠物名称 |
| breed | String! | 品种 |
| species | String! | 物种 |
| analysis | String! | AI 健康分析 |
| knowledgeSources | [KnowledgeSource!] | 引用的品种知识片段 |
| analysisType | String! | BASIC / RAG |

**请求示例:**
```json
{
  "query": "query { activityHealthAnalysis(petId: \"1\", days: 30) { petName analysis knowledgeSources { title score } } }"
}
```

**数据来源:** `GET /api/pets/{petId}` + `GET /api/activities/records/pet/{petId}` + `knowledge_chunk` 本地表

---

### 2. identifyCatBreed — 猫品种识别

上传猫的图片（支持 URL 或 base64 data URI），AI 识别品种并匹配数据库信息。

```graphql
identifyCatBreed(imageData: String!): BreedResult
```

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| imageData | String! | 是 | 猫图片URL 或 data URI（如 `data:image/jpeg;base64,/9j...`） |

**返回 BreedResult:**

| 字段 | 类型 | 说明 |
|------|------|------|
| identifiedBreed | String! | 识别品种 |
| description | String | 品种描述 |
| characteristics | String | 品种特征 |
| confidence | Float! | 信心指数 |
| rawAnalysis | String! | VLM 原始返回 |

**请求示例:**
```json
{
  "query": "query { identifyCatBreed(imageData: \"https://example.com/cat.jpg\") { identifiedBreed confidence description } }"
}
```

**前端上传文件用法:**
```js
const toBase64 = file => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => resolve(reader.result)  // 得到 "data:image/jpeg;base64,..."
  reader.onerror = reject
  reader.readAsDataURL(file)
})
// 然后传给 imageData 参数
```

---

### 3. aiAgent — AI 智能体

自然语言对话，AI 自动调用工具完成操作。支持多轮对话，参数收集完成后调用工具并清空记忆。

```graphql
aiAgent(petId: ID!, message: String!, conversationId: String): AgentResult
```

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| petId | ID! | 是 | 宠物ID |
| message | String! | 是 | 用户输入 |
| conversationId | String | 否 | 对话ID。首次调用不传，后端自动生成；后续调用传回以维持上下文 |

**返回 AgentResult:**

| 字段 | 类型 | 说明 |
|------|------|------|
| petId | ID! | 宠物ID |
| petName | String! | 宠物名称 |
| message | String! | AI 回复 |
| conversationId | String | 对话ID。参数未齐时返回，用于后续调用；工具执行成功或取消后为 null |
| toolCalls | [ToolCallLog!] | 调用的工具记录 |

**多轮对话流程：**

```
请求1: aiAgent(petId=1, message="记录散步")
  → 返回 conversationId="abc", message="请问是几点散步的？"

请求2: aiAgent(petId=1, message="下午5点", conversationId="abc")
  → 返回 conversationId="abc", message="有什么要备注的吗？"

请求3: aiAgent(petId=1, message="在小区遛了30分钟", conversationId="abc")
  → 返回 conversationId="abc", message="请确认：为旺财记录[散步]..."

请求4: aiAgent(petId=1, message="执行", conversationId="abc")
  → 调用工具 → 返回 conversationId=null, message="已记录 ✅"

请求取消: aiAgent(petId=1, message="算了", conversationId="abc")
  → 返回 conversationId=null, message="好的，已取消"
```

**可用工具:**

| 工具名 | 功能 | 调用的接口 |
|--------|------|-----------|
| get_pet_info | 查看宠物信息 | `GET /api/pets/{petId}` |
| get_activity_records | 查看活动记录 | `GET /api/activities/records/pet/{petId}` |
| create_activity_record | 记录新活动（写操作） | `POST /api/activities/records/pet/{petId}` |
| get_health_analysis | AI 健康分析 | 调用本服务 activityHealthAnalysis |
| search_knowledge | 查养护知识 | `knowledge_chunk` 本地表 |

**请求示例:**
```json
{
  "query": "query { aiAgent(petId: \"1\", message: \"记录一下散步\") { message conversationId } }"
}
```

**带 conversationId 的后续请求:**
```json
{
  "query": "query { aiAgent(petId: \"1\", message: \"执行\", conversationId: \"abc\") { message conversationId toolCalls { toolName success } } }"
}
```

---

## 数据来源汇总

| 接口 | 调用的内部 API |
|------|--------------|
| activityHealthAnalysis | `GET /api/pets/{petId}`, `GET /api/activities/records/pet/{petId}`, `knowledge_chunk` 本地表 |
| identifyCatBreed | `cat_breed` 本地表 |
| aiAgent | 各工具对应接口（同上） |
