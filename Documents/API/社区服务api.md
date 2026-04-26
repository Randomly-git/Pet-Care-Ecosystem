# 社区服务 API 文档

> 更新时间：2026-03-18
> 服务：community-backend
> 基础路径：`/api/v1/community` (通过网关)

---

## 一、动态管理接口

### 1.1 发布新动态

**接口地址**: `POST /api/v1/moments`

**请求参数** (JSON Body):

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 发布者用户ID |
| content | String | 是 | 动态内容 |
| mediaIds | List<Long> | 否 | 关联的媒体文件ID列表 |

**响应示例**:

```json
{
  "id": 1,
  "userId": 2,
  "content": "今天带猫咪去公园玩！",
  "createdAt": "2026-03-18T10:30:00",
  "mediaUrls": [],
  "likeCount": 0,
  "commentCount": 0
}
```

---

### 1.2 获取用户动态列表

**接口地址**: `GET /api/v1/moments/user/{userId}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| userId | Long | 用户ID |

**响应示例**:

```json
[
  {
    "id": 1,
    "userId": 2,
    "content": "今天带猫咪去公园玩！",
    "createdAt": "2026-03-18T10:30:00",
    "mediaUrls": ["https://..."],
    "likeCount": 5,
    "commentCount": 3
  }
]
```

---

### 1.3 获取所有用户动态（分页）

**接口地址**: `GET /api/v1/moments/all`

**查询参数**:

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| page | Int | 0 | 页码，从0开始 |
| size | Int | 20 | 每页数量 |

**响应示例**:

```json
[
  {
    "id": 1,
    "userId": 2,
    "content": "今天带猫咪去公园玩！",
    "createdAt": "2026-03-18T10:30:00",
    "authorName": "用户名",
    "authorAvatar": "https://...",
    "mediaUrls": [],
    "likeCount": 0,
    "commentCount": 0
  }
]
```

---

### 1.4 删除动态

**接口地址**: `DELETE /api/v1/moments/{momentId}?userId={userId}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| momentId | Long | 动态ID |

**查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（用于权限验证和冷库数据清理） |

> ⚠️ **重要说明**：
> - `userId` 必须传入，用于验证用户是否有权删除该动态
> - 只有动态的作者才能删除自己的动态
> - 删除时会同时清理冷库(HBase)中的相关数据

**权限说明**:

| 响应 | 说明 |
|------|------|
| 200 OK | 删除成功 |
| 403 Forbidden | 无权删除他人的动态 |
| 404 Not Found | 动态不存在 |

---

## 二、评论管理接口

### 2.1 添加评论

**接口地址**: `POST /api/v1/comments`

**请求参数** (JSON Body):

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| momentId | Long | 是 | 动态ID |
| userId | Long | 是 | 评论者用户ID |
| content | String | 是 | 评论内容 |
| parentId | Long | 否 | 父评论ID（用于回复） |

**响应示例**:

```json
{
  "commentId": 1,
  "momentId": 1,
  "userId": 2,
  "userName": "用户名",
  "content": "好可爱的猫咪！",
  "parentId": null,
  "createdAt": "2026-03-18T10:30:00"
}
```

---

### 2.2 获取动态评论

**接口地址**: `GET /api/v1/comments/moment/{momentId}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| momentId | Long | 动态ID |

**响应示例**:

```json
[
  {
    "commentId": 1,
    "momentId": 1,
    "userId": 2,
    "userName": "用户名",
    "userAvatar": "https://...",
    "content": "好可爱的猫咪！",
    "parentId": null,
    "createdAt": "2026-03-18T10:30:00",
    "replies": []
  }
]
```

---

### 2.3 删除评论

**接口地址**: `DELETE /api/v1/comments/{commentId}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| commentId | Long | 评论ID |

---

## 三、点赞管理接口

### 3.1 切换点赞状态

**接口地址**: `POST /api/v1/likes`

**请求参数** (JSON Body):

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 点赞者用户ID |
| targetType | String | 是 | 目标类型：`MOMENT` / `COMMENT` |
| targetId | Long | 是 | 目标ID |

**响应示例** (取消点赞成功):

```json
{
  "success": true,
  "message": "取消点赞成功",
  "liked": false
}
```

---

## 四、关注管理接口

### 4.1 切换关注状态

**接口地址**: `POST /api/v1/follows`

**请求参数** (JSON Body):

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| followerId | Long | 是 | 关注者用户ID |
| followingId | Long | 是 | 被关注者用户ID |

**响应示例**:

```json
{
  "success": true,
  "message": "关注成功",
  "following": true
}
```

---

### 4.2 获取粉丝数量

**接口地址**: `GET /api/v1/followers/count/{userId}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| userId | Long | 用户ID |

---

### 4.3 获取关注数量

**接口地址**: `GET /api/v1/follows/count/{userId}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| userId | Long | 用户ID |

---

## 五、通知管理接口

### 5.1 获取通知列表（分页）

**接口地址**: `GET /api/v1/notifications`

**查询参数**:

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| userId | Long | 是 | - | 用户ID |
| page | Int | 否 | 0 | 页码，从0开始 |
| size | Int | 否 | 20 | 每页数量 |

**响应示例**:

```json
{
  "content": [
    {
      "id": 1,
      "type": "COMMENT",
      "userId": 2,
      "actorUserId": 3,
      "actorUserName": "活了一百万次的猫",
      "actorUserAvatar": "https://...",
      "businessId": 1,
      "businessType": "MOMENT",
      "content": "活了一百万次的猫评论了你的动态",
      "isRead": false,
      "createdAt": "2026-03-18T10:30:00"
    }
  ],
  "totalElements": 10,
  "totalPages": 1,
  "number": 0
}
```

---

### 5.2 获取未读通知列表

**接口地址**: `GET /api/v1/notifications/unread`

**查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

---

### 5.3 获取未读通知数量

**接口地址**: `GET /api/v1/notifications/unread-count`

**查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例**:

```json
{
  "count": 5
}
```

---

### 5.4 标记通知为已读

**接口地址**: `PUT /api/v1/notifications/{notificationId}/read`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| notificationId | Long | 通知ID |

---

### 5.5 标记所有通知为已读

**接口地址**: `PUT /api/v1/notifications/read-all`

**查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

---

## 六、WebSocket 实时通知

### 6.1 连接地址

**开发环境**: `http://localhost:端口号/ws`
**生产环境**: `ws://网关地址/ws`

### 6.2 订阅主题

用户需要订阅个人通知队列：

```
/queue/user/{userId}
```

例如用户ID为2，则订阅：`/queue/user/2`

### 6.3 消息格式

收到通知时的消息体：

```json
{
  "id": 1,
  "type": "COMMENT",
  "userId": 2,
  "actorUserId": 3,
  "actorUserName": "活了一百万次的猫",
  "actorUserAvatar": null,
  "businessId": 1,
  "businessType": "MOMENT",
  "content": "活了一百万次的猫评论了你的动态",
  "isRead": false,
  "createdAt": "2026-03-18T10:30:00"
}
```

### 6.4 STOMP 客户端示例

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  // 订阅个人通知队列
  stompClient.subscribe('/queue/user/2', function(message) {
    const notification = JSON.parse(message.body);
    console.log('收到新通知:', notification);
    // 更新UI显示通知
  });
});
```

---

## 七、消息队列事件

### 7.1 通知事件

**交换机**: `petcare.notification.exchange`
**队列**: `petcare.notification.queue`
**路由键**: `notification.*`

**事件格式**:

```json
{
  "eventId": "uuid-字符串",
  "type": "COMMENT",
  "actorUserId": 3,
  "actorUserName": "活了一百万次的猫",
  "targetUserId": 2,
  "businessId": 1,
  "businessType": "MOMENT",
  "content": "活了一百万次的猫评论了你的动态",
  "eventTime": "2026-03-18T10:30:00"
}
```

**事件类型**:

| 类型 | 说明 |
|------|------|
| COMMENT | 评论通知 |
| LIKE | 点赞通知 |
| FOLLOW | 关注通知 |

---

## 八、媒体绑定事件

### 8.1 社区动态媒体绑定

**交换机**: `petcare.media.exchange`
**队列**: `petcare.media.bind.queue`
**路由键**: `media.bind.community`

**事件格式**:

```json
{
  "eventId": "uuid-字符串",
  "relatedType": "COMMUNITY",
  "relatedId": 1,
  "mediaIds": [1, 2, 3],
  "userId": 2,
  "eventTime": "2026-03-18T10:30:00"
}
```

---

## 九、错误响应格式

所有接口的错误响应统一格式：

```json
{
  "timestamp": "2026-03-18T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "动态不存在或删除失败",
  "path": "/api/v1/moments/999"
}
```

---
## 十一、审核管理接口 (管理端)

### 11.1 获取待审核动态列表

**接口地址**: `GET /api/v1/admin/moments/pending`

**查询参数**:
| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| page | Int | 0 | 页码 |
| size | Int | 20 | 每页数量 |

### 11.2 审核动态操作

**接口地址**: `PUT /api/v1/admin/moments/{momentId}/audit`

**请求参数** (JSON Body):
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | String | 是 | `APPROVED` (通过) / `REJECTED` (拒绝) |
| reason | String | 否 | 拒绝原因 |
| auditorId | Long | 是 | 审核人用户ID |

**响应示例**:
```json
{
  "momentId": 1,
  "status": "APPROVED",
  "auditTime": "2026-03-20T10:00:00"
}
```

---

## 十二、HBase 状态历史增强说明

### 12.1 状态变更溯源查询

**对应接口**: `GET /api/status/timeline/{petId}` (见状态记录API 2.0.5)

**底层逻辑说明**:
- **热数据**: MySQL `status_records` 存储当前活跃状态。
- **冷数据**: HBase `petcare_cold:pet_status_history` 存储所有历史变更。
- **RowKey**: `{pet_id}_{reversed_timestamp}`。
- **查询建议**: 前端在展示时间轴时，若用户滚动到底部，应触发分页查询以调取 HBase 中的历史记录。

---

## 十、通用响应状态码

| 状态码 | 说明 |
|--------|------|
| 200 OK | 请求成功 |
| 201 Created | 创建成功 |
| 400 Bad Request | 请求参数错误 |
| 401 Unauthorized | 未授权 |
| 403 Forbidden | 禁止访问 |
| 404 Not Found | 资源不存在 |
| 500 Internal Server Error | 服务器内部错误 |
