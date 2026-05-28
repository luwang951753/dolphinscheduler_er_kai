# 同步任务字段映射 V4 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将已确认的 `field-mapping-workbench-v4.html` 原型落地到 DolphinScheduler 同步任务第 2 步正式页面。

**Architecture:** 保留现有 `/sync-task` 四步向导、后端接口和 SeaTunnel 配置生成链路，只重构第 2 步字段映射 UI、映射状态和拖拽交互。字段数据仍来自 `state.source.columns`、`state.target.columns`、`state.fieldRows`，目标是让正式页面与 V4 原型一致，并确保 DDL 与配置生成继续使用最新映射。

**Tech Stack:** Vue 3 TSX、Naive UI、SCSS Modules、DolphinScheduler 现有数据源和工作流接口。

---

## Files

- Modify: `dolphinscheduler-ui/src/views/sync-task/index.tsx`
  - 第 2 步字段映射 UI、字段列、目标字段编辑控件、拖拽映射、类型兼容校验、目标字段名规则。
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.module.scss`
  - 字段映射 V4 布局、锚点、虚线/实线、图例、表格紧凑度、滚动对齐。
- Reference: `.ai/sync-task/prototype/field-mapping-workbench-v4.html`
  - 已确认原型，只作为对照，不再随意改动。
- Reference: `.ai/sync-task/test/test-matrix.md`
  - TC-P2-009 到 TC-P2-013 是本轮新增验收项。
- Reference: `.ai/quality/e2e-click-testing/test-cases.md`
  - 正式开发完成后的真实点击测试脚本和人工验证基线。

---

### Task 1: 对齐源字段区 V4 布局

**Files:**
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.tsx`
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.module.scss`

- [ ] **Step 1: 修改源字段列顺序**

将 `sourceFieldColumns` 调整为以下顺序：

```ts
同步 -> 源字段 -> 源类型 -> 字段注释
```

源字段列里需要展示字段名和内联标签：

```tsx
<div class={styles.sourceFieldInline}>
  <span class={styles.columnName}>{row.sourceColumn || '-'}</span>
  {row.sourcePrimaryKey ? <NTag size='small' bordered={false} type='warning'>主键</NTag> : null}
  <NTag size='small' bordered={false} type={row.sourceNullable ? 'success' : 'error'}>
    {row.sourceNullable ? '可空' : '非空'}
  </NTag>
  <span class={[styles.mappingAnchor, styles.sourceMappingAnchor]} />
</div>
```

- [ ] **Step 2: 收敛源字段区按钮**

第 2 步源字段区工具栏只保留：

```text
全选
反选
清空
只看异常
```

从源字段区工具栏移除：

```text
按名称映射
按顺序映射
清除映射
```

如果这些能力仍需保留，放到字段映射摘要或更多菜单中，不能占据源字段区主操作。

- [ ] **Step 3: 调整样式**

在 `index.module.scss` 中新增或调整：

```scss
.sourceFieldInline {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  position: relative;
  padding-right: 18px;
}
```

确保主键、非空、可空标签不再撑到第二行。

- [ ] **Step 4: 静态验证**

运行：

```bash
cd dolphinscheduler-ui
./node_modules/.bin/vue-tsc --noEmit
```

Expected: PASS。

---

### Task 2: 对齐目标字段设计区 V4 布局

**Files:**
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.tsx`
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.module.scss`

- [ ] **Step 1: 修改目标字段列**

将 `targetFieldColumns` 调整为以下顺序：

```text
目标字段
目标类型
字段注释
主键
可空
```

删除重复的 `字段名` 列。

- [ ] **Step 2: 目标字段改为输入框**

目标字段列直接渲染 `NInput`：

```tsx
<NInput
  value={row.targetColumn}
  placeholder='输入目标字段'
  onUpdateValue={(value) => handleTargetColumnNameChange(row.key, value)}
/>
```

不要在目标字段下方展示 `来自 xx`。

- [ ] **Step 3: 目标类型改为下拉框**

目标类型列使用现有 `targetTypeOptions`：

```tsx
<NSelect
  value={row.targetType}
  options={targetTypeOptions.value}
  placeholder='选择目标类型'
  filterable
  onUpdateValue={(value) => handleTargetTypeChange(row.key, value)}
/>
```

- [ ] **Step 4: 字段注释、主键、可空保持逐行可编辑**

字段注释使用 `NInput`，主键和可空使用 `NCheckbox`。正式页面必须能看到主键、可空复选框。

- [ ] **Step 5: 去除冗余文案**

确认页面不再出现：

```text
来自
当前来源
映射方式
字段名
```

其中 `字段名` 不应作为目标字段区单独表头出现。

- [ ] **Step 6: 静态验证**

运行：

```bash
cd dolphinscheduler-ui
./node_modules/.bin/vue-tsc --noEmit
```

Expected: PASS。

---

### Task 3: 实现目标字段名规则

**Files:**
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.tsx`

- [ ] **Step 1: 增加字段名规则状态**

增加状态：

```ts
targetNameRule: 'KEEP_SOURCE' | 'LOWERCASE' | 'UPPERCASE'
```

并为 `FieldDesignRow` 增加标记：

```ts
targetColumnTouched?: boolean
```

- [ ] **Step 2: 修改目标字段名变更函数**

用户手工编辑目标字段名时，将 `targetColumnTouched` 置为 `true`。

- [ ] **Step 3: 增加规则切换函数**

规则只作用于新建目标表模式，且只更新未手工编辑过的目标字段。

```ts
const applyTargetNameRule = (rule: TargetNameRule) => {
  state.targetNameRule = rule
  if (targetTableMode.value === 'EXISTING_TABLE') return
  state.fieldRows = state.fieldRows.map((row) => {
    if (row.targetColumnTouched) return row
    const sourceName = row.sourceColumn || row.targetColumn
    const nextName =
      rule === 'LOWERCASE' ? sourceName.toLowerCase() :
      rule === 'UPPERCASE' ? sourceName.toUpperCase() :
      sourceName
    return { ...row, targetColumn: nextName }
  })
}
```

- [ ] **Step 4: 在目标字段区顶部加入规则切换**

使用 `NRadioGroup` 或 `NButton` 组展示：

```text
保持源名
全小写
全大写
```

- [ ] **Step 5: 验证**

在页面手动执行 TC-P2-012。

---

### Task 4: 连线图例、虚线/实线和锚点统一

**Files:**
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.tsx`
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.module.scss`

- [ ] **Step 1: 为映射关系增加来源类型**

映射线需要区分：

```ts
type MappingKind = 'AUTO' | 'MANUAL'
```

自动按名称生成的映射为 `AUTO`，用户拖拽建立或改绑的映射为 `MANUAL`。

- [ ] **Step 2: 修改 `mappingLinePaths` 输出**

输出 line kind：

```ts
{
  key: string
  path: string
  kind: 'AUTO' | 'MANUAL'
}
```

- [ ] **Step 3: 自动映射虚线，手动映射实线**

SCSS：

```scss
.mappingPathAuto {
  fill: none;
  stroke: rgba(37, 99, 235, 0.42);
  stroke-width: 2;
  stroke-dasharray: 6 5;
}

.mappingPathManual {
  fill: none;
  stroke: rgba(37, 99, 235, 0.62);
  stroke-width: 2.5;
}
```

- [ ] **Step 4: 增加图例**

在第 2 步工作台顶部或中间区域增加：

```text
虚线：系统自动映射
实线：手动拖拽映射
```

- [ ] **Step 5: 统一左右锚点样式**

右侧目标锚点样式使用与左侧相同的 `.mappingAnchor` 样式。避免右侧点和左侧点尺寸、颜色、阴影不一致。

- [ ] **Step 6: 验证**

执行 TC-P2-010。

---

### Task 5: 完善双向拖拽和类型兼容校验

**Files:**
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.tsx`

- [ ] **Step 1: 补齐所有字段锚点**

确认 `sourceFieldRows` 和 `targetFieldRows` 中未映射行也渲染锚点，并能参与拖拽。

- [ ] **Step 2: 增加类型兼容函数**

实现保守判断：

```ts
const canMapFieldType = (sourceType: string, targetType: string) => {
  // 规范化大小写、去掉多余空格，解析 varchar/decimal 长度。
  // 允许 INT -> BIGINT、VARCHAR(n) -> VARCHAR(m 且 m>=n)、DATE -> TIMESTAMP。
  // 禁止 BIGINT -> INT、VARCHAR(n) -> VARCHAR(m 且 m<n)、TIMESTAMP -> DATE。
}
```

- [ ] **Step 3: 在 `handleMapSourceToTarget` 和 `handleMapTargetToSource` 前调用校验**

不兼容时：

```ts
window.$message.warning(`字段类型不兼容：${sourceType} 不能安全写入 ${targetType}`)
return
```

不能修改原映射。

- [ ] **Step 4: 手动拖拽设置为实线**

用户拖拽建立映射后，映射 kind 必须为 `MANUAL`。

- [ ] **Step 5: 验证**

执行 TC-P0-006、TC-P2-011。

---

### Task 6: DDL 与配置生成回归

**Files:**
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.tsx`

- [ ] **Step 1: 检查字段选择输出**

确认 DDL 和 SeaTunnel 配置只使用：

```ts
state.fieldRows.filter((row) => row.sync && row.mappedTargetKey)
```

或当前代码中等价的已选且有效映射字段。

- [ ] **Step 2: 检查目标字段修改生效**

确认目标字段名、目标类型、字段注释、主键、可空修改后，`preview-target-table` 请求体和 SeaTunnel 配置使用最新值。

- [ ] **Step 3: 静态验证**

运行：

```bash
cd dolphinscheduler-ui
./node_modules/.bin/vue-tsc --noEmit
```

Expected: PASS。

- [ ] **Step 4: 真实点击验证**

执行：

```text
TC-P1-004
TC-P1-005
```

Expected: DDL 和配置预览均使用最新字段映射和目标字段设计。

---

### Task 7: 完整真实点击测试

**Files:**
- Reference: `.ai/quality/e2e-click-testing/test-cases.md`

- [ ] **Step 1: 启动依赖**

确认 MySQL、PostgreSQL、DolphinScheduler 后端和前端可用。

- [ ] **Step 2: 进入页面**

访问：

```text
http://localhost:5173/sync-task
```

或当前 Dolphin 前端实际地址。

- [ ] **Step 3: 执行最小回归**

执行：

```text
TC-P0-001
TC-P0-002
TC-P0-003
TC-P0-004
TC-P0-005
TC-P0-006
TC-P1-004
TC-P1-005
TC-P2-007
TC-P2-009
TC-P2-010
TC-P2-011
TC-P2-012
TC-P2-013
```

- [ ] **Step 4: 记录结果**

在最终回复和必要的 change-log 中记录：

```text
测试日期：
测试入口：
测试数据：
通过用例：
失败用例：
阻塞用例：
结论：
```

---

## Self-Review

- Spec coverage:
  - V4 表格布局：Task 1、Task 2、TC-P2-009。
  - 目标字段输入、类型下拉、注释输入、主键/可空复选框：Task 2、TC-P2-009。
  - 去除“来自 xx / 当前来源 / 映射方式”：Task 2、TC-P2-013。
  - 大小写规则：Task 3、TC-P2-012。
  - 锚点、虚线/实线、图例：Task 4、TC-P2-010。
  - 双向拖拽、所有字段锚点、类型兼容：Task 5、TC-P0-006、TC-P2-011。
  - DDL 和配置生成回归：Task 6。
  - 真人点击验收：Task 7。
- Placeholder scan:
  - 无 `TBD`、`TODO`、`后续补充` 作为实施占位。
- Type consistency:
  - `TargetNameRule`、`MappingKind` 需要在 Task 3、Task 4 中实际定义后再使用。

---

## Execution Handoff

Plan complete and saved to `.ai/sync-task/plan/2026-05-14-field-mapping-v4-implementation-plan.md`.

推荐下一步使用 `superpowers:executing-plans` 按任务顺序执行。由于本轮改动集中在同步任务同一页面，暂不建议并行多 agent 同时改同一文件，避免 `index.tsx` 和 `index.module.scss` 冲突。
