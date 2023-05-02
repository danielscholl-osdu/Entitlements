/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.opengroup.osdu.entitlements.v2.jdbc.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.authenticator.IAuthenticator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RequestHeaderInterceptor implements HandlerInterceptor {

    private final JaxRsDpsLog log;
    private final IAuthenticator iAuthenticator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!request.getMethod().equalsIgnoreCase("GET") && request.getHeader(DpsHeaders.ON_BEHALF_OF) != null) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Impersonation not allowed for all methods except GET");
        }
        if (isSwaggerRequest(request) || isVersionInfo(request)) {
            return true;
        }
        log.debug("Intercepted the request. Now validating headers..");
        if (iAuthenticator.requestIsAuthenticated(request)) {
            return true;
        } else {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Not valid authorization headers provided");
        }
    }

    private boolean isSwaggerRequest(HttpServletRequest request) {
        String endpoint = request.getRequestURI().replace(request.getContextPath(), "");
        return endpoint.startsWith("/swagger")
            || endpoint.startsWith("/webjars")
            || endpoint.startsWith("/v3/api-docs")
            || endpoint.startsWith("/api-docs");
    }

    private boolean isVersionInfo(HttpServletRequest request) {
        String endpoint = request.getRequestURI().replace(request.getContextPath(), "");
        return endpoint.startsWith("/info");
    }
}
