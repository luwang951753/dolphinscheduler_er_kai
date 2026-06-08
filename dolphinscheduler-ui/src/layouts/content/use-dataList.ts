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

import { reactive, h } from 'vue'
import { NEllipsis, NIcon } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import {
  HomeOutlined,
  ProfileOutlined,
  FolderOutlined,
  DatabaseOutlined,
  DeploymentUnitOutlined,
  DesktopOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
  SelectOutlined,
  LogoutOutlined,
  FundProjectionScreenOutlined,
  PartitionOutlined,
  SettingOutlined,
  FileSearchOutlined,
  AppstoreOutlined,
  UsergroupAddOutlined,
  UserAddOutlined,
  WarningOutlined,
  InfoCircleOutlined,
  ControlOutlined,
  SlackOutlined,
  EnvironmentOutlined,
  KeyOutlined,
  SafetyOutlined,
  GroupOutlined,
  CloudServerOutlined,
  ClusterOutlined,
  TableOutlined,
  ApartmentOutlined,
  SwapOutlined
} from '@vicons/antd'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user/user'
import { timezoneList } from '@/common/timezone'
import type { UserInfoRes } from '@/service/modules/users/types'
import {
  hasModulePermission,
  type ModulePermissionKey
} from '@/common/module-permissions'

export function useDataList() {
  const { t } = useI18n()
  const route = useRoute()
  const userStore = useUserStore()

  const renderIcon = (icon: any) => {
    return () => h(NIcon, null, { default: () => h(icon) })
  }

  const localesOptions = [
    {
      label: 'English',
      key: 'en_US'
    },
    {
      label: '中文',
      key: 'zh_CN'
    }
  ]

  const timezoneOptions = () =>
    timezoneList.map((item) => ({ label: item, value: item }))

  const canShowModule = (permission: ModulePermissionKey) =>
    hasModulePermission(
      userStore.getUserInfo,
      permission,
      userStore.getModulePermissions
    )

  const filterVisibleMenus = (menus: any[]): any[] =>
    menus
      .filter((menu) => !menu.modulePermission || canShowModule(menu.modulePermission))
      .map((menu) => ({
        ...menu,
        children: menu.children ? filterVisibleMenus(menu.children) : menu.children
      }))

  const state = reactive({
    isShowSide: false,
    localesOptions,
    timezoneOptions: timezoneOptions(),
    userDropdownOptions: [],
    menuOptions: [],
    headerMenuOptions: [],
    sideMenuOptions: []
  })

  const changeMenuOption = (state: any) => {
    const projectCode = route.params.projectCode || ''
    const projectName = route.query.projectName || ''
    state.menuOptions = [
      {
        label: () => h(NEllipsis, null, { default: () => t('menu.home') }),
        key: 'home',
        icon: renderIcon(HomeOutlined)
      },
      {
        label: () => h(NEllipsis, null, { default: () => t('menu.project') }),
        key: 'projects',
        icon: renderIcon(ProfileOutlined),
        children: [
          {
            label: t('menu.project') + (projectName ? `[${projectName}]` : ''),
            key: `/projects/${projectCode}`,
            icon: renderIcon(FundProjectionScreenOutlined),
            payload: { projectName: projectName },
            children: [
              {
                label: t('menu.project_overview'),
                key: `/projects/${projectCode}`,
                payload: { projectName: projectName }
              },
              {
                label: t('menu.project_parameter'),
                key: `/projects/${projectCode}/parameter`,
                payload: { projectName: projectName }
              },
              {
                label: t('menu.project_preferences'),
                key: `/projects/${projectCode}/preferences`,
                payload: { projectName: projectName }
              }
            ]
          },
          {
            label: t('menu.workflow'),
            key: 'workflow',
            icon: renderIcon(PartitionOutlined),
            children: [
              {
                label: t('menu.workflow_relation'),
                key: `/projects/${projectCode}/workflow/relation`,
                payload: { projectName: projectName }
              },
              {
                label: t('menu.workflow_definition'),
                key: `/projects/${projectCode}/workflow-definition`,
                payload: { projectName: projectName }
              },
              {
                label: t('menu.workflow_instance'),
                key: `/projects/${projectCode}/workflow/instances`,
                payload: { projectName: projectName }
              },
              {
                label: t('menu.workflow_timing'),
                key: `/projects/${projectCode}/workflow/timings`,
                payload: { projectName: projectName }
              }
            ]
          },
          {
            label: t('menu.task'),
            key: 'task',
            icon: renderIcon(SettingOutlined),
            children: [
              {
                label: t('menu.task_instance'),
                key: `/projects/${projectCode}/task/instances`,
                payload: { projectName: projectName }
              }
            ]
          }
        ]
      },
      {
        label: () => h(NEllipsis, null, { default: () => t('menu.resources') }),
        key: 'resource',
        icon: renderIcon(FolderOutlined),
        modulePermission: 'resources:view',
        children: [
          {
            label: t('menu.file_manage'),
            key: '/resource/file-manage',
            icon: renderIcon(FileSearchOutlined)
          },
          {
            label: t('menu.task_group_manage'),
            key: 'task-group-manage',
            icon: renderIcon(GroupOutlined),
            children: [
              {
                label: t('menu.task_group_option'),
                key: '/resource/task-group-option'
              },
              {
                label: t('menu.task_group_queue'),
                key: '/resource/task-group-queue'
              }
            ]
          }
        ]
      },
      {
        label: () =>
          h(NEllipsis, null, { default: () => t('menu.datasource') }),
        key: 'datasource',
        icon: renderIcon(DatabaseOutlined),
        children: []
      },
      {
        label: () =>
          h(NEllipsis, null, { default: () => t('menu.sync_task') }),
        key: 'sync-task',
        icon: renderIcon(DeploymentUnitOutlined),
        modulePermission: 'sync-task:view',
        children: []
      },
      {
        label: () =>
          h(NEllipsis, null, { default: () => t('menu.data_preview') }),
        key: 'data-preview',
        icon: renderIcon(TableOutlined),
        modulePermission: 'data-preview:view',
        children: []
      },
      {
        label: () =>
          h(NEllipsis, null, { default: () => t('menu.theme_library') }),
        key: 'theme-library',
        icon: renderIcon(ClusterOutlined),
        modulePermission: 'theme-library:view',
        children: []
      },
      {
        label: () =>
          h(NEllipsis, null, { default: () => t('menu.data_governance') }),
        key: 'data-governance',
        icon: renderIcon(ApartmentOutlined),
        modulePermission: 'data-governance:view',
        children: []
      },
      {
        label: () =>
          h(NEllipsis, null, { default: () => t('menu.data_return') }),
        key: 'data-return',
        icon: renderIcon(SwapOutlined),
        children: []
      },
      {
        label: () =>
          h(NEllipsis, null, { default: () => t('menu.data_issue') }),
        key: 'data-issue',
        icon: renderIcon(SelectOutlined),
        children: []
      },
      {
        label: () => h(NEllipsis, null, { default: () => t('menu.monitor') }),
        key: 'monitor',
        icon: renderIcon(DesktopOutlined),
        modulePermission: 'monitor:view',
        children: [
          {
            label: t('menu.instance_statistics'),
            key: '/monitor/instance-statistics',
            icon: renderIcon(FundProjectionScreenOutlined)
          },
          {
            label: t('menu.service_manage'),
            key: 'service-manage',
            icon: renderIcon(AppstoreOutlined),
            children: [
              {
                label: t('menu.master'),
                key: '/monitor/master'
              },
              {
                label: t('menu.worker'),
                key: '/monitor/worker'
              },
              {
                label: t('menu.alert_server'),
                key: '/monitor/alert_server'
              },
              {
                label: t('menu.db'),
                key: '/monitor/db'
              }
            ]
          },
          {
            label: t('menu.statistical_manage'),
            key: 'statistical-manage',
            icon: renderIcon(AppstoreOutlined),
            children: [
              {
                label: t('menu.statistics'),
                key: '/monitor/statistics'
              },
              {
                label: t('menu.audit_log'),
                key: '/monitor/audit-log'
              }
            ]
          }
        ]
      },
      {
        label: () => h(NEllipsis, null, { default: () => t('menu.security') }),
        key: 'security',
        icon: renderIcon(SafetyCertificateOutlined),
        children:
          (userStore.getUserInfo as UserInfoRes).userType === 'ADMIN_USER'
            ? [
                {
                  label: t('menu.tenant_manage'),
                  key: '/security/tenant-manage',
                  icon: renderIcon(UsergroupAddOutlined)
                },
                {
                  label: t('menu.user_manage'),
                  key: '/security/user-manage',
                  icon: renderIcon(UserAddOutlined)
                },
                {
                  label: t('menu.alarm_group_manage'),
                  key: '/security/alarm-group-manage',
                  icon: renderIcon(WarningOutlined)
                },
                {
                  label: t('menu.alarm_instance_manage'),
                  key: '/security/alarm-instance-manage',
                  icon: renderIcon(InfoCircleOutlined)
                },
                {
                  label: t('menu.worker_group_manage'),
                  key: '/security/worker-group-manage',
                  icon: renderIcon(ControlOutlined)
                },
                {
                  label: t('menu.yarn_queue_manage'),
                  key: '/security/yarn-queue-manage',
                  icon: renderIcon(SlackOutlined)
                },
                {
                  label: t('menu.environment_manage'),
                  key: '/security/environment-manage',
                  icon: renderIcon(EnvironmentOutlined)
                },
                {
                  label: t('menu.cluster_manage'),
                  key: '/security/cluster-manage',
                  icon: renderIcon(ClusterOutlined)
                },
                {
                  label: t('menu.k8s_namespace_manage'),
                  key: '/security/k8s-namespace-manage',
                  icon: renderIcon(CloudServerOutlined)
                },
                {
                  label: t('menu.token_manage'),
                  key: '/security/token-manage',
                  icon: renderIcon(SafetyOutlined)
                },
                {
                  label: t('menu.magic_api'),
                  key: '/security/magic-api',
                  icon: renderIcon(ApartmentOutlined)
                }
              ]
            : [
                {
                  label: t('menu.token_manage'),
                  key: '/security/token-manage',
                  icon: renderIcon(SafetyOutlined)
                }
              ]
      }
    ]
    state.menuOptions = filterVisibleMenus(state.menuOptions)
  }

  const changeHeaderMenuOptions = (state: any) => {
    const headerOrder = [
      'home',
      'projects',
      'datasource',
      'sync-task',
      'data-preview',
      'theme-library',
      'data-governance',
      'data-return',
      'data-issue',
      'monitor',
      'resource',
      'security'
    ]
    const headerLabels: Record<string, string> = {
      home: '首页',
      projects: '项目',
      resource: '资源',
      datasource: '数据源',
      'sync-task': '同步',
      'data-preview': '预览',
      'theme-library': '主题库',
      'data-governance': '治理',
      'data-return': '回传',
      'data-issue': '下发',
      monitor: '监控',
      security: '安全'
    }
    state.headerMenuOptions = state.menuOptions
      .map((item: { label: string; key: string; icon: any }) => {
        return {
          label: headerLabels[item.key] || item.label,
          key: item.key,
          icon: item.icon
        }
      })
      .sort((left: { key: string }, right: { key: string }) => {
        return headerOrder.indexOf(left.key) - headerOrder.indexOf(right.key)
      }
      )
  }

  const changeUserDropdown = (state: any) => {
    state.userDropdownOptions = [
      {
        label: t('user_dropdown.profile'),
        key: 'profile',
        icon: renderIcon(UserOutlined)
      },
      {
        label: t('user_dropdown.password'),
        key: 'password',
        icon: renderIcon(KeyOutlined),
        disabled: userStore.getSecurityConfigType !== 'PASSWORD'
      },
      {
        label: t('user_dropdown.about'),
        key: 'about',
        icon: renderIcon(SelectOutlined)
      },
      {
        label: t('user_dropdown.logout'),
        key: 'logout',
        icon: renderIcon(LogoutOutlined)
      }
    ]
  }

  return {
    state,
    changeHeaderMenuOptions,
    changeMenuOption,
    changeUserDropdown
  }
}
