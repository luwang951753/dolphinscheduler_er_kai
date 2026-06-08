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

import org.apache.dolphinscheduler.installer.dto.CheckItem;
import org.apache.dolphinscheduler.installer.dto.EnvironmentCheckRequest;
import org.apache.dolphinscheduler.installer.service.EnvironmentCheckService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/installer/api/check")
public class EnvironmentController {

    private final EnvironmentCheckService environmentCheckService;

    public EnvironmentController(EnvironmentCheckService environmentCheckService) {
        this.environmentCheckService = environmentCheckService;
    }

    @PostMapping("/environment")
    public Map<String, Object> checkEnvironment(@RequestBody EnvironmentCheckRequest request) {
        List<CheckItem> items = environmentCheckService.check(request);
        boolean success = true;
        for (CheckItem item : items) {
            if (CheckItem.STATUS_FAIL.equals(item.getStatus())) {
                success = false;
                break;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("code", success ? "SUCCESS" : "ENV_CHECK_FAILED");
        response.put("message", success ? "环境检查通过" : "环境检查未通过");
        response.put("data", data);
        return response;
    }
}
