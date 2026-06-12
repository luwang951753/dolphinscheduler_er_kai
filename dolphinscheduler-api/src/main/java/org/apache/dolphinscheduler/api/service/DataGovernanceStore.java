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

package org.apache.dolphinscheduler.api.service;

import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.Issue;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.LineageNode;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.MetadataRequest;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.QualityRule;
import org.apache.dolphinscheduler.dao.entity.DataGovernanceIssue;
import org.apache.dolphinscheduler.dao.entity.DataGovernanceLineage;
import org.apache.dolphinscheduler.dao.entity.DataGovernanceMetadata;
import org.apache.dolphinscheduler.dao.entity.DataGovernanceRule;
import org.apache.dolphinscheduler.dao.mapper.DataGovernanceIssueMapper;
import org.apache.dolphinscheduler.dao.mapper.DataGovernanceLineageMapper;
import org.apache.dolphinscheduler.dao.mapper.DataGovernanceMetadataMapper;
import org.apache.dolphinscheduler.dao.mapper.DataGovernanceRuleMapper;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class DataGovernanceStore {

    private static final String DIRECTION_UPSTREAM = "UPSTREAM";
    private static final String DIRECTION_DOWNSTREAM = "DOWNSTREAM";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DataGovernanceMetadataMapper metadataMapper;

    @Autowired
    private DataGovernanceRuleMapper ruleMapper;

    @Autowired
    private DataGovernanceIssueMapper issueMapper;

    @Autowired
    private DataGovernanceLineageMapper lineageMapper;

    public StoreState snapshot() {
        StoreState state = new StoreState();
        metadataMapper.selectList(new QueryWrapper<DataGovernanceMetadata>().lambda()
                .orderByAsc(DataGovernanceMetadata::getId))
                .forEach(row -> state.metadata.put(row.getAssetId(), toMetadata(row)));
        ruleMapper.selectList(new QueryWrapper<DataGovernanceRule>().lambda()
                .orderByDesc(DataGovernanceRule::getUpdateTime)
                .orderByDesc(DataGovernanceRule::getId))
                .forEach(row -> state.rules.computeIfAbsent(row.getAssetId(), key -> new ArrayList<>())
                        .add(toRule(row)));
        issueMapper.selectList(new QueryWrapper<DataGovernanceIssue>().lambda()
                .orderByDesc(DataGovernanceIssue::getUpdateTime)
                .orderByDesc(DataGovernanceIssue::getId))
                .forEach(row -> state.issues.computeIfAbsent(row.getAssetId(), key -> new ArrayList<>())
                        .add(toIssue(row)));
        lineageMapper.selectList(new QueryWrapper<DataGovernanceLineage>().lambda()
                .orderByDesc(DataGovernanceLineage::getUpdateTime)
                .orderByDesc(DataGovernanceLineage::getId))
                .forEach(row -> {
                    LineageNode node = toLineageNode(row);
                    if (DIRECTION_UPSTREAM.equals(row.getDirection())) {
                        state.upstream.computeIfAbsent(row.getAssetId(), key -> new ArrayList<>()).add(node);
                    } else if (DIRECTION_DOWNSTREAM.equals(row.getDirection())) {
                        state.downstream.computeIfAbsent(row.getAssetId(), key -> new ArrayList<>()).add(node);
                    }
                });
        return state;
    }

    public MetadataRequest getMetadata(String assetId) {
        DataGovernanceMetadata row = metadataMapper.selectOne(new QueryWrapper<DataGovernanceMetadata>().lambda()
                .eq(DataGovernanceMetadata::getAssetId, assetId)
                .last("limit 1"));
        return row == null ? null : toMetadata(row);
    }

    public MetadataRequest saveMetadata(String assetId, MetadataRequest metadata) {
        DataGovernanceMetadata row = metadataMapper.selectOne(new QueryWrapper<DataGovernanceMetadata>().lambda()
                .eq(DataGovernanceMetadata::getAssetId, assetId)
                .last("limit 1"));
        Date now = new Date();
        if (row == null) {
            row = new DataGovernanceMetadata();
            row.setAssetId(assetId);
            row.setCreateTime(now);
        }
        row.setOwner(metadata == null ? "" : metadata.getOwner());
        row.setDescription(metadata == null ? "" : metadata.getDescription());
        row.setTagsJson(toJson(metadata == null ? new ArrayList<>() : metadata.getTags()));
        row.setUpdateTime(now);
        if (row.getId() == null) {
            metadataMapper.insert(row);
        } else {
            metadataMapper.updateById(row);
        }
        return metadata;
    }

    public List<QualityRule> getRules(String assetId) {
        return ruleMapper.selectList(new QueryWrapper<DataGovernanceRule>().lambda()
                .eq(DataGovernanceRule::getAssetId, assetId)
                .orderByDesc(DataGovernanceRule::getUpdateTime)
                .orderByDesc(DataGovernanceRule::getId))
                .stream()
                .map(this::toRule)
                .collect(Collectors.toList());
    }

    public QualityRule saveRule(String assetId, QualityRule rule) {
        DataGovernanceRule row = ruleMapper.selectOne(new QueryWrapper<DataGovernanceRule>().lambda()
                .eq(DataGovernanceRule::getAssetId, assetId)
                .eq(DataGovernanceRule::getRuleCode, rule.getId())
                .last("limit 1"));
        Date now = new Date();
        if (row == null) {
            row = new DataGovernanceRule();
            row.setAssetId(assetId);
            row.setRuleCode(rule.getId());
            row.setCreateTime(now);
        }
        row.setName(rule.getName());
        row.setType(rule.getType());
        row.setLevel(rule.getLevel());
        row.setFieldName(rule.getFieldName());
        row.setSeverity(rule.getSeverity());
        row.setFrequency(rule.getFrequency());
        row.setEnabled(Boolean.TRUE.equals(rule.getEnabled()));
        row.setStatus(rule.getStatus());
        row.setLastRunAt(rule.getLastRunAt());
        row.setAbnormalCount(rule.getAbnormalCount());
        row.setAbnormalRate(rule.getAbnormalRate());
        row.setPayloadJson(toJson(rule));
        row.setUpdateTime(now);
        if (row.getId() == null) {
            ruleMapper.insert(row);
        } else {
            ruleMapper.updateById(row);
        }
        return rule;
    }

    public List<Issue> getIssues(String assetId) {
        return issueMapper.selectList(new QueryWrapper<DataGovernanceIssue>().lambda()
                .eq(DataGovernanceIssue::getAssetId, assetId)
                .orderByDesc(DataGovernanceIssue::getUpdateTime)
                .orderByDesc(DataGovernanceIssue::getId))
                .stream()
                .map(this::toIssue)
                .collect(Collectors.toList());
    }

    public Issue saveIssue(String assetId, Issue issue) {
        DataGovernanceIssue row = issueMapper.selectOne(new QueryWrapper<DataGovernanceIssue>().lambda()
                .eq(DataGovernanceIssue::getAssetId, assetId)
                .eq(DataGovernanceIssue::getIssueCode, issue.getId())
                .last("limit 1"));
        Date now = new Date();
        if (row == null) {
            row = new DataGovernanceIssue();
            row.setAssetId(assetId);
            row.setIssueCode(issue.getId());
            row.setCreateTime(now);
        }
        row.setRuleId(issue.getRuleId());
        row.setTitle(issue.getTitle());
        row.setSeverity(issue.getSeverity());
        row.setStatus(issue.getStatus());
        row.setAbnormalCount(issue.getAbnormalCount());
        row.setDiscoveredAt(issue.getDiscoveredAt());
        row.setResolvedAt(
                StringUtils.equals(issue.getStatus(), "RESOLVED") ? issue.getUpdatedAt() : row.getResolvedAt());
        row.setPayloadJson(toJson(issue));
        row.setUpdateTime(now);
        if (row.getId() == null) {
            issueMapper.insert(row);
        } else {
            issueMapper.updateById(row);
        }
        return issue;
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceLineage(String targetAssetId, LineageNode upstreamNode, LineageNode downstreamNode) {
        List<LineageNode> upstreamNodes = new ArrayList<>();
        if (upstreamNode != null) {
            upstreamNodes.add(upstreamNode);
        }
        replaceLineages(targetAssetId, upstreamNodes, downstreamNode);
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceLineages(String targetAssetId, List<LineageNode> upstreamNodes, LineageNode downstreamNode) {
        lineageMapper.delete(new QueryWrapper<DataGovernanceLineage>().lambda()
                .eq(DataGovernanceLineage::getAssetId, targetAssetId)
                .eq(DataGovernanceLineage::getDirection, DIRECTION_UPSTREAM));
        List<LineageNode> safeUpstreamNodes = upstreamNodes == null ? new ArrayList<>() : upstreamNodes;
        for (LineageNode upstreamNode : safeUpstreamNodes) {
            if (upstreamNode != null && StringUtils.isNotBlank(upstreamNode.getAssetId())) {
                saveLineage(targetAssetId, upstreamNode, DIRECTION_UPSTREAM);
            }
        }
        lineageMapper.delete(new QueryWrapper<DataGovernanceLineage>().lambda()
                .eq(DataGovernanceLineage::getAssetId, targetAssetId)
                .eq(DataGovernanceLineage::getRelatedAssetId, targetAssetId)
                .eq(DataGovernanceLineage::getDirection, DIRECTION_DOWNSTREAM));
        if (downstreamNode == null || StringUtils.isBlank(downstreamNode.getAssetId())) {
            return;
        }
        for (LineageNode upstreamNode : safeUpstreamNodes) {
            if (upstreamNode == null || StringUtils.isBlank(upstreamNode.getAssetId())) {
                continue;
            }
            lineageMapper.delete(new QueryWrapper<DataGovernanceLineage>().lambda()
                    .eq(DataGovernanceLineage::getAssetId, upstreamNode.getAssetId())
                    .eq(DataGovernanceLineage::getRelatedAssetId, targetAssetId)
                    .eq(DataGovernanceLineage::getDirection, DIRECTION_DOWNSTREAM));
            saveLineage(upstreamNode.getAssetId(), downstreamNode, DIRECTION_DOWNSTREAM);
        }
    }

    public List<LineageNode> getUpstream(String assetId) {
        return getLineage(assetId, DIRECTION_UPSTREAM);
    }

    public List<LineageNode> getDownstream(String assetId) {
        return getLineage(assetId, DIRECTION_DOWNSTREAM);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteLineageBySyncTaskName(String syncTaskName) {
        if (StringUtils.isBlank(syncTaskName)) {
            return 0;
        }
        return lineageMapper.delete(new QueryWrapper<DataGovernanceLineage>().lambda()
                .eq(DataGovernanceLineage::getSyncTaskName, syncTaskName));
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteLineageBySyncTaskNamePrefix(String syncTaskNamePrefix) {
        if (StringUtils.isBlank(syncTaskNamePrefix)) {
            return 0;
        }
        return lineageMapper.delete(new QueryWrapper<DataGovernanceLineage>().lambda()
                .likeRight(DataGovernanceLineage::getSyncTaskName, syncTaskNamePrefix));
    }

    @Transactional(rollbackFor = Exception.class)
    public int repairRunningLineageStatus(String syncTaskName, String lastRunStatus, String lastRunTime) {
        List<DataGovernanceLineage> rows = lineageMapper.selectList(new QueryWrapper<DataGovernanceLineage>().lambda()
                .eq(DataGovernanceLineage::getSyncTaskName, syncTaskName)
                .eq(DataGovernanceLineage::getLastRunStatus, "RUNNING"));
        Date now = new Date();
        int repairedRows = 0;
        for (DataGovernanceLineage row : rows) {
            LineageNode node = toLineageNode(row);
            node.setLastRunStatus(lastRunStatus);
            node.setLastRunTime(lastRunTime);
            row.setLastRunStatus(lastRunStatus);
            row.setLastRunTime(lastRunTime);
            row.setPayloadJson(toJson(node));
            row.setUpdateTime(now);
            repairedRows += lineageMapper.updateById(row);
        }
        return repairedRows;
    }

    private List<LineageNode> getLineage(String assetId, String direction) {
        return lineageMapper.selectList(new QueryWrapper<DataGovernanceLineage>().lambda()
                .eq(DataGovernanceLineage::getAssetId, assetId)
                .eq(DataGovernanceLineage::getDirection, direction)
                .orderByDesc(DataGovernanceLineage::getUpdateTime)
                .orderByDesc(DataGovernanceLineage::getId))
                .stream()
                .map(this::toLineageNode)
                .collect(Collectors.toList());
    }

    private void saveLineage(String assetId, LineageNode node, String direction) {
        Date now = new Date();
        DataGovernanceLineage row = new DataGovernanceLineage();
        row.setAssetId(assetId);
        row.setRelatedAssetId(node.getAssetId());
        row.setDirection(direction);
        row.setSyncTaskName(node.getSyncTaskName());
        row.setLastRunStatus(node.getLastRunStatus());
        row.setLastRunTime(node.getLastRunTime());
        row.setPayloadJson(toJson(node));
        row.setCreateTime(now);
        row.setUpdateTime(now);
        lineageMapper.insert(row);
    }

    private MetadataRequest toMetadata(DataGovernanceMetadata row) {
        MetadataRequest metadata = new MetadataRequest();
        metadata.setOwner(row.getOwner());
        metadata.setDescription(row.getDescription());
        metadata.setTags(readJson(row.getTagsJson(), new TypeReference<List<String>>() {
        }, new ArrayList<>()));
        return metadata;
    }

    private QualityRule toRule(DataGovernanceRule row) {
        QualityRule rule = readJson(row.getPayloadJson(), QualityRule.class, new QualityRule());
        rule.setId(StringUtils.defaultIfBlank(rule.getId(), row.getRuleCode()));
        rule.setAssetId(StringUtils.defaultIfBlank(rule.getAssetId(), row.getAssetId()));
        rule.setName(StringUtils.defaultIfBlank(rule.getName(), row.getName()));
        rule.setType(StringUtils.defaultIfBlank(rule.getType(), row.getType()));
        rule.setLevel(StringUtils.defaultIfBlank(rule.getLevel(), row.getLevel()));
        rule.setFieldName(StringUtils.defaultIfBlank(rule.getFieldName(), row.getFieldName()));
        rule.setSeverity(StringUtils.defaultIfBlank(rule.getSeverity(), row.getSeverity()));
        rule.setFrequency(StringUtils.defaultIfBlank(rule.getFrequency(), row.getFrequency()));
        rule.setEnabled(rule.getEnabled() == null ? row.getEnabled() : rule.getEnabled());
        rule.setStatus(StringUtils.defaultIfBlank(rule.getStatus(), row.getStatus()));
        rule.setLastRunAt(StringUtils.defaultIfBlank(rule.getLastRunAt(), row.getLastRunAt()));
        rule.setAbnormalCount(rule.getAbnormalCount() == null ? row.getAbnormalCount() : rule.getAbnormalCount());
        rule.setAbnormalRate(rule.getAbnormalRate() == null ? row.getAbnormalRate() : rule.getAbnormalRate());
        return rule;
    }

    private Issue toIssue(DataGovernanceIssue row) {
        Issue issue = readJson(row.getPayloadJson(), Issue.class, new Issue());
        issue.setId(StringUtils.defaultIfBlank(issue.getId(), row.getIssueCode()));
        issue.setAssetId(StringUtils.defaultIfBlank(issue.getAssetId(), row.getAssetId()));
        issue.setRuleId(StringUtils.defaultIfBlank(issue.getRuleId(), row.getRuleId()));
        issue.setTitle(StringUtils.defaultIfBlank(issue.getTitle(), row.getTitle()));
        issue.setSeverity(StringUtils.defaultIfBlank(issue.getSeverity(), row.getSeverity()));
        issue.setStatus(StringUtils.defaultIfBlank(issue.getStatus(), row.getStatus()));
        issue.setAbnormalCount(issue.getAbnormalCount() == null ? row.getAbnormalCount() : issue.getAbnormalCount());
        issue.setDiscoveredAt(StringUtils.defaultIfBlank(issue.getDiscoveredAt(), row.getDiscoveredAt()));
        issue.setUpdatedAt(StringUtils.defaultIfBlank(issue.getUpdatedAt(),
                row.getUpdateTime() == null ? null : row.getUpdateTime().toString()));
        return issue;
    }

    private LineageNode toLineageNode(DataGovernanceLineage row) {
        LineageNode node = readJson(row.getPayloadJson(), LineageNode.class, new LineageNode());
        node.setAssetId(StringUtils.defaultIfBlank(node.getAssetId(), row.getRelatedAssetId()));
        node.setSyncTaskName(StringUtils.defaultIfBlank(node.getSyncTaskName(), row.getSyncTaskName()));
        node.setLastRunStatus(StringUtils.defaultIfBlank(node.getLastRunStatus(), row.getLastRunStatus()));
        node.setLastRunTime(StringUtils.defaultIfBlank(node.getLastRunTime(), row.getLastRunTime()));
        return node;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Serialize data governance payload failed.", ex);
            return "{}";
        }
    }

    private <T> T readJson(String json, Class<T> type, T defaultValue) {
        if (StringUtils.isBlank(json)) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.warn("Deserialize data governance payload failed.", ex);
            return defaultValue;
        }
    }

    private <T> T readJson(String json, TypeReference<T> type, T defaultValue) {
        if (StringUtils.isBlank(json)) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.warn("Deserialize data governance payload failed.", ex);
            return defaultValue;
        }
    }

    public static class StoreState {

        private Map<String, MetadataRequest> metadata = new LinkedHashMap<>();
        private Map<String, List<QualityRule>> rules = new LinkedHashMap<>();
        private Map<String, List<Issue>> issues = new LinkedHashMap<>();
        private Map<String, List<LineageNode>> upstream = new LinkedHashMap<>();
        private Map<String, List<LineageNode>> downstream = new LinkedHashMap<>();

        public Map<String, MetadataRequest> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, MetadataRequest> metadata) {
            this.metadata = metadata;
        }

        public Map<String, List<QualityRule>> getRules() {
            return rules;
        }

        public void setRules(Map<String, List<QualityRule>> rules) {
            this.rules = rules;
        }

        public Map<String, List<Issue>> getIssues() {
            return issues;
        }

        public void setIssues(Map<String, List<Issue>> issues) {
            this.issues = issues;
        }

        public Map<String, List<LineageNode>> getUpstream() {
            return upstream;
        }

        public void setUpstream(Map<String, List<LineageNode>> upstream) {
            this.upstream = upstream;
        }

        public Map<String, List<LineageNode>> getDownstream() {
            return downstream;
        }

        public void setDownstream(Map<String, List<LineageNode>> downstream) {
            this.downstream = downstream;
        }
    }
}
