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
import org.apache.dolphinscheduler.installer.service.ConfigWriteService;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/installer/api")
public class RollbackController {

    private final ConfigWriteService configWriteService;

    private final String standaloneHome;

    private final String token;

    private final int installerPort;

    public RollbackController(ConfigWriteService configWriteService,
                              @Value("${installer.standalone-home:}") String standaloneHome,
                              @Value("${installer.token:}") String token,
                              @Value("${server.port:18080}") int installerPort) {
        this.configWriteService = configWriteService;
        this.standaloneHome = standaloneHome;
        this.token = token;
        this.installerPort = installerPort;
    }

    @PostMapping("/rollback")
    public Map<String, Object> rollback(@RequestBody Map<String, String> request) throws Exception {
        InstallContext context = InstallContext.from(Paths.get(standaloneHome), token, installerPort);
        configWriteService.rollback(context, request.get("backupId"));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "配置已回滚");
        response.put("data", new HashMap<String, Object>());
        return response;
    }
}
