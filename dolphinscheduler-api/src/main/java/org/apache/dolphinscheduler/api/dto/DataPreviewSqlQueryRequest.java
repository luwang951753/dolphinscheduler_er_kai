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

package org.apache.dolphinscheduler.api.dto;

public class DataPreviewSqlQueryRequest {

    private Integer datasourceId;

    private String database;

    private String schema;

    private String tableName;

    private String sql;

    private Integer pageSize;

    private Integer timeoutSeconds;

    private Boolean executeAll;

    public Integer getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(Integer datasourceId) {
        this.datasourceId = datasourceId;
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

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Boolean getExecuteAll() {
        return executeAll;
    }

    public void setExecuteAll(Boolean executeAll) {
        this.executeAll = executeAll;
    }
}
