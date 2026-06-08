# DataFlow ECC 安装记录

## 复制来源

- 旧项目：`/Users/luwang/bigdata-build/dolphinscheduler`
- 新项目：`/Users/luwang/bigdata-build/dolphinscheduler-everything-claude-code`
- Git 分支：`codex/everything-claude-code`
- 创建方式：`git worktree add` 后同步旧项目未提交改动和新增文件。

## ECC 来源

- GitHub：`https://github.com/affaan-m/everything-claude-code`
- 本地临时克隆：`/tmp/everything-claude-code`
- 项目内说明：`docs/ecc/README.zh-CN.md`

## 已安装内容

- Claude Code 项目级 ECC：`.claude/`
- Codex 项目级配置：`.codex/`
- Codex/ECC skills：`.agents/skills/`
- Codex 插件清单：`.codex-plugin/`
- Claude 插件清单：`.claude-plugin/`
- 项目级 agent 规范：`AGENTS.md`

## 安装原则

- 不污染旧项目。
- 不把 ECC 安装到全局 `~/.codex`。
- Claude Code 使用项目级 `.claude/`。
- Codex 使用项目级 `.codex/` 和 `.agents/skills/`。

## 后续使用建议

进入新目录后再启动工具：

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler-everything-claude-code
codex
```

或：

```bash
cd /Users/luwang/bigdata-build/dolphinscheduler-everything-claude-code
claude
```

后续所有 DataFlow 规范化二开优先在此目录进行。

