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

package org.opengroup.osdu.entitlements.v2.jdbc.interceptor.userinfo.impl;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import java.text.ParseException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.userinfo.IUserInfoProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "gcp-authentication-mode", havingValue = "ISTIO")
@RequiredArgsConstructor
public class IstioUserInfoProvider implements IUserInfoProvider {

  private static final String USER_INFO_ISSUE_REASON = "Obtaining user info issue";
  private static final String NOT_VALID_TOKEN_PROVIDED_MESSAGE = "Not valid token provided";

  private final JaxRsDpsLog log;

  @Override
  public UserInfo getUserInfoFromToken(String token) {
    try {
      String aptToken = token.replace("Bearer ", "");
      JWT jwt = JWTParser.parse(aptToken);
      Map<String, Object> jwtClaimsMap = jwt.getJWTClaimsSet().toJSONObject();
      return UserInfo.parse(JSONObject.toJSONString(jwtClaimsMap));
    } catch (ParseException | com.nimbusds.oauth2.sdk.ParseException e) {
      log.error("id_token parsing failed. Original exception: {}.", e.getMessage());
      throw new AppException(HttpStatus.UNAUTHORIZED.value(),
          USER_INFO_ISSUE_REASON,
          NOT_VALID_TOKEN_PROVIDED_MESSAGE, e);
    }
  }
}
