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

package org.apache.dolphinscheduler.installer.dto;

public class DatabaseCheckResult {

    private boolean success;

    private String code;

    private String message;

    private String databaseVersion;

    private boolean canCreateTable;

    private boolean canInsert;

    public static DatabaseCheckResult success(String databaseVersion) {
        DatabaseCheckResult result = new DatabaseCheckResult();
        result.setSuccess(true);
        result.setCode("SUCCESS");
        result.setMessage("数据库连接成功");
        result.setDatabaseVersion(databaseVersion);
        result.setCanCreateTable(true);
        result.setCanInsert(true);
        return result;
    }

    public static DatabaseCheckResult fail(String code, String message) {
        DatabaseCheckResult result = new DatabaseCheckResult();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDatabaseVersion() {
        return databaseVersion;
    }

    public void setDatabaseVersion(String databaseVersion) {
        this.databaseVersion = databaseVersion;
    }

    public boolean isCanCreateTable() {
        return canCreateTable;
    }

    public void setCanCreateTable(boolean canCreateTable) {
        this.canCreateTable = canCreateTable;
    }

    public boolean isCanInsert() {
        return canInsert;
    }

    public void setCanInsert(boolean canInsert) {
        this.canInsert = canInsert;
    }
}
