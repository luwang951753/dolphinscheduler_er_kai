# 大屏设计器新标签页交接上下文

本文用于把“大屏设计器”任务交给 Codex 的另一个标签页继续推进。当前标签页继续作为“主题库设计”主线，不在这里继续展开大屏设计器。

## 背景

用户正在做 DolphinScheduler 二开，前期围绕“主题库”设计过业务主题、业务项、分析场景、SQL 数据板块等原型。后来发现“主题库 + BI 大屏设计器”混在一起会让产品边界变乱，因此用户明确调整方向：

- 不再围绕主题库转 BI 大屏设计器。
- 独立规划一个“大屏设计器”。
- 后期再基于“大屏设计器”的能力去做主题库页面。
- 当前优先选择 A：先做独立 HTML 原型体验版。

## 用户偏好

- 用户使用中文沟通，明确不希望 Codex 输出英文解释。
- 用户很重视可视化原型，需要能在浏览器中体验。
- 用户对抽象概念不耐烦，偏好具体页面、具体交互、具体数据样例。
- 用户目前不希望直接影响 DolphinScheduler 二开程序。
- 需要先按 `superpowers:brainstorming` 做设计，再写文档，再得到确认，最后才能进入实施。

## 当前决策

大屏设计器应作为独立产品能力来规划，不依赖主题库。第一阶段目标是一个可点击、可拖拽体验的独立 HTML 原型，建议位置：

`/Users/luwang/bigdata-build/data-preview-design-repo/.ai/big-screen-designer/prototype/`

不要先改这些 DolphinScheduler 文件：

- `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/theme-library/index.tsx`
- `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/theme-library/index.module.scss`

## 已知相关文件

已有主题库原型：

- `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/theme-library/prototype/theme-library-prototype.html`
- `/Users/luwang/bigdata-build/data-preview-design-repo/.ai/theme-library/prototype/theme-library-sql-blocks-prototype.html`

已有 DolphinScheduler 主题库页面：

- `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/theme-library/index.tsx`
- `/Users/luwang/bigdata-build/dolphinscheduler/dolphinscheduler-ui/src/views/theme-library/index.module.scss`

已有 SQL/数据预览相关设计文档可参考：

- `/Users/luwang/bigdata-build/dolphinscheduler/docs/superpowers/specs/2026-05-20-data-preview-sql-query-design.md`
- `/Users/luwang/bigdata-build/dolphinscheduler/docs/superpowers/specs/2026-05-21-data-governance-module-design.md`

## 大屏设计器第一版建议范围

第一版原型建议只验证核心体验：

1. 左侧组件库：指标卡、表格、柱状图、折线图、饼图、地图占位、文本、图片、装饰线。
2. 中间画布：支持组件摆放、选中、移动、缩放、对齐参考线或网格。
3. 右侧配置：基础属性、样式、数据源、SQL、字段映射。
4. SQL 编辑：每个组件都能绑定一个数据源和一段 SQL。
5. 数据预览：执行 SQL 后用模拟结果刷新组件展示。
6. 保存布局：前端本地状态保存即可，不接真实后端。
7. 预览模式：隐藏编辑面板，查看大屏效果。

第一版不要做：

- 不要接真实数据库。
- 不要做复杂权限。
- 不要做完整发布流程。
- 不要把它塞回主题库页面。
- 不要先做后端建表。

## 候选成熟产品参考

可参考以下产品的大屏/BI/可视化设计器能力：

1. 阿里云 DataV
2. 阿里云 Quick BI
3. 帆软 FineBI / FineReport
4. DataEase
5. Grafana
6. Apache Superset
7. Metabase
8. Tableau
9. Microsoft Power BI
10. Kibana

重点不是复制视觉风格，而是参考这些交互：

- 拖拽组件到画布。
- 组件自由移动和缩放。
- 选中组件后右侧配置属性。
- 组件绑定数据集、SQL 或查询。
- 图表字段映射。
- 编辑态和预览态分离。
- 支持画布分辨率、背景、网格、吸附。

## 新标签页推荐启动提示词

把下面这段话粘贴到新的 Codex 标签页：

```text
请你接手“大屏设计器”任务。请先阅读交接文件：

/Users/luwang/bigdata-build/dolphinscheduler/docs/superpowers/specs/2026-05-22-big-screen-designer-handoff.md

背景：我正在做 DolphinScheduler 二开，但这个新标签页只负责“大屏设计器”，不要继续围绕主题库展开。当前目标是先按 superpowers:brainstorming 做设计，然后做一个独立 HTML 原型体验版，暂时不要改 DolphinScheduler 二开代码。

请全程用中文。先不要写代码，先继续 brainstorming，规划大屏设计器的原型和实现方式。第一阶段我倾向于独立 HTML 原型，能体验组件拖拽、缩放、右侧 SQL 配置、字段映射、数据预览、保存布局和预览模式。
```

## 当前标签页后续边界

当前标签页继续做“主题库设计”。如需引用大屏能力，只把它当成未来可嵌入的能力，不在本线程详细展开大屏设计器。
