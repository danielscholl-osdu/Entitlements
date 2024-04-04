/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.jdbc.Utils;
import org.opengroup.osdu.entitlements.v2.jdbc.config.DatabaseCheck;
import org.opengroup.osdu.entitlements.v2.jdbc.config.IDTokenValidatorFactory;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.jdbc.interceptor.AuthTestConfig.MATCHING_USER_EMAIL;
import static org.opengroup.osdu.entitlements.v2.jdbc.interceptor.AuthTestConfig.NOT_MATCHING_USER_EMAIL;

@SpringBootTest(
    properties = {
      "gcp-authentication-mode=INTERNAL",
      "openid.provider.url=https://accounts.google.com",
      "openid.provider.user-id-claim-name=email"
    },
    classes = AuthTestConfig.class)
@RunWith(SpringRunner.class)
public class InternalAuthTest {

  @Mock private Object handler;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @MockBean private JaxRsDpsLog jaxRsDpsLog;
  @MockBean private IRedisCache<String, UserInfo> cache;
  @MockBean private IDTokenValidatorFactory tokenValidatorFactory;
  @Autowired private EntConfigProperties entConfigProperties;
  @Autowired public RequestHeaderInterceptor interceptor;
  @Autowired private ApplicationContext applicationContext;

  @MockBean private DatabaseCheck databaseCheck;

  private String correctToken;

  @Before
  public void setUp() {
    when(request.getContextPath()).thenReturn("");
    when(request.getRequestURI()).thenReturn("");
    when(request.getMethod()).thenReturn("GET");
    when(request.getHeader(DpsHeaders.ON_BEHALF_OF)).thenReturn(null);
    when(tokenValidatorFactory.createTokenValidator(anyString())).thenReturn(new IDTokenValidator(
            new Issuer("testIssuerId"),
            new ClientID("testClientId"),
            JWSAlgorithm.HS256,
            new Secret("qwertyuiopasdfghjklzxcvbnm123456")));
    try {
      correctToken = Utils.generateJWT();
    } catch (JOSEException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testShouldAuthenticateWhenValidAuthorizationHeaderPresent() {
    when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn("Bearer " + correctToken);
    assertTrue(interceptor.preHandle(request, response, handler));
    DpsHeaders headers = applicationContext.getBean(DpsHeaders.class);
    assertEquals(MATCHING_USER_EMAIL, headers.getUserId());
  }

  @Test
  public void testShouldAuthenticateWhenValidAuthorizationAndUserIdHeadersPresent() {
    when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn("Bearer " + correctToken);
    when(request.getHeader(entConfigProperties.getGcpXUserIdentityHeaderName()))
        .thenReturn(MATCHING_USER_EMAIL);
    assertTrue(interceptor.preHandle(request, response, handler));
    DpsHeaders headers = applicationContext.getBean(DpsHeaders.class);
    assertEquals(MATCHING_USER_EMAIL, headers.getUserId());
  }

  @Test(expected = AppException.class)
  public void testShouldNotAuthenticateWhenNoHeaderPresent() {
    interceptor.preHandle(request, response, handler);
  }

  @Test(expected = AppException.class)
  public void testShouldNotAuthenticateWhenNotMatchingUserIdHeaderPresent() {
    when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(correctToken);
    when(request.getHeader(entConfigProperties.getGcpXUserIdentityHeaderName()))
        .thenReturn(NOT_MATCHING_USER_EMAIL);
    interceptor.preHandle(request, response, handler);
  }

  @Test(expected = AppException.class)
  public void testShouldNotAuthenticateWhenOnlyUserIdHeaderPresent() {
    when(request.getHeader(entConfigProperties.getGcpXUserIdentityHeaderName()))
        .thenReturn(NOT_MATCHING_USER_EMAIL);
    interceptor.preHandle(request, response, handler);
  }
}
