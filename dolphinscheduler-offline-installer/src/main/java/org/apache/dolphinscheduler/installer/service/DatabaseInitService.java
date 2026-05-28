/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.dolphinscheduler.installer.service;

import org.apache.dolphinscheduler.installer.core.InstallContext;
import org.apache.dolphinscheduler.installer.dto.InstallConfigRequest;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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

        String jdbcUrl = buildJdbcUrl(database);
        try (Connection connection = DriverManager.getConnection(jdbcUrl, database.getUsername(), database.getPassword())) {
            int existingTableCount = countExistingTables(connection, database.getDatabase());
            if (existingTableCount > 0) {
                return "目标库已有 " + existingTableCount + " 张表，跳过初始化，避免覆盖已有数据";
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
        String sql = "select count(*) from information_schema.tables where table_schema = '" + databaseName + "'";
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
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
