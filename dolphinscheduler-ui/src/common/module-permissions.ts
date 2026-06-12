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
  ModulePermissionKey,
  UserInfoRes
} from '@/service/modules/users/types'

export type { ModulePermissionKey } from '@/service/modules/users/types'

export interface ModulePermissionOption {
  label: string
  value: ModulePermissionKey
  description: string
}

export const MODULE_PERMISSION_OPTIONS: ModulePermissionOption[] = [
  {
    label: '同步',
    value: 'sync-task:view',
    description: '允许访问同步任务模块，配置和查看同步作业。'
  },
  {
    label: '预览',
    value: 'data-preview:view',
    description: '允许访问数据预览模块，查看表结构和查询数据。'
  },
  {
    label: '主题库',
    value: 'theme-library:view',
    description: '允许访问主题库模块，查看主题、业务项和分析场景。'
  },
  {
    label: '白皮书',
    value: 'whitepaper:view',
    description: '允许访问白皮书模块，配置报表数据集和编辑报告模板。'
  },
  {
    label: '治理',
    value: 'data-governance:view',
    description: '允许访问数据治理模块，查看血缘、质量和治理事项。'
  },
  {
    label: '监控',
    value: 'monitor:view',
    description: '允许访问监控模块，查看服务、统计和审计日志。'
  },
  {
    label: '资源',
    value: 'resources:view',
    description: '允许访问资源中心，管理文件资源和任务组资源。'
  }
]

export const hasModulePermission = (
  userInfo: UserInfoRes | {},
  permission?: ModulePermissionKey,
  modulePermissions?: ModulePermissionKey[] | null
) => {
  if (!permission) {
    return true
  }
  const user = userInfo as UserInfoRes
  if (user.userType === 'ADMIN_USER') {
    return true
  }
  if (!modulePermissions) {
    return true
  }
  return modulePermissions.includes(permission)
}
