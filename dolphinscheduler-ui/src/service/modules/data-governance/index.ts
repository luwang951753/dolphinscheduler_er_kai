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

import { axios } from '@/service/service'
import type {
  IGovernanceMetadataRequest,
  IGovernanceQualityRule,
  IGovernanceSyncTaskLineageRequest,
  IGovernanceTrialRunRequest
} from './types'

export function queryGovernanceAssets(params: {
  keyword?: string
  datasourceId?: number | null
  database?: string | null
  qualityStatus?: string | null
  limit?: number
}): any {
  return axios({
    url: '/data-governance/assets',
    method: 'get',
    params
  })
}

export function queryGovernanceFields(assetId: string): any {
  return axios({
    url: `/data-governance/assets/${encodeURIComponent(assetId)}/fields`,
    method: 'get'
  })
}

export function saveGovernanceMetadata(
  assetId: string,
  data: IGovernanceMetadataRequest
): any {
  return axios({
    url: `/data-governance/assets/${encodeURIComponent(assetId)}/metadata`,
    method: 'put',
    data,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: (params) => JSON.stringify(params)
  })
}

export function queryGovernanceRules(assetId: string): any {
  return axios({
    url: `/data-governance/assets/${encodeURIComponent(assetId)}/rules`,
    method: 'get'
  })
}

export function saveGovernanceRule(
  assetId: string,
  data: IGovernanceQualityRule
): any {
  return axios({
    url: `/data-governance/assets/${encodeURIComponent(assetId)}/rules`,
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: (params) => JSON.stringify(params)
  })
}

export function generateGovernanceRuleSql(
  assetId: string,
  data: IGovernanceQualityRule
): any {
  return axios({
    url: `/data-governance/assets/${encodeURIComponent(
      assetId
    )}/rules/generate-sql`,
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: (params) => JSON.stringify(params)
  })
}

export function trialRunGovernanceRule(
  assetId: string,
  data: IGovernanceTrialRunRequest
): any {
  return axios({
    url: `/data-governance/assets/${encodeURIComponent(
      assetId
    )}/rules/trial-run`,
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: (params) => JSON.stringify(params)
  })
}

export function queryGovernanceLineage(assetId: string): any {
  return axios({
    url: `/data-governance/assets/${encodeURIComponent(assetId)}/lineage`,
    method: 'get'
  })
}

export function registerGovernanceSyncTaskLineage(
  data: IGovernanceSyncTaskLineageRequest
): any {
  return axios({
    url: '/data-governance/sync-task-lineage',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: (params) => JSON.stringify(params)
  })
}

export function queryGovernanceIssues(assetId: string): any {
  return axios({
    url: `/data-governance/assets/${encodeURIComponent(assetId)}/issues`,
    method: 'get'
  })
}

export function updateGovernanceIssueStatus(
  assetId: string,
  issueId: string,
  status: string
): any {
  return axios({
    url: `/data-governance/assets/${encodeURIComponent(
      assetId
    )}/issues/${encodeURIComponent(issueId)}/status`,
    method: 'put',
    data: { status },
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: (params) => JSON.stringify(params)
  })
}
