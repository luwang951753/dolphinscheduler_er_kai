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

import type { Component } from 'vue'
import utils from '@/utils'

// All TSX files under the views folder automatically generate mapping relationship
const modules = import.meta.glob('/src/views/**/**.tsx')
const components: { [key: string]: Component } = utils.mapping(modules)

export default {
  path: '/monitor',
  name: 'monitor',
  meta: { title: 'monitor' },
  redirect: { name: 'instance-statistics' },
  component: () => import('@/layouts/content'),
  children: [
    {
      path: '/monitor/instance-statistics',
      name: 'instance-statistics',
      component: components['monitor-statistics-instance-statistics'],
      meta: {
        title: '实例统计',
        activeMenu: 'monitor',
        showSide: true,
        auth: [],
        modulePermission: 'monitor:view'
      }
    },
    {
      path: '/monitor/master',
      name: 'servers-master',
      component: components['monitor-servers-master'],
      meta: {
        title: '服务管理-Master',
        activeMenu: 'monitor',
        showSide: true,
        auth: [],
        modulePermission: 'monitor:view'
      }
    },
    {
      path: '/monitor/worker',
      name: 'servers-worker',
      component: components['monitor-servers-worker'],
      meta: {
        title: '服务管理-Worker',
        activeMenu: 'monitor',
        showSide: true,
        auth: [],
        modulePermission: 'monitor:view'
      }
    },
    {
      path: '/monitor/alert_server',
      name: 'servers-alert-server',
      component: components['monitor-servers-alert_server'],
      meta: {
        title: '服务管理-Alert Server',
        activeMenu: 'monitor',
        showSide: true,
        auth: [],
        modulePermission: 'monitor:view'
      }
    },
    {
      path: '/monitor/db',
      name: 'servers-db',
      component: components['monitor-servers-db'],
      meta: {
        title: '服务管理-DB',
        activeMenu: 'monitor',
        showSide: true,
        auth: [],
        modulePermission: 'monitor:view'
      }
    },
    {
      path: '/monitor/statistics',
      name: 'statistics-statistics',
      component: components['monitor-statistics-statistics'],
      meta: {
        title: '统计管理-Statistics',
        activeMenu: 'monitor',
        showSide: true,
        auth: [],
        modulePermission: 'monitor:view'
      }
    },
    {
      path: '/monitor/audit-log',
      name: 'statistics-audit-log',
      component: components['monitor-statistics-audit-log'],
      meta: {
        title: '审计日志-AuditLog',
        activeMenu: 'monitor',
        showSide: true,
        auth: [],
        modulePermission: 'monitor:view'
      }
    }
  ]
}
