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
import org.apache.dolphinscheduler.installer.dto.PreviewFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

class ConfigRenderServiceTest {

    private final ConfigRenderService configRenderService = new ConfigRenderService();

    @TempDir
    private Path tempDir;

    @Test
    void shouldRenderPreviewFilesAndMaskPassword() {
        InstallContext context = InstallContext.from(tempDir.resolve("standalone-server"), "token-1", 18080);
        createBaseApplicationYaml(context);
        InstallConfigRequest request = createRequest(context.getStandaloneHome());

        List<PreviewFile> files = configRenderService.renderPreview(context, request);

        assertThat(files).hasSize(3);
        assertThat(findContent(files, "application.yaml"))
                .contains("active: mysql")
                .contains("driver-class-name: com.mysql.cj.jdbc.Driver")
                .contains("base-url: http://127.0.0.1:12345/dolphinscheduler")
                .contains("master:\n  master-address: 127.0.0.1:5678")
                .contains("worker:\n  worker-address: 127.0.0.1:1234")
                .contains("registry:\n  type: jdbc")
                .contains("password: ******")
                .doesNotContain("real-password");
        assertThat(findContent(files, "common.properties")).contains("resource.storage.type=LOCAL");
        assertThat(findContent(files, "dolphinscheduler_env.sh")).contains("SEATUNNEL_HOME");
    }

    @Test
    void shouldKeepRealPasswordForWriteRendering() {
        InstallContext context = InstallContext.from(tempDir.resolve("standalone-server"), "token-1", 18080);
        createBaseApplicationYaml(context);
        InstallConfigRequest request = createRequest(context.getStandaloneHome());

        List<PreviewFile> files = configRenderService.renderForWrite(context, request);

        assertThat(findContent(files, "application.yaml")).contains("password: real-password");
    }

    @Test
    void shouldRenderJdbcRegistryConfigWhenExistingYamlDoesNotContainRegistry() {
        InstallContext context = InstallContext.from(tempDir.resolve("standalone-server"), "token-1", 18080);
        createApplicationYamlWithoutRegistry(context);
        InstallConfigRequest request = createRequest(context.getStandaloneHome());

        List<PreviewFile> files = configRenderService.renderForWrite(context, request);

        assertThat(findContent(files, "application.yaml"))
                .contains("registry:\n"
                        + "  type: jdbc\n"
                        + "  heartbeat-refresh-interval: 3s\n"
                        + "  session-timeout: 60s\n"
                        + "  hikari-config:\n"
                        + "    jdbc-url: jdbc:mysql://192.168.10.25:3306/dolphinscheduler")
                .contains("    username: ds_user\n")
                .contains("    password: real-password\n")
                .contains("    maximum-pool-size: 5\n");
    }

    @Test
    void shouldUseLoopbackForInternalRpcAddressWhenPublicHostBindsAllInterfaces() {
        InstallContext context = InstallContext.from(tempDir.resolve("standalone-server"), "token-1", 18080);
        createBaseApplicationYaml(context);
        InstallConfigRequest request = createRequest(context.getStandaloneHome());
        request.getService().setPublicHost("0.0.0.0");

        List<PreviewFile> files = configRenderService.renderForWrite(context, request);

        assertThat(findContent(files, "application.yaml"))
                .contains("base-url: http://0.0.0.0:12345/dolphinscheduler")
                .contains("master:\n  master-address: 127.0.0.1:5678")
                .contains("worker:\n  worker-address: 127.0.0.1:1234");
    }

    private void createBaseApplicationYaml(InstallContext context) {
        try {
            Files.createDirectories(context.getConfDir());
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
                            + "  port: 12345\n"
                            + "registry:\n"
                            + "  type: jdbc\n")
                            .getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void createApplicationYamlWithoutRegistry(InstallContext context) {
        try {
            Files.createDirectories(context.getConfDir());
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
                            + "  port: 12345\n"
                            + "dolphinscheduler:\n"
                            + "  api:\n"
                            + "    base-url: http://0.0.0.0:12345/dolphinscheduler\n")
                            .getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String findContent(List<PreviewFile> files, String name) {
        return files.stream()
                .filter(file -> name.equals(file.getName()))
                .findFirst()
                .map(PreviewFile::getContent)
                .orElse("");
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
        database.setInitDatabase(true);
        request.setDatabase(database);

        InstallConfigRequest.ServiceConfig service = new InstallConfigRequest.ServiceConfig();
        service.setPublicHost("127.0.0.1");
        service.setLogDir("./logs");
        service.setResourceDir("./data/resource");
        service.setTimezone("Asia/Shanghai");
        request.setService(service);

        InstallConfigRequest.SyncConfig sync = new InstallConfigRequest.SyncConfig();
        sync.setSeatunnelHome("/opt/apache-seatunnel-2.3.3");
        sync.setTmpDir("./data/sync/tmp");
        sync.setLogDir("./logs/sync-task");
        sync.setJdbcDir("./libs");
        request.setSync(sync);
        return request;
    }
}
