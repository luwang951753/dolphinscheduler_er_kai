# DataFlow 公司环境首次部署检查清单

本文档用于公司内网首次部署 DataFlow/DolphinScheduler 二开包时现场执行。当前推荐路径是使用安装包内置的 standalone 引导安装器，并使用 MySQL 作为元数据库。

## 1. 交付物确认

交付目录：

```bash
/Users/luwang/bigdata-build/dolphinscheduler-everything-claude-code/release/dataflow-intranet/
```

必须包含：

- `apache-dolphinscheduler-3.4.1-bin.tar.gz`
- `apache-dolphinscheduler-3.4.1-bin.tar.gz.sha256`
- `README.md`
- `COMPANY-DEPLOYMENT-CHECKLIST.md`

当前包 SHA256：

```bash
7ee31cdce545f18b1c82e39489472b85de6db2ea97e6b9b0734bf506d93ef5b3  apache-dolphinscheduler-3.4.1-bin.tar.gz
```

本地模拟验证结论：

- 安装器可启动。
- MySQL 初始化成功。
- standalone 可启动。
- UI 可访问。
- 首页、主题库、数据回传、数据问题、数据治理、权限、Oracle 插件检查通过。
- 未使用 H2。

## 2. 服务器前置检查

在公司服务器执行：

```bash
java -version
echo "$JAVA_HOME"
df -h
free -h
ulimit -n
```

要求：

- JDK 8，不建议使用 JDK 11/17。
- 安装目录至少预留 5 GB 可用空间。
- 部署用户对安装目录、日志目录、资源目录有读写权限。
- API 端口默认 `12345`，安装器端口默认 `18080`，不要被占用。

端口检查：

```bash
lsof -nP -iTCP:18080 -sTCP:LISTEN || true
lsof -nP -iTCP:12345 -sTCP:LISTEN || true
lsof -nP -iTCP:5678 -sTCP:LISTEN || true
lsof -nP -iTCP:1234 -sTCP:LISTEN || true
```

## 3. MySQL 前置检查

安装器第一版只支持 MySQL 元数据库，不使用 H2。建议提前创建空库，库名只使用字母、数字和下划线。

```sql
CREATE DATABASE dataflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER 'dataflow'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON dataflow.* TO 'dataflow'@'%';
FLUSH PRIVILEGES;
```

连接检查：

```bash
mysql -h 数据库IP -P 3306 -u dataflow -p dataflow -e "select version();"
```

## 4. 解压与校验

```bash
mkdir -p /opt/dataflow
cd /opt/dataflow
tar -xzf /path/to/apache-dolphinscheduler-3.4.1-bin.tar.gz -C /opt/dataflow
cd /path/to/release/dataflow-intranet
sha256sum -c apache-dolphinscheduler-3.4.1-bin.tar.gz.sha256
```

如果校验文件和安装包不在同一目录，手工比对 SHA256。

## 5. 启动安装引导

```bash
cd /opt/dataflow/apache-dolphinscheduler-3.4.1-bin/standalone-server
export JAVA_HOME=/usr/local/jdk8
export INSTALLER_PORT=18080
export INSTALLER_HOST=服务器IP
bash bin/install-web.sh
```

终端会打印带 token 的安装地址：

```text
http://服务器IP:18080/install?token=...
```

安装页面按顺序执行：

1. 环境检查。
2. MySQL 连接检查。
3. 配置预览。
4. 确认安装。
5. 等待安装完成。

注意：

- 不要重复点击安装按钮。
- 安装完成后会生成 `standalone-server/install.lock`。
- 已生成 `install.lock` 后不要直接二次安装，除非先备份配置和数据库。

## 6. 安装后服务检查

```bash
cd /opt/dataflow/apache-dolphinscheduler-3.4.1-bin/standalone-server
bash bin/status.sh
curl -s http://127.0.0.1:12345/dolphinscheduler/actuator/health
```

预期：

```json
{"status":"UP"}
```

如果不是本机访问，把 `127.0.0.1` 替换为服务器 IP。

日志检查：

```bash
tail -200 logs/dolphinscheduler-standalone.log
tail -120 logs/offline-installer.log
```

重点确认没有：

- MySQL 连接失败。
- 端口占用。
- SQL 初始化失败。
- Magic API 资源加载失败。

## 7. 浏览器验收

访问：

```text
http://服务器IP:12345/dolphinscheduler/ui/
```

默认账号：

```text
admin / dolphinscheduler123
```

必须验收：

- 登录页可打开，标题和图标为 DataFlow。
- 登录后首页可打开。
- 首页 Magic API 指标能返回数据。
- 主题库页面可打开。
- 数据回传页面查询能返回表格数据。
- 数据问题页面查询能返回表格数据。
- 数据治理资产接口可访问。
- 数据源列表可访问。
- 未登录访问 `/dolphinscheduler/users/get-user-info` 返回 401。
- 安装包内存在 Oracle datasource 插件。

可用命令抽查：

```bash
curl -s http://服务器IP:12345/dolphinscheduler/actuator/health
curl -s http://服务器IP:12345/dolphinscheduler/magic-api/sy/metrics
curl -s 'http://服务器IP:12345/dolphinscheduler/magic-api/data-return/query?target=TIANDI_MIDDLE'
curl -s 'http://服务器IP:12345/dolphinscheduler/magic-api/data-issue/query?target=DATA_CENTER'
```

## 8. Oracle 兼容性检查

安装包应包含 Oracle datasource 插件：

```bash
find /opt/dataflow/apache-dolphinscheduler-3.4.1-bin/standalone-server/plugins/datasource-plugins \
  -name '*datasource-oracle*-shade.jar' -print
```

如果公司环境需要连接 Oracle 业务库，需要另外确认：

- Oracle 网络连通性。
- Oracle 服务名或 SID。
- 用户权限。
- 是否需要额外放置 Oracle JDBC 驱动。

## 9. 停止与重启

停止：

```bash
cd /opt/dataflow/apache-dolphinscheduler-3.4.1-bin/standalone-server
bash bin/stop.sh
```

启动：

```bash
cd /opt/dataflow/apache-dolphinscheduler-3.4.1-bin/standalone-server
export JAVA_HOME=/usr/local/jdk8
bash bin/start.sh
```

## 10. 常见问题处理

安装器打不开：

- 检查 `18080` 是否被占用。
- 检查 URL 是否带 token。
- 检查服务器防火墙是否放行。

MySQL 连接失败：

- 检查库名是否只含字母、数字、下划线。
- 检查账号是否有当前库权限。
- 检查 MySQL 是否允许服务器 IP 访问。

安装成功但页面打不开：

- 检查 `12345` 是否监听。
- 检查 `/dolphinscheduler/actuator/health`。
- 检查 `logs/dolphinscheduler-standalone.log`。

Magic API 返回 HTML：

- 说明请求落到了前端静态资源 fallback，通常是 Magic API 资源未加载或路径不匹配。
- 检查 `data/magic-api/api/数据回传/回传查询.ms` 和 `data/magic-api/api/数据问题/下发查询.ms` 是否在解压目录中。

需要重新安装：

- 先停止服务。
- 备份 MySQL 数据库。
- 备份 `standalone-server/conf/` 和 `standalone-server/install.lock`。
- 确认可以重置后再移除 `install.lock`。
