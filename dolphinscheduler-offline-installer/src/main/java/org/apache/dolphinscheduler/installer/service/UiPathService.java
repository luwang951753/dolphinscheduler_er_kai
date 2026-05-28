/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.dolphinscheduler.installer.service;

import org.apache.dolphinscheduler.installer.core.InstallContext;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Service
public class UiPathService {

    private static final String PACKAGED_UI_PREFIX = "/dolphinscheduler/ui/";

    private static final String STANDALONE_UI_PREFIX = "/ui/";

    private static final String PACKAGED_API_PREFIX_DOUBLE_QUOTED = "\"/dolphinscheduler\"";

    private static final String PACKAGED_API_PREFIX_SINGLE_QUOTED = "'/dolphinscheduler'";

    public void normalizeUiAssetPaths(InstallContext context) throws IOException {
        normalizeUiDir(context.getStandaloneHome().resolve("ui"));
        normalizeUiDir(context.getStandaloneHome().resolve("api-server").resolve("ui"));
    }

    private void normalizeUiDir(Path uiDir) throws IOException {
        if (!Files.isDirectory(uiDir)) {
            return;
        }
        normalizePathReferences(uiDir.resolve("index.html"));

        Path assetsDir = uiDir.resolve("assets");
        if (!Files.isDirectory(assetsDir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(assetsDir)) {
            files.filter(Files::isRegularFile)
                    .filter(this::isUiAssetWithPathReferences)
                    .forEach(path -> {
                        try {
                            normalizePathReferences(path);
                        } catch (IOException ex) {
                            throw new IllegalStateException("Failed to normalize UI asset path: " + path, ex);
                        }
                    });
        } catch (IllegalStateException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause();
            }
            throw ex;
        }
    }

    private void normalizePathReferences(Path file) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        String normalized = content
                .replace(PACKAGED_UI_PREFIX, STANDALONE_UI_PREFIX)
                .replace(PACKAGED_API_PREFIX_DOUBLE_QUOTED, "\"\"")
                .replace(PACKAGED_API_PREFIX_SINGLE_QUOTED, "''");
        if (!normalized.equals(content)) {
            Files.write(file, normalized.getBytes(StandardCharsets.UTF_8));
        }
    }

    private boolean isUiAssetWithPathReferences(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".js") || fileName.endsWith(".css");
    }
}
