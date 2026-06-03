# PetCare 后端服务 API 文档

## 基础信息
- **基础URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8

---

## 一、活动管理 (Activity)

### 基础URL: `/api/activities`

### 1.1 获取所有活动种类
**GET** `/kinds`

#### 响应格式
```json
[
  { "activityKindId": 1, "activityKindName": "喂养" },
  { "activityKindId": 2, "activityKindName": "互动" },
  { "activityKindId": 3, "activityKindName": "清洁" }
]
```

### 1.2 获取用户活动列表
**GET** `/user/{userId}`

#### 查询参数
- `activityKindId` (Long, 可选): 活动种类ID

### 1.3 获取活动详情
**GET** `/{activityId}`

### 1.4 创建新活动
**POST** `/`

#### 请求体
```json
{
  "activityName": "散步活动",
  "activityKindId": 1,
  "userId": 32
}
```

### 1.5 更新活动信息
**PUT** `/`

### 1.6 软删除活动
**DELETE** `/{activityId}`

### 1.7 彻底删除活动
**DELETE** `/{activityId}/complete`

---

## 二、活动记录管理 (Activity Records)

### 基础URL: `/api/activities/records`

### 2.1 搜索活动记录
**GET** `/pet/{petId}`

#### 查询参数
- `startDate` (LocalDateTime, 可选): 开始时间
- `endDate` (LocalDateTime, 可选): 结束时间
- `activityKindId` (Long, 可选): 活动种类ID

### 2.2 创建活动记录（支持文件上传）
**POST** `/pet/{petId}`

#### 请求格式: `multipart/form-data`

| 参数名 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| activityId | Long | 是 | 活动ID |
| description | String | 否 | 活动描述 |
| date | LocalDateTime | 否 | 活动日期 |
| file | MultipartFile | 否 | 媒体文件 |
| userId | Long | 是 | 用户ID |

### 2.3 更新活动记录（支持文件更新）
**PUT** `/{recordId}`

### 2.4 删除活动记录
**DELETE** `/{recordId}`

### 2.5 为活动记录上传媒体文件
**POST** `/{recordId}/media`

---

## 三、状态管理 (Status)

### 基础URL: `/api/status`

### 3.1 获取用户有效状态列表
**GET** `/user/{userId}`

### 3.2 创建新状态
**POST** `/`

### 3.3 更新状态名称
**PUT** `/{statusId}/name`

### 3.4 软删除状态
**DELETE** `/{statusId}`

### 3.5 彻底删除状态及其记录
**DELETE** `/{statusId}/with-records`

---

## 四、状态记录管理 (Status Records)

### 基础URL: `/api/status/records`

### 4.1 获取宠物所有状态记录
**GET** `/pet/{petId}`

### 4.2 获取某天活跃状态记录
**GET** `/active?petId={petId}&targetDate={date}`

### 4.3 创建状态记录（支持文件上传）
**POST** `/` (multipart/form-data)

### 4.4 更新状态记录（支持文件更新）
**PUT** `/{statusRecordId}` (multipart/form-data)

### 4.5 停止状态记录
**PUT** `/{statusRecordId}/stop?endDate={date}`

### 4.6 删除状态记录
**DELETE** `/{statusRecordId}`

### 4.7 为状态记录上传媒体文件
**POST** `/{statusRecordId}/media`

---

## 五、用户认证 (Auth)

### 基础URL: `/api/auth`

### 5.1 用户注册
**POST** `/register`

```json
{
  "name": "testuser",
  "password": "password123"
}
```

### 5.2 用户登录
**POST** `/login`

```json
{
  "name": "testuser",
  "password": "password123"
}
```

#### 成功响应
```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "userId": 32,
    "name": "testuser",
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

#### 统一响应格式 (ApiResponse)
```json
{
  "success": true,
  "message": "响应消息",
  "data": {},
  "timestamp": "2024-01-18T12:00:00.000Z"
}
```

### 5.3 后续请求认证
```
Authorization: Bearer {token}
```

---

## 六、宠物管理 (Pet)

### 基础URL: `/api/pets`

### 6.1 创建宠物
**POST** `/`

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

### 6.2 根据ID获取宠物信息
**GET** `/{petId}`

### 6.3 根据用户ID获取宠物列表
**GET** `/user/{userId}`

### 6.4 获取所有宠物
**GET** `/`

---

## 七、定时活动管理 (Fixed Activities)

### 基础URL: `/api/fixed-activities`

### 7.1 创建定时活动
**POST** `/`

```json
{
  "petId": 1,
  "activityId": 101,
  "gapTime": 7
}
```

### 7.2 修改定时活动间隔时间
**PUT** `/{fixedActivityId}`

### 7.3 查看宠物所有定时活动
**GET** `/pet/{petId}`

### 7.4 删除定时活动
**DELETE** `/{fixedActivityId}`

---

## 八、预约活动管理 (Reserved Activities)

### 基础URL: `/api/reserved-activities`

### 8.1 创建预约活动
**POST** `/`

```json
{
  "activityId": 201,
  "petId": 1,
  "reminderDate": "2023-10-10"
}
```

### 8.2 查看宠物所有预约活动
**GET** `/pet/{petId}`

### 8.3 修改预约活动日期
**PUT** `/date`

---

## 九、状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 204 | 删除成功，无返回内容 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 404 | 资源未找到 |
| 409 | 冲突（如用户名已存在） |
| 500 | 服务器内部错误 |
