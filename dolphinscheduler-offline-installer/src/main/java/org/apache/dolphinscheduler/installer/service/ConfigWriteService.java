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

import org.apache.dolphinscheduler.installer.core.InstallContext;
import org.apache.dolphinscheduler.installer.dto.PreviewFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ConfigWriteService {

    public static final List<String> MANAGED_FILE_NAMES = Arrays.asList(
            "application.yaml",
            "common.properties",
            "dolphinscheduler_env.sh");

    private static final List<String> SERVER_NAMES = Arrays.asList(
            "api-server",
            "master-server",
            "worker-server",
            "alert-server");

    public void writeFiles(InstallContext context, List<PreviewFile> files) throws IOException {
        for (PreviewFile file : files) {
            Path target = validateManagedFile(context, file.getPath());
            writeAtomically(target, file.getContent());
        }
        syncServerConfigFiles(context);
    }

    public void rollback(InstallContext context, String backupId) throws IOException {
        Path backupDir = validateBackupDir(context, backupId);
        for (String fileName : MANAGED_FILE_NAMES) {
            Path source = backupDir.resolve(fileName);
            if (Files.exists(source)) {
                Path target = context.getConfDir().resolve(fileName);
                writeAtomically(target, new String(Files.readAllBytes(source), StandardCharsets.UTF_8));
            }
        }
        syncServerConfigFiles(context);
    }

    private Path validateManagedFile(InstallContext context, String targetPath) throws IOException {
        if (!StringUtils.hasText(targetPath)) {
            throw new IOException("配置文件路径不能为空");
        }
        Path confDir = context.getConfDir().toAbsolutePath().normalize();
        Path target = confDir.resolve(targetPath).toAbsolutePath().normalize();
        if (Paths.get(targetPath).isAbsolute()) {
            target = Paths.get(targetPath).toAbsolutePath().normalize();
        }

        String fileName = target.getFileName().toString();
        if (!target.startsWith(confDir) || !MANAGED_FILE_NAMES.contains(fileName)) {
            throw new IOException("非法配置文件路径: " + targetPath);
        }
        Files.createDirectories(target.getParent());
        return target;
    }

    private void syncServerConfigFiles(InstallContext context) throws IOException {
        for (String serverName : SERVER_NAMES) {
            Path serverHome = context.getStandaloneHome().resolve(serverName);
            if (!Files.isDirectory(serverHome)) {
                continue;
            }
            Path serverConfDir = serverHome.resolve("conf");
            Files.createDirectories(serverConfDir);
            for (String fileName : MANAGED_FILE_NAMES) {
                Path source = context.getConfDir().resolve(fileName);
                if (Files.exists(source)) {
                    writeAtomically(serverConfDir.resolve(fileName),
                            new String(Files.readAllBytes(source), StandardCharsets.UTF_8));
                }
            }
        }

        Path envSource = context.getConfDir().resolve("dolphinscheduler_env.sh");
        if (Files.exists(envSource)) {
            Path binEnv = context.getStandaloneHome().resolve("bin").resolve("env").resolve("dolphinscheduler_env.sh");
            Files.createDirectories(binEnv.getParent());
            writeAtomically(binEnv, new String(Files.readAllBytes(envSource), StandardCharsets.UTF_8));
        }
    }

    private Path validateBackupDir(InstallContext context, String backupId) throws IOException {
        if (!StringUtils.hasText(backupId) || backupId.contains("..") || backupId.contains("/")
                || backupId.contains("\\")) {
            throw new IOException("非法备份编号: " + backupId);
        }
        Path backupRoot = context.getBackupDir().toAbsolutePath().normalize();
        Path backupDir = backupRoot.resolve(backupId).normalize();
        if (!backupDir.startsWith(backupRoot) || !Files.isDirectory(backupDir)) {
            throw new IOException("备份不存在: " + backupId);
        }
        return backupDir;
    }

    private void writeAtomically(Path target, String content) throws IOException {
        Set<PosixFilePermission> permissions = readPosixPermissions(target);
        Path tempFile = Files.createTempFile(target.getParent(), "." + target.getFileName(), ".tmp");
        Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
        restorePosixPermissions(target, permissions);
    }

    private Set<PosixFilePermission> readPosixPermissions(Path target) {
        try {
            if (Files.exists(target)) {
                return new HashSet<>(Files.getPosixFilePermissions(target));
            }
        } catch (UnsupportedOperationException | IOException ignored) {
            return null;
        }
        return null;
    }

    private void restorePosixPermissions(Path target, Set<PosixFilePermission> permissions) throws IOException {
        if (permissions == null) {
            return;
        }
        try {
            Files.setPosixFilePermissions(target, permissions);
        } catch (UnsupportedOperationException ignored) {
            // 非 POSIX 文件系统不支持权限恢复，Windows 内网部署时可安全忽略。
        }
    }
}
