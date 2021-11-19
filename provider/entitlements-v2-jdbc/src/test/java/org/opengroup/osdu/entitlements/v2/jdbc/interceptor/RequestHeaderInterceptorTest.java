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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.jdbc.config.IDTokenValidatorConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.OpenIDProviderConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntitlementsConfigurationProperties;

@RunWith(MockitoJUnitRunner.class)
public class RequestHeaderInterceptorTest {

  @Mock
  private DpsHeaders dpsHeaders;

  @Mock
  private JaxRsDpsLog log;

  @Mock
  private OpenIDProviderConfig provider;

  @Mock
  private IDTokenValidatorConfig validatorProvider;

  @Mock
  private EntitlementsConfigurationProperties properties;

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

    requestHeaderInterceptor = new RequestHeaderInterceptor(dpsHeaders, log, provider,
        validatorProvider, properties);
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

  @Test(expected = AppException.class)
  public void should_throwAnException_when_AuthorizationIsNull() throws IOException {
    when(dpsHeaders.getAuthorization()).thenReturn(null);

    RequestHeaderInterceptor requestHeaderInterceptor = new RequestHeaderInterceptor(dpsHeaders,
        log, provider, validatorProvider, properties);

    requestHeaderInterceptor.preHandle(request, response, handler);
  }

  @Test(expected = AppException.class)
  public void should_throwAnException_when_flagExternalAuthenticationIsNull() throws IOException {
    when(dpsHeaders.getAuthorization()).thenReturn(null);

    RequestHeaderInterceptor requestHeaderInterceptor = new RequestHeaderInterceptor(dpsHeaders,
        log, provider, validatorProvider, properties);

    requestHeaderInterceptor.preHandle(request, response, handler);
  }

  @Test(expected = AppException.class)
  public void should_throwAnException_when_flagApplicationIdentityHeaderNameNonNull_and_flagUserIdentityHeaderNameIsNull()
      throws IOException {
    when(dpsHeaders.getAuthorization()).thenReturn("String");
    when(properties.getGcpXUserIdentityHeaderName()).thenReturn("x-user-id");
    when(properties.getGcpXApplicationIdentityHeaderName()).thenReturn("x-app-id");
    when(request.getHeader(properties.getGcpXApplicationIdentityHeaderName())).thenReturn(
        "testApp");

    RequestHeaderInterceptor requestHeaderInterceptor = new RequestHeaderInterceptor(dpsHeaders,
        log, provider, validatorProvider, properties);

    requestHeaderInterceptor.preHandle(request, response, handler);
  }

  @Test
  public void should_returnFalse_when_AuthorizationNonNull_and_UserIdentityHeaderNameAndApplicationIdentityHeaderNameAreOthers()
      throws IOException {
    String aptToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0ZXN0SXNzdWVySWQiLCJpYXQiOjE2MzY5NzI0NTAsImV4cCI6MTY2ODUwODQ1MCwiYXVkIjoidGVzdENsaWVudElkIiwic3ViIjoidGVzdFVzZXJOYW1lQGV4YW1wbGUuY29tIiwiRmlyc3ROYW1lIjoidGVzdFVzZXJGaXJzdE5hbWUiLCJTdXJuYW1lIjoidGVzdFVzZXJTZWNvbmROYW1lIiwiZW1haWwiOiJ0ZXN0VXNlck5hbWVAZXhhbXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.KM7ewU34QH55UMCoF-cZjmlEdr8MphTp2np8XKIKiGQ";
    IDTokenValidator idTokenValidator = new IDTokenValidator(new Issuer("testIssuerId"),
        new ClientID("testClientId"),
        JWSAlgorithm.HS256, new Secret("qwertyuiopasdfghjklzxcvbnm123456"));

    when(dpsHeaders.getAuthorization()).thenReturn(aptToken);
    when(validatorProvider.getValidator()).thenReturn(idTokenValidator);
    when(properties.getGcpXUserIdentityHeaderName()).thenReturn("x-user-id");
    when(properties.getGcpXApplicationIdentityHeaderName()).thenReturn("x-app-id");
    when(request.getHeader(properties.getGcpXUserIdentityHeaderName())).thenReturn(
        "testUserName2@example.com");
    when(request.getHeader(properties.getGcpXApplicationIdentityHeaderName())).thenReturn(
        "testClientId2");

    RequestHeaderInterceptor requestHeaderInterceptor = new RequestHeaderInterceptor(dpsHeaders,
        log, provider, validatorProvider, properties);
    boolean result = requestHeaderInterceptor.preHandle(request, response, handler);

    assertFalse(result);
  }

  @Test(expected = AppException.class)
  public void should_throwAnException_when_AuthorizationNonNull_and_NonCorrect()
      throws IOException {
    String aptToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0ZXN0SXNzdWVySWQiLCJpYXQiOjE2MzY5NzI0NTAsImV4cCI6MTY2ODUwODQ1MCwiYXVkIjoidGVzdENsaWVudElkIiwic3ViIjoidGVzdFVzZXJOYW1lQGV4YW1wbGUuY29tIiwiRmlyc3ROYW1lIjoidGVzdFVzZXJGaXJzdE5hbWUiLCJTdXJuYW1lIjoidGVzdFVzZXJTZWNvbmROYW1lIiwiZW1haWwiOiJ0ZXN0VXNlck5hbWVAZXhhbXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.KM7ewU34QH55UMCoF-cZjmlEdr8MphTp2np8XKIKiGQ";
    IDTokenValidator idTokenValidator = new IDTokenValidator(new Issuer("testIssuerId"),
        new ClientID("testClientId"),
        JWSAlgorithm.HS256, new Secret("qwertyuiopasdfghjklzxcvbnm1234567"));

    when(dpsHeaders.getAuthorization()).thenReturn(aptToken);
    when(validatorProvider.getValidator()).thenReturn(idTokenValidator);

    RequestHeaderInterceptor requestHeaderInterceptor = new RequestHeaderInterceptor(dpsHeaders,
        log, provider, validatorProvider, properties);
    requestHeaderInterceptor.preHandle(request, response, handler);
  }
  @Test
  public void should_returnTrue_when_AuthorizationNonNull_and_UserIdentityHeaderNameAndApplicationIdentityHeaderNameAreCorrect()
      throws IOException {
    String aptToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0ZXN0SXNzdWVySWQiLCJpYXQiOjE2MzY5NzI0NTAsImV4cCI6MTY2ODUwODQ1MCwiYXVkIjoidGVzdENsaWVudElkIiwic3ViIjoidGVzdFVzZXJOYW1lQGV4YW1wbGUuY29tIiwiRmlyc3ROYW1lIjoidGVzdFVzZXJGaXJzdE5hbWUiLCJTdXJuYW1lIjoidGVzdFVzZXJTZWNvbmROYW1lIiwiZW1haWwiOiJ0ZXN0VXNlck5hbWVAZXhhbXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.KM7ewU34QH55UMCoF-cZjmlEdr8MphTp2np8XKIKiGQ";
    IDTokenValidator idTokenValidator = new IDTokenValidator(new Issuer("testIssuerId"),
        new ClientID("testClientId"),
        JWSAlgorithm.HS256, new Secret("qwertyuiopasdfghjklzxcvbnm123456"));

    when(dpsHeaders.getAuthorization()).thenReturn(aptToken);
    when(properties.getGcpXUserIdentityHeaderName()).thenReturn("x-user-id");
    when(properties.getGcpXApplicationIdentityHeaderName()).thenReturn("x-app-id");
    when(request.getHeader(properties.getGcpXUserIdentityHeaderName())).thenReturn(
        "testUserName@example.com");
    when(request.getHeader(properties.getGcpXApplicationIdentityHeaderName())).thenReturn(
        "testClientId");
    when(validatorProvider.getValidator()).thenReturn(idTokenValidator);

    RequestHeaderInterceptor requestHeaderInterceptor = new RequestHeaderInterceptor(dpsHeaders,
        log, provider, validatorProvider, properties);
    boolean result = requestHeaderInterceptor.preHandle(request, response, handler);

    assertTrue(result);
  }

  @Test
  public void should_returnTrue_when_AuthorizationIsNullAndOneOfHeaderIsExist() throws IOException {
    when(dpsHeaders.getAuthorization()).thenReturn(null);
    when(properties.getGcpXUserIdentityHeaderName()).thenReturn("x-user-id");
    when(request.getHeader(properties.getGcpXUserIdentityHeaderName())).thenReturn(
        "testUserName@example.com");

    when(properties.getGcpTrustExternalAuthentication()).thenReturn(true);

    boolean result = requestHeaderInterceptor.preHandle(request, response, handler);

    assertTrue(result);
  }
}
