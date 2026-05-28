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
PID_FILE=${DOLPHINSCHEDULER_PID_FILE:-${STANDALONE_HOME}/logs/dolphinscheduler-standalone.pid}

if [ ! -f "$PID_FILE" ]; then
  echo "DolphinScheduler standalone is stopped"
  exit 3
fi

PID=$(cat "$PID_FILE")
if [[ "$PID" =~ ^[0-9]+$ ]] && kill -0 "$PID" >/dev/null 2>&1; then
  echo "DolphinScheduler standalone is running, pid=$PID"
  exit 0
fi

echo "DolphinScheduler standalone is stopped: stale pid=$PID"
exit 3
