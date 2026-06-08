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

interface IGovernanceAsset {
  id: string
  datasourceId: number
  datasourceName: string
  datasourceType: string
  database: string
  schema?: string
  tableName: string
  tableType?: string
  fullName: string
  owner?: string
  description?: string
  tags?: string[]
  qualityStatus: string
  fieldCount?: number
  ruleCount?: number
  issueCount?: number
  lastCheckTime?: string
  lastSyncTask?: string
  updateTime?: string
}

interface IGovernanceField {
  name: string
  type: string
  nullable: boolean
  primaryKey: boolean
  comment?: string
  sensitiveTag?: string
}

interface IGovernanceMetadataRequest {
  owner?: string
  description?: string
  tags?: string[]
}

interface IGovernanceQualityRule {
  id?: string
  assetId?: string
  name: string
  type: string
  level?: string
  fieldName?: string
  conditions?: Record<string, any>
  rangeCondition?: string
  samplePolicy?: string
  failureThreshold?: string
  severity?: string
  frequency?: string
  enabled?: boolean
  createIssue?: boolean
  escalateIssue?: boolean
  autoCloseIssue?: boolean
  manualSql?: boolean
  sql?: string
  status?: string
  lastRunAt?: string
  abnormalCount?: number
  abnormalRate?: number
}

interface IGovernanceTrialRunRequest {
  rule?: IGovernanceQualityRule
  sql?: string
}

interface IGovernanceTrialRunResult {
  passed: boolean
  abnormalCount: number
  abnormalRate: number
  executedAt: string
  message: string
  samples?: Array<Record<string, any>>
}

interface IGovernanceLineageNode {
  assetId: string
  assetName: string
  relationType?: string
  syncTaskName?: string
  lastRunStatus?: string
  lastRunTime?: string
  fieldMappings?: Array<{
    sourceField: string
    targetField: string
  }>
}

interface IGovernanceLineage {
  upstream: IGovernanceLineageNode[]
  downstream: IGovernanceLineageNode[]
}

interface IGovernanceSyncTaskLineageRequest {
  sourceDatasourceId: number
  sourceDatasourceName?: string
  sourceDatabase: string
  sourceSchema?: string
  sourceTable: string
  targetDatasourceId: number
  targetDatasourceName?: string
  targetDatabase: string
  targetSchema?: string
  targetTable: string
  syncTaskName: string
  lastRunStatus?: string
  lastRunTime?: string
  fieldMappings?: Array<{
    sourceField: string
    targetField: string
  }>
}

interface IGovernanceIssue {
  id: string
  assetId: string
  ruleId: string
  title: string
  severity: string
  status: string
  abnormalCount?: number
  discoveredAt?: string
  updatedAt?: string
}

export type {
  IGovernanceAsset,
  IGovernanceField,
  IGovernanceMetadataRequest,
  IGovernanceQualityRule,
  IGovernanceTrialRunRequest,
  IGovernanceTrialRunResult,
  IGovernanceLineage,
  IGovernanceLineageNode,
  IGovernanceSyncTaskLineageRequest,
  IGovernanceIssue
}
