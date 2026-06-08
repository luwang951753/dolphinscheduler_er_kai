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

package org.apache.dolphinscheduler.api.configuration;

import org.apache.dolphinscheduler.api.security.Authenticator;
import org.apache.dolphinscheduler.common.enums.UserType;
import org.apache.dolphinscheduler.dao.entity.User;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.interceptor.AuthorizationInterceptor;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;

/**
 * Integrates magic-api editor permissions with DolphinScheduler login sessions.
 */
@Configuration
public class MagicApiConfiguration {

    @Bean
    public AuthorizationInterceptor magicApiAuthorizationInterceptor(Authenticator authenticator) {
        return new DolphinSchedulerMagicApiAuthorizationInterceptor(authenticator);
    }

    private static final class DolphinSchedulerMagicApiAuthorizationInterceptor implements AuthorizationInterceptor {

        private final Authenticator authenticator;

        private DolphinSchedulerMagicApiAuthorizationInterceptor(Authenticator authenticator) {
            this.authenticator = authenticator;
        }

        @Override
        public boolean requireLogin() {
            return false;
        }

        @Override
        public boolean allowVisit(MagicUser magicUser,
                                  MagicHttpServletRequest request,
                                  Authorization authorization) {
            User user = getDolphinSchedulerUser(request);
            return user != null && user.getUserType() == UserType.ADMIN_USER;
        }

        private User getDolphinSchedulerUser(MagicHttpServletRequest request) {
            if (request == null) {
                return null;
            }
            HttpServletRequest servletRequest = request.getRequest();
            if (servletRequest == null) {
                return null;
            }
            return authenticator.getAuthUser(servletRequest);
        }
    }
}
