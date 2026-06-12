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
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
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

    @FunctionalInterface
    private interface DdlFallback {

        String build();
    }

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
            String catalog = getCatalog(dataSource.getType(), database);
            String schemaPattern = getDbSchemaPattern(dataSource.getType(), schema, connectionParam);
            String normalizedTableName = normalizeMetadataTableName(dataSource.getType(), tableName);
            Set<String> primaryKeys = new LinkedHashSet<>();
            primaryKeyRs = metaData.getPrimaryKeys(catalog, schemaPattern, normalizedTableName);
            while (primaryKeyRs != null && primaryKeyRs.next()) {
                primaryKeys.add(primaryKeyRs.getString(COLUMN_NAME));
            }

            Map<String, DataPreviewTableStructureResult.Index> indexByNameAndColumn = new LinkedHashMap<>();
            Map<String, String> firstIndexByColumn = new LinkedHashMap<>();
            indexRs = metaData.getIndexInfo(catalog, schemaPattern, normalizedTableName, false, false);
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
            columnRs = metaData.getColumns(catalog, schemaPattern, normalizedTableName, "%");
            while (columnRs != null && columnRs.next()) {
                DataPreviewTableStructureResult.Column column = new DataPreviewTableStructureResult.Column();
                String columnName = columnRs.getString(COLUMN_NAME);
                column.setName(columnName);
                column.setType(columnRs.getString("TYPE_NAME"));
                column.setLength(columnRs.getInt("COLUMN_SIZE"));
                column.setScale(columnRs.getInt("DECIMAL_DIGITS"));
                column.setNullable(columnRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setPrimaryKey(primaryKeys.contains(columnName));
                column.setDefaultValue(safeGetMetadataString(columnRs, "COLUMN_DEF"));
                column.setComment(resolveColumnComment(connection, dataSource.getType(), schemaPattern,
                        normalizedTableName, columnName, safeGetMetadataString(columnRs, "REMARKS")));
                column.setIndexName(firstIndexByColumn.get(columnName));
                columns.add(column);
            }

            DataPreviewTableStructureResult.TableSummary summary = new DataPreviewTableStructureResult.TableSummary();
            summary.setTableName(normalizedTableName);
            summary.setDatabase(database);
            summary.setSchema(StringUtils.trimToEmpty(schema));
            summary.setDatasourceType(dataSource.getType().name());
            summary.setFieldCount(columns.size());
            tableRs = metaData.getTables(catalog, schemaPattern, normalizedTableName, TABLE_TYPES);
            if (tableRs != null && tableRs.next()) {
                summary.setTableType(tableRs.getString("TABLE_TYPE"));
                summary.setTableComment(resolveTableComment(connection, dataSource.getType(), catalog, schemaPattern,
                        normalizedTableName, safeGetMetadataString(tableRs, "REMARKS")));
            }
            if (StringUtils.isBlank(summary.getTableComment())) {
                summary.setTableComment("");
            }
            summary.setEngine(resolvePreviewEngine(dataSource.getType()));

            DataPreviewTableStructureResult result = new DataPreviewTableStructureResult();
            result.setSummary(summary);
            result.setColumns(columns);
            result.setIndexes(new ArrayList<>(indexByNameAndColumn.values()));
            result.setConstraints(primaryKeys.isEmpty()
                    ? Collections.emptyList()
                    : Collections.singletonList("PRIMARY KEY (" + String.join(", ", primaryKeys) + ")"));
            result.setDdl(resolveRealTableDdl(connection, dataSource.getType(), catalog, schemaPattern,
                    normalizedTableName, summary.getTableType(),
                    () -> buildPreviewDdl(dataSource.getType(), normalizedTableName, summary.getTableComment(), columns,
                            primaryKeys)));
            return result;
        } catch (Exception ex) {
            log.error("Query data preview table structure error, datasourceId:{} table:{}.", datasourceId, tableName,
                    ex);
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
            applyPreviewSqlConnectionContext(connection, dataSource.getType(), request.getDatabase(),
                    request.getSchema());
            DataPreviewQueryResult lastResult = null;
            for (String rawStatement : statements) {
                String statementSql = normalizePreviewReadonlySql(rawStatement);
                if (explain && dataSource.getType() == DbType.ORACLE) {
                    lastResult = executeOracleExplainPlan(connection, statementSql, timeoutSeconds, pageSize);
                    continue;
                }
                if (explain && !StringUtils.startsWithIgnoreCase(statementSql, "EXPLAIN")) {
                    statementSql = "EXPLAIN " + statementSql;
                }
                boolean userLimited = containsReadonlySqlResultLimit(dataSource.getType(), statementSql);
                String executableSql = appendReadonlySqlLimit(dataSource.getType(), statementSql, pageSize);
                lastResult = executeReadonlySql(connection, executableSql, timeoutSeconds, userLimited ? 0 : pageSize);
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

    private DataPreviewQueryResult executeOracleExplainPlan(Connection connection,
                                                            String sql,
                                                            int timeoutSeconds,
                                                            int pageSize) throws SQLException {
        String explainSql = "EXPLAIN PLAN FOR " + sql;
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(timeoutSeconds);
            statement.execute(explainSql);
        }
        return executeReadonlySql(connection,
                "SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY())",
                timeoutSeconds,
                pageSize);
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
        if (!isSupportedPreviewDataSourceType(dataSource.getType())) {
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
            if ((dbType == DbType.MYSQL || dbType == DbType.DORIS) && StringUtils.isNotBlank(database)) {
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
        List<String> statements = new ArrayList<>();
        StringBuilder statement = new StringBuilder();
        SqlScanState state = new SqlScanState();
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (!state.isInsideQuotedOrComment() && current == ';') {
                addPreviewSqlStatement(statements, statement.toString());
                statement.setLength(0);
                continue;
            }
            statement.append(current);
            if (state.accept(current, next, true)) {
                statement.append(next);
                i++;
            }
        }
        addPreviewSqlStatement(statements, statement.toString());
        if (statements.isEmpty()) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        return statements;
    }

    private void addPreviewSqlStatement(List<String> statements, String statement) {
        String trimmed = StringUtils.trimToEmpty(statement);
        if (StringUtils.isBlank(maskSqlCommentsAndLiterals(trimmed))) {
            return;
        }
        statements.add(trimmed);
    }

    private String maskSqlCommentsAndLiterals(String sql) {
        StringBuilder masked = new StringBuilder(sql.length());
        SqlScanState state = new SqlScanState();
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            boolean consumeNext = state.accept(current, next, false);
            masked.append(state.shouldMask(current) ? ' ' : current);
            if (consumeNext) {
                masked.append(' ');
                i++;
            }
        }
        return masked.toString();
    }

    private boolean containsReadonlySqlLimit(String sql) {
        String masked = maskSqlCommentsAndLiterals(sql).toLowerCase(Locale.ROOT);
        return masked.matches("[\\s\\S]*\\blimit\\s+\\d+[\\s\\S]*");
    }

    private boolean containsOracleReadonlySqlLimit(String sql) {
        String masked = maskSqlCommentsAndLiterals(sql).toLowerCase(Locale.ROOT);
        return masked.matches("[\\s\\S]*\\b(fetch\\s+next|rownum)\\b[\\s\\S]*");
    }

    private boolean containsReadonlySqlResultLimit(DbType dbType, String sql) {
        if ("explain".equals(firstSqlTokenText(sql))) {
            return true;
        }
        if (dbType == DbType.ORACLE) {
            return containsOracleReadonlySqlLimit(sql);
        }
        return containsReadonlySqlLimit(sql);
    }

    private boolean containsForbiddenReadonlySqlKeyword(String sql) {
        String masked = maskSqlCommentsAndLiterals(sql).toLowerCase(Locale.ROOT);
        return masked.matches(
                "[\\s\\S]*\\b(insert|update|delete|drop|truncate|alter|create|grant|revoke|merge|replace|call)\\b[\\s\\S]*");
    }

    private String firstSqlTokenText(String sql) {
        String masked = StringUtils.trimToEmpty(maskSqlCommentsAndLiterals(sql));
        if (StringUtils.isBlank(masked)) {
            return "";
        }
        int index = 0;
        while (index < masked.length() && Character.isLetter(masked.charAt(index))) {
            index++;
        }
        return masked.substring(0, index).toLowerCase(Locale.ROOT);
    }

    private static final class SqlScanState {

        private boolean singleQuote;
        private boolean doubleQuote;
        private boolean backtickQuote;
        private boolean lineComment;
        private boolean blockComment;

        private boolean isInsideQuotedOrComment() {
            return singleQuote || doubleQuote || backtickQuote || lineComment || blockComment;
        }

        private boolean shouldMask(char current) {
            return singleQuote || doubleQuote || backtickQuote || lineComment || blockComment
                    || current == '\'' || current == '"' || current == '`';
        }

        private boolean accept(char current, char next, boolean keepEscapedPair) {
            if (lineComment) {
                if (current == '\n' || current == '\r') {
                    lineComment = false;
                }
                return false;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    return true;
                }
                return false;
            }
            if (singleQuote) {
                if (current == '\'' && next == '\'') {
                    return keepEscapedPair;
                }
                if (current == '\'') {
                    singleQuote = false;
                }
                return false;
            }
            if (doubleQuote) {
                if (current == '"' && next == '"') {
                    return keepEscapedPair;
                }
                if (current == '"') {
                    doubleQuote = false;
                }
                return false;
            }
            if (backtickQuote) {
                if (current == '`' && next == '`') {
                    return keepEscapedPair;
                }
                if (current == '`') {
                    backtickQuote = false;
                }
                return false;
            }
            if (current == '-' && next == '-') {
                lineComment = true;
                return true;
            }
            if (current == '/' && next == '*') {
                blockComment = true;
                return true;
            }
            if (current == '\'') {
                singleQuote = true;
            } else if (current == '"') {
                doubleQuote = true;
            } else if (current == '`') {
                backtickQuote = true;
            }
            return false;
        }
    }

    private String normalizePreviewReadonlySql(String sql) {
        String normalized = StringUtils.trimToEmpty(sql);
        String firstToken = firstSqlTokenText(normalized);
        if (!"select".equals(firstToken) && !"with".equals(firstToken) && !"explain".equals(firstToken)) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        if (containsForbiddenReadonlySqlKeyword(normalized)) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
        return normalized;
    }

    private String appendReadonlySqlLimit(DbType dbType, String sql, int pageSize) {
        if ("explain".equals(firstSqlTokenText(sql))) {
            return sql;
        }
        if (containsReadonlySqlLimit(sql)) {
            return sql;
        }
        if (dbType == DbType.ORACLE) {
            if (containsOracleReadonlySqlLimit(sql)) {
                return sql;
            }
            return "SELECT * FROM (" + sql + ") WHERE ROWNUM <= " + pageSize;
        }
        if (dbType == DbType.MYSQL || dbType == DbType.DORIS || dbType == DbType.POSTGRESQL) {
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
            if (pageSize > 0) {
                statement.setMaxRows(pageSize);
            }
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
    private String getDbSchemaPattern(DbType dbType, String schema, BaseConnectionParam connectionParam) {
        if (dbType == DbType.POSTGRESQL && StringUtils.isNotBlank(schema)) {
            return schema;
        }
        if (dbType == DbType.ORACLE) {
            return upperCaseOracleIdentifier(StringUtils.defaultIfBlank(schema, connectionParam.getUser()));
        }
        return null;
    }

    private boolean isSupportedPreviewDataSourceType(DbType dbType) {
        return dbType == DbType.MYSQL
                || dbType == DbType.DORIS
                || dbType == DbType.POSTGRESQL
                || dbType == DbType.ORACLE;
    }

    private String getCatalog(DbType dbType, String database) {
        return dbType == DbType.ORACLE ? null : database;
    }

    private String resolvePreviewEngine(DbType dbType) {
        if (dbType == DbType.MYSQL) {
            return "InnoDB / metadata";
        }
        if (dbType == DbType.DORIS) {
            return "Doris / metadata";
        }
        return "heap / metadata";
    }

    private String normalizeMetadataTableName(DbType dbType, String tableName) {
        if (dbType == DbType.ORACLE) {
            return upperCaseOracleIdentifier(tableName);
        }
        return tableName;
    }

    private String upperCaseOracleIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            return identifier;
        }
        return StringUtils.upperCase(StringUtils.trim(identifier), Locale.ROOT);
    }

    private String resolveTableComment(Connection connection,
                                       DbType dbType,
                                       String catalog,
                                       String schema,
                                       String tableName,
                                       String jdbcRemark) {
        String normalizedJdbcRemark = normalizeMetadataText(jdbcRemark);
        if (StringUtils.isNotBlank(normalizedJdbcRemark)) {
            return normalizedJdbcRemark;
        }
        try {
        if (dbType == DbType.MYSQL || dbType == DbType.DORIS) {
            return queryMysqlTableComment(connection, catalog, tableName);
        }
            if (dbType == DbType.ORACLE) {
                return queryOracleTableComment(connection, schema, tableName);
            }
        } catch (Exception ex) {
            log.warn("Resolve data preview table comment failed, dbType:{} catalog:{} schema:{} table:{}, error:{}.",
                    dbType, catalog, schema, tableName, ex.toString());
        }
        return "";
    }

    private String safeGetMetadataString(ResultSet resultSet, String columnLabel) {
        try {
            return resultSet.getString(columnLabel);
        } catch (SQLException | RuntimeException ex) {
            log.warn("Read data preview metadata column failed, columnLabel:{}, error:{}.",
                    columnLabel, ex.toString());
            return "";
        }
    }

    private String queryMysqlTableComment(Connection connection, String database,
                                          String tableName) throws SQLException {
        if (StringUtils.isBlank(database)) {
            return "";
        }
        String sql = "SELECT TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, database);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return normalizeMetadataText(resultSet.getString("TABLE_COMMENT"));
                }
            }
        }
        return "";
    }

    private String queryOracleTableComment(Connection connection, String schema, String tableName) throws SQLException {
        String sql = StringUtils.isBlank(schema)
                ? "SELECT COMMENTS FROM USER_TAB_COMMENTS WHERE TABLE_NAME = ?"
                : "SELECT COMMENTS FROM ALL_TAB_COMMENTS WHERE OWNER = ? AND TABLE_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (StringUtils.isNotBlank(schema)) {
                statement.setString(index++, upperCaseOracleIdentifier(schema));
            }
            statement.setString(index, upperCaseOracleIdentifier(tableName));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return normalizeMetadataText(resultSet.getString("COMMENTS"));
                }
            }
        }
        return "";
    }

    private String resolveColumnComment(Connection connection,
                                        DbType dbType,
                                        String schema,
                                        String tableName,
                                        String columnName,
                                        String jdbcRemark) {
        String normalizedJdbcRemark = normalizeMetadataText(jdbcRemark);
        if (StringUtils.isNotBlank(normalizedJdbcRemark)) {
            return normalizedJdbcRemark;
        }
        if (dbType != DbType.ORACLE) {
            return "";
        }
        try {
            return queryOracleColumnComment(connection, schema, tableName, columnName);
        } catch (Exception ex) {
            log.warn("Resolve oracle column comment failed, schema:{} table:{} column:{}.",
                    schema, tableName, columnName, ex);
            return "";
        }
    }

    private String queryOracleColumnComment(Connection connection,
                                            String schema,
                                            String tableName,
                                            String columnName) throws SQLException {
        String sql = StringUtils.isBlank(schema)
                ? "SELECT COMMENTS FROM USER_COL_COMMENTS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?"
                : "SELECT COMMENTS FROM ALL_COL_COMMENTS WHERE OWNER = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (StringUtils.isNotBlank(schema)) {
                statement.setString(index++, upperCaseOracleIdentifier(schema));
            }
            statement.setString(index++, upperCaseOracleIdentifier(tableName));
            statement.setString(index, upperCaseOracleIdentifier(columnName));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return normalizeMetadataText(resultSet.getString("COMMENTS"));
                }
            }
        }
        return "";
    }

    private void validateIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier) || !identifier.matches("[A-Za-z0-9_.$-]+")) {
            throw new ServiceException(Status.DATA_PREVIEW_QUERY_ERROR);
        }
    }

    private String quoteIdentifier(DbType dbType, String identifier) {
        validateIdentifier(identifier);
        if (dbType == DbType.MYSQL || dbType == DbType.DORIS) {
            return "`" + identifier.replace("`", "``") + "`";
        }
        if (dbType == DbType.ORACLE) {
            String normalizedIdentifier = StringUtils.upperCase(StringUtils.trim(identifier), Locale.ROOT);
            return "\"" + normalizedIdentifier.replace("\"", "\"\"") + "\"";
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
            if (StringUtils.isNotBlank(column.getComment()) && (dbType == DbType.MYSQL || dbType == DbType.DORIS)) {
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
        } else if (dbType == DbType.DORIS) {
            ddl.append(" ENGINE=OLAP");
        }
        ddl.append(";");
        return ddl.toString();
    }

    private String resolveRealTableDdl(Connection connection,
                                       DbType dbType,
                                       String catalog,
                                       String schema,
                                       String tableName,
                                       String tableType,
                                       DdlFallback fallback) {
        try {
            String ddl = "";
            if (dbType == DbType.MYSQL || dbType == DbType.DORIS) {
                ddl = queryMysqlTableDdl(connection, catalog, tableName);
            } else if (dbType == DbType.ORACLE) {
                ddl = queryOracleTableDdl(connection, schema, tableName, tableType);
            } else if (dbType == DbType.POSTGRESQL) {
                ddl = fallback.build();
            }
            if (StringUtils.isNotBlank(ddl)) {
                return ddl.trim();
            }
        } catch (Exception ex) {
            log.warn("Resolve data preview real table DDL failed, dbType:{} catalog:{} schema:{} table:{}, error:{}.",
                    dbType, catalog, schema, tableName, ex.toString());
        }
        return fallback.build();
    }

    private String queryMysqlTableDdl(Connection connection, String database, String tableName) throws SQLException {
        String tableReference = StringUtils.isBlank(database)
                ? "`" + escapeMysqlIdentifier(tableName) + "`"
                : "`" + escapeMysqlIdentifier(database) + "`.`" + escapeMysqlIdentifier(tableName) + "`";
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SHOW CREATE TABLE " + tableReference)) {
            if (resultSet.next()) {
                return resultSet.getString(2);
            }
        }
        return "";
    }

    private String queryOracleTableDdl(Connection connection,
                                       String schema,
                                       String tableName,
                                       String tableType) throws SQLException {
        String metadataType = StringUtils.equalsIgnoreCase(tableType, VIEW) ? "VIEW" : "TABLE";
        try (
                PreparedStatement transform = connection.prepareStatement(
                        "BEGIN DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'SQLTERMINATOR', TRUE); "
                                + "DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'PRETTY', TRUE); END;")) {
            transform.execute();
        }
        String sql = StringUtils.isBlank(schema)
                ? "SELECT DBMS_METADATA.GET_DDL(?, ?) AS DDL FROM DUAL"
                : "SELECT DBMS_METADATA.GET_DDL(?, ?, ?) AS DDL FROM DUAL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, metadataType);
            statement.setString(2, upperCaseOracleIdentifier(tableName));
            if (StringUtils.isNotBlank(schema)) {
                statement.setString(3, upperCaseOracleIdentifier(schema));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return readClobOrString(resultSet.getObject("DDL"));
                }
            }
        }
        return "";
    }

    private String readClobOrString(Object value) throws SQLException {
        if (value == null) {
            return "";
        }
        if (value instanceof Clob) {
            Clob clob = (Clob) value;
            long length = clob.length();
            if (length <= 0) {
                return "";
            }
            return clob.getSubString(1, (int) Math.min(length, Integer.MAX_VALUE));
        }
        return String.valueOf(value);
    }

    private String escapeMysqlIdentifier(String identifier) {
        validateIdentifier(identifier);
        return identifier.replace("`", "``");
    }

    private Object normalizePreviewCellValue(Object value) {
        if (!(value instanceof String)) {
            return DataPreviewCellValueNormalizer.normalize(value);
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
        return value.codePoints()
                .anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
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
