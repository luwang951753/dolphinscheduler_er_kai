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

import org.apache.dolphinscheduler.api.dto.DataPreviewQueryRequest;
import org.apache.dolphinscheduler.api.dto.DatasourceTableCreateRequest;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.common.enums.AuthorizationType;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.DataSourceMapper;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DataSourceServiceImplTest {

    @Test
    public void buildOraclePreviewSqlUsesRowNumPaginationForOracle11g() throws Exception {
        DataPreviewQueryRequest request = new DataPreviewQueryRequest();
        request.setDatabase("ORCL");
        request.setSchema("POLICE_APP");
        request.setTableName("ALARM_EVENT");

        DataPreviewQueryRequest.Sort sort = new DataPreviewQueryRequest.Sort();
        sort.setField("ID");
        sort.setDirection("DESC");
        request.setSorts(Arrays.asList(sort));

        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "buildPreviewSql",
                DbType.class,
                DataPreviewQueryRequest.class,
                java.util.Set.class,
                int.class,
                int.class);
        method.setAccessible(true);

        String sql = (String) method.invoke(
                new DataSourceServiceImpl(),
                DbType.ORACLE,
                request,
                new LinkedHashSet<>(Arrays.asList("ID", "NAME")),
                2,
                50);

        Assertions.assertFalse(sql.contains("OFFSET"), sql);
        Assertions.assertFalse(sql.contains("FETCH NEXT"), sql);
        Assertions.assertTrue(sql.contains("ROWNUM <= 100"), sql);
        Assertions.assertTrue(sql.contains("rn__ > 50"), sql);
        Assertions.assertTrue(sql.contains("ORDER BY \"ID\" DESC"), sql);
    }

    @Test
    public void buildOraclePreviewSqlUsesExplicitSchemaInTableReference() throws Exception {
        DataPreviewQueryRequest request = new DataPreviewQueryRequest();
        request.setDatabase("ORCLPDB1");
        request.setSchema("POLICE_APP");
        request.setTableName("ALARM_EVENT");

        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "buildPreviewSql",
                DbType.class,
                DataPreviewQueryRequest.class,
                java.util.Set.class,
                int.class,
                int.class);
        method.setAccessible(true);

        String sql = (String) method.invoke(
                new DataSourceServiceImpl(),
                DbType.ORACLE,
                request,
                new LinkedHashSet<>(Arrays.asList("ID", "NAME")),
                1,
                20);

        Assertions.assertTrue(sql.contains("FROM \"POLICE_APP\".\"ALARM_EVENT\""), sql);
        Assertions.assertTrue(sql.contains("ROWNUM <= 20"), sql);
    }

    @Test
    public void buildPreviewDdlIsOnlyFallbackForOracleStructure() throws Exception {
        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "buildPreviewDdl",
                DbType.class,
                String.class,
                String.class,
                List.class,
                java.util.Set.class);
        method.setAccessible(true);

        org.apache.dolphinscheduler.api.dto.DataPreviewTableStructureResult.Column id =
                new org.apache.dolphinscheduler.api.dto.DataPreviewTableStructureResult.Column();
        id.setName("ID");
        id.setType("NUMBER");
        id.setLength(19);
        id.setNullable(false);

        @SuppressWarnings("unchecked")
        String ddl = (String) method.invoke(
                new DataSourceServiceImpl(),
                DbType.ORACLE,
                "DEMO_ORDERS",
                "订单演示表",
                Arrays.asList(id),
                new LinkedHashSet<>(Arrays.asList("ID")));

        Assertions.assertTrue(ddl.contains("CREATE TABLE \"DEMO_ORDERS\""), ddl);
        Assertions.assertTrue(ddl.contains("\"ID\" NUMBER(19) NOT NULL"), ddl);
        Assertions.assertFalse(ddl.contains("COMMENT ON TABLE"), ddl);
    }

    @Test
    public void normalizeMetadataTableNameUpperCasesOracleOnly() throws Exception {
        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "normalizeMetadataTableName",
                DbType.class,
                String.class);
        method.setAccessible(true);

        Assertions.assertEquals("DEMO_ORDERS",
                method.invoke(new DataSourceServiceImpl(), DbType.ORACLE, "demo_orders"));
        Assertions.assertEquals("demo_orders", method.invoke(new DataSourceServiceImpl(), DbType.MYSQL, "demo_orders"));
    }

    @Test
    public void buildPostgresqlPreviewSqlUsesSchemaAndLimitOffset() throws Exception {
        DataPreviewQueryRequest request = new DataPreviewQueryRequest();
        request.setDatabase("case_workbench");
        request.setSchema("public");
        request.setTableName("ajxx_tab");

        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "buildPreviewSql",
                DbType.class,
                DataPreviewQueryRequest.class,
                java.util.Set.class,
                int.class,
                int.class);
        method.setAccessible(true);

        String sql = (String) method.invoke(
                new DataSourceServiceImpl(),
                DbType.POSTGRESQL,
                request,
                new LinkedHashSet<>(Arrays.asList("case_id", "case_no")),
                3,
                25);

        Assertions.assertTrue(sql.contains("FROM \"public\".\"ajxx_tab\""), sql);
        Assertions.assertTrue(sql.endsWith("LIMIT 25 OFFSET 50"), sql);
    }

    @Test
    public void getDbSchemaPatternUsesOracleConnectionUserAndPostgresqlExplicitSchema() throws Exception {
        BaseConnectionParam connectionParam = Mockito.mock(BaseConnectionParam.class);
        Mockito.when(connectionParam.getUser()).thenReturn("police_app");

        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "getDbSchemaPattern",
                DbType.class,
                String.class,
                BaseConnectionParam.class);
        method.setAccessible(true);

        Assertions.assertEquals("POLICE_APP", method.invoke(new DataSourceServiceImpl(), DbType.ORACLE, null,
                connectionParam));
        Assertions.assertEquals("POLICE_APP", method.invoke(new DataSourceServiceImpl(), DbType.ORACLE, "ignored",
                connectionParam));
        Assertions.assertEquals("ods_public", method.invoke(new DataSourceServiceImpl(), DbType.POSTGRESQL,
                "ods_public", connectionParam));
    }

    @Test
    public void buildPreviewSqlEscapesContainsWildcardAsLiteralText() throws Exception {
        DataPreviewQueryRequest request = new DataPreviewQueryRequest();
        request.setDatabase("case_workbench");
        request.setTableName("ajxx_tab");

        DataPreviewQueryRequest.Filter filter = new DataPreviewQueryRequest.Filter();
        filter.setField("NAME");
        filter.setOperator("CONTAINS");
        filter.setValue("100%_done\\case");
        request.setFilters(Arrays.asList(filter));

        LinkedHashSet<String> allowedColumns = new LinkedHashSet<>(Arrays.asList("ID", "NAME"));

        Method sqlMethod = DataSourceServiceImpl.class.getDeclaredMethod(
                "buildPreviewSql",
                DbType.class,
                DataPreviewQueryRequest.class,
                java.util.Set.class,
                int.class,
                int.class);
        sqlMethod.setAccessible(true);
        String sql = (String) sqlMethod.invoke(
                new DataSourceServiceImpl(),
                DbType.MYSQL,
                request,
                allowedColumns,
                1,
                50);

        Assertions.assertTrue(sql.contains("`NAME` LIKE ? ESCAPE '\\\\'"), sql);

        Method parametersMethod = DataSourceServiceImpl.class.getDeclaredMethod(
                "buildPreviewParameters",
                DataPreviewQueryRequest.class,
                java.util.Set.class);
        parametersMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> parameters = (List<Object>) parametersMethod.invoke(
                new DataSourceServiceImpl(),
                request,
                allowedColumns);

        Assertions.assertEquals(1, parameters.size());
        Assertions.assertEquals("%100\\%\\_done\\\\case%", parameters.get(0));
    }

    @Test
    public void buildMysqlPreviewSqlUsesBacktickIdentifiers() throws Exception {
        DataPreviewQueryRequest request = new DataPreviewQueryRequest();
        request.setDatabase("case_workbench");
        request.setTableName("DWD_SJC3_TDWY_BAZX_XYRRQDJXX_450");

        DataPreviewQueryRequest.Filter filter = new DataPreviewQueryRequest.Filter();
        filter.setField("RYBH");
        filter.setOperator("CONTAINS");
        filter.setValue("2026");
        request.setFilters(Arrays.asList(filter));

        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "buildPreviewSql",
                DbType.class,
                DataPreviewQueryRequest.class,
                java.util.Set.class,
                int.class,
                int.class);
        method.setAccessible(true);

        String sql = (String) method.invoke(
                new DataSourceServiceImpl(),
                DbType.MYSQL,
                request,
                new LinkedHashSet<>(Arrays.asList("RYBH", "XM")),
                1,
                50);

        Assertions.assertTrue(sql.contains("`RYBH` LIKE ? ESCAPE '\\\\'"), sql);
        Assertions.assertFalse(sql.contains("\"RYBH\""), sql);
        Assertions.assertTrue(sql.endsWith("LIMIT 50 OFFSET 0"), sql);
    }

    @Test
    public void buildPreviewSqlResolvesFilterAndSortFieldsCaseInsensitively() throws Exception {
        DataPreviewQueryRequest request = new DataPreviewQueryRequest();
        request.setDatabase("case_workbench");
        request.setTableName("ajxx_tab");

        DataPreviewQueryRequest.Filter filter = new DataPreviewQueryRequest.Filter();
        filter.setField("case_no");
        filter.setOperator("CONTAINS");
        filter.setValue("AJ");
        request.setFilters(Arrays.asList(filter));

        DataPreviewQueryRequest.Sort sort = new DataPreviewQueryRequest.Sort();
        sort.setField("case_id");
        sort.setDirection("DESC");
        request.setSorts(Arrays.asList(sort));

        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "buildPreviewSql",
                DbType.class,
                DataPreviewQueryRequest.class,
                java.util.Set.class,
                int.class,
                int.class);
        method.setAccessible(true);

        String sql = (String) method.invoke(
                new DataSourceServiceImpl(),
                DbType.MYSQL,
                request,
                new LinkedHashSet<>(Arrays.asList("CASE_ID", "CASE_NO", "CASE_NAME")),
                1,
                50);

        Assertions.assertTrue(sql.contains("WHERE `CASE_NO` LIKE ? ESCAPE '\\\\'"), sql);
        Assertions.assertTrue(sql.contains("ORDER BY `CASE_ID` DESC"), sql);
    }

    @Test
    public void buildDorisPreviewSqlUsesMysqlCompatibleTableReference() throws Exception {
        DataPreviewQueryRequest request = new DataPreviewQueryRequest();
        request.setDatabase("ods");
        request.setTableName("ajxx_tab");

        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "buildPreviewSql",
                DbType.class,
                DataPreviewQueryRequest.class,
                java.util.Set.class,
                int.class,
                int.class);
        method.setAccessible(true);

        String sql = (String) method.invoke(
                new DataSourceServiceImpl(),
                DbType.DORIS,
                request,
                new LinkedHashSet<>(Arrays.asList("case_id", "case_type")),
                1,
                50);

        Assertions.assertTrue(sql.contains("FROM `ods`.`ajxx_tab`"), sql);
        Assertions.assertTrue(sql.endsWith("LIMIT 50 OFFSET 0"), sql);
        Assertions.assertFalse(sql.contains("\"ajxx_tab\""), sql);
    }

    @Test
    public void dataPreviewSupportIncludesDoris() throws Exception {
        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "isSupportedPreviewDataSourceType",
                DbType.class);
        method.setAccessible(true);

        Assertions.assertEquals(true, method.invoke(new DataSourceServiceImpl(), DbType.DORIS));
    }

    @Test
    public void buildDorisPreviewDdlUsesOlapEngineFallback() throws Exception {
        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "buildPreviewDdl",
                DbType.class,
                String.class,
                String.class,
                List.class,
                java.util.Set.class);
        method.setAccessible(true);

        org.apache.dolphinscheduler.api.dto.DataPreviewTableStructureResult.Column id =
                new org.apache.dolphinscheduler.api.dto.DataPreviewTableStructureResult.Column();
        id.setName("case_id");
        id.setType("BIGINT");
        id.setNullable(false);

        String ddl = (String) method.invoke(
                new DataSourceServiceImpl(),
                DbType.DORIS,
                "ajxx_tab",
                "案件表",
                Arrays.asList(id),
                new LinkedHashSet<>(Arrays.asList("case_id")));

        Assertions.assertTrue(ddl.contains("CREATE TABLE `ajxx_tab`"), ddl);
        Assertions.assertTrue(ddl.contains("ENGINE=OLAP"), ddl);
        Assertions.assertTrue(ddl.contains("PRIMARY KEY (`case_id`)"), ddl);
    }

    @Test
    public void buildCreateTableSqlNormalizesMysqlColumnTypesForOracleTarget() throws Exception {
        DatasourceTableCreateRequest request = createTableRequest("FREEPDB1", "SYSTEM", "DF_SYNC_MYSQL_TO_ORACLE");
        List<DatasourceTableCreateRequest.ColumnDefinition> columns = new ArrayList<>();
        columns.add(createColumn("id", "BIGINT", false, true));
        columns.add(createColumn("name", "VARCHAR(80)", true, false));
        columns.add(createColumn("amount", "DECIMAL(12, 2)", true, false));
        columns.add(createColumn("enabled", "TINYINT(1)", true, false));
        columns.add(createColumn("created_at", "DATETIME", true, false));
        request.setColumns(columns);

        String sql = invokeBuildCreateTableSql(DbType.ORACLE, request, true);

        Assertions.assertTrue(sql.contains("\"ID\" NUMBER(19) NOT NULL"), sql);
        Assertions.assertTrue(sql.contains("\"NAME\" VARCHAR2(80) NULL"), sql);
        Assertions.assertTrue(sql.contains("\"AMOUNT\" NUMBER(12,2) NULL"), sql);
        Assertions.assertTrue(sql.contains("\"ENABLED\" NUMBER(1) NULL"), sql);
        Assertions.assertTrue(sql.contains("\"CREATED_AT\" TIMESTAMP NULL"), sql);
        Assertions.assertTrue(sql.contains("PRIMARY KEY (\"ID\")"), sql);
    }

    @Test
    public void buildCreateTableSqlNormalizesOracleColumnTypesForMysqlTarget() throws Exception {
        DatasourceTableCreateRequest request = createTableRequest("sync_case", null, "df_sync_oracle_to_mysql");
        List<DatasourceTableCreateRequest.ColumnDefinition> columns = new ArrayList<>();
        columns.add(createColumn("ID", "NUMBER(19)", false, true));
        columns.add(createColumn("NAME", "VARCHAR2(120)", true, false));
        columns.add(createColumn("AMOUNT", "NUMBER(12,2)", true, false));
        columns.add(createColumn("CREATED_AT", "TIMESTAMP", true, false));
        request.setColumns(columns);

        String sql = invokeBuildCreateTableSql(DbType.MYSQL, request, true);

        Assertions.assertTrue(sql.contains("`ID` BIGINT NOT NULL"), sql);
        Assertions.assertTrue(sql.contains("`NAME` VARCHAR(120) NULL"), sql);
        Assertions.assertTrue(sql.contains("`AMOUNT` DECIMAL(12,2) NULL"), sql);
        Assertions.assertTrue(sql.contains("`CREATED_AT` TIMESTAMP NULL"), sql);
        Assertions.assertTrue(sql.contains("PRIMARY KEY (`ID`)"), sql);
    }

    @Test
    public void buildCreateTableSqlSupportsPartialColumnMappings() throws Exception {
        DatasourceTableCreateRequest request = createTableRequest("FREEPDB1", "SYSTEM", "DF_SYNC_PARTIAL");
        List<DatasourceTableCreateRequest.ColumnDefinition> columns = new ArrayList<>();
        columns.add(createColumn("case_id", "INT", false, true));
        columns.add(createColumn("case_name", "VARCHAR(64)", true, false));
        columns.add(createColumn("case_amount", "DECIMAL(18,4)", true, false));
        request.setColumns(columns);

        String sql = invokeBuildCreateTableSql(DbType.ORACLE, request, true);

        Assertions.assertTrue(sql.contains("\"CASE_ID\" NUMBER(10) NOT NULL"), sql);
        Assertions.assertTrue(sql.contains("\"CASE_NAME\" VARCHAR2(64) NULL"), sql);
        Assertions.assertTrue(sql.contains("\"CASE_AMOUNT\" NUMBER(18,4) NULL"), sql);
        Assertions.assertFalse(sql.contains("CREATED_AT"), sql);
    }

    @Test
    public void buildCreateTableSqlRejectsInjectedTargetType() throws Exception {
        DatasourceTableCreateRequest request = createTableRequest("case_workbench", null, "target_case");
        List<DatasourceTableCreateRequest.ColumnDefinition> columns = new ArrayList<>();
        columns.add(createColumn("id", "BIGINT", false, true));
        columns.add(createColumn("payload", "VARCHAR(255)); DROP TABLE ajxx_tab; --", true, false));
        request.setColumns(columns);

        InvocationTargetException exception = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeBuildCreateTableSql(DbType.MYSQL, request, true));
        Assertions.assertTrue(exception.getCause() instanceof ServiceException);
    }

    @Test
    public void previewCreateTableSqlRejectsUserWithoutDatasourcePermission() throws Exception {
        DataSourceServiceImpl service = Mockito.spy(new DataSourceServiceImpl());
        DataSource dataSource = new DataSource();
        dataSource.setId(10);
        dataSource.setType(DbType.MYSQL);
        DataSourceMapper dataSourceMapper = Mockito.mock(DataSourceMapper.class);
        Mockito.when(dataSourceMapper.selectById(10)).thenReturn(dataSource);
        setField(service, "dataSourceMapper", dataSourceMapper);
        Mockito.doReturn(false).when(service).canOperatorPermissions(
                Mockito.any(User.class),
                Mockito.any(Object[].class),
                Mockito.eq(AuthorizationType.DATASOURCE),
                Mockito.anyString());

        DatasourceTableCreateRequest request = createTableRequest("case_workbench", null, "target_case");
        request.setDatasourceId(10);
        request.setColumns(Arrays.asList(createColumn("id", "BIGINT", false, true)));

        ServiceException exception = Assertions.assertThrows(
                ServiceException.class,
                () -> service.previewCreateTableSql(new User(), request));
        Assertions.assertEquals(Status.USER_NO_OPERATION_PERM.getCode(), exception.getCode());
    }

    private String invokeBuildCreateTableSql(DbType dbType,
                                             DatasourceTableCreateRequest request,
                                             boolean includePrimaryKey) throws Exception {
        Method method = DataSourceServiceImpl.class.getDeclaredMethod(
                "buildCreateTableSql",
                DbType.class,
                DatasourceTableCreateRequest.class,
                boolean.class);
        method.setAccessible(true);
        return (String) method.invoke(new DataSourceServiceImpl(), dbType, request, includePrimaryKey);
    }

    private DatasourceTableCreateRequest createTableRequest(String database, String schema, String tableName) {
        DatasourceTableCreateRequest request = new DatasourceTableCreateRequest();
        request.setDatabase(database);
        request.setSchema(schema);
        request.setTableName(tableName);
        return request;
    }

    private DatasourceTableCreateRequest.ColumnDefinition createColumn(String name,
                                                                       String type,
                                                                       boolean nullable,
                                                                       boolean primaryKey) {
        DatasourceTableCreateRequest.ColumnDefinition column = new DatasourceTableCreateRequest.ColumnDefinition();
        column.setSourceColumn(name);
        column.setSourceType(type);
        column.setTargetColumn(name);
        column.setTargetType(type);
        column.setNullable(nullable);
        column.setPrimaryKey(primaryKey);
        return column;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
