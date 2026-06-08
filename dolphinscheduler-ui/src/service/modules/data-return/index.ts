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
  DataReturnParamMappingResult,
  DataReturnQueryParams,
  DataReturnQueryResult
} from './types'

const dataReturnMagicPrefix = '/magic-api/data-return'

export function queryDataReturn(
  params: DataReturnQueryParams
): Promise<DataReturnQueryResult | Array<Record<string, any>>> {
  return axios({
    url: `${dataReturnMagicPrefix}/query`,
    method: 'get',
    params,
    acceptMagicApiSuccess: true,
    suppressErrorMessage: true
  } as any) as any
}

export function queryDataReturnParamMapping(): Promise<DataReturnParamMappingResult> {
  return axios({
    url: `${dataReturnMagicPrefix}/param-mapping`,
    method: 'get',
    acceptMagicApiSuccess: true,
    suppressErrorMessage: true
  } as any) as any
}
