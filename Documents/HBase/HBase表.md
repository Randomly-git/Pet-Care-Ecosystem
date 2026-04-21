### HBase 表设计图：`community_moments_archive`

#### 1. 表基本信息

- **表名**: `community_moments_archive`
- **列族 (Column Family)**: `d` (Data)

#### 2. RowKey 设计

**格式**: `{userId_prefix}_{momentId}`

- **userId_prefix**: 取 `userId` 的前 4 位（或取模哈希），目的是为了将同一用户的冷数据分布在不同的 Region 中，防止热点写。
- **momentId**: 原始 MySQL 中的动态 ID，保证全局唯一性。
- **示例**: `1001_5678`

#### 3. 存储结构图解

| 区域        | 字段 (Qualifier) | 数据类型            | 说明                                    |
| ----------- | ---------------- | ------------------- | --------------------------------------- |
| **RowKey**  | `ROW`            | String              | 格式：`{userId前4位}_{momentId}`        |
| **列族: d** | `moment_id`      | Long                | 动态 ID (冗余存储，便于验证)            |
|             | `user_id`        | Long                | 作者用户 ID                             |
|             | `content`        | String              | 动态正文内容                            |
|             | `created_at`     | Long/String         | 原始创建时间戳                          |
|             | **`full_data`**  | **Binary (byte[])** | **核心负载：GZIP 压缩后的 JSON 字符串** |

------

### 4. `full_data` 负载详解 (JSON 结构)

`full_data` 字段通过 `ColdArchiveData` 类进行序列化，包含了该动态被归档时的完整快照：

```
json{
  "metadata": {
    "commentCount": 10,       // 归档时的总评论数
    "likeCount": 50,          // 归档时的总点赞数
    "snapshotTime": "2026-03-25T..." // 归档执行时间
  },
  "comments": [               // 关联的评论列表（ArchivedComment）
    {
      "commentId": 1,
      "userId": 200,
      "content": "好可爱的猫咪",
      "parentId": null,       // 支持一级或多级嵌套
      "userName": "宠物达人",
      "userAvatar": "http://...",
      "createdAt": "..."
    }
  ],
  "likes": [                  // 关联的点赞列表（ArchivedLike）
    {
      "likeId": 10,
      "userId": 300,
      "targetType": "MOMENT", // 点赞对象：MOMENT 或 COMMENT
      "targetId": 5678,
      "userName": "小王",
      "userAvatar": "http://..."
    }
  ]
}
```

### 5. 设计亮点说明

1. **查询性能优化**:
   - 传统的冷数据如果分散存储在多张 HBase 表中，恢复一条动态需要多次 RPC。
   - 现在的设计通过 `full_data` 将所有关联实体（评论、点赞）聚合，**一次 `Get` 操作**即可完成全量数据的反序列化。
2. **存储效率**:
   - 由于评论和点赞通常包含大量重复的 `userName` 或 `userAvatar`，使用 **GZIP 压缩**存储到 `full_data` 可以显著降低 HBase 的磁盘占用。
3. **可推算性恢复**:
   - RowKey 包含 `userId` 和 `momentId`，这意味着即使 MySQL 记录完全丢失，只要知道用户 ID 范围，就可以通过 HBase Scan 恢复出该用户的所有动态。
