# Pet API 接口说明文档

## 基础信息
- **基础URL**: `http://localhost:8080/api/pets`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8
- **跨域支持**: 允许所有域名跨域访问

---

## 1. 宠物管理接口

### 1.1 创建宠物
**POST** `/`

#### 请求体
```json
{
  "name": "豆豆",
  "species": "狗",
  "breed": "金毛寻回犬",
  "birthday": "2020-05-15",
  "gender": true,
  "userId": 32
}
```

#### 字段说明
- `name` (String, 必需): 宠物名称，最大长度100字符
- `species` (String, 必需): 宠物种类，最大长度50字符
- `breed` (String, 可选): 宠物品种，最大长度100字符
- `birthday` (LocalDate, 可选): 宠物生日，格式: `yyyy-MM-dd`
- `gender` (Boolean, 可选): 宠物性别，`true`代表公，`false`代表母，`null`表示未知
- `userId` (Long, 必需): 用户ID

#### 请求示例
```bash
curl -X POST "http://localhost:8080/api/pets" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "豆豆",
    "species": "狗",
    "breed": "金毛寻回犬",
    "birthday": "2020-05-15",
    "gender": true,
    "userId": 32
  }'
```

#### 成功响应格式
```json
{
  "success": true,
  "message": "创建成功",
  "data": {
    "petId": 393,
    "name": "豆豆",
    "species": "狗",
    "breed": "金毛寻回犬",
    "birthday": "2020-05-15",
    "gender": true,
    "genderText": "公",
    "createdAt": "2024-01-18T12:00:00.000Z",
    "userId": 32,
    "userName": "testuser",
    "statusRecordCount": 0,
    "activityRecordCount": 0
  },
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

#### 错误响应格式
```json
{
  "success": false,
  "message": "用户不存在，ID: 999",
  "data": null,
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

### 1.2 根据ID获取宠物信息
**GET** `/{petId}`

#### 路径参数
- `petId` (Long, 必需): 宠物ID

#### 请求示例
```bash
curl -X GET "http://localhost:8080/api/pets/393"
```

#### 成功响应格式
```json
{
  "success": true,
  "message": "请求成功",
  "data": {
    "petId": 393,
    "name": "豆豆",
    "species": "狗",
    "breed": "金毛寻回犬",
    "birthday": "2020-05-15",
    "gender": true,
    "genderText": "公",
    "createdAt": "2024-01-18T12:00:00.000Z",
    "userId": 32,
    "userName": "testuser",
    "statusRecordCount": 5,
    "activityRecordCount": 12
  },
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

#### 错误响应格式
```json
{
  "success": false,
  "message": "未找到ID为 999 的宠物",
  "data": null,
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

### 1.3 根据用户ID获取宠物列表
**GET** `/user/{userId}`

#### 路径参数
- `userId` (Long, 必需): 用户ID

#### 请求示例
```bash
curl -X GET "http://localhost:8080/api/pets/user/32"
```

#### 成功响应格式
```json
{
  "success": true,
  "message": "请求成功",
  "data": [
    {
      "petId": 393,
      "name": "豆豆",
      "species": "狗",
      "breed": "金毛寻回犬",
      "birthday": "2020-05-15",
      "gender": true,
      "genderText": "公",
      "createdAt": "2024-01-18T12:00:00.000Z",
      "userId": 32,
      "userName": "testuser",
      "statusRecordCount": 5,
      "activityRecordCount": 12
    },
    {
      "petId": 394,
      "name": "咪咪",
      "species": "猫",
      "breed": "英国短毛猫",
      "birthday": "2021-03-20",
      "gender": false,
      "genderText": "母",
      "createdAt": "2024-01-18T12:30:00.000Z",
      "userId": 32,
      "userName": "testuser",
      "statusRecordCount": 3,
      "activityRecordCount": 8
    },
    {
      "petId": 395,
      "name": "小白",
      "species": "狗",
      "breed": "贵宾犬",
      "birthday": "2019-08-10",
      "gender": null,
      "genderText": "未知",
      "createdAt": "2024-01-18T13:00:00.000Z",
      "userId": 32,
      "userName": "testuser",
      "statusRecordCount": 7,
      "activityRecordCount": 15
    }
  ],
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

#### 空列表响应格式
```json
{
  "success": true,
  "message": "请求成功",
  "data": [],
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

#### 错误响应格式
```json
{
  "success": false,
  "message": "用户不存在，ID: 999",
  "data": null,
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

### 1.4 获取所有宠物（用于推荐）
**GET** `/`

#### 请求示例
```bash
curl -X GET "http://localhost:8080/api/pets"
```

#### 成功响应格式
```json
{
  "success": true,
  "message": "请求成功",
  "data": [
    {
      "petId": 393,
      "name": "豆豆",
      "species": "狗",
      "breed": "金毛寻回犬",
      "birthday": "2020-05-15",
      "gender": true,
      "genderText": "公",
      "createdAt": "2024-01-18T12:00:00.000Z",
      "userId": 32,
      "userName": "testuser",
      "statusRecordCount": 5,
      "activityRecordCount": 12
    },
    {
      "petId": 396,
      "name": "小黑",
      "species": "猫",
      "breed": "波斯猫",
      "birthday": "2022-02-14",
      "gender": true,
      "genderText": "公",
      "createdAt": "2024-01-18T14:00:00.000Z",
      "userId": 45,
      "userName": "anotheruser",
      "statusRecordCount": 2,
      "activityRecordCount": 6
    }
  ],
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

---

## 2. 数据模型说明

### 2.1 请求数据模型

#### CreatePetRequest (创建宠物请求)
```json
{
  "name": "宠物名称 (String, 必需, 最大100字符)",
  "species": "宠物种类 (String, 必需, 最大50字符)",
  "breed": "宠物品种 (String, 可选, 最大100字符)",
  "birthday": "宠物生日 (LocalDate, 可选, yyyy-MM-dd格式)",
  "gender": "宠物性别 (Boolean, 可选, true=公, false=母, null=未知)",
  "userId": "用户ID (Long, 必需)"
}
```

### 2.2 响应数据模型

#### PetResponse (宠物响应)
```json
{
  "petId": "宠物ID (Long)",
  "name": "宠物名称 (String)",
  "species": "宠物种类 (String)",
  "breed": "宠物品种 (String)",
  "birthday": "宠物生日 (LocalDate)",
  "gender": "宠物性别 (Boolean, true=公, false=母, null=未知)",
  "genderText": "性别文字描述 (String, '公'/'母'/'未知')",
  "createdAt": "创建时间 (LocalDateTime)",
  "userId": "用户ID (Long)",
  "userName": "用户名 (String)",
  "statusRecordCount": "状态记录数量 (Long)",
  "activityRecordCount": "活动记录数量 (Long)"
}
```

#### 性别字段说明
| 字段 | 类型 | 说明 | 示例值 |
|------|------|------|--------|
| `gender` | Boolean | 原始性别值 | `true` (公), `false` (母), `null` (未知) |
| `genderText` | String | 性别文字描述 | `"公"`, `"母"`, `"未知"` |

### 2.3 统一响应格式 (ApiResponse)
```json
{
  "success": "请求是否成功 (Boolean)",
  "message": "响应消息 (String)",
  "data": "响应数据 (Object 或 Array)",
  "timestamp": "响应时间戳 (String)"
}
```

---

## 3. 状态码说明

| 状态码 | 说明 | 典型场景 |
|--------|------|----------|
| 200 | 请求成功 | 获取宠物信息成功 |
| 201 | 创建成功 | 创建宠物成功 |
| 400 | 请求参数错误 | 参数验证失败 |
| 404 | 资源未找到 | 用户或宠物不存在 |
| 500 | 服务器内部错误 | 数据库操作失败 |

---

## 4. 错误处理

### 4.1 异常类型

#### PetNotFoundException
- **触发条件**: 宠物ID不存在
- **HTTP状态码**: 404 Not Found
- **错误消息**: `"未找到ID为 {petId} 的宠物"`

#### UserNotFoundException
- **触发条件**: 用户ID不存在
- **HTTP状态码**: 404 Not Found
- **错误消息**: `"用户不存在，ID: {userId}"`

### 4.2 参数验证错误
当请求参数不符合验证规则时，返回400错误：
```json
{
  "success": false,
  "message": "宠物名称不能为空",
  "data": null,
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

---

## 5. 业务逻辑说明

### 5.1 创建宠物流程
1. 验证请求参数格式
2. 检查用户ID是否存在
3. 创建宠物记录（包含gender字段）
4. 返回完整的宠物信息（包含统计信息和genderText）

### 5.2 数据关联
- 宠物与用户关联：一个用户可以拥有多个宠物
- 宠物与状态记录关联：一个宠物可以有多个状态记录
- 宠物与活动记录关联：一个宠物可以有多个活动记录

### 5.3 统计信息
- `statusRecordCount`: 该宠物的状态记录总数
- `activityRecordCount`: 该宠物的活动记录总数

### 5.4 性别处理逻辑
- 创建宠物时，`gender`字段可选，默认为`null`
- 查询宠物时，自动计算`genderText`字段：
  - `gender = true` → `genderText = "公"`
  - `gender = false` → `genderText = "母"`
  - `gender = null` → `genderText = "未知"`

## 7. 常见用例

### 7.1 创建雄性宠物
```bash
curl -X POST "http://localhost:8080/api/pets" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "大黄",
    "species": "狗",
    "breed": "中华田园犬",
    "gender": true,
    "userId": 32
  }'
```

### 7.2 创建雌性宠物
```bash
curl -X POST "http://localhost:8080/api/pets" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "小花",
    "species": "猫",
    "breed": "布偶猫",
    "gender": false,
    "userId": 32
  }'
```

### 7.3 创建性别未知的宠物
```bash
curl -X POST "http://localhost:8080/api/pets" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "未知",
    "species": "仓鼠",
    "userId": 32
  }'
```
