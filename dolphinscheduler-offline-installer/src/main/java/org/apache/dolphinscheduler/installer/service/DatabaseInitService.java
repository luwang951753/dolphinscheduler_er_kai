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

package org.apache.dolphinscheduler.installer.service;

import org.apache.dolphinscheduler.installer.core.InstallContext;
import org.apache.dolphinscheduler.installer.dto.InstallConfigRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class DatabaseInitService {

    public String initializeIfNeeded(InstallContext context, InstallConfigRequest request) throws Exception {
        InstallConfigRequest.Database database = request.getDatabase();
        if (database == null || !database.isInitDatabase()) {
            return "未选择初始化数据库，跳过";
        }
        if (!"MYSQL".equalsIgnoreCase(database.getType())) {
            return "当前安装器仅支持自动初始化 MySQL，跳过";
        }
        if (!DatabaseCheckService.isSafeMysqlIdentifier(database.getDatabase())) {
            throw new IllegalArgumentException("MySQL 数据库名只能包含字母、数字和下划线，长度不能超过 64");
        }

        String jdbcUrl = buildJdbcUrl(database);
        try (
                Connection connection =
                        DriverManager.getConnection(jdbcUrl, database.getUsername(), database.getPassword())) {
            int existingTableCount = countExistingTables(connection, database.getDatabase());
            if (existingTableCount > 0) {
                int patchedTableCount = createDataFlowExtensionTablesIfNeeded(connection);
                return "目标库已有 " + existingTableCount + " 张表，跳过全量初始化，已补齐 "
                        + patchedTableCount + " 张 DataFlow 二开扩展表";
            }
            Path schema = context.getConfDir().resolve("sql").resolve("dolphinscheduler_mysql.sql");
            executeSqlScript(connection, schema);
            return "已初始化 MySQL schema: " + database.getDatabase();
        }
    }

    private String buildJdbcUrl(InstallConfigRequest.Database database) {
        return "jdbc:mysql://" + database.getHost() + ":" + database.getPort() + "/" + database.getDatabase()
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }

    private int countExistingTables(Connection connection, String databaseName) throws Exception {
        String sql = "select count(*) from information_schema.tables where table_schema = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int createDataFlowExtensionTablesIfNeeded(Connection connection) throws Exception {
        int createdCount = 0;
        for (String tableName : Arrays.asList(
                "t_ds_data_preview_view",
                "t_ds_user_module_permission",
                "t_ds_data_governance_metadata",
                "t_ds_data_governance_rule",
                "t_ds_data_governance_issue",
                "t_ds_data_governance_lineage")) {
            if (!tableExists(connection, tableName)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(dataFlowExtensionTableDdl(tableName));
                    createdCount++;
                }
            }
        }
        return createdCount;
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        try (
                ResultSet resultSet = connection.getMetaData().getTables(
                        connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private String dataFlowExtensionTableDdl(String tableName) {
        switch (tableName) {
            case "t_ds_data_preview_view":
                return "CREATE TABLE `t_ds_data_preview_view` ("
                        + "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'key',"
                        + "`user_id` int(11) NOT NULL COMMENT 'user id',"
                        + "`datasource_id` int(11) NOT NULL COMMENT 'datasource id',"
                        + "`database_name` varchar(255) NOT NULL COMMENT 'database name',"
                        + "`schema_name` varchar(255) NOT NULL DEFAULT '' COMMENT 'schema name',"
                        + "`table_name` varchar(255) NOT NULL COMMENT 'table name',"
                        + "`view_name` varchar(64) NOT NULL COMMENT 'view name',"
                        + "`view_config` text NOT NULL COMMENT 'view config json',"
                        + "`create_time` datetime NOT NULL COMMENT 'create time',"
                        + "`update_time` datetime DEFAULT NULL COMMENT 'update time',"
                        + "PRIMARY KEY (`id`),"
                        + "UNIQUE KEY `unique_data_preview_view_scope_name` (`user_id`,`datasource_id`,`database_name`,`schema_name`,`table_name`,`view_name`),"
                        + "KEY `idx_data_preview_view_scope` (`user_id`,`datasource_id`,`database_name`,`schema_name`,`table_name`)"
                        + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE = utf8_bin";
            case "t_ds_user_module_permission":
                return "CREATE TABLE `t_ds_user_module_permission` ("
                        + "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'key',"
                        + "`user_id` int(11) NOT NULL COMMENT 'user id',"
                        + "`module_key` varchar(128) NOT NULL COMMENT 'module permission key',"
                        + "`create_time` datetime DEFAULT NULL COMMENT 'create time',"
                        + "`update_time` datetime DEFAULT NULL COMMENT 'update time',"
                        + "PRIMARY KEY (`id`),"
                        + "UNIQUE KEY `uk_user_module_permission` (`user_id`, `module_key`),"
                        + "KEY `idx_user_module_permission_user_id` (`user_id`)"
                        + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE = utf8_bin";
            case "t_ds_data_governance_metadata":
                return "CREATE TABLE `t_ds_data_governance_metadata` ("
                        + "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'key',"
                        + "`asset_id` varchar(512) NOT NULL COMMENT 'asset id',"
                        + "`owner` varchar(128) DEFAULT NULL COMMENT 'business owner',"
                        + "`description` text DEFAULT NULL COMMENT 'asset description',"
                        + "`tags_json` text DEFAULT NULL COMMENT 'tags json',"
                        + "`create_time` datetime DEFAULT NULL COMMENT 'create time',"
                        + "`update_time` datetime DEFAULT NULL COMMENT 'update time',"
                        + "PRIMARY KEY (`id`),"
                        + "UNIQUE KEY `uk_data_governance_metadata_asset` (`asset_id`)"
                        + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE = utf8mb4_bin";
            case "t_ds_data_governance_rule":
                return "CREATE TABLE `t_ds_data_governance_rule` ("
                        + "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'key',"
                        + "`rule_code` varchar(128) NOT NULL COMMENT 'rule code',"
                        + "`asset_id` varchar(512) NOT NULL COMMENT 'asset id',"
                        + "`name` varchar(255) DEFAULT NULL COMMENT 'rule name',"
                        + "`type` varchar(64) DEFAULT NULL COMMENT 'rule type',"
                        + "`level` varchar(32) DEFAULT NULL COMMENT 'rule level',"
                        + "`field_name` varchar(255) DEFAULT NULL COMMENT 'field name',"
                        + "`severity` varchar(32) DEFAULT NULL COMMENT 'severity',"
                        + "`frequency` varchar(64) DEFAULT NULL COMMENT 'run frequency',"
                        + "`enabled` tinyint(1) DEFAULT 1 COMMENT 'enabled',"
                        + "`status` varchar(32) DEFAULT NULL COMMENT 'run status',"
                        + "`last_run_at` varchar(64) DEFAULT NULL COMMENT 'last run time',"
                        + "`abnormal_count` bigint DEFAULT NULL COMMENT 'abnormal count',"
                        + "`abnormal_rate` double DEFAULT NULL COMMENT 'abnormal rate',"
                        + "`payload_json` text DEFAULT NULL COMMENT 'rule payload json',"
                        + "`create_time` datetime DEFAULT NULL COMMENT 'create time',"
                        + "`update_time` datetime DEFAULT NULL COMMENT 'update time',"
                        + "PRIMARY KEY (`id`),"
                        + "UNIQUE KEY `uk_data_governance_rule_code` (`asset_id`, `rule_code`),"
                        + "KEY `idx_data_governance_rule_asset` (`asset_id`)"
                        + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE = utf8mb4_bin";
            case "t_ds_data_governance_issue":
                return "CREATE TABLE `t_ds_data_governance_issue` ("
                        + "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'key',"
                        + "`issue_code` varchar(128) NOT NULL COMMENT 'issue code',"
                        + "`asset_id` varchar(512) NOT NULL COMMENT 'asset id',"
                        + "`rule_id` varchar(128) DEFAULT NULL COMMENT 'rule id',"
                        + "`title` varchar(255) DEFAULT NULL COMMENT 'issue title',"
                        + "`severity` varchar(32) DEFAULT NULL COMMENT 'severity',"
                        + "`status` varchar(32) DEFAULT NULL COMMENT 'issue status',"
                        + "`abnormal_count` bigint DEFAULT NULL COMMENT 'abnormal count',"
                        + "`discovered_at` varchar(64) DEFAULT NULL COMMENT 'discovered time',"
                        + "`resolved_at` varchar(64) DEFAULT NULL COMMENT 'resolved time',"
                        + "`payload_json` text DEFAULT NULL COMMENT 'issue payload json',"
                        + "`create_time` datetime DEFAULT NULL COMMENT 'create time',"
                        + "`update_time` datetime DEFAULT NULL COMMENT 'update time',"
                        + "PRIMARY KEY (`id`),"
                        + "UNIQUE KEY `uk_data_governance_issue_code` (`asset_id`, `issue_code`),"
                        + "KEY `idx_data_governance_issue_asset` (`asset_id`)"
                        + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE = utf8mb4_bin";
            case "t_ds_data_governance_lineage":
                return "CREATE TABLE `t_ds_data_governance_lineage` ("
                        + "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'key',"
                        + "`asset_id` varchar(512) NOT NULL COMMENT 'asset id',"
                        + "`related_asset_id` varchar(512) NOT NULL COMMENT 'related asset id',"
                        + "`direction` varchar(32) NOT NULL COMMENT 'lineage direction',"
                        + "`sync_task_name` varchar(255) DEFAULT NULL COMMENT 'sync task name',"
                        + "`last_run_status` varchar(64) DEFAULT NULL COMMENT 'last run status',"
                        + "`last_run_time` varchar(64) DEFAULT NULL COMMENT 'last run time',"
                        + "`payload_json` text DEFAULT NULL COMMENT 'lineage payload json',"
                        + "`create_time` datetime DEFAULT NULL COMMENT 'create time',"
                        + "`update_time` datetime DEFAULT NULL COMMENT 'update time',"
                        + "PRIMARY KEY (`id`),"
                        + "KEY `idx_data_governance_lineage_asset` (`asset_id`, `direction`),"
                        + "KEY `idx_data_governance_lineage_related` (`related_asset_id`)"
                        + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE = utf8mb4_bin";
            default:
                throw new IllegalArgumentException("Unsupported DataFlow extension table: " + tableName);
        }
    }

    private void executeSqlScript(Connection connection, Path schema) throws Exception {
        if (!Files.exists(schema)) {
            throw new IOException("数据库初始化脚本不存在: " + schema);
        }
        for (String sql : splitSqlStatements(new String(Files.readAllBytes(schema), StandardCharsets.UTF_8))) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
    }

    private List<String> splitSqlStatements(String sqlScript) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inBlockComment = false;
        for (String line : sqlScript.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (inBlockComment) {
                if (trimmed.endsWith("*/")) {
                    inBlockComment = false;
                }
                continue;
            }
            if (trimmed.startsWith("/*")) {
                if (!trimmed.endsWith("*/")) {
                    inBlockComment = true;
                }
                continue;
            }
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String statement = current.toString().trim();
                statements.add(statement.substring(0, statement.length() - 1).trim());
                current.setLength(0);
            }
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }
}
