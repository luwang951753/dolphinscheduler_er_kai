/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.dolphinscheduler.installer.service;

import org.apache.dolphinscheduler.installer.dto.CheckItem;
import org.apache.dolphinscheduler.installer.dto.EnvironmentCheckRequest;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class EnvironmentCheckService {

    private final StandaloneHomeValidator standaloneHomeValidator;

    public EnvironmentCheckService(StandaloneHomeValidator standaloneHomeValidator) {
        this.standaloneHomeValidator = standaloneHomeValidator;
    }

    public List<CheckItem> check(EnvironmentCheckRequest request) {
        List<CheckItem> items = new ArrayList<>();
        items.add(checkJdk(request));
        items.add(standaloneHomeValidator.check(request == null ? null : request.getInstallDir()));
        items.add(checkDolphinPort(request));
        items.add(checkServicePort("MASTER_RPC_PORT", "Master RPC 端口", 5678,
                "已被占用，请停止已有 DolphinScheduler standalone 或调整 Master RPC 端口"));
        items.add(checkServicePort("WORKER_RPC_PORT", "Worker RPC 端口", 1234,
                "已被占用，请停止已有 DolphinScheduler standalone 或调整 Worker RPC 端口"));
        items.add(checkWritableDirectories(request));
        items.add(checkInstallLock(request));
        return items;
    }

    private CheckItem checkJdk(EnvironmentCheckRequest request) {
        if (request == null || !StringUtils.hasText(request.getJavaHome())) {
            return CheckItem.fail("JDK_VERSION", "JDK 版本", "请填写 JDK 1.8 的 JAVA_HOME");
        }
        Path javaHome = Paths.get(request.getJavaHome());
        if (!Files.exists(javaHome)) {
            return CheckItem.fail("JDK_VERSION", "JDK 版本", "JAVA_HOME 不存在: " + request.getJavaHome());
        }
        if (request.getJavaHome().contains("1.8") || request.getJavaHome().contains("8u")) {
            return CheckItem.pass("JDK_VERSION", "JDK 版本", "检测到 Java 1.8");
        }
        return CheckItem.fail("JDK_VERSION", "JDK 版本", "当前路径不是 JDK 1.8: " + request.getJavaHome());
    }

    private CheckItem checkDolphinPort(EnvironmentCheckRequest request) {
        int port = request == null ? 0 : request.getDolphinPort();
        if (port <= 0 || port > 65535) {
            return CheckItem.fail("DOLPHIN_PORT", "Dolphin 服务端口", "端口必须在 1-65535 之间");
        }
        return checkServicePort("DOLPHIN_PORT", "Dolphin 服务端口", port, "已被占用，请更换 Dolphin 服务端口");
    }

    private CheckItem checkServicePort(String key, String name, int port, String occupiedMessage) {
        if (port <= 0 || port > 65535) {
            return CheckItem.fail(key, name, "端口必须在 1-65535 之间");
        }
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return CheckItem.pass(key, name, port + " 当前未被占用");
        } catch (IOException ex) {
            return CheckItem.fail(key, name, port + " " + occupiedMessage);
        }
    }

    private CheckItem checkWritableDirectories(EnvironmentCheckRequest request) {
        if (request == null || !StringUtils.hasText(request.getInstallDir())) {
            return CheckItem.fail("CONF_PERMISSION", "配置目录权限", "请填写 standalone 安装目录");
        }
        CheckItem standaloneHomeCheck = standaloneHomeValidator.check(request.getInstallDir());
        if (CheckItem.STATUS_FAIL.equals(standaloneHomeCheck.getStatus())) {
            return CheckItem.fail("CONF_PERMISSION", "配置目录权限", "请先选择正确的 standalone-server 根目录");
        }
        try {
            Path installDir = Paths.get(request.getInstallDir());
            assertWritableDirectory(installDir.resolve("conf"));
            assertWritableDirectory(installDir.resolve("logs"));
            assertWritableDirectory(installDir.resolve("backup"));
            return CheckItem.pass("CONF_PERMISSION", "配置目录权限", "conf、logs、backup 可写");
        } catch (IOException ex) {
            return CheckItem.fail("CONF_PERMISSION", "配置目录权限", "目录不可写: " + ex.getMessage());
        }
    }

    private void assertWritableDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path marker = Files.createTempFile(directory, ".installer-write-check-", ".tmp");
        Files.deleteIfExists(marker);
    }

    private CheckItem checkInstallLock(EnvironmentCheckRequest request) {
        if (request == null || !StringUtils.hasText(request.getInstallDir())) {
            return CheckItem.fail("INSTALL_LOCK", "安装锁", "请填写 standalone 安装目录");
        }
        Path installLock = Paths.get(request.getInstallDir()).resolve("install.lock");
        if (Files.exists(installLock)) {
            return CheckItem.fail("INSTALL_LOCK", "安装锁", "检测到 install.lock，当前目录已完成安装");
        }
        return CheckItem.pass("INSTALL_LOCK", "安装锁", "未检测到 install.lock");
    }
}
