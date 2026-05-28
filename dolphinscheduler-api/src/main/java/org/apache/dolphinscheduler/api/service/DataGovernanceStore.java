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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Slf4j
@Component
public class DataGovernanceStore {

    private static final String STORE_ENV = "DOLPHINSCHEDULER_DATA_GOVERNANCE_STORE";

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private File storeFile;
    private StoreState state;

    @PostConstruct
    public synchronized void init() {
        String configuredPath = System.getenv(STORE_ENV);
        String path = configuredPath == null || configuredPath.trim().isEmpty()
                ? System.getProperty("user.home") + File.separator + ".dolphinscheduler"
                        + File.separator + "data-governance-store.json"
                : configuredPath;
        storeFile = new File(path);
        File parent = storeFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.warn("Create data governance store directory failed: {}", parent);
        }
        if (!storeFile.exists()) {
            state = new StoreState();
            persist();
            return;
        }
        try {
            state = objectMapper.readValue(storeFile, StoreState.class);
            ensureState();
        } catch (Exception ex) {
            log.warn("Read data governance store failed, recreate empty store: {}", storeFile, ex);
            state = new StoreState();
            persist();
        }
    }

    public synchronized StoreState snapshot() {
        ensureState();
        return objectMapper.convertValue(state, StoreState.class);
    }

    public synchronized MetadataRequest getMetadata(String assetId) {
        ensureState();
        return state.metadata.get(assetId);
    }

    public synchronized MetadataRequest saveMetadata(String assetId, MetadataRequest metadata) {
        ensureState();
        state.metadata.put(assetId, metadata);
        persist();
        return metadata;
    }

    public synchronized List<QualityRule> getRules(String assetId) {
        ensureState();
        return new ArrayList<>(state.rules.getOrDefault(assetId, new ArrayList<>()));
    }

    public synchronized QualityRule saveRule(String assetId, QualityRule rule) {
        ensureState();
        List<QualityRule> rules = state.rules.computeIfAbsent(assetId, key -> new ArrayList<>());
        int existingIndex = -1;
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getId() != null && rules.get(i).getId().equals(rule.getId())) {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0) {
            rules.set(existingIndex, rule);
        } else {
            rules.add(0, rule);
        }
        persist();
        return rule;
    }

    public synchronized List<Issue> getIssues(String assetId) {
        ensureState();
        return new ArrayList<>(state.issues.getOrDefault(assetId, new ArrayList<>()));
    }

    public synchronized Issue saveIssue(String assetId, Issue issue) {
        ensureState();
        List<Issue> issues = state.issues.computeIfAbsent(assetId, key -> new ArrayList<>());
        int existingIndex = -1;
        for (int i = 0; i < issues.size(); i++) {
            if (issues.get(i).getId() != null && issues.get(i).getId().equals(issue.getId())) {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0) {
            issues.set(existingIndex, issue);
        } else {
            issues.add(0, issue);
        }
        persist();
        return issue;
    }

    public synchronized void replaceLineage(String targetAssetId, LineageNode upstreamNode, LineageNode downstreamNode) {
        ensureState();
        state.upstream.put(targetAssetId, new ArrayList<>());
        if (upstreamNode != null) {
            state.upstream.get(targetAssetId).add(upstreamNode);
        }
        if (downstreamNode != null && downstreamNode.getAssetId() != null) {
            List<LineageNode> sourceDownstream =
                    state.downstream.computeIfAbsent(downstreamNode.getAssetId(), key -> new ArrayList<>());
            sourceDownstream.removeIf(node -> targetAssetId.equals(node.getAssetId()));
            sourceDownstream.add(0, downstreamNode);
        }
        persist();
    }

    public synchronized List<LineageNode> getUpstream(String assetId) {
        ensureState();
        return new ArrayList<>(state.upstream.getOrDefault(assetId, new ArrayList<>()));
    }

    public synchronized List<LineageNode> getDownstream(String assetId) {
        ensureState();
        return new ArrayList<>(state.downstream.getOrDefault(assetId, new ArrayList<>()));
    }

    private void ensureState() {
        if (state == null) {
            state = new StoreState();
        }
        if (state.metadata == null) {
            state.metadata = new LinkedHashMap<>();
        }
        if (state.rules == null) {
            state.rules = new LinkedHashMap<>();
        }
        if (state.issues == null) {
            state.issues = new LinkedHashMap<>();
        }
        if (state.upstream == null) {
            state.upstream = new LinkedHashMap<>();
        }
        if (state.downstream == null) {
            state.downstream = new LinkedHashMap<>();
        }
    }

    private void persist() {
        ensureState();
        try {
            objectMapper.writeValue(storeFile, state);
        } catch (IOException ex) {
            log.error("Persist data governance store failed: {}", storeFile, ex);
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
