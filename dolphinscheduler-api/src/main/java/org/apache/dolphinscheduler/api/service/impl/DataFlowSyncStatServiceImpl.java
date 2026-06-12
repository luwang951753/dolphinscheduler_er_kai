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

import static org.apache.dolphinscheduler.api.constants.ApiFuncIdentificationConstant.WORKFLOW_INSTANCE;

import org.apache.dolphinscheduler.api.dto.DataFlowSyncStatDtos.QueryRequest;
import org.apache.dolphinscheduler.api.dto.DataFlowSyncStatDtos.StatResponse;
import org.apache.dolphinscheduler.api.dto.DataFlowSyncStatDtos.UpsertRequest;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.api.service.DataFlowSyncStatService;
import org.apache.dolphinscheduler.api.service.LoggerService;
import org.apache.dolphinscheduler.api.service.ProjectService;
import org.apache.dolphinscheduler.common.utils.DateUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.DataFlowSyncInstanceStat;
import org.apache.dolphinscheduler.dao.entity.TaskInstance;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.entity.WorkflowInstance;
import org.apache.dolphinscheduler.dao.mapper.DataFlowSyncInstanceStatMapper;
import org.apache.dolphinscheduler.dao.mapper.TaskInstanceMapper;
import org.apache.dolphinscheduler.dao.mapper.WorkflowInstanceMapper;
import org.apache.dolphinscheduler.plugin.task.api.enums.TaskExecutionStatus;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

@Service
public class DataFlowSyncStatServiceImpl extends BaseServiceImpl implements DataFlowSyncStatService {

    private static final int MAX_INSTANCE_IDS = 100;
    private static final int LOG_READ_LINE_LIMIT = 12000;

    private static final Pattern TOTAL_READ_COUNT_PATTERN = counterPattern("Total Read Count");
    private static final Pattern TOTAL_WRITE_COUNT_PATTERN = counterPattern("Total Write Count");
    private static final Pattern NUMBER_TOTAL_ROWS_PATTERN = counterPattern("NumberTotalRows");
    private static final Pattern NUMBER_LOADED_ROWS_PATTERN = counterPattern("NumberLoadedRows");

    @Autowired
    private ProjectService projectService;

    @Autowired
    private WorkflowInstanceMapper workflowInstanceMapper;

    @Autowired
    private TaskInstanceMapper taskInstanceMapper;

    @Autowired
    private DataFlowSyncInstanceStatMapper syncInstanceStatMapper;

    @Autowired
    private LoggerService loggerService;

    @Override
    public List<StatResponse> queryStats(User loginUser, QueryRequest request) {
        Long projectCode = requirePositiveLong(request == null ? null : request.getProjectCode());
        Long workflowDefinitionCode = requirePositiveLong(
                request == null ? null : request.getWorkflowDefinitionCode());
        List<Integer> workflowInstanceIds = normalizeInstanceIds(
                request == null ? null : request.getWorkflowInstanceIds());
        if (workflowInstanceIds.isEmpty()) {
            return new ArrayList<>();
        }
        projectService.checkProjectAndAuthThrowException(loginUser, projectCode, WORKFLOW_INSTANCE);
        List<DataFlowSyncInstanceStat> stats = queryPersistedStats(projectCode, workflowDefinitionCode,
                workflowInstanceIds);
        if (!Boolean.FALSE.equals(request.getRefreshMissing())) {
            stats.addAll(refreshMissingStats(loginUser, projectCode, workflowDefinitionCode, workflowInstanceIds,
                    stats));
        }
        return stats.stream()
                .sorted((left, right) -> Integer.compare(right.getWorkflowInstanceId(), left.getWorkflowInstanceId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StatResponse upsertStat(User loginUser, UpsertRequest request) {
        Long projectCode = requirePositiveLong(request == null ? null : request.getProjectCode());
        Long workflowDefinitionCode = requirePositiveLong(
                request == null ? null : request.getWorkflowDefinitionCode());
        Integer workflowInstanceId = requirePositiveInteger(request == null ? null : request.getWorkflowInstanceId());
        Integer taskInstanceId = request == null ? null : request.getTaskInstanceId();
        projectService.checkProjectAndAuthThrowException(loginUser, projectCode, WORKFLOW_INSTANCE);
        validateWorkflowInstance(projectCode, workflowDefinitionCode, workflowInstanceId);
        if (taskInstanceId != null) {
            validateTaskInstance(projectCode, workflowInstanceId, taskInstanceId);
        }

        QueryWrapper<DataFlowSyncInstanceStat> query = new QueryWrapper<>();
        query.eq("project_code", projectCode)
                .eq("workflow_definition_code", workflowDefinitionCode)
                .eq("workflow_instance_id", workflowInstanceId);
        DataFlowSyncInstanceStat stat = syncInstanceStatMapper.selectOne(query);
        Date now = new Date();
        if (stat == null) {
            stat = new DataFlowSyncInstanceStat();
            stat.setProjectCode(projectCode);
            stat.setWorkflowDefinitionCode(workflowDefinitionCode);
            stat.setWorkflowInstanceId(workflowInstanceId);
            stat.setCreateTime(now);
        }
        stat.setTaskInstanceId(taskInstanceId);
        stat.setReadRows(nonNegativeLong(request.getReadRows()));
        stat.setWriteRows(nonNegativeLong(request.getWriteRows()));
        stat.setFailedRows(nonNegativeLong(request.getFailedRows()));
        stat.setDurationSeconds(nonNegativeInteger(request.getDurationSeconds()));
        stat.setRunStatus(StringUtils.defaultIfBlank(request.getRunStatus(), "SUCCESS"));
        stat.setStatSource(StringUtils.defaultIfBlank(request.getStatSource(), "LOG_PARSE"));
        stat.setPayloadJson(StringUtils.left(StringUtils.defaultString(request.getPayloadJson()), 4000));
        stat.setUpdateTime(now);
        if (stat.getId() == null) {
            syncInstanceStatMapper.insert(stat);
        } else {
            syncInstanceStatMapper.updateById(stat);
        }
        return toResponse(stat);
    }

    private List<DataFlowSyncInstanceStat> queryPersistedStats(Long projectCode, Long workflowDefinitionCode,
                                                              List<Integer> workflowInstanceIds) {
        QueryWrapper<DataFlowSyncInstanceStat> query = new QueryWrapper<>();
        query.eq("project_code", projectCode)
                .eq("workflow_definition_code", workflowDefinitionCode)
                .in("workflow_instance_id", workflowInstanceIds)
                .orderByDesc("workflow_instance_id");
        return syncInstanceStatMapper.selectList(query);
    }

    private List<DataFlowSyncInstanceStat> refreshMissingStats(User loginUser,
                                                              Long projectCode,
                                                              Long workflowDefinitionCode,
                                                              List<Integer> workflowInstanceIds,
                                                              List<DataFlowSyncInstanceStat> persistedStats) {
        Set<Integer> persistedInstanceIds = persistedStats.stream()
                .map(DataFlowSyncInstanceStat::getWorkflowInstanceId)
                .collect(Collectors.toSet());
        List<DataFlowSyncInstanceStat> refreshedStats = new ArrayList<>();
        for (Integer workflowInstanceId : workflowInstanceIds) {
            if (persistedInstanceIds.contains(workflowInstanceId)) {
                continue;
            }
            DataFlowSyncInstanceStat refreshedStat =
                    parseAndPersistStat(loginUser, projectCode, workflowDefinitionCode, workflowInstanceId);
            if (refreshedStat != null) {
                refreshedStats.add(refreshedStat);
            }
        }
        return refreshedStats;
    }

    private DataFlowSyncInstanceStat parseAndPersistStat(User loginUser, Long projectCode,
                                                        Long workflowDefinitionCode, Integer workflowInstanceId) {
        WorkflowInstance workflowInstance = validateWorkflowInstance(projectCode, workflowDefinitionCode,
                workflowInstanceId);
        List<TaskInstance> taskInstances = taskInstanceMapper.findByWorkflowInstanceId(workflowInstanceId);
        if (CollectionUtils.isEmpty(taskInstances)) {
            return null;
        }

        long totalReadRows = 0L;
        long totalWriteRows = 0L;
        boolean hasReadRows = false;
        boolean hasWriteRows = false;
        TaskInstance latestTaskInstance = null;
        Map<String, Object> payload = new HashMap<>();
        for (TaskInstance taskInstance : taskInstances) {
            if (taskInstance == null || !TaskExecutionStatus.SUCCESS.equals(taskInstance.getState())) {
                continue;
            }
            if (latestTaskInstance == null || taskInstance.getId() > latestTaskInstance.getId()) {
                latestTaskInstance = taskInstance;
            }
            String taskLogText = safeQueryTaskLog(loginUser, projectCode, taskInstance.getId());
            SyncReadWriteCount taskCount = extractReadWriteCountFromLog(taskLogText);
            if (taskCount.readRows != null) {
                totalReadRows += taskCount.readRows;
                hasReadRows = true;
            }
            if (taskCount.writeRows != null) {
                totalWriteRows += taskCount.writeRows;
                hasWriteRows = true;
            }
        }

        if (!hasReadRows && !hasWriteRows) {
            return null;
        }

        DataFlowSyncInstanceStat stat = new DataFlowSyncInstanceStat();
        Date now = new Date();
        stat.setProjectCode(projectCode);
        stat.setWorkflowDefinitionCode(workflowDefinitionCode);
        stat.setWorkflowInstanceId(workflowInstanceId);
        stat.setTaskInstanceId(latestTaskInstance == null ? null : latestTaskInstance.getId());
        stat.setReadRows(hasReadRows ? totalReadRows : null);
        stat.setWriteRows(hasWriteRows ? totalWriteRows : null);
        stat.setFailedRows(0L);
        stat.setDurationSeconds(calculateDurationSeconds(workflowInstance));
        stat.setRunStatus(workflowInstance.getState() == null ? null : workflowInstance.getState().name());
        stat.setStatSource("SERVER_LOG_PARSE");
        payload.put("workflowInstanceName", workflowInstance.getName());
        payload.put("parsedTaskCount", taskInstances.size());
        stat.setPayloadJson(StringUtils.left(JSONUtils.toJsonString(payload), 4000));
        stat.setCreateTime(now);
        stat.setUpdateTime(now);
        syncInstanceStatMapper.insert(stat);
        return stat;
    }

    private String safeQueryTaskLog(User loginUser, Long projectCode, Integer taskInstanceId) {
        try {
            return loggerService.queryLog(loginUser, projectCode, taskInstanceId, 0, LOG_READ_LINE_LIMIT);
        } catch (Exception ex) {
            return StringUtils.EMPTY;
        }
    }

    private Integer calculateDurationSeconds(WorkflowInstance workflowInstance) {
        if (workflowInstance == null
                || workflowInstance.getStartTime() == null
                || workflowInstance.getEndTime() == null) {
            return null;
        }
        long durationMillis = workflowInstance.getEndTime().getTime() - workflowInstance.getStartTime().getTime();
        return durationMillis < 0 ? null : (int) (durationMillis / 1000);
    }

    private WorkflowInstance validateWorkflowInstance(Long projectCode, Long workflowDefinitionCode,
                                                      Integer workflowInstanceId) {
        WorkflowInstance workflowInstance = workflowInstanceMapper.selectById(workflowInstanceId);
        if (workflowInstance == null
                || !Objects.equals(workflowInstance.getProjectCode(), projectCode)
                || !Objects.equals(workflowInstance.getWorkflowDefinitionCode(), workflowDefinitionCode)) {
            throw new ServiceException(Status.WORKFLOW_INSTANCE_NOT_EXIST);
        }
        return workflowInstance;
    }

    private void validateTaskInstance(Long projectCode, Integer workflowInstanceId, Integer taskInstanceId) {
        TaskInstance taskInstance = taskInstanceMapper.selectById(taskInstanceId);
        if (taskInstance == null
                || !Objects.equals(taskInstance.getProjectCode(), projectCode)
                || taskInstance.getWorkflowInstanceId() != workflowInstanceId) {
            throw new ServiceException(Status.TASK_INSTANCE_NOT_FOUND);
        }
    }

    private Long requirePositiveLong(Long value) {
        if (value == null || value <= 0) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        return value;
    }

    private Integer requirePositiveInteger(Integer value) {
        if (value == null || value <= 0) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        return value;
    }

    private List<Integer> normalizeInstanceIds(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        Set<Integer> normalized = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .limit(MAX_INSTANCE_IDS)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(normalized);
    }

    private Long nonNegativeLong(Long value) {
        return value == null || value < 0 ? null : value;
    }

    private Integer nonNegativeInteger(Integer value) {
        return value == null || value < 0 ? null : value;
    }

    private StatResponse toResponse(DataFlowSyncInstanceStat stat) {
        StatResponse response = new StatResponse();
        response.setId(stat.getId());
        response.setProjectCode(stat.getProjectCode());
        response.setWorkflowDefinitionCode(stat.getWorkflowDefinitionCode());
        response.setWorkflowInstanceId(stat.getWorkflowInstanceId());
        response.setTaskInstanceId(stat.getTaskInstanceId());
        response.setReadRows(stat.getReadRows());
        response.setWriteRows(stat.getWriteRows());
        response.setFailedRows(stat.getFailedRows());
        response.setDurationSeconds(stat.getDurationSeconds());
        response.setRunStatus(stat.getRunStatus());
        response.setStatSource(stat.getStatSource());
        response.setPayloadJson(stat.getPayloadJson());
        response.setCreateTime(formatDate(stat.getCreateTime()));
        response.setUpdateTime(formatDate(stat.getUpdateTime()));
        return response;
    }

    private String formatDate(Date date) {
        return date == null ? null : DateUtils.dateToString(date);
    }

    private static Pattern counterPattern(String label) {
        return Pattern.compile("\"?" + Pattern.quote(label) + "\"?\\s*[:=]\\s*\"?([\\d,]+)\"?",
                Pattern.CASE_INSENSITIVE);
    }

    private static Long extractCounterFromLog(String logText, Pattern pattern) {
        if (StringUtils.isBlank(logText)) {
            return null;
        }
        Matcher matcher = pattern.matcher(logText);
        Long value = null;
        while (matcher.find()) {
            String counterText = matcher.group(1);
            if (StringUtils.isBlank(counterText)) {
                continue;
            }
            try {
                value = Long.parseLong(counterText.replace(",", ""));
            } catch (NumberFormatException ex) {
                value = null;
            }
        }
        return value;
    }

    private static Long firstCount(Long... values) {
        for (Long value : values) {
            if (value != null && value >= 0) {
                return value;
            }
        }
        return null;
    }

    private SyncReadWriteCount extractReadWriteCountFromLog(String logText) {
        Long totalReadCount = extractCounterFromLog(logText, TOTAL_READ_COUNT_PATTERN);
        Long totalWriteCount = extractCounterFromLog(logText, TOTAL_WRITE_COUNT_PATTERN);
        Long numberTotalRows = extractCounterFromLog(logText, NUMBER_TOTAL_ROWS_PATTERN);
        Long numberLoadedRows = extractCounterFromLog(logText, NUMBER_LOADED_ROWS_PATTERN);
        return new SyncReadWriteCount(
                firstCount(totalReadCount, numberTotalRows, numberLoadedRows),
                firstCount(totalWriteCount, numberLoadedRows, numberTotalRows));
    }

    private static final class SyncReadWriteCount {
        private final Long readRows;
        private final Long writeRows;

        private SyncReadWriteCount(Long readRows, Long writeRows) {
            this.readRows = readRows;
            this.writeRows = writeRows;
        }
    }
}
