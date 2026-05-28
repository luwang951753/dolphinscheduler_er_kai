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

import { defineComponent, PropType, computed } from 'vue'
import { NDropdown, NIcon, NButton, NSelect, NTag } from 'naive-ui'
import { DownOutlined, UserOutlined } from '@vicons/antd'
import { useDropDown } from './use-dropdown'
import { useUserStore } from '@/store/user/user'
import { useLocalesStore } from '@/store/locales/locales'
import { useTimezoneStore } from '@/store/timezone/timezone'
import { useThemeStore } from '@/store/theme/theme'
import styles from './index.module.scss'
import type { UserInfoRes } from '@/service/modules/users/types'
import { useI18n } from 'vue-i18n'
import { h } from 'vue'

const User = defineComponent({
  name: 'User',
  props: {
    userDropdownOptions: {
      type: Array as PropType<any>,
      default: []
    },
    localesOptions: {
      type: Array as PropType<any>,
      default: []
    },
    timezoneOptions: {
      type: Array as PropType<any>,
      default: []
    }
  },
  setup(props) {
    const { t } = useI18n()
    const { handleSelect, handleTimezoneSelect } = useDropDown()
    const userStore = useUserStore()
    const localesStore = useLocalesStore()
    const timezoneStore = useTimezoneStore()
    const themeStore = useThemeStore()
    const currentTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone

    const localesLabel = computed(
      () =>
        props.localesOptions.find(
          (item: { key: string }) => item.key === localesStore.getLocales
        )?.label || localesStore.getLocales
    )

    const timezoneLabel = computed(() => timezoneStore.getTimezone || currentTimeZone)

    const mergedDropdownOptions = computed(() => [
      {
        label: t('menu.ui_setting'),
        key: 'ui-setting'
      },
      {
        label: t(themeStore.darkTheme ? 'theme.light' : 'theme.dark'),
        key: 'theme-toggle'
      },
      {
        label: () =>
          h('div', { class: styles['preference-row'] }, [
            h('span', null, t('user_dropdown.language')),
            h(NTag, { size: 'small', bordered: false }, { default: () => localesLabel.value })
          ]),
        key: 'language',
        children: props.localesOptions.map((item: { label: string; key: string }) => ({
          label: item.label,
          key: `language:${item.key}`
        }))
      },
      {
        key: 'timezone-panel',
        type: 'render',
        render: () =>
          h('div', { class: styles['preference-column'] }, [
            h('div', { class: styles['preference-row'] }, [
              h('span', null, t('user_dropdown.timezone')),
              h(NTag, { size: 'small', bordered: false, type: 'info' }, { default: () => timezoneLabel.value })
            ]),
            h(NSelect, {
              class: styles['timezone-select'],
              filterable: true,
              size: 'small',
              placeholder: t('profile.please_select_timezone'),
              options: props.timezoneOptions,
              value: timezoneStore.getTimezone,
              onUpdateValue: handleTimezoneSelect
            })
          ])
      },
      {
        key: 'preference-divider',
        type: 'divider'
      },
      ...props.userDropdownOptions
    ])

    return { handleSelect, userStore, mergedDropdownOptions }
  },
  render() {
    return (
      <NDropdown
        trigger='click'
        show-arrow
        options={this.mergedDropdownOptions}
        on-select={this.handleSelect}
      >
        <NButton text>
          <NIcon class={styles.icon}>
            <UserOutlined />
          </NIcon>
          {(this.userStore.getUserInfo as UserInfoRes).userName}
          <NIcon class={styles.icon}>
            <DownOutlined />
          </NIcon>
        </NButton>
      </NDropdown>
    )
  }
})

export default User
