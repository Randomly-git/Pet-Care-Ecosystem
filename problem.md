1. 日常记录

   编辑活动记录→删除附件按钮点击无效果，无日志。

2. 宠物社区

   新增动态图片无法加载，无报错，日志：

   API响应 [GET] /media/related/MOMENT/1434 (44ms) Objectdata: code: 20000data: []message: "操作成功"[[Prototype]]: Objectstatus: 200[[Prototype]]: Objectconstructor: ƒ Object()hasOwnProperty: ƒ hasOwnProperty()isPrototypeOf: ƒ isPrototypeOf()propertyIsEnumerable: ƒ propertyIsEnumerable()toLocaleString: ƒ toLocaleString()toString: ƒ toString()valueOf: ƒ valueOf()__defineGetter__: ƒ __defineGetter__()__defineSetter__: ƒ __defineSetter__()__lookupGetter__: ƒ __lookupGetter__()__lookupSetter__: ƒ __lookupSetter__()__proto__: (...)get __proto__: ƒ __proto__()set __proto__: ƒ __proto__()
   MomentsView.vue:600 动态 1434 的媒体文件: Array(0)length: 0[[Prototype]]: Array(0)

3. 需要新增的内容见《前端开发指导.md》 文档位置在Documents\前端开发指导.md

AI新增API接口：
以下是这两个接口的说明文档：

---

## 1. 获取宠物异常健康记录

### 接口信息

| 项目 | 说明 |
|------|------|
| **接口路径** | `GET /api/activities/records/pet/{petId}/abnormal` |
| **功能描述** | 获取指定宠物所有经BERT分析为异常的活动记录（异常类型值为1-5），包括正常记录（0）和忽略记录（-1）会被过滤。 |

### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 描述 |
|--------|------|------|------|------|
| petId | path | Long | 是 | 宠物ID |

### 响应参数

返回 `List<ActivityRecordDTO>` 数组，字段说明如下：

| 字段 | 类型 | 描述 |
|------|------|------|
| activityRecordId | Long | 活动记录ID |
| activityId | Long | 活动定义ID |
| activityName | String | 活动名称 |
| activityKindId | Long | 活动种类ID |
| activityKindName | String | 活动种类名称 |
| petId | Long | 宠物ID |
| petName | String | 宠物名称 |
| userId | Long | 用户ID |
| activityDescription | String | 活动描述备注 |
| activityDate | String | 活动日期（格式：yyyy-MM-dd HH:mm:ss） |
| bertResult | Integer | BERT分析原始结果（1-5表示异常类型） |
| bertResultName | String | BERT分析结果中文描述 |

### BERT结果码说明

| 结果码 | 中文描述 |
|--------|----------|
| -1 | 忽略（用户手动忽略） |
| 0 | 正常 |
| 1 | 消化问题 |
| 2 | 寄生虫 |
| 3 | 皮肤问题 |
| 4 | 行动不便 |
| 5 | 耳部感染 |

### 请求示例

```
GET /api/activities/records/pet/12345/abnormal
```

### 响应示例

```json
[
  {
    "activityRecordId": 1001,
    "activityId": 5,
    "activityName": "喂食",
    "activityKindId": 1,
    "activityKindName": "饮食",
    "petId": 12345,
    "petName": "旺财",
    "userId": 67890,
    "activityDescription": "今天狗狗食欲不振，大便稀软",
    "activityDate": "2024-03-15 08:30:00",
    "bertResult": 1,
    "bertResultName": "消化问题"
]
```

### 业务说明

- 该接口主要用于宠物健康监控场景，帮助用户快速发现宠物的健康异常
- 异常记录基于BERT模型对用户填写的文字描述进行分析得出
- 返回的记录按活动日期倒序排列
- 如果宠物没有异常记录，返回空数组

---

## 2. 忽略异常记录

### 接口信息

| 项目 | 说明 |
|------|------|
| **接口路径** | `PUT /api/activities/records/{recordId}/ignore` |
| **功能描述** | 将指定活动记录的BERT分析结果标记为已忽略（状态码 -1），用于处理误报或已关注的不需要重复提醒的异常记录。 |

### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 描述 |
|--------|------|------|------|------|
| recordId | path | Long | 是 | 活动记录ID |

### 响应参数

无响应体（HTTP状态码200表示成功）

### 请求示例

```
PUT /api/activities/records/1001/ignore
```

### 响应示例

成功时返回 HTTP 200 OK，无响应体。

### 业务说明

- 调用此接口后，记录的 `bertResult` 字段将被更新为 `-1`
- `bertResultName` 字段会自动更新为"忽略"
- 被标记为忽略的记录将不会出现在异常健康记录列表中
- 适合以下场景：
  - 用户已查看并确认该异常，无需再次提醒
  - BERT模型分析结果与实际情况不符（误报）
  - 用户暂时不关心此项异常

### 错误码说明

| HTTP状态码 | 说明 |
|------------|------|
| 200 | 成功忽略异常记录 |
| 404 | 指定的活动记录不存在 |

---

## 关联关系说明

这两个接口通常是配合使用的：

1. **查询异常** → 用户通过 `/abnormal` 接口获取所有异常记录
2. **逐条处理** → 用户查看每条异常记录后，可调用 `/ignore` 接口将其标记为已处理
3. **已忽略记录** → 被忽略的记录不会再出现在异常列表中，避免重复提醒

这种设计实现了异常提醒的闭环管理，提升用户体验。



之前还写过这种东西
也就是说，现在后端有几个功能需要前端实现：
1.给定时活动/设置提醒
2.活动记录的分页
3.社区消息（收到点赞/收到评论等） （3月15号）




关于我们的style
角色设定：你是专长玻璃态/毛玻璃界面的设计师，要把品牌展示与产品页做得通透、优雅又有科技感，让用户感觉内容漂浮在渐变背景之上。

场景定位：科技产品着陆页、金融/云服务资讯页、音乐/创意工具的展示页。用户期待清晰层级、透明卡片、柔光描边与微量霓虹高光。

视觉设计理念：以「雾面玻璃卡片 + 背景渐变 + 细腻高光」构建层次。卡片使用半透明白/灰与 backdrop-blur，边框采用 1px–2px 高光或渐变描边；主要文字保持高对比，次级资讯可用半透明浅色。背景选择深色或彩色渐变衬托通透感。

材质与质感：卡片带柔和阴影和内部光晕，营造悬浮感；使用透明度梯度（0.05–0.2）形成层级；可在边角加入微弱高光/玻璃切面。避免厚重实体感，保持光洁与轻盈。

交互体验：Hover 时透明度略升、阴影加深、CTA 亮度提高并可能带微弱光晕；Active 时轻微下沉或缩放 0.98，确保科技感而不夸张。动效 150–250ms，曲线平滑，避免弹跳。

整体氛围：清透、现代、略带未来感。用户进入页面时看到漂浮的玻璃卡片与深色渐变背景，感觉界面轻盈且专业，适合承载高价值内容。

---