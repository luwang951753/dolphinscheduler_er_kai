---
acp-version: "3.9"
file-type: req
domain: offline-installer
module: core
created: 2026-05-16
updated: 2026-05-18
inherit: ../../rules.md
---

# DolphinScheduler 离线安装向导需求

## 1. 背景

当前 DolphinScheduler 二开版本面向公司内网部署。内网不能连接互联网，服务器通常只有 JDK 1.8。手工修改 `conf/application.yaml`、`conf/common.properties`、`conf/dolphinscheduler_env.sh` 对非开发同事不友好，容易出现 YAML 缩进错误、数据库连接错误、端口冲突和脚本环境变量遗漏。

需要提供一个类似 Windows 桌面软件安装向导的体验，但部署目标是 Linux 服务器，因此采用“本地 Web 安装向导”的方式：运行一个安装服务，浏览器打开 HTML 页面填写配置，安装服务自动写入配置文件、检查环境、初始化数据库并启动 DolphinScheduler。

## 2. 成熟产品参考

| 产品 | 可借鉴点 | 对本项目的启发 |
|------|----------|----------------|
| Gitea | 首次访问进入 Web 安装页，填写数据库、站点 URL、管理员账号后生成 `app.ini` | 安装完成后生成锁文件，避免重复安装；配置写入前先检测 |
| WordPress | 浏览器填写数据库信息后生成 `wp-config.php` | 非开发用户只需要理解表单，不直接编辑配置文件 |
| Nextcloud | Web 安装向导负责检查依赖、填写数据库和管理员信息 | 安装页必须把环境检查、数据库检测、写配置、初始化进度拆开 |
| Jenkins | 首次启动后进入初始化向导 | 安装服务可以临时开放，完成后进入正式系统 |

## 3. 产品目标

1. 内网安装人员只需要执行一个启动命令并打开浏览器填表。
2. 安装向导自动写入 standalone 包内配置文件。
3. 所有危险操作前可预览，所有文件修改前自动备份。
4. 安装完成后自动生成 `install.lock`，默认关闭安装入口。
5. 失败时给出明确原因和建议，而不是要求用户查看大量日志。

## 4. 非目标

1. 不在浏览器中直接写服务器文件；必须通过本地安装服务写文件。
2. 不依赖互联网、npm、Maven 仓库或 CDN。
3. 第一版不做分布式集群部署，只支持 standalone 离线安装。
4. 第一版不做 Docker 镜像安装；Docker 可作为后续增强。

## 5. 安装方式

离线包内新增：

```text
bin/install-web.sh
installer/ds-offline-installer.jar
installer/web/**
conf/application.yaml
conf/common.properties
conf/dolphinscheduler_env.sh
```

安装人员执行：

```bash
export JAVA_HOME=/path/to/jdk8
./bin/install-web.sh
```

终端输出：

```text
安装向导已启动：
http://服务器IP:18080/install?token=一次性Token
```

## 6. 安装向导步骤

| 步骤 | 名称 | 目标 |
|------|------|------|
| 1 | 环境检查 | 检查 JDK、目录权限、Dolphin 服务端口、安装锁 |
| 2 | 数据库配置 | 填写 MySQL 地址、库名、账号、密码并测试连接 |
| 3 | 服务配置 | 配置 Dolphin 访问地址、端口、日志目录、资源目录 |
| 4 | 同步运行配置 | 配置 SeaTunnel 路径、临时目录、任务日志目录 |
| 5 | 预览与安装 | 预览将写入的配置，执行备份、写文件、初始化库、启动服务 |

## 7. 配置写入范围

### 7.1 `conf/application.yaml`

- `spring.profiles.active=mysql`
- `spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver`
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `server.port`
- `dolphinscheduler.api.base-url`

### 7.2 `conf/common.properties`

- `resource.storage.type`
- `resource.storage.upload.base.path`
- `data.basedir.path`
- `shell.env_source_list`
- 同步任务运行相关目录，后续按实现代码确认最终 key。

### 7.3 `conf/dolphinscheduler_env.sh`

- `JAVA_HOME`
- `DOLPHINSCHEDULER_HOME`
- `PATH`
- `SEATUNNEL_HOME`
- 其他同步任务执行需要的环境变量。

## 8. 安全要求

1. 安装 URL 必须带一次性 token。
2. 安装向导服务默认监听 `0.0.0.0:18080`，但该端口只用于临时打开安装页，不作为页面表单项展示；如端口冲突，通过启动脚本参数或环境变量调整。
3. 安装完成后写入 `install.lock`，再次访问只显示“已安装”。
4. 密码字段不写入日志，预览时默认脱敏。
5. 每次写配置前备份到 `backup/install-YYYYMMDDHHmmss/`。
6. 安装器只允许写 standalone 包目录内的白名单文件。

## 9. 验收标准

1. 用户不需要手工编辑 YAML、properties、sh 文件。
2. 数据库连接失败、端口占用、JDK 版本不对都能在页面看到明确提示。
3. 安装完成后可以访问 `http://服务器IP:12345/dolphinscheduler/ui`。
4. 重复安装时能识别已有配置并要求二次确认或阻止覆盖。
5. 安装失败可以回滚到上一次备份配置。
