/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.dolphinscheduler.installer.dto.DatabaseCheckRequest;
import org.apache.dolphinscheduler.installer.dto.DatabaseCheckResult;

import org.junit.jupiter.api.Test;

class DatabaseCheckServiceTest {

    private final DatabaseCheckService databaseCheckService = new DatabaseCheckService();

    @Test
    void shouldReturnValidationErrorWhenHostIsBlank() {
        DatabaseCheckRequest request = createValidRequest();
        request.setHost("");

        DatabaseCheckResult result = databaseCheckService.check(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("INVALID_DATABASE_CONFIG");
        assertThat(result.getMessage()).contains("主机");
    }

    @Test
    void shouldReturnValidationErrorWhenUsernameIsBlank() {
        DatabaseCheckRequest request = createValidRequest();
        request.setUsername("");

        DatabaseCheckResult result = databaseCheckService.check(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("INVALID_DATABASE_CONFIG");
        assertThat(result.getMessage()).contains("用户名");
    }

    @Test
    void shouldReturnValidationErrorWhenPasswordIsBlank() {
        DatabaseCheckRequest request = createValidRequest();
        request.setPassword("");

        DatabaseCheckResult result = databaseCheckService.check(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("INVALID_DATABASE_CONFIG");
        assertThat(result.getMessage()).contains("密码");
    }

    private DatabaseCheckRequest createValidRequest() {
        DatabaseCheckRequest request = new DatabaseCheckRequest();
        request.setType("MYSQL");
        request.setHost("127.0.0.1");
        request.setPort(3306);
        request.setDatabase("dolphinscheduler");
        request.setUsername("ds_user");
        request.setPassword("ds_password");
        return request;
    }
}
