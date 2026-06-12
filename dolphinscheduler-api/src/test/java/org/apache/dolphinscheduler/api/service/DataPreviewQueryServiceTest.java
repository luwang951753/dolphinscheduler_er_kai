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

import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.spi.enums.DbType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataPreviewQueryServiceTest {

    private final DataPreviewQueryService service = new DataPreviewQueryService();

    @Test
    public void splitPreviewSqlStatementsIgnoresSemicolonInsideString() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> statements = (List<String>) invoke("splitPreviewSqlStatements",
                new Class[]{String.class},
                "select 'a;b' as text; select 'c' as text");

        Assertions.assertEquals(2, statements.size());
        Assertions.assertEquals("select 'a;b' as text", statements.get(0));
        Assertions.assertEquals("select 'c' as text", statements.get(1));
    }

    @Test
    public void normalizePreviewReadonlySqlAllowsForbiddenWordsInsideStringLiteral() throws Exception {
        String sql = (String) invoke("normalizePreviewReadonlySql",
                new Class[]{String.class},
                "select 'drop table x' as text");

        Assertions.assertEquals("select 'drop table x' as text", sql);
    }

    @Test
    public void normalizePreviewReadonlySqlRejectsForbiddenKeywordOutsideStringLiteral() {
        ServiceException exception = Assertions.assertThrows(ServiceException.class,
                () -> invoke("normalizePreviewReadonlySql",
                        new Class[]{String.class},
                        "select id from t; drop table t"));

        Assertions.assertEquals(Status.DATA_PREVIEW_QUERY_ERROR.getCode(), exception.getCode());
    }

    @Test
    public void appendReadonlySqlLimitDoesNotTreatStringLiteralAsLimitClause() throws Exception {
        String sql = (String) invoke("appendReadonlySqlLimit",
                new Class[]{DbType.class, String.class, int.class},
                DbType.MYSQL,
                "select 'limit 1' as text",
                50);

        Assertions.assertEquals("select 'limit 1' as text LIMIT 50", sql);
    }

    @Test
    public void appendReadonlySqlLimitSupportsDoris() throws Exception {
        String sql = (String) invoke("appendReadonlySqlLimit",
                new Class[]{DbType.class, String.class, int.class},
                DbType.DORIS,
                "select case_id, case_type from ods.ajxx_tab",
                50);

        Assertions.assertEquals("select case_id, case_type from ods.ajxx_tab LIMIT 50", sql);
    }

    @Test
    public void containsReadonlySqlResultLimitHonorsUserMysqlLimit() throws Exception {
        Boolean userLimited = (Boolean) invoke("containsReadonlySqlResultLimit",
                new Class[]{DbType.class, String.class},
                DbType.MYSQL,
                "select * from case_workbench.ajxx_tab limit 1000");

        Assertions.assertTrue(userLimited);
    }

    @Test
    public void supportedPreviewDatasourceTypesIncludeDoris() throws Exception {
        Boolean supported = (Boolean) invoke("isSupportedPreviewDataSourceType",
                new Class[]{DbType.class},
                DbType.DORIS);

        Assertions.assertTrue(supported);
    }

    @Test
    public void quoteIdentifierUsesBackticksForDoris() throws Exception {
        String quoted = (String) invoke("quoteIdentifier",
                new Class[]{DbType.class, String.class},
                DbType.DORIS,
                "ajxx_tab");

        Assertions.assertEquals("`ajxx_tab`", quoted);
    }

    @Test
    public void appendReadonlySqlLimitUsesRownumForOracleWithoutExistingLimit() throws Exception {
        String sql = (String) invoke("appendReadonlySqlLimit",
                new Class[]{DbType.class, String.class, int.class},
                DbType.ORACLE,
                "select ID, NAME from POLICE_APP.ALARM_EVENT",
                50);

        Assertions.assertEquals("SELECT * FROM (select ID, NAME from POLICE_APP.ALARM_EVENT) WHERE ROWNUM <= 50", sql);
    }

    @Test
    public void appendReadonlySqlLimitKeepsOracleRownumPredicate() throws Exception {
        String sql = (String) invoke("appendReadonlySqlLimit",
                new Class[]{DbType.class, String.class, int.class},
                DbType.ORACLE,
                "select ID from POLICE_APP.ALARM_EVENT where rownum <= 10",
                50);

        Assertions.assertEquals("select ID from POLICE_APP.ALARM_EVENT where rownum <= 10", sql);
    }

    @Test
    public void executeOracleExplainPlanUsesExplainPlanForSyntax() throws Exception {
        List<String> executedSql = new ArrayList<>();
        Connection connection = connectionCapturingSql(executedSql);

        invoke("executeOracleExplainPlan",
                new Class[]{Connection.class, String.class, int.class, int.class},
                connection,
                "select ID, NAME from POLICE_APP.ALARM_EVENT",
                30,
                50);

        Assertions.assertEquals("EXPLAIN PLAN FOR select ID, NAME from POLICE_APP.ALARM_EVENT", executedSql.get(0));
        Assertions.assertEquals("SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY())", executedSql.get(1));
    }

    @Test
    public void normalizePreviewCellValueConvertsJdbcDatesToJsonSafeText() throws Exception {
        Object value = invoke("normalizePreviewCellValue",
                new Class[]{Object.class},
                Timestamp.valueOf("2026-06-04 10:17:53"));

        Assertions.assertEquals("2026-06-04 10:17:53", value);
    }

    @Test
    public void normalizePreviewCellValueConvertsOracleLikeTimestampObjectsToJsonSafeText() throws Exception {
        Object value = invoke("normalizePreviewCellValue",
                new Class[]{Object.class},
                new OracleLikeTimestamp(Timestamp.valueOf("2026-06-04 10:17:53")));

        Assertions.assertEquals("2026-06-04 10:17:53", value);
    }

    @Test
    public void normalizePreviewCellValueFallsBackToStringForOracleLikeRowIdObjects() throws Exception {
        Object value = invoke("normalizePreviewCellValue",
                new Class[]{Object.class},
                new OracleLikeRowId("AAAR3vAAEAAAACXAAA"));

        Assertions.assertEquals("AAAR3vAAEAAAACXAAA", value);
    }

    private static final class OracleLikeTimestamp {

        private final Timestamp value;

        private OracleLikeTimestamp(Timestamp value) {
            this.value = value;
        }

        public Timestamp timestampValue() {
            return value;
        }
    }

    private static final class OracleLikeRowId {

        private final String value;

        private OracleLikeRowId(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private Connection connectionCapturingSql(List<String> executedSql) {
        return (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if ("createStatement".equals(method.getName())) {
                        return statementCapturingSql(executedSql);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Statement statementCapturingSql(List<String> executedSql) {
        return (Statement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{Statement.class},
                (proxy, method, args) -> {
                    if ("execute".equals(method.getName()) && args != null && args.length > 0) {
                        executedSql.add((String) args[0]);
                        return true;
                    }
                    if ("executeQuery".equals(method.getName()) && args != null && args.length > 0) {
                        executedSql.add((String) args[0]);
                        return emptyResultSet();
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private ResultSet emptyResultSet() {
        return (ResultSet) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, args) -> {
                    if ("getMetaData".equals(method.getName())) {
                        return emptyResultSetMetaData();
                    }
                    if ("next".equals(method.getName())) {
                        return false;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private ResultSetMetaData emptyResultSetMetaData() {
        return (ResultSetMetaData) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    if ("getColumnCount".equals(method.getName())) {
                        return 0;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Float.TYPE) {
            return 0F;
        }
        if (returnType == Double.TYPE) {
            return 0D;
        }
        return null;
    }

    private Object invoke(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = DataPreviewQueryService.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        try {
            return method.invoke(service, args);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof Exception) {
                throw (Exception) ex.getCause();
            }
            throw ex;
        }
    }
}
