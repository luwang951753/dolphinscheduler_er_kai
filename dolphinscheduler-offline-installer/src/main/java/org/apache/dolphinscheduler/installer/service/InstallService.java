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
import org.apache.dolphinscheduler.installer.dto.InstallConfigRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

@Service
public class InstallService {

    private static final DateTimeFormatter INSTALL_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ConfigBackupService configBackupService;

    private final ConfigWriteService configWriteService;

    private final ConfigRenderService configRenderService;

    private final DolphinProcessService dolphinProcessService;

    private final DatabaseInitService databaseInitService;

    private final InstallProgressService installProgressService;

    private final StandaloneHomeValidator standaloneHomeValidator;

    private final UiPathService uiPathService;

    public InstallService(ConfigBackupService configBackupService,
                          ConfigWriteService configWriteService,
                          ConfigRenderService configRenderService,
                          DolphinProcessService dolphinProcessService,
                          DatabaseInitService databaseInitService,
                          InstallProgressService installProgressService,
                          StandaloneHomeValidator standaloneHomeValidator,
                          UiPathService uiPathService) {
        this.configBackupService = configBackupService;
        this.configWriteService = configWriteService;
        this.configRenderService = configRenderService;
        this.dolphinProcessService = dolphinProcessService;
        this.databaseInitService = databaseInitService;
        this.installProgressService = installProgressService;
        this.standaloneHomeValidator = standaloneHomeValidator;
        this.uiPathService = uiPathService;
    }

    public String createInstallId() {
        return "install-" + INSTALL_ID_FORMATTER.format(LocalDateTime.now());
    }

    public String install(InstallContext context, InstallConfigRequest request) {
        return install(createInstallId(), context, request);
    }

    public String install(String installId, InstallContext context, InstallConfigRequest request) {
        String backupId = null;
        try {
            installProgressService.running(installId, "VALIDATE_STANDALONE", "正在校验 standalone-server 安装目录");
            standaloneHomeValidator.validateOrThrow(context.getStandaloneHome());

            installProgressService.running(installId, "BACKUP_CONFIG", "正在备份旧配置");
            backupId = configBackupService.backup(context);

            installProgressService.running(installId, "WRITE_CONFIG", "正在写入新配置，备份编号: " + backupId);
            configWriteService.writeFiles(context, configRenderService.renderForWrite(context, request));
            uiPathService.normalizeUiAssetPaths(context);

            installProgressService.running(installId, "INIT_DATABASE", "正在检查并初始化 DolphinScheduler 数据库");
            String initMessage = databaseInitService.initializeIfNeeded(context, request);

            installProgressService.running(installId, "START_DOLPHIN", "正在启动 DolphinScheduler standalone");
            DolphinProcessService.ProcessResult processResult = dolphinProcessService.start(context,
                    request.getDolphinPort());
            if (!processResult.isSuccess()) {
                dolphinProcessService.stop(context);
                installProgressService.failed(installId, "START_DOLPHIN",
                        rollbackAfterFailure(context, backupId, processResult.getOutput()));
                return installId;
            }

            installProgressService.running(installId, "WRITE_LOCK", "正在写入安装锁");
            Files.write(context.getInstallLock(), installId.getBytes(StandardCharsets.UTF_8));
            installProgressService.success(installId, "SUCCESS", "安装完成。" + initMessage);
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (backupId != null) {
                message = rollbackAfterFailure(context, backupId, message);
            }
            installProgressService.failed(installId, "FAILED", message);
        }
        return installId;
    }

    private String rollbackAfterFailure(InstallContext context, String backupId, String failureMessage) {
        try {
            configWriteService.rollback(context, backupId);
            return failureMessage + "；已自动回滚配置，备份编号: " + backupId;
        } catch (Exception rollbackEx) {
            return failureMessage + "；自动回滚失败，请手工调用回滚接口，备份编号: " + backupId
                    + "，回滚错误: " + rollbackEx.getMessage();
        }
    }
}
