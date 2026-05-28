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

import org.apache.dolphinscheduler.installer.core.InstallContext;
import org.apache.dolphinscheduler.installer.dto.InstallConfigRequest;
import org.apache.dolphinscheduler.installer.dto.InstallProgress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class InstallServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldInstallSuccessfully() throws Exception {
        InstallContext context = createStandaloneHome("echo started > ../logs/start.marker\nexit 0\n");
        InstallProgressService progressService = new InstallProgressService();
        InstallService installService = createInstallService(progressService,
                new DolphinProcessService.ProcessResult(true, "started"));

        String installId = installService.install(context, createRequest(context.getStandaloneHome()));

        assertThat(context.getBackupDir()).exists();
        assertThat(new String(Files.readAllBytes(context.getConfDir().resolve("application.yaml")), StandardCharsets.UTF_8))
                .contains("password: real-password");
        assertThat(context.getInstallLock()).hasContent(installId);
        assertThat(progressService.get(installId).getStatus()).isEqualTo(InstallProgress.STATUS_SUCCESS);
        assertThat(progressService.get(installId).getItems())
                .extracting(InstallProgress.ProgressItem::getKey)
                .contains("BACKUP_CONFIG", "WRITE_CONFIG", "START_DOLPHIN", "WRITE_LOCK", "SUCCESS");
    }

    @Test
    void shouldMarkFailedWhenStartScriptFails() throws Exception {
        InstallContext context = createStandaloneHome("echo failed\nexit 1\n");
        InstallProgressService progressService = new InstallProgressService();
        InstallService installService = createInstallService(progressService,
                new DolphinProcessService.ProcessResult(false, "failed"));

        String installId = installService.install(context, createRequest(context.getStandaloneHome()));

        assertThat(context.getInstallLock()).doesNotExist();
        assertThat(progressService.get(installId).getStatus()).isEqualTo(InstallProgress.STATUS_FAILED);
        assertThat(progressService.get(installId).getCurrentStep()).isEqualTo("START_DOLPHIN");
    }

    private InstallService createInstallService(InstallProgressService progressService,
                                                DolphinProcessService.ProcessResult processResult) {
        return new InstallService(
                new ConfigBackupService(),
                new ConfigWriteService(),
                new ConfigRenderService(),
                new DolphinProcessService() {
                    @Override
                    public ProcessResult start(InstallContext context) {
                        return processResult;
                    }
                },
                new DatabaseInitService(),
                progressService,
                new StandaloneHomeValidator(),
                new UiPathService());
    }

    private InstallContext createStandaloneHome(String startScriptContent) throws Exception {
        InstallContext context = InstallContext.from(tempDir.resolve("standalone-server"), "token-1", 18080);
        Files.createDirectories(context.getConfDir());
        Files.createDirectories(context.getStandaloneHome().resolve("logs"));
        Files.createDirectories(context.getStandaloneHome().resolve("bin"));
        Files.createDirectories(context.getStandaloneHome().resolve("installer"));
        Files.createDirectories(context.getStandaloneHome().resolve("ui").resolve("assets"));
        Files.createDirectories(context.getStandaloneHome().resolve("libs"));
        Files.createDirectories(context.getStandaloneHome().resolve("api-server").resolve("libs"));
        Files.createDirectories(context.getStandaloneHome().resolve("master-server").resolve("libs"));
        Files.createDirectories(context.getStandaloneHome().resolve("worker-server").resolve("libs"));
        Files.createDirectories(context.getStandaloneHome().resolve("alert-server").resolve("libs"));
        Files.createDirectories(context.getStandaloneHome().resolve("plugins"));
        Files.write(context.getConfDir().resolve("application.yaml"),
                ("spring:\n"
                        + "  profiles:\n"
                        + "    active: h2\n"
                        + "  datasource:\n"
                        + "    driver-class-name: org.h2.Driver\n"
                        + "    url: jdbc:h2:mem:dolphinscheduler\n"
                        + "    username: sa\n"
                        + "    password: \"\"\n"
                        + "server:\n"
                        + "  port: " + findAvailablePort() + "\n"
                        + "registry:\n"
                        + "  type: jdbc\n").getBytes(StandardCharsets.UTF_8));
        Files.write(context.getConfDir().resolve("common.properties"), "old-common".getBytes(StandardCharsets.UTF_8));
        Files.write(context.getConfDir().resolve("dolphinscheduler_env.sh"), "old-env".getBytes(StandardCharsets.UTF_8));
        Files.write(context.getStandaloneHome().resolve("bin").resolve("install-web.sh"), new byte[0]);
        Files.write(context.getStandaloneHome().resolve("bin").resolve("stop.sh"), new byte[0]);
        Files.write(context.getStandaloneHome().resolve("bin").resolve("status.sh"), new byte[0]);
        Files.write(context.getStandaloneHome().resolve("installer").resolve("ds-offline-installer.jar"), new byte[0]);
        Files.write(context.getStandaloneHome().resolve("ui").resolve("index.html"),
                ("<div id=\"app\"></div>"
                        + "<script type=\"module\" src=\"/dolphinscheduler/ui/assets/index.js\"></script>")
                                .getBytes(StandardCharsets.UTF_8));
        Files.write(context.getStandaloneHome().resolve("ui").resolve("lodash.min.js"),
                "window._ = {};".getBytes(StandardCharsets.UTF_8));

        Path startScript = context.getStandaloneHome().resolve("bin").resolve("start.sh");
        Files.write(startScript, ("#!/bin/bash\n" + startScriptContent).getBytes(StandardCharsets.UTF_8));
        startScript.toFile().setExecutable(true);
        return context;
    }

    private InstallConfigRequest createRequest(Path standaloneHome) {
        InstallConfigRequest request = new InstallConfigRequest();
        request.setJavaHome("/usr/local/jdk1.8.0_371");
        request.setInstallDir(standaloneHome.toString());
        request.setDolphinPort(12345);

        InstallConfigRequest.Database database = new InstallConfigRequest.Database();
        database.setType("MYSQL");
        database.setHost("192.168.10.25");
        database.setPort(3306);
        database.setDatabase("dolphinscheduler");
        database.setUsername("ds_user");
        database.setPassword("real-password");
        database.setInitDatabase(false);
        request.setDatabase(database);

        InstallConfigRequest.ServiceConfig service = new InstallConfigRequest.ServiceConfig();
        service.setPublicHost("192.168.10.80");
        service.setResourceDir("./data/resource");
        request.setService(service);

        InstallConfigRequest.SyncConfig sync = new InstallConfigRequest.SyncConfig();
        sync.setSeatunnelHome("/opt/apache-seatunnel-2.3.3");
        sync.setTmpDir("./data/sync/tmp");
        sync.setLogDir("./logs/sync-task");
        sync.setJdbcDir("./libs");
        request.setSync(sync);
        return request;
    }

    private int findAvailablePort() throws Exception {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
