---
domain: sync-task
module: agent
type: implementation-plan
updated_at: 2026-05-19 00:33:17 +0800
---

# 同步任务 Agent MVP 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 `/sync-task` 页面新增一个可体验的自然语言同步任务 Agent，能够解析命令、填充向导，并复用现有保存执行链路。

**Architecture:** 第一版在前端组件内实现确定性解析器和同步任务编排，不引入外部模型与 Milvus 运行依赖。Agent 输出结构化计划，再写入现有同步任务向导状态。

**Tech Stack:** Vue 3 TSX、Naive UI、现有 DolphinScheduler 数据源 / 工作流 API、现有同步任务状态和 SeaTunnel 配置生成逻辑。

---

## Task 1: 文档基线

- [x] 新建 `.ai/sync-task/agent/req.md`。
- [x] 新建 `.ai/sync-task/agent/prd.md`。
- [x] 新建 `.ai/sync-task/agent/test-cases.md`。
- [x] 更新 `.ai/index.md`。
- [x] 更新 `.ai/sync-task/core/change-log.md`。

## Task 2: 前端 Agent 状态和解析器

- [x] 在 `dolphinscheduler-ui/src/views/sync-task/index.tsx` 增加 Agent 类型、状态、解析函数。
- [x] 支持数据源类型、库表、目标表、limit 和立即执行意图识别。
- [x] 支持置信度和风险提示生成。

## Task 3: Agent 元数据编排

- [x] 复用现有数据源、项目、库表、字段加载函数。
- [x] 根据 Agent 计划写入 `state.source`、`state.target`、`state.fieldRows`、`state.sourceFilters`。
- [x] 自动全选字段并按 V4 规则建立自动映射。

## Task 4: Agent UI

- [x] 列表页新增 `Agent 创建同步任务` 按钮。
- [x] 新增右侧抽屉：命令输入、示例、进度、方案卡片、风险提示、动作按钮。
- [x] 新增 `index.module.scss` 样式，保持同步任务现有产品风格。

## Task 5: 验证

- [x] 运行 `dolphinscheduler-ui` 类型检查。
- [x] 启动前端页面并用浏览器模拟输入命令、解析、套用到向导。
- [x] 在 mock Dolphin 接口环境中验证确认执行链路。
- [x] 生成 `.ai/sync-task/agent/test-report-2026-05-19.md`。
