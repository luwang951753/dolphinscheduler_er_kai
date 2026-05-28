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

import { useRouter } from 'vue-router'
import { logout } from '@/service/modules/logout'
import { useUserStore } from '@/store/user/user'
import { useLocalesStore } from '@/store/locales/locales'
import { useTimezoneStore } from '@/store/timezone/timezone'
import { useThemeStore } from '@/store/theme/theme'
import { updateUser } from '@/service/modules/users'
import type { Router } from 'vue-router'
import { DropdownOption } from 'naive-ui'
import cookies from 'js-cookie'
import { useI18n } from 'vue-i18n'
import type { Locales } from '@/store/locales/types'
import type { UserInfoRes } from '@/service/modules/users/types'

export function useDropDown() {
  const router: Router = useRouter()
  const userStore = useUserStore()
  const localesStore = useLocalesStore()
  const timezoneStore = useTimezoneStore()
  const themeStore = useThemeStore()
  const { locale } = useI18n()

  const handleSelect = (key: string | number, unused: DropdownOption) => {
    const selectedKey = String(key)
    if (selectedKey === 'logout') {
      useLogout()
    } else if (selectedKey === 'password') {
      router.push({ path: '/password' })
    } else if (selectedKey === 'about') {
      router.push({ path: '/about' })
    } else if (selectedKey === 'profile') {
      router.push({ path: '/profile' })
    } else if (selectedKey === 'ui-setting') {
      router.push({ path: '/ui-setting' })
    } else if (selectedKey === 'theme-toggle') {
      themeStore.darkTheme = !themeStore.darkTheme
    } else if (selectedKey.startsWith('language:')) {
      const nextLocale = selectedKey.replace('language:', '') as Locales
      locale.value = nextLocale
      localesStore.setLocales(nextLocale)
      cookies.set('language', nextLocale, { path: '/' })
    }
  }

  const handleTimezoneSelect = (key: string) => {
    const userInfo = userStore.userInfo as UserInfoRes
    updateUser({
      userPassword: '',
      id: userInfo.id,
      userName: '',
      tenantId: userInfo.tenantId,
      email: '',
      phone: userInfo.phone,
      state: userInfo.state,
      timeZone: key
    }).then(() => {
      timezoneStore.setTimezone(key)
    })
  }

  const useLogout = () => {
    logout().then(() => {
      userStore.setSessionId('')
      userStore.setSecurityConfigType('')
      userStore.setUserInfo({})
      userStore.setBaseResDir('')
      cookies.remove('sessionId')

      router.push({ path: '/login' })
    })
  }

  return {
    handleSelect,
    handleTimezoneSelect
  }
}
