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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.jdbc.config.IDTokenValidatorConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.OpenIDProviderConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntitlementsConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RequestHeaderInterceptor implements HandlerInterceptor {

  public static final String USER_INFO_ISSUE_REASON = "Obtaining user info issue";
  public static final String NOT_VALID_TOKEN_PROVIDED_MESSAGE = "Not valid token provided";
  private static final String GCP_TRUST_EXTERNAL_AUTHENTICATION = "gcp-trust-external-authentication";

  @Autowired
  private final DpsHeaders dpsHeaders;

  private final JaxRsDpsLog log;

  private final OpenIDProviderConfig provider;
  private final IDTokenValidatorConfig validatorProvider;
  private final EntitlementsConfigurationProperties properties;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    if (isSwaggerRequest(request) || isVersionInfo(request)) {
      return true;
    }

    log.info("Intercepted the request. Now validating token..");

    if ((Objects.isNull(dpsHeaders.getAuthorization()) || dpsHeaders.getAuthorization().isEmpty())
        && (Objects.isNull(request.getHeader(properties.getGcpXUserIdentityHeaderName())))
        && (Objects.isNull(request.getHeader(properties.getGcpXApplicationIdentityHeaderName())))) {
      throw new AppException(HttpStatus.UNAUTHORIZED.value(),
          HttpStatus.UNAUTHORIZED.getReasonPhrase(),
          String.format("Token and headers %s and %s are required.",
              properties.getGcpXUserIdentityHeaderName(),
              properties.getGcpXApplicationIdentityHeaderName()));

    }

    if ((Objects.isNull(dpsHeaders.getAuthorization()) || dpsHeaders.getAuthorization().isEmpty())
        && Boolean.FALSE.equals(properties.getGcpTrustExternalAuthentication())) {
      throw new AppException(HttpStatus.UNAUTHORIZED.value(),
          HttpStatus.UNAUTHORIZED.getReasonPhrase(),
          "Token and a flag 'gcp-trust-external-authentication' are required.");
    }

    if (Objects.isNull(dpsHeaders.getAuthorization())
        && (Objects.nonNull(request.getHeader(properties.getGcpXApplicationIdentityHeaderName()))
        || Objects.nonNull(request.getHeader(properties.getGcpXUserIdentityHeaderName())))) {
      log.info("Trusted external authentication is used.");
      return true;
    }

    if (Objects.nonNull(request.getHeader(properties.getGcpXApplicationIdentityHeaderName()))
        && Objects.isNull(request.getHeader(properties.getGcpXUserIdentityHeaderName()))) {
      throw new AppException(HttpStatus.UNAUTHORIZED.value(),
          HttpStatus.UNAUTHORIZED.getReasonPhrase(),
          String.format("A header %s are required.",
              properties.getGcpXUserIdentityHeaderName()));
    }

    UserInfo userInfo = getUserInfoFromToken(dpsHeaders.getAuthorization());

    String memberEmail = userInfo.getEmailAddress();
    List<Audience> audience = userInfo.getAudience();

    String gcpUserIdentity = request.getHeader(properties.getGcpXUserIdentityHeaderName());
    String gcpApplicationIdentity = request.getHeader(
        properties.getGcpXApplicationIdentityHeaderName());

    if (Objects.nonNull(memberEmail) && (Objects.nonNull(audience) && !audience.isEmpty())) {
      if (Objects.nonNull(gcpUserIdentity) && Objects.nonNull(gcpApplicationIdentity)) {
        Audience gcpApplicationAudience = new Audience(gcpApplicationIdentity);
        if (!memberEmail.equals(gcpUserIdentity) && !audience.contains(gcpApplicationAudience)) {
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
          return true;
        }
      } else {
        dpsHeaders.put(properties.getGcpXUserIdentityHeaderName(), memberEmail);
        dpsHeaders.put(properties.getGcpXApplicationIdentityHeaderName(),
            Audience.toStringList(audience).toString());
        return true;
      }
    }

    return false;
  }

  private boolean isSwaggerRequest(HttpServletRequest request) {
    String endpoint = request.getRequestURI().replace(request.getContextPath(), "");
    return endpoint.startsWith("/swagger") || endpoint.startsWith("/webjars");
  }

  private boolean isVersionInfo(HttpServletRequest request) {
    String endpoint = request.getRequestURI().replace(request.getContextPath(), "");
    return endpoint.startsWith("/info");
  }

  private UserInfo getUserInfoFromToken(String token) {
    return getUserInfoFromIDToken(token);
  }

  private UserInfo getUserInfoFromAccessToken(String accessToken) {
    try {
      BearerAccessToken token = BearerAccessToken.parse(accessToken);
      HTTPResponse httpResponseUserInfo = new UserInfoRequest(
          provider.getProviderMetadata().getUserInfoEndpointURI(),
          token)
          .toHTTPRequest()
          .send();
      if (httpResponseUserInfo.indicatesSuccess()) {
        return new UserInfo(httpResponseUserInfo.getContentAsJSONObject());
      } else {
        throw new AppException(HttpStatus.UNAUTHORIZED.value(),
            USER_INFO_ISSUE_REASON,
            NOT_VALID_TOKEN_PROVIDED_MESSAGE);
      }
    } catch (ParseException | IOException e) {
      throw new AppException(HttpStatus.UNAUTHORIZED.value(),
          USER_INFO_ISSUE_REASON,
          NOT_VALID_TOKEN_PROVIDED_MESSAGE, e);
    }
  }

  private UserInfo getUserInfoFromIDToken(String token) {
    String aptToken = token.replace("Bearer ", "");
    IDTokenValidator validator = validatorProvider.getValidator();

    IDTokenClaimsSet claims = null;
    try {

      JWT jwt = JWTParser.parse(aptToken);
      claims = validator.validate(jwt, null);
      return UserInfo.parse(claims.toJSONString());

    } catch (BadJOSEException | JOSEException | java.text.ParseException | ParseException e) {

      log.info("id_token parsing failed. Trying access_token...");
      log.info("Original exception: " + e.getMessage());

      return getUserInfoFromAccessToken(token);
    }
  }
}
