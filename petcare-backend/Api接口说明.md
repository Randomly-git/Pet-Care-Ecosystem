# Activity API 接口说明文档

## 基础信息
- **基础URL**: `http://localhost:8080/api/activities`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8

---

## 1. 活动种类管理

### 1.1 获取所有活动种类
**GET** `/kinds`

#### 请求参数
无

#### 响应格式
```json
[
  {
    "activityKindId": 1,
    "activityKindName": "喂养"
  },
  {
    "activityKindId": 2,
    "activityKindName": "互动"
  },
  {
    "activityKindId": 3,
    "activityKindName": "清洁"
  }
]
```

---

## 2. 活动管理

### 2.1 获取用户活动列表
**GET** `/user/{userId}`

#### 路径参数
- `userId` (Long, 必需): 用户ID

#### 查询参数
- `activityKindId` (Long, 可选): 活动种类ID，用于筛选特定种类的活动

#### 请求示例
```
GET /api/activities/user/32
GET /api/activities/user/32?activityKindId=1
```

#### 响应格式
```json
[
  {
    "activityId": 1,
    "activityName": "跑步",
    "activityKindId": 1,
    "activityKindName": "喂养",
    "userId": 32,
    "userName": "testuser_1763386840264",
    "state": 1
  }
]
```

### 2.2 获取活动详情
**GET** `/{activityId}`

#### 路径参数
- `activityId` (Long, 必需): 活动ID

#### 请求示例
```
GET /api/activities/1
```

#### 响应格式
```json
{
  "activityId": 1,
  "activityName": "跑步",
  "activityKindId": 1,
  "activityKindName": "喂养",
  "userId": 32,
  "userName": "testuser_1763386840264",
  "state": 1
}
```

### 2.3 创建新活动
**POST** `/`

#### 请求体
```json
{
  "activityName": "散步活动",
  "activityKindId": 1,
  "userId": 32
}
```

#### 字段说明
- `activityName` (String, 必需): 活动名称，最大长度100字符
- `activityKindId` (Long, 必需): 活动种类ID
- `userId` (Long, 必需): 用户ID

#### 响应格式
```json
{
  "activityId": 2,
  "activityName": "散步活动",
  "activityKind": {
    "activityKindId": 1,
    "activityKindName": "喂养"
  },
  "state": 1
}
```

### 2.4 更新活动信息
**PUT** `/`

#### 请求体
```json
{
  "activityId": 1,
  "activityName": "更新后的散步活动",
  "activityKindId": 2
}
```

#### 字段说明
- `activityId` (Long, 必需): 要更新的活动ID
- `activityName` (String, 可选): 新的活动名称
- `activityKindId` (Long, 可选): 新的活动种类ID

#### 响应格式
```json
{
  "activityId": 1,
  "activityName": "更新后的散步活动",
  "activityKind": {
    "activityKindId": 2,
    "activityKindName": "互动"
  },
  "state": 1
}
```

### 2.5 软删除活动
**DELETE** `/{activityId}`

#### 路径参数
- `activityId` (Long, 必需): 活动ID

#### 请求示例
```
DELETE /api/activities/1
```

#### 响应格式
- 成功: HTTP 200 OK，响应体为空
- 失败: HTTP 404 Not Found

### 2.6 彻底删除活动
**DELETE** `/{activityId}/complete`

#### 路径参数
- `activityId` (Long, 必需): 活动ID

#### 请求示例
```
DELETE /api/activities/1/complete
```

#### 响应格式
- 成功: HTTP 200 OK，响应体为空
- 失败: HTTP 404 Not Found

---

以下是更新后的活动记录管理接口文档，已根据 `ActivityController.java` 中的实现进行了调整，特别是添加了文件上传相关参数：

## 3. 活动记录管理

### 3.1 搜索活动记录
**GET** `/api/activities/records/pet/{petId}`

#### 路径参数
- `petId` (Long, 必需): 宠物ID

#### 查询参数
- `startDate` (LocalDateTime, 可选): 开始时间，格式: `yyyy-MM-dd'T'HH:mm:ss`
- `endDate` (LocalDateTime, 可选): 结束时间，格式: `yyyy-MM-dd'T'HH:mm:ss`
- `activityKindId` (Long, 可选): 活动种类ID

#### 响应格式
```json
[
  {
    "activityRecordId": 1,
    "activityId": 1,
    "activityName": "更新后的散步活动",
    "activityKindId": 2,
    "activityKindName": "互动",
    "petId": 393,
    "activityDescription": "我爱跑步",
    "activityDate": "2025-10-11T10:00:00",
    "mediaFiles": [
      {
        "mediaId": 1,
        "fileName": "walking.jpg",
        "fileUrl": "https://example.com/walking.jpg",
        "fileType": "image/jpeg",
        "fileSize": 204800,
        "uploadTime": "2025-11-18T11:53:24",
        "userId": 123,
        "relatedType": "ACTIVITY",
        "relatedTypeDesc": "活动记录",
        "relatedId": 1
      }
    ],
    "mediaCount": 1,
    "firstMediaUrl": "https://example.com/walking.jpg"
  }
]
```

#### 说明
- 响应中包含关联的媒体文件信息
- `mediaFiles`: 媒体文件列表
- `mediaCount`: 媒体文件总数
- `firstMediaUrl`: 第一个媒体文件的URL（用于列表展示）

### 3.2 创建活动记录（支持文件上传）
**POST** `/api/activities/records/pet/{petId}`

#### 路径参数
- `petId` (Long, 必需): 宠物ID

#### 查询参数
- `activityId` (Long, 必需): 活动ID
- `description` (String, 可选): 活动描述
- `date` (LocalDateTime, 可选): 活动日期，格式: `yyyy-MM-dd'T'HH:mm:ss`（如不提供则使用当前时间）
- `file` (MultipartFile, 可选): 媒体文件（图片/视频）
- `userId` (Long, 必需): 用户ID（用于文件上传权限）

#### 请求示例
**示例1：带文件和描述的活动记录**
```
POST /api/activities/records/pet/393
Content-Type: multipart/form-data

activityId=1
description=今天带宠物散步30分钟
date=2025-11-18T11:53:24
userId=123
file=<walking.jpg>
```

**示例2：不带文件的活动记录**
```
POST /api/activities/records/pet/393
Content-Type: multipart/form-data

activityId=1
description=今天带宠物散步30分钟
date=2025-11-18T11:53:24
userId=123
```

**示例3：只传必要参数**
```
POST /api/activities/records/pet/393
Content-Type: multipart/form-data

activityId=1
userId=123
```

#### 响应格式
```json
{
  "activityRecordId": 51,
  "activityDescription": "今天带宠物散步30分钟",
  "activityDate": "2025-11-18T11:53:24"
}
```

#### 说明
- 文件上传为异步操作，不影响活动记录的创建
- 如果未提供 `date` 参数，将使用当前系统时间
- 如果未提供 `description` 参数，响应中的 `activityDescription` 字段将为 `null`
- 文件上传成功后，会在媒体服务中建立与活动记录的关联

### 3.3 更新活动记录（支持文件更新）
**PUT** `/api/activities/records/{recordId}`

#### 路径参数
- `recordId` (Long, 必需): 活动记录ID

#### 查询参数
- `newActivityId` (Long, 可选): 新的活动ID（如不提供则保持不变）
- `description` (String, 可选): 新的活动描述（如不提供则保持不变，如提供空值则设置为null）
- `date` (LocalDateTime, 可选): 新的活动日期，格式: `yyyy-MM-dd'T'HH:mm:ss`（如不提供则保持不变）
- `file` (MultipartFile, 可选): 新的媒体文件（上传新文件将替换旧文件）
- `userId` (Long, 可选): 用户ID（仅当上传文件时需要）

#### 请求示例
**示例1：更新所有字段并替换文件**
```
PUT /api/activities/records/1
Content-Type: multipart/form-data

newActivityId=2
description=更新后的活动描述
date=2025-11-18T11:53:25
userId=123
file=<new_walking.jpg>
```

**示例2：只更新描述**
```
PUT /api/activities/records/1
Content-Type: multipart/form-data

description=只更新描述字段
```

**示例3：清空描述并更新文件**
```
PUT /api/activities/records/1
Content-Type: multipart/form-data

description=
userId=123
file=<new_file.jpg>
```

#### 响应格式
```json
{
  "activityRecordId": 1,
  "activityDescription": "更新后的活动描述",
  "activityDate": "2025-11-18T11:53:25"
}
```

#### 说明
- 所有查询参数都是可选的，可以只更新需要修改的字段
- 如果 `description` 参数为空字符串，将被设置为 `null`
- 未提供的参数将保持原有值不变
- 上传新文件时会先删除旧的关联文件，然后上传新文件
- 文件更新为异步操作，不影响活动记录的更新

### 3.4 删除活动记录
**DELETE** `/api/activities/records/{recordId}`

#### 路径参数
- `recordId` (Long, 必需): 活动记录ID

#### 请求示例
```
DELETE /api/activities/records/1
```

#### 响应格式
- 成功: HTTP 200 OK，响应体为空
- 失败: HTTP 404 Not Found

#### 说明
- 删除活动记录时会同步删除关联的媒体文件
- 如果媒体文件删除失败，仍会继续删除活动记录

### 3.5 为活动记录上传媒体文件（新增接口）
**POST** `/api/activities/records/{recordId}/media`

#### 路径参数
- `recordId` (Long, 必需): 活动记录ID

#### 查询参数
- `file` (MultipartFile, 必需): 媒体文件（图片/视频）
- `userId` (Long, 必需): 用户ID

#### 请求示例
```
POST /api/activities/records/1/media
Content-Type: multipart/form-data

userId=123
file=<additional_photo.jpg>
```

#### 响应格式
- 成功: HTTP 200 OK，响应体为空
- 失败: HTTP 500 Internal Server Error

#### 说明
- 此接口用于为已存在的活动记录单独上传媒体文件
- 不破坏原有创建/更新接口的返回格式
- 文件上传为同步操作，会立即返回结果

--- 

主要变更点总结：
1. **创建活动记录**：新增 `file` 和 `userId` 参数，支持可选文件上传
2. **更新活动记录**：新增 `file` 和 `userId` 参数，支持文件替换
3. **响应格式**：搜索接口现在包含媒体文件相关信息
4. **新增接口**：添加了单独上传媒体文件的接口
5. **异步处理**：文件上传采用异步方式，不影响主流程

## 4. 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 404 | 资源未找到 |
| 500 | 服务器内部错误 |

---

## 5. 注意事项

1. **时间格式**: 所有日期时间参数必须使用 ISO 8601 格式: `yyyy-MM-dd'T'HH:mm:ss`
2. **软删除 vs 彻底删除**: 
   - 软删除只是将活动状态设为0，数据仍保留在数据库中
   - 彻底删除会删除活动及其所有相关记录
3. **参数验证**: 所有必需参数必须提供，否则返回400错误
4. **数据关联**: 删除活动前请确保没有关联的活动记录，或先删除相关记录

---

## 6. 错误响应格式

当发生错误时，返回格式如下：

```json
{
  "timestamp": "2025-11-18T03:53:24.576+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/activities"
}
```

# Status API 接口说明文档

## 基础信息
- **基础URL**: `http://localhost:8080/api/status`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8

---

## 1. 状态管理

### 1.1 获取用户有效状态列表
**GET** `/user/{userId}`

#### 路径参数
- `userId` (Long, 必需): 用户ID

#### 请求示例
```
GET /api/status/user/32
```

#### 响应格式
```json
[
  {
    "statusId": 1,
    "statusName": "健康",
    "state": 1
  },
  {
    "statusId": 2,
    "statusName": "生病",
    "state": 1
  },
  {
    "statusId": 3,
    "statusName": "怀孕",
    "state": 1
  }
]
```

### 1.2 创建新状态
**POST** `/`

#### 查询参数
- `userId` (Long, 必需): 用户ID
- `statusName` (String, 必需): 状态名称

#### 请求示例
```
POST /api/status?userId=32&statusName=康复中
```

#### 响应格式
```json
{
  "statusId": 4,
  "statusName": "康复中",
  "state": 1
}
```

### 1.3 更新状态名称
**PUT** `/{statusId}/name`

#### 路径参数
- `statusId` (Long, 必需): 状态ID

#### 查询参数
- `newName` (String, 必需): 新的状态名称

#### 请求示例
```
PUT /api/status/1/name?newName=非常健康
```

#### 响应格式
```json
{
  "statusId": 1,
  "statusName": "非常健康",
  "state": 1
}
```

### 1.4 软删除状态
**DELETE** `/{statusId}`

#### 路径参数
- `statusId` (Long, 必需): 状态ID

#### 请求示例
```
DELETE /api/status/1
```

#### 响应格式
- 成功: HTTP 204 No Content
- 失败: HTTP 500 Internal Server Error

### 1.5 彻底删除状态及其记录
**DELETE** `/{statusId}/with-records`

#### 路径参数
- `statusId` (Long, 必需): 状态ID

#### 请求示例
```
DELETE /api/status/1/with-records
```

#### 响应格式
- 成功: HTTP 204 No Content
- 失败: HTTP 500 Internal Server Error

---




## 2. 状态记录管理

### 2.1 获取宠物所有状态记录（带媒体文件信息）
**GET** `/api/status/records/pet/{petId}`

#### 路径参数
- `petId` (Long, 必需): 宠物ID

#### 请求示例
```
GET /api/status/records/pet/393
```

#### 响应格式
```json
[
  {
    "statusRecordId": 1,
    "statusId": 1,
    "statusName": "健康",
    "petId": 393,
    "startDate": "2024-01-01",
    "endDate": null,
    "statusDescription": "宠物健康状况良好",
    "mediaFiles": [
      {
        "mediaId": 1,
        "fileName": "health_report.pdf",
        "fileUrl": "https://example.com/health_report.pdf",
        "fileType": "application/pdf",
        "fileSize": 204800,
        "uploadTime": "2024-01-01T10:30:00",
        "userId": 32,
        "relatedType": "STATUS",
        "relatedTypeDesc": "状态记录",
        "relatedId": 1
      }
    ],
    "mediaCount": 1,
    "firstMediaUrl": "https://example.com/health_report.pdf"
  }
]
```

#### 说明
- 响应中包含关联的媒体文件信息
- `mediaFiles`: 媒体文件列表
- `mediaCount`: 媒体文件总数
- `firstMediaUrl`: 第一个媒体文件的URL（用于列表展示）

### 2.2 获取某天活跃状态记录（带媒体文件信息）
**GET** `/api/status/records/active`

#### 查询参数
- `petId` (Long, 必需): 宠物ID
- `targetDate` (LocalDate, 必需): 目标日期，格式: `yyyy-MM-dd`

#### 请求示例
```
GET /api/status/records/active?petId=393&targetDate=2024-01-16
```

#### 响应格式
```json
[
  {
    "statusRecordId": 2,
    "statusId": 2,
    "statusName": "生病",
    "petId": 393,
    "startDate": "2024-01-15",
    "endDate": "2024-01-20",
    "statusDescription": "感冒发烧",
    "mediaFiles": [],
    "mediaCount": 0,
    "firstMediaUrl": null
  }
]
```

### 2.3 创建状态记录（支持文件上传）
**POST** `/api/status/records`

#### 请求格式
`multipart/form-data`

#### 请求参数
- `statusId` (Long, 必需): 状态ID
- `petId` (Long, 必需): 宠物ID
- `description` (String, 可选): 状态描述
- `startDate` (LocalDate, 必需): 开始日期，格式: `yyyy-MM-dd`
- `file` (MultipartFile, 可选): 媒体文件（图片/文档/视频等）
- `userId` (Long, 必需): 用户ID（用于文件上传权限）

#### 请求示例
**示例1：带文件和描述的状态记录**
```
POST /api/status/records
Content-Type: multipart/form-data

statusId=1
petId=393
description=宠物恢复健康状态
startDate=2024-01-21
userId=32
file=<health_report.pdf>
```

**示例2：不带文件的状态记录**
```
POST /api/status/records
Content-Type: multipart/form-data

statusId=1
petId=393
description=宠物恢复健康状态
startDate=2024-01-21
userId=32
```

**示例3：只传必要参数**
```
POST /api/status/records
Content-Type: multipart/form-data

statusId=1
petId=393
startDate=2024-01-21
userId=32
```

#### 响应格式
```json
{
  "statusRecordId": 3,
  "statusDescription": "宠物恢复健康状态",
  "startDate": "2024-01-21",
  "endDate": null
}
```

#### 说明
- 文件上传为异步操作，不影响状态记录的创建
- 如果未提供 `description` 参数，响应中的 `statusDescription` 字段将为 `null`
- 文件上传成功后，会在媒体服务中建立与状态记录的关联

### 2.4 更新状态记录（支持文件更新）
**PUT** `/api/status/records/{statusRecordId}`

#### 路径参数
- `statusRecordId` (Long, 必需): 状态记录ID

#### 请求格式
`multipart/form-data`

#### 请求参数
- `description` (String, 可选): 新的状态描述
- `startDate` (LocalDate, 可选): 新的开始日期，格式: `yyyy-MM-dd`
- `endDate` (LocalDate, 可选): 新的结束日期，格式: `yyyy-MM-dd`
- `file` (MultipartFile, 可选): 新的媒体文件（上传新文件将替换旧文件）
- `userId` (Long, 可选): 用户ID（仅当上传文件时需要）

#### 请求示例
**示例1：更新所有字段并替换文件**
```
PUT /api/status/records/1
Content-Type: multipart/form-data

description=更新后的状态描述
startDate=2024-01-22
endDate=2024-01-25
userId=32
file=<updated_report.pdf>
```

**示例2：只更新描述**
```
PUT /api/status/records/1
Content-Type: multipart/form-data

description=只更新描述字段
```

**示例3：清空描述并更新文件**
```
PUT /api/status/records/1
Content-Type: multipart/form-data

description=
userId=32
file=<new_file.jpg>
```

#### 响应格式
```json
{
  "statusRecordId": 1,
  "statusDescription": "更新后的状态描述",
  "startDate": "2024-01-22",
  "endDate": "2024-01-25"
}
```

#### 说明
- 所有查询参数都是可选的，可以只更新需要修改的字段
- 如果 `description` 参数为空字符串，将被设置为 `null`
- 未提供的参数将保持原有值不变
- 上传新文件时会先删除旧的关联文件，然后上传新文件
- 文件更新为异步操作，不影响状态记录的更新

### 2.5 停止状态记录
**PUT** `/api/status/records/{statusRecordId}/stop`

#### 路径参数
- `statusRecordId` (Long, 必需): 状态记录ID

#### 查询参数
- `endDate` (LocalDate, 必需): 结束日期，格式: `yyyy-MM-dd`

#### 请求示例
```
PUT /api/status/records/1/stop?endDate=2024-01-25
```

#### 响应格式
```json
{
  "statusRecordId": 1,
  "statusDescription": "宠物恢复健康状态",
  "startDate": "2024-01-21",
  "endDate": "2024-01-25"
}
```

### 2.6 删除状态记录
**DELETE** `/api/status/records/{statusRecordId}`

#### 路径参数
- `statusRecordId` (Long, 必需): 状态记录ID

#### 请求示例
```
DELETE /api/status/records/1
```

#### 响应格式
- 成功: HTTP 204 No Content
- 失败: HTTP 500 Internal Server Error

#### 说明
- 删除状态记录时会同步删除关联的媒体文件
- 如果媒体文件删除失败，仍会继续删除状态记录

### 2.7 为状态记录上传媒体文件（新增接口）
**POST** `/api/status/records/{statusRecordId}/media`

#### 路径参数
- `statusRecordId` (Long, 必需): 状态记录ID

#### 请求格式
`multipart/form-data`

#### 请求参数
- `file` (MultipartFile, 必需): 媒体文件（图片/文档/视频等）
- `userId` (Long, 必需): 用户ID

#### 请求示例
```
POST /api/status/records/1/media
Content-Type: multipart/form-data

userId=32
file=<additional_report.pdf>
```

#### 响应格式
- 成功: HTTP 200 OK，响应体为空
- 失败: HTTP 500 Internal Server Error

#### 说明
- 此接口用于为已存在的状态记录单独上传媒体文件
- 文件上传为同步操作，会立即返回结果
- 不影响原有创建/更新接口的返回格式
---

## 主要变更点总结

1. **创建状态记录**：
   - 从 JSON 请求体改为 `multipart/form-data`
   - 新增 `file` 和 `userId` 参数
   - 支持可选文件上传

2. **更新状态记录**：
   - 从 JSON 请求体改为 `multipart/form-data`
   - 新增 `file` 和 `userId` 参数
   - 支持文件替换

3. **新增接口**：
   - 添加了单独上传媒体文件的接口

4. **响应增强**：
   - 获取接口现在包含媒体文件信息

5. **删除优化**：
   - 删除状态记录时自动删除关联的媒体文件
   - 删除状态时会同时删除所有相关记录的媒体文件

---

## 3. 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 204 | 删除成功，无返回内容 |
| 400 | 请求参数错误 |
| 404 | 资源未找到 |
| 500 | 服务器内部错误 |

---

## 4. 数据模型说明

### 4.1 状态 (Status)
```json
{
  "statusId": "状态ID (Long)",
  "statusName": "状态名称 (String)",
  "state": "状态标识 (Integer, 1:有效, 0:已删除)"
}
```

### 4.2 状态记录 (StatusRecord)
```json
{
  "statusRecordId": "状态记录ID (Long)",
  "statusId": "状态ID (Long)",
  "statusName": "状态名称 (String)",
  "petId": "宠物ID (Long)",
  "startDate": "开始日期 (LocalDate)",
  "endDate": "结束日期 (LocalDate, 可为null表示进行中)",
  "statusDescription": "状态描述 (String)"
}
```

---

## 5. 业务逻辑说明

### 5.1 状态记录生命周期
1. **创建**: 只设置 `startDate`，`endDate` 为 null
2. **进行中**: `endDate` 为 null 的记录表示当前活跃状态
3. **停止**: 设置 `endDate` 表示状态结束
4. **更新**: 可以修改状态、日期和描述

### 5.2 活跃状态判断
- 活跃状态记录：`startDate <= targetDate` 且 (`endDate` 为 null 或 `endDate >= targetDate`)
- 使用 `/records/active` 接口可查询特定日期的活跃状态

### 5.3 删除策略
- **软删除**: 仅标记状态为已删除，数据保留
- **彻底删除**: 删除状态及其所有相关记录

---

## 6. 使用场景示例

### 6.1 记录宠物生病过程
```
1. POST /api/status/records - 创建生病状态记录
2. PUT /api/status/records/{id}/stop - 宠物康复时停止记录
```

### 6.2 查看宠物当前状态
```
GET /api/status/records/active?petId=393&targetDate=2024-01-18
```

### 6.3 管理用户自定义状态
```
1. POST /api/status - 创建新状态
2. GET /api/status/user/32 - 查看用户所有状态
3. DELETE /api/status/1 - 删除不再使用的状态
```

---

## 7. 注意事项

1. **日期格式**: 所有日期参数必须使用 `yyyy-MM-dd` 格式
2. **状态关联**: 删除状态前请确认无关联的状态记录
3. **数据完整性**: 彻底删除操作不可逆，请谨慎使用
4. **活跃状态**: 一个宠物在同一个日期可能有多个活跃状态记录

# Auth API 接口说明文档

## 基础信息
- **基础URL**: `http://localhost:8080/api/auth`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8
- **跨域支持**: 允许所有域名跨域访问

---

## 1. 用户认证接口

### 1.1 用户注册
**POST** `/register`

#### 请求体
```json
{
  "name": "testuser",
  "password": "password123"
}
```

#### 字段说明
- `name` (String, 必需): 用户名，长度1-50个字符
- `password` (String, 必需): 密码，长度1-100个字符

#### 请求示例
```bash
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "testuser",
    "password": "password123"
  }'
```

#### 成功响应格式
```json
{
  "success": true,
  "message": "注册成功",
  "data": {
    "userId": 32,
    "name": "testuser",
    "message": "注册成功"
  },
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

#### 错误响应格式
```json
{
  "success": false,
  "message": "用户名 'testuser' 已存在",
  "data": null,
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

### 1.2 用户登录
**POST** `/login`

#### 请求体
```json
{
  "name": "testuser",
  "password": "password123"
}
```

#### 字段说明
- `name` (String, 必需): 用户名，不能为空
- `password` (String, 必需): 密码，不能为空

#### 请求示例
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "testuser",
    "password": "password123"
  }'
```

#### 成功响应格式
```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "userId": 32,
    "name": "testuser",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "message": "登录成功"
  },
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

#### 错误响应格式
```json
{
  "success": false,
  "message": "用户名或密码错误",
  "data": null,
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

---

## 2. 数据模型说明

### 2.1 请求数据模型

#### LoginRequest (登录请求)
```json
{
  "name": "用户名 (String, 必需)",
  "password": "密码 (String, 必需)"
}
```

#### RegisterRequest (注册请求)
```json
{
  "name": "用户名 (String, 必需, 1-50字符)",
  "password": "密码 (String, 必需, 1-100字符)"
}
```

### 2.2 响应数据模型

#### LoginResponse (登录响应)
```json
{
  "userId": "用户ID (Long)",
  "name": "用户名 (String)",
  "token": "认证令牌 (String)",
  "message": "响应消息 (String)"
}
```

#### RegisterResponse (注册响应)
```json
{
  "userId": "用户ID (Long)",
  "name": "用户名 (String)",
  "message": "响应消息 (String)"
}
```

### 2.3 统一响应格式 (ApiResponse)
```json
{
  "success": "请求是否成功 (Boolean)",
  "message": "响应消息 (String)",
  "data": "响应数据 (Object)",
  "timestamp": "响应时间戳 (String)"
}
```

---

## 3. 状态码说明

| 状态码 | 说明 | 典型场景 |
|--------|------|----------|
| 200 | 请求成功 | 登录成功 |
| 201 | 创建成功 | 注册成功 |
| 400 | 请求参数错误 | 参数验证失败 |
| 401 | 未授权 | 用户名或密码错误 |
| 409 | 冲突 | 用户名已存在 |
| 500 | 服务器内部错误 | 数据库操作失败 |

---

## 4. 错误处理

### 4.1 异常类型

#### UserAlreadyExistsException
- **触发条件**: 注册时用户名已存在
- **HTTP状态码**: 409 Conflict
- **错误消息**: `"用户名 '{username}' 已存在"`

#### InvalidCredentialsException
- **触发条件**: 登录时用户名或密码错误
- **HTTP状态码**: 401 Unauthorized
- **错误消息**: `"用户名或密码错误"`

### 4.2 参数验证错误
当请求参数不符合验证规则时，返回400错误：
```json
{
  "success": false,
  "message": "用户名不能为空",
  "data": null,
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

---

## 5. 业务逻辑说明

### 5.1 注册流程
1. 验证用户名和密码格式
2. 检查用户名是否已存在
3. 创建新用户账户
4. 返回用户ID和成功消息

### 5.2 登录流程
1. 验证用户名和密码格式
2. 检查用户名和密码是否匹配
3. 生成认证令牌
4. 返回用户信息和令牌

### 5.3 安全考虑
- 密码在传输过程中应使用HTTPS加密
- 密码在数据库中应进行哈希存储
- 令牌应设置合理的过期时间

---

## 6. 使用示例

### 6.1 完整注册流程
```javascript
// 1. 用户注册
const registerResponse = await fetch('/api/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    name: 'newuser',
    password: 'securepassword'
  })
});

// 2. 处理响应
if (registerResponse.ok) {
  const result = await registerResponse.json();
  console.log('注册成功，用户ID:', result.data.userId);
} else {
  const error = await registerResponse.json();
  console.log('注册失败:', error.message);
}
```

### 6.2 完整登录流程
```javascript
// 1. 用户登录
const loginResponse = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    name: 'testuser',
    password: 'password123'
  })
});

// 2. 处理响应
if (loginResponse.ok) {
  const result = await loginResponse.json();
  const token = result.data.token;
  
  // 存储token用于后续请求
  localStorage.setItem('authToken', token);
  console.log('登录成功，用户:', result.data.name);
} else {
  const error = await loginResponse.json();
  console.log('登录失败:', error.message);
}
```

---

## 7. 注意事项

1. **密码安全**: 确保使用强密码策略
2. **HTTPS**: 在生产环境中必须使用HTTPS
3. **令牌存储**: 客户端应安全存储认证令牌
4. **输入验证**: 客户端和服务端都应进行输入验证
5. **错误信息**: 避免在错误响应中泄露敏感信息

---

## 8. 后续请求认证

登录成功后，后续请求需要在Header中携带token：
```http
Authorization: Bearer {token}
```

示例：
```bash
curl -X GET "http://localhost:8080/api/user/profile" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

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


# 定时活动管理 API 文档

## 接口基础信息

- **基础URL**: `http://localhost:8080/api/fixed-activities`
- **认证方式**: 无认证（根据实际需求添加）
- **数据格式**: JSON
- **字符编码**: UTF-8

---

## API 接口列表

### 1. 创建定时活动

创建新的定时活动记录，同一个宠物下不能创建两个活动ID相同的定时活动。

- **URL**: `POST /api/fixed-activities`
- **请求头**: `Content-Type: application/json`

#### 请求参数

```json
{
  "petId": 1,
  "activityId": 101,
  "gapTime": 7
}
```
| 字段名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| petId | Long | 是 | 宠物ID | 1 |
| activityId | Long | 是 | 活动ID | 101 |
| gapTime | Integer | 是 | 活动间隔时间（天） | 7 |

#### 响应示例

**成功响应 (201 Created)**
```json
{
  "fixedActivityId": 1,
  "petId": 1,
  "activityId": 101,
  "gapTime": 7,
}
```
---

### 2. 修改定时活动间隔时间

修改指定定时活动的间隔时间。

- **URL**: `PUT /api/fixed-activities/{fixedActivityId}`
- **请求头**: `Content-Type: application/json`

#### 路径参数

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| fixedActivityId | Long | 是 | 定时活动ID | 1 |

#### 请求参数

```json
{
  "gapTime": 14
}
```

| 字段名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| gapTime | Integer | 是 | 新的间隔时间（天） | 14 |

#### 响应示例

**成功响应 (200 OK)**
```json
{
  "fixedActivityId": 1,
  "petId": 1,
  "activityId": 101,
  "gapTime": 14,
}
```

---

### 3. 查看宠物所有定时活动

获取指定宠物的所有定时活动列表，包含活动名称、宠物名称等详细信息。

- **URL**: `GET /api/fixed-activities/pet/{petId}`

#### 路径参数

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| petId | Long | 是 | 宠物ID | 1 |

#### 响应示例

**成功响应 (200 OK)**
```json
[
  {
    "fixedActivityId": 1,
    "activityId": 101,
    "activityName": "喂食",
    "activityKindId": 1,
    "activityKindName": "日常护理",
    "petId": 1,
    "petName": "旺财",
    "gapTime": 7,
    "nextReminderDate": "2023-10-08"
  },
  {
    "fixedActivityId": 2,
    "activityId": 102,
    "activityName": "洗澡",
    "activityKindId": 1,
    "activityKindName": "日常护理",
    "petId": 1,
    "petName": "旺财",
    "gapTime": 14,
    "nextReminderDate": "2023-10-15"
  }
]
```

**空数据响应 (200 OK)**
```json
[]
```

---

### 4. 删除定时活动

删除指定的定时活动，同时会删除对应的提醒记录。

- **URL**: `DELETE /api/fixed-activities/{fixedActivityId}`

#### 路径参数

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| fixedActivityId | Long | 是 | 定时活动ID | 1 |

#### 响应示例

**成功响应 (204 No Content)**
```
无响应体
```

---

## 使用示例

### cURL 示例

```bash
# 1. 创建定时活动
curl -X POST "http://localhost:8080/api/fixed-activities" \
  -H "Content-Type: application/json" \
  -d '{"petId": 1, "activityId": 101, "gapTime": 7}'

# 2. 查询宠物定时活动
curl -X GET "http://localhost:8080/api/fixed-activities/pet/1"

# 3. 修改间隔时间
curl -X PUT "http://localhost:8080/api/fixed-activities/1" \
  -H "Content-Type: application/json" \
  -d '{"gapTime": 14}'

# 4. 删除定时活动
curl -X DELETE "http://localhost:8080/api/fixed-activities/1"
```
# 预约活动管理 API 文档

## 接口基础信息

- **基础URL**: `http://localhost:8080/api/reserved-activities`
- **认证方式**: 无认证
- **数据格式**: JSON
- **字符编码**: UTF-8

---

## API 接口列表

### 1. 创建预约活动

创建新的预约活动记录。

- **URL**: `POST /api/reserved-activities`
- **请求头**: `Content-Type: application/json`

#### 请求参数

```json
{
  "activityId": 201,
  "petId": 1,
  "reminderDate": "2023-10-10"
}
```

| 字段名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| activityId | Long | 是 | 活动ID | 201 |
| petId | Long | 是 | 宠物ID | 1 |
| reminderDate | LocalDate | 是 | 提醒日期 | "2023-10-10" |

#### 响应示例

**成功响应 (200 OK)**
```json
{
  "activityReminderId": 1,
  "activityId": 201,
  "reminderDate": "2023-10-10",
  "type": 2,
  "petId": 1
}
```

**错误响应 (400 Bad Request)**
```json
{
  "timestamp": "2023-10-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "请求参数验证失败",
  "path": "/api/reserved-activities"
}
```

---

### 2. 查看宠物所有预约活动

获取指定宠物的所有预约活动列表，包含活动名称等详细信息。

- **URL**: `GET /api/reserved-activities/pet/{petId}`

#### 路径参数

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| petId | Long | 是 | 宠物ID | 1 |

#### 响应示例

**成功响应 (200 OK)**
```json
[
  {
    "activityReminderId": 1,
    "activityId": 201,
    "petId": 1,
    "reminderDate": "2023-10-10",
    "activityName": "兽医检查"
  },
  {
    "activityReminderId": 2,
    "activityId": 202,
    "petId": 1,
    "reminderDate": "2023-10-15",
    "activityName": "美容护理"
  }
]
```

**空数据响应 (200 OK)**
```json
[]
```

---

### 3. 修改预约活动日期

修改指定预约活动的提醒日期。

- **URL**: `PUT /api/reserved-activities/date`
- **请求头**: `Content-Type: application/json`

#### 请求参数

```json
{
  "activityReminderId": 1,
  "reminderDate": "2023-10-12"
}
```

| 字段名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| activityReminderId | Long | 是 | 活动提醒ID | 1 |
| reminderDate | LocalDate | 是 | 新的提醒日期 | "2023-10-12" |

#### 响应示例

**成功响应 (200 OK)**
```json
{
  "activityReminderId": 1,
  "activityId": 201,
  "reminderDate": "2023-10-12",
  "type": 2,
  "petId": 1
}
```

**错误响应 (404 Not Found)**
```json
{
  "timestamp": "2023-10-01T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "预约活动不存在",
  "path": "/api/reserved-activities/date"
}
```

---

### 4. 删除预约活动

删除指定的预约活动。

- **URL**: `DELETE /api/reserved-activities/{activityReminderId}`

#### 路径参数

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| activityReminderId | Long | 是 | 活动提醒ID | 1 |

#### 响应示例

**成功响应 (200 OK)**
```
无响应体
```

**错误响应 (404 Not Found)**
```json
{
  "timestamp": "2023-10-01T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "预约活动不存在",
  "path": "/api/reserved-activities/1"
}
```

## 业务规则

1. **类型标识**: 预约活动固定使用 `type = 2`（一次性提醒）
2. **日期格式**: 所有日期字段使用 `yyyy-MM-dd` 格式
3. **数据关联**: 通过 `activityId` 关联活动基础信息
4. **宠物关联**: 通过 `petId` 关联宠物信息

---

## 错误码说明

| HTTP状态码 | 错误码 | 描述 |
|------------|--------|------|
| 400 | BAD_REQUEST | 请求参数验证失败 |
| 404 | NOT_FOUND | 资源不存在 |
| 500 | INTERNAL_SERVER_ERROR | 服务器内部错误 |

---

## 使用示例

### cURL 示例

```bash
# 1. 创建预约活动
curl -X POST "http://localhost:8080/api/reserved-activities" \
  -H "Content-Type: application/json" \
  -d '{"activityId": 201, "petId": 1, "reminderDate": "2023-10-10"}'

# 2. 查询宠物预约活动
curl -X GET "http://localhost:8080/api/reserved-activities/pet/1"

# 3. 修改预约活动日期
curl -X PUT "http://localhost:8080/api/reserved-activities/date" \
  -H "Content-Type: application/json" \
  -d '{"activityReminderId": 1, "reminderDate": "2023-10-12"}'

# 4. 删除预约活动
curl -X DELETE "http://localhost:8080/api/reserved-activities/1"
```

### JavaScript Fetch 示例

```javascript
// 创建预约活动
const createReservedActivity = async () => {
  const response = await fetch('http://localhost:8080/api/reserved-activities', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      activityId: 201,
      petId: 1,
      reminderDate: '2023-10-10'
    })
  });
  const result = await response.json();
  console.log('创建结果:', result);
  return result.activityReminderId;
};

// 查询宠物预约活动
const getReservedActivities = async (petId) => {
  const response = await fetch(`http://localhost:8080/api/reserved-activities/pet/${petId}`);
  const result = await response.json();
  console.log('预约活动列表:', result);
  return result;
};

// 修改预约活动日期
const updateReservedActivity = async (activityReminderId, newDate) => {
  const response = await fetch('http://localhost:8080/api/reserved-activities/date', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      activityReminderId: activityReminderId,
      reminderDate: newDate
    })
  });
  const result = await response.json();
  console.log('修改结果:', result);
  return result;
};

// 删除预约活动
const deleteReservedActivity = async (activityReminderId) => {
  const response = await fetch(`http://localhost:8080/api/reserved-activities/${activityReminderId}`, {
    method: 'DELETE'
  });
  console.log('删除成功，状态:', response.status);
};
```


