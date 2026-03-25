### 社区微服务 API 文档 (Community-Backend)

本微服务负责处理用户发布宠物动态（Moment）的核心 CRUD 操作。所有 API 均以 `/api/v1/moments` 为根路径。

> **重要更新 (2026-03-25)**：删除动态接口现已要求传入 `userId` 参数进行权限验证和冷库数据清理。

| **模块** | **方法** | **路径** | **描述** | **请求体/参数** | **响应体 (DTO)** |
| -------- | -------- | ---------------------------------------- | ------------------ | ------------------------------------------------------------ | --------------------------------------- |
| **动态** | **POST** | `/api/v1/moments` | 创建新动态 | `{"userId": 1, "content": "我的宠物真可爱！", "mediaIds": [101, 102]}` | `MomentResponseDTO` |
| | **GET** | `/api/v1/moments/user/{userId}` | 获取用户动态列表 | 无 | `List<MomentResponseDTO>` |
| | **DELETE** | `/api/v1/moments/{momentId}?userId={userId}` | 删除指定动态 | `userId` 查询参数（必填，用于权限验证） | `200 OK` (成功) / `403 Forbidden` (无权删除) / `404 Not Found` (不存在) |
| **评论** | **POST** | `/api/v1/comments` | 创建评论或回复 | **评论：** `{"userId": 1, "momentId": 50, "content": "赞！"}` **回复：** `{"userId": 2, "momentId": 50, "content": "谢谢！", "parentId": 10}` | `CommentResponseDTO` |
| | **GET** | `/api/v1/comments/moment/{momentId}` | 获取动态下所有评论 | 无 | `List<CommentResponseDTO>` (含嵌套回复) |
| | **DELETE** | `/api/v1/comments/{commentId}` | 删除评论及回复 | 无 | `200 OK` (成功) |
| **点赞** | **POST** | `/api/v1/likes` | 切换点赞状态 | **动态：** `{"userId": 1, "targetType": "MOMENT", "targetId": 50}` **评论：** `{"userId": 1, "targetType": "COMMENT", "targetId": 10}` | `200 OK` (成功/取消成功) |
| **关注** | **POST** | `/api/v1/follows` | 关注/取消关注 | `{"followerId": 1, "followedId": 2}` | `200 OK` (关注成功/取消关注成功) |
| | **GET** | `/api/v1/follows/followers/count/{userId}` | 获取粉丝数 | 无 | `Long` (粉丝数) |
| | **GET** | `/api/v1/follows/following/count/{userId}` | 获取关注数 | 无 | `Long` (关注数) |

---

### 删除动态接口说明

**接口**: `DELETE /api/v1/moments/{momentId}?userId={userId}`

**必填参数**:
- `momentId` (路径参数): 要删除的动态ID
- `userId` (查询参数): 当前登录用户的ID

**功能说明**:
1. **权限验证**: 只有动态的作者(`moment.userId == userId`)才能删除该动态
2. **级联删除**: 同时删除关联的评论、点赞、媒体文件
3. **冷库清理**: 自动清理冷库(HBase)中的相关数据

**响应示例**:
- `200 OK`: `{"data": "删除成功"}`
- `403 Forbidden`: `{"message": "无权删除他人的动态"}`
- `404 Not Found`: `{"message": "动态不存在"}`
