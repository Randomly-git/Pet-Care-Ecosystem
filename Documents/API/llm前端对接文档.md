# LLM 后端 API 对接文档（给前端同学）

---

## 一、调用说明

**网关地址（所有请求都打这里）：**
```
http://localhost:9000
```

**认证：**
- GraphQL 接口（`POST /graphql`）**不需要 JWT token**
- 上传图片接口（`POST /api/cat/identify`）**不需要 JWT token**

---

## 二、GraphQL 通用调用方式

所有 GraphQL 接口都是 `POST /graphql`，Content-Type: `application/json`。

```js
// 通用调用模板
async function callGraphQL(query) {
  const res = await fetch('http://localhost:9000/graphql', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query })
  })
  return res.json()
}
```

---

## 三、功能 1：活动健康分析

### 说明

用户查看宠物的健康报告。后端自动完成：
1. 读取 petcare-backend 的活动记录
2. 从知识库（PDF）检索匹配的品种知识
3. 调用 Qwen AI 生成分析结论

### GraphQL

```graphql
activityHealthAnalysis(petId: ID!, days: Int, userRequirement: String): AnalysisResult
```

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| petId | ID! | 是 | 宠物ID |
| days | Int | 否 | 分析过去N天的数据，默认30 |
| userRequirement | String | 否 | 主人特别关心的问题 |

### 返回

```json
{
  "data": {
    "activityHealthAnalysis": {
      "petId": "4",
      "petName": "可乐",
      "breed": "边牧",
      "species": "dog",
      "analysis": "根据可乐近30天的活动记录...（AI生成的健康分析文本）",
      "analysisType": "RAG",
      "knowledgeSources": [
        { "title": "边牧养护指南", "content": "边牧精力旺盛...", "score": 0.89 }
      ]
    }
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| petName | String! | 宠物名称 |
| breed | String! | 品种 |
| analysis | String! | AI生成的健康分析（可渲染为Markdown） |
| analysisType | String! | "RAG"=有知识库支撑 / "BASIC"=仅LLM |
| knowledgeSources | [Object] | 引用的知识片段列表（可展示来源） |

### 请求示例

```js
// 查看宠物4（可乐）过去30天的健康报告
const query = `{
  activityHealthAnalysis(petId: "4", days: 30) {
    petName breed species analysis analysisType
    knowledgeSources { title score }
  }
}`

const res = await callGraphQL(query)
document.getElementById('report').innerHTML = res.data.activityHealthAnalysis.analysis
```

### UI 建议

| 字段 | 展示方式 |
|------|---------|
| analysis | Markdown 渲染（对话气泡或卡片） |
| knowledgeSources | 底部小字引用来源（如"参考了：边牧养护指南"） |
| analysisType | 显示标签：RAG（书本图标）或 BASIC（通用图标） |

---

## 四、功能 2：猫品种识别

### 说明

用户上传猫的图片 → 后端识别品种 → 返回品种名。

### REST API（推荐，前端更方便）

```
POST /api/cat/identify
Content-Type: multipart/form-data

参数: image (file)  ← 选中的图片文件
```

### 返回

```json
{
  "breed": "橘猫"
}
```

### 前端调用

```js
const formData = new FormData()
formData.append('image', fileInput.files[0])  // fileInput 是 <input type="file">

fetch('http://localhost:9000/api/cat/identify', {
  method: 'POST',
  body: formData
  // 不要手动设置 Content-Type，浏览器会自动设为 multipart/form-data
})
.then(r => r.json())
.then(data => {
  console.log(data.breed)  // "橘猫" 或 "未知"
  if (data.breed !== '未知') {
    // 识别成功
  }
})
```

### 也可以走 GraphQL（如果不想用 FormData）

```graphql
identifyCatBreed(imageData: String!): BreedResult
```

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| imageData | String! | 是 | 图片的 data URI（如 `data:image/jpeg;base64,/9j...`） |

```js
// 前端：File → base64 data URI
function fileToDataUri(file) {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)  // "data:image/jpeg;base64,..."
    reader.readAsDataURL(file)
  })
}

const imageData = await fileToDataUri(fileInput.files[0])
const query = `{ identifyCatBreed(imageData: "${imageData}") { identifiedBreed confidence description } }`
```

### 返回

```json
{
  "data": {
    "identifyCatBreed": {
      "identifiedBreed": "英国短毛猫",
      "description": "圆脸大眼，毛发短密，性格温和安静",
      "characteristics": "体型圆润、脸大、短毛、易胖",
      "confidence": 0.92
    }
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| identifiedBreed | String! | 识别出的品种名称（"未知"表示没识别到） |
| description | String | 品种描述 |
| characteristics | String | 品种特征 |
| confidence | Float! | 信心指数 0~1 |
| rawAnalysis | String! | AI原始分析（调试用） |

---

## 五、功能 3：AI 智能体

### 说明

自然语言对话 + 工具调用。支持多轮对话——AI 会追问缺少的参数，待用户确认后调用工具，执行完毕后清空记忆。

### GraphQL

```graphql
aiAgent(petId: ID!, message: String!, conversationId: String): AgentResult
```

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| petId | ID! | 是 | 宠物ID |
| message | String! | 是 | 用户输入文本 |
| conversationId | String | 否 | 对话ID。首次不传，后续调用传回 |

### 返回

```json
{
  "data": {
    "aiAgent": {
      "petId": "4",
      "petName": "可乐",
      "message": "请问是几点散步的？",
      "conversationId": "abc123",
      "toolCalls": []
    }
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| petName | String! | 宠物名称 |
| message | String! | AI 回复文本 |
| conversationId | String | 对话ID。非 null 时需在下一轮传回；null 表示对话已结束（工具已执行或已取消） |
| toolCalls | [ToolCallLog!] | 调用的工具记录（前端可用来显示操作日志） |

**ToolCallLog：**
| 字段 | 类型 | 说明 |
|------|------|------|
| toolName | String! | 工具名 |
| arguments | String! | 调用参数（JSON） |
| success | Boolean! | 是否成功 |

### 完整对话示例

```js
// 前端对话管理
let conversationId = null

async function agentChat(message) {
  const query = `{ aiAgent(petId: "4", message: "${message.replace(/"/g, '\\"')}", conversationId: ${conversationId ? '"' + conversationId + '"' : null}) { message conversationId toolCalls { toolName arguments success } } }`
  const res = await callGraphQL(query)
  const data = res.data.aiAgent
  conversationId = data.conversationId  // 保存用于下一轮
  return data
}

// 典型多轮流程
let r1 = await agentChat("记录散步")
console.log(r1.message)  // "请问是几点散步的？"

let r2 = await agentChat("下午5点")
console.log(r2.message)  // "有什么要备注的吗？"

let r3 = await agentChat("在小区遛了30分钟")
console.log(r3.message)  // "请确认：为可乐记录[散步]（下午5点，在小区遛了30分钟），是否执行？"

let r4 = await agentChat("执行")
console.log(r4.message)  // "已调用create_activity_record，操作成功。"
console.log(r4.conversationId)  // null，对话结束
console.log(r4.toolCalls) // [{ toolName: "create_activity_record", success: true, ... }]
```

### Agent 可用的工具

| 工具名 | 触发方式 | 说明 |
|--------|---------|------|
| get_pet_info | 用户问宠物信息 | 直接返回宠物信息 |
| get_activity_records | 用户查询活动记录 | 缺日期会问用户 |
| create_activity_record | 用户要记录活动 | 多步流程：收集参数 → 确认 → 执行 |

---

## 六、汇总

| 功能 | 接口类型 | 地址 | 是否需要登录 |
|------|---------|------|------------|
| 活动健康分析 | GraphQL | `POST /graphql` | ❌ 不需要 |
| 猫品种识别 | REST | `POST /api/cat/identify` | ❌ 不需要 |
| 猫品种识别 | GraphQL | `POST /graphql` | ❌ 不需要 |
| AI 智能体 | GraphQL | `POST /graphql` | ❌ 不需要 |

**所有接口都不需要登录，不需要 JWT。**
