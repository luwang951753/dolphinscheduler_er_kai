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

package org.apache.dolphinscheduler.api.service.impl;

import org.apache.dolphinscheduler.api.constants.ApiFuncIdentificationConstant;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Asset;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Field;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Issue;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.IssueStatusRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Lineage;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.LineageNode;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.LineageRepairRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.LineageRepairResult;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.MetadataRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.QualityRule;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.QualityRuleRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SyncTaskLineageRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.TrialRunRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.TrialRunResult;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.api.service.DataGovernanceService;
import org.apache.dolphinscheduler.api.service.DataGovernanceStore;
import org.apache.dolphinscheduler.common.enums.AuthorizationType;
import org.apache.dolphinscheduler.common.enums.WorkflowExecutionStatus;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.entity.WorkflowDefinition;
import org.apache.dolphinscheduler.dao.entity.WorkflowInstance;
import org.apache.dolphinscheduler.dao.mapper.DataSourceMapper;
import org.apache.dolphinscheduler.dao.mapper.WorkflowDefinitionMapper;
import org.apache.dolphinscheduler.dao.mapper.WorkflowInstanceMapper;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;

import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

@Slf4j
@Service
public class DataGovernanceServiceImpl extends BaseServiceImpl implements DataGovernanceService {

    private static final String[] TABLE_TYPES = new String[]{"TABLE", "VIEW"};
    private static final String DEFAULT_SCHEMA = "public";
    private static final int DEFAULT_ASSET_LIMIT = 80;
    private static final int MAX_ASSET_LIMIT = 200;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired
    private DataGovernanceStore dataGovernanceStore;

    @Autowired
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Autowired
    private WorkflowInstanceMapper workflowInstanceMapper;

    @Override
    public List<Asset> queryAssets(
                                   User loginUser,
                                   Integer datasourceId,
                                   String database,
                                   String keyword,
                                   String qualityStatus,
                                   Integer limit) {
        String normalizedKeyword = normalizeAssetKeyword(keyword);
        int queryLimit = normalizeAssetLimit(limit);
        Map<String, Asset> assets = new java.util.LinkedHashMap<>();
        if (datasourceId != null && StringUtils.isNotBlank(database)) {
            DataSource dataSource = getDatasource(loginUser, datasourceId);
            discoverDatasourceAssets(dataSource, database, normalizedKeyword, queryLimit).stream()
                    .map(this::mergePersistedAssetState)
                    .forEach(asset -> assets.put(asset.getId(), asset));
        } else if (StringUtils.isNotBlank(normalizedKeyword)) {
            discoverKeywordDatasourceAssets(loginUser, normalizedKeyword, queryLimit).stream()
                    .map(this::mergePersistedAssetState)
                    .forEach(asset -> assets.put(asset.getId(), asset));
        }
        discoverLineageAssets(loginUser, datasourceId, database).stream()
                .map(this::mergePersistedAssetState)
                .forEach(asset -> assets.putIfAbsent(asset.getId(), asset));
        return assets.values().stream()
                .filter(asset -> matchesKeyword(asset, normalizedKeyword))
                .filter(asset -> StringUtils.isBlank(qualityStatus)
                        || StringUtils.equalsIgnoreCase(qualityStatus, asset.getQualityStatus()))
                .peek(asset -> fillAssetFieldCount(loginUser, asset))
                .limit(queryLimit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Field> queryFields(User loginUser, String assetId) {
        AssetRef assetRef = parseAssetId(assetId);
        DataSource dataSource = getDatasource(loginUser, assetRef.datasourceId);
        return queryFields(dataSource, assetRef.database, assetRef.schema, assetRef.tableName);
    }

    @Override
    public MetadataRequest saveMetadata(User loginUser, String assetId, MetadataRequest request) {
        parseAndCheckAsset(loginUser, assetId);
        MetadataRequest metadata = new MetadataRequest();
        metadata.setOwner(StringUtils.trimToEmpty(request == null ? null : request.getOwner()));
        metadata.setDescription(StringUtils.trimToEmpty(request == null ? null : request.getDescription()));
        metadata.setTags(request == null || request.getTags() == null ? new ArrayList<>() : request.getTags());
        return dataGovernanceStore.saveMetadata(assetId, metadata);
    }

    @Override
    public List<QualityRule> queryRules(User loginUser, String assetId) {
        parseAndCheckAsset(loginUser, assetId);
        return dataGovernanceStore.getRules(assetId);
    }

    @Override
    public QualityRule saveRule(User loginUser, String assetId, QualityRuleRequest request) {
        parseAndCheckAsset(loginUser, assetId);
        if (request == null || StringUtils.isBlank(request.getName()) || StringUtils.isBlank(request.getType())) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        QualityRule rule = new QualityRule();
        BeanUtils.copyProperties(request, rule);
        rule.setAssetId(assetId);
        if (StringUtils.isBlank(rule.getId())) {
            rule.setId("rule-" + UUID.randomUUID());
        }
        if (StringUtils.isBlank(rule.getLevel())) {
            rule.setLevel(StringUtils.isBlank(rule.getFieldName()) ? "TABLE" : "FIELD");
        }
        if (StringUtils.isBlank(rule.getSamplePolicy())) {
            rule.setSamplePolicy("TOP_50");
        }
        if (StringUtils.isBlank(rule.getFailureThreshold())) {
            rule.setFailureThreshold("COUNT_GT_0");
        }
        if (StringUtils.isBlank(rule.getSeverity())) {
            rule.setSeverity("MEDIUM");
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(Boolean.TRUE);
        }
        if (rule.getCreateIssue() == null) {
            rule.setCreateIssue(Boolean.TRUE);
        }
        if (rule.getEscalateIssue() == null) {
            rule.setEscalateIssue(Boolean.TRUE);
        }
        if (rule.getAutoCloseIssue() == null) {
            rule.setAutoCloseIssue(Boolean.FALSE);
        }
        if (StringUtils.isBlank(rule.getSql())) {
            rule.setSql(generateRuleSql(loginUser, assetId, request));
            rule.setManualSql(Boolean.FALSE);
        }
        validateReadonlySql(rule.getSql());
        rule.setStatus(StringUtils.defaultIfBlank(rule.getStatus(), "NOT_RUN"));
        return dataGovernanceStore.saveRule(assetId, rule);
    }

    @Override
    public TrialRunResult trialRun(User loginUser, String assetId, TrialRunRequest request) {
        AssetRef assetRef = parseAndCheckAsset(loginUser, assetId);
        DataSource dataSource = getDatasource(loginUser, assetRef.datasourceId);
        String sql = request == null ? null : request.getSql();
        QualityRuleRequest ruleRequest = request == null ? null : request.getRule();
        if (StringUtils.isBlank(sql) && ruleRequest != null) {
            sql = generateRuleSql(loginUser, assetId, ruleRequest);
        }
        validateReadonlySql(sql);
        if (ruleRequest != null && Boolean.FALSE.equals(ruleRequest.getEnabled())) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        TrialRunResult result = executeTrialSql(dataSource, assetRef, sql);
        if (ruleRequest != null && StringUtils.isNotBlank(ruleRequest.getId())) {
            QualityRule savedRule = saveRule(loginUser, assetId, ruleRequest);
            savedRule.setSql(sql);
            savedRule.setLastRunAt(result.getExecutedAt());
            savedRule.setAbnormalCount(result.getAbnormalCount());
            savedRule.setAbnormalRate(result.getAbnormalRate());
            savedRule.setStatus(Boolean.TRUE.equals(result.getPassed()) ? "PASS" : "FAILED");
            dataGovernanceStore.saveRule(assetId, savedRule);
            if (!Boolean.TRUE.equals(result.getPassed()) && savedRule.getCreateIssue() != Boolean.FALSE) {
                createOrUpdateIssue(assetId, savedRule, result);
            } else if (Boolean.TRUE.equals(result.getPassed()) && Boolean.TRUE.equals(savedRule.getAutoCloseIssue())) {
                closeRuleIssues(assetId, savedRule, result.getExecutedAt());
            }
        }
        return result;
    }

    @Override
    public Lineage queryLineage(User loginUser, String assetId) {
        parseAndCheckAsset(loginUser, assetId);
        Lineage lineage = new Lineage();
        lineage.setUpstream(dataGovernanceStore.getUpstream(assetId));
        lineage.setDownstream(dataGovernanceStore.getDownstream(assetId));
        return lineage;
    }

    @Override
    public Lineage registerSyncTaskLineage(User loginUser, SyncTaskLineageRequest request) {
        if (request == null
                || request.getSourceDatasourceId() == null
                || request.getTargetDatasourceId() == null
                || StringUtils.isBlank(request.getSourceDatabase())
                || StringUtils.isBlank(request.getSourceTable())
                || StringUtils.isBlank(request.getTargetDatabase())
                || StringUtils.isBlank(request.getTargetTable())) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        DataSource sourceDataSource = getDatasource(loginUser, request.getSourceDatasourceId());
        DataSource targetDataSource = getDatasource(loginUser, request.getTargetDatasourceId());
        String sourceSchema = normalizeSchema(sourceDataSource, request.getSourceSchema());
        String targetSchema = normalizeSchema(targetDataSource, request.getTargetSchema());
        String sourceAssetId =
                buildAssetId(sourceDataSource.getId(), request.getSourceDatabase(), sourceSchema,
                        request.getSourceTable());
        String targetAssetId =
                buildAssetId(targetDataSource.getId(), request.getTargetDatabase(), targetSchema,
                        request.getTargetTable());
        parseAssetId(sourceAssetId);
        parseAssetId(targetAssetId);

        String syncTaskName = StringUtils.defaultIfBlank(request.getSyncTaskName(), "同步任务");
        String lastRunStatus = StringUtils.defaultIfBlank(request.getLastRunStatus(), "SAVED");
        String lastRunTime = StringUtils.defaultIfBlank(request.getLastRunTime(), now());

        LineageNode upstream = new LineageNode();
        upstream.setAssetId(sourceAssetId);
        upstream.setAssetName(buildLineageAssetName(sourceDataSource, request.getSourceDatabase(), sourceSchema,
                request.getSourceTable()));
        upstream.setRelationType("源表");
        upstream.setSyncTaskName(syncTaskName);
        upstream.setLastRunStatus(lastRunStatus);
        upstream.setLastRunTime(lastRunTime);
        upstream.setFieldMappings(normalizeFieldMappings(request));

        LineageNode downstream = new LineageNode();
        downstream.setAssetId(targetAssetId);
        downstream.setAssetName(buildLineageAssetName(targetDataSource, request.getTargetDatabase(), targetSchema,
                request.getTargetTable()));
        downstream.setRelationType("目标表");
        downstream.setSyncTaskName(syncTaskName);
        downstream.setLastRunStatus(lastRunStatus);
        downstream.setLastRunTime(lastRunTime);
        downstream.setFieldMappings(normalizeFieldMappings(request));

        dataGovernanceStore.replaceLineage(targetAssetId, upstream, downstream);
        if (shouldRunAfterSyncRules(lastRunStatus)) {
            runAfterSyncRules(loginUser, targetAssetId);
        }
        Lineage lineage = new Lineage();
        lineage.setUpstream(dataGovernanceStore.getUpstream(targetAssetId));
        lineage.setDownstream(dataGovernanceStore.getDownstream(targetAssetId));
        return lineage;
    }

    @Override
    public LineageRepairResult repairSyncTaskLineage(User loginUser, LineageRepairRequest request) {
        if (!isAdmin(loginUser)) {
            throw new ServiceException(Status.USER_NO_OPERATION_PERM);
        }
        String syncTaskName = StringUtils.trimToEmpty(request == null ? null : request.getSyncTaskName());
        if (StringUtils.isBlank(syncTaskName)) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        WorkflowInstance workflowInstance = findLatestWorkflowInstance(syncTaskName);
        LineageRepairResult result = new LineageRepairResult();
        result.setSyncTaskName(syncTaskName);
        if (workflowInstance == null) {
            result.setRepairedRows(0);
            result.setMessage("未找到对应工作流实例，未修改血缘状态。");
            return result;
        }
        result.setWorkflowInstanceId(workflowInstance.getId());
        String lineageStatus = toLineageRunStatus(workflowInstance.getState());
        if (StringUtils.isBlank(lineageStatus)) {
            result.setRepairedRows(0);
            result.setMessage("工作流实例尚未终态，未修改血缘状态。");
            return result;
        }
        String repairedAt = workflowInstance.getEndTime() == null ? now()
                : TIME_FORMATTER.format(
                        workflowInstance.getEndTime().toInstant().atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime());
        int repairedRows = dataGovernanceStore.repairRunningLineageStatus(syncTaskName, lineageStatus, repairedAt);
        result.setRepairedStatus(lineageStatus);
        result.setRepairedAt(repairedAt);
        result.setRepairedRows(repairedRows);
        result.setMessage(repairedRows > 0 ? "已回补同步任务血缘终态。" : "未发现需要回补的 RUNNING 血缘记录。");
        return result;
    }

    @Override
    public List<TrialRunResult> runAfterSyncRules(User loginUser, String assetId) {
        parseAndCheckAsset(loginUser, assetId);
        List<TrialRunResult> results = new ArrayList<>();
        for (QualityRule rule : dataGovernanceStore.getRules(assetId)) {
            if (!Boolean.TRUE.equals(rule.getEnabled()) || !StringUtils.equals(rule.getFrequency(), "AFTER_SYNC")) {
                continue;
            }
            TrialRunRequest request = new TrialRunRequest();
            QualityRuleRequest ruleRequest = new QualityRuleRequest();
            BeanUtils.copyProperties(rule, ruleRequest);
            request.setRule(ruleRequest);
            request.setSql(rule.getSql());
            try {
                results.add(trialRun(loginUser, assetId, request));
            } catch (Exception ex) {
                log.warn("Run after-sync data governance rule failed, assetId:{}, ruleId:{}.", assetId, rule.getId(),
                        ex);
            }
        }
        return results;
    }

    static boolean shouldRunAfterSyncRules(String lastRunStatus) {
        return StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(lastRunStatus), "SUCCESS");
    }

    static String toLineageRunStatus(WorkflowExecutionStatus workflowStatus) {
        if (workflowStatus == null || !workflowStatus.isFinalState()) {
            return null;
        }
        return workflowStatus.isSuccess() ? "SUCCESS" : "FAILED";
    }

    private WorkflowInstance findLatestWorkflowInstance(String syncTaskName) {
        WorkflowDefinition definition =
                workflowDefinitionMapper.selectOne(new QueryWrapper<WorkflowDefinition>().lambda()
                        .eq(WorkflowDefinition::getName, syncTaskName)
                        .orderByDesc(WorkflowDefinition::getUpdateTime)
                        .last("limit 1"));
        if (definition != null) {
            List<WorkflowInstance> instances =
                    workflowInstanceMapper.queryByWorkflowDefinitionCode(definition.getCode(), 1);
            if (!instances.isEmpty()) {
                return instances.get(0);
            }
        }
        return workflowInstanceMapper.selectOne(new QueryWrapper<WorkflowInstance>().lambda()
                .likeRight(WorkflowInstance::getName, syncTaskName + "-")
                .orderByDesc(WorkflowInstance::getStartTime)
                .orderByDesc(WorkflowInstance::getId)
                .last("limit 1"));
    }

    @Override
    public List<Issue> queryIssues(User loginUser, String assetId) {
        parseAndCheckAsset(loginUser, assetId);
        return dataGovernanceStore.getIssues(assetId);
    }

    @Override
    public Issue updateIssueStatus(User loginUser, String assetId, String issueId, IssueStatusRequest request) {
        parseAndCheckAsset(loginUser, assetId);
        String status = request == null ? null : request.getStatus();
        if (!StringUtils.equalsAny(status, "OPEN", "PROCESSING", "RESOLVED")) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        for (Issue issue : dataGovernanceStore.getIssues(assetId)) {
            if (StringUtils.equals(issue.getId(), issueId)) {
                issue.setStatus(status);
                issue.setUpdatedAt(now());
                return dataGovernanceStore.saveIssue(assetId, issue);
            }
        }
        throw new ServiceException(Status.RESOURCE_NOT_EXIST);
    }

    @Override
    public String generateRuleSql(User loginUser, String assetId, QualityRuleRequest request) {
        AssetRef assetRef = parseAndCheckAsset(loginUser, assetId);
        if (request == null) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        DataSource dataSource = getDatasource(loginUser, assetRef.datasourceId);
        String table = qualifiedTableName(dataSource.getType(), assetRef);
        String where = buildRuleWhere(request, dataSource.getType(), table);
        String scopedWhere = StringUtils.isBlank(request.getRangeCondition())
                ? where
                : "(" + where + ") AND (" + request.getRangeCondition() + ")";
        return "SELECT abnormal_count,\n"
                + "       CASE WHEN total_count = 0 THEN 0 ELSE ROUND(abnormal_count * 1.0 / total_count, 6) END AS abnormal_rate\n"
                + "FROM (\n"
                + "  SELECT SUM(CASE WHEN " + scopedWhere + " THEN 1 ELSE 0 END) AS abnormal_count,\n"
                + "         COUNT(1) AS total_count\n"
                + "  FROM " + table + "\n"
                + ") quality_check";
    }

    private List<Asset> discoverDatasourceAssets(DataSource dataSource, String databaseFilter, String keyword,
                                                 int limit) {
        BaseConnectionParam connectionParam = buildConnectionParam(dataSource);
        Connection connection = DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
        if (connection == null) {
            return Collections.emptyList();
        }
        ResultSet databaseRs = null;
        ResultSet tableRs = null;
        try {
            List<String> databases = new ArrayList<>();
            if (StringUtils.isNotBlank(databaseFilter)) {
                databases.add(StringUtils.trim(databaseFilter));
            } else if (dataSource.getType() == DbType.POSTGRESQL) {
                databases.add(StringUtils.defaultIfBlank(connectionParam.getDatabase(), connection.getCatalog()));
            } else {
                databaseRs = connection.getMetaData().getCatalogs();
                while (databaseRs != null && databaseRs.next()) {
                    databases.add(databaseRs.getString(1));
                }
                if (databases.isEmpty() && StringUtils.isNotBlank(connectionParam.getDatabase())) {
                    databases.add(connectionParam.getDatabase());
                }
            }
            List<Asset> assets = new ArrayList<>();
            DatabaseMetaData metaData = connection.getMetaData();
            String tableNamePattern = buildTableNamePattern(keyword);
            for (String database : databases) {
                if (StringUtils.isBlank(database) || isSystemDatabase(dataSource.getType(), database)) {
                    continue;
                }
                validateIdentifier(database);
                String catalog = getCatalog(dataSource.getType(), database);
                String schema = getSchemaPattern(dataSource.getType(), null, connectionParam);
                tableRs = metaData.getTables(catalog, schema, tableNamePattern, TABLE_TYPES);
                while (tableRs != null && tableRs.next()) {
                    Asset asset = new Asset();
                    asset.setDatasourceId(dataSource.getId());
                    asset.setDatasourceName(dataSource.getName());
                    asset.setDatasourceType(dataSource.getType().name());
                    asset.setDatabase(database);
                    asset.setSchema(StringUtils.trimToEmpty(schema));
                    asset.setTableName(tableRs.getString("TABLE_NAME"));
                    asset.setTableType(tableRs.getString("TABLE_TYPE"));
                    asset.setDescription(StringUtils.trimToEmpty(tableRs.getString("REMARKS")));
                    asset.setFullName(buildFullName(dataSource, database, schema, asset.getTableName()));
                    asset.setId(buildAssetId(dataSource.getId(), database, schema, asset.getTableName()));
                    asset.setFieldCount(countTableFields(metaData, catalog, schema, asset.getTableName()));
                    asset.setQualityStatus("NOT_CONFIGURED");
                    assets.add(asset);
                    if (assets.size() >= limit) {
                        return assets;
                    }
                }
                closeResult(tableRs);
                tableRs = null;
            }
            return assets;
        } catch (Exception ex) {
            log.warn("Discover data governance assets failed, datasourceId:{}.", dataSource.getId(), ex);
            return Collections.emptyList();
        } finally {
            closeResult(databaseRs);
            closeResult(tableRs);
            releaseConnection(connection);
        }
    }

    private List<Asset> discoverKeywordDatasourceAssets(User loginUser, String keyword, int limit) {
        List<Asset> assets = new ArrayList<>();
        List<DataSource> dataSources = dataSourceMapper.selectList(new QueryWrapper<DataSource>().lambda()
                .orderByAsc(DataSource::getId));
        for (DataSource dataSource : dataSources) {
            if (assets.size() >= limit || !isSupported(dataSource) || !canReadDatasource(loginUser, dataSource.getId())) {
                continue;
            }
            int remaining = limit - assets.size();
            try {
                assets.addAll(discoverDatasourceAssets(dataSource, null, keyword, remaining));
            } catch (Exception ex) {
                log.warn("Skip data governance keyword discovery for datasourceId:{}.", dataSource.getId(), ex);
            }
        }
        return assets;
    }

    private List<Asset> discoverLineageAssets(User loginUser, Integer datasourceId, String database) {
        DataGovernanceStore.StoreState state = dataGovernanceStore.snapshot();
        Set<String> assetIds = new LinkedHashSet<>();
        assetIds.addAll(state.getUpstream().keySet());
        assetIds.addAll(state.getDownstream().keySet());
        state.getUpstream().values().forEach(nodes -> nodes.forEach(node -> assetIds.add(node.getAssetId())));
        state.getDownstream().values().forEach(nodes -> nodes.forEach(node -> assetIds.add(node.getAssetId())));
        List<Asset> assets = new ArrayList<>();
        for (String assetId : assetIds) {
            AssetRef assetRef;
            try {
                assetRef = parseAssetId(assetId);
            } catch (ServiceException ex) {
                continue;
            }
            if (datasourceId != null && !datasourceId.equals(assetRef.datasourceId)) {
                continue;
            }
            if (StringUtils.isNotBlank(database) && !StringUtils.equalsIgnoreCase(database, assetRef.database)) {
                continue;
            }
            DataSource dataSource;
            try {
                dataSource = getDatasource(loginUser, assetRef.datasourceId);
            } catch (ServiceException ex) {
                continue;
            }
            Asset asset = new Asset();
            asset.setId(assetRef.assetId);
            asset.setDatasourceId(dataSource.getId());
            asset.setDatasourceName(dataSource.getName());
            asset.setDatasourceType(dataSource.getType().name());
            asset.setDatabase(assetRef.database);
            asset.setSchema(StringUtils.trimToEmpty(assetRef.schema));
            asset.setTableName(assetRef.tableName);
            asset.setTableType("SQL_LINEAGE");
            asset.setFullName(buildFullName(dataSource, assetRef.database, assetRef.schema, assetRef.tableName));
            try {
                asset.setFieldCount(queryFields(dataSource, assetRef.database, assetRef.schema, assetRef.tableName).size());
            } catch (Exception ex) {
                log.warn("Resolve lineage asset field count failed, assetId:{}.", assetId, ex);
            }
            asset.setQualityStatus("NOT_CONFIGURED");
            assets.add(asset);
        }
        return assets;
    }

    private int countTableFields(DatabaseMetaData metaData, String catalog, String schema, String tableName)
            throws java.sql.SQLException {
        int count = 0;
        try (ResultSet columnRs = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (columnRs != null && columnRs.next()) {
                count++;
            }
        }
        return count;
    }

    private int normalizeAssetLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_ASSET_LIMIT;
        }
        return Math.min(limit, MAX_ASSET_LIMIT);
    }

    private String buildTableNamePattern(String keyword) {
        String normalized = normalizeAssetKeyword(keyword);
        if (StringUtils.isBlank(normalized)) {
            return "%";
        }
        return "%" + normalized + "%";
    }

    private String normalizeAssetKeyword(String keyword) {
        return StringUtils.trimToEmpty(keyword)
                .replace("%", "")
                .replace("*", "")
                .replace("?", "");
    }

    private Asset mergePersistedAssetState(Asset asset) {
        MetadataRequest metadata = dataGovernanceStore.getMetadata(asset.getId());
        if (metadata != null) {
            asset.setOwner(metadata.getOwner());
            asset.setDescription(metadata.getDescription());
            asset.setTags(metadata.getTags());
        }
        List<QualityRule> rules = dataGovernanceStore.getRules(asset.getId());
        List<Issue> issues = dataGovernanceStore.getIssues(asset.getId());
        asset.setRuleCount(rules.size());
        asset.setIssueCount(
                (int) issues.stream().filter(issue -> !StringUtils.equals(issue.getStatus(), "RESOLVED")).count());
        QualityRule lastRule =
                rules.stream().filter(rule -> StringUtils.isNotBlank(rule.getLastRunAt())).findFirst().orElse(null);
        if (lastRule != null) {
            asset.setLastCheckTime(lastRule.getLastRunAt());
        }
        if (asset.getIssueCount() != null && asset.getIssueCount() > 0) {
            asset.setQualityStatus("FAILED");
        } else if (!rules.isEmpty()) {
            asset.setQualityStatus(
                    rules.stream().anyMatch(rule -> StringUtils.equals(rule.getStatus(), "PASS")) ? "PASS" : "NOT_RUN");
        }
        List<LineageNode> upstream = dataGovernanceStore.getUpstream(asset.getId());
        if (!upstream.isEmpty()) {
            asset.setLastSyncTask(upstream.get(0).getSyncTaskName());
        }
        asset.setUpdateTime(now());
        return asset;
    }

    private void fillAssetFieldCount(User loginUser, Asset asset) {
        if (asset == null || asset.getFieldCount() != null) {
            return;
        }
        try {
            DataSource dataSource = getDatasource(loginUser, asset.getDatasourceId());
            asset.setFieldCount(queryFields(dataSource, asset.getDatabase(), asset.getSchema(), asset.getTableName()).size());
        } catch (Exception ex) {
            log.warn("Resolve data governance asset field count failed, assetId:{}.", asset == null ? null : asset.getId(), ex);
        }
    }

    private List<Field> queryFields(DataSource dataSource, String database, String schema, String tableName) {
        BaseConnectionParam connectionParam = buildConnectionParam(dataSource);
        Connection connection = DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
        if (connection == null) {
            throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
        }
        ResultSet columnRs = null;
        ResultSet primaryKeyRs = null;
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            Set<String> primaryKeys = new LinkedHashSet<>();
            String catalog = getCatalog(dataSource.getType(), database);
            String schemaPattern = getSchemaPattern(dataSource.getType(), schema, connectionParam);
            primaryKeyRs = metaData.getPrimaryKeys(catalog, schemaPattern, tableName);
            while (primaryKeyRs != null && primaryKeyRs.next()) {
                primaryKeys.add(primaryKeyRs.getString("COLUMN_NAME"));
            }
            List<Field> fields = new ArrayList<>();
            columnRs = metaData.getColumns(catalog, schemaPattern, tableName, "%");
            while (columnRs != null && columnRs.next()) {
                Field field = new Field();
                field.setName(columnRs.getString("COLUMN_NAME"));
                field.setType(columnRs.getString("TYPE_NAME"));
                field.setNullable(columnRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                field.setPrimaryKey(primaryKeys.contains(field.getName()));
                field.setComment(StringUtils.defaultString(columnRs.getString("REMARKS")));
                field.setSensitiveTag(guessSensitiveTag(field.getName()));
                fields.add(field);
            }
            return fields;
        } catch (Exception ex) {
            log.error("Query data governance fields failed, asset:{}.", tableName, ex);
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        } finally {
            closeResult(columnRs);
            closeResult(primaryKeyRs);
            releaseConnection(connection);
        }
    }

    private TrialRunResult executeTrialSql(DataSource dataSource, AssetRef assetRef, String sql) {
        BaseConnectionParam connectionParam = buildConnectionParam(dataSource);
        Connection connection = DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
        if (connection == null) {
            throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
        }
        try (Statement statement = connection.createStatement()) {
            if (isMysqlLike(dataSource.getType())) {
                connection.setCatalog(assetRef.database);
            } else if (dataSource.getType() == DbType.POSTGRESQL) {
                connection.setSchema(StringUtils.defaultIfBlank(assetRef.schema, DEFAULT_SCHEMA));
            }
            statement.setQueryTimeout(30);
            try (ResultSet rs = statement.executeQuery(sql)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int abnormalIndex = findColumnIndex(metaData, "abnormal_count");
                int rateIndex = findColumnIndex(metaData, "abnormal_rate");
                if (abnormalIndex < 1) {
                    throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
                }
                TrialRunResult result = new TrialRunResult();
                if (rs.next()) {
                    long abnormalCount = rs.getLong(abnormalIndex);
                    double abnormalRate = rateIndex > 0 ? rs.getDouble(rateIndex) : 0D;
                    result.setAbnormalCount(abnormalCount);
                    result.setAbnormalRate(abnormalRate);
                    result.setPassed(abnormalCount <= 0);
                    result.setMessage(abnormalCount <= 0 ? "试运行通过，未发现异常数据。" : "试运行发现异常数据。");
                } else {
                    result.setAbnormalCount(0L);
                    result.setAbnormalRate(0D);
                    result.setPassed(Boolean.TRUE);
                    result.setMessage("试运行完成，SQL 未返回异常。");
                }
                result.setExecutedAt(now());
                return result;
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Trial run data governance rule failed, assetId:{}.", assetRef.assetId, ex);
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        } finally {
            releaseConnection(connection);
        }
    }

    private void createOrUpdateIssue(String assetId, QualityRule rule, TrialRunResult result) {
        Issue issue = new Issue();
        issue.setId("issue-" + rule.getId());
        issue.setAssetId(assetId);
        issue.setRuleId(rule.getId());
        issue.setTitle(rule.getName() + " 检测失败");
        issue.setSeverity(resolveIssueSeverity(assetId, rule));
        issue.setStatus("OPEN");
        issue.setAbnormalCount(result.getAbnormalCount());
        issue.setDiscoveredAt(result.getExecutedAt());
        issue.setUpdatedAt(result.getExecutedAt());
        dataGovernanceStore.saveIssue(assetId, issue);
    }

    private void closeRuleIssues(String assetId, QualityRule rule, String updatedAt) {
        for (Issue issue : dataGovernanceStore.getIssues(assetId)) {
            if (StringUtils.equals(issue.getRuleId(), rule.getId())
                    && !StringUtils.equals(issue.getStatus(), "RESOLVED")) {
                issue.setStatus("RESOLVED");
                issue.setUpdatedAt(updatedAt);
                dataGovernanceStore.saveIssue(assetId, issue);
            }
        }
    }

    private String resolveIssueSeverity(String assetId, QualityRule rule) {
        if (!Boolean.TRUE.equals(rule.getEscalateIssue())) {
            return rule.getSeverity();
        }
        boolean hasOpenIssue = dataGovernanceStore.getIssues(assetId).stream()
                .anyMatch(issue -> StringUtils.equals(issue.getRuleId(), rule.getId())
                        && !StringUtils.equals(issue.getStatus(), "RESOLVED"));
        if (!hasOpenIssue) {
            return rule.getSeverity();
        }
        if (StringUtils.equals(rule.getSeverity(), "LOW")) {
            return "MEDIUM";
        }
        if (StringUtils.equals(rule.getSeverity(), "MEDIUM")) {
            return "HIGH";
        }
        return rule.getSeverity();
    }

    private String buildRuleWhere(QualityRuleRequest request, DbType dbType, String table) {
        String type = StringUtils.upperCase(request.getType(), Locale.ROOT);
        if (StringUtils.equals(type, "CUSTOM_SQL")) {
            return "1 = 0";
        }
        if (StringUtils.isBlank(request.getFieldName())) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        String field = quote(dbType, request.getFieldName());
        Map<String, Object> conditions =
                request.getConditions() == null ? Collections.emptyMap() : request.getConditions();
        String castAsText = isMysqlLike(dbType)
                ? "TRIM(CAST(" + field + " AS CHAR))"
                : "TRIM(CAST(" + field + " AS VARCHAR))";
        switch (type) {
            case "NOT_NULL":
                if (dbType == DbType.ORACLE) {
                    return field + " IS NULL";
                }
                return field + " IS NULL OR " + castAsText + " = ''";
            case "UNIQUE":
                return field + " IN (SELECT " + field + " FROM " + table + " GROUP BY " + field
                        + " HAVING COUNT(1) > 1)";
            case "RANGE":
                return field + " < " + valueOrDefault(conditions.get("min"), "0") + " OR "
                        + field + " > " + valueOrDefault(conditions.get("max"), "999999999");
            case "ENUM":
                return field + " NOT IN (" + enumValues(conditions.get("values")) + ")";
            case "REGEX":
                String pattern = StringUtils.defaultIfBlank(String.valueOf(conditions.get("pattern")), ".*");
                if (isMysqlLike(dbType)) {
                    return field + " IS NULL OR " + field + " NOT REGEXP '" + pattern.replace("'", "''") + "'";
                }
                return field + " IS NULL OR " + field + " !~ '" + pattern.replace("'", "''") + "'";
            default:
                return field + " IS NULL";
        }
    }

    private String valueOrDefault(Object value, String defaultValue) {
        return value == null || StringUtils.isBlank(String.valueOf(value)) ? defaultValue : String.valueOf(value);
    }

    private String enumValues(Object values) {
        if (values == null) {
            return "'UNKNOWN'";
        }
        String text = String.valueOf(values);
        return java.util.Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(value -> "'" + value.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
    }

    private void validateReadonlySql(String sql) {
        String normalized = StringUtils.trimToEmpty(sql);
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (StringUtils.isBlank(normalized) || !lower.matches("^(select|with)\\b[\\s\\S]*")) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        if (lower.matches(
                "[\\s\\S]*\\b(insert|update|delete|drop|truncate|alter|create|grant|revoke|merge|replace|call)\\b[\\s\\S]*")) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        if (!lower.contains("abnormal_count")) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
    }

    private AssetRef parseAndCheckAsset(User loginUser, String assetId) {
        AssetRef assetRef = parseAssetId(assetId);
        getDatasource(loginUser, assetRef.datasourceId);
        return assetRef;
    }

    private AssetRef parseAssetId(String assetId) {
        if (StringUtils.isBlank(assetId)) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        String[] parts = assetId.split("\\|", -1);
        if (parts.length != 4) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        try {
            AssetRef assetRef = new AssetRef();
            assetRef.assetId = assetId;
            assetRef.datasourceId = Integer.parseInt(parts[0]);
            assetRef.database = parts[1];
            assetRef.schema = parts[2];
            assetRef.tableName = parts[3];
            validateIdentifier(assetRef.database);
            if (StringUtils.isNotBlank(assetRef.schema)) {
                validateIdentifier(assetRef.schema);
            }
            validateIdentifier(assetRef.tableName);
            return assetRef;
        } catch (NumberFormatException ex) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
    }

    private DataSource getDatasource(User loginUser, Integer datasourceId) {
        DataSource dataSource = dataSourceMapper.selectById(datasourceId);
        if (dataSource == null || !isSupported(dataSource)) {
            throw new ServiceException(Status.RESOURCE_NOT_EXIST);
        }
        if (!canReadDatasource(loginUser, datasourceId)) {
            throw new ServiceException(Status.USER_NO_OPERATION_PERM);
        }
        return dataSource;
    }

    private boolean canReadDatasource(User loginUser, Integer datasourceId) {
        return canOperatorPermissions(loginUser, new Object[]{datasourceId}, AuthorizationType.DATASOURCE,
                ApiFuncIdentificationConstant.DATASOURCE);
    }

    private boolean isSupported(DataSource dataSource) {
        return dataSource != null && isSupportedDatasourceType(dataSource.getType());
    }

    static boolean isSupportedDatasourceType(DbType dbType) {
        return dbType == DbType.MYSQL
                || dbType == DbType.DORIS
                || dbType == DbType.POSTGRESQL
                || dbType == DbType.ORACLE;
    }

    private boolean matchesKeyword(Asset asset, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return true;
        }
        String target = StringUtils.join(new String[]{
                asset.getTableName(), asset.getFullName(), asset.getDatasourceName(), asset.getOwner(),
                StringUtils.join(asset.getTags(), ",")
        }, " ").toLowerCase(Locale.ROOT);
        return target.contains(keyword.toLowerCase(Locale.ROOT));
    }

    private BaseConnectionParam buildConnectionParam(DataSource dataSource) {
        BaseConnectionParam connectionParam =
                (BaseConnectionParam) DataSourceUtils.buildConnectionParams(dataSource.getType(),
                        dataSource.getConnectionParams());
        if (connectionParam == null) {
            throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
        }
        return connectionParam;
    }

    private String getCatalog(DbType dbType, String database) {
        if (dbType == DbType.ORACLE) {
            return null;
        }
        return database;
    }

    private String getSchemaPattern(DbType dbType, String schema, BaseConnectionParam connectionParam) {
        if (dbType == DbType.POSTGRESQL) {
            return StringUtils.defaultIfBlank(schema, DEFAULT_SCHEMA);
        }
        if (dbType == DbType.ORACLE) {
            return StringUtils.upperCase(StringUtils.defaultIfBlank(schema, connectionParam.getUser()));
        }
        return null;
    }

    private String qualifiedTableName(DbType dbType, AssetRef assetRef) {
        if (isMysqlLike(dbType)) {
            return quote(dbType, assetRef.database) + "." + quote(dbType, assetRef.tableName);
        }
        return quote(dbType, getQualifiedSchema(dbType, assetRef.schema)) + "." + quote(dbType, assetRef.tableName);
    }

    private String getQualifiedSchema(DbType dbType, String schema) {
        if (dbType == DbType.POSTGRESQL) {
            return StringUtils.defaultIfBlank(schema, DEFAULT_SCHEMA);
        }
        return StringUtils.trimToEmpty(schema);
    }

    private String quote(DbType dbType, String identifier) {
        validateIdentifier(identifier);
        if (isMysqlLike(dbType)) {
            return "`" + identifier.replace("`", "``") + "`";
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private void validateIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier) || !identifier.matches("[A-Za-z0-9_.$-]+")) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
    }

    private String buildAssetId(Integer datasourceId, String database, String schema, String tableName) {
        return datasourceId + "|" + database + "|" + StringUtils.trimToEmpty(schema) + "|" + tableName;
    }

    private String buildFullName(DataSource dataSource, String database, String schema, String tableName) {
        List<String> parts = new ArrayList<>();
        parts.add(dataSource.getName());
        parts.add(database);
        if (StringUtils.isNotBlank(schema)) {
            parts.add(schema);
        }
        parts.add(tableName);
        return String.join(".", parts);
    }

    private String buildLineageAssetName(DataSource dataSource, String database, String schema, String tableName) {
        return buildFullName(dataSource, database, schema, tableName);
    }

    private List<org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.FieldMapping> normalizeFieldMappings(
                                                                                                                            SyncTaskLineageRequest request) {
        if (request.getFieldMappings() == null) {
            return new ArrayList<>();
        }
        return request.getFieldMappings().stream()
                .filter(mapping -> mapping != null
                        && StringUtils.isNotBlank(mapping.getSourceField())
                        && StringUtils.isNotBlank(mapping.getTargetField()))
                .collect(Collectors.toList());
    }

    private String normalizeSchema(DataSource dataSource, String schema) {
        if (dataSource.getType() == DbType.POSTGRESQL) {
            return StringUtils.defaultIfBlank(schema, DEFAULT_SCHEMA);
        }
        if (dataSource.getType() == DbType.ORACLE) {
            BaseConnectionParam connectionParam = buildConnectionParam(dataSource);
            return StringUtils.upperCase(StringUtils.defaultIfBlank(schema, connectionParam.getUser()));
        }
        return StringUtils.trimToEmpty(schema);
    }

    private boolean isSystemDatabase(DbType dbType, String database) {
        if (isMysqlLike(dbType)) {
            return StringUtils.equalsAnyIgnoreCase(database, "information_schema", "mysql", "performance_schema",
                    "sys", "__internal_schema");
        }
        return StringUtils.equalsAnyIgnoreCase(database, "template0", "template1");
    }

    private boolean isMysqlLike(DbType dbType) {
        return dbType == DbType.MYSQL || dbType == DbType.DORIS;
    }

    private String guessSensitiveTag(String fieldName) {
        String lower = StringUtils.defaultString(fieldName).toLowerCase(Locale.ROOT);
        if (lower.contains("phone") || lower.contains("mobile")) {
            return "手机号";
        }
        if (lower.contains("email")) {
            return "邮箱";
        }
        if (lower.contains("id_card") || lower.contains("identity")) {
            return "证件号";
        }
        return "";
    }

    private int findColumnIndex(ResultSetMetaData metaData, String name) throws Exception {
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (StringUtils.equalsIgnoreCase(metaData.getColumnLabel(i), name)) {
                return i;
            }
        }
        return -1;
    }

    private void closeResult(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception ex) {
                log.warn("Close result set failed.", ex);
            }
        }
    }

    private void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ex) {
                log.warn("Close connection failed.", ex);
            }
        }
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    private static final class AssetRef {

        private String assetId;
        private Integer datasourceId;
        private String database;
        private String schema;
        private String tableName;
    }
}
