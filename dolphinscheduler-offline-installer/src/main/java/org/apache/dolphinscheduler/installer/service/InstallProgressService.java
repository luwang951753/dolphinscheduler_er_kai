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

package org.apache.dolphinscheduler.installer.service;

import org.apache.dolphinscheduler.installer.dto.InstallProgress;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class InstallProgressService {

    private final Map<String, InstallProgress> progresses = new ConcurrentHashMap<>();

    public void running(String installId, String step, String message) {
        InstallProgress progress = progresses.get(installId);
        if (progress == null) {
            progress = InstallProgress.running(step, message);
        } else {
            progress.setStatus(InstallProgress.STATUS_RUNNING);
            progress.setCurrentStep(step);
            progress.getItems()
                    .add(new InstallProgress.ProgressItem(step, step, InstallProgress.STATUS_RUNNING, message));
        }
        progresses.put(installId, progress);
    }

    public void success(String installId, String step, String message) {
        InstallProgress progress = progresses.get(installId);
        if (progress == null) {
            progress = InstallProgress.running(step, message);
        }
        progress.setStatus(InstallProgress.STATUS_SUCCESS);
        progress.setCurrentStep(step);
        progress.getItems().add(new InstallProgress.ProgressItem(step, step, InstallProgress.STATUS_SUCCESS, message));
        progresses.put(installId, progress);
    }

    public void failed(String installId, String step, String message) {
        InstallProgress progress = progresses.get(installId);
        if (progress == null) {
            progress = InstallProgress.running(step, message);
        }
        progress.setStatus(InstallProgress.STATUS_FAILED);
        progress.setCurrentStep(step);
        progress.getItems().add(new InstallProgress.ProgressItem(step, step, InstallProgress.STATUS_FAILED, message));
        progresses.put(installId, progress);
    }

    public InstallProgress get(String installId) {
        return progresses.get(installId);
    }
}
