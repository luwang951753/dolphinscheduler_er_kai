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

type DataIssueTarget = 'DATA_CENTER' | 'PHASE3_PLATFORM'

interface DataIssueQueryParams {
  [key: string]: string | undefined
  suspectNo?: string
  suspectName?: string
  suspectIdCard?: string
  caseNo?: string
  caseName?: string
  target: DataIssueTarget
}

interface DataIssueColumn {
  key: string
  title: string
  width?: number
}

interface DataIssueQueryResult {
  title?: string
  columns?: DataIssueColumn[]
  rows?: Array<Record<string, any>>
  total?: number
  updatedAt?: string
  remark?: string
}

interface DataIssueParamMappingField {
  key: string
  label: string
  paramName: string
}

interface DataIssueParamMappingResult {
  fields?: DataIssueParamMappingField[]
}

export type {
  DataIssueTarget,
  DataIssueQueryParams,
  DataIssueColumn,
  DataIssueQueryResult,
  DataIssueParamMappingField,
  DataIssueParamMappingResult
}
