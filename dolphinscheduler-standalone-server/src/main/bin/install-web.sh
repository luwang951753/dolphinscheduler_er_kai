#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
set -eo pipefail

BIN_DIR=$(cd "$(dirname "$0")"; pwd)
STANDALONE_HOME=$(cd "${BIN_DIR}/.."; pwd)
INSTALLER_JAR="${STANDALONE_HOME}/installer/ds-offline-installer.jar"
INSTALLER_PORT="${INSTALLER_PORT:-18080}"
INSTALLER_TOKEN="${INSTALLER_TOKEN:-$(date +%s)-$(od -An -N8 -tx1 /dev/urandom | tr -d ' \n')}"
INSTALLER_LOG_DIR="${STANDALONE_HOME}/logs"
INSTALLER_LOG_FILE="${INSTALLER_LOG_DIR}/offline-installer.log"

if [ ! -f "${INSTALLER_JAR}" ]; then
  echo "安装器 jar 不存在: ${INSTALLER_JAR}"
  exit 1
fi

if command -v lsof >/dev/null 2>&1; then
  if lsof -iTCP:"${INSTALLER_PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "安装器端口 ${INSTALLER_PORT} 已被占用，请先释放端口或指定 INSTALLER_PORT=其他端口。"
    exit 1
  fi
fi

mkdir -p "${INSTALLER_LOG_DIR}"

if [ -f "${STANDALONE_HOME}/conf/dolphinscheduler_env.sh" ]; then
  # 只读取 JAVA_HOME 等基础环境变量，不在安装向导页面展示安装器端口。
  # shellcheck source=/dev/null
  source "${STANDALONE_HOME}/conf/dolphinscheduler_env.sh"
fi

JAVA_BIN="${JAVA_HOME:-}/bin/java"
if [ ! -x "${JAVA_BIN}" ]; then
  JAVA_BIN="java"
fi

if [ -n "${INSTALLER_HOST:-}" ]; then
  HOST_IP="${INSTALLER_HOST}"
else
  # Linux 常见环境支持 hostname -I；macOS 和部分精简 Linux 不支持时继续降级。
  HOST_IP="$(hostname -I 2>/dev/null | awk '{print $1}' || true)"
  if [ -z "${HOST_IP}" ] && command -v ipconfig >/dev/null 2>&1; then
    HOST_IP="$(ipconfig getifaddr en0 2>/dev/null || true)"
  fi
  if [ -z "${HOST_IP}" ]; then
    HOST_IP="127.0.0.1"
  fi
fi

{
  echo "DolphinScheduler 离线安装向导已启动："
  echo "  本机访问: http://127.0.0.1:${INSTALLER_PORT}/install?token=${INSTALLER_TOKEN}"
  echo "  内网访问: http://${HOST_IP}:${INSTALLER_PORT}/install?token=${INSTALLER_TOKEN}"
  echo "  安装器日志: ${INSTALLER_LOG_FILE}"
  echo ""
  echo "说明：INSTALLER_PORT 只是安装向导临时端口，不会写入 DolphinScheduler 正式服务配置。"

  "${JAVA_BIN}" \
    -jar "${INSTALLER_JAR}" \
    --server.port="${INSTALLER_PORT}" \
    --installer.token="${INSTALLER_TOKEN}" \
    --installer.standalone-home="${STANDALONE_HOME}"
} 2>&1 | tee -a "${INSTALLER_LOG_FILE}"

exit "${PIPESTATUS[0]}"
