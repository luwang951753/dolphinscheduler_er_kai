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

import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.api.service.UserModulePermissionService;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.entity.UserModulePermission;
import org.apache.dolphinscheduler.dao.mapper.UserMapper;
import org.apache.dolphinscheduler.dao.mapper.UserModulePermissionMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

@Service
public class UserModulePermissionServiceImpl extends BaseServiceImpl implements UserModulePermissionService {

    private static final Set<String> SUPPORTED_MODULE_KEYS = new LinkedHashSet<>(Arrays.asList(
            "sync-task:view",
            "data-preview:view",
            "theme-library:view",
            "whitepaper:view",
            "data-governance:view",
            "monitor:view",
            "resources:view"));

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserModulePermissionMapper userModulePermissionMapper;

    @Override
    public List<String> queryModulePermissions(User loginUser, Integer userId) {
        Integer resolvedUserId = resolveUserId(loginUser, userId);
        ensureReadable(loginUser, resolvedUserId);
        ensureUserExists(resolvedUserId);
        return userModulePermissionMapper.selectList(new QueryWrapper<UserModulePermission>().lambda()
                .eq(UserModulePermission::getUserId, resolvedUserId)
                .orderByAsc(UserModulePermission::getId))
                .stream()
                .map(UserModulePermission::getModuleKey)
                .filter(SUPPORTED_MODULE_KEYS::contains)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> saveModulePermissions(User loginUser, Integer userId, List<String> moduleKeys) {
        if (!isAdmin(loginUser)) {
            throw new ServiceException(Status.USER_NO_OPERATION_PERM);
        }
        Integer resolvedUserId = resolveUserId(loginUser, userId);
        ensureUserExists(resolvedUserId);
        List<String> normalized = normalizeModuleKeys(moduleKeys);
        userModulePermissionMapper.delete(new QueryWrapper<UserModulePermission>().lambda()
                .eq(UserModulePermission::getUserId, resolvedUserId));
        Date now = new Date();
        for (String moduleKey : normalized) {
            UserModulePermission permission = new UserModulePermission();
            permission.setUserId(resolvedUserId);
            permission.setModuleKey(moduleKey);
            permission.setCreateTime(now);
            permission.setUpdateTime(now);
            userModulePermissionMapper.insert(permission);
        }
        return normalized;
    }

    private Integer resolveUserId(User loginUser, Integer userId) {
        if (userId != null) {
            return userId;
        }
        if (loginUser == null || loginUser.getId() == null) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
        return loginUser.getId();
    }

    private void ensureReadable(User loginUser, Integer userId) {
        if (!isAdmin(loginUser) && !loginUser.getId().equals(userId)) {
            throw new ServiceException(Status.USER_NO_OPERATION_PERM);
        }
    }

    private void ensureUserExists(Integer userId) {
        if (userId == null || userMapper.selectById(userId) == null) {
            throw new ServiceException(Status.USER_NOT_EXIST, userId);
        }
    }

    private List<String> normalizeModuleKeys(List<String> moduleKeys) {
        if (moduleKeys == null) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String moduleKey : moduleKeys) {
            if (!SUPPORTED_MODULE_KEYS.contains(moduleKey)) {
                throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
            }
            normalized.add(moduleKey);
        }
        return new ArrayList<>(normalized);
    }
}
