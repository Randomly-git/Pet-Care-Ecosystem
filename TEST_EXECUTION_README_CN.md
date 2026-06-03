# 测试执行指南 — 社区动态 API

本文档说明如何配置和执行社区动态管理模块（`3.1.x` 需求）的自动化 API 测试。

## 前提条件

### 必需的服务

所有后端服务必须在测试前启动：

| 服务 | 地址 | 状态 |
|------|------|------|
| Nacos 注册中心 | `47.111.230.62:8848` | 必需 |
| Gateway 网关 | `http://localhost:9000` | 必需 |
| petcare-backend | 注册至 Nacos | 必需 |
| community-backend | 注册至 Nacos | 必需 |
| media-backend | 注册至 Nacos | 创建测试必需 |
| llm-backend | 注册至 Nacos | 可选 |

### 软件要求

- **Java**：JDK 21（Temurin 21.0.9+）
- **Maven**：3.9.x（项目已包含 `mvnw.cmd` 包装器）
- **数据库**：MySQL，需通过配置的数据源可访问

### 测试账号

系统中预注册的登录凭证：
- 用户名：`1`
- 密码：`123`

## 文件放置指南

测试套件包含三个文件，必须放在 `community-backend/` 下的正确目录中。

### 1. 测试 Java 源码

**文件**：`MomentsApiTest.java`

**目标路径**：
```
community-backend
└── src
    └── test
        └── java
            └── petcare
                └── example
                    └── community_backend
                        └── api
                            └── MomentsApiTest.java
```

### 2. EP/BVA 测试数据（CSV）

**文件**：`dynamic_management_test_cases.csv`

**目标路径**：
```
community-backend
└── src
    └── test
        └── resources
            └── dynamic_management_test_cases.csv
```

### 3. STT 测试数据（CSV）

**文件**：`dynamic_management_test_cases_stt.csv`

**目标路径**：
```
community-backend
└── src
    └── test
        └── resources
            └── dynamic_management_test_cases_stt.csv
```

### 文件结构总览

```
community-backend/
├── pom.xml                          （Maven 构建文件——已存在）
├── mvnw.cmd                         （Maven 包装器——已存在）
└── src/
    └── test/
        ├── java/
        │   └── petcare/example/community_backend/api/
        │       └── MomentsApiTest.java          ← 放在这里
        └── resources/
            ├── dynamic_management_test_cases.csv      ← 放在这里
            └── dynamic_management_test_cases_stt.csv  ← 放在这里
```

## 如何运行

### 方式一：通过 Maven 运行（全部测试）

```bash
cd community-backend
./mvnw.cmd test -Dtest=MomentsApiTest
```

### 方式二：通过 IntelliJ IDEA 运行

1. 打开项目根目录（`Pet-Care-Ecosystem/`）
2. 导航至 `community-backend/src/test/java/petcare/example/community_backend/api/MomentsApiTest.java`
3. 右键点击类名或单个测试方法
4. 选择 **Run 'MomentsApiTest'**

### 方式三：运行指定测试组

```bash
cd community-backend
./mvnw.cmd test -Dtest=MomentsApiTest#testCreateMoment_EP
```

可用的测试方法名：
- `testCreateMoment_EP` / `testCreateMoment_BVA`（3.1.1）
- `testCreateMoment_Media_EP` / `testCreateMoment_Media_BVA`（3.1.2）
- `testCreateMoment_DbException`（3.1.3）
- `testCreateMoment_Invisible`（3.1.4）
- `testGetMomentById_EP` / `testGetMomentById_BVA`（3.1.5）
- `testGetMoment_Migrating`（3.1.6）
- `testGetMoment_NoRecord`（3.1.7）
- `testGetMoment_AdminAudit`（3.1.8）
- `testGetMomentsByUser`（3.1.9）
- `testGetMoment_SpecialStates`（3.1.10~3.1.12）
- `testGetAllMomentsPaged`（3.1.13）
- `testGetMoment_ColdData`（3.1.14~3.1.17）
- `testUpdateMoment`（3.1.18~3.1.21）
- `testDeleteMoment`（3.1.22~3.1.25）
- `testSttScenarios`（STT）

## 测试架构

```
dynamic_management_test_cases.csv  ──→  loadCsv()  ──→  List<TestCaseRow>
                                                        ↓
dynamic_management_test_cases_stt.csv ──→  loadCsv()  ──→  List<TestCaseRow>
                                                        ↓
                                           @MethodSource  →  Arguments
                                                        ↓
                              ┌──────────────────────────────────────────┐
                              │  @ParameterizedTest  (JUnit 5)          │
                              │  → executeCreate()  POST /api/v1/moments │
                              │  → executeGetMoment() GET /api/v1/moments│
                              │  → testUpdateMoment() PUT /api/v1/moments│
                              │  → testDeleteMoment() DELETE /api/v1/mom │
                              │  → testSttScenarios()                    │
                              └──────────────────────────────────────────┘
```

### 关键设计决策

| 方面 | 选择 | 理由 |
|------|------|------|
| **测试框架** | JUnit 5 + ParameterizedTest | Spring Boot 原生支持，数据驱动 |
| **HTTP 客户端** | RestTemplate | 不引入额外依赖，完整 HTTP 支持 |
| **替代方案** | REST Assured | 可读性更好但增加依赖 |
| **替代方案** | TestNG @DataProvider | 功能类似但非 Spring Boot 默认 |

### 异常处理策略

```
try {
    ResponseEntity<String> resp = restTemplate.exchange(...);
    // 正常状态码断言
} catch (HttpClientErrorException e) {
    // 4xx：提取状态码继续验证
} catch (HttpServerErrorException e) {
    // 5xx：对服务端无法处理的情况做可接受处理（如 momentId 不存在）
} catch (Exception e) {
    // 其他异常：有效用例则失败，无效用例则通过
}
```

## 测试数据格式

### EP/BVA CSV 列（10 列）

| 索引 | 字段 | 说明 |
|------|------|------|
| 0 | `test_case_id` | 用例唯一标识 |
| 1 | `requirement_id` | 关联需求 ID |
| 2 | `requirement_desc` | 需求描述 |
| 3 | `technique` | EP 或 BVA |
| 4 | `title` | 用例标题 |
| 5 | `test_data` | Python 字典格式：`{'userId': 1, 'content': 'a', 'mediaIds': 1}` |
| 6 | `covered_partitions` | 覆盖的等价类分区 |
| 7 | `expected_results` | 预期行为描述 |
| 8 | `is_valid_case` | True 为有效用例，False 为无效用例 |
| 9 | `boundary_type` | 边界类型（仅 BVA） |

### STT CSV 列（15 列）

| 索引 | 字段 | 说明 |
|------|------|------|
| 0 | `test_case_id` | 用例唯一标识 |
| 1 | `requirement_id` | 关联需求 ID |
| 2 | `requirement_desc` | 需求描述 |
| 3 | `technique` | STT |
| 4 | `title` | 用例标题 |
| 5 | `source_state` | 源状态 |
| 6 | `event` | 触发事件 |
| 7 | `guard` | 守卫条件 |
| 8 | `target_state` | 目标状态 |
| 9 | `action` | 动作描述 |
| 10-14 | 各字段 | 前置条件、步骤、预期结果、覆盖 ID |

## 已知问题

| 问题 | 数量 | 根因 | 状态 |
|------|------|------|------|
| Create 返回 500 | ~20 | `media-backend` 服务未启动 | 环境问题 |
| 无效参数返回 200 | ~30 | Controller 缺少 `@Valid` / `@Min` / `@Max` 注解 | 服务端缺陷 |
| STT 返回 405 | ~34 | CSV 列结构与解析器不匹配 | 需要修复路由 |

## 预期测试结果

- **测试用例总数**：419（385 EP/BVA + 34 STT）
- **预期通过**：~369（~88%）
- **预期失败**：~50（30 个服务端缺陷 + 20 个环境问题）

## 常见问题排查

**问：测试失败，报错 "500 Internal Server Error: [no body]"**
- 检查 `media-backend` 是否运行。创建操作需要媒体服务验证。

**问：测试失败，报错 "Connection refused"**
- 确保 Nacos（`47.111.230.62:8848`）可以访问。
- 确保所有后端服务已注册到 Nacos。

**问：测试失败，报错 "401 Unauthorized"**
- 验证登录端点（`/api/auth/login`）是否可用。
- 确认测试账号凭证（用户名="1"，密码="123"）有效。

**问：测试失败，报错 "405 Method Not Allowed"**
- 仅适用于 STT 测试。需要增强 STT 路由实现。

**问：如何添加新的测试用例？**
1. 编辑 `dynamic_management_test_cases.csv` 或 `dynamic_management_test_cases_stt.csv`
2. 无需修改 Java 代码——测试框架完全数据驱动。
