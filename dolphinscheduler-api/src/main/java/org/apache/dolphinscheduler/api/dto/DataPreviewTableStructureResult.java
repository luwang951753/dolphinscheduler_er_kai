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

import java.util.List;

public class DataPreviewTableStructureResult {

    private TableSummary summary;

    private List<Column> columns;

    private List<Index> indexes;

    private List<String> constraints;

    private String ddl;

    public static class TableSummary {

        private String tableName;

        private String tableComment;

        private String database;

        private String schema;

        private String datasourceType;

        private String tableType;

        private String engine;

        private Integer fieldCount;

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getTableComment() {
            return tableComment;
        }

        public void setTableComment(String tableComment) {
            this.tableComment = tableComment;
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

        public String getDatasourceType() {
            return datasourceType;
        }

        public void setDatasourceType(String datasourceType) {
            this.datasourceType = datasourceType;
        }

        public String getTableType() {
            return tableType;
        }

        public void setTableType(String tableType) {
            this.tableType = tableType;
        }

        public String getEngine() {
            return engine;
        }

        public void setEngine(String engine) {
            this.engine = engine;
        }

        public Integer getFieldCount() {
            return fieldCount;
        }

        public void setFieldCount(Integer fieldCount) {
            this.fieldCount = fieldCount;
        }
    }

    public static class Column {

        private String name;

        private String type;

        private Integer length;

        private Integer scale;

        private Boolean nullable;

        private Boolean primaryKey;

        private String defaultValue;

        private String comment;

        private String indexName;

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

        public Integer getLength() {
            return length;
        }

        public void setLength(Integer length) {
            this.length = length;
        }

        public Integer getScale() {
            return scale;
        }

        public void setScale(Integer scale) {
            this.scale = scale;
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

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getIndexName() {
            return indexName;
        }

        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }
    }

    public static class Index {

        private String name;

        private String columnName;

        private Boolean unique;

        private String type;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public Boolean getUnique() {
            return unique;
        }

        public void setUnique(Boolean unique) {
            this.unique = unique;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public TableSummary getSummary() {
        return summary;
    }

    public void setSummary(TableSummary summary) {
        this.summary = summary;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<Index> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<Index> indexes) {
        this.indexes = indexes;
    }

    public List<String> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }

    public String getDdl() {
        return ddl;
    }

    public void setDdl(String ddl) {
        this.ddl = ddl;
    }
}
