---
acp-version: "3.9"
file-type: tech
domain: offline-installer
module: core
created: 2026-05-18
updated: 2026-05-18
inherit: req.md
---

# 离线安装向导技术设计

## 1. 技术定位

离线安装向导是 standalone 包内的临时安装服务，不属于 DolphinScheduler 正式运行进程。它只负责首次安装前的环境检查、配置预览、配置写入、数据库初始化和启动 DolphinScheduler。

安装完成后生成 `install.lock`，默认关闭安装入口，避免生产环境中误覆盖配置。

## 2. 交付形态

standalone 分发包新增：

```text
standalone-server/
  bin/
    install-web.sh
    start.sh
  installer/
    ds-offline-installer.jar
    web/
      index.html
      assets/**
  conf/
    application.yaml
    common.properties
    dolphinscheduler_env.sh
    sql/**
  backup/
  install.lock
```

说明：

- `install-web.sh` 启动临时安装服务并输出一次性访问地址。
- `ds-offline-installer.jar` 内置后端 API 和静态页面服务。
- `installer/web/**` 可作为静态资源目录，便于不改 Java 代码也能替换页面。
- `install.lock` 位于 `standalone-server/install.lock`。

## 3. 启动方式

```bash
export JAVA_HOME=/path/to/jdk8
./bin/install-web.sh
```

默认输出：

```text
DolphinScheduler 离线安装向导已启动
访问地址: http://<server-ip>:18080/install?token=<one-time-token>
说明: 18080 是安装向导临时端口，不是 DolphinScheduler 服务端口
```

安装器端口规则：

1. 默认使用 `18080`。
2. 页面不展示安装器端口，避免和 Dolphin 服务端口混淆。
3. 如果 `18080` 被占用，脚本提示使用 `INSTALLER_PORT=18081 ./bin/install-web.sh`。
4. 安装器端口不写入 `application.yaml`、`common.properties` 或 `dolphinscheduler_env.sh`。

## 4. 技术架构

```text
浏览器
  |
  | HTTP + token
  v
ds-offline-installer.jar
  ├─ StaticWebController      提供安装页面
  ├─ EnvironmentController    环境检查
  ├─ DatabaseController       MySQL 连接测试
  ├─ PreviewController        生成配置文件预览
  ├─ InstallController        执行安装、查询进度
  ├─ RollbackController       安装失败后回滚
  └─ service/
      ├─ InstallContextService      解析 standalone 路径和 token
      ├─ EnvironmentCheckService    JDK、端口、权限、安装锁
      ├─ ConfigRenderService        渲染 application/common/env
      ├─ ConfigBackupService        备份白名单配置文件
      ├─ ConfigWriteService         原子写入配置文件
      ├─ DatabaseInitService        初始化 MySQL 元数据库
      ├─ DolphinProcessService      调用 bin/start.sh 启动 DS
      └─ InstallProgressService     安装步骤进度
```

## 5. 文件写入白名单

安装器只能写 standalone 包内以下路径：

| 文件 | 允许操作 |
|------|----------|
| `conf/application.yaml` | 备份、覆盖写入 |
| `conf/common.properties` | 备份、覆盖写入 |
| `conf/dolphinscheduler_env.sh` | 备份、覆盖写入 |
| `backup/install-YYYYMMDDHHmmss/**` | 创建备份 |
| `install.lock` | 安装完成后创建 |
| `logs/installer.log` | 写安装器日志 |

禁止：

- 写 standalone 包目录外的任意文件。
- 使用用户输入直接拼接成任意路径。
- 在日志中输出数据库密码。

## 6. 配置生成规则

### 6.1 `application.yaml`

必须写入：

- `spring.profiles.active=mysql`
- `spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver`
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `server.port`
- `dolphinscheduler.api.base-url`

要求：

- 预览阶段密码显示为 `******`。
- 写入阶段使用真实密码。
- YAML 使用结构化渲染或模板渲染，不使用脆弱的字符串替换。

### 6.2 `common.properties`

必须写入：

- `resource.storage.type=LOCAL`
- `resource.storage.upload.base.path`
- `data.basedir.path`
- `shell.env_source_list`
- 同步任务临时目录和日志目录 key。

要求：

- 保留未被安装器管理的原有 key。
- 已管理 key 由安装器统一覆盖。

### 6.3 `dolphinscheduler_env.sh`

必须写入或更新：

- `JAVA_HOME`
- `DOLPHINSCHEDULER_HOME`
- `SEATUNNEL_HOME`
- `PATH`

要求：

- 保留用户已有的非安装器管理环境变量。
- 生成文件保持可执行权限。

## 7. 安装执行流程

```text
1. 校验 token 和 install.lock
2. 再次执行环境检查
3. 再次执行数据库连接测试
4. 生成配置文件内容
5. 创建 backup/install-YYYYMMDDHHmmss/
6. 备份 application.yaml、common.properties、dolphinscheduler_env.sh
7. 原子写入新配置
8. 按需初始化 MySQL 元数据库
9. 调用 standalone-server/bin/start.sh
10. 健康检查 /dolphinscheduler/actuator/health 或登录页
11. 写入 install.lock
12. 返回安装完成信息
```

任意步骤失败：

1. 记录失败步骤和错误摘要。
2. 停止继续执行后续步骤。
3. 页面展示失败原因、日志路径和回滚入口。
4. 如果配置已写入但服务未启动成功，允许从最近备份回滚。

## 8. 安全策略

1. 安装 URL 必须带一次性 token。
2. token 存在内存中，安装服务重启后重新生成。
3. 安装完成后 token 失效。
4. 安装完成后访问安装页面只展示“已安装”和正式访问地址。
5. 后端所有接口都校验 token。
6. 密码只参与数据库连接测试和最终写入，不进入普通日志、错误响应或预览内容。

## 9. 测试策略

### 9.1 单元测试

- 配置渲染测试：验证 3 个文件内容和密码脱敏。
- 路径白名单测试：验证不能写 standalone 外部路径。
- 端口检测测试：验证 Dolphin 服务端口占用时返回明确错误。
- install.lock 测试：验证已有锁时阻止安装。

### 9.2 集成测试

使用临时目录模拟 standalone 包：

```text
tmp-standalone/
  bin/start.sh
  conf/application.yaml
  conf/common.properties
  conf/dolphinscheduler_env.sh
  conf/sql/**
```

测试内容：

- 执行预览不改文件。
- 执行安装后生成备份目录。
- 执行安装后 3 个配置文件写入正确。
- 安装完成后生成 `install.lock`。
- 失败后能从备份回滚。

### 9.3 浏览器点击测试

必须覆盖：

1. 打开安装页面。
2. 环境检查。
3. 填写 MySQL 配置并测试连接。
4. 填写服务配置和同步运行配置。
5. 切换 3 个配置文件预览选项卡。
6. 点击开始安装。
7. 查看安装进度到安装完成。

