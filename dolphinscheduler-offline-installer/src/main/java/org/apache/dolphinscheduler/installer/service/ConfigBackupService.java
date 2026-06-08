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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

@Service
public class ConfigBackupService {

    private static final DateTimeFormatter BACKUP_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String backup(InstallContext context) throws IOException {
        String backupId = "install-" + BACKUP_ID_FORMATTER.format(LocalDateTime.now());
        Path backupDir = context.getBackupDir().resolve(backupId);
        Files.createDirectories(backupDir);

        for (String fileName : ConfigWriteService.MANAGED_FILE_NAMES) {
            Path source = context.getConfDir().resolve(fileName);
            if (Files.exists(source)) {
                Files.copy(source, backupDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
        return backupId;
    }
}
