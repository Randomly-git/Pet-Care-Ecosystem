


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
