/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  defineComponent,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  computed,
  ref,
  watch,
  h
} from 'vue'
import { useRouter } from 'vue-router'
import type { Router } from 'vue-router'
import {
  NAlert,
  NButton,
  NCheckbox,
  NDataTable,
  NDrawer,
  NDrawerContent,
  NEmpty,
  NInput,
  NRadioButton,
  NRadioGroup,
  NSelect,
  NSpace,
  NSpin,
  NTag,
  NThing,
  NSteps,
  NStep,
  NDescriptions,
  NDescriptionsItem
} from 'naive-ui'
import type { DataTableColumns, SelectOption } from 'naive-ui'
import { format } from 'date-fns'
import Card from '@/components/card'
import MonacoEditor from '@/components/monaco-editor'
import TimingModal from './timing-modal'
import utils from '@/utils'
import {
  queryDataSourceList,
  getDatasourceDatabasesById,
  getDatasourceTablesById,
  getDatasourceTableColumnMetasById,
  createDatasourceTargetTable,
  previewDatasourceTargetTable
} from '@/service/modules/data-source'
import { queryAllProjectList } from '@/service/modules/projects'
import {
  createWorkflowDefinition,
  verifyName,
  queryWorkflowDefinitionByName,
  queryWorkflowDefinitionByCode,
  queryListPaging as queryWorkflowDefinitionListPaging,
  updateWorkflowDefinition,
  release
} from '@/service/modules/workflow-definition'
import { genTaskCodeList } from '@/service/modules/task-definition'
import { startWorkflowInstance } from '@/service/modules/executors'
import {
  online,
  queryScheduleListPaging
} from '@/service/modules/schedules'
import {
  queryWorkflowInstanceById,
  queryWorkflowInstanceListPaging,
  queryTaskListByWorkflowId
} from '@/service/modules/workflow-instances'
import { queryLog } from '@/service/modules/log'
import { registerGovernanceSyncTaskLineage } from '@/service/modules/data-governance'
import styles from './index.module.scss'

type SyncDatasourceType = 'MYSQL' | 'POSTGRESQL' | 'ORACLE' | 'DORIS'
type ExecutionMode = 'IMMEDIATE' | 'SCHEDULE'
type TargetTableMode = 'CREATE_TABLE' | 'EXISTING_TABLE'
type MappingKind = 'AUTO' | 'MANUAL'
type TargetNameRule = 'KEEP_SOURCE' | 'LOWERCASE' | 'UPPERCASE'
type SyncSolutionModule = 'MAPPING' | 'FILTER' | 'SINK' | 'PROCESSING'
type SyncTaskViewMode = 'LIST' | 'WIZARD'
type SyncTaskAssetStatus = 'SUCCESS' | 'FAILED' | 'RUNNING' | 'DRAFT' | 'OFFLINE'
type SyncTaskDetailTab = 'OVERVIEW' | 'CONFIG' | 'HISTORY' | 'LOGS' | 'CHANGES'
type SyncTaskAssetSource = 'REAL' | 'LOCAL'
type SyncAgentStageKey = 'PARSE' | 'MATCH' | 'METADATA' | 'MAPPING' | 'PLAN'
type SyncAgentStageStatus = 'WAITING' | 'RUNNING' | 'SUCCESS' | 'ERROR'

interface DatasourceOption extends SelectOption {
  value: number
  label: string
  type: SyncDatasourceType
}

interface ColumnItem {
  name: string
  type: string
  key: string
  nullable?: boolean
  primaryKey?: boolean
  comment?: string
}

interface DatasourceDetail {
  id: number
  name: string
  type: SyncDatasourceType
  host: string
  port: number
  userName: string
  password: string
  database: string
}

interface DatasourceRecord {
  id: number
  name: string
  type: SyncDatasourceType
  host?: string
  port?: number | string
  userName?: string
  dbUser?: string
  password?: string
  database?: string
  connectionParams?: string | Record<string, any>
}

interface ProjectOption extends SelectOption {
  value: number
  label: string
}

interface MappingRow {
  key: string
  sourceColumn: string
  sourceType: string
  targetColumn: string
  targetType: string
  sync: boolean
  mappedTargetKey?: string | null
  targetPrimaryKey?: boolean
  mappingKind?: MappingKind
}

interface FieldDesignRow {
  key: string
  sourceColumn: string
  sourceType: string
  sourceComment: string
  sourcePrimaryKey: boolean
  sourceNullable: boolean
  targetColumn: string
  targetType: string
  targetComment: string
  targetPrimaryKey: boolean
  targetNullable: boolean
  sync: boolean
  mappedTargetKey: string | null
  mappingKind?: MappingKind
  targetColumnTouched?: boolean
}

type SourceFilterOperator =
  | 'EQ'
  | 'NE'
  | 'GT'
  | 'LT'
  | 'GTE'
  | 'LTE'
  | 'CONTAINS'
  | 'PREFIX'
  | 'IN'
  | 'BETWEEN'
  | 'IS_NULL'
  | 'NOT_NULL'

interface SourceFilterRule {
  key: string
  enabled: boolean
  field: string
  operator: SourceFilterOperator
  value: string
  valueEnd: string
}

interface SyncTaskAsset {
  id: string
  name: string
  projectCode: number | null
  projectName: string
  status: SyncTaskAssetStatus
  scheduleStatus: 'ON' | 'OFF'
  sourceType: SyncDatasourceType
  targetType: SyncDatasourceType
  sourceName: string
  sourcePath: string
  targetName: string
  targetPath: string
  workflowCode: number | null
  workflowName: string
  workflowVersion: number
  lastRunTime: string
  lastInstanceId: number | null
  readRows: number | null
  writeRows: number | null
  duration: string
  updatedAt: string
  owner: string
  errorMessage: string
  sourceFilters: SourceFilterRule[]
  sinkCustomSql: string
  fieldRows: FieldDesignRow[]
  sourceColumns: ColumnItem[]
  targetColumns: ColumnItem[]
  configText: string
  history: Array<{
    id: string
    status: SyncTaskAssetStatus
    trigger: string
    startTime: string
    endTime: string
    duration: string
    rows: string
  }>
  changes: Array<{
    time: string
    user: string
    action: string
  }>
  source?: SyncTaskAssetSource
  logLoading?: boolean
  logLoaded?: boolean
  logError?: string
  logText?: string
}

interface SyncAgentParsedIntent {
  command: string
  sourceType: SyncDatasourceType | null
  targetType: SyncDatasourceType | null
  sourceDatabase: string
  sourceTable: string
  targetDatabase: string
  targetSchema: string
  targetTable: string
  limit: number | null
  autoExecute: boolean
  confidence: number
  warnings: string[]
  missing: string[]
}

interface SyncAgentPlan extends SyncAgentParsedIntent {
  projectCode: number | null
  projectName: string
  sourceDatasourceId: number | null
  sourceDatasourceName: string
  targetDatasourceId: number | null
  targetDatasourceName: string
  sourceColumnCount: number
  mappedColumnCount: number
}

interface SyncAgentStage {
  key: SyncAgentStageKey
  label: string
  status: SyncAgentStageStatus
  message: string
}

const SOURCE_FILTER_OPERATOR_LABELS: Record<SourceFilterOperator, string> = {
  EQ: '=',
  NE: '!=',
  GT: '>',
  LT: '<',
  GTE: '>=',
  LTE: '<=',
  CONTAINS: '包含',
  PREFIX: '前缀',
  IN: 'IN',
  BETWEEN: '区间',
  IS_NULL: '为空',
  NOT_NULL: '不为空'
}

const SOURCE_FILTER_MAX_COUNT = 3

const solutionModules = [
  {
    key: 'MAPPING',
    title: '字段映射关系',
    tag: '核心步骤',
    desc: '配置源字段、目标字段和映射连线'
  },
  {
    key: 'FILTER',
    title: '源端过滤条件',
    tag: '可配置',
    desc: '按字段限制源端读取范围'
  },
  {
    key: 'SINK',
    title: '数据去向',
    tag: '可配置',
    desc: '配置同步前 SQL（custom_sql）'
  },
  {
    key: 'PROCESSING',
    title: '数据处理',
    tag: '暂未实现',
    desc: '预留字符串替换、AI 处理、向量化能力'
  }
] as const

const createSourceFilterRule = (seed = 1): SourceFilterRule => ({
  key: `filter-${seed}-${Date.now()}`,
  enabled: true,
  field: '',
  operator: 'EQ',
  value: '',
  valueEnd: ''
})

const cloneSourceFilters = (filters: SourceFilterRule[]): SourceFilterRule[] =>
  filters.map((item, index) => ({
    ...item,
    key: item.key || `filter-clone-${index}-${Date.now()}`
  }))

const cloneFieldRows = (rows: FieldDesignRow[]): FieldDesignRow[] =>
  rows.map((item) => ({ ...item }))

const cloneColumns = (columns: ColumnItem[]): ColumnItem[] =>
  columns.map((item) => ({ ...item }))

const SYNC_TASK_ASSET_STORAGE_KEY = 'dolphinscheduler.sync-task.assets.v1'

const createDemoAssetRows = (): FieldDesignRow[] => [
  {
    key: 'id',
    sourceColumn: 'id',
    sourceType: 'BIGINT',
    sourceComment: '主键 ID',
    sourcePrimaryKey: true,
    sourceNullable: false,
    targetColumn: 'id',
    targetType: 'BIGINT',
    targetComment: '主键 ID',
    targetPrimaryKey: true,
    targetNullable: false,
    sync: true,
    mappedTargetKey: 'id',
    mappingKind: 'AUTO',
    targetColumnTouched: false
  },
  {
    key: 'ajbh',
    sourceColumn: 'ajbh',
    sourceType: 'VARCHAR(64)',
    sourceComment: '案件编号',
    sourcePrimaryKey: false,
    sourceNullable: false,
    targetColumn: 'ajbh',
    targetType: 'VARCHAR(64)',
    targetComment: '案件编号',
    targetPrimaryKey: false,
    targetNullable: false,
    sync: true,
    mappedTargetKey: 'ajbh',
    mappingKind: 'AUTO',
    targetColumnTouched: false
  },
  {
    key: 'ajmc',
    sourceColumn: 'ajmc',
    sourceType: 'VARCHAR(255)',
    sourceComment: '案件名称',
    sourcePrimaryKey: false,
    sourceNullable: true,
    targetColumn: 'case_name',
    targetType: 'VARCHAR(255)',
    targetComment: '案件名称',
    targetPrimaryKey: false,
    targetNullable: true,
    sync: true,
    mappedTargetKey: 'ajmc',
    mappingKind: 'MANUAL',
    targetColumnTouched: true
  },
  {
    key: 'update_time',
    sourceColumn: 'update_time',
    sourceType: 'DATETIME',
    sourceComment: '更新时间',
    sourcePrimaryKey: false,
    sourceNullable: true,
    targetColumn: 'synced_at',
    targetType: 'TIMESTAMP',
    targetComment: '同步时间',
    targetPrimaryKey: false,
    targetNullable: true,
    sync: true,
    mappedTargetKey: 'update_time',
    mappingKind: 'MANUAL',
    targetColumnTouched: true
  }
]

const createDemoAssets = (): SyncTaskAsset[] => {
  const rows = createDemoAssetRows()
  const sourceColumns = rows.map((item) => ({
    name: item.sourceColumn,
    type: item.sourceType,
    key: item.sourceColumn,
    nullable: item.sourceNullable,
    primaryKey: item.sourcePrimaryKey,
    comment: item.sourceComment
  }))
  const targetColumns = rows.map((item) => ({
    name: item.targetColumn,
    type: item.targetType,
    key: item.key,
    nullable: item.targetNullable,
    primaryKey: item.targetPrimaryKey,
    comment: item.targetComment
  }))
  return [
    {
      id: 'demo-success',
      name: 'sync_mysql_ajxx_to_pgsql_a6',
      projectCode: null,
      projectName: 'test1',
      status: 'SUCCESS',
      scheduleStatus: 'ON',
      sourceType: 'MYSQL',
      targetType: 'POSTGRESQL',
      sourceName: 'mysql_case_workbench',
      sourcePath: 'case_workbench.ajxx_tab',
      targetName: 'pgsql_test1',
      targetPath: 'test1.public.a6',
      workflowCode: 173455487604512,
      workflowName: 'sync_mysql_ajxx_to_pgsql_a6',
      workflowVersion: 3,
      lastRunTime: '2026-05-15 12:49',
      lastInstanceId: 161,
      readRows: 5,
      writeRows: 5,
      duration: '15s',
      updatedAt: '2026-05-15 12:49',
      owner: 'admin',
      errorMessage: '',
      sourceFilters: [
        {
          key: 'demo-filter-1',
          enabled: true,
          field: 'update_time',
          operator: 'GTE',
          value: '${bizdate}',
          valueEnd: ''
        }
      ],
      sinkCustomSql: 'truncate table public.a6;',
      fieldRows: rows,
      sourceColumns,
      targetColumns,
      configText: '',
      history: [
        {
          id: '161',
          status: 'SUCCESS',
          trigger: '手动运行',
          startTime: '2026-05-15 12:40:48',
          endTime: '2026-05-15 12:41:03',
          duration: '15s',
          rows: '5 / 5'
        },
        {
          id: '159',
          status: 'SUCCESS',
          trigger: '调度运行',
          startTime: '2026-05-15 12:31:56',
          endTime: '2026-05-15 12:35:11',
          duration: '3m15s',
          rows: '5 / 5'
        }
      ],
      changes: [
        { time: '2026-05-15 12:49', user: 'admin', action: '保存并执行同步任务' },
        { time: '2026-05-15 10:12', user: 'admin', action: '创建同步任务' }
      ]
    },
    {
      id: 'demo-failed',
      name: 'sync_oracle_order_to_doris_ods',
      projectCode: null,
      projectName: 'ods',
      status: 'FAILED',
      scheduleStatus: 'ON',
      sourceType: 'ORACLE',
      targetType: 'DORIS',
      sourceName: 'oracle_sync_test',
      sourcePath: 'APP.ORDERS',
      targetName: 'doris_sync_test',
      targetPath: 'ods.order_detail',
      workflowCode: 173455278088992,
      workflowName: 'sync_oracle_order_to_doris_ods',
      workflowVersion: 2,
      lastRunTime: '2026-05-15 11:42',
      lastInstanceId: 160,
      readRows: 1200,
      writeRows: 0,
      duration: '41s',
      updatedAt: '2026-05-15 11:43',
      owner: 'admin',
      errorMessage: '目标字段 amount 类型 DECIMAL(10,2) 无法承接 Oracle NUMBER(18,4)',
      sourceFilters: [
        {
          key: 'demo-filter-2',
          enabled: true,
          field: 'ORDER_TIME',
          operator: 'GTE',
          value: '${bizdate}',
          valueEnd: ''
        }
      ],
      sinkCustomSql: '',
      fieldRows: [
        {
          key: 'order_id',
          sourceColumn: 'ORDER_ID',
          sourceType: 'NUMBER(20)',
          sourceComment: '订单 ID',
          sourcePrimaryKey: true,
          sourceNullable: false,
          targetColumn: 'order_id',
          targetType: 'BIGINT',
          targetComment: '订单 ID',
          targetPrimaryKey: true,
          targetNullable: false,
          sync: true,
          mappedTargetKey: 'order_id',
          mappingKind: 'AUTO',
          targetColumnTouched: false
        },
        {
          key: 'amount',
          sourceColumn: 'AMOUNT',
          sourceType: 'NUMBER(18,4)',
          sourceComment: '订单金额',
          sourcePrimaryKey: false,
          sourceNullable: true,
          targetColumn: 'amount',
          targetType: 'DECIMAL(10,2)',
          targetComment: '订单金额',
          targetPrimaryKey: false,
          targetNullable: true,
          sync: true,
          mappedTargetKey: 'amount',
          mappingKind: 'MANUAL',
          targetColumnTouched: false
        },
        {
          key: 'order_time',
          sourceColumn: 'ORDER_TIME',
          sourceType: 'TIMESTAMP',
          sourceComment: '订单时间',
          sourcePrimaryKey: false,
          sourceNullable: true,
          targetColumn: 'order_time',
          targetType: 'DATETIME',
          targetComment: '订单时间',
          targetPrimaryKey: false,
          targetNullable: true,
          sync: true,
          mappedTargetKey: 'order_time',
          mappingKind: 'AUTO',
          targetColumnTouched: false
        }
      ],
      sourceColumns: [],
      targetColumns: [],
      configText: '',
      history: [
        {
          id: '160',
          status: 'FAILED',
          trigger: '调度运行',
          startTime: '2026-05-15 11:42:01',
          endTime: '2026-05-15 11:42:42',
          duration: '41s',
          rows: '1200 / 0'
        }
      ],
      changes: [
        { time: '2026-05-15 11:43', user: 'admin', action: '失败后进入修复' },
        { time: '2026-05-15 09:20', user: 'admin', action: '创建同步任务' }
      ]
    }
  ]
}

const escapeSeatunnelString = (value: string): string =>
  value.replaceAll('\\', '\\\\').replaceAll('"', '\\"')

interface WorkflowTaskProgressRow {
  key: string
  taskInstanceId: number | null
  name: string
  state: string
  stateLabel: string
  stateType: 'default' | 'info' | 'success' | 'warning' | 'error'
  startTime: string
  endTime: string
  host: string
}

interface EndpointState {
  datasourceId: number | null
  database: string | null
  table: string | null
  databases: string[]
  tables: string[]
  columns: ColumnItem[]
  loading: boolean
}

const SUPPORT_TYPES: SyncDatasourceType[] = ['MYSQL', 'POSTGRESQL', 'ORACLE', 'DORIS']
const SYNC_AGENT_EXAMPLES = [
  '把 mysql case_workbench.ajxx_tab 同步到 pg public.agent_ajxx_tab，只同步5条',
  '将 MySQL case_workbench.ajxx_tab 同步到 PostgreSQL public.agent_sync_test，字段自动映射并立即执行'
]
const DEFAULT_PORTS: Record<SyncDatasourceType, number> = {
  MYSQL: 3306,
  POSTGRESQL: 5432,
  ORACLE: 1521,
  DORIS: 9030
}
const PASSWORD_MASK = '******'
const TARGET_TABLE_EXISTS_PREFIX = '-- DS_TARGET_TABLE_ALREADY_EXISTS'
const RUN_PROGRESS_LABELS = {
  IDLE: '等待执行',
  PREPARING: '准备工作流',
  STARTING: '提交运行',
  MONITORING: '运行监控中',
  SUCCESS: '执行成功',
  FAILURE: '执行失败'
} as const
const TERMINAL_WORKFLOW_STATES = new Set(['SUCCESS', 'FAILURE', 'STOP', 'PAUSE'])
const WORKFLOW_STATE_META: Record<
  string,
  {
    label: string
    type: 'default' | 'info' | 'success' | 'warning' | 'error'
  }
> = {
  SUBMITTED_SUCCESS: {
    label: '已提交',
    type: 'info'
  },
  RUNNING_EXECUTION: {
    label: '运行中',
    type: 'info'
  },
  READY_PAUSE: {
    label: '准备暂停',
    type: 'warning'
  },
  PAUSE: {
    label: '已暂停',
    type: 'warning'
  },
  READY_STOP: {
    label: '准备停止',
    type: 'warning'
  },
  STOP: {
    label: '已停止',
    type: 'warning'
  },
  FAILURE: {
    label: '失败',
    type: 'error'
  },
  SUCCESS: {
    label: '成功',
    type: 'success'
  },
  SERIAL_WAIT: {
    label: '串行等待',
    type: 'default'
  }
}
const TARGET_TYPE_OPTIONS: Record<SyncDatasourceType, string[]> = {
  MYSQL: [
    'BIGINT',
    'INT',
    'INTEGER',
    'SMALLINT',
    'TINYINT',
    'DECIMAL(10,2)',
    'DOUBLE',
    'FLOAT',
    'VARCHAR(255)',
    'TEXT',
    'LONGTEXT',
    'DATE',
    'TIME',
    'DATETIME',
    'TIMESTAMP',
    'BOOLEAN',
    'JSON'
  ],
  POSTGRESQL: [
    'BIGINT',
    'INTEGER',
    'SMALLINT',
    'NUMERIC(10,2)',
    'DOUBLE PRECISION',
    'REAL',
    'VARCHAR(255)',
    'TEXT',
    'DATE',
    'TIME',
    'TIMESTAMP',
    'TIMESTAMPTZ',
    'BOOLEAN',
    'JSONB'
  ],
  ORACLE: [
    'NUMBER(19)',
    'NUMBER(10)',
    'NUMBER(5)',
    'NUMBER(3)',
    'NUMBER(10,2)',
    'BINARY_DOUBLE',
    'BINARY_FLOAT',
    'VARCHAR2(255)',
    'NVARCHAR2(255)',
    'CLOB',
    'DATE',
    'TIMESTAMP'
  ],
  DORIS: [
    'BIGINT',
    'INT',
    'SMALLINT',
    'TINYINT',
    'DECIMAL(10,2)',
    'DOUBLE',
    'FLOAT',
    'VARCHAR(255)',
    'STRING',
    'DATE',
    'DATETIME',
    'BOOLEAN',
    'JSON'
  ]
}
const GENERIC_TARGET_TYPE_OPTIONS = Array.from(
  new Set(Object.values(TARGET_TYPE_OPTIONS).flat())
)

const normalizeList = (payload: any): any[] => {
  if (!payload) return []
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload.totalList)) return payload.totalList
  if (Array.isArray(payload.records)) return payload.records
  if (Array.isArray(payload.data)) return payload.data
  return []
}

const normalizeTextList = (payload: any): string[] => {
  return normalizeList(payload)
    .map((item) => {
      if (typeof item === 'string') return item
      return item?.name || item?.label || item?.value || ''
    })
    .filter(Boolean)
}

const normalizeColumnList = (payload: any): ColumnItem[] => {
  return normalizeList(payload)
    .map((item, index) => {
      const name =
        item?.columnName || item?.name || item?.field || item?.label || ''
      if (!name) return null
      return {
        name,
        type:
          item?.type ||
          item?.dataType ||
          item?.columnType ||
          item?.jdbcType ||
          'unknown',
        key: `${name}-${index}`,
        nullable: item?.nullable,
        primaryKey: !!item?.primaryKey,
        comment: item?.comment || item?.remarks || ''
      }
    })
    .filter(Boolean) as ColumnItem[]
}

const normalizeDateText = (value: any): string => {
  if (!value) return '-'
  if (typeof value === 'string') return value
  if (typeof value === 'number') return formatDateTime(value)
  return '-'
}

const extractErrorMessage = (error: any, fallback: string): string => {
  return (
    error?.response?.data?.msg ||
    error?.response?.data?.message ||
    error?.msg ||
    error?.message ||
    fallback
  )
}

const isWorkflowNameExistsError = (error: any): boolean => {
  const message = extractErrorMessage(error, '')
  return message.includes('工作流定义名称') && message.includes('已存在')
}

const stripCreateTableResponsePrefix = (ddl: string) => {
  if (!ddl) {
    return {
      ddl: '',
      targetTableExists: false
    }
  }
  if (ddl.startsWith(TARGET_TABLE_EXISTS_PREFIX)) {
    return {
      ddl: ddl.replace(`${TARGET_TABLE_EXISTS_PREFIX}\n`, '').trim(),
      targetTableExists: true
    }
  }
  return {
    ddl,
    targetTableExists: false
  }
}

const parsePort = (value: any, fallback: number): number => {
  const port = Number(value)
  return Number.isFinite(port) ? port : fallback
}

const parseJdbcHostPort = (
  jdbcUrl: string | undefined,
  fallbackPort: number
): { host: string; port: number } => {
  if (!jdbcUrl) {
    return {
      host: '127.0.0.1',
      port: fallbackPort
    }
  }
  const matched = jdbcUrl.match(/^jdbc:[^:]+:\/\/([^/:?]+)(?::(\d+))?/)
  return {
    host: matched?.[1] || '127.0.0.1',
    port: parsePort(matched?.[2], fallbackPort)
  }
}

const parseConnectionParams = (
  item: DatasourceRecord
): Partial<DatasourceDetail> => {
  const fallbackPort = DEFAULT_PORTS[item.type]
  let params: Record<string, any> = {}
  if (typeof item.connectionParams === 'string') {
    try {
      params = JSON.parse(item.connectionParams)
    } catch (err) {
      params = {}
    }
  } else if (item.connectionParams) {
    params = item.connectionParams
  }

  const jdbcUrl = params.jdbcUrl || params.address
  const parsedAddress = parseJdbcHostPort(jdbcUrl, fallbackPort)
  return {
    host: item.host || parsedAddress.host,
    port: parsePort(item.port, parsedAddress.port || fallbackPort),
    userName: item.userName || item.dbUser || params.user || '',
    password:
      item.password && item.password !== PASSWORD_MASK
        ? item.password
        : params.password || '',
    database: item.database || params.database || ''
  }
}

const formatDateTime = (value: number | null): string => {
  if (!value) return ''
  return format(new Date(value), 'yyyy-MM-dd HH:mm:ss')
}

const extractCounterFromLog = (logText: string, label: string): number | null => {
  if (!logText) return null
  const counterRegExp = new RegExp(`${label}\\s*:\\s*([\\d,]+)`, 'g')
  const matchedValues = [...logText.matchAll(counterRegExp)]
  if (!matchedValues.length) return null
  const parsedValue = Number(
    matchedValues[matchedValues.length - 1][1]?.replaceAll(',', '')
  )
  return Number.isFinite(parsedValue) ? parsedValue : null
}

const extractReadWriteCountFromLog = (
  logText: string
): { readRows: number | null; writeRows: number | null } => ({
  readRows: extractCounterFromLog(logText, 'Total Read Count'),
  writeRows: extractCounterFromLog(logText, 'Total Write Count')
})

const formatReadWriteRows = (
  readRows: number | null,
  writeRows: number | null
): string => `${readRows ?? '-'} / ${writeRows ?? '-'}`

// 这里按“目标字段设计区”的行顺序来拼 source select，
// 这样当用户通过连线把源字段重新映射到别的目标字段时，生成的 Seatunnel SQL 仍然与目标表列顺序一致。
const buildOrderedSourceRows = (rows: MappingRow[]): MappingRow[] => {
  const selectedRows = rows.filter((item) => item.sync && item.sourceColumn)
  if (!selectedRows.length) return []
  const mappedByTargetKey = new Map(
    selectedRows
      .filter((item) => item.mappedTargetKey)
      .map((item) => [item.mappedTargetKey as string, item])
  )
  return selectedRows.map((targetRow) => mappedByTargetKey.get(targetRow.key) || targetRow)
}

const buildOrderedMappingRows = (rows: MappingRow[]): MappingRow[] =>
  buildOrderedSourceRows(rows).filter(
    (item) => item.sync && item.sourceColumn && item.targetColumn && item.mappedTargetKey
  )

const quoteQueryIdentifier = (type: SyncDatasourceType, name: string): string => {
  if (type === 'MYSQL' || type === 'DORIS') {
    return `\`${name.replaceAll('`', '``')}\``
  }
  return `"${name.replaceAll('"', '""')}"`
}

const quoteSeatunnelSqlIdentifier = (name: string): string => {
  const normalized = name.trim()
  return /^[A-Za-z_][A-Za-z0-9_]*$/.test(normalized)
    ? normalized
    : `"${normalized.replaceAll('"', '""')}"`
}

const buildSourceSelectByType = (
  rows: MappingRow[],
  sourceType: SyncDatasourceType
): string => {
  const orderedSourceRows = buildOrderedMappingRows(rows)
  if (!orderedSourceRows.length) return 'select *'
  const uniqueSourceColumns = orderedSourceRows.reduce<string[]>((columns, item) => {
    if (!columns.includes(item.sourceColumn)) {
      columns.push(item.sourceColumn)
    }
    return columns
  }, [])
  return `select ${uniqueSourceColumns
    .map((sourceColumn) => quoteQueryIdentifier(sourceType, sourceColumn))
    .join(', ')}`
}

const buildSourceSelectSql = (
  rows: MappingRow[],
  sourceFilters: SourceFilterRule[],
  sourceType: SyncDatasourceType,
  sourceTable: string,
  sourceColumns: ColumnItem[],
  sampleLimit: number | null = null
): string => {
  const selectClause = buildSourceSelectByType(rows, sourceType)
  const fromClause = buildSourceFromClause(sourceType, sourceTable)
  const whereClause = buildSourceWhereClause(sourceFilters, sourceType, sourceColumns)
  const limitClause = buildSourceLimitClause(sourceType, sampleLimit)
  return `${selectClause} from ${fromClause}${whereClause}${limitClause}`
}

const buildSourceLimitClause = (
  sourceType: SyncDatasourceType,
  sampleLimit: number | null
): string => {
  if (!sampleLimit || sampleLimit <= 0) return ''
  if (sourceType === 'ORACLE') {
    return ` FETCH FIRST ${sampleLimit} ROWS ONLY`
  }
  return ` LIMIT ${sampleLimit}`
}

const buildSourceFromClause = (
  sourceType: SyncDatasourceType,
  sourceTable: string
): string => {
  return sourceTable.includes('.')
    ? sourceTable
    : quoteQueryIdentifier(sourceType, sourceTable)
}

const getSourceFilterOperatorClause = (
  rule: SourceFilterRule,
  sourceType: SyncDatasourceType,
  fieldType: string
): string => {
  const field = quoteQueryIdentifier(sourceType, rule.field)
  const value = rule.value.trim()
  const valueEnd = rule.valueEnd.trim()
  switch (rule.operator) {
    case 'EQ':
      return `${field} = ${buildSourceFilterLiteral(value, fieldType)}`
    case 'NE':
      return `${field} <> ${buildSourceFilterLiteral(value, fieldType)}`
    case 'GT':
      return `${field} > ${buildSourceFilterLiteral(value, fieldType)}`
    case 'LT':
      return `${field} < ${buildSourceFilterLiteral(value, fieldType)}`
    case 'GTE':
      return `${field} >= ${buildSourceFilterLiteral(value, fieldType)}`
    case 'LTE':
      return `${field} <= ${buildSourceFilterLiteral(value, fieldType)}`
    case 'CONTAINS':
      return `${field} LIKE '%${value.replaceAll("'", "''")}%'`
    case 'PREFIX':
      return `${field} LIKE '${value.replaceAll("'", "''")}%'`
    case 'IN': {
      const values = value
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean)
      if (!values.length) return ''
      return `${field} IN (${values
        .map((item) => buildSourceFilterLiteral(item, fieldType))
        .join(', ')})`
    }
    case 'BETWEEN':
      return `${field} BETWEEN ${buildSourceFilterLiteral(value, fieldType)} AND ${buildSourceFilterLiteral(valueEnd, fieldType)}`
    case 'IS_NULL':
      return `${field} IS NULL`
    case 'NOT_NULL':
      return `${field} IS NOT NULL`
    default:
      return ''
  }
}

const buildSourceWhereClause = (
  filters: SourceFilterRule[],
  sourceType: SyncDatasourceType,
  sourceColumns: ColumnItem[]
): string => {
  const enabledFilters = filters.filter((item) => item.enabled && item.field.trim())
  if (!enabledFilters.length) {
    return ''
  }
  const columnTypeMap = new Map(
    sourceColumns.map((item) => [item.name.toLowerCase(), item.type || ''])
  )
  const clauses = enabledFilters
    .map((item) =>
      getSourceFilterOperatorClause(
        item,
        sourceType,
        columnTypeMap.get(item.field.toLowerCase()) || ''
      )
    )
    .filter(Boolean)
  return clauses.length ? ` WHERE ${clauses.join(' AND ')}` : ''
}

const getSourceFieldFamily = (fieldType: string): string => {
  return getNormalizedTypeFamily(fieldType)
}

const getSourceFilterOperatorOptions = (fieldType: string) => {
  const family = getSourceFieldFamily(fieldType)
  const stringLikeOptions = [
    { label: '等于', value: 'EQ' },
    { label: '不等于', value: 'NE' },
    { label: '包含', value: 'CONTAINS' },
    { label: '前缀匹配', value: 'PREFIX' },
    { label: 'IN', value: 'IN' },
    { label: '为空', value: 'IS_NULL' },
    { label: '不为空', value: 'NOT_NULL' }
  ]
  const numberLikeOptions = [
    { label: '等于', value: 'EQ' },
    { label: '不等于', value: 'NE' },
    { label: '大于', value: 'GT' },
    { label: '小于', value: 'LT' },
    { label: '大于等于', value: 'GTE' },
    { label: '小于等于', value: 'LTE' },
    { label: 'IN', value: 'IN' },
    { label: '为空', value: 'IS_NULL' },
    { label: '不为空', value: 'NOT_NULL' }
  ]
  const dateLikeOptions = [
    { label: '等于', value: 'EQ' },
    { label: '不等于', value: 'NE' },
    { label: '大于', value: 'GT' },
    { label: '小于', value: 'LT' },
    { label: '大于等于', value: 'GTE' },
    { label: '小于等于', value: 'LTE' },
    { label: '区间', value: 'BETWEEN' },
    { label: '为空', value: 'IS_NULL' },
    { label: '不为空', value: 'NOT_NULL' }
  ]
  if (family === 'int32' || family === 'int64' || family === 'decimal' || family === 'float') {
    return numberLikeOptions
  }
  if (family === 'date' || family === 'time' || family === 'timestamp') {
    return dateLikeOptions
  }
  if (family === 'boolean') {
    return [
      { label: '等于', value: 'EQ' },
      { label: '不等于', value: 'NE' },
      { label: '为空', value: 'IS_NULL' },
      { label: '不为空', value: 'NOT_NULL' }
    ]
  }
  return stringLikeOptions
}

const buildSourceFilterLiteral = (value: string, fieldType: string): string => {
  const family = getSourceFieldFamily(fieldType)
  const trimmed = value.trim()
  if (!trimmed) return "''"
  if (family === 'int32' || family === 'int64' || family === 'decimal' || family === 'float') {
    return Number.isFinite(Number(trimmed)) ? trimmed : `'${trimmed.replaceAll("'", "''")}'`
  }
  if (family === 'boolean') {
    const normalized = trimmed.toLowerCase()
    if (['true', '1', 'yes', 'y'].includes(normalized)) return 'TRUE'
    if (['false', '0', 'no', 'n'].includes(normalized)) return 'FALSE'
    return `'${trimmed.replaceAll("'", "''")}'`
  }
  return `'${trimmed.replaceAll("'", "''")}'`
}

const describeSourceFilters = (filters: SourceFilterRule[]): string => {
  const activeCount = filters.filter((item) => item.enabled && item.field.trim()).length
  return activeCount ? `源端过滤 ${activeCount} 条` : '源端过滤未启用'
}

const normalizeAgentCommand = (command: string): string =>
  command
    .replaceAll('，', ',')
    .replaceAll('。', '.')
    .replaceAll('：', ':')
    .replaceAll('；', ';')
    .replace(/\s+/g, ' ')
    .trim()

const detectAgentDatasourceTypes = (
  command: string
): { sourceType: SyncDatasourceType | null; targetType: SyncDatasourceType | null } => {
  const normalized = command.toLowerCase()
  const typeMatches = Array.from(
    normalized.matchAll(/\b(mysql|postgresql|postgres|pgsql|pg|oracle|doris)\b/g)
  ).map((item) => {
    const token = item[1]
    if (token === 'mysql') return 'MYSQL'
    if (token === 'oracle') return 'ORACLE'
    if (token === 'doris') return 'DORIS'
    return 'POSTGRESQL'
  }) as SyncDatasourceType[]
  return {
    sourceType: typeMatches[0] || null,
    targetType: typeMatches[1] || null
  }
}

const extractAgentTableRefs = (command: string): string[] => {
  return Array.from(
    command.matchAll(/([A-Za-z_][\w$-]*)\.([A-Za-z_][\w$-]*)/g)
  )
    .map((item) => `${item[1]}.${item[2]}`)
    .filter((item, index, array) => array.indexOf(item) === index)
}

const parseAgentLimit = (command: string): number | null => {
  const matched =
    command.match(/(?:只同步|同步前|前)\s*(\d+)\s*条/i) ||
    command.match(/\blimit\s+(\d+)\b/i)
  const limit = Number(matched?.[1] || 0)
  return Number.isFinite(limit) && limit > 0 ? Math.min(limit, 100000) : null
}

const parseSyncAgentIntent = (rawCommand: string): SyncAgentParsedIntent => {
  const command = normalizeAgentCommand(rawCommand)
  const { sourceType, targetType } = detectAgentDatasourceTypes(command)
  const tableRefs = extractAgentTableRefs(command)
  const sourceRef = tableRefs[0] || ''
  const targetRef = tableRefs[1] || ''
  const [sourceDatabase = '', sourceTable = ''] = sourceRef.split('.')
  const [targetFirst = '', targetSecond = ''] = targetRef.split('.')
  const isSchemaLikeTarget =
    !!targetType &&
    ['POSTGRESQL', 'ORACLE'].includes(targetType) &&
    ['public', 'dbo', 'app', 'ods', 'dwd', 'dws'].includes(targetFirst.toLowerCase())
  const targetSchema = isSchemaLikeTarget
    ? targetFirst
    : targetType === 'POSTGRESQL'
      ? 'public'
      : getDefaultSchemaName(targetType || undefined)
  const targetDatabase = isSchemaLikeTarget ? '' : targetFirst
  const targetTable = targetSecond || targetFirst
  const autoExecute = /立即执行|执行|跑起来|启动|运行/.test(command)
  const limit = parseAgentLimit(command)
  const missing: string[] = []
  if (!sourceType) missing.push('源端类型')
  if (!targetType) missing.push('目标端类型')
  if (!sourceDatabase) missing.push('源库')
  if (!sourceTable) missing.push('源表')
  if (!targetTable) missing.push('目标表')
  const warnings: string[] = []
  if (!targetDatabase && !isSchemaLikeTarget) {
    warnings.push('命令中没有明确目标库，将优先使用目标数据源默认库或第一个可用库。')
  }
  if (limit) {
    warnings.push(`识别到只同步 ${limit} 条，本期会作为抽样条件写入 Agent 方案，适合验证链路。`)
  }
  if (missing.length) {
    warnings.push(`命令信息不完整：缺少 ${missing.join('、')}。`)
  }
  const confidenceBase = 100 - missing.length * 16 - (!targetDatabase ? 8 : 0)
  return {
    command,
    sourceType,
    targetType,
    sourceDatabase,
    sourceTable,
    targetDatabase,
    targetSchema,
    targetTable,
    limit,
    autoExecute,
    confidence: Math.max(20, Math.min(96, confidenceBase)),
    warnings,
    missing
  }
}

const buildSeatunnelTransformQuery = (rows: MappingRow[]): string => {
  const orderedRows = buildOrderedMappingRows(rows)
  if (!orderedRows.length) return 'select * from sync_source'
  const selectFields = orderedRows
    .map((item) => {
      const sourceColumn = quoteSeatunnelSqlIdentifier(item.sourceColumn)
      const targetColumn = quoteSeatunnelSqlIdentifier(item.targetColumn)
      return item.sourceColumn === item.targetColumn
        ? sourceColumn
        : `${sourceColumn} as ${targetColumn}`
    })
    .join(', ')
  return `select ${selectFields} from sync_source`
}

const buildJdbcSinkInsertQuery = (
  rows: MappingRow[],
  tableName: string
): string => {
  const orderedRows = buildOrderedMappingRows(rows)
  if (!orderedRows.length) return ''
  const targetColumns = orderedRows
    .map((item) => quoteSeatunnelSqlIdentifier(item.targetColumn))
    .join(', ')
  const placeholders = orderedRows.map(() => '?').join(', ')
  return `insert into ${tableName} (${targetColumns}) values (${placeholders})`
}

const buildJdbcUrl = (detail: DatasourceDetail, databaseName: string): string => {
  if (detail.type === 'MYSQL' || detail.type === 'DORIS') {
    return `jdbc:mysql://${detail.host}:${detail.port}/${databaseName}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`
  }
  if (detail.type === 'ORACLE') {
    return `jdbc:oracle:thin:@//${detail.host}:${detail.port}/${databaseName}`
  }
  return `jdbc:postgresql://${detail.host}:${detail.port}/${databaseName}`
}

const buildDriver = (type: SyncDatasourceType): string => {
  if (type === 'MYSQL' || type === 'DORIS') {
    return 'com.mysql.cj.jdbc.Driver'
  }
  if (type === 'ORACLE') {
    return 'oracle.jdbc.OracleDriver'
  }
  return 'org.postgresql.Driver'
}

const buildSinkTable = (
  targetType: SyncDatasourceType,
  databaseName: string,
  schemaName: string,
  tableName: string
): string => {
  if (targetType === 'POSTGRESQL' || targetType === 'ORACLE') {
    return tableName.includes('.') ? tableName : `${schemaName}.${tableName}`
  }
  return tableName
}

const getDefaultSchemaName = (targetType?: SyncDatasourceType): string => {
  if (targetType === 'ORACLE') return 'APP'
  if (targetType === 'POSTGRESQL') return 'public'
  return ''
}

const getSchemaPlaceholder = (targetType?: SyncDatasourceType): string => {
  if (targetType === 'ORACLE') return 'APP'
  if (targetType === 'POSTGRESQL') return 'public'
  return '可选'
}

const buildPrimaryKeys = (rows: MappingRow[]): string[] => {
  const configuredPrimaryKeys = rows
    .filter((item) => item.sync && item.targetPrimaryKey)
    .map((item) => item.targetColumn)
    .filter(Boolean)
  if (configuredPrimaryKeys.length) {
    return configuredPrimaryKeys
  }
  const candidates = ['id', 'ajbh', 'rybh']
  const matched = rows
    .filter((item) => item.sync)
    .map((item) => item.targetColumn)
    .filter((name) => candidates.includes(name))
  return matched.length ? [matched[0]] : []
}

const buildWorkflowName = (
  sourceName: string,
  sourceTable: string,
  targetName: string,
  targetTable: string
): string => {
  return `sync_${sourceName}_${sourceTable}_to_${targetName}_${targetTable}`
    .replaceAll(/[^a-zA-Z0-9_\u4e00-\u9fa5]+/g, '_')
    .slice(0, 120)
}

const buildDraftWorkflowName = (): string => {
  return `sync_draft_${format(new Date(), 'yyyyMMddHHmmss')}`
}

const buildSuggestedTaskName = (
  sourceOption: DatasourceOption | undefined,
  sourceTable: string | null,
  targetOption: DatasourceOption | undefined,
  targetTable: string
): string => {
  if (!sourceOption || !sourceTable || !targetOption || !targetTable.trim()) {
    return ''
  }
  return buildWorkflowName(
    sourceOption.label,
    sourceTable,
    targetOption.label,
    targetTable.trim()
  )
}

const inferSyncAssetPath = (
  workflow: any
): {
  sourcePath: string
  targetPath: string
  sourceType: SyncDatasourceType
  targetType: SyncDatasourceType
} => {
  const descriptionMeta = parseSyncWorkflowDescription(workflow?.description || '')
  const name = String(workflow?.name || '')
  const lowerName = name.toLowerCase()
  const sourceType: SyncDatasourceType =
    lowerName.includes('oracle') ? 'ORACLE' : lowerName.includes('doris') ? 'DORIS' : 'MYSQL'
  const targetType: SyncDatasourceType =
    lowerName.includes('doris')
      ? 'DORIS'
      : lowerName.includes('pgsql') || lowerName.includes('postgres')
        ? 'POSTGRESQL'
        : lowerName.includes('oracle')
          ? 'ORACLE'
          : 'MYSQL'

  if (descriptionMeta.sourceTable !== '-' || descriptionMeta.targetTable !== '-') {
    return {
      sourcePath: descriptionMeta.sourceTable,
      targetPath: descriptionMeta.targetTable,
      sourceType,
      targetType
    }
  }

  const nameMatched = name.match(/sync_(.+?)_to_(.+)$/)
  return {
    sourcePath: nameMatched?.[1] || '-',
    targetPath: nameMatched?.[2] || '-',
    sourceType,
    targetType
  }
}

const findCreateTableColumnRange = (statement: string) => {
  const createTableIndex = statement.search(/\bCREATE\s+TABLE\b/i)
  if (createTableIndex < 0) return null

  let start = -1
  let depth = 0
  let quote: string | null = null
  for (let index = createTableIndex; index < statement.length; index += 1) {
    const char = statement[index]
    const previousChar = statement[index - 1]
    if (quote) {
      if (char === quote && previousChar !== '\\') {
        quote = null
      }
      continue
    }
    if (char === '\'' || char === '"' || char === '`') {
      quote = char
      continue
    }
    if (char === '(') {
      if (depth === 0) start = index
      depth += 1
    } else if (char === ')') {
      depth -= 1
      if (depth === 0 && start >= 0) {
        return {
          start,
          end: index
        }
      }
    }
  }
  return null
}

const splitTopLevelSqlItems = (content: string): string[] => {
  const items: string[] = []
  let depth = 0
  let quote: string | null = null
  let current = ''

  for (let index = 0; index < content.length; index += 1) {
    const char = content[index]
    const previousChar = content[index - 1]
    if (quote) {
      current += char
      if (char === quote && previousChar !== '\\') {
        quote = null
      }
      continue
    }
    if (char === '\'' || char === '"' || char === '`') {
      quote = char
      current += char
      continue
    }
    if (char === '(') {
      depth += 1
      current += char
      continue
    }
    if (char === ')') {
      depth -= 1
      current += char
      continue
    }
    if (char === ',' && depth === 0) {
      if (current.trim()) {
        items.push(current.trim())
      }
      current = ''
      continue
    }
    current += char
  }

  if (current.trim()) {
    items.push(current.trim())
  }
  return items
}

const normalizeSqlWhitespace = (value: string): string => {
  return value.replace(/\s+/g, ' ').trim()
}

const formatCreateTableStatement = (statement: string): string => {
  const range = findCreateTableColumnRange(statement)
  if (!range) {
    return normalizeSqlWhitespace(statement)
  }
  const beforeColumns = normalizeSqlWhitespace(statement.slice(0, range.start))
  const columnContent = statement.slice(range.start + 1, range.end)
  const afterColumns = normalizeSqlWhitespace(statement.slice(range.end + 1))
  const columnLines = splitTopLevelSqlItems(columnContent).map((item, index, list) => {
    const suffix = index === list.length - 1 ? '' : ','
    return `  ${normalizeSqlWhitespace(item)}${suffix}`
  })

  return [
    `${beforeColumns} (`,
    ...columnLines,
    `)${afterColumns ? ` ${afterColumns}` : ''}`
  ].join('\n')
}

const formatSqlStatement = (statement: string): string => {
  if (/\bCREATE\s+TABLE\b/i.test(statement)) {
    return formatCreateTableStatement(statement)
  }
  return normalizeSqlWhitespace(statement)
}

const formatSql = (sql: string): string => {
  if (!sql.trim()) return ''
  return sql
    .split(/;\s*(?:\r?\n|$)/)
    .map((statement) => statement.trim())
    .filter(Boolean)
    .map(formatSqlStatement)
    .join(';\n\n')
}

const extractWorkflowDefinitionMeta = (
  payload: any
): {
  code: number | null
  version: number
  releaseState: string
  name: string
} | null => {
  if (!payload) return null

  // query-by-name 接口返回的是一个 DAG 结构，真正的工作流定义在 workflowDefinition 字段里。
  // 这里统一兼容 query-by-name、query-by-code 以及可能的平铺对象，避免后续判断“是否已存在工作流”失真。
  const definition = payload.workflowDefinition || payload
  const code = Number(definition?.code)
  if (!Number.isFinite(code) || code <= 0) {
    return null
  }

  return {
    code,
    version: Number(definition?.version) || 1,
    releaseState: definition?.releaseState || '-',
    name: definition?.name || ''
  }
}

const findWorkflowDefinitionMetaByName = async (
  projectCode: number,
  workflowName: string
): Promise<ReturnType<typeof extractWorkflowDefinitionMeta>> => {
  const page = await queryWorkflowDefinitionListPaging(
    {
      pageNo: 1,
      pageSize: 100,
      searchVal: workflowName
    },
    projectCode
  )
  const matched = normalizeList(page).find(
    (item) => String(item?.name || '') === workflowName
  )
  return extractWorkflowDefinitionMeta(matched)
}

const extractWorkflowReleaseState = (payload: any): string => {
  return extractWorkflowDefinitionMeta(payload)?.releaseState || '-'
}

const parseSyncWorkflowDescription = (
  description: string
): { sourceTable: string; targetTable: string } => {
  const normalized = description || ''
  const generatedMatched = normalized.match(/同步任务页面自动生成:\s*(.+?)\s*->\s*(.+)$/)
  if (generatedMatched) {
    return {
      sourceTable: generatedMatched[1]?.trim() || '-',
      targetTable: generatedMatched[2]?.trim() || '-'
    }
  }
  const qaMatched = normalized.match(/同步任务完整 QA:\s*(.+?)_to_(.+)$/)
  if (qaMatched) {
    return {
      sourceTable: qaMatched[1]?.trim() || '-',
      targetTable: qaMatched[2]?.trim() || '-'
    }
  }
  return {
    sourceTable: '-',
    targetTable: '-'
  }
}

const unquoteSqlIdentifier = (value: string): string =>
  value.trim().replace(/^["`]|["`]$/g, '').trim()

const extractQuotedConfigValue = (rawScript: string, key: string): string => {
  const matched = rawScript.match(new RegExp(`${key}\\s*=\\s*"([\\s\\S]*?)"`))
  return matched?.[1]?.trim() || ''
}

const extractQuotedConfigValues = (rawScript: string, key: string): string[] =>
  [...rawScript.matchAll(new RegExp(`${key}\\s*=\\s*"([\\s\\S]*?)"`, 'g'))]
    .map((item) => item[1]?.trim() || '')
    .filter(Boolean)

const parseDatabaseFromJdbcUrl = (jdbcUrl: string): string => {
  if (!jdbcUrl) return ''
  const mysqlOrPgMatched = jdbcUrl.match(/^jdbc:[^:]+:\/\/[^/]+\/([^?;]+)/i)
  if (mysqlOrPgMatched?.[1]) {
    return mysqlOrPgMatched[1].replace(/^\/+|\/+$/g, '')
  }
  const oracleMatched = jdbcUrl.match(/^jdbc:oracle:[^@]+@\/\/[^/]+\/([^?;]+)/i)
  return oracleMatched?.[1]?.trim() || ''
}

const splitSqlColumns = (value: string): string[] =>
  value
    .split(',')
    .map((item) => unquoteSqlIdentifier(item.replace(/\s+as\s+.+$/i, '')))
    .filter(Boolean)

const parseSourceQuery = (
  sourceQuery: string
): { sourceTable: string; columns: string[]; whereClause: string } => {
  const matched = sourceQuery.match(/select\s+([\s\S]+?)\s+from\s+([^\s]+)(?:\s+where\s+([\s\S]+))?$/i)
  if (!matched) {
    return {
      sourceTable: '',
      columns: [],
      whereClause: ''
    }
  }
  return {
    columns: splitSqlColumns(matched[1] || ''),
    sourceTable: unquoteSqlIdentifier(matched[2] || ''),
    whereClause: matched[3]?.trim() || ''
  }
}

const normalizeWhereIdentifier = (value: string): string => {
  const trimmed = value.trim()
  const lastPart = trimmed.includes('.') ? trimmed.split('.').at(-1) || trimmed : trimmed
  return unquoteSqlIdentifier(lastPart)
}

const unescapeSqlLiteral = (value: string): string => {
  const trimmed = value.trim()
  if (
    (trimmed.startsWith("'") && trimmed.endsWith("'")) ||
    (trimmed.startsWith('"') && trimmed.endsWith('"'))
  ) {
    return trimmed.slice(1, -1).replaceAll("''", "'").replaceAll('\\"', '"')
  }
  return trimmed
}

const splitWhereClauses = (whereClause: string): string[] => {
  const clauses: string[] = []
  let current = ''
  let quote: string | null = null
  let index = 0
  while (index < whereClause.length) {
    const char = whereClause[index]
    if (quote) {
      current += char
      if (char === quote && whereClause[index - 1] !== '\\') {
        quote = null
      }
      index += 1
      continue
    }
    if (char === "'" || char === '"') {
      quote = char
      current += char
      index += 1
      continue
    }
    const nextAnd = whereClause.slice(index, index + 5)
    if (/^\sAND\s$/i.test(nextAnd)) {
      const hasOpenBetween =
        /\bBETWEEN\b/i.test(current) && !/\bBETWEEN\b[\s\S]+\bAND\b/i.test(current)
      if (hasOpenBetween) {
        current += nextAnd
      } else {
        if (current.trim()) clauses.push(current.trim())
        current = ''
      }
      index += 5
      continue
    }
    current += char
    index += 1
  }
  if (current.trim()) clauses.push(current.trim())
  return clauses
}

const parseSingleWhereClause = (
  clause: string,
  index: number
): SourceFilterRule | null => {
  const seedKey = `raw-filter-${index}-${Date.now()}`
  const isNotNullMatched = clause.match(/^(.+?)\s+IS\s+NOT\s+NULL$/i)
  if (isNotNullMatched) {
    return {
      key: seedKey,
      enabled: true,
      field: normalizeWhereIdentifier(isNotNullMatched[1] || ''),
      operator: 'NOT_NULL',
      value: '',
      valueEnd: ''
    }
  }
  const isNullMatched = clause.match(/^(.+?)\s+IS\s+NULL$/i)
  if (isNullMatched) {
    return {
      key: seedKey,
      enabled: true,
      field: normalizeWhereIdentifier(isNullMatched[1] || ''),
      operator: 'IS_NULL',
      value: '',
      valueEnd: ''
    }
  }
  const betweenMatched = clause.match(/^(.+?)\s+BETWEEN\s+([\s\S]+?)\s+AND\s+([\s\S]+)$/i)
  if (betweenMatched) {
    return {
      key: seedKey,
      enabled: true,
      field: normalizeWhereIdentifier(betweenMatched[1] || ''),
      operator: 'BETWEEN',
      value: unescapeSqlLiteral(betweenMatched[2] || ''),
      valueEnd: unescapeSqlLiteral(betweenMatched[3] || '')
    }
  }
  const inMatched = clause.match(/^(.+?)\s+IN\s*\(([\s\S]+)\)$/i)
  if (inMatched) {
    return {
      key: seedKey,
      enabled: true,
      field: normalizeWhereIdentifier(inMatched[1] || ''),
      operator: 'IN',
      value: splitSqlColumns(inMatched[2] || '').map(unescapeSqlLiteral).join(', '),
      valueEnd: ''
    }
  }
  const likeMatched = clause.match(/^(.+?)\s+LIKE\s+([\s\S]+)$/i)
  if (likeMatched) {
    const literal = unescapeSqlLiteral(likeMatched[2] || '')
    const isContains = literal.startsWith('%') && literal.endsWith('%')
    return {
      key: seedKey,
      enabled: true,
      field: normalizeWhereIdentifier(likeMatched[1] || ''),
      operator: isContains ? 'CONTAINS' : 'PREFIX',
      value: literal.replace(/^%/, '').replace(/%$/, ''),
      valueEnd: ''
    }
  }
  const compareMatched = clause.match(/^(.+?)\s*(>=|<=|<>|!=|=|>|<)\s*([\s\S]+)$/)
  if (compareMatched) {
    const operatorMap: Record<string, SourceFilterOperator> = {
      '=': 'EQ',
      '!=': 'NE',
      '<>': 'NE',
      '>': 'GT',
      '<': 'LT',
      '>=': 'GTE',
      '<=': 'LTE'
    }
    return {
      key: seedKey,
      enabled: true,
      field: normalizeWhereIdentifier(compareMatched[1] || ''),
      operator: operatorMap[compareMatched[2] || '='] || 'EQ',
      value: unescapeSqlLiteral(compareMatched[3] || ''),
      valueEnd: ''
    }
  }
  return null
}

const parseSourceFiltersFromWhereClause = (whereClause: string): SourceFilterRule[] => {
  if (!whereClause.trim()) return []
  return splitWhereClauses(whereClause)
    .map((clause, index) => parseSingleWhereClause(clause, index + 1))
    .filter(Boolean)
    .slice(0, SOURCE_FILTER_MAX_COUNT) as SourceFilterRule[]
}

const unescapeSeatunnelString = (value: string): string =>
  value.replaceAll('\\"', '"').replaceAll('\\\\', '\\')

const parseTransformSourceColumns = (transformQuery: string): string[] => {
  const matched = transformQuery.match(/select\s+([\s\S]+?)\s+from\s+sync_source/i)
  if (!matched) return []
  return (matched[1] || '')
    .split(',')
    .map((item) => {
      const aliasMatched = item.match(/^(.+?)\s+as\s+.+$/i)
      return unquoteSqlIdentifier(aliasMatched?.[1] || item)
    })
    .filter(Boolean)
}

const parseSinkInsertColumns = (sinkQuery: string): string[] => {
  const matched = sinkQuery.match(/insert\s+into\s+.+?\(([\s\S]+?)\)\s+values/i)
  if (!matched) return []
  return splitSqlColumns(matched[1] || '')
}

const extractTaskRawScript = (workflowDetail: any): string => {
  const taskList = normalizeList(workflowDetail?.taskDefinitionList)
  const seatunnelTask =
    taskList.find((task) => task?.taskType === 'SEATUNNEL') || taskList[0]
  return seatunnelTask?.taskParams?.rawScript || ''
}

const buildAssetDesignFromRawScript = (
  rawScript: string,
  pathMeta: {
    sourcePath: string
    targetPath: string
    sourceType: SyncDatasourceType
    targetType: SyncDatasourceType
  }
): {
  sourcePath: string
  targetPath: string
  sourceColumns: ColumnItem[]
  targetColumns: ColumnItem[]
  fieldRows: FieldDesignRow[]
  sourceFilters: SourceFilterRule[]
  sinkCustomSql: string
  configText: string
} => {
  if (!rawScript.trim()) {
    return {
      sourcePath: pathMeta.sourcePath,
      targetPath: pathMeta.targetPath,
      sourceColumns: [],
      targetColumns: [],
      fieldRows: [],
      sourceFilters: [],
      sinkCustomSql: '',
      configText: ''
    }
  }

  const sourceQuery = extractQuotedConfigValue(rawScript, 'query')
  const allQueries = extractQuotedConfigValues(rawScript, 'query')
  const jdbcDatabases = extractQuotedConfigValues(rawScript, 'url')
    .map(parseDatabaseFromJdbcUrl)
    .filter(Boolean)
  const transformQuery = allQueries.find((item) => item.includes('sync_source')) || ''
  const sinkQuery = allQueries.find((item) => /insert\s+into/i.test(item)) || ''
  const parsedSource = parseSourceQuery(sourceQuery)
  const sourceColumnsFromTransform = parseTransformSourceColumns(transformQuery)
  const sourceColumns =
    sourceColumnsFromTransform.length
      ? sourceColumnsFromTransform
      : parsedSource.columns
  const targetColumns = parseSinkInsertColumns(sinkQuery)
  const sourceFilters = parseSourceFiltersFromWhereClause(parsedSource.whereClause)
  const sinkCustomSql = unescapeSeatunnelString(extractQuotedConfigValue(rawScript, 'custom_sql'))
  const targetDatabase = extractQuotedConfigValue(rawScript, 'database') || jdbcDatabases[1] || ''
  const targetTable = extractQuotedConfigValue(rawScript, 'table') || pathMeta.targetPath
  const targetPath =
    targetDatabase && targetTable && targetTable.split('.').length < 3
      ? `${targetDatabase}.${targetTable}`
      : targetTable
  const sourceDatabase = jdbcDatabases[0] || ''
  const sourceTable = parsedSource.sourceTable || pathMeta.sourcePath
  const sourcePath =
    sourceDatabase && sourceTable && !sourceTable.includes('.')
      ? `${sourceDatabase}.${sourceTable}`
      : sourceTable
  const maxLength = Math.max(sourceColumns.length, targetColumns.length)
  const fieldRows: FieldDesignRow[] = Array.from({ length: maxLength }).map((_, index) => {
    const sourceColumn = sourceColumns[index] || targetColumns[index] || `source_${index + 1}`
    const targetColumn = targetColumns[index] || sourceColumn
    const key = targetColumn || sourceColumn
    return {
      key,
      sourceColumn,
      sourceType: 'unknown',
      sourceComment: '',
      sourcePrimaryKey: false,
      sourceNullable: true,
      targetColumn,
      targetType: 'unknown',
      targetComment: '',
      targetPrimaryKey: false,
      targetNullable: true,
      sync: true,
      mappedTargetKey: key,
      mappingKind: sourceColumn === targetColumn ? 'AUTO' : 'MANUAL',
      targetColumnTouched: sourceColumn !== targetColumn
    }
  })

  return {
    sourcePath,
    targetPath,
    sourceColumns: fieldRows.map((item, index) => ({
      name: item.sourceColumn,
      type: item.sourceType,
      key: `${item.sourceColumn}-${index}`,
      nullable: item.sourceNullable,
      primaryKey: item.sourcePrimaryKey,
      comment: item.sourceComment
    })),
    targetColumns: fieldRows.map((item, index) => ({
      name: item.targetColumn,
      type: item.targetType,
      key: item.key || `${item.targetColumn}-${index}`,
      nullable: item.targetNullable,
      primaryKey: item.targetPrimaryKey,
      comment: item.targetComment
    })),
    fieldRows,
    sourceFilters,
    sinkCustomSql,
    configText: rawScript
  }
}

const isSyncWorkflowDefinition = (workflow: any): boolean => {
  const name = String(workflow?.name || '')
  const description = String(workflow?.description || '')
  return (
    name.startsWith('sync_') ||
    description.includes('同步任务页面自动生成') ||
    description.includes('同步任务完整 QA')
  )
}

const inferTargetColumnType = (
  sourceType: string,
  targetDatasourceType?: SyncDatasourceType
): string => {
  const normalized = sourceType.toLowerCase()
  const targetType = targetDatasourceType || 'MYSQL'

  if (normalized.includes('json')) {
    if (targetType === 'POSTGRESQL') return 'JSONB'
    if (targetType === 'ORACLE') return 'CLOB'
    if (targetType === 'DORIS') return 'JSON'
    return 'JSON'
  }
  if (
    normalized.includes('bool') ||
    normalized.includes('bit') ||
    normalized.includes('tinyint(1)')
  ) {
    return 'BOOLEAN'
  }
  if (normalized.includes('bigint')) {
    if (targetType === 'ORACLE') return 'NUMBER(19)'
    return 'BIGINT'
  }
  if (normalized.includes('smallint')) {
    if (targetType === 'ORACLE') return 'NUMBER(5)'
    return 'SMALLINT'
  }
  if (normalized.includes('tinyint')) {
    if (targetType === 'ORACLE') return 'NUMBER(3)'
    return targetType === 'POSTGRESQL' ? 'SMALLINT' : 'TINYINT'
  }
  if (
    normalized.includes('int') ||
    normalized.includes('serial') ||
    normalized.includes('number')
  ) {
    if (targetType === 'ORACLE') return 'NUMBER(10)'
    return targetType === 'POSTGRESQL' ? 'INTEGER' : 'INT'
  }
  if (normalized.includes('decimal') || normalized.includes('numeric')) {
    if (targetType === 'ORACLE') return 'NUMBER(10,2)'
    return targetType === 'POSTGRESQL' ? 'NUMERIC(10,2)' : 'DECIMAL(10,2)'
  }
  if (normalized.includes('double')) {
    if (targetType === 'ORACLE') return 'BINARY_DOUBLE'
    return targetType === 'POSTGRESQL' ? 'DOUBLE PRECISION' : 'DOUBLE'
  }
  if (normalized.includes('float') || normalized.includes('real')) {
    if (targetType === 'ORACLE') return 'BINARY_FLOAT'
    return targetType === 'POSTGRESQL' ? 'REAL' : 'FLOAT'
  }
  if (normalized.includes('longtext')) {
    if (targetType === 'ORACLE') return 'CLOB'
    if (targetType === 'DORIS') return 'STRING'
    return targetType === 'POSTGRESQL' ? 'TEXT' : 'LONGTEXT'
  }
  if (
    normalized.includes('char') ||
    normalized.includes('varchar') ||
    normalized.includes('string')
  ) {
    if (targetType === 'ORACLE') return 'VARCHAR2(255)'
    return 'VARCHAR(255)'
  }
  if (normalized.includes('text') || normalized.includes('clob')) {
    if (targetType === 'ORACLE') return 'CLOB'
    if (targetType === 'DORIS') return 'STRING'
    return 'TEXT'
  }
  if (normalized.includes('timestamp') || normalized.includes('datetime')) {
    if (targetType === 'DORIS') return 'DATETIME'
    return 'TIMESTAMP'
  }
  if (normalized.includes('date')) {
    return 'DATE'
  }
  if (normalized.includes('time')) {
    return 'TIME'
  }
  if (targetType === 'ORACLE') return 'VARCHAR2(255)'
  if (targetType === 'DORIS') return 'STRING'
  return 'TEXT'
}

const extractTypeArgs = (type: string): number[] => {
  const matched = type.match(/\(([^)]+)\)/)
  if (!matched) return []
  return matched[1]
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item))
}

const getNormalizedTypeFamily = (type: string): string => {
  const normalized = type.trim().toLowerCase()
  if (!normalized) return 'unknown'
  if (normalized.includes('bigint') || normalized.includes('number(19)')) return 'int64'
  if (
    normalized.includes('int') ||
    normalized.includes('serial') ||
    normalized.includes('number(10)') ||
    normalized === 'number'
  ) {
    return 'int32'
  }
  if (
    normalized.includes('decimal') ||
    normalized.includes('numeric') ||
    normalized.includes('number(')
  ) {
    return 'decimal'
  }
  if (
    normalized.includes('double') ||
    normalized.includes('float') ||
    normalized.includes('real') ||
    normalized.includes('binary_double') ||
    normalized.includes('binary_float')
  ) {
    return 'float'
  }
  if (
    normalized.includes('varchar') ||
    normalized.includes('char') ||
    normalized.includes('string') ||
    normalized.includes('text') ||
    normalized.includes('clob')
  ) {
    return 'string'
  }
  if (normalized.includes('timestamp') || normalized.includes('datetime')) return 'timestamp'
  if (normalized.includes('date')) return 'date'
  if (normalized.includes('time')) return 'time'
  if (normalized.includes('bool') || normalized.includes('bit')) return 'boolean'
  if (normalized.includes('json')) return 'json'
  return normalized
}

const getNumericRank = (family: string): number => {
  const ranks: Record<string, number> = {
    boolean: 0,
    int32: 1,
    int64: 2,
    decimal: 3,
    float: 4
  }
  return ranks[family] ?? -1
}

const getStringLength = (type: string): number => {
  const normalized = type.toLowerCase()
  if (normalized.includes('text') || normalized.includes('clob') || normalized.includes('string')) {
    return Number.MAX_SAFE_INTEGER
  }
  return extractTypeArgs(type)[0] || Number.MAX_SAFE_INTEGER
}

const canMapFieldType = (
  sourceType: string,
  targetType: string
): { ok: boolean; reason?: string } => {
  const sourceFamily = getNormalizedTypeFamily(sourceType)
  const targetFamily = getNormalizedTypeFamily(targetType)
  if (sourceFamily === 'unknown' || targetFamily === 'unknown') {
    return {
      ok: false,
      reason: '字段类型未知'
    }
  }
  if (sourceFamily === targetFamily) {
    if (sourceFamily === 'string') {
      const sourceLength = getStringLength(sourceType)
      const targetLength = getStringLength(targetType)
      if (targetLength < sourceLength) {
        return {
          ok: false,
          reason: '目标字符长度小于源字段长度'
        }
      }
    }
    if (sourceFamily === 'decimal') {
      const [sourcePrecision = 10, sourceScale = 0] = extractTypeArgs(sourceType)
      const [targetPrecision = 10, targetScale = 0] = extractTypeArgs(targetType)
      if (targetPrecision < sourcePrecision || targetScale < sourceScale) {
        return {
          ok: false,
          reason: '目标数值精度小于源字段精度'
        }
      }
    }
    return { ok: true }
  }
  const sourceRank = getNumericRank(sourceFamily)
  const targetRank = getNumericRank(targetFamily)
  if (sourceRank >= 0 && targetRank >= 0) {
    return targetRank >= sourceRank
      ? { ok: true }
      : {
          ok: false,
          reason: '目标数值类型范围小于源字段类型'
        }
  }
  if (sourceFamily === 'date' && targetFamily === 'timestamp') {
    return { ok: true }
  }
  if (sourceFamily === 'timestamp' && targetFamily === 'date') {
    return {
      ok: false,
      reason: '目标日期类型会丢失时间信息'
    }
  }
  if (targetFamily === 'string') {
    return { ok: true }
  }
  return {
    ok: false,
    reason: '字段类型不属于安全扩容映射'
  }
}

const DRAFT_SEATUNNEL_CONFIG = [
  'env {',
  '  execution.parallelism = 1',
  '  job.mode = "BATCH"',
  '}',
  '',
  'source {',
  '  FakeSource {',
  '    result_table_name = "draft_source"',
  '    row.num = 1',
  '    schema = {',
  '      fields {',
  '        draft_id = "int"',
  '      }',
  '    }',
  '  }',
  '}',
  '',
  'sink {',
  '  Console {}',
  '}'
].join('\n')

const syncTask = defineComponent({
  name: 'sync-task',
  setup() {
    const router: Router = useRouter()
    const state = reactive({
      datasourceOptions: [] as DatasourceOption[],
      datasourceDetails: {} as Record<number, DatasourceDetail>,
      loadingDatasources: false,
      projectOptions: [] as ProjectOption[],
      selectedProjectCode: null as number | null,
      viewMode: 'LIST' as SyncTaskViewMode,
      assetKeyword: '',
      assetProjectFilter: '',
      assetStatusFilter: '',
      assetScheduleFilter: '',
      assetTypeFilter: '',
      assetDetailVisible: false,
      assetDetailTab: 'OVERVIEW' as SyncTaskDetailTab,
      assetLogFullscreenVisible: false,
      agentDrawerVisible: false,
      agentCommand: SYNC_AGENT_EXAMPLES[0],
      agentRunning: false,
      agentAutoExecute: false,
      agentSampleLimit: null as number | null,
      agentPlan: null as SyncAgentPlan | null,
      agentError: '',
      agentStages: [
        { key: 'PARSE', label: '解析自然语言', status: 'WAITING', message: '等待输入命令' },
        { key: 'MATCH', label: '匹配项目和数据源', status: 'WAITING', message: '等待解析结果' },
        { key: 'METADATA', label: '加载库表元数据', status: 'WAITING', message: '等待数据源匹配' },
        { key: 'MAPPING', label: '生成字段映射', status: 'WAITING', message: '等待字段加载' },
        { key: 'PLAN', label: '生成同步方案', status: 'WAITING', message: '等待映射结果' }
      ] as SyncAgentStage[],
      selectedAsset: null as SyncTaskAsset | null,
      editingAssetId: '' as string,
      latestPublishedAssetId: '' as string,
      loadingAssets: false,
      hydratingAsset: false,
      syncTaskAssets: [] as SyncTaskAsset[],
      currentStep: 1,
      loadingProjects: false,
      creatingWorkflow: false,
      savingWorkflow: false,
      creatingTable: false,
      runningWorkflow: false,
      source: {
        datasourceId: null,
        database: null,
        table: null,
        databases: [],
        tables: [],
        columns: [],
        loading: false
      } as EndpointState,
      target: {
        datasourceId: null,
        database: null,
        table: null,
        databases: [],
        tables: [],
        columns: [],
        loading: false
      } as EndpointState,
      targetTableName: '',
      targetSchemaName: 'public',
      taskName: '',
      sourceFilters: [createSourceFilterRule()] as SourceFilterRule[],
      activeSolutionModule: 'MAPPING' as SyncSolutionModule,
      sinkCustomSql: '',
      dataProcessingEnabled: false,
      previewVisible: false,
      configEditorText: '',
      configManualOverride: false,
      fieldRows: [] as FieldDesignRow[],
      creatingMapping: false,
      mappingLinesVisible: true,
      mappingExceptionOnly: false,
      targetNameRule: 'KEEP_SOURCE' as TargetNameRule,
      latestWorkflowCode: null as number | null,
      latestWorkflowName: '',
      latestWorkflowVersion: 1,
      latestWorkflowReleaseState: '-' as string,
      latestRunStage: 'IDLE' as keyof typeof RUN_PROGRESS_LABELS,
      latestRunMessage: '尚未发起同步运行。',
      latestInstanceId: null as number | null,
      latestInstanceName: '',
      latestInstanceState: '' as string,
      latestInstanceStateLabel: '等待执行',
      latestInstanceStateType: 'default' as 'default' | 'info' | 'success' | 'warning' | 'error',
      latestInstanceStartTime: '',
      latestInstanceEndTime: '',
      latestInstanceTaskRows: [] as WorkflowTaskProgressRow[],
      latestInstanceTaskTotal: 0,
      latestInstanceTaskSuccess: 0,
      latestInstanceTaskRunning: 0,
      latestInstanceTaskFailed: 0,
      latestReadRowCount: null as number | null,
      latestSyncedRowCount: null as number | null,
      latestSyncedRowCountLoading: false,
      latestSyncedRowCountInstanceId: null as number | null,
      latestScheduleId: null as number | null,
      latestCreateTableDdl: '',
      latestCreateTableDdlManual: false,
      previewingTableDdl: false,
      executionParallelism: 1,
      executionMode: 'IMMEDIATE' as ExecutionMode,
      scheduleModalVisible: false,
      scheduleModalType: 'create' as 'create' | 'update',
      scheduleModalState: 'OFFLINE',
      scheduleModalRow: {} as Record<string, any>,
      latestScheduleSummary: '未配置' as string
    })
    const mappingWorkbenchRef = ref<HTMLElement | null>(null)
    const mappingAnchorPositions = ref<Record<string, { x: number; y: number }>>({})
    const draggingMapping = ref<{
      side: 'source' | 'target'
      key: string
    } | null>(null)
    const mappingDraftPoint = ref<{ x: number; y: number } | null>(null)
    let latestInstancePollingTimer: number | null = null

    const sourceDatasourceOption = computed(() =>
      state.datasourceOptions.find(
        (item) => item.value === state.source.datasourceId
      )
    )
    const targetDatasourceOption = computed(() =>
      state.datasourceOptions.find(
        (item) => item.value === state.target.datasourceId
      )
    )

    const sourceDatabaseOptions = computed(() =>
      state.source.databases.map((item) => ({ label: item, value: item }))
    )
    const sourceTableOptions = computed(() =>
      state.source.tables.map((item) => ({ label: item, value: item }))
    )
    const targetDatabaseOptions = computed(() =>
      state.target.databases.map((item) => ({ label: item, value: item }))
    )
    const targetTableOptions = computed(() =>
      state.target.tables.map((item) => ({ label: item, value: item }))
    )
    const targetSchemaPlaceholder = computed(() =>
      getSchemaPlaceholder(targetDatasourceOption.value?.type)
    )
    const syncWarnings = computed(() => {
      const warnings: string[] = []
      if (!state.source.datasourceId || !state.target.datasourceId) {
        warnings.push('请选择源数据源和目标数据源。')
      }
      if (sourceDatasourceOption.value?.type === targetDatasourceOption.value?.type) {
        warnings.push('当前建议优先使用异构同步场景，例如 MySQL -> PostgreSQL。')
      }
      if (!state.fieldRows.some((item) => item.sync)) {
        warnings.push('当前没有选中任何字段，生成的配置将无法用于真实同步。')
      }
      if (!state.targetTableName.trim()) {
        warnings.push('目标表名称未确认，建议使用已有表或明确新表名。')
      }
      if (state.executionMode === 'SCHEDULE' && !state.latestScheduleId) {
        warnings.push('当前还没有配置周期调度，请先点击“配置周期调度”。')
      }
      return warnings
    })

    const mappedCount = computed(
      () =>
        state.fieldRows.filter(
          (item) => item.sync && item.sourceColumn && item.mappedTargetKey
        ).length
    )

    const sourceFieldRows = computed<FieldDesignRow[]>(() => {
      if (targetTableMode.value !== 'EXISTING_TABLE') {
        return state.fieldRows
      }

      return state.source.columns.map((sourceColumn) => {
        const mappedTargetRow = state.fieldRows.find(
          (item) => item.sync && item.sourceColumn === sourceColumn.name
        )
        return {
          key: sourceColumn.name,
          sourceColumn: sourceColumn.name,
          sourceType: sourceColumn.type,
          sourceComment: sourceColumn.comment || '',
          sourcePrimaryKey: !!sourceColumn.primaryKey,
          sourceNullable: !!sourceColumn.nullable,
          targetColumn: mappedTargetRow?.targetColumn || '',
          targetType: mappedTargetRow?.targetType || '',
          targetComment: mappedTargetRow?.targetComment || '',
          targetPrimaryKey: mappedTargetRow?.targetPrimaryKey || false,
          targetNullable: mappedTargetRow?.targetNullable || false,
          sync: !!mappedTargetRow,
          mappedTargetKey: mappedTargetRow?.mappedTargetKey || null,
          mappingKind: mappedTargetRow?.mappingKind || 'AUTO',
          targetColumnTouched: mappedTargetRow?.targetColumnTouched || false
        }
      })
    })

    const selectedProjectOption = computed(() =>
      state.projectOptions.find((item) => item.value === state.selectedProjectCode)
    )

    const sourceFieldStats = computed(() => {
      const columns = state.source.columns
      return {
        total: columns.length,
        primaryKeys: columns.filter((item) => item.primaryKey).length,
        nullable: columns.filter((item) => item.nullable).length
      }
    })

    const getSourceColumnMeta = (sourceColumnName: string) => {
      const normalizedName = sourceColumnName.trim().toLowerCase()
      if (!normalizedName) {
        return null
      }
      return (
        state.source.columns.find(
          (item) => item.name.toLowerCase() === normalizedName
        ) || null
      )
    }

    const targetTableMode = computed<TargetTableMode>(() =>
      state.target.table ? 'EXISTING_TABLE' : 'CREATE_TABLE'
    )

    const targetTableModeLabel = computed(() =>
      targetTableMode.value === 'EXISTING_TABLE' ? '写入已有表' : '新建目标表'
    )

    const targetTableExists = computed(() => {
      const tableName = state.targetTableName.trim()
      if (!tableName) return false
      return state.target.tables.some(
        (item) => item.toLowerCase() === tableName.toLowerCase()
      )
    })

    const targetTableCheckText = computed(() => {
      const tableName = state.targetTableName.trim()
      if (!tableName) {
        return '请输入目标表名，系统会自动判断是新建目标表还是使用已有表。'
      }
      return targetTableExists.value
        ? `目标表 ${tableName} 已存在，下一步读取目标字段并按已有表映射。`
        : `目标表 ${tableName} 不存在，下一步按“新建目标表”处理。`
    })

    const assetMetrics = computed(() => [
      {
        key: '',
        label: '全部',
        value: state.syncTaskAssets.length
      },
      {
        key: 'RUNNING',
        label: '运行中',
        value: state.syncTaskAssets.filter((item) => item.status === 'RUNNING').length
      },
      {
        key: 'FAILED',
        label: '失败',
        value: state.syncTaskAssets.filter((item) => item.status === 'FAILED').length
      },
      {
        key: 'SCHEDULED',
        label: '已调度',
        value: state.syncTaskAssets.filter((item) => item.scheduleStatus === 'ON').length
      },
      {
        key: 'DRAFT',
        label: '草稿',
        value: state.syncTaskAssets.filter((item) => item.status === 'DRAFT').length
      }
    ])

    const filteredAssets = computed(() => {
      const keyword = state.assetKeyword.trim().toLowerCase()
      return state.syncTaskAssets.filter((item) => {
        const matchedKeyword =
          !keyword ||
          [
            item.name,
            item.projectName,
            item.sourcePath,
            item.targetPath,
            String(item.workflowCode || '')
          ]
            .join(' ')
            .toLowerCase()
            .includes(keyword)
        const matchedProject =
          !state.assetProjectFilter || item.projectName === state.assetProjectFilter
        const matchedStatus =
          !state.assetStatusFilter || item.status === state.assetStatusFilter
        const matchedSchedule =
          !state.assetScheduleFilter || item.scheduleStatus === state.assetScheduleFilter
        const matchedType =
          !state.assetTypeFilter ||
          item.sourceType === state.assetTypeFilter ||
          item.targetType === state.assetTypeFilter
        return (
          matchedKeyword &&
          matchedProject &&
          matchedStatus &&
          matchedSchedule &&
          matchedType
        )
      })
    })

    const buildAssetFromWorkflow = async (
      workflow: any,
      project: ProjectOption
    ): Promise<SyncTaskAsset> => {
      const workflowCode = Number(workflow?.code) || null
      const pathMeta = inferSyncAssetPath(workflow)
      let workflowDetail: any = null
      let designFromRawScript = buildAssetDesignFromRawScript('', pathMeta)
      if (workflowCode) {
        try {
          workflowDetail = await queryWorkflowDefinitionByCode(workflowCode, project.value)
          designFromRawScript = buildAssetDesignFromRawScript(
            extractTaskRawScript(workflowDetail),
            pathMeta
          )
        } catch (err) {
          workflowDetail = null
        }
      }
      let latestInstance: any = null
      let latestReadRows: number | null = null
      let latestWriteRows: number | null = null
      if (workflowCode) {
        try {
          const instanceResult = await queryWorkflowInstanceListPaging(
            {
              pageNo: 1,
              pageSize: 1,
              workflowDefinitionCode: workflowCode,
              searchVal: ''
            },
            project.value
          )
          latestInstance = normalizeList(instanceResult)[0] || null
        } catch (err) {
          latestInstance = null
        }
      }
      if (latestInstance?.id && latestInstance?.state === 'SUCCESS') {
        try {
          const taskResult = await queryTaskListByWorkflowId(
            Number(latestInstance.id),
            project.value
          )
          let totalReadRows = 0
          let totalWriteRows = 0
          let hasReadRows = false
          let hasWriteRows = false
          for (const task of normalizeList(taskResult?.taskList || taskResult)) {
            const taskInstanceId = Number(task?.id)
            if (task?.state !== 'SUCCESS' || !Number.isFinite(taskInstanceId)) {
              continue
            }
            let skipLineNum = 0
            let taskLogText = ''
            let previousMessage = ''
            for (let attempt = 0; attempt < 12; attempt += 1) {
              const logChunk = await queryLog({
                taskInstanceId,
                skipLineNum,
                limit: 1000
              })
              const message = logChunk?.message || ''
              const lineNum = Number(logChunk?.lineNum || 0)
              if (!message || message === previousMessage) break
              taskLogText += message
              previousMessage = message
              skipLineNum += lineNum || message.split(/\r?\n/).length
              if (!lineNum) break
            }
            const counts = extractReadWriteCountFromLog(taskLogText)
            if (counts.readRows !== null) {
              totalReadRows += counts.readRows
              hasReadRows = true
            }
            if (counts.writeRows !== null) {
              totalWriteRows += counts.writeRows
              hasWriteRows = true
            }
          }
          latestReadRows = hasReadRows ? totalReadRows : null
          latestWriteRows = hasWriteRows ? totalWriteRows : null
        } catch (err) {
          latestReadRows = null
          latestWriteRows = null
        }
      }

      const instanceState = latestInstance?.state || ''
      const status: SyncTaskAssetStatus =
        instanceState === 'RUNNING_EXECUTION' ||
        instanceState === 'SUBMITTED_SUCCESS' ||
        instanceState === 'SERIAL_WAIT'
          ? 'RUNNING'
          : instanceState === 'FAILURE' || instanceState === 'STOP'
            ? 'FAILED'
            : instanceState === 'SUCCESS'
              ? 'SUCCESS'
              : workflow?.releaseState === 'OFFLINE'
                ? 'OFFLINE'
                : 'OFFLINE'
      const scheduleOnline =
        workflow?.scheduleReleaseState === 'ONLINE' ||
        workflow?.schedule?.releaseState === 'ONLINE'
      const updatedAt = normalizeDateText(workflow?.updateTime || workflow?.createTime)
      const lastRunTime = normalizeDateText(
        latestInstance?.startTime || latestInstance?.submitTime || workflow?.updateTime
      )

      return {
        id: `workflow-${project.value}-${workflowCode || workflow?.id || workflow?.name}`,
        name: workflow?.name || '-',
        projectCode: project.value,
        projectName: project.label,
        status,
        scheduleStatus: scheduleOnline ? 'ON' : 'OFF',
        sourceType: pathMeta.sourceType,
        targetType: pathMeta.targetType,
        sourceName: pathMeta.sourceType,
        sourcePath: designFromRawScript.sourcePath || pathMeta.sourcePath,
        targetName: pathMeta.targetType,
        targetPath: designFromRawScript.targetPath || pathMeta.targetPath,
        workflowCode,
        workflowName: workflow?.name || '',
        workflowVersion: Number(workflow?.version) || 1,
        lastRunTime,
        lastInstanceId: Number(latestInstance?.id) || null,
        readRows: latestReadRows,
        writeRows: latestWriteRows,
        duration: '-',
        updatedAt,
        owner: workflow?.userName || workflow?.modifyBy || 'admin',
        errorMessage: status === 'FAILED' ? '最近一次实例执行失败，请进入详情查看日志诊断。' : '',
        sourceFilters: cloneSourceFilters(
          designFromRawScript.sourceFilters.length
            ? designFromRawScript.sourceFilters
            : [createSourceFilterRule()]
        ),
        sinkCustomSql: designFromRawScript.sinkCustomSql,
        fieldRows: cloneFieldRows(designFromRawScript.fieldRows),
        sourceColumns: cloneColumns(designFromRawScript.sourceColumns),
        targetColumns: cloneColumns(designFromRawScript.targetColumns),
        configText: designFromRawScript.configText,
        history: latestInstance
          ? [
              {
                id: String(latestInstance.id),
                status,
                trigger: workflow?.schedule ? '调度运行' : '手动运行',
                startTime: normalizeDateText(latestInstance.startTime),
                endTime: normalizeDateText(latestInstance.endTime),
                duration: '-',
                rows: formatReadWriteRows(latestReadRows, latestWriteRows)
              }
            ]
          : [],
        changes: [
          {
            time: updatedAt,
            user: workflow?.modifyBy || workflow?.userName || 'admin',
            action: '从 Dolphin 工作流定义同步'
          }
        ],
        source: 'REAL'
      }
    }

    const loadSyncTaskAssets = async () => {
      if (state.loadingAssets || !state.projectOptions.length) return
      state.loadingAssets = true
      try {
        const assetGroups = await Promise.all(
          state.projectOptions.map(async (project) => {
            try {
              const response = await queryWorkflowDefinitionListPaging(
                {
                  pageNo: 1,
                  pageSize: 200,
                  searchVal: ''
                },
                project.value
              )
              const workflows = normalizeList(response).filter(isSyncWorkflowDefinition)
              const assets = await Promise.all(
                workflows.map((workflow) => buildAssetFromWorkflow(workflow, project))
              )
              return assets
            } catch (err) {
              return [] as SyncTaskAsset[]
            }
          })
        )
        const realAssets = assetGroups.flat().sort((left, right) =>
          right.updatedAt.localeCompare(left.updatedAt)
        )
        const localAssets = state.syncTaskAssets.filter((item) => item.source === 'LOCAL')
        const realAssetKeys = new Set(
          realAssets.map((item) => `${item.projectCode}:${item.workflowCode || item.name}`)
        )
        state.syncTaskAssets = [
          ...localAssets.filter(
            (item) => !realAssetKeys.has(`${item.projectCode}:${item.workflowCode || item.name}`)
          ),
          ...realAssets
        ]
      } finally {
        state.loadingAssets = false
      }
    }

    const stepOneCheckItems = computed(() => [
      {
        label: '项目',
        done: !!state.selectedProjectCode,
        detail: selectedProjectOption.value?.label || '未选择'
      },
      {
        label: '源端对象',
        done: !!state.source.datasourceId && !!state.source.database && !!state.source.table,
        detail:
          state.source.database && state.source.table
            ? `${state.source.database}.${state.source.table}`
            : '未完成'
      },
      {
        label: '目标端对象',
        done: !!state.target.datasourceId && !!state.target.database,
        detail:
          state.target.database && state.targetTableName.trim()
            ? `${state.target.database}.${state.targetTableName.trim()}`
            : '未完成'
      },
      {
        label: '目标预检查',
        done: !!state.targetTableName.trim(),
        detail: state.targetTableName.trim()
          ? targetTableExists.value
            ? '已存在'
            : '不存在'
          : '待确认'
      }
    ])

    const activeSourceFilterCount = computed(
      () => state.sourceFilters.filter((item) => item.enabled && item.field.trim()).length
    )

    const stepOneReadyText = computed(() => {
      const missing = stepOneCheckItems.value.filter((item) => !item.done)
      if (!missing.length) {
        return targetTableExists.value ? '使用已有表' : '将新建目标表'
      }
      return `还需完成：${missing.map((item) => item.label).join('、')}`
    })

    const targetTypeOptions = computed(() => {
      const targetType = targetDatasourceOption.value?.type
      const options = targetType
        ? TARGET_TYPE_OPTIONS[targetType]
        : GENERIC_TARGET_TYPE_OPTIONS
      return options.map((item) => ({
        label: item,
        value: item
      }))
    })

    const normalizeTargetMappings = (
      rows: FieldDesignRow[],
      targetKeys = rows.filter((item) => item.sync).map((item) => item.key)
    ): FieldDesignRow[] => {
      const selectedKeys = rows.filter((item) => item.sync).map((item) => item.key)
      if (!selectedKeys.length) {
        return rows.map((item) => ({
          ...item,
          mappedTargetKey: null,
          mappingKind: undefined
        }))
      }

      const usedTargetKeys = new Set<string>()
      const availableTargetKeys = targetKeys.length ? targetKeys : selectedKeys
      const remainingTargetKeys = [...availableTargetKeys]

      return rows.map((row) => {
        if (!row.sync) {
          return {
            ...row,
            mappedTargetKey: null,
            mappingKind: undefined
          }
        }
        const nextTargetKey = row.mappedTargetKey
        if (nextTargetKey && availableTargetKeys.includes(nextTargetKey) && !usedTargetKeys.has(nextTargetKey)) {
          usedTargetKeys.add(nextTargetKey)
          const remainIndex = remainingTargetKeys.indexOf(nextTargetKey)
          if (remainIndex >= 0) {
            remainingTargetKeys.splice(remainIndex, 1)
          }
          return row
        }
        if (!remainingTargetKeys.length) {
          return {
            ...row,
            mappedTargetKey: null,
            mappingKind: undefined
          }
        }
        const fallbackTargetKey = remainingTargetKeys.shift() || null
        if (!fallbackTargetKey) {
          return {
            ...row,
            mappedTargetKey: null,
            mappingKind: undefined
          }
        }
        usedTargetKeys.add(fallbackTargetKey)
        return {
          ...row,
          mappedTargetKey: fallbackTargetKey,
          mappingKind: row.mappingKind || 'AUTO'
        }
      })
    }

    const selectedFieldRows = computed(() => state.fieldRows.filter((item) => item.sync))
    const mappedSourceByTargetKey = computed(() => {
      const targetMap = new Map<string, FieldDesignRow>()
      selectedFieldRows.value.forEach((row) => {
        if (row.mappedTargetKey) {
          targetMap.set(row.mappedTargetKey, row)
        }
      })
      return targetMap
    })

    const targetFieldRows = computed(() =>
      targetTableMode.value === 'EXISTING_TABLE'
        ? state.fieldRows
        : selectedFieldRows.value
    )

    const generatedConfig = computed(() => {
      const sourceDetail = state.source.datasourceId
        ? state.datasourceDetails[state.source.datasourceId]
        : null
      const targetDetail = state.target.datasourceId
        ? state.datasourceDetails[state.target.datasourceId]
        : null
      if (
        !sourceDetail ||
        !targetDetail ||
        !state.source.database ||
        !state.source.table ||
        !state.target.database ||
        !state.targetTableName.trim()
      ) {
        return '# 请选择完整的源端、目标端和字段映射后，再自动生成 SeaTunnel 配置'
      }

      const sourceQuery = buildSourceSelectSql(
        state.fieldRows,
        state.sourceFilters,
        sourceDetail.type,
        state.source.table,
        state.source.columns,
        state.agentSampleLimit
      )
      const transformQuery = buildSeatunnelTransformQuery(state.fieldRows)
      const primaryKeys = buildPrimaryKeys(state.fieldRows)
      const targetType = targetDetail.type
      const targetSchema = state.targetSchemaName.trim() || getDefaultSchemaName(targetType)
      const sinkTableName =
        targetType === 'POSTGRESQL' || targetType === 'ORACLE'
          ? `${targetSchema}.${state.targetTableName.trim()}`
          : state.targetTableName.trim()
      const sinkTable = buildSinkTable(
        targetType,
        state.target.database,
        targetSchema,
        sinkTableName
      )
      const sinkInsertQuery = buildJdbcSinkInsertQuery(
        state.fieldRows,
        sinkTable
      )

      const lines = [
        'env {',
        '  execution.parallelism = 1',
        '  job.mode = "BATCH"',
        '}',
        '',
        'source {',
        '  Jdbc {',
        `    url = "${buildJdbcUrl(sourceDetail, state.source.database)}"`,
        `    driver = "${buildDriver(sourceDetail.type)}"`,
        `    user = "${sourceDetail.userName}"`,
        `    password = "${sourceDetail.password}"`,
        `    query = "${sourceQuery}"`,
        '    result_table_name = "sync_source"',
        '  }',
        '}',
        '',
        'transform {',
        '  Sql {',
        '    source_table_name = "sync_source"',
        '    result_table_name = "sync_mapped"',
        `    query = "${transformQuery}"`,
        '  }',
        '}',
        '',
        'sink {',
        '  Jdbc {',
        '    source_table_name = "sync_mapped"',
        `    url = "${buildJdbcUrl(targetDetail, state.target.database)}"`,
        `    driver = "${buildDriver(targetDetail.type)}"`,
        `    user = "${targetDetail.userName}"`,
        `    password = "${targetDetail.password}"`,
        sinkInsertQuery ? `    query = "${sinkInsertQuery}"` : '    generate_sink_sql = true',
        `    database = "${state.target.database}"`,
        `    table = "${sinkTable}"`
      ]
      const customSql = state.sinkCustomSql.trim()

      if (customSql) {
        lines.push('    data_save_mode = "CUSTOM_PROCESSING"')
        lines.push(`    custom_sql = "${escapeSeatunnelString(customSql)}"`)
      }

      if (primaryKeys.length) {
        lines.push(
          `    primary_keys = [${primaryKeys.map((item) => `"${item}"`).join(', ')}]`
        )
      }

      lines.push('  }', '}')
      return lines.join('\n')
    })

    const effectiveConfigText = computed(() =>
      state.configManualOverride ? state.configEditorText : generatedConfig.value
    )

    const summaryItems = computed(() => [
      {
        label: '执行方式',
        value: state.executionMode === 'IMMEDIATE' ? '立即执行' : '周期调度'
      },
      {
        label: '同步方向',
        value: `${sourceDatasourceOption.value?.type || '-'} -> ${
          targetDatasourceOption.value?.type || '-'
        }`
      },
      {
        label: '源端过滤',
        value: describeSourceFilters(state.sourceFilters)
      },
      {
        label: '源表',
        value: state.source.table || '-'
      },
      {
        label: '目标表',
        value: state.targetTableName || state.target.table || '-'
      },
      {
        label: '映射字段数',
        value: String(mappedCount.value)
      },
      {
        label: '调度状态',
        value: state.latestScheduleSummary
      }
    ])

    watch(
      generatedConfig,
      (value) => {
        if (!state.configManualOverride || !state.configEditorText.trim()) {
          state.configEditorText = value
        }
      },
      {
        immediate: true
      }
    )

    const stepItems = computed(() => [
      {
        index: 1,
        title: '选择源与目标',
        description: '确认项目、数据源、库表和目标表'
      },
      {
        index: 2,
        title: '配置同步方案',
        description: '配置字段映射、源端过滤、数据去向'
      },
      {
        index: 3,
        title: '执行与调度',
        description: '配置建表、立即执行或周期调度'
      },
      {
        index: 4,
        title: '预览与发布',
        description: '检查配置结果并保存、执行'
      }
    ])

    const currentStepMeta = computed(
      () =>
        stepItems.value.find((item) => item.index === state.currentStep) ||
        stepItems.value[0]
    )

    const mappingLinePaths = computed(() => {
      if (!state.mappingLinesVisible) {
        return []
      }
      return selectedFieldRows.value
        .filter((sourceRow) => {
          if (!state.mappingExceptionOnly) return true
          const targetRow = state.fieldRows.find(
            (item) => item.key === sourceRow.mappedTargetKey
          )
          return !targetRow || targetRow.targetColumn !== sourceRow.sourceColumn
        })
        .map((sourceRow) => {
          const sourcePoint =
            mappingAnchorPositions.value[`source:${sourceRow.sourceColumn}`] ||
            mappingAnchorPositions.value[`source:${sourceRow.key}`]
          const targetPoint = sourceRow.mappedTargetKey
            ? mappingAnchorPositions.value[`target:${sourceRow.mappedTargetKey}`]
            : null
          if (!sourcePoint || !targetPoint) {
            return null
          }
          return {
            key: sourceRow.key,
            path: buildMappingPath(sourcePoint, targetPoint),
            kind: sourceRow.mappingKind || 'AUTO',
            active:
              draggingMapping.value?.side === 'source'
                ? draggingMapping.value.key === sourceRow.key ||
                  draggingMapping.value.key === sourceRow.sourceColumn
                : draggingMapping.value?.side === 'target'
                  ? sourceRow.mappedTargetKey === draggingMapping.value.key
                  : false
          }
        })
        .filter(Boolean) as Array<{ key: string; path: string; kind: MappingKind; active: boolean }>
    })

    const mappingDraftPath = computed(() => {
      if (!state.mappingLinesVisible || !draggingMapping.value || !mappingDraftPoint.value) {
        return ''
      }
      const startPoint =
        mappingAnchorPositions.value[
          `${draggingMapping.value.side}:${draggingMapping.value.key}`
        ]
      if (!startPoint) {
        return ''
      }
      return draggingMapping.value.side === 'source'
        ? buildMappingPath(startPoint, mappingDraftPoint.value)
        : buildMappingPath(mappingDraftPoint.value, startPoint)
    })

    watch(
      () =>
        `${state.currentStep}|${state.fieldRows
          .map(
            (item) =>
              `${item.key}:${item.sync}:${item.mappedTargetKey || ''}:${item.targetPrimaryKey}:${item.targetNullable}`
          )
          .join('|')}`,
      () => {
        void nextTick(refreshMappingLayout)
      }
    )

    onMounted(() => {
      window.addEventListener('resize', refreshMappingLayout)
      window.addEventListener('mousemove', handleGlobalMouseMove)
      window.addEventListener('mouseup', handleGlobalMouseUp)
    })

    onBeforeUnmount(() => {
      window.removeEventListener('resize', refreshMappingLayout)
      window.removeEventListener('mousemove', handleGlobalMouseMove)
      window.removeEventListener('mouseup', handleGlobalMouseUp)
      stopLatestInstancePolling()
    })

    const loadDatasourceDetail = async (datasourceId: number) => {
      const cachedDetail = state.datasourceDetails[datasourceId]
      if (
        cachedDetail &&
        cachedDetail.host &&
        cachedDetail.userName &&
        cachedDetail.password
      ) {
        return
      }
      // 同步任务页面直接复用数据源列表中已返回的连接参数，
      // 不再额外要求用户手工补密码，避免“数据源已可用、页面却提示密码为空”的割裂体验。
      if (!cachedDetail) {
        throw new Error(`未找到数据源详情: ${datasourceId}`)
      }
    }

    const loadDatasourceList = async () => {
      if (state.loadingDatasources) return
      state.loadingDatasources = true
      const datasourceResponses = await Promise.all(
        SUPPORT_TYPES.map((type) => queryDataSourceList({ type }))
      )
      const res = datasourceResponses.flatMap((item) => normalizeList(item))
      state.loadingDatasources = false
      state.datasourceOptions = res
        .filter((item) => SUPPORT_TYPES.includes(item.type))
        .map((item) => ({
          label: `${item.name} (${item.type})`,
          value: item.id,
          type: item.type
        }))
      res.forEach((item: DatasourceRecord) => {
        const parsedDetail = parseConnectionParams(item)
        state.datasourceDetails[item.id] = {
          id: item.id,
          name: item.name,
          type: item.type,
          host: parsedDetail.host || '127.0.0.1',
          port: parsedDetail.port || DEFAULT_PORTS[item.type],
          userName: parsedDetail.userName || '',
          password: parsedDetail.password || '',
          database: parsedDetail.database || ''
        }
      })
    }

    const loadProjects = async () => {
      if (state.loadingProjects) return
      state.loadingProjects = true
      const res = await queryAllProjectList()
      state.loadingProjects = false
      state.projectOptions = normalizeList(res).map((item) => ({
        label: item.name,
        value: item.code
      }))
      await loadSyncTaskAssets()
    }

    const resetEndpoint = (endpoint: EndpointState) => {
      endpoint.database = null
      endpoint.table = null
      endpoint.databases = []
      endpoint.tables = []
      endpoint.columns = []
    }

    const refreshFieldRows = () => {
      if (targetTableMode.value === 'EXISTING_TABLE' && !state.target.columns.length) {
        // 已有目标表模式必须等目标字段元数据返回后再构建字段设计行。
        // 否则会先按“新建表”逻辑生成一批 sync=false 的源驱动行，
        // 后续再切回目标驱动时把这份旧状态错误继承下来，导致同名字段不自动映射。
        state.fieldRows = []
        void nextTick(refreshMappingLayout)
        return
      }

      const existed = new Map(
        state.fieldRows.map((item) => [item.sourceColumn, item])
      )
      const existedByKey = new Map(
        state.fieldRows.map((item) => [item.key, item])
      )
      const inferredTargetType = targetDatasourceOption.value?.type
      const sourceColumnMap = new Map(
        state.source.columns.map((item) => [item.name.toLowerCase(), item])
      )

      if (targetTableMode.value === 'EXISTING_TABLE' && state.target.columns.length) {
        const targetKeys = state.target.columns.map((targetColumn) => targetColumn.name)
        state.fieldRows = normalizeTargetMappings(
          state.target.columns.map((targetColumn) => {
            const matchedSource = sourceColumnMap.get(targetColumn.name.toLowerCase()) || null
            const oldRow = existedByKey.get(targetColumn.name) || null
            const preservedSource =
              (oldRow?.sourceColumn
                ? sourceColumnMap.get(oldRow.sourceColumn.toLowerCase())
                : null) || matchedSource
            return {
              key: targetColumn.name,
              sourceColumn: preservedSource?.name || oldRow?.sourceColumn || '',
              sourceType: preservedSource?.type || oldRow?.sourceType || '',
              sourceComment: preservedSource?.comment || oldRow?.sourceComment || '',
              sourcePrimaryKey:
                preservedSource?.primaryKey ?? oldRow?.sourcePrimaryKey ?? false,
              sourceNullable:
                preservedSource?.nullable ?? oldRow?.sourceNullable ?? true,
              targetColumn: targetColumn.name,
              targetType: targetColumn.type,
              targetComment: targetColumn.comment || '',
              targetPrimaryKey: !!targetColumn.primaryKey,
              targetNullable: !!targetColumn.nullable,
              sync: oldRow?.sync ?? !!matchedSource,
              mappedTargetKey:
                oldRow?.sync && oldRow?.mappedTargetKey
                  ? oldRow.mappedTargetKey
                  : oldRow?.sync || matchedSource
                    ? targetColumn.name
                    : null,
              mappingKind: oldRow?.mappingKind || (matchedSource ? 'AUTO' : undefined),
              targetColumnTouched: oldRow?.targetColumnTouched || false
            }
          }),
          targetKeys
        )
        void nextTick(refreshMappingLayout)
        return
      }

      state.fieldRows = normalizeTargetMappings(
        state.source.columns.map((sourceColumn) => {
          const oldRow = existed.get(sourceColumn.name)
          return {
            key: sourceColumn.name,
            sourceColumn: sourceColumn.name,
            sourceType: sourceColumn.type,
            sourceComment: oldRow?.sourceComment || sourceColumn.comment || '',
            sourcePrimaryKey: oldRow?.sourcePrimaryKey ?? !!sourceColumn.primaryKey,
            sourceNullable: oldRow?.sourceNullable ?? !!sourceColumn.nullable,
            targetColumn: oldRow?.targetColumn || sourceColumn.name,
            targetType:
              oldRow?.targetType ||
              inferTargetColumnType(sourceColumn.type, inferredTargetType),
            targetComment:
              oldRow?.targetComment ||
              sourceColumn.comment ||
              '',
            targetPrimaryKey:
              oldRow?.targetPrimaryKey ?? !!sourceColumn.primaryKey,
            targetNullable:
              oldRow?.targetNullable ?? !!sourceColumn.nullable,
            sync: oldRow?.sync || false,
            mappedTargetKey: oldRow?.mappedTargetKey || null,
            mappingKind: oldRow?.mappingKind || (oldRow?.mappedTargetKey ? 'AUTO' : undefined),
            targetColumnTouched: oldRow?.targetColumnTouched || false
          }
        })
      )
      void nextTick(refreshMappingLayout)
    }

    const updateFieldRow = (
      sourceColumnName: string,
      updater: (row: FieldDesignRow) => FieldDesignRow
    ) => {
      state.fieldRows = state.fieldRows.map((item) =>
        item.sourceColumn === sourceColumnName || item.key === sourceColumnName
          ? updater(item)
          : item
      )
    }

    const handleToggleField = (sourceColumnName: string, checked: boolean) => {
      if (targetTableMode.value === 'EXISTING_TABLE') {
        const sourceMeta = getSourceColumnMeta(sourceColumnName)
        const currentTargetRow = state.fieldRows.find(
          (item) => item.sourceColumn === sourceColumnName
        )
        const sameNameTargetRow = state.fieldRows.find(
          (item) => item.key.toLowerCase() === sourceColumnName.toLowerCase()
        )

        if (!checked) {
          if (!currentTargetRow) {
            return
          }
          state.fieldRows = state.fieldRows.map((item) =>
            item.sourceColumn === sourceColumnName
              ? {
                  ...item,
                  sourceColumn: '',
                  sourceType: '',
                  sourceComment: '',
                  sourcePrimaryKey: false,
                  sourceNullable: true,
                  sync: false,
                  mappedTargetKey: null,
                  mappingKind: undefined
                }
              : item
          )
          void nextTick(refreshMappingLayout)
          return
        }

        const targetRow = currentTargetRow || sameNameTargetRow
        if (!sourceMeta || !targetRow) {
          window.$message.warning('该源字段在已有目标表中没有同名目标字段，请通过拖拽连线手动关联。')
          return
        }

        state.fieldRows = state.fieldRows.map((item) =>
          item.key === targetRow.key
            ? {
                ...item,
                sourceColumn: sourceMeta.name,
                sourceType: sourceMeta.type,
                sourceComment: sourceMeta.comment || '',
                sourcePrimaryKey: !!sourceMeta.primaryKey,
                sourceNullable: !!sourceMeta.nullable,
                sync: true,
                mappedTargetKey: targetRow.key,
                mappingKind: 'AUTO'
              }
            : item
        )
        void nextTick(refreshMappingLayout)
        return
      }

      state.fieldRows = normalizeTargetMappings(
        state.fieldRows.map((item) =>
          item.sourceColumn === sourceColumnName || item.key === sourceColumnName
            ? {
                ...item,
                sync: checked
              }
            : item
        )
      )
      void nextTick(refreshMappingLayout)
    }

    const handleTargetColumnNameChange = (
      sourceColumnName: string,
      targetColumnName: string
    ) => {
      updateFieldRow(sourceColumnName, (row) => ({
        ...row,
        targetColumn: targetColumnName,
        targetColumnTouched: true
      }))
    }

    const applyTargetNameRule = (rule: TargetNameRule) => {
      state.targetNameRule = rule
      if (targetTableMode.value === 'EXISTING_TABLE') {
        return
      }
      state.fieldRows = state.fieldRows.map((row) => {
        if (row.targetColumnTouched) return row
        const sourceName = row.sourceColumn || row.targetColumn
        const targetColumn =
          rule === 'LOWERCASE'
            ? sourceName.toLowerCase()
            : rule === 'UPPERCASE'
              ? sourceName.toUpperCase()
              : sourceName
        return {
          ...row,
          targetColumn
        }
      })
      void nextTick(refreshMappingLayout)
    }

    const handleTargetTypeChange = (
      sourceColumnName: string,
      targetType: string | null
    ) => {
      updateFieldRow(sourceColumnName, (row) => ({
        ...row,
        targetType: targetType || row.targetType
      }))
    }

    const handleTargetCommentChange = (
      sourceColumnName: string,
      targetComment: string
    ) => {
      updateFieldRow(sourceColumnName, (row) => ({
        ...row,
        targetComment
      }))
    }

    const handleTargetPrimaryKeyChange = (
      sourceColumnName: string,
      targetPrimaryKey: boolean
    ) => {
      updateFieldRow(sourceColumnName, (row) => ({
        ...row,
        targetPrimaryKey
      }))
    }

    const handleTargetNullableChange = (
      sourceColumnName: string,
      targetNullable: boolean
    ) => {
      updateFieldRow(sourceColumnName, (row) => ({
        ...row,
        targetNullable
      }))
    }

    const getSourceTypeForMapping = (sourceKey: string) => {
      return getSourceColumnMeta(sourceKey)?.type ||
        state.fieldRows.find((item) => item.key === sourceKey || item.sourceColumn === sourceKey)?.sourceType ||
        ''
    }

    const validateMappingCompatibility = (sourceKey: string, targetKey: string) => {
      const sourceType = getSourceTypeForMapping(sourceKey)
      const targetType =
        state.fieldRows.find((item) => item.key === targetKey)?.targetType || ''
      const result = canMapFieldType(sourceType, targetType)
      if (!result.ok) {
        window.$message.warning(
          `字段类型不兼容：${sourceType || 'UNKNOWN'} 不能安全写入 ${targetType || 'UNKNOWN'}。${result.reason || ''}`
        )
        return false
      }
      return true
    }

    const handleMapSourceToTarget = (sourceKey: string, targetKey: string) => {
      if (!validateMappingCompatibility(sourceKey, targetKey)) {
        draggingMapping.value = null
        mappingDraftPoint.value = null
        return
      }
      if (targetTableMode.value === 'EXISTING_TABLE') {
        const sourceMeta = getSourceColumnMeta(sourceKey)
        const targetRow = state.fieldRows.find((item) => item.key === targetKey)
        if (!sourceMeta || !targetRow) {
          return
        }
        state.fieldRows = state.fieldRows.map((item) => {
          if (item.key === targetKey) {
            return {
              ...item,
              sourceColumn: sourceMeta.name,
              sourceType: sourceMeta.type,
              sourceComment: sourceMeta.comment || '',
              sourcePrimaryKey: !!sourceMeta.primaryKey,
              sourceNullable: !!sourceMeta.nullable,
              sync: true,
              mappedTargetKey: targetKey,
              mappingKind: 'MANUAL'
            }
          }
          return item
        })
        draggingMapping.value = null
        mappingDraftPoint.value = null
        void nextTick(refreshMappingLayout)
        return
      }

      const sourceRow = state.fieldRows.find(
        (item) => item.sync && (item.key === sourceKey || item.sourceColumn === sourceKey)
      )
      const targetOwner = state.fieldRows.find(
        (item) => item.sync && item.mappedTargetKey === targetKey
      )
      if (!sourceRow) {
        return
      }
      const sourceCurrentTargetKey = sourceRow.mappedTargetKey
      state.fieldRows = normalizeTargetMappings(
        state.fieldRows.map((item) => {
          if (item.key === sourceKey) {
            return {
              ...item,
              mappedTargetKey: targetKey,
              sync: true,
              mappingKind: 'MANUAL'
            }
          }
          if (item.key === targetKey && targetTableMode.value === 'EXISTING_TABLE') {
            return {
              ...item,
              sync: true,
              mappedTargetKey: targetKey,
              sourceColumn: sourceRow.sourceColumn,
              sourceType: sourceRow.sourceType,
              sourceComment: sourceRow.sourceComment,
              sourcePrimaryKey: sourceRow.sourcePrimaryKey,
              sourceNullable: sourceRow.sourceNullable,
              mappingKind: 'MANUAL'
            }
          }
          if (
            targetOwner &&
            targetOwner.key !== sourceKey &&
            item.key === targetOwner.key
          ) {
            return {
              ...item,
              mappedTargetKey: targetTableMode.value === 'EXISTING_TABLE'
                ? null
                : sourceCurrentTargetKey || item.key,
              mappingKind: targetTableMode.value === 'EXISTING_TABLE'
                ? undefined
                : item.mappingKind
            }
          }
          return item
        })
      )
      draggingMapping.value = null
      mappingDraftPoint.value = null
      void nextTick(refreshMappingLayout)
    }

    const handleMapTargetToSource = (targetKey: string, sourceKey: string) => {
      handleMapSourceToTarget(sourceKey, targetKey)
    }

    const loadDatabases = async (endpoint: EndpointState) => {
      if (!endpoint.datasourceId) return
      endpoint.loading = true
      try {
        await loadDatasourceDetail(endpoint.datasourceId)
        const res = await getDatasourceDatabasesById(endpoint.datasourceId)
        endpoint.databases = normalizeTextList(res)
        const datasourceDetail = state.datasourceDetails[endpoint.datasourceId]
        if (!endpoint.database && datasourceDetail?.database) {
          endpoint.database = datasourceDetail.database
        }
        if (!endpoint.database && endpoint.databases.length) {
          endpoint.database = endpoint.databases[0]
        }
      } catch (err) {
        endpoint.databases = []
        endpoint.database = null
        window.$message.error('读取数据源库列表失败，请检查该数据源的连接信息。')
      }
      endpoint.loading = false
    }

    const loadTables = async (endpoint: EndpointState) => {
      if (!endpoint.datasourceId || !endpoint.database) return
      endpoint.loading = true
      try {
        const res = await getDatasourceTablesById(
          endpoint.datasourceId,
          endpoint.database
        )
        endpoint.tables = normalizeTextList(res)
        if (endpoint.table) {
          const matchedTable = endpoint.tables.find(
            (item) =>
              item === endpoint.table ||
              item.split('.').at(-1) === endpoint.table ||
              endpoint.table?.split('.').at(-1) === item
          )
          if (matchedTable) {
            endpoint.table = matchedTable
            return
          }
          if (state.editingAssetId) {
            endpoint.tables = [endpoint.table, ...endpoint.tables]
            return
          }
        }
        endpoint.table = endpoint === state.source ? endpoint.tables[0] || null : null
      } catch (err) {
        endpoint.tables = []
        if (!state.editingAssetId || !endpoint.table) {
          endpoint.table = null
        }
        window.$message.error('读取表列表失败，请确认数据库名称和数据源连接是否正确。')
      }
      endpoint.loading = false
    }

    const loadColumns = async (endpoint: EndpointState) => {
      if (!endpoint.datasourceId || !endpoint.database || !endpoint.table) return
      endpoint.loading = true
      try {
        const res = await getDatasourceTableColumnMetasById(
          endpoint.datasourceId,
          endpoint.database,
          endpoint.table
        )
        endpoint.columns = normalizeColumnList(res)
      } catch (err) {
        endpoint.columns = []
        window.$message.error('读取字段列表失败，请确认目标表存在且当前账号有查询权限。')
      }
      endpoint.loading = false
    }

    const refreshMappingLayout = () => {
      const container = mappingWorkbenchRef.value
      if (!container || state.currentStep !== 2) return
      const containerRect = container.getBoundingClientRect()
      const nextPositions: Record<string, { x: number; y: number }> = {}

      container.querySelectorAll<HTMLElement>('[data-source-anchor]').forEach((node) => {
        const key = node.dataset.sourceAnchor
        if (!key) return
        const rect = node.getBoundingClientRect()
        nextPositions[`source:${key}`] = {
          x: rect.left + rect.width / 2 - containerRect.left,
          y: rect.top + rect.height / 2 - containerRect.top
        }
      })

      container.querySelectorAll<HTMLElement>('[data-target-anchor]').forEach((node) => {
        const key = node.dataset.targetAnchor
        if (!key) return
        const rect = node.getBoundingClientRect()
        nextPositions[`target:${key}`] = {
          x: rect.left + rect.width / 2 - containerRect.left,
          y: rect.top + rect.height / 2 - containerRect.top
        }
      })

      mappingAnchorPositions.value = nextPositions
    }

    const handleGlobalMouseMove = (event: MouseEvent) => {
      if (!draggingMapping.value || !mappingWorkbenchRef.value) {
        return
      }
      const containerRect = mappingWorkbenchRef.value.getBoundingClientRect()
      mappingDraftPoint.value = {
        x: event.clientX - containerRect.left,
        y: event.clientY - containerRect.top
      }
    }

    const handleGlobalMouseUp = () => {
      if (!draggingMapping.value) {
        return
      }
      draggingMapping.value = null
      mappingDraftPoint.value = null
    }

    const handleStartMappingDrag = (
      side: 'source' | 'target',
      key: string,
      event: MouseEvent
    ) => {
      if (
        side === 'source' &&
        !getSourceColumnMeta(key) &&
        !state.fieldRows.find((item) => item.key === key || item.sourceColumn === key)
      ) {
        return
      }
      if (side === 'target' && !state.fieldRows.find((item) => item.key === key)) {
        return
      }
      event.preventDefault()
      event.stopPropagation()
      draggingMapping.value = {
        side,
        key
      }
      handleGlobalMouseMove(event)
    }

    const resetAgentStages = () => {
      state.agentStages = state.agentStages.map((stage) => ({
        ...stage,
        status: 'WAITING',
        message: '等待执行'
      }))
      state.agentError = ''
    }

    const setAgentStage = (
      key: SyncAgentStageKey,
      status: SyncAgentStageStatus,
      message: string
    ) => {
      state.agentStages = state.agentStages.map((stage) =>
        stage.key === key ? { ...stage, status, message } : stage
      )
    }

    const openAgentDrawer = () => {
      state.agentDrawerVisible = true
      if (!state.agentCommand.trim()) {
        state.agentCommand = SYNC_AGENT_EXAMPLES[0]
      }
    }

    const fillAgentExample = (command: string) => {
      state.agentCommand = command
      state.agentError = ''
    }

    const findAgentDatasource = (
      type: SyncDatasourceType | null,
      databaseName: string,
      command: string
    ) => {
      if (!type) return null
      const normalizedDatabase = databaseName.toLowerCase()
      const normalizedCommand = command.toLowerCase()
      const candidates = state.datasourceOptions.filter((item) => item.type === type)
      if (!candidates.length) return null
      return (
        candidates.find((item) =>
          String(item.label).toLowerCase().includes(normalizedDatabase)
        ) ||
        candidates.find((item) =>
          normalizedCommand.includes(String(item.label).split(' ')[0].toLowerCase())
        ) ||
        candidates[0]
      )
    }

    const chooseAgentTargetDatabase = (
      parsed: SyncAgentParsedIntent,
      targetEndpoint: EndpointState
    ) => {
      if (parsed.targetDatabase) {
        const matched = targetEndpoint.databases.find(
          (item) => item.toLowerCase() === parsed.targetDatabase.toLowerCase()
        )
        if (matched) return matched
      }
      const detailDatabase = targetEndpoint.datasourceId
        ? state.datasourceDetails[targetEndpoint.datasourceId]?.database
        : ''
      return detailDatabase || targetEndpoint.databases[0] || parsed.targetDatabase || ''
    }

    const applyAgentFieldMappings = () => {
      if (!state.source.columns.length) {
        state.fieldRows = []
        return
      }
      if (targetTableMode.value === 'EXISTING_TABLE' && state.target.columns.length) {
        const sourceMap = new Map(
          state.source.columns.map((item) => [item.name.toLowerCase(), item])
        )
        state.fieldRows = state.target.columns.map((targetColumn) => {
          const matchedSource = sourceMap.get(targetColumn.name.toLowerCase()) || null
          return {
            key: targetColumn.name,
            sourceColumn: matchedSource?.name || '',
            sourceType: matchedSource?.type || '',
            sourceComment: matchedSource?.comment || '',
            sourcePrimaryKey: !!matchedSource?.primaryKey,
            sourceNullable: matchedSource?.nullable ?? true,
            targetColumn: targetColumn.name,
            targetType: targetColumn.type,
            targetComment: targetColumn.comment || '',
            targetPrimaryKey: !!targetColumn.primaryKey,
            targetNullable: !!targetColumn.nullable,
            sync: !!matchedSource,
            mappedTargetKey: matchedSource ? targetColumn.name : null,
            mappingKind: matchedSource ? 'AUTO' : undefined,
            targetColumnTouched: false
          }
        })
        void nextTick(refreshMappingLayout)
        return
      }
      const targetType = targetDatasourceOption.value?.type
      state.fieldRows = state.source.columns.map((sourceColumn) => ({
        key: sourceColumn.name,
        sourceColumn: sourceColumn.name,
        sourceType: sourceColumn.type,
        sourceComment: sourceColumn.comment || '',
        sourcePrimaryKey: !!sourceColumn.primaryKey,
        sourceNullable: !!sourceColumn.nullable,
        targetColumn: sourceColumn.name,
        targetType: inferTargetColumnType(sourceColumn.type, targetType),
        targetComment: sourceColumn.comment || '',
        targetPrimaryKey: !!sourceColumn.primaryKey,
        targetNullable: !!sourceColumn.nullable,
        sync: true,
        mappedTargetKey: sourceColumn.name,
        mappingKind: 'AUTO',
        targetColumnTouched: false
      }))
      void nextTick(refreshMappingLayout)
    }

    const applyAgentLimitHint = (parsed: SyncAgentParsedIntent) => {
      state.agentSampleLimit = parsed.limit
      if (!parsed.limit) {
        state.sourceFilters = [createSourceFilterRule()]
        return
      }
      state.sourceFilters = [
        {
          key: `agent-limit-${Date.now()}`,
          enabled: true,
          field: state.source.columns[0]?.name || '',
          operator: 'NOT_NULL',
          value: '',
          valueEnd: ''
        }
      ]
    }

    const applyAgentPlanToWizard = async (executeAfterApply = false) => {
      const parsed = parseSyncAgentIntent(state.agentCommand)
      resetAgentStages()
      state.agentRunning = true
      state.agentPlan = null
      state.agentAutoExecute = parsed.autoExecute || state.agentAutoExecute
      setAgentStage('PARSE', 'RUNNING', '正在解析命令')

      if (!parsed.command.trim()) {
        setAgentStage('PARSE', 'ERROR', '命令为空')
        state.agentError = '请输入同步任务描述。'
        state.agentRunning = false
        return false
      }
      setAgentStage('PARSE', parsed.missing.length ? 'ERROR' : 'SUCCESS',
        parsed.missing.length ? `缺少 ${parsed.missing.join('、')}` : '已识别同步方向和目标表')

      if (parsed.missing.length) {
        state.agentError = `命令信息不完整：缺少 ${parsed.missing.join('、')}。`
        state.agentRunning = false
        state.agentPlan = {
          ...parsed,
          projectCode: null,
          projectName: '-',
          sourceDatasourceId: null,
          sourceDatasourceName: '-',
          targetDatasourceId: null,
          targetDatasourceName: '-',
          sourceColumnCount: 0,
          mappedColumnCount: 0
        }
        return false
      }

      setAgentStage('MATCH', 'RUNNING', '正在匹配项目和数据源')
      if (!state.projectOptions.length) {
        await loadProjects()
      }
      if (!state.datasourceOptions.length) {
        await loadDatasourceList()
      }
      const project = state.projectOptions[0] || null
      const sourceDatasource = findAgentDatasource(
        parsed.sourceType,
        parsed.sourceDatabase,
        parsed.command
      )
      const targetDatasource = findAgentDatasource(
        parsed.targetType,
        parsed.targetDatabase,
        parsed.command
      )
      if (!project || !sourceDatasource || !targetDatasource) {
        const errors = [
          !project ? '项目' : '',
          !sourceDatasource ? '源数据源' : '',
          !targetDatasource ? '目标数据源' : ''
        ].filter(Boolean)
        setAgentStage('MATCH', 'ERROR', `未匹配到 ${errors.join('、')}`)
        state.agentError = `Agent 未匹配到 ${errors.join('、')}，请先检查 Dolphin 项目和数据源配置。`
        state.agentRunning = false
        return false
      }
      setAgentStage('MATCH', 'SUCCESS', `${project.label} / ${sourceDatasource.label} -> ${targetDatasource.label}`)

      resetWizardState()
      state.selectedProjectCode = project.value
      state.source.datasourceId = sourceDatasource.value
      state.target.datasourceId = targetDatasource.value
      state.executionMode = 'IMMEDIATE'
      state.targetSchemaName = parsed.targetSchema || getDefaultSchemaName(parsed.targetType || undefined)
      state.targetTableName = parsed.targetTable
      state.taskName = buildWorkflowName(
        sourceDatasource.label,
        parsed.sourceTable,
        targetDatasource.label,
        parsed.targetTable
      )

      setAgentStage('METADATA', 'RUNNING', '正在加载源端和目标端元数据')
      await loadDatabases(state.source)
      state.source.database =
        state.source.databases.find(
          (item) => item.toLowerCase() === parsed.sourceDatabase.toLowerCase()
        ) ||
        parsed.sourceDatabase ||
        state.source.database
      await loadTables(state.source)
      state.source.table =
        state.source.tables.find(
          (item) =>
            item.toLowerCase() === parsed.sourceTable.toLowerCase() ||
            item.split('.').at(-1)?.toLowerCase() === parsed.sourceTable.toLowerCase()
        ) ||
        parsed.sourceTable ||
        state.source.table
      await loadColumns(state.source)

      await loadDatabases(state.target)
      state.target.database = chooseAgentTargetDatabase(parsed, state.target)
      await loadTables(state.target)
      const targetMatchedTable = state.target.tables.find(
        (item) =>
          item.toLowerCase() === parsed.targetTable.toLowerCase() ||
          item.split('.').at(-1)?.toLowerCase() === parsed.targetTable.toLowerCase()
      )
      state.target.table = targetMatchedTable || null
      if (targetMatchedTable) {
        state.targetTableName = targetMatchedTable
        await loadColumns(state.target)
      }
      if (!state.source.columns.length) {
        setAgentStage('METADATA', 'ERROR', '源表字段加载失败')
        state.agentError = 'Agent 没有读取到源表字段，请检查源库、源表和数据源权限。'
        state.agentRunning = false
        return false
      }
      setAgentStage('METADATA', 'SUCCESS', `源表 ${state.source.columns.length} 列，目标${targetMatchedTable ? '已有表' : '新建表'}`)

      setAgentStage('MAPPING', 'RUNNING', '正在生成字段映射')
      applyAgentFieldMappings()
      applyAgentLimitHint(parsed)
      await nextTick()
      refreshMappingLayout()
      const mapped = state.fieldRows.filter((item) => item.sync && item.mappedTargetKey).length
      setAgentStage('MAPPING', mapped ? 'SUCCESS' : 'ERROR', mapped ? `已映射 ${mapped} 个字段` : '没有可用映射字段')
      if (!mapped) {
        state.agentError = 'Agent 未生成有效字段映射，请进入向导手工检查字段。'
        state.agentRunning = false
        return false
      }

      const plan: SyncAgentPlan = {
        ...parsed,
        projectCode: project.value,
        projectName: String(project.label),
        sourceDatasourceId: sourceDatasource.value,
        sourceDatasourceName: String(sourceDatasource.label),
        targetDatasourceId: targetDatasource.value,
        targetDatasourceName: String(targetDatasource.label),
        sourceDatabase: state.source.database || parsed.sourceDatabase,
        sourceTable: state.source.table || parsed.sourceTable,
        targetDatabase: state.target.database || parsed.targetDatabase,
        targetSchema: state.targetSchemaName,
        targetTable: state.targetTableName,
        sourceColumnCount: state.source.columns.length,
        mappedColumnCount: mapped,
        warnings: [
          ...parsed.warnings,
          targetMatchedTable
            ? '目标表已存在，已按目标字段进行同名自动映射。'
            : '目标表未匹配到已有表，将按源字段生成新目标表结构。'
        ]
      }
      state.agentPlan = plan
      state.agentAutoExecute = parsed.autoExecute || state.agentAutoExecute
      setAgentStage('PLAN', 'SUCCESS', `方案已生成，置信度 ${plan.confidence}%`)
      state.currentStep = 2
      state.activeSolutionModule = 'MAPPING'
      state.viewMode = 'WIZARD'
      state.agentRunning = false
      await nextTick()
      refreshMappingLayout()
      window.$message.success('Agent 已生成同步方案并套用到向导。')
      if (executeAfterApply || state.agentAutoExecute) {
        await handleRunWorkflow()
      }
      return true
    }

    const buildMappingPath = (
      startPoint: { x: number; y: number },
      endPoint: { x: number; y: number }
    ) => {
      if (Math.abs(endPoint.x - startPoint.x) < 120) {
        return `M ${startPoint.x} ${startPoint.y} L ${endPoint.x} ${endPoint.y}`
      }
      const controlOffset = Math.min(
        Math.max(Math.abs(endPoint.x - startPoint.x) * 0.36, 72),
        140
      )
      return `M ${startPoint.x} ${startPoint.y} C ${startPoint.x + controlOffset} ${startPoint.y}, ${endPoint.x - controlOffset} ${endPoint.y}, ${endPoint.x} ${endPoint.y}`
    }

    const handleChooseAllMappings = (checked: boolean) => {
      if (targetTableMode.value === 'EXISTING_TABLE') {
        if (checked) {
          handleAutoMapByName()
          return
        }
        state.fieldRows = state.fieldRows.map((item) => ({
          ...item,
          sourceColumn: '',
          sourceType: '',
          sourceComment: '',
          sourcePrimaryKey: false,
          sourceNullable: true,
          sync: false,
          mappedTargetKey: null,
          mappingKind: undefined
        }))
        void nextTick(refreshMappingLayout)
        return
      }
      state.fieldRows = normalizeTargetMappings(
        state.fieldRows.map((item) => ({
          ...item,
          sync: checked
        }))
      )
      void nextTick(refreshMappingLayout)
    }

    const handleInvertMappings = () => {
      if (targetTableMode.value === 'EXISTING_TABLE') {
        const sourceColumnMap = new Map(
          state.source.columns.map((item) => [item.name.toLowerCase(), item])
        )
        state.fieldRows = state.fieldRows.map((item) => {
          if (item.sync) {
            return {
              ...item,
              sourceColumn: '',
              sourceType: '',
              sourceComment: '',
              sourcePrimaryKey: false,
              sourceNullable: true,
              sync: false,
              mappedTargetKey: null,
              mappingKind: undefined
            }
          }
          const matchedSource = sourceColumnMap.get(item.targetColumn.toLowerCase()) || null
          if (!matchedSource) {
            return item
          }
          return {
            ...item,
            sourceColumn: matchedSource.name,
            sourceType: matchedSource.type,
            sourceComment: matchedSource.comment || '',
            sourcePrimaryKey: !!matchedSource.primaryKey,
            sourceNullable: !!matchedSource.nullable,
            sync: true,
            mappedTargetKey: item.key,
            mappingKind: 'AUTO'
          }
        })
        void nextTick(refreshMappingLayout)
        return
      }
      state.fieldRows = normalizeTargetMappings(
        state.fieldRows.map((item) => ({
          ...item,
          sync: !item.sync
        }))
      )
      void nextTick(refreshMappingLayout)
    }

    const allFieldsChecked = computed(
      () => !!sourceFieldRows.value.length && sourceFieldRows.value.every((item) => item.sync)
    )

    const someFieldsChecked = computed(
      () =>
        sourceFieldRows.value.some((item) => item.sync) &&
        !sourceFieldRows.value.every((item) => item.sync)
    )

    const handleAutoMapByName = () => {
      const selectedKeys = state.fieldRows.filter((item) => item.sync).map((item) => item.key)
      const sourceColumnMap = new Map(
        state.source.columns.map((item) => [item.name.toLowerCase(), item])
      )
      state.fieldRows = normalizeTargetMappings(
        state.fieldRows.map((item) => {
          if (targetTableMode.value === 'EXISTING_TABLE') {
            const matchedSource = sourceColumnMap.get(item.targetColumn.toLowerCase()) || null
            return {
              ...item,
              sourceColumn: matchedSource?.name || '',
              sourceType: matchedSource?.type || '',
              sourceComment: matchedSource?.comment || '',
              sourcePrimaryKey: matchedSource?.primaryKey ?? false,
              sourceNullable: matchedSource?.nullable ?? true,
              sync: !!matchedSource,
              mappedTargetKey: matchedSource ? item.key : null,
              mappingKind: matchedSource ? 'AUTO' : undefined
            }
          }
          if (!item.sync) return item
          const sameNameTarget = state.fieldRows.find(
            (targetRow) =>
              selectedKeys.includes(targetRow.key) &&
              targetRow.targetColumn === item.sourceColumn
          )
          return {
            ...item,
            mappedTargetKey: sameNameTarget?.key || item.mappedTargetKey,
            mappingKind: sameNameTarget?.key || item.mappedTargetKey ? 'AUTO' : undefined
          }
        })
      )
      void nextTick(refreshMappingLayout)
    }

    const handleToggleMappingExceptionOnly = () => {
      state.mappingExceptionOnly = !state.mappingExceptionOnly
      void nextTick(refreshMappingLayout)
    }

    const handleCopyDdl = () => {
      const copied = utils.copy(state.latestCreateTableDdl)
      if (copied) {
        window.$message.success('建表 SQL 已复制。')
      } else {
        window.$message.error('复制失败，请手动复制 SQL。')
      }
    }

    const handleFormatDdl = () => {
      state.latestCreateTableDdl = formatSql(state.latestCreateTableDdl)
      state.latestCreateTableDdlManual = true
      window.$message.success('SQL 已格式化。')
    }

    const handleCopyConfig = () => {
      const copied = utils.copy(effectiveConfigText.value)
      if (copied) {
        window.$message.success('SeaTunnel 配置已复制。')
      } else {
        window.$message.error('复制失败，请手动复制配置内容。')
      }
    }

    const handleOpenPreview = () => {
      if (!state.configManualOverride) {
        state.configEditorText = generatedConfig.value
      }
      state.previewVisible = true
    }

    const handleConfigEditorChange = (value: string) => {
      state.configEditorText = value
      state.configManualOverride = true
    }

    const handleResetConfigEditor = () => {
      state.configManualOverride = false
      state.configEditorText = generatedConfig.value
      window.$message.success('已恢复为自动生成的 SeaTunnel 配置。')
    }

    const openWorkflowInstanceDetail = (instanceId?: number | null) => {
      if (!state.selectedProjectCode || !instanceId) return
      void router.push({
        name: 'workflow-instance-detail',
        params: {
          projectCode: state.selectedProjectCode,
          id: instanceId
        },
        query: {
          code: String(state.latestWorkflowCode || '')
        }
      })
    }

    const loadAssetLogs = async (asset: SyncTaskAsset | null) => {
      if (!asset || asset.logLoading || asset.logLoaded) return
      if (!asset.projectCode || !asset.lastInstanceId) {
        asset.logLoaded = true
        asset.logText = ''
        return
      }

      asset.logLoading = true
      asset.logError = ''
      try {
        const taskResult = await queryTaskListByWorkflowId(
          asset.lastInstanceId,
          asset.projectCode
        )
        const taskRows = normalizeList(taskResult?.taskList || taskResult)
        if (!taskRows.length) {
          asset.logText = '最近实例下没有查询到任务实例日志。'
          asset.logLoaded = true
          return
        }

        const logSections: string[] = []
        for (const task of taskRows) {
          const taskInstanceId = Number(task?.id)
          const taskName = task?.name || task?.taskName || `task_${taskInstanceId || '-'}`
          const taskState = task?.state || '-'
          if (!Number.isFinite(taskInstanceId) || taskInstanceId <= 0) {
            logSections.push(
              `===== ${taskName} / 实例 - / 状态 ${taskState} =====\n未获取到任务实例 ID，无法读取日志。`
            )
            continue
          }

          let skipLineNum = 0
          let taskLogText = ''
          let previousMessage = ''
          for (let attempt = 0; attempt < 20; attempt += 1) {
            const logChunk = await queryLog({
              taskInstanceId,
              skipLineNum,
              limit: 1000
            })
            const message = logChunk?.message || ''
            const lineNum = Number(logChunk?.lineNum || 0)
            if (!message) break
            if (message === previousMessage) break
            taskLogText += message
            previousMessage = message
            skipLineNum += lineNum || message.split(/\r?\n/).length
            if (!lineNum) break
          }

          logSections.push(
            [
              `===== ${taskName} / 实例 ${taskInstanceId} / 状态 ${taskState} =====`,
              taskLogText.trim() || '该任务实例暂无日志内容。'
            ].join('\n')
          )
        }
        asset.logText = logSections.join('\n\n')
        asset.logLoaded = true
      } catch (err) {
        asset.logError = extractErrorMessage(err, '读取任务日志失败，请稍后重试。')
      } finally {
        asset.logLoading = false
      }
    }

    const stopLatestInstancePolling = () => {
      if (latestInstancePollingTimer) {
        window.clearInterval(latestInstancePollingTimer)
        latestInstancePollingTimer = null
      }
    }

    const resolveWorkflowStateMeta = (stateValue: string) => {
      return WORKFLOW_STATE_META[stateValue] || {
        label: stateValue || '未知状态',
        type: 'default' as const
      }
    }

    const refreshLatestInstanceProgress = async (instanceId: number) => {
      if (!state.selectedProjectCode) return null
      const [instanceDetail, taskResult] = await Promise.all([
        queryWorkflowInstanceById(instanceId, state.selectedProjectCode),
        queryTaskListByWorkflowId(instanceId, state.selectedProjectCode)
      ])

      const instanceState =
        instanceDetail?.state ||
        instanceDetail?.processInstance?.state ||
        ''
      const stateMeta = resolveWorkflowStateMeta(instanceState)
      const taskRows = normalizeList(taskResult?.taskList || taskResult).map((task: any, index) => {
        const taskState = task?.state || ''
        const taskStateMeta = resolveWorkflowStateMeta(taskState)
        return {
          key: `${task?.id || task?.taskCode || index}`,
          taskInstanceId: Number(task?.id) || null,
          name: task?.name || task?.taskName || `task_${index + 1}`,
          state: taskState,
          stateLabel: taskStateMeta.label,
          stateType: taskStateMeta.type,
          startTime: formatDateTime(task?.startTime || null),
          endTime: formatDateTime(task?.endTime || null),
          host: task?.host || '-'
        }
      })

      state.latestInstanceState = instanceState
      state.latestInstanceStateLabel = stateMeta.label
      state.latestInstanceStateType = stateMeta.type
      state.latestInstanceStartTime = formatDateTime(instanceDetail?.startTime || null)
      state.latestInstanceEndTime = formatDateTime(instanceDetail?.endTime || null)
      state.latestInstanceTaskRows = taskRows
      state.latestInstanceTaskTotal = taskRows.length
      state.latestInstanceTaskSuccess = taskRows.filter((item) => item.state === 'SUCCESS').length
      state.latestInstanceTaskRunning = taskRows.filter((item) =>
        ['RUNNING_EXECUTION', 'SUBMITTED_SUCCESS', 'SERIAL_WAIT'].includes(item.state)
      ).length
      state.latestInstanceTaskFailed = taskRows.filter((item) =>
        ['FAILURE', 'STOP'].includes(item.state)
      ).length

      if (instanceState === 'SUCCESS') {
        state.latestRunStage = 'SUCCESS'
        state.latestRunMessage = '执行成功'
      } else if (instanceState && TERMINAL_WORKFLOW_STATES.has(instanceState)) {
        state.latestRunStage = 'FAILURE'
        state.latestRunMessage = '执行失败'
      } else {
        state.latestRunStage = 'MONITORING'
        state.latestRunMessage = '运行中'
      }

      if (
        instanceState === 'SUCCESS' &&
        state.latestSyncedRowCountInstanceId !== instanceId
      ) {
        state.latestSyncedRowCountLoading = true
        try {
          let totalReadCount = 0
          let totalWriteCount = 0
          let hasReadCount = false
          let hasWriteCount = false
          for (const taskRow of taskRows) {
            if (taskRow.state !== 'SUCCESS' || !taskRow.taskInstanceId) {
              continue
            }
            // 这里复用 DolphinScheduler 原生日志接口，从 SeaTunnel 执行日志中解析
            // Total Read Count / Total Write Count，确保列表、详情和历史展示同一组真实行数。
            let skipLineNum = 0
            let taskLogText = ''
            for (let attempt = 0; attempt < 12; attempt += 1) {
              const logChunk = await queryLog({
                taskInstanceId: taskRow.taskInstanceId,
                skipLineNum,
                limit: 1000
              })
              const message = logChunk?.message || ''
              const lineNum = Number(logChunk?.lineNum || 0)
              if (!message) {
                break
              }
              taskLogText += message
              skipLineNum += lineNum || message.split(/\r?\n/).length
            }
            const taskCounts = extractReadWriteCountFromLog(taskLogText)
            if (taskCounts.readRows !== null) {
              totalReadCount += taskCounts.readRows
              hasReadCount = true
            }
            if (taskCounts.writeRows !== null) {
              totalWriteCount += taskCounts.writeRows
              hasWriteCount = true
            }
          }
          state.latestReadRowCount = hasReadCount ? totalReadCount : null
          state.latestSyncedRowCount = hasWriteCount ? totalWriteCount : null
          state.latestSyncedRowCountInstanceId = instanceId
        } catch (error) {
          state.latestReadRowCount = null
          state.latestSyncedRowCount = null
          state.latestSyncedRowCountInstanceId = instanceId
        }
        state.latestSyncedRowCountLoading = false
      } else if (instanceState !== 'SUCCESS') {
        state.latestReadRowCount = null
        state.latestSyncedRowCount = null
        state.latestSyncedRowCountLoading = false
        state.latestSyncedRowCountInstanceId = null
      }

      const publishedAssetId = state.latestPublishedAssetId || state.editingAssetId
      if (publishedAssetId && instanceState && TERMINAL_WORKFLOW_STATES.has(instanceState)) {
        const asset = state.syncTaskAssets.find((item) => item.id === publishedAssetId)
        if (asset) {
          const terminalStatus: SyncTaskAssetStatus =
            instanceState === 'SUCCESS' ? 'SUCCESS' : 'FAILED'
          const nowText = format(new Date(), 'yyyy-MM-dd HH:mm')
          asset.status = terminalStatus
          asset.errorMessage = terminalStatus === 'FAILED' ? state.latestRunMessage : ''
          asset.lastInstanceId = state.latestInstanceId || instanceId
          asset.readRows = state.latestReadRowCount
          asset.writeRows = state.latestSyncedRowCount
          asset.updatedAt = nowText
          asset.history = [
            {
              id: String(instanceId),
              status: terminalStatus,
              trigger: '立即执行',
              startTime: state.latestInstanceStartTime || nowText,
              endTime: state.latestInstanceEndTime || nowText,
              duration: '-',
              rows: formatReadWriteRows(state.latestReadRowCount, state.latestSyncedRowCount)
            },
            ...asset.history
          ].slice(0, 8)
        }
      }

      return instanceState
    }

    const startLatestInstancePolling = async (instanceId: number) => {
      stopLatestInstancePolling()
      const firstState = await refreshLatestInstanceProgress(instanceId)
      if (firstState && TERMINAL_WORKFLOW_STATES.has(firstState)) {
        return
      }
      latestInstancePollingTimer = window.setInterval(async () => {
        try {
          const currentState = await refreshLatestInstanceProgress(instanceId)
          if (currentState && TERMINAL_WORKFLOW_STATES.has(currentState)) {
            stopLatestInstancePolling()
          }
        } catch (error) {
          stopLatestInstancePolling()
        }
      }, 3000)
    }

    const queryLatestWorkflowInstanceId = async (
      workflowDefinitionCode: number
    ): Promise<number | null> => {
      if (!state.selectedProjectCode) return null

      // DolphinScheduler 的启动接口并不总是稳定返回实例 ID。
      // 这里在启动后主动按工作流编码查询最新实例，避免“任务已启动但页面无法跳转”的体验断层。
      for (let attempt = 0; attempt < 6; attempt += 1) {
        const result = await queryWorkflowInstanceListPaging(
          {
            pageNo: 1,
            pageSize: 10,
            workflowDefinitionCode,
            searchVal: ''
          },
          state.selectedProjectCode
        )
        const latestRow = normalizeList(result)[0]
        const latestInstanceId = Number(latestRow?.id)
        if (Number.isFinite(latestInstanceId) && latestInstanceId > 0) {
          state.latestInstanceName = latestRow?.name || ''
          return latestInstanceId
        }
        await new Promise((resolve) => window.setTimeout(resolve, 800))
      }

      return null
    }

    const validateProjectSelection = (): number | null => {
      if (!state.selectedProjectCode) {
        window.$message.error('请先选择要落入的 DolphinScheduler 项目。')
        return null
      }
      return state.selectedProjectCode
    }

    const validateSyncDesign = () => {
      const sourceOption = sourceDatasourceOption.value
      const targetOption = targetDatasourceOption.value
      const projectCode = validateProjectSelection()
      if (!projectCode) {
        return null
      }
      if (!sourceOption || !targetOption) {
        window.$message.error('请先选择源数据源和目标数据源。')
        return null
      }
      if (!state.source.table || !state.targetTableName.trim()) {
        window.$message.error('请先选择源表并确认目标表名称。')
        return null
      }
      if (
        !state.fieldRows.some(
          (item) =>
            item.sync &&
            item.sourceColumn.trim() &&
            item.targetColumn.trim() &&
            item.targetType.trim() &&
            item.mappedTargetKey
        )
      ) {
        window.$message.error('请至少配置一个有效的字段映射。')
        return null
      }
      if (state.executionMode === 'SCHEDULE' && !state.latestScheduleId) {
        window.$message.error('请先配置周期调度。')
        return null
      }
      return {
        projectCode,
        sourceOption,
        targetOption
      }
    }

    // 第一步只校验连接设计是否完整，方便用户以“步骤式”方式推进配置。
    const validateStepOne = (showMessage = true) => {
      if (!state.selectedProjectCode) {
        if (showMessage) {
          window.$message.error('请先选择要落入的 DolphinScheduler 项目。')
        }
        return false
      }
      if (!state.source.datasourceId || !state.target.datasourceId) {
        if (state.editingAssetId && state.source.table && state.targetTableName.trim()) {
          return true
        }
        if (showMessage) {
          window.$message.error('请先选择源数据源和目标数据源。')
        }
        return false
      }
      if (!state.source.database || !state.source.table) {
        if (showMessage) {
          window.$message.error('请先选择完整的源库和源表。')
        }
        return false
      }
      if (!state.target.database) {
        if (showMessage) {
          window.$message.error('请先选择目标库。')
        }
        return false
      }
      if (!state.targetTableName.trim()) {
        if (showMessage) {
          window.$message.error('请先确认目标表名称。')
        }
        return false
      }
      return true
    }

    // 第二步只关心字段设计本身，避免和调度、执行耦合在一起。
    const validateStepTwo = (showMessage = true) => {
      if (!validateStepOne(showMessage)) {
        return false
      }
      const selectedRows = state.fieldRows.filter((item) => item.sync)
      if (!selectedRows.length) {
        if (showMessage) {
          window.$message.error('请至少勾选一个需要同步的源字段。')
        }
        return false
      }
      const invalidRow = selectedRows.find(
        (item) =>
          !item.sourceColumn.trim() ||
          !item.targetColumn.trim() ||
          !item.targetType.trim() ||
          !item.mappedTargetKey
      )
      if (invalidRow) {
        if (showMessage) {
          window.$message.error(`字段 ${invalidRow.targetColumn || invalidRow.sourceColumn} 的映射或目标类型未配置完整。`)
        }
        return false
      }
      return true
    }

    const buildTargetTableRequest = (showMessage = true) => {
      if (!validateStepTwo(showMessage)) {
        return null
      }
      if (!state.target.datasourceId || !state.target.database) {
        if (showMessage) {
          window.$message.error('请先确认目标数据源和目标库。')
        }
        return null
      }

      return {
        datasourceId: state.target.datasourceId,
        database: state.target.database,
        schema:
          state.targetSchemaName.trim() ||
          getDefaultSchemaName(targetDatasourceOption.value?.type),
        tableName: state.targetTableName.trim(),
        columns: targetFieldRows.value.filter((targetRow) => targetRow.sync).map((targetRow) => {
          const mappedSourceRow =
            mappedSourceByTargetKey.value.get(targetRow.key) || targetRow
          return {
            sourceColumn: mappedSourceRow.sourceColumn,
            sourceType: mappedSourceRow.sourceType,
            sourceComment: mappedSourceRow.sourceComment,
            targetColumn: targetRow.targetColumn,
            targetType: targetRow.targetType,
            targetComment: targetRow.targetComment,
            nullable: targetRow.targetNullable,
            primaryKey: targetRow.targetPrimaryKey
          }
        })
      }
    }

    const resetWizardState = () => {
      state.currentStep = 1
      state.selectedProjectCode = null
      state.taskName = ''
      state.source.datasourceId = null
      state.target.datasourceId = null
      resetEndpoint(state.source)
      resetEndpoint(state.target)
      state.targetTableName = ''
      state.targetSchemaName = 'public'
      state.fieldRows = []
      state.sourceFilters = [createSourceFilterRule()]
      state.activeSolutionModule = 'MAPPING'
      state.sinkCustomSql = ''
      state.agentSampleLimit = null
      state.configEditorText = ''
      state.configManualOverride = false
      state.latestWorkflowCode = null
      state.latestWorkflowName = ''
      state.latestWorkflowVersion = 1
      state.latestWorkflowReleaseState = '-'
      state.latestRunStage = 'IDLE'
      state.latestRunMessage = '尚未发起同步运行。'
      state.latestInstanceId = null
      state.latestInstanceName = ''
      state.latestInstanceState = ''
      state.latestInstanceStateLabel = '等待执行'
      state.latestInstanceStateType = 'default'
      state.latestInstanceStartTime = ''
      state.latestInstanceEndTime = ''
      state.latestInstanceTaskRows = []
      state.latestInstanceTaskTotal = 0
      state.latestInstanceTaskSuccess = 0
      state.latestInstanceTaskRunning = 0
      state.latestInstanceTaskFailed = 0
      state.latestReadRowCount = null
      state.latestSyncedRowCount = null
      state.latestSyncedRowCountLoading = false
      state.latestSyncedRowCountInstanceId = null
      state.latestScheduleId = null
      state.latestCreateTableDdl = ''
      state.latestCreateTableDdlManual = false
      state.executionMode = 'IMMEDIATE'
      state.latestScheduleSummary = '未配置'
      state.editingAssetId = ''
      state.latestPublishedAssetId = ''
      state.agentSampleLimit = null
    }

    const openCreateWizard = () => {
      resetWizardState()
      state.viewMode = 'WIZARD'
    }

    const backToAssetList = () => {
      state.viewMode = 'LIST'
      state.assetDetailVisible = false
      state.selectedAsset = null
      state.editingAssetId = ''
    }

    const returnToAssetListAfterPublish = () => {
      // 发布或执行成功后统一回到同步任务列表；同时清空筛选，避免新任务被旧筛选条件隐藏。
      state.assetKeyword = ''
      state.assetProjectFilter = ''
      state.assetStatusFilter = ''
      state.assetScheduleFilter = ''
      state.assetTypeFilter = ''
      state.viewMode = 'LIST'
      state.assetDetailVisible = false
      state.selectedAsset = null
    }

    const openAssetDetail = (asset: SyncTaskAsset, tab: SyncTaskDetailTab = 'OVERVIEW') => {
      state.selectedAsset = asset
      state.assetDetailTab = tab
      state.assetDetailVisible = true
      if (tab === 'LOGS') {
        void loadAssetLogs(asset)
      }
    }

    const openAssetLogFullscreen = () => {
      if (!state.selectedAsset) return
      state.assetLogFullscreenVisible = true
      void loadAssetLogs(state.selectedAsset)
    }

    const hydrateWizardFromAsset = (asset: SyncTaskAsset) => {
      state.hydratingAsset = true
      resetWizardState()
      state.editingAssetId = asset.id
      state.selectedProjectCode =
        asset.projectCode ||
        state.projectOptions.find((item) => item.label === asset.projectName)?.value ||
        null
      state.taskName = asset.name
      const sourceOption = state.datasourceOptions.find(
        (item) =>
          item.type === asset.sourceType &&
          (asset.sourceName.includes(String(item.label).split(' ')[0]) ||
            String(item.label).includes(asset.sourceName.split(' ')[0]))
      )
      const targetOption = state.datasourceOptions.find(
        (item) =>
          item.type === asset.targetType &&
          (asset.targetName.includes(String(item.label).split(' ')[0]) ||
            String(item.label).includes(asset.targetName.split(' ')[0]))
      )
      state.source.datasourceId = sourceOption?.value || null
      state.target.datasourceId = targetOption?.value || null
      state.source.columns = cloneColumns(
        asset.sourceColumns.length
          ? asset.sourceColumns
          : asset.fieldRows.map((item) => ({
              name: item.sourceColumn,
              type: item.sourceType,
              key: item.sourceColumn,
              nullable: item.sourceNullable,
              primaryKey: item.sourcePrimaryKey,
              comment: item.sourceComment
            }))
      )
      state.target.columns = cloneColumns(
        asset.targetColumns.length
          ? asset.targetColumns
          : asset.fieldRows.map((item) => ({
              name: item.targetColumn,
              type: item.targetType,
              key: item.key,
              nullable: item.targetNullable,
              primaryKey: item.targetPrimaryKey,
              comment: item.targetComment
            }))
      )
      const sourceParts = asset.sourcePath.split('.').filter(Boolean)
      const sourceDatabase =
        sourceParts.length >= 2
          ? sourceParts[sourceParts.length - 2]
          : sourceOption
            ? state.datasourceDetails[sourceOption.value]?.database || ''
            : ''
      const sourceTable = sourceParts[sourceParts.length - 1] || asset.sourcePath
      const targetParts = asset.targetPath.split('.')
      state.source.database = sourceDatabase || null
      state.source.table = sourceTable || asset.sourcePath || null
      state.source.tables = state.source.table ? [state.source.table] : []
      state.source.databases = state.source.database ? [state.source.database] : []
      state.target.database = targetParts[0] || null
      state.targetSchemaName =
        targetParts.length >= 3 ? targetParts[targetParts.length - 2] : 'public'
      state.targetTableName = targetParts[targetParts.length - 1] || asset.targetPath
      state.target.table = state.targetTableName
      state.target.tables = state.targetTableName ? [state.targetTableName] : []
      state.target.databases = state.target.database ? [state.target.database] : []
      state.fieldRows = cloneFieldRows(asset.fieldRows)
      state.sourceFilters = cloneSourceFilters(asset.sourceFilters.length ? asset.sourceFilters : [createSourceFilterRule()])
      state.sinkCustomSql = asset.sinkCustomSql
      state.configEditorText = asset.configText || generatedConfig.value
      state.configManualOverride = !!asset.configText
      state.latestWorkflowCode = asset.workflowCode
      state.latestWorkflowName = asset.workflowName || asset.name
      state.latestWorkflowVersion = asset.workflowVersion || 1
      state.latestWorkflowReleaseState = asset.status === 'OFFLINE' ? 'OFFLINE' : 'ONLINE'
      state.latestInstanceId = asset.lastInstanceId
      state.latestReadRowCount = asset.readRows
      state.latestSyncedRowCount = asset.writeRows
      state.latestScheduleSummary =
        asset.scheduleStatus === 'ON' ? 'ONLINE / 已配置' : '未配置'
      state.currentStep = 1
      state.activeSolutionModule = 'MAPPING'
      state.viewMode = 'WIZARD'
      state.assetDetailVisible = false
      void nextTick(() => {
        state.hydratingAsset = false
        refreshMappingLayout()
        if (state.source.datasourceId && state.source.database && state.source.table) {
          void loadDatabases(state.source).then(async () => {
            await loadTables(state.source)
            await loadColumns(state.source)
          })
        }
        if (state.target.datasourceId && state.target.database && state.target.table) {
          void loadDatabases(state.target).then(async () => {
            await loadTables(state.target)
            await loadColumns(state.target)
          })
        }
      })
    }

    const upsertCurrentAsset = (status: SyncTaskAssetStatus) => {
      const nowText = format(new Date(), 'yyyy-MM-dd HH:mm')
      const sourceOption = sourceDatasourceOption.value
      const targetOption = targetDatasourceOption.value
      const projectName =
        selectedProjectOption.value?.label ||
        state.syncTaskAssets.find((item) => item.id === state.editingAssetId)?.projectName ||
        '-'
      const asset: SyncTaskAsset = {
        id: state.editingAssetId || `asset-${Date.now()}`,
        name: state.latestWorkflowName || state.taskName || buildDraftWorkflowName(),
        projectCode: state.selectedProjectCode,
        projectName,
        status,
        scheduleStatus: state.latestScheduleId || state.executionMode === 'SCHEDULE' ? 'ON' : 'OFF',
        sourceType: sourceOption?.type || 'MYSQL',
        targetType: targetOption?.type || 'POSTGRESQL',
        sourceName: sourceOption?.label || '-',
        sourcePath: `${state.source.database || '-'}.${state.source.table || '-'}`,
        targetName: targetOption?.label || '-',
        targetPath: `${state.target.database || '-'}.${state.targetSchemaName || getDefaultSchemaName(targetOption?.type)}.${state.targetTableName || '-'}`,
        workflowCode: state.latestWorkflowCode,
        workflowName: state.latestWorkflowName || state.taskName,
        workflowVersion: state.latestWorkflowVersion,
        lastRunTime: status === 'DRAFT' ? '-' : nowText,
        lastInstanceId: state.latestInstanceId,
        readRows: state.latestReadRowCount,
        writeRows: state.latestSyncedRowCount,
        duration: '-',
        updatedAt: nowText,
        owner: 'admin',
        errorMessage: status === 'FAILED' ? state.latestRunMessage : '',
        sourceFilters: cloneSourceFilters(state.sourceFilters),
        sinkCustomSql: state.sinkCustomSql,
        fieldRows: cloneFieldRows(state.fieldRows),
        sourceColumns: cloneColumns(state.source.columns),
        targetColumns: cloneColumns(state.target.columns),
        configText: effectiveConfigText.value,
        history: [
          {
            id: state.latestInstanceId ? String(state.latestInstanceId) : `draft-${Date.now()}`,
            status,
            trigger: status === 'DRAFT' ? '保存草稿' : '页面发布',
            startTime: nowText,
            endTime: status === 'RUNNING' ? '-' : nowText,
            duration: '-',
            rows: formatReadWriteRows(state.latestReadRowCount, state.latestSyncedRowCount)
          }
        ],
        changes: [
          {
            time: nowText,
            user: 'admin',
            action: state.editingAssetId ? '编辑同步任务配置' : '创建同步任务'
          }
        ],
        source: 'LOCAL'
      }
      const existedIndex = state.syncTaskAssets.findIndex((item) => item.id === asset.id)
      if (existedIndex >= 0) {
        const previous = state.syncTaskAssets[existedIndex]
        state.syncTaskAssets[existedIndex] = {
          ...previous,
          ...asset,
          history: [...asset.history, ...previous.history].slice(0, 8),
          changes: [...asset.changes, ...previous.changes].slice(0, 8)
        }
      } else {
        state.syncTaskAssets.unshift(asset)
      }
      state.editingAssetId = asset.id
      return asset
    }

    const registerCurrentGovernanceLineage = async (
      status: SyncTaskAssetStatus,
      asset?: SyncTaskAsset
    ) => {
      const sourceOption = sourceDatasourceOption.value
      const targetOption = targetDatasourceOption.value
      if (
        !sourceOption ||
        !targetOption ||
        !state.source.datasourceId ||
        !state.target.datasourceId ||
        !state.source.database ||
        !state.source.table ||
        !state.target.database ||
        !state.targetTableName.trim()
      ) {
        return
      }
      const fieldMappings = buildOrderedMappingRows(state.fieldRows)
        .filter((item) => item.sourceColumn && item.targetColumn)
        .map((item) => ({
          sourceField: item.sourceColumn,
          targetField: item.targetColumn
        }))
      try {
        await registerGovernanceSyncTaskLineage({
          sourceDatasourceId: state.source.datasourceId,
          sourceDatasourceName: String(sourceOption.label || ''),
          sourceDatabase: state.source.database,
          sourceSchema: '',
          sourceTable: state.source.table,
          targetDatasourceId: state.target.datasourceId,
          targetDatasourceName: String(targetOption.label || ''),
          targetDatabase: state.target.database,
          targetSchema: state.targetSchemaName || getDefaultSchemaName(targetOption.type),
          targetTable: state.targetTableName.trim(),
          syncTaskName: asset?.name || state.latestWorkflowName || state.taskName || buildDraftWorkflowName(),
          lastRunStatus: status,
          lastRunTime: asset?.lastRunTime && asset.lastRunTime !== '-' ? asset.lastRunTime : formatDateTime(Date.now()),
          fieldMappings
        })
      } catch (err) {
        console.warn('Register data governance lineage failed.', err)
      }
    }

    const handlePrevStep = () => {
      state.currentStep = Math.max(1, state.currentStep - 1)
    }

    const handleNextStep = () => {
      if (state.currentStep === 1 && !validateStepOne()) {
        return
      }
      if (state.currentStep === 2 && !validateStepTwo()) {
        return
      }
      state.currentStep = Math.min(4, state.currentStep + 1)
      if (state.currentStep === 3) {
        void nextTick(() => {
          void handlePreviewTargetTable(false)
        })
      }
    }

    const handleJumpStep = (step: number) => {
      state.currentStep = step
      if (step === 3) {
        void nextTick(() => {
          void handlePreviewTargetTable(false)
        })
      }
    }

    const loadScheduleMeta = async (workflowDefinitionCode: number) => {
      if (!state.selectedProjectCode) return null
      const scheduleList = await queryScheduleListPaging(
        {
          pageNo: 1,
          pageSize: 20,
          searchVal: '',
          workflowDefinitionCode
        },
        state.selectedProjectCode
      )
      const scheduleRow = normalizeList(scheduleList)[0] || null
      if (!scheduleRow) {
        state.latestScheduleId = null
        state.latestScheduleSummary = '未配置'
        return null
      }

      state.latestScheduleId = scheduleRow.id || null
      state.latestScheduleSummary = `${scheduleRow.releaseState || 'OFFLINE'} / ${
        scheduleRow.crontab || '未生成'
      }`
      return scheduleRow
    }

    const buildWorkflowPayload = async (draftOnly = false) => {
      if (draftOnly) {
        const projectCode = validateProjectSelection()
        if (!projectCode) return null
        const [taskCode] = await genTaskCodeList(1, projectCode)
        return {
          projectCode,
          workflowName: state.latestWorkflowName || buildDraftWorkflowName(),
          taskDefinition: {
            code: taskCode,
            delayTime: '0',
            description: '同步任务调度草稿，占位用，正式保存后会被真实同步任务覆盖',
            environmentCode: -1,
            failRetryInterval: '1',
            failRetryTimes: '0',
            flag: 'YES',
            name: 'sync_task_draft',
            taskGroupId: null,
            taskGroupPriority: null,
            taskParams: {
              localParams: [],
              rawScript: DRAFT_SEATUNNEL_CONFIG,
              resourceList: [],
              startupScript: 'seatunnel.sh',
              useCustom: true,
              deployMode: 'local',
              others: ''
            },
            taskPriority: 'MEDIUM',
            taskType: 'SEATUNNEL',
            timeout: 0,
            timeoutFlag: 'CLOSE',
            timeoutNotifyStrategy: '',
            workerGroup: 'default',
            cpuQuota: -1,
            memoryMax: -1,
            taskExecuteType: 'BATCH'
          },
          taskRelation: {
            name: '',
            preTaskCode: 0,
            preTaskVersion: 0,
            postTaskCode: taskCode,
            postTaskVersion: 1,
            conditionType: 'NONE',
            conditionParams: {}
          },
          location: {
            taskCode,
            x: 320,
            y: 160
          },
          description:
            '同步任务页面自动生成的调度草稿，等待补全源端、目标端和字段映射'
        }
      }

      const validated = validateSyncDesign()
      if (!validated) return null
      const { sourceOption, targetOption } = validated

      const workflowName =
        state.latestWorkflowCode && state.latestWorkflowName
          ? state.latestWorkflowName
          : state.taskName.trim() ||
        buildWorkflowName(
          sourceOption.label,
          state.source.table || '',
          targetOption.label,
          state.targetTableName.trim()
        )
      const taskName = `${state.source.table}_to_${state.targetTableName.trim()}`
        .replaceAll(/[^a-zA-Z0-9_\u4e00-\u9fa5]+/g, '_')
        .slice(0, 120)
      const [taskCode] = await genTaskCodeList(1, state.selectedProjectCode as number)
      const taskDefinition = {
        code: taskCode,
        delayTime: '0',
        description: `由同步任务页面自动生成，来源 ${sourceOption.label} -> ${targetOption.label}`,
        environmentCode: -1,
        failRetryInterval: '1',
        failRetryTimes: '0',
        flag: 'YES',
        name: taskName,
        taskGroupId: null,
        taskGroupPriority: null,
        taskParams: {
          localParams: [],
          rawScript: effectiveConfigText.value,
          resourceList: [],
          startupScript: 'seatunnel.sh',
          useCustom: true,
          deployMode: 'local',
          others: ''
        },
        taskPriority: 'MEDIUM',
        taskType: 'SEATUNNEL',
        timeout: 0,
        timeoutFlag: 'CLOSE',
        timeoutNotifyStrategy: '',
        workerGroup: 'default',
        cpuQuota: -1,
        memoryMax: -1,
        taskExecuteType: 'BATCH'
      }
      const taskRelation = {
        name: '',
        preTaskCode: 0,
        preTaskVersion: 0,
        postTaskCode: taskCode,
        postTaskVersion: 1,
        conditionType: 'NONE',
        conditionParams: {}
      }
      const location = {
        taskCode,
        x: 320,
        y: 160
      }

      return {
        projectCode: validated.projectCode,
        workflowName,
        taskDefinition,
        taskRelation,
        location,
        description: `同步任务页面自动生成: ${state.source.table} -> ${state.targetTableName.trim()}`
      }
    }

    const handleCreateTargetTable = async () => {
      const request = buildTargetTableRequest()
      if (!request) return
      if (!state.latestCreateTableDdl.trim()) {
        window.$message.error('建表 SQL 还没有生成，请稍后重试或点击重新生成。')
        return
      }
      state.creatingTable = true
      try {
        const ddlResponse = await createDatasourceTargetTable({
          ...request,
          ddl: state.latestCreateTableDdl
        })
        const { ddl, targetTableExists } = stripCreateTableResponsePrefix(ddlResponse)
        state.latestCreateTableDdl = ddl
        if (targetTableExists) {
          window.$message.success('目标表已存在，已跳过重复建表，可直接继续保存或执行同步任务。')
        } else {
          window.$message.success('目标端建表成功。')
        }
      } catch (err) {
        window.$message.error(
          extractErrorMessage(err, '目标端建表失败，请检查目标库连接和建表语句。')
        )
        state.creatingTable = false
        return
      }
      state.creatingTable = false
    }

    const handlePreviewTargetTable = async (force = true) => {
      const request = buildTargetTableRequest()
      if (!request) return
      if (state.latestCreateTableDdlManual && !force) {
        return
      }
      if (
        state.latestCreateTableDdlManual &&
        force &&
        !window.confirm('当前建表 SQL 已手工修改，重新生成会覆盖修改内容，是否继续？')
      ) {
        return
      }
      state.previewingTableDdl = true
      try {
        const ddl = await previewDatasourceTargetTable(request)
        state.latestCreateTableDdl = ddl
        state.latestCreateTableDdlManual = false
        if (force) {
          window.$message.success('已重新生成目标端建表语句，你可以继续审阅或编辑。')
        }
      } catch (err) {
        window.$message.error('生成建表 SQL 失败，请检查字段设计和目标库配置。')
        state.previewingTableDdl = false
        return
      }
      state.previewingTableDdl = false
    }

    const handleSaveWorkflow = async () => {
      const payload = await buildWorkflowPayload()
      if (!payload) return false
      state.savingWorkflow = true
      try {
        let existedMeta = await findWorkflowDefinitionMetaByName(
          payload.projectCode,
          payload.workflowName
        )

        if (existedMeta?.code) {
          if (existedMeta.releaseState === 'ONLINE') {
            // DolphinScheduler 原生限制：上线状态的工作流定义不允许直接修改。
            // 这里先自动下线，再执行更新，这样同步任务页面可以持续迭代同一条工作流。
            await release(
              {
                name: payload.workflowName,
                releaseState: 'OFFLINE'
              },
              payload.projectCode,
              existedMeta.code
            )
          }
          await updateWorkflowDefinition(
            {
              name: payload.workflowName,
              executionType: 'PARALLEL',
              description: `同步任务页面自动生成: ${state.source.table} -> ${state.targetTableName.trim()}`,
              globalParams: '[]',
              timeout: 0,
              taskDefinitionJson: JSON.stringify([payload.taskDefinition]),
              taskRelationJson: JSON.stringify([payload.taskRelation]),
              locations: JSON.stringify([payload.location]),
              releaseState: 'OFFLINE'
            },
            existedMeta.code,
            payload.projectCode
          )
          state.latestWorkflowCode = existedMeta.code
          const latest = await queryWorkflowDefinitionByCode(
            existedMeta.code,
            payload.projectCode
          )
          const latestMeta = extractWorkflowDefinitionMeta(latest)
          state.latestWorkflowVersion = latestMeta?.version || existedMeta.version || 1
          state.latestWorkflowReleaseState = latestMeta?.releaseState || 'OFFLINE'
        } else {
          try {
            await verifyName(
              {
                name: payload.workflowName
              },
              payload.projectCode
            )
          } catch (err) {
            if (!isWorkflowNameExistsError(err)) {
              throw err
            }
            existedMeta = await findWorkflowDefinitionMetaByName(
              payload.projectCode,
              payload.workflowName
            )
          }
          if (existedMeta?.code) {
            await updateWorkflowDefinition(
              {
                name: payload.workflowName,
                executionType: 'PARALLEL',
                description: `同步任务页面自动生成: ${state.source.table} -> ${state.targetTableName.trim()}`,
                globalParams: '[]',
                timeout: 0,
                taskDefinitionJson: JSON.stringify([payload.taskDefinition]),
                taskRelationJson: JSON.stringify([payload.taskRelation]),
                locations: JSON.stringify([payload.location]),
                releaseState: 'OFFLINE'
              },
              existedMeta.code,
              payload.projectCode
            )
          } else {
            const created = await createWorkflowDefinition(
              {
                name: payload.workflowName,
                executionType: 'PARALLEL',
                description: payload.description,
                globalParams: '[]',
                timeout: 0,
                taskDefinitionJson: JSON.stringify([payload.taskDefinition]),
                taskRelationJson: JSON.stringify([payload.taskRelation]),
                locations: JSON.stringify([payload.location])
              },
              payload.projectCode
            )
            existedMeta = extractWorkflowDefinitionMeta(created)
          }
          const latestMeta =
            existedMeta?.code
              ? extractWorkflowDefinitionMeta(
                await queryWorkflowDefinitionByCode(
                  existedMeta.code,
                  payload.projectCode
                )
              ) || existedMeta
              : null
          state.latestWorkflowCode = latestMeta?.code || null
          state.latestWorkflowVersion = latestMeta?.version || 1
          state.latestWorkflowReleaseState = latestMeta?.releaseState || '-'
        }
        state.latestWorkflowName = payload.workflowName
        if (!state.latestWorkflowCode) {
          throw new Error('工作流已提交保存，但没有拿到 DolphinScheduler 返回的工作流编码。')
        }
        if (state.latestWorkflowCode) {
          await loadScheduleMeta(state.latestWorkflowCode)
        }
        const asset = upsertCurrentAsset('DRAFT')
        await registerCurrentGovernanceLineage('DRAFT', asset)
        window.$message.success('同步任务已保存为 DolphinScheduler 工作流定义。')
      } catch (err) {
        window.$message.error(
          extractErrorMessage(err, '保存同步任务失败，请检查当前项目和任务配置。')
        )
        state.savingWorkflow = false
        return false
      }
      state.savingWorkflow = false
      return true
    }

    const handleEnsureScheduleDraft = async () => {
      const payload = await buildWorkflowPayload(true)
      if (!payload) return false
      if (state.latestWorkflowCode) {
        return true
      }

      state.savingWorkflow = true
      try {
        await verifyName(
          {
            name: payload.workflowName
          },
          payload.projectCode
        )
        await createWorkflowDefinition(
          {
            name: payload.workflowName,
            executionType: 'PARALLEL',
            description: payload.description,
            globalParams: '[]',
            timeout: 0,
            taskDefinitionJson: JSON.stringify([payload.taskDefinition]),
            taskRelationJson: JSON.stringify([payload.taskRelation]),
            locations: JSON.stringify([payload.location])
          },
          payload.projectCode
        )
        const latest = await queryWorkflowDefinitionByName(
          {
            name: payload.workflowName
          },
          payload.projectCode
        )
        const latestMeta = extractWorkflowDefinitionMeta(latest)
        state.latestWorkflowCode = latestMeta?.code || null
        state.latestWorkflowVersion = latestMeta?.version || 1
        state.latestWorkflowReleaseState = latestMeta?.releaseState || '-'
        state.latestWorkflowName = latestMeta?.name || payload.workflowName
        if (state.latestWorkflowCode) {
          // Dolphin 原生调度创建要求工作流定义已上线。
          // 同步任务页允许先配置调度，因此这里把占位草稿上线，后续保存正式任务时会复用同一工作流并自动下线更新。
          await release(
            {
              name: state.latestWorkflowName,
              releaseState: 'ONLINE'
            },
            payload.projectCode,
            state.latestWorkflowCode
          )
          const released = await queryWorkflowDefinitionByCode(
            state.latestWorkflowCode,
            payload.projectCode
          )
          state.latestWorkflowReleaseState =
            extractWorkflowReleaseState(released) || 'ONLINE'
        }
        state.savingWorkflow = false
        return true
      } catch (err) {
        window.$message.error('创建调度草稿失败，请检查当前项目权限。')
        state.savingWorkflow = false
        return false
      }
    }

    const handleOpenScheduleModal = async () => {
      const projectCode = validateProjectSelection()
      if (!projectCode) return
      const saved = await handleEnsureScheduleDraft()
      if (!saved || !state.latestWorkflowCode) return
      const releasedWorkflow = await queryWorkflowDefinitionByCode(
        state.latestWorkflowCode,
        projectCode
      )
      state.latestWorkflowReleaseState =
        extractWorkflowReleaseState(releasedWorkflow) ||
        state.latestWorkflowReleaseState

      const scheduleRow = await loadScheduleMeta(state.latestWorkflowCode)
      state.scheduleModalType = scheduleRow?.id ? 'update' : 'create'
      state.scheduleModalState = scheduleRow?.releaseState || 'OFFLINE'
      state.scheduleModalRow = scheduleRow?.id
        ? {
            ...scheduleRow
          }
        : {
            code: state.latestWorkflowCode,
            warningGroupId: 0,
            workerGroup: 'default',
            tenantCode: 'default',
            environmentCode: null
          }
      state.scheduleModalVisible = true
    }

    const handleRunWorkflow = async () => {
      const validated = validateSyncDesign()
      if (!validated) {
        return
      }
      state.runningWorkflow = true
      state.latestRunStage = 'PREPARING'
      state.latestRunMessage = '保存中'
      state.latestReadRowCount = null
      state.latestSyncedRowCount = null
      state.latestSyncedRowCountLoading = false
      state.latestSyncedRowCountInstanceId = null
      try {
        const saved = await handleSaveWorkflow()
        if (!saved) {
          state.runningWorkflow = false
          return
        }
        if (!state.latestWorkflowCode) {
          window.$message.error('运行失败，未获取到工作流编码。')
          state.latestRunStage = 'FAILURE'
          state.latestRunMessage = '启动失败'
          state.runningWorkflow = false
          return
        }
        state.latestRunStage = 'STARTING'
        state.latestRunMessage = '提交中'
        await release(
          {
            name: state.latestWorkflowName,
            releaseState: 'ONLINE'
          },
          state.selectedProjectCode as number,
          state.latestWorkflowCode
        )
        const releasedWorkflow = await queryWorkflowDefinitionByCode(
          state.latestWorkflowCode,
          state.selectedProjectCode as number
        )
        state.latestWorkflowReleaseState =
          extractWorkflowReleaseState(releasedWorkflow) ||
          state.latestWorkflowReleaseState
        if (state.latestWorkflowReleaseState !== 'ONLINE') {
          window.$message.error('工作流未成功上线，请稍后重试。')
          state.latestRunStage = 'FAILURE'
          state.latestRunMessage = '上线失败'
          state.runningWorkflow = false
          return
        }
        if (state.executionMode === 'SCHEDULE') {
          const scheduleRow = await loadScheduleMeta(state.latestWorkflowCode)
          if (!scheduleRow?.id) {
            window.$message.error('当前还没有可启用的周期调度，请先完成调度配置。')
            state.latestRunStage = 'FAILURE'
            state.latestRunMessage = '调度未配置'
            state.runningWorkflow = false
            return
          }
          await online(state.selectedProjectCode as number, scheduleRow.id)
          state.latestScheduleId = scheduleRow.id
          state.latestScheduleSummary = `ONLINE / ${scheduleRow.crontab || '已配置'}`
          state.latestRunStage = 'SUCCESS'
          state.latestRunMessage = '调度已启用'
          const asset = upsertCurrentAsset('SUCCESS')
          state.latestPublishedAssetId = asset.id
          await registerCurrentGovernanceLineage('SUCCESS', asset)
          window.$message.success('同步任务已保存并上线为周期调度。')
          state.runningWorkflow = false
          returnToAssetListAfterPublish()
          return
        }
        const businessTime = formatDateTime(Date.now())
        const result = await startWorkflowInstance(
          {
            workflowDefinitionCode: state.latestWorkflowCode,
            failureStrategy: 'CONTINUE',
            workflowInstancePriority: 'MEDIUM',
            scheduleTime: JSON.stringify({
              complementScheduleDateList: businessTime
            }),
            warningGroupId: 0,
            warningType: 'NONE',
            execType: 'START_PROCESS',
            runMode: 'RUN_MODE_SERIAL',
            workerGroup: 'default',
            environmentCode: -1,
            timeout: 0,
            startParams: '',
            version: state.latestWorkflowVersion,
            dryRun: 0
          },
          state.selectedProjectCode as number
        )
        const latestInstanceId = Array.isArray(result)
          ? Number(result[0])
          : Number(
              result?.id ||
                result?.workflowInstanceId ||
                result?.processInstanceId ||
                0
            )
        state.latestInstanceId = Number.isFinite(latestInstanceId) &&
          latestInstanceId > 0
          ? latestInstanceId
          : null
        state.latestInstanceName = state.latestInstanceId
          ? `${state.latestWorkflowName || 'sync_workflow'}-${state.latestInstanceId}`
          : ''
        if (!state.latestInstanceId && state.latestWorkflowCode) {
          state.latestInstanceId = await queryLatestWorkflowInstanceId(
            state.latestWorkflowCode
          )
        }
        if (state.latestInstanceId) {
          state.latestRunStage = 'MONITORING'
          state.latestRunMessage = '运行中'
          const asset = upsertCurrentAsset('RUNNING')
          state.latestPublishedAssetId = asset.id
          await registerCurrentGovernanceLineage('RUNNING', asset)
          await startLatestInstancePolling(state.latestInstanceId)
          window.$message.success('同步实例已启动，已返回同步任务列表。')
        } else {
          state.latestRunStage = 'FAILURE'
          state.latestRunMessage = '实例未返回'
          const asset = upsertCurrentAsset('FAILED')
          state.latestPublishedAssetId = asset.id
          await registerCurrentGovernanceLineage('FAILED', asset)
          window.$message.success('同步实例已启动，已返回同步任务列表。')
        }
        returnToAssetListAfterPublish()
      } catch (err) {
        state.latestRunStage = 'FAILURE'
        state.latestRunMessage = '执行失败'
        upsertCurrentAsset('FAILED')
        window.$message.error(extractErrorMessage(err, '执行失败，请检查工作流发布、调度或任务日志。'))
        state.runningWorkflow = false
        return
      }
      state.runningWorkflow = false
    }

    const sourceFieldColumns = computed<DataTableColumns<FieldDesignRow>>(() => [
      {
        title: '同步',
        key: 'sync',
        width: 72,
        titleAlign: 'center',
        renderHeader: () => (
          <NCheckbox
            checked={allFieldsChecked.value}
            indeterminate={someFieldsChecked.value}
            onUpdateChecked={(checked) => handleChooseAllMappings(checked)}
          />
        ),
        render: (row) => {
          return (
            <NCheckbox
              v-model:checked={row.sync}
              onUpdateChecked={(checked) =>
                handleToggleField(row.key, checked)
              }
            />
          )
        }
      },
      {
        title: '源字段',
        key: 'sourceColumn',
        minWidth: 260,
        render: (row) => (
          <div class={[styles.columnCell, styles.sourceColumnCell]}>
            <div
              class={[styles.mappingFieldHead, styles.sourceFieldHead]}
            >
              <div class={styles.sourceFieldInline}>
                <span class={styles.columnName}>{row.sourceColumn || '-'}</span>
                {row.sourcePrimaryKey ? (
                  <NTag size='small' bordered={false} type='warning'>
                    主键
                  </NTag>
                ) : null}
                <NTag size='small' bordered={false} type={row.sourceNullable ? 'success' : 'error'}>
                  {row.sourceNullable ? '可空' : '非空'}
                </NTag>
              </div>
            </div>
          </div>
        )
      },
      {
        title: '源类型',
        key: 'sourceType',
        width: 140,
        render: (row) => (
          <span class={styles.typeText}>{row.sourceType || 'UNKNOWN'}</span>
        )
      },
      {
        title: '字段注释',
        key: 'sourceComment',
        minWidth: 220,
        render: (row) => (
          <div
            class={styles.sourceCommentCell}
            onMouseup={() => {
              if (draggingMapping.value?.side === 'target') {
                handleMapTargetToSource(draggingMapping.value.key, row.sourceColumn || row.key)
              }
            }}
          >
            <span title={row.sourceComment || '暂无注释'} class={styles.commentText}>
              {row.sourceComment || '暂无注释'}
            </span>
            <div
              class={[styles.mappingAnchor, styles.sourceMappingAnchor]}
              data-source-anchor={row.sourceColumn || row.key}
              onMousedown={(event: MouseEvent) =>
                handleStartMappingDrag('source', row.sourceColumn || row.key, event)
              }
            />
          </div>
        )
      }
    ])

    const targetFieldColumns = computed<DataTableColumns<FieldDesignRow>>(() => [
      {
        title: '目标字段',
        key: 'targetColumnName',
        minWidth: 260,
        render: (row) => (
          <div class={[styles.columnCell, styles.targetColumnCell]}>
            <div
              class={styles.mappingFieldHead}
              onMouseup={() => {
                if (draggingMapping.value?.side === 'source') {
                  handleMapSourceToTarget(draggingMapping.value.key, row.key)
                }
              }}
            >
              <div
                class={styles.mappingAnchor}
                data-target-anchor={row.key}
                onMousedown={(event: MouseEvent) =>
                  handleStartMappingDrag('target', row.key, event)
                }
                onMouseup={() => {
                  if (draggingMapping.value?.side === 'source') {
                    handleMapSourceToTarget(draggingMapping.value.key, row.key)
                  }
                }}
              />
              <NInput
                value={row.targetColumn}
                placeholder='输入目标字段'
                onUpdateValue={(value) =>
                  handleTargetColumnNameChange(row.key, value)
                }
              />
            </div>
          </div>
        )
      },
      {
        title: '目标类型',
        key: 'targetType',
        width: 220,
        render: (row) => (
          <NSelect
            value={row.targetType}
            options={targetTypeOptions.value}
            placeholder='选择目标字段类型'
            filterable
            onUpdateValue={(value) =>
              handleTargetTypeChange(row.key, value)
            }
          />
        )
      },
      {
        title: '字段注释',
        key: 'targetComment',
        minWidth: 220,
        render: (row) => (
          <NInput
            value={row.targetComment}
            placeholder='输入目标字段注释'
            onUpdateValue={(value) => handleTargetCommentChange(row.key, value)}
          />
        )
      },
      {
        title: '主键',
        key: 'targetPrimaryKey',
        width: 90,
        render: (row) => (
          <NCheckbox
            checked={row.targetPrimaryKey}
            onUpdateChecked={(checked) =>
              handleTargetPrimaryKeyChange(row.key, checked)
            }
          />
        )
      },
      {
        title: '可空',
        key: 'targetNullable',
        width: 90,
        render: (row) => (
          <NCheckbox
            checked={row.targetNullable}
            onUpdateChecked={(checked) =>
              handleTargetNullableChange(row.key, checked)
            }
          />
        )
      }
    ])

    const latestInstanceTaskColumns = computed<DataTableColumns<WorkflowTaskProgressRow>>(() => [
      {
        title: '任务节点',
        key: 'name',
        minWidth: 220,
        render: (row) => (
          <button
            class={styles.taskLinkButton}
            type='button'
            onClick={() => openWorkflowInstanceDetail(state.latestInstanceId)}
          >
            {row.name}
          </button>
        )
      },
      {
        title: '状态',
        key: 'stateLabel',
        width: 120,
        render: (row) => (
          <NTag bordered={false} type={row.stateType}>
            {row.stateLabel}
          </NTag>
        )
      },
      {
        title: '开始时间',
        key: 'startTime',
        minWidth: 160,
        render: (row) => row.startTime || '-'
      },
      {
        title: '结束时间',
        key: 'endTime',
        minWidth: 160,
        render: (row) => row.endTime || '-'
      },
      {
        title: '执行机器',
        key: 'host',
        minWidth: 180
      }
    ])

    const statusTagMeta = (status: SyncTaskAssetStatus) => {
      const meta = {
        SUCCESS: ['成功', 'success'],
        FAILED: ['失败', 'error'],
        RUNNING: ['运行中', 'info'],
        DRAFT: ['草稿', 'warning'],
        OFFLINE: ['下线', 'default']
      } as const
      return meta[status]
    }

    const assetTableColumns = computed<DataTableColumns<SyncTaskAsset>>(() => [
      {
        title: '任务名称',
        key: 'name',
        minWidth: 260,
        render: (row) => (
          <div class={styles.assetNameCell}>
            <button
              class={styles.taskLinkButton}
              type='button'
              onClick={() => openAssetDetail(row)}
            >
              {row.name}
            </button>
            {row.errorMessage ? (
              <div class={styles.assetErrorText}>{row.errorMessage}</div>
            ) : null}
          </div>
        )
      },
      {
        title: '项目',
        key: 'projectName',
        width: 110
      },
      {
        title: '源端',
        key: 'sourcePath',
        minWidth: 190,
        render: (row) => (
          <div class={styles.assetPathCell}>
            <strong>{row.sourcePath}</strong>
            <span>{row.sourceName}</span>
          </div>
        )
      },
      {
        title: '目标端',
        key: 'targetPath',
        minWidth: 190,
        render: (row) => (
          <div class={styles.assetPathCell}>
            <strong>{row.targetPath}</strong>
            <span>{row.targetName}</span>
          </div>
        )
      },
      {
        title: '状态',
        key: 'status',
        width: 96,
        render: (row) => {
          const [label, type] = statusTagMeta(row.status)
          return <NTag bordered={false} type={type}>{label}</NTag>
        }
      },
      {
        title: '调度',
        key: 'scheduleStatus',
        width: 88,
        render: (row) => (
          <NTag bordered={false} type={row.scheduleStatus === 'ON' ? 'info' : 'default'}>
            {row.scheduleStatus === 'ON' ? '已调度' : '未调度'}
          </NTag>
        )
      },
      {
        title: '最近运行',
        key: 'lastRunTime',
        minWidth: 138,
        render: (row) => row.lastRunTime || '-'
      },
      {
        title: '读 / 写',
        key: 'rows',
        width: 100,
        render: (row) => `${row.readRows ?? '-'} / ${row.writeRows ?? '-'}`
      },
      {
        title: '操作',
        key: 'actions',
        width: 150,
        render: (row) => (
          <NSpace size={6}>
            <NButton size='small' text type='primary' onClick={() => openAssetDetail(row)}>
              详情
            </NButton>
            <NButton size='small' text type='primary' onClick={() => hydrateWizardFromAsset(row)}>
              {row.status === 'FAILED' ? '修复' : '编辑'}
            </NButton>
          </NSpace>
        )
      }
    ])

    watch(
      () => state.source.datasourceId,
      async () => {
        if (state.hydratingAsset) return
        resetEndpoint(state.source)
        state.fieldRows = []
        state.sourceFilters = [createSourceFilterRule()]
        if (!state.source.datasourceId) return
        await loadDatabases(state.source)
      }
    )

    watch(
      () => state.target.datasourceId,
      async () => {
        if (state.hydratingAsset) return
        resetEndpoint(state.target)
        state.targetSchemaName = getDefaultSchemaName(targetDatasourceOption.value?.type)
        if (!state.target.datasourceId) return
        await loadDatabases(state.target)
      }
    )

    watch(
      () => state.source.database,
      async () => {
        if (state.hydratingAsset) return
        state.source.table = null
        state.source.tables = []
        state.source.columns = []
        state.fieldRows = []
        state.sourceFilters = [createSourceFilterRule()]
        if (!state.source.database) return
        await loadTables(state.source)
      }
    )

    watch(
      () => state.target.database,
      async () => {
        if (state.hydratingAsset) return
        state.target.table = null
        state.target.tables = []
        state.target.columns = []
        if (!state.target.database) return
        await loadTables(state.target)
      }
    )

    watch(
      () => state.source.table,
      async () => {
        if (state.hydratingAsset) return
        state.source.columns = []
        state.fieldRows = []
        state.sourceFilters = [createSourceFilterRule()]
        if (!state.source.table) return
        await loadColumns(state.source)
        if (!state.targetTableName) {
          state.targetTableName = state.source.table || ''
        }
      }
    )

    watch(
      () => [
        sourceDatasourceOption.value?.label || '',
        state.source.table || '',
        targetDatasourceOption.value?.label || '',
        state.targetTableName.trim()
      ],
      () => {
        if (state.taskName.trim()) return
        state.taskName = buildSuggestedTaskName(
          sourceDatasourceOption.value,
          state.source.table,
          targetDatasourceOption.value,
          state.targetTableName
        )
      }
    )

    watch(
      () => [state.assetDetailTab, state.selectedAsset?.id || ''],
      () => {
        if (state.assetDetailTab === 'LOGS') {
          void loadAssetLogs(state.selectedAsset)
        }
      }
    )

    watch(
      () => [
        state.currentStep,
        state.fieldRows
          .map(
            (item) =>
              `${item.key}:${item.sync}:${item.targetColumn}:${item.targetType}:${item.targetComment}:${item.targetPrimaryKey}:${item.targetNullable}:${item.mappedTargetKey || ''}`
          )
          .join('|'),
        state.target.datasourceId,
        state.target.database,
        state.targetSchemaName,
        state.targetTableName,
        state.target.table
      ],
      () => {
        if (state.currentStep !== 3) return
        state.latestCreateTableDdl = ''
        state.latestCreateTableDdlManual = false
        void handlePreviewTargetTable(false)
      }
    )

    watch(
      () => state.target.table,
      async (targetTable, previousTargetTable) => {
        if (state.hydratingAsset) return
        state.target.columns = []
        if (!targetTable) return
        if (targetTable !== previousTargetTable) {
          // 切换到已有目标表模式时，先清掉“新建目标表”遗留的旧映射状态，
          // 避免同名字段因旧的未勾选状态被错误保留下来。
          state.fieldRows = []
        }
        state.targetTableName = targetTable
        await loadColumns(state.target)
      }
    )

    watch(
      () => [state.targetTableName.trim(), state.target.tables.join('|')],
      async ([targetTableName]) => {
        if (state.hydratingAsset) return
        const tableName = String(targetTableName || '').trim()
        const matchedTable = tableName
          ? state.target.tables.find(
              (item) => item.toLowerCase() === tableName.toLowerCase()
            ) || null
          : null

        // 第 1 步不再暴露“目标表模式”控件。这里用目标表名的存在性检查结果
        // 自动维护内部已有表/新建表状态，保证第 2 步仍能按正确模式加载字段映射。
        if (!matchedTable) {
          if (state.target.table) {
            state.target.table = null
          }
          state.target.columns = []
          return
        }

        if (state.target.table === matchedTable && state.target.columns.length) {
          return
        }
        if (state.target.table !== matchedTable) {
          state.fieldRows = []
        }
        state.target.table = matchedTable
        await loadColumns(state.target)
      }
    )

    watch(
      () => [
        state.source.datasourceId,
        state.source.database,
        state.source.table,
        state.target.datasourceId,
        state.target.database,
        state.target.table,
        state.targetTableName.trim(),
        targetTableMode.value
      ],
      () => {
        if (state.hydratingAsset) return
        if (!state.source.columns.length) {
          state.fieldRows = []
          return
        }
        refreshFieldRows()
      }
    )

    watch(
      () => [
        state.source.columns
          .map(
            (item) =>
              `${item.name}:${item.type}:${item.comment || ''}:${item.primaryKey}:${item.nullable}`
          )
          .join(','),
        state.target.columns
          .map(
            (item) =>
              `${item.name}:${item.type}:${item.comment || ''}:${item.primaryKey}:${item.nullable}`
          )
          .join(',')
      ],
      () => {
        if (state.hydratingAsset) return
        if (!state.source.columns.length) return
        refreshFieldRows()
      }
    )

    onMounted(() => {
      loadDatasourceList()
      loadProjects()
    })

    return {
      state,
      sourceDatasourceOption,
      targetDatasourceOption,
      sourceDatabaseOptions,
      sourceTableOptions,
      targetDatabaseOptions,
      targetTableOptions,
      targetSchemaPlaceholder,
      targetTableMode,
      targetTableModeLabel,
      targetTableExists,
      targetTableCheckText,
      sourceFieldRows,
      targetTypeOptions,
      syncWarnings,
      summaryItems,
      selectedProjectOption,
      sourceFieldStats,
      stepOneCheckItems,
      activeSourceFilterCount,
      stepOneReadyText,
      stepItems,
      currentStepMeta,
      generatedConfig,
      effectiveConfigText,
      mappedCount,
      mappingWorkbenchRef,
      mappingLinePaths,
      mappingDraftPath,
      targetFieldRows,
      refreshMappingLayout,
      sourceFieldColumns,
      targetFieldColumns,
      latestInstanceTaskColumns,
      assetMetrics,
      filteredAssets,
      assetTableColumns,
      statusTagMeta,
      openCreateWizard,
      openAgentDrawer,
      fillAgentExample,
      applyAgentPlanToWizard,
      backToAssetList,
      openAssetDetail,
      hydrateWizardFromAsset,
      openWorkflowInstanceDetail,
      loadAssetLogs,
      openAssetLogFullscreen,
      allFieldsChecked,
      someFieldsChecked,
      handleChooseAllMappings,
      handleInvertMappings,
      handleAutoMapByName,
      applyTargetNameRule,
      handleToggleMappingExceptionOnly,
      handleCopyDdl,
      handleFormatDdl,
      handleCopyConfig,
      handleOpenPreview,
      handleConfigEditorChange,
      handleResetConfigEditor,
      handleCreateTargetTable,
      handlePreviewTargetTable,
      handleSaveWorkflow,
      handleRunWorkflow,
      handleOpenScheduleModal,
      loadScheduleMeta,
      handlePrevStep,
      handleNextStep,
      handleJumpStep,
      validateStepOne,
      validateStepTwo
    }
  },
  render() {
    const datasourceSelectOptions = this.state.datasourceOptions
    const selectedAsset = this.state.selectedAsset
    const agentPlan = this.state.agentPlan
    const renderAgentDrawer = () => (
      <NDrawer
        show={this.state.agentDrawerVisible}
        placement='right'
        width='560px'
        onUpdateShow={(value) => { this.state.agentDrawerVisible = value }}
      >
        <NDrawerContent title='同步任务 Agent' closable>
          <NSpace vertical size={14}>
            <div class={styles.agentIntro}>
              用一句话描述同步需求，Agent 会解析数据源、库表、字段映射和执行动作，并套用到现有同步任务向导。
            </div>
            <div class={styles.agentExampleList}>
              {SYNC_AGENT_EXAMPLES.map((example) => (
                <button
                  key={example}
                  type='button'
                  class={styles.agentExample}
                  onClick={() => this.fillAgentExample(example)}
                >
                  {example}
                </button>
              ))}
            </div>
            <NInput
              type='textarea'
              value={this.state.agentCommand}
              placeholder='例如：把 mysql case_workbench.ajxx_tab 同步到 pg public.agent_ajxx_tab，只同步5条'
              autosize={{ minRows: 5, maxRows: 8 }}
              onUpdateValue={(value) => {
                this.state.agentCommand = value
              }}
            />
            <NCheckbox
              checked={this.state.agentAutoExecute}
              onUpdateChecked={(checked) => {
                this.state.agentAutoExecute = checked
              }}
            >
              解析成功后自动执行
            </NCheckbox>
            <NSpace>
              <NButton
                type='primary'
                loading={this.state.agentRunning}
                onClick={() => this.applyAgentPlanToWizard(false)}
              >
                解析并生成方案
              </NButton>
              <NButton
                type='primary'
                ghost
                loading={this.state.agentRunning || this.state.runningWorkflow}
                onClick={() => this.applyAgentPlanToWizard(true)}
              >
                确认并执行
              </NButton>
            </NSpace>
            {this.state.agentError ? (
              <NAlert type='error' showIcon={false}>
                {this.state.agentError}
              </NAlert>
            ) : null}
            <div class={styles.agentStages}>
              {this.state.agentStages.map((stage) => (
                <div key={stage.key} class={styles.agentStage}>
                  <NTag
                    bordered={false}
                    type={
                      stage.status === 'SUCCESS'
                        ? 'success'
                        : stage.status === 'ERROR'
                          ? 'error'
                          : stage.status === 'RUNNING'
                            ? 'info'
                            : 'default'
                    }
                  >
                    {stage.status === 'WAITING'
                      ? '等待'
                      : stage.status === 'RUNNING'
                        ? '进行中'
                        : stage.status === 'SUCCESS'
                          ? '完成'
                          : '失败'}
                  </NTag>
                  <div>
                    <strong>{stage.label}</strong>
                    <span>{stage.message}</span>
                  </div>
                </div>
              ))}
            </div>
            {agentPlan ? (
              <div class={styles.agentPlanCard}>
                <div class={styles.agentPlanHead}>
                  <div>
                    <div class={styles.sectionTitle}>Agent 同步方案</div>
                    <div class={styles.hintText}>
                      置信度 {agentPlan.confidence}% · {agentPlan.autoExecute || this.state.agentAutoExecute ? '立即执行' : '先生成草稿'}
                    </div>
                  </div>
                  <NTag bordered={false} type={agentPlan.confidence >= 80 ? 'success' : 'warning'}>
                    {agentPlan.sourceType || '-'} → {agentPlan.targetType || '-'}
                  </NTag>
                </div>
                <div class={styles.agentPlanGrid}>
                  <div><span>项目</span><strong>{agentPlan.projectName}</strong></div>
                  <div><span>源端</span><strong>{agentPlan.sourceDatasourceName}</strong></div>
                  <div><span>源表</span><strong>{agentPlan.sourceDatabase}.{agentPlan.sourceTable}</strong></div>
                  <div><span>目标端</span><strong>{agentPlan.targetDatasourceName}</strong></div>
                  <div><span>目标表</span><strong>{agentPlan.targetSchema}.{agentPlan.targetTable}</strong></div>
                  <div><span>字段</span><strong>{agentPlan.mappedColumnCount} / {agentPlan.sourceColumnCount}</strong></div>
                  <div><span>抽样</span><strong>{agentPlan.limit ? `${agentPlan.limit} 条` : '未限制'}</strong></div>
                  <div><span>动作</span><strong>{agentPlan.autoExecute || this.state.agentAutoExecute ? '保存并执行' : '套用到向导'}</strong></div>
                </div>
                {agentPlan.warnings.length ? (
                  <div class={styles.agentWarnings}>
                    {agentPlan.warnings.map((warning) => (
                      <NAlert key={warning} type='warning' showIcon={false}>
                        {warning}
                      </NAlert>
                    ))}
                  </div>
                ) : null}
                <NSpace>
                  <NButton
                    type='primary'
                    onClick={() => {
                      this.state.viewMode = 'WIZARD'
                      this.state.currentStep = 2
                      this.state.activeSolutionModule = 'MAPPING'
                      this.state.agentDrawerVisible = false
                      this.$nextTick(() => this.refreshMappingLayout())
                    }}
                  >
                    查看字段映射
                  </NButton>
                  <NButton
                    loading={this.state.runningWorkflow}
                    onClick={this.handleRunWorkflow}
                  >
                    运行这个方案
                  </NButton>
                </NSpace>
              </div>
            ) : null}
          </NSpace>
        </NDrawerContent>
      </NDrawer>
    )
    const renderAssetLogContent = (asset: SyncTaskAsset, fullscreen = false) => (
      <NSpin show={!!asset.logLoading}>
        <NSpace vertical>
          {asset.errorMessage ? (
            <NAlert type='error' showIcon={false}>
              {asset.errorMessage}
            </NAlert>
          ) : null}
          <div class={styles.assetKvGrid}>
            <div><span>最近实例</span><strong>{asset.lastInstanceId || '-'}</strong></div>
            <div><span>工作流编码</span><strong>{asset.workflowCode || '-'}</strong></div>
            <div><span>项目</span><strong>{asset.projectName}</strong></div>
            <div><span>日志状态</span><strong>{asset.logLoading ? '读取中' : asset.logLoaded ? '已读取' : '等待读取'}</strong></div>
          </div>
          {asset.lastInstanceId ? (
            <NSpace>
              <NButton
                size='small'
                loading={!!asset.logLoading}
                onClick={() => {
                  asset.logLoaded = false
                  void this.loadAssetLogs(asset)
                }}
              >
                刷新日志
              </NButton>
              {!fullscreen ? (
                <NButton size='small' type='primary' ghost onClick={this.openAssetLogFullscreen}>
                  全屏查看
                </NButton>
              ) : null}
            </NSpace>
          ) : null}
          {asset.logError ? (
            <NAlert type='error' showIcon={false}>
              {asset.logError}
            </NAlert>
          ) : null}
          {asset.lastInstanceId ? (
            <div class={[styles.codeWrap, fullscreen ? styles.fullscreenCodeWrap : '']}>
              <pre class={styles.codeBlock}>
                {asset.logText || (asset.logLoading ? '日志读取中...' : '暂无日志内容。')}
              </pre>
            </div>
          ) : (
            <NEmpty description='当前同步任务还没有最近运行实例，暂无日志。' />
          )}
        </NSpace>
      </NSpin>
    )
    const renderAssetDetailBody = () => {
      if (!selectedAsset) return null
      if (this.state.assetDetailTab === 'CONFIG') {
        return (
          <NSpace vertical>
            <div class={styles.assetKvGrid}>
              <div><span>源端</span><strong>{selectedAsset.sourceName} / {selectedAsset.sourcePath}</strong></div>
              <div><span>目标端</span><strong>{selectedAsset.targetName} / {selectedAsset.targetPath}</strong></div>
              <div><span>源端过滤</span><strong>{describeSourceFilters(selectedAsset.sourceFilters)}</strong></div>
              <div><span>数据去向</span><strong>{selectedAsset.sinkCustomSql || '未配置'}</strong></div>
            </div>
            <div class={styles.sectionTitle}>字段映射</div>
            <NDataTable
              columns={[
                { title: '源字段', key: 'sourceColumn' },
                { title: '目标字段', key: 'targetColumn' },
                { title: '目标类型', key: 'targetType' },
                {
                  title: '方式',
                  key: 'mappingKind',
                  render: (row: FieldDesignRow) => row.mappingKind === 'MANUAL' ? '手动' : '自动'
                }
              ]}
              data={selectedAsset.fieldRows.filter((item) => item.sync)}
              row-key={(row: FieldDesignRow) => row.key}
              size='small'
              pagination={false}
            />
          </NSpace>
        )
      }
      if (this.state.assetDetailTab === 'HISTORY') {
        return (
          <NDataTable
            columns={[
              { title: '实例', key: 'id' },
              {
                title: '状态',
                key: 'status',
                render: (row: any) => {
                  const [label, type] = this.statusTagMeta(row.status)
                  return <NTag bordered={false} type={type}>{label}</NTag>
                }
              },
              { title: '触发方式', key: 'trigger' },
              { title: '开始时间', key: 'startTime' },
              { title: '结束时间', key: 'endTime' },
              { title: '耗时', key: 'duration' },
              { title: '读 / 写', key: 'rows' }
            ]}
            data={selectedAsset.history}
            row-key={(row: any) => row.id}
            size='small'
            pagination={false}
          />
        )
      }
      if (this.state.assetDetailTab === 'LOGS') {
        return renderAssetLogContent(selectedAsset)
      }
      if (this.state.assetDetailTab === 'CHANGES') {
        return (
          <div class={styles.assetTimeline}>
            {selectedAsset.changes.map((item) => (
              <div class={styles.assetTimelineItem} key={`${item.time}-${item.action}`}>
                <span>{item.time}</span>
                <strong>{item.action}</strong>
                <em>{item.user}</em>
              </div>
            ))}
          </div>
        )
      }
      const [statusLabel, statusType] = this.statusTagMeta(selectedAsset.status)
      return (
        <NSpace vertical>
          {selectedAsset.errorMessage ? (
            <NAlert type='error' showIcon={false}>
              {selectedAsset.errorMessage}
            </NAlert>
          ) : null}
          <div class={styles.assetKvGrid}>
            <div><span>当前状态</span><strong><NTag bordered={false} type={statusType}>{statusLabel}</NTag></strong></div>
            <div><span>工作流编码</span><strong>{selectedAsset.workflowCode || '-'}</strong></div>
            <div><span>最近实例</span><strong>{selectedAsset.lastInstanceId || '-'}</strong></div>
            <div><span>最近读 / 写</span><strong>{selectedAsset.readRows ?? '-'} / {selectedAsset.writeRows ?? '-'}</strong></div>
            <div><span>耗时</span><strong>{selectedAsset.duration}</strong></div>
            <div><span>更新时间</span><strong>{selectedAsset.updatedAt}</strong></div>
          </div>
        </NSpace>
      )
    }
    if (this.state.viewMode === 'LIST') {
      return (
        <NSpace vertical class={styles.page}>
          <div class={styles.pageHeader}>
            <div class={styles.heroBlock}>
              <h2 class={styles.heroTitle}>同步任务</h2>
              <div class={styles.hintText}>管理已保存的同步任务，查看运行状态、历史配置和失败诊断。</div>
            </div>
            <div class={styles.heroActions}>
              <NButton ghost onClick={this.openAgentDrawer}>Agent 创建同步任务</NButton>
              <NButton type='primary' onClick={this.openCreateWizard}>新建同步任务</NButton>
            </div>
          </div>
          <div class={styles.assetMetrics}>
            {this.assetMetrics.map((item) => (
              <button
                key={item.key || 'all'}
                type='button'
                class={[
                  styles.assetMetric,
                  (item.key === 'SCHEDULED'
                    ? this.state.assetScheduleFilter === 'ON'
                    : this.state.assetStatusFilter === item.key) ||
                    (!item.key && !this.state.assetStatusFilter && !this.state.assetScheduleFilter)
                    ? styles.assetMetricActive
                    : ''
                ]}
                onClick={() => {
                  if (item.key === 'SCHEDULED') {
                    this.state.assetStatusFilter = ''
                    this.state.assetScheduleFilter = 'ON'
                  } else {
                    this.state.assetStatusFilter = item.key
                    this.state.assetScheduleFilter = ''
                  }
                }}
              >
                <span>{item.label}</span>
                <strong>{item.value}</strong>
              </button>
            ))}
          </div>
          <div class={styles.assetToolbar}>
            <NInput
              value={this.state.assetKeyword}
              placeholder='搜索任务名称、源表、目标表、工作流编码'
              clearable
              onUpdateValue={(value) => { this.state.assetKeyword = value }}
            />
            <NSelect
              value={this.state.assetProjectFilter}
              placeholder='全部项目'
              clearable
              options={[...new Set(this.state.syncTaskAssets.map((item) => item.projectName))]
                .map((item) => ({ label: item, value: item }))}
              onUpdateValue={(value) => { this.state.assetProjectFilter = value || '' }}
            />
            <NSelect
              value={this.state.assetStatusFilter}
              placeholder='全部状态'
              clearable
              options={[
                { label: '成功', value: 'SUCCESS' },
                { label: '失败', value: 'FAILED' },
                { label: '运行中', value: 'RUNNING' },
                { label: '草稿', value: 'DRAFT' },
                { label: '下线', value: 'OFFLINE' }
              ]}
              onUpdateValue={(value) => { this.state.assetStatusFilter = value || '' }}
            />
            <NSelect
              value={this.state.assetScheduleFilter}
              placeholder='全部调度'
              clearable
              options={[
                { label: '已调度', value: 'ON' },
                { label: '未调度', value: 'OFF' }
              ]}
              onUpdateValue={(value) => { this.state.assetScheduleFilter = value || '' }}
            />
            <NSelect
              value={this.state.assetTypeFilter}
              placeholder='全部链路'
              clearable
              options={SUPPORT_TYPES.map((item) => ({ label: item, value: item }))}
              onUpdateValue={(value) => { this.state.assetTypeFilter = value || '' }}
            />
            <NButton
              onClick={() => {
                this.state.assetKeyword = ''
                this.state.assetProjectFilter = ''
                this.state.assetStatusFilter = ''
                this.state.assetScheduleFilter = ''
                this.state.assetTypeFilter = ''
              }}
            >
              重置
            </NButton>
          </div>
          <Card>
            <NDataTable
              columns={this.assetTableColumns}
              data={this.filteredAssets}
              row-key={(row: SyncTaskAsset) => row.id}
              size='small'
              loading={this.state.loadingAssets}
              pagination={{ pageSize: 10 }}
              striped
            />
          </Card>
          <NDrawer
            show={this.state.assetDetailVisible}
            placement='right'
            width='58vw'
            minWidth={760}
            onUpdateShow={(value) => { this.state.assetDetailVisible = value }}
          >
            <NDrawerContent title={selectedAsset?.name || '同步任务详情'} closable>
              {selectedAsset ? (
                <NSpace vertical>
                  <div class={styles.assetDrawerHead}>
                    <div>
                      <div class={styles.hintText}>{selectedAsset.sourcePath} → {selectedAsset.targetPath}</div>
                    </div>
                    <NSpace>
                      <NButton type='primary' onClick={() => this.hydrateWizardFromAsset(selectedAsset)}>
                        编辑配置
                      </NButton>
                      <NButton onClick={() => { this.state.assetDetailTab = 'HISTORY' }}>运行历史</NButton>
                    </NSpace>
                  </div>
                  <div class={styles.assetTabs}>
                    {[
                      ['OVERVIEW', '概览'],
                      ['CONFIG', '配置'],
                      ['HISTORY', '运行历史'],
                      ['LOGS', '日志诊断'],
                      ['CHANGES', '变更记录']
                    ].map(([key, label]) => (
                      <button
                        key={key}
                        type='button'
                        class={[
                          styles.assetTab,
                          this.state.assetDetailTab === key ? styles.assetTabActive : ''
                        ]}
                        onClick={() => { this.state.assetDetailTab = key as SyncTaskDetailTab }}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                  <div class={styles.assetDrawerBody}>
                    {renderAssetDetailBody()}
                  </div>
                </NSpace>
              ) : null}
            </NDrawerContent>
          </NDrawer>
          <NDrawer
            show={this.state.assetLogFullscreenVisible}
            placement='right'
            width='96vw'
            minWidth={960}
            onUpdateShow={(value) => { this.state.assetLogFullscreenVisible = value }}
          >
            <NDrawerContent title={selectedAsset ? `日志诊断 - ${selectedAsset.name}` : '日志诊断'} closable>
              {selectedAsset ? renderAssetLogContent(selectedAsset, true) : null}
            </NDrawerContent>
          </NDrawer>
          {renderAgentDrawer()}
        </NSpace>
      )
    }
    let stepContent = null
    const sourceFilterContent = (
      <div class={styles.solutionPanel}>
        <div class={styles.solutionPanelHeader}>
          <div>
            <div class={styles.sectionTitle}>源端过滤条件</div>
              <div class={styles.hintText}>
              可选配置。结构化条件会写入 source query，不配置时表示全量读取。
              {this.state.agentSampleLimit ? ` Agent 已识别抽样限制：LIMIT ${this.state.agentSampleLimit}。` : ''}
            </div>
          </div>
          <NTag bordered={false} type='info'>
            已启用 {this.activeSourceFilterCount} / {this.state.sourceFilters.length}
          </NTag>
        </div>
        <div class={styles.sourceFilterList}>
          {this.state.sourceFilters.map((rule, index) => {
            const fieldMeta = this.state.source.columns.find(
              (item) => item.name === rule.field
            )
            const filterOptions = getSourceFilterOperatorOptions(fieldMeta?.type || '')
            const noValueOperator =
              rule.operator === 'IS_NULL' || rule.operator === 'NOT_NULL'
            return (
              <div key={rule.key} class={styles.sourceFilterRow}>
                <NCheckbox
                  checked={rule.enabled}
                  onUpdateChecked={(checked) => {
                    rule.enabled = checked
                  }}
                />
                <NSelect
                  value={rule.field}
                  options={this.state.source.columns.map((item) => ({
                    label: `${item.name}${item.comment ? ` · ${item.comment}` : ''}`,
                    value: item.name
                  }))}
                  placeholder='选择字段'
                  filterable
                  clearable
                  style={{ width: '240px' }}
                  onUpdateValue={(value) => {
                    rule.field = value || ''
                    if (!value) {
                      rule.operator = 'EQ'
                      rule.value = ''
                      rule.valueEnd = ''
                      return
                    }
                    const nextField = this.state.source.columns.find(
                      (item) => item.name === value
                    )
                    const nextOperators = getSourceFilterOperatorOptions(nextField?.type || '')
                    if (!nextOperators.some((item) => item.value === rule.operator)) {
                      rule.operator = (nextOperators[0]?.value as SourceFilterOperator) || 'EQ'
                      rule.value = ''
                      rule.valueEnd = ''
                    }
                  }}
                />
                <NSelect
                  value={rule.operator}
                  options={filterOptions}
                  placeholder='操作符'
                  style={{ width: '160px' }}
                  onUpdateValue={(value) => {
                    rule.operator = (value as SourceFilterOperator) || 'EQ'
                    if (rule.operator !== 'BETWEEN') {
                      rule.valueEnd = ''
                    }
                    if (rule.operator === 'IS_NULL' || rule.operator === 'NOT_NULL') {
                      rule.value = ''
                      rule.valueEnd = ''
                    }
                  }}
                />
                <NInput
                  value={rule.value}
                  placeholder={
                    noValueOperator
                      ? '该操作符不需要值'
                      : rule.operator === 'IN'
                        ? '逗号分隔多个值'
                        : rule.operator === 'BETWEEN'
                          ? '起始值'
                          : '输入值'
                  }
                  disabled={noValueOperator}
                  style={{ width: '220px' }}
                  onUpdateValue={(value) => {
                    rule.value = value
                  }}
                />
                {rule.operator === 'BETWEEN' ? (
                  <NInput
                    value={rule.valueEnd}
                    placeholder='结束值'
                    style={{ width: '220px' }}
                    onUpdateValue={(value) => {
                      rule.valueEnd = value
                    }}
                  />
                ) : null}
                <NButton
                  size='small'
                  onClick={() => {
                    const nextRule = {
                      ...rule,
                      key: `filter-${Date.now()}-${index}`
                    }
                    this.state.sourceFilters.splice(index + 1, 0, nextRule)
                  }}
                  disabled={this.state.sourceFilters.length >= SOURCE_FILTER_MAX_COUNT}
                >
                  复制
                </NButton>
                <NButton
                  size='small'
                  onClick={() => {
                    if (this.state.sourceFilters.length === 1) {
                      this.state.sourceFilters[0] = createSourceFilterRule()
                      return
                    }
                    this.state.sourceFilters.splice(index, 1)
                  }}
                >
                  删除
                </NButton>
              </div>
            )
          })}
        </div>
        <div class={styles.sourceFilterFooter}>
          <NButton
            size='small'
            onClick={() => {
              if (this.state.sourceFilters.length >= SOURCE_FILTER_MAX_COUNT) return
              this.state.sourceFilters.push(
                createSourceFilterRule(this.state.sourceFilters.length + 1)
              )
            }}
            disabled={this.state.sourceFilters.length >= SOURCE_FILTER_MAX_COUNT}
          >
            添加条件
          </NButton>
          <span class={styles.hintText}>
            最多支持 {SOURCE_FILTER_MAX_COUNT} 条条件，保持条件可读、可审阅。
          </span>
        </div>
      </div>
    )
    const fieldMappingContent = (
      <div
        class={styles.mappingWorkbench}
        ref={(el) => {
          this.mappingWorkbenchRef = el as HTMLElement | null
        }}
      >
        <svg class={styles.mappingSvgLayer}>
          {this.mappingLinePaths.map((item) => (
            <path
              key={item.key}
              d={item.path}
              class={[
                item.kind === 'MANUAL' ? styles.mappingPathManual : styles.mappingPathAuto,
                item.active ? styles.mappingPathActive : ''
              ]}
            />
          ))}
          {this.mappingDraftPath ? (
            <path d={this.mappingDraftPath} class={styles.mappingPathDraft} />
          ) : null}
        </svg>
        <div class={styles.mappingPane}>
          <div class={styles.paneTitleBar}>
            <NThing>
              {{
                header: () => <div class={styles.paneTitle}>源字段区</div>,
                description: () => (
                  <div class={styles.paneDesc}>
                    展示源字段名、类型、注释、主键和可空属性。勾选哪些字段，就同步哪些字段。
                  </div>
                )
              }}
            </NThing>
            <div class={styles.paneStats}>
              <NTag type='info' bordered={false}>
                {this.state.source.table || '未选择源表'}
              </NTag>
              <NTag bordered={false}>共 {this.sourceFieldRows.length} 列</NTag>
            </div>
          </div>
          <div class={styles.fieldToolbar}>
            <NButton
              size='small'
              onClick={() => this.handleChooseAllMappings(true)}
            >
              全选
            </NButton>
            <NButton size='small' onClick={this.handleInvertMappings}>
              反选
            </NButton>
            <NButton
              size='small'
              onClick={() => this.handleChooseAllMappings(false)}
            >
              清空
            </NButton>
            <NButton size='small' onClick={this.handleToggleMappingExceptionOnly}>
              {this.state.mappingExceptionOnly ? '查看全部' : '只看异常'}
            </NButton>
          </div>
          <div class={styles.scrollTable} onScroll={this.refreshMappingLayout}>
            <NDataTable
              columns={this.sourceFieldColumns}
              data={this.sourceFieldRows}
              row-key={(row: FieldDesignRow) => row.sourceColumn || row.key}
              size='small'
              pagination={false}
              striped
            />
          </div>
        </div>

        <div class={styles.mappingLane}>
          <span class={styles.mappingLaneGuide} />
        </div>

        <div class={styles.mappingPane}>
          <div class={styles.paneTitleBar}>
            <NThing>
              {{
                header: () => <div class={styles.paneTitle}>目标字段设计区</div>,
                description: () => (
                  <div class={styles.paneDesc}>
                    <span>
                      {this.targetTableMode === 'EXISTING_TABLE'
                        ? '按已有目标表字段展示，同名字段已自动映射，未映射字段可手动连线。'
                        : '新建目标表时按源字段生成目标字段，可继续调整字段名、类型、注释、主键和可空属性。'}
                    </span>
                    <span class={styles.mappingLegendInline}>
                      <span><i class={styles.legendAuto} />虚线：系统自动映射</span>
                      <span><i class={styles.legendManual} />实线：手动拖拽映射</span>
                    </span>
                  </div>
                )
              }}
            </NThing>
            <div class={styles.paneStats}>
              <NTag type='success' bordered={false}>
                {this.state.targetTableName || '未确认目标表'}
              </NTag>
              <NTag bordered={false}>{this.targetTableModeLabel}</NTag>
              <NTag bordered={false}>已映射 {this.mappedCount} 列</NTag>
            </div>
          </div>
          <div class={[styles.fieldToolbar, styles.targetFieldToolbar]}>
            {this.targetTableMode === 'CREATE_TABLE' ? (
              <NRadioGroup
                value={this.state.targetNameRule}
                size='small'
                onUpdateValue={(value: TargetNameRule) =>
                  this.applyTargetNameRule(value)
                }
              >
                <NRadioButton value='KEEP_SOURCE'>保持源名</NRadioButton>
                <NRadioButton value='LOWERCASE'>全小写</NRadioButton>
                <NRadioButton value='UPPERCASE'>全大写</NRadioButton>
              </NRadioGroup>
            ) : (
              <NTag bordered={false} type='info'>
                已有目标表
              </NTag>
            )}
          </div>
          <div class={styles.scrollTable} onScroll={this.refreshMappingLayout}>
            {this.targetFieldRows.length ? (
              <NDataTable
                columns={this.targetFieldColumns}
                data={this.targetFieldRows}
                row-key={(row: FieldDesignRow) => row.key}
                size='small'
                pagination={false}
                striped
              />
            ) : (
              <div class={styles.emptyPanel}>
                <NEmpty description='还没有可展示的目标字段。新建表请先勾选源字段；已有表请先选择目标表。' />
              </div>
            )}
          </div>
        </div>
      </div>
    )
    const sinkContent = (
      <div class={styles.solutionPanel}>
        <div class={styles.solutionPanelHeader}>
          <div>
            <div class={styles.sectionTitle}>数据去向</div>
            <div class={styles.hintText}>
              可选配置。SeaTunnel JDBC sink 使用 custom_sql 作为同步前 SQL。
            </div>
          </div>
          <NTag bordered={false} type='warning'>可配置</NTag>
        </div>
        {this.state.sinkCustomSql.trim() ? (
          <NAlert type='warning' showIcon={false}>
            当前 sink 使用显式 query 写入字段列表；请确认 SeaTunnel JDBC sink 在该模式下会执行 custom_sql。
          </NAlert>
        ) : null}
        <NInput
          type='textarea'
          value={this.state.sinkCustomSql}
          placeholder='例如：truncate table ajxx_tab_sync;'
          autosize={{ minRows: 6, maxRows: 12 }}
          onUpdateValue={(value) => {
            this.state.sinkCustomSql = value
          }}
        />
      </div>
    )
    const processingContent = (
      <div class={styles.solutionPanel}>
        <div class={styles.solutionPanelHeader}>
          <div>
            <div class={styles.sectionTitle}>数据处理</div>
            <div class={styles.hintText}>
              后续用于字符串替换、AI 辅助处理、数据向量化等能力。
            </div>
          </div>
          <NTag bordered={false} type='default'>暂未实现</NTag>
        </div>
        <NAlert type='info' showIcon={false}>
          数据处理暂未实现。本期只保留入口，不参与保存、执行或 SeaTunnel 配置生成。
        </NAlert>
      </div>
    )
    const solutionContent = {
      MAPPING: fieldMappingContent,
      FILTER: sourceFilterContent,
      SINK: sinkContent,
      PROCESSING: processingContent
    }[this.state.activeSolutionModule]

    // 这里用单一分支而不是多个 && 并列分支，避免 Vue JSX 在同层复用节点时把上一步内容残留在页面里。
    if (this.state.currentStep === 1) {
      stepContent = (
        <div class={styles.stageStack} key='step-1'>
          <Card contentStyle={{ padding: 0 }}>
            <div class={styles.stepOneContext}>
              <div class={styles.stepOneCheckHead}>
                <div class={styles.statusTitle}>
                  <strong>配置检查</strong>
                  <NTag bordered={false} type={this.validateStepOne(false) ? 'success' : 'warning'}>
                    {this.stepOneReadyText}
                  </NTag>
                </div>
                <div class={styles.validationItems}>
                  {this.stepOneCheckItems.map((item) => (
                    <div
                      key={item.label}
                      class={[
                        styles.validationPill,
                        item.done ? styles.validationPillDone : styles.validationPillTodo
                      ]}
                    >
                      <span>{item.label}</span>
                      <strong>{item.detail}</strong>
                    </div>
                  ))}
                </div>
              </div>
              <div class={styles.stepOneBaseGrid}>
                <div class={styles.fieldBlock}>
                  <div class={styles.fieldLabel}>
                    归属项目
                    <span class={styles.requiredMark}>*</span>
                  </div>
                  <NSelect
                    value={this.state.selectedProjectCode}
                    options={this.state.projectOptions}
                    placeholder='选择项目'
                    filterable
                    clearable
                    loading={this.state.loadingProjects}
                    onUpdateValue={(value) => {
                      this.state.selectedProjectCode = value
                    }}
                  />
                </div>
                <div class={styles.fieldBlock}>
                  <div class={styles.fieldLabel}>任务名称</div>
                  <NInput
                    value={this.state.taskName}
                    placeholder='根据源端和目标端自动生成，可编辑'
                    onUpdateValue={(value) => {
                      this.state.taskName = value
                    }}
                  />
                </div>
                <div class={styles.fieldBlock}>
                  <div class={styles.fieldLabel}>同步目标</div>
                  <NInput
                    value={
                      this.state.source.table && this.state.targetTableName
                        ? `从 ${this.sourceDatasourceOption?.type || '源端'} 读取 ${this.state.source.table}，同步到 ${this.targetDatasourceOption?.type || '目标端'} ${this.state.targetSchemaName || this.targetSchemaPlaceholder}.${this.state.targetTableName}`
                        : ''
                    }
                    placeholder='选择源表和目标表后自动生成'
                    readonly
                  />
                </div>
              </div>
            </div>
          </Card>

          <Card contentStyle={{ padding: 0 }}>
            <div class={styles.objectWorkbench}>
              <div class={styles.objectWorkbenchTitle}>
                <div>
                  <strong>选择同步对象</strong>
                  <span>只确定源表和目标表，字段映射、过滤条件、数据去向在下一步配置。</span>
                </div>
              </div>

              <div class={styles.endpointWorkbench}>
                <div class={styles.endpointCard}>
                  <div class={styles.endpointCardHead}>
                    <div class={styles.endpointTitle}>
                      <span class={styles.sourceToken}>SOURCE</span>
                      <strong>源端</strong>
                    </div>
                    <NTag bordered={false} type='success'>连接正常</NTag>
                  </div>
                  <NSpin show={this.state.source.loading || this.state.loadingDatasources}>
                    <div class={styles.endpointPanel}>
                      <div class={styles.formGrid}>
                        <div class={styles.fieldBlock}>
                          <div class={styles.fieldLabel}>数据源</div>
                          <NSelect
                            value={this.state.source.datasourceId}
                            options={datasourceSelectOptions}
                            placeholder='选择源数据源'
                            filterable
                            clearable
                            onUpdateValue={(value) => {
                              this.state.source.datasourceId = value
                            }}
                          />
                        </div>
                        <div class={styles.fieldBlock}>
                          <div class={styles.fieldLabel}>数据库</div>
                          <NSelect
                            value={this.state.source.database}
                            options={this.sourceDatabaseOptions}
                            placeholder='选择源库'
                            filterable
                            clearable
                            onUpdateValue={(value) => {
                              this.state.source.database = value
                            }}
                          />
                        </div>
                        <div class={[styles.fieldBlock, styles.fieldBlockSpan2]}>
                          <div class={styles.fieldLabel}>数据表</div>
                          <NSelect
                            value={this.state.source.table}
                            options={this.sourceTableOptions}
                            placeholder='选择源表'
                            filterable
                            clearable
                            onUpdateValue={(value) => {
                              this.state.source.table = value
                            }}
                          />
                        </div>
                      </div>
                    </div>
                  </NSpin>
                </div>

                <div class={styles.objectConfirmRail}>
                  <span class={styles.objectConfirmLine}></span>
                  <div class={styles.objectConfirmNode}>对象<br />确认</div>
                  <span class={styles.objectConfirmLine}></span>
                </div>

                <div class={styles.endpointCard}>
                  <div class={styles.endpointCardHead}>
                    <div class={styles.endpointTitle}>
                      <span class={styles.targetToken}>TARGET</span>
                      <strong>目标端</strong>
                    </div>
                    <NTag bordered={false} type={this.targetTableExists ? 'success' : 'warning'}>
                      {this.targetTableExists ? '使用已有表' : '将新建目标表'}
                    </NTag>
                  </div>
                  <NSpin show={this.state.target.loading || this.state.loadingDatasources}>
                    <div class={styles.endpointPanel}>
                      <div class={styles.formGrid}>
                        <div class={styles.fieldBlock}>
                          <div class={styles.fieldLabel}>数据源</div>
                          <NSelect
                            value={this.state.target.datasourceId}
                            options={datasourceSelectOptions}
                            placeholder='选择目标数据源'
                            filterable
                            clearable
                            onUpdateValue={(value) => {
                              this.state.target.datasourceId = value
                            }}
                          />
                        </div>
                        <div class={styles.fieldBlock}>
                          <div class={styles.fieldLabel}>数据库</div>
                          <NSelect
                            value={this.state.target.database}
                            options={this.targetDatabaseOptions}
                            placeholder='选择目标库'
                            filterable
                            clearable
                            onUpdateValue={(value) => {
                              this.state.target.database = value
                            }}
                          />
                        </div>
                        <div class={styles.fieldBlock}>
                          <div class={styles.fieldLabel}>Schema</div>
                          <NInput
                            value={this.state.targetSchemaName}
                            placeholder={this.targetSchemaPlaceholder}
                            onUpdateValue={(value) => {
                              this.state.targetSchemaName = value
                            }}
                          />
                        </div>
                        <div class={styles.fieldBlock}>
                          <div class={styles.fieldLabel}>目标表名</div>
                          <NInput
                            value={this.state.targetTableName}
                            placeholder='输入目标表名称'
                            onUpdateValue={(value) => {
                              this.state.targetTableName = value
                            }}
                          />
                        </div>
                      </div>
                      <div
                        class={[
                          styles.targetCheckHint,
                          this.targetTableExists ? styles.targetCheckHintExisting : ''
                        ]}
                      >
                        <span>{this.targetTableCheckText}</span>
                        <NTag bordered={false} type={this.targetTableExists ? 'success' : 'warning'}>
                          {this.targetTableExists ? '使用已有表' : '将新建'}
                        </NTag>
                      </div>
                    </div>
                  </NSpin>
                </div>
              </div>
            </div>
          </Card>
        </div>
      )
    } else if (this.state.currentStep === 2) {
      stepContent = (
        <div class={styles.solutionWorkbench} key='step-2'>
          <aside class={styles.solutionSidebar}>
            {solutionModules.map((item) => (
              <button
                key={item.key}
                class={[
                  styles.solutionModule,
                  this.state.activeSolutionModule === item.key
                    ? styles.solutionModuleActive
                    : ''
                ]}
                type='button'
                onClick={() => {
                  this.state.activeSolutionModule = item.key
                  this.$nextTick(() => {
                    this.refreshMappingLayout()
                  })
                }}
              >
                <div class={styles.solutionModuleTop}>
                  <span class={styles.solutionModuleTitle}>{item.title}</span>
                  <NTag
                    size='small'
                    bordered={false}
                    type={item.key === 'PROCESSING' ? 'default' : 'info'}
                  >
                    {item.tag}
                  </NTag>
                </div>
                <div class={styles.solutionModuleDesc}>{item.desc}</div>
              </button>
            ))}
          </aside>
          <section class={styles.solutionContent}>
            {solutionContent}
          </section>
        </div>
      )
    } else if (this.state.currentStep === 3) {
      stepContent = (
        <div class={styles.stageStack} key='step-3'>
          <Card title='执行方式'>
            <NDescriptions columns={4} labelPlacement='left' bordered size='small'>
              <NDescriptionsItem label='执行方式'>
                <NRadioGroup
                  value={this.state.executionMode}
                  onUpdateValue={(value: ExecutionMode) => {
                    this.state.executionMode = value
                  }}
                >
                  <NSpace>
                    <NRadioButton value='IMMEDIATE'>立即执行</NRadioButton>
                    <NRadioButton value='SCHEDULE'>周期调度</NRadioButton>
                  </NSpace>
                </NRadioGroup>
              </NDescriptionsItem>
              <NDescriptionsItem label='目标 Schema'>
                {this.state.targetSchemaName || this.targetSchemaPlaceholder}
              </NDescriptionsItem>
              <NDescriptionsItem label='工作流编码'>
                {this.state.latestWorkflowCode || '-'}
              </NDescriptionsItem>
              <NDescriptionsItem label='工作流状态'>
                {this.state.latestWorkflowReleaseState || '-'}
              </NDescriptionsItem>
            </NDescriptions>

            {this.state.executionMode === 'SCHEDULE' && (
              <div class={styles.schedulePanelWrap}>
                <div class={styles.schedulePanel}>
                  <div>
                    <div class={styles.scheduleTitle}>周期调度配置</div>
                    <div class={styles.hintText}>
                      这里复用同步任务专用的定时弹框，布局和原生工作流“定时”保持一致，但不再改动 Dolphin 原生页面。
                    </div>
                  </div>
                  <NSpace>
                    <NTag bordered={false} type='info'>
                      {this.state.latestScheduleSummary}
                    </NTag>
                    <NButton type='primary' ghost onClick={this.handleOpenScheduleModal}>
                      配置周期调度
                    </NButton>
                  </NSpace>
                </div>
                <div class={styles.hintText}>
                  周期调度的配置入口不再和字段设计耦合。只要项目已选择，就可以先打开调度弹框进行配置。
                </div>
              </div>
            )}
          </Card>

          <Card title='发布前动作'>
            <div class={styles.actionGrid}>
              <div class={styles.actionPanel}>
                <div class={styles.sectionTitle}>目标端建表</div>
                <div class={styles.hintText}>
                  进入本步骤后会自动生成 DDL。自动生成不等于自动建表，只有点击确认建表后才会真正下发到目标端。
                </div>
                <NSpace>
                  <NButton
                    ghost
                    loading={this.state.previewingTableDdl}
                    onClick={() => this.handlePreviewTargetTable(true)}
                  >
                    重新生成
                  </NButton>
                  <NButton ghost disabled={!this.state.latestCreateTableDdl} onClick={this.handleCopyDdl}>
                    复制 SQL
                  </NButton>
                  <NButton ghost disabled={!this.state.latestCreateTableDdl} onClick={this.handleFormatDdl}>
                    格式化
                  </NButton>
                  <NButton
                    type='primary'
                    loading={this.state.creatingTable}
                    onClick={this.handleCreateTargetTable}
                  >
                    确认建表
                  </NButton>
                </NSpace>
                <div class={styles.ddlPanel}>
                  <div class={styles.ddlHeader}>
                    <div class={styles.ddlTitleRow}>
                      <div class={styles.sectionTitle}>目标端建表 SQL</div>
                      <NTag
                        bordered={false}
                        type={this.state.latestCreateTableDdlManual ? 'warning' : 'info'}
                      >
                        {this.state.previewingTableDdl
                          ? '生成中'
                          : this.state.latestCreateTableDdlManual
                            ? '已手工修改'
                            : this.state.latestCreateTableDdl
                              ? '系统生成'
                              : '等待生成'}
                      </NTag>
                    </div>
                    <div class={styles.hintText}>
                      SQL 编辑器支持高亮、行号和直接编辑；修改后会优先使用当前编辑内容建表。
                    </div>
                  </div>
                  <MonacoEditor
                    value={this.state.latestCreateTableDdl}
                    height='360px'
                    options={{
                      language: 'sql',
                      minimap: { enabled: false },
                      wordWrap: 'on',
                      lineNumbers: 'on',
                      scrollBeyondLastLine: false,
                      fontSize: 13
                    }}
                    onUpdateValue={(value) => {
                      this.state.latestCreateTableDdl = value
                      this.state.latestCreateTableDdlManual = true
                    }}
                  />
                </div>
              </div>
              <div class={styles.actionPanel}>
                <div class={styles.sectionTitle}>保存工作流草稿</div>
                <div class={styles.hintText}>
                  如果你希望先把同步任务保存到 DolphinScheduler，再稍后运行，可以先执行保存动作。
                </div>
                <NButton
                  type='primary'
                  ghost
                  loading={this.state.savingWorkflow}
                  onClick={this.handleSaveWorkflow}
                >
                  保存同步任务
                </NButton>
              </div>
            </div>
          </Card>
        </div>
      )
    } else {
      stepContent = (
        <div class={styles.stageStack} key='step-4'>
          <Card title='同步概览' contentClass={styles.overviewCardContent}>
            <div class={styles.overviewSummaryGrid}>
              {this.summaryItems.map((item) => (
                <div class={styles.overviewSummaryItem} key={item.label}>
                  <div class={styles.overviewSummaryLabel}>{item.label}</div>
                  <div class={styles.overviewSummaryValue}>{item.value}</div>
                </div>
              ))}
            </div>
          </Card>

          <Card title='保存与运行'>
            <div class={styles.publishBar}>
              <div class={styles.sectionTitle}>发布</div>
              <NSpace>
                <NButton
                  type='primary'
                  ghost
                  loading={this.state.savingWorkflow}
                  onClick={this.handleSaveWorkflow}
                >
                  保存同步任务
                </NButton>
                <NButton
                  type='primary'
                  loading={this.state.runningWorkflow}
                  onClick={this.handleRunWorkflow}
                >
                  {this.state.executionMode === 'SCHEDULE'
                    ? '保存并启用调度'
                    : '保存并执行'}
                </NButton>
              </NSpace>
            </div>
            <div class={styles.resultPanel} style={{ marginTop: '14px' }}>
              <div class={styles.sectionTitle}>提交与运行状态</div>
              <NSpace style={{ marginTop: '12px' }}>
                <NTag bordered={false} type='info'>
                  {RUN_PROGRESS_LABELS[this.state.latestRunStage]}
                </NTag>
                {this.state.latestInstanceId ? (
                  <NTag bordered={false} type={this.state.latestInstanceStateType}>
                    实例状态：{this.state.latestInstanceStateLabel}
                  </NTag>
                ) : null}
                {this.state.latestWorkflowCode ? (
                  <NTag bordered={false}>
                    工作流编码：{this.state.latestWorkflowCode}
                  </NTag>
                ) : null}
              </NSpace>
              <div class={styles.hintText} style={{ marginTop: '10px' }}>
                {this.state.latestRunMessage}
              </div>
              {this.state.latestInstanceId ? (
                <div class={styles.summaryGrid} style={{ marginTop: '14px' }}>
                  <div class={styles.summaryItem}>
                    <div class={styles.summaryLabel}>实例 ID</div>
                    <div class={styles.summaryValue}>{this.state.latestInstanceId}</div>
                  </div>
                  <div class={styles.summaryItem}>
                    <div class={styles.summaryLabel}>开始时间</div>
                    <div class={styles.summaryValue}>
                      {this.state.latestInstanceStartTime || '-'}
                    </div>
                  </div>
                  <div class={styles.summaryItem}>
                    <div class={styles.summaryLabel}>成功节点</div>
                    <div class={styles.summaryValue}>{this.state.latestInstanceTaskSuccess}</div>
                  </div>
                  <div class={styles.summaryItem}>
                    <div class={styles.summaryLabel}>运行中 / 失败</div>
                    <div class={styles.summaryValue}>
                      {this.state.latestInstanceTaskRunning} / {this.state.latestInstanceTaskFailed}
                    </div>
                  </div>
                  <div class={styles.summaryItem}>
                    <div class={styles.summaryLabel}>同步数据量</div>
                    <div class={styles.summaryValue}>
                      {this.state.latestSyncedRowCountLoading
                        ? '统计中'
                        : this.state.latestSyncedRowCount !== null
                          ? `${this.state.latestSyncedRowCount} 行`
                          : '-'}
                    </div>
                  </div>
                </div>
              ) : null}
              {this.state.latestInstanceTaskRows.length ? (
                <div style={{ marginTop: '14px' }}>
                  <NDataTable
                    columns={this.latestInstanceTaskColumns}
                    data={this.state.latestInstanceTaskRows}
                    row-key={(row: WorkflowTaskProgressRow) => row.key}
                    size='small'
                    pagination={false}
                    striped
                  />
                </div>
              ) : null}
            </div>
          </Card>
        </div>
      )
    }

    return (
      <NSpace vertical class={styles.page}>
        <div class={styles.pageHeader}>
          <div class={styles.heroBlock}>
            <h2 class={styles.heroTitle}>
              {this.state.editingAssetId ? '编辑同步任务' : '新建同步任务'}
            </h2>
            <div class={styles.hintText}>
              {this.state.editingAssetId
                ? '已从同步任务列表回填历史配置，可修改后保存、发布或执行。'
                : '按步骤选择源端、目标端、同步方案、执行与调度。'}
            </div>
          </div>
          <div class={styles.heroActions}>
            <NButton onClick={this.backToAssetList}>返回列表</NButton>
            <NButton type='primary' ghost onClick={this.handleOpenPreview}>
              查看配置预览
            </NButton>
          </div>
        </div>

        <div class={styles.lightStepBar}>
          <NSteps current={this.state.currentStep} status='process' size='small'>
            {this.stepItems.map((item) => (
              <NStep key={item.index} title={item.title} />
            ))}
          </NSteps>
        </div>

        <div class={styles.wizardMain}>
          <div key={`stage-${this.state.currentStep}`}>
            {stepContent}
          </div>

          <div class={styles.wizardFooterBar}>
            <div class={styles.wizardFooter}>
              <NButton
                disabled={this.state.currentStep === 1}
                onClick={this.handlePrevStep}
              >
                上一步
              </NButton>
              <NSpace>
                <NButton
                  type='primary'
                  disabled={this.state.currentStep === 4}
                  onClick={this.handleNextStep}
                >
                  下一步
                </NButton>
              </NSpace>
            </div>
          </div>
        </div>

        {!this.state.datasourceOptions.length && !this.state.loadingDatasources ? (
          <Card>
            <NEmpty description='当前未发现可用的 MySQL / PostgreSQL 数据源，请先在源中心创建。' />
          </Card>
        ) : null}

        <NDrawer
          show={this.state.previewVisible}
          placement='right'
          width='40vw'
          minWidth={520}
          onUpdateShow={(value) => {
            this.state.previewVisible = value
          }}
        >
          <NDrawerContent title='SeaTunnel 配置预览' closable>
            <NSpace vertical>
              {this.syncWarnings.length ? (
                <div class={styles.warningList}>
                  {this.syncWarnings.map((item) => (
                    <NAlert type='warning' showIcon={false}>
                      {item}
                    </NAlert>
                  ))}
                </div>
              ) : null}
              <NSpace justify='space-between'>
                <div>
                  <span class={styles.sectionTitle}>自动生成结果</span>
                  <span class={styles.sectionHint}>
                    支持在这里直接修正配置，保存工作流和运行时都会优先使用这里的内容
                  </span>
                </div>
                <NSpace>
                  <NButton size='small' onClick={this.handleResetConfigEditor}>
                    恢复自动生成
                  </NButton>
                  <NButton
                    type='primary'
                    size='small'
                    onClick={this.handleCopyConfig}
                  >
                    复制配置
                  </NButton>
                </NSpace>
              </NSpace>
              <div class={styles.codeWrap}>
                <NInput
                  type='textarea'
                  autosize={{
                    minRows: 18,
                    maxRows: 28
                  }}
                  value={this.effectiveConfigText}
                  onUpdateValue={this.handleConfigEditorChange}
                />
              </div>
            </NSpace>
          </NDrawerContent>
        </NDrawer>

        <TimingModal
          v-model:row={this.state.scheduleModalRow}
          v-model:show={this.state.scheduleModalVisible}
          v-model:type={this.state.scheduleModalType}
          v-model:state={this.state.scheduleModalState}
          projectCode={this.state.selectedProjectCode}
          onUpdateList={async () => {
            if (this.state.latestWorkflowCode) {
              await this.loadScheduleMeta(this.state.latestWorkflowCode)
            }
          }}
        />
        {renderAgentDrawer()}
      </NSpace>
    )
  }
})

export default syncTask
