# Data Governance Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the approved “数据治理” module as a real DolphinScheduler feature with backend APIs, frontend UI, persistence, and simulated click QA.

**Architecture:** Add a first-party DolphinScheduler route and view for data governance. Add API endpoints under `/data-governance` backed by a service that discovers assets from configured datasources, reads table structures through JDBC metadata, and persists governance metadata/rules/issues/lineage to a backend JSON store so refreshes do not lose data.

**Tech Stack:** Java 8, Spring MVC, DolphinScheduler API/DAO, MyBatis existing datasource mapper, Vue 3 TSX, Naive UI, Axios, Vite, Docker MySQL/PostgreSQL, Chrome/Puppeteer QA.

---

## File Map

- Create `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/controller/DataGovernanceController.java`: REST endpoints for assets, metadata, rules, trial run, lineage, issues.
- Create `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/DataGovernanceService.java`: service contract.
- Create `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/DataGovernanceStore.java`: JSON file persistence helper.
- Create `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/impl/DataGovernanceServiceImpl.java`: asset discovery, rule SQL generation, trial run, issue updates.
- Create DTO classes under `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/dto/datagovernance/`.
- Create `dolphinscheduler-ui/src/router/modules/data-governance.ts`: route registration.
- Modify `dolphinscheduler-ui/src/router/routes.ts`: add route.
- Create `dolphinscheduler-ui/src/service/modules/data-governance/index.ts`: API client.
- Create `dolphinscheduler-ui/src/service/modules/data-governance/types.ts`: frontend types.
- Create `dolphinscheduler-ui/src/views/data-governance/index.tsx`: governance workbench.
- Create `dolphinscheduler-ui/src/views/data-governance/index.module.scss`: Dolphin-style layout and row expansion.
- Modify `dolphinscheduler-ui/src/locales/zh_CN/menu.ts` and `dolphinscheduler-ui/src/locales/en_US/menu.ts`: menu labels.
- Create `tmp_data_governance_qa.js`: simulated click QA script, removed or left as temporary report helper after validation.

## Tasks

### Task 1: Backend DTOs and Store

**Files:**
- Create: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/dto/datagovernance/DataGovernanceDtos.java`
- Create: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/DataGovernanceStore.java`

- [ ] Create DTOs for `Asset`, `Field`, `MetadataRequest`, `QualityRule`, `QualityRuleRequest`, `TrialRunRequest`, `TrialRunResult`, `Lineage`, `Issue`, and `IssueStatusRequest`.
- [ ] Create a JSON persistence store using Jackson `ObjectMapper`.
- [ ] Store data in `${user.home}/.dolphinscheduler/data-governance-store.json` unless `DOLPHINSCHEDULER_DATA_GOVERNANCE_STORE` is set.
- [ ] Persist metadata, rules, issues, and lineage keyed by asset id.
- [ ] Run `mvn -pl dolphinscheduler-api -am -DskipTests compile` and fix compile errors.

### Task 2: Backend Service and Controller

**Files:**
- Create: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/DataGovernanceService.java`
- Create: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/impl/DataGovernanceServiceImpl.java`
- Create: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/controller/DataGovernanceController.java`

- [ ] Implement `queryAssets(User, keyword, datasourceType, qualityStatus)` by scanning authorized MySQL/PostgreSQL datasources.
- [ ] Use JDBC metadata to read table list and column metadata.
- [ ] Merge persisted owner/tags/description/quality status into discovered assets.
- [ ] Implement `queryFields(User, assetId)` using datasource id, database, schema, table.
- [ ] Implement `saveMetadata(User, assetId, request)`.
- [ ] Implement `queryRules`, `saveRule`, `trialRun`, `queryLineage`, `queryIssues`, and `updateIssueStatus`.
- [ ] Generate default SQL for non-null, unique, range, enum, regex, and custom SQL rules.
- [ ] Validate trial-run SQL is read-only and returns `abnormal_count`.
- [ ] Create issue records when trial run finds abnormalities.
- [ ] Run backend compile.

### Task 3: Frontend Route and API Client

**Files:**
- Create: `dolphinscheduler-ui/src/router/modules/data-governance.ts`
- Modify: `dolphinscheduler-ui/src/router/routes.ts`
- Create: `dolphinscheduler-ui/src/service/modules/data-governance/types.ts`
- Create: `dolphinscheduler-ui/src/service/modules/data-governance/index.ts`
- Modify: `dolphinscheduler-ui/src/locales/zh_CN/menu.ts`
- Modify: `dolphinscheduler-ui/src/locales/en_US/menu.ts`

- [ ] Register `/data-governance`.
- [ ] Add menu title `数据治理`.
- [ ] Add typed Axios wrappers for all backend endpoints.
- [ ] Run `npm run build:prod` in `dolphinscheduler-ui` and fix type errors.

### Task 4: Frontend Workbench

**Files:**
- Create: `dolphinscheduler-ui/src/views/data-governance/index.tsx`
- Create: `dolphinscheduler-ui/src/views/data-governance/index.module.scss`

- [ ] Build metrics header, filters, and asset table.
- [ ] Implement row expand/collapse directly below the selected table row.
- [ ] Implement tabs: 概览、字段、质量、血缘、问题.
- [ ] Load fields/rules/lineage/issues lazily when opening tabs.
- [ ] Implement metadata edit modal.
- [ ] Implement quality rule modal with rule conditions, preview/trial tab, SQL tab, manual SQL state, regenerate SQL button.
- [ ] Save rule and return to current asset’s “质量” tab.
- [ ] Implement issue status change.
- [ ] Run frontend build and fix type/style errors.

### Task 5: Environment Startup and Data Preparation

**Files:**
- Use existing Docker/Dolphin files only; do not add production config unless required.

- [ ] Start Docker if not running.
- [ ] Start or create MySQL container.
- [ ] Start or create PostgreSQL container.
- [ ] Create test tables with null/duplicate data for quality rules.
- [ ] Start Dolphin backend.
- [ ] Start Dolphin frontend.
- [ ] Confirm login page and data governance route are reachable.

### Task 6: Simulated Human Click QA

**Files:**
- Create: `tmp_data_governance_qa.js`
- Create: `.ai/data-governance/test/data-governance-implementation-report.md`

- [ ] Use Chrome/Puppeteer to login.
- [ ] Click “数据治理”.
- [ ] Verify assets load or empty state is explicit.
- [ ] Search and filter assets.
- [ ] Expand an asset.
- [ ] Click each detail tab.
- [ ] Create a non-null rule.
- [ ] Verify generated SQL appears.
- [ ] Edit SQL and verify manual mode.
- [ ] Run trial run and capture result.
- [ ] Save rule and verify it appears in quality tab after refresh.
- [ ] Change issue status if a failure issue exists.
- [ ] Save screenshots and write final QA report.

## Verification Commands

```bash
mvn -pl dolphinscheduler-api -am -DskipTests compile
cd dolphinscheduler-ui && npm run build:prod
node tmp_data_governance_qa.js
```

## Self-Review

- Spec coverage: covered frontend route/page, backend API, persistence, SQL generation/editing, lineage, issues, Docker/app startup, click QA.
- Placeholder scan: no TBD/TODO placeholders.
- Type consistency: DTO names and frontend service names are defined in the file map and used consistently across tasks.
