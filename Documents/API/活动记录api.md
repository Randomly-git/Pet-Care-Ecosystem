




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
        "relatedId": 1,
        "status": "Hot",
        "lastAccessTime": "2025-11-18T11:53:24"
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
- `status`: 冷热状态（Hot=热数据，Cold=冷数据）
- `lastAccessTime`: 最后访问时间，用于社区动态的7天倒计时归档

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

## 冷热数据分离机制

### 归档策略

系统对媒体文件实行冷热数据分离管理，以优化存储成本：

| 数据类型 | 归档条件 | 访问时重置倒计时 |
|---------|---------|----------------|
| 活动记录 (ACTIVITY) | 超过30天（按上传时间） | 否 |
| 状态记录 (STATUS) | 超过30天（按上传时间） | 否 |
| 社区动态 (MOMENT) | 超过7天未访问（按最后访问时间） | 是 |
| 用户头像 (USER_AVATAR) | 不参与归档 | 不适用 |

### 媒体文件状态

响应中的 `status` 字段说明：
- `Hot`: 热数据，可直接访问
- `Cold`: 冷数据，已归档到低成本存储，首次访问需要恢复

### lastAccessTime 字段说明

- **活动/状态记录**: 该字段为上传时间的副本，用于记录文件上传时间，归档不受访问影响
- **社区动态**: 每次访问该动态都会更新 `lastAccessTime`，重置7天倒计时

--- 

主要变更点总结：
1. **创建活动记录**：新增 `file` 和 `userId` 参数，支持可选文件上传
2. **更新活动记录**：新增 `file` 和 `userId` 参数，支持文件替换
3. **响应格式**：搜索接口现在包含媒体文件相关信息
4. **新增接口**：添加了单独上传媒体文件的接口
5. **异步处理**：文件上传采用异步方式，不影响主流程ackEdit中文版](https://stackedit.cn/).
