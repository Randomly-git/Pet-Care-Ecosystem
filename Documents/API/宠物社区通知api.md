# 宠物社区通知 API 文档

> 更新时间：2026-03-19
> 服务：community-backend
> 基础路径：`http://localhost:8083` (网关转发：`/api/v1/notifications`)

---

## 一、接口总览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/notifications` | 获取通知列表（分页） |
| GET | `/notifications/unread` | 获取未读通知列表 |
| GET | `/notifications/unread-count` | 获取未读通知数量 |
| PUT | `/notifications/{id}/read` | 标记单条通知为已读 |
| PUT | `/notifications/read-all` | 标记所有通知为已读 |
| DELETE | `/notifications/{id}` | 删除单条通知 |
| DELETE | `/notifications/user/{userId}` | 删除用户所有通知 |

---

## 二、接口详情

### 2.1 获取通知列表（分页）

**接口地址**: `GET /notifications`

**网关路径**: `GET /api/v1/notifications`

**查询参数**:

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| userId | Long | 是 | - | 用户ID |
| page | Int | 否 | 0 | 页码，从0开始 |
| size | Int | 否 | 20 | 每页数量 |

**请求示例**:

```
GET /notifications?userId=1&page=0&size=20
```

**响应示例**:

```json
{
  "notifications": [
    {
      "id": 1,
      "type": "COMMENT",
      "userId": 2,
      "actorUserId": 3,
      "actorUserName": "活了一百万次的猫",
      "actorUserAvatar": "https://example.com/avatar.jpg",
      "businessId": 10,
      "businessType": "MOMENT",
      "content": "活了一百万次的猫评论了你的动态",
      "isRead": false,
      "createdAt": "2026-03-19T17:30:00",
      "readAt": null
    },
    {
      "id": 2,
      "type": "LIKE",
      "userId": 2,
      "actorUserId": 5,
      "actorUserName": "可爱汪星人",
      "actorUserAvatar": "https://example.com/avatar2.jpg",
      "businessId": 10,
      "businessType": "MOMENT",
      "content": "可爱汪星人赞了你的动态",
      "isRead": true,
      "createdAt": "2026-03-19T16:00:00",
      "readAt": "2026-03-19T16:30:00"
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "unreadCount": 3,
  "currentPage": 0,
  "pageSize": 20
}
```

---

### 2.2 获取未读通知列表

**接口地址**: `GET /notifications/unread`

**网关路径**: `GET /api/v1/notifications/unread`

**查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**请求示例**:

```
GET /notifications/unread?userId=1
```

**响应示例**:

```json
[
  {
    "id": 1,
    "type": "FOLLOW",
    "userId": 2,
    "actorUserId": 8,
    "actorUserName": "新粉丝用户",
    "actorUserAvatar": "https://example.com/avatar3.jpg",
    "businessId": 8,
    "businessType": "USER",
    "content": "新粉丝用户关注了你",
    "isRead": false,
    "createdAt": "2026-03-19T17:25:00",
    "readAt": null
  }
]
```

---

### 2.3 获取未读通知数量

**接口地址**: `GET /notifications/unread-count`

**网关路径**: `GET /api/v1/notifications/unread-count`

**查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**请求示例**:

```
GET /notifications/unread-count?userId=1
```

**响应示例**:

```json
{
  "userId": 1,
  "unreadCount": 5
}
```

---

### 2.4 标记单条通知为已读

**接口地址**: `PUT /notifications/{id}/read`

**网关路径**: `PUT /api/v1/notifications/{id}/read`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 通知ID |

**请求示例**:

```
PUT /notifications/1/read
```

**响应示例**:

```json
{
  "id": 1,
  "success": true,
  "message": "标记成功"
}
```

**失败响应示例**:

```json
{
  "id": 999,
  "success": false,
  "message": "通知不存在或已读"
}
```

---

### 2.5 标记所有通知为已读

**接口地址**: `PUT /notifications/read-all`

**网关路径**: `PUT /api/v1/notifications/read-all`

**查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**请求示例**:

```
PUT /notifications/read-all?userId=1
```

**响应示例**:

```json
{
  "userId": 1,
  "updatedCount": 5,
  "success": true
}
```

---

### 2.6 删除单条通知

**接口地址**: `DELETE /notifications/{id}`

**网关路径**: `DELETE /api/v1/notifications/{id}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 通知ID |

**请求示例**:

```
DELETE /notifications/1
```

**响应示例**:

```json
{
  "id": 1,
  "success": true
}
```

---

### 2.7 删除用户所有通知

**接口地址**: `DELETE /notifications/user/{userId}`

**网关路径**: `DELETE /api/v1/notifications/user/{userId}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| userId | Long | 用户ID |

**请求示例**:

```
DELETE /notifications/user/1
```

**响应示例**:

```json
{
  "userId": 1,
  "success": true
}
```

---

## 三、通知类型说明

### 3.1 type 字段值

| 类型 | 说明 | 示例内容 |
|------|------|----------|
| LIKE | 有人点赞了你的动态/评论 | "用户名赞了你的动态" |
| COMMENT | 有人评论了你的动态 | "用户名评论了你的动态" |
| FOLLOW | 有人关注了你 | "用户名关注了你" |
| REPLY | 有人回复了你的评论 | "用户名回复了你的评论" |

### 3.2 businessType 字段值

| 类型 | 说明 |
|------|------|
| MOMENT | 社区动态 |
| COMMENT | 评论 |
| USER | 用户（用于关注通知） |
| REPLY | 回复 |

---

## 四、WebSocket 实时通知

### 4.1 连接地址

- **开发环境**: `http://localhost:8083/ws`
- **生产环境**: `ws://网关地址/ws`

### 4.2 订阅主题

用户需要订阅个人通知队列：

```
/queue/user/{userId}
```

例如用户ID为1，则订阅： `/queue/user/1`

### 4.3 消息格式

收到通知时的消息体：

```json
{
  "id": 1,
  "type": "COMMENT",
  "userId": 1,
  "actorUserId": 3,
  "actorUserName": "活了一百万次的猫",
  "actorUserAvatar": "https://example.com/avatar.jpg",
  "businessId": 10,
  "businessType": "MOMENT",
  "content": "活了一百万次的猫评论了你的动态",
  "isRead": false,
  "createdAt": "2026-03-19T17:30:00",
  "readAt": null
}
```

### 4.4 前端订阅示例（JavaScript）

```javascript
// 连接到 WebSocket
const socket = new SockJS('http://localhost:8083/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  console.log('Connected to WebSocket');

  // 订阅个人通知队列，userId 从登录态获取
  const userId = 1; // 示例用户ID
  stompClient.subscribe('/queue/user/' + userId, function(message) {
    const notification = JSON.parse(message.body);
    console.log('收到新通知:', notification);

    // 处理新通知
    handleNewNotification(notification);
  });
});

// 处理新通知
function handleNewNotification(notification) {
  switch (notification.type) {
    case 'LIKE':
      showToast('有人赞了你的动态');
      break;
    case 'COMMENT':
      showToast('有人评论了你的动态');
      break;
    case 'FOLLOW':
      showToast('有人关注了你');
      break;
    case 'REPLY':
      showToast('有人回复了你的评论');
      break;
  }

  // 更新通知列表（插入到列表顶部）
  addNotificationToList(notification);

  // 更新未读数量
  updateUnreadCount(1);
}
```

### 4.5 前端订阅示例（微信小程序）

```javascript
// 微信小程序 WebSocket 连接示例
const socket = wx.connectSocket({
  url: 'ws://localhost:8083/ws'
});

socket.onOpen(function() {
  console.log('WebSocket 连接成功');
  // 订阅通知主题
  socket.send({
    data: JSON.stringify({
      type: 'SUBSCRIBE',
      destination: '/queue/user/1'  // userId 从登录态获取
    })
  });
});

socket.onMessage(function(res) {
  const data = JSON.parse(res.data);
  if (data.type === 'MESSAGE') {
    const notification = JSON.parse(data.body);
    console.log('收到新通知:', notification);
  }
});
```

---

## 五、错误响应格式

所有接口的错误响应统一格式：

```json
{
  "timestamp": "2026-03-19T17:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "通知不存在",
  "path": "/notifications/999"
}
```

---

## 六、通用响应状态码

| 状态码 | 说明 |
|--------|------|
| 200 OK | 请求成功 |
| 201 Created | 创建成功 |
| 400 Bad Request | 请求参数错误 |
| 401 Unauthorized | 未授权 |
| 403 Forbidden | 禁止访问 |
| 404 Not Found | 资源不存在 |
| 500 Internal Server Error | 服务器内部错误 |

---

## 七、数据库表结构（供参考）

```sql
CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `type` VARCHAR(20) NOT NULL COMMENT '通知类型: LIKE, COMMENT, FOLLOW, REPLY',
    `user_id` BIGINT NOT NULL COMMENT '接收通知的用户ID',
    `actor_user_id` BIGINT NOT NULL COMMENT '触发操作的用户ID',
    `actor_user_name` VARCHAR(100) NULL COMMENT '触发操作的用户名称',
    `actor_user_avatar` VARCHAR(500) NULL COMMENT '触发操作的用户头像',
    `business_id` BIGINT NULL COMMENT '关联的业务ID',
    `business_type` VARCHAR(20) NULL COMMENT '业务类型: MOMENT, COMMENT, USER',
    `content` VARCHAR(500) NULL COMMENT '通知内容摘要',
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已读',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `read_at` DATETIME NULL COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
