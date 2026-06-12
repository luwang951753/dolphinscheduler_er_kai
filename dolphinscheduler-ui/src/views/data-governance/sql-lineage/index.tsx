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

import { computed, defineComponent, ref } from 'vue'
import { NButton, NEmpty, NRadioButton, NRadioGroup, NSpace, NSpin, NTag } from 'naive-ui'
import MonacoEditor from '@/components/monaco-editor'
import { parseSqlLineage } from '@/service/modules/data-governance'
import type {
  ISqlLineage,
  ISqlLineageEdge,
  ISqlLineageTable
} from '@/service/modules/data-governance/types'
import styles from './index.module.scss'

type LineageMode = 'TABLE' | 'FIELD'

const DEFAULT_SQL = `INSERT INTO money_laundering.money_laundering_feature (
  card_id,
  suspect_id,
  total_amount
)
SELECT
  bc.card_id,
  s.suspect_id,
  SUM(tx.amount) AS total_amount
FROM money_laundering.bank_card bc
JOIN money_laundering.suspect s
  ON bc.owner_id = s.suspect_id
JOIN money_laundering.transaction_detail tx
  ON bc.card_id = tx.card_id
GROUP BY bc.card_id, s.suspect_id;`

const NODE_WIDTH = 260
const NODE_BASE_HEIGHT = 74
const COLUMN_HEIGHT = 30
const SOURCE_X = 40
const TARGET_X = 560

export default defineComponent({
  name: 'data-governance-sql-lineage',
  setup() {
    const sql = ref(DEFAULT_SQL)
    const loading = ref(false)
    const mode = ref<LineageMode>('FIELD')
    const lineage = ref<ISqlLineage | null>(null)
    const activeKey = ref('')

    const sourceTables = computed(() => {
      if (!lineage.value) return []
      const targetIds = new Set(lineage.value.edges.map((edge) => edge.targetTable))
      return lineage.value.tables.filter((table) => !targetIds.has(table.id))
    })

    const targetTables = computed(() => {
      if (!lineage.value) return []
      const targetIds = new Set(lineage.value.edges.map((edge) => edge.targetTable))
      return lineage.value.tables.filter((table) => targetIds.has(table.id))
    })

    const nodePosition = computed(() => {
      const positions: Record<string, { x: number; y: number; height: number }> = {}
      let sourceY = 34
      sourceTables.value.forEach((table) => {
        const height = nodeHeight(table)
        positions[table.id] = { x: SOURCE_X, y: sourceY, height }
        sourceY += height + 28
      })
      let targetY = Math.max(34, Math.floor(sourceY / 2) - 90)
      targetTables.value.forEach((table) => {
        const height = nodeHeight(table)
        positions[table.id] = { x: TARGET_X, y: targetY, height }
        targetY += height + 28
      })
      return positions
    })

    const visibleEdges = computed(() => {
      const edges = lineage.value?.edges || []
      return edges.filter((edge) => edge.lineageType === mode.value)
    })

    const parse = async () => {
      if (loading.value) {
        window.$message.info('SQL 正在解析，请稍候')
        return
      }
      if (!sql.value.trim()) {
        window.$message.warning('请输入 SQL 后再解析')
        return
      }
      loading.value = true
      activeKey.value = ''
      try {
        lineage.value = await parseSqlLineage({ sql: sql.value })
        window.$message.success('SQL 血缘解析完成')
      } catch (error) {
        try {
          lineage.value = parseSqlLineageLocally(sql.value)
          window.$message.warning('后端解析接口不可用，已使用前端本地 MVP 解析结果')
        } catch (fallbackError) {
          window.$message.error('SQL 血缘解析失败，请确认后端服务已启动或稍后重试')
        }
      } finally {
        loading.value = false
      }
    }

    const copySql = async () => {
      await navigator.clipboard?.writeText(sql.value)
      window.$message.success('SQL 已复制')
    }

    const resetSql = () => {
      sql.value = DEFAULT_SQL
      activeKey.value = ''
    }

    const setActiveColumn = (tableId: string, columnName: string) => {
      activeKey.value = `${tableId}.${columnName}`
    }

    const isColumnActive = (tableId: string, columnName: string) => {
      if (!activeKey.value) return false
      return activeKey.value === `${tableId}.${columnName}`
    }

    const isEdgeActive = (edge: ISqlLineageEdge) => {
      if (!activeKey.value) return false
      return [
        `${edge.sourceTable}.${edge.sourceColumn || '*'}`,
        `${edge.targetTable}.${edge.targetColumn || '*'}`
      ].includes(activeKey.value)
    }

    const renderEdge = (edge: ISqlLineageEdge, index: number) => {
      const source = nodePosition.value[edge.sourceTable]
      const target = nodePosition.value[edge.targetTable]
      if (!source || !target) return null
      const sourceColumnIndex = columnIndex(edge.sourceTable, edge.sourceColumn)
      const targetColumnIndex = columnIndex(edge.targetTable, edge.targetColumn)
      const sourceY = mode.value === 'FIELD'
        ? source.y + NODE_BASE_HEIGHT + Math.max(sourceColumnIndex, 0) * COLUMN_HEIGHT + 15
        : source.y + source.height / 2
      const targetY = mode.value === 'FIELD'
        ? target.y + NODE_BASE_HEIGHT + Math.max(targetColumnIndex, 0) * COLUMN_HEIGHT + 15
        : target.y + target.height / 2
      const x1 = source.x + NODE_WIDTH
      const x2 = target.x
      const c1 = x1 + 90
      const c2 = x2 - 90
      return (
        <path
          key={`${edge.sourceTable}-${edge.sourceColumn}-${edge.targetTable}-${edge.targetColumn}-${index}`}
          class={`${styles.edge} ${isEdgeActive(edge) ? styles.edgeActive : ''}`}
          d={`M ${x1} ${sourceY} C ${c1} ${sourceY}, ${c2} ${targetY}, ${x2} ${targetY}`}
        />
      )
    }

    const columnIndex = (tableId: string, columnName?: string) => {
      const table = lineage.value?.tables.find((item) => item.id === tableId)
      if (!table || !columnName) return 0
      return Math.max(0, table.columns.findIndex((column) => column.name === columnName))
    }

    const renderNode = (table: ISqlLineageTable) => {
      const position = nodePosition.value[table.id]
      if (!position) return null
      const isTarget = targetTables.value.some((item) => item.id === table.id)
      return (
        <div
          class={`${styles.node} ${isTarget ? styles.nodeTarget : ''}`}
          style={{
            left: `${position.x}px`,
            top: `${position.y}px`
          }}
        >
          <div class={styles.nodeHead}>
            <div class={styles.nodeName}>{table.name}</div>
            <div class={styles.nodeSchema}>{table.schema || table.id}</div>
          </div>
          <div class={styles.columnList}>
            {(table.columns || []).map((column) => (
              <div
                class={`${styles.column} ${isColumnActive(table.id, column.name) ? styles.columnActive : ''}`}
                onClick={() => setActiveColumn(table.id, column.name)}
              >
                <span>{column.name}</span>
                <span class={styles.columnType}>{column.type || 'UNKNOWN'}</span>
              </div>
            ))}
          </div>
        </div>
      )
    }

    return {
      sql,
      loading,
      mode,
      lineage,
      sourceTables,
      targetTables,
      visibleEdges,
      parse,
      copySql,
      resetSql,
      renderEdge,
      renderNode
    }
  },
  render() {
    return (
      <div class={styles.page}>
        <div class={styles.shell}>
          <div class={styles.hero}>
            <div>
              <h2 class={styles.title}>在线 SQL 数据血缘解析</h2>
              <div class={styles.subtitle}>
                输入 SQL 后解析表级和字段级血缘，第一版重点支持 INSERT INTO ... SELECT ... FROM/JOIN。
              </div>
            </div>
            <NSpace>
              <NTag type='info'>Monaco Editor</NTag>
              <NTag type='success'>JSqlParser MVP</NTag>
            </NSpace>
          </div>
          <div class={styles.workspace}>
            <section class={styles.panel}>
              <div class={styles.panelHead}>
                <span class={styles.panelTitle}>SQL 编辑器</span>
                <NSpace class={styles.panelActions} size={8}>
                  <NButton size='small' onClick={this.copySql}>复制</NButton>
                  <NButton size='small' onClick={this.resetSql}>示例 SQL</NButton>
                  <NButton size='small' type='primary' loading={this.loading} onClick={this.parse}>解析</NButton>
                </NSpace>
              </div>
              <div class={styles.editorBox}>
                <MonacoEditor
                  value={this.sql}
                  height='100%'
                  options={{
                    language: 'sql',
                    minimap: { enabled: false },
                    fontSize: 13,
                    lineNumbers: 'on',
                    wordWrap: 'on',
                    scrollBeyondLastLine: false
                  }}
                  onUpdateValue={(value: string) => (this.sql = value)}
                />
              </div>
            </section>
            <section class={styles.panel}>
              <div class={styles.panelHead}>
                <span class={styles.panelTitle}>血缘图</span>
                <NRadioGroup value={this.mode} onUpdateValue={(value: LineageMode) => (this.mode = value)}>
                  <NRadioButton value='TABLE'>表级</NRadioButton>
                  <NRadioButton value='FIELD'>字段级</NRadioButton>
                </NRadioGroup>
              </div>
              <NSpin show={this.loading}>
                <div class={styles.graphBox}>
                  {!this.lineage ? (
                    <div class={styles.empty}>
                      <NEmpty description='点击“解析”后展示 SQL 血缘图' />
                    </div>
                  ) : (
                    <div class={styles.graphInner}>
                      <svg class={styles.svgLayer}>
                        {this.visibleEdges.map((edge: ISqlLineageEdge, index: number) => this.renderEdge(edge, index))}
                      </svg>
                      {this.sourceTables.map((table: ISqlLineageTable) => this.renderNode(table))}
                      {this.targetTables.map((table: ISqlLineageTable) => this.renderNode(table))}
                    </div>
                  )}
                </div>
                {this.lineage?.warnings?.length ? (
                  <div class={styles.warnings}>
                    {this.lineage.warnings.map((warning: string) => <div>{warning}</div>)}
                  </div>
                ) : null}
              </NSpin>
            </section>
          </div>
        </div>
      </div>
    )
  }
})

function nodeHeight(table: ISqlLineageTable) {
  return NODE_BASE_HEIGHT + Math.max(1, table.columns?.length || 1) * COLUMN_HEIGHT + 12
}

function parseSqlLineageLocally(sql: string): ISqlLineage {
  const normalizedSql = sql.replace(/--.*$/gm, ' ').replace(/\s+/g, ' ').trim()
  const target = parseInsertTarget(normalizedSql)
  const selectItems = splitSelectItems(normalizedSql)
  const sourceTables = parseSourceTables(normalizedSql)

  if (!sourceTables.length) {
    throw new Error('No source table found')
  }

  const targetTable = target?.table || {
    id: 'query_result',
    name: 'query_result',
    schema: '',
    columns: []
  }
  const targetColumns = target?.columns?.length
    ? target.columns
    : selectItems.map((item, index) => resolveOutputColumn(item, index))

  const tables: ISqlLineageTable[] = [...sourceTables.map((item) => item.table), targetTable]
  const aliasToTableId = new Map<string, string>()
  sourceTables.forEach((source) => {
    aliasToTableId.set(source.table.name.toLowerCase(), source.table.id)
    aliasToTableId.set(source.table.id.toLowerCase(), source.table.id)
    if (source.alias) {
      aliasToTableId.set(source.alias.toLowerCase(), source.table.id)
    }
  })

  const edges: ISqlLineageEdge[] = []
  sourceTables.forEach((source) => {
    addEdge(edges, {
      sourceTable: source.table.id,
      targetTable: targetTable.id,
      lineageType: 'TABLE'
    })
  })

  selectItems.forEach((item, index) => {
    const targetColumn = targetColumns[index] || resolveOutputColumn(item, index)
    addColumn(targetTable, targetColumn)
    const sourceColumns = extractSourceColumns(item, aliasToTableId, sourceTables[0].table.id)
    sourceColumns.forEach((column) => {
      const sourceTable = tables.find((table) => table.id === column.tableId)
      addColumn(sourceTable, column.columnName)
      addEdge(edges, {
        sourceTable: column.tableId,
        sourceColumn: column.columnName,
        targetTable: targetTable.id,
        targetColumn,
        lineageType: 'FIELD'
      })
    })
  })

  return {
    tables,
    edges,
    warnings: ['当前结果由前端本地 MVP 解析生成；后端服务可用后会自动使用 JSqlParser 接口。']
  }
}

function parseInsertTarget(sql: string) {
  const match = sql.match(/insert\s+into\s+([`"\w.]+)\s*\(([\s\S]*?)\)\s*select/i)
  if (!match) return null
  return {
    table: createTable(match[1]),
    columns: match[2]
      .split(',')
      .map((column) => stripQuote(column))
      .filter(Boolean)
  }
}

function parseSourceTables(sql: string) {
  const sources: Array<{ table: ISqlLineageTable; alias?: string }> = []
  const tablePattern = /\b(?:from|join)\s+([`"\w.]+)(?:\s+([a-zA-Z_]\w*))?/gi
  let match = tablePattern.exec(sql)
  while (match) {
    const table = createTable(match[1])
    const alias = match[2] && !isSqlKeyword(match[2]) ? match[2] : undefined
    if (!sources.some((source) => source.table.id === table.id)) {
      sources.push({ table, alias })
    }
    match = tablePattern.exec(sql)
  }
  return sources
}

function splitSelectItems(sql: string) {
  const match = sql.match(/\bselect\b([\s\S]*?)\bfrom\b/i)
  if (!match) return []
  const items: string[] = []
  let current = ''
  let depth = 0
  for (const char of match[1]) {
    if (char === '(') depth += 1
    if (char === ')') depth = Math.max(0, depth - 1)
    if (char === ',' && depth === 0) {
      if (current.trim()) items.push(current.trim())
      current = ''
    } else {
      current += char
    }
  }
  if (current.trim()) items.push(current.trim())
  return items
}

function extractSourceColumns(
  expression: string,
  aliasToTableId: Map<string, string>,
  defaultTableId: string
) {
  const columns: Array<{ tableId: string; columnName: string }> = []
  const qualifiedPattern = /\b([a-zA-Z_]\w*)\.([a-zA-Z_]\w*)\b/g
  let match = qualifiedPattern.exec(expression)
  while (match) {
    const tableId = aliasToTableId.get(match[1].toLowerCase()) || defaultTableId
    const columnName = stripQuote(match[2])
    if (!columns.some((column) => column.tableId === tableId && column.columnName === columnName)) {
      columns.push({ tableId, columnName })
    }
    match = qualifiedPattern.exec(expression)
  }
  if (columns.length) return columns

  const plainColumn = expression
    .replace(/\bas\s+[`"\w]+\b/i, '')
    .replace(/\b(sum|avg|max|min|count|distinct)\s*\(/gi, '')
    .replace(/[()]/g, '')
    .trim()
  if (/^[`"]?[a-zA-Z_]\w*[`"]?$/.test(plainColumn) && !isSqlKeyword(plainColumn)) {
    columns.push({ tableId: defaultTableId, columnName: stripQuote(plainColumn) })
  }
  return columns
}

function resolveOutputColumn(selectItem: string, index: number) {
  const aliasMatch = selectItem.match(/\bas\s+([`"\w]+)$/i)
  if (aliasMatch) return stripQuote(aliasMatch[1])
  const qualifiedMatch = selectItem.match(/\.([`"\w]+)$/)
  if (qualifiedMatch) return stripQuote(qualifiedMatch[1])
  const plainMatch = selectItem.match(/^([`"\w]+)$/)
  if (plainMatch) return stripQuote(plainMatch[1])
  return `expr_${index + 1}`
}

function createTable(fullName: string): ISqlLineageTable {
  const cleanName = stripQuote(fullName)
  const parts = cleanName.split('.')
  const name = parts[parts.length - 1]
  const schema = parts.length > 1 ? parts.slice(0, -1).join('.') : ''
  return {
    id: schema ? `${schema}.${name}` : name,
    name,
    schema,
    columns: []
  }
}

function addColumn(table: ISqlLineageTable | undefined, columnName: string) {
  if (!table || !columnName) return
  if (table.columns.some((column) => column.name === columnName)) return
  table.columns.push({ name: columnName, type: 'UNKNOWN' })
}

function addEdge(edges: ISqlLineageEdge[], edge: ISqlLineageEdge) {
  const exists = edges.some((item) =>
    item.sourceTable === edge.sourceTable
      && item.sourceColumn === edge.sourceColumn
      && item.targetTable === edge.targetTable
      && item.targetColumn === edge.targetColumn
      && item.lineageType === edge.lineageType
  )
  if (!exists) edges.push(edge)
}

function stripQuote(value: string) {
  return value.replace(/[`"]/g, '').trim()
}

function isSqlKeyword(value: string) {
  return ['on', 'where', 'group', 'order', 'left', 'right', 'inner', 'outer', 'join'].includes(
    stripQuote(value).toLowerCase()
  )
}
