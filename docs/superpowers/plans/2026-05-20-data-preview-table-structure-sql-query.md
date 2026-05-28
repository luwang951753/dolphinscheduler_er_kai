# Data Preview Table Structure And SQL Query Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build real DolphinScheduler data preview support for table structure viewing and read-only SQL query.

**Architecture:** Reuse existing datasource permission checks and JDBC connection flow in `DataSourceServiceImpl`. Add metadata/query DTOs and controller endpoints, then wire the existing Vue data preview page with new workspace tabs matching the approved prototypes.

**Tech Stack:** Java Spring MVC, JDBC metadata, Vue 3 TSX, Naive UI, existing DolphinScheduler datasource service module.

---

### Task 1: Backend DTOs And Service Contract

**Files:**
- Create: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/dto/DataPreviewTableStructureResult.java`
- Create: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/dto/DataPreviewSqlQueryRequest.java`
- Modify: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/DataSourceService.java`

- [ ] Add DTOs for table summary, columns, indexes, DDL, SQL request.
- [ ] Add service methods `queryTableStructure`, `executePreviewSql`, `explainPreviewSql`.

### Task 2: Backend Service Implementation

**Files:**
- Modify: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/service/impl/DataSourceServiceImpl.java`

- [ ] Reuse datasource permission/type validation from `previewData`.
- [ ] Query JDBC metadata for table structure and indexes.
- [ ] Build simple formatted DDL from metadata.
- [ ] Validate SQL as read-only `SELECT/WITH/EXPLAIN`.
- [ ] Reject DML/DDL keywords and unsafe multi-statement SQL.
- [ ] Execute SQL with max rows and timeout.

### Task 3: Backend Controller Endpoints

**Files:**
- Modify: `dolphinscheduler-api/src/main/java/org/apache/dolphinscheduler/api/controller/DataSourceController.java`

- [ ] Add `GET /datasources/preview-table-structure`.
- [ ] Add `POST /datasources/preview-sql`.
- [ ] Add `POST /datasources/preview-sql-explain`.

### Task 4: Frontend Service Types

**Files:**
- Modify: `dolphinscheduler-ui/src/service/modules/data-source/types.ts`
- Modify: `dolphinscheduler-ui/src/service/modules/data-source/index.ts`

- [ ] Add table structure and SQL query interfaces.
- [ ] Add API wrappers for new endpoints.

### Task 5: Frontend Data Preview Page

**Files:**
- Modify: `dolphinscheduler-ui/src/views/data-preview/index.tsx`
- Modify: `dolphinscheduler-ui/src/views/data-preview/index.module.scss`

- [ ] Add workspace tabs: data, structure, DDL, indexes, SQL.
- [ ] Render table structure with search and filters.
- [ ] Render formatted DDL and indexes/constraints.
- [ ] Render SQL workbench with editor, history, message, result, explain.
- [ ] Ensure switching table refreshes structure and default SQL.

### Task 6: Verification

**Commands:**
- `./mvnw -pl dolphinscheduler-api -DskipITs -DskipCheckstyle -DskipRat -DskipSpotless -DskipFrontend -DskipWebapp -DskipDocker -DskipKubernetes -DskipPython -DskipUI -DskipDoc -DskipShade -DskipJavadoc -DskipSource -DskipTests compile`
- `pnpm --dir dolphinscheduler-ui type-check`

- [ ] Run backend compile.
- [ ] Run frontend type check.
- [ ] Start services if needed and manually verify data preview tabs.
