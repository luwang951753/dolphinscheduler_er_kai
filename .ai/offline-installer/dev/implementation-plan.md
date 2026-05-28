---
acp-version: "3.9"
file-type: implementation-plan
domain: offline-installer
module: dev
created: 2026-05-18
updated: 2026-05-18
inherit: ../core/tech.md
---

# 离线安装向导 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 standalone 分发包中新增一个可离线运行的 Web 安装向导，让实施同事通过浏览器完成环境检查、MySQL 配置、配置预览、备份写入和首次启动。

**Architecture:** 安装器作为 standalone 包内的独立临时 Java 服务运行，不嵌入 DolphinScheduler 正式进程。前端页面调用 `/installer/api/**`，后端只允许写 standalone 包内白名单配置文件，并在安装完成后生成 `install.lock`。

**Tech Stack:** Java 8、Spring Boot、Maven、HTML/CSS/JS、JUnit、临时目录集成测试、Puppeteer 浏览器点击验证。

---

## 1. 文件结构

计划新增或修改：

| 类型 | 路径 | 责任 |
|------|------|------|
| 新增 Maven 模块 | `dolphinscheduler-offline-installer/` | 离线安装器后端和静态页面 |
| 修改根 POM | `pom.xml` | 注册安装器模块 |
| 新增启动脚本 | `dolphinscheduler-standalone-server/src/main/bin/install-web.sh` | 启动安装向导并输出 token URL |
| 修改 assembly | `dolphinscheduler-standalone-server/src/main/assembly/dolphinscheduler-standalone-server.xml` | 把安装器 jar、web 静态资源和脚本放进 standalone 包 |
| 新增静态页面 | `dolphinscheduler-offline-installer/src/main/resources/web/index.html` | 从原型迁移为真实页面 |
| 新增 API | `dolphinscheduler-offline-installer/src/main/java/org/apache/dolphinscheduler/installer/controller/**` | 环境检查、数据库测试、预览、安装、进度、回滚 |
| 新增服务 | `dolphinscheduler-offline-installer/src/main/java/org/apache/dolphinscheduler/installer/service/**` | 文件、配置、数据库、进程和进度逻辑 |
| 新增测试 | `dolphinscheduler-offline-installer/src/test/java/**` | 单元和临时目录集成测试 |

## 2. 开发任务

### Task 1: 创建安装器 Maven 模块

**Files:**

- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/pom.xml`
- Create: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-offline-installer/pom.xml`
- Create: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-offline-installer/src/main/java/org/apache/dolphinscheduler/installer/OfflineInstallerApplication.java`
- Create: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-offline-installer/src/main/resources/application.yaml`

- [x] **Step 1: 注册 Maven 模块**

在根 `pom.xml` 的 `<modules>` 中增加：

```xml
<module>dolphinscheduler-offline-installer</module>
```

- [x] **Step 2: 新增安装器 POM**

模块依赖控制在最小范围：

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
  </dependency>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

- [x] **Step 3: 新增启动类**

启动类只负责启动临时安装服务：

```java
package org.apache.dolphinscheduler.installer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OfflineInstallerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OfflineInstallerApplication.class, args);
    }
}
```

- [x] **Step 4: 编译模块**

运行：

```bash
./mvnw -pl dolphinscheduler-offline-installer -am -DskipTests package
```

预期：安装器模块编译通过。

### Task 2: 实现安装上下文和 token

**Files:**

- Create: `dolphinscheduler-offline-installer/src/main/java/org/apache/dolphinscheduler/installer/core/InstallContext.java`
- Create: `dolphinscheduler-offline-installer/src/main/java/org/apache/dolphinscheduler/installer/core/TokenFilter.java`
- Create: `dolphinscheduler-offline-installer/src/test/java/org/apache/dolphinscheduler/installer/core/InstallContextTest.java`

- [x] **Step 1: 写 InstallContext 测试**

验证 standalone 路径、安装锁路径、白名单根目录：

```java
@Test
void shouldResolveStandaloneHomeAndInstallLock() {
    Path home = tempDir.resolve("standalone-server");
    InstallContext context = InstallContext.from(home, "token-1", 18080);
    assertThat(context.getStandaloneHome()).isEqualTo(home);
    assertThat(context.getInstallLock()).isEqualTo(home.resolve("install.lock"));
}
```

- [x] **Step 2: 实现 InstallContext**

字段：

- `standaloneHome`
- `confDir`
- `backupDir`
- `installLock`
- `token`
- `installerPort`

- [x] **Step 3: 实现 TokenFilter**

规则：

- `/install`、`/assets/**` 放行。
- `/installer/api/**` 必须校验 `X-Installer-Token`。
- token 错误返回 `INVALID_TOKEN`。

- [x] **Step 4: 跑测试**

```bash
./mvnw -pl dolphinscheduler-offline-installer -Dtest=InstallContextTest test
```

### Task 3: 环境检查接口

**Files:**

- Create: `controller/EnvironmentController.java`
- Create: `service/EnvironmentCheckService.java`
- Create: `dto/EnvironmentCheckRequest.java`
- Create: `dto/CheckItem.java`
- Create: `service/EnvironmentCheckServiceTest.java`

- [x] **Step 1: 写端口检测测试**

用本地 `ServerSocket` 占用端口，调用检查服务后应返回 `DOLPHIN_PORT` 失败。

- [x] **Step 2: 写安装锁测试**

临时目录创建 `install.lock` 后，环境检查应返回安装锁失败。

- [x] **Step 3: 实现检查逻辑**

检查项：

- JDK 是否存在且版本包含 `1.8`
- standalone 安装目录是否为解压后的 `standalone-server` 根目录，必须包含 `bin/start.sh`、`bin/install-web.sh`、`installer/ds-offline-installer.jar`、`conf`、`libs`、`api-server/libs`、`master-server/libs`、`worker-server/libs`、`alert-server/libs`、`plugins`
- Dolphin 服务端口是否被占用
- `conf`、`logs`、`backup` 是否可写
- `install.lock` 是否存在

- [x] **Step 6: 修复空目录误放行**

安装目录校验必须先于目录可写校验执行。错误目录不能被自动创建 `conf/logs/backup` 后继续安装；安装接口也需要二次校验，避免跳过页面检查直接调用 API。

- [x] **Step 7: 修复 standalone 离线包自包含**

`start.sh` 默认从当前 `standalone-server` 根目录查找 `api-server/master-server/worker-server/alert-server/plugins`，assembly 打包时必须把这些运行依赖目录放入 `standalone-server` 内，避免内网机器只有 `standalone-server` 目录时启动缺少 classpath。

- [x] **Step 4: 实现 API**

接口：

```http
POST /installer/api/check/environment
```

- [x] **Step 5: 跑测试**

```bash
./mvnw -pl dolphinscheduler-offline-installer -Dtest=EnvironmentCheckServiceTest test
```

### Task 4: 数据库连接测试接口

**Files:**

- Create: `controller/DatabaseController.java`
- Create: `service/DatabaseCheckService.java`
- Create: `dto/DatabaseCheckRequest.java`
- Create: `service/DatabaseCheckServiceTest.java`

- [x] **Step 1: 写参数校验测试**

空 host、空 username、空 password 应返回明确错误，不尝试连接。

- [x] **Step 2: 实现 MySQL JDBC 连接测试**

逻辑：

- 构造 JDBC URL。
- 设置连接超时。
- 执行 `select 1`。
- 查询数据库版本。
- 不记录密码。

- [x] **Step 3: 实现 API**

接口：

```http
POST /installer/api/check/database
```

- [x] **Step 4: 跑测试**

```bash
./mvnw -pl dolphinscheduler-offline-installer -Dtest=DatabaseCheckServiceTest test
```

### Task 5: 配置预览与脱敏

**Files:**

- Create: `controller/PreviewController.java`
- Create: `service/ConfigRenderService.java`
- Create: `dto/InstallConfigRequest.java`
- Create: `dto/PreviewFile.java`
- Create: `service/ConfigRenderServiceTest.java`

- [x] **Step 1: 写预览测试**

输入 MySQL、服务、同步配置，断言：

- 返回 3 个文件。
- `application.yaml` 包含 `spring.profiles.active=mysql`。
- `common.properties` 包含 `resource.storage.type=LOCAL`。
- `dolphinscheduler_env.sh` 包含 `SEATUNNEL_HOME`。
- 预览内容不包含真实密码。

- [x] **Step 2: 实现渲染服务**

规则：

- 预览使用脱敏密码。
- 写入使用真实密码。
- 文件路径全部来自 `InstallContext.confDir`。

- [x] **Step 3: 实现 API**

接口：

```http
POST /installer/api/preview
```

- [x] **Step 4: 跑测试**

```bash
./mvnw -pl dolphinscheduler-offline-installer -Dtest=ConfigRenderServiceTest test
```

### Task 6: 备份、写入和回滚

**Files:**

- Create: `service/ConfigBackupService.java`
- Create: `service/ConfigWriteService.java`
- Create: `controller/RollbackController.java`
- Create: `service/ConfigWriteServiceTest.java`

- [x] **Step 1: 写白名单测试**

尝试写 `../evil.yaml` 必须失败。

- [x] **Step 2: 写备份测试**

临时 standalone 目录有 3 个配置文件，执行备份后应生成：

```text
backup/install-YYYYMMDDHHmmss/application.yaml
backup/install-YYYYMMDDHHmmss/common.properties
backup/install-YYYYMMDDHHmmss/dolphinscheduler_env.sh
```

- [x] **Step 3: 写原子写入测试**

写入后 3 个目标文件内容变更，权限不丢失。

- [x] **Step 4: 写回滚测试**

从指定 backupId 回滚后，3 个配置恢复为备份内容。

- [x] **Step 5: 实现服务和回滚 API**

接口：

```http
POST /installer/api/rollback
```

- [x] **Step 6: 跑测试**

```bash
./mvnw -pl dolphinscheduler-offline-installer -Dtest=ConfigWriteServiceTest test
```

### Task 7: 安装执行和进度查询

**Files:**

- Create: `controller/InstallController.java`
- Create: `service/InstallService.java`
- Create: `service/InstallProgressService.java`
- Create: `service/DolphinProcessService.java`
- Create: `service/InstallServiceTest.java`

- [x] **Step 1: 写成功流程测试**

使用临时 standalone 目录和假的 `bin/start.sh`：

```bash
#!/bin/bash
echo started > ../logs/start.marker
```

执行安装后断言：

- 备份目录存在。
- 3 个配置文件已写入。
- `install.lock` 存在。
- 进度最终为 `SUCCESS`。

- [x] **Step 2: 写失败流程测试**

让 `bin/start.sh` 返回非 0，断言：

- 进度为 `FAILED`。
- 不生成 `install.lock`。
- 响应包含日志路径和失败步骤。

- [x] **Step 3: 实现安装服务**

按技术设计第 7 节顺序执行。

- [x] **Step 4: 实现进度 API**

接口：

```http
POST /installer/api/install
GET /installer/api/install/{installId}/progress
```

- [x] **Step 5: 跑测试**

```bash
./mvnw -pl dolphinscheduler-offline-installer -Dtest=InstallServiceTest test
```

### Task 8: 集成前端页面

**Files:**

- Create: `dolphinscheduler-offline-installer/src/main/resources/web/index.html`
- Modify: `dolphinscheduler-offline-installer/src/main/java/org/apache/dolphinscheduler/installer/controller/StaticWebController.java`

- [x] **Step 1: 从原型迁移页面**

来源：

```text
.ai/offline-installer/prototype/offline-installer-prototype.html
```

保留当前交互：

- 5 步向导。
- 环境检查不展示安装器端口。
- 第 5 步文件选项卡预览。

- [x] **Step 2: 接入真实 API**

替换页面中的假数据和模拟进度：

- 环境检查调用 `/installer/api/check/environment`
- 数据库检测调用 `/installer/api/check/database`
- 预览调用 `/installer/api/preview`
- 安装调用 `/installer/api/install`
- 安装进度轮询 `/installer/api/install/{installId}/progress`

- [x] **Step 3: 浏览器测试**

使用临时安装器服务启动页面，模拟点击 5 步主流程。

### Task 9: 打包进 standalone

**Files:**

- Modify: `dolphinscheduler-standalone-server/src/main/bin/install-web.sh`
- Modify: `dolphinscheduler-standalone-server/src/main/assembly/dolphinscheduler-standalone-server.xml`

- [x] **Step 1: 写启动脚本**

脚本职责：

- 解析 `INSTALLER_PORT`，默认 `18080`。
- 生成一次性 token。
- 检查端口占用。
- 输出安装 URL。
- 启动 `installer/ds-offline-installer.jar`。

- [x] **Step 2: 修改 assembly**

把以下文件打入 standalone 包：

```text
bin/install-web.sh
installer/ds-offline-installer.jar
installer/web/**
```

- [x] **Step 3: 打包验证**

```bash
./mvnw -pl dolphinscheduler-standalone-server -am -DskipTests package
```

预期：`target/standalone-server/bin/install-web.sh` 和 `target/standalone-server/installer/ds-offline-installer.jar` 存在。

执行记录（2026-05-18）：

- 使用 JDK 8 和临时 Maven settings 完成 `dolphinscheduler-standalone-server -am package`，结果 `BUILD SUCCESS`。
- 验证 `target/standalone-server/bin/install-web.sh`、`target/standalone-server/installer/ds-offline-installer.jar`、`target/standalone-server/installer/web/index.html` 均存在。
- 生成离线交付包 `dolphinscheduler-standalone-server/target/dolphinscheduler-standalone-server-3.4.1-offline-installer.tar.gz`，并验证 tar.gz 内包含上述 3 个安装器入口文件。

### Task 10: 最终验证

**Files:**

- Modify: `.ai/offline-installer/test/test-cases.md`
- Create: `.ai/offline-installer/test/test-report-YYYYMMDD.md`

- [x] **Step 1: 更新测试用例**

补充：

- 安装器端口不在页面表单展示。
- 安装器端口冲突由脚本提示。
- 配置预览文件选项卡。
- 安装失败回滚。

- [x] **Step 2: 执行单元测试**

```bash
./mvnw -pl dolphinscheduler-offline-installer test
```

- [x] **Step 3: 执行打包验证**

```bash
./mvnw -pl dolphinscheduler-standalone-server -am -DskipTests package
```

- [x] **Step 4: 执行浏览器点击验证**

启动安装器后用浏览器逐步点击：

1. 环境检查。
2. 数据库配置。
3. 服务配置。
4. 同步运行配置。
5. 配置预览。
6. 开始安装。
7. 查看安装完成状态。

- [x] **Step 5: 生成测试报告**

报告记录：

- 测试时间。
- 测试环境。
- 通过用例。
- 失败用例和修复记录。
- 截图路径。

## 3. 范围控制

第一版不做：

- 分布式集群安装。
- Docker 镜像安装。
- PostgreSQL 元数据库。
- 可视化编辑任意 YAML。
- 在线下载安装依赖。

第一版必须保证：

- 无互联网依赖。
- 不展示安装器端口表单。
- 不泄露数据库密码。
- 只写白名单文件。
- 安装失败可诊断、可回滚。

## 4. 自检

- 需求覆盖：环境检查、数据库检测、配置预览、备份写入、初始化、启动、锁定、回滚均有任务覆盖。
- 占位符检查：本文档无待补充占位项。
- 类型一致性：API 请求体统一使用 `InstallConfigRequest`，Dolphin 正式服务端口统一命名为 `dolphinPort`，安装器端口只在启动脚本和状态接口中出现。
