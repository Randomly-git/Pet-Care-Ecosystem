# 媒体文件微服务 API 参考

**服务名称:** Media Backend Service
**基础路径 (Base URL):** `[您的服务IP/域名]:[端口]/api/media`
**成功业务码:** `20000`

---

## 统一 API 响应格式 (ApiResponse<T>)

所有 API 接口均返回统一的 JSON 结构，其中 HTTP 状态码用于表示连接或请求的基本状态 (200 OK, 400 Bad Request, 500 Internal Error)，而 `code` 字段用于表示业务处理结果。

| 字段 | 类型 | 描述 |
| :--- | :--- | :--- |
| `code` | `integer` | **业务状态码**。`20000` 表示成功；`4xxxx` 表示业务错误（如参数错误、资源未找到）；`5xxxx` 表示系统错误（如文件读写失败）。 |
| `message` | `string` | 响应消息或错误描述。 |
| `data` | `T` | 业务数据主体，成功时为 `MediaResponse` 或列表，失败时为 `null`。 |

---

## 1. 文件上传

| 属性 | 描述 |
| :--- | :--- |
| **URL** | `POST /api/media/upload` |
| **请求类型** | `multipart/form-data` |

### 请求参数 (Query/Form Data)

| 参数名 | 类型 | 描述 | 必需 |
| :--- | :--- | :--- | :--- |
| `file` | `file` | 要上传的媒体文件。 | 是 |
| `petId` | `long` | 关联的宠物 ID。 | 是 |
| `relatedType` | `string` | 关联的业务类型 (如 `ACTIVITY`, `MOMENT`, `PET_AVATAR`)。 | 是 |
| `relatedId` | `long` | 关联的业务记录 ID (如活动 ID，动态 ID)。 | 是 |

### 成功响应示例 (HTTP 200)

```json
{
  "code": 20000,
  "message": "文件上传成功",
  "data": {
    "mediaId": 123,
    "fileName": "avatar.png",
    "fileUrl": "[https://cos.url/pet_1/PET_AVATAR_1.png](https://cos.url/pet_1/PET_AVATAR_1.png)",
    "fileType": "image/png",
    "fileSize": 51200,
    "uploadTime": "2025-12-03T10:00:00",
    "petId": 1,
    "relatedType": "PET_AVATAR",
    "relatedTypeDesc": "宠物头像",
    "relatedId": 1
  }
}
```

### 失败响应示例 (HTTP 400)

JSON

```
{
  "code": 40001,
  "message": "无效的关联类型: INVALID_TYPE，有效值: ACTIVITY, STATUS, MOMENT, PET_AVATAR",
  "data": null
}
```

------

## 2. 文件信息查询

| **属性**     | **描述**                   |
| ------------ | -------------------------- |
| **URL**      | `GET /api/media/{mediaId}` |
| **请求类型** | `application/json`         |

### Path 参数

| **参数名** | **类型** | **描述**      |
| ---------- | -------- | ------------- |
| `mediaId`  | `long`   | 媒体文件 ID。 |

### 成功响应

返回 `ApiResponse<MediaResponse>`，`data` 字段为单个媒体文件对象。

**URL:** `GET /api/media/pet/{petId}` **成功响应格式**

**(`ApiResponse<MediaResponse>`):**

JSON

```
{
  "code": 20000,
  "message": "文件信息查询成功",
  "data": {
    "mediaId": 123,
    "fileName": "avatar.png",
    "fileUrl": "https://cos.url/pet_1/PET_AVATAR_1.png",
    "fileType": "image/png",
    "fileSize": 51200,
    "uploadTime": "2025-12-03T10:00:00",
    "petId": 1,
    "relatedType": "PET_AVATAR",
    "relatedTypeDesc": "宠物头像",
    "relatedId": 1
  }
}
```

------

## 3. 按宠物查询文件

| **属性**     | **描述**                     |
| ------------ | ---------------------------- |
| **URL**      | `GET /api/media/pet/{petId}` |
| **请求类型** | `application/json`           |

### Path 参数

| **参数名** | **类型** | **描述**  |
| ---------- | -------- | --------- |
| `petId`    | `long`   | 宠物 ID。 |

### 成功响应

返回 `ApiResponse<List<MediaResponse>>`，`data` 字段为文件列表。

**URL:** `GET /api/media/pet/{petId}` **成功响应格式 (`ApiResponse<List<MediaResponse>>`):**

JSON

```
{
  "code": 20000,
  "message": "按宠物ID查询文件列表成功",
  "data": [
    {
      "mediaId": 123,
      "fileName": "avatar.png",
      "fileUrl": "https://cos.url/pet_1/PET_AVATAR_1.png",
      "fileType": "image/png",
      "fileSize": 51200,
      "uploadTime": "2025-12-03T10:00:00",
      "petId": 1,
      "relatedType": "PET_AVATAR",
      "relatedTypeDesc": "宠物头像",
      "relatedId": 1
    },
    {
      "mediaId": 124,
      "fileName": "moment_photo.jpg",
      "fileUrl": "https://cos.url/pet_1/MOMENT_101_1.jpg",
      "fileType": "image/jpeg",
      "fileSize": 102400,
      "uploadTime": "2025-12-04T15:30:00",
      "petId": 1,
      "relatedType": "MOMENT",
      "relatedTypeDesc": "动态",
      "relatedId": 101
    }
  ]
}
```

------

## 4. 按业务关联查询文件

| **属性**     | **描述**                                           |
| ------------ | -------------------------------------------------- |
| **URL**      | `GET /api/media/related/{relatedType}/{relatedId}` |
| **请求类型** | `application/json`                                 |

### Path 参数

| **参数名**    | **类型** | **描述**            |
| ------------- | -------- | ------------------- |
| `relatedType` | `string` | 关联的业务类型。    |
| `relatedId`   | `long`   | 关联的业务记录 ID。 |

### 成功响应

返回 `ApiResponse<List<MediaResponse>>`，`data` 字段为文件列表。

**URL:** `GET /api/media/related/{relatedType}/{relatedId}` **成功响应格式 (`ApiResponse<List<MediaResponse>>`):**

JSON

```
{
  "code": 20000,
  "message": "按业务关联查询文件列表成功",
  "data": [
    {
      "mediaId": 124,
      "fileName": "moment_photo.jpg",
      "fileUrl": "https://cos.url/pet_1/MOMENT_101_1.jpg",
      "fileType": "image/jpeg",
      "fileSize": 102400,
      "uploadTime": "2025-12-04T15:30:00",
      "petId": 1,
      "relatedType": "MOMENT",
      "relatedTypeDesc": "动态",
      "relatedId": 101
    },
    {
      "mediaId": 125,
      "fileName": "moment_video.mp4",
      "fileUrl": "https://cos.url/pet_1/MOMENT_101_2.mp4",
      "fileType": "video/mp4",
      "fileSize": 5120000,
      "uploadTime": "2025-12-04T15:31:00",
      "petId": 1,
      "relatedType": "MOMENT",
      "relatedTypeDesc": "动态",
      "relatedId": 101
    }
  ]
}
```

------

## 5. 文件删除

| **属性**     | **描述**                      |
| ------------ | ----------------------------- |
| **URL**      | `DELETE /api/media/{mediaId}` |
| **请求类型** | `application/json`            |

### Path 参数

| **参数名** | **类型** | **描述**      |
| ---------- | -------- | ------------- |
| `mediaId`  | `long`   | 媒体文件 ID。 |

### 成功响应

返回 `ApiResponse<Void>`，`data` 字段为 `null`。

**URL:** `DELETE /api/media/{mediaId}` **成功响应格式 (`ApiResponse<Void>`):**

JSON

```
{
  "code": 20000,
  "message": "文件删除成功",
  "data": null
}
```

------

## 6. 删除关联业务的所有文件

| **属性**     | **描述**                                              |
| ------------ | ----------------------------------------------------- |
| **URL**      | `DELETE /api/media/related/{relatedType}/{relatedId}` |
| **请求类型** | `application/json`                                    |

### Path 参数

| **参数名**    | **类型** | **描述**            |
| ------------- | -------- | ------------------- |
| `relatedType` | `string` | 关联的业务类型。    |
| `relatedId`   | `long`   | 关联的业务记录 ID。 |

### 成功响应

返回 `ApiResponse<Void>`，`data` 字段为 `null`。

**URL:** `DELETE /api/media/related/{relatedType}/{relatedId}` **成功响应格式 (`ApiResponse<Void>`):**

JSON

```
{
  "code": 20000,
  "message": "关联文件删除成功",
  "data": null
}
```