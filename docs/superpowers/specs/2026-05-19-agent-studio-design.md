# Agent Studio 设计说明

日期：2026-05-19

## 背景

`agent-studio` 是一个独立的企业智能 Agent 工作台。它不放在 DolphinScheduler 前端里，也不替代 DolphinScheduler。它作为上层智能入口，通过自然语言理解、RAG、工具调用和 LangGraph 流程控制，把用户需求转成 DolphinScheduler 可执行的工作流。

第一期业务能力是数据同步 Agent。数据同步是第一个垂直 Agent，不是整个产品边界。

## 已确认方向

- 项目名称：`agent-studio`
- 产品定位：企业智能 Agent 平台
- 第一阶段：最小平台底座 + 数据同步 Agent
- 交互形态：Web 控制台优先
- 第一条业务链路：关系型数据库到关系型数据库
- 同步执行：优先使用 DolphinScheduler 现有任务类型，例如 DataX、Shell、Python
- 数据源来源：复用 DolphinScheduler 已有数据源，同时允许在 Agent 控制台新增数据源
- 同步策略：全量同步、增量同步、定时调度
- 执行策略：低风险步骤可自动推进，高风险操作必须人工确认

## 产品范围

### MVP 包含

- 一个聚焦的 Web Agent 工作台
- 一个内置 Agent：数据同步 Agent
- 自然语言需求输入
- 意图解析和缺失信息追问
- 通过 DolphinScheduler 数据源 API 做元数据查询
- 同步方案生成与预览
- 字段映射和基础类型兼容判断
- 全量同步和增量同步
- 调度语义解析，例如“每天凌晨 2 点”或“每小时一次”
- 风险评估和确认卡片
- DolphinScheduler 工作流创建、发布、调度、执行对接
- 执行记录追踪，包括 workflow code、instance id、状态、日志入口和错误摘要
- LLM、Milvus、DolphinScheduler API 的基础配置

### MVP 不包含

- 多 Agent 市场
- 复杂插件系统
- 企业级多租户权限模型
- 完整审计中心
- 跨 Agent 编排
- DolphinScheduler 前端改造
- CDC 实时同步
- 复杂多表编排
- 自动清空或覆盖目标表
- 自动修改已有生产工作流

## 信息架构

```text
agent-studio
  首页 / 工作台
    自然语言输入
    当前 Agent：数据同步 Agent
    最近会话
    最近执行

  数据同步 Agent
    需求输入
    解析结果
    元数据校验
    同步方案预览
    风险确认
    创建 / 发布 / 执行
    执行状态

  数据源
    DolphinScheduler 已有数据源
    新增数据源
    连接测试
    库 / 表 / 字段预览

  知识库
    DolphinScheduler API 说明
    同步模板
    字段类型映射
    常见错误与修复建议

  执行记录
    Agent 会话
    生成方案
    DolphinScheduler 工作流
    DolphinScheduler 实例
    状态与错误摘要

  系统配置
    LLM 配置
    Milvus 配置
    DolphinScheduler API 配置
```

## 核心用户流程

```text
用户输入自然语言同步需求
  -> Agent 解析同步意图
  -> Agent 检索知识库和同步模板
  -> Agent 匹配或创建 DolphinScheduler 数据源
  -> Agent 查询源端和目标端元数据
  -> Agent 生成结构化同步方案
  -> Agent 评估风险
  -> 低风险动作可自动推进
  -> 高风险动作等待用户确认
  -> Agent 创建 / 发布 / 执行 DolphinScheduler 工作流
  -> Agent 总结工作流、实例、日志、状态和下一步建议
```

MVP 的 UI 应保持轻量。主页面重点展示自然语言输入、Agent 对话、解析结果、方案预览、风险确认和执行反馈。平台导航保留但不做复杂：数据同步 Agent、数据源、知识库、执行记录、系统配置。

## 技术架构

```text
Web 控制台
  -> FastAPI Backend
    -> LangGraph Agent Runtime
      -> RAG Service
        -> Milvus
      -> Tool Layer
        -> DolphinScheduler API Client
        -> Metadata Tools
        -> Plan Validator
      -> Application Storage
```

推荐项目结构：

```text
agent-studio/
  backend/
    app/
      api/
      core/
      agents/
        data_sync/
      rag/
      tools/
      integrations/
        dolphinscheduler/
      schemas/
      storage/
      config/

  frontend/
    src/
      pages/
      components/
      services/

  docs/
    superpowers/specs/
```

## 模块职责

### Web 控制台

Web 控制台只负责交互。它展示自然语言输入、Agent 追问、解析结果、方案预览、风险卡片和执行状态，不内嵌复杂同步规划规则。

### FastAPI Backend

后端为前端提供接口，管理会话，保存生成方案和执行记录，并把 Agent 流程交给 LangGraph Runtime。它是平台入口，不直接拼接 DolphinScheduler 工作流 payload。

### LangGraph Agent Runtime

Runtime 负责协调数据同步 Agent 的状态机：

```text
parse_intent
  -> retrieve_knowledge
  -> resolve_datasource
  -> inspect_metadata
  -> generate_sync_plan
  -> validate_risk
  -> build_ds_workflow
  -> wait_or_execute
  -> summarize_result
```

### 数据同步 Agent

数据同步 Agent 负责数据同步领域行为：同步模式、增量条件、调度语义、字段映射、目标表策略和 DolphinScheduler 工作流草案生成。

### RAG Service

RAG Service 从 Milvus 检索上下文。第一阶段知识库应包含：

- DolphinScheduler API 使用说明
- DataX、Shell、Python 任务模板
- 关系型数据库字段类型映射规则
- 常见全量 / 增量同步策略
- 常见错误与修复建议
- 本地 DolphinScheduler 二开接口说明

### Tool Layer

Agent 必须通过类型化工具访问外部系统，不直接调用外部系统。初始工具包括：

- `list_datasources`
- `create_datasource`
- `test_datasource_connection`
- `list_databases`
- `list_tables`
- `list_columns`
- `preview_create_table_ddl`
- `create_workflow_definition`
- `release_workflow_definition`
- `start_workflow_instance`
- `query_workflow_instance`

### DolphinScheduler Integration

该模块封装 DolphinScheduler 登录、token 或 session 管理、请求重试、参数组装和错误归一化。第一期对接本地已确认的 API 区域：

- 创建工作流定义：`POST /projects/{projectCode}/workflow-definition`
- 发布工作流定义：`POST /projects/{projectCode}/workflow-definition/{code}/release`
- 启动工作流实例：`POST /projects/{projectCode}/executors/start-workflow-instance`
- 查询数据源表：`GET /datasources/tables`
- 查询数据源字段：`GET /datasources/tableColumns`
- 查询字段元数据：`GET /datasources/tableColumnMetas`
- 预览目标表 DDL：`POST /datasources/preview-target-table`
- 创建目标表：`POST /datasources/create-target-table`

### Storage

MVP 存储层必须保存：

- 会话输入和追问历史
- 解析后的 intent
- 同步方案版本
- 风险评估和确认记录
- DolphinScheduler workflow code
- DolphinScheduler instance id
- 执行状态摘要

## 数据同步 Agent 流程

1. 需求解析
   - 抽取源端、目标端、表、字段、同步模式、调度和执行偏好。

2. 信息补全
   - 只追问最少必要问题。

3. 数据源处理
   - 优先使用 DolphinScheduler 已有数据源。
   - 如果数据源不存在，允许在 Agent Studio 中创建，但必须经过用户确认。

4. 元数据校验
   - 查询源表和目标表字段。
   - 判断目标表是否存在、字段是否兼容。

5. 同步方案生成
   - 生成结构化 `SyncPlan`，包含 source、target、sync_mode、incremental_column、field_mappings、schedule、ds_task_type、failure_strategy、target_table_strategy。

6. 风险评估
   - 输出风险等级和原因。

7. 工作流生成
   - 把校验后的 `SyncPlan` 转成 DolphinScheduler 工作流参数，包括 `taskDefinitionJson`、`taskRelationJson`、`locations`、`globalParams`、`executionType`。

8. 执行动作
   - 低风险场景可自动创建新工作流草案。
   - 中风险或高风险动作按风险规则要求用户确认。

9. 结果总结
   - 返回 workflow code、instance id、调度状态、日志入口、错误摘要和下一步建议。

## 风险规则

### 低风险

- 解析需求
- 检索知识
- 查询元数据
- 生成同步方案
- 创建新的工作流草案

### 中风险

- 发布新工作流
- 开启定时调度
- 立即执行一次同步
- 目标表不存在但字段完全来自源端元数据，且字段映射兼容时创建目标表

### 高风险

- 新增数据源
- 大表全量同步
- 覆盖目标表
- 增量字段不确定
- 字段缺失或字段类型明显不兼容
- 修改已有工作流

高风险动作不得绕过用户确认。

## 测试方案

### 单元测试

- 自然语言 intent 抽取
- 调度语义到 cron 转换
- 字段映射
- 类型兼容判断
- 风险等级计算
- `SyncPlan` schema 校验
- DolphinScheduler 工作流参数生成

### Agent 流程测试

- 信息完整时一次生成方案
- 缺少源数据源时触发追问
- 缺少增量字段时触发追问
- 目标表不存在时生成 DDL 和确认卡片
- 高风险动作在执行前停止
- DolphinScheduler API 失败时生成清晰错误摘要

### 集成测试

- 查询 DolphinScheduler 数据源、库、表、字段
- 创建 DolphinScheduler 数据源
- 创建工作流定义
- 发布工作流定义
- 启动工作流实例
- 查询工作流实例状态

### 端到端验收

- MySQL 到 MySQL 全量同步
- MySQL 到 PostgreSQL 全量同步
- MySQL 到 MySQL 按 `update_time` 增量同步
- 定时同步创建成功
- 目标表创建后再执行同步
- 执行失败时返回 DolphinScheduler 错误摘要和下一步建议

## 成功标准

- 用户用自然语言描述关系型数据库同步需求后，能得到可读、可确认的同步方案。
- 至少一条关系型数据库同步链路能从方案生成闭环到 DolphinScheduler 工作流执行。
- 高风险动作不能绕过用户确认。
- 每次 Agent 运行都能追踪到需求、方案、确认、DolphinScheduler 工作流和 DolphinScheduler 实例。
- 后续新增第二个 Agent 时，不需要重写平台底座。
