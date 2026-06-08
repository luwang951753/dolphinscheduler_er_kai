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

public class InstallConfigRequest {

    private String javaHome;

    private String installDir;

    private int dolphinPort;

    private Database database = new Database();

    private ServiceConfig service = new ServiceConfig();

    private SyncConfig sync = new SyncConfig();

    public String getJavaHome() {
        return javaHome;
    }

    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    public String getInstallDir() {
        return installDir;
    }

    public void setInstallDir(String installDir) {
        this.installDir = installDir;
    }

    public int getDolphinPort() {
        return dolphinPort;
    }

    public void setDolphinPort(int dolphinPort) {
        this.dolphinPort = dolphinPort;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public ServiceConfig getService() {
        return service;
    }

    public void setService(ServiceConfig service) {
        this.service = service;
    }

    public SyncConfig getSync() {
        return sync;
    }

    public void setSync(SyncConfig sync) {
        this.sync = sync;
    }

    public static class Database {

        private String type;

        private String host;

        private int port;

        private String database;

        private String username;

        private String password;

        private boolean initDatabase;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isInitDatabase() {
            return initDatabase;
        }

        public void setInitDatabase(boolean initDatabase) {
            this.initDatabase = initDatabase;
        }
    }

    public static class ServiceConfig {

        private String publicHost;

        private String logDir;

        private String resourceDir;

        private String timezone;

        public String getPublicHost() {
            return publicHost;
        }

        public void setPublicHost(String publicHost) {
            this.publicHost = publicHost;
        }

        public String getLogDir() {
            return logDir;
        }

        public void setLogDir(String logDir) {
            this.logDir = logDir;
        }

        public String getResourceDir() {
            return resourceDir;
        }

        public void setResourceDir(String resourceDir) {
            this.resourceDir = resourceDir;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }
    }

    public static class SyncConfig {

        private String seatunnelHome;

        private String tmpDir;

        private String logDir;

        private String jdbcDir;

        public String getSeatunnelHome() {
            return seatunnelHome;
        }

        public void setSeatunnelHome(String seatunnelHome) {
            this.seatunnelHome = seatunnelHome;
        }

        public String getTmpDir() {
            return tmpDir;
        }

        public void setTmpDir(String tmpDir) {
            this.tmpDir = tmpDir;
        }

        public String getLogDir() {
            return logDir;
        }

        public void setLogDir(String logDir) {
            this.logDir = logDir;
        }

        public String getJdbcDir() {
            return jdbcDir;
        }

        public void setJdbcDir(String jdbcDir) {
            this.jdbcDir = jdbcDir;
        }
    }
}
