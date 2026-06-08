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
  queryHomeAssetMap,
  queryHomeGovernance,
  queryHomeIntegration,
  queryHomeMetrics,
  queryHomeReturns,
  queryHomeSharing,
  queryHomeTheme
} from '@/service/modules/home'
import type { HomeDashboardData, HomePanelHeader } from '@/service/modules/home/types'
import styles from './index.module.scss'

const fallbackDashboard: HomeDashboardData = {
  metrics: [
    { label: '接入数据源', value: '38' },
    { label: '资产表总量', value: '12,846' },
    { label: '主题库覆盖', value: '27' },
    { label: '共享服务', value: '156' },
    { label: '治理规则', value: '428' },
    { label: '质量评分', value: '96.8' }
  ],
  integration: {
    header: { label: '数据汇聚', title: '来源系统接入状态', tag: '99.21%' },
    summary: [
      { value: '8.72TB', label: '本月入湖' },
      { value: '430', label: '库表接入' }
    ],
    rows: [
      { name: 'Oracle 警情库', value: '126 表', status: '稳定' },
      { name: 'MySQL 业务库', value: '18 源', status: '稳定' },
      { name: 'Hive 离线数仓', value: '9 源', status: '同步中' },
      { name: 'SFTP 文件交换', value: '11 源', status: '待治理' }
    ]
  },
  returns: {
    header: { label: '数据回传', title: '外部系统回传状态', tag: '5 类' },
    rows: [
      { name: '市局C3回传', value: '', status: '运行中' },
      { name: 'APP预约回传', value: '', status: '正常' },
      { name: '涉案财务回传', value: '', status: '待复核' },
      { name: '回传到警综库', value: '', status: '稳定' },
      { name: '回传到海淀数据中心', value: '', status: '同步中' }
    ]
  },
  theme: {
    header: { label: '主题库建设', title: '重点主题域进度', tag: '27 个主题' },
    domains: [
      { name: '警情主题库', percent: '92%', asset: '资产 2,418', service: '服务 35', quality: '质量 98.1' },
      { name: '人口主题库', percent: '86%', asset: '资产 1,906', service: '服务 28', quality: '质量 96.7' },
      { name: '案件主题库', percent: '78%', asset: '资产 1,324', service: '服务 21', quality: '质量 94.9' },
      { name: '车辆主题库', percent: '71%', asset: '资产 968', service: '服务 17', quality: '质量 92.6' }
    ]
  },
  assetMap: {
    header: { label: '资产地图', title: '从汇聚到共享的主链路', tag: '运营中' },
    nodes: [
      { name: '数据汇聚', value: '38 源', left: '8%', top: '50%' },
      { name: '标准建模', value: '214 模型', left: '30%', top: '24%' },
      { name: '主题沉淀', value: '27 主题', left: '52%', top: '50%' },
      { name: '共享服务', value: '156 API', left: '74%', top: '24%' },
      { name: '治理闭环', value: '96.8 分', left: '88%', top: '58%' }
    ]
  },
  governance: {
    header: { label: '治理闭环', title: '质量问题处理', tag: '97.4%' },
    summary: [
      { value: '37', label: '待处理' },
      { value: '18', label: '今日修复' }
    ],
    issues: [
      { level: '高', title: '人口主题库手机号空值异常', status: '处理中' },
      { level: '中', title: '案件明细表枚举值超出标准', status: '待复核' },
      { level: '中', title: '共享接口缺少调用方授权', status: '处理中' }
    ]
  },
  sharing: {
    header: { label: '数据共享', title: '服务调用与性能', tag: '82ms' },
    hero: { value: '186.4万', label: '本月服务调用' },
    rows: [
      { name: '警情态势服务', value: '42.8万', status: '+18%' },
      { name: '重点人员画像', value: '31.6万', status: '+9%' },
      { name: '案件协同查询', value: '25.1万', status: '+12%' },
      { name: '区域风险评估', value: '18.7万', status: '+6%' }
    ]
  }
}

const runSilently = <T,>(request: Promise<T>, apply: (data: T) => void) => {
  request.then(apply).catch(() => undefined)
}

const PanelHeader = (
  props: HomePanelHeader & { hideTag?: boolean; hideTitle?: boolean }
) => (
  <div class={styles.head}>
    <div>
      <span class={props.hideTitle ? styles.headPrimary : undefined}>
        {props.label}
      </span>
      {props.hideTitle ? null : <strong>{props.title}</strong>}
    </div>
    {props.hideTag ? null : <b>{props.tag}</b>}
  </div>
)

export default defineComponent({
  name: 'home',
  setup() {
    const dashboard = reactive<HomeDashboardData>(
      JSON.parse(JSON.stringify(fallbackDashboard))
    )

    onMounted(() => {
      runSilently(queryHomeMetrics(), (data) => {
        dashboard.metrics = data
      })
      runSilently(queryHomeIntegration(), (data) => {
        dashboard.integration = data
      })
      runSilently(queryHomeReturns(), (data) => {
        dashboard.returns = data
      })
      runSilently(queryHomeTheme(), (data) => {
        dashboard.theme = data
      })
      runSilently(queryHomeAssetMap(), (data) => {
        dashboard.assetMap = data
      })
      runSilently(queryHomeGovernance(), (data) => {
        dashboard.governance = data
      })
      runSilently(queryHomeSharing(), (data) => {
        dashboard.sharing = data
      })
    })

    return () => (
      <main class={styles.page}>
        <section class={styles.metrics}>
          {dashboard.metrics.map((item) => (
            <div class={styles.metric} key={item.label}>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </div>
          ))}
        </section>

        <section class={styles.grid}>
          <div class={[styles.col, styles.left].join(' ')}>
            <section class={styles.panel}>
              <PanelHeader {...dashboard.integration.header} hideTag hideTitle />
              <div class={styles.summary}>
                {dashboard.integration.summary.map((item) => (
                  <div key={item.label}><strong>{item.value}</strong><span>{item.label}</span></div>
                ))}
              </div>
              <div class={styles.rows}>
                {dashboard.integration.rows.map((item) => (
                  <div class={styles.row} key={item.name}>
                    <strong>{item.name}</strong>
                    <span>{item.value}</span>
                    <em>{item.status}</em>
                  </div>
                ))}
              </div>
            </section>

            <section class={[styles.panel, styles.returnPanel].join(' ')}>
              <PanelHeader {...dashboard.returns.header} hideTag hideTitle />
              <div class={styles.rows}>
                {dashboard.returns.rows.map((item) => (
                  <div class={styles.row} key={item.name}>
                    <strong>{item.name}</strong>
                    {item.value ? <span>{item.value}</span> : null}
                    <em>{item.status}</em>
                  </div>
                ))}
              </div>
            </section>
          </div>

          <div class={[styles.col, styles.center].join(' ')}>
            <section class={styles.panel}>
              <PanelHeader {...dashboard.theme.header} hideTitle />
              <div class={styles.domains}>
                {dashboard.theme.domains.map((item) => (
                  <div class={styles.domain} key={item.name}>
                    <div class={styles.domainTop}>
                      <strong>{item.name}</strong>
                      <span>{item.percent}</span>
                    </div>
                    <div class={styles.bar}>
                      <i style={{ width: item.percent }} />
                    </div>
                    <div class={styles.domainMeta}>
                      <span>{item.asset}</span>
                      <span>{item.service}</span>
                      <span>{item.quality}</span>
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section class={styles.panel}>
              <PanelHeader {...dashboard.assetMap.header} hideTitle />
              <div class={styles.flow}>
                <svg viewBox='0 0 100 70' preserveAspectRatio='none'>
                  <path d='M12 50 C 24 18, 38 18, 50 46 S 76 76, 88 32' />
                  <path d='M12 50 C 32 62, 48 12, 68 24 S 82 38, 88 32' />
                </svg>
                {dashboard.assetMap.nodes.map((item) => (
                  <div class={styles.node} style={{ left: item.left, top: item.top }} key={item.name}>
                    <strong>{item.name}</strong>
                    <span>{item.value}</span>
                  </div>
                ))}
              </div>
            </section>
          </div>

          <div class={[styles.col, styles.right].join(' ')}>
            <section class={[styles.panel, styles.issue].join(' ')}>
              <PanelHeader {...dashboard.governance.header} hideTag hideTitle />
              <div class={styles.summary}>
                {dashboard.governance.summary.map((item) => (
                  <div key={item.label}><strong>{item.value}</strong><span>{item.label}</span></div>
                ))}
              </div>
              <div class={styles.rows}>
                {dashboard.governance.issues.map((item) => (
                  <div class={styles.row} key={item.title}>
                    <b>{item.level}</b>
                    <strong>{item.title}</strong>
                    <em>{item.status}</em>
                  </div>
                ))}
              </div>
            </section>

            <section class={styles.panel}>
              <PanelHeader {...dashboard.sharing.header} hideTag hideTitle />
              <div class={styles.shareHero}>
                <strong>{dashboard.sharing.hero.value}</strong>
                <span>{dashboard.sharing.hero.label}</span>
              </div>
              <div class={styles.rows}>
                {dashboard.sharing.rows.map((item) => (
                  <div class={styles.row} key={item.name}>
                    <strong>{item.name}</strong>
                    <span>{item.value}</span>
                    <em>{item.status}</em>
                  </div>
                ))}
              </div>
            </section>
          </div>
        </section>
      </main>
    )
  }
})
