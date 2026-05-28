---
domain: sync-task
module: agent
type: test-report
created_at: 2026-05-19 01:10:00 +0800
---

# 同步任务 Agent MVP 测试报告

## 1. 测试对象

- 页面：`/sync-task`
- 前端文件：
  - `dolphinscheduler-ui/src/views/sync-task/index.tsx`
  - `dolphinscheduler-ui/src/views/sync-task/index.module.scss`
- 文档：
  - `.ai/sync-task/agent/req.md`
  - `.ai/sync-task/agent/prd.md`
  - `.ai/sync-task/agent/test-cases.md`
  - `.ai/sync-task/agent/implementation-plan.md`

## 2. 测试环境

- 前端：`http://127.0.0.1:5173/sync-task`
- 浏览器：Google Chrome headless，通过 Puppeteer 模拟人工点击。
- 数据源容器：
  - `mysql-container3`：运行中。
  - `my_postgres`：运行中。
- 本轮浏览器 QA 使用 mock Dolphin 接口，验证前端交互、状态编排和保存执行 API 调用链路。

## 3. 验证命令

### 3.1 类型检查

命令：

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui
./node_modules/.bin/vue-tsc --noEmit
```

结果：通过，退出码 `0`。

### 3.2 Agent 解析与套用向导

命令：

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler
node tmp_sync_task_agent_qa.js
```

结果：通过。

覆盖内容：

- 打开 `Agent 创建同步任务` 抽屉。
- 输入命令：`把 mysql case_workbench.ajxx_tab 同步到 pg public.agent_ajxx_tab，只同步5条`。
- Agent 识别 `MYSQL -> POSTGRESQL`。
- Agent 识别源表 `case_workbench.ajxx_tab`。
- Agent 识别目标表 `public.agent_ajxx_tab`。
- Agent 识别抽样限制 `5 条`。
- 自动进入第 2 步“配置同步方案”。
- 字段映射工作台显示源字段区、目标字段设计区。
- 源端和目标端锚点存在，连线存在。
- SeaTunnel 配置预览包含 `LIMIT 5`。

截图：`/tmp/sync-task-agent-qa.png`

### 3.3 Agent 确认并执行

命令：

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler
node tmp_sync_task_agent_execute_qa.js
```

结果：通过。

覆盖内容：

- 打开 Agent 抽屉。
- 输入命令：`将 MySQL case_workbench.ajxx_tab 同步到 PostgreSQL public.agent_run_tab，字段自动映射并立即执行`。
- Agent 自动套用字段映射。
- 调用保存工作流相关接口。
- 调用工作流上线接口。
- 调用启动工作流实例接口。
- 返回同步任务列表。
- 列表出现运行中的 Agent 创建任务。

截图：`/tmp/sync-task-agent-execute-qa.png`

## 4. 用例结果

| 用例 | 结果 | 说明 |
|---|---|---|
| TC-AGENT-P0-001 打开 Agent 抽屉 | PASS | 按钮和抽屉可见 |
| TC-AGENT-P0-002 解析 MySQL 到 PostgreSQL 命令 | PASS | 类型、源表、目标表、limit 均识别 |
| TC-AGENT-P0-003 套用到向导 | PASS | 自动进入第 2 步并生成字段映射 |
| TC-AGENT-P0-004 确认并执行 | PASS | 保存、上线、启动和返回列表链路通过 mock 接口验证 |
| TC-AGENT-P1-002 不破坏手工创建流程 | PARTIAL | 类型检查通过；本轮重点未重复完整手工四步点击 |

## 5. 已知边界

- 本轮 Agent 是本地确定性解析器，不依赖外部 LLM。
- Milvus 未作为 MVP 运行依赖，后续用于元数据语义召回。
- 复杂自然语言、多表同步、调度表达解析暂未实现。
- 确认执行的浏览器 QA 使用 mock Dolphin 接口验证前端调用链路；真实后端运行仍依赖本地 SeaTunnel、数据源权限和 Dolphin 后端可用性。

## 6. 明早查看方式

1. 打开：`http://localhost:5173/sync-task`
2. 点击右上角：`Agent 创建同步任务`
3. 使用示例命令：

```text
把 mysql case_workbench.ajxx_tab 同步到 pg public.agent_ajxx_tab，只同步5条
```

4. 点击：`解析并生成方案`
5. 查看第 2 步字段映射和 Agent 方案卡片。
