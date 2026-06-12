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

import axios, { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import { useUserStore } from '@/store/user/user'
import { useUISettingStore } from '@/store/ui-setting/ui-setting'
import qs from 'qs'
import _ from 'lodash'
import cookies from 'js-cookie'
import router from '@/router'
import utils from '@/utils'

const userStore = useUserStore()
const uiSettingStore = useUISettingStore()
export const API_CONTEXT_PATH = '/dolphinscheduler'

export const normalizeApiBaseUrl = (baseUrl?: string) => {
  const normalized = (baseUrl || '').trim().replace(/\/+$/, '')

  return `${normalized}${API_CONTEXT_PATH}`
}

export const apiBaseUrl =
  import.meta.env.MODE === 'development'
    ? API_CONTEXT_PATH
    : normalizeApiBaseUrl(import.meta.env.VITE_APP_PROD_WEB_URL)

/**
 * @description Log and display errors
 * @param {Error} error Error object
 */
const handleError = (res: AxiosResponse<any, any>) => {
  // Print to console
  if (import.meta.env.MODE === 'development') {
    utils.log.capsule('DolphinScheduler', 'UI')
    utils.log.error(res)
  }
  if ((res.config as any)?.suppressErrorMessage) {
    return
  }
  window.$message.error(
    res.data.msg || res.data.message || '请求失败，请稍后重试。'
  )
}

const baseRequestConfig: AxiosRequestConfig = {
  baseURL: apiBaseUrl,
  timeout: uiSettingStore.getApiTimer ? uiSettingStore.getApiTimer : 20000,
  transformRequest: (params) => {
    if (_.isPlainObject(params)) {
      return qs.stringify(params, { arrayFormat: 'repeat' })
    } else {
      return params
    }
  },
  paramsSerializer: (params) => {
    return qs.stringify(params, { arrayFormat: 'repeat' })
  }
}

const service = axios.create(baseRequestConfig)

const clearClientSession = () => {
  userStore.setSessionId('')
  userStore.setSecurityConfigType('')
  userStore.setUserInfo({})
  userStore.setBaseResDir('')
  userStore.setModulePermissions(null)
  cookies.remove('sessionId', { path: '/' })
  cookies.remove('sessionId')
}

const redirectToLogin = () => {
  if (router.currentRoute.value.name === 'login') return
  void router.push({
    path: '/login',
    query: { redirect: router.currentRoute.value.fullPath }
  })
}

const isAuthFailureCode = (code: unknown) => code === 401 || code === 504

const createAuthError = (message: string) => {
  const error = new Error(message) as Error & {
    response?: { status: number }
  }
  error.response = { status: 401 }
  return error
}

const err = (err: AxiosError): Promise<AxiosError> => {
  if (err.response?.status === 401 || err.response?.status === 504) {
    clearClientSession()
    redirectToLogin()
  }

  return Promise.reject(err)
}

service.interceptors.request.use((config: AxiosRequestConfig<any>) => {
  config.headers = config.headers || {}
  const sessionId = userStore.getSessionId || cookies.get('sessionId')
  if (sessionId) {
    config.headers.sessionId = sessionId
  }
  const language = cookies.get('language')
  if (language) config.headers.language = language

  return config
}, err)

// The response to intercept
service.interceptors.response.use((res: AxiosResponse) => {
  // No code will be processed
  if (res.data.code === undefined) {
    return res.data
  }

  if (isAuthFailureCode(res.data.code)) {
    clearClientSession()
    redirectToLogin()
    throw createAuthError(
      res.data.msg || res.data.message || '登录状态已失效，请重新登录'
    )
  }

  if (
    (res.config as any)?.acceptMagicApiSuccess &&
    res.data.code === 1 &&
    res.data.message === 'success'
  ) {
    return res.data.data
  }

  switch (res.data.code) {
    case 0:
      return res.data.data
    default:
      handleError(res)
      throw new Error(res.data.msg || res.data.message || 'Request failed')
  }
}, err)

export { service as axios }
