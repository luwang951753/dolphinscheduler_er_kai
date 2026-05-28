# Theme Library Dashboard Composition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a lightweight grid-based dashboard composition capability inside the Theme Library, so each analysis scenario can organize multiple SQL data blocks without becoming a general big-screen designer.

**Architecture:** Implement the feature in two phases. First update the standalone HTML prototype so the product interaction can be reviewed quickly. Then port the same data model and interaction model into the DolphinScheduler Vue theme-library page using local mock state and existing Naive UI components. Keep the scope constrained to scenario-level SQL blocks, grid widths, display types, sorting, visibility, and edit/view mode.

**Tech Stack:** Static HTML/CSS/JS prototype, Vue 3 TSX, Naive UI, SCSS modules, existing DolphinScheduler data-source service.

---

## Scope Summary

This plan implements the confirmed PRD:

- Lightweight grid composition, not a free canvas.
- Scenario-level dashboard configuration.
- Multiple SQL data blocks per scenario.
- Existing dataset mode plus advanced SQL mode in the UI model; first implementation may use mock dataset options and existing datasource list.
- Display types: metric, trend, bar, pie, rank, table, text, alert.
- Edit mode supports add, edit, copy, delete, show/hide, sort, width, display type.
- No publish/share, no blueprint, no layer panel, no generic component marketplace.

## File Structure

Prototype phase:

- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/theme-library/prototype/theme-library-sql-blocks-prototype.html`
  - Add dashboard edit mode, grid width controls, block editor modal, block renderers, save/cancel behavior.

Prototype QA:

- Create: `/tmp/theme_library_dashboard_composition_proto_qa.py`
  - Browser automation for prototype interactions.

DolphinScheduler phase:

- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/theme-library/index.tsx`
  - Replace single table result area with scenario dashboard configuration and block renderers.
  - Add block editor modal and edit/view mode state.
  - Keep datasource loading through `queryDataSourceList`.

- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/theme-library/index.module.scss`
  - Add grid dashboard, block cards, editor toolbar, display preview, modal layout, and responsive styles.

DolphinScheduler QA:

- Create: `/tmp/theme_library_dashboard_composition_vue_qa.py`
  - Browser automation for the running Dolphin UI route, if the dev server is available.

---

### Task 1: Extend the Prototype Data Model

**Files:**
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/theme-library/prototype/theme-library-sql-blocks-prototype.html`

- [ ] **Step 1: Add dashboard block model comments near the existing script data**

Add a short comment before the prototype data definitions:

```js
// Dashboard composition model:
// scenario.dashboard.blocks[] stores SQL data blocks for the current analysis scenario.
// This is intentionally a lightweight grid model: width, sortNo, visible, displayType.
// It is not a free-canvas designer model.
```

- [ ] **Step 2: Add block factories**

Add these functions inside the existing `<script>` section, before initial render functions:

```js
function createBlock(overrides) {
  const seed = overrides || {}
  return {
    id: seed.id || `block-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
    title: seed.title || '新增 SQL 数据板块',
    description: seed.description || '请配置数据来源、字段映射和展示方式。',
    displayType: seed.displayType || 'metric',
    width: seed.width || '1/2',
    visible: seed.visible !== false,
    sortNo: Number(seed.sortNo || 10),
    dataMode: seed.dataMode || 'dataset',
    datasetId: seed.datasetId || 'alarm-receive-summary',
    datasourceId: seed.datasourceId || 'oracle-110-core',
    sqlText: seed.sqlText || 'select label, value from ads_theme_metric where stat_date = ${endDate}',
    fieldMapping: seed.fieldMapping || {
      label: 'label',
      value: 'value',
      category: 'category',
      time: 'stat_date',
      unit: 'unit'
    },
    previewRows: seed.previewRows || [
      { label: '今日接报', value: 1286, unit: '起', category: '接警', stat_date: '05-22' },
      { label: '较昨日', value: 8.4, unit: '%', category: '趋势', stat_date: '05-22' },
      { label: '待核查异常', value: 17, unit: '起', category: '质量', stat_date: '05-22' }
    ],
    remark: seed.remark || '数据来自当前主题库场景配置。'
  }
}

function defaultBlocksForScenario(scenarioTitle) {
  return [
    createBlock({
      id: `${scenarioTitle}-metric`,
      title: '核心指标',
      displayType: 'metric',
      width: '1/3',
      sortNo: 10
    }),
    createBlock({
      id: `${scenarioTitle}-trend`,
      title: '趋势变化',
      displayType: 'trend',
      width: '2/3',
      sortNo: 20,
      previewRows: [
        { label: '周一', value: 1086, stat_date: '周一' },
        { label: '周二', value: 1132, stat_date: '周二' },
        { label: '周三', value: 1198, stat_date: '周三' },
        { label: '周四', value: 1210, stat_date: '周四' },
        { label: '周五', value: 1286, stat_date: '周五' }
      ]
    }),
    createBlock({
      id: `${scenarioTitle}-rank`,
      title: '责任单位排行',
      displayType: 'rank',
      width: '1/2',
      sortNo: 30,
      previewRows: [
        { label: '西城派出所', value: 218 },
        { label: '东城派出所', value: 196 },
        { label: '南城派出所', value: 173 },
        { label: '交警一中队', value: 139 }
      ]
    }),
    createBlock({
      id: `${scenarioTitle}-table`,
      title: '明细数据',
      displayType: 'table',
      width: 'full',
      sortNo: 40
    })
  ]
}
```

- [ ] **Step 3: Ensure every scenario has dashboard blocks**

Where scenarios are created or normalized, add:

```js
function ensureScenarioDashboard(scenario) {
  if (!scenario.dashboard) {
    scenario.dashboard = {
      editing: false,
      blocks: defaultBlocksForScenario(scenario.title)
    }
  }
  if (!Array.isArray(scenario.dashboard.blocks)) {
    scenario.dashboard.blocks = defaultBlocksForScenario(scenario.title)
  }
  scenario.dashboard.blocks.sort((a, b) => a.sortNo - b.sortNo)
  return scenario.dashboard
}
```

- [ ] **Step 4: Call `ensureScenarioDashboard(activeScenario)` before rendering dashboard content**

In the function that renders selected scenario content, add:

```js
const dashboard = ensureScenarioDashboard(activeScenario)
```

- [ ] **Step 5: Manually open the prototype**

Run:

```bash
cd /Users/luwang/bigdata-build/data-preview-design-repo/.ai/theme-library/prototype
python3 -m http.server 8033
```

Expected:

```text
Serving HTTP on :: port 8033
```

Open:

```text
http://127.0.0.1:8033/theme-library-sql-blocks-prototype.html
```

Expected: page still loads without console errors.

---

### Task 2: Build Prototype Dashboard View Mode

**Files:**
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/theme-library/prototype/theme-library-sql-blocks-prototype.html`

- [ ] **Step 1: Add dashboard toolbar HTML in scenario panel render**

In the scenario content render function, above the SQL block grid, render:

```js
function renderDashboardToolbar(scenario, dashboard) {
  return `
    <div class="dashboardToolbar">
      <div>
        <strong>场景看板</strong>
        <span class="muted">${dashboard.blocks.filter((block) => block.visible).length} 个可见板块 / ${dashboard.blocks.length} 个配置板块</span>
      </div>
      <div class="blockActions">
        <button class="btn btnPrimary" type="button" onclick="enterDashboardEdit()">编辑看板</button>
        <button class="btn" type="button" onclick="openBlockEditor()">新增 SQL 板块</button>
      </div>
    </div>
  `
}
```

- [ ] **Step 2: Add grid renderer**

Add:

```js
function renderDashboardGrid(scenario) {
  const dashboard = ensureScenarioDashboard(scenario)
  const visibleBlocks = dashboard.blocks.filter((block) => block.visible).sort((a, b) => a.sortNo - b.sortNo)
  if (!visibleBlocks.length) {
    return '<div class="emptyBoard">当前场景没有可见 SQL 数据板块。</div>'
  }
  return `<div class="dashboardGrid">${visibleBlocks.map(renderDashboardBlock).join('')}</div>`
}
```

- [ ] **Step 3: Add width class helper**

Add:

```js
function blockWidthClass(width) {
  return {
    '1/3': 'widthThird',
    '1/2': 'widthHalf',
    '2/3': 'widthTwoThirds',
    full: 'widthFull'
  }[width] || 'widthHalf'
}
```

- [ ] **Step 4: Add block renderer**

Add:

```js
function renderDashboardBlock(block) {
  return `
    <article class="dashboardBlock ${blockWidthClass(block.width)}">
      <div class="blockHead">
        <div>
          <h3>${escapeHtml(block.title)}</h3>
          <span class="muted">${escapeHtml(displayTypeName(block.displayType))} / ${escapeHtml(block.description)}</span>
        </div>
        <span class="pill">${escapeHtml(block.width)}</span>
      </div>
      ${renderBlockPreview(block)}
      <div class="blockRemark">${escapeHtml(block.remark || '')}</div>
    </article>
  `
}
```

- [ ] **Step 5: Add display type name helper**

Add:

```js
function displayTypeName(type) {
  return {
    metric: '指标卡',
    trend: '趋势图',
    bar: '柱状图',
    pie: '饼图',
    rank: '排行榜',
    table: '明细表',
    text: '文字说明',
    alert: '提醒卡片'
  }[type] || type
}
```

- [ ] **Step 6: Add block preview renderer**

Add:

```js
function renderBlockPreview(block) {
  if (block.displayType === 'metric') return renderMetricBlock(block)
  if (block.displayType === 'trend' || block.displayType === 'bar') return renderBarLikeBlock(block)
  if (block.displayType === 'pie') return renderPieBlock(block)
  if (block.displayType === 'rank') return renderRankBlock(block)
  if (block.displayType === 'table') return renderTableBlock(block)
  if (block.displayType === 'alert') return renderAlertBlock(block)
  return renderTextBlock(block)
}
```

- [ ] **Step 7: Add preview implementations**

Add:

```js
function renderMetricBlock(block) {
  return `<div class="metricRow">${block.previewRows.slice(0, 4).map((row) => `
    <div class="metric"><strong>${escapeHtml(row.value)}${escapeHtml(row.unit || '')}</strong><span>${escapeHtml(row.label)}</span></div>
  `).join('')}</div>`
}

function renderBarLikeBlock(block) {
  const max = Math.max(...block.previewRows.map((row) => Number(row.value) || 0), 1)
  return `<div class="chart">${block.previewRows.slice(0, 7).map((row) => `
    <span class="bar"><i style="height:${Math.max(18, (Number(row.value) || 0) / max * 100)}%"></i><em>${escapeHtml(row.label || row.stat_date)}</em></span>
  `).join('')}</div>`
}

function renderRankBlock(block) {
  return `<ol class="rankList">${block.previewRows.slice(0, 6).map((row, index) => `
    <li><b>${index + 1}</b><span>${escapeHtml(row.label)}</span><strong>${escapeHtml(row.value)}</strong></li>
  `).join('')}</ol>`
}

function renderPieBlock(block) {
  return `<div class="piePanel"><div class="pieChart"></div><ul class="legend">${block.previewRows.slice(0, 4).map((row) => `
    <li><i></i><span>${escapeHtml(row.label)}</span><strong>${escapeHtml(row.value)}</strong></li>
  `).join('')}</ul></div>`
}

function renderTableBlock(block) {
  return `<div class="tableWrap"><table><thead><tr><th>名称</th><th>数值</th><th>分类</th><th>日期</th></tr></thead><tbody>${block.previewRows.map((row) => `
    <tr><td>${escapeHtml(row.label)}</td><td>${escapeHtml(row.value)}${escapeHtml(row.unit || '')}</td><td>${escapeHtml(row.category || '-')}</td><td>${escapeHtml(row.stat_date || '-')}</td></tr>
  `).join('')}</tbody></table></div>`
}

function renderTextBlock(block) {
  return `<div class="textCard"><p>${escapeHtml(block.description)}</p><p>${escapeHtml(block.remark)}</p></div>`
}

function renderAlertBlock(block) {
  return `<div class="alertCard"><ul class="alertList">${block.previewRows.map((row) => `<li><span>${escapeHtml(row.label)}</span><strong>${escapeHtml(row.value)}${escapeHtml(row.unit || '')}</strong></li>`).join('')}</ul></div>`
}
```

- [ ] **Step 8: Add CSS for grid widths**

Add to the prototype `<style>`:

```css
.dashboardToolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #d8dde6;
  border-radius: 8px;
  background: #fff;
}
.dashboardToolbar strong { display: block; margin-bottom: 4px; color: #101828; }
.dashboardGrid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}
.dashboardBlock {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #d8dde6;
  border-radius: 8px;
  background: #fff;
}
.widthThird { grid-column: span 2; }
.widthHalf { grid-column: span 3; }
.widthTwoThirds { grid-column: span 4; }
.widthFull { grid-column: 1 / -1; }
.blockRemark {
  color: #667085;
  font-size: 12px;
}
.emptyBoard {
  padding: 24px;
  border: 1px dashed #c8d7ee;
  border-radius: 8px;
  background: #fbfdff;
  color: #667085;
  text-align: center;
}
```

- [ ] **Step 9: Verify view mode**

Open:

```text
http://127.0.0.1:8033/theme-library-sql-blocks-prototype.html
```

Expected:

- Scenario panel shows “场景看板”.
- Multiple blocks render in a grid.
- Full-width table block spans the full row.
- No “画布 / 图层 / 蓝图 / 发布” text appears.

---

### Task 3: Build Prototype Edit Mode

**Files:**
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/theme-library/prototype/theme-library-sql-blocks-prototype.html`

- [ ] **Step 1: Add edit mode state**

Add:

```js
let dashboardEditing = false
let editingBlockId = ''
```

- [ ] **Step 2: Add edit mode actions**

Add:

```js
function enterDashboardEdit() {
  dashboardEditing = true
  render()
}

function cancelDashboardEdit() {
  dashboardEditing = false
  render()
}

function saveDashboardEdit() {
  dashboardEditing = false
  render()
  showStatus('看板配置已保存。', 'success')
}
```

- [ ] **Step 3: Render edit toolbar when editing**

Update `renderDashboardToolbar`:

```js
function renderDashboardToolbar(scenario, dashboard) {
  if (dashboardEditing) {
    return `
      <div class="dashboardToolbar editing">
        <div>
          <strong>编辑场景看板</strong>
          <span class="muted">可调整顺序、宽度、显隐和展示方式。</span>
        </div>
        <div class="blockActions">
          <button class="btn" type="button" onclick="openBlockEditor()">新增 SQL 板块</button>
          <button class="btn" type="button" onclick="cancelDashboardEdit()">取消编辑</button>
          <button class="btn btnPrimary" type="button" onclick="saveDashboardEdit()">保存配置</button>
        </div>
      </div>
    `
  }
  return `
    <div class="dashboardToolbar">
      <div>
        <strong>场景看板</strong>
        <span class="muted">${dashboard.blocks.filter((block) => block.visible).length} 个可见板块 / ${dashboard.blocks.length} 个配置板块</span>
      </div>
      <div class="blockActions">
        <button class="btn btnPrimary" type="button" onclick="enterDashboardEdit()">编辑看板</button>
        <button class="btn" type="button" onclick="openBlockEditor()">新增 SQL 板块</button>
      </div>
    </div>
  `
}
```

- [ ] **Step 4: Render hidden blocks in edit mode**

Update `renderDashboardGrid`:

```js
const blocks = dashboard.blocks
  .filter((block) => dashboardEditing || block.visible)
  .sort((a, b) => a.sortNo - b.sortNo)
```

- [ ] **Step 5: Render block edit controls**

Inside `renderDashboardBlock`, below `blockHead`, add:

```js
${dashboardEditing ? renderBlockEditControls(block) : ''}
```

Add:

```js
function renderBlockEditControls(block) {
  return `
    <div class="blockEditControls">
      <button class="btn" type="button" onclick="moveBlock('${block.id}', -1)">上移</button>
      <button class="btn" type="button" onclick="moveBlock('${block.id}', 1)">下移</button>
      <select class="field" onchange="updateBlockWidth('${block.id}', this.value)">
        <option value="1/3" ${block.width === '1/3' ? 'selected' : ''}>1/3</option>
        <option value="1/2" ${block.width === '1/2' ? 'selected' : ''}>1/2</option>
        <option value="2/3" ${block.width === '2/3' ? 'selected' : ''}>2/3</option>
        <option value="full" ${block.width === 'full' ? 'selected' : ''}>整行</option>
      </select>
      <select class="field" onchange="updateBlockDisplay('${block.id}', this.value)">
        ${['metric', 'trend', 'bar', 'pie', 'rank', 'table', 'text', 'alert'].map((type) => `<option value="${type}" ${block.displayType === type ? 'selected' : ''}>${displayTypeName(type)}</option>`).join('')}
      </select>
      <button class="btn" type="button" onclick="toggleBlockVisible('${block.id}')">${block.visible ? '隐藏' : '显示'}</button>
      <button class="btn" type="button" onclick="openBlockEditor('${block.id}')">编辑</button>
      <button class="btn" type="button" onclick="copyBlock('${block.id}')">复制</button>
      <button class="btn danger" type="button" onclick="deleteBlock('${block.id}')">删除</button>
    </div>
  `
}
```

- [ ] **Step 6: Add block mutation helpers**

Add:

```js
function activeDashboard() {
  return ensureScenarioDashboard(activeScenario)
}

function findBlock(blockId) {
  return activeDashboard().blocks.find((block) => block.id === blockId)
}

function moveBlock(blockId, direction) {
  const dashboard = activeDashboard()
  const blocks = dashboard.blocks.sort((a, b) => a.sortNo - b.sortNo)
  const index = blocks.findIndex((block) => block.id === blockId)
  const nextIndex = index + direction
  if (index < 0 || nextIndex < 0 || nextIndex >= blocks.length) return
  const currentSort = blocks[index].sortNo
  blocks[index].sortNo = blocks[nextIndex].sortNo
  blocks[nextIndex].sortNo = currentSort
  render()
}

function updateBlockWidth(blockId, width) {
  const block = findBlock(blockId)
  if (!block) return
  block.width = width
  render()
}

function updateBlockDisplay(blockId, displayType) {
  const block = findBlock(blockId)
  if (!block) return
  block.displayType = displayType
  render()
}

function toggleBlockVisible(blockId) {
  const block = findBlock(blockId)
  if (!block) return
  block.visible = !block.visible
  render()
}

function copyBlock(blockId) {
  const dashboard = activeDashboard()
  const block = findBlock(blockId)
  if (!block) return
  dashboard.blocks.push(createBlock({
    ...block,
    id: `block-copy-${Date.now()}`,
    title: `${block.title} 副本`,
    sortNo: Math.max(...dashboard.blocks.map((item) => item.sortNo), 0) + 10
  }))
  render()
}

function deleteBlock(blockId) {
  if (!confirm('确认删除该 SQL 数据板块吗？')) return
  const dashboard = activeDashboard()
  dashboard.blocks = dashboard.blocks.filter((block) => block.id !== blockId)
  render()
}
```

- [ ] **Step 7: Add edit control CSS**

Add:

```css
.dashboardToolbar.editing {
  border-color: #8db4ee;
  background: #eef6ff;
}
.dashboardBlock.hiddenBlock {
  opacity: .48;
}
.blockEditControls {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px;
  border: 1px dashed #c8d7ee;
  border-radius: 8px;
  background: #f8fafc;
}
.btn.danger {
  border-color: #fecdca;
  color: #b42318;
}
```

- [ ] **Step 8: Verify edit mode**

Manual checks:

- Click “编辑看板”.
- Hidden/edit controls appear.
- Change width to “整行”; block spans whole row.
- Change display type; preview changes.
- Hide a block; it is dimmed in edit mode and absent after save.
- Copy creates a duplicate block.
- Delete removes a block after confirmation.

---

### Task 4: Build Prototype Block Editor Modal

**Files:**
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/theme-library/prototype/theme-library-sql-blocks-prototype.html`

- [ ] **Step 1: Reuse or add a block editor modal**

Add modal HTML near the existing SQL modal:

```html
<div class="modalBackdrop hidden" id="blockModal">
  <div class="modal">
    <div class="modalHead">
      <div>
        <h2 id="blockModalTitle">编辑 SQL 数据板块</h2>
        <div class="muted">配置基础信息、数据来源、字段映射和预览效果。</div>
      </div>
      <button class="btn" type="button" onclick="closeBlockEditor()">关闭</button>
    </div>
    <div class="modalBody">
      <div class="formGrid">
        <label>板块名称<input class="field" id="blockTitleInput" /></label>
        <label>展示方式<select class="field" id="blockDisplayInput"></select></label>
        <label>宽度<select class="field" id="blockWidthInput"></select></label>
        <label>数据模式<select class="field" id="blockDataModeInput"><option value="dataset">选择已有数据集</option><option value="sql">高级模式：手写 SQL</option></select></label>
        <label class="span2">板块说明<input class="field" id="blockDescInput" /></label>
        <label>数据集<select class="field" id="blockDatasetInput"><option value="alarm-receive-summary">接报警情汇总数据集</option><option value="alarm-repeat-rank">重复报警排行数据集</option></select></label>
        <label>数据源<select class="field" id="blockDatasourceInput"><option value="oracle-110-core">Oracle / 110_alarm_core</option><option value="mysql-police-warehouse">MySQL / police_warehouse_prod</option></select></label>
        <label class="span2">SQL<textarea id="blockSqlInput"></textarea></label>
        <label class="span2">字段映射 JSON<textarea id="blockMappingInput"></textarea></label>
        <label class="span2">口径备注<input class="field" id="blockRemarkInput" /></label>
      </div>
      <div class="statusLine" id="blockPreviewStatus">未试运行</div>
      <div id="blockPreviewArea"></div>
    </div>
    <div class="modalFoot">
      <button class="btn" type="button" onclick="previewBlockSql()">试运行</button>
      <button class="btn btnPrimary" type="button" onclick="saveBlockEditor()">保存板块</button>
    </div>
  </div>
</div>
```

- [ ] **Step 2: Add modal option initialization**

Add:

```js
function fillBlockEditorOptions() {
  document.getElementById('blockDisplayInput').innerHTML = ['metric', 'trend', 'bar', 'pie', 'rank', 'table', 'text', 'alert']
    .map((type) => `<option value="${type}">${displayTypeName(type)}</option>`)
    .join('')
  document.getElementById('blockWidthInput').innerHTML = [
    ['1/3', '1/3'],
    ['1/2', '1/2'],
    ['2/3', '2/3'],
    ['full', '整行']
  ].map(([value, label]) => `<option value="${value}">${label}</option>`).join('')
}
```

- [ ] **Step 3: Add open/close editor functions**

Add:

```js
function openBlockEditor(blockId) {
  fillBlockEditorOptions()
  const block = blockId ? findBlock(blockId) : createBlock({ sortNo: Math.max(...activeDashboard().blocks.map((item) => item.sortNo), 0) + 10 })
  editingBlockId = blockId || ''
  document.getElementById('blockTitleInput').value = block.title
  document.getElementById('blockDisplayInput').value = block.displayType
  document.getElementById('blockWidthInput').value = block.width
  document.getElementById('blockDataModeInput').value = block.dataMode
  document.getElementById('blockDescInput').value = block.description
  document.getElementById('blockDatasetInput').value = block.datasetId
  document.getElementById('blockDatasourceInput').value = block.datasourceId
  document.getElementById('blockSqlInput').value = block.sqlText
  document.getElementById('blockMappingInput').value = JSON.stringify(block.fieldMapping, null, 2)
  document.getElementById('blockRemarkInput').value = block.remark
  document.getElementById('blockPreviewArea').innerHTML = renderBlockPreview(block)
  document.getElementById('blockModal').classList.remove('hidden')
}

function closeBlockEditor() {
  document.getElementById('blockModal').classList.add('hidden')
}
```

- [ ] **Step 4: Add block form reader**

Add:

```js
function readBlockEditor() {
  let mapping = {}
  try {
    mapping = JSON.parse(document.getElementById('blockMappingInput').value || '{}')
  } catch {
    mapping = { label: 'label', value: 'value' }
  }
  return createBlock({
    id: editingBlockId || undefined,
    title: document.getElementById('blockTitleInput').value,
    displayType: document.getElementById('blockDisplayInput').value,
    width: document.getElementById('blockWidthInput').value,
    dataMode: document.getElementById('blockDataModeInput').value,
    description: document.getElementById('blockDescInput').value,
    datasetId: document.getElementById('blockDatasetInput').value,
    datasourceId: document.getElementById('blockDatasourceInput').value,
    sqlText: document.getElementById('blockSqlInput').value,
    fieldMapping: mapping,
    remark: document.getElementById('blockRemarkInput').value
  })
}
```

- [ ] **Step 5: Add preview and save functions**

Add:

```js
function previewBlockSql() {
  const block = readBlockEditor()
  block.previewRows = buildPreviewRowsFromSql(block.sqlText, block.displayType)
  document.getElementById('blockPreviewArea').innerHTML = renderBlockPreview(block)
  document.getElementById('blockPreviewStatus').textContent = `试运行成功，返回 ${block.previewRows.length} 行模拟数据。`
  document.getElementById('blockPreviewStatus').className = 'statusLine success'
}

function saveBlockEditor() {
  const dashboard = activeDashboard()
  const next = readBlockEditor()
  const existing = editingBlockId ? dashboard.blocks.find((block) => block.id === editingBlockId) : null
  if (existing) Object.assign(existing, next, { id: existing.id })
  else dashboard.blocks.push(next)
  closeBlockEditor()
  dashboardEditing = true
  render()
}
```

- [ ] **Step 6: Add SQL preview rows helper**

Add:

```js
function buildPreviewRowsFromSql(sql, displayType) {
  if (displayType === 'rank') {
    return [
      { label: '西城派出所', value: 218 },
      { label: '东城派出所', value: 196 },
      { label: '南城派出所', value: 173 }
    ]
  }
  if (displayType === 'trend' || displayType === 'bar') {
    return [
      { label: '周一', value: 1086, stat_date: '周一' },
      { label: '周二', value: 1132, stat_date: '周二' },
      { label: '周三', value: 1198, stat_date: '周三' },
      { label: '今日', value: 1286, stat_date: '今日' }
    ]
  }
  return [
    { label: '查询结果', value: 1286, unit: '条', category: '模拟预览', stat_date: '今日' },
    { label: '异常记录', value: 17, unit: '条', category: '模拟预览', stat_date: '今日' }
  ]
}
```

- [ ] **Step 7: Verify block editor**

Manual checks:

- Click “新增 SQL 板块”.
- Modal opens with four configuration areas.
- Change display type to ranking.
- Click “试运行”.
- Preview updates.
- Click “保存板块”.
- New block appears in dashboard edit mode.

---

### Task 5: Write Prototype Browser QA

**Files:**
- Create: `/tmp/theme_library_dashboard_composition_proto_qa.py`

- [ ] **Step 1: Create QA script**

Create a script that opens:

```text
http://127.0.0.1:8033/theme-library-sql-blocks-prototype.html
```

It should verify:

- Page loads.
- A scenario dashboard toolbar exists.
- At least four dashboard blocks render.
- Clicking “编辑看板” shows edit controls.
- Changing one block width to full changes class to `widthFull`.
- Hiding a block removes it after saving.
- Opening block editor shows SQL textarea and field mapping textarea.
- Previewing SQL shows success status.
- Saving a new block increases block count.

- [ ] **Step 2: Run QA script**

Run:

```bash
python3 /tmp/theme_library_dashboard_composition_proto_qa.py
```

Expected:

```json
{"ok": true}
```

- [ ] **Step 3: Fix prototype until QA passes**

If any check fails, fix the prototype and rerun the same script.

---

### Task 6: Port the Data Model to Vue

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/theme-library/index.tsx`

- [ ] **Step 1: Replace single SQL result interfaces with dashboard block interfaces**

Add these interfaces near existing type declarations:

```ts
type DisplayType = 'metric' | 'trend' | 'bar' | 'pie' | 'rank' | 'table' | 'text' | 'alert'
type BlockWidth = '1/3' | '1/2' | '2/3' | 'full'
type DataMode = 'dataset' | 'sql'

interface PreviewRow {
  label: string
  value: string | number
  unit?: string
  category?: string
  statDate?: string
}

interface FieldMapping {
  label?: string
  value?: string
  unit?: string
  category?: string
  time?: string
}

interface DashboardBlock {
  id: string
  title: string
  description: string
  displayType: DisplayType
  width: BlockWidth
  visible: boolean
  sortNo: number
  dataMode: DataMode
  datasetId: string
  datasourceId: string | number
  sqlText: string
  fieldMapping: FieldMapping
  previewRows: PreviewRow[]
  remark: string
}

interface DashboardConfig {
  editing: boolean
  blocks: DashboardBlock[]
}
```

- [ ] **Step 2: Extend `Scenario`**

Modify `Scenario`:

```ts
interface Scenario {
  title: string
  goal: string
  summary: SummaryItem[]
  items: ScenarioRow[]
  dashboard?: DashboardConfig
}
```

Remove reliance on `queryRows` and `sqlConfig` for final rendering after porting.

- [ ] **Step 3: Add block factory functions**

Add:

```ts
const createBlock = (overrides: Partial<DashboardBlock> = {}): DashboardBlock => ({
  id: overrides.id || `block-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
  title: overrides.title || '新增 SQL 数据板块',
  description: overrides.description || '请配置数据来源、字段映射和展示方式。',
  displayType: overrides.displayType || 'metric',
  width: overrides.width || '1/2',
  visible: overrides.visible !== false,
  sortNo: Number(overrides.sortNo || 10),
  dataMode: overrides.dataMode || 'dataset',
  datasetId: overrides.datasetId || 'alarm-receive-summary',
  datasourceId: overrides.datasourceId || 'oracle-110-core',
  sqlText: overrides.sqlText || 'select label, value from ads_theme_metric where stat_date = ${endDate}',
  fieldMapping: overrides.fieldMapping || {
    label: 'label',
    value: 'value',
    category: 'category',
    time: 'statDate',
    unit: 'unit'
  },
  previewRows: overrides.previewRows || [
    { label: '今日接报', value: 1286, unit: '起', category: '接警', statDate: '今日' },
    { label: '较昨日', value: 8.4, unit: '%', category: '趋势', statDate: '今日' },
    { label: '待核查异常', value: 17, unit: '起', category: '质量', statDate: '今日' }
  ],
  remark: overrides.remark || '数据来自当前主题库场景配置。'
})

const defaultBlocksForScenario = (scenarioTitle: string): DashboardBlock[] => [
  createBlock({ id: `${scenarioTitle}-metric`, title: '核心指标', displayType: 'metric', width: '1/3', sortNo: 10 }),
  createBlock({
    id: `${scenarioTitle}-trend`,
    title: '趋势变化',
    displayType: 'trend',
    width: '2/3',
    sortNo: 20,
    previewRows: [
      { label: '周一', value: 1086, statDate: '周一' },
      { label: '周二', value: 1132, statDate: '周二' },
      { label: '周三', value: 1198, statDate: '周三' },
      { label: '今日', value: 1286, statDate: '今日' }
    ]
  }),
  createBlock({
    id: `${scenarioTitle}-rank`,
    title: '责任单位排行',
    displayType: 'rank',
    width: '1/2',
    sortNo: 30,
    previewRows: [
      { label: '西城派出所', value: 218 },
      { label: '东城派出所', value: 196 },
      { label: '南城派出所', value: 173 }
    ]
  }),
  createBlock({ id: `${scenarioTitle}-table`, title: '明细数据', displayType: 'table', width: 'full', sortNo: 40 })
]
```

- [ ] **Step 4: Add dashboard normalizer**

Inside `setup`, add:

```ts
const ensureScenarioDashboard = (scenario?: Scenario): DashboardConfig | undefined => {
  if (!scenario) return undefined
  if (!scenario.dashboard) {
    scenario.dashboard = {
      editing: false,
      blocks: defaultBlocksForScenario(scenario.title)
    }
  }
  if (!Array.isArray(scenario.dashboard.blocks)) {
    scenario.dashboard.blocks = defaultBlocksForScenario(scenario.title)
  }
  scenario.dashboard.blocks.sort((a, b) => a.sortNo - b.sortNo)
  return scenario.dashboard
}
```

- [ ] **Step 5: Run type check**

Run the repository’s UI typecheck command if available. If not known, run:

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui
pnpm lint
```

Expected: no TypeScript syntax errors from `theme-library/index.tsx`.

---

### Task 7: Build Vue Dashboard Renderers

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/theme-library/index.tsx`

- [ ] **Step 1: Add display helpers**

Inside `setup`, add:

```ts
const displayTypeName = (type: DisplayType) =>
  ({
    metric: '指标卡',
    trend: '趋势图',
    bar: '柱状图',
    pie: '饼图',
    rank: '排行榜',
    table: '明细表',
    text: '文字说明',
    alert: '提醒卡片'
  }[type])

const blockWidthClass = (width: BlockWidth) =>
  ({
    '1/3': styles.widthThird,
    '1/2': styles.widthHalf,
    '2/3': styles.widthTwoThirds,
    full: styles.widthFull
  }[width])
```

- [ ] **Step 2: Add preview renderers**

Inside `setup`, add:

```tsx
const renderMetricBlock = (block: DashboardBlock) => (
  <div class={styles.metricRow}>
    {block.previewRows.slice(0, 4).map((row) => (
      <div class={styles.metric}>
        <strong>{row.value}{row.unit || ''}</strong>
        <span>{row.label}</span>
      </div>
    ))}
  </div>
)

const renderBarLikeBlock = (block: DashboardBlock) => {
  const max = Math.max(...block.previewRows.map((row) => Number(row.value) || 0), 1)
  return (
    <div class={styles.chart}>
      {block.previewRows.slice(0, 7).map((row) => (
        <span class={styles.bar}>
          <i style={{ height: `${Math.max(18, ((Number(row.value) || 0) / max) * 100)}%` }} />
          <em>{row.label || row.statDate}</em>
        </span>
      ))}
    </div>
  )
}

const renderRankBlock = (block: DashboardBlock) => (
  <ol class={styles.rankList}>
    {block.previewRows.slice(0, 6).map((row, index) => (
      <li><b>{index + 1}</b><span>{row.label}</span><strong>{row.value}</strong></li>
    ))}
  </ol>
)

const renderTableBlock = (block: DashboardBlock) => (
  <div class={styles.tableWrap}>
    <table>
      <thead><tr><th>名称</th><th>数值</th><th>分类</th><th>日期</th></tr></thead>
      <tbody>
        {block.previewRows.map((row) => (
          <tr><td>{row.label}</td><td>{row.value}{row.unit || ''}</td><td>{row.category || '-'}</td><td>{row.statDate || '-'}</td></tr>
        ))}
      </tbody>
    </table>
  </div>
)
```

- [ ] **Step 3: Add generic preview switch**

Add:

```tsx
const renderBlockPreview = (block: DashboardBlock) => {
  if (block.displayType === 'metric') return renderMetricBlock(block)
  if (block.displayType === 'trend' || block.displayType === 'bar') return renderBarLikeBlock(block)
  if (block.displayType === 'rank') return renderRankBlock(block)
  if (block.displayType === 'table') return renderTableBlock(block)
  if (block.displayType === 'alert') {
    return <div class={styles.alertCard}>{block.previewRows.map((row) => <p>{row.label}：{row.value}{row.unit || ''}</p>)}</div>
  }
  return <div class={styles.textCard}><p>{block.description}</p><p>{block.remark}</p></div>
}
```

- [ ] **Step 4: Add dashboard block renderer**

Add:

```tsx
const renderDashboardBlock = (block: DashboardBlock, dashboard: DashboardConfig) => (
  <article class={[styles.dashboardBlock, blockWidthClass(block.width), !block.visible ? styles.hiddenBlock : '']}>
    <div class={styles.blockHead}>
      <div>
        <h3>{block.title}</h3>
        <span>{displayTypeName(block.displayType)} / {block.description}</span>
      </div>
      <NTag size='small'>{block.width === 'full' ? '整行' : block.width}</NTag>
    </div>
    {dashboard.editing && renderBlockEditControls(block)}
    {renderBlockPreview(block)}
    <div class={styles.blockRemark}>{block.remark}</div>
  </article>
)
```

- [ ] **Step 5: Add dashboard grid renderer**

Add:

```tsx
const renderScenarioDashboard = () => {
  const scenario = activeScenario.value
  const dashboard = ensureScenarioDashboard(scenario)
  if (!scenario || !dashboard) return null
  const blocks = dashboard.blocks
    .filter((block) => dashboard.editing || block.visible)
    .sort((a, b) => a.sortNo - b.sortNo)

  return (
    <div class={styles.dashboardArea}>
      {renderDashboardToolbar(dashboard)}
      {blocks.length ? (
        <div class={styles.dashboardGrid}>
          {blocks.map((block) => renderDashboardBlock(block, dashboard))}
        </div>
      ) : (
        <div class={styles.emptyBoard}>当前场景没有可见 SQL 数据板块。</div>
      )}
    </div>
  )
}
```

- [ ] **Step 6: Replace old table-only scenario body**

In `renderAssetFocus`, replace the old summary/table area after tabs with:

```tsx
<div class={styles.scenarioMain}>
  <div class={styles.summaryGrid}>
    {activeScenario.value.summary.map((item) => (
      <div class={styles.summaryTile}>
        <strong>{item.value}</strong>
        <span>{item.label}</span>
      </div>
    ))}
  </div>
  {renderScenarioDashboard()}
</div>
```

- [ ] **Step 7: Run UI lint/type check**

Run:

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui
pnpm lint
```

Expected: no lint/type errors in the modified theme-library files.

---

### Task 8: Build Vue Edit Mode Actions

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/theme-library/index.tsx`

- [ ] **Step 1: Add edit state actions**

Inside `setup`, add:

```ts
const enterDashboardEdit = () => {
  const dashboard = ensureScenarioDashboard(activeScenario.value)
  if (dashboard) dashboard.editing = true
}

const cancelDashboardEdit = () => {
  const dashboard = ensureScenarioDashboard(activeScenario.value)
  if (dashboard) dashboard.editing = false
}

const saveDashboardEdit = () => {
  const dashboard = ensureScenarioDashboard(activeScenario.value)
  if (dashboard) dashboard.editing = false
  message.success('看板配置已保存。')
}
```

- [ ] **Step 2: Add block lookup helper**

Add:

```ts
const activeDashboard = () => ensureScenarioDashboard(activeScenario.value)

const findBlock = (blockId: string) =>
  activeDashboard()?.blocks.find((block) => block.id === blockId)
```

- [ ] **Step 3: Add mutation actions**

Add:

```ts
const moveBlock = (blockId: string, direction: -1 | 1) => {
  const dashboard = activeDashboard()
  if (!dashboard) return
  const blocks = dashboard.blocks.sort((a, b) => a.sortNo - b.sortNo)
  const index = blocks.findIndex((block) => block.id === blockId)
  const nextIndex = index + direction
  if (index < 0 || nextIndex < 0 || nextIndex >= blocks.length) return
  const currentSort = blocks[index].sortNo
  blocks[index].sortNo = blocks[nextIndex].sortNo
  blocks[nextIndex].sortNo = currentSort
}

const updateBlockWidth = (blockId: string, width: BlockWidth) => {
  const block = findBlock(blockId)
  if (block) block.width = width
}

const updateBlockDisplay = (blockId: string, displayType: DisplayType) => {
  const block = findBlock(blockId)
  if (block) block.displayType = displayType
}

const toggleBlockVisible = (blockId: string) => {
  const block = findBlock(blockId)
  if (block) block.visible = !block.visible
}

const copyBlock = (blockId: string) => {
  const dashboard = activeDashboard()
  const block = findBlock(blockId)
  if (!dashboard || !block) return
  dashboard.blocks.push(createBlock({
    ...block,
    id: `block-copy-${Date.now()}`,
    title: `${block.title} 副本`,
    sortNo: Math.max(...dashboard.blocks.map((item) => item.sortNo), 0) + 10
  }))
}

const deleteBlock = (blockId: string) => {
  const dashboard = activeDashboard()
  if (!dashboard) return
  dashboard.blocks = dashboard.blocks.filter((block) => block.id !== blockId)
}
```

- [ ] **Step 4: Add dashboard toolbar renderer**

Add:

```tsx
const renderDashboardToolbar = (dashboard: DashboardConfig) => (
  <div class={[styles.dashboardToolbar, dashboard.editing ? styles.editing : '']}>
    <div>
      <strong>{dashboard.editing ? '编辑场景看板' : '场景看板'}</strong>
      <span>{dashboard.editing ? '可调整顺序、宽度、显隐和展示方式。' : `${dashboard.blocks.filter((block) => block.visible).length} 个可见板块 / ${dashboard.blocks.length} 个配置板块`}</span>
    </div>
    {canEdit.value && (
      <NSpace>
        <NButton onClick={() => openBlockEditor()}>新增 SQL 板块</NButton>
        {dashboard.editing ? (
          <>
            <NButton onClick={cancelDashboardEdit}>取消编辑</NButton>
            <NButton type='primary' onClick={saveDashboardEdit}>保存配置</NButton>
          </>
        ) : (
          <NButton type='primary' onClick={enterDashboardEdit}>编辑看板</NButton>
        )}
      </NSpace>
    )}
  </div>
)
```

- [ ] **Step 5: Add block edit controls renderer**

Add:

```tsx
const displayTypeOptions: SelectOption[] = [
  { label: '指标卡', value: 'metric' },
  { label: '趋势图', value: 'trend' },
  { label: '柱状图', value: 'bar' },
  { label: '饼图', value: 'pie' },
  { label: '排行榜', value: 'rank' },
  { label: '明细表', value: 'table' },
  { label: '文字说明', value: 'text' },
  { label: '提醒卡片', value: 'alert' }
]

const widthOptions: SelectOption[] = [
  { label: '1/3', value: '1/3' },
  { label: '1/2', value: '1/2' },
  { label: '2/3', value: '2/3' },
  { label: '整行', value: 'full' }
]

const renderBlockEditControls = (block: DashboardBlock) => (
  <div class={styles.blockEditControls}>
    <NButton size='tiny' onClick={() => moveBlock(block.id, -1)}>上移</NButton>
    <NButton size='tiny' onClick={() => moveBlock(block.id, 1)}>下移</NButton>
    <NSelect size='small' value={block.width} options={widthOptions} onUpdateValue={(value) => updateBlockWidth(block.id, value as BlockWidth)} />
    <NSelect size='small' value={block.displayType} options={displayTypeOptions} onUpdateValue={(value) => updateBlockDisplay(block.id, value as DisplayType)} />
    <NButton size='tiny' onClick={() => toggleBlockVisible(block.id)}>{block.visible ? '隐藏' : '显示'}</NButton>
    <NButton size='tiny' onClick={() => openBlockEditor(block.id)}>编辑</NButton>
    <NButton size='tiny' onClick={() => copyBlock(block.id)}>复制</NButton>
    <NButton size='tiny' type='error' onClick={() => deleteBlock(block.id)}>删除</NButton>
  </div>
)
```

- [ ] **Step 6: Verify edit mode in browser**

Run Dolphin UI dev server if needed, then manually check:

- Click a business object.
- Click a scenario tab.
- Click “编辑看板”.
- Controls appear in each block.
- Width and display type changes immediately affect the block.
- Hide, copy, delete work.
- Save exits edit mode.

---

### Task 9: Build Vue Block Editor Modal

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/theme-library/index.tsx`

- [ ] **Step 1: Add modal state**

Inside `setup`, add:

```ts
const blockModalVisible = ref(false)
const editingBlockId = ref('')
const blockDraft = reactive<DashboardBlock>(createBlock())
const blockPreviewStatus = ref('未试运行')
```

- [ ] **Step 2: Add editor open/close functions**

Add:

```ts
const resetBlockDraft = (block: DashboardBlock) => {
  Object.assign(blockDraft, JSON.parse(JSON.stringify(block)))
}

const openBlockEditor = (blockId = '') => {
  const dashboard = activeDashboard()
  if (!dashboard) return
  const block = blockId ? findBlock(blockId) : createBlock({
    sortNo: Math.max(...dashboard.blocks.map((item) => item.sortNo), 0) + 10
  })
  if (!block) return
  editingBlockId.value = blockId
  resetBlockDraft(block)
  blockPreviewStatus.value = '未试运行'
  blockModalVisible.value = true
}

const closeBlockEditor = () => {
  blockModalVisible.value = false
}
```

- [ ] **Step 3: Add SQL preview helper**

Add:

```ts
const buildPreviewRowsFromSql = (sql: string, displayType: DisplayType): PreviewRow[] => {
  if (displayType === 'rank') {
    return [
      { label: '西城派出所', value: 218 },
      { label: '东城派出所', value: 196 },
      { label: '南城派出所', value: 173 }
    ]
  }
  if (displayType === 'trend' || displayType === 'bar') {
    return [
      { label: '周一', value: 1086, statDate: '周一' },
      { label: '周二', value: 1132, statDate: '周二' },
      { label: '今日', value: 1286, statDate: '今日' }
    ]
  }
  return [
    { label: '查询结果', value: 1286, unit: '条', category: '模拟预览', statDate: '今日' },
    { label: '异常记录', value: 17, unit: '条', category: '模拟预览', statDate: '今日' }
  ]
}

const previewBlockSql = () => {
  if (!blockDraft.sqlText.trim()) {
    message.warning('请先输入 SQL。')
    return
  }
  blockDraft.previewRows = buildPreviewRowsFromSql(blockDraft.sqlText, blockDraft.displayType)
  blockPreviewStatus.value = `试运行成功，返回 ${blockDraft.previewRows.length} 行模拟数据。`
}
```

- [ ] **Step 4: Add save function**

Add:

```ts
const saveBlockEditor = () => {
  const dashboard = activeDashboard()
  if (!dashboard) return
  const existing = editingBlockId.value ? findBlock(editingBlockId.value) : undefined
  if (existing) Object.assign(existing, JSON.parse(JSON.stringify(blockDraft)), { id: existing.id })
  else dashboard.blocks.push(JSON.parse(JSON.stringify(blockDraft)))
  dashboard.editing = true
  blockModalVisible.value = false
  message.success('SQL 数据板块已保存。')
}
```

- [ ] **Step 5: Render modal in return JSX**

Before closing root `<div>`, add:

```tsx
<NModal v-model:show={blockModalVisible.value} preset='card' class={styles.blockModal} title='编辑 SQL 数据板块' bordered={false}>
  <div class={styles.blockForm}>
    <div class={styles.formGrid}>
      <label><span>板块名称</span><NInput value={blockDraft.title} onUpdateValue={(value) => (blockDraft.title = value)} /></label>
      <label><span>展示方式</span><NSelect value={blockDraft.displayType} options={displayTypeOptions} onUpdateValue={(value) => (blockDraft.displayType = value as DisplayType)} /></label>
      <label><span>宽度</span><NSelect value={blockDraft.width} options={widthOptions} onUpdateValue={(value) => (blockDraft.width = value as BlockWidth)} /></label>
      <label><span>数据模式</span><NSelect value={blockDraft.dataMode} options={[{ label: '选择已有数据集', value: 'dataset' }, { label: '高级模式：手写 SQL', value: 'sql' }]} onUpdateValue={(value) => (blockDraft.dataMode = value as DataMode)} /></label>
      <label class={styles.span2}><span>板块说明</span><NInput value={blockDraft.description} onUpdateValue={(value) => (blockDraft.description = value)} /></label>
      <label><span>数据集</span><NSelect value={blockDraft.datasetId} options={[{ label: '接报警情汇总数据集', value: 'alarm-receive-summary' }, { label: '重复报警排行数据集', value: 'alarm-repeat-rank' }]} onUpdateValue={(value) => (blockDraft.datasetId = value as string)} /></label>
      <label><span>DolphinScheduler 数据源</span><NSelect value={blockDraft.datasourceId} options={datasources.value} onUpdateValue={(value) => (blockDraft.datasourceId = value)} /></label>
      <label class={styles.span2}><span>SQL</span><NInput type='textarea' value={blockDraft.sqlText} onUpdateValue={(value) => (blockDraft.sqlText = value)} /></label>
      <label class={styles.span2}><span>口径备注</span><NInput value={blockDraft.remark} onUpdateValue={(value) => (blockDraft.remark = value)} /></label>
    </div>
    <div class={styles.statusLine}>{blockPreviewStatus.value}</div>
    <div class={styles.previewBlock}>{renderBlockPreview(blockDraft)}</div>
    <NSpace justify='end'>
      <NButton onClick={previewBlockSql}>试运行</NButton>
      <NButton type='primary' onClick={saveBlockEditor}>保存板块</NButton>
    </NSpace>
  </div>
</NModal>
```

- [ ] **Step 6: Verify modal**

Manual checks:

- Click “新增 SQL 板块”.
- Modal opens.
- Change title, display type, width.
- Click “试运行”.
- Preview updates.
- Save block.
- New block appears in edit grid.

---

### Task 10: Add Vue Styles

**Files:**
- Modify: `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/theme-library/index.module.scss`

- [ ] **Step 1: Add dashboard area styles**

Add:

```scss
.dashboardArea {
  display: grid;
  gap: 12px;
}

.dashboardToolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #d8dde6;
  border-radius: 8px;
  background: #fff;

  strong {
    display: block;
    margin-bottom: 4px;
    color: #101828;
  }

  span {
    color: #667085;
    font-size: 12px;
  }
}

.editing {
  border-color: #8db4ee;
  background: #eef6ff;
}

.dashboardGrid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}
```

- [ ] **Step 2: Add block styles**

Add:

```scss
.dashboardBlock {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #d8dde6;
  border-radius: 8px;
  background: #fff;
}

.hiddenBlock {
  opacity: .48;
}

.widthThird { grid-column: span 2; }
.widthHalf { grid-column: span 3; }
.widthTwoThirds { grid-column: span 4; }
.widthFull { grid-column: 1 / -1; }

.blockHead {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;

  h3 {
    margin: 0 0 4px;
    color: #101828;
    font-size: 15px;
  }

  span {
    color: #667085;
    font-size: 12px;
  }
}

.blockRemark {
  color: #667085;
  font-size: 12px;
}

.blockEditControls {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 8px;
  border: 1px dashed #c8d7ee;
  border-radius: 8px;
  background: #f8fafc;
}
```

- [ ] **Step 3: Add modal form styles**

Add:

```scss
.blockModal {
  width: min(980px, calc(100vw - 48px));
}

.blockForm {
  display: grid;
  gap: 12px;
}

.formGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;

  label {
    display: grid;
    gap: 6px;
    color: #475467;
    font-weight: 700;
  }
}

.span2 {
  grid-column: 1 / -1;
}

.statusLine {
  min-height: 20px;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.previewBlock {
  display: grid;
  gap: 8px;
}

.emptyBoard {
  padding: 24px;
  border: 1px dashed #c8d7ee;
  border-radius: 8px;
  background: #fbfdff;
  color: #667085;
  text-align: center;
}
```

- [ ] **Step 4: Add responsive styles**

Add inside existing media query:

```scss
.dashboardGrid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.widthThird,
.widthHalf,
.widthTwoThirds,
.widthFull {
  grid-column: 1 / -1;
}

.blockEditControls {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
```

- [ ] **Step 5: Run style check**

Run:

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui
pnpm lint
```

Expected: no SCSS module syntax errors.

---

### Task 11: Run Dolphin UI QA

**Files:**
- Create: `/tmp/theme_library_dashboard_composition_vue_qa.py`

- [ ] **Step 1: Start Dolphin UI dev server**

Run:

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui
pnpm dev --host 127.0.0.1
```

Expected: Vite dev server prints a local URL, usually:

```text
http://127.0.0.1:5173/
```

- [ ] **Step 2: Create browser QA script**

Create `/tmp/theme_library_dashboard_composition_vue_qa.py` that verifies:

- `/theme-library` loads after login/session setup available in the local environment.
- Domain list renders.
- Clicking “接报警情” opens scenario panel.
- Scenario dashboard toolbar renders.
- At least four dashboard blocks render.
- “编辑看板” shows block edit controls.
- Width selector changes a block class.
- Display type selector changes preview.
- “新增 SQL 板块” opens modal.
- “试运行” updates preview status.
- Saving adds or updates a block.

- [ ] **Step 3: Run QA script**

Run:

```bash
python3 /tmp/theme_library_dashboard_composition_vue_qa.py
```

Expected:

```json
{"ok": true}
```

- [ ] **Step 4: Fix until QA passes**

If the script fails due to login redirect, first restore local Dolphin auth/session according to the current project practice, then rerun. If it fails due to UI behavior, fix the Vue code and rerun.

---

### Task 12: Final Verification and Handoff

**Files:**
- Modify if needed: `/Users/luwang/bigdata-build/dolphinscheduler/docs/superpowers/specs/2026-05-25-theme-library-dashboard-composition-prd.md`
- Create or update: `/Users/luwang/bigdata-build/dolphinscheduler/docs/superpowers/plans/2026-05-25-theme-library-dashboard-composition.md`

- [ ] **Step 1: Run prototype QA**

Run:

```bash
python3 /tmp/theme_library_dashboard_composition_proto_qa.py
```

Expected:

```json
{"ok": true}
```

- [ ] **Step 2: Run UI lint**

Run:

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui
pnpm lint
```

Expected: no errors introduced by `src/views/theme-library`.

- [ ] **Step 3: Run Dolphin UI QA**

Run:

```bash
python3 /tmp/theme_library_dashboard_composition_vue_qa.py
```

Expected:

```json
{"ok": true}
```

- [ ] **Step 4: Manual browser review**

Open:

```text
http://127.0.0.1:5173/theme-library
```

Verify:

- The page does not show big-screen designer concepts.
- The page stays centered on Theme Library scenario analysis.
- View mode is clean.
- Edit mode is explicit.
- SQL block modal is understandable.

- [ ] **Step 5: Document residual risks**

If real SQL execution is still mocked, note:

```text
当前 SQL 试运行仍为前端模拟，真实执行需要后端接口支持数据源查询、参数替换、安全校验和超时控制。
```

- [ ] **Step 6: Commit if repository metadata is available**

If a `.git` directory is available in the active workspace, run:

```bash
git add \
  docs/superpowers/plans/2026-05-25-theme-library-dashboard-composition.md \
  docs/superpowers/specs/2026-05-25-theme-library-dashboard-composition-prd.md \
  dolphinscheduler-ui/src/views/theme-library/index.tsx \
  dolphinscheduler-ui/src/views/theme-library/index.module.scss
git commit -m "feat: add theme library dashboard composition"
```

If `.git` is not available, skip commit and report that the workspace is not currently a git repository.

