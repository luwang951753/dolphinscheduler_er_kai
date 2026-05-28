/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.dolphinscheduler.installer.dto.CheckItem;
import org.apache.dolphinscheduler.installer.dto.EnvironmentCheckRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class EnvironmentCheckServiceTest {

    private final StandaloneHomeValidator standaloneHomeValidator = new StandaloneHomeValidator();

    private final EnvironmentCheckService environmentCheckService = new EnvironmentCheckService(standaloneHomeValidator);

    @TempDir
    private Path tempDir;

    @Test
    void shouldFailWhenDolphinPortIsOccupied() throws IOException {
        try (ServerSocket occupiedSocket = new ServerSocket(0)) {
            EnvironmentCheckRequest request = createValidRequest(occupiedSocket.getLocalPort());

            List<CheckItem> items = environmentCheckService.check(request);

            assertThat(items)
                    .filteredOn(item -> "DOLPHIN_PORT".equals(item.getKey()))
                    .extracting(CheckItem::getStatus)
                    .containsExactly(CheckItem.STATUS_FAIL);
        }
    }

    @Test
    void shouldFailWhenInstallLockExists() throws IOException {
        EnvironmentCheckRequest request = createValidRequest(0);
        Files.write(tempDir.resolve("standalone-server").resolve("install.lock"), new byte[0]);

        List<CheckItem> items = environmentCheckService.check(request);

        assertThat(items)
                .filteredOn(item -> "INSTALL_LOCK".equals(item.getKey()))
                .extracting(CheckItem::getStatus)
                .containsExactly(CheckItem.STATUS_FAIL);
    }

    @Test
    void shouldFailWhenInstallDirIsEmptyAndNotCreateManagedDirectories() throws IOException {
        Path javaHome = tempDir.resolve("jdk1.8.0_371");
        Path installDir = tempDir.resolve("empty-target");
        Files.createDirectories(javaHome);
        Files.createDirectories(installDir);

        EnvironmentCheckRequest request = new EnvironmentCheckRequest();
        request.setJavaHome(javaHome.toString());
        request.setInstallDir(installDir.toString());
        request.setDolphinPort(findAvailablePort());

        List<CheckItem> items = environmentCheckService.check(request);

        assertThat(items)
                .filteredOn(item -> "STANDALONE_HOME".equals(item.getKey()))
                .extracting(CheckItem::getStatus)
                .containsExactly(CheckItem.STATUS_FAIL);
        assertThat(installDir.resolve("conf")).doesNotExist();
        assertThat(installDir.resolve("logs")).doesNotExist();
        assertThat(installDir.resolve("backup")).doesNotExist();
    }

    @Test
    void shouldPassWhenInstallDirIsStandaloneHome() throws IOException {
        EnvironmentCheckRequest request = createValidRequest(0);

        List<CheckItem> items = environmentCheckService.check(request);

        assertThat(items)
                .filteredOn(item -> "STANDALONE_HOME".equals(item.getKey()))
                .extracting(CheckItem::getStatus)
                .containsExactly(CheckItem.STATUS_PASS);
    }

    @Test
    void shouldFailWhenUiFilesAreEmpty() throws IOException {
        EnvironmentCheckRequest request = createValidRequest(0);
        Path installDir = tempDir.resolve("standalone-server");
        Files.write(installDir.resolve("ui").resolve("index.html"), new byte[0]);

        List<CheckItem> items = environmentCheckService.check(request);

        assertThat(items)
                .filteredOn(item -> "STANDALONE_HOME".equals(item.getKey()))
                .extracting(CheckItem::getStatus)
                .containsExactly(CheckItem.STATUS_FAIL);
    }

    private EnvironmentCheckRequest createValidRequest(int dolphinPort) throws IOException {
        Path javaHome = tempDir.resolve("jdk1.8.0_371");
        Path installDir = tempDir.resolve("standalone-server");
        Files.createDirectories(javaHome);
        createStandaloneHome(installDir);

        EnvironmentCheckRequest request = new EnvironmentCheckRequest();
        request.setJavaHome(javaHome.toString());
        request.setInstallDir(installDir.toString());
        request.setDolphinPort(dolphinPort == 0 ? findAvailablePort() : dolphinPort);
        return request;
    }

    private void createStandaloneHome(Path installDir) throws IOException {
        Files.createDirectories(installDir.resolve("bin"));
        Files.createDirectories(installDir.resolve("installer"));
        Files.createDirectories(installDir.resolve("conf"));
        Files.createDirectories(installDir.resolve("ui").resolve("assets"));
        Files.createDirectories(installDir.resolve("libs"));
        Files.createDirectories(installDir.resolve("api-server").resolve("libs"));
        Files.createDirectories(installDir.resolve("master-server").resolve("libs"));
        Files.createDirectories(installDir.resolve("worker-server").resolve("libs"));
        Files.createDirectories(installDir.resolve("alert-server").resolve("libs"));
        Files.createDirectories(installDir.resolve("plugins"));
        Files.write(installDir.resolve("bin").resolve("start.sh"), new byte[0]);
        Files.write(installDir.resolve("bin").resolve("stop.sh"), new byte[0]);
        Files.write(installDir.resolve("bin").resolve("status.sh"), new byte[0]);
        Files.write(installDir.resolve("bin").resolve("install-web.sh"), new byte[0]);
        Files.write(installDir.resolve("installer").resolve("ds-offline-installer.jar"), new byte[0]);
        Files.write(installDir.resolve("conf").resolve("application.yaml"), new byte[0]);
        Files.write(installDir.resolve("conf").resolve("common.properties"), new byte[0]);
        Files.write(installDir.resolve("conf").resolve("dolphinscheduler_env.sh"), new byte[0]);
        Files.write(installDir.resolve("ui").resolve("index.html"),
                "<div id=\"app\"></div>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Files.write(installDir.resolve("ui").resolve("lodash.min.js"),
                "window._ = {};".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
