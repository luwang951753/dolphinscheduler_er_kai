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

import { defineComponent, onMounted, reactive } from 'vue'
import {
  NButton,
  NDataTable,
  NEmpty,
  NIcon,
  NInput,
  NModal,
  NTag,
  NTabs,
  NTabPane,
  type DataTableColumns
} from 'naive-ui'
import { SearchOutlined, SettingOutlined } from '@vicons/antd'
import {
  queryDataIssue,
  queryDataIssueParamMapping
} from '@/service/modules/data-issue'
import type {
  DataIssueColumn,
  DataIssueQueryParams,
  DataIssueQueryResult,
  DataIssueTarget
} from '@/service/modules/data-issue/types'
import styles from './index.module.scss'

type QueryState = {
  suspectNo: string
  suspectName: string
  suspectIdCard: string
  caseNo: string
  caseName: string
}

type QueryField = keyof QueryState
type ParamMappingState = Record<QueryField, string>

type TargetState = {
  title: string
  rows: Array<Record<string, any>>
  columns: DataIssueColumn[]
  total: number
  remark: string
  loading: boolean
  queried: boolean
}

const targetTabs: Array<{ key: DataIssueTarget; label: string; hint: string }> = [
  { key: 'DATA_CENTER', label: '数据中台', hint: '数据中台查询结果' },
  { key: 'PHASE3_PLATFORM', label: '三期平台', hint: '三期平台查询结果' }
]

const columnTitleMap: Record<string, string> = {
  suspectNo: '嫌疑人编号',
  suspect_no: '嫌疑人编号',
  xyrbh: '嫌疑人编号',
  suspectName: '嫌疑人姓名',
  suspect_name: '嫌疑人姓名',
  xyrxm: '嫌疑人姓名',
  suspectIdCard: '嫌疑人身份证号',
  suspect_id_card: '嫌疑人身份证号',
  idCard: '身份证号',
  sfzh: '身份证号',
  caseNo: '案件编号',
  case_no: '案件编号',
  ajbh: '案件编号',
  caseName: '案件名称',
  case_name: '案件名称',
  ajmc: '案件名称',
  issueStatus: '下发状态',
  issue_status: '下发状态',
  status: '状态',
  targetPlatform: '目标平台',
  target_platform: '目标平台',
  targetRecordNo: '目标平台记录号',
  target_record_no: '目标平台记录号',
  updateTime: '更新时间',
  update_time: '更新时间'
}

const defaultColumnOrder = [
  'suspectNo',
  'suspectName',
  'suspectIdCard',
  'caseNo',
  'caseName',
  'issueStatus',
  'targetRecordNo',
  'updateTime'
]

const queryFieldMeta: Array<{
  key: QueryField
  label: string
  placeholder: string
}> = [
  { key: 'suspectNo', label: '嫌疑人编号', placeholder: '输入嫌疑人编号' },
  { key: 'suspectName', label: '嫌疑人姓名', placeholder: '输入嫌疑人姓名' },
  { key: 'suspectIdCard', label: '嫌疑人身份证号', placeholder: '输入身份证号' },
  { key: 'caseNo', label: '案件编号', placeholder: '输入案件编号' },
  { key: 'caseName', label: '案件名称', placeholder: '输入案件名称' }
]

const createDefaultParamMapping = (): ParamMappingState => ({
  suspectNo: 'xyrbh',
  suspectName: 'xyrmc',
  suspectIdCard: 'xyrSfzh',
  caseNo: 'ajbh',
  caseName: 'ajmc'
})

const normalizeParamMapping = (
  value: Partial<Record<QueryField, string>> | null | undefined
): ParamMappingState => {
  const defaults = createDefaultParamMapping()
  queryFieldMeta.forEach(({ key }) => {
    const mapped = value?.[key]?.trim()
    if (mapped) defaults[key] = mapped
  })
  return defaults
}

const createTargetState = (): TargetState => ({
  title: '',
  rows: [],
  columns: [],
  total: 0,
  remark: '',
  loading: false,
  queried: false
})

const buildTargetTabLabel = (
  tab: { label: string },
  targetState: TargetState
) => {
  const label = targetState.title || tab.label
  return `${label}${targetState.queried ? ` (${targetState.total})` : ''}`
}

const normalizeResult = (
  result: DataIssueQueryResult | Array<Record<string, any>> | null | undefined
): DataIssueQueryResult => {
  if (Array.isArray(result)) {
    return { rows: result, total: result.length }
  }
  return result || { rows: [], total: 0 }
}

const formatValue = (value: any) => {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

export default defineComponent({
  name: 'data-issue',
  setup() {
    const query = reactive<QueryState>({
      suspectNo: '',
      suspectName: '',
      suspectIdCard: '',
      caseNo: '',
      caseName: ''
    })
    const paramMapping = reactive<ParamMappingState>(createDefaultParamMapping())
    const editingParamMapping = reactive<ParamMappingState>(createDefaultParamMapping())
    const state = reactive({
      activeTarget: 'DATA_CENTER' as DataIssueTarget,
      lastQueried: false,
      mappingModalVisible: false,
      targetData: {
        DATA_CENTER: createTargetState(),
        PHASE3_PLATFORM: createTargetState()
      } as Record<DataIssueTarget, TargetState>
    })

    const getQueryParams = (target: DataIssueTarget): DataIssueQueryParams => {
      const params: DataIssueQueryParams = { target }
      queryFieldMeta.forEach(({ key }) => {
        const value = query[key].trim()
        if (!value) return
        params[paramMapping[key].trim() || key] = value
      })
      return params
    }

    const inferColumns = (rows: Array<Record<string, any>>) => {
      const keys = new Set<string>()
      defaultColumnOrder.forEach((key) => {
        if (rows.some((row) => Object.prototype.hasOwnProperty.call(row, key))) {
          keys.add(key)
        }
      })
      rows.slice(0, 20).forEach((row) => {
        Object.keys(row || {}).forEach((key) => keys.add(key))
      })
      return Array.from(keys).map((key) => ({
        key,
        title: columnTitleMap[key] || key,
        width: key.toLowerCase().includes('name') || key.includes('名称') ? 180 : 150
      }))
    }

    const buildTableColumns = (
      columns: DataIssueColumn[]
    ): DataTableColumns<Record<string, any>> =>
      columns.map((column) => ({
        title: column.title || column.key,
        key: column.key,
        width: column.width || 150,
        ellipsis: { tooltip: true },
        render: (row) => {
          const value = formatValue(row[column.key])
          if (
            column.key.toLowerCase().includes('status') ||
            column.title.includes('状态')
          ) {
            const success = /成功|正常|已下发|SUCCESS/i.test(value)
            const warning = /待|处理中|RUNNING/i.test(value)
            return (
              <NTag
                size='small'
                bordered={false}
                type={success ? 'success' : warning ? 'warning' : 'default'}
              >
                {value}
              </NTag>
            )
          }
          return <span class={styles.cellText} title={value}>{value}</span>
        }
      }))

    const loadTarget = async (target: DataIssueTarget) => {
      const targetState = state.targetData[target]
      targetState.loading = true
      targetState.queried = true
      try {
        const result = normalizeResult(await queryDataIssue(getQueryParams(target)))
        const rows = result.rows || []
        targetState.title = result.title || ''
        targetState.rows = rows
        targetState.columns = result.columns?.length ? result.columns : inferColumns(rows)
        targetState.total = result.total ?? rows.length
        targetState.remark = result.remark || ''
      } catch (err) {
        targetState.rows = []
        targetState.title = ''
        targetState.columns = []
        targetState.total = 0
        targetState.remark = 'Magic API 接口暂未返回数据，请确认 /magic-api/data-issue/query 已配置。'
      } finally {
        targetState.loading = false
      }
    }

    const handleSearch = async () => {
      state.lastQueried = true
      await Promise.all(targetTabs.map((tab) => loadTarget(tab.key)))
    }

    const openMappingModal = () => {
      queryFieldMeta.forEach(({ key }) => {
        editingParamMapping[key] = paramMapping[key]
      })
      state.mappingModalVisible = true
    }

    const closeMappingModal = () => {
      state.mappingModalVisible = false
    }

    const saveMappingModal = () => {
      queryFieldMeta.forEach(({ key }) => {
        paramMapping[key] = editingParamMapping[key].trim() || key
      })
      closeMappingModal()
    }

    const loadParamMapping = async () => {
      try {
        const result = await queryDataIssueParamMapping()
        const remoteMapping = Object.fromEntries(
          (result.fields || []).map((field) => [field.key, field.paramName])
        ) as Partial<Record<QueryField, string>>
        const normalized = normalizeParamMapping(remoteMapping)
        queryFieldMeta.forEach(({ key }) => {
          paramMapping[key] = normalized[key]
          editingParamMapping[key] = normalized[key]
        })
      } catch {
        const defaults = createDefaultParamMapping()
        queryFieldMeta.forEach(({ key }) => {
          paramMapping[key] = defaults[key]
          editingParamMapping[key] = defaults[key]
        })
      }
    }

    const handleTabChange = async (value: string | number) => {
      state.activeTarget = value as DataIssueTarget
      const targetState = state.targetData[state.activeTarget]
      if (!targetState.queried) {
        await loadTarget(state.activeTarget)
      }
    }

    onMounted(() => {
      void loadParamMapping().finally(() => {
        void Promise.all(targetTabs.map((tab) => loadTarget(tab.key)))
      })
    })

    const renderSearchField = (
      key: QueryField,
      label: string,
      placeholder: string
    ) => (
      <label class={styles.field}>
        <span class={styles.fieldLabel}>{label}</span>
        <NInput
          v-model:value={query[key]}
          clearable
          placeholder={placeholder}
          onKeyup={(event: KeyboardEvent) => {
            if (event.key === 'Enter') void handleSearch()
          }}
        />
      </label>
    )

    const renderMappingModal = () => (
      <NModal
        show={state.mappingModalVisible}
        preset='card'
        title='参数映射'
        class={styles.mappingModal}
        maskClosable
        onClose={closeMappingModal}
        onMaskClick={closeMappingModal}
      >
        <p class={styles.mappingTip}>
          配置来自 Magic API 参数映射脚本。这里修改后会应用到当前页面，永久调整请修改对应 Magic API 脚本。
        </p>
        <div class={styles.mappingList}>
          {queryFieldMeta.map((field) => (
            <label class={styles.mappingRow} key={field.key}>
              <span>{field.label}</span>
              <NInput
                v-model:value={editingParamMapping[field.key]}
                placeholder='请输入请求参数名'
                onKeyup={(event: KeyboardEvent) => {
                  if (event.key === 'Enter') saveMappingModal()
                }}
              />
            </label>
          ))}
        </div>
        <div class={styles.modalActions}>
          <NButton onClick={closeMappingModal}>取消</NButton>
          <NButton type='primary' onClick={saveMappingModal}>应用</NButton>
        </div>
      </NModal>
    )

    return () => {
      const current = state.targetData[state.activeTarget]
      return (
        <main class={styles.page}>
          <div class={styles.shell}>
            <section class={styles.hero}>
              <div class={styles.titleBlock}>
                <h2 class={styles.title}>数据下发</h2>
              </div>
            </section>

            <section class={styles.queryPanel}>
              <div class={styles.queryGrid}>
                {renderSearchField('suspectNo', '嫌疑人编号', '输入嫌疑人编号')}
                {renderSearchField('suspectName', '嫌疑人姓名', '输入嫌疑人姓名')}
                {renderSearchField('suspectIdCard', '嫌疑人身份证号', '输入身份证号')}
                {renderSearchField('caseNo', '案件编号', '输入案件编号')}
                {renderSearchField('caseName', '案件名称', '输入案件名称')}
                <div class={styles.actions}>
                  <NButton onClick={openMappingModal}>
                    {{
                      icon: () => <NIcon><SettingOutlined /></NIcon>,
                      default: () => '参数映射'
                    }}
                  </NButton>
                  <NButton type='primary' onClick={handleSearch} loading={current.loading}>
                    {{
                      icon: () => <NIcon><SearchOutlined /></NIcon>,
                      default: () => '查询'
                    }}
                  </NButton>
                </div>
              </div>
            </section>

            <section class={styles.resultPanel}>
              <div class={styles.resultHead}>
                <div class={styles.tabTitle}>
                  <strong>{current.title || '下发查询结果'}</strong>
                </div>
              </div>

              <NTabs value={state.activeTarget} type='line' animated onUpdateValue={handleTabChange}>
                {targetTabs.map((tab) => {
                  const targetState = state.targetData[tab.key]
                  return (
                    <NTabPane
                      key={tab.key}
                      name={tab.key}
                      tab={buildTargetTabLabel(tab, targetState)}
                    >
                      <div class={styles.tableWrap}>
                        <NDataTable
                          size='small'
                          bordered
                          loading={targetState.loading}
                          columns={buildTableColumns(targetState.columns)}
                          data={targetState.rows}
                          rowKey={(row) => row.id || JSON.stringify(row)}
                          maxHeight={520}
                          pagination={{
                            pageSize: 20,
                            showSizePicker: true,
                            pageSizes: [20, 50, 100]
                          }}
                        />
                        {!targetState.loading && !targetState.rows.length ? (
                          <div class={styles.emptyHint}>
                            <NEmpty
                              description={
                                targetState.queried
                                  ? targetState.remark || '当前查询条件下暂无下发数据。'
                                  : `${tab.hint}将在查询后展示。`
                              }
                            />
                          </div>
                        ) : null}
                      </div>
                    </NTabPane>
                  )
                })}
              </NTabs>
            </section>
            {renderMappingModal()}
          </div>
        </main>
      )
    }
  }
})
