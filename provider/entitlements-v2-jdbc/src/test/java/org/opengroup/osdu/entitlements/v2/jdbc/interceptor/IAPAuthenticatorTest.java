/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.IapConfigurationProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.OpenIdProviderProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.authenticator.impl.IAPAuthenticator;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.userinfo.IUserInfoProvider;

@ExtendWith(MockitoExtension.class)
class IAPAuthenticatorTest {
  private static final String TEST_IAP_TOKEN = "testIapToken";
  private static final String TEST_AUTH_TOKEN = "testAuthToken";
  private static final String CORRECT_USER_ID = "account:test1@example.com";
  private static final String EMAIL = "email";
  private static final String USER_ID = "test2@example.com";

  @Mock
  private DpsHeaders dpsHeaders;

  @Mock
  private IapConfigurationProperties iapConfigurationProperties;

  @Mock
  private IUserInfoProvider userInfoProvider;

  @Mock
  private OpenIdProviderProperties openIdProviderProperties;

  @InjectMocks
  private IAPAuthenticator iapAuthenticator;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Test
  void requestIsAuthenticated_noIapTokenNoAuthorization_returnsFalse() {
    boolean result = iapAuthenticator.requestIsAuthenticated(request);

    assertFalse(result);
  }

  @Test
  void requestIsAuthenticated_iapTokenAndUserIdPresent_malformedUserIdHeader_returnsFalse() {
    UserInfo userInfoMock = Mockito.mock(UserInfo.class);
    when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(TEST_AUTH_TOKEN);
    when(request.getHeader(iapConfigurationProperties.getJwtHeader())).thenReturn(TEST_IAP_TOKEN);
    when(request.getHeader(iapConfigurationProperties.getUserIdHeader())).thenReturn(CORRECT_USER_ID);
    when(openIdProviderProperties.getUserIdClaimName()).thenReturn(EMAIL);
    when(userInfoProvider.getUserInfoFromToken(any())).thenReturn(userInfoMock);
    when(userInfoProvider.getUserInfoFromToken(any()).getStringClaim(any())).thenReturn(USER_ID);

    boolean result = iapAuthenticator.requestIsAuthenticated(request);

    assertFalse(result);
  }

  @Test
  void requestIsAuthenticated_iapTokenAndUserIdPresent_matchingEmails_returnsTrue() {
    UserInfo userInfoMock = Mockito.mock(UserInfo.class);

    when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(TEST_AUTH_TOKEN);
    when(request.getHeader(iapConfigurationProperties.getJwtHeader())).thenReturn(TEST_IAP_TOKEN);
    when(request.getHeader(iapConfigurationProperties.getUserIdHeader()))
        .thenReturn(CORRECT_USER_ID);
    when(openIdProviderProperties.getUserIdClaimName()).thenReturn(EMAIL);
    when(userInfoProvider.getUserInfoFromToken(any())).thenReturn(userInfoMock);
    when(userInfoProvider.getUserInfoFromToken(any()).getStringClaim(any()))
        .thenReturn(CORRECT_USER_ID.split(":")[1]);

    boolean result = iapAuthenticator.requestIsAuthenticated(request);

    assertTrue(result);
  }

  @Test
  void requestIsAuthenticated_iapTokenAndUserIdPresent_nonMatchingEmails_returnsFalse() {
    UserInfo userInfoMock = Mockito.mock(UserInfo.class);
    when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(TEST_AUTH_TOKEN);
    when(request.getHeader(iapConfigurationProperties.getJwtHeader())).thenReturn(TEST_IAP_TOKEN);
    when(request.getHeader(iapConfigurationProperties.getUserIdHeader())).thenReturn(CORRECT_USER_ID);
    when(openIdProviderProperties.getUserIdClaimName()).thenReturn(EMAIL);
    when(userInfoProvider.getUserInfoFromToken(any())).thenReturn(userInfoMock);
    when(userInfoProvider.getUserInfoFromToken(any()).getStringClaim(any()))
        .thenReturn(USER_ID);

    boolean result = iapAuthenticator.requestIsAuthenticated(request);

    assertFalse(result);
  }

  @Test
  void requestIsAuthenticated_iapTokenAndUserIdNonPresent_AuthorizationPresent_returnsTrue() {
    UserInfo userInfoMock = Mockito.mock(UserInfo.class);
    when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(TEST_AUTH_TOKEN);
    when(openIdProviderProperties.getUserIdClaimName()).thenReturn(EMAIL);
    when(userInfoProvider.getUserInfoFromToken(any())).thenReturn(userInfoMock);
    when(userInfoProvider.getUserInfoFromToken(any()).getStringClaim(any()))
        .thenReturn(USER_ID);

    boolean result = iapAuthenticator.requestIsAuthenticated(request);
    assertTrue(result);
  }
}
