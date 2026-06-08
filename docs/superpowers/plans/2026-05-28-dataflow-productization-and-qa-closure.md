# DataFlow Productization And QA Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the current DataFlow DolphinScheduler fork from demo-ready into a persisted, testable product slice across module permissions, data governance, datasource management, sync tasks, data preview, theme library, and QA automation.

**Architecture:** Replace browser/local-file persistence for product data with DolphinScheduler DAO-backed tables and API endpoints. Keep the existing Vue/TSX UI patterns, but move durable permissions and governance state behind backend services. Build Playwright-based smoke/regression suites for every critical closed loop so the remaining modules can be verified repeatedly.

**Tech Stack:** Java 8, Spring MVC, MyBatis Plus, DolphinScheduler DAO, MySQL/PostgreSQL SQL init scripts, Vue 3 TSX, Naive UI, Axios, Vite, Playwright, Docker MySQL/PostgreSQL.

---

## Scope And Priority

This plan is intentionally split into seven independently testable workstreams. Execute them in order:

1. Backend-persisted module permissions.
2. Database-backed data governance store.
3. Data governance automation policies and rule-type QA.
4. Datasource CRUD regression QA.
5. Sync task wizard, Agent, schedule, failure, and log QA.
6. Data preview regression QA.
7. Theme library productization decision and minimal CRUD/API closure.

Do not start theme library productization until permission and governance persistence are stable.

## File Map

### Module Permissions

- Create `dolphinscheduler-dao/src/main/java/org/apache/dolphinscheduler/dao/entity/UserModulePermission.java`: MyBatis entity for per-user module permissions.
- Create `dolphinscheduler-dao/src/main/java/org/apache/dolphinscheduler/dao/mapper/UserModulePermissionMapper.java`: mapper for permission table.
- Modify `dolphinscheduler-dao/src/main/resources/sql/dolphinscheduler_mysql.sql`: add `t_ds_user_module_permission`.
- Modify `dolphinscheduler-dao/src/main/resources/sql/dolphinscheduler_postgresql.sql`: add `t_ds_user_module_permission`.
- Modify `dolphinscheduler-dao/src/main/resources/sql/dolphinscheduler_h2.sql`: add `t_ds_user_module_permission`.
- Create `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/UserModulePermissionService.java`: service contract.
- Create `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/impl/UserModulePermissionServiceImpl.java`: permission query/save logic.
- Modify `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/controller/UsersController.java`: add user module permission query/save endpoints.
- Modify `dolphinscheduler-ui/src/common/module-permissions.ts`: remove localStorage as source of truth; keep only fallback cache if API fails.
- Modify `dolphinscheduler-ui/src/service/modules/users/index.ts`: add module permission API client.
- Modify `dolphinscheduler-ui/src/service/modules/users/types.ts`: add module permission types.
- Modify `dolphinscheduler-ui/src/views/security/user-manage/components/use-authorize.ts`: save/load module permissions from backend.
- Modify `dolphinscheduler-ui/src/router/index.ts` and `dolphinscheduler-ui/src/layouts/content/use-dataList.ts`: use userStore module permissions loaded from backend.
- Modify `dolphinscheduler-ui/src/store/user/types.ts` and `dolphinscheduler-ui/src/store/user/user.ts`: include module permission list in user state.

### Data Governance Persistence

- Create DAO entities and mappers:
  - `DataGovernanceMetadata.java` / `DataGovernanceMetadataMapper.java`
  - `DataGovernanceRule.java` / `DataGovernanceRuleMapper.java`
  - `DataGovernanceIssue.java` / `DataGovernanceIssueMapper.java`
  - `DataGovernanceLineage.java` / `DataGovernanceLineageMapper.java`
- Modify SQL init scripts to add:
  - `t_ds_data_governance_metadata`
  - `t_ds_data_governance_rule`
  - `t_ds_data_governance_issue`
  - `t_ds_data_governance_lineage`
- Replace `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/DataGovernanceStore.java` with DAO-backed persistence or retire it after migration.
- Modify `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/impl/DataGovernanceServiceImpl.java`: use mappers instead of JSON store.
- Modify `dolphinscheduler-api/src/main/resources/application.yaml`: remove reliance on `DOLPHINSCHEDULER_DATA_GOVERNANCE_STORE` for normal runtime.

### Governance Automation

- Modify `DataGovernanceServiceImpl.java`: persist policy flags from rule request, support auto-close and severity escalation.
- Modify `dolphinscheduler-ui/src/service/modules/data-governance/types.ts`: expose issue policy fields explicitly.
- Modify `dolphinscheduler-ui/src/views/data-governance/index.tsx`: bind auto-close/escalation/create-issue to backend payload, not only preview text.
- Modify `dolphinscheduler-ui/src/views/sync-task/index.tsx`: after successful sync lineage registration, trigger governance rules with `frequency=AFTER_SYNC` for target asset.
- Add `POST /data-governance/assets/{assetId}/rules/run-after-sync` or equivalent service method called from sync-task backend/client.

### QA Automation

- Create `dolphinscheduler-ui/qa/dataflow/playwright.config.ts`: shared Playwright config for local DataFlow.
- Create `dolphinscheduler-ui/qa/dataflow/helpers.ts`: login, API session, datasource selectors, screenshot helper.
- Create tests:
  - `datasource.spec.ts`
  - `sync-task.spec.ts`
  - `sync-agent.spec.ts`
  - `data-preview.spec.ts`
  - `data-governance.spec.ts`
  - `module-permissions.spec.ts`
  - `theme-library.spec.ts`
- Create `dolphinscheduler-ui/qa/dataflow/README.md`: how to run and what data fixtures are required.
- Create `scripts/dataflow-demo-fixtures.sh`: idempotent MySQL/PostgreSQL demo fixture setup.

### Theme Library

- Keep current iframe prototype as reference.
- Create backend entities only after product schema is confirmed:
  - theme domain
  - business object
  - scenario
  - SQL block
  - saved query result snapshot
- First closure task is to replace iframe with Vue route components and API contracts, not a big-screen designer.

---

## Task 1: Persist Module Permissions In Backend

**Files:**
- Create: `dolphinscheduler-dao/src/main/java/org/apache/dolphinscheduler/dao/entity/UserModulePermission.java`
- Create: `dolphinscheduler-dao/src/main/java/org/apache/dolphinscheduler/dao/mapper/UserModulePermissionMapper.java`
- Modify: `dolphinscheduler-dao/src/main/resources/sql/dolphinscheduler_mysql.sql`
- Modify: `dolphinscheduler-dao/src/main/resources/sql/dolphinscheduler_postgresql.sql`
- Modify: `dolphinscheduler-dao/src/main/resources/sql/dolphinscheduler_h2.sql`
- Create: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/UserModulePermissionService.java`
- Create: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/impl/UserModulePermissionServiceImpl.java`
- Modify: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/controller/UsersController.java`
- Modify: `dolphinscheduler-ui/src/service/modules/users/index.ts`
- Modify: `dolphinscheduler-ui/src/service/modules/users/types.ts`
- Modify: `dolphinscheduler-ui/src/common/module-permissions.ts`
- Modify: `dolphinscheduler-ui/src/views/security/user-manage/components/use-authorize.ts`
- Modify: `dolphinscheduler-ui/src/store/user/user.ts`
- Modify: `dolphinscheduler-ui/src/store/user/types.ts`

- [ ] Step 1: Add the SQL table to MySQL.

Add this DDL near the user relation tables in `dolphinscheduler_mysql.sql`:

```sql
DROP TABLE IF EXISTS `t_ds_user_module_permission`;
CREATE TABLE `t_ds_user_module_permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `module_key` varchar(128) NOT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_module_permission` (`user_id`, `module_key`),
  KEY `idx_user_module_permission_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
```

- [ ] Step 2: Add equivalent PostgreSQL and H2 tables.

PostgreSQL:

```sql
DROP TABLE IF EXISTS t_ds_user_module_permission;
CREATE TABLE t_ds_user_module_permission (
  id serial NOT NULL,
  user_id int NOT NULL,
  module_key varchar(128) NOT NULL,
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_user_module_permission ON t_ds_user_module_permission(user_id, module_key);
CREATE INDEX idx_user_module_permission_user_id ON t_ds_user_module_permission(user_id);
```

H2:

```sql
DROP TABLE IF EXISTS t_ds_user_module_permission CASCADE;
CREATE TABLE t_ds_user_module_permission (
  id int NOT NULL AUTO_INCREMENT,
  user_id int NOT NULL,
  module_key varchar(128) NOT NULL,
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_module_permission (user_id, module_key)
);
```

- [ ] Step 3: Create entity and mapper.

`UserModulePermission.java` must include `id`, `userId`, `moduleKey`, `createTime`, `updateTime`, with MyBatis Plus `@TableName("t_ds_user_module_permission")`.

`UserModulePermissionMapper.java`:

```java
package org.apache.dolphinscheduler.dao.mapper;

import org.apache.dolphinscheduler.dao.entity.UserModulePermission;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface UserModulePermissionMapper extends BaseMapper<UserModulePermission> {
}
```

- [ ] Step 4: Implement service.

Service contract:

```java
List<String> queryModulePermissions(User loginUser, Integer userId);
List<String> saveModulePermissions(User loginUser, Integer userId, List<String> moduleKeys);
```

Rules:
- Only admin can save permissions for other users.
- Admin users always effectively have all modules, but saved values can still be queried for UI display.
- Supported module keys are exactly: `sync-task:view`, `data-preview:view`, `theme-library:view`, `data-governance:view`, `monitor:view`, `resources:view`.
- Saving replaces existing rows for the user.

- [ ] Step 5: Add controller endpoints.

Add to `UsersController`:

```java
@GetMapping(value = "/{userId}/module-permissions")
public Result<List<String>> queryModulePermissions(
    @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
    @PathVariable("userId") Integer userId) { ... }

@PutMapping(value = "/{userId}/module-permissions")
public Result<List<String>> saveModulePermissions(
    @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
    @PathVariable("userId") Integer userId,
    @RequestBody List<String> moduleKeys) { ... }
```

- [ ] Step 6: Switch frontend authorize modal to API.

Replace `setUserModulePermissions(userId, state.authorizedModules)` with `await saveUserModulePermissions(userId, state.authorizedModules)`. Replace `getUserModulePermissions(userId) || []` with an API call in `onInit`.

- [ ] Step 7: Load current user's module permissions after login/user-info.

Add `modulePermissions?: string[]` to user store state. Load permissions for the current user after user info is available. Router/menu should read from store, not localStorage.

- [ ] Step 8: Verify.

Run:

```bash
./mvnw -pl dolphinscheduler-api,dolphinscheduler-dao -am -DskipTests -DskipITs -DskipCheckstyle -DskipRat -DskipSpotless compile
npm --prefix dolphinscheduler-ui run build:prod
```

Manual QA:
- As admin, remove `data-governance:view` from a normal user.
- Log in as that normal user in a fresh browser context.
- Verify `治理` menu is hidden.
- Directly open `/data-governance`; verify redirected to `/home`.
- Clear browser storage and reload; permission must still apply.

---

## Task 2: Move Data Governance Store From JSON File To Database

**Files:**
- Create DAO entities/mappers listed in File Map.
- Modify SQL init scripts.
- Modify `DataGovernanceServiceImpl.java`.
- Delete or deprecate `DataGovernanceStore.java` after all callers are migrated.

- [ ] Step 1: Add governance DDL.

Create these tables in MySQL/PostgreSQL/H2:

```sql
-- metadata: one row per asset
-- rule: one row per quality rule
-- issue: one row per generated issue
-- lineage: one row per sync lineage edge
```

Required columns:
- `asset_id varchar(512)` on all tables.
- metadata: `owner`, `description`, `tags_json`.
- rule: `rule_id`, `rule_name`, `rule_type`, `rule_level`, `field_name`, `conditions_json`, `range_condition`, `sample_policy`, `failure_threshold`, `severity`, `frequency`, `enabled`, `manual_sql`, `sql_text`, `status`, `last_run_at`, `abnormal_count`, `abnormal_rate`, `create_issue`, `escalate_issue`, `auto_close_issue`.
- issue: `issue_id`, `rule_id`, `title`, `status`, `severity`, `abnormal_count`, `abnormal_rate`, `discovered_at`, `updated_at`, `samples_json`.
- lineage: `source_asset_id`, `target_asset_id`, `source_asset_name`, `target_asset_name`, `sync_task_name`, `last_run_status`, `last_run_time`, `field_mappings_json`.

- [ ] Step 2: Implement mappers and entity classes.

Use MyBatis Plus `BaseMapper` and JSON string fields for `tags_json`, `conditions_json`, `samples_json`, and `field_mappings_json`.

- [ ] Step 3: Migrate service read/write methods.

Replace:
- `dataGovernanceStore.saveMetadata`
- `dataGovernanceStore.getMetadata`
- `dataGovernanceStore.saveRule`
- `dataGovernanceStore.getRules`
- `dataGovernanceStore.saveIssue`
- `dataGovernanceStore.getIssues`
- `dataGovernanceStore.replaceLineage`
- `dataGovernanceStore.getUpstream/getDownstream`

with DAO queries.

- [ ] Step 4: Keep backward compatibility for existing demo data.

On startup is not required. Instead, provide a one-time fallback: if DB has no governance row for an asset and the old JSON file exists, the service may read it and upsert rows when that asset is first queried.

- [ ] Step 5: Verify persistence.

Manual QA:
- Edit asset owner/tags.
- Save a quality rule.
- Restart backend.
- Reload page in browser.
- Verify metadata, rule, issue, lineage are still present.

---

## Task 3: Complete Data Governance Rule Types And Automation

**Files:**
- Modify `DataGovernanceServiceImpl.java`.
- Modify `DataGovernanceDtos.java`.
- Modify `dolphinscheduler-ui/src/views/data-governance/index.tsx`.
- Modify `dolphinscheduler-ui/src/service/modules/data-governance/types.ts`.
- Add Playwright test `dolphinscheduler-ui/qa/dataflow/data-governance.spec.ts`.

- [ ] Step 1: Persist policy fields.

Add fields to DTO/rule request:

```java
private Boolean createIssue;
private Boolean escalateIssue;
private Boolean autoCloseIssue;
```

Frontend must send these values from the checkboxes.

- [ ] Step 2: Implement all six rule types with real SQL generation and trial-run validation.

Rule types:
- `NOT_NULL`
- `UNIQUE`
- `RANGE`
- `ENUM`
- `REGEX`
- `CUSTOM_SQL`

Expected SQL must return `abnormal_count` and `abnormal_rate`.

- [ ] Step 3: Implement issue policy behavior.

Rules:
- If `createIssue=false`, a failed trial run updates rule status but does not create issue.
- If failed and open issue exists for the same `assetId + ruleId`, update it instead of duplicating.
- If passed and `autoCloseIssue=true`, close open/processing issues for the same `assetId + ruleId`.
- If `escalateIssue=true` and the same rule has three consecutive failed runs, severity moves LOW -> MEDIUM -> HIGH.

- [ ] Step 4: Implement after-sync run.

When sync task execution succeeds and lineage is registered, run enabled rules whose frequency is `AFTER_SYNC` for the target asset.

- [ ] Step 5: Playwright QA must cover:

- Create and run NOT_NULL rule.
- Create and run UNIQUE rule.
- Create and run RANGE rule.
- Create and run ENUM rule.
- Create and run REGEX rule.
- Create and run CUSTOM_SQL rule.
- Disable a rule and verify it does not run after sync.
- Enable a rule and verify it can run.
- Verify auto-close issue.

---

## Task 4: Datasource CRUD Regression Suite

**Files:**
- Create `dolphinscheduler-ui/qa/dataflow/datasource.spec.ts`.
- Create `scripts/dataflow-demo-fixtures.sh`.

- [ ] Step 1: Prepare idempotent fixtures.

The fixture script must ensure:
- MySQL is available at `127.0.0.1:3306`.
- PostgreSQL is available at `127.0.0.1:5432`.
- `case_workbench.ajxx_tab` exists and has at least 5 rows.
- PostgreSQL target schema/table can be created and dropped safely.

- [ ] Step 2: Test datasource list and connection.

Test cases:
- MySQL demo datasource exists.
- PostgreSQL demo datasource exists.
- Connection test succeeds for both.

- [ ] Step 3: Test create/edit/delete.

Test cases:
- Create a temporary MySQL datasource with valid credentials.
- Edit note/name.
- Connection test succeeds.
- Create datasource with wrong password and verify clear failure message.
- Delete temporary datasource.

- [ ] Step 4: Test different datasource types.

At minimum:
- MySQL.
- PostgreSQL.

Mark Hive/Oracle/SQL Server as unsupported in local QA unless containers are provided.

---

## Task 5: Sync Task And Agent Regression Suite

**Files:**
- Create `dolphinscheduler-ui/qa/dataflow/sync-task.spec.ts`.
- Create `dolphinscheduler-ui/qa/dataflow/sync-agent.spec.ts`.

- [ ] Step 1: Test full manual wizard.

Flow:
- Open `/sync-task`.
- Click `新建同步任务`.
- Select project.
- Select source datasource/database/table.
- Select target datasource/database/schema/table.
- Load source fields.
- Map fields.
- Add source filter.
- Preview target DDL.
- Save draft.
- Verify task appears in list.

- [ ] Step 2: Test immediate execution.

Flow:
- Use demo MySQL -> PostgreSQL target table.
- Run immediately.
- Verify task returns to list.
- Verify latest status becomes running/success/failed with a readable message.
- Query PostgreSQL to confirm target rows exist if execution succeeds.

- [ ] Step 3: Test schedule flow.

Flow:
- Configure cycle schedule.
- Save and publish schedule.
- Verify schedule state is visible in list.

- [ ] Step 4: Test edit existing task.

Flow:
- Open existing task.
- Change target table name or filter.
- Save.
- Verify list reflects update.

- [ ] Step 5: Test failure diagnosis and log.

Flow:
- Configure wrong target table or broken SQL.
- Run.
- Verify failure state and readable diagnostic.
- Open detail/log drawer.

- [ ] Step 6: Agent natural language cases.

Test commands:
- Valid command: `把 mysql case_workbench.ajxx_tab 同步到 pg public.agent_ajxx_tab，只同步5条`.
- Missing field/table command: verify agent shows missing items.
- Table not found command: verify clear failure.
- Duplicate target command: verify overwrite/update behavior is explicit.
- `只同步 N 条`: verify limit/filter appears in plan.

---

## Task 6: Data Preview Regression Suite

**Files:**
- Create `dolphinscheduler-ui/qa/dataflow/data-preview.spec.ts`.

- [ ] Step 1: Test default table load.

Verify `/data-preview` loads demo datasource/database/table and displays rows.

- [ ] Step 2: Test filter/sort.

Flow:
- Add filter `case_type = 盗窃`.
- Apply.
- Verify returned rows match.
- Add sort `amount desc`.
- Verify order.

- [ ] Step 3: Test table structure/DDL/index tabs.

Verify:
- columns display.
- DDL is copyable.
- indexes tab shows empty or real indexes with clear state.

- [ ] Step 4: Test SQL workbench.

Cases:
- Execute selected statement.
- Execute all.
- Explain plan.
- Reject `delete from ajxx_tab`.
- Reject multi-statement unsafe SQL.

- [ ] Step 5: Test CSV export.

Click export and verify a CSV download is produced and contains current visible columns.

- [ ] Step 6: Test saved views.

Flow:
- Save current view.
- Modify filters and `另存`.
- Switch views.
- Delete a view.
- Reload page and verify remaining saved view persists.

- [ ] Step 7: Test relation/join.

Use a fixture table such as `case_owner_tab` and verify a left join preview works.

---

## Task 7: Theme Library Productization Closure

**Files:**
- Modify `dolphinscheduler-ui/src/views/theme-library/index.tsx` only for first pass if keeping prototype.
- Later create backend DAO/API after schema review.

- [ ] Step 1: Decide productization path.

Current state is iframe prototype. Replace the page header with an explicit mode label if it remains a prototype:

```text
主题库（原型预览）
```

or begin Vue rewrite.

- [ ] Step 2: Minimum formal CRUD scope.

If productizing now, implement these first:
- Theme domain list create/edit/delete.
- Business object create/edit/delete under domain.
- Scenario create/edit/delete under object.
- SQL block create/edit/delete under scenario.
- SQL block data source selection and read-only SQL preview.

- [ ] Step 3: Integration closure.

Required links:
- SQL block uses existing datasource permission.
- SQL preview uses data preview backend read-only SQL endpoint.
- A SQL block can be marked as governance asset input.
- Sync task can be created from selected source/target assets.

- [ ] Step 4: QA.

Playwright must cover CRUD, SQL preview, permission filtering, and links to data preview/governance.

---

## Task 8: Final Regression Report

**Files:**
- Create `docs/qa/dataflow-regression-report-2026-05-28.md`.

- [ ] Step 1: Run backend compile.

```bash
./mvnw -pl dolphinscheduler-api,dolphinscheduler-dao -am -DskipTests -DskipITs -DskipCheckstyle -DskipRat -DskipSpotless compile
```

- [ ] Step 2: Run frontend build.

```bash
npm --prefix dolphinscheduler-ui run build:prod
```

- [ ] Step 3: Run Playwright suites.

```bash
npx --yes playwright test dolphinscheduler-ui/qa/dataflow --config=dolphinscheduler-ui/qa/dataflow/playwright.config.ts
```

- [ ] Step 4: Write report with pass/fail matrix.

Report sections:
- Environment.
- Build status.
- Tested modules.
- Passed cases.
- Failed cases.
- Deferred cases with reason.
- Screenshots/artifacts.

---

## Self-Review

**Spec coverage:** Covers all user-listed gaps: backend module permissions, DB-backed governance, datasource CRUD, sync task wizard, Agent cases, data preview features, governance rule types/automation, permission behavior, and theme library productization.

**Placeholder scan:** No `TBD` or `TODO` placeholders. Theme library is explicitly split because it requires a product decision: keep prototype label or rewrite to CRUD.

**Type consistency:** Module permission keys match existing frontend constants. Governance fields match existing DTO naming plus new policy fields.
