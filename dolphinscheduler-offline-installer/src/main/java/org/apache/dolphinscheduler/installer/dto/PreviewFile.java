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

package org.apache.dolphinscheduler.installer.dto;

public class PreviewFile {

    private String name;

    private String path;

    private String risk;

    private int changes;

    private String content;

    public PreviewFile() {
    }

    public PreviewFile(String name, String path, String risk, int changes, String content) {
        this.name = name;
        this.path = path;
        this.risk = risk;
        this.changes = changes;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public int getChanges() {
        return changes;
    }

    public void setChanges(int changes) {
        this.changes = changes;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
