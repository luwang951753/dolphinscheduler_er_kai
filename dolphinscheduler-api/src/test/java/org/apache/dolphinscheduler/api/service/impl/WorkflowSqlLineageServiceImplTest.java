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

import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.LineageNode;
import org.apache.dolphinscheduler.api.service.DataGovernanceStore;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.entity.TaskDefinitionLog;
import org.apache.dolphinscheduler.dao.mapper.DataSourceMapper;
import org.apache.dolphinscheduler.spi.enums.DbType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

public class WorkflowSqlLineageServiceImplTest {

    @Test
    public void shouldRegisterWorkflowSqlLineageForChainedInsertStatements() {
        WorkflowSqlLineageServiceImpl service = new WorkflowSqlLineageServiceImpl();
        DataGovernanceStore store = Mockito.mock(DataGovernanceStore.class);
        DataSourceMapper dataSourceMapper = Mockito.mock(DataSourceMapper.class);
        ReflectionTestUtils.setField(service, "sqlLineageParseService", new SqlLineageParseServiceImpl());
        ReflectionTestUtils.setField(service, "dataGovernanceStore", store);
        ReflectionTestUtils.setField(service, "dataSourceMapper", dataSourceMapper);

        DataSource dataSource = mysqlDataSource();
        Mockito.when(dataSourceMapper.selectById(1)).thenReturn(dataSource);

        TaskDefinitionLog task = sqlTask("etl-sql", "insert into dwd.ajxx_tab(ajbh, ajmc) "
                + "select ajbh, ajmc from ods.ajxx_tab;"
                + "insert into dws.dws_ajxx_tab(ajbh, ajmc) select ajbh, ajmc from dwd.ajxx_tab");

        service.registerWorkflowSqlLineage(1001L, 3, "案件ETL", Arrays.asList(task));

        Mockito.verify(store).deleteLineageBySyncTaskNamePrefix("SQL工作流[1001]");
        ArgumentCaptor<String> targetCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<LineageNode>> upstreamCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LineageNode> downstreamCaptor = ArgumentCaptor.forClass(LineageNode.class);
        Mockito.verify(store, Mockito.times(2)).replaceLineages(targetCaptor.capture(), upstreamCaptor.capture(),
                downstreamCaptor.capture());

        Assertions.assertEquals("1|dwd||ajxx_tab", targetCaptor.getAllValues().get(0));
        Assertions.assertEquals("1|dws||dws_ajxx_tab", targetCaptor.getAllValues().get(1));
        Assertions.assertEquals("1|ods||ajxx_tab", upstreamCaptor.getAllValues().get(0).get(0).getAssetId());
        Assertions.assertEquals("1|dwd||ajxx_tab", upstreamCaptor.getAllValues().get(1).get(0).getAssetId());
        Assertions.assertEquals(2, upstreamCaptor.getAllValues().get(0).get(0).getFieldMappings().size());
        Assertions.assertEquals("SQL目标表", downstreamCaptor.getAllValues().get(0).getRelationType());
    }

    @Test
    public void shouldRegisterMultipleUpstreamTablesForJoinSql() {
        WorkflowSqlLineageServiceImpl service = new WorkflowSqlLineageServiceImpl();
        DataGovernanceStore store = Mockito.mock(DataGovernanceStore.class);
        DataSourceMapper dataSourceMapper = Mockito.mock(DataSourceMapper.class);
        ReflectionTestUtils.setField(service, "sqlLineageParseService", new SqlLineageParseServiceImpl());
        ReflectionTestUtils.setField(service, "dataGovernanceStore", store);
        ReflectionTestUtils.setField(service, "dataSourceMapper", dataSourceMapper);

        Mockito.when(dataSourceMapper.selectById(1)).thenReturn(mysqlDataSource());

        TaskDefinitionLog task = sqlTask("join-sql", "insert into dwd.aj_suspect(ajbh, xyrmc) "
                + "select a.ajbh, x.xyrmc from ods.ajxx_tab a join ods.xyr_tab x on a.xyrbh = x.xyrbh");

        service.registerWorkflowSqlLineage(1002L, 1, "嫌疑人ETL", Arrays.asList(task));

        ArgumentCaptor<List<LineageNode>> upstreamCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(store).replaceLineages(Mockito.eq("1|dwd||aj_suspect"), upstreamCaptor.capture(),
                Mockito.any(LineageNode.class));

        Assertions.assertEquals(2, upstreamCaptor.getValue().size());
        Assertions.assertTrue(upstreamCaptor.getValue().stream().anyMatch(node -> "1|ods||ajxx_tab".equals(
                node.getAssetId())));
        Assertions.assertTrue(upstreamCaptor.getValue().stream().anyMatch(node -> "1|ods||xyr_tab".equals(
                node.getAssetId())));
    }

    @Test
    public void shouldMapOracleTwoPartNameAsSchemaAndTable() {
        WorkflowSqlLineageServiceImpl service = new WorkflowSqlLineageServiceImpl();
        DataGovernanceStore store = Mockito.mock(DataGovernanceStore.class);
        DataSourceMapper dataSourceMapper = Mockito.mock(DataSourceMapper.class);
        ReflectionTestUtils.setField(service, "sqlLineageParseService", new SqlLineageParseServiceImpl());
        ReflectionTestUtils.setField(service, "dataGovernanceStore", store);
        ReflectionTestUtils.setField(service, "dataSourceMapper", dataSourceMapper);

        DataSource oracle = new DataSource();
        oracle.setId(7);
        oracle.setName("oracle_demo");
        oracle.setType(DbType.ORACLE);
        oracle.setConnectionParams("{\"user\":\"SYSTEM\",\"password\":\"***\",\"database\":\"FREEPDB1\","
                + "\"jdbcUrl\":\"jdbc:oracle:thin:@localhost:1521/FREEPDB1\"}");
        Mockito.when(dataSourceMapper.selectById(7)).thenReturn(oracle);

        TaskDefinitionLog task = new TaskDefinitionLog();
        task.setTaskType("SQL");
        task.setName("oracle-sql");
        task.setTaskParams("{\"type\":\"ORACLE\",\"datasource\":7,\"sql\":\"insert into DWD.AJXX_TAB(AJBH) "
                + "select AJBH from ODS.AJXX_TAB\"}");

        service.registerWorkflowSqlLineage(1003L, 1, "Oracle案件ETL", Arrays.asList(task));

        Mockito.verify(store).replaceLineages(Mockito.eq("7|FREEPDB1|DWD|AJXX_TAB"), Mockito.anyList(),
                Mockito.any(LineageNode.class));
    }

    @Test
    public void shouldSplitMultiStatementSqlWithoutBreakingSemicolonInString() {
        List<String> statements = WorkflowSqlLineageServiceImpl.splitSqlStatements(
                "insert into dwd.t(name) select 'a;b' from ods.t; insert into dws.t select name from dwd.t;");

        Assertions.assertEquals(2, statements.size());
        Assertions.assertTrue(statements.get(0).contains("'a;b'"));
    }

    private TaskDefinitionLog sqlTask(String name, String sql) {
        TaskDefinitionLog task = new TaskDefinitionLog();
        task.setTaskType("SQL");
        task.setName(name);
        task.setTaskParams("{\"type\":\"MYSQL\",\"datasource\":1,\"sql\":\"" + sql.replace("\"", "\\\"") + "\"}");
        return task;
    }

    private DataSource mysqlDataSource() {
        DataSource dataSource = new DataSource();
        dataSource.setId(1);
        dataSource.setName("mysql_demo");
        dataSource.setType(DbType.MYSQL);
        dataSource.setConnectionParams("{\"user\":\"root\",\"password\":\"***\",\"database\":\"case_workbench\","
                + "\"jdbcUrl\":\"jdbc:mysql://localhost:3306/case_workbench\"}");
        return dataSource;
    }
}
