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
STANDALONE_HOME=$(cd ${BIN_DIR}/..;pwd)
DOLPHINSCHEDULER_HOME=${DOLPHINSCHEDULER_HOME:-${STANDALONE_HOME}}
PID_FILE=${DOLPHINSCHEDULER_PID_FILE:-${STANDALONE_HOME}/logs/dolphinscheduler-standalone.pid}
STARTUP_LOG=${DOLPHINSCHEDULER_STARTUP_LOG:-${STANDALONE_HOME}/logs/dolphinscheduler-startup.log}

export DATABASE=${DATABASE:-h2}
source "$STANDALONE_HOME/conf/dolphinscheduler_env.sh"

JVM_ARGS_ENV_FILE=${STANDALONE_HOME}/bin/jvm_args_env.sh
JVM_ARGS="-server"

if [ -f $JVM_ARGS_ENV_FILE ]; then
  while read line
  do
      if [[ "$line" == -* ]]; then
            JVM_ARGS="${JVM_ARGS} $line"
      fi
  done < $JVM_ARGS_ENV_FILE
fi

JAVA_OPTS=${JAVA_OPTS:-"${JVM_ARGS}"}

if [[ "$DOCKER" == "true" ]]; then
  JAVA_OPTS="${JAVA_OPTS} -XX:-UseContainerSupport -DDOCKER=true"
fi

echo "JAVA_HOME=${JAVA_HOME}"
echo "JAVA_OPTS=${JAVA_OPTS}"

MODULES_PATH=(
api-server
master-server
worker-server
alert-server
)

CP=""
for module in ${MODULES_PATH[@]}; do
  CP=$CP:"$DOLPHINSCHEDULER_HOME/$module/libs/*"
done

PLUGINS_PATH=(
alert-plugins
datasource-plugins
registry-plugins
storage-plugins
task-plugins
)

for plugin in ${PLUGINS_PATH[@]}; do
  if [ -d "$DOLPHINSCHEDULER_HOME/plugins/$plugin" ]; then
    CP=$CP:"$DOLPHINSCHEDULER_HOME/plugins/$plugin/*"
  fi
done

if [ -d "$STANDALONE_HOME/libs" ]; then
  for jar in "$STANDALONE_HOME"/libs/*.jar; do
    if [ -f "$jar" ]; then
      CP=$CP:"$jar"
    fi
  done
fi

JAVA_CMD=("$JAVA_HOME/bin/java" $JAVA_OPTS \
  -cp "$STANDALONE_HOME/conf""$CP" \
  org.apache.dolphinscheduler.StandaloneServer)

if [[ "${1:-}" == "console" || "${DOLPHINSCHEDULER_FOREGROUND:-false}" == "true" ]]; then
  exec "${JAVA_CMD[@]}"
fi

mkdir -p "$(dirname "$PID_FILE")" "$(dirname "$STARTUP_LOG")"

if [ -f "$PID_FILE" ]; then
  OLD_PID=$(cat "$PID_FILE")
  if [[ "$OLD_PID" =~ ^[0-9]+$ ]] && kill -0 "$OLD_PID" >/dev/null 2>&1; then
    echo "DolphinScheduler standalone is already running, pid=$OLD_PID"
    exit 0
  fi
  rm -f "$PID_FILE"
fi

nohup "${JAVA_CMD[@]}" >> "$STARTUP_LOG" 2>&1 &
PID=$!
echo "$PID" > "$PID_FILE"
echo "DolphinScheduler standalone started, pid=$PID, log=$STARTUP_LOG"
