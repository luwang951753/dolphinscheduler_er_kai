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

package org.apache.dolphinscheduler.installer.controller;

import org.apache.dolphinscheduler.installer.core.InstallContext;
import org.apache.dolphinscheduler.installer.dto.InstallRequest;
import org.apache.dolphinscheduler.installer.service.InstallProgressService;
import org.apache.dolphinscheduler.installer.service.InstallService;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/installer/api")
public class InstallController {

    private final InstallService installService;

    private final InstallProgressService installProgressService;

    private final String standaloneHome;

    private final String token;

    private final int installerPort;

    private final ExecutorService installExecutor = Executors.newCachedThreadPool();

    private final AtomicBoolean installRunning = new AtomicBoolean(false);

    public InstallController(InstallService installService,
                             InstallProgressService installProgressService,
                             @Value("${installer.standalone-home:}") String standaloneHome,
                             @Value("${installer.token:}") String token,
                             @Value("${server.port:18080}") int installerPort) {
        this.installService = installService;
        this.installProgressService = installProgressService;
        this.standaloneHome = standaloneHome;
        this.token = token;
        this.installerPort = installerPort;
    }

    @PostMapping("/install")
    public Map<String, Object> install(@RequestBody InstallRequest request) {
        if (request == null || !request.isConfirmPreview()) {
            return fail("PREVIEW_NOT_CONFIRMED", "请先完成配置预览并确认后再开始安装");
        }
        if (!installRunning.compareAndSet(false, true)) {
            return fail("INSTALL_ALREADY_RUNNING", "已有安装任务正在执行，请等待当前任务完成");
        }
        InstallContext context = InstallContext.from(Paths.get(resolveStandaloneHome(request)), token, installerPort);
        String installId = installService.createInstallId();
        installProgressService.running(installId, "PENDING", "安装任务已提交，等待后台执行");
        installExecutor.submit(() -> {
            try {
                installService.install(installId, context, request.getConfig());
            } finally {
                installRunning.set(false);
            }
        });

        Map<String, Object> data = new HashMap<>();
        data.put("installId", installId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "安装任务已开始");
        response.put("data", data);
        return response;
    }

    private Map<String, Object> fail(String code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", code);
        response.put("message", message);
        response.put("data", new HashMap<String, Object>());
        return response;
    }

    @GetMapping("/install/{installId}/progress")
    public Map<String, Object> progress(@PathVariable("installId") String installId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "ok");
        response.put("data", installProgressService.get(installId));
        return response;
    }

    private String resolveStandaloneHome(InstallRequest request) {
        if (request != null && request.getConfig() != null
                && request.getConfig().getInstallDir() != null
                && !request.getConfig().getInstallDir().trim().isEmpty()) {
            return request.getConfig().getInstallDir();
        }
        return standaloneHome;
    }
}
