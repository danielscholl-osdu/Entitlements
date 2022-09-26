/*
 *  Copyright 2020-2021 Google LLC
 *  Copyright 2020-2021 EPAM Systems, Inc
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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.userinfo.IUserInfoProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConditionalOnExpression(value = "'${openid.provider.url}' != 'https://accounts.google.com' && '${gcp-authentication-mode}' != 'ISTIO'")
public class OpenIdUserInfoProvider implements IUserInfoProvider {

    public static final String USER_INFO_ISSUE_REASON = "Obtaining user info issue";
    public static final String NOT_VALID_TOKEN_PROVIDED_MESSAGE = "Not valid token provided";

    private final JaxRsDpsLog log;
    private final Map<String, IDTokenValidator> openIdValidators;

  public OpenIdUserInfoProvider(JaxRsDpsLog log, Map<String, IDTokenValidator> openIdValidators) {
        this.log = log;
        this.openIdValidators = openIdValidators;
    }

    @Override
    public UserInfo getUserInfoFromToken(String token) {
        try {
            return getUserInfoFromIDToken(token);
        } catch (BadJOSEException | JOSEException | java.text.ParseException | ParseException e) {
            log.error("id_token parsing failed. Original exception: {}.", e.getMessage(), e);
            throw new AppException(HttpStatus.UNAUTHORIZED.value(),
                USER_INFO_ISSUE_REASON,
                NOT_VALID_TOKEN_PROVIDED_MESSAGE, e);
        }
    }

  private UserInfo getUserInfoFromIDToken(String token)
      throws java.text.ParseException, BadJOSEException, JOSEException, ParseException {
    String aptToken = token.replace("Bearer ", "");
    JWT jwt = JWTParser.parse(aptToken);
    List<String> audiences = jwt.getJWTClaimsSet().getAudience();
    String validatorName =
        audiences.stream()
            .filter(openIdValidators::containsKey)
            .findFirst()
            .orElseThrow(
                () ->
                    new AppException(
                        HttpStatus.UNAUTHORIZED.value(),
                        USER_INFO_ISSUE_REASON,
                        NOT_VALID_TOKEN_PROVIDED_MESSAGE));
    IDTokenValidator openIdValidator = openIdValidators.get(validatorName);
    IDTokenClaimsSet claims = openIdValidator.validate(jwt, null);
    return UserInfo.parse(claims.toJSONString());
  }
}
