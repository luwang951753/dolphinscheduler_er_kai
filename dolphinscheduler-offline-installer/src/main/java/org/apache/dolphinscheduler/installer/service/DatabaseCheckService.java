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

import org.apache.dolphinscheduler.installer.dto.DatabaseCheckRequest;
import org.apache.dolphinscheduler.installer.dto.DatabaseCheckResult;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

@Service
public class DatabaseCheckService {

    public DatabaseCheckResult check(DatabaseCheckRequest request) {
        DatabaseCheckResult invalidResult = validate(request);
        if (invalidResult != null) {
            return invalidResult;
        }

        String jdbcUrl = buildMysqlJdbcUrl(request);
        Properties properties = new Properties();
        properties.setProperty("user", request.getUsername());
        properties.setProperty("password", request.getPassword());

        try {
            DriverManager.setLoginTimeout(5);
            try (Connection connection = DriverManager.getConnection(jdbcUrl, properties);
                    Statement statement = connection.createStatement()) {
                statement.execute("select 1");
                return DatabaseCheckResult.success(queryDatabaseVersion(statement));
            }
        } catch (SQLException ex) {
            return DatabaseCheckResult.fail("DB_CONNECT_FAILED", "MySQL 连接失败，请检查主机、端口、库名和账号权限");
        }
    }

    private DatabaseCheckResult validate(DatabaseCheckRequest request) {
        if (request == null) {
            return DatabaseCheckResult.fail("INVALID_DATABASE_CONFIG", "请填写 MySQL 连接信息");
        }
        if (!"MYSQL".equalsIgnoreCase(request.getType())) {
            return DatabaseCheckResult.fail("INVALID_DATABASE_CONFIG", "第一版离线安装器只支持 MySQL 元数据库");
        }
        if (!StringUtils.hasText(request.getHost())) {
            return DatabaseCheckResult.fail("INVALID_DATABASE_CONFIG", "请填写 MySQL 主机地址");
        }
        if (request.getPort() <= 0 || request.getPort() > 65535) {
            return DatabaseCheckResult.fail("INVALID_DATABASE_CONFIG", "MySQL 端口必须在 1-65535 之间");
        }
        if (!StringUtils.hasText(request.getDatabase())) {
            return DatabaseCheckResult.fail("INVALID_DATABASE_CONFIG", "请填写 MySQL 数据库名");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            return DatabaseCheckResult.fail("INVALID_DATABASE_CONFIG", "请填写 MySQL 用户名");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            return DatabaseCheckResult.fail("INVALID_DATABASE_CONFIG", "请填写 MySQL 密码");
        }
        return null;
    }

    private String buildMysqlJdbcUrl(DatabaseCheckRequest request) {
        return "jdbc:mysql://" + request.getHost() + ":" + request.getPort() + "/" + request.getDatabase()
                + "?useSSL=false&connectTimeout=5000&socketTimeout=5000&serverTimezone=UTC";
    }

    private String queryDatabaseVersion(Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery("select version()")) {
            if (resultSet.next()) {
                return "MySQL " + resultSet.getString(1);
            }
            return "MySQL";
        }
    }
}
