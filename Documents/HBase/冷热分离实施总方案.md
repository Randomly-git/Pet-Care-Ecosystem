# HBase 冷存储设计方案

## 一、需要进行冷存储的表分析

### 1.1 冷数据筛选标准

| 标准         | 说明                   |
| :----------- | :--------------------- |
| 数据增长速度 | 高频写入，数据持续增长 |
| 历史查询频率 | 历史数据查询频率低     |
| 访问时效性   | 老数据访问概率低       |
| 数据关联性   | 关联关系相对简单       |

### 1.2 建议进行 HBase 冷存储的表

| 优先级 | 表名              | 当前数据量级 | 冷热分离策略          | 建议             |
| :----: | :---------------- | :----------- | :-------------------- | :--------------- |
|  🔴 高  | `activity_record` | 100万-1000万 | 30天后转冷            | 立即冷存储       |
|  🔴 高  | `likes`           | 500万-5000万 | 超过30天可考虑        | 立即冷存储       |
|  🔴 高  | `notifications`   | 500万-5000万 | 超过30天已读通知      | 立即冷存储       |
|  🟡 中  | `moments`         | 100万-1000万 | 7天未访问转冷(已实现) | 继续实现         |
|  🟡 中  | `status`          | 50万-500万   | 30天后转冷            | 建议冷存储       |
|  🟡 中  | `comments`        | 100万-1000万 | 超过90天可考虑        | 视情况冷存储     |
|  🟢 低  | `media_files`     | 1000万-1亿   | 已实现(COS)           | 数据库记录需处理 |

### 1.3 不建议冷存储的表

| 表名                | 原因                 |
| :------------------ | :------------------- |
| `user`              | 核心用户表，频繁查询 |
| `pets`              | 宠物信息需要实时访问 |
| `activity`          | 活动定义表，数据量小 |
| `activity_kind`     | 枚举表，无需冷存储   |
| `follows`           | 关注关系需要实时查询 |
| `fixed_activity`    | 固定活动配置         |
| `activity_reminder` | 提醒配置，需实时处理 |

------

## 二、HBase 表设计

### 2.1 RowKey 设计原则

| 原则     | 说明                        |
| :------- | :-------------------------- |
| 唯一性   | 必须唯一标识一行数据        |
| 散列性   | 避免热点，使用盐值或哈希    |
| 查询模式 | 根据查询场景设计前缀        |
| 长度控制 | RowKey 尽量短（16-100字节） |

### 2.2 命名空间设计

*-- 创建命名空间*

create_namespace 'petcare_cold'

### 2.3 表结构设计

#### 表1: `petcare_cold:activity_record` - 活动记录冷存储

RowKey 设计: {pet_id}_{activity_date}_{activity_record_id}

例如: 12345_20250319_987654321

列族设计:

├── info (核心数据)

│   ├── activity_record_id (活动记录ID)

│   ├── activity_id (活动ID)

│   ├── activity_name (活动名称，可反查)

│   ├── activity_description (活动描述)

│   ├── activity_date (活动时间)

│   ├── pet_id (宠物ID)

│   └── user_id (用户ID，反查用)

├── metadata (元数据)

│   ├── created_at (创建时间)

│   └── last_access_time (最后访问时间)

└── statistics (统计字段)

​    ├── view_count (浏览次数)

​    └── shared_count (分享次数)

查询场景:

- 查询某宠物某时间范围的活动 → RowKey 前缀匹配
- 查询某活动的所有记录 → 业务ID反向索引

------

#### 表2: `petcare_cold:likes` - 点赞冷存储

RowKey 设计: {target_type}_{target_id}_{user_id}_{timestamp}

例如: MOMENT_12345_67890_1710835200000

列族设计:

├── info (核心数据)

│   ├── like_id (点赞ID)

│   ├── user_id (点赞用户ID)

│   ├── target_type (目标类型: MOMENT/COMMENT)

│   ├── target_id (目标ID)

│   └── created_at (点赞时间)

├── user_info (用户信息快照)

│   ├── user_name (点赞用户昵称)

│   └── user_avatar (点赞用户头像)

└── metadata (元数据)

​    └── is_valid (是否有效: 用于取消点赞)

查询场景:

- 查询某动态的所有历史点赞 → target_type + target_id
- 查询某用户的点赞历史 → user_id 散列

------

#### 表3: `petcare_cold:notifications` - 通知冷存储

RowKey 设计: {user_id}_{timestamp}_{notification_id}

例如: 67890_1710835200000_1122334455

列族设计:

├── info (核心数据)

│   ├── notification_id (通知ID)

│   ├── type (通知类型: LIKE/COMMENT/FOLLOW/REPLY)

│   ├── user_id (接收用户ID)

│   ├── actor_user_id (触发用户ID)

│   ├── business_id (业务ID)

│   ├── business_type (业务类型)

│   └── content (通知内容)

├── actor_info (触发者信息快照)

│   ├── actor_user_name (触发者昵称)

│   ├── actor_user_avatar (触发者头像)

│   └── actor_user_badge (触发者徽章)

├── read_info (阅读信息)

│   ├── is_read (是否已读)

│   ├── read_at (阅读时间)

│   └── read_device (阅读设备)

└── metadata (元数据)

​    ├── created_at (创建时间)

​    ├── priority (优先级)

​    └── expire_at (过期时间)

查询场景:

- 查询某用户的历史通知 → user_id 前缀
- 查询某时间范围的未读通知 → 时间范围扫描

------

#### 表4: `petcare_cold:moments` - 社区动态冷存储

RowKey 设计: {user_id}_{timestamp}_{moment_id}

例如: 67890_1710835200000_9988776655

列族设计:

├── content (内容数据)

│   ├── moment_id (动态ID)

│   ├── user_id (发布用户ID)

│   ├── content (动态内容)

│   ├── location (位置信息)

│   └── tags (标签)

├── media (媒体信息)

│   ├── media_ids (媒体ID列表)

│   ├── media_count (媒体数量)

│   └── cover_url (封面URL)

├── engagement (互动数据)

│   ├── like_count (点赞数)

│   ├── comment_count (评论数)

│   └── share_count (分享数)

├── user_info (用户信息快照)

│   ├── user_name (发布者昵称)

│   ├── user_avatar (发布者头像)

│   └── user_verified (是否认证)

├── metadata (元数据)

│   ├── created_at (创建时间)

│   ├── updated_at (更新时间)

│   ├── last_access_time (最后访问时间)

│   ├── is_public (是否公开)

│   └── is_deleted (是否删除)

└── statistics (统计字段)

​    ├── view_count (浏览次数)

​    ├── reach_count (触达人数)

​    └── engagement_rate (互动率)

查询场景:

- 查询某用户的历史动态 → user_id 前缀
- 查询热点动态 → 时间范围 + 互动数排序

------

#### 表5: `petcare_cold:status` - 宠物状态冷存储

RowKey 设计: {pet_id}_{timestamp}_{status_id}

例如: 12345_1710835200000_5566778899

列族设计:

├── info (核心数据)

│   ├── status_id (状态ID)

│   ├── pet_id (宠物ID)

│   ├── status_name (状态名称)

│   ├── status_value (状态值)

│   └── record_time (记录时间)

├── pet_info (宠物信息快照)

│   ├── pet_name (宠物名称)

│   ├── pet_species (宠物种类)

│   └── pet_breed (宠物品种)

├── health (健康相关)

│   ├── weight (体重)

│   ├── temperature (体温)

│   └── health_score (健康评分)

└── metadata (元数据)

​    ├── created_at (创建时间)

​    └── source (数据来源: APP/设备/手动)

------

#### 表6: `petcare_cold:comments` - 评论冷存储

RowKey 设计: {moment_id}_{timestamp}_{comment_id}

例如: 9988776655_1710835200000_3344556677

列族设计:

├── content (内容数据)

│   ├── comment_id (评论ID)

│   ├── moment_id (所属动态ID)

│   ├── user_id (评论用户ID)

│   ├── content (评论内容)

│   └── parent_id (父评论ID，支持嵌套)

├── user_info (用户信息快照)

│   ├── user_name (评论者昵称)

│   ├── user_avatar (评论者头像)

│   └── user_verified (是否认证)

├── engagement (互动数据)

│   ├── like_count (点赞数)

│   └── reply_count (回复数)

├── thread (线程信息)

│   ├── root_comment_id (根评论ID)

│   ├── thread_depth (嵌套深度)

│   └── thread_path (路径，用于排序)

├── metadata (元数据)

│   ├── created_at (创建时间)

│   ├── updated_at (更新时间)

│   └── is_deleted (是否删除)

└── sentiment (情感分析)

​    ├── sentiment_score (情感得分)

​    └── sentiment_label (情感标签)

------

## 三、完整表结构汇总

| 命名空间     | 表名            | RowKey                        | 主要列族                   | 适用场景         |
| :----------- | :-------------- | :---------------------------- | :------------------------- | :--------------- |
| petcare_cold | activity_record | {pet_id}*{date}*{id}          | info, metadata             | 宠物历史活动查询 |
| petcare_cold | likes           | {type}*{target}*{user}_{time} | info, metadata             | 点赞历史查询     |
| petcare_cold | notifications   | {user}*{time}*{id}            | info, read_info            | 用户通知历史     |
| petcare_cold | moments         | {user}*{time}*{id}            | content, media, engagement | 社区动态历史     |
| petcare_cold | status          | {pet}*{time}*{id}             | info, health               | 宠物状态历史     |
| petcare_cold | comments        | {moment}*{time}*{id}          | content, thread            | 评论历史查询     |

------

## 四、RowKey 设计详解

### 4.1 热点问题解决方案

问题: 如果 RowKey 以时间戳开头，会导致新数据都写入同一个 Region

解决方案:

*// 方案1: 盐值散列*

String rowKey = String.format("%09d", userId % 100) + "_" + timestamp + "_" + id;

*// 方案2: 哈希前缀*

String rowKey = HashUtils.md5(id).substring(0, 4) + "_" + id + "_" + timestamp;

*// 方案3: 翻转时间戳*

long reverseTimestamp = Long.MAX_VALUE - System.currentTimeMillis();

String rowKey = String.format("%d_%d", reverseTimestamp, id);

### 4.2 推荐 RowKey 格式

*// 活动记录 - 按宠物+时间定位*

rowKey = String.format("%015d_%s_%015d", petId, dateStr, activityRecordId);

*// 点赞 - 按目标+时间定位*

rowKey = String.format("%s_%015d_%015d_%015d", 

​    targetType, targetId, userId, timestamp);

*// 通知 - 按用户+时间定位*

rowKey = String.format("%015d_%015d_%015d", 

​    userId, Long.MAX_VALUE - timestamp, notificationId);

*// 动态 - 按用户+时间定位*

rowKey = String.format("%015d_%015d_%015d", 

​    userId, Long.MAX_VALUE - timestamp, momentId);

------

## 五、列族设计原则

### 5.1 列族数量控制

| 原则           | 说明                 |
| :------------- | :------------------- |
| 列族不宜过多   | 一般 2-3 个列族      |
| 按访问模式分组 | 冷数据放在单独的列族 |
| 预分区考虑     | 列族影响 Region 划分 |

### 5.2 列族配置建议

*<!-- activity_record 表配置 -->*

<table>

​    <name>petcare_cold:activity_record</name>

​    

​    *<!-- 主数据列族 -->*

​    <column_family>

​        <name>info</name>

​        <compression>LZ4</compression>

​        <bloomfilter>true</bloomfilter>

​        <blockcache>true</blockcache>

​        <versions>1</versions>

​        <min_versions>0</min_versions>

​        <ttl>31536000</ttl>  *<!-- 1年 -->*

​    </column_family>

​    

​    *<!-- 元数据列族 -->*

​    <column_family>

​        <name>metadata</name>

​        <compression>LZ4</compression>

​        <bloomfilter>true</bloomfilter>

​        <blockcache>false</blockcache>

​        <versions>1</versions>

​        <ttl>31536000</ttl>

​    </column_family>

</table>

------

## 六、冷数据迁移策略

### 6.1 迁移触发条件

| 表名            | 迁移条件                          | 保留周期     |
| :-------------- | :-------------------------------- | :----------- |
| activity_record | 创建时间 > 30天                   | 保留原始记录 |
| likes           | 创建时间 > 30天 且 非当日热点     | 保留原始记录 |
| notifications   | 创建时间 > 30天 且 is_read = true | 保留原始记录 |
| moments         | 最后访问时间 > 7天                | 保留原始记录 |
| status          | 创建时间 > 30天                   | 保留原始记录 |
| comments        | 创建时间 > 90天                   | 保留原始记录 |

### 6.2 迁移流程

┌─────────────────────────────────────────────────────────────────┐

│                        冷数据迁移流程                            │

└─────────────────────────────────────────────────────────────────┘

​                                │

​                                ▼

┌─────────────────────────────────────────────────────────────────┐

│ 1. 定时任务扫描 MySQL 冷数据 (每天凌晨2点执行)                    │

│    - 查询条件: created_at < NOW() - INTERVAL 30 DAY            │

│    - 批量读取: 每次1000条                                        │

└─────────────────────────────────────────────────────────────────┘

​                                │

​                                ▼

┌─────────────────────────────────────────────────────────────────┐

│ 2. 数据转换与清洗                                                │

│    - 转换为 HBase RowKey 格式                                    │

│    - 补充反查所需的关联数据 (快照)                                │

│    - 数据压缩 (可选)                                             │

└─────────────────────────────────────────────────────────────────┘

​                                │

​                                ▼

┌─────────────────────────────────────────────────────────────────┐

│ 3. 批量写入 HBase                                                │

│    - 使用 Put API 批量写入                                       │

│    - 写入配置: WAL = false (批量写入)                           │

│    - 性能配置: 预分区、并发写入                                  │

└─────────────────────────────────────────────────────────────────┘

​                                │

​                                ▼

┌─────────────────────────────────────────────────────────────────┐

│ 4. MySQL 标记冷数据状态                                          │

│    - UPDATE table SET storage_status = 'COLD'                   │

│    - 添加 storage_location = 'HBASE'                            │

│    - 添加 hbase_row_key = '...'                                 │

└─────────────────────────────────────────────────────────────────┘

​                                │

​                                ▼

┌─────────────────────────────────────────────────────────────────┐

│ 5. 定期清理 MySQL 冷数据 (可选)                                  │

│    - 确认 HBase 数据完整性后                                     │

│    - DELETE FROM mysql WHERE storage_status = 'COLD'            │

└─────────────────────────────────────────────────────────────────┘

------

## 七、查询接口设计

### 7.1 双写/双读策略

@Service

public class ActivityRecordService {

​    

​    @Autowired

​    private JdbcTemplate mysqlTemplate;

​    

​    @Autowired

​    private HBaseTemplate hbaseTemplate;

​    

​    */***

​     ** 查询活动记录 - 自动路由*

​     **/*

​    public List<ActivityRecordDTO> queryActivityRecords(Long petId, Date startDate, Date endDate) {

​        *// 判断查询范围是否涉及冷数据*

​        boolean includesColdData = startDate.before(getColdDataThreshold());

​        

​        if (includesColdData) {

​            *// 1. 查询热数据 (MySQL)*

​            List<ActivityRecordDTO> hotRecords = queryHotFromMySQL(petId, startDate, endDate);

​            

​            *// 2. 查询冷数据 (HBase)*

​            List<ActivityRecordDTO> coldRecords = queryColdFromHBase(petId, startDate, endDate);

​            

​            *// 3. 合并结果*

​            return mergeRecords(hotRecords, coldRecords);

​        } else {

​            *// 仅查询热数据*

​            return queryHotFromMySQL(petId, startDate, endDate);

​        }

​    }

​    

​    */***

​     ** 保存活动记录*

​     **/*

​    public void saveActivityRecord(ActivityRecord record) {

​        *// 1. 写入 MySQL*

​        mysqlTemplate.insert(record);

​        

​        *// 2. 如果是历史数据，直接写入 HBase*

​        if (record.getCreatedAt().before(getColdDataThreshold())) {

​            saveToHBase(record);

​        }

​    }

}

### 7.2 HBase 查询服务

@Service

@Slf4j

public class HBaseQueryService {

​    

​    private final HBaseTemplate hbaseTemplate;

​    

​    */***

​     ** 按时间范围查询冷数据*

​     **/*

​    public List<ActivityRecordDTO> queryByTimeRange(

​            String tableName,

​            Long petId,

​            Date startDate,

​            Date endDate) {

​        

​        *// 构建 Scan 条件*

​        Scan scan = new Scan();

​        

​        *// RowKey 前缀: petId_startDate*

​        String startRowKey = String.format("%015d_%s", petId, formatDate(startDate));

​        String stopRowKey = String.format("%015d_%s", petId, formatDate(endDate) + "~");

​        

​        scan.withStartRow(startRowKey.getBytes(), true);

​        scan.withStopRow(stopRowKey.getBytes(), true);

​        

​        *// 添加过滤器*

​        scan.setFilter(new SingleColumnValueFilter(

​            Bytes.toBytes("info"),

​            Bytes.toBytes("activity_date"),

​            CompareOp.GTE,

​            Bytes.toBytes(startDate)

​        ));

​        

​        *// 执行查询*

​        List<ActivityRecordDTO> results = new ArrayList<>();

​        try (Scanner<ActivityRecordDTO> scanner = hbaseTemplate.scan(tableName, scan)) {

​            for (ActivityRecordDTO record : scanner) {

​                results.add(record);

​            }

​        }

​        

​        return results;

​    }

}

------

## 八、性能优化建议

### 8.1 HBase 集群配置

| 配置项                 | 推荐值     | 说明                 |
| :--------------------- | :--------- | :------------------- |
| RegionServer 数量      | 3-5 台     | 根据数据量调整       |
| 每个 RegionServer 内存 | 32-64 GB   | HBase 堆内存 16-32GB |
| HDFS 副本数            | 3          | 保证数据可靠性       |
| BlockCache             | 40% 堆内存 | 热数据缓存           |
| MemStore               | 40% 堆内存 | 写缓存               |

### 8.2 表预分区策略

*// 创建表时预分区*

byte[][] splitKeys = new byte[][] {

​    Bytes.toBytes("000000000000000_"),

​    Bytes.toBytes("111111111111111_"),

​    Bytes.toBytes("222222222222222_"),

​    Bytes.toBytes("333333333333333_"),

​    Bytes.toBytes("444444444444444_"),

​    Bytes.toBytes("555555555555555_"),

​    Bytes.toBytes("666666666666666_"),

​    Bytes.toBytes("777777777777777_"),

​    Bytes.toBytes("888888888888888_"),

​    Bytes.toBytes("999999999999999_")

};

admin.createTable(tableDescriptor, splitKeys);

### 8.3 压缩配置

| 压缩算法 | 压缩比 | 性能影响 | 适用场景     |
| :------- | :----- | :------- | :----------- |
| LZ4      | 2:1    | 低       | 通用场景     |
| SNAPPY   | 2:1    | 低       | 追求写入性能 |
| ZSTD     | 3:1    | 中       | 存储优先     |

------

## 九、实施计划建议

### 9.1 第一阶段: activity_record 冷存储

时间: 第1-2周

目标: 完成活动记录表冷存储

任务:

1. HBase 环境验证
2. activity_record 表设计
3. 数据迁移工具开发
4. 迁移脚本编写与测试
5. 上线与监控

### 9.2 第二阶段: likes + notifications 冷存储

时间: 第3-4周

目标: 完成点赞和通知表冷存储

任务:

1. 表设计
2. 数据迁移
3. 查询路由改造
4. 上线验证

### 9.3 第三阶段: moments + comments 冷存储

时间: 第5-6周

目标: 完成社区数据冷存储

任务:

1. 表设计
2. 历史数据迁移
3. 新数据双写改造
4. 上线验证

------

## 十、总结

| 项目        | 内容                   |
| :---------- | :--------------------- |
| 命名空间    | `petcare_cold`         |
| 冷存储表数  | 6 张                   |
| RowKey 策略 | 业务ID + 时间戳 + 散列 |
| 列族数量    | 每表 2-4 个列族        |
| 数据保留    | 至少 1 年              |
| 迁移策略    | 定时任务 + 双写/双读   |