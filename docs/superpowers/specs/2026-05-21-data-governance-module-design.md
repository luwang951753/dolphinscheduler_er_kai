# 数据治理模块二开设计说明

日期：2026-05-21

## 背景

当前 DolphinScheduler 二开已经覆盖数据预览、同步任务、离线安装向导等能力，但数据同步任务运行后缺少统一的治理入口。用户需要知道哪些表已经接入、表字段和责任信息是什么、质量规则是否通过、同步任务形成了哪些上下游关系，以及问题是否已经处理。

本设计基于已确认的数据治理原型：

`/Users/luwang/bigdata-build/dolphinscheduler/.ai/data-governance/prototype/data-governance-workbench.html`

目标是把原型落到 DolphinScheduler 真实模块中，不使用 iframe，不做孤立静态页。

## 设计目标

第一版目标是轻量内置数据治理，不引入 OpenMetadata、DataHub 等独立治理平台。

必须实现：

- 在 DolphinScheduler 中新增“数据治理”菜单和真实页面。
- 从已配置数据源、数据预览元数据、同步任务记录中组织数据资产。
- 支持资产列表、表行展开详情、字段、质量规则、血缘、问题闭环。
- 支持新建质量规则弹框，规则 SQL 可自动生成且可人工修改。
- 支持规则试运行，试运行使用当前 SQL 内容。
- 支持保存规则后回显在当前资产的“质量”Tab。
- 支持点击式业务验证，覆盖资产展开、Tab 切换、规则创建、SQL 编辑、试运行和问题状态变更。

不做：

- 不引入独立数据治理服务。
- 不做复杂图谱引擎。
- 不做权限体系重构。
- 不做质量规则调度引擎的完整生产化闭环，第一版保留手动检测和同步任务后触发的扩展点。

## 方案选择

采用“Dolphin 内置治理模块 + 轻量后端 API + 本地持久化”的方案。

### 为什么不只做前端

纯前端实现会再次出现静态数据、刷新丢失、编辑回显不完整的问题。数据治理涉及规则、问题、标签、血缘等可维护对象，必须进入后端 API 和持久化层。

### 为什么不集成 OpenMetadata

OpenMetadata 功能强，但组件较多，部署和运维成本高。当前用户明确希望减少外部系统整合，因此第一版参考其信息架构，不直接引入其服务。

## 信息架构

顶层菜单：

```text
数据治理
└── 治理工作台
    ├── 指标概览
    ├── 资产目录
    └── 问题列表
```

资产目录采用表格行内展开，不使用右侧详情抽屉：

```text
资产行
└── 展开详情
    ├── 概览
    ├── 字段
    ├── 质量
    ├── 血缘
    └── 问题
```

展开区顶部只展示资产身份信息：资产名、完整路径、质量状态、标签、编辑元数据入口。Owner、字段数、最近检测、最近同步放入“概览”Tab。

## 前端设计

新增前端模块：

- `dolphinscheduler-ui/src/router/modules/data-governance.ts`
- `dolphinscheduler-ui/src/views/data-governance/index.tsx`
- `dolphinscheduler-ui/src/views/data-governance/index.module.scss`
- `dolphinscheduler-ui/src/service/modules/data-governance/index.ts`

菜单文案加入：

- `dolphinscheduler-ui/src/locales/zh_CN/menu.ts`
- `dolphinscheduler-ui/src/locales/en_US/menu.ts`

页面组件拆分建议：

- `GovernanceWorkbench`：页面容器、筛选、指标、资产表。
- `AssetExpandedPanel`：行内展开区，负责 Tab 与资产详情。
- `AssetOverviewTab`：Owner、字段数、最近检测、最近同步、说明、标签。
- `AssetFieldsTab`：字段名、类型、注释、主键、可空、敏感标签。
- `QualityRulesTab`：规则列表、最近结果、异常数、启用状态。
- `QualityRuleModal`：新建/编辑规则弹框。
- `LineageTab`：轻量上游、当前表、下游关系。
- `IssuesTab`：问题列表与状态变更。

交互要求：

- 点击资产行展开，再次点击收起。
- 展开后默认进入“概览”Tab。
- 新建质量规则必须在当前资产上下文内打开弹框。
- 保存规则后关闭弹框并定位到当前资产“质量”Tab。
- 检测 SQL 使用 Tab 方式展示“规则预览与试运行”和“检测 SQL”。
- 用户手动修改 SQL 后进入“手工编辑”状态，规则条件变化不能静默覆盖 SQL。
- 点击“重新生成 SQL”才按当前规则条件覆盖 SQL。

## 后端设计

新增后端接口分组：

```text
/data-governance/assets
/data-governance/assets/{assetId}
/data-governance/assets/{assetId}/fields
/data-governance/assets/{assetId}/rules
/data-governance/rules/{ruleId}/trial-run
/data-governance/assets/{assetId}/lineage
/data-governance/assets/{assetId}/issues
```

建议新增文件：

- `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/controller/DataGovernanceController.java`
- `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/DataGovernanceService.java`
- `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/impl/DataGovernanceServiceImpl.java`
- `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/dto/datagovernance/*`

第一版 API 行为：

- 资产列表：从 Dolphin 数据源元数据、数据预览已发现对象、同步任务记录中聚合。
- 字段列表：复用数据预览表结构能力读取真实字段。
- 元数据编辑：保存 Owner、标签、说明。
- 质量规则：保存规则类型、字段、条件、执行策略、当前 SQL、启用状态。
- 试运行：校验 SQL 安全性后，在对应数据源只读执行统计 SQL。
- 血缘：优先读取同步任务保存的源表和目标表关系；没有关系时展示空状态。
- 问题：质量试运行失败后生成或更新问题记录，支持状态变更。

## 持久化设计

第一版需要持久化以下对象：

- 资产人工元数据：资产 ID、说明、Owner、标签。
- 质量规则：规则名称、类型、字段、条件、范围、阈值、样本保存策略、SQL、状态。
- 质量检测结果：规则 ID、检测时间、通过状态、异常数、异常率、样本摘要。
- 治理问题：资产 ID、规则 ID、严重程度、状态、发现时间、最近处理时间。
- 血缘关系：源资产、目标资产、来源同步任务、最近运行状态。

若当前 Dolphin 二开没有合适的数据库迁移机制，第一版可以先采用后端内置表初始化或现有持久化方式，但不能只存在前端 localStorage。

## 质量规则 SQL

系统生成 SQL 分三类：

- 异常统计 SQL：必须返回 `abnormal_count` 和 `abnormal_rate`。
- 异常样本 SQL：受“样本保存”控制，不保存、前 50 条、前 200 条。
- 失败判定 SQL：受失败阈值控制，例如异常行数大于 0、异常率大于 1%。

安全要求：

- 禁止 `delete`、`update`、`insert`、`drop`、`truncate`、`alter`、`create` 等变更类语句。
- 自定义 SQL 必须返回 `abnormal_count`。
- 试运行和保存均以 SQL 编辑区当前内容为准。
- SQL 校验失败时，前端展示可理解错误，不保存规则。

## 数据血缘

第一版血缘为轻量链路，不做复杂画布：

```text
上游表 -> 当前表 -> 下游表
```

同步任务是血缘生产方，数据治理是血缘展示和管理方。同步任务保存或执行成功后，后续应写入 `源表 -> 目标表` 关系。第一版如果同步任务侧还未改造，则治理模块先通过已有同步任务配置解析可识别关系，并提供空状态。

## Docker 和运行环境

二开完成后的自测需要启动：

- Docker Desktop 或本机 Docker。
- MySQL 容器。
- PostgreSQL 容器。
- DolphinScheduler 后端。
- DolphinScheduler 前端。

测试数据应至少包含：

- MySQL 源表。
- PostgreSQL 目标表。
- 至少一个已配置数据源。
- 至少一个同步任务产生的血缘关系或可展示空状态。
- 至少一条质量规则和一条失败问题。

## 测试设计

必须进行模拟人工点击测试，不以接口通为完成标准。

前端点击测试覆盖：

- 打开 Dolphin 登录页并登录。
- 进入“数据治理”菜单。
- 查看资产列表真实加载。
- 搜索、筛选资产。
- 点击资产行展开详情。
- 切换“概览、字段、质量、血缘、问题”Tab。
- 新建非空质量规则。
- 查看系统生成 SQL。
- 手动修改 SQL，确认进入手工编辑状态。
- 点击试运行，看到成功或可理解失败信息。
- 保存规则后，规则出现在当前资产“质量”Tab。
- 切换到问题 Tab，验证问题状态可变更。
- 刷新页面后，规则和元数据仍能回显。

后端测试覆盖：

- 资产 API 返回真实数据或明确空状态。
- 字段 API 可读取 MySQL 和 PostgreSQL 字段。
- 规则保存和查询一致。
- SQL 安全校验能拦截变更类语句。
- 试运行失败时返回可理解错误。
- 问题状态变更可持久化。

回归测试覆盖：

- 数据预览页面仍可打开。
- 同步任务页面仍可打开。
- 登录态和菜单权限不受影响。

## 成功标准

- 数据治理不是静态原型，而是 Dolphin 内部真实模块。
- 页面风格与现有 Dolphin 二开一致。
- 资产、字段、规则、问题至少有一类数据来自后端真实 API。
- 质量规则保存后能回显，刷新不丢失。
- SQL 编辑与重新生成逻辑符合原型设计。
- 完成模拟人工点击测试，并输出测试结果。

## 风险与处理

- 如果本地 Docker 未启动，先启动 Docker，再拉起 MySQL 和 PostgreSQL。
- 如果本地数据库端口冲突，复用已有容器或选择空闲端口。
- 如果 Dolphin 后端启动失败，先查看日志定位，再修复启动问题。
- 如果真实同步任务血缘不可解析，第一版先展示空状态和治理侧预留接口，不能伪造静态血缘。
- 如果当前仓库无 Git 元数据，规格文档无法提交，只记录未提交原因。
