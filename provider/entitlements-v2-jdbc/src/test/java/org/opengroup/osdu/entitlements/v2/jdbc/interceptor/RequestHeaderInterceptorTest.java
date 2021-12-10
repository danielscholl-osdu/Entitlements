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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.authenticator.IAuthenticator;

@RunWith(MockitoJUnitRunner.class)
public class RequestHeaderInterceptorTest {

    @Mock
    private JaxRsDpsLog log;

    @Mock
    private IAuthenticator authenticator;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    private RequestHeaderInterceptor requestHeaderInterceptor;

    @Before
    public void init() {
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("");

        requestHeaderInterceptor = new RequestHeaderInterceptor(log, authenticator);
    }

    @Test
    public void should_returnTrue_when_requestIsSwagger() throws IOException {
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/swagger");

        boolean result = requestHeaderInterceptor.preHandle(request, response, handler);

        assertTrue(result);
    }

    @Test
    public void should_returnTrue_when_requestIsVersionInfo() throws IOException {
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/info");

        boolean result = requestHeaderInterceptor.preHandle(request, response, handler);

        assertTrue(result);
    }
}
