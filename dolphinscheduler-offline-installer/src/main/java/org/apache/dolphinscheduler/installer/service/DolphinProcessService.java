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

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.Socket;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DolphinProcessService {

    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(180);

    private static final Duration STABLE_AFTER_PORT_OPEN = Duration.ofSeconds(12);

    public ProcessResult start(InstallContext context) throws IOException, InterruptedException {
        Path startScript = context.getStandaloneHome().resolve("bin").resolve("start.sh");
        if (!Files.exists(startScript)) {
            return new ProcessResult(false, "start.sh 不存在: " + startScript);
        }

        int dolphinPort = readDolphinPort(context);
        if (isPortOpen(dolphinPort)) {
            return new ProcessResult(false, "DolphinScheduler 端口已被占用: " + dolphinPort);
        }

        Path startupLog = context.getStandaloneHome().resolve("logs").resolve("dolphinscheduler-startup.log");
        Files.createDirectories(startupLog.getParent());

        ProcessBuilder processBuilder = new ProcessBuilder(startScript.toAbsolutePath().toString());
        processBuilder.directory(context.getStandaloneHome().toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(startupLog.toFile()));
        Process process = processBuilder.start();

        long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isDolphinAvailable(dolphinPort)) {
                TimeUnit.MILLISECONDS.sleep(STABLE_AFTER_PORT_OPEN.toMillis());
                if (isDolphinAvailable(dolphinPort)) {
                    return new ProcessResult(true, "DolphinScheduler 已启动，端口: " + dolphinPort);
                }
            }
            if (!process.isAlive()) {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    return new ProcessResult(false,
                            "DolphinScheduler 启动进程已退出，exitCode=" + exitCode + "\n"
                                    + readStartupFailure(context, startupLog));
                }
            }
            TimeUnit.SECONDS.sleep(2);
        }

        if (process.isAlive()) {
            process.destroy();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
        return new ProcessResult(false, "等待 DolphinScheduler 端口 " + dolphinPort + " 启动超时\n"
                + readStartupFailure(context, startupLog));
    }

    private String readStartupFailure(InstallContext context, Path startupLog) throws IOException {
        Path standaloneLog = context.getStandaloneHome().resolve("logs").resolve("dolphinscheduler-standalone.log");
        StringBuilder builder = new StringBuilder();
        builder.append(readTail(startupLog, 120));
        if (Files.exists(standaloneLog)) {
            builder.append(System.lineSeparator()).append(readTail(standaloneLog, 120));
        }
        return builder.toString();
    }

    private boolean isDolphinAvailable(int port) {
        if (!isPortOpen(port)) {
            return false;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(
                    "http://127.0.0.1:" + port + "/ui/").openConnection();
            connection.setConnectTimeout(500);
            connection.setReadTimeout(2000);
            connection.setRequestMethod("GET");
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                return false;
            }
            try (InputStream inputStream = connection.getInputStream()) {
                String body = new String(readFirstBytes(inputStream, 8192), StandardCharsets.UTF_8);
                return body.contains("<div id=\"app\">") && uiAssetsAvailable(port, body);
            }
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean uiAssetsAvailable(int port, String indexHtml) {
        List<String> assetPaths = extractAssetPaths(indexHtml);
        if (assetPaths.isEmpty()) {
            return false;
        }
        for (String assetPath : assetPaths) {
            if (!isAssetAvailable(port, assetPath)) {
                return false;
            }
        }
        return true;
    }

    private List<String> extractAssetPaths(String indexHtml) {
        List<String> paths = new ArrayList<>();
        addBetween(paths, indexHtml, "src=\"", "\"");
        addBetween(paths, indexHtml, "href=\"", "\"");
        paths.removeIf(path -> !(path.endsWith(".js") || path.endsWith(".css")));
        return paths;
    }

    private void addBetween(List<String> paths, String content, String prefix, String suffix) {
        int offset = 0;
        while (offset < content.length()) {
            int start = content.indexOf(prefix, offset);
            if (start < 0) {
                return;
            }
            start += prefix.length();
            int end = content.indexOf(suffix, start);
            if (end < 0) {
                return;
            }
            String path = content.substring(start, end);
            if (path.startsWith("/")) {
                paths.add(path);
            }
            offset = end + suffix.length();
        }
    }

    private boolean isAssetAvailable(int port, String assetPath) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(
                    "http://127.0.0.1:" + port + assetPath).openConnection();
            connection.setConnectTimeout(500);
            connection.setReadTimeout(2000);
            connection.setRequestMethod("GET");
            int code = connection.getResponseCode();
            String contentType = connection.getContentType();
            if (code != HttpURLConnection.HTTP_OK || contentType == null) {
                return false;
            }
            if (assetPath.endsWith(".js")) {
                return contentType.contains("javascript") || contentType.contains("ecmascript");
            }
            if (assetPath.endsWith(".css")) {
                return contentType.contains("text/css");
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private byte[] readFirstBytes(InputStream inputStream, int maxBytes) throws IOException {
        byte[] buffer = new byte[maxBytes];
        int offset = 0;
        while (offset < maxBytes) {
            int read = inputStream.read(buffer, offset, maxBytes - offset);
            if (read < 0) {
                break;
            }
            offset += read;
        }
        byte[] result = new byte[offset];
        System.arraycopy(buffer, 0, result, 0, offset);
        return result;
    }

    private int readDolphinPort(InstallContext context) throws IOException {
        List<String> lines = Files.readAllLines(context.getConfDir().resolve("application.yaml"), StandardCharsets.UTF_8);
        for (int index = 0; index < lines.size(); index++) {
            if ("server:".equals(lines.get(index).trim())) {
                for (int nested = index + 1; nested < lines.size(); nested++) {
                    String line = lines.get(nested);
                    if (!line.startsWith("  ")) {
                        break;
                    }
                    String trimmed = line.trim();
                    if (trimmed.startsWith("port:")) {
                        return Integer.parseInt(trimmed.substring("port:".length()).trim());
                    }
                }
            }
        }
        return 12345;
    }

    private boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private String readTail(Path logFile, int maxLines) throws IOException {
        if (!Files.exists(logFile)) {
            return "";
        }
        List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        int fromIndex = Math.max(0, lines.size() - maxLines);
        List<String> tail = new ArrayList<>(lines.subList(fromIndex, lines.size()));
        if (tail.isEmpty()) {
            Files.write(logFile, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        return String.join(System.lineSeparator(), tail);
    }

    public static class ProcessResult {

        private final boolean success;

        private final String output;

        public ProcessResult(boolean success, String output) {
            this.success = success;
            this.output = output;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutput() {
            return output;
        }
    }
}
