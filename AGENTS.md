# DataFlow DolphinScheduler 二开项目规范

本仓库是从 `/Users/luwang/bigdata-build/dolphinscheduler` 复制出来的隔离开发工作区。后续 DataFlow/DolphinScheduler 二开应优先在本仓库推进，避免影响旧项目。

## 项目目标

- 以 DolphinScheduler 为底座，形成可演示、可交付、可部署的轻量数据中台 DataFlow。
- 核心模块包括：首页、同步任务、数据预览、主题库、数据治理、监控、资源、安全中心、Magic API 接口开发和数据回传。
- 目标不是堆原型，而是逐步达到“基本可使用交付状态”：功能链路清楚、数据来源清楚、权限边界清楚、可启动、可验证、可演示。

## 工作方式

- 每个新需求先澄清目标、边界、用户角色、数据来源和验收标准。
- 涉及较大改动时，先写实施计划，再做代码改动。
- 需求不清楚时，不要盲目开发；先输出问题清单或可选方案。
- 页面原型、产品说明、接口设计、数据库设计和实现代码要保持一致。
- 修改后必须做可复现验证，不能只说“应该可以”。

## 质量门槛

- 前端改动至少运行对应 TypeScript/构建/页面验证中可行的一项。
- 后端改动至少运行对应 Maven 编译或目标测试中可行的一项。
- 关键业务流程需要用浏览器实际点击验证，尤其是登录、授权、Magic API、主题库、首页、数据预览和同步任务。
- 发现历史遗留问题时要记录风险，不要顺手扩大改动范围。

## DataFlow 专用约束

- 主题库、首页、回传等展示数据应优先通过 Magic API 或明确接口获取，避免继续写死假数据。
- Magic API 分组和接口路径必须在文档或代码注释中说明清楚，便于甲方演示和后续维护。
- 权限相关功能必须区分 admin、维护人员和只读用户，不允许只做前端隐藏。
- Oracle 数据源兼容性是必须考虑的交付要求；涉及数据源、同步、预览时不得只按 MySQL 思路实现。
- 公安业务场景文案要贴近业务侧理解，避免只暴露技术概念。

## ECC 使用约定

本项目已安装 Everything Claude Code / ECC 的项目级配置：

- Claude Code 项目配置在 `.claude/`
- Codex 项目配置在 `.codex/`
- Codex/ECC skills 在 `.agents/skills/`
- ECC 中文说明在 `docs/ecc/README.zh-CN.md`
- ECC 原始 agent 规范备份在 `docs/ecc/ECC-AGENTS.md`

使用 ECC 时优先采用以下流程：

1. `planner` / 计划：复杂需求先拆解。
2. `tdd-workflow` / 测试驱动：修 bug 或做核心功能时先定义失败用例。
3. `security-review` / 安全评审：权限、SQL、Magic API、数据源相关改动必须检查。
4. `verification-loop` / 验证闭环：完成前跑构建、测试或浏览器验证。
5. `code-reviewer` / 代码评审：重要改动完成后做审查。

## Git 与隔离

- 当前分支：`codex/everything-claude-code`。
- 不要把旧项目 `/Users/luwang/bigdata-build/dolphinscheduler` 当成主要改动目录。
- 不要重置或删除旧项目未提交改动。
- 提交前先检查 `git status` 和 `git diff`，确认只包含当前任务相关内容。

