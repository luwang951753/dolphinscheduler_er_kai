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

import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Asset;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Field;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Issue;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.IssueStatusRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Lineage;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.LineageRepairRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.LineageRepairResult;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.MetadataRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.QualityRule;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.QualityRuleRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SqlLineage;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SqlLineageParseRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SyncTaskLineageRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.TrialRunRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.TrialRunResult;
import org.apache.dolphinscheduler.api.service.DataGovernanceService;
import org.apache.dolphinscheduler.api.service.SqlLineageParseService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.dao.entity.User;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "DATA_GOVERNANCE_TAG")
@RestController
@RequestMapping("data-governance")
public class DataGovernanceController extends BaseController {

    @Autowired
    private DataGovernanceService dataGovernanceService;

    @Autowired
    private SqlLineageParseService sqlLineageParseService;

    @GetMapping(value = "/assets")
    @ResponseStatus(HttpStatus.OK)
    public Result<List<Asset>> queryAssets(
                                           @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                           @RequestParam(value = "keyword", required = false) String keyword,
                                           @RequestParam(value = "datasourceId", required = false) Integer datasourceId,
                                           @RequestParam(value = "database", required = false) String database,
                                           @RequestParam(value = "qualityStatus", required = false) String qualityStatus,
                                           @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(
                dataGovernanceService.queryAssets(loginUser, datasourceId, database, keyword, qualityStatus, limit));
    }

    @GetMapping(value = "/assets/{assetId}/fields")
    @ResponseStatus(HttpStatus.OK)
    public Result<List<Field>> queryFields(
                                           @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                           @PathVariable("assetId") String assetId) {
        return Result.success(dataGovernanceService.queryFields(loginUser, assetId));
    }

    @PutMapping(value = "/assets/{assetId}/metadata")
    @ResponseStatus(HttpStatus.OK)
    public Result<MetadataRequest> saveMetadata(
                                                @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                                @PathVariable("assetId") String assetId,
                                                @RequestBody MetadataRequest request) {
        return Result.success(dataGovernanceService.saveMetadata(loginUser, assetId, request));
    }

    @GetMapping(value = "/assets/{assetId}/rules")
    @ResponseStatus(HttpStatus.OK)
    public Result<List<QualityRule>> queryRules(
                                                @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                                @PathVariable("assetId") String assetId) {
        return Result.success(dataGovernanceService.queryRules(loginUser, assetId));
    }

    @PostMapping(value = "/assets/{assetId}/rules")
    @ResponseStatus(HttpStatus.OK)
    public Result<QualityRule> saveRule(
                                        @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                        @PathVariable("assetId") String assetId,
                                        @RequestBody QualityRuleRequest request) {
        return Result.success(dataGovernanceService.saveRule(loginUser, assetId, request));
    }

    @PostMapping(value = "/assets/{assetId}/rules/generate-sql")
    @ResponseStatus(HttpStatus.OK)
    public Result<String> generateRuleSql(
                                          @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                          @PathVariable("assetId") String assetId,
                                          @RequestBody QualityRuleRequest request) {
        return Result.success(dataGovernanceService.generateRuleSql(loginUser, assetId, request));
    }

    @PostMapping(value = "/assets/{assetId}/rules/trial-run")
    @ResponseStatus(HttpStatus.OK)
    public Result<TrialRunResult> trialRun(
                                           @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                           @PathVariable("assetId") String assetId,
                                           @RequestBody TrialRunRequest request) {
        return Result.success(dataGovernanceService.trialRun(loginUser, assetId, request));
    }

    @GetMapping(value = "/assets/{assetId}/lineage")
    @ResponseStatus(HttpStatus.OK)
    public Result<Lineage> queryLineage(
                                        @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                        @PathVariable("assetId") String assetId) {
        return Result.success(dataGovernanceService.queryLineage(loginUser, assetId));
    }

    @PostMapping(value = "/sql-lineage/parse")
    @ResponseStatus(HttpStatus.OK)
    public Result<SqlLineage> parseSqlLineage(
                                              @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                              @RequestBody SqlLineageParseRequest request) {
        return Result.success(sqlLineageParseService.parse(request == null ? null : request.getSql()));
    }

    @PostMapping(value = "/sync-task-lineage")
    @ResponseStatus(HttpStatus.OK)
    public Result<Lineage> registerSyncTaskLineage(
                                                   @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                                   @RequestBody SyncTaskLineageRequest request) {
        return Result.success(dataGovernanceService.registerSyncTaskLineage(loginUser, request));
    }

    @PostMapping(value = "/sync-task-lineage/repair")
    @ResponseStatus(HttpStatus.OK)
    public Result<LineageRepairResult> repairSyncTaskLineage(
                                                             @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                                             @RequestBody LineageRepairRequest request) {
        return Result.success(dataGovernanceService.repairSyncTaskLineage(loginUser, request));
    }

    @PostMapping(value = "/assets/{assetId}/rules/run-after-sync")
    @ResponseStatus(HttpStatus.OK)
    public Result<List<TrialRunResult>> runAfterSyncRules(
                                                          @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                                          @PathVariable("assetId") String assetId) {
        return Result.success(dataGovernanceService.runAfterSyncRules(loginUser, assetId));
    }

    @GetMapping(value = "/assets/{assetId}/issues")
    @ResponseStatus(HttpStatus.OK)
    public Result<List<Issue>> queryIssues(
                                           @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                           @PathVariable("assetId") String assetId) {
        return Result.success(dataGovernanceService.queryIssues(loginUser, assetId));
    }

    @PutMapping(value = "/assets/{assetId}/issues/{issueId}/status")
    @ResponseStatus(HttpStatus.OK)
    public Result<Issue> updateIssueStatus(
                                           @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                           @PathVariable("assetId") String assetId,
                                           @PathVariable("issueId") String issueId,
                                           @RequestBody IssueStatusRequest request) {
        return Result.success(dataGovernanceService.updateIssueStatus(loginUser, assetId, issueId, request));
    }
}
