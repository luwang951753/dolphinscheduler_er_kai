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

import type {
  HomeAssetMapData,
  HomeGovernanceData,
  HomeIntegrationData,
  HomeMetric,
  HomeReturnData,
  HomeSharingData,
  HomeThemeData
} from './types'
import { axios } from '@/service/service'

const homeMagicPrefix = '/magic-api/sy'

type MagicApiEnvelope<T> = {
  code?: number
  msg?: string
  message?: string
  data?: T
}

async function queryHomeMagic<T>(path: string): Promise<T> {
  const body = (await axios({
    url: `${homeMagicPrefix}/${path}`,
    method: 'get',
    suppressErrorMessage: true,
    acceptMagicApiSuccess: true
  } as any)) as MagicApiEnvelope<T> | T
  if (Array.isArray(body)) return body as T

  if (!body || typeof body !== 'object') return body as T

  const envelope = body as MagicApiEnvelope<T>
  const success =
    envelope.code === undefined ||
    envelope.code === 0 ||
    (envelope.code === 1 && envelope.message === 'success')

  if (!success) {
    throw new Error(
      envelope.msg || envelope.message || `首页 Magic API 返回失败: ${path}`
    )
  }

  return envelope.data === undefined ? (body as T) : envelope.data
}

export function queryHomeMetrics(): Promise<HomeMetric[]> {
  return queryHomeMagic<HomeMetric[]>('metrics')
}

export function queryHomeIntegration(): Promise<HomeIntegrationData> {
  return queryHomeMagic<HomeIntegrationData>('integration')
}

export function queryHomeReturns(): Promise<HomeReturnData> {
  return queryHomeMagic<HomeReturnData>('returns')
}

export function queryHomeTheme(): Promise<HomeThemeData> {
  return queryHomeMagic<HomeThemeData>('theme-progress')
}

export function queryHomeAssetMap(): Promise<HomeAssetMapData> {
  return queryHomeMagic<HomeAssetMapData>('asset-map')
}

export function queryHomeGovernance(): Promise<HomeGovernanceData> {
  return queryHomeMagic<HomeGovernanceData>('governance')
}

export function queryHomeSharing(): Promise<HomeSharingData> {
  return queryHomeMagic<HomeSharingData>('sharing')
}
