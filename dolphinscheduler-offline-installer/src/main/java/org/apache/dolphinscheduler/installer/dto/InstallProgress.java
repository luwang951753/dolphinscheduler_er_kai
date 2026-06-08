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

import java.util.ArrayList;
import java.util.List;

public class InstallProgress {

    public static final String STATUS_RUNNING = "RUNNING";

    public static final String STATUS_SUCCESS = "SUCCESS";

    public static final String STATUS_FAILED = "FAILED";

    private String status;

    private String currentStep;

    private List<ProgressItem> items = new ArrayList<>();

    public static InstallProgress running(String step, String message) {
        InstallProgress progress = new InstallProgress();
        progress.setStatus(STATUS_RUNNING);
        progress.setCurrentStep(step);
        progress.getItems().add(new ProgressItem(step, step, STATUS_RUNNING, message));
        return progress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public List<ProgressItem> getItems() {
        return items;
    }

    public void setItems(List<ProgressItem> items) {
        this.items = items;
    }

    public static class ProgressItem {

        private String key;

        private String name;

        private String status;

        private String message;

        public ProgressItem() {
        }

        public ProgressItem(String key, String name, String status, String message) {
            this.key = key;
            this.name = name;
            this.status = status;
            this.message = message;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
