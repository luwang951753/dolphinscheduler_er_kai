# DataFlow 内网安装包部署说明

本文档对应本次二开打包产物，用于在公司内网 Linux 服务器部署 DataFlow/DolphinScheduler。

## 1. 安装包信息

- 交付目录：`release/dataflow-intranet/`
- 安装包：`apache-dolphinscheduler-3.4.1-bin.tar.gz`
- 校验文件：`apache-dolphinscheduler-3.4.1-bin.tar.gz.sha256`
- 公司现场部署清单：`release/dataflow-intranet/COMPANY-DEPLOYMENT-CHECKLIST.md`
- 编译 JDK：`Java 8`
- 前端产物：已执行生产构建，并打入安装包 `ui/` 目录
- 数据库要求：正式部署使用 MySQL 或 PostgreSQL，不使用 H2

SHA256：

```bash
7ee31cdce545f18b1c82e39489472b85de6db2ea97e6b9b0734bf506d93ef5b3  apache-dolphinscheduler-3.4.1-bin.tar.gz
```

## 2. 服务器准备

推荐准备：

- JDK 8，配置 `JAVA_HOME`
- MySQL 5.7+/8.0 或 PostgreSQL 12+
- ZooKeeper 3.5+，默认配置使用 `localhost:2181`
- Linux 用户具备安装目录读写权限

示例环境变量：

```bash
export JAVA_HOME=/usr/local/jdk8
export PATH=$JAVA_HOME/bin:$PATH
java -version
```

## 3. 解压安装包

```bash
mkdir -p /opt/dataflow
tar -xzf apache-dolphinscheduler-3.4.1-bin.tar.gz -C /opt/dataflow
cd /opt/dataflow/apache-dolphinscheduler-3.4.1-bin
```

校验包完整性：

```bash
sha256sum -c apache-dolphinscheduler-3.4.1-bin.tar.gz.sha256
```

如果校验文件不在服务器同目录，可手工比对 SHA256。

## 4. 推荐：使用引导安装程序

当前安装包内置 standalone 引导安装器，适合公司内网首次部署和演示环境部署。安装器只支持 MySQL 元数据库初始化，不使用 H2。

进入解压后的 standalone 目录：

```bash
cd /opt/dataflow/apache-dolphinscheduler-3.4.1-bin/standalone-server
export JAVA_HOME=/usr/local/jdk8
bash bin/install-web.sh
```

脚本会自动生成一次性 `INSTALLER_TOKEN`，并在终端打印访问地址，例如：

```text
本机访问: http://127.0.0.1:18080/install?token=...
内网访问: http://服务器IP:18080/install?token=...
```

如需固定安装器端口或内网访问 IP：

```bash
export INSTALLER_PORT=18080
export INSTALLER_HOST=服务器IP
bash bin/install-web.sh
```

安装器会执行：

1. 校验 JDK 8、standalone 根目录、端口、目录权限和安装锁。
2. 校验 MySQL 连接。
3. 预览将写入的 `application.yaml`、`common.properties`、`dolphinscheduler_env.sh`。
4. 确认预览后才允许安装。
5. 备份旧配置，写入新配置，必要时初始化 MySQL schema。
6. 启动 standalone 并生成 `install.lock`。

注意：

- `INSTALLER_TOKEN` 必须通过 URL 或请求头传递；没有 token 时安装器 API 不可用。
- 同一时间只允许一个安装任务运行，避免重复点击导致配置并发写入。
- 安装失败时会自动回滚受管配置文件；如果已经执行过数据库初始化，需要人工确认数据库状态。
- 安装完成后 `standalone-server/install.lock` 会阻止重复安装。确需重新安装时，先备份现场配置和数据库，再移除安装锁。
- 回滚接口只回滚安装器管理的 3 个配置文件，不回滚数据库表结构或业务数据。

## 5. 手工初始化数据库

### PostgreSQL

```sql
CREATE DATABASE dolphinscheduler;
CREATE USER dolphinscheduler WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE dolphinscheduler TO dolphinscheduler;
```

初始化表结构：

```bash
psql -h 127.0.0.1 -p 5432 -U dolphinscheduler -d dolphinscheduler \
  -f tools/sql/sql/dolphinscheduler_postgresql.sql
```

### MySQL

```sql
CREATE DATABASE dolphinscheduler DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER 'dolphinscheduler'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON dolphinscheduler.* TO 'dolphinscheduler'@'%';
FLUSH PRIVILEGES;
```

初始化表结构：

```bash
mysql -h 127.0.0.1 -P 3306 -u dolphinscheduler -p dolphinscheduler \
  < tools/sql/sql/dolphinscheduler_mysql.sql
```

也可以配置好 `tools/conf/application.yaml` 和 `bin/env/dolphinscheduler_env.sh` 后执行：

```bash
bash tools/bin/upgrade-schema.sh
```

## 6. 手工修改配置

需要同步修改这些配置文件：

- `api-server/conf/application.yaml`
- `master-server/conf/application.yaml`
- `alert-server/conf/application.yaml`
- `tools/conf/application.yaml`
- 如果使用单进程模式，还要改 `standalone-server/conf/application.yaml`

关键项：

```yaml
spring:
  profiles:
    active: postgresql # 或 mysql
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://数据库IP:5432/dolphinscheduler
    username: dolphinscheduler
    password: your_password

registry:
  type: zookeeper
  zookeeper:
    namespace: dolphinscheduler
    connect-string: ZooKeeperIP:2181
```

MySQL 示例：

```yaml
spring:
  profiles:
    active: mysql
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://数据库IP:3306/dolphinscheduler?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: dolphinscheduler
    password: your_password
```

如果内网访问地址不是本机，还要修改 `api-server/conf/application.yaml`：

```yaml
api:
  base-url: http://服务器IP:12345/dolphinscheduler
  ui-url: http://服务器IP:5173
```

## 7. 启动方式

### 推荐：服务拆分启动

```bash
export JAVA_HOME=/usr/local/jdk8

bash bin/dolphinscheduler-daemon.sh start master-server
bash bin/dolphinscheduler-daemon.sh start worker-server
bash bin/dolphinscheduler-daemon.sh start alert-server
bash bin/dolphinscheduler-daemon.sh start api-server
```

查看状态：

```bash
bash bin/dolphinscheduler-daemon.sh status master-server
bash bin/dolphinscheduler-daemon.sh status worker-server
bash bin/dolphinscheduler-daemon.sh status alert-server
bash bin/dolphinscheduler-daemon.sh status api-server
```

停止：

```bash
bash bin/dolphinscheduler-daemon.sh stop api-server
bash bin/dolphinscheduler-daemon.sh stop alert-server
bash bin/dolphinscheduler-daemon.sh stop worker-server
bash bin/dolphinscheduler-daemon.sh stop master-server
```

### 单机演示模式

正式环境不建议使用 H2。若使用 standalone，请显式指定 MySQL/PostgreSQL：

```bash
export JAVA_HOME=/usr/local/jdk8
export DATABASE=postgresql # 或 mysql
bash standalone-server/bin/start.sh
```

查看：

```bash
bash standalone-server/bin/status.sh
```

停止：

```bash
bash standalone-server/bin/stop.sh
```

## 8. 访问入口

- API 服务默认端口：`12345`
- 默认后端路径：`/dolphinscheduler/`
- 前端静态文件已在安装包 `ui/` 目录内

如果公司内网有 Nginx，建议由 Nginx 托管 `ui/`，并反向代理 `/dolphinscheduler/` 到 API 服务。

## 9. 验收检查

部署后建议按顺序检查：

1. `java -version` 是 1.8。
2. 数据库能连接，表结构已初始化。
3. ZooKeeper 可访问，命名空间正常创建。
4. `master-server`、`worker-server`、`alert-server`、`api-server` 状态为 RUNNING。
5. `api-server/logs/` 无数据库连接失败、ZooKeeper 连接失败、端口占用错误。
6. 浏览器能打开登录页，Logo 显示 DataFlow。
7. 登录后检查数据源、同步任务、数据预览、数据治理、主题库、监控等二开模块。

## 10. 常见问题

### 数据库连接失败

检查 `application.yaml` 里的 `spring.profiles.active`、JDBC URL、用户名密码、数据库防火墙和账号授权。

### ZooKeeper 连接失败

检查 `registry.zookeeper.connect-string`，确认服务器能访问 `2181` 端口。

### API 能启动但页面接口 404

确认前端代理和后端 `server.servlet.context-path: /dolphinscheduler/` 一致。

### 端口冲突

默认端口可在各服务的 `conf/application.yaml` 中修改：

- API：`server.port: 12345`
- Master：`server.port`
- Worker：`server.port`
- Alert：`server.port`

修改后同步调整 Nginx 或访问地址。
