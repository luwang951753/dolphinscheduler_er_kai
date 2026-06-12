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
  getCurrentInstance,
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
  NDescriptionsItem,
  NSwitch,
  NTooltip,
  NIcon,
  NPopconfirm
} from 'naive-ui'
import type { DataTableColumns, SelectOption } from 'naive-ui'
import {
  ApartmentOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  ClockCircleOutlined,
  CopyOutlined,
  DeleteOutlined,
  DownloadOutlined,
  FormOutlined,
  InfoCircleFilled,
  PlayCircleOutlined,
  UploadOutlined
} from '@vicons/antd'
import { format } from 'date-fns'
import Card from '@/components/card'
import MonacoEditor from '@/components/monaco-editor'
import NodeDetailModal from '@/views/projects/task/components/node/detail-modal'
import TimingModal from './timing-modal'
import { formatParams } from '@/views/projects/task/components/node/format-data'
import type {
  INodeData,
  ITaskData
} from '@/views/projects/task/components/node/types'
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
  offline,
  online,
  queryScheduleListPaging
} from '@/service/modules/schedules'
import {
  queryWorkflowInstanceById,
  queryWorkflowInstanceListPaging,
  queryTaskListByWorkflowId
} from '@/service/modules/workflow-instances'
import { queryLog } from '@/service/modules/log'
import {
  registerGovernanceSyncTaskLineage,
  queryDataFlowSyncInstanceStats,
  upsertDataFlowSyncInstanceStat
} from '@/service/modules/data-governance'
import styles from './index.module.scss'

type SyncDatasourceType = 'MYSQL' | 'POSTGRESQL' | 'ORACLE' | 'DORIS'
type ExecutionMode = 'IMMEDIATE' | 'SCHEDULE'
type TargetTableMode = 'CREATE_TABLE' | 'EXISTING_TABLE'
type MappingKind = 'AUTO' | 'MANUAL'
type TargetNameRule = 'KEEP_SOURCE' | 'LOWERCASE' | 'UPPERCASE'
type SyncSolutionModule = 'MAPPING' | 'FILTER' | 'SINK' | 'PROCESSING'
type SyncTaskViewMode = 'LIST' | 'WIZARD'
type SyncTaskAssetStatus = 'SUCCESS' | 'FAILED' | 'RUNNING' | 'DRAFT' | 'OFFLINE'
type SyncTaskDetailTab = 'OVERVIEW' | 'CONFIG' | 'HISTORY' | 'LOGS'
type SyncTaskAssetSource = 'REAL' | 'LOCAL'
type SinkConfigTab = 'BASE' | 'MODE' | 'THROUGHPUT' | 'TEMPLATE' | 'CONFIG'
type SeaTunnelDeployMode = 'cluster' | 'client' | 'local'
type DataProcessingRuleType = 'VALUE_TRANSLATE'

type SyncTaskWorkflowActionRow = {
  code: number | null
  name: string
  releaseState: 'ONLINE' | 'OFFLINE'
  scheduleReleaseState: 'ONLINE' | 'OFFLINE'
  schedule: Record<string, any> | null
}

interface DataProcessingMapping {
  key: string
  sourceValue: string
  targetValue: string
}

interface DataProcessingRule {
  key: string
  enabled: boolean
  type: DataProcessingRuleType
  sourceField: string
  targetField: string
  defaultMode: 'KEEP_SOURCE' | 'EMPTY'
  mappings: DataProcessingMapping[]
}

interface SinkOptions {
  tab: SinkConfigTab
  schemaSaveMode: string
  dataSaveMode: string
  customSql: string
  generateSinkSql: string
  enableUpsert: string
  fieldIde: string
  batchSize: string
  maxRetries: string
  connectionCheckTimeoutSec: string
  isExactlyOnce: string
  xaDataSourceClassName: string
  maxCommitAttempts: string
  transactionTimeoutSec: string
  autoCommit: string
  batchIntervalMs: string
  jdbcProperties: string
  jdbcQuery: string
  dorisFenodes: string
  dorisQueryPort: string
  dorisLabelPrefix: string
  dorisEnable2pc: string
  dorisEnableDelete: string
  dorisBufferSize: string
  dorisBufferCount: string
  dorisBatchSize: string
  dorisCheckInterval: string
  dorisMaxRetries: string
  dorisFormat: string
  dorisReadJsonByLine: string
  dorisStripOuterArray: string
  dorisColumnSeparator: string
  dorisLoadToSingleTablet: string
  dorisNeedsUnsupportedTypeCasting: string
  dorisCaseSensitive: string
  dorisSaveModeCreateTemplate: string
}

interface SeaTunnelRunSettings {
  nodeName: string
  runFlag: 'YES' | 'NO'
  description: string
  taskPriority: string
  workerGroup: string
  environmentCode: number | null
  taskGroupName: string
  taskGroupPriority: string
  failRetryTimes: string
  failRetryInterval: string
  delayTime: string
  cpuQuota: string
  memoryMax: string
  timeoutFlag: boolean
  timeoutNotifyStrategy: string
  timeout: string
  startupScript: string
  runMode: string
  others: string
  deployMode: SeaTunnelDeployMode
  master: string
  masterUrl: string
  useCustom: boolean
  rawScript: string
  resourceList: string[]
  localParams: string
  taskGroupId: number | null
}

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
  workflowReleaseState?: 'ONLINE' | 'OFFLINE' | string
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
  sinkOptions: SinkOptions
  dataProcessingRules: DataProcessingRule[]
  fieldRows: FieldDesignRow[]
  sourceColumns: ColumnItem[]
  targetColumns: ColumnItem[]
  configText: string
  history: SyncTaskHistoryRow[]
  changes: Array<{
    time: string
    user: string
    action: string
  }>
  source?: SyncTaskAssetSource
  schedule?: Record<string, any> | null
  logLoading?: boolean
  logLoaded?: boolean
  logError?: string
  logText?: string
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

const getDefaultDorisCreateTemplate = () => `CREATE TABLE IF NOT EXISTS \`\${database}\`.\`\${table_name}\` (
  \${rowtype_primary_key},
  \${rowtype_fields}
)
ENGINE=OLAP
UNIQUE KEY (\${rowtype_primary_key})
DISTRIBUTED BY HASH (\${rowtype_primary_key})
PROPERTIES (
  "replication_allocation" = "tag.location.default: 1"
);`

const createDefaultSinkOptions = (): SinkOptions => ({
  tab: 'BASE',
  schemaSaveMode: 'CREATE_SCHEMA_WHEN_NOT_EXIST',
  dataSaveMode: 'APPEND_DATA',
  customSql: '',
  generateSinkSql: 'false',
  enableUpsert: 'true',
  fieldIde: '',
  batchSize: '1000',
  maxRetries: '0',
  connectionCheckTimeoutSec: '30',
  isExactlyOnce: 'false',
  xaDataSourceClassName: '',
  maxCommitAttempts: '3',
  transactionTimeoutSec: '-1',
  autoCommit: 'true',
  batchIntervalMs: '1000',
  jdbcProperties: '',
  jdbcQuery: '',
  dorisFenodes: '',
  dorisQueryPort: '9030',
  dorisLabelPrefix: 'dataflow_sync',
  dorisEnable2pc: 'true',
  dorisEnableDelete: 'false',
  dorisBufferSize: '100000',
  dorisBufferCount: '3',
  dorisBatchSize: '1024',
  dorisCheckInterval: '10000',
  dorisMaxRetries: '3',
  dorisFormat: 'json',
  dorisReadJsonByLine: 'true',
  dorisStripOuterArray: 'false',
  dorisColumnSeparator: ',',
  dorisLoadToSingleTablet: 'false',
  dorisNeedsUnsupportedTypeCasting: 'false',
  dorisCaseSensitive: 'false',
  dorisSaveModeCreateTemplate: getDefaultDorisCreateTemplate()
})

const cloneSinkOptions = (options?: Partial<SinkOptions> | null): SinkOptions => ({
  ...createDefaultSinkOptions(),
  ...(options || {})
})

const createDataProcessingMapping = (
  seed = Date.now(),
  sourceValue = '',
  targetValue = ''
): DataProcessingMapping => ({
  key: `processing-map-${seed}-${Math.random().toString(16).slice(2)}`,
  sourceValue,
  targetValue
})

const createDataProcessingRule = (seed = Date.now()): DataProcessingRule => ({
  key: `processing-rule-${seed}-${Math.random().toString(16).slice(2)}`,
  enabled: true,
  type: 'VALUE_TRANSLATE',
  sourceField: '',
  targetField: '',
  defaultMode: 'KEEP_SOURCE',
  mappings: [
    createDataProcessingMapping(seed + 1),
    createDataProcessingMapping(seed + 2)
  ]
})

const cloneDataProcessingRules = (
  rules?: DataProcessingRule[] | null
): DataProcessingRule[] =>
  (rules && rules.length ? rules : []).map((rule, index) => ({
    ...rule,
    key: rule.key || `processing-rule-clone-${index}-${Date.now()}`,
    type: rule.type || 'VALUE_TRANSLATE',
    defaultMode: rule.defaultMode || 'KEEP_SOURCE',
    mappings: (rule.mappings && rule.mappings.length
      ? rule.mappings
      : [createDataProcessingMapping(index)]
    ).map((mapping, mappingIndex) => ({
      ...mapping,
      key: mapping.key || `processing-map-clone-${index}-${mappingIndex}-${Date.now()}`
    }))
  }))

const createDefaultRunSettings = (): SeaTunnelRunSettings => ({
  nodeName: 'sync_task',
  runFlag: 'YES',
  description: '',
  taskPriority: 'MEDIUM',
  workerGroup: 'default',
  environmentCode: null,
  taskGroupName: '',
  taskGroupPriority: '0',
  failRetryTimes: '0',
  failRetryInterval: '1',
  delayTime: '0',
  cpuQuota: '-1',
  memoryMax: '-1',
  timeoutFlag: false,
  timeoutNotifyStrategy: 'WARN',
  timeout: '30',
  startupScript: 'seatunnel.sh',
  runMode: 'RUN',
  others: '',
  deployMode: 'local',
  master: 'YARN',
  masterUrl: '',
  useCustom: true,
  rawScript: '',
  resourceList: [],
  localParams: '',
  taskGroupId: null
})

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
    tag: '可配置',
    desc: '配置字段翻译、派生字段等 ETL 处理'
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

const buildSourceColumnsFromFieldRows = (rows: FieldDesignRow[]): ColumnItem[] => {
  const seen = new Set<string>()
  return rows
    .filter((item) => item.sourceColumn)
    .map((item, index) => ({
      name: item.sourceColumn,
      type: item.sourceType || 'unknown',
      key: `${item.sourceColumn}-${index}`,
      nullable: item.sourceNullable,
      primaryKey: item.sourcePrimaryKey,
      comment: item.sourceComment || ''
    }))
    .filter((item) => {
      const key = item.name.toLowerCase()
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
}

const buildTargetColumnsFromFieldRows = (rows: FieldDesignRow[]): ColumnItem[] =>
  rows
    .filter((item) => item.targetColumn)
    .map((item, index) => ({
      name: item.targetColumn,
      type: item.targetType || 'unknown',
      key: item.key || `${item.targetColumn}-${index}`,
      nullable: item.targetNullable,
      primaryKey: item.targetPrimaryKey,
      comment: item.targetComment || ''
    }))

const formatQualifiedPath = (...parts: Array<string | null | undefined>) =>
  parts
    .map((part) => String(part || '').trim())
    .filter((part) => part && part !== '-')
    .join('.') || '-'

const SYNC_TASK_ASSET_STORAGE_KEY = 'dolphinscheduler.sync-task.assets.v1'

const toPositiveNumber = (value: unknown): number | null => {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null
}

const escapeSeatunnelString = (value: string): string =>
  value.replaceAll('\\', '\\\\').replaceAll('"', '\\"')

const seatunnelQuotedString = (value: string): string =>
  `"${escapeSeatunnelString(value)}"`

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

interface SyncTaskHistoryRow {
  id: string
  instanceId: number | null
  status: SyncTaskAssetStatus
  state: string
  trigger: string
  startTime: string
  endTime: string
  duration: string
  rows: string
}

const normalizeHistoryRow = (row: Partial<SyncTaskHistoryRow>): SyncTaskHistoryRow => {
  const id = String(row.id || `history-${Date.now()}`)
  const instanceId = toPositiveNumber(row.instanceId || row.id)
  return {
    id,
    instanceId,
    status: row.status || 'DRAFT',
    state: row.state || '',
    trigger: row.trigger || '-',
    startTime: row.startTime || '-',
    endTime: row.endTime || '-',
    duration: row.duration || '-',
    rows: row.rows || '- / -'
  }
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
    'NUMBER(1)',
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

const isTaskLogMissingMessage = (message: string): boolean =>
  /file path: .* not exists/i.test(message)

const formatTaskLogMissingMessage = (): string =>
  '最近实例日志文件不存在，可能是历史实例或服务运行目录迁移导致。请重新运行同步任务后再查看最新日志。'

const formatTaskLogReadError = (error: any): string => {
  const message = extractErrorMessage(error, '')
  if (isTaskLogMissingMessage(message)) {
    return formatTaskLogMissingMessage()
  }
  return message || '读取任务日志失败，请稍后重试。'
}

const normalizeTaskHost = (task: any): string =>
  String(task?.host || task?.executePath || '').trim()

const formatTaskStateText = (stateValue: any): string => {
  const normalized = String(stateValue ?? '').trim()
  if (!normalized) return '未知状态'
  if (normalized === '0') return '已提交'
  return WORKFLOW_STATE_META[normalized]?.label || normalized
}

const formatTaskUndispatchedMessage = (task: any): string => {
  const workerGroup = task?.workerGroup || task?.worker_group || 'default'
  return [
    '任务尚未分发到 Worker，当前任务实例 host 为空，因此没有可读取的运行日志。',
    '',
    `当前状态：${formatTaskStateText(task?.state)}。`,
    `Worker 分组：${workerGroup}。`,
    '',
    '请重点检查：',
    '1. DolphinScheduler Worker 是否在线。',
    '2. Worker 分组是否和任务配置一致。',
    '3. 注册中心里的 Worker 地址是否为当前机器可达地址。',
    '4. 如果机器 IP 变化过，需要重启 DolphinScheduler，让 Worker 使用新地址重新注册。',
    '',
    '说明：这不是日志文件为空，而是任务还没有真正被 Worker 接收执行。'
  ].join('\n')
}

const isUsefulTaskErrorLine = (line: string): boolean => {
  const normalized = line.trim()
  return (
    normalized.length > 0 &&
    !/^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}/.test(normalized) &&
    !/^\[.*\]\s*$/.test(normalized) &&
    /(exception|error|failed|failure|caused by|cannot|not found|denied|refused|timeout|syntax|permission|ora-\d+)/i.test(normalized)
  )
}

const extractTaskFailureSummaryFromLog = (logText: string): string => {
  const lines = logText
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
  const usefulLine = lines.find(isUsefulTaskErrorLine)
  if (!usefulLine) return ''
  return usefulLine.replace(/\s+/g, ' ').slice(0, 260)
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
  const counterRegExp = new RegExp(`"?${label}"?\\s*[:=]\\s*"?([\\d,]+)"?`, 'gi')
  const matchedValues = [...logText.matchAll(counterRegExp)]
  if (!matchedValues.length) return null
  const parsedValue = Number(
    matchedValues[matchedValues.length - 1][1]?.replaceAll(',', '')
  )
  return Number.isFinite(parsedValue) ? parsedValue : null
}

const firstNumber = (...values: Array<number | null>): number | null => {
  for (const value of values) {
    if (value !== null && Number.isFinite(value)) return value
  }
  return null
}

const extractReadWriteCountFromLog = (
  logText: string
): { readRows: number | null; writeRows: number | null } => {
  const totalReadCount = extractCounterFromLog(logText, 'Total Read Count')
  const totalWriteCount = extractCounterFromLog(logText, 'Total Write Count')
  const numberTotalRows = extractCounterFromLog(logText, 'NumberTotalRows')
  const numberLoadedRows = extractCounterFromLog(logText, 'NumberLoadedRows')
  return {
    readRows: firstNumber(totalReadCount, numberTotalRows, numberLoadedRows),
    writeRows: firstNumber(totalWriteCount, numberLoadedRows, numberTotalRows)
  }
}

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

const quoteQueryTableIdentifier = (
  type: SyncDatasourceType,
  tableName: string
): string =>
  tableName
    .split('.')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => quoteQueryIdentifier(type, part))
    .join('.')

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

const hasActiveProcessingRules = (
  rows: MappingRow[],
  processingRules: DataProcessingRule[] = []
): boolean =>
  buildOrderedMappingRows(rows).some((row) =>
    processingRules.some(
      (item) =>
        item.enabled &&
        item.type === 'VALUE_TRANSLATE' &&
        item.sourceField.trim() &&
        item.targetField.trim() &&
        item.sourceField.trim().toLowerCase() === row.sourceColumn.trim().toLowerCase() &&
        item.targetField.trim().toLowerCase() === row.targetColumn.trim().toLowerCase() &&
        item.mappings.some((mapping) => mapping.sourceValue.trim() && mapping.targetValue.trim())
    )
  )

const toUtf8HexLiteral = (value: string): string => {
  const encoder = new TextEncoder()
  return [...encoder.encode(value)]
    .map((item) => item.toString(16).padStart(2, '0'))
    .join('')
    .toUpperCase()
}

const buildSourceProcessingExpression = (
  row: MappingRow,
  sourceType: SyncDatasourceType,
  processingRules: DataProcessingRule[] = []
): string => {
  const sourceColumn = quoteQueryIdentifier(sourceType, row.sourceColumn)
  const rule = processingRules.find(
    (item) =>
      item.enabled &&
      item.type === 'VALUE_TRANSLATE' &&
      item.sourceField.trim() &&
      item.targetField.trim() &&
      item.sourceField.trim().toLowerCase() === row.sourceColumn.trim().toLowerCase() &&
      item.targetField.trim().toLowerCase() === row.targetColumn.trim().toLowerCase()
  )
  if (!rule) return sourceColumn
  const mappings = rule.mappings.filter(
    (item) => item.sourceValue.trim() && item.targetValue.trim()
  )
  if (!mappings.length) return sourceColumn
  if (sourceType === 'DORIS') {
    const whenClauses = mappings
      .map((item) => {
        const sourceHex = toUtf8HexLiteral(item.sourceValue.trim())
        const targetHex = toUtf8HexLiteral(item.targetValue.trim())
        return `when HEX(${sourceColumn}) = '${sourceHex}' then CAST(UNHEX('${targetHex}') AS STRING)`
      })
      .join(' ')
    const fallback = rule.defaultMode === 'EMPTY' ? "''" : sourceColumn
    return `case ${whenClauses} else ${fallback} end`
  }
  const whenClauses = mappings
    .map(
      (item) =>
        `when ${sourceColumn} = '${escapeSqlStringLiteral(item.sourceValue.trim())}' then '${escapeSqlStringLiteral(item.targetValue.trim())}'`
    )
    .join(' ')
  const fallback = rule.defaultMode === 'EMPTY' ? "''" : sourceColumn
  return `case ${whenClauses} else ${fallback} end`
}

const buildSourceSelectWithProcessing = (
  rows: MappingRow[],
  sourceType: SyncDatasourceType,
  processingRules: DataProcessingRule[] = []
): string => {
  const orderedRows = buildOrderedMappingRows(rows)
  if (!orderedRows.length) return 'select *'
  return `select ${orderedRows
    .map((row) => {
      const expression = buildSourceProcessingExpression(row, sourceType, processingRules)
      const targetColumn = quoteQueryIdentifier(sourceType, row.targetColumn)
      const sourceColumn = quoteQueryIdentifier(sourceType, row.sourceColumn)
      return expression === sourceColumn && row.sourceColumn === row.targetColumn
        ? expression
        : `${expression} as ${targetColumn}`
    })
    .join(', ')}`
}

const buildSourceSelectSql = (
  rows: MappingRow[],
  sourceFilters: SourceFilterRule[],
  sourceType: SyncDatasourceType,
  sourceTable: string,
  sourceColumns: ColumnItem[],
  sampleLimit: number | null = null,
  processingRules: DataProcessingRule[] = []
): string => {
  const selectClause = hasActiveProcessingRules(rows, processingRules)
    ? buildSourceSelectWithProcessing(rows, sourceType, processingRules)
    : buildSourceSelectByType(rows, sourceType)
  const fromClause = buildSourceFromClause(sourceType, sourceTable)
  const whereClause = buildSourceWhereClause(sourceFilters, sourceType, sourceColumns)
  const baseSql = `${selectClause} from ${fromClause}${whereClause}`
  if (sourceType === 'ORACLE' && sampleLimit && sampleLimit > 0) {
    return `select * from (${baseSql}) WHERE ROWNUM <= ${Math.floor(sampleLimit)}`
  }
  const limitClause = buildSourceLimitClause(sourceType, sampleLimit)
  return `${baseSql}${limitClause}`
}

const buildSourceLimitClause = (
  sourceType: SyncDatasourceType,
  sampleLimit: number | null
): string => {
  if (!sampleLimit || sampleLimit <= 0) return ''
  if (sourceType === 'ORACLE') return ''
  return ` LIMIT ${Math.floor(sampleLimit)}`
}

const buildSourceFromClause = (
  sourceType: SyncDatasourceType,
  sourceTable: string
): string => {
  return quoteQueryTableIdentifier(sourceType, sourceTable)
}

const escapeSqlStringLiteral = (value: string): string =>
  value.replaceAll("'", "''")

const escapeSqlLikeLiteral = (value: string): string =>
  escapeSqlStringLiteral(value)
    .replaceAll('#', '##')
    .replaceAll('%', '#%')
    .replaceAll('_', '#_')

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
      return `${field} LIKE '%${escapeSqlLikeLiteral(value)}%' ESCAPE '#'`
    case 'PREFIX':
      return `${field} LIKE '${escapeSqlLikeLiteral(value)}%' ESCAPE '#'`
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
    return Number.isFinite(Number(trimmed)) ? trimmed : `'${escapeSqlStringLiteral(trimmed)}'`
  }
  if (family === 'boolean') {
    const normalized = trimmed.toLowerCase()
    if (['true', '1', 'yes', 'y'].includes(normalized)) return 'TRUE'
    if (['false', '0', 'no', 'n'].includes(normalized)) return 'FALSE'
    return `'${escapeSqlStringLiteral(trimmed)}'`
  }
  return `'${escapeSqlStringLiteral(trimmed)}'`
}

const describeSourceFilters = (filters: SourceFilterRule[]): string => {
  const activeCount = filters.filter((item) => item.enabled && item.field.trim()).length
  return activeCount ? `源端过滤 ${activeCount} 条` : '源端过滤未启用'
}

const buildProcessingExpression = (
  row: MappingRow,
  processingRules: DataProcessingRule[]
): string => {
  const sourceColumn = quoteSeatunnelSqlIdentifier(row.sourceColumn)
  const rule = processingRules.find(
    (item) =>
      item.enabled &&
      item.type === 'VALUE_TRANSLATE' &&
      item.sourceField.trim() &&
      item.targetField.trim() &&
      item.sourceField.trim().toLowerCase() === row.sourceColumn.trim().toLowerCase() &&
      item.targetField.trim().toLowerCase() === row.targetColumn.trim().toLowerCase()
  )
  if (!rule) return sourceColumn
  const mappings = rule.mappings.filter(
    (item) => item.sourceValue.trim() && item.targetValue.trim()
  )
  if (!mappings.length) return sourceColumn
  const whenClauses = mappings
    .map(
      (item) =>
        `when ${sourceColumn} = '${escapeSqlStringLiteral(item.sourceValue.trim())}' then '${escapeSqlStringLiteral(item.targetValue.trim())}'`
    )
    .join(' ')
  const fallback = rule.defaultMode === 'EMPTY' ? "''" : sourceColumn
  return `case ${whenClauses} else ${fallback} end`
}

const buildSeatunnelTransformQuery = (
  rows: MappingRow[],
  processingRules: DataProcessingRule[] = []
): string => {
  const orderedRows = buildOrderedMappingRows(rows)
  if (!orderedRows.length) return 'select * from sync_source'
  const selectFields = orderedRows
    .map((item) => {
      const expression = buildProcessingExpression(item, processingRules)
      const targetColumn = quoteSeatunnelSqlIdentifier(item.targetColumn)
      return expression === targetColumn ? expression : `${expression} as ${targetColumn}`
    })
    .join(', ')
  return `select ${selectFields} from sync_source`
}

const buildSeatunnelPassthroughTransformQuery = (rows: MappingRow[]): string => {
  const orderedRows = buildOrderedMappingRows(rows)
  if (!orderedRows.length) return 'select * from sync_source'
  const selectFields = orderedRows
    .map((item) => quoteSeatunnelSqlIdentifier(item.targetColumn))
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

const getSinkConnectorName = (type: SyncDatasourceType): string => {
  if (type === 'DORIS') return 'Doris'
  return 'Jdbc'
}

const getJdbcFieldIde = (type: SyncDatasourceType, options: SinkOptions): string =>
  options.fieldIde || (type === 'ORACLE' ? 'UPPERCASE' : 'ORIGINAL')

const getXaDataSourceClassName = (type: SyncDatasourceType, options: SinkOptions): string => {
  if (options.xaDataSourceClassName.trim()) return options.xaDataSourceClassName.trim()
  if (type === 'MYSQL') return 'com.mysql.cj.jdbc.MysqlXADataSource'
  if (type === 'ORACLE') return 'oracle.jdbc.xa.client.OracleXADataSource'
  return ''
}

const getDefaultJdbcProperties = (type: SyncDatasourceType): string => {
  if (type === 'MYSQL') return 'rewriteBatchedStatements=true\nuseServerPrepStmts=false'
  if (type === 'ORACLE') return 'oracle.jdbc.timezoneAsRegion=false'
  return ''
}

const parseKeyValueLines = (value: string): Array<[string, string]> =>
  String(value || '')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const index = line.indexOf('=')
      return (index >= 0
        ? [line.slice(0, index).trim(), line.slice(index + 1).trim()]
        : [line, '']) as [string, string]
    })
    .filter(([key]) => !!key)

const parseSeaTunnelLocalParams = (value: string) =>
  parseKeyValueLines(value).map(([prop, direct]) => ({
    prop,
    direct,
    type: 'VARCHAR'
  }))

const normalizeNumberText = (value: string, fallback: string) => {
  const trimmed = String(value ?? '').trim()
  return trimmed || fallback
}

const resolveRunNodeName = (settings: SeaTunnelRunSettings, fallbackName: string) => {
  const nodeName = settings.nodeName.trim()
  return !nodeName || nodeName === 'sync_task' ? fallbackName : nodeName
}

const buildSeaTunnelTaskData = (
  settings: SeaTunnelRunSettings,
  fallbackName: string,
  rawScript: string,
  code = 0
): ITaskData => ({
  code,
  delayTime: Number(normalizeNumberText(settings.delayTime, '0')),
  description: settings.description,
  environmentCode: settings.environmentCode ?? -1,
  failRetryInterval: Number(normalizeNumberText(settings.failRetryInterval, '1')),
  failRetryTimes: Number(normalizeNumberText(settings.failRetryTimes, '0')),
  flag: settings.runFlag || 'YES',
  name: resolveRunNodeName(settings, fallbackName),
  taskGroupId: settings.taskGroupId ?? undefined,
  taskGroupPriority: settings.taskGroupPriority.trim()
    ? Number(settings.taskGroupPriority)
    : undefined,
  taskParams: {
    localParams: parseSeaTunnelLocalParams(settings.localParams),
    rawScript: settings.useCustom ? rawScript : settings.rawScript,
    resourceList: settings.useCustom
      ? []
      : settings.resourceList.map((resourceName) => ({ resourceName })),
    startupScript: settings.startupScript || 'seatunnel.sh',
    useCustom: settings.useCustom,
    runMode: settings.runMode || 'RUN',
    others: settings.others || '',
    deployMode: settings.deployMode || 'local',
    master: settings.master || 'YARN',
    masterUrl: settings.masterUrl || ''
  },
  taskPriority: settings.taskPriority || 'MEDIUM',
  taskType: 'SEATUNNEL',
  timeout: settings.timeoutFlag ? Number(normalizeNumberText(settings.timeout, '30')) : 0,
  timeoutFlag: settings.timeoutFlag ? 'OPEN' : 'CLOSE',
  timeoutNotifyStrategy: settings.timeoutFlag ? settings.timeoutNotifyStrategy : '',
  workerGroup: settings.workerGroup || 'default',
  cpuQuota: Number(normalizeNumberText(settings.cpuQuota, '-1')),
  memoryMax: Number(normalizeNumberText(settings.memoryMax, '-1')),
  taskExecuteType: 'BATCH'
})

const serializeLocalParams = (localParams: any[]) =>
  (localParams || [])
    .map((item) => {
      const prop = String(item?.prop || '').trim()
      if (!prop) return ''
      return `${prop}=${String(item?.value ?? '')}`
    })
    .filter(Boolean)
    .join('\n')

const applyNativeSeaTunnelModel = (
  settings: SeaTunnelRunSettings,
  model: INodeData
) => {
  settings.nodeName = model.name || settings.nodeName
  settings.runFlag = model.flag || 'YES'
  settings.description = model.description || ''
  settings.taskPriority = model.taskPriority || 'MEDIUM'
  settings.workerGroup = model.workerGroup || 'default'
  settings.environmentCode = model.environmentCode ?? null
  settings.taskGroupId = model.taskGroupId ?? null
  settings.taskGroupPriority =
    model.taskGroupPriority === null || model.taskGroupPriority === undefined
      ? ''
      : String(model.taskGroupPriority)
  settings.failRetryTimes = String(model.failRetryTimes ?? 0)
  settings.failRetryInterval = String(model.failRetryInterval ?? 1)
  settings.delayTime = String(model.delayTime ?? 0)
  settings.cpuQuota = String(model.cpuQuota ?? -1)
  settings.memoryMax = String(model.memoryMax ?? -1)
  settings.timeoutFlag = !!model.timeoutFlag
  settings.timeoutNotifyStrategy = Array.isArray(model.timeoutNotifyStrategy)
    ? model.timeoutNotifyStrategy.length === 2
      ? 'WARNFAILED'
      : model.timeoutNotifyStrategy[0] || 'WARN'
    : 'WARN'
  settings.timeout = String(model.timeout ?? 30)
  settings.startupScript = model.startupScript || 'seatunnel.sh'
  settings.runMode = model.runMode || 'RUN'
  settings.others = model.others || ''
  settings.deployMode = (model.deployMode as SeaTunnelDeployMode) || 'local'
  settings.master = model.master || 'YARN'
  settings.masterUrl = model.masterUrl || ''
  settings.useCustom = model.useCustom !== false
  settings.rawScript = model.rawScript || ''
  settings.resourceList = model.resourceList || []
  settings.localParams = serializeLocalParams(model.localParams || [])
}

const pushPropertiesBlock = (lines: string[], value: string) => {
  const entries = parseKeyValueLines(value)
  if (!entries.length) return
  lines.push('    properties = {')
  entries.forEach(([key, val]) => {
    lines.push(`      ${key} = ${seatunnelQuotedString(val)}`)
  })
  lines.push('    }')
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

const buildSinkConfigLines = (
  targetDetail: DatasourceDetail,
  databaseName: string,
  sinkTable: string,
  rows: MappingRow[],
  primaryKeys: string[],
  options: SinkOptions
): string[] => {
  const targetType = targetDetail.type
  const connectorName = getSinkConnectorName(targetType)
  const sinkInsertQuery = buildJdbcSinkInsertQuery(rows, sinkTable)
  const customSql = options.customSql.trim()
  const lines = [
    'sink {',
    `  ${connectorName} {`,
    '    source_table_name = "sync_mapped"'
  ]

  if (targetType === 'DORIS') {
    lines.push(
      `    fenodes = ${seatunnelQuotedString(options.dorisFenodes.trim() || `${targetDetail.host}:8030`)}`,
      `    query-port = ${options.dorisQueryPort || targetDetail.port || 9030}`,
      `    username = ${seatunnelQuotedString(targetDetail.userName)}`,
      `    password = ${seatunnelQuotedString(targetDetail.password)}`,
      `    database = ${seatunnelQuotedString(databaseName)}`,
      `    table = ${seatunnelQuotedString(sinkTable)}`,
      `    table.identifier = ${seatunnelQuotedString(`${databaseName}.${sinkTable}`)}`,
      `    sink.label-prefix = ${seatunnelQuotedString(options.dorisLabelPrefix || 'dataflow_sync')}`,
      `    sink.enable-2pc = ${options.dorisEnable2pc}`,
      `    sink.enable-delete = ${options.dorisEnableDelete}`,
      `    sink.buffer-count = ${options.dorisBufferCount}`,
      `    doris.batch.size = ${options.dorisBatchSize}`,
      `    sink.check-interval = ${options.dorisCheckInterval}`,
      `    sink.max-retries = ${options.dorisMaxRetries}`,
      `    needs_unsupported_type_casting = ${options.dorisNeedsUnsupportedTypeCasting}`,
      `    case_sensitive = ${options.dorisCaseSensitive}`,
      `    schema_save_mode = ${seatunnelQuotedString(options.schemaSaveMode)}`,
      `    data_save_mode = ${seatunnelQuotedString(options.dataSaveMode)}`
    )
    if (options.dorisEnable2pc !== 'true') {
      lines.splice(12, 0, `    sink.buffer-size = ${options.dorisBufferSize}`)
    }
    if (customSql && options.dataSaveMode === 'CUSTOM_PROCESSING') {
      lines.push(`    custom_sql = ${seatunnelQuotedString(customSql)}`)
    }
    if (options.schemaSaveMode !== 'ERROR_WHEN_SCHEMA_NOT_EXIST') {
      lines.push('    save_mode_create_template = """')
      lines.push(...options.dorisSaveModeCreateTemplate.split('\n').map((line) => `      ${line}`))
      lines.push('    """')
    }
    lines.push('    doris.config = {')
    lines.push(`      format = ${seatunnelQuotedString(options.dorisFormat)}`)
    if (options.dorisFormat === 'json') {
      lines.push(`      read_json_by_line = ${seatunnelQuotedString(options.dorisReadJsonByLine)}`)
      lines.push(`      strip_outer_array = ${seatunnelQuotedString(options.dorisStripOuterArray)}`)
    } else {
      lines.push(`      column_separator = ${seatunnelQuotedString(options.dorisColumnSeparator)}`)
    }
    lines.push(`      load_to_single_tablet = ${seatunnelQuotedString(options.dorisLoadToSingleTablet)}`)
    lines.push('    }')
  } else {
    const properties = options.jdbcProperties.trim() || getDefaultJdbcProperties(targetType)
    lines.push(
      `    url = ${seatunnelQuotedString(buildJdbcUrl(targetDetail, databaseName))}`,
      `    driver = ${seatunnelQuotedString(buildDriver(targetType))}`,
      `    user = ${seatunnelQuotedString(targetDetail.userName)}`,
      `    password = ${seatunnelQuotedString(targetDetail.password)}`,
      `    database = ${seatunnelQuotedString(databaseName)}`,
      `    table = ${seatunnelQuotedString(sinkTable)}`,
      `    schema_save_mode = ${seatunnelQuotedString(options.schemaSaveMode)}`,
      `    data_save_mode = ${seatunnelQuotedString(options.dataSaveMode)}`,
      `    enable_upsert = ${options.enableUpsert}`,
      `    field_ide = ${seatunnelQuotedString(getJdbcFieldIde(targetType, options))}`,
      `    batch_size = ${options.batchSize}`,
      `    max_retries = ${options.maxRetries}`,
      `    connection_check_timeout_sec = ${options.connectionCheckTimeoutSec}`,
      `    is_exactly_once = ${options.isExactlyOnce}`,
      `    auto_commit = ${options.autoCommit}`
    )
    const explicitSinkQuery = options.jdbcQuery.trim() || sinkInsertQuery
    if (explicitSinkQuery) {
      lines.splice(7, 0, `    query = ${seatunnelQuotedString(explicitSinkQuery)}`)
    } else {
      lines.splice(7, 0, `    generate_sink_sql = ${options.generateSinkSql}`)
    }
    if (targetType === 'ORACLE') {
      lines.splice(lines.length - 5, 0, `    batch_interval_ms = ${options.batchIntervalMs}`)
    }
    if (options.isExactlyOnce === 'true') {
      const xaClassName = getXaDataSourceClassName(targetType, options)
      if (xaClassName) {
        lines.push(
          `    xa_data_source_class_name = ${seatunnelQuotedString(xaClassName)}`,
          `    max_commit_attempts = ${options.maxCommitAttempts}`,
          `    transaction_timeout_sec = ${options.transactionTimeoutSec}`
        )
      }
    }
    if (customSql && options.dataSaveMode === 'CUSTOM_PROCESSING') {
      lines.push(`    custom_sql = ${seatunnelQuotedString(customSql)}`)
    }
    if (primaryKeys.length) {
      lines.push(`    primary_keys = [${primaryKeys.map(seatunnelQuotedString).join(', ')}]`)
    }
    pushPropertiesBlock(lines, properties)
  }

  lines.push('  }', '}')
  return lines
}

const getDefaultSchemaName = (targetType?: SyncDatasourceType): string => {
  if (targetType === 'POSTGRESQL') return 'public'
  return ''
}

const getDefaultSchemaNameByDetail = (
  targetType?: SyncDatasourceType,
  detail?: DatasourceDetail | null
): string => {
  if (targetType === 'ORACLE') {
    return (detail?.userName || '').trim().toUpperCase()
  }
  return getDefaultSchemaName(targetType)
}

const getDatasourceDefaultSchema = (
  datasourceId: number | null,
  datasourceType: SyncDatasourceType | undefined,
  details: Record<number, DatasourceDetail>
): string => getDefaultSchemaNameByDetail(
  datasourceType,
  datasourceId ? details[datasourceId] : null
)

const isScheduleOnline = (summary?: string): boolean =>
  String(summary || '').trim().toUpperCase().startsWith('ONLINE')

const getSchemaPlaceholder = (targetType?: SyncDatasourceType): string => {
  if (targetType === 'ORACLE') return '默认使用数据源用户名'
  if (targetType === 'POSTGRESQL') return 'public'
  return '可选'
}

const getSinkTabOptions = (targetType?: SyncDatasourceType) => {
  if (targetType === 'DORIS') {
    return [
      { label: '基础写入配置', value: 'BASE' },
      { label: '写入模式', value: 'MODE' },
      { label: '吞吐与提交', value: 'THROUGHPUT' },
      { label: '建表模板', value: 'TEMPLATE' },
      { label: 'doris.config 参数', value: 'CONFIG' }
    ]
  }
  return [
    { label: '连接与目标表', value: 'BASE' },
    { label: '写入模式', value: 'MODE' },
    { label: '吞吐与事务', value: 'THROUGHPUT' },
    { label: 'JDBC 扩展参数', value: 'CONFIG' }
  ]
}

const normalizeSinkTab = (
  tab: SinkConfigTab,
  targetType?: SyncDatasourceType
): SinkConfigTab => {
  const values = getSinkTabOptions(targetType).map((item) => item.value)
  return values.includes(tab) ? tab : 'BASE'
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

const getUnmappedRequiredTargetColumns = (
  targetMode: TargetTableMode,
  rows: FieldDesignRow[]
): string[] => {
  if (targetMode !== 'EXISTING_TABLE') {
    return []
  }
  return rows
    .filter((item) =>
      (item.targetPrimaryKey || item.targetNullable === false) &&
      (!item.sync || !item.sourceColumn.trim() || !item.mappedTargetKey)
    )
    .map((item) => item.targetColumn || item.key)
    .filter(Boolean)
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
  const code = toPositiveNumber(definition?.code)
  if (!code) return null

  return {
    code,
    version: toPositiveNumber(definition?.version) || 1,
    releaseState: definition?.releaseState || '-',
    name: definition?.name || ''
  }
}

const findWorkflowDefinitionMetaByName = async (
  projectCode: number,
  workflowName: string
): Promise<ReturnType<typeof extractWorkflowDefinitionMeta>> => {
  if (!toPositiveNumber(projectCode)) return null
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
  sinkOptions: SinkOptions
  dataProcessingRules: DataProcessingRule[]
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
      sinkOptions: createDefaultSinkOptions(),
      dataProcessingRules: [],
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
  const sinkOptions = cloneSinkOptions({
    customSql: sinkCustomSql,
    dataSaveMode: sinkCustomSql ? 'CUSTOM_PROCESSING' : 'APPEND_DATA'
  })
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
    sinkOptions,
    dataProcessingRules: [],
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
  const sourceTypeArgs = extractTypeArgs(sourceType)
  const [sourcePrecision = 0, sourceScale = 0] = sourceTypeArgs

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
    if (targetType === 'ORACLE') return 'NUMBER(1)'
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
  if (normalized.includes('decimal') || normalized.includes('numeric') || normalized.includes('number')) {
    if (sourceScale > 0) {
      if (targetType === 'ORACLE') return 'NUMBER(10,2)'
      return targetType === 'POSTGRESQL' ? 'NUMERIC(10,2)' : 'DECIMAL(10,2)'
    }
    if (normalized.includes('number') && sourcePrecision > 10) {
      if (targetType === 'ORACLE') return 'NUMBER(19)'
      return 'BIGINT'
    }
    if (normalized.includes('decimal') || normalized.includes('numeric')) {
      if (targetType === 'ORACLE') return 'NUMBER(10,2)'
      return targetType === 'POSTGRESQL' ? 'NUMERIC(10,2)' : 'DECIMAL(10,2)'
    }
    if (normalized.includes('number')) {
      if (targetType === 'ORACLE') return 'NUMBER(38,10)'
      return targetType === 'POSTGRESQL' ? 'NUMERIC(38,10)' : 'DECIMAL(38,10)'
    }
  }
  if (
    normalized.includes('int') ||
    normalized.includes('serial')
  ) {
    if (targetType === 'ORACLE') return 'NUMBER(10)'
    return targetType === 'POSTGRESQL' ? 'INTEGER' : 'INT'
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
    const sourceLength = sourceTypeArgs[0]
    const normalizedLength =
      Number.isFinite(sourceLength) && sourceLength > 0 ? sourceLength : 255
    if (targetType === 'ORACLE') return `VARCHAR2(${Math.min(normalizedLength, 4000)})`
    return `VARCHAR(${Math.min(normalizedLength, 65535)})`
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
  if (normalized.includes('number(')) {
    const [, scale = 0] = extractTypeArgs(type)
    if (scale > 0) return 'decimal'
    if (normalized.includes('number(19)')) return 'int64'
    return 'int32'
  }
  if (normalized.includes('bigint')) return 'int64'
  if (
    normalized.includes('int') ||
    normalized.includes('serial')
  ) {
    return 'int32'
  }
  if (normalized === 'number') return 'decimal'
  if (
    normalized.includes('decimal') ||
    normalized.includes('numeric')
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
  return extractTypeArgs(type)[0] || 0
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
      if (sourceLength > 0 && targetLength > 0 && targetLength < sourceLength) {
        return {
          ok: false,
          reason: '目标字符长度小于源字段长度'
        }
      }
    }
    if (sourceFamily === 'decimal') {
      const [sourcePrecision = 10, sourceScale = 0] = extractTypeArgs(sourceType)
      const targetArgs = extractTypeArgs(targetType)
      if (!targetArgs.length && targetType.trim().toLowerCase() === 'number') {
        return { ok: true }
      }
      const [targetPrecision = 10, targetScale = 0] = targetArgs
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
    if (sourceType.trim().toLowerCase() === 'number') {
      return { ok: true }
    }
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
    const instance = getCurrentInstance()
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
      assetHistoryLoading: false,
      assetHistoryLoadedKey: '',
      assetHistoryError: '',
      assetLogInstanceId: null as number | null,
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
      targetSchemaName: '',
      taskName: '',
      taskNameTouched: false,
      sourceFilters: [createSourceFilterRule()] as SourceFilterRule[],
      activeSolutionModule: 'MAPPING' as SyncSolutionModule,
      sinkCustomSql: '',
      sinkOptions: createDefaultSinkOptions(),
      dataProcessingRules: [createDataProcessingRule()] as DataProcessingRule[],
      previewVisible: false,
      configEditorText: '',
      configManualOverride: false,
      runNodeData: null as ITaskData | null,
      runNodeModalKey: 0,
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
      scheduleModalKey: 0,
      schedulingAssetId: '',
      latestScheduleSummary: '未配置' as string,
      runSettingsVisible: false,
      runSettings: createDefaultRunSettings()
    })
    const mappingWorkbenchRef = ref<HTMLElement | null>(null)
    const mappingAnchorPositions = ref<Record<string, { x: number; y: number }>>({})
    const draggingMapping = ref<{
      side: 'source' | 'target'
      key: string
    } | null>(null)
    const mappingDraftPoint = ref<{ x: number; y: number } | null>(null)
    let latestInstancePollingTimer: number | null = null
    let latestInstancePollingErrorCount = 0
    let assetFilterRefreshTimer: number | null = null

    const refreshScheduleAssetState = async () => {
      if (!state.latestWorkflowCode) return
      const scheduleRow = await loadScheduleMeta(state.latestWorkflowCode)
      if (state.schedulingAssetId) {
        const asset = state.syncTaskAssets.find(
          (item) => item.id === state.schedulingAssetId
        )
        if (asset) {
          asset.schedule = scheduleRow
          asset.scheduleStatus =
            scheduleRow?.releaseState === 'ONLINE' ? 'ON' : 'OFF'
          asset.updatedAt = format(new Date(), 'yyyy-MM-dd HH:mm')
        }
      }
    }

    const showScheduleModal = () => {
      state.scheduleModalKey += 1
      state.scheduleModalVisible = true
    }

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
      if (unmappedRequiredTargetColumns.value.length) {
        warnings.push(
          `已有目标表存在未映射的必填字段：${unmappedRequiredTargetColumns.value.slice(0, 5).join('、')}。`
        )
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

      const sourceColumns = state.source.columns.length
        ? state.source.columns
        : buildSourceColumnsFromFieldRows(state.fieldRows)

      return sourceColumns.map((sourceColumn) => {
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
      const columns = state.source.columns.length
        ? state.source.columns
        : buildSourceColumnsFromFieldRows(state.fieldRows)
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

    const assetProjectOptions = computed(() =>
      Array.from(new Set(state.syncTaskAssets.map((item) => item.projectName).filter(Boolean)))
        .map((item) => ({ label: item, value: item }))
    )

    const assetTypeOptions = computed(() =>
      SUPPORT_TYPES.map((item) => ({ label: item, value: item }))
    )

    const assetStatusOptions = computed(() => [
      { label: '成功', value: 'SUCCESS' },
      { label: '失败', value: 'FAILED' },
      { label: '运行中', value: 'RUNNING' },
      { label: '草稿', value: 'DRAFT' },
      { label: '下线', value: 'OFFLINE' }
    ])

    const assetScheduleOptions = computed(() => [
      { label: '已调度', value: 'ON' },
      { label: '未调度', value: 'OFF' }
    ])

    const filteredAssets = computed(() => {
      return state.syncTaskAssets.filter((item) => {
        const matchedStatus =
          !state.assetStatusFilter || item.status === state.assetStatusFilter
        const matchedSchedule =
          !state.assetScheduleFilter || item.scheduleStatus === state.assetScheduleFilter
        const matchedType =
          !state.assetTypeFilter ||
          item.sourceType === state.assetTypeFilter ||
          item.targetType === state.assetTypeFilter
        return (
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
      const projectCode = toPositiveNumber(project.value)
      const workflowCode = toPositiveNumber(workflow?.code)
      const pathMeta = inferSyncAssetPath(workflow)
      let workflowDetail: any = null
      let designFromRawScript = buildAssetDesignFromRawScript('', pathMeta)
      if (projectCode && workflowCode) {
        try {
          workflowDetail = await queryWorkflowDefinitionByCode(workflowCode, projectCode)
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
      let latestFailureMessage = ''
      if (projectCode && workflowCode) {
        try {
          const instanceResult = await queryWorkflowInstanceListPaging(
            {
              pageNo: 1,
              pageSize: 1,
              workflowDefinitionCode: workflowCode,
              searchVal: ''
            },
            projectCode
          )
          latestInstance = normalizeList(instanceResult)[0] || null
        } catch (err) {
          latestInstance = null
        }
      }
      const latestInstanceId = toPositiveNumber(latestInstance?.id)
      if (projectCode && latestInstanceId && latestInstance?.state && TERMINAL_WORKFLOW_STATES.has(latestInstance.state)) {
        try {
          const taskResult = await queryTaskListByWorkflowId(
            latestInstanceId,
            projectCode
          )
          for (const task of normalizeList(taskResult?.taskList || taskResult)) {
            if (
              !latestFailureMessage &&
              ['FAILURE', 'STOP', 'PAUSE'].includes(task?.state)
            ) {
              latestFailureMessage = `${task?.name || task?.taskName || '同步任务'} 执行失败，请进入详情查看日志诊断。`
            }
            if (latestInstance.state === 'SUCCESS' && task?.state === 'SUCCESS') {
              const taskInstanceId = toPositiveNumber(task?.id)
              if (!taskInstanceId) continue
              const taskLogText = await queryTaskLogText(taskInstanceId, 12)
              const taskCounts = extractReadWriteCountFromLog(taskLogText)
              if (taskCounts.readRows !== null) {
                latestReadRows = (latestReadRows || 0) + taskCounts.readRows
              }
              if (taskCounts.writeRows !== null) {
                latestWriteRows = (latestWriteRows || 0) + taskCounts.writeRows
              }
            }
          }
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
          : instanceState === 'FAILURE' || instanceState === 'STOP' || instanceState === 'PAUSE'
            ? 'FAILED'
            : instanceState === 'SUCCESS'
              ? 'SUCCESS'
              : workflow?.releaseState === 'OFFLINE'
                ? 'OFFLINE'
                : 'OFFLINE'
      let scheduleRow = workflow?.schedule || null
      if (projectCode && workflowCode) {
        try {
          const scheduleList = await queryScheduleListPaging(
            {
              pageNo: 1,
              pageSize: 20,
              searchVal: '',
              workflowDefinitionCode: workflowCode
            },
            projectCode
          )
          scheduleRow = normalizeList(scheduleList)[0] || scheduleRow
        } catch (err) {
          scheduleRow = workflow?.schedule || null
        }
      }
      const scheduleOnline =
        workflow?.scheduleReleaseState === 'ONLINE' ||
        scheduleRow?.releaseState === 'ONLINE'
      const updatedAt = normalizeDateText(workflow?.updateTime || workflow?.createTime)
      const lastRunTime = normalizeDateText(
        latestInstance?.startTime || latestInstance?.submitTime || workflow?.updateTime
      )

      return {
        id: `workflow-${project.value}-${workflowCode || workflow?.id || workflow?.name}`,
        name: workflow?.name || '-',
        projectCode,
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
        workflowReleaseState: workflow?.releaseState || 'OFFLINE',
        lastRunTime,
        lastInstanceId: latestInstanceId,
        readRows: latestReadRows,
        writeRows: latestWriteRows,
        duration: '-',
        updatedAt,
        owner: workflow?.userName || workflow?.modifyBy || 'admin',
        errorMessage: status === 'FAILED'
          ? latestFailureMessage || '最近一次实例执行失败，请进入详情查看日志诊断。'
          : '',
        sourceFilters: cloneSourceFilters(
          designFromRawScript.sourceFilters.length
            ? designFromRawScript.sourceFilters
            : [createSourceFilterRule()]
        ),
        sinkCustomSql: designFromRawScript.sinkCustomSql,
        sinkOptions: cloneSinkOptions(designFromRawScript.sinkOptions),
        dataProcessingRules: cloneDataProcessingRules(designFromRawScript.dataProcessingRules),
        fieldRows: cloneFieldRows(designFromRawScript.fieldRows),
        sourceColumns: cloneColumns(designFromRawScript.sourceColumns),
        targetColumns: cloneColumns(designFromRawScript.targetColumns),
        configText: designFromRawScript.configText,
        history: latestInstance
          ? [
              {
                id: String(latestInstance.id),
                instanceId: latestInstanceId,
                status,
                state: instanceState,
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
        source: 'REAL',
        schedule: scheduleRow
      }
    }

    const loadSyncTaskAssets = async () => {
      if (state.loadingAssets || !state.projectOptions.length) return
      const keyword = state.assetKeyword.trim()
      const projectFilter = state.assetProjectFilter
      const projects = projectFilter
        ? state.projectOptions.filter((item) => item.label === projectFilter)
        : state.projectOptions
      state.loadingAssets = true
      try {
        const assetGroups = await Promise.all(
          projects.map(async (project) => {
            try {
              const response = await queryWorkflowDefinitionListPaging(
                {
                  pageNo: 1,
                  pageSize: 200,
                  searchVal: keyword
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
        state.syncTaskAssets = realAssets
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

    const unmappedRequiredTargetColumns = computed(() =>
      getUnmappedRequiredTargetColumns(targetTableMode.value, state.fieldRows)
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
        null,
        state.dataProcessingRules
      )
      const transformQuery = hasActiveProcessingRules(state.fieldRows, state.dataProcessingRules)
        ? buildSeatunnelPassthroughTransformQuery(state.fieldRows)
        : buildSeatunnelTransformQuery(state.fieldRows, state.dataProcessingRules)
      const primaryKeys = buildPrimaryKeys(state.fieldRows)
      const targetType = targetDetail.type
      const targetSchema =
        state.targetSchemaName.trim() ||
        getDefaultSchemaNameByDetail(targetType, targetDetail)
      const sinkTable = buildSinkTable(
        targetType,
        state.target.database,
        targetSchema,
        state.targetTableName.trim()
      )

      const lines = [
        'env {',
        '  execution.parallelism = 1',
        '  job.mode = "BATCH"',
        '}',
        '',
        'source {',
        '  Jdbc {',
        `    url = ${seatunnelQuotedString(buildJdbcUrl(sourceDetail, state.source.database))}`,
        `    driver = ${seatunnelQuotedString(buildDriver(sourceDetail.type))}`,
        `    user = ${seatunnelQuotedString(sourceDetail.userName)}`,
        `    password = ${seatunnelQuotedString(sourceDetail.password)}`,
        `    query = ${seatunnelQuotedString(sourceQuery)}`,
        '    result_table_name = "sync_source"',
        '  }',
        '}',
        '',
        'transform {',
        '  Sql {',
        '    source_table_name = "sync_source"',
        '    result_table_name = "sync_mapped"',
        `    query = ${seatunnelQuotedString(transformQuery)}`,
        '  }',
        '}',
        '',
        ...buildSinkConfigLines(
          targetDetail,
          state.target.database,
          sinkTable,
          state.fieldRows,
          primaryKeys,
          state.sinkOptions
        )
      ]
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
      if (assetFilterRefreshTimer) {
        window.clearTimeout(assetFilterRefreshTimer)
      }
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
      state.projectOptions = normalizeList(res)
        .map((item) => ({
          label: item.name,
          value: toPositiveNumber(item.code)
        }))
        .filter((item): item is ProjectOption => !!item.label && !!item.value)
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
          const rowSync = oldRow?.sync ?? true
          const targetKey = oldRow?.mappedTargetKey || sourceColumn.name
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
            sync: rowSync,
            mappedTargetKey: rowSync ? targetKey : null,
            mappingKind: rowSync
              ? oldRow?.mappingKind || 'AUTO'
              : undefined,
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

    const loadColumns = async (
      endpoint: EndpointState,
      options: { preserveExisting?: boolean } = {}
    ) => {
      if (!endpoint.datasourceId || !endpoint.database || !endpoint.table) return
      endpoint.loading = true
      try {
        const res = await getDatasourceTableColumnMetasById(
          endpoint.datasourceId,
          endpoint.database,
          endpoint.table
        )
        const columns = normalizeColumnList(res)
        if (columns.length || !options.preserveExisting || !endpoint.columns.length) {
          endpoint.columns = columns
        }
        if (!columns.length && options.preserveExisting && endpoint.columns.length) {
          window.$message.warning('实时读取字段为空，已保留当前同步任务保存的字段映射。')
        }
      } catch (err) {
        if (!options.preserveExisting || !endpoint.columns.length) {
          endpoint.columns = []
        }
        window.$message.error(
          options.preserveExisting && endpoint.columns.length
            ? '读取字段列表失败，已保留当前同步任务保存的字段映射。'
            : '读取字段列表失败，请确认目标表存在且当前账号有查询权限。'
        )
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

    const buildMappingPath = (
      startPoint: { x: number; y: number },
      endPoint: { x: number; y: number }
    ) => {
      const distance = Math.max(endPoint.x - startPoint.x, 40)
      const laneLeft = startPoint.x + Math.min(Math.max(distance * 0.2, 24), 56)
      const laneRight = endPoint.x - Math.min(Math.max(distance * 0.2, 24), 56)
      const laneCenter = startPoint.x + distance / 2
      return [
        `M ${startPoint.x} ${startPoint.y}`,
        `L ${laneLeft} ${startPoint.y}`,
        `C ${laneCenter} ${startPoint.y}, ${laneCenter} ${endPoint.y}, ${laneRight} ${endPoint.y}`,
        `L ${endPoint.x} ${endPoint.y}`
      ].join(' ')
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

    const mapWorkflowStateToAssetStatus = (stateValue: string): SyncTaskAssetStatus => {
      if (stateValue === 'SUCCESS') return 'SUCCESS'
      if (['FAILURE', 'STOP', 'PAUSE'].includes(stateValue)) return 'FAILED'
      if (['RUNNING_EXECUTION', 'SUBMITTED_SUCCESS', 'SERIAL_WAIT'].includes(stateValue)) {
        return 'RUNNING'
      }
      return 'DRAFT'
    }

    const loadAssetHistory = async (asset: SyncTaskAsset | null) => {
      if (!asset) return
      const projectCode = toPositiveNumber(asset.projectCode)
      const workflowCode = toPositiveNumber(asset.workflowCode)
      if (!projectCode || !workflowCode) {
        asset.history = []
        return
      }
      const historyKey = `${projectCode}-${workflowCode}`
      if (state.assetHistoryLoadedKey === historyKey && asset.history.length) {
        void refreshAssetHistoryReadWriteCounts(asset)
        return
      }
      state.assetHistoryLoading = true
      state.assetHistoryError = ''
      try {
        const result = await queryWorkflowInstanceListPaging(
          {
            pageNo: 1,
            pageSize: 50,
            workflowDefinitionCode: workflowCode,
            searchVal: ''
          },
          projectCode
        )
        const rows: SyncTaskHistoryRow[] = normalizeList(result).map((item, index) => {
          const instanceId = Number(item?.id)
          const stateValue = String(item?.state || '')
          return {
            id: String(item?.id || `instance-${index}-${Date.now()}`),
            instanceId: Number.isFinite(instanceId) && instanceId > 0 ? instanceId : null,
            status: mapWorkflowStateToAssetStatus(stateValue),
            state: stateValue,
            trigger: item?.commandType || item?.command_type || item?.scheduleTime ? '调度运行' : '手动运行',
            startTime: normalizeDateText(item?.startTime || item?.submitTime),
            endTime: normalizeDateText(item?.endTime),
            duration: item?.duration || '-',
            rows: '- / -'
          }
        })
        asset.history = rows
        state.assetHistoryLoadedKey = historyKey
        void refreshAssetHistoryReadWriteCounts(asset)
      } catch (err) {
        state.assetHistoryError = extractErrorMessage(err, '读取运行历史失败，请稍后重试。')
      } finally {
        state.assetHistoryLoading = false
      }
    }

    const loadAssetLogsForInstance = async (
      asset: SyncTaskAsset,
      instanceId: number
    ) => {
      asset.lastInstanceId = instanceId
      state.assetLogInstanceId = instanceId
      asset.logLoaded = false
      asset.logText = ''
      asset.logError = ''
      await loadAssetLogs(asset, instanceId)
    }

    const loadAssetLogs = async (
      asset: SyncTaskAsset | null,
      targetInstanceId?: number | null
    ) => {
      if (!asset || asset.logLoading || asset.logLoaded) return
      const projectCode = toPositiveNumber(asset.projectCode)
      const instanceId = toPositiveNumber(targetInstanceId || state.assetLogInstanceId || asset.lastInstanceId)
      if (!projectCode || !instanceId) {
        asset.logLoaded = true
        asset.logText = ''
        return
      }

      asset.logLoading = true
      asset.logError = ''
      try {
        const taskResult = await queryTaskListByWorkflowId(
          instanceId,
          projectCode
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
          const taskHost = normalizeTaskHost(task)
          if (!Number.isFinite(taskInstanceId) || taskInstanceId <= 0) {
            logSections.push(
              `===== ${taskName} / 实例 - / 状态 ${taskState} =====\n未获取到任务实例 ID，无法读取日志。`
            )
            continue
          }
          if (!taskHost) {
            logSections.push(
              [
                `===== ${taskName} / 实例 ${taskInstanceId} / 状态 ${formatTaskStateText(taskState)} =====`,
                formatTaskUndispatchedMessage(task)
              ].join('\n')
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
            }, true)
            const message = logChunk?.message || ''
            const lineNum = Number(logChunk?.lineNum || 0)
            if (isTaskLogMissingMessage(message)) {
              taskLogText = formatTaskLogMissingMessage()
              break
            }
            if (!message) break
            if (message === previousMessage) break
            taskLogText += message
            previousMessage = message
            skipLineNum += lineNum || message.split(/\r?\n/).length
            if (!lineNum) break
          }

          logSections.push(
            [
              `===== ${taskName} / 实例 ${taskInstanceId} / 状态 ${formatTaskStateText(taskState)} / Host ${taskHost} =====`,
              taskLogText.trim() || '该任务实例暂无日志内容。'
            ].join('\n')
          )
        }
        asset.logText = logSections.join('\n\n')
        asset.logLoaded = true
      } catch (err) {
        asset.logError = formatTaskLogReadError(err)
      } finally {
        asset.logLoading = false
      }
    }

    const queryTaskLogText = async (
      taskInstanceId: number,
      maxAttempts = 12
    ): Promise<string> => {
      let skipLineNum = 0
      let taskLogText = ''
      let previousMessage = ''
      for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
        const logChunk = await queryLog({
          taskInstanceId,
          skipLineNum,
          limit: 1000
        }, true)
        const message = logChunk?.message || ''
        const lineNum = Number(logChunk?.lineNum || 0)
        if (isTaskLogMissingMessage(message)) {
          return taskLogText || formatTaskLogMissingMessage()
        }
        if (!message || message === previousMessage) break
        taskLogText += message
        previousMessage = message
        skipLineNum += lineNum || message.split(/\r?\n/).length
        if (!lineNum) break
      }
      return taskLogText
    }

    const queryInstanceReadWriteCounts = async (
      instanceId: number,
      projectCode: number
    ): Promise<{ readRows: number | null; writeRows: number | null; taskInstanceId: number | null }> => {
      const taskResult = await queryTaskListByWorkflowId(instanceId, projectCode)
      let totalReadRows = 0
      let totalWriteRows = 0
      let hasReadRows = false
      let hasWriteRows = false
      let latestTaskInstanceId: number | null = null
      for (const task of normalizeList(taskResult?.taskList || taskResult)) {
        if (task?.state !== 'SUCCESS') continue
        const taskInstanceId = toPositiveNumber(task?.id)
        if (!taskInstanceId) continue
        latestTaskInstanceId = taskInstanceId
        const taskLogText = await queryTaskLogText(taskInstanceId, 12)
        const taskCounts = extractReadWriteCountFromLog(taskLogText)
        if (taskCounts.readRows !== null) {
          totalReadRows += taskCounts.readRows
          hasReadRows = true
        }
        if (taskCounts.writeRows !== null) {
          totalWriteRows += taskCounts.writeRows
          hasWriteRows = true
        }
      }
      return {
        readRows: hasReadRows ? totalReadRows : null,
        writeRows: hasWriteRows ? totalWriteRows : null,
        taskInstanceId: latestTaskInstanceId
      }
    }

    const buildSyncStatPayload = (
      asset: SyncTaskAsset,
      instanceId: number,
      counts: { readRows: number | null; writeRows: number | null; taskInstanceId?: number | null }
    ) => ({
      projectCode: Number(asset.projectCode),
      workflowDefinitionCode: Number(asset.workflowCode),
      workflowInstanceId: instanceId,
      taskInstanceId: counts.taskInstanceId || null,
      readRows: counts.readRows,
      writeRows: counts.writeRows,
      failedRows: 0,
      runStatus: 'SUCCESS',
      statSource: 'LOG_PARSE',
      payloadJson: JSON.stringify({
        taskName: asset.name,
        sourcePath: asset.sourcePath,
        targetPath: asset.targetPath
      })
    })

    const applyPersistedHistoryStats = (asset: SyncTaskAsset, stats: any[]) => {
      const statByInstanceId = new Map<number, any>()
      normalizeList(stats).forEach((stat) => {
        const instanceId = toPositiveNumber(stat?.workflowInstanceId)
        if (instanceId) statByInstanceId.set(instanceId, stat)
      })
      asset.history.forEach((row) => {
        if (!row.instanceId) return
        const stat = statByInstanceId.get(row.instanceId)
        if (!stat) return
        const readRows = stat.readRows ?? null
        const writeRows = stat.writeRows ?? null
        row.rows = formatReadWriteRows(readRows, writeRows)
      })
    }

    const loadPersistedHistoryStats = async (asset: SyncTaskAsset) => {
      const projectCode = toPositiveNumber(asset.projectCode)
      const workflowCode = toPositiveNumber(asset.workflowCode)
      const workflowInstanceIds = asset.history
        .map((row) => row.instanceId)
        .filter((id): id is number => !!toPositiveNumber(id))
      if (!projectCode || !workflowCode || !workflowInstanceIds.length) return
      const stats = await queryDataFlowSyncInstanceStats({
        projectCode,
        workflowDefinitionCode: workflowCode,
        workflowInstanceIds,
        refreshMissing: true
      })
      applyPersistedHistoryStats(asset, stats)
    }

    const refreshAssetReadWriteCounts = async (asset: SyncTaskAsset) => {
      const projectCode = toPositiveNumber(asset.projectCode)
      const instanceId = toPositiveNumber(asset.lastInstanceId)
      if (!projectCode || !instanceId) return
      try {
        const counts = await queryInstanceReadWriteCounts(instanceId, projectCode)
        const hasReadRows = counts.readRows !== null
        const hasWriteRows = counts.writeRows !== null
        if (hasReadRows) asset.readRows = counts.readRows
        if (hasWriteRows) asset.writeRows = counts.writeRows
        if (hasReadRows || hasWriteRows) {
          void upsertDataFlowSyncInstanceStat(buildSyncStatPayload(asset, instanceId, counts))
        }
        if ((hasReadRows || hasWriteRows) && asset.history.length) {
          asset.history[0].rows = formatReadWriteRows(asset.readRows, asset.writeRows)
        }
      } catch (err) {
        // 读写数是辅助展示，失败时保留原值，避免覆盖已解析出的统计结果。
      }
    }

    const refreshAssetHistoryReadWriteCounts = async (asset: SyncTaskAsset) => {
      const projectCode = toPositiveNumber(asset.projectCode)
      if (!projectCode || !asset.history.length) return
      try {
        await loadPersistedHistoryStats(asset)
        return
      } catch (err) {
        // 统计表查询失败时仍保留日志解析兜底。
      }
      const rowsNeedingCounts = asset.history.filter((row) =>
        row.status === 'SUCCESS' &&
        row.instanceId &&
        (!row.rows || row.rows === '- / -')
      )
      for (const row of rowsNeedingCounts) {
        try {
          const counts = await queryInstanceReadWriteCounts(row.instanceId as number, projectCode)
          if (counts.readRows !== null || counts.writeRows !== null) {
            row.rows = formatReadWriteRows(counts.readRows, counts.writeRows)
            void upsertDataFlowSyncInstanceStat(
              buildSyncStatPayload(asset, row.instanceId as number, counts)
            )
          }
        } catch (err) {
          // 单条历史读写解析失败不影响其他历史实例展示。
        }
      }
    }

    const queryWorkflowFailureSummary = async (
      taskRows: WorkflowTaskProgressRow[]
    ): Promise<string> => {
      const failedTasks = taskRows.filter((taskRow) =>
        taskRow.taskInstanceId &&
        ['FAILURE', 'STOP'].includes(taskRow.state)
      )
      for (const taskRow of failedTasks) {
        const taskLogText = await queryTaskLogText(taskRow.taskInstanceId as number, 8)
        const summary = extractTaskFailureSummaryFromLog(taskLogText)
        if (summary) {
          return `${taskRow.name}: ${summary}`
        }
      }
      return ''
    }

    const stopLatestInstancePolling = () => {
      if (latestInstancePollingTimer) {
        window.clearInterval(latestInstancePollingTimer)
        latestInstancePollingTimer = null
      }
      latestInstancePollingErrorCount = 0
    }

    const resolveWorkflowStateMeta = (stateValue: string) => {
      return WORKFLOW_STATE_META[stateValue] || {
        label: stateValue || '未知状态',
        type: 'default' as const
      }
    }

    const refreshLatestInstanceProgress = async (instanceId: number) => {
      const projectCode = toPositiveNumber(state.selectedProjectCode)
      const safeInstanceId = toPositiveNumber(instanceId)
      if (!projectCode || !safeInstanceId) return null
      const [instanceDetail, taskResult] = await Promise.all([
        queryWorkflowInstanceById(safeInstanceId, projectCode),
        queryTaskListByWorkflowId(safeInstanceId, projectCode)
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
        state.latestRunMessage =
          (await queryWorkflowFailureSummary(taskRows)) || '执行失败，请进入日志诊断查看任务详情。'
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
              }, true)
              const message = logChunk?.message || ''
              const lineNum = Number(logChunk?.lineNum || 0)
              if (isTaskLogMissingMessage(message)) {
                break
              }
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
          asset.errorMessage = terminalStatus === 'FAILED'
            ? state.latestRunMessage
            : ''
          asset.lastInstanceId = state.latestInstanceId || instanceId
          asset.readRows = state.latestReadRowCount
          asset.writeRows = state.latestSyncedRowCount
          asset.updatedAt = nowText
          asset.history = [
            {
              id: String(instanceId),
              instanceId,
              status: terminalStatus,
              state: instanceState,
              trigger: '立即执行',
              startTime: state.latestInstanceStartTime || nowText,
              endTime: state.latestInstanceEndTime || nowText,
              duration: '-',
              rows: formatReadWriteRows(state.latestReadRowCount, state.latestSyncedRowCount)
            },
            ...asset.history
          ].slice(0, 8)
          try {
            await registerCurrentGovernanceLineage(terminalStatus, asset)
          } catch (err) {
            console.warn('Update data governance lineage after sync failed.', err)
          }
        }
      }

      return instanceState
    }

    const markLatestInstancePollingFailed = async (
      instanceId: number,
      error: any
    ) => {
      stopLatestInstancePolling()
      state.latestRunStage = 'FAILURE'
      state.latestRunMessage = extractErrorMessage(
        error,
        '运行状态刷新失败，请进入工作流实例或日志诊断确认执行结果。'
      )
      state.latestInstanceState = 'UNKNOWN'
      state.latestInstanceStateLabel = '状态刷新失败'
      state.latestInstanceStateType = 'error'
      state.latestInstanceId = state.latestInstanceId || instanceId
      const asset = upsertCurrentAsset('FAILED')
      state.latestPublishedAssetId = asset.id
      await registerCurrentGovernanceLineage('FAILED', asset)
      window.$message.error(state.latestRunMessage)
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
          latestInstancePollingErrorCount = 0
          if (currentState && TERMINAL_WORKFLOW_STATES.has(currentState)) {
            stopLatestInstancePolling()
          }
        } catch (error) {
          latestInstancePollingErrorCount += 1
          if (latestInstancePollingErrorCount >= 2) {
            await markLatestInstancePollingFailed(instanceId, error)
          }
        }
      }, 3000)
    }

    const queryLatestWorkflowInstanceId = async (
      workflowDefinitionCode: number
    ): Promise<number | null> => {
      const projectCode = toPositiveNumber(state.selectedProjectCode)
      const workflowCode = toPositiveNumber(workflowDefinitionCode)
      if (!projectCode || !workflowCode) return null

      // DolphinScheduler 的启动接口并不总是稳定返回实例 ID。
      // 这里在启动后主动按工作流编码查询最新实例，避免“任务已启动但页面无法跳转”的体验断层。
      for (let attempt = 0; attempt < 6; attempt += 1) {
        const result = await queryWorkflowInstanceListPaging(
          {
            pageNo: 1,
            pageSize: 10,
            workflowDefinitionCode: workflowCode,
            searchVal: ''
          },
          projectCode
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
        window.$message.error('请先选择要落入的 DataFlow 项目。')
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
          window.$message.error('请先选择要落入的 DataFlow 项目。')
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
      if (unmappedRequiredTargetColumns.value.length) {
        if (showMessage) {
          window.$message.error(
            `已有目标表存在未映射的必填字段：${unmappedRequiredTargetColumns.value.slice(0, 5).join('、')}，请补充映射后再保存或执行。`
          )
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
          getDefaultSchemaNameByDetail(
            targetDatasourceOption.value?.type,
            state.target.datasourceId ? state.datasourceDetails[state.target.datasourceId] : null
          ),
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
      state.taskNameTouched = false
      state.source.datasourceId = null
      state.target.datasourceId = null
      resetEndpoint(state.source)
      resetEndpoint(state.target)
      state.targetTableName = ''
      state.targetSchemaName = ''
      state.fieldRows = []
      state.sourceFilters = [createSourceFilterRule()]
      state.activeSolutionModule = 'MAPPING'
      state.sinkCustomSql = ''
      state.sinkOptions = createDefaultSinkOptions()
      state.dataProcessingRules = [createDataProcessingRule()]
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
      void loadSyncTaskAssets()
    }

    const openAssetDetail = (asset: SyncTaskAsset, tab: SyncTaskDetailTab = 'OVERVIEW') => {
      state.selectedAsset = asset
      state.assetDetailTab = tab
      state.assetDetailVisible = true
      state.assetLogInstanceId = asset.lastInstanceId
      void refreshAssetReadWriteCounts(asset)
      if (tab === 'LOGS') {
        void loadAssetLogs(asset)
      }
      if (tab === 'HISTORY') {
        void loadAssetHistory(asset)
      }
    }

    const buildAssetWorkflowActionState = (asset: SyncTaskAsset): SyncTaskWorkflowActionRow => {
      const releaseState =
        asset.workflowReleaseState === 'ONLINE'
          ? 'ONLINE'
          : asset.status === 'OFFLINE' || asset.status === 'DRAFT'
            ? 'OFFLINE'
            : 'ONLINE'
      const scheduleReleaseState =
        asset.schedule?.releaseState || (asset.scheduleStatus === 'ON' ? 'ONLINE' : 'OFFLINE')

      return {
        code: asset.workflowCode,
        name: asset.workflowName || asset.name,
        releaseState,
        scheduleReleaseState,
        schedule: asset.schedule || null
      }
    }

    const createDefaultScheduleRow = (workflowCode: number) => ({
      code: workflowCode,
      startTime: format(new Date(), 'yyyy-MM-dd HH:mm:ss'),
      endTime: format(new Date(new Date().setFullYear(new Date().getFullYear() + 100)), 'yyyy-MM-dd HH:mm:ss'),
      crontab: '0 0 * * * ? *',
      timezoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
      failureStrategy: 'CONTINUE',
      warningType: 'NONE',
      warningGroupId: 0,
      workflowInstancePriority: 'MEDIUM',
      workerGroup: 'default',
      tenantCode: 'default',
      environmentCode: null
    })

    const normalizeScheduleModalRow = (
      scheduleRow: Record<string, any> | null | undefined,
      workflowCode: number
    ) => {
      if (!scheduleRow?.id) {
        return createDefaultScheduleRow(workflowCode)
      }
      return {
        ...scheduleRow,
        code: workflowCode,
        warningGroupId: scheduleRow.warningGroupId || 0,
        workflowInstancePriority:
          scheduleRow.workflowInstancePriority || 'MEDIUM',
        workerGroup: scheduleRow.workerGroup || 'default',
        tenantCode: scheduleRow.tenantCode || 'default',
        environmentCode: scheduleRow.environmentCode || null
      }
    }

    const resolveAssetProjectCode = (asset: SyncTaskAsset): number | null => {
      const directCode = Number(asset.projectCode)
      if (Number.isFinite(directCode) && directCode > 0) return directCode

      const matchedProject = state.projectOptions.find(
        (item) => item.label === asset.projectName
      )
      const matchedCode = Number(matchedProject?.value)
      if (Number.isFinite(matchedCode) && matchedCode > 0) {
        asset.projectCode = matchedCode
        return matchedCode
      }
      return null
    }

    const ensureAssetWorkflowMeta = async (
      asset: SyncTaskAsset,
      silent = false
    ): Promise<boolean> => {
      const projectCode = resolveAssetProjectCode(asset)
      if (!projectCode) {
        if (!silent) {
          window.$message.warning('该同步任务没有关联 Dolphin 项目，请先编辑并保存任务。')
        }
        return false
      }

      const directWorkflowCode = Number(asset.workflowCode)
      if (Number.isFinite(directWorkflowCode) && directWorkflowCode > 0) {
        return true
      }

      const workflowName = asset.workflowName || asset.name
      if (!workflowName) {
        if (!silent) {
          window.$message.warning('该同步任务没有关联 Dolphin 工作流，请先编辑并保存任务。')
        }
        return false
      }

      try {
        const latestMeta = await findWorkflowDefinitionMetaByName(
          projectCode,
          workflowName
        )
        if (!latestMeta?.code) {
          if (!silent) {
            window.$message.warning('没有找到对应 Dolphin 工作流定义，请先编辑并保存任务。')
          }
          return false
        }
        asset.projectCode = projectCode
        asset.workflowCode = latestMeta.code
        asset.workflowName = latestMeta.name || workflowName
        asset.workflowVersion = latestMeta.version || asset.workflowVersion || 1
        asset.workflowReleaseState = latestMeta.releaseState || asset.workflowReleaseState
        return true
      } catch (err) {
        if (!silent) {
          window.$message.error(
            extractErrorMessage(err, '读取同步任务工作流失败，请检查项目权限。')
          )
        }
        return false
      }
    }

    const ensureAssetHasWorkflow = (asset: SyncTaskAsset): boolean => {
      if (resolveAssetProjectCode(asset) && toPositiveNumber(asset.workflowCode)) return true
      window.$message.warning('该同步任务还没有关联 Dolphin 工作流定义，请先编辑并保存任务。')
      return false
    }

    const refreshAssetWorkflowReleaseState = async (
      asset: SyncTaskAsset
    ): Promise<'ONLINE' | 'OFFLINE'> => {
      if (!ensureAssetHasWorkflow(asset)) {
        return asset.workflowReleaseState === 'ONLINE' ? 'ONLINE' : 'OFFLINE'
      }
      const projectCode = toPositiveNumber(asset.projectCode)
      const workflowCode = toPositiveNumber(asset.workflowCode)
      if (!projectCode || !workflowCode) {
        return asset.workflowReleaseState === 'ONLINE' ? 'ONLINE' : 'OFFLINE'
      }
      const latest = await queryWorkflowDefinitionByCode(
        workflowCode,
        projectCode
      )
      const latestMeta = extractWorkflowDefinitionMeta(latest)
      const releaseState =
        latestMeta?.releaseState === 'ONLINE' ? 'ONLINE' : 'OFFLINE'
      asset.workflowReleaseState = releaseState
      asset.workflowVersion = latestMeta?.version || asset.workflowVersion || 1
      asset.workflowName = latestMeta?.name || asset.workflowName || asset.name
      return releaseState
    }

    const openAssetWorkflowDefinition = (asset: SyncTaskAsset) => {
      if (!ensureAssetHasWorkflow(asset)) return
      void router.push({
        name: 'workflow-definition-detail',
        params: {
          projectCode: asset.projectCode,
          code: asset.workflowCode
        }
      })
    }

    const openAssetWorkflowTree = (asset: SyncTaskAsset) => {
      if (!ensureAssetHasWorkflow(asset)) return
      void router.push({
        name: 'workflow-definition-tree',
        params: {
          projectCode: asset.projectCode,
          definitionCode: asset.workflowCode
        }
      })
    }

    const startAssetWorkflow = async (asset: SyncTaskAsset) => {
      if (!ensureAssetHasWorkflow(asset)) return
      const projectCode = asset.projectCode as number
      const workflowCode = asset.workflowCode as number
      try {
        await release(
          {
            name: asset.workflowName || asset.name,
            releaseState: 'ONLINE'
          },
          projectCode,
          workflowCode
        )
        const result = await startWorkflowInstance(
          {
            workflowDefinitionCode: workflowCode,
            failureStrategy: 'CONTINUE',
            workflowInstancePriority: 'MEDIUM',
            scheduleTime: JSON.stringify({
              complementScheduleDateList: formatDateTime(Date.now())
            }),
            warningGroupId: 0,
            warningType: 'NONE',
            execType: 'START_PROCESS',
            runMode: 'RUN_MODE_SERIAL',
            workerGroup: 'default',
            environmentCode: -1,
            timeout: 0,
            startParams: '',
            version: Number(asset.workflowVersion) || 1,
            dryRun: 0
          },
          projectCode
        )
        const instanceId = Array.isArray(result)
          ? Number(result[0])
          : Number(result?.id || result?.workflowInstanceId || result?.processInstanceId || 0)
        asset.status = 'RUNNING'
        asset.workflowReleaseState = 'ONLINE'
        asset.lastInstanceId = instanceId || asset.lastInstanceId
        asset.lastRunTime = formatDateTime(Date.now())
        asset.updatedAt = asset.lastRunTime
        window.$message.success('同步任务已提交运行。')
      } catch (err) {
        window.$message.error(extractErrorMessage(err, '启动同步任务失败，请检查工作流定义状态。'))
      }
    }

    const releaseAssetWorkflow = async (
      asset: SyncTaskAsset,
      currentReleaseState?: 'ONLINE' | 'OFFLINE'
    ) => {
      if (!ensureAssetHasWorkflow(asset)) return
      try {
        const releaseState =
          currentReleaseState ||
          (asset.workflowReleaseState === 'ONLINE' ? 'ONLINE' : 'OFFLINE')
        const nextReleaseState = releaseState === 'ONLINE'
          ? 'OFFLINE'
          : 'ONLINE'
        await release(
          {
            name: asset.workflowName || asset.name,
            releaseState: nextReleaseState
          },
          asset.projectCode as number,
          asset.workflowCode as number
        )
        asset.status = nextReleaseState === 'ONLINE' ? 'SUCCESS' : 'OFFLINE'
        asset.workflowReleaseState = nextReleaseState
        void refreshAssetWorkflowReleaseState(asset)
        asset.updatedAt = formatDateTime(Date.now())
        window.$message.success(nextReleaseState === 'ONLINE' ? '同步任务工作流已上线。' : '同步任务工作流已下线。')
      } catch (err) {
        window.$message.error(extractErrorMessage(err, '切换工作流上线状态失败。'))
      }
    }

    const openAssetScheduleModal = async (asset: SyncTaskAsset) => {
      const projectCode = resolveAssetProjectCode(asset)
      const workflowCode = Number(asset.workflowCode)
      if (!projectCode || !Number.isFinite(workflowCode) || workflowCode <= 0) {
        const hasWorkflow = await ensureAssetWorkflowMeta(asset)
        if (!hasWorkflow) return
      }
      const resolvedProjectCode = asset.projectCode as number
      const resolvedWorkflowCode = asset.workflowCode as number
      state.schedulingAssetId = asset.id
      state.selectedProjectCode = resolvedProjectCode
      state.latestWorkflowCode = resolvedWorkflowCode
      state.latestWorkflowName = asset.workflowName || asset.name
      state.latestWorkflowVersion = asset.workflowVersion || 1
      state.latestWorkflowReleaseState =
        asset.workflowReleaseState ||
        (asset.status === 'OFFLINE' || asset.status === 'DRAFT' ? 'OFFLINE' : 'ONLINE')
      state.scheduleModalType = asset.schedule?.id ? 'update' : 'create'
      state.scheduleModalState = asset.schedule?.releaseState || 'OFFLINE'
      state.scheduleModalRow = normalizeScheduleModalRow(
        asset.schedule,
        resolvedWorkflowCode
      )
      showScheduleModal()
      try {
        const releasedWorkflow = await queryWorkflowDefinitionByCode(
          resolvedWorkflowCode,
          resolvedProjectCode
        )
        const latestMeta = extractWorkflowDefinitionMeta(releasedWorkflow)
        state.latestWorkflowReleaseState =
          latestMeta?.releaseState ||
          (asset.status === 'OFFLINE' || asset.status === 'DRAFT' ? 'OFFLINE' : 'ONLINE')
        asset.workflowReleaseState = state.latestWorkflowReleaseState
        state.latestWorkflowVersion = latestMeta?.version || state.latestWorkflowVersion
        state.latestWorkflowName = latestMeta?.name || state.latestWorkflowName
        const scheduleRow = await loadScheduleMeta(resolvedWorkflowCode)
        asset.schedule = scheduleRow
        asset.scheduleStatus = scheduleRow?.releaseState === 'ONLINE' ? 'ON' : 'OFF'
        state.scheduleModalType = scheduleRow?.id ? 'update' : 'create'
        state.scheduleModalState = scheduleRow?.releaseState || 'OFFLINE'
        state.scheduleModalRow = normalizeScheduleModalRow(
          scheduleRow,
          resolvedWorkflowCode
        )
      } catch (err) {
        window.$message.warning(extractErrorMessage(err, '已打开定时配置，但读取已有调度信息失败，请检查项目权限和工作流定义。'))
      }
    }

    const releaseAssetScheduler = async (asset: SyncTaskAsset) => {
      if (!ensureAssetHasWorkflow(asset)) return
      const projectCode = toPositiveNumber(asset.projectCode)
      const workflowCode = toPositiveNumber(asset.workflowCode)
      if (!projectCode || !workflowCode) return
      state.selectedProjectCode = projectCode
      state.latestWorkflowCode = workflowCode
      const scheduleRow = await loadScheduleMeta(workflowCode)
      if (!scheduleRow?.id) {
        window.$message.warning('该同步任务还没有周期调度配置，请先点击定时按钮配置。')
        return
      }
      try {
        if (scheduleRow.releaseState === 'ONLINE') {
          await offline(projectCode, scheduleRow.id)
          asset.scheduleStatus = 'OFF'
          asset.schedule = {
            ...scheduleRow,
            releaseState: 'OFFLINE'
          }
          window.$message.success('同步任务调度已下线。')
        } else {
          await online(projectCode, scheduleRow.id)
          asset.scheduleStatus = 'ON'
          asset.schedule = {
            ...scheduleRow,
            releaseState: 'ONLINE'
          }
          window.$message.success('同步任务调度已上线。')
        }
        asset.updatedAt = formatDateTime(Date.now())
      } catch (err) {
        window.$message.error(extractErrorMessage(err, '切换调度状态失败。'))
      }
    }

    const copyAssetWorkflow = (asset: SyncTaskAsset) => {
      if (!ensureAssetHasWorkflow(asset)) return
      navigator.clipboard?.writeText(asset.workflowName || asset.name)
      window.$message.success('已复制同步任务工作流名称。')
    }

    const deleteAssetWorkflow = (asset: SyncTaskAsset) => {
      window.$message.warning(`为避免误删 Dolphin 工作流，当前请先在详情中确认后再处理：${asset.name}`)
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
      state.taskNameTouched = true
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
          : buildSourceColumnsFromFieldRows(asset.fieldRows)
      )
      state.target.columns = cloneColumns(
        asset.targetColumns.length
          ? asset.targetColumns
          : buildTargetColumnsFromFieldRows(asset.fieldRows)
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
        targetParts.length >= 3
          ? targetParts[targetParts.length - 2]
          : getDatasourceDefaultSchema(
            state.target.datasourceId,
            targetOption?.type,
            state.datasourceDetails
          )
      state.targetTableName = targetParts[targetParts.length - 1] || asset.targetPath
      state.target.table = state.targetTableName
      state.target.tables = state.targetTableName ? [state.targetTableName] : []
      state.target.databases = state.target.database ? [state.target.database] : []
      state.fieldRows = cloneFieldRows(asset.fieldRows)
      state.sourceFilters = cloneSourceFilters(asset.sourceFilters.length ? asset.sourceFilters : [createSourceFilterRule()])
      state.sinkCustomSql = asset.sinkCustomSql
      state.sinkOptions = cloneSinkOptions({
        ...asset.sinkOptions,
        customSql: asset.sinkOptions?.customSql || asset.sinkCustomSql
      })
      state.dataProcessingRules = cloneDataProcessingRules(asset.dataProcessingRules)
      state.configEditorText = generatedConfig.value
      state.configManualOverride = false
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
            await loadColumns(state.source, { preserveExisting: true })
          })
        }
        if (state.target.datasourceId && state.target.database && state.target.table) {
          void loadDatabases(state.target).then(async () => {
            await loadTables(state.target)
            await loadColumns(state.target, { preserveExisting: true })
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
        scheduleStatus: state.latestScheduleId && isScheduleOnline(state.latestScheduleSummary) ? 'ON' : 'OFF',
        sourceType: sourceOption?.type || 'MYSQL',
        targetType: targetOption?.type || 'POSTGRESQL',
        sourceName: sourceOption?.label || '-',
        sourcePath: formatQualifiedPath(state.source.database, state.source.table),
        targetName: targetOption?.label || '-',
        targetPath: formatQualifiedPath(
          state.target.database,
          state.targetSchemaName ||
            getDatasourceDefaultSchema(state.target.datasourceId, targetOption?.type, state.datasourceDetails),
          state.targetTableName
        ),
        workflowCode: state.latestWorkflowCode,
        workflowName: state.latestWorkflowName || state.taskName,
        workflowVersion: state.latestWorkflowVersion,
        workflowReleaseState: state.latestWorkflowReleaseState,
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
        sinkOptions: cloneSinkOptions(state.sinkOptions),
        dataProcessingRules: cloneDataProcessingRules(state.dataProcessingRules),
        fieldRows: cloneFieldRows(state.fieldRows),
        sourceColumns: cloneColumns(state.source.columns),
        targetColumns: cloneColumns(state.target.columns),
        configText: effectiveConfigText.value,
        history: [
          {
            id: state.latestInstanceId ? String(state.latestInstanceId) : `draft-${Date.now()}`,
            instanceId: state.latestInstanceId || null,
            status,
            state: state.latestInstanceState || '',
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
          history: [...asset.history, ...previous.history.map(normalizeHistoryRow)].slice(0, 8),
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
          targetField: state.dataProcessingRules.some(
            (rule) =>
              rule.enabled &&
              rule.sourceField.toLowerCase() === item.sourceColumn.toLowerCase() &&
              rule.targetField.toLowerCase() === item.targetColumn.toLowerCase()
          )
            ? `${item.targetColumn}（字段值翻译）`
            : item.targetColumn
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
          targetSchema:
            state.targetSchemaName ||
            getDatasourceDefaultSchema(
              state.target.datasourceId,
              targetOption.type,
              state.datasourceDetails
            ),
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
      const projectCode = toPositiveNumber(state.selectedProjectCode)
      const workflowCode = toPositiveNumber(workflowDefinitionCode)
      if (!projectCode || !workflowCode) return null
      const scheduleList = await queryScheduleListPaging(
        {
          pageNo: 1,
          pageSize: 20,
          searchVal: '',
          workflowDefinitionCode: workflowCode
        },
        projectCode
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
      const currentRawScript = effectiveConfigText.value.trim()
      if (!currentRawScript) {
        window.$message.error('SeaTunnel 配置为空，请先检查源端、目标端和字段映射。')
        return null
      }
      const nodeData = buildSeaTunnelTaskData(
        state.runSettings,
        taskName,
        currentRawScript,
        taskCode
      )
      if (!nodeData.description) {
        nodeData.description = `由同步任务页面自动生成，来源 ${sourceOption.label} -> ${targetOption.label}`
      }
      const taskDefinition = formatParams(nodeData as INodeData).taskDefinitionJsonObj
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

    const handleCreateTargetTable = async (): Promise<boolean> => {
      const request = buildTargetTableRequest()
      if (!request) return false
      if (!state.latestCreateTableDdl.trim()) {
        window.$message.error('建表 SQL 还没有生成，请稍后重试或点击重新生成。')
        return false
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
        return false
      }
      state.creatingTable = false
      return true
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

    const ensureTargetTableBeforeRun = async (): Promise<boolean> => {
      if (targetTableMode.value === 'EXISTING_TABLE') {
        return true
      }
      if (!state.latestCreateTableDdl.trim()) {
        await handlePreviewTargetTable(false)
      }
      if (!state.latestCreateTableDdl.trim()) {
        window.$message.error('目标表还没有完成建表预检查，请先生成建表 SQL。')
        return false
      }
      state.latestRunStage = 'PREPARING'
      state.latestRunMessage = '建表中'
      return handleCreateTargetTable()
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
          throw new Error('工作流已提交保存，但没有拿到 DataFlow 返回的工作流编码。')
        }
        if (state.latestWorkflowCode) {
          await loadScheduleMeta(state.latestWorkflowCode)
        }
        const asset = upsertCurrentAsset('DRAFT')
        await registerCurrentGovernanceLineage('DRAFT', asset)
        window.$message.success('同步任务已保存为 DataFlow 工作流定义。')
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
      state.scheduleModalRow = normalizeScheduleModalRow(
        scheduleRow,
        state.latestWorkflowCode
      )
      showScheduleModal()
    }

    const openRunSettingsModal = () => {
      const validated = validateSyncDesign()
      if (!validated) {
        return
      }
      const { sourceOption, targetOption } = validated
      const fallbackTaskName = `${state.source.table}_to_${state.targetTableName.trim()}`
        .replaceAll(/[^a-zA-Z0-9_\u4e00-\u9fa5]+/g, '_')
        .slice(0, 120)
      state.runSettings.nodeName = state.runSettings.nodeName === 'sync_task'
        ? fallbackTaskName
        : state.runSettings.nodeName
      state.runSettings.description =
        state.runSettings.description ||
        `由同步任务页面自动生成，来源 ${sourceOption.label} -> ${targetOption.label}`
      state.runSettings.rawScript = effectiveConfigText.value
      const nextNodeData = buildSeaTunnelTaskData(
        state.runSettings,
        fallbackTaskName,
        effectiveConfigText.value
      )
      state.runNodeData = {
        ...nextNodeData,
        taskParams: {
          ...nextNodeData.taskParams,
          rawScript: effectiveConfigText.value
        }
      }
      state.runNodeModalKey += 1
      state.runSettingsVisible = true
    }

    const handleRunWorkflow = () => {
      openRunSettingsModal()
    }

    const handleConfirmRunWorkflow = async (payload?: { data?: INodeData }) => {
      if (payload?.data) {
        const modalData = payload.data as INodeData & {
          taskParams?: {
            rawScript?: string
            useCustom?: boolean
          }
        }
        const fallbackRawScript = effectiveConfigText.value
        const payloadRawScript = modalData.rawScript || modalData.taskParams?.rawScript || ''
        const normalizedData = {
          ...modalData,
          rawScript: payloadRawScript || fallbackRawScript,
          useCustom: modalData.useCustom !== false
        } as INodeData
        applyNativeSeaTunnelModel(state.runSettings, normalizedData)
        if (normalizedData.useCustom !== false) {
          handleConfigEditorChange(normalizedData.rawScript || fallbackRawScript)
        }
      }
      const validated = validateSyncDesign()
      if (!validated) {
        return
      }
      state.runSettingsVisible = false
      state.runningWorkflow = true
      state.latestRunStage = 'PREPARING'
      state.latestRunMessage = '保存中'
      state.latestReadRowCount = null
      state.latestSyncedRowCount = null
      state.latestSyncedRowCountLoading = false
      state.latestSyncedRowCountInstanceId = null
      try {
        const targetReady = await ensureTargetTableBeforeRun()
        if (!targetReady) {
          state.latestRunStage = 'FAILURE'
          state.latestRunMessage = '建表失败'
          state.runningWorkflow = false
          return
        }
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
          window.$message.error('同步实例启动后未获取到实例编号，请稍后刷新任务列表或进入工作流实例确认。')
        }
        returnToAssetListAfterPublish()
      } catch (err) {
        state.latestRunStage = 'FAILURE'
        state.latestRunMessage = '执行失败'
        const asset = upsertCurrentAsset('FAILED')
        state.latestPublishedAssetId = asset.id
        await registerCurrentGovernanceLineage('FAILED', asset)
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
          <div
            class={[styles.columnCell, styles.sourceColumnCell]}
            onMouseup={() => {
              if (draggingMapping.value?.side === 'target') {
                handleMapTargetToSource(draggingMapping.value.key, row.sourceColumn || row.key)
              }
            }}
          >
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
              <div
                class={[styles.mappingAnchor, styles.sourceMappingAnchor]}
                data-source-anchor={row.sourceColumn || row.key}
                onMousedown={(event: MouseEvent) =>
                  handleStartMappingDrag('source', row.sourceColumn || row.key, event)
                }
              />
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
              class={[styles.mappingFieldHead, styles.targetFieldHead]}
              onMouseup={() => {
                if (draggingMapping.value?.side === 'source') {
                  handleMapSourceToTarget(draggingMapping.value.key, row.key)
                }
              }}
            >
              <div
                class={[styles.mappingAnchor, styles.targetMappingAnchor]}
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

    const renderAssetActionButton = (
      tooltip: string,
      icon: any,
      onClick: () => void | Promise<void>,
      options: {
        type?: 'default' | 'primary' | 'info' | 'success' | 'warning' | 'error'
        disabled?: boolean
        className?: string
      } = {}
    ) => {
      const handleClick = async (event: MouseEvent) => {
        event.stopPropagation()
        if (!options.disabled) {
          try {
            await onClick()
          } catch (err) {
            window.$message.error(
              extractErrorMessage(err, `${tooltip}操作失败，请稍后重试。`)
            )
          }
        }
      }

      return (
        <NTooltip trigger='hover'>
          {{
            default: () => tooltip,
            trigger: () => (
            <span
              class={['sync-task-action-hitbox', options.className]}
              role='button'
              aria-label={tooltip}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '28px',
                height: '28px',
                cursor: options.disabled ? 'not-allowed' : 'pointer'
              }}
              onClick={handleClick}
            >
            <NButton
              size='small'
              type={options.type || 'info'}
              circle
              disabled={options.disabled}
              class={options.className}
              onClick={(event: MouseEvent) => {
                event.stopPropagation()
                void handleClick(event)
              }}
            >
              <NIcon>
                {h(icon)}
              </NIcon>
            </NButton>
            </span>
            )
          }}
        </NTooltip>
      )
    }

    const renderAssetActions = (row: SyncTaskAsset) => {
      const actionState = buildAssetWorkflowActionState(row)
      const releaseState = actionState.releaseState
      const scheduleReleaseState = actionState.scheduleReleaseState
      const schedule = actionState.schedule
      const handleScheduleClick = (event: MouseEvent) => {
        event.preventDefault()
        event.stopPropagation()
        void openAssetScheduleModal(row)
      }

      return (
        <NSpace>
          {renderAssetActionButton('编辑', FormOutlined, () => hydrateWizardFromAsset(row), {
            disabled: releaseState === 'ONLINE',
            className: 'btn-edit'
          })}
          {renderAssetActionButton('启动', PlayCircleOutlined, () => void startAssetWorkflow(row), {
            type: 'primary',
            disabled: releaseState === 'OFFLINE',
            className: 'btn-run'
          })}
          <NTooltip trigger='hover'>
            {{
              default: () => releaseState === 'ONLINE' ? '下线' : '上线',
              trigger: () => (
                <NPopconfirm
                  onPositiveClick={() =>
                    void releaseAssetWorkflow(row, releaseState)
                  }
                >
                  {{
                    default: () => releaseState === 'ONLINE' ? '确认下线该工作流？' : '确认上线该工作流？',
                    trigger: () => (
                      <NButton
                        size='small'
                        type={releaseState === 'ONLINE' ? 'warning' : 'error'}
                        circle
                        class='btn-publish'
                        onClick={(event: MouseEvent) => event.stopPropagation()}
                      >
                        <NIcon>
                          {releaseState === 'ONLINE' ? h(DownloadOutlined) : h(UploadOutlined)}
                        </NIcon>
                      </NButton>
                    )
                  }}
                </NPopconfirm>
              )
            }}
          </NTooltip>
          <NTooltip trigger='hover'>
            {{
              default: () => '定时',
              trigger: () => (
                <span
                  class='btn-schedule-wrapper'
                  role='button'
                  aria-label='定时'
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: '28px',
                    height: '28px',
                    cursor: 'pointer'
                  }}
                >
                  <NButton
                    size='small'
                    type='info'
                    circle
                    class='btn-schedule'
                    onClick={handleScheduleClick}
                  >
                    <NIcon>
                      {h(ClockCircleOutlined)}
                    </NIcon>
                  </NButton>
                </span>
              )
            }}
          </NTooltip>
          <NTooltip trigger='hover'>
            {{
              default: () => scheduleReleaseState === 'ONLINE' ? '调度下线' : '调度上线',
              trigger: () => (
                <NPopconfirm onPositiveClick={() => void releaseAssetScheduler(row)}>
                  {{
                    default: () => scheduleReleaseState === 'ONLINE' ? '确认下线该调度？' : '确认上线该调度？',
                    trigger: () => (
                      <NButton
                        size='small'
                        type={scheduleReleaseState === 'ONLINE' ? 'warning' : 'error'}
                        circle
                        class='btn-publish'
                        disabled={!schedule || releaseState !== 'ONLINE'}
                        onClick={(event: MouseEvent) => event.stopPropagation()}
                      >
                        <NIcon>
                          {scheduleReleaseState === 'ONLINE' ? h(ArrowDownOutlined) : h(ArrowUpOutlined)}
                        </NIcon>
                      </NButton>
                    )
                  }}
                </NPopconfirm>
              )
            }}
          </NTooltip>
          {renderAssetActionButton('复制工作流', CopyOutlined, () => copyAssetWorkflow(row))}
          <NTooltip trigger='hover'>
            {{
              default: () => '删除',
              trigger: () => (
                <NPopconfirm
                  disabled={releaseState === 'ONLINE'}
                  onPositiveClick={() => deleteAssetWorkflow(row)}
                >
                  {{
                    default: () => '确认删除该同步任务？',
                    trigger: () => (
                      <NButton
                        size='small'
                        type='error'
                        circle
                        disabled={releaseState === 'ONLINE'}
                        class='btn-delete'
                        onClick={(event: MouseEvent) => event.stopPropagation()}
                      >
                        <NIcon>
                          {h(DeleteOutlined)}
                        </NIcon>
                      </NButton>
                    )
                  }}
                </NPopconfirm>
              )
            }}
          </NTooltip>
          {renderAssetActionButton('血缘图', ApartmentOutlined, () => openAssetWorkflowTree(row))}
          {renderAssetActionButton('版本信息', InfoCircleFilled, () => openAssetDetail(row, 'CONFIG'))}
        </NSpace>
      )
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
        width: 380,
        fixed: 'right',
        render: renderAssetActions
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
        if (!state.target.datasourceId) return
        state.targetSchemaName = getDefaultSchemaNameByDetail(
          targetDatasourceOption.value?.type,
          state.target.datasourceId ? state.datasourceDetails[state.target.datasourceId] : null
        )
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
        if (state.taskNameTouched) return
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
        if (state.assetDetailTab === 'HISTORY') {
          void loadAssetHistory(state.selectedAsset)
        }
      }
    )

    watch(
      () => [
        state.assetKeyword,
        state.assetProjectFilter
      ],
      () => {
        if (state.viewMode !== 'LIST') return
        if (assetFilterRefreshTimer) {
          window.clearTimeout(assetFilterRefreshTimer)
        }
        assetFilterRefreshTimer = window.setTimeout(() => {
          void loadSyncTaskAssets()
        }, 300)
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
      assetProjectOptions,
      assetTypeOptions,
      assetStatusOptions,
      assetScheduleOptions,
      filteredAssets,
      assetTableColumns,
      statusTagMeta,
      openCreateWizard,
      backToAssetList,
      openAssetDetail,
      hydrateWizardFromAsset,
      openWorkflowInstanceDetail,
      loadAssetLogs,
      loadAssetHistory,
      loadAssetLogsForInstance,
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
      handleConfirmRunWorkflow,
      handleOpenScheduleModal,
      refreshScheduleAssetState,
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
    const renderScheduleModal = () => {
      const projectCode = Number(this.state.selectedProjectCode)
      if (!this.state.scheduleModalVisible || !Number.isFinite(projectCode) || projectCode <= 0) {
        return null
      }
      return (
        <TimingModal
        key={`sync-schedule-${this.state.scheduleModalKey}`}
        row={this.state.scheduleModalRow}
        show={this.state.scheduleModalVisible}
        type={this.state.scheduleModalType}
        state={this.state.scheduleModalState}
        projectCode={projectCode}
        onUpdate:row={(row: Record<string, any>) => {
          this.state.scheduleModalRow = row
        }}
        onUpdate:show={(show?: boolean) => {
          this.state.scheduleModalVisible = !!show
        }}
        onUpdate:type={(type: 'create' | 'update') => {
          this.state.scheduleModalType = type
        }}
        onUpdate:state={(value: string) => {
          this.state.scheduleModalState = value
        }}
        onUpdateList={async () => {
          await this.refreshScheduleAssetState()
          this.state.scheduleModalVisible = false
        }}
      />
      )
    }
    const renderAssetLogContent = (asset: SyncTaskAsset, fullscreen = false) => (
      <NSpin show={!!asset.logLoading}>
        <NSpace vertical>
          {asset.errorMessage ? (
            <NAlert type='error' showIcon={false}>
              {asset.errorMessage}
            </NAlert>
          ) : null}
          <div class={styles.assetKvGrid}>
            <div><span>当前实例</span><strong>{this.state.assetLogInstanceId || asset.lastInstanceId || '-'}</strong></div>
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
                  void this.loadAssetLogs(asset, this.state.assetLogInstanceId || asset.lastInstanceId)
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
          <NSpin show={this.state.assetHistoryLoading}>
            <NSpace vertical>
              {this.state.assetHistoryError ? (
                <NAlert type='error' showIcon={false}>
                  {this.state.assetHistoryError}
                </NAlert>
              ) : null}
              <NDataTable
                columns={[
                  {
                    title: '实例',
                    key: 'id',
                    render: (row: SyncTaskHistoryRow) => (
                      <NButton
                        text
                        type='primary'
                        disabled={!row.instanceId}
                        onClick={async () => {
                          if (!row.instanceId) return
                          await this.loadAssetLogsForInstance(selectedAsset, row.instanceId)
                          this.state.assetDetailTab = 'LOGS'
                        }}
                      >
                        {row.id}
                      </NButton>
                    )
                  },
                  {
                    title: '状态',
                    key: 'status',
                    render: (row: SyncTaskHistoryRow) => {
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
                row-key={(row: SyncTaskHistoryRow) => row.id}
                size='small'
                pagination={false}
              />
            </NSpace>
          </NSpin>
        )
      }
      if (this.state.assetDetailTab === 'LOGS') {
        return renderAssetLogContent(selectedAsset)
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
            <label class={[styles.assetFilterField, styles.assetKeywordField]}>
              <span>关键字</span>
              <NInput
                value={this.state.assetKeyword}
                placeholder='任务名称 / 源表 / 目标表 / 工作流编码'
                clearable
                onUpdateValue={(value) => { this.state.assetKeyword = value }}
              />
            </label>
            <label class={styles.assetFilterField}>
              <span>项目</span>
              <NSelect
                value={this.state.assetProjectFilter}
                placeholder='全部'
                clearable
                options={this.assetProjectOptions}
                onUpdateValue={(value) => { this.state.assetProjectFilter = value || '' }}
              />
            </label>
            <label class={styles.assetFilterField}>
              <span>数据源类型</span>
              <NSelect
                value={this.state.assetTypeFilter}
                placeholder='全部'
                clearable
                options={this.assetTypeOptions}
                onUpdateValue={(value) => { this.state.assetTypeFilter = value || '' }}
              />
            </label>
            <label class={styles.assetFilterField}>
              <span>状态</span>
              <NSelect
                value={this.state.assetStatusFilter}
                placeholder='全部'
                clearable
                options={this.assetStatusOptions}
                onUpdateValue={(value) => { this.state.assetStatusFilter = value || '' }}
              />
            </label>
            <label class={styles.assetFilterField}>
              <span>调度</span>
              <NSelect
                value={this.state.assetScheduleFilter}
                placeholder='全部'
                clearable
                options={this.assetScheduleOptions}
                onUpdateValue={(value) => { this.state.assetScheduleFilter = value || '' }}
              />
            </label>
            <NButton
              class={styles.assetToolbarReset}
              onClick={() => {
                this.state.assetKeyword = ''
                this.state.assetProjectFilter = ''
                this.state.assetStatusFilter = ''
                this.state.assetScheduleFilter = ''
                this.state.assetTypeFilter = ''
              }}
            >
              清空
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
              scrollX={1480}
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
                    </NSpace>
                  </div>
                  <div class={styles.assetTabs}>
                    {[
                      ['OVERVIEW', '概览'],
                      ['CONFIG', '配置'],
                      ['HISTORY', '运行历史'],
                      ['LOGS', '日志诊断']
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
          {renderScheduleModal()}
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
    const sinkTargetType = this.targetDatasourceOption?.type || 'MYSQL'
    const sinkOptions = this.state.sinkOptions
    const activeSinkTab = normalizeSinkTab(sinkOptions.tab, sinkTargetType)
    if (sinkOptions.tab !== activeSinkTab) {
      sinkOptions.tab = activeSinkTab
    }
    const sinkTabOptions = getSinkTabOptions(sinkTargetType)
    const sinkHelp = (text: string) => (
      <NTooltip trigger='hover'>
        {{
          trigger: () => <span class={styles.sinkHelpIcon}>!</span>,
          default: () => <span>{text}</span>
        }}
      </NTooltip>
    )
    const renderSinkFieldLabel = (label: string, help?: string) => (
      <div class={styles.fieldLabel}>
        {label}
        {help ? sinkHelp(help) : null}
      </div>
    )
    const updateSinkCustomSql = (value: string) => {
      sinkOptions.customSql = value
      this.state.sinkCustomSql = value
    }
    const renderDorisSinkConfig = () => {
      if (activeSinkTab === 'BASE') {
        return (
          <div class={styles.sinkFormGrid}>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('FE HTTP 地址（fenodes）', 'Doris Stream Load 使用 FE HTTP 端口，通常是 8030；多个 FE 用英文逗号分隔。')}
              <NInput
                value={sinkOptions.dorisFenodes}
                placeholder='默认使用目标数据源 host:8030'
                onUpdateValue={(value) => {
                  sinkOptions.dorisFenodes = value
                }}
              />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('FE MySQL 查询端口（query-port）')}
              <NInput
                value={sinkOptions.dorisQueryPort}
                onUpdateValue={(value) => {
                  sinkOptions.dorisQueryPort = value
                }}
              />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('Label 前缀（sink.label-prefix）')}
              <NInput
                value={sinkOptions.dorisLabelPrefix}
                onUpdateValue={(value) => {
                  sinkOptions.dorisLabelPrefix = value
                }}
              />
            </div>
          </div>
        )
      }
      if (activeSinkTab === 'MODE') {
        return (
          <div class={styles.sinkFormGrid}>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('结构处理（schema_save_mode）', 'CREATE 表不存在时自动建表；RECREATE 会重建表；ERROR 表不存在时报错。')}
              <NSelect
                value={sinkOptions.schemaSaveMode}
                options={[
                  { label: 'CREATE_SCHEMA_WHEN_NOT_EXIST', value: 'CREATE_SCHEMA_WHEN_NOT_EXIST' },
                  { label: 'RECREATE_SCHEMA', value: 'RECREATE_SCHEMA' },
                  { label: 'ERROR_WHEN_SCHEMA_NOT_EXIST', value: 'ERROR_WHEN_SCHEMA_NOT_EXIST' }
                ]}
                onUpdateValue={(value) => {
                  sinkOptions.schemaSaveMode = value
                }}
              />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('数据处理（data_save_mode）', 'APPEND 追加；DROP 写入前清空；ERROR 有数据时报错；CUSTOM 先执行 custom_sql。')}
              <NSelect
                value={sinkOptions.dataSaveMode}
                options={[
                  { label: 'APPEND_DATA', value: 'APPEND_DATA' },
                  { label: 'DROP_DATA', value: 'DROP_DATA' },
                  { label: 'ERROR_WHEN_DATA_EXISTS', value: 'ERROR_WHEN_DATA_EXISTS' },
                  { label: 'CUSTOM_PROCESSING', value: 'CUSTOM_PROCESSING' }
                ]}
                onUpdateValue={(value) => {
                  sinkOptions.dataSaveMode = value
                }}
              />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('分布式事务（sink.enable-2pc）')}
              <NSelect
                value={sinkOptions.dorisEnable2pc}
                options={[{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }]}
                onUpdateValue={(value) => {
                  sinkOptions.dorisEnable2pc = value
                }}
              />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('删除事件（sink.enable-delete）')}
              <NSelect
                value={sinkOptions.dorisEnableDelete}
                options={[{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }]}
                onUpdateValue={(value) => {
                  sinkOptions.dorisEnableDelete = value
                }}
              />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('大小写敏感（case_sensitive）')}
              <NSelect
                value={sinkOptions.dorisCaseSensitive}
                options={[{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }]}
                onUpdateValue={(value) => {
                  sinkOptions.dorisCaseSensitive = value
                }}
              />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('不支持类型转换')}
              <NSelect
                value={sinkOptions.dorisNeedsUnsupportedTypeCasting}
                options={[{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }]}
                onUpdateValue={(value) => {
                  sinkOptions.dorisNeedsUnsupportedTypeCasting = value
                }}
              />
            </div>
            {sinkOptions.dataSaveMode === 'CUSTOM_PROCESSING' ? (
              <div class={[styles.fieldBlock, styles.fieldBlockSpan2]}>
                {renderSinkFieldLabel('同步前 SQL（custom_sql）')}
                <NInput
                  type='textarea'
                  value={sinkOptions.customSql}
                  autosize={{ minRows: 3, maxRows: 8 }}
                  onUpdateValue={updateSinkCustomSql}
                />
              </div>
            ) : null}
          </div>
        )
      }
      if (activeSinkTab === 'THROUGHPUT') {
        return (
          <div class={styles.sinkFormGrid}>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('单批行数（sink.buffer-size）')}
              <NInput
                value={sinkOptions.dorisBufferSize}
                disabled={sinkOptions.dorisEnable2pc === 'true'}
                onUpdateValue={(value) => {
                  sinkOptions.dorisBufferSize = value
                }}
              />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('缓冲区数量（sink.buffer-count）')}
              <NInput value={sinkOptions.dorisBufferCount} onUpdateValue={(value) => { sinkOptions.dorisBufferCount = value }} />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('HTTP 批量行数（doris.batch.size）')}
              <NInput value={sinkOptions.dorisBatchSize} onUpdateValue={(value) => { sinkOptions.dorisBatchSize = value }} />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('刷新间隔（sink.check-interval ms）')}
              <NInput value={sinkOptions.dorisCheckInterval} onUpdateValue={(value) => { sinkOptions.dorisCheckInterval = value }} />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('最大重试次数（sink.max-retries）')}
              <NInput value={sinkOptions.dorisMaxRetries} onUpdateValue={(value) => { sinkOptions.dorisMaxRetries = value }} />
            </div>
          </div>
        )
      }
      if (activeSinkTab === 'TEMPLATE') {
        return (
          <div class={styles.sinkFormGrid}>
            <div class={[styles.fieldBlock, styles.fieldBlockSpan2]}>
              {renderSinkFieldLabel('建表模板（save_mode_create_template）')}
              <NInput
                type='textarea'
                value={sinkOptions.dorisSaveModeCreateTemplate}
                autosize={{ minRows: 8, maxRows: 14 }}
                onUpdateValue={(value) => {
                  sinkOptions.dorisSaveModeCreateTemplate = value
                }}
              />
            </div>
          </div>
        )
      }
      return (
        <div class={styles.sinkFormGrid}>
          <div class={styles.fieldBlock}>
            {renderSinkFieldLabel('Stream Load 格式', '决定传给 Doris Stream Load 的数据格式。JSON 适合结构化字段，CSV 适合分隔符文本。')}
            <NSelect
              value={sinkOptions.dorisFormat}
              options={[{ label: 'json', value: 'json' }, { label: 'csv', value: 'csv' }]}
              onUpdateValue={(value) => {
                sinkOptions.dorisFormat = value
              }}
            />
          </div>
          {sinkOptions.dorisFormat === 'json' ? (
            <>
              <div class={styles.fieldBlock}>
                {renderSinkFieldLabel('read_json_by_line', 'JSON 每行一条记录时设为 true。')}
                <NSelect value={sinkOptions.dorisReadJsonByLine} options={[{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }]} onUpdateValue={(value) => { sinkOptions.dorisReadJsonByLine = value }} />
              </div>
              <div class={styles.fieldBlock}>
                {renderSinkFieldLabel('strip_outer_array', 'JSON 外层是数组时设为 true，让 Doris 拆开数组逐行写入。')}
                <NSelect value={sinkOptions.dorisStripOuterArray} options={[{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }]} onUpdateValue={(value) => { sinkOptions.dorisStripOuterArray = value }} />
              </div>
            </>
          ) : (
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('column_separator', 'CSV 字段分隔符，必须和 SeaTunnel 输出字段顺序一致。')}
              <NInput value={sinkOptions.dorisColumnSeparator} onUpdateValue={(value) => { sinkOptions.dorisColumnSeparator = value }} />
            </div>
          )}
          <div class={styles.fieldBlock}>
            {renderSinkFieldLabel('load_to_single_tablet', '默认 false 更适合分布式写入，true 只适合小表或特殊调试。')}
            <NSelect value={sinkOptions.dorisLoadToSingleTablet} options={[{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }]} onUpdateValue={(value) => { sinkOptions.dorisLoadToSingleTablet = value }} />
          </div>
        </div>
      )
    }
    const renderJdbcSinkConfig = () => {
      const isOracle = sinkTargetType === 'ORACLE'
      const exactlyOnce = sinkOptions.isExactlyOnce === 'true'
      if (activeSinkTab === 'BASE') {
        return (
          <div class={styles.sinkFormGrid}>
            <div class={[styles.fieldBlock, styles.fieldBlockSpan2]}>
              {renderSinkFieldLabel('JDBC URL', isOracle ? 'Oracle 通常使用 jdbc:oracle:thin:@//host:1521/serviceName。' : 'MySQL 建议在 URL 或 properties 中启用 rewriteBatchedStatements=true。')}
              <NInput
                value={
                  this.state.target.datasourceId
                    ? buildJdbcUrl(
                      this.state.datasourceDetails[this.state.target.datasourceId],
                      this.state.target.database || ''
                    )
                    : ''
                }
                disabled
              />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('驱动类（driver）')}
              <NInput value={buildDriver(sinkTargetType)} disabled />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('字段名处理（field_ide）', 'MySQL 通常保持 ORIGINAL；Oracle 常见字段大写，建议默认 UPPERCASE。')}
              <NSelect
                value={getJdbcFieldIde(sinkTargetType, sinkOptions)}
                options={[
                  { label: 'ORIGINAL', value: 'ORIGINAL' },
                  { label: 'LOWERCASE', value: 'LOWERCASE' },
                  { label: 'UPPERCASE', value: 'UPPERCASE' }
                ]}
                onUpdateValue={(value) => {
                  sinkOptions.fieldIde = value
                }}
              />
            </div>
            <div class={[styles.fieldBlock, styles.fieldBlockSpan2]}>
              {renderSinkFieldLabel('自定义写入 SQL（query）', '留空时按字段映射自动生成 INSERT。只有需要完全接管写入语句时才填写。')}
              <NInput
                type='textarea'
                value={sinkOptions.jdbcQuery}
                autosize={{ minRows: 3, maxRows: 8 }}
                onUpdateValue={(value) => {
                  sinkOptions.jdbcQuery = value
                }}
              />
            </div>
          </div>
        )
      }
      if (activeSinkTab === 'MODE') {
        return (
          <div class={styles.sinkFormGrid}>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('结构处理（schema_save_mode）', 'CREATE 表不存在时自动建表；RECREATE 会重建表；ERROR 表不存在时报错。')}
              <NSelect value={sinkOptions.schemaSaveMode} options={[{ label: 'CREATE_SCHEMA_WHEN_NOT_EXIST', value: 'CREATE_SCHEMA_WHEN_NOT_EXIST' }, { label: 'RECREATE_SCHEMA', value: 'RECREATE_SCHEMA' }, { label: 'ERROR_WHEN_SCHEMA_NOT_EXIST', value: 'ERROR_WHEN_SCHEMA_NOT_EXIST' }]} onUpdateValue={(value) => { sinkOptions.schemaSaveMode = value }} />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('数据处理（data_save_mode）', 'APPEND 追加；DROP 写入前清空；ERROR 有数据时报错；CUSTOM 先执行 custom_sql。')}
              <NSelect value={sinkOptions.dataSaveMode} options={[{ label: 'APPEND_DATA', value: 'APPEND_DATA' }, { label: 'DROP_DATA', value: 'DROP_DATA' }, { label: 'ERROR_WHEN_DATA_EXISTS', value: 'ERROR_WHEN_DATA_EXISTS' }, { label: 'CUSTOM_PROCESSING', value: 'CUSTOM_PROCESSING' }]} onUpdateValue={(value) => { sinkOptions.dataSaveMode = value }} />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('启用 Upsert（enable_upsert）')}
              <NSelect value={sinkOptions.enableUpsert} options={[{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }]} onUpdateValue={(value) => { sinkOptions.enableUpsert = value }} />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('自动生成写入 SQL')}
              <NSelect value={sinkOptions.generateSinkSql} options={[{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }]} onUpdateValue={(value) => { sinkOptions.generateSinkSql = value }} />
            </div>
            {sinkOptions.dataSaveMode === 'CUSTOM_PROCESSING' ? (
              <div class={[styles.fieldBlock, styles.fieldBlockSpan2]}>
                {renderSinkFieldLabel('同步前 SQL（custom_sql）')}
                <NInput type='textarea' value={sinkOptions.customSql} autosize={{ minRows: 3, maxRows: 8 }} onUpdateValue={updateSinkCustomSql} />
              </div>
            ) : null}
          </div>
        )
      }
      if (activeSinkTab === 'THROUGHPUT') {
        return (
          <div class={styles.sinkFormGrid}>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('批量行数（batch_size）')}
              <NInput value={sinkOptions.batchSize} onUpdateValue={(value) => { sinkOptions.batchSize = value }} />
            </div>
            {isOracle ? (
              <div class={styles.fieldBlock}>
                {renderSinkFieldLabel('批量间隔（batch_interval_ms）', 'Oracle Sink 支持按时间间隔触发批量提交。')}
                <NInput value={sinkOptions.batchIntervalMs} onUpdateValue={(value) => { sinkOptions.batchIntervalMs = value }} />
              </div>
            ) : null}
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('最大重试次数（max_retries）')}
              <NInput value={sinkOptions.maxRetries} onUpdateValue={(value) => { sinkOptions.maxRetries = value }} />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('连接检查超时秒数')}
              <NInput value={sinkOptions.connectionCheckTimeoutSec} onUpdateValue={(value) => { sinkOptions.connectionCheckTimeoutSec = value }} />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('Exactly Once', '开启后依赖 XA 数据源和两阶段提交；演示环境可关闭。')}
              <NSelect value={sinkOptions.isExactlyOnce} options={[{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }]} onUpdateValue={(value) => { sinkOptions.isExactlyOnce = value }} />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('自动提交（auto_commit）')}
              <NSelect value={sinkOptions.autoCommit} options={[{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }]} onUpdateValue={(value) => { sinkOptions.autoCommit = value }} />
            </div>
            <div class={[styles.fieldBlock, styles.fieldBlockSpan2]}>
              {renderSinkFieldLabel('XA 数据源类', isOracle ? 'Oracle 常用 oracle.jdbc.xa.client.OracleXADataSource。' : 'MySQL 常用 com.mysql.cj.jdbc.MysqlXADataSource。')}
              <NInput value={getXaDataSourceClassName(sinkTargetType, sinkOptions)} disabled={!exactlyOnce} onUpdateValue={(value) => { sinkOptions.xaDataSourceClassName = value }} />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('最大提交尝试次数')}
              <NInput value={sinkOptions.maxCommitAttempts} disabled={!exactlyOnce} onUpdateValue={(value) => { sinkOptions.maxCommitAttempts = value }} />
            </div>
            <div class={styles.fieldBlock}>
              {renderSinkFieldLabel('事务超时秒数')}
              <NInput value={sinkOptions.transactionTimeoutSec} disabled={!exactlyOnce} onUpdateValue={(value) => { sinkOptions.transactionTimeoutSec = value }} />
            </div>
          </div>
        )
      }
      return (
        <div class={styles.sinkFormGrid}>
          <div class={[styles.fieldBlock, styles.fieldBlockSpan2]}>
            {renderSinkFieldLabel('连接属性（properties）', isOracle ? '按 key=value 每行填写，例如 oracle.jdbc.timezoneAsRegion=false。' : '按 key=value 每行填写，例如 rewriteBatchedStatements=true。')}
            <NInput
              type='textarea'
              value={sinkOptions.jdbcProperties || getDefaultJdbcProperties(sinkTargetType)}
              autosize={{ minRows: 5, maxRows: 10 }}
              onUpdateValue={(value) => {
                sinkOptions.jdbcProperties = value
              }}
            />
          </div>
        </div>
      )
    }
    const sinkContent = (
      <div class={styles.solutionPanel}>
        <div class={styles.solutionPanelHeader}>
          <div>
            <div class={styles.sectionTitle}>数据去向</div>
            <div class={styles.hintText}>
              根据目标端数据源类型配置 SeaTunnel Sink 参数；Doris 使用 Stream Load，MySQL/Oracle 使用 JDBC 体系。
            </div>
          </div>
          <NTag bordered={false} type='warning'>{sinkTargetType}</NTag>
        </div>
        <div class={styles.sinkTabs}>
          {sinkTabOptions.map((item) => (
            <button
              key={item.value}
              type='button'
              class={[
                styles.sinkTab,
                activeSinkTab === item.value ? styles.sinkTabActive : ''
              ]}
              onClick={() => {
                sinkOptions.tab = item.value as SinkConfigTab
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
        <div class={styles.sinkTabPanel}>
          {sinkTargetType === 'DORIS' ? renderDorisSinkConfig() : renderJdbcSinkConfig()}
        </div>
        <div class={styles.codeWrap}>
          <pre class={styles.codeBlock}>{this.generatedConfig}</pre>
        </div>
      </div>
    )
    const sourceFieldOptions = this.state.fieldRows
      .filter((item) => item.sync && item.sourceColumn)
      .map((item) => ({
        label: item.sourceColumn,
        value: item.sourceColumn
      }))
    const targetFieldOptions = this.state.fieldRows
      .filter((item) => item.sync && item.targetColumn)
      .map((item) => ({
        label: item.targetColumn,
        value: item.targetColumn
      }))
    const processingContent = (
      <div class={styles.solutionPanel}>
        <div class={styles.solutionPanelHeader}>
          <div>
            <div class={styles.sectionTitle}>数据处理</div>
            <div class={styles.hintText}>
              配置字段翻译、派生字段等 ETL 处理，规则会进入 SeaTunnel Transform SQL。
            </div>
          </div>
          <NButton
            size='small'
            onClick={() => {
              this.state.dataProcessingRules.push(createDataProcessingRule())
            }}
          >
            新增规则
          </NButton>
        </div>
        <div class={[styles.processingRuleList, 'df-processing-rule-list']}>
          {this.state.dataProcessingRules.map((rule, ruleIndex) => (
            <div class={[styles.processingRuleCard, 'df-processing-rule-card']} key={rule.key}>
              <div class={styles.processingRuleHead}>
                <NSpace align='center'>
                  <NSwitch
                    value={rule.enabled}
                    onUpdateValue={(value) => {
                      rule.enabled = value
                    }}
                  />
                  <strong>字段值翻译</strong>
                  <NTag bordered={false} type='info'>CASE WHEN</NTag>
                </NSpace>
                <NButton
                  size='tiny'
                  quaternary
                  disabled={this.state.dataProcessingRules.length <= 1}
                  onClick={() => {
                    this.state.dataProcessingRules.splice(ruleIndex, 1)
                  }}
                >
                  删除
                </NButton>
              </div>
              <div class={styles.sinkFormGrid}>
                <div class={styles.fieldBlock}>
                  {renderSinkFieldLabel('源字段')}
                  <NSelect
                    filterable
                    value={rule.sourceField}
                    options={sourceFieldOptions}
                    onUpdateValue={(value) => {
                      rule.sourceField = String(value || '')
                      if (!rule.targetField) rule.targetField = String(value || '')
                    }}
                  />
                </div>
                <div class={styles.fieldBlock}>
                  {renderSinkFieldLabel('写入字段')}
                  <NSelect
                    filterable
                    value={rule.targetField}
                    options={targetFieldOptions}
                    onUpdateValue={(value) => {
                      rule.targetField = String(value || '')
                    }}
                  />
                </div>
                <div class={styles.fieldBlock}>
                  {renderSinkFieldLabel('未命中时')}
                  <NSelect
                    value={rule.defaultMode}
                    options={[
                      { label: '保留源值', value: 'KEEP_SOURCE' },
                      { label: '置为空字符串', value: 'EMPTY' }
                    ]}
                    onUpdateValue={(value) => {
                      rule.defaultMode = value as DataProcessingRule['defaultMode']
                    }}
                  />
                </div>
              </div>
              <div class={[styles.processingMappingList, 'df-processing-mapping-list']}>
                {rule.mappings.map((mapping, mappingIndex) => (
                  <div class={[styles.processingMappingRow, 'df-processing-mapping-row']} key={mapping.key}>
                    <NInput
                      value={mapping.sourceValue}
                      placeholder='源字段值'
                      onUpdateValue={(value) => {
                        mapping.sourceValue = value
                      }}
                    />
                    <span>翻译为</span>
                    <NInput
                      value={mapping.targetValue}
                      placeholder='目标字段值'
                      onUpdateValue={(value) => {
                        mapping.targetValue = value
                      }}
                    />
                    <NButton
                      size='tiny'
                      quaternary
                      disabled={rule.mappings.length <= 1}
                      onClick={() => {
                        rule.mappings.splice(mappingIndex, 1)
                      }}
                    >
                      删除
                    </NButton>
                  </div>
                ))}
              </div>
              <NButton
                size='small'
                secondary
                onClick={() => {
                  rule.mappings.push(createDataProcessingMapping())
                }}
              >
                添加翻译项
              </NButton>
            </div>
          ))}
        </div>
        <div class={styles.codeWrap}>
          <pre class={styles.codeBlock}>{this.generatedConfig}</pre>
        </div>
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
                      this.state.taskNameTouched = true
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
                  如果你希望先把同步任务保存到 DataFlow，再稍后运行，可以先执行保存动作。
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

        {this.state.runNodeData ? (
          <NodeDetailModal
            key={`${this.state.runNodeData.taskType}-${this.state.runNodeModalKey}`}
            show={this.state.runSettingsVisible}
            data={this.state.runNodeData}
            projectCode={this.state.selectedProjectCode || 0}
            saving={this.state.runningWorkflow}
            onCancel={() => {
              this.state.runSettingsVisible = false
            }}
            onSubmit={this.handleConfirmRunWorkflow}
          />
        ) : null}

        {renderScheduleModal()}

      </NSpace>
    )
  }
})

export default syncTask
