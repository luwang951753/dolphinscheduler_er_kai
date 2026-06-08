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

package org.apache.dolphinscheduler.installer.core;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 安装器运行上下文只保存 standalone 包内的可信路径，后续写配置和备份都必须从这里取根路径。
 */
public final class InstallContext {

    private final Path standaloneHome;

    private final Path confDir;

    private final Path backupDir;

    private final Path installLock;

    private final String token;

    private final int installerPort;

    private InstallContext(Path standaloneHome, String token, int installerPort) {
        this.standaloneHome = Objects.requireNonNull(standaloneHome, "standaloneHome").normalize();
        this.confDir = this.standaloneHome.resolve("conf");
        this.backupDir = this.standaloneHome.resolve("backup");
        this.installLock = this.standaloneHome.resolve("install.lock");
        this.token = Objects.requireNonNull(token, "token");
        this.installerPort = installerPort;
    }

    public static InstallContext from(Path standaloneHome, String token, int installerPort) {
        return new InstallContext(standaloneHome, token, installerPort);
    }

    public Path getStandaloneHome() {
        return standaloneHome;
    }

    public Path getConfDir() {
        return confDir;
    }

    public Path getBackupDir() {
        return backupDir;
    }

    public Path getInstallLock() {
        return installLock;
    }

    public String getToken() {
        return token;
    }

    public int getInstallerPort() {
        return installerPort;
    }
}
