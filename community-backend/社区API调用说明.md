### 社区微服务 API 文档 (Community-Backend)

本微服务负责处理用户发布宠物动态（Moment）的核心 CRUD 操作。所有 API 均以 `/api/v1/moments` 为根路径。

| **模块** | **方法**   | **路径**                                   | **描述**           | **请求体示例 (JSON)**                                        | **响应体 (DTO)**                        |
| -------- | ---------- | ------------------------------------------ | ------------------ | ------------------------------------------------------------ | --------------------------------------- |
| **动态** | **POST**   | `/api/v1/moments`                          | 创建新动态         | `{"userId": 1, "content": "我的宠物真可爱！", "mediaIds": [101, 102]}` | `MomentResponseDTO`                     |
|          | **GET**    | `/api/v1/moments/user/{userId}`            | 获取用户动态列表   | 无                                                           | `List<MomentResponseDTO>`               |
|          | **DELETE** | `/api/v1/moments/{momentId}`               | 删除指定动态       | 无                                                           | `200 OK` (成功)                         |
| **评论** | **POST**   | `/api/v1/comments`                         | 创建评论或回复     | **评论：** `{"userId": 1, "momentId": 50, "content": "赞！"}` **回复：** `{"userId": 2, "momentId": 50, "content": "谢谢！", "parentId": 10}` | `CommentResponseDTO`                    |
|          | **GET**    | `/api/v1/comments/moment/{momentId}`       | 获取动态下所有评论 | 无                                                           | `List<CommentResponseDTO>` (含嵌套回复) |
|          | **DELETE** | `/api/v1/comments/{commentId}`             | 删除评论及回复     | 无                                                           | `200 OK` (成功)                         |
| **点赞** | **POST**   | `/api/v1/likes`                            | 切换点赞状态       | **动态：** `{"userId": 1, "targetType": "MOMENT", "targetId": 50}` **评论：** `{"userId": 1, "targetType": "COMMENT", "targetId": 10}` | `200 OK` (成功/取消成功)                |
| **关注** | **POST**   | `/api/v1/follows`                          | 关注/取消关注      | `{"followerId": 1, "followedId": 2}`                         | `200 OK` (关注成功/取消关注成功)        |
|          | **GET**    | `/api/v1/follows/followers/count/{userId}` | 获取粉丝数         | 无                                                           | `Long` (粉丝数)                         |
|          | **GET**    | `/api/v1/follows/following/count/{userId}` | 获取关注数         | 无                                                           | `Long` (关注数)                         |