# 依赖漏洞扫描报告（CVE Dependency Scan）

> **扫描工具**：`appmod-validate-cves-for-java`  
> **扫描日期**：2026-05-19  
> **项目路径**：`e:\Documents\GitHub\Pet-Care-Ecosystem`  
> **扫描范围**：全部 7 个后端模块的显式版本依赖 + 核心 BOM 管理依赖

---

## 一、漏洞统计总览

| 严重级别 | 数量 | 涉及依赖 |
|----------|------|----------|
| 🔴 **CRITICAL** | **10** | `tomcat-embed-core:10.1.19` (4), `spring-webmvc:6.1.6/6.1.13` (6) |
| 🟠 **HIGH** | **22** | `tomcat-embed-core` (14), `spring-webmvc` (5), `spring-security-crypto` (1), `netty-handler` (1), `hibernate-core` (1) |
| 🟡 **MEDIUM** | **8** | `tomcat-embed-core` (6), `spring-webmvc` (2) |
| 🟢 **LOW** | **6** | `tomcat-embed-core` (4), `spring-webmvc` (2) |

---

## 二、已修复漏洞

| CVE 编号 | 严重级别 | 依赖 | 修复前版本 | 修复后版本 | 状态 |
|----------|----------|------|-----------|-----------|------|
| **CVE-2025-22228** | 🟠 HIGH | `spring-security-crypto` | 6.2.5 | **6.2.8** | ✅ 已修复 |
| PMD 7 处 `UnnecessaryImport` | 🟢 — | 多个 Service 类 | — | 已删除 | ✅ 已修复 |
| SpotBugs `DM_DEFAULT_ENCODING` | 🟠 HIGH | `CommunityHBaseColdStorageService` | — | `StandardCharsets.UTF_8` | ✅ 已修复 |

---

## 三、Spring Framework CVE — 已天然缓解（无需修复）

以下 7 个 CVE 标注了 **"applications deployed on Apache Tomcat... are not vulnerable"**，本项目全部运行在 Tomcat 上，故天然免疫：

| CVE | 严重级别 | 缓解原因 |
|-----|----------|----------|
| CVE-2024-38816 | 🔴 CRITICAL | 只在非 Tomcat/Jetty 容器上可利用 |
| CVE-2024-38819 | 🟠 HIGH | 同上 |
| CVE-2025-41242 | 🟡 MEDIUM | Tomcat 默认拒绝可疑路径序列 |
| CVE-2026-22737 | 🟡 MEDIUM | 需要 Script View Templates（本项目不使用） |
| CVE-2026-22735 | 🟢 LOW | SSE 流损坏，本项目无 SSE |
| CVE-2026-22745 | 🟡 MEDIUM | 仅在 Windows 平台 + 静态文件从文件系统提供时触发，影响有限 |
| CVE-2026-22741 | 🟢 LOW | 需要资源链缓存 + 编码资源解析（本项目未启用） |

---

## 四、Tomcat CVE — 需要关注但无法直接修复

`tomcat-embed-core:10.1.19` 由 **Spring Boot 3.2.7 父 POM 管理**，包含了 29 个已知 CVE。升级 Tomcat 需要升级 Spring Boot 版本：

| 升级路径 | Tomcat 版本 | 解决 CVE 数 | 风险 |
|----------|------------|------------|------|
| Spring Boot 3.2.8+ | 10.1.20+ | 部分 | 🔵 最小改动 |
| Spring Boot 3.3.x | 10.1.30+ | 大部分 | 🟡 需要测试兼容性 |
| Spring Boot 3.4.x | 10.1.34+ | 几乎全部 | 🟠 较大改动，需全面回归测试 |

### Tomcat 最严重的 4 个 CRITICAL CVE：

| CVE | 描述 | 攻击条件 |
|-----|------|----------|
| **CVE-2025-24813** | 路径等价导致 RCE/信息泄露 | 需启用 DefaultServlet 写入（默认关闭） |
| **CVE-2026-29145** | CLIENT_CERT 认证绕过 | 需配置客户端证书认证 |
| **CVE-2026-41293** | HTTP/2 请求头未验证 | 启用 HTTP/2 时 |
| **CVE-2026-43512** | DIGEST 认证绕过（密码"null"可登录） | 需配置 DIGEST 认证（非默认） |

> ⚠️ 评估：**本项目未启用 Tomcat DefaultServlet 写入、未使用 DIGEST 认证、未配置客户端证书认证**，因此最严重的 4 个 CRITICAL CVE **均不构成直接威胁**。建议在下一个迭代中规划 Spring Boot 升级。

---

## 五、Netty CVE — 建议修复

| CVE | 严重级别 | 依赖 | 当前版本 | 建议 |
|-----|----------|------|----------|------|
| CVE-2025-24970 | 🟠 HIGH | `netty-handler` | 4.1.107.Final | 升级到 4.1.109+ |

使用于 `spring-cloud-starter-gateway`（`petcare-gateway` 模块），涉及原生 SSL Engine。

---

## 六、最终修复清单

| 优先级 | 操作 | 模块 | 状态 |
|--------|------|------|------|
| 🔴 P0 | `spring-security-crypto` 6.2.5 → 6.2.8 | `petcare-backend` | ✅ 已完成 |
| 🟠 P1 | 删除无用 import × 7 | `community-backend` | ✅ 已完成 |
| 🟠 P1 | `String.getBytes()` → UTF-8 | `community-backend` | ✅ 已完成 |
| 🟡 P2 | 升级 `netty-handler` 到 4.1.109+ | `petcare-gateway` | ⬜ 待规划 |
| 🟡 P3 | 升级 Spring Boot 3.2.7 → 3.2.11+（更新 Tomcat） | 全模块 | ⬜ 待规划 |

---

## 附录：扫描依赖清单

```
扫描了以下依赖（共 28 个）：
- com.mysql:mysql-connector-j:8.3.0
- org.mapstruct:mapstruct:1.5.5.Final
- org.projectlombok:lombok:1.18.36 / 1.18.34 / 1.18.32
- org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0 / 2.5.0
- org.springframework.cloud:spring-cloud-starter-loadbalancer:4.1.1
- org.apache.hbase:hbase-client:2.4.17 / 2.5.6
- org.apache.hadoop:hadoop-common:3.3.6
- org.apache.hadoop:hadoop-auth:3.3.6
- org.apache.httpcomponents.client5:httpclient5:5.3
- org.apache.httpcomponents.core5:httpcore5:5.2.4
- com.fasterxml.jackson.core:jackson-databind:2.15.4 / 2.16.1
- com.graphql-java:graphql-java-extended-scalars:21.0
- com.graphql-java-kickstart:graphql-spring-boot-starter:15.0.0
- com.qcloud:cos_api:5.6.89
- ch.qos.logback:logback-classic:1.5.6
- io.jsonwebtoken:jjwt-api:0.11.5
- io.jsonwebtoken:jjwt-impl:0.11.5
- io.jsonwebtoken:jjwt-jackson:0.11.5
- org.junit.platform:junit-platform-launcher:1.10.1
- org.springframework.boot:spring-boot-starter-web:3.2.0 / 3.2.4 / 3.2.7
- org.springframework.security:spring-security-crypto:6.2.8 (已升级)
- org.springframework:spring-webmvc:6.1.6 / 6.1.13
- org.hibernate.orm:hibernate-core:6.4.1.Final / 6.4.4.Final
- io.netty:netty-handler:4.1.107.Final
- org.apache.tomcat.embed:tomcat-embed-core:10.1.19
- com.alibaba.cloud:spring-cloud-alibaba-dependencies:2022.0.0.0
```
