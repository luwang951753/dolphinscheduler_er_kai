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

import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SqlLineage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SqlLineageParseServiceImplTest {

    private final SqlLineageParseServiceImpl service = new SqlLineageParseServiceImpl();

    @Test
    public void shouldParseInsertSelectFieldLineage() {
        String sql = "INSERT INTO money_laundering.money_laundering_feature "
                + "(card_id, suspect_id, total_amount) "
                + "SELECT bc.card_id, s.suspect_id, SUM(tx.amount) AS total_amount "
                + "FROM money_laundering.bank_card bc "
                + "JOIN money_laundering.suspect s ON bc.owner_id = s.suspect_id "
                + "JOIN money_laundering.transaction_detail tx ON bc.card_id = tx.card_id "
                + "GROUP BY bc.card_id, s.suspect_id";

        SqlLineage lineage = service.parse(sql);

        Assertions.assertEquals(4, lineage.getTables().size());
        Assertions.assertTrue(lineage.getEdges().stream().anyMatch(edge ->
                "TABLE".equals(edge.getLineageType())
                        && "money_laundering.bank_card".equals(edge.getSourceTable())
                        && "money_laundering.money_laundering_feature".equals(edge.getTargetTable())));
        Assertions.assertTrue(lineage.getEdges().stream().anyMatch(edge ->
                "FIELD".equals(edge.getLineageType())
                        && "money_laundering.bank_card".equals(edge.getSourceTable())
                        && "card_id".equals(edge.getSourceColumn())
                        && "money_laundering.money_laundering_feature".equals(edge.getTargetTable())
                        && "card_id".equals(edge.getTargetColumn())));
        Assertions.assertTrue(lineage.getEdges().stream().anyMatch(edge ->
                "FIELD".equals(edge.getLineageType())
                        && "money_laundering.transaction_detail".equals(edge.getSourceTable())
                        && "amount".equals(edge.getSourceColumn())
                        && "total_amount".equals(edge.getTargetColumn())));
    }

    @Test
    public void shouldParseSelectAsVirtualResultLineage() {
        SqlLineage lineage = service.parse("SELECT card_id, amount FROM bank_card");

        Assertions.assertTrue(lineage.getTables().stream().anyMatch(table -> "query_result".equals(table.getId())));
        Assertions.assertTrue(lineage.getEdges().stream().anyMatch(edge ->
                "FIELD".equals(edge.getLineageType())
                        && "bank_card".equals(edge.getSourceTable())
                        && "card_id".equals(edge.getSourceColumn())
                        && "query_result".equals(edge.getTargetTable())
                        && "card_id".equals(edge.getTargetColumn())));
    }

    @Test
    public void shouldParseSimpleOdsToDwdInsertLineage() {
        SqlLineage lineage = service.parse("insert into dwd.ajxx_tab select * from ods.ajxx_tab");

        Assertions.assertTrue(hasTableEdge(lineage, "ods.ajxx_tab", "dwd.ajxx_tab"));
        Assertions.assertTrue(hasFieldEdge(lineage, "ods.ajxx_tab", "*", "dwd.ajxx_tab", "*"));
    }

    @Test
    public void shouldParseExplicitColumnInsertLineage() {
        SqlLineage lineage = service.parse("insert into dwd.ajxx_tab(ajbh, ajmc) "
                + "select ajbh, ajmc from ods.ajxx_tab");

        Assertions.assertTrue(hasFieldEdge(lineage, "ods.ajxx_tab", "ajbh", "dwd.ajxx_tab", "ajbh"));
        Assertions.assertTrue(hasFieldEdge(lineage, "ods.ajxx_tab", "ajmc", "dwd.ajxx_tab", "ajmc"));
    }

    @Test
    public void shouldParseJoinFieldLineageWithAliases() {
        SqlLineage lineage = service.parse("insert into dwd.aj_suspect(ajbh, xyrmc) "
                + "select a.ajbh, x.xyrmc from ods.ajxx_tab a "
                + "join ods.xyr_tab x on a.xyrbh = x.xyrbh");

        Assertions.assertTrue(hasTableEdge(lineage, "ods.ajxx_tab", "dwd.aj_suspect"));
        Assertions.assertTrue(hasTableEdge(lineage, "ods.xyr_tab", "dwd.aj_suspect"));
        Assertions.assertTrue(hasFieldEdge(lineage, "ods.ajxx_tab", "ajbh", "dwd.aj_suspect", "ajbh"));
        Assertions.assertTrue(hasFieldEdge(lineage, "ods.xyr_tab", "xyrmc", "dwd.aj_suspect", "xyrmc"));
    }

    @Test
    public void shouldParseExpressionLineageToOneTargetField() {
        SqlLineage lineage = service.parse("insert into dwd.ajxx_tab(aj_title) "
                + "select concat(ajbh, '-', ajmc) as aj_title from ods.ajxx_tab");

        Assertions.assertTrue(hasFieldEdge(lineage, "ods.ajxx_tab", "ajbh", "dwd.ajxx_tab", "aj_title"));
        Assertions.assertTrue(hasFieldEdge(lineage, "ods.ajxx_tab", "ajmc", "dwd.ajxx_tab", "aj_title"));
    }

    @Test
    public void shouldParseCaseWhenLineage() {
        SqlLineage lineage = service.parse("insert into dwd.ajxx_tab(case_type_name) "
                + "select case when case_type = '1' then '刑事' else '其他' end from ods.ajxx_tab");

        Assertions.assertTrue(hasFieldEdge(lineage, "ods.ajxx_tab", "case_type", "dwd.ajxx_tab",
                "case_type_name"));
    }

    @Test
    public void shouldParseAggregateLineage() {
        SqlLineage lineage = service.parse("insert into dws.dws_ajxx_tab(case_type, cnt) "
                + "select case_type, count(1) as cnt from dwd.ajxx_tab group by case_type");

        Assertions.assertTrue(hasFieldEdge(lineage, "dwd.ajxx_tab", "case_type", "dws.dws_ajxx_tab",
                "case_type"));
    }

    @Test
    public void shouldParseSubquerySourceLineageApproximately() {
        SqlLineage lineage = service.parse("insert into dwd.ajxx_tab(ajbh) "
                + "select t.ajbh from (select ajbh from ods.ajxx_tab where ajbh is not null) t");

        Assertions.assertTrue(hasTableEdge(lineage, "ods.ajxx_tab", "dwd.ajxx_tab"));
        Assertions.assertTrue(lineage.getWarnings().stream().anyMatch(warning -> warning.contains("FROM 子查询")));
    }

    @Test
    public void shouldParseUnionWithWarning() {
        SqlLineage lineage = service.parse("insert into dwd.ajxx_tab(ajbh) "
                + "select ajbh from ods.ajxx_tab union all select ajbh from ods.ajxx_history");

        Assertions.assertTrue(hasTableEdge(lineage, "ods.ajxx_tab", "dwd.ajxx_tab"));
        Assertions.assertTrue(hasTableEdge(lineage, "ods.ajxx_history", "dwd.ajxx_tab"));
        Assertions.assertTrue(hasFieldEdge(lineage, "ods.ajxx_history", "ajbh", "dwd.ajxx_tab", "ajbh"));
        Assertions.assertTrue(lineage.getWarnings().stream().anyMatch(warning -> warning.contains("UNION")));
    }

    @Test
    public void shouldParseQuotedIdentifiers() {
        SqlLineage lineage = service.parse("insert into `dwd`.`ajxx_tab`(ajbh) "
                + "select `a`.`ajbh` from `ods`.`ajxx_tab` a");

        Assertions.assertTrue(hasFieldEdge(lineage, "ods.ajxx_tab", "ajbh", "dwd.ajxx_tab", "ajbh"));
    }

    @Test
    public void shouldRejectUnsupportedStatement() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.parse("delete from ods.ajxx_tab"));
    }

    private boolean hasTableEdge(SqlLineage lineage, String sourceTable, String targetTable) {
        return lineage.getEdges().stream().anyMatch(edge ->
                "TABLE".equals(edge.getLineageType())
                        && sourceTable.equals(edge.getSourceTable())
                        && targetTable.equals(edge.getTargetTable()));
    }

    private boolean hasFieldEdge(SqlLineage lineage,
                                 String sourceTable,
                                 String sourceColumn,
                                 String targetTable,
                                 String targetColumn) {
        return lineage.getEdges().stream().anyMatch(edge ->
                "FIELD".equals(edge.getLineageType())
                        && sourceTable.equals(edge.getSourceTable())
                        && sourceColumn.equals(edge.getSourceColumn())
                        && targetTable.equals(edge.getTargetTable())
                        && targetColumn.equals(edge.getTargetColumn()));
    }
}
