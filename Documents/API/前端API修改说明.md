# 🔧 前端API修改说明

## 📋 修改背景
根据团队成员反馈，创建活动记录时显示 `activityid` 参数缺失。这是由于后端API更新，前端调用需要适配新的API格式。

## 🔄 主要变更

### 1. **活动记录API** (`frontend/src/api/activities.js`)

#### 创建活动记录 (`createActivityRecord`)
**变更前:**
```javascript
// JSON格式请求
const response = await request({
  url: `/activities/records/pet/${petId}`,
  method: 'POST',
  data: recordData  // JSON对象
})
```

**变更后:**
```javascript
// multipart/form-data格式请求
const formData = new FormData()
formData.append('activityId', recordData.activityId)  // 必需
formData.append('userId', recordData.userId)          // 必需
if (recordData.description) {
  formData.append('description', recordData.description)
}
if (recordData.date) {
  formData.append('date', recordData.date)
}
if (recordData.file) {
  formData.append('file', recordData.file)
}

const response = await request({
  url: `/activities/records/pet/${petId}`,
  method: 'POST',
  data: formData,
  headers: {
    'Content-Type': 'multipart/form-data'
  }
})
```

**新参数要求:**
- `activityId`: 活动ID（必需）
- `userId`: 用户ID（必需）
- `description`: 活动描述（可选）
- `date`: 活动日期（可选，默认当前时间）
- `file`: 媒体文件（可选）

#### 更新活动记录 (`updateActivityRecord`)
**变更:**
- 同样改为 `multipart/form-data` 格式
- 支持文件替换
- 参数名调整：`newActivityId`（可选）

#### 新增函数
```javascript
// 为活动记录单独上传媒体文件
export const uploadMediaToActivityRecord = async (recordId, fileData) => {
  // 支持为已存在的活动记录单独上传文件
}
```

### 2. **状态记录API** (`frontend/src/api/status.js`)

#### 创建状态记录 (`createStatusRecord`)
**变更前:**
```javascript
// JSON格式请求
const response = await request({
  url: '/status/records',
  method: 'POST',
  data: recordData  // JSON对象
})
```

**变更后:**
```javascript
// multipart/form-data格式请求
const formData = new FormData()
formData.append('statusId', recordData.statusId)      // 必需
formData.append('petId', recordData.petId)            // 必需
formData.append('startDate', recordData.startDate)    // 必需
formData.append('userId', recordData.userId)          // 必需
if (recordData.description) {
  formData.append('description', recordData.description)
}
if (recordData.file) {
  formData.append('file', recordData.file)
}

const response = await request({
  url: '/status/records',
  method: 'POST',
  data: formData,
  headers: {
    'Content-Type': 'multipart/form-data'
  }
})
```

**新参数要求:**
- `statusId`: 状态ID（必需）
- `petId`: 宠物ID（必需）
- `startDate`: 开始日期 yyyy-MM-dd（必需）
- `userId`: 用户ID（必需）
- `description`: 状态描述（可选）
- `file`: 媒体文件（可选）

#### 更新状态记录 (`updateStatusRecord`)
**变更:**
- 改为 `multipart/form-data` 格式
- 支持文件替换
- 参数名调整：`description` 替代 `statusDescription`
- 日期格式改为 yyyy-MM-dd

#### 停止状态记录 (`stopStatusRecord`)
**变更:**
- `endDate` 参数从可选改为必需
- 日期格式从 yyyy-MM-dd'T'HH:mm:ss 改为 yyyy-MM-dd

#### 新增函数
```javascript
// 为状态记录单独上传媒体文件
export const uploadMediaToStatusRecord = async (statusRecordId, fileData) => {
  // 支持为已存在的状态记录单独上传文件
}
```

## 🎯 关键变更总结

### 1. **请求格式变更**
- **从**: `application/json`
- **到**: `multipart/form-data`

### 2. **参数变更**
- **活动记录**: `activityId` 现在是必需参数
- **状态记录**: `userId` 现在是必需参数
- **文件上传**: 新增支持 `file` 和 `userId` 参数

### 3. **日期格式变更**
- **活动记录**: yyyy-MM-dd'T'HH:mm:ss（保持不变）
- **状态记录**: yyyy-MM-dd（简化格式）

### 4. **新增功能**
- 文件上传支持
- 单独上传媒体文件的接口
- 异步文件处理（不影响主流程）

## 🔧 使用示例

### 创建活动记录（带文件）
```javascript
const recordData = {
  activityId: 1,
  userId: 76,
  description: "今天带宠物散步30分钟",
  date: "2025-12-07T17:30:00",
  file: selectedFile  // File对象
}

await createActivityRecord(petId, recordData)
```

### 创建状态记录（不带文件）
```javascript
const recordData = {
  statusId: 1,
  petId: 123,
  startDate: "2025-12-07",
  userId: 76,
  description: "宠物健康状况良好"
}

await createStatusRecord(recordData)
```

## ⚠️ 注意事项

1. **向后兼容**: 如果已有代码使用旧的API格式，需要更新调用方式
2. **错误处理**: 新的API返回格式可能有变化，需要检查响应处理逻辑
3. **文件大小**: 注意文件上传的大小限制
4. **异步处理**: 文件上传是异步的，不会影响记录的创建/更新

## 🧪 测试建议

1. **测试基本创建**: 不带文件的基本记录创建
2. **测试文件上传**: 带文件的记录创建
3. **测试更新**: 记录更新功能
4. **测试单独上传**: 为已存在记录单独上传文件
5. **错误处理**: 测试缺少必需参数的情况

---

**完成修改后，活动记录创建时的 `activityid` 参数缺失问题应该得到解决！** ✅