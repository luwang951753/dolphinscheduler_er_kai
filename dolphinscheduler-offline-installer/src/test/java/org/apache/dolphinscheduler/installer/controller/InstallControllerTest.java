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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.dolphinscheduler.installer.dto.InstallProgress;
import org.apache.dolphinscheduler.installer.dto.InstallRequest;
import org.apache.dolphinscheduler.installer.service.InstallProgressService;
import org.apache.dolphinscheduler.installer.service.InstallService;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class InstallControllerTest {

    @Test
    void shouldReturnInstallIdBeforeBackgroundInstallFinishes() throws Exception {
        InstallProgressService progressService = new InstallProgressService();
        InstallService slowInstallService =
                new InstallService(null, null, null, null, null, progressService, null, null) {

                    @Override
                    public String createInstallId() {
                        return "install-test";
                    }

                    @Override
                    public String install(String installId,
                                          org.apache.dolphinscheduler.installer.core.InstallContext context,
                                          org.apache.dolphinscheduler.installer.dto.InstallConfigRequest request) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(250);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        progressService.success(installId, "SUCCESS", "done");
                        return installId;
                    }
                };

        InstallController controller = new InstallController(slowInstallService, progressService,
                "/tmp/standalone-server", "token", 18080);
        InstallRequest request = new InstallRequest();
        request.setConfirmPreview(true);

        long startedAt = System.currentTimeMillis();
        Map<String, Object> response = controller.install(request);

        assertThat(System.currentTimeMillis() - startedAt).isLessThan(200);
        assertThat(response.get("message")).isEqualTo("安装任务已开始");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertThat(data.get("installId")).isEqualTo("install-test");
        assertThat(progressService.get("install-test").getStatus()).isEqualTo(InstallProgress.STATUS_RUNNING);
    }

    @Test
    void shouldRejectInstallWhenPreviewIsNotConfirmed() {
        InstallProgressService progressService = new InstallProgressService();
        AtomicInteger installCount = new AtomicInteger();
        InstallService installService = new InstallService(null, null, null, null, null, progressService, null, null) {

            @Override
            public String install(String installId,
                                  org.apache.dolphinscheduler.installer.core.InstallContext context,
                                  org.apache.dolphinscheduler.installer.dto.InstallConfigRequest request) {
                installCount.incrementAndGet();
                return installId;
            }
        };
        InstallController controller = new InstallController(installService, progressService,
                "/tmp/standalone-server", "token", 18080);

        Map<String, Object> response = controller.install(new InstallRequest());

        assertThat(response.get("success")).isEqualTo(false);
        assertThat(response.get("code")).isEqualTo("PREVIEW_NOT_CONFIRMED");
        assertThat(installCount.get()).isZero();
    }

    @Test
    void shouldRejectConcurrentInstallRequests() throws Exception {
        InstallProgressService progressService = new InstallProgressService();
        InstallService slowInstallService =
                new InstallService(null, null, null, null, null, progressService, null, null) {

                    @Override
                    public String createInstallId() {
                        return "install-test";
                    }

                    @Override
                    public String install(String installId,
                                          org.apache.dolphinscheduler.installer.core.InstallContext context,
                                          org.apache.dolphinscheduler.installer.dto.InstallConfigRequest request) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(300);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        progressService.success(installId, "SUCCESS", "done");
                        return installId;
                    }
                };
        InstallController controller = new InstallController(slowInstallService, progressService,
                "/tmp/standalone-server", "token", 18080);
        InstallRequest request = new InstallRequest();
        request.setConfirmPreview(true);

        Map<String, Object> first = controller.install(request);
        Map<String, Object> second = controller.install(request);

        assertThat(first.get("success")).isEqualTo(true);
        assertThat(second.get("success")).isEqualTo(false);
        assertThat(second.get("code")).isEqualTo("INSTALL_ALREADY_RUNNING");
    }
}
