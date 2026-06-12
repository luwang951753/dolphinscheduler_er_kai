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
import { useRouter } from 'vue-router'
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

const emptyDashboard: HomeDashboardData = {
  metrics: [],
  integration: {
    header: { label: '数据汇聚', title: '', tag: '' },
    summary: [],
    rows: []
  },
  returns: {
    header: { label: '数据回传', title: '', tag: '' },
    rows: []
  },
  theme: {
    header: { label: '主题库建设', title: '', tag: '' },
    domains: []
  },
  assetMap: {
    header: { label: '资产地图', title: '', tag: '' },
    nodes: []
  },
  governance: {
    header: { label: '治理闭环', title: '', tag: '' },
    summary: [],
    issues: []
  },
  sharing: {
    header: { label: '数据共享', title: '', tag: '' },
    hero: { value: '', label: '' },
    rows: []
  }
}

const emptyText = '暂无数据'

const homeRouteByBusiness = [
  {
    routeName: 'theme-library-index',
    keywords: ['主题域', '主题库', '主题沉淀']
  },
  {
    routeName: 'data-return-index',
    keywords: ['回传']
  },
  {
    routeName: 'data-issue-index',
    keywords: ['下发', '数据问题']
  }
]

const cloneEmptyDashboard = () =>
  JSON.parse(JSON.stringify(emptyDashboard)) as HomeDashboardData

const resolveHomeRoute = (...values: Array<string | undefined>) => {
  const text = values.filter(Boolean).join(' ')
  return homeRouteByBusiness.find((item) =>
    item.keywords.some((keyword) => text.includes(keyword))
  )?.routeName
}

const EmptyBlock = (props: { text?: string }) => (
  <div class={styles.emptyBlock}>{props.text || emptyText}</div>
)

const applyDashboard = (
  dashboard: HomeDashboardData,
  data: HomeDashboardData
) => {
  dashboard.metrics = data.metrics || []
  dashboard.integration = data.integration || cloneEmptyDashboard().integration
  dashboard.returns = data.returns || cloneEmptyDashboard().returns
  dashboard.theme = data.theme || cloneEmptyDashboard().theme
  dashboard.assetMap = data.assetMap || cloneEmptyDashboard().assetMap
  dashboard.governance = data.governance || cloneEmptyDashboard().governance
  dashboard.sharing = data.sharing || cloneEmptyDashboard().sharing
}

const isAuthError = (error: unknown) => {
  const status = (error as any)?.response?.status
  return status === 401 || status === 504
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
    const router = useRouter()
    const dashboard = reactive<HomeDashboardData>(cloneEmptyDashboard())
    const state = reactive({
      loading: true,
      error: ''
    })

    const navigateTo = (routeName?: string) => {
      if (!routeName) return
      void router.push({ name: routeName })
    }

    const handlePanelKeydown = (event: KeyboardEvent, routeName: string) => {
      if (event.key !== 'Enter' && event.key !== ' ') return
      event.preventDefault()
      navigateTo(routeName)
    }

    onMounted(async () => {
      state.loading = true
      const fallback = cloneEmptyDashboard()
      const results = await Promise.allSettled([
        queryHomeMetrics(),
        queryHomeIntegration(),
        queryHomeReturns(),
        queryHomeTheme(),
        queryHomeAssetMap(),
        queryHomeGovernance(),
        queryHomeSharing()
      ])

      if (
        results.some(
          (item) => item.status === 'rejected' && isAuthError(item.reason)
        )
      ) {
        state.error = '登录状态已失效，请重新登录'
        state.loading = false
        return
      }

      const [
        metrics,
        integration,
        returns,
        theme,
        assetMap,
        governance,
        sharing
      ] = results

      applyDashboard(dashboard, {
        metrics:
          metrics.status === 'fulfilled' ? metrics.value : fallback.metrics,
        integration:
          integration.status === 'fulfilled'
            ? integration.value
            : fallback.integration,
        returns:
          returns.status === 'fulfilled' ? returns.value : fallback.returns,
        theme: theme.status === 'fulfilled' ? theme.value : fallback.theme,
        assetMap:
          assetMap.status === 'fulfilled' ? assetMap.value : fallback.assetMap,
        governance:
          governance.status === 'fulfilled'
            ? governance.value
            : fallback.governance,
        sharing:
          sharing.status === 'fulfilled' ? sharing.value : fallback.sharing
      })

      const failedCount = results.filter(
        (item) => item.status === 'rejected'
      ).length
      state.error = failedCount
        ? `首页部分数据接口加载失败，已展示可用数据（失败 ${failedCount} 项）`
        : ''
      state.loading = false
    })

    return () => (
      <main class={styles.page}>
        <section class={styles.metrics}>
          {dashboard.metrics.length ? dashboard.metrics.map((item) => (
            <button
              class={[
                styles.metric,
                resolveHomeRoute(item.label) ? styles.clickable : ''
              ]}
              key={item.label}
              type='button'
              onClick={() => navigateTo(resolveHomeRoute(item.label))}
            >
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </button>
          )) : <EmptyBlock text={state.loading ? '加载中' : state.error || emptyText} />}
        </section>

        <section class={styles.grid}>
          <div class={[styles.col, styles.left].join(' ')}>
            <section
              class={[styles.panel, styles.clickablePanel].join(' ')}
              role='button'
              tabindex={0}
              data-home-target='data-issue'
              onClickCapture={() => navigateTo('data-issue-index')}
              onKeydown={(event) => handlePanelKeydown(event, 'data-issue-index')}
            >
              <PanelHeader {...dashboard.integration.header} hideTag hideTitle />
              {dashboard.integration.summary.length ? <div class={styles.summary}>
                {dashboard.integration.summary.map((item) => (
                  <div key={item.label}><strong>{item.value}</strong><span>{item.label}</span></div>
                ))}
              </div> : null}
              {dashboard.integration.rows.length ? <div class={styles.rows}>
                {dashboard.integration.rows.map((item) => (
                  <button
                    class={[
                      styles.row,
                      resolveHomeRoute(item.name, item.status) ? styles.clickable : ''
                    ]}
                    key={item.name}
                    type='button'
                    onClick={() => navigateTo('data-issue-index')}
                  >
                    <strong>{item.name}</strong>
                    <span>{item.value}</span>
                    <em>{item.status}</em>
                  </button>
                ))}
              </div> : <EmptyBlock />}
            </section>

            <section
              class={[styles.panel, styles.returnPanel, styles.clickablePanel].join(' ')}
              role='button'
              tabindex={0}
              data-home-target='data-return'
              onClickCapture={() => navigateTo('data-return-index')}
              onKeydown={(event) => handlePanelKeydown(event, 'data-return-index')}
            >
              <PanelHeader {...dashboard.returns.header} hideTag hideTitle />
              {dashboard.returns.rows.length ? <div class={styles.rows}>
                {dashboard.returns.rows.map((item) => (
                  <button
                    class={[styles.row, styles.clickable]}
                    key={item.name}
                    type='button'
                    onClick={() => navigateTo('data-return-index')}
                  >
                    <strong>{item.name}</strong>
                    {item.value ? <span>{item.value}</span> : null}
                    <em>{item.status}</em>
                  </button>
                ))}
              </div> : <EmptyBlock />}
            </section>
          </div>

          <div class={[styles.col, styles.center].join(' ')}>
            <section
              class={[styles.panel, styles.clickablePanel].join(' ')}
              role='button'
              tabindex={0}
              data-home-target='theme-library'
              onClickCapture={() => navigateTo('theme-library-index')}
              onKeydown={(event) => handlePanelKeydown(event, 'theme-library-index')}
            >
              <PanelHeader {...dashboard.theme.header} hideTitle />
              {dashboard.theme.domains.length ? <div class={styles.domains}>
                {dashboard.theme.domains.map((item) => (
                  <button
                    class={[styles.domain, styles.clickable]}
                    key={item.name}
                    type='button'
                    onClick={() => navigateTo('theme-library-index')}
                  >
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
                  </button>
                ))}
              </div> : <EmptyBlock />}
            </section>

            <section class={styles.panel}>
              <PanelHeader {...dashboard.assetMap.header} hideTitle />
              {dashboard.assetMap.nodes.length ? <div class={styles.flow}>
                <svg viewBox='0 0 100 70' preserveAspectRatio='none'>
                  <path d='M12 50 C 24 18, 38 18, 50 46 S 76 76, 88 32' />
                  <path d='M12 50 C 32 62, 48 12, 68 24 S 82 38, 88 32' />
                </svg>
                {dashboard.assetMap.nodes.map((item) => (
                  <button
                    class={[
                      styles.node,
                      resolveHomeRoute(item.name, item.value) ? styles.clickable : ''
                    ]}
                    style={{ left: item.left, top: item.top }}
                    key={item.name}
                    type='button'
                    onClick={() => navigateTo(resolveHomeRoute(item.name, item.value))}
                  >
                    <strong>{item.name}</strong>
                    <span>{item.value}</span>
                  </button>
                ))}
              </div> : <EmptyBlock />}
            </section>
          </div>

          <div class={[styles.col, styles.right].join(' ')}>
            <section class={[styles.panel, styles.issue].join(' ')}>
              <PanelHeader {...dashboard.governance.header} hideTag hideTitle />
              {dashboard.governance.summary.length ? <div class={styles.summary}>
                {dashboard.governance.summary.map((item) => (
                  <div key={item.label}><strong>{item.value}</strong><span>{item.label}</span></div>
                ))}
              </div> : null}
              {dashboard.governance.issues.length ? <div class={styles.rows}>
                {dashboard.governance.issues.map((item) => (
                  <button
                    class={[
                      styles.row,
                      resolveHomeRoute(item.title, item.status) ? styles.clickable : ''
                    ]}
                    key={item.title}
                    type='button'
                    onClick={() => navigateTo(resolveHomeRoute(item.title, item.status))}
                  >
                    <b>{item.level}</b>
                    <strong>{item.title}</strong>
                    <em>{item.status}</em>
                  </button>
                ))}
              </div> : <EmptyBlock />}
            </section>

            <section class={styles.panel}>
              <PanelHeader {...dashboard.sharing.header} hideTag hideTitle />
              {dashboard.sharing.hero.value || dashboard.sharing.hero.label ? <div class={styles.shareHero}>
                <strong>{dashboard.sharing.hero.value}</strong>
                <span>{dashboard.sharing.hero.label}</span>
              </div> : null}
              {dashboard.sharing.rows.length ? <div class={styles.rows}>
                {dashboard.sharing.rows.map((item) => (
                  <button
                    class={[
                      styles.row,
                      resolveHomeRoute(item.name, item.status) ? styles.clickable : ''
                    ]}
                    key={item.name}
                    type='button'
                    onClick={() => navigateTo(resolveHomeRoute(item.name, item.status))}
                  >
                    <strong>{item.name}</strong>
                    <span>{item.value}</span>
                    <em>{item.status}</em>
                  </button>
                ))}
              </div> : <EmptyBlock />}
            </section>
          </div>
        </section>
      </main>
    )
  }
})
