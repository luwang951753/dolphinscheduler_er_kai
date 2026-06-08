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

import { computed, defineComponent, onMounted, reactive, ref, watch } from 'vue'
import type { SelectOption } from 'naive-ui'
import {
  NButton,
  NCheckbox,
  NEmpty,
  NForm,
  NFormItem,
  NInput,
  NModal,
  NSelect,
  NSpace,
  NSpin,
  NSwitch,
  NTabPane,
  NTabs,
  NTag,
  NText
} from 'naive-ui'
import {
  getDatasourceDatabasesById,
  queryDataSourceList
} from '@/service/modules/data-source'
import type { IDataSource } from '@/service/modules/data-source/types'
import {
  generateGovernanceRuleSql,
  queryGovernanceAssets,
  queryGovernanceFields,
  queryGovernanceIssues,
  queryGovernanceLineage,
  queryGovernanceRules,
  saveGovernanceMetadata,
  saveGovernanceRule,
  trialRunGovernanceRule,
  updateGovernanceIssueStatus
} from '@/service/modules/data-governance'
import type {
  IGovernanceAsset,
  IGovernanceField,
  IGovernanceIssue,
  IGovernanceLineage,
  IGovernanceQualityRule,
  IGovernanceTrialRunResult
} from '@/service/modules/data-governance/types'
import styles from './index.module.scss'

type DetailTab = 'overview' | 'fields' | 'quality' | 'lineage' | 'issues'

const qualityOptions: SelectOption[] = [
  { label: '全部质量状态', value: '' },
  { label: '未配置', value: 'NOT_CONFIGURED' },
  { label: '未检测', value: 'NOT_RUN' },
  { label: '通过', value: 'PASS' },
  { label: '失败', value: 'FAILED' }
]

const SUPPORTED_DATASOURCE_TYPES = ['MYSQL', 'POSTGRESQL', 'ORACLE'] as const
const ASSET_QUERY_LIMIT = 80
const SYSTEM_DATABASE_NAMES = new Set([
  'information_schema',
  'mysql',
  'performance_schema',
  'sys',
  'postgres',
  'template0',
  'template1'
])
const normalizeList = (payload: any): any[] => {
  if (!payload) return []
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload.totalList)) return payload.totalList
  if (Array.isArray(payload.records)) return payload.records
  if (Array.isArray(payload.data)) return payload.data
  return []
}

const normalizeTextList = (payload: any): string[] =>
  normalizeList(payload)
    .map((item) => {
      if (typeof item === 'string') return item
      return item?.name || item?.label || item?.value || ''
    })
    .filter(Boolean)

const qualityDisplayText = (status?: string) => {
  if (status === 'PASS') return '健康'
  if (status === 'FAILED') return '异常'
  if (status === 'NOT_RUN') return '未检测'
  if (status === 'NOT_CONFIGURED') return '未配置'
  return statusText[status || ''] || status || '-'
}

const ruleTypeOptions: SelectOption[] = [
  { label: '非空校验', value: 'NOT_NULL' },
  { label: '唯一性校验', value: 'UNIQUE' },
  { label: '范围校验', value: 'RANGE' },
  { label: '枚举值校验', value: 'ENUM' },
  { label: '正则校验', value: 'REGEX' },
  { label: '自定义 SQL', value: 'CUSTOM_SQL' }
]

const severityOptions: SelectOption[] = [
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' }
]

const ruleLevelOptions: SelectOption[] = [
  { label: '字段级', value: 'FIELD' },
  { label: '表级', value: 'TABLE' }
]

const samplePolicyOptions: SelectOption[] = [
  { label: '保存前 50 条异常样本', value: 'TOP_50' },
  { label: '仅保存异常数量', value: 'COUNT_ONLY' },
  { label: '保存前 200 条异常样本', value: 'TOP_200' }
]

const failureThresholdOptions: SelectOption[] = [
  { label: '异常行数 > 0 即失败', value: 'COUNT_GT_0' },
  { label: '异常率 > 1% 即失败', value: 'RATE_GT_1' },
  { label: '异常率 > 5% 即失败', value: 'RATE_GT_5' },
  { label: '自定义阈值', value: 'CUSTOM' }
]

const frequencyOptions: SelectOption[] = [
  { label: '随同步任务完成后检测', value: 'AFTER_SYNC' },
  { label: '每天 02:00', value: 'DAILY_2' },
  { label: '每小时', value: 'HOURLY' },
  { label: '手动执行', value: 'MANUAL' }
]

const scopeOptions: SelectOption[] = [
  { label: '全表', value: 'FULL' },
  { label: '最近 1 天增量', value: 'LAST_1_DAY' },
  { label: '最近 7 天增量', value: 'LAST_7_DAYS' },
  { label: '自定义 WHERE 条件', value: 'WHERE' }
]

const ruleTypeMeta: Record<string, { label: string; defaultName: string; issueType: string }> = {
  NOT_NULL: { label: '非空校验', defaultName: '字段不能为空', issueType: '空值异常' },
  UNIQUE: { label: '唯一性校验', defaultName: '业务键不能重复', issueType: '重复数据' },
  RANGE: { label: '范围校验', defaultName: '数值必须在合理范围内', issueType: '范围异常' },
  ENUM: { label: '枚举值校验', defaultName: '字段值必须在枚举范围内', issueType: '枚举异常' },
  REGEX: { label: '正则校验', defaultName: '字段格式必须正确', issueType: '格式异常' },
  CUSTOM_SQL: { label: '自定义 SQL', defaultName: '自定义异常 SQL 检测', issueType: '自定义质量异常' }
}

const statusText: Record<string, string> = {
  NOT_CONFIGURED: '未配置',
  NOT_RUN: '未检测',
  PASS: '通过',
  FAILED: '失败',
  OPEN: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决'
}

const statusType = (status?: string) => {
  if (status === 'PASS' || status === 'RESOLVED') return 'success'
  if (status === 'FAILED' || status === 'OPEN') return 'error'
  if (status === 'PROCESSING') return 'warning'
  return 'default'
}

export default defineComponent({
  name: 'DataGovernance',
  setup() {
    const loading = ref(false)
    const assets = ref<IGovernanceAsset[]>([])
    const expandedAssetId = ref('')
    const activeTab = ref<DetailTab>('overview')
    const keyword = ref('')
    const datasourceId = ref<number | null>(null)
    const datasourceOptions = ref<SelectOption[]>([])
    const datasourceLoading = ref(false)
    const databaseOptions = ref<SelectOption[]>([])
    const databaseLoading = ref(false)
    const qualityStatus = ref('')
    const ownerFilter = ref('')
    const databaseFilter = ref('')
    const tagFilter = ref('')
    const governanceFilter = ref('')
    const sortMode = ref('UPDATED')
    const sidebarWidth = ref(220)
    const fields = ref<IGovernanceField[]>([])
    const rules = ref<IGovernanceQualityRule[]>([])
    const issues = ref<IGovernanceIssue[]>([])
    const lineage = ref<IGovernanceLineage>({ upstream: [], downstream: [] })
    const detailLoading = ref(false)
    const ruleAssetId = ref('')
    const ruleFields = ref<IGovernanceField[]>([])
    const ruleModalVisible = ref(false)
    const metadataModalVisible = ref(false)
    const trialResult = ref<IGovernanceTrialRunResult | null>(null)
    const ruleSqlMode = ref<'summary' | 'sql'>('summary')
    const currentAsset = computed(() =>
      assets.value.find((asset) => asset.id === expandedAssetId.value)
    )
    const trimmedKeyword = computed(() => keyword.value.trim())
    const hasAssetScope = computed(() =>
      Boolean(datasourceId.value && databaseFilter.value)
    )
    const ruleAsset = computed(() =>
      assets.value.find((asset) => asset.id === ruleAssetId.value)
    )

    const displayedAssets = computed(() => {
      let list = [...assets.value]
      if (ownerFilter.value === 'NO_OWNER') {
        list = list.filter((asset) => !asset.owner)
      }
      if (ownerFilter.value && ownerFilter.value !== 'NO_OWNER') {
        list = list.filter((asset) => (asset.owner || '未分配') === ownerFilter.value)
      }
      if (tagFilter.value) {
        list = list.filter((asset) => (asset.tags || []).includes(tagFilter.value))
      }
      if (governanceFilter.value === 'HAS_ISSUE') {
        list = list.filter((asset) => (asset.issueCount || 0) > 0)
      }
      if (governanceFilter.value === 'HAS_LINEAGE') {
        list = list.filter((asset) => Boolean(asset.lastSyncTask))
      }
      if (governanceFilter.value === 'HAS_RULE') {
        list = list.filter((asset) => (asset.ruleCount || 0) > 0)
      }
      if (sortMode.value === 'NAME') {
        list.sort((left, right) => left.tableName.localeCompare(right.tableName))
      }
      if (sortMode.value === 'ISSUE') {
        list.sort((left, right) => (right.issueCount || 0) - (left.issueCount || 0))
      }
      return list
    })

    const getAssetDatabase = (asset: IGovernanceAsset) =>
      `${asset.database}${asset.schema ? `.${asset.schema}` : ''}`

    const ownerOptions = computed<SelectOption[]>(() => {
      const owners = Array.from(
        new Set(assets.value.map((asset) => asset.owner || '未分配'))
      ).sort()
      return [
        { label: '全部 Owner', value: '' },
        ...owners.map((owner) => ({
          label: owner,
          value: owner === '未分配' ? 'NO_OWNER' : owner
        }))
      ]
    })

    const countByQuality = (status: string) =>
      assets.value.filter((asset) => asset.qualityStatus === status).length

    const countByTag = (tag: string) =>
      assets.value.filter((asset) => (asset.tags || []).includes(tag)).length

    const countByGovernance = (state: string) => {
      if (state === 'HAS_LINEAGE') {
        return assets.value.filter((asset) => Boolean(asset.lastSyncTask)).length
      }
      if (state === 'NO_OWNER') {
        return assets.value.filter((asset) => !asset.owner).length
      }
      if (state === 'HAS_ISSUE') {
        return assets.value.filter((asset) => (asset.issueCount || 0) > 0).length
      }
      return 0
    }

    const tagFilters = computed(() => {
      const tags = Array.from(
        new Set(assets.value.flatMap((asset) => asset.tags || []))
      ).slice(0, 4)
      return tags.length ? tags : ['核心表', '敏感', 'ODS', '订单']
    })

    const toggleSidebar = (width: number) => {
      sidebarWidth.value = width
      localStorage.setItem('dg_sidebar_width', String(width))
    }

    const metadataForm = reactive({
      owner: '',
      description: '',
      tagsText: ''
    })

    const ruleForm = reactive<IGovernanceQualityRule>({
      name: '',
      type: 'NOT_NULL',
      level: 'FIELD',
      fieldName: '',
      conditions: {},
      rangeCondition: '',
      samplePolicy: 'TOP_50',
      failureThreshold: 'COUNT_GT_0',
      severity: 'MEDIUM',
      frequency: 'MANUAL',
      enabled: true,
      manualSql: false,
      sql: ''
    })

    const ruleOptions = reactive({
      scopeType: 'FULL',
      scopeWhere: '',
      createIssue: true,
      escalateIssue: true,
      autoCloseIssue: false,
      emptyPolicy: 'ALL',
      uniqueFields: '',
      duplicatePolicy: 'EXCEPT_FIRST',
      rangeMin: '0',
      rangeMax: '1000000',
      rangeInclusive: 'BOTH',
      rangeNullPolicy: 'SKIP_NULL',
      enumValues: 'CREATED, PAID, SHIPPED, CLOSED, CANCELLED',
      enumCase: 'SENSITIVE',
      enumNullPolicy: 'SKIP_NULL',
      regexPattern: '^1[3-9][0-9]{9}$',
      regexNullPolicy: 'SKIP_NULL',
      regexSample: '13800138000',
      customSql: ''
    })

    const metrics = computed(() => {
      const total = assets.value.length
      const configured = assets.value.filter((asset) => asset.owner).length
      const passed = assets.value.filter((asset) => asset.qualityStatus === 'PASS')
        .length
      const issueCount = assets.value.reduce(
        (sum, asset) => sum + (asset.issueCount || 0),
        0
      )
      const lineageCount = assets.value.filter((asset) => Boolean(asset.lastSyncTask)).length
      const ownerRate = total ? `${Math.round((configured / total) * 100)}%` : '0%'
      const passRate = total ? `${((passed / total) * 100).toFixed(1)}%` : '0.0%'
      return [
        { label: '数据资产', value: total, hint: hasAssetScope.value ? `当前库最多展示 ${ASSET_QUERY_LIMIT} 张表` : '请先选择数据源和数据库' },
        { label: '有 Owner 资产', value: ownerRate, hint: `${Math.max(total - configured, 0)} 张表待补责任人` },
        { label: '质量通过率', value: passRate, hint: `${issueCount} 条规则异常` },
        { label: '血缘关系', value: lineageCount, hint: '来自同步任务' },
        { label: '待处理问题', value: issueCount, hint: `${issueCount} 个待处理` }
      ]
    })

    const loadAssets = async () => {
      if (!datasourceId.value || !databaseFilter.value) {
        assets.value = []
        expandedAssetId.value = ''
        return
      }
      loading.value = true
      try {
        assets.value = await queryGovernanceAssets({
          keyword: trimmedKeyword.value,
          datasourceId: datasourceId.value,
          database: databaseFilter.value,
          qualityStatus: qualityStatus.value || null,
          limit: ASSET_QUERY_LIMIT
        })
      } finally {
        loading.value = false
      }
    }

    const loadDatasourceOptions = async () => {
      datasourceLoading.value = true
      try {
        const responses = await Promise.all(
          SUPPORTED_DATASOURCE_TYPES.map((type) => queryDataSourceList({ type }))
        )
        const rows = responses.flatMap((item) => normalizeList(item)) as IDataSource[]
        datasourceOptions.value = rows
          .filter((item) => item.id && item.type && SUPPORTED_DATASOURCE_TYPES.includes(item.type as any))
          .map((item) => ({
            label: `${item.name || item.id} (${item.type})`,
            value: item.id as number
          }))
      } catch (error) {
        datasourceOptions.value = []
        window.$message.error('读取数据源列表失败，请先确认数据源配置。')
      } finally {
        datasourceLoading.value = false
      }
    }

    const getDefaultDatabase = () => {
      const databaseValues = databaseOptions.value
        .map((item) => String(item.value || ''))
        .filter(Boolean)
      return (
        databaseValues.find((database) => !SYSTEM_DATABASE_NAMES.has(database.toLowerCase())) ||
        databaseValues[0] ||
        ''
      )
    }

    const loadDatabaseOptions = async () => {
      if (!datasourceId.value) {
        databaseOptions.value = []
        return
      }
      databaseLoading.value = true
      try {
        const response = await getDatasourceDatabasesById(datasourceId.value)
        databaseOptions.value = normalizeTextList(response).map((database) => ({
          label: database,
          value: database
        }))
      } catch (error) {
        databaseOptions.value = []
        window.$message.error('读取数据库列表失败，请检查当前数据源是否可连接。')
      } finally {
        databaseLoading.value = false
      }
    }

    const initializeDefaultAssetScope = async () => {
      await loadDatasourceOptions()
      if (!datasourceOptions.value.length || datasourceId.value) {
        return
      }
      datasourceId.value = datasourceOptions.value[0].value as number
      await loadDatabaseOptions()
      const defaultDatabase = getDefaultDatabase()
      if (defaultDatabase) {
        databaseFilter.value = defaultDatabase
        await loadAssets()
      }
    }

    const reloadDetail = async (tab = activeTab.value) => {
      if (!currentAsset.value) return
      detailLoading.value = true
      try {
        if (tab === 'fields') {
          fields.value = await queryGovernanceFields(currentAsset.value.id)
        }
        if (tab === 'quality') {
          rules.value = await queryGovernanceRules(currentAsset.value.id)
        }
        if (tab === 'lineage') {
          lineage.value = await queryGovernanceLineage(currentAsset.value.id)
        }
        if (tab === 'issues') {
          issues.value = await queryGovernanceIssues(currentAsset.value.id)
        }
      } finally {
        detailLoading.value = false
      }
    }

    const expandAsset = async (asset: IGovernanceAsset) => {
      if (expandedAssetId.value === asset.id) {
        expandedAssetId.value = ''
        return
      }
      expandedAssetId.value = asset.id
      activeTab.value = 'overview'
      fields.value = []
      rules.value = []
      issues.value = []
      lineage.value = { upstream: [], downstream: [] }
    }

    const handleDatasourceChange = async (value: number | null) => {
      datasourceId.value = value
      databaseFilter.value = ''
      assets.value = []
      expandedAssetId.value = ''
      await loadDatabaseOptions()
    }

    const handleDatabaseChange = async (value: string | null) => {
      databaseFilter.value = value || ''
      assets.value = []
      expandedAssetId.value = ''
      if (databaseFilter.value) {
        await loadAssets()
      }
    }

    const setQualityStatus = (value: string) => {
      qualityStatus.value = value
      loadAssets()
    }

    const resetFilters = () => {
      keyword.value = ''
      datasourceId.value = null
      qualityStatus.value = ''
      ownerFilter.value = ''
      databaseFilter.value = ''
      tagFilter.value = ''
      governanceFilter.value = ''
      sortMode.value = 'UPDATED'
      databaseOptions.value = []
      assets.value = []
      expandedAssetId.value = ''
    }

    const openMetadataModal = () => {
      if (!currentAsset.value) return
      metadataForm.owner = currentAsset.value.owner || ''
      metadataForm.description = currentAsset.value.description || ''
      metadataForm.tagsText = (currentAsset.value.tags || []).join(',')
      metadataModalVisible.value = true
    }

    const saveMetadata = async () => {
      if (!currentAsset.value) return
      await saveGovernanceMetadata(currentAsset.value.id, {
        owner: metadataForm.owner,
        description: metadataForm.description,
        tags: metadataForm.tagsText
          .split(',')
          .map((tag) => tag.trim())
          .filter(Boolean)
      })
      metadataModalVisible.value = false
      await loadAssets()
    }

    const resetRuleForm = () => {
      const defaultField =
        ruleFields.value.find((field) => !field.primaryKey && field.nullable)?.name ||
        ruleFields.value.find((field) => !field.primaryKey)?.name ||
        ruleFields.value[0]?.name ||
        ''
      ruleForm.id = undefined
      ruleForm.name = '字段不能为空'
      ruleForm.type = 'NOT_NULL'
      ruleForm.level = 'FIELD'
      ruleForm.fieldName = defaultField
      ruleForm.conditions = {}
      ruleForm.rangeCondition = ''
      ruleForm.samplePolicy = 'TOP_50'
      ruleForm.failureThreshold = 'COUNT_GT_0'
      ruleForm.severity = 'MEDIUM'
      ruleForm.frequency = 'MANUAL'
      ruleForm.enabled = true
      ruleForm.manualSql = false
      ruleForm.sql = ''
      ruleOptions.scopeType = 'FULL'
      ruleOptions.scopeWhere = ''
      ruleOptions.createIssue = true
      ruleOptions.escalateIssue = true
      ruleOptions.autoCloseIssue = false
      ruleOptions.emptyPolicy = 'ALL'
      ruleOptions.uniqueFields = defaultField
      ruleOptions.duplicatePolicy = 'EXCEPT_FIRST'
      ruleOptions.rangeMin = '0'
      ruleOptions.rangeMax = '1000000'
      ruleOptions.rangeInclusive = 'BOTH'
      ruleOptions.rangeNullPolicy = 'SKIP_NULL'
      ruleOptions.enumValues = 'CREATED, PAID, SHIPPED, CLOSED, CANCELLED'
      ruleOptions.enumCase = 'SENSITIVE'
      ruleOptions.enumNullPolicy = 'SKIP_NULL'
      ruleOptions.regexPattern = '^1[3-9][0-9]{9}$'
      ruleOptions.regexNullPolicy = 'SKIP_NULL'
      ruleOptions.regexSample = '13800138000'
      ruleOptions.customSql = ''
      trialResult.value = null
      ruleSqlMode.value = 'summary'
    }

    const openRuleModal = async () => {
      ruleAssetId.value = currentAsset.value?.id || ruleAssetId.value || assets.value[0]?.id || ''
      await loadRuleAssetFields()
      resetRuleForm()
      await regenerateRuleSql()
      ruleModalVisible.value = true
    }

    const loadRuleAssetFields = async () => {
      if (!ruleAssetId.value) {
        ruleFields.value = []
        return
      }
      ruleFields.value = await queryGovernanceFields(ruleAssetId.value)
    }

    const updateRuleAsset = async (assetId: string) => {
      ruleAssetId.value = assetId
      await loadRuleAssetFields()
      resetRuleForm()
      await regenerateRuleSql()
    }

    const regenerateRuleSql = async () => {
      if (!ruleAsset.value) return
      syncRulePayload()
      if (ruleForm.type === 'CUSTOM_SQL' && ruleOptions.customSql.trim()) {
        ruleForm.sql = ruleOptions.customSql
        ruleForm.manualSql = true
        return
      }
      ruleForm.sql = await generateGovernanceRuleSql(ruleAsset.value.id, ruleForm)
      ruleForm.manualSql = false
    }

    const runTrial = async () => {
      if (!ruleAsset.value) return
      syncRulePayload()
      if (!ruleForm.id) {
        ruleForm.id = `rule-${Date.now()}`
      }
      const result = await trialRunGovernanceRule(ruleAsset.value.id, {
        rule: ruleForm,
        sql: ruleForm.sql
      })
      trialResult.value = result
      ruleForm.lastRunAt = result.executedAt
      ruleForm.abnormalCount = result.abnormalCount
      ruleForm.abnormalRate = result.abnormalRate
      ruleForm.status = result.passed ? 'PASS' : 'FAILED'
    }

    const saveRule = async () => {
      if (!ruleAsset.value) return
      syncRulePayload()
      await saveGovernanceRule(ruleAsset.value.id, ruleForm)
      ruleModalVisible.value = false
      if (expandedAssetId.value === ruleAsset.value.id) {
        activeTab.value = 'quality'
        await reloadDetail('quality')
      }
      await loadAssets()
    }

    const saveRuleWithEnabled = async (enabled: boolean) => {
      ruleForm.enabled = enabled
      await saveRule()
    }

    const syncMetadata = async () => {
      if (!hasAssetScope.value) {
        window.$message.warning('请先选择数据源和数据库，再同步元数据。')
        return
      }
      expandedAssetId.value = ''
      activeTab.value = 'overview'
      fields.value = []
      rules.value = []
      issues.value = []
      lineage.value = { upstream: [], downstream: [] }
      await loadAssets()
    }

    const runExistingRule = async (rule: IGovernanceQualityRule) => {
      if (!currentAsset.value) return
      await trialRunGovernanceRule(currentAsset.value.id, {
        rule,
        sql: rule.sql
      })
      await reloadDetail('quality')
      await reloadDetail('issues')
      await loadAssets()
    }

    const toggleRuleEnabled = async (rule: IGovernanceQualityRule) => {
      if (!currentAsset.value) return
      await saveGovernanceRule(currentAsset.value.id, {
        ...rule,
        enabled: !rule.enabled
      })
      await reloadDetail('quality')
      await loadAssets()
    }

    const editExistingRule = async (rule: IGovernanceQualityRule) => {
      if (!currentAsset.value) return
      ruleAssetId.value = currentAsset.value.id
      await loadRuleAssetFields()
      Object.assign(ruleForm, rule)
      ruleOptions.rangeMin = String(rule.conditions?.min || ruleOptions.rangeMin)
      ruleOptions.rangeMax = String(rule.conditions?.max || ruleOptions.rangeMax)
      ruleOptions.enumValues = String(rule.conditions?.values || ruleOptions.enumValues)
      ruleOptions.regexPattern = String(rule.conditions?.pattern || ruleOptions.regexPattern)
      ruleOptions.uniqueFields = String(rule.conditions?.uniqueFields || rule.fieldName || ruleOptions.uniqueFields)
      ruleOptions.scopeType = rule.rangeCondition ? 'WHERE' : 'FULL'
      ruleOptions.scopeWhere = rule.rangeCondition || ''
      ruleOptions.createIssue = rule.createIssue !== false
      ruleOptions.escalateIssue = rule.escalateIssue !== false
      ruleOptions.autoCloseIssue = rule.autoCloseIssue === true
      trialResult.value = null
      ruleSqlMode.value = 'sql'
      ruleModalVisible.value = true
    }

    const updateRuleType = async (value: string) => {
      ruleForm.type = value
      const meta = ruleTypeMeta[value]
      if (meta) {
        ruleForm.name = meta.defaultName
      }
      ruleForm.level = value === 'CUSTOM_SQL' ? 'TABLE' : 'FIELD'
      if (value === 'UNIQUE') {
        ruleOptions.uniqueFields = ruleForm.fieldName || ruleFields.value[0]?.name || ''
      }
      if (value === 'CUSTOM_SQL') {
        const table = ruleAsset.value?.tableName || 'target_table'
        ruleOptions.customSql = `select count(*) as abnormal_count\nfrom ${table}\nwhere 1 = 0`
        ruleForm.sql = ruleOptions.customSql
        ruleForm.manualSql = true
      } else {
        await regenerateRuleSql()
      }
    }

    const getScopeCondition = () => {
      if (ruleOptions.scopeType === 'LAST_1_DAY') return "dt >= current_date - interval '1 day'"
      if (ruleOptions.scopeType === 'LAST_7_DAYS') return "dt >= current_date - interval '7 day'"
      if (ruleOptions.scopeType === 'WHERE') return ruleOptions.scopeWhere
      return ''
    }

    const getScopeText = () => {
      const option = scopeOptions.find((item) => item.value === ruleOptions.scopeType)
      if (ruleOptions.scopeType === 'WHERE') {
        return `WHERE ${ruleOptions.scopeWhere || '未填写'}`
      }
      return option?.label || '全表'
    }

    const getRuleConditionText = () => {
      const field = ruleForm.level === 'TABLE' ? '整表' : ruleForm.fieldName || '未选择字段'
      if (ruleForm.type === 'NOT_NULL') return `${field} 不能为 NULL、空字符串或空格`
      if (ruleForm.type === 'UNIQUE') return `${ruleOptions.uniqueFields || field} 组合值不能重复`
      if (ruleForm.type === 'RANGE') return `${field} 必须在 ${ruleOptions.rangeMin} 到 ${ruleOptions.rangeMax} 之间`
      if (ruleForm.type === 'ENUM') return `${field} 必须属于：${ruleOptions.enumValues}`
      if (ruleForm.type === 'REGEX') return `${field} 必须匹配正则：${ruleOptions.regexPattern}`
      return 'SQL 必须返回 abnormal_count，且 abnormal_count 等于异常记录数'
    }

    const syncRulePayload = () => {
      const conditions: Record<string, string> = {}
      if (ruleForm.type === 'RANGE') {
        conditions.min = ruleOptions.rangeMin
        conditions.max = ruleOptions.rangeMax
      }
      if (ruleForm.type === 'ENUM') {
        conditions.values = ruleOptions.enumValues
      }
      if (ruleForm.type === 'REGEX') {
        conditions.pattern = ruleOptions.regexPattern
      }
      if (ruleForm.type === 'UNIQUE') {
        conditions.uniqueFields = ruleOptions.uniqueFields
      }
      ruleForm.conditions = conditions
      ruleForm.rangeCondition = getScopeCondition()
      ruleForm.samplePolicy = ruleForm.samplePolicy || 'TOP_50'
      ruleForm.failureThreshold = ruleForm.failureThreshold || 'COUNT_GT_0'
      ruleForm.frequency = ruleForm.frequency || 'AFTER_SYNC'
      ruleForm.createIssue = ruleOptions.createIssue
      ruleForm.escalateIssue = ruleOptions.escalateIssue
      ruleForm.autoCloseIssue = ruleOptions.autoCloseIssue
      if (ruleForm.type === 'CUSTOM_SQL' && ruleOptions.customSql.trim() && !ruleForm.manualSql) {
        ruleForm.sql = ruleOptions.customSql
      }
    }

    const validationItems = computed(() => {
      const sql = ruleForm.sql || ''
      return [
        { label: ruleAsset.value ? '资产已选择' : '资产未选择', ok: Boolean(ruleAsset.value) },
        { label: ruleForm.level === 'TABLE' ? '表级规则' : '字段已选择', ok: ruleForm.level === 'TABLE' || Boolean(ruleForm.fieldName) },
        { label: '失败阈值已设置', ok: Boolean(ruleForm.failureThreshold) },
        { label: '检测 SQL 已填写', ok: Boolean(sql.trim()) },
        { label: sql.toLowerCase().includes('abnormal_count') ? 'SQL 返回 abnormal_count' : 'SQL 缺少 abnormal_count', ok: sql.toLowerCase().includes('abnormal_count') }
      ]
    })

    const rulePreview = computed(() => {
      const meta = ruleTypeMeta[ruleForm.type] || ruleTypeMeta.NOT_NULL
      const field = ruleForm.level === 'TABLE' ? '整表' : ruleForm.fieldName || '未选择字段'
      const threshold = failureThresholdOptions.find((item) => item.value === ruleForm.failureThreshold)?.label || '异常行数 > 0 即失败'
      const severity = severityOptions.find((item) => item.value === ruleForm.severity)?.label || '中'
      const frequency = frequencyOptions.find((item) => item.value === ruleForm.frequency)?.label || '手动执行'
      const autoIssue = ruleOptions.createIssue ? '开启' : '关闭'
      const escalate = ruleOptions.escalateIssue ? '连续失败升级' : '不自动升级'
      const autoClose = ruleOptions.autoCloseIssue ? '检测通过自动关闭历史问题' : '历史问题需人工关闭'
      return [
        `规则名称：${ruleForm.name || meta.defaultName}`,
        `治理资产：${ruleAsset.value?.fullName || '未选择资产'}`,
        `规则类型：${meta.label}（${ruleForm.level === 'TABLE' ? '表级' : '字段级'}）`,
        `检测对象：${field}`,
        `检测范围：${getScopeText()}`,
        `失败条件：${getRuleConditionText()}`,
        `失败阈值：${threshold}`,
        `问题策略：${autoIssue}自动生成“${meta.issueType}”问题，严重程度${severity}，${escalate}，${autoClose}`,
        `执行频率：${frequency}`
      ].join('\n')
    })

    const renderRuleConditionFields = () => {
      if (ruleForm.type === 'NOT_NULL') {
        return (
          <>
            <div class={styles.ruleAlert}>非空校验用于发现字段为空、空字符串或仅包含空格的记录。订单号、主键、业务编号这类字段通常必须配置。</div>
            <NFormItem label='空值判断'>
              <NSelect
                value={ruleOptions.emptyPolicy}
                options={[
                  { label: 'NULL、空字符串、空格都算异常', value: 'ALL' },
                  { label: '仅 NULL 算异常', value: 'NULL_ONLY' },
                  { label: 'NULL 和空字符串算异常', value: 'NULL_EMPTY' }
                ]}
                onUpdateValue={(value) => (ruleOptions.emptyPolicy = value as string)}
              />
            </NFormItem>
            <NFormItem label='样本保存'>
              <NSelect value={ruleForm.samplePolicy} options={samplePolicyOptions} onUpdateValue={(value) => (ruleForm.samplePolicy = value as string)} />
            </NFormItem>
          </>
        )
      }
      if (ruleForm.type === 'UNIQUE') {
        return (
          <>
            <div class={styles.ruleAlert}>唯一性校验需要明确唯一键。单字段唯一适合订单号，多字段唯一适合业务主键组合。</div>
            <NFormItem label='唯一键字段'>
              <NInput value={ruleOptions.uniqueFields} onUpdateValue={(value) => (ruleOptions.uniqueFields = value)} />
            </NFormItem>
            <NFormItem label='重复处理口径'>
              <NSelect
                value={ruleOptions.duplicatePolicy}
                options={[
                  { label: '除第一条外均计为异常', value: 'EXCEPT_FIRST' },
                  { label: '重复组内全部记录计为异常', value: 'ALL_DUPLICATED' }
                ]}
                onUpdateValue={(value) => (ruleOptions.duplicatePolicy = value as string)}
              />
            </NFormItem>
          </>
        )
      }
      if (ruleForm.type === 'RANGE') {
        return (
          <>
            <div class={styles.ruleAlert}>范围校验适合金额、数量、比例、年龄等数值字段。边界必须明确，否则容易误报。</div>
            <NFormItem label='最小值'>
              <NInput value={ruleOptions.rangeMin} onUpdateValue={(value) => (ruleOptions.rangeMin = value)} />
            </NFormItem>
            <NFormItem label='最大值'>
              <NInput value={ruleOptions.rangeMax} onUpdateValue={(value) => (ruleOptions.rangeMax = value)} />
            </NFormItem>
            <NFormItem label='边界包含'>
              <NSelect
                value={ruleOptions.rangeInclusive}
                options={[
                  { label: '包含最小值和最大值', value: 'BOTH' },
                  { label: '只包含最小值', value: 'MIN' },
                  { label: '只包含最大值', value: 'MAX' },
                  { label: '均不包含', value: 'NONE' }
                ]}
                onUpdateValue={(value) => (ruleOptions.rangeInclusive = value as string)}
              />
            </NFormItem>
            <NFormItem label='空值处理'>
              <NSelect
                value={ruleOptions.rangeNullPolicy}
                options={[
                  { label: '空值不参与范围检测', value: 'SKIP_NULL' },
                  { label: '空值也算异常', value: 'NULL_ABNORMAL' }
                ]}
                onUpdateValue={(value) => (ruleOptions.rangeNullPolicy = value as string)}
              />
            </NFormItem>
          </>
        )
      }
      if (ruleForm.type === 'ENUM') {
        return (
          <>
            <div class={styles.ruleAlert}>枚举校验用于状态、类型、渠道等有限取值字段。建议保存业务含义，后续可沉淀成数据标准。</div>
            <NFormItem class={styles.span2} label='允许值'>
              <NInput value={ruleOptions.enumValues} onUpdateValue={(value) => (ruleOptions.enumValues = value)} />
            </NFormItem>
            <NFormItem label='大小写规则'>
              <NSelect
                value={ruleOptions.enumCase}
                options={[
                  { label: '区分大小写', value: 'SENSITIVE' },
                  { label: '忽略大小写', value: 'IGNORE_CASE' }
                ]}
                onUpdateValue={(value) => (ruleOptions.enumCase = value as string)}
              />
            </NFormItem>
            <NFormItem label='异常口径'>
              <NSelect
                value={ruleOptions.enumNullPolicy}
                options={[
                  { label: '空值单独按非空规则处理', value: 'SKIP_NULL' },
                  { label: '空值也按枚举异常处理', value: 'NULL_ABNORMAL' }
                ]}
                onUpdateValue={(value) => (ruleOptions.enumNullPolicy = value as string)}
              />
            </NFormItem>
          </>
        )
      }
      if (ruleForm.type === 'REGEX') {
        return (
          <>
            <div class={styles.ruleAlert}>正则校验适合手机号、邮箱、证件号等格式字段。正则规则建议先试运行，避免误伤历史数据。</div>
            <NFormItem class={styles.span2} label='正则表达式'>
              <NInput value={ruleOptions.regexPattern} onUpdateValue={(value) => (ruleOptions.regexPattern = value)} />
            </NFormItem>
            <NFormItem label='空值处理'>
              <NSelect
                value={ruleOptions.regexNullPolicy}
                options={[
                  { label: '空值跳过格式检测', value: 'SKIP_NULL' },
                  { label: '空值也算异常', value: 'NULL_ABNORMAL' }
                ]}
                onUpdateValue={(value) => (ruleOptions.regexNullPolicy = value as string)}
              />
            </NFormItem>
            <NFormItem label='示例值'>
              <NInput value={ruleOptions.regexSample} onUpdateValue={(value) => (ruleOptions.regexSample = value)} />
            </NFormItem>
          </>
        )
      }
      return (
        <>
          <div class={[styles.ruleAlert, styles.warn]}>自定义 SQL 必须返回 abnormal_count 字段。建议只写 SELECT，不允许在质量规则里执行 DDL/DML。</div>
          <NFormItem class={styles.span2} label='检测 SQL'>
            <NInput
              type='textarea'
              value={ruleOptions.customSql}
              onUpdateValue={(value) => {
                ruleOptions.customSql = value
                ruleForm.sql = value
                ruleForm.manualSql = true
              }}
            />
          </NFormItem>
        </>
      )
    }

    const updateIssue = async (issue: IGovernanceIssue, status: string) => {
      if (!currentAsset.value) return
      await updateGovernanceIssueStatus(currentAsset.value.id, issue.id, status)
      await reloadDetail('issues')
      await loadAssets()
    }

    const handleSidebarMouseDown = (event: MouseEvent) => {
      const startX = event.clientX
      const startWidth = sidebarWidth.value
      const collapseThreshold = 96
      const clampWidth = (width: number) => {
        if (width <= collapseThreshold) return 0
        return Math.max(180, Math.min(360, width))
      }

      document.body.classList.add(styles.resizingSidebar)
      const onMove = (moveEvent: MouseEvent) => {
        sidebarWidth.value = clampWidth(startWidth + moveEvent.clientX - startX)
      }
      const onUp = () => {
        toggleSidebar(sidebarWidth.value)
        document.body.classList.remove(styles.resizingSidebar)
        window.removeEventListener('mousemove', onMove)
        window.removeEventListener('mouseup', onUp)
      }

      window.addEventListener('mousemove', onMove)
      window.addEventListener('mouseup', onUp)
    }

    watch(activeTab, (tab) => {
      reloadDetail(tab)
    })

    watch(
      () => [
        ruleAssetId.value,
        ruleForm.type,
        ruleForm.fieldName,
        ruleOptions.scopeType,
        ruleOptions.scopeWhere,
        ruleOptions.rangeMin,
        ruleOptions.rangeMax,
        ruleOptions.enumValues,
        ruleOptions.regexPattern
      ],
      () => {
        if (ruleModalVisible.value && !ruleForm.manualSql) {
          regenerateRuleSql()
        }
      }
    )

    onMounted(() => {
      const savedWidth = Number(localStorage.getItem('dg_sidebar_width'))
      if (Number.isFinite(savedWidth) && savedWidth >= 0) {
        sidebarWidth.value = savedWidth
      }
      initializeDefaultAssetScope()
    })

    const renderFieldTable = () => {
      if (!fields.value.length && !detailLoading.value) {
        return <NEmpty description='暂无字段信息' />
      }
      return (
        <table class={styles.miniTable}>
          <thead>
            <tr>
              <th>字段</th>
              <th>类型</th>
              <th>注释</th>
              <th>约束</th>
              <th>标签</th>
            </tr>
          </thead>
          <tbody>
            {fields.value.map((field) => (
              <tr>
                <td>{field.name}</td>
                <td>{field.type}</td>
                <td>{field.comment || '-'}</td>
                <td>
                  {field.primaryKey ? 'PK ' : ''}
                  {field.nullable ? '可空' : '非空'}
                </td>
                <td>
                  {field.sensitiveTag ? (
                    <NTag size='small' type='warning'>{field.sensitiveTag}</NTag>
                  ) : '-'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )
    }

    const renderQualityRules = () => {
      if (!rules.value.length && !detailLoading.value) {
        return (
          <div class={styles.emptyBlock}>
            <NText depth={3}>当前资产还没有质量规则。</NText>
          </div>
        )
      }
      return (
        <div class={styles.cardList}>
          {rules.value.map((rule) => (
            <div class={styles.qualityCard}>
              <div class={styles.cardHead}>
                <div class={styles.cardTitle}>
                  <strong>{rule.name}</strong>
                  <span>{ruleTypeMeta[rule.type]?.label || rule.type}；{rule.fieldName || '整表'}；{rule.frequency || 'MANUAL'}</span>
                </div>
                <NTag type={statusType(rule.status) as any}>
                  {statusText[rule.status || ''] || rule.status || '未检测'}
                </NTag>
              </div>
              <div class={styles.cardMeta}>
                <span>异常数：{rule.abnormalCount ?? '-'}</span>
                <span>最近检测：{rule.lastRunAt || '暂无'}</span>
                <span>阈值：{rule.failureThreshold || 'COUNT_GT_0'}</span>
              </div>
              <div class={styles.cardActions}>
                <NButton size='tiny' onClick={() => runExistingRule(rule)}>立即检测</NButton>
                <NButton size='tiny' onClick={() => editExistingRule(rule)}>
                  编辑
                </NButton>
                <NButton size='tiny' onClick={() => toggleRuleEnabled(rule)}>
                  {rule.enabled === false ? '启用' : '禁用'}
                </NButton>
              </div>
            </div>
          ))}
        </div>
      )
    }

    const renderIssues = () => {
      if (!issues.value.length && !detailLoading.value) {
        return <NEmpty description='暂无治理问题' />
      }
      return (
        <div class={styles.cardList}>
          {issues.value.map((issue) => (
            <div class={styles.issueCard}>
              <div class={styles.cardHead}>
                <div class={styles.cardTitle}>
                  <strong>{issue.title}</strong>
                  <span>异常数：{issue.abnormalCount ?? '-'}；发现时间：{issue.discoveredAt || '暂无'}</span>
                </div>
                <NSelect
                  size='small'
                  class={styles.issueStatus}
                  value={issue.status}
                  options={[
                    { label: '待处理', value: 'OPEN' },
                    { label: '处理中', value: 'PROCESSING' },
                    { label: '已解决', value: 'RESOLVED' }
                  ]}
                  onUpdateValue={(value) => updateIssue(issue, value)}
                />
              </div>
              <div class={styles.statusFlow}>
                {[
                  ['OPEN', '待处理'],
                  ['PROCESSING', '处理中'],
                  ['RESOLVED', '已解决']
                ].map(([status, label]) => (
                  <button
                    class={[styles.statusStep, issue.status === status ? styles.activeStep : '']}
                    onClick={() => updateIssue(issue, status)}
                  >
                    {label}
                  </button>
                ))}
              </div>
              <div class={styles.cardActions}>
                <NButton size='tiny' onClick={() => updateIssue(issue, 'PROCESSING')}>开始处理</NButton>
                <NButton size='tiny' onClick={() => updateIssue(issue, 'RESOLVED')}>标记解决</NButton>
              </div>
            </div>
          ))}
        </div>
      )
    }

    const renderAssetDetail = (asset: IGovernanceAsset) => (
      <div class={styles.expandedPanel}>
        <div class={styles.expandedHeader}>
          <span>当前资产详情</span>
          <NSpace size={8}>
            <NTag type={statusType(asset.qualityStatus) as any}>
              {qualityDisplayText(asset.qualityStatus)}
            </NTag>
            <NButton size='small' onClick={openMetadataModal}>编辑元数据</NButton>
          </NSpace>
        </div>
        <NTabs value={activeTab.value} onUpdateValue={(value) => (activeTab.value = value as DetailTab)}>
          <NTabPane name='overview' tab='概览'>
            <div class={styles.overviewGrid}>
              {[
                ['Owner', asset.owner || '未设置'],
                ['字段数', asset.fieldCount ?? '进入字段页后加载'],
                ['最近检测', asset.lastCheckTime || '暂无'],
                ['最近同步', asset.lastSyncTask || '暂无'],
                ['数据源', asset.datasourceName],
                ['数据库 / Schema', `${asset.database}${asset.schema ? ` / ${asset.schema}` : ''}`],
                ['说明', asset.description || '未填写']
              ].map(([label, value]) => (
                <div class={styles.overviewItem}>
                  <div class={styles.overviewLabel}>{label}</div>
                  <div class={styles.overviewValue}>{value}</div>
                </div>
              ))}
            </div>
          </NTabPane>
          <NTabPane name='fields' tab='字段'>
            <NSpin show={detailLoading.value}>
              {renderFieldTable()}
            </NSpin>
          </NTabPane>
          <NTabPane name='quality' tab='质量'>
            <div class={styles.qualityToolbar}>
              <NText depth={3}>质量规则保存后会在这里回显，支持试运行生成问题。</NText>
              <NButton type='primary' onClick={openRuleModal}>
                新建质量规则
              </NButton>
            </div>
            <NSpin show={detailLoading.value}>
              {renderQualityRules()}
            </NSpin>
          </NTabPane>
          <NTabPane name='lineage' tab='血缘'>
            <NSpin show={detailLoading.value}>
              <div class={styles.lineageGuide}>
                <strong>当前表血缘</strong>
                <span>这里展示当前表和字段的上下游关系，来源于同步任务保存和执行时沉淀的源表、目标表、字段映射信息。</span>
              </div>
              <div class={styles.lineageCanvas}>
                <div class={styles.lineageSide}>
                  {lineage.value.upstream.length ? lineage.value.upstream.map((node) => (
                    <div class={styles.lineageNode}>
                      <strong>{node.assetName}</strong>
                      <span>{node.relationType || '上游表'} · {node.syncTaskName || '同步任务'}</span>
                      <em>{node.fieldMappings?.length ? `${node.fieldMappings.length} 个字段映射` : '表级血缘'}</em>
                    </div>
                  )) : <NEmpty description='暂无上游血缘' />}
                </div>
                <div class={styles.lineageCenter}>
                  <strong>{asset.tableName}</strong>
                  <span>当前资产</span>
                </div>
                <div class={styles.lineageSide}>
                  {lineage.value.downstream.length ? lineage.value.downstream.map((node) => (
                    <div class={styles.lineageNode}>
                      <strong>{node.assetName}</strong>
                      <span>{node.relationType || '下游表'} · {node.syncTaskName || '同步任务'}</span>
                      <em>{node.fieldMappings?.length ? `${node.fieldMappings.length} 个字段映射` : '表级血缘'}</em>
                    </div>
                  )) : <NEmpty description='暂无下游血缘' />}
                </div>
              </div>
              <div class={styles.lineageTask}>
                <div>
                  <strong>血缘来源：同步任务保存与执行记录</strong>
                  <span>后续同步任务生成字段映射时，会沉淀当前表、字段的上下游关系。</span>
                </div>
                <NButton size='small' disabled={!asset.lastSyncTask}>
                  查看同步任务
                </NButton>
              </div>
            </NSpin>
          </NTabPane>
          <NTabPane name='issues' tab='问题'>
            <NSpin show={detailLoading.value}>
              {renderIssues()}
            </NSpin>
          </NTabPane>
        </NTabs>
      </div>
    )

    return () => (
      <div class={styles.governancePage}>
        <div class={styles.toolbar}>
          <div class={styles.titleBlock}>
            <h2 class={styles.title}>数据治理</h2>
            <div class={styles.subtitle}>资产目录、质量规则、血缘和问题闭环</div>
          </div>
          <NInput
            value={keyword.value}
            placeholder='输入表名关键词后按 Enter 查询'
            clearable
            onUpdateValue={(value) => (keyword.value = value)}
            onKeyup={(event) => {
              if ((event as KeyboardEvent).key === 'Enter') {
                loadAssets()
              }
            }}
          />
          <NSpace justify='end'>
            <NButton disabled={!hasAssetScope.value} onClick={syncMetadata}>
              同步元数据
            </NButton>
            <NButton type='primary' disabled={!assets.value.length} onClick={openRuleModal}>
              新建质量规则
            </NButton>
          </NSpace>
        </div>

        <div class={styles.metricGrid}>
          {metrics.value.map((metric) => (
            <div class={styles.metricItem}>
              <div class={styles.metricLabel}>{metric.label}</div>
              <div class={styles.metricValue}>{metric.value}</div>
              <div class={styles.metricHint}>{metric.hint}</div>
            </div>
          ))}
        </div>

        <div
          class={[styles.workspace, sidebarWidth.value === 0 ? styles.sidebarCollapsed : '']}
          style={{ '--side-width': `${sidebarWidth.value}px` }}
        >
          <aside class={styles.sidePanel}>
            <div class={styles.panelHead}>治理筛选</div>
            <div class={styles.filters}>
              <div class={styles.filterGroup}>
                <div class={styles.filterTitle}>资产范围</div>
              </div>
              <div class={styles.filterGroup}>
                <div class={styles.filterTitle}>质量状态</div>
                <div class={styles.chipList}>
                  {[
                    ['健康', 'PASS'],
                    ['有风险', 'NOT_RUN'],
                    ['异常', 'FAILED']
                  ].map(([label, value]) => (
                    <button
                      class={[styles.chip, qualityStatus.value === value ? styles.active : '']}
                      onClick={() => setQualityStatus(value)}
                    >
                      {label} <em>({countByQuality(value)})</em>
                    </button>
                  ))}
                </div>
              </div>
              <div class={styles.filterGroup}>
                <div class={styles.filterTitle}>标签</div>
                <div class={styles.chipList}>
                  {tagFilters.value.map((tag) => (
                    <button
                      class={[styles.chip, tagFilter.value === tag ? styles.active : '']}
                      onClick={() => (tagFilter.value = tagFilter.value === tag ? '' : tag)}
                    >
                      {tag} <em>({countByTag(tag)})</em>
                    </button>
                  ))}
                </div>
              </div>
              <div class={styles.filterGroup}>
                <div class={styles.filterTitle}>治理状态</div>
                <div class={styles.chipList}>
                  <button
                    class={[styles.chip, governanceFilter.value === 'HAS_LINEAGE' ? styles.active : '']}
                    onClick={() => (governanceFilter.value = governanceFilter.value === 'HAS_LINEAGE' ? '' : 'HAS_LINEAGE')}
                  >
                    有血缘 <em>({countByGovernance('HAS_LINEAGE')})</em>
                  </button>
                  <button
                    class={[styles.chip, ownerFilter.value === 'NO_OWNER' ? styles.active : '']}
                    onClick={() => (ownerFilter.value = ownerFilter.value === 'NO_OWNER' ? '' : 'NO_OWNER')}
                  >
                    无 Owner <em>({countByGovernance('NO_OWNER')})</em>
                  </button>
                  <button
                    class={[styles.chip, governanceFilter.value === 'HAS_ISSUE' ? styles.active : '']}
                    onClick={() => (governanceFilter.value = governanceFilter.value === 'HAS_ISSUE' ? '' : 'HAS_ISSUE')}
                  >
                    有问题 <em>({countByGovernance('HAS_ISSUE')})</em>
                  </button>
                </div>
              </div>
              <NButton block onClick={resetFilters}>重置筛选</NButton>
            </div>
          </aside>
          <div
            class={styles.sideResizer}
            title='拖拽调整筛选栏宽度'
            onMousedown={handleSidebarMouseDown}
            onDblclick={() => toggleSidebar(220)}
          />

          <section class={styles.assetPanel}>
            <div class={styles.assetTools}>
              <NSelect
                value={datasourceId.value}
                options={datasourceOptions.value}
                loading={datasourceLoading.value}
                clearable
                placeholder='请选择数据源'
                onUpdateValue={(value) => handleDatasourceChange(value as number | null)}
              />
              <NSelect
                value={databaseFilter.value}
                options={databaseOptions.value}
                loading={databaseLoading.value}
                disabled={!datasourceId.value}
                clearable
                placeholder={datasourceId.value ? '请选择数据库' : '请先选择数据源'}
                onUpdateValue={(value) => handleDatabaseChange(value as string | null)}
              />
              <NSelect
                value={ownerFilter.value}
                options={ownerOptions.value}
                onUpdateValue={(value) => (ownerFilter.value = value as string)}
              />
              <NSelect
                value={qualityStatus.value}
                options={qualityOptions}
                onUpdateValue={(value) => setQualityStatus(value as string)}
              />
              <NSelect
                value={sortMode.value}
                options={[
                  { label: '最近更新优先', value: 'UPDATED' },
                  { label: '资产名称', value: 'NAME' },
                  { label: '问题数', value: 'ISSUE' }
                ]}
                onUpdateValue={(value) => (sortMode.value = value as string)}
              />
              <NButton type='primary' disabled={!hasAssetScope.value} onClick={loadAssets}>查询</NButton>
            </div>
            <NSpin show={loading.value}>
              {!datasourceId.value || !databaseFilter.value ? (
                <div class={styles.emptyBlock}>
                  <NEmpty description='请选择数据源和数据库后查询资产。' />
                </div>
              ) : !displayedAssets.value.length && !loading.value ? (
                <div class={styles.emptyBlock}>
                  <NEmpty description='当前条件下暂无资产' />
                </div>
              ) : (
                <div class={styles.assetTable}>
                  <table>
                    <thead>
                      <tr>
                        <th>资产</th>
                        <th>表注释</th>
                        <th>标签</th>
                        <th>Owner</th>
                        <th>质量</th>
                        <th>血缘</th>
                        <th>最近同步任务</th>
                      </tr>
                    </thead>
                    <tbody>
                      {displayedAssets.value.map((asset) => (
                        <>
                          <tr
                            class={[styles.assetRow, expandedAssetId.value === asset.id ? styles.assetRowActive : '']}
                            onClick={() => expandAsset(asset)}
                          >
                            <td>
                              <div class={styles.assetName}>{asset.tableName}</div>
                              <div class={styles.path}>{asset.fullName}</div>
                            </td>
                            <td>
                              <div class={styles.assetComment}>
                                {asset.description || '未填写表注释'}
                              </div>
                            </td>
                            <td>
                              <NSpace size={4}>
                                {(asset.tags || []).map((tag) => (
                                  <NTag size='small'>{tag}</NTag>
                                ))}
                              </NSpace>
                            </td>
                            <td>{asset.owner || '未分配'}</td>
                            <td>
                              <NTag type={statusType(asset.qualityStatus) as any}>
                                {qualityDisplayText(asset.qualityStatus)}
                              </NTag>
                            </td>
                            <td>{asset.lastSyncTask ? '1 条' : '0 条'}</td>
                            <td>{asset.lastSyncTask || '-'}</td>
                          </tr>
                          {expandedAssetId.value === asset.id && (
                            <tr class={styles.assetDetailRow}>
                              <td colspan={7}>{renderAssetDetail(asset)}</td>
                            </tr>
                          )}
                        </>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </NSpin>
          </section>
        </div>

        <NModal
          show={metadataModalVisible.value}
          preset='card'
          title='编辑治理元数据'
          style={{ width: '620px' }}
          onClose={() => (metadataModalVisible.value = false)}
        >
          <NForm labelPlacement='top'>
            <NFormItem label='Owner'>
              <NInput value={metadataForm.owner} onUpdateValue={(value) => (metadataForm.owner = value)} />
            </NFormItem>
            <NFormItem label='标签'>
              <NInput value={metadataForm.tagsText} placeholder='多个标签用英文逗号分隔' onUpdateValue={(value) => (metadataForm.tagsText = value)} />
            </NFormItem>
            <NFormItem label='说明'>
              <NInput type='textarea' value={metadataForm.description} onUpdateValue={(value) => (metadataForm.description = value)} />
            </NFormItem>
          </NForm>
          <NSpace justify='end'>
            <NButton onClick={() => (metadataModalVisible.value = false)}>取消</NButton>
            <NButton type='primary' onClick={saveMetadata}>保存</NButton>
          </NSpace>
        </NModal>

        <NModal
          show={ruleModalVisible.value}
          preset='card'
          title='新建质量规则'
          style={{ width: '980px', maxHeight: '88vh', overflow: 'auto' }}
          onClose={() => (ruleModalVisible.value = false)}
        >
          <div class={styles.ruleModalBody}>
            <section class={styles.ruleSection}>
              <div class={styles.ruleSectionHead}>
                <strong>1. 基础信息</strong>
                <span>规则必须绑定到一个资产，可选字段级或表级检测。</span>
              </div>
              <div class={styles.ruleGrid}>
                <NFormItem label='治理资产'>
                  <NSelect
                    value={ruleAssetId.value}
                    options={assets.value.map((asset) => ({
                      label: asset.fullName,
                      value: asset.id
                    }))}
                    filterable
                    placeholder='请选择要绑定质量规则的数据资产'
                    onUpdateValue={(value) => updateRuleAsset(value as string)}
                  />
                </NFormItem>
                <NFormItem label='规则名称'>
                  <NInput value={ruleForm.name} onUpdateValue={(value) => (ruleForm.name = value)} />
                </NFormItem>
                <NFormItem label='规则类型'>
                  <NSelect value={ruleForm.type} options={ruleTypeOptions} onUpdateValue={(value) => updateRuleType(value as string)} />
                </NFormItem>
                <NFormItem label='规则层级'>
                  <NSelect value={ruleForm.level} options={ruleLevelOptions} onUpdateValue={(value) => (ruleForm.level = value as string)} />
                </NFormItem>
                {ruleForm.level !== 'TABLE' && (
                  <NFormItem label='检测字段'>
                    <NSelect
                      value={ruleForm.fieldName}
                      options={ruleFields.value.map((field) => ({ label: `${field.name} (${field.type})`, value: field.name }))}
                      onUpdateValue={(value) => (ruleForm.fieldName = value as string)}
                    />
                  </NFormItem>
                )}
              </div>
            </section>

            <section class={styles.ruleSection}>
              <div class={styles.ruleSectionHead}>
                <strong>2. 规则条件</strong>
                <span>不同规则类型需要的参数不同，避免保存无效规则。</span>
              </div>
              <div class={styles.ruleGrid}>
                {renderRuleConditionFields()}
              </div>
            </section>

            <section class={styles.ruleSection}>
              <div class={styles.ruleSectionHead}>
                <strong>3. 执行与问题策略</strong>
                <span>决定检测范围、失败阈值，以及异常后如何进入治理闭环。</span>
              </div>
              <div class={styles.ruleGrid}>
                <NFormItem label='检测范围'>
                  <NSelect value={ruleOptions.scopeType} options={scopeOptions} onUpdateValue={(value) => (ruleOptions.scopeType = value as string)} />
                </NFormItem>
                <NFormItem label='范围条件'>
                  <NInput
                    value={ruleOptions.scopeWhere}
                    disabled={ruleOptions.scopeType !== 'WHERE'}
                    placeholder='例如 dt = current_date'
                    onUpdateValue={(value) => (ruleOptions.scopeWhere = value)}
                  />
                </NFormItem>
                <NFormItem label='失败阈值'>
                  <NSelect value={ruleForm.failureThreshold} options={failureThresholdOptions} onUpdateValue={(value) => (ruleForm.failureThreshold = value as string)} />
                </NFormItem>
                <NFormItem label='严重程度'>
                  <NSelect value={ruleForm.severity} options={severityOptions} onUpdateValue={(value) => (ruleForm.severity = value as string)} />
                </NFormItem>
                <NFormItem label='检测频率'>
                  <NSelect value={ruleForm.frequency} options={frequencyOptions} onUpdateValue={(value) => (ruleForm.frequency = value as string)} />
                </NFormItem>
              </div>
              <div class={styles.checkList}>
                <NCheckbox checked={ruleOptions.createIssue} onUpdateChecked={(value) => (ruleOptions.createIssue = value)}>
                  检测失败时自动生成治理问题
                </NCheckbox>
                <NCheckbox checked={ruleOptions.escalateIssue} onUpdateChecked={(value) => (ruleOptions.escalateIssue = value)}>
                  连续 3 次失败后升级严重程度
                </NCheckbox>
                <NCheckbox checked={ruleOptions.autoCloseIssue} onUpdateChecked={(value) => (ruleOptions.autoCloseIssue = value)}>
                  检测通过后自动关闭历史问题
                </NCheckbox>
              </div>
            </section>

            <section class={styles.rulePreview}>
              <div class={styles.ruleSectionHead}>
                <strong>规则预览与试运行</strong>
                <span>保存后会绑定到当前资产，质量检测失败时自动生成治理问题。</span>
              </div>
              <div class={styles.validationGrid}>
                {validationItems.value.map((item) => (
                  <div class={styles.validationItem}>
                    <span class={[styles.dot, item.ok ? '' : styles.error]} />
                    <span>{item.label}</span>
                  </div>
                ))}
              </div>
              <NTabs value={ruleSqlMode.value} onUpdateValue={(value) => (ruleSqlMode.value = value as any)}>
                <NTabPane name='summary' tab='规则预览'>
                  <pre class={styles.previewCode}>{rulePreview.value}</pre>
                  {trialResult.value && (
                    <div class={styles.testResult}>
                      <div class={styles.resultTile}><span>扫描行数</span><strong>按 SQL 返回</strong></div>
                      <div class={styles.resultTile}><span>异常行数</span><strong>{trialResult.value.abnormalCount}</strong></div>
                      <div class={styles.resultTile}><span>异常率</span><strong>{(trialResult.value.abnormalRate * 100).toFixed(3)}%</strong></div>
                      <div class={styles.resultTile}><span>预计结果</span><strong>{trialResult.value.passed ? '通过' : '失败'}</strong></div>
                    </div>
                  )}
                </NTabPane>
                <NTabPane name='sql' tab='检测 SQL'>
                  <div class={styles.sqlPane}>
                    <div class={styles.sqlToolbar}>
                      <div>
                        <NTag type={ruleForm.manualSql ? 'warning' : 'success'}>{ruleForm.manualSql ? '手工编辑' : '系统生成'}</NTag>
                        <NText class={styles.sqlHint} depth={3}>
                          {ruleForm.manualSql ? '当前 SQL 已由用户调整，修改规则条件不会自动覆盖；点击“重新生成 SQL”可恢复系统 SQL。' : '规则条件变化时自动刷新 SQL；手工编辑后不会被覆盖。'}
                        </NText>
                      </div>
                      <NButton size='small' onClick={regenerateRuleSql}>重新生成 SQL</NButton>
                    </div>
                    <NInput
                      class={styles.sqlEditor}
                      type='textarea'
                      value={ruleForm.sql}
                      onUpdateValue={(value) => {
                        ruleForm.sql = value
                        ruleForm.manualSql = true
                      }}
                    />
                  </div>
                </NTabPane>
              </NTabs>
            </section>
          </div>
          <NSpace justify='end' class={styles.modalFoot}>
            <NButton onClick={runTrial}>试运行</NButton>
            <NButton onClick={() => saveRuleWithEnabled(false)}>保存草稿</NButton>
            <NButton type='primary' onClick={() => saveRuleWithEnabled(true)}>保存并启用</NButton>
          </NSpace>
        </NModal>
      </div>
    )
  }
})
