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

import org.apache.dolphinscheduler.api.constants.ApiFuncIdentificationConstant;
import org.apache.dolphinscheduler.api.dto.DataPreviewQueryResult;
import org.apache.dolphinscheduler.api.dto.DataPreviewSqlQueryRequest;
import org.apache.dolphinscheduler.api.dto.DataPreviewTableStructureResult;
import org.apache.dolphinscheduler.api.dto.DatasourceColumnDto;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.api.service.impl.BaseServiceImpl;
import org.apache.dolphinscheduler.common.enums.AuthorizationType;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.DataSourceMapper;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DataPreviewQueryService extends BaseServiceImpl {

    private static final String TABLE = "TABLE";
    private static final String VIEW = "VIEW";
    private static final String[] TABLE_TYPES = new String[]{TABLE, VIEW};
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final int DATA_PREVIEW_DEFAULT_PAGE_SIZE = 50;
    private static final int DATA_PREVIEW_MAX_PAGE_SIZE = 200;
    private static final int DATA_PREVIEW_SQL_MAX_LENGTH = 20000;
    private static final int DATA_PREVIEW_SQL_DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DATA_PREVIEW_SQL_MAX_TIMEOUT_SECONDS = 60;
    private static final Charset WINDOWS_1252 = Charset.forName("Windows-1252");
    private static final DateTimeFormatter DATA_PREVIEW_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private DataSourceMapper dataSourceMapper;

    public DataPreviewTableStructureResult queryTableStructure(User loginUser,
                                                               Integer datasourceId,
                                                               String database,
                                                               String schema,
                                                               String tableName) {
        validatePreviewScope(loginUser, datasourceId, database, schema, tableName);
        DataSource dataSource = getSupportedPreviewDataSource(loginUser, datasourceId);
        BaseConnectionParam connectionParam = getPreviewConnectionParam(dataSource);
        Connection connection = DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
        if (connection == null) {
            throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
        }

        ResultSet tableRs = null;
        ResultSet columnRs = null;
        ResultSet primaryKeyRs = null;
        ResultSet indexRs = null;
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String schemaPattern = getDbSchemaPattern(dataSource.getType(), schema);
            Set<String> primaryKeys = new LinkedHashSet<>();
            primaryKeyRs = metaData.getPrimaryKeys(database, schemaPattern, tableName);
            while (primaryKeyRs != null && primaryKeyRs.next()) {
                primaryKeys.add(primaryKeyRs.getString(COLUMN_NAME));
            }

            Map<String, DataPreviewTableStructureResult.Index> indexByNameAndColumn = new LinkedHashMap<>();
            Map<String, String> firstIndexByColumn = new LinkedHashMap<>();
            indexRs = metaData.getIndexInfo(database, schemaPattern, tableName, false, false);
            while (indexRs != null && indexRs.next()) {
                String indexName = indexRs.getString("INDEX_NAME");
                String columnName = indexRs.getString(COLUMN_NAME);
                if (StringUtils.isBlank(indexName) || StringUtils.isBlank(columnName)) {
                    continue;
                }
                DataPreviewTableStructureResult.Index index = new DataPreviewTableStructureResult.Index();
                index.setName(indexName);
                index.setColumnName(columnName);
                index.setUnique(!indexRs.getBoolean("NON_UNIQUE"));
                index.setType(String.valueOf(indexRs.getShort("TYPE")));
                indexByNameAndColumn.put(indexName + ":" + columnName, index);
                firstIndexByColumn.putIfAbsent(columnName, indexName);
            }

            List<DataPreviewTableStructureResult.Column> columns = new ArrayList<>();
            columnRs = metaData.getColumns(database, schemaPattern, tableName, "%");
            while (columnRs != null && columnRs.next()) {
                DataPreviewTableStructureResult.Column column = new DataPreviewTableStructureResult.Column();
                String columnName = columnRs.getString(COLUMN_NAME);
                column.setName(columnName);
                column.setType(columnRs.getString("TYPE_NAME"));
                column.setLength(columnRs.getInt("COLUMN_SIZE"));
                column.setScale(columnRs.getInt("DECIMAL_DIGITS"));
                column.setNullable(columnRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setPrimaryKey(primaryKeys.contains(columnName));
                column.setDefaultValue(columnRs.getString("COLUMN_DEF"));
                column.setComment(normalizeMetadataText(columnRs.getString("REMARKS")));
                column.setIndexName(firstIndexByColumn.get(columnName));
                columns.add(column);
            }

            DataPreviewTableStructureResult.TableSummary summary = new DataPreviewTableStructureResult.TableSummary();
            summary.setTableName(tableName);
            summary.setDatabase(database);
            summary.setSchema(StringUtils.trimToEmpty(schema));
            summary.setDatasourceType(dataSource.getType().name());
            summary.setFieldCount(columns.size());
            tableRs = metaData.getTables(database, schemaPattern, tableName, TABLE_TYPES);
            if (tableRs != null && tableRs.next()) {
                summary.setTableType(tableRs.getString("TABLE_TYPE"));
                summary.setTableComment(normalizeMetadataText(tableRs.getString("REMARKS")));
            }
            if (StringUtils.isBlank(summary.getTableComment())) {
                summary.setTableComment("");
            }
            summary.setEngine(dataSource.getType() == DbType.MYSQL ? "InnoDB / metadata" : "heap / metadata");

            DataPreviewTableStructureResult result = new DataPreviewTableStructureResult();
            result.setSummary(summary);
            result.setColumns(columns);
            result.setIndexes(new ArrayList<>(indexByNameAndColumn.values()));
            result.setConstraints(primaryKeys.isEmpty()
                    ? Collections.emptyList()
                    : Collections.singletonList("PRIMARY KEY (" + String.join(", ", primaryKeys) + ")"));
            result.setDdl(buildPreviewDdl(dataSource.getType(), tableName, summary.getTableComment(), columns, primaryKeys));
            return result;
        } catch (Exception ex) {
            log.error("Query data preview table structure error, datasourceId:{} table:{}.", datasourceId, tableName, ex);
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        } finally {
            closeResult(tableRs);
            closeResult(columnRs);
            closeResult(primaryKeyRs);
            closeResult(indexRs);
            releaseConnection(connection);
        }
    }

    public DataPreviewQueryResult executePreviewSql(User loginUser, DataPreviewSqlQueryRequest request) {
        return executePreviewSqlInternal(loginUser, request, false);
    }

    public DataPreviewQueryResult explainPreviewSql(User loginUser, DataPreviewSqlQueryRequest request) {
        return executePreviewSqlInternal(loginUser, request, true);
    }

    private DataPreviewQueryResult executePreviewSqlInternal(User loginUser,
                                                             DataPreviewSqlQueryRequest request,
                                                             boolean explain) {
        long start = System.currentTimeMillis();
        validatePreviewSqlRequest(request);
        DataSource dataSource = getSupportedPreviewDataSource(loginUser, request.getDatasourceId());
        BaseConnectionParam connectionParam = getPreviewConnectionParam(dataSource);
        int pageSize = normalizePreviewSqlPageSize(request.getPageSize());
        int timeoutSeconds = normalizePreviewSqlTimeout(request.getTimeoutSeconds());
        List<String> statements = splitPreviewSqlStatements(request.getSql());
        if (statements.size() > 1 && !Boolean.TRUE.equals(request.getExecuteAll())) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        List<String> warnings = new ArrayList<>();
        if (statements.size() > 1) {
            warnings.add("已执行多条只读语句，结果区展示最后一条语句的结果。");
        }

        Connection connection = DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
        if (connection == null) {
            throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
        }
        try {
            applyPreviewSqlConnectionContext(connection, dataSource.getType(), request.getDatabase(), request.getSchema());
            DataPreviewQueryResult lastResult = null;
            for (String rawStatement : statements) {
                String statementSql = normalizePreviewReadonlySql(rawStatement);
                if (explain && !StringUtils.startsWithIgnoreCase(statementSql, "EXPLAIN")) {
                    statementSql = "EXPLAIN " + statementSql;
                }
                String executableSql = appendReadonlySqlLimit(dataSource.getType(), statementSql, pageSize);
                lastResult = executeReadonlySql(connection, executableSql, timeoutSeconds, pageSize);
            }
            if (lastResult == null) {
                throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
            }
            lastResult.setElapsedMs(System.currentTimeMillis() - start);
            lastResult.setExecutedAt(LocalDateTime.now().format(DATA_PREVIEW_TIME_FORMATTER));
            lastResult.setWarnings(warnings);
            return lastResult;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Execute data preview SQL error, datasourceId:{}.", request.getDatasourceId(), ex);
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        } finally {
            releaseConnection(connection);
        }
    }

    private void validatePreviewScope(User loginUser,
                                      Integer datasourceId,
                                      String database,
                                      String schema,
                                      String tableName) {
        if (datasourceId == null || StringUtils.isBlank(database) || StringUtils.isBlank(tableName)) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        validateIdentifier(database);
        validateIdentifier(tableName);
        if (StringUtils.isNotBlank(schema)) {
            validateIdentifier(schema);
        }
        getSupportedPreviewDataSource(loginUser, datasourceId);
    }

    private void validatePreviewSqlRequest(DataPreviewSqlQueryRequest request) {
        if (request == null || request.getDatasourceId() == null || StringUtils.isBlank(request.getDatabase())
                || StringUtils.isBlank(request.getSql())) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        validateIdentifier(request.getDatabase());
        if (StringUtils.isNotBlank(request.getSchema())) {
            validateIdentifier(request.getSchema());
        }
        if (StringUtils.isNotBlank(request.getTableName())) {
            validateIdentifier(request.getTableName());
        }
        if (StringUtils.length(request.getSql()) > DATA_PREVIEW_SQL_MAX_LENGTH) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
    }

    private DataSource getSupportedPreviewDataSource(User loginUser, Integer datasourceId) {
        DataSource dataSource = dataSourceMapper.selectById(datasourceId);
        if (dataSource == null) {
            throw new ServiceException(Status.RESOURCE_NOT_EXIST);
        }
        if (dataSource.getType() != DbType.MYSQL && dataSource.getType() != DbType.POSTGRESQL) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        if (!canOperatorPermissions(loginUser, new Object[]{datasourceId}, AuthorizationType.DATASOURCE,
                ApiFuncIdentificationConstant.DATASOURCE)) {
            throw new ServiceException(Status.USER_NO_OPERATION_PERM);
        }
        return dataSource;
    }

    private BaseConnectionParam getPreviewConnectionParam(DataSource dataSource) {
        BaseConnectionParam connectionParam =
                (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                        dataSource.getType(),
                        dataSource.getConnectionParams());
        if (connectionParam == null) {
            throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
        }
        return connectionParam;
    }

    private void applyPreviewSqlConnectionContext(Connection connection,
                                                  DbType dbType,
                                                  String database,
                                                  String schema) {
        try {
            if (dbType == DbType.MYSQL && StringUtils.isNotBlank(database)) {
                connection.setCatalog(database);
            }
            if (dbType == DbType.POSTGRESQL && StringUtils.isNotBlank(schema)) {
                connection.setSchema(schema);
            }
        } catch (Exception ex) {
            log.warn("Apply data preview SQL connection context failed, database:{} schema:{}.", database, schema, ex);
        }
    }

    private int normalizePreviewSqlPageSize(Integer pageSize) {
        if (pageSize == null) {
            return DATA_PREVIEW_DEFAULT_PAGE_SIZE;
        }
        if (pageSize < 1 || pageSize > DATA_PREVIEW_MAX_PAGE_SIZE) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        return pageSize;
    }

    private int normalizePreviewSqlTimeout(Integer timeoutSeconds) {
        if (timeoutSeconds == null) {
            return DATA_PREVIEW_SQL_DEFAULT_TIMEOUT_SECONDS;
        }
        if (timeoutSeconds < 1 || timeoutSeconds > DATA_PREVIEW_SQL_MAX_TIMEOUT_SECONDS) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        return timeoutSeconds;
    }

    private List<String> splitPreviewSqlStatements(String sql) {
        String withoutComments = sql.replaceAll("(?m)--.*$", "");
        List<String> statements = new ArrayList<>();
        for (String statement : withoutComments.split(";")) {
            String trimmed = StringUtils.trimToEmpty(statement);
            if (StringUtils.isNotBlank(trimmed)) {
                statements.add(trimmed);
            }
        }
        if (statements.isEmpty()) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        return statements;
    }

    private String normalizePreviewReadonlySql(String sql) {
        String normalized = StringUtils.trimToEmpty(sql);
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (!lower.matches("^(select|with|explain)\\b[\\s\\S]*")) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        if (lower.matches("[\\s\\S]*\\b(insert|update|delete|drop|truncate|alter|create|grant|revoke|merge|replace|call)\\b[\\s\\S]*")) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        return normalized;
    }

    private String appendReadonlySqlLimit(DbType dbType, String sql, int pageSize) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (StringUtils.startsWithIgnoreCase(sql, "EXPLAIN")) {
            return sql;
        }
        if (lower.matches("[\\s\\S]*\\blimit\\s+\\d+[\\s\\S]*")) {
            return sql;
        }
        if (dbType == DbType.MYSQL || dbType == DbType.POSTGRESQL) {
            return sql + " LIMIT " + pageSize;
        }
        throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
    }

    private DataPreviewQueryResult executeReadonlySql(Connection connection,
                                                      String sql,
                                                      int timeoutSeconds,
                                                      int pageSize) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<DatasourceColumnDto> columns = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(timeoutSeconds);
            statement.setMaxRows(pageSize);
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(new DatasourceColumnDto(
                            metaData.getColumnLabel(i),
                            metaData.getColumnTypeName(i),
                            metaData.isNullable(i) == ResultSetMetaData.columnNullable,
                            false,
                            ""));
                }
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), normalizePreviewCellValue(resultSet.getObject(i)));
                    }
                    rows.add(row);
                }
            }
        }
        DataPreviewQueryResult result = new DataPreviewQueryResult();
        result.setColumns(columns);
        result.setRows(rows);
        result.setPageNo(1);
        result.setPageSize(pageSize);
        result.setRowCount(rows.size());
        result.setWarnings(Collections.emptyList());
        return result;
    }

    private String getDbSchemaPattern(DbType dbType, String schema) {
        if (dbType == DbType.POSTGRESQL && StringUtils.isNotBlank(schema)) {
            return schema;
        }
        return null;
    }

    private void validateIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier) || !identifier.matches("[A-Za-z0-9_.$-]+")) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
    }

    private String quoteIdentifier(DbType dbType, String identifier) {
        validateIdentifier(identifier);
        if (dbType == DbType.MYSQL) {
            return "`" + identifier.replace("`", "``") + "`";
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private String buildPreviewDdl(DbType dbType,
                                   String tableName,
                                   String tableComment,
                                   List<DataPreviewTableStructureResult.Column> columns,
                                   Set<String> primaryKeys) {
        List<String> lines = new ArrayList<>();
        for (DataPreviewTableStructureResult.Column column : columns) {
            StringBuilder line = new StringBuilder();
            line.append("  ").append(quoteIdentifier(dbType, column.getName())).append(" ");
            line.append(StringUtils.upperCase(StringUtils.defaultString(column.getType()), Locale.ROOT));
            if (column.getLength() != null && column.getLength() > 0) {
                line.append("(").append(column.getLength());
                if (column.getScale() != null && column.getScale() > 0) {
                    line.append(",").append(column.getScale());
                }
                line.append(")");
            }
            line.append(Boolean.TRUE.equals(column.getNullable()) ? " NULL" : " NOT NULL");
            if (StringUtils.isNotBlank(column.getDefaultValue())) {
                line.append(" DEFAULT ").append(column.getDefaultValue());
            }
            if (StringUtils.isNotBlank(column.getComment()) && dbType == DbType.MYSQL) {
                line.append(" COMMENT '").append(column.getComment().replace("'", "''")).append("'");
            }
            lines.add(line.toString());
        }
        if (!primaryKeys.isEmpty()) {
            lines.add("  PRIMARY KEY (" + primaryKeys.stream()
                    .map(column -> quoteIdentifier(dbType, column))
                    .collect(Collectors.joining(", ")) + ")");
        }
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE ").append(quoteIdentifier(dbType, tableName)).append(" (\n");
        ddl.append(String.join(",\n", lines));
        ddl.append("\n)");
        if (dbType == DbType.MYSQL) {
            ddl.append(" ENGINE=InnoDB");
            if (StringUtils.isNotBlank(tableComment)) {
                ddl.append(" COMMENT='").append(tableComment.replace("'", "''")).append("'");
            }
        }
        ddl.append(";");
        return ddl.toString();
    }

    private Object normalizePreviewCellValue(Object value) {
        if (!(value instanceof String)) {
            return value;
        }
        return normalizeMetadataText((String) value);
    }

    private String normalizeMetadataText(String rawValue) {
        if (StringUtils.isBlank(rawValue)) {
            return StringUtils.defaultString(rawValue);
        }
        String trimmedValue = rawValue.trim();
        if (containsHan(trimmedValue) && !trimmedValue.contains("\uFFFD")) {
            return trimmedValue;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(trimmedValue);
        candidates.add(new String(trimmedValue.getBytes(WINDOWS_1252), StandardCharsets.UTF_8));
        candidates.add(new String(trimmedValue.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
        return candidates.stream()
                .max((left, right) -> Integer.compare(scoreMetadataText(left), scoreMetadataText(right)))
                .orElse(trimmedValue);
    }

    private int scoreMetadataText(String value) {
        if (StringUtils.isBlank(value)) {
            return Integer.MIN_VALUE;
        }
        int hanCount = (int) value.codePoints()
                .filter(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN)
                .count();
        int replacementCount = (int) value.chars().filter(ch -> ch == '\uFFFD').count();
        int questionMarkCount = (int) value.chars().filter(ch -> ch == '?').count();
        return hanCount * 1000 - replacementCount * 200 - questionMarkCount * 100;
    }

    private boolean containsHan(String value) {
        return value.codePoints().anyMatch(codePoint ->
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private static void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ex) {
                log.error("Connection release error", ex);
            }
        }
    }

    private static void closeResult(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception ex) {
                log.error("ResultSet close error", ex);
            }
        }
    }
}
