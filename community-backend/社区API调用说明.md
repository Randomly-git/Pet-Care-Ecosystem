### 社区微服务 API 文档 (Community-Backend)

本微服务负责处理用户发布宠物动态（Moment）的核心 CRUD 操作。所有 API 均以 `/api/v1/moments` 为根路径。

**总 API 数量：3个** (1 POST, 1 GET, 1 DELETE)

------

## 1. 数据结构 (DTO)

### 1.1. `MomentCreateRequestDTO` (请求体 - 用于 POST)

| **字段名** | **类型** | **是否必填** | **约束条件** | **描述**                                  |
| ---------- | -------- | ------------ | ------------ | ----------------------------------------- |
| `userId`   | `Long`   | 是           | `@NotNull`   | 发布动态的用户 ID，由系统管理微服务提供。 |
| `content`  | `String` | 是           | `@NotBlank`  | 动态的主要文字内容。                      |

### 1.2. `MomentResponseDTO` (响应体 - 用于 GET/POST 响应)

此 DTO 是一个**聚合数据结构**，包含了本服务及其他服务的关联信息。

| **字段名**     | **类型**        | **来源**                       | **描述**                                        |
| -------------- | --------------- | ------------------------------ | ----------------------------------------------- |
| `id`           | `Long`          | Community-Backend              | 动态的唯一标识符（数据库列名：`moment_id`）。   |
| `userId`       | `Long`          | Community-Backend              | 发布用户 ID。                                   |
| `content`      | `String`        | Community-Backend              | 动态内容。                                      |
| `createdAt`    | `LocalDateTime` | Community-Backend              | 动态发布时间。                                  |
| `mediaUrls`    | `List<String>`  | **媒体微服务 (Media Service)** | **聚合字段：** 动态中所有图片/视频的 URL 列表。 |
| `commentCount` | `int`           | **互动微服务**                 | **聚合字段：** 该动态下的评论数量。             |
| `likeCount`    | `int`           | **互动微服务**                 | **聚合字段：** 该动态获得的赞数量。             |

------

## 2. API 端点详情

### 2.1. API 1: 发布新动态 (POST)

用于创建一个新的宠物动态。

| **属性**          | **值**                                     |
| ----------------- | ------------------------------------------ |
| **URI**           | `/api/v1/moments`                          |
| **Method**        | `POST`                                     |
| **Request Body**  | `MomentCreateRequestDTO` (JSON)            |
| **Response Code** | `201 Created`                              |
| **Response Body** | `MomentResponseDTO` (包含新创建的ID和时间) |

### 2.2. API 2: 获取用户动态列表 (GET)

根据用户 ID 获取该用户发布的所有动态，按时间倒序排列（最新在前）。

| **属性**          | **值**                          |
| ----------------- | ------------------------------- |
| **URI**           | `/api/v1/moments/user/{userId}` |
| **Method**        | `GET`                           |
| **URL 参数**      | `{userId}` (路径变量)           |
| **Response Code** | `200 OK`                        |
| **Response Body** | `List<MomentResponseDTO>`       |

### 2.3. API 3: 删除动态 (DELETE)

根据动态 ID 删除指定的动态。

| **属性**          | **值**                                                       |
| ----------------- | ------------------------------------------------------------ |
| **URI**           | `/api/v1/moments/{momentId}`                                 |
| **Method**        | `DELETE`                                                     |
| **URL 参数**      | `{momentId}` (路径变量)                                      |
| **Response Code** | `204 No Content` / `404 Not Found`                           |
| **Response Body** | `204 No Content`: **无内容 (空响应)** / `404`: 纯文本 "动态不存在或删除失败" |

------

## 3. 微服务协作和聚合说明 (后端开发人员必读)

本微服务在处理 `GET` 请求时，必须通过内部 HTTP/RPC 调用来集成其他微服务的数据。

| **协作方向** | **目标微服务**      | **触发条件**                             | **聚合责任**                                                 |
| ------------ | ------------------- | ---------------------------------------- | ------------------------------------------------------------ |
| **媒体信息** | **媒体微服务**      | 调用 `GET /api/v1/moments/user/{userId}` | 根据查询到的所有 `momentId`，批量调用媒体微服务的 API 获取对应的 `mediaUrls`，填充到 DTO 中。 |
| **互动数据** | **评论/互动微服务** | 调用 `GET /api/v1/moments/user/{userId}` | 根据查询到的所有 `momentId`，批量调用评论和互动微服务的 API 获取 `commentCount` 和 `likeCount`，填充到 DTO 中。 |