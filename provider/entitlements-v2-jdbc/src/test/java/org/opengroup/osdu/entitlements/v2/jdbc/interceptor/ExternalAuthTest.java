/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.jdbc.interceptor;

import com.nimbusds.jose.JOSEException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.jdbc.Utils;
import org.opengroup.osdu.entitlements.v2.jdbc.config.EntOpenIDProviderConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntitlementsConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.jdbc.interceptor.AuthTestConfig.*;

@SpringBootTest(
    properties = {
      "gcp-authentication-mode=EXTERNAL",
      "openid.provider.url=http://test",
      "openid.provider.user-id-claim-name=email",
      "service.token.provider=OPENID"
    },
    classes = AuthTestConfig.class)
@RunWith(SpringRunner.class)
public class ExternalAuthTest {

  @Mock private Object handler;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @MockBean private JaxRsDpsLog jaxRsDpsLog;
  @MockBean private EntOpenIDProviderConfig entOpenIDProviderConfig;
  @Autowired private EntitlementsConfigurationProperties entitlementsConfigurationProperties;
  @Autowired public RequestHeaderInterceptor interceptor;
  @Autowired private ApplicationContext applicationContext;
  private String correctToken;

  @Before
  public void setUp() {
    when(request.getContextPath()).thenReturn("");
    when(request.getRequestURI()).thenReturn("");
    when(request.getMethod()).thenReturn("GET");
    when(request.getHeader(DpsHeaders.ON_BEHALF_OF)).thenReturn(null);
    try {
      correctToken = Utils.generateJWT();
    } catch (JOSEException e) {
      throw new RuntimeException(e);
    }
  }

  @After
  public void tearDown() {}

  @Test
  public void shouldAuthenticateWhenOnlyUserIdHeaderPresent() {
    when(request.getHeader(entitlementsConfigurationProperties.getGcpXUserIdentityHeaderName()))
        .thenReturn(MATCHING_USER_EMAIL);
    assertTrue(interceptor.preHandle(request, response, handler));
    DpsHeaders headers = applicationContext.getBean(DpsHeaders.class);
    assertEquals(MATCHING_USER_EMAIL, headers.getUserId());
  }

  @Test
  public void shouldAuthenticateWhenOnlyOpenIDTokenPresent() {
    when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(correctToken);
    assertTrue(interceptor.preHandle(request, response, handler));
    DpsHeaders headers = applicationContext.getBean(DpsHeaders.class);
    assertEquals(MATCHING_USER_EMAIL, headers.getUserId());
  }

  @Test
  public void shouldAuthenticateWhenOpenIDTokenAndUserIdHeadersPresentAndMatching() {
    when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(correctToken);
    when(request.getHeader(entitlementsConfigurationProperties.getGcpXUserIdentityHeaderName()))
        .thenReturn(MATCHING_USER_EMAIL);
    assertTrue(interceptor.preHandle(request, response, handler));
    DpsHeaders headers = applicationContext.getBean(DpsHeaders.class);
    assertEquals(MATCHING_USER_EMAIL, headers.getUserId());
  }

  @Test(expected = AppException.class)
  public void shouldNotAuthenticateWhenNoHeadersPresent() {
    interceptor.preHandle(request, response, handler);
  }

  @Test(expected = AppException.class)
  public void shouldNotAuthenticateWhenOpenIDTokenAndUserIdHeadersPresentAndNotMatching() {
    when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(correctToken);
    when(request.getHeader(entitlementsConfigurationProperties.getGcpXUserIdentityHeaderName()))
        .thenReturn(NOT_MATCHING_USER_EMAIL);
    interceptor.preHandle(request, response, handler);
  }

  @Test(expected = AppException.class)
  public void shouldNotAuthenticateWhenOpenIDTokenAndUserIdHeadersPresentAndTokenNotCorrect() {
    when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(TOKEN_WITH_NOT_CORRECT_SECRET);
    when(request.getHeader(entitlementsConfigurationProperties.getGcpXUserIdentityHeaderName()))
        .thenReturn(MATCHING_USER_EMAIL);
    interceptor.preHandle(request, response, handler);
  }
}
