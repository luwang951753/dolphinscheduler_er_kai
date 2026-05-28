# Agent Studio MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first working `agent-studio` vertical slice: a Web console where users describe a relational database sync requirement, preview a validated Data Sync Agent plan, and create or execute a DolphinScheduler workflow through typed backend tools.

**Architecture:** Create a new independent project at `/Users/luwang/bigdata-build/agent-studio`, separate from the DolphinScheduler frontend. Use FastAPI as the backend API, LangGraph-compatible service boundaries for the Agent runtime, Milvus-backed RAG as a swappable service, a DolphinScheduler client wrapper for all DS calls, and a minimal React/Vite Web console for input, plan preview, risk confirmation, and execution feedback.

**Tech Stack:** Python 3 in conda env `RAG_PROJECT`, FastAPI, Pydantic v2, pytest, httpx, LangChain, LangGraph, Milvus/pymilvus, React, TypeScript, Vite, Vitest.

---

## Scope Check

This plan intentionally builds one MVP vertical slice, not the full future enterprise Agent platform. It includes the minimum platform shell, Data Sync Agent, DolphinScheduler integration, RAG abstraction, persistence, and Web console needed to validate the first use case. Multi-Agent marketplace, enterprise RBAC, full audit center, CDC, and cross-Agent orchestration remain out of scope.

## File Structure

The implementation creates a new project outside DolphinScheduler:

```text
/Users/luwang/bigdata-build/agent-studio/
  backend/
    pyproject.toml
    pytest.ini
    app/
      main.py
      api/
        routes.py
      agents/
        data_sync/
          graph.py
          planner.py
          workflow_builder.py
          risk.py
      config/
        settings.py
      integrations/
        dolphinscheduler/
          client.py
          models.py
      rag/
        service.py
      schemas/
        data_sync.py
      storage/
        repository.py
      tools/
        datasource_tools.py
        workflow_tools.py
    tests/
      agents/
        test_data_sync_planner.py
        test_risk.py
        test_workflow_builder.py
      api/
        test_routes.py
      integrations/
        test_dolphinscheduler_client.py
  frontend/
    package.json
    index.html
    src/
      main.tsx
      App.tsx
      api/client.ts
      components/
        DataSyncWorkbench.tsx
        PlanPreview.tsx
        RiskBanner.tsx
      types.ts
      __tests__/
        DataSyncWorkbench.test.tsx
  docs/
    local-development.md
```

Each unit has one clear job:

- `schemas/data_sync.py`: stable Pydantic contracts used by API, Agent, tools, and tests.
- `planner.py`: converts parsed intent and metadata into a `SyncPlan`.
- `risk.py`: deterministic risk policy, independent from LLM output.
- `workflow_builder.py`: converts `SyncPlan` into DolphinScheduler workflow parameters.
- `client.py`: all raw DolphinScheduler HTTP details.
- `datasource_tools.py` and `workflow_tools.py`: typed Agent-facing tool wrappers.
- `graph.py`: orchestration state machine; no raw HTTP calls.
- `repository.py`: MVP in-memory persistence, replaceable later.
- frontend components: input, preview, risk, execution feedback only.

---

### Task 1: Create Backend Project Skeleton

**Files:**
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/pyproject.toml`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/pytest.ini`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/main.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/api/routes.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/tests/api/test_routes.py`

- [ ] **Step 1: Create backend directories**

Run:

```bash
mkdir -p /Users/luwang/bigdata-build/agent-studio/backend/app/api
mkdir -p /Users/luwang/bigdata-build/agent-studio/backend/tests/api
```

Expected: directories exist and no output.

- [ ] **Step 2: Write the failing API health test**

Create `/Users/luwang/bigdata-build/agent-studio/backend/tests/api/test_routes.py`:

```python
from fastapi.testclient import TestClient

from app.main import app


def test_health_endpoint_returns_ok():
    client = TestClient(app)

    response = client.get("/api/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "agent-studio"}
```

- [ ] **Step 3: Add backend project metadata**

Create `/Users/luwang/bigdata-build/agent-studio/backend/pyproject.toml`:

```toml
[project]
name = "agent-studio-backend"
version = "0.1.0"
requires-python = ">=3.11"
dependencies = [
  "fastapi>=0.111.0",
  "uvicorn[standard]>=0.30.0",
  "pydantic>=2.7.0",
  "pydantic-settings>=2.2.0",
  "httpx>=0.27.0",
  "langchain>=0.2.0",
  "langgraph>=0.1.0",
  "pymilvus>=2.4.0",
]

[project.optional-dependencies]
test = [
  "pytest>=8.2.0",
  "pytest-asyncio>=0.23.0",
]

[tool.pytest.ini_options]
pythonpath = ["."]
testpaths = ["tests"]
```

Create `/Users/luwang/bigdata-build/agent-studio/backend/pytest.ini`:

```ini
[pytest]
pythonpath = .
testpaths = tests
```

- [ ] **Step 4: Run test to verify it fails before implementation**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/api/test_routes.py::test_health_endpoint_returns_ok -v
```

Expected: FAIL with an import error for `app.main` or missing route.

- [ ] **Step 5: Implement FastAPI app and health route**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/api/routes.py`:

```python
from fastapi import APIRouter

router = APIRouter(prefix="/api")


@router.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "agent-studio"}
```

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/main.py`:

```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import router

app = FastAPI(title="Agent Studio", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)
```

- [ ] **Step 6: Run backend health test**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/api/test_routes.py::test_health_endpoint_returns_ok -v
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio
git add backend
git commit -m "feat: add backend skeleton"
```

Expected: commit succeeds. If `/Users/luwang/bigdata-build/agent-studio` is not a Git repository yet, run `git init` first and then repeat the commit.

---

### Task 2: Define Data Sync Schemas

**Files:**
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/schemas/data_sync.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/tests/agents/test_data_sync_planner.py`

- [ ] **Step 1: Create schema and test directories**

Run:

```bash
mkdir -p /Users/luwang/bigdata-build/agent-studio/backend/app/schemas
mkdir -p /Users/luwang/bigdata-build/agent-studio/backend/tests/agents
```

Expected: directories exist and no output.

- [ ] **Step 2: Write failing schema test**

Create `/Users/luwang/bigdata-build/agent-studio/backend/tests/agents/test_data_sync_planner.py`:

```python
from app.schemas.data_sync import (
    DataEndpoint,
    FieldMapping,
    ScheduleSpec,
    SyncMode,
    SyncPlan,
    TargetTableStrategy,
)


def test_sync_plan_schema_accepts_incremental_plan():
    plan = SyncPlan(
        name="customer_incremental_sync",
        source=DataEndpoint(datasource_name="mysql_crm", database="crm", table="customer"),
        target=DataEndpoint(datasource_name="postgres_dw", database="dw", table="customer"),
        sync_mode=SyncMode.INCREMENTAL,
        incremental_column="update_time",
        field_mappings=[
            FieldMapping(source="id", target="id", source_type="BIGINT", target_type="BIGINT"),
            FieldMapping(source="name", target="name", source_type="VARCHAR", target_type="VARCHAR"),
        ],
        schedule=ScheduleSpec(kind="cron", expression="0 0 2 * * ?"),
        ds_task_type="DATAX",
        failure_strategy="CONTINUE",
        target_table_strategy=TargetTableStrategy.CREATE_IF_MISSING,
    )

    assert plan.sync_mode == SyncMode.INCREMENTAL
    assert plan.incremental_column == "update_time"
    assert plan.target_table_strategy == TargetTableStrategy.CREATE_IF_MISSING
```

- [ ] **Step 3: Run test to verify it fails**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/agents/test_data_sync_planner.py::test_sync_plan_schema_accepts_incremental_plan -v
```

Expected: FAIL because `app.schemas.data_sync` does not exist.

- [ ] **Step 4: Implement schemas**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/schemas/data_sync.py`:

```python
from enum import StrEnum

from pydantic import BaseModel, Field


class SyncMode(StrEnum):
    FULL = "full"
    INCREMENTAL = "incremental"


class TargetTableStrategy(StrEnum):
    USE_EXISTING = "use_existing"
    CREATE_IF_MISSING = "create_if_missing"
    REQUIRE_MANUAL_ACTION = "require_manual_action"


class RiskLevel(StrEnum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"


class DataEndpoint(BaseModel):
    datasource_name: str = Field(min_length=1)
    database: str = Field(min_length=1)
    table: str = Field(min_length=1)


class FieldMapping(BaseModel):
    source: str = Field(min_length=1)
    target: str = Field(min_length=1)
    source_type: str = Field(min_length=1)
    target_type: str = Field(min_length=1)


class ScheduleSpec(BaseModel):
    kind: str = Field(pattern="^(manual|cron)$")
    expression: str | None = None


class SyncIntent(BaseModel):
    raw_text: str
    source: DataEndpoint | None = None
    target: DataEndpoint | None = None
    sync_mode: SyncMode | None = None
    incremental_column: str | None = None
    schedule_text: str | None = None


class SyncPlan(BaseModel):
    name: str = Field(min_length=1)
    source: DataEndpoint
    target: DataEndpoint
    sync_mode: SyncMode
    incremental_column: str | None = None
    field_mappings: list[FieldMapping]
    schedule: ScheduleSpec
    ds_task_type: str
    failure_strategy: str
    target_table_strategy: TargetTableStrategy


class RiskAssessment(BaseModel):
    level: RiskLevel
    reasons: list[str]
    requires_confirmation: bool


class WorkflowDraft(BaseModel):
    name: str
    description: str
    global_params: str
    locations: str
    timeout: int = 0
    task_relation_json: str
    task_definition_json: str
    other_params_json: str | None = None
    execution_type: str = "PARALLEL"
```

- [ ] **Step 5: Run schema test**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/agents/test_data_sync_planner.py::test_sync_plan_schema_accepts_incremental_plan -v
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio
git add backend/app/schemas backend/tests/agents/test_data_sync_planner.py
git commit -m "feat: define data sync schemas"
```

Expected: commit succeeds.

---

### Task 3: Implement Deterministic Risk Policy

**Files:**
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/agents/data_sync/risk.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/tests/agents/test_risk.py`

- [ ] **Step 1: Create Agent directory**

Run:

```bash
mkdir -p /Users/luwang/bigdata-build/agent-studio/backend/app/agents/data_sync
```

Expected: directory exists and no output.

- [ ] **Step 2: Write failing risk tests**

Create `/Users/luwang/bigdata-build/agent-studio/backend/tests/agents/test_risk.py`:

```python
from app.agents.data_sync.risk import assess_risk
from app.schemas.data_sync import (
    DataEndpoint,
    FieldMapping,
    ScheduleSpec,
    SyncMode,
    SyncPlan,
    TargetTableStrategy,
    RiskLevel,
)


def _plan(**overrides):
    base = SyncPlan(
        name="customer_sync",
        source=DataEndpoint(datasource_name="mysql_crm", database="crm", table="customer"),
        target=DataEndpoint(datasource_name="postgres_dw", database="dw", table="customer"),
        sync_mode=SyncMode.INCREMENTAL,
        incremental_column="update_time",
        field_mappings=[
            FieldMapping(source="id", target="id", source_type="BIGINT", target_type="BIGINT"),
            FieldMapping(source="update_time", target="update_time", source_type="TIMESTAMP", target_type="TIMESTAMP"),
        ],
        schedule=ScheduleSpec(kind="cron", expression="0 0 2 * * ?"),
        ds_task_type="DATAX",
        failure_strategy="CONTINUE",
        target_table_strategy=TargetTableStrategy.USE_EXISTING,
    )
    return base.model_copy(update=overrides)


def test_incremental_without_column_is_high_risk():
    assessment = assess_risk(_plan(incremental_column=None))

    assert assessment.level == RiskLevel.HIGH
    assert assessment.requires_confirmation is True
    assert "增量同步缺少增量字段" in assessment.reasons


def test_create_target_table_is_medium_risk():
    assessment = assess_risk(_plan(target_table_strategy=TargetTableStrategy.CREATE_IF_MISSING))

    assert assessment.level == RiskLevel.MEDIUM
    assert assessment.requires_confirmation is True
    assert "需要创建目标表" in assessment.reasons


def test_existing_table_with_valid_incremental_plan_is_low_risk():
    assessment = assess_risk(_plan())

    assert assessment.level == RiskLevel.LOW
    assert assessment.requires_confirmation is False
```

- [ ] **Step 3: Run test to verify it fails**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/agents/test_risk.py -v
```

Expected: FAIL because `app.agents.data_sync.risk` does not exist.

- [ ] **Step 4: Implement risk policy**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/agents/data_sync/risk.py`:

```python
from app.schemas.data_sync import RiskAssessment, RiskLevel, SyncMode, SyncPlan, TargetTableStrategy


def assess_risk(plan: SyncPlan) -> RiskAssessment:
    reasons: list[str] = []
    level = RiskLevel.LOW

    if plan.sync_mode == SyncMode.INCREMENTAL and not plan.incremental_column:
        reasons.append("增量同步缺少增量字段")
        level = RiskLevel.HIGH

    if plan.target_table_strategy == TargetTableStrategy.CREATE_IF_MISSING:
        reasons.append("需要创建目标表")
        if level != RiskLevel.HIGH:
            level = RiskLevel.MEDIUM

    if plan.target_table_strategy == TargetTableStrategy.REQUIRE_MANUAL_ACTION:
        reasons.append("目标表策略需要人工处理")
        level = RiskLevel.HIGH

    incompatible_fields = [
        mapping
        for mapping in plan.field_mappings
        if not _is_type_compatible(mapping.source_type, mapping.target_type)
    ]
    if incompatible_fields:
        field_names = ", ".join(mapping.source for mapping in incompatible_fields)
        reasons.append(f"字段类型不兼容: {field_names}")
        level = RiskLevel.HIGH

    return RiskAssessment(
        level=level,
        reasons=reasons,
        requires_confirmation=level in {RiskLevel.MEDIUM, RiskLevel.HIGH},
    )


def _is_type_compatible(source_type: str, target_type: str) -> bool:
    source = source_type.upper()
    target = target_type.upper()
    if source == target:
        return True
    numeric = {"INT", "INTEGER", "BIGINT", "SMALLINT", "TINYINT", "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE"}
    text = {"CHAR", "VARCHAR", "TEXT", "STRING"}
    time = {"DATE", "DATETIME", "TIMESTAMP"}
    return (
        source in numeric and target in numeric
        or source in text and target in text
        or source in time and target in time
    )
```

- [ ] **Step 5: Run risk tests**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/agents/test_risk.py -v
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio
git add backend/app/agents/data_sync/risk.py backend/tests/agents/test_risk.py
git commit -m "feat: add data sync risk policy"
```

Expected: commit succeeds.

---

### Task 4: Implement Basic Planner

**Files:**
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/agents/data_sync/planner.py`
- Modify: `/Users/luwang/bigdata-build/agent-studio/backend/tests/agents/test_data_sync_planner.py`

- [ ] **Step 1: Add failing planner test**

Append to `/Users/luwang/bigdata-build/agent-studio/backend/tests/agents/test_data_sync_planner.py`:

```python
from app.agents.data_sync.planner import build_plan_from_intent
from app.schemas.data_sync import SyncIntent


def test_build_plan_from_intent_uses_metadata_field_mappings():
    intent = SyncIntent(
        raw_text="每天凌晨2点把 MySQL crm.customer 增量同步到 PostgreSQL dw.customer，按 update_time 增量",
        source=DataEndpoint(datasource_name="mysql_crm", database="crm", table="customer"),
        target=DataEndpoint(datasource_name="postgres_dw", database="dw", table="customer"),
        sync_mode=SyncMode.INCREMENTAL,
        incremental_column="update_time",
        schedule_text="每天凌晨2点",
    )
    metadata = {
        "source_columns": [
            {"name": "id", "type": "BIGINT"},
            {"name": "name", "type": "VARCHAR"},
            {"name": "update_time", "type": "TIMESTAMP"},
        ],
        "target_columns": [
            {"name": "id", "type": "BIGINT"},
            {"name": "name", "type": "VARCHAR"},
            {"name": "update_time", "type": "TIMESTAMP"},
        ],
        "target_exists": True,
    }

    plan = build_plan_from_intent(intent, metadata)

    assert plan.name == "customer_incremental_sync"
    assert plan.schedule.expression == "0 0 2 * * ?"
    assert [mapping.source for mapping in plan.field_mappings] == ["id", "name", "update_time"]
    assert plan.target_table_strategy == TargetTableStrategy.USE_EXISTING
```

- [ ] **Step 2: Run planner test to verify it fails**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/agents/test_data_sync_planner.py::test_build_plan_from_intent_uses_metadata_field_mappings -v
```

Expected: FAIL because `app.agents.data_sync.planner` does not exist.

- [ ] **Step 3: Implement planner**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/agents/data_sync/planner.py`:

```python
from app.schemas.data_sync import (
    FieldMapping,
    ScheduleSpec,
    SyncIntent,
    SyncMode,
    SyncPlan,
    TargetTableStrategy,
)


def build_plan_from_intent(intent: SyncIntent, metadata: dict) -> SyncPlan:
    if intent.source is None:
        raise ValueError("缺少源端信息")
    if intent.target is None:
        raise ValueError("缺少目标端信息")

    sync_mode = intent.sync_mode or SyncMode.FULL
    source_columns = metadata.get("source_columns", [])
    target_columns = metadata.get("target_columns", [])
    target_exists = bool(metadata.get("target_exists", False))

    mappings = _build_field_mappings(source_columns, target_columns if target_exists else source_columns)
    strategy = TargetTableStrategy.USE_EXISTING if target_exists else TargetTableStrategy.CREATE_IF_MISSING

    return SyncPlan(
        name=f"{intent.source.table}_{sync_mode.value}_sync",
        source=intent.source,
        target=intent.target,
        sync_mode=sync_mode,
        incremental_column=intent.incremental_column,
        field_mappings=mappings,
        schedule=_parse_schedule(intent.schedule_text),
        ds_task_type="DATAX",
        failure_strategy="CONTINUE",
        target_table_strategy=strategy,
    )


def _build_field_mappings(source_columns: list[dict], target_columns: list[dict]) -> list[FieldMapping]:
    target_by_name = {column["name"]: column for column in target_columns}
    mappings: list[FieldMapping] = []
    for source in source_columns:
        target = target_by_name.get(source["name"])
        if target is None:
            continue
        mappings.append(
            FieldMapping(
                source=source["name"],
                target=target["name"],
                source_type=source["type"],
                target_type=target["type"],
            )
        )
    return mappings


def _parse_schedule(schedule_text: str | None) -> ScheduleSpec:
    if not schedule_text:
        return ScheduleSpec(kind="manual", expression=None)
    normalized = schedule_text.replace(" ", "")
    if "每天" in normalized and ("凌晨2点" in normalized or "02:00" in normalized):
        return ScheduleSpec(kind="cron", expression="0 0 2 * * ?")
    if "每小时" in normalized or "每1小时" in normalized:
        return ScheduleSpec(kind="cron", expression="0 0 * * * ?")
    return ScheduleSpec(kind="manual", expression=None)
```

- [ ] **Step 4: Run planner tests**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/agents/test_data_sync_planner.py -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio
git add backend/app/agents/data_sync/planner.py backend/tests/agents/test_data_sync_planner.py
git commit -m "feat: build data sync plans from intent"
```

Expected: commit succeeds.

---

### Task 5: Build DolphinScheduler Workflow Draft

**Files:**
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/agents/data_sync/workflow_builder.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/tests/agents/test_workflow_builder.py`

- [ ] **Step 1: Write failing workflow builder test**

Create `/Users/luwang/bigdata-build/agent-studio/backend/tests/agents/test_workflow_builder.py`:

```python
import json

from app.agents.data_sync.workflow_builder import build_workflow_draft
from app.schemas.data_sync import (
    DataEndpoint,
    FieldMapping,
    ScheduleSpec,
    SyncMode,
    SyncPlan,
    TargetTableStrategy,
)


def test_build_workflow_draft_contains_single_datax_task():
    plan = SyncPlan(
        name="customer_incremental_sync",
        source=DataEndpoint(datasource_name="mysql_crm", database="crm", table="customer"),
        target=DataEndpoint(datasource_name="postgres_dw", database="dw", table="customer"),
        sync_mode=SyncMode.INCREMENTAL,
        incremental_column="update_time",
        field_mappings=[
            FieldMapping(source="id", target="id", source_type="BIGINT", target_type="BIGINT"),
            FieldMapping(source="update_time", target="update_time", source_type="TIMESTAMP", target_type="TIMESTAMP"),
        ],
        schedule=ScheduleSpec(kind="cron", expression="0 0 2 * * ?"),
        ds_task_type="DATAX",
        failure_strategy="CONTINUE",
        target_table_strategy=TargetTableStrategy.USE_EXISTING,
    )

    draft = build_workflow_draft(plan)
    task_defs = json.loads(draft.task_definition_json)
    relations = json.loads(draft.task_relation_json)

    assert draft.name == "customer_incremental_sync"
    assert task_defs[0]["taskType"] == "DATAX"
    assert task_defs[0]["name"] == "customer_incremental_sync_datax"
    assert relations == []
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/agents/test_workflow_builder.py -v
```

Expected: FAIL because `workflow_builder.py` does not exist.

- [ ] **Step 3: Implement workflow builder**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/agents/data_sync/workflow_builder.py`:

```python
import json
from hashlib import md5

from app.schemas.data_sync import SyncPlan, WorkflowDraft


def build_workflow_draft(plan: SyncPlan) -> WorkflowDraft:
    task_code = _stable_task_code(plan.name)
    task_definition = {
        "code": task_code,
        "name": f"{plan.name}_datax",
        "version": 1,
        "description": f"Generated by agent-studio for {plan.source.table} -> {plan.target.table}",
        "taskType": plan.ds_task_type,
        "taskParams": _build_task_params(plan),
        "flag": "YES",
        "taskPriority": "MEDIUM",
        "workerGroup": "default",
        "failRetryTimes": 0,
        "failRetryInterval": 1,
        "timeoutFlag": "CLOSE",
        "timeoutNotifyStrategy": "WARN",
        "timeout": 0,
        "delayTime": 0,
        "resourceIds": [],
    }
    location = {str(task_code): {"name": task_definition["name"], "targetarr": "", "x": 320, "y": 160}}

    return WorkflowDraft(
        name=plan.name,
        description=f"Generated sync workflow for {plan.source.datasource_name}.{plan.source.table}",
        global_params="[]",
        locations=json.dumps(location, ensure_ascii=False),
        timeout=0,
        task_relation_json="[]",
        task_definition_json=json.dumps([task_definition], ensure_ascii=False),
        other_params_json=None,
        execution_type="PARALLEL",
    )


def _stable_task_code(name: str) -> int:
    digest = md5(name.encode("utf-8")).hexdigest()
    return int(digest[:12], 16)


def _build_task_params(plan: SyncPlan) -> dict:
    return {
        "customConfig": 1,
        "json": {
            "job": {
                "content": [
                    {
                        "reader": {
                            "name": "rdbmsreader",
                            "parameter": {
                                "datasource": plan.source.datasource_name,
                                "database": plan.source.database,
                                "table": plan.source.table,
                                "column": [mapping.source for mapping in plan.field_mappings],
                                "where": _build_incremental_where(plan),
                            },
                        },
                        "writer": {
                            "name": "rdbmswriter",
                            "parameter": {
                                "datasource": plan.target.datasource_name,
                                "database": plan.target.database,
                                "table": plan.target.table,
                                "column": [mapping.target for mapping in plan.field_mappings],
                            },
                        },
                    }
                ],
                "setting": {"speed": {"channel": 1}},
            }
        },
    }


def _build_incremental_where(plan: SyncPlan) -> str:
    if plan.sync_mode == "incremental" and plan.incremental_column:
        return f"{plan.incremental_column} >= '${{biz_date}}'"
    return ""
```

- [ ] **Step 4: Run workflow builder test**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/agents/test_workflow_builder.py -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio
git add backend/app/agents/data_sync/workflow_builder.py backend/tests/agents/test_workflow_builder.py
git commit -m "feat: build dolphin workflow drafts"
```

Expected: commit succeeds.

---

### Task 6: Add DolphinScheduler Client Wrapper

**Files:**
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/config/settings.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/integrations/dolphinscheduler/models.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/integrations/dolphinscheduler/client.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/tests/integrations/test_dolphinscheduler_client.py`

- [ ] **Step 1: Create integration directories**

Run:

```bash
mkdir -p /Users/luwang/bigdata-build/agent-studio/backend/app/config
mkdir -p /Users/luwang/bigdata-build/agent-studio/backend/app/integrations/dolphinscheduler
mkdir -p /Users/luwang/bigdata-build/agent-studio/backend/tests/integrations
```

Expected: directories exist and no output.

- [ ] **Step 2: Write failing client tests**

Create `/Users/luwang/bigdata-build/agent-studio/backend/tests/integrations/test_dolphinscheduler_client.py`:

```python
import httpx
import pytest

from app.integrations.dolphinscheduler.client import DolphinSchedulerClient
from app.schemas.data_sync import WorkflowDraft


@pytest.mark.asyncio
async def test_create_workflow_definition_posts_expected_form():
    captured = {}

    async def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["content"] = request.content.decode()
        return httpx.Response(200, json={"code": 0, "data": {"code": 123456}})

    transport = httpx.MockTransport(handler)
    client = DolphinSchedulerClient(base_url="http://ds.local/dolphinscheduler", token="token", transport=transport)
    draft = WorkflowDraft(
        name="customer_sync",
        description="generated",
        global_params="[]",
        locations="{}",
        timeout=0,
        task_relation_json="[]",
        task_definition_json="[]",
        other_params_json=None,
        execution_type="PARALLEL",
    )

    result = await client.create_workflow_definition(project_code=1001, draft=draft)

    assert result == {"code": 123456}
    assert captured["url"] == "http://ds.local/dolphinscheduler/projects/1001/workflow-definition"
    assert "name=customer_sync" in captured["content"]
    assert "taskDefinitionJson=%5B%5D" in captured["content"]
```

- [ ] **Step 3: Run test to verify it fails**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/integrations/test_dolphinscheduler_client.py -v
```

Expected: FAIL because the DolphinScheduler client does not exist.

- [ ] **Step 4: Implement settings**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/config/settings.py`:

```python
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="AGENT_STUDIO_", env_file=".env", extra="ignore")

    dolphinscheduler_base_url: str = "http://localhost:12345/dolphinscheduler"
    dolphinscheduler_token: str = ""
    milvus_uri: str = "http://localhost:19530"
    project_code: int = 0


settings = Settings()
```

- [ ] **Step 5: Implement integration models**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/integrations/dolphinscheduler/models.py`:

```python
from pydantic import BaseModel


class DolphinSchedulerError(RuntimeError):
    def __init__(self, message: str, response_code: int | None = None):
        super().__init__(message)
        self.response_code = response_code


class WorkflowCreateResult(BaseModel):
    code: int
```

- [ ] **Step 6: Implement DolphinScheduler client**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/integrations/dolphinscheduler/client.py`:

```python
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import httpx

from app.integrations.dolphinscheduler.models import DolphinSchedulerError
from app.schemas.data_sync import WorkflowDraft


class DolphinSchedulerClient:
    def __init__(
        self,
        base_url: str,
        token: str = "",
        transport: httpx.AsyncBaseTransport | None = None,
    ):
        self.base_url = base_url.rstrip("/")
        self.token = token
        self.transport = transport

    async def create_workflow_definition(self, project_code: int, draft: WorkflowDraft) -> dict:
        data = {
            "name": draft.name,
            "description": draft.description,
            "globalParams": draft.global_params,
            "locations": draft.locations,
            "timeout": str(draft.timeout),
            "taskRelationJson": draft.task_relation_json,
            "taskDefinitionJson": draft.task_definition_json,
            "executionType": draft.execution_type,
        }
        if draft.other_params_json is not None:
            data["otherParamsJson"] = draft.other_params_json
        async with self._client() as client:
            response = await client.post(f"/projects/{project_code}/workflow-definition", data=data)
        payload = self._unwrap(response)
        return payload["data"]

    async def release_workflow_definition(self, project_code: int, workflow_code: int) -> bool:
        async with self._client() as client:
            response = await client.post(
                f"/projects/{project_code}/workflow-definition/{workflow_code}/release",
                params={"releaseState": "ONLINE"},
            )
        payload = self._unwrap(response)
        return bool(payload.get("data", True))

    async def start_workflow_instance(self, project_code: int, workflow_code: int) -> list[int]:
        data = {
            "workflowDefinitionCode": str(workflow_code),
            "scheduleTime": "",
            "failureStrategy": "CONTINUE",
            "taskDependType": "TASK_POST",
            "execType": "START_PROCESS",
            "warningType": "NONE",
            "workerGroup": "default",
            "tenantCode": "default",
            "environmentCode": "-1",
            "dryRun": "0",
        }
        async with self._client() as client:
            response = await client.post(f"/projects/{project_code}/executors/start-workflow-instance", data=data)
        payload = self._unwrap(response)
        return payload["data"]

    @asynccontextmanager
    async def _client(self) -> AsyncIterator[httpx.AsyncClient]:
        headers = {"token": self.token} if self.token else {}
        async with httpx.AsyncClient(
            base_url=self.base_url,
            headers=headers,
            timeout=30,
            transport=self.transport,
        ) as client:
            yield client

    def _unwrap(self, response: httpx.Response) -> dict:
        if response.status_code >= 400:
            raise DolphinSchedulerError(f"DolphinScheduler HTTP {response.status_code}", response.status_code)
        payload = response.json()
        if payload.get("code") not in (0, None):
            raise DolphinSchedulerError(payload.get("msg", "DolphinScheduler API error"), payload.get("code"))
        return payload
```

- [ ] **Step 7: Run integration client tests**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/integrations/test_dolphinscheduler_client.py -v
```

Expected: PASS.

- [ ] **Step 8: Commit**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio
git add backend/app/config backend/app/integrations backend/tests/integrations
git commit -m "feat: add dolphinscheduler client"
```

Expected: commit succeeds.

---

### Task 7: Add Tool Layer and Agent Runtime

**Files:**
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/tools/workflow_tools.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/tools/datasource_tools.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/rag/service.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/storage/repository.py`
- Create: `/Users/luwang/bigdata-build/agent-studio/backend/app/agents/data_sync/graph.py`
- Modify: `/Users/luwang/bigdata-build/agent-studio/backend/tests/api/test_routes.py`

- [ ] **Step 1: Create service directories**

Run:

```bash
mkdir -p /Users/luwang/bigdata-build/agent-studio/backend/app/tools
mkdir -p /Users/luwang/bigdata-build/agent-studio/backend/app/rag
mkdir -p /Users/luwang/bigdata-build/agent-studio/backend/app/storage
```

Expected: directories exist and no output.

- [ ] **Step 2: Add failing API orchestration test**

Append to `/Users/luwang/bigdata-build/agent-studio/backend/tests/api/test_routes.py`:

```python
def test_data_sync_plan_endpoint_returns_plan_and_risk():
    client = TestClient(app)

    response = client.post(
        "/api/data-sync/plan",
        json={
            "raw_text": "每天凌晨2点把 MySQL crm.customer 增量同步到 PostgreSQL dw.customer，按 update_time 增量",
            "source": {"datasource_name": "mysql_crm", "database": "crm", "table": "customer"},
            "target": {"datasource_name": "postgres_dw", "database": "dw", "table": "customer"},
            "sync_mode": "incremental",
            "incremental_column": "update_time",
            "schedule_text": "每天凌晨2点"
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["plan"]["name"] == "customer_incremental_sync"
    assert body["risk"]["level"] == "low"
    assert body["workflow_draft"]["name"] == "customer_incremental_sync"
```

- [ ] **Step 3: Run endpoint test to verify it fails**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/api/test_routes.py::test_data_sync_plan_endpoint_returns_plan_and_risk -v
```

Expected: FAIL with 404 for `/api/data-sync/plan`.

- [ ] **Step 4: Implement MVP RAG service stub**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/rag/service.py`:

```python
class RagService:
    def retrieve(self, query: str) -> list[str]:
        return [
            "MVP uses DolphinScheduler existing task types for sync execution.",
            "Relational incremental sync should use a stable time or numeric column.",
        ]
```

- [ ] **Step 5: Implement MVP repository**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/storage/repository.py`:

```python
from dataclasses import dataclass, field
from uuid import uuid4


@dataclass
class InMemoryRepository:
    records: dict[str, dict] = field(default_factory=dict)

    def save_record(self, payload: dict) -> str:
        record_id = str(uuid4())
        self.records[record_id] = payload
        return record_id

    def get_record(self, record_id: str) -> dict | None:
        return self.records.get(record_id)
```

- [ ] **Step 6: Implement workflow tools**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/tools/workflow_tools.py`:

```python
from app.agents.data_sync.workflow_builder import build_workflow_draft
from app.schemas.data_sync import SyncPlan, WorkflowDraft


class WorkflowTools:
    def build_draft(self, plan: SyncPlan) -> WorkflowDraft:
        return build_workflow_draft(plan)
```

- [ ] **Step 7: Implement datasource tools with deterministic MVP metadata**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/tools/datasource_tools.py`:

```python
class DatasourceTools:
    def inspect_metadata(self, source_table: str, target_table: str) -> dict:
        return {
            "source_columns": [
                {"name": "id", "type": "BIGINT"},
                {"name": "name", "type": "VARCHAR"},
                {"name": "update_time", "type": "TIMESTAMP"},
            ],
            "target_columns": [
                {"name": "id", "type": "BIGINT"},
                {"name": "name", "type": "VARCHAR"},
                {"name": "update_time", "type": "TIMESTAMP"},
            ],
            "target_exists": True,
        }
```

- [ ] **Step 8: Implement Data Sync graph service**

Create `/Users/luwang/bigdata-build/agent-studio/backend/app/agents/data_sync/graph.py`:

```python
from app.agents.data_sync.planner import build_plan_from_intent
from app.agents.data_sync.risk import assess_risk
from app.rag.service import RagService
from app.schemas.data_sync import SyncIntent
from app.storage.repository import InMemoryRepository
from app.tools.datasource_tools import DatasourceTools
from app.tools.workflow_tools import WorkflowTools


class DataSyncAgentRuntime:
    def __init__(
        self,
        rag: RagService | None = None,
        datasource_tools: DatasourceTools | None = None,
        workflow_tools: WorkflowTools | None = None,
        repository: InMemoryRepository | None = None,
    ):
        self.rag = rag or RagService()
        self.datasource_tools = datasource_tools or DatasourceTools()
        self.workflow_tools = workflow_tools or WorkflowTools()
        self.repository = repository or InMemoryRepository()

    def create_plan(self, intent: SyncIntent) -> dict:
        if intent.source is None or intent.target is None:
            raise ValueError("源端和目标端信息不能为空")
        knowledge = self.rag.retrieve(intent.raw_text)
        metadata = self.datasource_tools.inspect_metadata(intent.source.table, intent.target.table)
        plan = build_plan_from_intent(intent, metadata)
        risk = assess_risk(plan)
        workflow_draft = self.workflow_tools.build_draft(plan)
        record_id = self.repository.save_record(
            {
                "intent": intent.model_dump(mode="json"),
                "plan": plan.model_dump(mode="json"),
                "risk": risk.model_dump(mode="json"),
                "workflow_draft": workflow_draft.model_dump(mode="json"),
                "knowledge": knowledge,
            }
        )
        return {
            "record_id": record_id,
            "knowledge": knowledge,
            "plan": plan,
            "risk": risk,
            "workflow_draft": workflow_draft,
        }
```

- [ ] **Step 9: Add plan endpoint**

Modify `/Users/luwang/bigdata-build/agent-studio/backend/app/api/routes.py` to:

```python
from fastapi import APIRouter

from app.agents.data_sync.graph import DataSyncAgentRuntime
from app.schemas.data_sync import SyncIntent

router = APIRouter(prefix="/api")
runtime = DataSyncAgentRuntime()


@router.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "agent-studio"}


@router.post("/data-sync/plan")
def create_data_sync_plan(intent: SyncIntent) -> dict:
    result = runtime.create_plan(intent)
    return {
        "record_id": result["record_id"],
        "knowledge": result["knowledge"],
        "plan": result["plan"].model_dump(mode="json"),
        "risk": result["risk"].model_dump(mode="json"),
        "workflow_draft": result["workflow_draft"].model_dump(mode="json"),
    }
```

- [ ] **Step 10: Run API tests**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/api/test_routes.py -v
```

Expected: PASS.

- [ ] **Step 11: Commit**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio
git add backend/app/tools backend/app/rag backend/app/storage backend/app/agents/data_sync/graph.py backend/app/api/routes.py backend/tests/api/test_routes.py
git commit -m "feat: add data sync agent runtime"
```

Expected: commit succeeds.

---

### Task 8: Add Execute Endpoint with Confirmation Gate

**Files:**
- Modify: `/Users/luwang/bigdata-build/agent-studio/backend/app/agents/data_sync/graph.py`
- Modify: `/Users/luwang/bigdata-build/agent-studio/backend/app/api/routes.py`
- Modify: `/Users/luwang/bigdata-build/agent-studio/backend/tests/api/test_routes.py`

- [ ] **Step 1: Add failing confirmation gate tests**

Append to `/Users/luwang/bigdata-build/agent-studio/backend/tests/api/test_routes.py`:

```python
def test_execute_requires_confirmation_for_medium_risk():
    client = TestClient(app)
    plan_response = client.post(
        "/api/data-sync/plan",
        json={
            "raw_text": "每天凌晨2点把 MySQL crm.customer 增量同步到 PostgreSQL dw.customer，按 update_time 增量",
            "source": {"datasource_name": "mysql_crm", "database": "crm", "table": "customer"},
            "target": {"datasource_name": "postgres_dw", "database": "dw", "table": "customer"},
            "sync_mode": "incremental",
            "incremental_column": "update_time",
            "schedule_text": "每天凌晨2点"
        },
    )
    record_id = plan_response.json()["record_id"]

    response = client.post(f"/api/data-sync/execute/{record_id}", json={"confirmed": False})

    assert response.status_code == 409
    assert response.json()["detail"] == "执行前需要用户确认"
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/api/test_routes.py::test_execute_requires_confirmation_for_medium_risk -v
```

Expected: FAIL with 404 for execute endpoint.

- [ ] **Step 3: Add execute method to runtime**

Modify `/Users/luwang/bigdata-build/agent-studio/backend/app/agents/data_sync/graph.py` by adding this method inside `DataSyncAgentRuntime`:

```python
    def execute_plan(self, record_id: str, confirmed: bool) -> dict:
        record = self.repository.get_record(record_id)
        if record is None:
            raise KeyError(record_id)
        risk = record["risk"]
        if risk["requires_confirmation"] and not confirmed:
            return {"status": "blocked", "message": "执行前需要用户确认"}
        return {
            "status": "submitted",
            "workflow_code": None,
            "instance_ids": [],
            "message": "MVP 已通过确认门禁；真实 DolphinScheduler 提交将在 DS 连接配置完成后执行",
        }
```

- [ ] **Step 4: Add execute endpoint**

Modify `/Users/luwang/bigdata-build/agent-studio/backend/app/api/routes.py` to include:

```python
from pydantic import BaseModel
from fastapi import HTTPException
```

Add this request model and route after `create_data_sync_plan`:

```python
class ExecuteRequest(BaseModel):
    confirmed: bool = False


@router.post("/data-sync/execute/{record_id}")
def execute_data_sync_plan(record_id: str, request: ExecuteRequest) -> dict:
    try:
        result = runtime.execute_plan(record_id, confirmed=request.confirmed)
    except KeyError as exc:
        raise HTTPException(status_code=404, detail="执行记录不存在") from exc
    if result["status"] == "blocked":
        raise HTTPException(status_code=409, detail=result["message"])
    return result
```

- [ ] **Step 5: Run API tests**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest tests/api/test_routes.py -v
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio
git add backend/app/agents/data_sync/graph.py backend/app/api/routes.py backend/tests/api/test_routes.py
git commit -m "feat: gate data sync execution by risk"
```

Expected: commit succeeds.

---

### Task 9: Create Frontend Workbench

**Files:**
- Create: `/Users/luwang/bigdata-build/agent-studio/frontend/package.json`
- Create: `/Users/luwang/bigdata-build/agent-studio/frontend/index.html`
- Create: `/Users/luwang/bigdata-build/agent-studio/frontend/src/main.tsx`
- Create: `/Users/luwang/bigdata-build/agent-studio/frontend/src/App.tsx`
- Create: `/Users/luwang/bigdata-build/agent-studio/frontend/src/types.ts`
- Create: `/Users/luwang/bigdata-build/agent-studio/frontend/src/api/client.ts`
- Create: `/Users/luwang/bigdata-build/agent-studio/frontend/src/components/DataSyncWorkbench.tsx`
- Create: `/Users/luwang/bigdata-build/agent-studio/frontend/src/components/PlanPreview.tsx`
- Create: `/Users/luwang/bigdata-build/agent-studio/frontend/src/components/RiskBanner.tsx`
- Create: `/Users/luwang/bigdata-build/agent-studio/frontend/src/__tests__/DataSyncWorkbench.test.tsx`

- [ ] **Step 1: Create frontend directories**

Run:

```bash
mkdir -p /Users/luwang/bigdata-build/agent-studio/frontend/src/api
mkdir -p /Users/luwang/bigdata-build/agent-studio/frontend/src/components
mkdir -p /Users/luwang/bigdata-build/agent-studio/frontend/src/__tests__
```

Expected: directories exist and no output.

- [ ] **Step 2: Add frontend package metadata**

Create `/Users/luwang/bigdata-build/agent-studio/frontend/package.json`:

```json
{
  "name": "agent-studio-frontend",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "tsc && vite build",
    "test": "vitest run"
  },
  "dependencies": {
    "@vitejs/plugin-react": "^4.2.0",
    "vite": "^5.2.0",
    "typescript": "^5.4.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0"
  },
  "devDependencies": {
    "@testing-library/jest-dom": "^6.4.0",
    "@testing-library/react": "^15.0.0",
    "@testing-library/user-event": "^14.5.0",
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "jsdom": "^24.0.0",
    "vitest": "^1.5.0"
  }
}
```

- [ ] **Step 3: Write failing frontend test**

Create `/Users/luwang/bigdata-build/agent-studio/frontend/src/__tests__/DataSyncWorkbench.test.tsx`:

```tsx
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { DataSyncWorkbench } from "../components/DataSyncWorkbench";

describe("DataSyncWorkbench", () => {
  it("renders natural language input and primary action", () => {
    render(<DataSyncWorkbench />);

    expect(screen.getByText("数据同步 Agent")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("描述你的同步需求")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "生成同步方案" })).toBeInTheDocument();
  });
});
```

- [ ] **Step 4: Run frontend test to verify it fails**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/frontend
npm install
npm test -- DataSyncWorkbench.test.tsx
```

Expected: FAIL because `DataSyncWorkbench` does not exist.

- [ ] **Step 5: Add frontend entry files**

Create `/Users/luwang/bigdata-build/agent-studio/frontend/index.html`:

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Agent Studio</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

Create `/Users/luwang/bigdata-build/agent-studio/frontend/src/main.tsx`:

```tsx
import React from "react";
import ReactDOM from "react-dom/client";

import { App } from "./App";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
```

Create `/Users/luwang/bigdata-build/agent-studio/frontend/src/App.tsx`:

```tsx
import { DataSyncWorkbench } from "./components/DataSyncWorkbench";

export function App() {
  return <DataSyncWorkbench />;
}
```

- [ ] **Step 6: Add frontend types and API client**

Create `/Users/luwang/bigdata-build/agent-studio/frontend/src/types.ts`:

```tsx
export type RiskLevel = "low" | "medium" | "high";

export interface DataSyncPlanResponse {
  record_id: string;
  plan: {
    name: string;
    sync_mode: "full" | "incremental";
    incremental_column: string | null;
    ds_task_type: string;
  };
  risk: {
    level: RiskLevel;
    reasons: string[];
    requires_confirmation: boolean;
  };
  workflow_draft: {
    name: string;
    description: string;
  };
}
```

Create `/Users/luwang/bigdata-build/agent-studio/frontend/src/api/client.ts`:

```tsx
import type { DataSyncPlanResponse } from "../types";

const API_BASE = "http://localhost:8000/api";

export async function createDataSyncPlan(rawText: string): Promise<DataSyncPlanResponse> {
  const response = await fetch(`${API_BASE}/data-sync/plan`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      raw_text: rawText,
      source: { datasource_name: "mysql_crm", database: "crm", table: "customer" },
      target: { datasource_name: "postgres_dw", database: "dw", table: "customer" },
      sync_mode: "incremental",
      incremental_column: "update_time",
      schedule_text: "每天凌晨2点",
    }),
  });
  if (!response.ok) {
    throw new Error(`请求失败: ${response.status}`);
  }
  return response.json();
}
```

- [ ] **Step 7: Add preview components**

Create `/Users/luwang/bigdata-build/agent-studio/frontend/src/components/RiskBanner.tsx`:

```tsx
import type { RiskLevel } from "../types";

interface Props {
  level: RiskLevel;
  reasons: string[];
}

export function RiskBanner({ level, reasons }: Props) {
  const label = level === "low" ? "低风险" : level === "medium" ? "中风险" : "高风险";
  return (
    <section>
      <h3>风险评估：{label}</h3>
      {reasons.length === 0 ? <p>当前方案未发现需要确认的风险。</p> : null}
      {reasons.map((reason) => (
        <p key={reason}>{reason}</p>
      ))}
    </section>
  );
}
```

Create `/Users/luwang/bigdata-build/agent-studio/frontend/src/components/PlanPreview.tsx`:

```tsx
import type { DataSyncPlanResponse } from "../types";
import { RiskBanner } from "./RiskBanner";

interface Props {
  result: DataSyncPlanResponse;
}

export function PlanPreview({ result }: Props) {
  return (
    <section>
      <h2>同步方案预览</h2>
      <p>方案名称：{result.plan.name}</p>
      <p>同步模式：{result.plan.sync_mode}</p>
      <p>增量字段：{result.plan.incremental_column ?? "无"}</p>
      <p>DS 任务类型：{result.plan.ds_task_type}</p>
      <p>工作流草案：{result.workflow_draft.name}</p>
      <RiskBanner level={result.risk.level} reasons={result.risk.reasons} />
    </section>
  );
}
```

- [ ] **Step 8: Add workbench component**

Create `/Users/luwang/bigdata-build/agent-studio/frontend/src/components/DataSyncWorkbench.tsx`:

```tsx
import { useState } from "react";

import { createDataSyncPlan } from "../api/client";
import type { DataSyncPlanResponse } from "../types";
import { PlanPreview } from "./PlanPreview";

export function DataSyncWorkbench() {
  const [rawText, setRawText] = useState("每天凌晨2点把 MySQL crm.customer 增量同步到 PostgreSQL dw.customer，按 update_time 增量");
  const [result, setResult] = useState<DataSyncPlanResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit() {
    setError(null);
    try {
      setResult(await createDataSyncPlan(rawText));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "请求失败");
    }
  }

  return (
    <main style={{ maxWidth: 1120, margin: "0 auto", padding: 24, fontFamily: "system-ui, sans-serif" }}>
      <h1>Agent Studio</h1>
      <section>
        <h2>数据同步 Agent</h2>
        <textarea
          aria-label="同步需求"
          placeholder="描述你的同步需求"
          value={rawText}
          onChange={(event) => setRawText(event.target.value)}
          style={{ width: "100%", minHeight: 120 }}
        />
        <button onClick={handleSubmit}>生成同步方案</button>
      </section>
      {error ? <p role="alert">{error}</p> : null}
      {result ? <PlanPreview result={result} /> : null}
    </main>
  );
}
```

- [ ] **Step 9: Run frontend tests**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/frontend
npm test -- DataSyncWorkbench.test.tsx
```

Expected: PASS.

- [ ] **Step 10: Commit**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio
git add frontend
git commit -m "feat: add data sync workbench UI"
```

Expected: commit succeeds.

---

### Task 10: Add Local Development Documentation and Smoke Test

**Files:**
- Create: `/Users/luwang/bigdata-build/agent-studio/docs/local-development.md`
- Create: `/Users/luwang/bigdata-build/agent-studio/.gitignore`

- [ ] **Step 1: Create docs directory**

Run:

```bash
mkdir -p /Users/luwang/bigdata-build/agent-studio/docs
```

Expected: directory exists and no output.

- [ ] **Step 2: Add gitignore**

Create `/Users/luwang/bigdata-build/agent-studio/.gitignore`:

```gitignore
.env
.venv/
__pycache__/
.pytest_cache/
node_modules/
dist/
.DS_Store
.superpowers/
```

- [ ] **Step 3: Add local development guide**

Create `/Users/luwang/bigdata-build/agent-studio/docs/local-development.md`:

```markdown
# Agent Studio Local Development

## Prerequisites

- Conda environment: `RAG_PROJECT`
- Milvus scripts: `/Users/luwang/milvus_db_ok/`
- DolphinScheduler project: `/Users/luwang/bigdata-build/dolphinscheduler`
- Agent Studio project: `/Users/luwang/bigdata-build/agent-studio`

## Start Milvus

Use the existing local scripts:

```bash
cd /Users/luwang/milvus_db_ok
./自动启动Milvus和Attu.command
```

## Start Backend

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Health check:

```bash
curl http://localhost:8000/api/health
```

Expected:

```json
{"status":"ok","service":"agent-studio"}
```

## Start Frontend

```bash
cd /Users/luwang/bigdata-build/agent-studio/frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## Run Tests

Backend:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest -v
```

Frontend:

```bash
cd /Users/luwang/bigdata-build/agent-studio/frontend
npm test
```

## MVP Smoke Path

1. Open the Web console.
2. Keep the default requirement:
   `每天凌晨2点把 MySQL crm.customer 增量同步到 PostgreSQL dw.customer，按 update_time 增量`
3. Click `生成同步方案`.
4. Confirm the plan preview shows:
   - `customer_incremental_sync`
   - sync mode `incremental`
   - incremental column `update_time`
   - DS task type `DATAX`
   - risk level `low`
```

- [ ] **Step 4: Run full backend test suite**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/backend
conda run -n RAG_PROJECT pytest -v
```

Expected: PASS for all backend tests.

- [ ] **Step 5: Run frontend test suite**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/frontend
npm test
```

Expected: PASS for all frontend tests.

- [ ] **Step 6: Build frontend**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio/frontend
npm run build
```

Expected: build succeeds and creates `dist/`.

- [ ] **Step 7: Commit**

Run:

```bash
cd /Users/luwang/bigdata-build/agent-studio
git add .gitignore docs/local-development.md
git commit -m "docs: add local development guide"
```

Expected: commit succeeds.

---

## Plan Self-Review

### Spec Coverage

- 独立项目：Task 1 creates `/Users/luwang/bigdata-build/agent-studio`.
- Web 控制台优先：Task 9 creates React/Vite workbench.
- 最小平台底座：Task 1, Task 7, and Task 10 create backend API, storage, docs.
- 数据同步 Agent：Task 2 through Task 8 define schemas, planner, risk, workflow draft, runtime, and execution gate.
- 关系型数据库到关系型数据库：Task 4 and Task 9 use MySQL to PostgreSQL examples.
- 全量 / 增量 / 定时：schemas support both modes; tests cover incremental and cron; full mode is accepted by `SyncMode.FULL`.
- 使用 DS 现有任务类型：Task 5 builds `DATAX` workflow draft.
- 复用或创建 DS 数据源：Task 6 and Task 7 define DS client and datasource tool boundary. Actual DS datasource creation is intentionally deferred until real DS connection details are configured.
- 风险确认：Task 3 and Task 8 implement deterministic confirmation gate.
- RAG / Milvus：Task 7 creates a swappable `RagService` stub so Milvus integration can replace it without changing Agent flow.

### Known MVP Limitations

- The first runtime uses deterministic metadata and RAG stubs so the vertical slice can be tested without a live DolphinScheduler or Milvus instance.
- Real datasource listing, datasource creation, Milvus retrieval, and live DS workflow submission should be implemented in the next plan after this MVP shell passes tests.
- The DataX task parameter format is a stable internal draft for Agent Studio tests. It must be validated against the target DolphinScheduler DataX plugin before production use.

### Placeholder Scan

The plan contains no unresolved placeholder markers or undefined task references. The known limitations are explicit scope boundaries, not placeholders.

### Type Consistency

The names `SyncIntent`, `SyncPlan`, `RiskAssessment`, `WorkflowDraft`, `DataSyncAgentRuntime`, `build_plan_from_intent`, `assess_risk`, and `build_workflow_draft` are introduced before use and remain consistent across tasks.
