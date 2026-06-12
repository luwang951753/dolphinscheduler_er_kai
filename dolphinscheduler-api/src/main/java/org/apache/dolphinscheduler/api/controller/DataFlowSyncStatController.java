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

package org.apache.dolphinscheduler.api.controller;

import org.apache.dolphinscheduler.api.dto.DataFlowSyncStatDtos.QueryRequest;
import org.apache.dolphinscheduler.api.dto.DataFlowSyncStatDtos.StatResponse;
import org.apache.dolphinscheduler.api.dto.DataFlowSyncStatDtos.UpsertRequest;
import org.apache.dolphinscheduler.api.service.DataFlowSyncStatService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.dao.entity.User;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "DATAFLOW_SYNC_STAT_TAG")
@RestController
@RequestMapping("dataflow/sync-instance-stats")
public class DataFlowSyncStatController extends BaseController {

    @Autowired
    private DataFlowSyncStatService dataFlowSyncStatService;

    @PostMapping(value = "/query")
    @ResponseStatus(HttpStatus.OK)
    public Result<List<StatResponse>> queryStats(
                                                  @Parameter(hidden = true)
                                                  @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                                  @RequestBody QueryRequest request) {
        return Result.success(dataFlowSyncStatService.queryStats(loginUser, request));
    }

    @PostMapping(value = "/upsert")
    @ResponseStatus(HttpStatus.OK)
    public Result<StatResponse> upsertStat(
                                            @Parameter(hidden = true)
                                            @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                            @RequestBody UpsertRequest request) {
        return Result.success(dataFlowSyncStatService.upsertStat(loginUser, request));
    }
}
