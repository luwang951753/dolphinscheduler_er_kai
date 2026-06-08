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

import { defineComponent, nextTick, ref } from 'vue'
import cookies from 'js-cookie'
import { NButton } from 'naive-ui'
import { useUserStore } from '@/store/user/user'
import styles from './index.module.scss'

const magicApiUrl = '/dolphinscheduler/magic/web/index.html'

export default defineComponent({
  name: 'magic-api',
  setup() {
    const userStore = useUserStore()
    const ready = ref(false)
    const sessionId = userStore.getSessionId || cookies.get('sessionId')

    if (sessionId) {
      cookies.set('sessionId', sessionId, { path: '/' })
    }
    nextTick(() => {
      ready.value = true
    })

    return () => (
      <div class={styles.page}>
        <NButton
          class={styles.openButton}
          size='small'
          secondary
          onClick={() => window.open(magicApiUrl, '_blank')}
        >
          新窗口打开
        </NButton>
        {ready.value ? (
          <iframe
            class={styles.frame}
            src={magicApiUrl}
            title='magic-api'
          />
        ) : null}
      </div>
    )
  }
})
