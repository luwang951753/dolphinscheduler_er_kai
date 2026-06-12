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

package org.apache.dolphinscheduler.api.dto;

import java.util.List;

import lombok.Data;

public final class DataFlowSyncStatDtos {

    private DataFlowSyncStatDtos() {
    }

    @Data
    public static class QueryRequest {
        private Long projectCode;
        private Long workflowDefinitionCode;
        private List<Integer> workflowInstanceIds;
        private Boolean refreshMissing;
    }

    @Data
    public static class UpsertRequest {
        private Long projectCode;
        private Long workflowDefinitionCode;
        private Integer workflowInstanceId;
        private Integer taskInstanceId;
        private Long readRows;
        private Long writeRows;
        private Long failedRows;
        private Integer durationSeconds;
        private String runStatus;
        private String statSource;
        private String payloadJson;
    }

    @Data
    public static class StatResponse {
        private Integer id;
        private Long projectCode;
        private Long workflowDefinitionCode;
        private Integer workflowInstanceId;
        private Integer taskInstanceId;
        private Long readRows;
        private Long writeRows;
        private Long failedRows;
        private Integer durationSeconds;
        private String runStatus;
        private String statSource;
        private String payloadJson;
        private String createTime;
        private String updateTime;
    }
}
