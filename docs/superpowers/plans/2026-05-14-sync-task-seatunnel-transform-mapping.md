# 同步任务 SeaTunnel Transform 字段映射修复计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复已有目标表场景下手动字段映射无法正确写入 SeaTunnel 执行配置的问题。

**Architecture:** 字段连线仍由第 2 步 `fieldRows` 维护；SeaTunnel 配置生成改为 `source -> transform -> sink` 三段链路。source 只读取需要的源字段，transform 按目标字段顺序生成 `源字段 as 目标字段`，sink 读取 transform 输出。

**Tech Stack:** Vue 3 TSX、Naive UI、SeaTunnel HOCON 配置、Puppeteer 点击 QA、ACP 文档基线。

---

### Task 1: 固化需求和测试规则

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/.ai/sync-task/core/req.md`
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/.ai/sync-task/product/prd.md`
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/.ai/sync-task/test/test-matrix.md`
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/.ai/quality/e2e-click-testing/test-cases.md`

- [x] **Step 1: 明确 SeaTunnel transform 规则**

要求已有目标表手动异名映射必须生成：

```hocon
source {
  Jdbc {
    result_table_name = "sync_source"
  }
}

transform {
  Sql {
    source_table_name = "sync_source"
    result_table_name = "sync_mapped"
    query = "select id, id as ajbh, ajbh as ajmc from sync_source"
  }
}

sink {
  Jdbc {
    source_table_name = "sync_mapped"
  }
}
```

- [x] **Step 2: 新增 TC-P2-014**

通过标准：手动连接异名字段后，配置预览包含 source 临时表、transform 映射表、sink 指向 transform 输出。

### Task 2: 写失败用例

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/tmp_sync_task_v4_qa.js`

- [x] **Step 1: 增加配置断言**

断言配置中必须包含：

```text
result_table_name = "sync_source"
transform {
Sql {
source_table_name = "sync_source"
result_table_name = "sync_mapped"
query = "select id, id as ajbh, ajbh as ajmc
source_table_name = "sync_mapped"
```

- [x] **Step 2: 运行并确认失败**

Run:

```bash
node tmp_sync_task_v4_qa.js
```

Expected before fix:

```text
手动字段映射没有生成 SeaTunnel transform 映射链路
```

### Task 3: 实现配置生成修复

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.tsx`

- [x] **Step 1: source query 只读取所需源字段**

同一个源字段映射多个目标字段时，source query 只输出一次源字段，避免上游字段重名。

- [x] **Step 2: transform query 表达映射关系**

按目标字段区顺序生成：

```sql
select id, id as ajbh, ajbh as ajmc from sync_source
```

- [x] **Step 3: sink 读取 transform 输出并显式写目标字段**

sink 增加：

```hocon
source_table_name = "sync_mapped"
query = "insert into public.a6 (id, ajbh, ajmc) values (?, ?, ?)"
```

### Task 4: 验证

**Files:**
- Test: `/Users/luwang/bigdata-build/dolphinscheduler/tmp_sync_task_v4_qa.js`

- [x] **Step 1: 跑浏览器点击 QA**

Run:

```bash
node tmp_sync_task_v4_qa.js
```

Expected:

```text
exit code 0
```

- [x] **Step 2: 跑类型检查**

Run:

```bash
cd dolphinscheduler-ui
./node_modules/.bin/vue-tsc --noEmit
```

Expected:

```text
exit code 0
```
