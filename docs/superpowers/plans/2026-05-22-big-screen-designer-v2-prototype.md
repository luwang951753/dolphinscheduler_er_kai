# 大屏设计器 V2 原型 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有独立 HTML 大屏设计器升级为成熟设计器 MVP，支持组件 / 图层 / 数据 / 模板四类左侧面板、元素化画布、数据集层、右侧分组配置、发布生命周期入口和模拟人工点击测试。

**Architecture:** 继续使用独立 HTML/CSS/JS 原型，不改 DolphinScheduler 二开代码。前端用统一 widget 模型和 dataset mock 模型驱动渲染，所有交互保存在内存和 localStorage，QA 使用现有 Chrome CDP 风格脚本模拟点击。

**Tech Stack:** HTML, CSS, Vanilla JavaScript, localStorage, SVG/CSS charts, Chrome CDP QA script.

---

## File Structure

- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.html`
  - 增加顶部生命周期按钮。
  - 左侧改为四个 Tab：组件、图层、数据、模板。
  - 右侧配置改为分组/Tab：基础、数据、字段、样式、交互。
  - 增加通用弹窗。

- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.css`
  - 新增成熟设计器布局、左侧 Tab、图层列表、数据集卡片、模板卡片、画布选中/锁定/隐藏样式、右侧配置分组、弹窗样式。

- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/big-screen-designer.js`
  - 扩展组件类型。
  - 引入 dataset 模型。
  - 将 widget 模型升级为 id/type/name/x/y/width/height/zIndex/locked/visible/dataBinding/fieldMapping/style/interactions/previewRows。
  - 实现左侧 Tab、图层操作、数据集选择、模板入口、生命周期弹窗、筛选器联动 mock。

- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/README.md`
  - 更新 V2 原型说明。

- Modify: `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/test-report.md`
  - 更新测试结果。

- Create/Modify: `/Users/luwang/bigdata-build/dolphinscheduler/tmp_big_screen_designer_qa.py`
  - 使用 Chrome DevTools Protocol 执行模拟人工点击测试。

## Tasks

### Task 1: Upgrade Shell To Mature Designer Layout

- [ ] 修改 HTML 顶部工具栏，加入保存草稿、发布、版本历史、权限、分享、导出、撤销、重做、预览、全屏播放。
- [ ] 修改左侧为 Tab 容器，提供组件、图层、数据、模板四个面板。
- [ ] 修改右侧为配置分组容器，提供基础、数据、字段、样式、交互。
- [ ] 增加通用弹窗节点，用于发布/版本/权限/分享/导出。
- [ ] 运行静态检查：`node --check big-screen-designer.js`。

### Task 2: Upgrade CSS Visual System

- [ ] 添加左侧 Tab、组件卡片、图层行、数据集卡片、模板卡片样式。
- [ ] 添加画布选中、锁定、隐藏、网格、吸附提示、resize handle 样式。
- [ ] 添加右侧配置分组、开关、字段映射、多选、弹窗样式。
- [ ] 添加预览模式和全屏播放模式样式。

### Task 3: Upgrade Data Model And Rendering

- [ ] 将组件类型扩展为文本、图片、容器、指标卡、折线图、柱状图、饼图、地图占位、表格、时间筛选器、下拉筛选器、装饰线。
- [ ] 引入 mock 数据源、数据集、维度和指标模型。
- [ ] 升级 widget 创建逻辑，使用 `name/x/y/width/height/zIndex/locked/visible/dataBinding/fieldMapping/style/interactions/previewRows`。
- [ ] 渲染图层列表和数据集列表。
- [ ] 隐藏元素不在画布渲染，但仍在图层展示。
- [ ] 锁定元素不可拖动和缩放。

### Task 4: Right Panel Configuration

- [ ] 基础配置支持名称、位置尺寸、zIndex、显示、锁定。
- [ ] 数据配置支持数据源、数据集、SQL、过滤条件、刷新频率。
- [ ] 字段配置支持维度、指标、系列、聚合、排序、最大条数。
- [ ] 样式配置支持背景、文字、边框、圆角、字体、主题皮肤。
- [ ] 交互配置支持点击联动、目标组件、下钻、参数传递 mock。
- [ ] 任意配置变化后立刻刷新画布和图层。

### Task 5: Dataset Preview And Chart Mapping

- [ ] 执行预览返回当前数据集 mock rows。
- [ ] 字段映射根据组件类型动态变化。
- [ ] 指标卡、折线图、柱状图、饼图、表格按字段映射真实渲染。
- [ ] 筛选器改变值后，目标组件显示参数接收提示并刷新 mock 数据。

### Task 6: Lifecycle And Persistence

- [ ] 保存草稿到 localStorage。
- [ ] 加载草稿恢复 widget、数据绑定、字段映射、样式和交互。
- [ ] 发布弹窗展示播放地址占位。
- [ ] 版本历史弹窗展示版本记录。
- [ ] 权限弹窗展示查看/编辑权限占位。
- [ ] 分享弹窗展示分享链接占位。
- [ ] 预览和全屏播放进入只读状态。

### Task 7: QA And Report

- [ ] 编写或更新 `/Users/luwang/bigdata-build/dolphinscheduler/tmp_big_screen_designer_qa.py`。
- [ ] 测试左侧四个 Tab 切换。
- [ ] 测试拖入指标卡、柱状图、筛选器。
- [ ] 测试组件选中、移动、缩放、锁定、隐藏、复制、删除。
- [ ] 测试数据集选择、执行预览、字段映射、样式修改。
- [ ] 测试保存/加载、发布/版本/权限/分享弹窗、预览模式。
- [ ] 生成截图和 `test-report.md`。
