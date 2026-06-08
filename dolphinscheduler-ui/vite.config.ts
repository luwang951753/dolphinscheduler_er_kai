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

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import viteCompression from 'vite-plugin-compression'
import path from 'path'

const env = loadEnv('development', './')
const devWebUrl = process.env.VITE_APP_DEV_WEB_URL || env.VITE_APP_DEV_WEB_URL
const stripContextPath =
  process.env.VITE_APP_DEV_STRIP_CONTEXT_PATH ||
  env.VITE_APP_DEV_STRIP_CONTEXT_PATH

export default defineConfig(({ mode }) => ({
  base: mode === 'production' ? '/dolphinscheduler/ui/' : '/',
  plugins: [
    vue(),
    vueJsx(),
    viteCompression({
      verbose: true,
      disable: false,
      threshold: 10240,
      algorithm: 'gzip',
      ext: '.gz',
      deleteOriginFile: false
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
      // resolve vue-i18n warning: You are running the esm-bundler build of vue-i18n.
    }
  },
  server: {
    proxy: {
      '/dolphinscheduler': {
        target: devWebUrl,
        changeOrigin: true,
        rewrite:
          stripContextPath === 'true'
            ? (path) => path.replace(/^\/dolphinscheduler/, '')
            : undefined
      }
    }
  }
}))
