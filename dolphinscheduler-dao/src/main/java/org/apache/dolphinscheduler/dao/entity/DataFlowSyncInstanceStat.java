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

package org.apache.dolphinscheduler.dao.entity;

import java.util.Date;

import lombok.Data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@TableName("t_ds_dataflow_sync_instance_stat")
public class DataFlowSyncInstanceStat {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("project_code")
    private Long projectCode;

    @TableField("workflow_definition_code")
    private Long workflowDefinitionCode;

    @TableField("workflow_instance_id")
    private Integer workflowInstanceId;

    @TableField("task_instance_id")
    private Integer taskInstanceId;

    @TableField("read_rows")
    private Long readRows;

    @TableField("write_rows")
    private Long writeRows;

    @TableField("failed_rows")
    private Long failedRows;

    @TableField("duration_seconds")
    private Integer durationSeconds;

    @TableField("run_status")
    private String runStatus;

    @TableField("stat_source")
    private String statSource;

    @TableField("payload_json")
    private String payloadJson;

    private Date createTime;

    private Date updateTime;
}
