---
acp-version: "3.9"
file-type: index
domain: root
module: root
created: 2026-05-09
updated: 2026-05-18
inherit: none
---

# DolphinScheduler 二开 ACP 路由表

## 项目定位

本项目是在 DolphinScheduler 源码基础上进行二次开发，目标是构建更成熟的数据同步与调度平台。当前主线是以 DolphinScheduler 为平台底座，以 SeaTunnel 作为数据同步执行组件，在现有菜单和工作流体系内新增“同步任务”能力。

## 当前代码位置

| 类型 | 路径 |
|------|------|
| 主要二开目录 | `/Users/luwang/bigdata-build/dolphinscheduler` |
| 备份/另一份源码目录 | `/Users/luwang/大数据平台/dolphinscheduler` |
| SeaTunnel 源码目录 | `/Users/luwang/bigdata-build/seatunnel` |
| SeaTunnel 运行包 | `/Users/luwang/大数据平台/runtime/seatunnel/apache-seatunnel-2.3.3` |

## 模块索引

| 域 | 模块 | 文件 | 说明 |
|----|------|------|------|
| platform | core | `platform/core/desc.md` | 平台二开边界、总体目标、源码保护原则 |
| platform | runtime-integration | `platform/runtime-integration/desc.md` | DolphinScheduler 与 SeaTunnel 运行集成 |
| data-preview | core | `data-preview/core/req.md` | 数据预览模块需求开发文档 |
| data-preview | ui-wizard | `data-preview/ui-wizard/ui.md` | 数据预览页面原型说明文档 |
| data-preview | test | `data-preview/test/test-cases.md` | 数据预览模块测试用例基线文档 |
| data-preview | prototype | `data-preview/prototype/data-preview-prototype.html` | 数据预览网页原型 V1 |
| sync-task | core | `sync-task/core/desc.md` | 同步任务模块总览 |
| sync-task | product-problem-list | `sync-task/product/problem-list.md` | 同步任务需求问题清单、已确认决策和待确认事项 |
| sync-task | product-prd | `sync-task/product/prd.md` | 同步任务维护型产品 PRD |
| sync-task | interaction-design | `sync-task/product/interaction-design.md` | 同步任务页面交互设计细则 |
| sync-task | ui-wizard | `sync-task/ui-wizard/ui.md` | 四步向导、页面交互、产品设计要求 |
| sync-task | backend-api | `sync-task/backend-api/tech.md` | 后端接口、工作流创建、建表接口 |
| sync-task | field-mapping | `sync-task/field-mapping/desc.md` | 字段勾选、字段映射、拖拽连线、类型转换 |
| sync-task | schedule-run | `sync-task/schedule-run/desc.md` | 立即执行、周期调度、保存并执行 |
| sync-task | agent | `sync-task/agent/req.md` | 自然语言创建同步任务 Agent |
| sync-task | test-matrix | `sync-task/test/test-matrix.md` | 同步任务需求到测试用例的覆盖矩阵 |
| data-governance | core | `data-governance/core/req.md` | 数据治理模块需求开发文档 |
| data-governance | product-prd | `data-governance/product/prd.md` | 轻量 OpenMetadata 风格数据治理 PRD |
| data-governance | ui-wizard | `data-governance/ui-wizard/ui.md` | 数据治理页面原型说明 |
| data-governance | test | `data-governance/test/test-cases.md` | 数据治理测试用例基线 |
| data-governance | prototype | `data-governance/prototype/data-governance-workbench.html` | 数据治理工作台 HTML 原型 |
| offline-installer | core | `offline-installer/core/req.md` | 内网离线安装向导需求 |
| offline-installer | core-tech | `offline-installer/core/tech.md` | 离线安装向导后端架构、写入白名单、安装流程 |
| offline-installer | core-api | `offline-installer/core/api.md` | 离线安装向导接口契约 |
| offline-installer | product-prd | `offline-installer/product/prd.md` | 离线安装向导产品 PRD |
| offline-installer | ui-wizard | `offline-installer/ui-wizard/ui.md` | 离线安装向导页面交互说明 |
| offline-installer | prototype | `offline-installer/prototype/offline-installer-prototype.html` | 离线安装向导网页原型 |
| offline-installer | test | `offline-installer/test/test-cases.md` | 离线安装向导测试用例 |
| offline-installer | dev-plan | `offline-installer/dev/implementation-plan.md` | 离线安装向导开发实施计划 |
| quality | e2e-click-testing | `quality/e2e-click-testing/desc.md` | 模拟人类点击自测规范 |
| quality | e2e-click-testing | `quality/e2e-click-testing/test-cases.md` | 同步任务详细测试用例基线 |
| design-system | patterns | `design-system/patterns.md` | 参考成熟产品的设计原则 |

## 关键页面和接口

| 类型 | 地址 |
|------|------|
| DolphinScheduler UI | `http://127.0.0.1:12345/dolphinscheduler/ui` |
| 同步任务页面 | `/ui/sync-task` |
| 字段元数据 | `/datasources/tableColumnMetas` |
| 建表预览 | `/datasources/preview-target-table` |
| 执行建表 | `/datasources/create-target-table` |
| 工作流定义 | `/projects/{projectCode}/workflow-definition` |
| 启动工作流实例 | `/projects/{projectCode}/executors/start-workflow-instance` |

## 用户长期要求

- 二开主对象是 DolphinScheduler，SeaTunnel 作为组件集成，能同步数据即可。
- 不能破坏 DolphinScheduler 原有功能，尤其是项目管理、工作流定义、定时等原生能力。
- 做任何设计都要参考市面成熟产品，尤其是 DataWorks、云厂商数据集成平台、大数据处理平台头部产品。
- 功能交付前必须用模拟人类逐步点击页面的方式完整自测。
- 不能只做页面样子，必须保证同步任务可以保存、建表、生成 SeaTunnel 配置并运行。
