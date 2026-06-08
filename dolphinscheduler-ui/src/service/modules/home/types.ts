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

export interface HomeMetric {
  label: string
  value: string
}

export interface HomePanelHeader {
  label: string
  title: string
  tag: string
}

export interface HomeSummaryItem {
  label: string
  value: string
}

export interface HomeRowItem {
  name: string
  value: string
  status: string
}

export interface HomeDomainItem {
  name: string
  percent: string
  asset: string
  service: string
  quality: string
}

export interface HomeFlowNode {
  name: string
  value: string
  left: string
  top: string
}

export interface HomeIssueItem {
  level: string
  title: string
  status: string
}

export interface HomeIntegrationData {
  header: HomePanelHeader
  summary: HomeSummaryItem[]
  rows: HomeRowItem[]
}

export interface HomeThemeData {
  header: HomePanelHeader
  domains: HomeDomainItem[]
}

export interface HomeAssetMapData {
  header: HomePanelHeader
  nodes: HomeFlowNode[]
}

export interface HomeGovernanceData {
  header: HomePanelHeader
  summary: HomeSummaryItem[]
  issues: HomeIssueItem[]
}

export interface HomeSharingData {
  header: HomePanelHeader
  hero: HomeSummaryItem
  rows: HomeRowItem[]
}

export interface HomeReturnData {
  header: HomePanelHeader
  rows: HomeRowItem[]
}

export interface HomeDashboardData {
  metrics: HomeMetric[]
  integration: HomeIntegrationData
  returns: HomeReturnData
  theme: HomeThemeData
  assetMap: HomeAssetMapData
  governance: HomeGovernanceData
  sharing: HomeSharingData
}
