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
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.userinfo.IUserInfoProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(value = "gcp-authentication-mode", havingValue = "IAP")
public class IapUserInfoProvider implements IUserInfoProvider {

    public static final String USER_INFO_ISSUE_REASON = "Obtaining user info issue";
    public static final String NOT_VALID_TOKEN_PROVIDED_MESSAGE = "Not valid token provided";

    private final IDTokenValidator tokenValidator;

    public IapUserInfoProvider(@Qualifier("iapValidator") IDTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public UserInfo getUserInfoFromToken(String token) {
        try {
            return getUserInfoFromIDToken(token);
        } catch (BadJOSEException | JOSEException | java.text.ParseException | ParseException e) {

            log.warn("iap token parsing failed.");
            log.warn("Original exception: " + e.getMessage());

            throw unauthorizedException();
        }
    }

    private UserInfo getUserInfoFromIDToken(String token) throws java.text.ParseException, BadJOSEException, JOSEException,
        ParseException {
        String aptToken = token.replace("Bearer ", "");

        IDTokenClaimsSet claims = null;

        JWT jwt = JWTParser.parse(aptToken);
        claims = tokenValidator.validate(jwt, null);
        return UserInfo.parse(claims.toJSONString());
    }

    private AppException unauthorizedException() {
        return new AppException(HttpStatus.UNAUTHORIZED.value(),
            USER_INFO_ISSUE_REASON,
            NOT_VALID_TOKEN_PROVIDED_MESSAGE);
    }
}
