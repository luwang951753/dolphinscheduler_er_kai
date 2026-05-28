# Sync Task Source Filters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add structured source-side filtering to the sync-task wizard so users can constrain source reads with DataGrip/Fivetran-style conditions and see those conditions carried into generated SeaTunnel config.

**Architecture:** Extend the sync-task step 1 state with a first-class source-filter collection, render it inside the source card, and thread the filters through SeaTunnel config generation as structured WHERE/predicate clauses. Keep the UX bounded: no freeform SQL editor, no extra step, and a small condition cap so the feature stays usable inside the existing wizard.

**Tech Stack:** Vue 3, TypeScript, Naive UI, existing sync-task SeaTunnel config generation, existing sync-task test matrix and e2e click test docs.

---

### Task 1: Model and generate structured source filters

**Files:**
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.tsx`
- Test: `dolphinscheduler-ui/src/views/sync-task/index.tsx` (manual smoke via browser after build)

- [ ] **Step 1: Write the failing test**

No automated test harness exists for this wizard state; verify the current generated config lacks any source filter section after adding a filter-shaped state record in the UI model.

- [ ] **Step 2: Run test to verify it fails**

Run: `rg -n "source_filter|filters|WHERE" dolphinscheduler-ui/src/views/sync-task/index.tsx`
Expected: no source-filter generation path in the sync-task config builder.

- [ ] **Step 3: Write minimal implementation**

Add a `SourceFilterRule` model, populate it from step 1 UI state, and append the generated structured filter into the source SQL / SeaTunnel query path with a bounded condition list.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm -C dolphinscheduler-ui exec vue-tsc --noEmit`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add dolphinscheduler-ui/src/views/sync-task/index.tsx
git commit -m "feat(sync-task): add structured source filters"
```

### Task 2: Render source filters in step 1 and keep the wizard readable

**Files:**
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.tsx`
- Modify: `dolphinscheduler-ui/src/views/sync-task/index.module.scss`

- [ ] **Step 1: Write the failing test**

Open the current sync-task wizard in the browser and confirm there is no source-filter section inside the source card.

- [ ] **Step 2: Run test to verify it fails**

Run: `rg -n "源端过滤条件|过滤条件" dolphinscheduler-ui/src/views/sync-task/index.tsx dolphinscheduler-ui/src/views/sync-task/index.module.scss`
Expected: no rendered source-filter section.

- [ ] **Step 3: Write minimal implementation**

Render a compact condition builder inside the source card with field/operator/value controls, add/remove/disable actions, and a small counter. Keep the section visually nested in the source card so step 1 remains a working wizard page, not a separate editor.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm -C dolphinscheduler-ui exec vue-tsc --noEmit`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add dolphinscheduler-ui/src/views/sync-task/index.tsx dolphinscheduler-ui/src/views/sync-task/index.module.scss
git commit -m "feat(sync-task): render source filter builder"
```

### Task 3: Update docs and regression cases

**Files:**
- Modify: `.ai/sync-task/core/change-log.md`
- Modify: `.ai/sync-task/core/req.md`
- Modify: `.ai/sync-task/product/prd.md`
- Modify: `.ai/sync-task/product/interaction-design.md`
- Modify: `.ai/sync-task/ui-wizard/ui.md`
- Modify: `.ai/sync-task/test/test-matrix.md`
- Modify: `.ai/quality/e2e-click-testing/test-cases.md`

- [ ] **Step 1: Write the failing test**

Confirm the docs currently lack a dedicated source-filter implementation section in the sync-task test matrix and e2e cases.

- [ ] **Step 2: Run test to verify it fails**

Run: `rg -n "源端过滤条件|过滤条件" .ai/sync-task .ai/quality/e2e-click-testing/test-cases.md`
Expected: the new behavior is not yet fully represented everywhere.

- [ ] **Step 3: Write minimal implementation**

Synchronize the docs so the feature is described once in requirements, once in UI behavior, and once in test coverage with dedicated P0/P1/P2 cases.

- [ ] **Step 4: Run test to verify it passes**

Run: `rg -n "源端过滤条件|TC-P0-002A|TC-P1-005" .ai/sync-task .ai/quality/e2e-click-testing/test-cases.md`
Expected: PASS with the new references present.

- [ ] **Step 5: Commit**

```bash
git add .ai/sync-task/core/change-log.md .ai/sync-task/core/req.md .ai/sync-task/product/prd.md .ai/sync-task/product/interaction-design.md .ai/sync-task/ui-wizard/ui.md .ai/sync-task/test/test-matrix.md .ai/quality/e2e-click-testing/test-cases.md
git commit -m "docs(sync-task): add source filter requirements"
```

### Self-review

- [ ] Step through the sync-task wizard flow and confirm step 1 remains readable.
- [ ] Check that source filters are structured and capped, not freeform SQL.
- [ ] Check that generated SeaTunnel config includes the filter chain.
- [ ] Check that the new test cases map to the documented behavior.
