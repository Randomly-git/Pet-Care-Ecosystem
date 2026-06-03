# 媒体微服务 API 调用说明

本微服务负责处理所有文件的上传、存储和关联关系管理。

**根路径：** `/api/media`

## 1. 基础数据格式

所有 API 响应均采用统一的 `ApiResponse<T>` 封装。

### 1.1. 统一响应格式 (`ApiResponse<T>`)

| **字段名** | **类型** | **描述** | **示例值** |
| ---------- | -------- | -------- | ---------- |
| `code`     | `int`    | 业务状态码。`20000` 表示成功。 | `20000` |
| `message`  | `String` | 响应消息。 | `"操作成功"` |
| `data`     | `T`      | 实际返回的业务数据。 | `{ MediaResponse 列表 }` |

### 1.2. 媒体文件响应 DTO (`MediaResponse`)

| **字段名**    | **类型**        | **描述** |
| ------------- | --------------- | -------- |
| `mediaId`     | `Long`          | 媒体文件的唯一 ID。 |
| `fileUrl`     | `String`        | 文件的访问 URL。 |
| `relatedType` | `String`        | 关联业务类型（MOMENT, USER_AVATAR, ACTIVITY, STATUS）。 |
| `relatedId`   | `Long`          | 关联的业务 ID。 |
| `fileName`    | `String`        | 文件原始名称。 |
| `fileType`    | `String`        | 文件类型（如：`image/jpeg/pdf/mp4`）。 |
| `uploadTime`  | `LocalDateTime` | 文件上传时间。 |

------

## 2. 媒体 API 接口详情

### 2.1. 文件上传 (前端直接调用)

| **属性**         | **值** |
| ---------------- | ------ |
| **用途**         | 上传文件到服务器，获取文件信息和临时 ID。 |
| **URI**          | `/api/media/upload` |
| **Method**       | `POST` |
| **Content-Type** | `multipart/form-data` |

#### 请求参数

| **参数名**    | **类型** | **描述** | **是否必须** |
| ------------- | -------- | -------- | ------------ |
| `file`        | `File`   | 要上传的媒体文件。 | 必填 |
| `userId`      | `Long`   | 上传文件的用户 ID。 | 必填 |
| `relatedType` | `String` | 关联的业务类型。 | 必填 |
| `relatedId`   | `Long`   | 关联的业务 ID。 | 必填 |

#### 响应格式

| **状态码** | **响应体 (Body)**            | **描述** |
| ---------- | ---------------------------- | -------- |
| `200 OK`   | `ApiResponse<MediaResponse>` | 上传成功，返回 MediaResponse 包含 mediaId。 |

### 2.2. 批量关联媒体文件

| **属性**   | **值** |
| ---------- | ------ |
| **用途**   | 关联媒体文件到业务实体。 |
| **URI**    | `/api/media/related/batch` |
| **Method** | `PATCH` |

#### 请求体 (`MediaBatchUpdateRequest`)

| **字段名**     | **类型**     | **约束**    | **描述** |
| -------------- | ------------ | ----------- | -------- |
| `mediaIds`     | `List<Long>` | `@NotEmpty` | 需要关联的媒体文件 ID 列表。 |
| `relatedType`  | `String`     | `@NotBlank` | 关联的业务类型，例如 `MOMENT`。 |
| `newRelatedId` | `Long`       | `@NotNull`  | 新的关联 ID。 |

### 2.3. 获取关联的媒体文件列表

| **属性**   | **值** |
| ---------- | ------ |
| **用途**   | 根据业务 ID 获取所有媒体文件的信息。 |
| **URI**    | `/api/media/related/{relatedType}/{relatedId}` |
| **Method** | `GET` |

#### 响应格式

| **状态码**      | **响应体 (Body)**                  | **描述** |
| --------------- | ---------------------------------- | -------- |
| `200 OK`        | `ApiResponse<List<MediaResponse>>` | 成功返回媒体文件列表。 |
| `404 Not Found` | `ApiResponse<Void>`                | 未找到关联的媒体文件。 |

### 2.4. 删除关联的媒体文件

| **属性**   | **值** |
| ---------- | ------ |
| **用途**   | 删除与业务实体关联的所有媒体文件。 |
| **URI**    | `/api/media/related/{relatedType}/{relatedId}` |
| **Method** | `DELETE` |

#### 响应格式

| **状态码**                  | **响应体 (Body)**   | **描述** |
| --------------------------- | ------------------- | -------- |
| `200 OK`                    | `ApiResponse<Void>` | 成功删除所有关联文件。 |
| `500 Internal Server Error` | `ApiResponse<Void>` | 删除操作失败。 |
