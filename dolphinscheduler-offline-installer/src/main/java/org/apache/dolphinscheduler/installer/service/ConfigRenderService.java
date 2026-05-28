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

import org.apache.dolphinscheduler.installer.core.InstallContext;
import org.apache.dolphinscheduler.installer.dto.InstallConfigRequest;
import org.apache.dolphinscheduler.installer.dto.PreviewFile;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

@Service
public class ConfigRenderService {

    private static final String MASKED_PASSWORD = "******";

    public List<PreviewFile> renderPreview(InstallContext context, InstallConfigRequest request) {
        return render(context, request, true);
    }

    public List<PreviewFile> renderForWrite(InstallContext context, InstallConfigRequest request) {
        return render(context, request, false);
    }

    private List<PreviewFile> render(InstallContext context, InstallConfigRequest request, boolean maskPassword) {
        List<PreviewFile> files = new ArrayList<>();
        files.add(new PreviewFile("application.yaml",
                context.getConfDir().resolve("application.yaml").toString(),
                "HIGH",
                7,
                renderApplicationYaml(context, request, maskPassword)));
        files.add(new PreviewFile("common.properties",
                context.getConfDir().resolve("common.properties").toString(),
                "MEDIUM",
                6,
                renderCommonProperties(request)));
        files.add(new PreviewFile("dolphinscheduler_env.sh",
                context.getConfDir().resolve("dolphinscheduler_env.sh").toString(),
                "MEDIUM",
                4,
                renderDolphinSchedulerEnv(request)));
        return files;
    }

    private String renderApplicationYaml(InstallContext context, InstallConfigRequest request, boolean maskPassword) {
        InstallConfigRequest.Database database = request.getDatabase();
        String password = maskPassword ? MASKED_PASSWORD : database.getPassword();
        String jdbcUrl = "jdbc:mysql://" + database.getHost() + ":" + database.getPort() + "/" + database.getDatabase()
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        String existing = readExistingApplicationYaml(context);
        if (existing == null) {
            return renderFallbackApplicationYaml(request, password, jdbcUrl);
        }

        String content = existing;
        content = replaceFirst(content, "(?m)^    active: .*$", "    active: mysql");
        content = replaceFirst(content, "(?m)^    driver-class-name: .*$", "    driver-class-name: com.mysql.cj.jdbc.Driver");
        content = replaceFirst(content, "(?m)^    url: jdbc:[^\\n]*$", "    url: " + jdbcUrl);
        content = replaceFirst(content, "(?m)^    username: .*$", "    username: " + database.getUsername());
        content = replaceFirst(content, "(?m)^    password: .*$", "    password: " + password);
        content = replaceFirst(content, "(?m)^  port: \\d+.*$", "  port: " + request.getDolphinPort());
        content = replaceFirst(content, "(?m)^    base-url: .*$", "    base-url: " + renderDolphinBaseUrl(request));
        content = replaceFirst(content, "(?m)^  type: (zookeeper|jdbc).*$", "  type: jdbc");
        content = replaceFirst(content, "(?m)^    url: jdbc:mysql://[^\\n]*dolphinscheduler\\?[^\\n]*$",
                "    url: " + jdbcUrl);
        content = replaceFirst(content, "(?m)^    password: root@123$", "    password: " + password);
        content = appendMysqlOverride(content, request, password, jdbcUrl);
        return content;
    }

    private String readExistingApplicationYaml(InstallContext context) {
        try {
            return new String(Files.readAllBytes(context.getConfDir().resolve("application.yaml")), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    private String replaceFirst(String content, String regex, String replacement) {
        if (content.matches("(?s).*" + regex + ".*")) {
            return content.replaceFirst(regex, Matcher.quoteReplacement(replacement));
        }
        return content;
    }

    private String appendMysqlOverride(String content, InstallConfigRequest request, String password, String jdbcUrl) {
        StringBuilder builder = new StringBuilder(content);
        if (!content.endsWith("\n")) {
            builder.append('\n');
        }
        builder.append("\n# Managed by DolphinScheduler offline installer. Keep this block at the end.\n");
        builder.append("---\n");
        builder.append("spring:\n");
        builder.append("  config:\n");
        builder.append("    activate:\n");
        builder.append("      on-profile: mysql\n");
        builder.append("  datasource:\n");
        builder.append("    driver-class-name: com.mysql.cj.jdbc.Driver\n");
        builder.append("    url: ").append(jdbcUrl).append('\n');
        builder.append("    username: ").append(request.getDatabase().getUsername()).append('\n');
        builder.append("    password: ").append(password).append('\n');
        builder.append("dolphinscheduler:\n");
        builder.append("  api:\n");
        builder.append("    base-url: ").append(renderDolphinBaseUrl(request)).append('\n');
        appendInternalRpcAddress(builder, request);
        appendJdbcRegistryConfig(builder, request, password, jdbcUrl);
        return builder.toString();
    }

    private String renderFallbackApplicationYaml(InstallConfigRequest request, String password, String jdbcUrl) {
        StringBuilder builder = new StringBuilder();
        builder.append("spring:\n");
        builder.append("  profiles:\n");
        builder.append("    active: mysql\n");
        builder.append("  datasource:\n");
        builder.append("    driver-class-name: com.mysql.cj.jdbc.Driver\n");
        builder.append("    url: ").append(jdbcUrl).append('\n');
        builder.append("    username: ").append(request.getDatabase().getUsername()).append('\n');
        builder.append("    password: ").append(password).append('\n');
        builder.append("server:\n");
        builder.append("  port: ").append(request.getDolphinPort()).append('\n');
        builder.append("  servlet:\n");
        builder.append("    context-path: /dolphinscheduler/\n");
        builder.append("dolphinscheduler:\n");
        builder.append("  api:\n");
        builder.append("    base-url: ").append(renderDolphinBaseUrl(request)).append('\n');
        appendInternalRpcAddress(builder, request);
        appendJdbcRegistryConfig(builder, request, password, jdbcUrl);
        return builder.toString();
    }

    private String renderDolphinBaseUrl(InstallConfigRequest request) {
        return "http://" + request.getService().getPublicHost() + ":" + request.getDolphinPort() + "/dolphinscheduler";
    }

    private void appendInternalRpcAddress(StringBuilder builder, InstallConfigRequest request) {
        String host = normalizeInternalHost(request.getService().getPublicHost());
        builder.append("master:\n");
        builder.append("  master-address: ").append(host).append(":5678\n");
        builder.append("worker:\n");
        builder.append("  worker-address: ").append(host).append(":1234\n");
    }

    private String normalizeInternalHost(String publicHost) {
        if ("0.0.0.0".equals(publicHost)) {
            return "127.0.0.1";
        }
        return publicHost;
    }

    private void appendJdbcRegistryConfig(StringBuilder builder,
                                          InstallConfigRequest request,
                                          String password,
                                          String jdbcUrl) {
        builder.append("registry:\n");
        builder.append("  type: jdbc\n");
        builder.append("  heartbeat-refresh-interval: 3s\n");
        builder.append("  session-timeout: 60s\n");
        builder.append("  hikari-config:\n");
        builder.append("    jdbc-url: ").append(jdbcUrl).append('\n');
        builder.append("    username: ").append(request.getDatabase().getUsername()).append('\n');
        builder.append("    password: ").append(password).append('\n');
        builder.append("    maximum-pool-size: 5\n");
        builder.append("    connection-timeout: 9000\n");
        builder.append("    idle-timeout: 600000\n");
    }

    private String renderCommonProperties(InstallConfigRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("resource.storage.type=LOCAL\n");
        builder.append("resource.storage.upload.base.path=").append(request.getService().getResourceDir()).append('\n');
        builder.append("data.basedir.path=").append(request.getInstallDir()).append("/data\n");
        builder.append("shell.env_source_list=dolphinscheduler_env.sh\n");
        builder.append("sync.task.tmp.dir=").append(request.getSync().getTmpDir()).append('\n');
        builder.append("sync.task.log.dir=").append(request.getSync().getLogDir()).append('\n');
        builder.append("sync.task.jdbc.dir=").append(request.getSync().getJdbcDir()).append('\n');
        return builder.toString();
    }

    private String renderDolphinSchedulerEnv(InstallConfigRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("#!/bin/bash\n");
        builder.append("export JAVA_HOME=").append(request.getJavaHome()).append('\n');
        builder.append("export DOLPHINSCHEDULER_HOME=").append(request.getInstallDir()).append('\n');
        builder.append("export SEATUNNEL_HOME=").append(request.getSync().getSeatunnelHome()).append('\n');
        builder.append("export PATH=$JAVA_HOME/bin:$SEATUNNEL_HOME/bin:$PATH\n");
        return builder.toString();
    }
}
