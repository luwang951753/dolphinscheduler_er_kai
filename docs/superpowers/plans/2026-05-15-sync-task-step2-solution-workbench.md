# Sync Task Step 2 Solution Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild sync-task Step 2 as a “配置同步方案” workbench with V4 field mapping, optional source filters, optional SeaTunnel `custom_sql`, and a disabled data-processing placeholder.

**Architecture:** Keep Step 1 focused on source/target selection, move source filters into Step 2, and split Step 2 into four UI modules. Extract the V4 field-mapping behavior into normal Vue/TSX component logic instead of iframe embedding. Thread filters and `custom_sql` into SeaTunnel config generation while leaving data-processing as a non-functional placeholder.

**Tech Stack:** Vue 3 TSX, Naive UI, existing `dolphinscheduler-ui/src/views/sync-task` page, SeaTunnel HOCON config generation, Puppeteer click QA, ACP documents.

---

### Task 1: Lock Step 2 State Model

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.tsx`

- [ ] **Step 1: Verify current state shape**

Run:

```bash
rg -n "currentStep|sourceFilters|fieldRows|generatedConfig|SourceFilterRule|FieldDesignRow" /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.tsx
```

Expected: existing `sourceFilters`, `fieldRows`, and `generatedConfig` are present; source filters are currently rendered in Step 1.

- [ ] **Step 2: Add Step 2 module state**

Add the following type and state field near the existing wizard state definitions:

```ts
type SyncSolutionModule = 'MAPPING' | 'FILTER' | 'SINK' | 'PROCESSING'

const state = reactive({
  // existing fields stay unchanged
  activeSolutionModule: 'MAPPING' as SyncSolutionModule,
  sinkCustomSql: '',
  dataProcessingEnabled: false
})
```

Expected: Step 2 can track which module is selected without changing current step navigation.

- [ ] **Step 3: Run type check**

Run:

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui
./node_modules/.bin/vue-tsc --noEmit
```

Expected: PASS.

### Task 2: Make The Top Step Bar Lightweight

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.tsx`
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.module.scss`

- [ ] **Step 1: Confirm current heavy step card**

Run:

```bash
rg -n "执行步骤|stepsCard|stepRail|NSteps" /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.tsx /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.module.scss
```

Expected: the current page renders `Card title='执行步骤'`.

- [ ] **Step 2: Replace heavy card with lightweight top bar**

In `index.tsx`, replace the `Card title='执行步骤'` block with:

```tsx
<div class={styles.lightStepBar}>
  <NSteps current={this.state.currentStep} status='process' size='small'>
    {this.stepItems.map((item) => (
      <NStep key={item.index} title={item.title} />
    ))}
  </NSteps>
</div>
```

In `index.module.scss`, add:

```scss
.lightStepBar {
  padding: 10px 14px;
  border: 1px solid #e5ebf4;
  border-radius: 8px;
  background: #fff;
}
```

Expected: the step indicator stays visible but no longer consumes a large standalone card.

- [ ] **Step 3: Rename Step 2 label**

Update the Step 2 item text from `设计字段` to `配置同步方案`.

Expected: Step bar shows `1 选择源与目标 / 2 配置同步方案 / 3 执行与调度 / 4 预览与发布`.

- [ ] **Step 4: Run type check**

Run:

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui
./node_modules/.bin/vue-tsc --noEmit
```

Expected: PASS.

### Task 3: Move Source Filters Out Of Step 1

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.tsx`
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.module.scss`

- [ ] **Step 1: Remove Step 1 filter render block**

Move the JSX block with `class={styles.sourceFilterBlock}` out of the source endpoint card.

Expected: Step 1 only contains project/task, source endpoint, target endpoint, stats, and validation summary.

- [ ] **Step 2: Extract reusable filter renderer**

Create a render helper inside `setup()` return scope:

```tsx
const renderSourceFilterModule = () => (
  <div class={styles.solutionPanel}>
    <div class={styles.solutionPanelHeader}>
      <div>
        <div class={styles.sectionTitle}>源端过滤条件</div>
        <div class={styles.hintText}>
          可选配置。结构化条件会写入 source query，不配置时表示全量读取。
        </div>
      </div>
      <NTag bordered={false} type='info'>
        已启用 {activeSourceFilterCount.value} / {state.sourceFilters.length}
      </NTag>
    </div>
    {/* move the existing sourceFilterList and footer JSX here */}
  </div>
)
```

Expected: filter UI is callable from Step 2 module rendering.

- [ ] **Step 3: Run browser visual check**

Open:

```text
http://localhost:5173/sync-task
```

Expected: Step 1 does not show “源端过滤条件”.

### Task 4: Build Step 2 Module Layout

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.tsx`
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.module.scss`

- [ ] **Step 1: Add module list renderer**

Add:

```tsx
const solutionModules = [
  { key: 'MAPPING', title: '字段映射关系', tag: '核心步骤', desc: '配置源字段、目标字段和映射连线' },
  { key: 'FILTER', title: '源端过滤条件', tag: '可配置', desc: '按字段限制源端读取范围' },
  { key: 'SINK', title: '数据去向', tag: '可配置', desc: '配置同步前 SQL（custom_sql）' },
  { key: 'PROCESSING', title: '数据处理', tag: '暂未实现', desc: '预留字符串替换、AI 处理、向量化能力' }
] as const
```

Expected: module order matches PRD exactly.

- [ ] **Step 2: Render left module list**

Add JSX:

```tsx
<div class={styles.solutionWorkbench}>
  <aside class={styles.solutionSidebar}>
    {solutionModules.map((item) => (
      <button
        class={[
          styles.solutionModule,
          this.state.activeSolutionModule === item.key ? styles.solutionModuleActive : ''
        ]}
        onClick={() => {
          this.state.activeSolutionModule = item.key
        }}
      >
        <div class={styles.solutionModuleTitle}>{item.title}</div>
        <NTag size='small' bordered={false}>{item.tag}</NTag>
        <div class={styles.solutionModuleDesc}>{item.desc}</div>
      </button>
    ))}
  </aside>
  <section class={styles.solutionContent}>{this.renderSolutionModule()}</section>
</div>
```

Expected: left list contains names only, no `1 映射 / 2 过滤 / 3 去向 / 4 处理` badges.

- [ ] **Step 3: Add layout styles**

Add:

```scss
.solutionWorkbench {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 12px;
}

.solutionSidebar,
.solutionContent {
  border: 1px solid #e5ebf4;
  border-radius: 8px;
  background: #fff;
}

.solutionSidebar {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.solutionModule {
  width: 100%;
  padding: 10px;
  border: 1px solid #e5ebf4;
  border-radius: 8px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.solutionModuleActive {
  border-color: #9bb8ff;
  background: #f4f7ff;
}

.solutionContent {
  padding: 12px;
  min-width: 0;
}
```

Expected: module layout matches prototype.

### Task 5: Componentize V4 Field Mapping Without iframe

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.tsx`
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.module.scss`

- [ ] **Step 1: Confirm no iframe is introduced**

Run:

```bash
rg -n "iframe|field-mapping-workbench-v4.html" /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task
```

Expected before implementation: no iframe. Expected after implementation: still no iframe.

- [ ] **Step 2: Reuse existing V4-like field tables**

Move the current field mapping JSX into `renderFieldMappingModule()` and keep:

```tsx
<div class={styles.lineLegend}>
  <span><i class={styles.autoLine}></i>虚线：系统自动映射</span>
  <span><i class={styles.manualLine}></i>实线：手动拖拽映射</span>
</div>
```

Expected: the field mapping module starts directly at the V4 workbench body and does not include a separate V4 step card or summary card.

- [ ] **Step 3: Keep V4 invariants**

Check the rendered JSX preserves:

```text
源字段区列顺序：同步、源字段、源类型、字段注释
目标字段区列顺序：目标字段、目标类型、字段注释、主键、可空
源字段工具：全选、反选、清空、只看异常
目标字段规则：保持源名、全小写、全大写
自动映射：虚线
手动映射：实线
```

Expected: no “来自 xx / 当前来源 / 映射方式” text.

- [ ] **Step 4: Preserve drag-line recalculation**

Keep the existing line rendering based on real row DOM positions. Verify scroll listeners still call line recalculation after the field mapping module is mounted.

Expected: scrolling source or target table keeps lines aligned.

### Task 6: Add Data Sink custom_sql Module

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.tsx`

- [ ] **Step 1: Render data sink module**

Add:

```tsx
const renderSinkModule = () => (
  <div class={styles.solutionPanel}>
    <div class={styles.solutionPanelHeader}>
      <div>
        <div class={styles.sectionTitle}>数据去向</div>
        <div class={styles.hintText}>
          可选配置。SeaTunnel JDBC sink 使用 custom_sql 作为同步前 SQL。
        </div>
      </div>
      <NTag bordered={false} type='warning'>可配置</NTag>
    </div>
    <NInput
      type='textarea'
      value={state.sinkCustomSql}
      placeholder='例如：truncate table ajxx_tab_sync;'
      onUpdateValue={(value) => {
        state.sinkCustomSql = value
      }}
    />
  </div>
)
```

Expected: module contains only `同步前 SQL（custom_sql）`; no “写入策略” control.

- [ ] **Step 2: Thread custom_sql into SeaTunnel config**

In `generatedConfig`, after sink `table` line and before primary keys, insert:

```ts
const customSql = state.sinkCustomSql.trim()
if (customSql) {
  lines.push('    data_save_mode = "CUSTOM_PROCESSING"')
  lines.push(`    custom_sql = "${escapeSeatunnelString(customSql)}"`)
}
```

Add helper:

```ts
const escapeSeatunnelString = (value: string): string =>
  value.replaceAll('\\', '\\\\').replaceAll('"', '\\"')
```

Expected: config preview shows `data_save_mode` and `custom_sql` only when user entered SQL.

- [ ] **Step 3: Guard query/custom_sql conflict**

If the generated sink config uses `query = "insert into ..."` and `custom_sql` is non-empty, add an inline warning:

```tsx
{state.sinkCustomSql.trim() && sinkInsertQuery ? (
  <NAlert type='warning' showIcon={false}>
    SeaTunnel JDBC sink 在 query 模式下 custom_sql 可能不会执行，请确认执行链路。
  </NAlert>
) : null}
```

Expected: user is warned instead of silently generating misleading config.

### Task 7: Add Data Processing Placeholder

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/sync-task/index.tsx`

- [ ] **Step 1: Render placeholder**

Add:

```tsx
const renderProcessingModule = () => (
  <div class={styles.solutionPanel}>
    <NAlert type='info' showIcon={false}>
      数据处理暂未实现。本期只保留入口，后续再扩展字符串替换、AI 辅助处理、数据向量化。
    </NAlert>
  </div>
)
```

Expected: no editable inputs; no saved state; config generation ignores this module.

### Task 8: Update Tests And Click QA

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/tmp_sync_task_v4_qa.js`
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/tmp_sync_task_mapping_e2e.js`
- Create: `/Users/luwang/bigdata-build/dolphinscheduler/tmp_sync_task_step2_solution_qa.js`

- [ ] **Step 1: Add no-iframe assertion**

In the new QA script, assert:

```js
const iframeCount = await page.locator('/sync-task iframe').count()
if (iframeCount !== 0) throw new Error('字段映射真实页面不能使用 iframe')
```

Expected: PASS.

- [ ] **Step 2: Add module order assertion**

Assert left module list text order:

```text
字段映射关系
源端过滤条件
数据去向
数据处理
```

Expected: PASS and no `1映射 / 2过滤 / 3去向 / 4处理`.

- [ ] **Step 3: Add source filter location assertion**

Assert Step 1 does not contain `源端过滤条件`; Step 2 filter module does.

Expected: PASS.

- [ ] **Step 4: Add custom_sql assertion**

Enter:

```sql
truncate table ajxx_tab_sync;
```

Open config preview and assert it contains:

```text
data_save_mode = "CUSTOM_PROCESSING"
custom_sql = "truncate table ajxx_tab_sync;"
```

Expected: PASS.

- [ ] **Step 5: Run QA**

Run:

```bash
node /Users/luwang/bigdata-build/dolphinscheduler/tmp_sync_task_step2_solution_qa.js
```

Expected: exit code 0.

### Task 9: Final Verification

**Files:**
- Test: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui`
- Test: `/Users/luwang/bigdata-build/dolphinscheduler/tmp_sync_task_step2_solution_qa.js`

- [ ] **Step 1: Run type check**

Run:

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui
./node_modules/.bin/vue-tsc --noEmit
```

Expected: exit code 0.

- [ ] **Step 2: Run existing V4 mapping QA**

Run:

```bash
node /Users/luwang/bigdata-build/dolphinscheduler/tmp_sync_task_v4_qa.js
```

Expected: exit code 0.

- [ ] **Step 3: Run new Step 2 solution QA**

Run:

```bash
node /Users/luwang/bigdata-build/dolphinscheduler/tmp_sync_task_step2_solution_qa.js
```

Expected: exit code 0.

- [ ] **Step 4: Manual browser check**

Open:

```text
http://localhost:5173/sync-task
```

Expected:

```text
顶部步骤条轻量展示
第 1 步不出现源端过滤条件
第 2 步默认选中字段映射关系
字段映射区源字段和目标字段可见，连线可拖拽
源端过滤条件模块可添加条件
数据去向只有 custom_sql
数据处理显示暂未实现
```

---

### Self-review

- [ ] PRD coverage: Step 2 module order, V4 mapping, filters, custom_sql, processing placeholder are covered.
- [ ] No iframe: implementation tasks explicitly forbid iframe and test it.
- [ ] SeaTunnel correctness: `custom_sql` only appears when configured and uses SeaTunnel naming, not `preSql`.
- [ ] Test coverage: new QA covers module order, source filter location, no iframe, and custom_sql config.
