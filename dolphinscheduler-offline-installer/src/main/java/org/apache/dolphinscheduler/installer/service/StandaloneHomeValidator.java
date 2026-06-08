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

package org.apache.dolphinscheduler.installer.service;

import org.apache.dolphinscheduler.installer.dto.CheckItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 校验用户填写的安装目录是否为解压后的 standalone-server 根目录，避免把空目录误当成安装目标。
 */
@Service
public class StandaloneHomeValidator {

    private static final List<String> REQUIRED_FILES = Arrays.asList(
            "bin/start.sh",
            "bin/stop.sh",
            "bin/status.sh",
            "bin/install-web.sh",
            "installer/ds-offline-installer.jar",
            "conf/application.yaml",
            "conf/common.properties",
            "conf/dolphinscheduler_env.sh",
            "ui/index.html",
            "ui/lodash.min.js");

    private static final List<String> REQUIRED_DIRECTORIES = Arrays.asList(
            "conf",
            "ui",
            "ui/assets",
            "libs",
            "installer",
            "api-server/libs",
            "master-server/libs",
            "worker-server/libs",
            "alert-server/libs",
            "plugins");

    public CheckItem check(String installDir) {
        if (!StringUtils.hasText(installDir)) {
            return CheckItem.fail("STANDALONE_HOME", "Standalone 安装目录", "请填写解压后的 standalone-server 根目录");
        }

        Path standaloneHome = Paths.get(installDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(standaloneHome)) {
            return CheckItem.fail("STANDALONE_HOME", "Standalone 安装目录",
                    "安装目录不存在或不是目录: " + standaloneHome);
        }

        List<String> missing = findMissingRequiredPaths(standaloneHome);
        if (!missing.isEmpty()) {
            return CheckItem.fail("STANDALONE_HOME", "Standalone 安装目录",
                    "安装目录必须选择解压后的 standalone-server 根目录，缺少: " + String.join("、", missing));
        }

        return CheckItem.pass("STANDALONE_HOME", "Standalone 安装目录",
                "已识别 standalone-server 根目录: " + standaloneHome);
    }

    public void validateOrThrow(Path standaloneHome) throws IOException {
        CheckItem item = check(standaloneHome == null ? null : standaloneHome.toString());
        if (CheckItem.STATUS_FAIL.equals(item.getStatus())) {
            throw new IOException(item.getMessage());
        }
    }

    private List<String> findMissingRequiredPaths(Path standaloneHome) {
        List<String> missing = new ArrayList<>();
        for (String directory : REQUIRED_DIRECTORIES) {
            if (!Files.isDirectory(standaloneHome.resolve(directory))) {
                missing.add(directory + "/");
            }
        }
        for (String file : REQUIRED_FILES) {
            Path requiredFile = standaloneHome.resolve(file);
            if (!Files.isRegularFile(requiredFile)) {
                missing.add(file);
                continue;
            }
            if (file.startsWith("ui/")) {
                try {
                    if (Files.size(requiredFile) == 0) {
                        missing.add(file + "(空文件)");
                    }
                } catch (IOException ex) {
                    missing.add(file + "(无法读取)");
                }
            }
        }
        return missing;
    }
}
