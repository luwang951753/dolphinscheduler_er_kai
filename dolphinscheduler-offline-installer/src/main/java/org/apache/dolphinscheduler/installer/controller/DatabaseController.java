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

package org.apache.dolphinscheduler.installer.controller;

import org.apache.dolphinscheduler.installer.dto.DatabaseCheckRequest;
import org.apache.dolphinscheduler.installer.dto.DatabaseCheckResult;
import org.apache.dolphinscheduler.installer.service.DatabaseCheckService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/installer/api/check")
public class DatabaseController {

    private final DatabaseCheckService databaseCheckService;

    public DatabaseController(DatabaseCheckService databaseCheckService) {
        this.databaseCheckService = databaseCheckService;
    }

    @PostMapping("/database")
    public Map<String, Object> checkDatabase(@RequestBody DatabaseCheckRequest request) {
        DatabaseCheckResult result = databaseCheckService.check(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("code", result.getCode());
        response.put("message", result.getMessage());
        response.put("data", result);
        return response;
    }
}
