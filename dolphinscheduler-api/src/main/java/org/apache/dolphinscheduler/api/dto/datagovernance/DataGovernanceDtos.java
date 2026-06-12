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

package org.apache.dolphinscheduler.api.dto.datagovernance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DataGovernanceDtos {

    private DataGovernanceDtos() {
    }

    public static class Asset {

        private String id;
        private Integer datasourceId;
        private String datasourceName;
        private String datasourceType;
        private String database;
        private String schema;
        private String tableName;
        private String tableType;
        private String fullName;
        private String owner;
        private String description;
        private List<String> tags = new ArrayList<>();
        private String qualityStatus;
        private Integer fieldCount;
        private Integer ruleCount;
        private Integer issueCount;
        private String lastCheckTime;
        private String lastSyncTask;
        private String updateTime;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getDatasourceId() {
            return datasourceId;
        }

        public void setDatasourceId(Integer datasourceId) {
            this.datasourceId = datasourceId;
        }

        public String getDatasourceName() {
            return datasourceName;
        }

        public void setDatasourceName(String datasourceName) {
            this.datasourceName = datasourceName;
        }

        public String getDatasourceType() {
            return datasourceType;
        }

        public void setDatasourceType(String datasourceType) {
            this.datasourceType = datasourceType;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getTableType() {
            return tableType;
        }

        public void setTableType(String tableType) {
            this.tableType = tableType;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public String getQualityStatus() {
            return qualityStatus;
        }

        public void setQualityStatus(String qualityStatus) {
            this.qualityStatus = qualityStatus;
        }

        public Integer getFieldCount() {
            return fieldCount;
        }

        public void setFieldCount(Integer fieldCount) {
            this.fieldCount = fieldCount;
        }

        public Integer getRuleCount() {
            return ruleCount;
        }

        public void setRuleCount(Integer ruleCount) {
            this.ruleCount = ruleCount;
        }

        public Integer getIssueCount() {
            return issueCount;
        }

        public void setIssueCount(Integer issueCount) {
            this.issueCount = issueCount;
        }

        public String getLastCheckTime() {
            return lastCheckTime;
        }

        public void setLastCheckTime(String lastCheckTime) {
            this.lastCheckTime = lastCheckTime;
        }

        public String getLastSyncTask() {
            return lastSyncTask;
        }

        public void setLastSyncTask(String lastSyncTask) {
            this.lastSyncTask = lastSyncTask;
        }

        public String getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(String updateTime) {
            this.updateTime = updateTime;
        }
    }

    public static class Field {

        private String name;
        private String type;
        private Boolean nullable;
        private Boolean primaryKey;
        private String comment;
        private String sensitiveTag;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getNullable() {
            return nullable;
        }

        public void setNullable(Boolean nullable) {
            this.nullable = nullable;
        }

        public Boolean getPrimaryKey() {
            return primaryKey;
        }

        public void setPrimaryKey(Boolean primaryKey) {
            this.primaryKey = primaryKey;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getSensitiveTag() {
            return sensitiveTag;
        }

        public void setSensitiveTag(String sensitiveTag) {
            this.sensitiveTag = sensitiveTag;
        }
    }

    public static class MetadataRequest {

        private String owner;
        private String description;
        private List<String> tags = new ArrayList<>();

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

    public static class QualityRule {

        private String id;
        private String assetId;
        private String name;
        private String type;
        private String level;
        private String fieldName;
        private Map<String, Object> conditions;
        private String rangeCondition;
        private String samplePolicy;
        private String failureThreshold;
        private String severity;
        private String frequency;
        private Boolean enabled;
        private Boolean createIssue;
        private Boolean escalateIssue;
        private Boolean autoCloseIssue;
        private Boolean manualSql;
        private String sql;
        private String status;
        private String lastRunAt;
        private Long abnormalCount;
        private Double abnormalRate;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public Map<String, Object> getConditions() {
            return conditions;
        }

        public void setConditions(Map<String, Object> conditions) {
            this.conditions = conditions;
        }

        public String getRangeCondition() {
            return rangeCondition;
        }

        public void setRangeCondition(String rangeCondition) {
            this.rangeCondition = rangeCondition;
        }

        public String getSamplePolicy() {
            return samplePolicy;
        }

        public void setSamplePolicy(String samplePolicy) {
            this.samplePolicy = samplePolicy;
        }

        public String getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(String failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getFrequency() {
            return frequency;
        }

        public void setFrequency(String frequency) {
            this.frequency = frequency;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Boolean getCreateIssue() {
            return createIssue;
        }

        public void setCreateIssue(Boolean createIssue) {
            this.createIssue = createIssue;
        }

        public Boolean getEscalateIssue() {
            return escalateIssue;
        }

        public void setEscalateIssue(Boolean escalateIssue) {
            this.escalateIssue = escalateIssue;
        }

        public Boolean getAutoCloseIssue() {
            return autoCloseIssue;
        }

        public void setAutoCloseIssue(Boolean autoCloseIssue) {
            this.autoCloseIssue = autoCloseIssue;
        }

        public Boolean getManualSql() {
            return manualSql;
        }

        public void setManualSql(Boolean manualSql) {
            this.manualSql = manualSql;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getLastRunAt() {
            return lastRunAt;
        }

        public void setLastRunAt(String lastRunAt) {
            this.lastRunAt = lastRunAt;
        }

        public Long getAbnormalCount() {
            return abnormalCount;
        }

        public void setAbnormalCount(Long abnormalCount) {
            this.abnormalCount = abnormalCount;
        }

        public Double getAbnormalRate() {
            return abnormalRate;
        }

        public void setAbnormalRate(Double abnormalRate) {
            this.abnormalRate = abnormalRate;
        }
    }

    public static class QualityRuleRequest extends QualityRule {
    }

    public static class TrialRunRequest {

        private QualityRuleRequest rule;
        private String sql;

        public QualityRuleRequest getRule() {
            return rule;
        }

        public void setRule(QualityRuleRequest rule) {
            this.rule = rule;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }
    }

    public static class SyncTaskLineageRequest {

        private Integer sourceDatasourceId;
        private String sourceDatasourceName;
        private String sourceDatabase;
        private String sourceSchema;
        private String sourceTable;
        private Integer targetDatasourceId;
        private String targetDatasourceName;
        private String targetDatabase;
        private String targetSchema;
        private String targetTable;
        private String syncTaskName;
        private String lastRunStatus;
        private String lastRunTime;
        private List<FieldMapping> fieldMappings = new ArrayList<>();

        public Integer getSourceDatasourceId() {
            return sourceDatasourceId;
        }

        public void setSourceDatasourceId(Integer sourceDatasourceId) {
            this.sourceDatasourceId = sourceDatasourceId;
        }

        public String getSourceDatasourceName() {
            return sourceDatasourceName;
        }

        public void setSourceDatasourceName(String sourceDatasourceName) {
            this.sourceDatasourceName = sourceDatasourceName;
        }

        public String getSourceDatabase() {
            return sourceDatabase;
        }

        public void setSourceDatabase(String sourceDatabase) {
            this.sourceDatabase = sourceDatabase;
        }

        public String getSourceSchema() {
            return sourceSchema;
        }

        public void setSourceSchema(String sourceSchema) {
            this.sourceSchema = sourceSchema;
        }

        public String getSourceTable() {
            return sourceTable;
        }

        public void setSourceTable(String sourceTable) {
            this.sourceTable = sourceTable;
        }

        public Integer getTargetDatasourceId() {
            return targetDatasourceId;
        }

        public void setTargetDatasourceId(Integer targetDatasourceId) {
            this.targetDatasourceId = targetDatasourceId;
        }

        public String getTargetDatasourceName() {
            return targetDatasourceName;
        }

        public void setTargetDatasourceName(String targetDatasourceName) {
            this.targetDatasourceName = targetDatasourceName;
        }

        public String getTargetDatabase() {
            return targetDatabase;
        }

        public void setTargetDatabase(String targetDatabase) {
            this.targetDatabase = targetDatabase;
        }

        public String getTargetSchema() {
            return targetSchema;
        }

        public void setTargetSchema(String targetSchema) {
            this.targetSchema = targetSchema;
        }

        public String getTargetTable() {
            return targetTable;
        }

        public void setTargetTable(String targetTable) {
            this.targetTable = targetTable;
        }

        public String getSyncTaskName() {
            return syncTaskName;
        }

        public void setSyncTaskName(String syncTaskName) {
            this.syncTaskName = syncTaskName;
        }

        public String getLastRunStatus() {
            return lastRunStatus;
        }

        public void setLastRunStatus(String lastRunStatus) {
            this.lastRunStatus = lastRunStatus;
        }

        public String getLastRunTime() {
            return lastRunTime;
        }

        public void setLastRunTime(String lastRunTime) {
            this.lastRunTime = lastRunTime;
        }

        public List<FieldMapping> getFieldMappings() {
            return fieldMappings;
        }

        public void setFieldMappings(List<FieldMapping> fieldMappings) {
            this.fieldMappings = fieldMappings;
        }
    }

    public static class FieldMapping {

        private String sourceField;
        private String targetField;

        public String getSourceField() {
            return sourceField;
        }

        public void setSourceField(String sourceField) {
            this.sourceField = sourceField;
        }

        public String getTargetField() {
            return targetField;
        }

        public void setTargetField(String targetField) {
            this.targetField = targetField;
        }
    }

    public static class TrialRunResult {

        private Boolean passed;
        private Long abnormalCount;
        private Double abnormalRate;
        private String executedAt;
        private String message;
        private List<Map<String, Object>> samples = new ArrayList<>();

        public Boolean getPassed() {
            return passed;
        }

        public void setPassed(Boolean passed) {
            this.passed = passed;
        }

        public Long getAbnormalCount() {
            return abnormalCount;
        }

        public void setAbnormalCount(Long abnormalCount) {
            this.abnormalCount = abnormalCount;
        }

        public Double getAbnormalRate() {
            return abnormalRate;
        }

        public void setAbnormalRate(Double abnormalRate) {
            this.abnormalRate = abnormalRate;
        }

        public String getExecutedAt() {
            return executedAt;
        }

        public void setExecutedAt(String executedAt) {
            this.executedAt = executedAt;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<Map<String, Object>> getSamples() {
            return samples;
        }

        public void setSamples(List<Map<String, Object>> samples) {
            this.samples = samples;
        }
    }

    public static class Lineage {

        private List<LineageNode> upstream = new ArrayList<>();
        private List<LineageNode> downstream = new ArrayList<>();

        public List<LineageNode> getUpstream() {
            return upstream;
        }

        public void setUpstream(List<LineageNode> upstream) {
            this.upstream = upstream;
        }

        public List<LineageNode> getDownstream() {
            return downstream;
        }

        public void setDownstream(List<LineageNode> downstream) {
            this.downstream = downstream;
        }
    }

    public static class LineageRepairRequest {

        private String syncTaskName;

        public String getSyncTaskName() {
            return syncTaskName;
        }

        public void setSyncTaskName(String syncTaskName) {
            this.syncTaskName = syncTaskName;
        }
    }

    public static class LineageRepairResult {

        private String syncTaskName;
        private String repairedStatus;
        private String repairedAt;
        private Integer workflowInstanceId;
        private Integer repairedRows;
        private String message;

        public String getSyncTaskName() {
            return syncTaskName;
        }

        public void setSyncTaskName(String syncTaskName) {
            this.syncTaskName = syncTaskName;
        }

        public String getRepairedStatus() {
            return repairedStatus;
        }

        public void setRepairedStatus(String repairedStatus) {
            this.repairedStatus = repairedStatus;
        }

        public String getRepairedAt() {
            return repairedAt;
        }

        public void setRepairedAt(String repairedAt) {
            this.repairedAt = repairedAt;
        }

        public Integer getWorkflowInstanceId() {
            return workflowInstanceId;
        }

        public void setWorkflowInstanceId(Integer workflowInstanceId) {
            this.workflowInstanceId = workflowInstanceId;
        }

        public Integer getRepairedRows() {
            return repairedRows;
        }

        public void setRepairedRows(Integer repairedRows) {
            this.repairedRows = repairedRows;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class LineageNode {

        private String assetId;
        private String assetName;
        private String relationType;
        private String syncTaskName;
        private String lastRunStatus;
        private String lastRunTime;
        private List<FieldMapping> fieldMappings = new ArrayList<>();

        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }

        public String getAssetName() {
            return assetName;
        }

        public void setAssetName(String assetName) {
            this.assetName = assetName;
        }

        public String getRelationType() {
            return relationType;
        }

        public void setRelationType(String relationType) {
            this.relationType = relationType;
        }

        public String getSyncTaskName() {
            return syncTaskName;
        }

        public void setSyncTaskName(String syncTaskName) {
            this.syncTaskName = syncTaskName;
        }

        public String getLastRunStatus() {
            return lastRunStatus;
        }

        public void setLastRunStatus(String lastRunStatus) {
            this.lastRunStatus = lastRunStatus;
        }

        public String getLastRunTime() {
            return lastRunTime;
        }

        public void setLastRunTime(String lastRunTime) {
            this.lastRunTime = lastRunTime;
        }

        public List<FieldMapping> getFieldMappings() {
            return fieldMappings;
        }

        public void setFieldMappings(List<FieldMapping> fieldMappings) {
            this.fieldMappings = fieldMappings;
        }
    }

    public static class Issue {

        private String id;
        private String assetId;
        private String ruleId;
        private String title;
        private String severity;
        private String status;
        private Long abnormalCount;
        private String discoveredAt;
        private String updatedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }

        public String getRuleId() {
            return ruleId;
        }

        public void setRuleId(String ruleId) {
            this.ruleId = ruleId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getAbnormalCount() {
            return abnormalCount;
        }

        public void setAbnormalCount(Long abnormalCount) {
            this.abnormalCount = abnormalCount;
        }

        public String getDiscoveredAt() {
            return discoveredAt;
        }

        public void setDiscoveredAt(String discoveredAt) {
            this.discoveredAt = discoveredAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class IssueStatusRequest {

        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class SqlLineageParseRequest {

        private String sql;

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }
    }

    public static class SqlLineage {

        private List<SqlLineageTable> tables = new ArrayList<>();
        private List<SqlLineageEdge> edges = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public List<SqlLineageTable> getTables() {
            return tables;
        }

        public void setTables(List<SqlLineageTable> tables) {
            this.tables = tables;
        }

        public List<SqlLineageEdge> getEdges() {
            return edges;
        }

        public void setEdges(List<SqlLineageEdge> edges) {
            this.edges = edges;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }
    }

    public static class SqlLineageTable {

        private String id;
        private String name;
        private String schema;
        private List<SqlLineageColumn> columns = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public List<SqlLineageColumn> getColumns() {
            return columns;
        }

        public void setColumns(List<SqlLineageColumn> columns) {
            this.columns = columns;
        }
    }

    public static class SqlLineageColumn {

        private String name;
        private String type;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class SqlLineageEdge {

        private String sourceTable;
        private String sourceColumn;
        private String targetTable;
        private String targetColumn;
        private String lineageType;

        public String getSourceTable() {
            return sourceTable;
        }

        public void setSourceTable(String sourceTable) {
            this.sourceTable = sourceTable;
        }

        public String getSourceColumn() {
            return sourceColumn;
        }

        public void setSourceColumn(String sourceColumn) {
            this.sourceColumn = sourceColumn;
        }

        public String getTargetTable() {
            return targetTable;
        }

        public void setTargetTable(String targetTable) {
            this.targetTable = targetTable;
        }

        public String getTargetColumn() {
            return targetColumn;
        }

        public void setTargetColumn(String targetColumn) {
            this.targetColumn = targetColumn;
        }

        public String getLineageType() {
            return lineageType;
        }

        public void setLineageType(String lineageType) {
            this.lineageType = lineageType;
        }
    }
}
