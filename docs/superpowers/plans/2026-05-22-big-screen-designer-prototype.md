# Big Screen Designer Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an independent HTML prototype for a big-screen designer where users start from a blank canvas, drag components, bind theme-library alarm-domain SQL data blocks, preview data, map fields, save layout, and enter preview mode.

**Architecture:** The prototype lives outside DolphinScheduler UI code under `data-preview-design-repo/.ai/big-screen-designer/prototype/`. It uses one HTML page plus focused CSS/JS files, local mock data, browser-native drag/mouse events, and `localStorage` persistence. No backend, no DolphinScheduler route changes, and no real database access.

**Tech Stack:** HTML, CSS, vanilla JavaScript, browser `localStorage`, optional inline SVG/CSS chart rendering, Puppeteer-based click QA.

---

## File Structure

Create this independent prototype directory:

```text
/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/
```

Files:

- `big-screen-designer.html`  
  Main page shell. Loads CSS and JS. Contains top toolbar, left component palette, central canvas viewport, right configuration panel, and preview overlay controls.

- `big-screen-designer.css`  
  All layout, visual styling, grid, selected component outline, resize handles, right panel controls, preview mode, and component rendering styles.

- `big-screen-designer.js`  
  Data model, mock theme-library data blocks, drag/drop creation, selection, move/resize, configuration updates, mock SQL execution, field mapping, rendering, save/load, preview mode, and keyboard shortcuts.

- `README.md`  
  Explains prototype scope, how to open it, and known first-version limits.

- `test-report.md`  
  Filled after QA with tested flows, screenshots, bugs found, and final status.

- `/Users/luwang/bigdata-build/dolphinscheduler/tmp_big_screen_designer_qa.js`  
  Puppeteer QA script kept in the current DolphinScheduler working directory, matching existing temporary QA script pattern in this workspace.

Important boundaries:

- Do not modify `dolphinscheduler-ui/`.
- Do not modify theme-library TSX/SCSS files.
- Do not add backend code.
- Do not depend on external CDN resources.

---

### Task 1: Prototype Directory And Static Shell

**Files:**
- Create: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.html`
- Create: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.css`
- Create: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.js`
- Create: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/README.md`

- [ ] **Step 1: Create prototype directory**

Run:

```bash
mkdir -p /Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype
```

Expected: directory exists.

- [ ] **Step 2: Create HTML shell**

Write `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.html` with:

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>大屏设计器 - 主题库原型</title>
  <link rel="stylesheet" href="./big-screen-designer.css" />
</head>
<body>
  <div id="app" class="designer-app">
    <header class="topbar">
      <div class="brand">
        <div class="brand-icon">B</div>
        <div>
          <strong>大屏设计器</strong>
          <span>主题库 / 警情域</span>
        </div>
      </div>
      <div class="toolbar">
        <button id="newCanvasBtn" class="btn">新建</button>
        <button id="saveLayoutBtn" class="btn primary">保存布局</button>
        <button id="loadLayoutBtn" class="btn">加载布局</button>
        <button id="clearCanvasBtn" class="btn danger">清空画布</button>
        <select id="zoomSelect" class="select">
          <option value="1">100%</option>
          <option value="0.75">75%</option>
          <option value="0.5">50%</option>
        </select>
        <button id="previewBtn" class="btn">预览模式</button>
      </div>
    </header>

    <main class="workspace">
      <aside class="left-panel">
        <div class="panel-title">组件库</div>
        <div id="componentPalette" class="component-palette"></div>
      </aside>

      <section class="canvas-area">
        <div class="canvas-meta">
          <span>空白画布</span>
          <span>1920 × 1080 · 10px 网格吸附</span>
        </div>
        <div id="canvasViewport" class="canvas-viewport">
          <div id="canvas" class="canvas"></div>
        </div>
      </section>

      <aside class="right-panel">
        <div class="panel-title">组件配置</div>
        <div id="emptyConfig" class="empty-config">请选择画布组件进行配置</div>
        <div id="configPanel" class="config-panel hidden">
          <section class="config-section">
            <h3>基础</h3>
            <label>标题<input id="titleInput" class="input" /></label>
            <div class="grid-2">
              <label>X<input id="xInput" class="input" type="number" /></label>
              <label>Y<input id="yInput" class="input" type="number" /></label>
              <label>宽<input id="wInput" class="input" type="number" /></label>
              <label>高<input id="hInput" class="input" type="number" /></label>
            </div>
            <div class="row-actions">
              <button id="copyBtn" class="btn small">复制</button>
              <button id="bringFrontBtn" class="btn small">置顶</button>
              <button id="sendBackBtn" class="btn small">置底</button>
              <button id="deleteBtn" class="btn small danger">删除</button>
            </div>
          </section>

          <section class="config-section">
            <h3>样式</h3>
            <label>背景色<input id="bgInput" class="input" type="color" /></label>
            <label>文字色<input id="colorInput" class="input" type="color" /></label>
          </section>

          <section class="config-section">
            <h3>数据</h3>
            <label>主题域<select id="domainSelect" class="select"></select></label>
            <label>业务项<select id="businessSelect" class="select"></select></label>
            <label>SQL 数据块<select id="blockSelect" class="select"></select></label>
            <label>SQL<textarea id="sqlInput" class="textarea"></textarea></label>
            <button id="runSqlBtn" class="btn primary full">执行预览</button>
            <div id="sqlMessage" class="message"></div>
          </section>

          <section class="config-section">
            <h3>字段映射与预览</h3>
            <div id="mappingPanel" class="mapping-panel"></div>
            <div id="dataPreview" class="data-preview"></div>
          </section>
        </div>
      </aside>
    </main>

    <button id="exitPreviewBtn" class="exit-preview hidden">退出预览</button>
  </div>
  <script src="./big-screen-designer.js"></script>
</body>
</html>
```

- [ ] **Step 3: Create minimal CSS placeholder**

Write `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.css` with a temporary smoke-test style:

```css
* { box-sizing: border-box; }
body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #f4f7fb; color: #172033; }
.designer-app { min-height: 100vh; display: flex; flex-direction: column; }
.topbar { height: 58px; display: flex; align-items: center; justify-content: space-between; padding: 0 18px; background: #fff; border-bottom: 1px solid #d8e1ef; }
.brand { display: flex; gap: 10px; align-items: center; }
.brand-icon { width: 32px; height: 32px; border-radius: 8px; display: grid; place-items: center; background: #1f65d6; color: #fff; font-weight: 800; }
.brand span { display: block; color: #64728a; font-size: 12px; margin-top: 2px; }
.toolbar { display: flex; gap: 8px; align-items: center; }
.workspace { flex: 1; display: grid; grid-template-columns: 236px 1fr 360px; min-height: 0; }
.left-panel, .right-panel { background: #fff; border-right: 1px solid #d8e1ef; padding: 14px; overflow: auto; }
.right-panel { border-left: 1px solid #d8e1ef; border-right: 0; }
.panel-title { font-weight: 700; margin-bottom: 12px; }
.canvas-area { min-width: 0; display: flex; flex-direction: column; padding: 14px; }
.canvas-meta { display: flex; justify-content: space-between; color: #64728a; font-size: 13px; margin-bottom: 10px; }
.canvas-viewport { flex: 1; overflow: auto; border: 1px solid #cbd8ec; border-radius: 8px; background: #eaf0f8; }
.canvas { position: relative; width: 1920px; height: 1080px; transform-origin: 0 0; background: #0b1220; }
.btn, .select, .input, .textarea { border: 1px solid #cbd8ec; border-radius: 6px; background: #fff; color: #172033; }
.btn { height: 32px; padding: 0 12px; cursor: pointer; }
.btn.primary { background: #1f65d6; color: #fff; border-color: #1f65d6; }
.btn.danger { color: #b42318; border-color: #f0b8b2; }
.btn.small { height: 28px; padding: 0 8px; font-size: 12px; }
.select, .input { height: 32px; width: 100%; padding: 0 8px; }
.textarea { min-height: 110px; width: 100%; padding: 8px; resize: vertical; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
.hidden { display: none !important; }
```

- [ ] **Step 4: Create minimal JS smoke test**

Write `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.js` with:

```javascript
(() => {
  const palette = document.getElementById('componentPalette')
  palette.innerHTML = '<div class="empty-config">组件库加载中...</div>'
  console.info('big-screen-designer prototype loaded')
})()
```

- [ ] **Step 5: Create README**

Write `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/README.md` with:

```markdown
# 大屏设计器独立原型

第一版目标：从空白画布拖组件，绑定主题库警情域 SQL 数据块，执行模拟预览，字段映射，保存布局，进入预览模式。

本原型不修改 DolphinScheduler 二开代码，不连接真实数据库，不依赖外网 CDN。

打开方式：

```bash
cd /Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype
python3 -m http.server 8032
```

访问：

http://127.0.0.1:8032/big-screen-designer.html
```

- [ ] **Step 6: Smoke test static page**

Run:

```bash
cd /Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype
python3 -m http.server 8032
```

Expected: server starts and page can be opened at `http://127.0.0.1:8032/big-screen-designer.html`.

- [ ] **Step 7: Commit or record no-git reason**

Run:

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler
git status --short
```

Expected in current environment: `fatal: not a git repository`. Record this in the final report instead of committing.

---

### Task 2: Visual System And Static Designer Layout

**Files:**
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.css`
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.js`

- [ ] **Step 1: Replace CSS with full designer styling**

Replace `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.css` with styling that includes:

```css
:root {
  --bg: #f4f7fb;
  --panel: #ffffff;
  --line: #d8e1ef;
  --text: #172033;
  --muted: #66748b;
  --blue: #1f65d6;
  --blue-soft: #e7f0ff;
  --danger: #b42318;
  --screen: #08111f;
  --screen-card: #101d33;
  --screen-card-2: #132640;
  --screen-line: rgba(121, 166, 255, 0.22);
}

* { box-sizing: border-box; }
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  background: var(--bg);
  color: var(--text);
}
button, input, select, textarea { font: inherit; }
.designer-app { min-height: 100vh; display: flex; flex-direction: column; }
.topbar {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  background: var(--panel);
  border-bottom: 1px solid var(--line);
}
.brand { display: flex; gap: 10px; align-items: center; min-width: 260px; }
.brand-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: var(--blue);
  color: #fff;
  font-weight: 800;
}
.brand span { display: block; color: var(--muted); font-size: 12px; margin-top: 2px; }
.toolbar { display: flex; align-items: center; gap: 8px; }
.workspace { flex: 1; min-height: 0; display: grid; grid-template-columns: 236px minmax(640px, 1fr) 360px; }
.left-panel, .right-panel {
  background: var(--panel);
  padding: 14px;
  overflow: auto;
}
.left-panel { border-right: 1px solid var(--line); }
.right-panel { border-left: 1px solid var(--line); }
.panel-title { font-size: 15px; font-weight: 800; margin-bottom: 12px; }
.component-palette { display: grid; gap: 10px; }
.component-item {
  min-height: 58px;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 10px;
  background: #fff;
  cursor: grab;
  display: flex;
  gap: 10px;
  align-items: center;
}
.component-item:active { cursor: grabbing; }
.component-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: var(--blue-soft);
  color: var(--blue);
  font-weight: 800;
}
.component-item strong { display: block; font-size: 14px; }
.component-item span { display: block; color: var(--muted); font-size: 12px; margin-top: 2px; }
.canvas-area { min-width: 0; min-height: 0; display: flex; flex-direction: column; padding: 14px; }
.canvas-meta { display: flex; justify-content: space-between; color: var(--muted); font-size: 13px; margin-bottom: 10px; }
.canvas-viewport {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid #cbd8ec;
  border-radius: 8px;
  background: #eaf0f8;
  padding: 24px;
}
.canvas {
  position: relative;
  width: 1920px;
  height: 1080px;
  transform-origin: 0 0;
  background:
    linear-gradient(rgba(255,255,255,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.04) 1px, transparent 1px),
    radial-gradient(circle at top left, rgba(47, 116, 255, 0.2), transparent 360px),
    var(--screen);
  background-size: 10px 10px, 10px 10px, auto, auto;
  box-shadow: 0 20px 60px rgba(23, 32, 51, 0.28);
}
.canvas.empty::after {
  content: "从左侧拖组件到画布，开始制作警情域大屏";
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: rgba(226, 238, 255, 0.58);
  font-size: 28px;
  letter-spacing: 0;
  pointer-events: none;
}
.screen-widget {
  position: absolute;
  border: 1px solid var(--screen-line);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(25, 48, 83, 0.94), rgba(13, 27, 49, 0.94));
  color: #e8f1ff;
  overflow: hidden;
  user-select: none;
}
.screen-widget.selected {
  outline: 2px solid #73a7ff;
  outline-offset: 2px;
}
.widget-head {
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  border-bottom: 1px solid rgba(121, 166, 255, 0.18);
  font-weight: 700;
}
.widget-body { height: calc(100% - 34px); padding: 12px; }
.resize-handle {
  position: absolute;
  right: -5px;
  bottom: -5px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #73a7ff;
  border: 2px solid #fff;
  cursor: nwse-resize;
}
.config-panel { display: grid; gap: 12px; }
.config-section {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}
.config-section h3 { margin: 0 0 10px; font-size: 14px; }
.config-section label { display: grid; gap: 6px; color: var(--muted); font-size: 12px; margin-bottom: 10px; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.row-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.btn, .select, .input, .textarea {
  border: 1px solid #cbd8ec;
  border-radius: 6px;
  background: #fff;
  color: var(--text);
}
.btn { height: 32px; padding: 0 12px; cursor: pointer; }
.btn:hover { border-color: var(--blue); }
.btn.primary { background: var(--blue); color: #fff; border-color: var(--blue); }
.btn.danger { color: var(--danger); border-color: #f0b8b2; }
.btn.small { height: 28px; padding: 0 8px; font-size: 12px; }
.btn.full { width: 100%; }
.select, .input { height: 32px; width: 100%; padding: 0 8px; }
.textarea {
  min-height: 120px;
  width: 100%;
  padding: 8px;
  resize: vertical;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  line-height: 1.45;
}
.empty-config {
  min-height: 120px;
  display: grid;
  place-items: center;
  color: var(--muted);
  border: 1px dashed #cbd8ec;
  border-radius: 8px;
  padding: 18px;
  text-align: center;
}
.message { min-height: 22px; color: var(--muted); font-size: 12px; margin-top: 8px; }
.message.ok { color: #1c7c3b; }
.message.error { color: var(--danger); }
.mapping-panel { display: grid; gap: 8px; }
.data-preview {
  margin-top: 10px;
  max-height: 180px;
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 6px;
}
.data-preview table { width: 100%; border-collapse: collapse; font-size: 12px; }
.data-preview th, .data-preview td { border-bottom: 1px solid #edf1f7; padding: 6px 8px; text-align: left; white-space: nowrap; }
.hidden { display: none !important; }
.preview-mode .left-panel, .preview-mode .right-panel, .preview-mode .topbar { display: none; }
.preview-mode .workspace { display: block; }
.preview-mode .canvas-area { padding: 0; height: 100vh; }
.preview-mode .canvas-meta { display: none; }
.preview-mode .canvas-viewport { height: 100vh; border: 0; border-radius: 0; padding: 0; background: #000; }
.preview-mode .canvas { transform: scale(var(--preview-scale, 1)); transform-origin: 0 0; box-shadow: none; }
.exit-preview {
  position: fixed;
  right: 18px;
  top: 18px;
  z-index: 1000;
  height: 34px;
  padding: 0 14px;
  border-radius: 6px;
  border: 1px solid rgba(255,255,255,0.4);
  background: rgba(8,17,31,0.8);
  color: #fff;
}
```

- [ ] **Step 2: Add static component palette rendering**

In `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.js`, replace the smoke-test script with:

```javascript
(() => {
  const componentTypes = [
    { type: 'metric', name: '指标卡', desc: '主指标、副指标、趋势' },
    { type: 'line', name: '折线图', desc: '趋势与时序分析' },
    { type: 'bar', name: '柱状图', desc: '分类对比' },
    { type: 'pie', name: '饼图', desc: '占比分析' },
    { type: 'table', name: '表格', desc: '明细数据' },
    { type: 'text', name: '文本', desc: '标题与说明' },
    { type: 'image', name: '图片', desc: '图标或背景' },
    { type: 'lineDecor', name: '装饰线', desc: '分隔与强调' },
    { type: 'group', name: '容器分组', desc: '组合布局' }
  ]

  const state = {
    widgets: [],
    selectedId: null,
    zoom: 1
  }

  const dom = {
    palette: document.getElementById('componentPalette'),
    canvas: document.getElementById('canvas')
  }

  function renderPalette() {
    dom.palette.innerHTML = componentTypes.map((item) => `
      <div class="component-item" draggable="true" data-type="${item.type}">
        <div class="component-icon">${item.name.slice(0, 1)}</div>
        <div>
          <strong>${item.name}</strong>
          <span>${item.desc}</span>
        </div>
      </div>
    `).join('')
  }

  function renderCanvas() {
    dom.canvas.classList.toggle('empty', state.widgets.length === 0)
  }

  renderPalette()
  renderCanvas()
})()
```

- [ ] **Step 3: Open page and verify layout**

Run:

```bash
cd /Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype
python3 -m http.server 8032
```

Open `http://127.0.0.1:8032/big-screen-designer.html`.

Expected:

- Top toolbar visible.
- Left component palette visible.
- Center dark 1920×1080 blank canvas visible.
- Right panel shows empty config.
- No console errors.

---

### Task 3: Data Model, Drag Creation, Selection, Move, Resize

**Files:**
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.js`

- [ ] **Step 1: Add core constants and widget factory**

In `big-screen-designer.js`, add after `componentTypes`:

```javascript
  const GRID_SIZE = 10
  const defaultSizeByType = {
    metric: { w: 260, h: 150 },
    line: { w: 520, h: 280 },
    bar: { w: 520, h: 280 },
    pie: { w: 360, h: 280 },
    table: { w: 620, h: 300 },
    text: { w: 360, h: 90 },
    image: { w: 280, h: 180 },
    lineDecor: { w: 420, h: 36 },
    group: { w: 540, h: 320 }
  }

  function uid() {
    return `w_${Date.now()}_${Math.random().toString(16).slice(2)}`
  }

  function snap(value) {
    return Math.round(value / GRID_SIZE) * GRID_SIZE
  }

  function createWidget(type, x, y) {
    const meta = componentTypes.find((item) => item.type === type) || componentTypes[0]
    const size = defaultSizeByType[type] || { w: 320, h: 180 }
    return {
      id: uid(),
      type,
      title: meta.name,
      x: snap(x),
      y: snap(y),
      w: size.w,
      h: size.h,
      z: state.widgets.length + 1,
      style: {
        background: '#132640',
        color: '#e8f1ff'
      },
      data: {
        domain: '',
        business: '',
        blockId: '',
        sql: '',
        previewRows: [],
        fields: [],
        mapping: {}
      }
    }
  }
```

- [ ] **Step 2: Add widget rendering**

Add these functions:

```javascript
  function selectedWidget() {
    return state.widgets.find((item) => item.id === state.selectedId) || null
  }

  function renderWidgetBody(widget) {
    if (widget.type === 'text') {
      return `<div class="text-widget">${escapeHtml(widget.title || '文本')}</div>`
    }
    if (!widget.data.previewRows.length) {
      return `<div class="empty-config">未配置数据</div>`
    }
    return `<div class="empty-config">已加载 ${widget.data.previewRows.length} 行模拟数据</div>`
  }

  function escapeHtml(value) {
    return String(value || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
  }

  function renderCanvas() {
    dom.canvas.classList.toggle('empty', state.widgets.length === 0)
    dom.canvas.style.transform = `scale(${state.zoom})`
    dom.canvas.innerHTML = state.widgets.map((widget) => `
      <div class="screen-widget ${widget.id === state.selectedId ? 'selected' : ''}"
           data-id="${widget.id}"
           style="left:${widget.x}px;top:${widget.y}px;width:${widget.w}px;height:${widget.h}px;z-index:${widget.z};background:${widget.style.background};color:${widget.style.color}">
        <div class="widget-head">
          <span>${escapeHtml(widget.title)}</span>
          <span>${widget.data.blockId ? '已绑定' : '未绑定'}</span>
        </div>
        <div class="widget-body">${renderWidgetBody(widget)}</div>
        ${widget.id === state.selectedId ? '<div class="resize-handle" data-resize="true"></div>' : ''}
      </div>
    `).join('')
  }
```

- [ ] **Step 3: Wire drag from palette to canvas**

Add:

```javascript
  let draggingType = ''

  function bindPaletteEvents() {
    dom.palette.addEventListener('dragstart', (event) => {
      const item = event.target.closest('.component-item')
      if (!item) return
      draggingType = item.dataset.type
      event.dataTransfer.setData('text/plain', draggingType)
    })

    dom.canvas.addEventListener('dragover', (event) => {
      event.preventDefault()
    })

    dom.canvas.addEventListener('drop', (event) => {
      event.preventDefault()
      const type = event.dataTransfer.getData('text/plain') || draggingType
      if (!type) return
      const rect = dom.canvas.getBoundingClientRect()
      const x = (event.clientX - rect.left) / state.zoom
      const y = (event.clientY - rect.top) / state.zoom
      const widget = createWidget(type, x, y)
      state.widgets.push(widget)
      state.selectedId = widget.id
      renderAll()
    })
  }
```

- [ ] **Step 4: Add selection, move, and resize events**

Add:

```javascript
  let pointerMode = null

  function bindCanvasPointerEvents() {
    dom.canvas.addEventListener('mousedown', (event) => {
      const widgetEl = event.target.closest('.screen-widget')
      if (!widgetEl) {
        state.selectedId = null
        renderAll()
        return
      }
      const widget = state.widgets.find((item) => item.id === widgetEl.dataset.id)
      if (!widget) return
      state.selectedId = widget.id
      const isResize = Boolean(event.target.closest('[data-resize="true"]'))
      pointerMode = {
        type: isResize ? 'resize' : 'move',
        id: widget.id,
        startX: event.clientX,
        startY: event.clientY,
        origin: { x: widget.x, y: widget.y, w: widget.w, h: widget.h }
      }
      renderAll()
      event.preventDefault()
    })

    window.addEventListener('mousemove', (event) => {
      if (!pointerMode) return
      const widget = state.widgets.find((item) => item.id === pointerMode.id)
      if (!widget) return
      const dx = (event.clientX - pointerMode.startX) / state.zoom
      const dy = (event.clientY - pointerMode.startY) / state.zoom
      if (pointerMode.type === 'move') {
        widget.x = Math.max(0, snap(pointerMode.origin.x + dx))
        widget.y = Math.max(0, snap(pointerMode.origin.y + dy))
      } else {
        widget.w = Math.max(80, snap(pointerMode.origin.w + dx))
        widget.h = Math.max(50, snap(pointerMode.origin.h + dy))
      }
      renderAll()
    })

    window.addEventListener('mouseup', () => {
      pointerMode = null
    })
  }
```

- [ ] **Step 5: Add `renderAll` and initialize events**

Add:

```javascript
  function renderAll() {
    renderCanvas()
    renderConfig()
  }

  function renderConfig() {
    // Implemented in Task 4.
    document.getElementById('emptyConfig').classList.toggle('hidden', Boolean(selectedWidget()))
    document.getElementById('configPanel').classList.toggle('hidden', !selectedWidget())
  }

  renderPalette()
  renderAll()
  bindPaletteEvents()
  bindCanvasPointerEvents()
```

Remove the earlier duplicate `renderCanvas()` call at the bottom if present.

- [ ] **Step 6: Manual test drag and resize**

Open `http://127.0.0.1:8032/big-screen-designer.html`.

Expected:

- Drag a metric component to canvas creates a component.
- Clicking a component selects it.
- Dragging selected component moves it by grid steps.
- Dragging bottom-right handle resizes it.
- Clicking empty canvas clears selection.

---

### Task 4: Right Panel Configuration And Theme-Library Mock Data

**Files:**
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.js`

- [ ] **Step 1: Add mock theme-library data blocks**

Add after constants:

```javascript
  const themeCatalog = [
    {
      domain: '警情域',
      businesses: [
        {
          name: '接报警情',
          blocks: [
            {
              id: 'alarm_receive_total_trend',
              name: '接警总量与趋势',
              recommended: ['metric', 'line', 'bar'],
              snapshotTime: '2026-05-22 09:30',
              fields: ['date', 'alarm_count', 'yoy_rate', 'abnormal_count'],
              sql: `SELECT DATE_FORMAT(receive_time, '%Y-%m-%d') AS date,\n       COUNT(1) AS alarm_count,\n       ROUND(COUNT(1) * 0.084, 4) AS yoy_rate,\n       SUM(CASE WHEN status = 'ABNORMAL' THEN 1 ELSE 0 END) AS abnormal_count\nFROM 110_alarm_core\nWHERE receive_time >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY)\nGROUP BY DATE_FORMAT(receive_time, '%Y-%m-%d')\nORDER BY date`,
              rows: [
                { date: '周一', alarm_count: 1086, yoy_rate: 0.021, abnormal_count: 13 },
                { date: '周二', alarm_count: 1132, yoy_rate: 0.034, abnormal_count: 16 },
                { date: '周三', alarm_count: 1198, yoy_rate: 0.041, abnormal_count: 14 },
                { date: '周四', alarm_count: 1216, yoy_rate: 0.057, abnormal_count: 18 },
                { date: '周五', alarm_count: 1268, yoy_rate: 0.084, abnormal_count: 17 }
              ]
            },
            {
              id: 'duplicate_alarm_identify',
              name: '重复报警识别',
              recommended: ['bar', 'table'],
              snapshotTime: '2026-05-22 09:30',
              fields: ['region', 'duplicate_count', 'duplicate_rate'],
              sql: `SELECT region,\n       COUNT(1) AS duplicate_count,\n       ROUND(COUNT(1) / SUM(COUNT(1)) OVER(), 4) AS duplicate_rate\nFROM alarm_duplicate_result\nGROUP BY region\nORDER BY duplicate_count DESC`,
              rows: [
                { region: '城东', duplicate_count: 126, duplicate_rate: 0.24 },
                { region: '城南', duplicate_count: 96, duplicate_rate: 0.18 },
                { region: '城西', duplicate_count: 82, duplicate_rate: 0.16 }
              ]
            },
            {
              id: 'alarm_quality_check',
              name: '接警质量检查',
              recommended: ['table', 'bar'],
              snapshotTime: '2026-05-22 09:30',
              fields: ['channel', 'total_count', 'missing_count', 'quality_score'],
              sql: `SELECT channel,\n       COUNT(1) AS total_count,\n       SUM(CASE WHEN address IS NULL OR alarm_type IS NULL THEN 1 ELSE 0 END) AS missing_count,\n       ROUND(100 - SUM(CASE WHEN address IS NULL OR alarm_type IS NULL THEN 1 ELSE 0 END) * 100.0 / COUNT(1), 2) AS quality_score\nFROM 110_alarm_core\nGROUP BY channel`,
              rows: [
                { channel: '电话', total_count: 842, missing_count: 12, quality_score: 98.57 },
                { channel: 'APP', total_count: 236, missing_count: 6, quality_score: 97.46 },
                { channel: '平台', total_count: 208, missing_count: 3, quality_score: 98.56 }
              ]
            }
          ]
        },
        {
          name: '处置警情',
          blocks: [
            {
              id: 'dispose_closed_loop',
              name: '处置闭环统计',
              recommended: ['metric', 'pie'],
              snapshotTime: '2026-05-22 09:30',
              fields: ['status', 'case_count'],
              sql: `SELECT status, COUNT(1) AS case_count\nFROM alarm_dispose_flow\nGROUP BY status`,
              rows: [
                { status: '已闭环', case_count: 712 },
                { status: '处理中', case_count: 96 },
                { status: '待反馈', case_count: 34 }
              ]
            }
          ]
        },
        {
          name: '热点警情',
          blocks: [
            {
              id: 'hot_region_distribution',
              name: '热点区域分布',
              recommended: ['bar', 'table'],
              snapshotTime: '2026-05-22 09:30',
              fields: ['region', 'alarm_count', 'trend'],
              sql: `SELECT region, COUNT(1) AS alarm_count, 'up' AS trend\nFROM alarm_hot_region\nGROUP BY region\nORDER BY alarm_count DESC`,
              rows: [
                { region: '中心商圈', alarm_count: 216, trend: 'up' },
                { region: '火车站', alarm_count: 188, trend: 'up' },
                { region: '大学城', alarm_count: 147, trend: 'flat' }
              ]
            }
          ]
        }
      ]
    }
  ]
```

- [ ] **Step 2: Implement config render**

Replace placeholder `renderConfig()` with:

```javascript
  function renderConfig() {
    const widget = selectedWidget()
    const empty = document.getElementById('emptyConfig')
    const panel = document.getElementById('configPanel')
    empty.classList.toggle('hidden', Boolean(widget))
    panel.classList.toggle('hidden', !widget)
    if (!widget) return

    document.getElementById('titleInput').value = widget.title
    document.getElementById('xInput').value = widget.x
    document.getElementById('yInput').value = widget.y
    document.getElementById('wInput').value = widget.w
    document.getElementById('hInput').value = widget.h
    document.getElementById('bgInput').value = widget.style.background
    document.getElementById('colorInput').value = widget.style.color
    renderDataSelectors(widget)
    document.getElementById('sqlInput').value = widget.data.sql
    renderMappingPanel(widget)
    renderDataPreview(widget)
  }

  function renderDataSelectors(widget) {
    const domainSelect = document.getElementById('domainSelect')
    domainSelect.innerHTML = '<option value="">手写 SQL / 不绑定数据块</option>' + themeCatalog.map((item) =>
      `<option value="${item.domain}">${item.domain}</option>`
    ).join('')
    domainSelect.value = widget.data.domain

    const domain = themeCatalog.find((item) => item.domain === widget.data.domain)
    const businessSelect = document.getElementById('businessSelect')
    businessSelect.innerHTML = '<option value="">请选择业务项</option>' + (domain ? domain.businesses.map((item) =>
      `<option value="${item.name}">${item.name}</option>`
    ).join('') : '')
    businessSelect.value = widget.data.business

    const business = domain?.businesses.find((item) => item.name === widget.data.business)
    const blockSelect = document.getElementById('blockSelect')
    blockSelect.innerHTML = '<option value="">请选择 SQL 数据块</option>' + (business ? business.blocks.map((item) =>
      `<option value="${item.id}">${item.name}</option>`
    ).join('') : '')
    blockSelect.value = widget.data.blockId
  }
```

- [ ] **Step 3: Bind basic input changes**

Add:

```javascript
  function bindConfigEvents() {
    const patchSelected = (patch) => {
      const widget = selectedWidget()
      if (!widget) return
      patch(widget)
      renderAll()
    }

    document.getElementById('titleInput').addEventListener('input', (event) => patchSelected((widget) => widget.title = event.target.value))
    document.getElementById('xInput').addEventListener('input', (event) => patchSelected((widget) => widget.x = snap(Number(event.target.value) || 0)))
    document.getElementById('yInput').addEventListener('input', (event) => patchSelected((widget) => widget.y = snap(Number(event.target.value) || 0)))
    document.getElementById('wInput').addEventListener('input', (event) => patchSelected((widget) => widget.w = Math.max(80, snap(Number(event.target.value) || 80))))
    document.getElementById('hInput').addEventListener('input', (event) => patchSelected((widget) => widget.h = Math.max(50, snap(Number(event.target.value) || 50))))
    document.getElementById('bgInput').addEventListener('input', (event) => patchSelected((widget) => widget.style.background = event.target.value))
    document.getElementById('colorInput').addEventListener('input', (event) => patchSelected((widget) => widget.style.color = event.target.value))
    document.getElementById('sqlInput').addEventListener('input', (event) => patchSelected((widget) => widget.data.sql = event.target.value))
  }
```

Call `bindConfigEvents()` during initialization.

- [ ] **Step 4: Bind data block selectors**

Inside `bindConfigEvents()`, add:

```javascript
    document.getElementById('domainSelect').addEventListener('change', (event) => patchSelected((widget) => {
      widget.data.domain = event.target.value
      widget.data.business = ''
      widget.data.blockId = ''
      widget.data.sql = ''
      widget.data.fields = []
      widget.data.previewRows = []
      widget.data.mapping = {}
    }))

    document.getElementById('businessSelect').addEventListener('change', (event) => patchSelected((widget) => {
      widget.data.business = event.target.value
      widget.data.blockId = ''
      widget.data.sql = ''
      widget.data.fields = []
      widget.data.previewRows = []
      widget.data.mapping = {}
    }))

    document.getElementById('blockSelect').addEventListener('change', (event) => patchSelected((widget) => {
      const block = findBlock(event.target.value)
      widget.data.blockId = event.target.value
      widget.data.sql = block?.sql || ''
      widget.data.fields = block?.fields || []
      widget.data.previewRows = []
      widget.data.mapping = {}
    }))
```

Add helper:

```javascript
  function findBlock(blockId) {
    for (const domain of themeCatalog) {
      for (const business of domain.businesses) {
        const block = business.blocks.find((item) => item.id === blockId)
        if (block) return block
      }
    }
    return null
  }
```

- [ ] **Step 5: Manual test config panel**

Expected:

- Selecting a component opens config panel.
- Title, x/y/w/h, colors update selected widget.
- Selecting `警情域 -> 接报警情 -> 接警总量与趋势` fills SQL text area.

---

### Task 5: SQL Preview, Field Mapping, And Component Rendering

**Files:**
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.js`
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.css`

- [ ] **Step 1: Implement mock SQL execution**

Add:

```javascript
  function executeMockSql(widget) {
    const sql = widget.data.sql.trim()
    if (!sql) {
      return { ok: false, message: '请先选择数据块或填写 SQL。' }
    }
    if (/\b(delete|update|insert|drop|truncate|alter|create)\b/i.test(sql)) {
      return { ok: false, message: '原型只允许模拟 SELECT 查询，已拦截变更类 SQL。' }
    }
    const block = findBlock(widget.data.blockId)
    if (block) {
      return { ok: true, message: `执行成功，读取最新快照 ${block.snapshotTime}。`, rows: block.rows, fields: block.fields }
    }
    const rows = [
      { label: '样例 A', value: 128, rate: 0.12 },
      { label: '样例 B', value: 96, rate: 0.08 },
      { label: '样例 C', value: 72, rate: 0.05 }
    ]
    return { ok: true, message: '手写 SQL 模拟执行成功。', rows, fields: Object.keys(rows[0]) }
  }
```

- [ ] **Step 2: Bind execute preview button**

Inside `bindConfigEvents()`, add:

```javascript
    document.getElementById('runSqlBtn').addEventListener('click', () => {
      const widget = selectedWidget()
      if (!widget) return
      const result = executeMockSql(widget)
      const message = document.getElementById('sqlMessage')
      message.textContent = result.message
      message.className = `message ${result.ok ? 'ok' : 'error'}`
      if (!result.ok) return
      widget.data.previewRows = result.rows
      widget.data.fields = result.fields
      widget.data.mapping = inferDefaultMapping(widget)
      renderAll()
      document.getElementById('sqlMessage').textContent = result.message
      document.getElementById('sqlMessage').className = 'message ok'
    })
```

Add:

```javascript
  function inferDefaultMapping(widget) {
    const fields = widget.data.fields
    if (widget.type === 'metric') return { main: fields[1] || fields[0] || '', sub: fields[2] || '', trend: fields[3] || '' }
    if (widget.type === 'line' || widget.type === 'bar') return { x: fields[0] || '', y: fields[1] || '', group: '' }
    if (widget.type === 'pie') return { category: fields[0] || '', value: fields[1] || '' }
    if (widget.type === 'table') return { columns: fields.slice(0, 5), sort: fields[0] || '' }
    return {}
  }
```

- [ ] **Step 3: Render mapping controls**

Add:

```javascript
  function optionList(fields, value) {
    return '<option value="">请选择字段</option>' + fields.map((field) =>
      `<option value="${field}" ${field === value ? 'selected' : ''}>${field}</option>`
    ).join('')
  }

  function renderMappingPanel(widget) {
    const panel = document.getElementById('mappingPanel')
    const fields = widget.data.fields
    if (!fields.length) {
      panel.innerHTML = '<div class="empty-config">执行预览后可配置字段映射</div>'
      return
    }
    if (widget.type === 'metric') {
      panel.innerHTML = `
        <label>主指标字段<select class="select mapping-input" data-map="main">${optionList(fields, widget.data.mapping.main)}</select></label>
        <label>副指标字段<select class="select mapping-input" data-map="sub">${optionList(fields, widget.data.mapping.sub)}</select></label>
        <label>趋势字段<select class="select mapping-input" data-map="trend">${optionList(fields, widget.data.mapping.trend)}</select></label>
      `
    } else if (widget.type === 'line' || widget.type === 'bar') {
      panel.innerHTML = `
        <label>X 轴字段<select class="select mapping-input" data-map="x">${optionList(fields, widget.data.mapping.x)}</select></label>
        <label>Y 轴字段<select class="select mapping-input" data-map="y">${optionList(fields, widget.data.mapping.y)}</select></label>
        <label>分组字段<select class="select mapping-input" data-map="group">${optionList(fields, widget.data.mapping.group)}</select></label>
      `
    } else if (widget.type === 'pie') {
      panel.innerHTML = `
        <label>分类字段<select class="select mapping-input" data-map="category">${optionList(fields, widget.data.mapping.category)}</select></label>
        <label>数值字段<select class="select mapping-input" data-map="value">${optionList(fields, widget.data.mapping.value)}</select></label>
      `
    } else if (widget.type === 'table') {
      panel.innerHTML = fields.map((field) => `
        <label class="checkbox-line"><input type="checkbox" class="mapping-column" value="${field}" ${widget.data.mapping.columns?.includes(field) ? 'checked' : ''} /> ${field}</label>
      `).join('')
    } else {
      panel.innerHTML = '<div class="empty-config">当前组件无需字段映射</div>'
    }
  }
```

- [ ] **Step 4: Bind mapping changes**

In `bindConfigEvents()`, add event delegation:

```javascript
    document.getElementById('mappingPanel').addEventListener('change', (event) => {
      const widget = selectedWidget()
      if (!widget) return
      if (event.target.classList.contains('mapping-input')) {
        widget.data.mapping[event.target.dataset.map] = event.target.value
      }
      if (event.target.classList.contains('mapping-column')) {
        const checked = [...document.querySelectorAll('.mapping-column:checked')].map((item) => item.value)
        widget.data.mapping.columns = checked
      }
      renderAll()
    })
```

- [ ] **Step 5: Render data preview table**

Add:

```javascript
  function renderDataPreview(widget) {
    const box = document.getElementById('dataPreview')
    const rows = widget.data.previewRows
    if (!rows.length) {
      box.innerHTML = '<div class="empty-config">暂无预览数据</div>'
      return
    }
    const fields = Object.keys(rows[0])
    box.innerHTML = `
      <table>
        <thead><tr>${fields.map((field) => `<th>${field}</th>`).join('')}</tr></thead>
        <tbody>${rows.map((row) => `<tr>${fields.map((field) => `<td>${row[field]}</td>`).join('')}</tr>`).join('')}</tbody>
      </table>
    `
  }
```

- [ ] **Step 6: Render chart-like widgets**

Replace `renderWidgetBody(widget)` with:

```javascript
  function renderWidgetBody(widget) {
    if (widget.type === 'text') return `<div class="text-widget">${escapeHtml(widget.title || '文本')}</div>`
    if (widget.type === 'lineDecor') return '<div class="decor-line"></div>'
    if (!widget.data.previewRows.length) return '<div class="empty-config">未配置数据</div>'

    const rows = widget.data.previewRows
    const map = widget.data.mapping
    if (widget.type === 'metric') {
      const row = rows[rows.length - 1] || {}
      return `<div class="metric-render">
        <strong>${escapeHtml(row[map.main] ?? '-')}</strong>
        <span>${escapeHtml(map.main || '主指标')}</span>
        <em>${map.sub ? `${escapeHtml(map.sub)}：${escapeHtml(row[map.sub])}` : '已执行预览'}</em>
      </div>`
    }
    if (widget.type === 'line' || widget.type === 'bar') {
      const max = Math.max(...rows.map((row) => Number(row[map.y]) || 0), 1)
      return `<div class="${widget.type === 'line' ? 'line-render' : 'bar-render'}">
        ${rows.map((row) => {
          const value = Number(row[map.y]) || 0
          const height = Math.max(8, Math.round(value / max * 100))
          return `<div class="bar-item" title="${escapeHtml(row[map.x])}: ${value}">
            <div class="bar-fill" style="height:${height}%"></div>
            <span>${escapeHtml(row[map.x])}</span>
          </div>`
        }).join('')}
      </div>`
    }
    if (widget.type === 'pie') {
      return `<div class="pie-render">
        ${rows.map((row) => `<div><span>${escapeHtml(row[map.category])}</span><strong>${escapeHtml(row[map.value])}</strong></div>`).join('')}
      </div>`
    }
    if (widget.type === 'table') {
      const columns = map.columns?.length ? map.columns : Object.keys(rows[0]).slice(0, 4)
      return `<table class="widget-table">
        <thead><tr>${columns.map((field) => `<th>${field}</th>`).join('')}</tr></thead>
        <tbody>${rows.map((row) => `<tr>${columns.map((field) => `<td>${escapeHtml(row[field])}</td>`).join('')}</tr>`).join('')}</tbody>
      </table>`
    }
    return `<div class="empty-config">已加载 ${rows.length} 行模拟数据</div>`
  }
```

- [ ] **Step 7: Add render CSS**

Append to CSS:

```css
.metric-render { height: 100%; display: grid; align-content: center; gap: 8px; }
.metric-render strong { font-size: 34px; line-height: 1; }
.metric-render span, .metric-render em { color: #9db4d8; font-style: normal; }
.bar-render, .line-render { height: 100%; display: flex; align-items: end; gap: 12px; padding: 8px 4px 0; }
.bar-item { flex: 1; height: 100%; min-width: 32px; display: flex; flex-direction: column; justify-content: end; align-items: center; gap: 6px; color: #9db4d8; font-size: 12px; }
.bar-fill { width: 100%; border-radius: 6px 6px 0 0; background: linear-gradient(180deg, #7eb3ff, #2e6bd5); }
.line-render .bar-fill { border-radius: 999px 999px 0 0; background: linear-gradient(180deg, #66e0c2, #238b77); }
.pie-render { height: 100%; display: grid; align-content: center; gap: 8px; }
.pie-render div { display: flex; justify-content: space-between; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 6px; }
.widget-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.widget-table th, .widget-table td { padding: 6px; border-bottom: 1px solid rgba(255,255,255,0.12); text-align: left; }
.text-widget { height: 100%; display: grid; place-items: center; font-size: 28px; font-weight: 800; }
.decor-line { height: 4px; margin-top: 14px; background: linear-gradient(90deg, transparent, #73a7ff, transparent); }
.checkbox-line { display: flex !important; grid-template-columns: none !important; align-items: center; gap: 8px !important; margin-bottom: 6px !important; }
```

- [ ] **Step 8: Manual test data closed loop**

Expected:

- Add line chart.
- Select `警情域 -> 接报警情 -> 接警总量与趋势`.
- SQL appears.
- Click execute preview.
- Data preview table appears.
- Mapping controls appear.
- Chart renders bars/line-like visualization.
- Changing X/Y mapping changes component display.

---

### Task 6: Toolbar Actions, Persistence, Keyboard, Preview Mode

**Files:**
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.js`

- [ ] **Step 1: Add action helpers**

Add:

```javascript
  const STORAGE_KEY = 'big-screen-designer-theme-library-layout-v1'

  function normalizeZ() {
    state.widgets.sort((a, b) => a.z - b.z).forEach((widget, index) => widget.z = index + 1)
  }

  function deleteSelected() {
    if (!state.selectedId) return
    state.widgets = state.widgets.filter((item) => item.id !== state.selectedId)
    state.selectedId = null
    renderAll()
  }

  function copySelected() {
    const widget = selectedWidget()
    if (!widget) return
    const clone = JSON.parse(JSON.stringify(widget))
    clone.id = uid()
    clone.x = snap(clone.x + 30)
    clone.y = snap(clone.y + 30)
    clone.z = state.widgets.length + 1
    state.widgets.push(clone)
    state.selectedId = clone.id
    renderAll()
  }

  function bringFront() {
    const widget = selectedWidget()
    if (!widget) return
    widget.z = Math.max(...state.widgets.map((item) => item.z), 0) + 1
    normalizeZ()
    renderAll()
  }

  function sendBack() {
    const widget = selectedWidget()
    if (!widget) return
    widget.z = 0
    normalizeZ()
    renderAll()
  }
```

- [ ] **Step 2: Bind toolbar and component action buttons**

Add:

```javascript
  function bindToolbarEvents() {
    document.getElementById('zoomSelect').addEventListener('change', (event) => {
      state.zoom = Number(event.target.value)
      renderAll()
    })
    document.getElementById('saveLayoutBtn').addEventListener('click', () => {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ widgets: state.widgets }))
      alert('布局已保存到本地浏览器。')
    })
    document.getElementById('loadLayoutBtn').addEventListener('click', () => {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) {
        alert('没有找到已保存的布局。')
        return
      }
      const data = JSON.parse(raw)
      state.widgets = Array.isArray(data.widgets) ? data.widgets : []
      state.selectedId = state.widgets[0]?.id || null
      renderAll()
    })
    document.getElementById('newCanvasBtn').addEventListener('click', () => {
      state.widgets = []
      state.selectedId = null
      renderAll()
    })
    document.getElementById('clearCanvasBtn').addEventListener('click', () => {
      if (!confirm('确认清空当前画布吗？此操作不会自动删除已保存布局。')) return
      state.widgets = []
      state.selectedId = null
      renderAll()
    })
    document.getElementById('previewBtn').addEventListener('click', enterPreviewMode)
    document.getElementById('exitPreviewBtn').addEventListener('click', exitPreviewMode)
    document.getElementById('deleteBtn').addEventListener('click', deleteSelected)
    document.getElementById('copyBtn').addEventListener('click', copySelected)
    document.getElementById('bringFrontBtn').addEventListener('click', bringFront)
    document.getElementById('sendBackBtn').addEventListener('click', sendBack)
  }
```

Call `bindToolbarEvents()` during initialization.

- [ ] **Step 3: Add preview mode**

Add:

```javascript
  function enterPreviewMode() {
    document.body.classList.add('preview-mode')
    document.getElementById('exitPreviewBtn').classList.remove('hidden')
    state.selectedId = null
    const scale = Math.min(window.innerWidth / 1920, window.innerHeight / 1080)
    document.documentElement.style.setProperty('--preview-scale', String(scale))
    renderAll()
  }

  function exitPreviewMode() {
    document.body.classList.remove('preview-mode')
    document.getElementById('exitPreviewBtn').classList.add('hidden')
    document.documentElement.style.removeProperty('--preview-scale')
    renderAll()
  }
```

- [ ] **Step 4: Add keyboard delete**

Add:

```javascript
  function bindKeyboardEvents() {
    window.addEventListener('keydown', (event) => {
      if (event.key !== 'Delete' && event.key !== 'Backspace') return
      const tag = document.activeElement?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
      deleteSelected()
    })
  }
```

Call `bindKeyboardEvents()` during initialization.

- [ ] **Step 5: Manual test persistence and preview**

Expected:

- Save layout shows success alert.
- Refresh page, click load layout, widgets return.
- Preview mode hides panels.
- Exit preview returns editor.
- Delete key removes selected widget.
- Copy creates a second widget offset by 30px.
- Clear canvas asks for confirmation.

---

### Task 7: README, QA Script, Test Report

**Files:**
- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/README.md`
- Create: `/Users/luwang/bigdata-build/dolphinscheduler/tmp_big_screen_designer_qa.js`
- Create: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/test-report.md`

- [ ] **Step 1: Update README with final scope**

Replace README with:

```markdown
# 大屏设计器独立 HTML 原型

## 定位

这是第一阶段独立原型，不修改 DolphinScheduler 二开代码，不连接真实数据库。

核心体验：

- 从空白画布开始拖组件。
- 主题库 / 警情域 / SQL 数据块作为右侧可绑定数据来源。
- 支持移动、缩放、选中、复制、删除、置顶、置底。
- 支持 SQL 副本编辑、模拟执行、字段映射、数据预览。
- 支持 localStorage 保存和加载布局。
- 支持预览模式。

## 启动

```bash
cd /Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype
python3 -m http.server 8032
```

访问：

http://127.0.0.1:8032/big-screen-designer.html

## 已知限制

- SQL 执行为前端模拟，不访问真实数据库。
- 图表为轻量 CSS/HTML 渲染，不是生产级图表库。
- 保存只保存在当前浏览器 localStorage。
- 暂不支持多大屏、多用户、发布审批、权限。
```

- [ ] **Step 2: Create Puppeteer QA script**

Write `/Users/luwang/bigdata-build/dolphinscheduler/tmp_big_screen_designer_qa.js`:

```javascript
const path = require('path')
const puppeteer = require(path.join(
  process.env.HOME,
  '.cache',
  'codex-puppeteer-sync',
  'node_modules',
  'puppeteer-core'
))

const BASE = 'http://127.0.0.1:8032/big-screen-designer.html'
const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function dragPaletteToCanvas(page, name, x, y) {
  const item = await page.evaluateHandle((name) => {
    return [...document.querySelectorAll('.component-item')]
      .find((el) => (el.innerText || '').includes(name))
  }, name)
  const itemBox = await item.asElement().boundingBox()
  const canvasBox = await (await page.$('#canvas')).boundingBox()
  await page.mouse.move(itemBox.x + itemBox.width / 2, itemBox.y + itemBox.height / 2)
  await page.mouse.down()
  await page.mouse.move(canvasBox.x + x, canvasBox.y + y, { steps: 12 })
  await page.mouse.up()
  await wait(300)
}

async function main() {
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: true,
    args: ['--no-sandbox', '--disable-gpu'],
    defaultViewport: { width: 1600, height: 1000 }
  })
  const page = await browser.newPage()
  page.setDefaultTimeout(20000)
  const errors = []
  page.on('pageerror', (err) => errors.push(err.message))
  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push(msg.text())
  })

  await page.goto(BASE, { waitUntil: 'networkidle2' })
  await dragPaletteToCanvas(page, '折线图', 360, 220)

  await page.select('#domainSelect', '警情域')
  await page.select('#businessSelect', '接报警情')
  await page.select('#blockSelect', 'alarm_receive_total_trend')
  await page.click('#runSqlBtn')
  await page.waitForFunction(() => document.body.innerText.includes('执行成功'))

  const afterPreview = await page.evaluate(() => ({
    widgets: document.querySelectorAll('.screen-widget').length,
    hasSql: document.querySelector('#sqlInput').value.includes('110_alarm_core'),
    hasPreviewRows: document.querySelector('#dataPreview').innerText.includes('alarm_count'),
    hasRenderedChart: document.querySelector('.bar-fill') !== null
  }))

  await page.click('#saveLayoutBtn')
  await wait(300)
  page.on('dialog', async (dialog) => dialog.accept())
  await page.reload({ waitUntil: 'networkidle2' })
  await page.click('#loadLayoutBtn')
  await wait(500)

  const afterLoad = await page.evaluate(() => ({
    widgets: document.querySelectorAll('.screen-widget').length,
    hasBoundLabel: document.body.innerText.includes('已绑定')
  }))

  await page.click('#previewBtn')
  await wait(500)
  const preview = await page.evaluate(() => ({
    previewMode: document.body.classList.contains('preview-mode'),
    exitVisible: !document.querySelector('#exitPreviewBtn').classList.contains('hidden')
  }))
  await page.click('#exitPreviewBtn')

  await page.screenshot({ path: '/tmp/big-screen-designer-qa.png', fullPage: true })
  await browser.close()

  if (errors.length) throw new Error(errors.join('\\n'))
  if (!afterPreview.widgets || !afterPreview.hasSql || !afterPreview.hasPreviewRows || !afterPreview.hasRenderedChart) {
    throw new Error(`preview check failed: ${JSON.stringify(afterPreview)}`)
  }
  if (!afterLoad.widgets || !afterLoad.hasBoundLabel) {
    throw new Error(`load check failed: ${JSON.stringify(afterLoad)}`)
  }
  if (!preview.previewMode || !preview.exitVisible) {
    throw new Error(`preview mode check failed: ${JSON.stringify(preview)}`)
  }

  console.log(JSON.stringify({
    ok: true,
    afterPreview,
    afterLoad,
    preview,
    screenshot: '/tmp/big-screen-designer-qa.png'
  }, null, 2))
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
```

- [ ] **Step 3: Run QA**

Start server:

```bash
cd /Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype
python3 -m http.server 8032
```

Run:

```bash
node /Users/luwang/bigdata-build/dolphinscheduler/tmp_big_screen_designer_qa.js
```

Expected output includes:

```json
{
  "ok": true
}
```

- [ ] **Step 4: Write test report**

Create `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/test-report.md`:

```markdown
# 大屏设计器原型测试报告

日期：2026-05-22

## 测试环境

- 原型地址：http://127.0.0.1:8032/big-screen-designer.html
- 浏览器自动化：Puppeteer + Google Chrome

## 已验证功能

- 打开独立 HTML 原型。
- 从左侧拖拽折线图到空白画布。
- 右侧选择 `警情域 -> 接报警情 -> 接警总量与趋势`。
- SQL 自动带出。
- 执行模拟 SQL 预览。
- 数据预览表出现。
- 字段映射生成默认值。
- 画布组件刷新为图表展示。
- 保存布局到 localStorage。
- 刷新页面后加载布局。
- 进入预览模式。
- 退出预览模式。

## 截图

- `/tmp/big-screen-designer-qa.png`

## 结论

第一版独立 HTML 原型满足“空白画布 + 主题库数据块绑定 + SQL 预览 + 字段映射 + 保存 + 预览模式”的核心闭环。
```

- [ ] **Step 5: Final manual check**

Open the prototype manually and verify:

- Component movement feels acceptable.
- Resize handle is visible and usable.
- Right panel labels are readable.
- The business wording uses “主题库 / 警情域”.
- No “数据同步运维大屏” wording appears.

---

## Self-Review Checklist

- [ ] Spec coverage: blank canvas, component drag, move, resize, right-side data binding, SQL preview, field mapping, save/load, preview mode, no DS code changes.
- [ ] Placeholder scan: no `TODO`, `TBD`, or vague “implement later” steps.
- [ ] Type consistency: widget fields are consistently `id/type/title/x/y/w/h/z/style/data`.
- [ ] Commands are exact and paths are absolute.
- [ ] Testing includes browser automation and manual check.

## Execution Notes

The current `/Users/luwang/bigdata-build/dolphinscheduler` directory is not a git repository, so commit steps are not executable here. The implementer should still keep changes scoped to:

- `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/`
- `/Users/luwang/bigdata-build/dolphinscheduler/tmp_big_screen_designer_qa.js`

