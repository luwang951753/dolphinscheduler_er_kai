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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.dolphinscheduler.installer.core.InstallContext;
import org.apache.dolphinscheduler.installer.dto.PreviewFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigWriteServiceTest {

    private final ConfigBackupService configBackupService = new ConfigBackupService();

    private final ConfigWriteService configWriteService = new ConfigWriteService();

    @TempDir
    private Path tempDir;

    @Test
    void shouldRejectFileOutsideConfWhitelist() throws Exception {
        InstallContext context = createContextWithFiles();
        List<PreviewFile> files = new ArrayList<>();
        files.add(new PreviewFile("evil.yaml", context.getConfDir().resolve("../evil.yaml").toString(), "HIGH", 1,
                "evil=true"));

        assertThatThrownBy(() -> configWriteService.writeFiles(context, files))
                .hasMessageContaining("非法配置文件路径");
    }

    @Test
    void shouldBackupManagedConfigFiles() throws Exception {
        InstallContext context = createContextWithFiles();

        String backupId = configBackupService.backup(context);

        Path backupDir = context.getBackupDir().resolve(backupId);
        assertThat(backupDir.resolve("application.yaml")).hasContent("old-application");
        assertThat(backupDir.resolve("common.properties")).hasContent("old-common");
        assertThat(backupDir.resolve("dolphinscheduler_env.sh")).hasContent("old-env");
    }

    @Test
    void shouldWriteFilesAtomicallyAndKeepPermission() throws Exception {
        InstallContext context = createContextWithFiles();
        Path envFile = context.getConfDir().resolve("dolphinscheduler_env.sh");
        Set<PosixFilePermission> oldPermissions = trySetExecutablePermission(envFile);

        configWriteService.writeFiles(context, createPreviewFiles(context, "new"));

        assertThat(context.getConfDir().resolve("application.yaml")).hasContent("new-application.yaml");
        assertThat(context.getConfDir().resolve("common.properties")).hasContent("new-common.properties");
        assertThat(envFile).hasContent("new-dolphinscheduler_env.sh");
        if (oldPermissions != null) {
            assertThat(Files.getPosixFilePermissions(envFile)).isEqualTo(oldPermissions);
        }
    }

    @Test
    void shouldSyncManagedConfigToServerConfAndDaemonEnv() throws Exception {
        InstallContext context = createContextWithFiles();
        createServerDirectories(context);

        configWriteService.writeFiles(context, createPreviewFiles(context, "new"));

        for (String serverName : new String[]{"api-server", "master-server", "worker-server", "alert-server"}) {
            Path serverConfDir = context.getStandaloneHome().resolve(serverName).resolve("conf");
            assertThat(serverConfDir.resolve("application.yaml")).hasContent("new-application.yaml");
            assertThat(serverConfDir.resolve("common.properties")).hasContent("new-common.properties");
            assertThat(serverConfDir.resolve("dolphinscheduler_env.sh")).hasContent("new-dolphinscheduler_env.sh");
        }
        assertThat(context.getStandaloneHome().resolve("bin/env/dolphinscheduler_env.sh"))
                .hasContent("new-dolphinscheduler_env.sh");
    }

    @Test
    void shouldRollbackFromBackup() throws Exception {
        InstallContext context = createContextWithFiles();
        String backupId = configBackupService.backup(context);
        configWriteService.writeFiles(context, createPreviewFiles(context, "new"));

        configWriteService.rollback(context, backupId);

        assertThat(context.getConfDir().resolve("application.yaml")).hasContent("old-application");
        assertThat(context.getConfDir().resolve("common.properties")).hasContent("old-common");
        assertThat(context.getConfDir().resolve("dolphinscheduler_env.sh")).hasContent("old-env");
    }

    private InstallContext createContextWithFiles() throws Exception {
        InstallContext context = InstallContext.from(tempDir.resolve("standalone-server"), "token-1", 18080);
        Files.createDirectories(context.getConfDir());
        write(context.getConfDir().resolve("application.yaml"), "old-application");
        write(context.getConfDir().resolve("common.properties"), "old-common");
        write(context.getConfDir().resolve("dolphinscheduler_env.sh"), "old-env");
        return context;
    }

    private void createServerDirectories(InstallContext context) throws Exception {
        for (String serverName : new String[]{"api-server", "master-server", "worker-server", "alert-server"}) {
            Files.createDirectories(context.getStandaloneHome().resolve(serverName).resolve("conf"));
        }
        Files.createDirectories(context.getStandaloneHome().resolve("bin"));
    }

    private List<PreviewFile> createPreviewFiles(InstallContext context, String prefix) {
        List<PreviewFile> files = new ArrayList<>();
        for (String fileName : ConfigWriteService.MANAGED_FILE_NAMES) {
            files.add(new PreviewFile(fileName, context.getConfDir().resolve(fileName).toString(), "MEDIUM", 1,
                    prefix + "-" + fileName));
        }
        return files;
    }

    private void write(Path path, String content) throws Exception {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private Set<PosixFilePermission> trySetExecutablePermission(Path path) throws Exception {
        try {
            Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(path, permissions);
            return permissions;
        } catch (UnsupportedOperationException ignored) {
            return null;
        }
    }
}
