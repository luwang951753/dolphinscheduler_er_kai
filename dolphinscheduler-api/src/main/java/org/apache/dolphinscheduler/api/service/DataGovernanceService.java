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

package org.apache.dolphinscheduler.api.service;

import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Asset;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Field;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Issue;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.IssueStatusRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Lineage;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.MetadataRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.QualityRule;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.QualityRuleRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SyncTaskLineageRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.TrialRunRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.TrialRunResult;
import org.apache.dolphinscheduler.dao.entity.User;

import java.util.List;

public interface DataGovernanceService {

    List<Asset> queryAssets(
            User loginUser,
            Integer datasourceId,
            String database,
            String keyword,
            String qualityStatus,
            Integer limit);

    List<Field> queryFields(User loginUser, String assetId);

    MetadataRequest saveMetadata(User loginUser, String assetId, MetadataRequest request);

    List<QualityRule> queryRules(User loginUser, String assetId);

    QualityRule saveRule(User loginUser, String assetId, QualityRuleRequest request);

    TrialRunResult trialRun(User loginUser, String assetId, TrialRunRequest request);

    Lineage queryLineage(User loginUser, String assetId);

    Lineage registerSyncTaskLineage(User loginUser, SyncTaskLineageRequest request);

    List<Issue> queryIssues(User loginUser, String assetId);

    Issue updateIssueStatus(User loginUser, String assetId, String issueId, IssueStatusRequest request);

    String generateRuleSql(User loginUser, String assetId, QualityRuleRequest request);
}
