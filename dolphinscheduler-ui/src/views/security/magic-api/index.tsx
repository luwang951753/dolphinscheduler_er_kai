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

import { defineComponent, ref } from 'vue'
import { NButton, NSpace } from 'naive-ui'
import styles from './index.module.scss'

const magicApiUrl = '/dolphinscheduler/magic/web/index.html'

export default defineComponent({
  name: 'magic-api',
  setup() {
    const loaded = ref(false)

    const loadEditor = () => {
      loaded.value = true
    }

    return () => (
      <div class={styles.page}>
        {loaded.value ? (
          <NButton
            class={styles.openButton}
            size='small'
            secondary
            onClick={() => window.open(magicApiUrl, '_blank')}
          >
            新窗口打开
          </NButton>
        ) : null}
        {loaded.value ? (
          <iframe
            class={styles.frame}
            src={magicApiUrl}
            title='magic-api'
          />
        ) : (
          <div class={styles.entry}>
            <div class={styles.entryPanel}>
              <div class={styles.eyebrow}>Magic API</div>
              <h2>接口开发</h2>
              <p>
                Magic API 编辑器会加载完整 IDE 资源。进入菜单时先展示轻量入口，避免拖慢安全中心页面。
              </p>
              <NSpace>
                <NButton type='primary' onClick={loadEditor}>
                  在当前页加载
                </NButton>
                <NButton secondary onClick={() => window.open(magicApiUrl, '_blank')}>
                  新窗口打开
                </NButton>
              </NSpace>
            </div>
          </div>
        )}
      </div>
    )
  }
})
