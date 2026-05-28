---
acp-version: "3.9"
file-type: api
domain: offline-installer
module: core
created: 2026-05-18
updated: 2026-05-18
inherit: tech.md
---

# 离线安装向导 API 设计

## 1. 通用约定

基础路径：

```text
/installer/api
```

认证方式：

```http
X-Installer-Token: <one-time-token>
```

通用响应：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

错误响应：

```json
{
  "success": false,
  "code": "DB_CONNECT_FAILED",
  "message": "MySQL 连接失败，请检查主机、端口、库名和账号权限",
  "data": {
    "step": "DATABASE",
    "detail": "Communications link failure"
  }
}
```

密码处理：

- 请求允许携带真实密码。
- 响应、预览和日志中一律脱敏为 `******`。

## 2. 数据结构

### 2.1 安装配置

```json
{
  "javaHome": "/usr/local/jdk1.8.0_371",
  "installDir": "/opt/dolphinscheduler/standalone-server",
  "dolphinPort": 12345,
  "database": {
    "type": "MYSQL",
    "host": "192.168.10.25",
    "port": 3306,
    "database": "dolphinscheduler",
    "username": "ds_user",
    "password": "真实密码",
    "initDatabase": true
  },
  "service": {
    "publicHost": "192.168.10.80",
    "logDir": "./logs",
    "resourceDir": "./data/resource",
    "timezone": "Asia/Shanghai"
  },
  "sync": {
    "seatunnelHome": "/opt/apache-seatunnel-2.3.3",
    "tmpDir": "./data/sync/tmp",
    "logDir": "./logs/sync-task",
    "jdbcDir": "./libs"
  }
}
```

说明：

- `dolphinPort` 是 DolphinScheduler 正式服务端口。
- 安装器自身端口不在 API 请求体里出现。

## 3. 接口列表

### 3.1 获取安装状态

```http
GET /installer/api/status
```

响应：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "installed": false,
    "installLockPath": "/opt/dolphinscheduler/standalone-server/install.lock",
    "standaloneHome": "/opt/dolphinscheduler/standalone-server",
    "version": "3.4.1-company",
    "installerPort": 18080
  }
}
```

用途：

- 页面初始化时判断是否已安装。
- `installerPort` 只用于页面展示“当前安装向导地址”，不作为可编辑配置。

### 3.2 环境检查

```http
POST /installer/api/check/environment
```

请求：

```json
{
  "javaHome": "/usr/local/jdk1.8.0_371",
  "installDir": "/opt/dolphinscheduler/standalone-server",
  "dolphinPort": 12345
}
```

响应：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "环境检查通过",
  "data": {
    "items": [
      {
        "key": "JDK_VERSION",
        "name": "JDK 版本",
        "status": "PASS",
        "message": "检测到 Java 1.8"
      },
      {
        "key": "DOLPHIN_PORT",
        "name": "Dolphin 服务端口",
        "status": "PASS",
        "message": "12345 当前未被占用"
      },
      {
        "key": "CONF_PERMISSION",
        "name": "配置目录权限",
        "status": "PASS",
        "message": "conf、logs、backup 可写"
      },
      {
        "key": "INSTALL_LOCK",
        "name": "安装锁",
        "status": "PASS",
        "message": "未检测到 install.lock"
      }
    ]
  }
}
```

### 3.3 数据库连接测试

```http
POST /installer/api/check/database
```

请求：

```json
{
  "type": "MYSQL",
  "host": "192.168.10.25",
  "port": 3306,
  "database": "dolphinscheduler",
  "username": "ds_user",
  "password": "真实密码"
}
```

响应：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "数据库连接成功",
  "data": {
    "databaseVersion": "MySQL 8.0",
    "canCreateTable": true,
    "canInsert": true
  }
}
```

### 3.4 生成配置预览

```http
POST /installer/api/preview
```

请求：

```json
{
  "config": "见 2.1 安装配置"
}
```

响应：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "配置预览已生成",
  "data": {
    "files": [
      {
        "name": "application.yaml",
        "path": "/opt/dolphinscheduler/standalone-server/conf/application.yaml",
        "risk": "HIGH",
        "changes": 6,
        "content": "spring:\\n  profiles:\\n    active: mysql\\n..."
      },
      {
        "name": "common.properties",
        "path": "/opt/dolphinscheduler/standalone-server/conf/common.properties",
        "risk": "MEDIUM",
        "changes": 5,
        "content": "resource.storage.type=LOCAL\\n..."
      },
      {
        "name": "dolphinscheduler_env.sh",
        "path": "/opt/dolphinscheduler/standalone-server/conf/dolphinscheduler_env.sh",
        "risk": "MEDIUM",
        "changes": 4,
        "content": "export JAVA_HOME=/usr/local/jdk1.8.0_371\\n..."
      }
    ]
  }
}
```

### 3.5 开始安装

```http
POST /installer/api/install
```

请求：

```json
{
  "config": "见 2.1 安装配置",
  "confirmPreview": true
}
```

响应：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "安装任务已开始",
  "data": {
    "installId": "install-20260518153000"
  }
}
```

### 3.6 查询安装进度

```http
GET /installer/api/install/{installId}/progress
```

响应：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "status": "RUNNING",
    "currentStep": "WRITE_CONFIG",
    "items": [
      {
        "key": "BACKUP_CONFIG",
        "name": "备份旧配置",
        "status": "SUCCESS",
        "message": "已备份到 backup/install-20260518153000"
      },
      {
        "key": "WRITE_CONFIG",
        "name": "写入新配置",
        "status": "RUNNING",
        "message": "正在写入 application.yaml"
      }
    ]
  }
}
```

状态枚举：

| 值 | 含义 |
|----|------|
| `PENDING` | 等待执行 |
| `RUNNING` | 执行中 |
| `SUCCESS` | 成功 |
| `FAILED` | 失败 |

### 3.7 回滚最近一次安装

```http
POST /installer/api/rollback
```

请求：

```json
{
  "backupId": "install-20260518153000"
}
```

响应：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "已从备份恢复配置",
  "data": {
    "restoredFiles": [
      "conf/application.yaml",
      "conf/common.properties",
      "conf/dolphinscheduler_env.sh"
    ]
  }
}
```

## 4. 错误码

| 错误码 | 含义 |
|--------|------|
| `INVALID_TOKEN` | token 缺失或错误 |
| `ALREADY_INSTALLED` | 已存在 install.lock |
| `JDK_VERSION_INVALID` | JDK 版本不是 1.8 |
| `DOLPHIN_PORT_IN_USE` | Dolphin 服务端口被占用 |
| `CONF_PERMISSION_DENIED` | 配置文件无写入权限 |
| `DB_CONNECT_FAILED` | 数据库连接失败 |
| `DB_PERMISSION_DENIED` | 数据库账号权限不足 |
| `PREVIEW_NOT_CONFIRMED` | 未确认配置预览 |
| `BACKUP_FAILED` | 配置备份失败 |
| `WRITE_CONFIG_FAILED` | 写入配置失败 |
| `INIT_DB_FAILED` | 初始化数据库失败 |
| `START_DOLPHIN_FAILED` | 启动 DolphinScheduler 失败 |
| `HEALTH_CHECK_TIMEOUT` | 健康检查超时 |
| `ROLLBACK_FAILED` | 回滚失败 |

