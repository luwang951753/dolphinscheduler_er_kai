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

import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.FieldMapping;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.LineageNode;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SqlLineage;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SqlLineageEdge;
import org.apache.dolphinscheduler.api.service.DataGovernanceStore;
import org.apache.dolphinscheduler.api.service.SqlLineageParseService;
import org.apache.dolphinscheduler.api.service.WorkflowSqlLineageService;
import org.apache.dolphinscheduler.common.utils.DateUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.entity.TaskDefinitionLog;
import org.apache.dolphinscheduler.dao.mapper.DataSourceMapper;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.plugin.task.api.parameters.SqlParameters;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class WorkflowSqlLineageServiceImpl implements WorkflowSqlLineageService {

    private static final String TASK_TYPE_SQL = "SQL";
    private static final String LINEAGE_TYPE_TABLE = "TABLE";
    private static final String LINEAGE_TYPE_FIELD = "FIELD";
    private static final String STATUS_SAVED = "SAVED";

    @Autowired
    private SqlLineageParseService sqlLineageParseService;

    @Autowired
    private DataGovernanceStore dataGovernanceStore;

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerWorkflowSqlLineage(long workflowDefinitionCode,
                                           int workflowDefinitionVersion,
                                           String workflowDefinitionName,
                                           List<TaskDefinitionLog> taskDefinitionLogs) {
        String workflowPrefix = buildWorkflowPrefix(workflowDefinitionCode);
        dataGovernanceStore.deleteLineageBySyncTaskNamePrefix(workflowPrefix);
        if (CollectionUtils.isEmpty(taskDefinitionLogs)) {
            return;
        }
        for (TaskDefinitionLog taskDefinitionLog : taskDefinitionLogs) {
            if (taskDefinitionLog == null || !StringUtils.equalsIgnoreCase(TASK_TYPE_SQL,
                    taskDefinitionLog.getTaskType())) {
                continue;
            }
            registerTaskSqlLineage(workflowDefinitionCode, workflowDefinitionVersion, workflowDefinitionName,
                    taskDefinitionLog);
        }
    }

    private void registerTaskSqlLineage(long workflowDefinitionCode,
                                        int workflowDefinitionVersion,
                                        String workflowDefinitionName,
                                        TaskDefinitionLog taskDefinitionLog) {
        SqlParameters sqlParameters;
        try {
            sqlParameters = JSONUtils.parseObject(taskDefinitionLog.getTaskParams(), SqlParameters.class);
        } catch (RuntimeException ex) {
            log.warn("Parse SQL task params failed, workflowCode:{}, taskCode:{}, taskName:{}",
                    workflowDefinitionCode, taskDefinitionLog.getCode(), taskDefinitionLog.getName(), ex);
            return;
        }
        if (sqlParameters == null || StringUtils.isBlank(sqlParameters.getSql())
                || sqlParameters.getDatasource() <= 0) {
            return;
        }
        DataSource dataSource = dataSourceMapper.selectById(sqlParameters.getDatasource());
        if (dataSource == null || dataSource.getType() == null) {
            log.warn("Skip SQL lineage because datasource does not exist, datasourceId:{}, taskName:{}",
                    sqlParameters.getDatasource(), taskDefinitionLog.getName());
            return;
        }

        List<String> statements = splitSqlStatements(sqlParameters.getSql());
        for (int index = 0; index < statements.size(); index++) {
            String sql = statements.get(index);
            try {
                SqlLineage lineage = sqlLineageParseService.parse(sql);
                persistSqlLineage(dataSource, lineage, buildSyncTaskName(workflowDefinitionCode,
                        workflowDefinitionVersion, workflowDefinitionName, taskDefinitionLog, index, statements.size()));
            } catch (RuntimeException ex) {
                log.warn("Parse workflow SQL lineage failed, workflowCode:{}, taskCode:{}, taskName:{}, sql:{}",
                        workflowDefinitionCode, taskDefinitionLog.getCode(), taskDefinitionLog.getName(),
                        abbreviateSql(sql), ex);
            }
        }
    }

    private void persistSqlLineage(DataSource dataSource, SqlLineage lineage, String syncTaskName) {
        if (lineage == null || CollectionUtils.isEmpty(lineage.getEdges())) {
            return;
        }
        Map<String, List<SqlLineageEdge>> fieldEdgesByPair = lineage.getEdges().stream()
                .filter(edge -> StringUtils.equals(edge.getLineageType(), LINEAGE_TYPE_FIELD))
                .collect(Collectors.groupingBy(edge -> pairKey(edge.getSourceTable(), edge.getTargetTable()),
                        LinkedHashMap::new, Collectors.toList()));

        Map<String, List<SqlLineageEdge>> tableEdgesByTarget = lineage.getEdges().stream()
                .filter(edge -> StringUtils.equals(edge.getLineageType(), LINEAGE_TYPE_TABLE))
                .collect(Collectors.groupingBy(SqlLineageEdge::getTargetTable, LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<String, List<SqlLineageEdge>> entry : tableEdgesByTarget.entrySet()) {
            String targetTable = entry.getKey();
            SqlTableRef targetRef = toTableRef(dataSource, targetTable);
            LineageNode downstream = buildNode(dataSource, targetRef, "SQL目标表", syncTaskName,
                    collectFieldMappingsForTarget(fieldEdgesByPair, targetTable));
            List<LineageNode> upstreamNodes = new ArrayList<>();
            for (SqlLineageEdge tableEdge : entry.getValue()) {
                SqlTableRef sourceRef = toTableRef(dataSource, tableEdge.getSourceTable());
                upstreamNodes.add(buildNode(dataSource, sourceRef, "SQL来源表", syncTaskName,
                        toFieldMappings(fieldEdgesByPair.get(pairKey(tableEdge.getSourceTable(), targetTable)))));
            }
            dataGovernanceStore.replaceLineages(targetRef.assetId, upstreamNodes, downstream);
        }
    }

    private List<FieldMapping> collectFieldMappingsForTarget(Map<String, List<SqlLineageEdge>> fieldEdgesByPair,
                                                             String targetTable) {
        List<FieldMapping> mappings = new ArrayList<>();
        for (Map.Entry<String, List<SqlLineageEdge>> entry : fieldEdgesByPair.entrySet()) {
            if (StringUtils.endsWith(entry.getKey(), "->" + targetTable)) {
                mappings.addAll(toFieldMappings(entry.getValue()));
            }
        }
        return mappings;
    }

    private LineageNode buildNode(DataSource dataSource,
                                  SqlTableRef tableRef,
                                  String relationType,
                                  String syncTaskName,
                                  List<FieldMapping> fieldMappings) {
        LineageNode node = new LineageNode();
        node.setAssetId(tableRef.assetId);
        node.setAssetName(buildAssetName(dataSource, tableRef));
        node.setRelationType(relationType);
        node.setSyncTaskName(syncTaskName);
        node.setLastRunStatus(STATUS_SAVED);
        node.setLastRunTime(DateUtils.dateToString(new Date()));
        node.setFieldMappings(fieldMappings == null ? new ArrayList<>() : fieldMappings);
        return node;
    }

    private List<FieldMapping> toFieldMappings(List<SqlLineageEdge> edges) {
        if (CollectionUtils.isEmpty(edges)) {
            return new ArrayList<>();
        }
        List<FieldMapping> mappings = new ArrayList<>();
        for (SqlLineageEdge edge : edges) {
            if (StringUtils.isBlank(edge.getSourceColumn()) || StringUtils.isBlank(edge.getTargetColumn())) {
                continue;
            }
            FieldMapping mapping = new FieldMapping();
            mapping.setSourceField(edge.getSourceColumn());
            mapping.setTargetField(edge.getTargetColumn());
            boolean exists = mappings.stream().anyMatch(item -> StringUtils.equals(item.getSourceField(),
                    mapping.getSourceField()) && StringUtils.equals(item.getTargetField(), mapping.getTargetField()));
            if (!exists) {
                mappings.add(mapping);
            }
        }
        return mappings;
    }

    private SqlTableRef toTableRef(DataSource dataSource, String tableId) {
        String cleanTableId = stripQuote(tableId);
        String[] parts = StringUtils.split(cleanTableId, '.');
        BaseConnectionParam connectionParam = buildBaseConnectionParam(dataSource);
        String defaultDatabase = StringUtils.defaultIfBlank(connectionParam == null ? "" : connectionParam.getDatabase(),
                dataSource.getName());
        String defaultSchema = dataSource.getType() == DbType.ORACLE
                ? StringUtils.upperCase(StringUtils.defaultIfBlank(connectionParam == null ? "" : connectionParam.getUser(), ""))
                : "";

        SqlTableRef ref = new SqlTableRef();
        if (isDatabaseFirst(dataSource.getType())) {
            ref.database = parts.length >= 2 ? parts[parts.length - 2] : defaultDatabase;
            ref.schema = "";
            ref.table = parts.length == 0 ? cleanTableId : parts[parts.length - 1];
        } else {
            ref.database = defaultDatabase;
            ref.schema = parts.length >= 2 ? parts[parts.length - 2] : defaultSchema;
            ref.table = parts.length == 0 ? cleanTableId : parts[parts.length - 1];
        }
        ref.database = stripQuote(StringUtils.defaultIfBlank(ref.database, defaultDatabase));
        ref.schema = stripQuote(ref.schema);
        ref.table = stripQuote(ref.table);
        ref.assetId = dataSource.getId() + "|" + ref.database + "|" + StringUtils.trimToEmpty(ref.schema)
                + "|" + ref.table;
        return ref;
    }

    private BaseConnectionParam buildBaseConnectionParam(DataSource dataSource) {
        try {
            ConnectionParam connectionParam = DataSourceUtils.buildConnectionParams(dataSource.getType(),
                    dataSource.getConnectionParams());
            if (connectionParam instanceof BaseConnectionParam) {
                return (BaseConnectionParam) connectionParam;
            }
        } catch (RuntimeException ex) {
            log.warn("Build datasource connection params failed when registering SQL lineage, datasourceId:{}",
                    dataSource.getId(), ex);
        }
        return null;
    }

    private boolean isDatabaseFirst(DbType dbType) {
        return dbType == DbType.MYSQL || dbType == DbType.DORIS;
    }

    private String buildAssetName(DataSource dataSource, SqlTableRef tableRef) {
        List<String> parts = new ArrayList<>();
        parts.add(dataSource.getName());
        parts.add(tableRef.database);
        if (StringUtils.isNotBlank(tableRef.schema)) {
            parts.add(tableRef.schema);
        }
        parts.add(tableRef.table);
        return String.join(".", parts);
    }

    static List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        if (StringUtils.isBlank(sql)) {
            return statements;
        }
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\\' && !escaped) {
                escaped = true;
                current.append(ch);
                continue;
            }
            if (ch == '\'' && !escaped && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !escaped && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (ch == ';' && !inSingleQuote && !inDoubleQuote) {
                addStatement(statements, current);
                current.setLength(0);
                escaped = false;
                continue;
            }
            current.append(ch);
            escaped = false;
        }
        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = StringUtils.trimToEmpty(current.toString());
        if (StringUtils.isNotBlank(statement)) {
            statements.add(statement);
        }
    }

    private String pairKey(String sourceTable, String targetTable) {
        return sourceTable + "->" + targetTable;
    }

    private String buildWorkflowPrefix(long workflowDefinitionCode) {
        return "SQL工作流[" + workflowDefinitionCode + "]";
    }

    private String buildSyncTaskName(long workflowDefinitionCode,
                                     int workflowDefinitionVersion,
                                     String workflowDefinitionName,
                                     TaskDefinitionLog taskDefinitionLog,
                                     int statementIndex,
                                     int statementCount) {
        String suffix = statementCount > 1 ? "#" + (statementIndex + 1) : "";
        return buildWorkflowPrefix(workflowDefinitionCode)
                + " v" + workflowDefinitionVersion
                + " " + StringUtils.defaultIfBlank(workflowDefinitionName, "未命名工作流")
                + "/" + StringUtils.defaultIfBlank(taskDefinitionLog.getName(), "SQL任务")
                + suffix;
    }

    private String abbreviateSql(String sql) {
        return StringUtils.abbreviate(StringUtils.normalizeSpace(sql), 300);
    }

    private String stripQuote(String value) {
        return value == null ? "" : value.replace("`", "").replace("\"", "").trim();
    }

    private static class SqlTableRef {

        private String assetId;
        private String database;
        private String schema;
        private String table;
    }
}
