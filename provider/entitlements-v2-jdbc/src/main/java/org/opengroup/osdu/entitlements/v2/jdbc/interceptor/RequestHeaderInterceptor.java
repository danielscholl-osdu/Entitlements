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
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.jdbc.config.IDTokenValidatorConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.OpenIDProviderConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static java.lang.Boolean.TRUE;

@Component
@RequiredArgsConstructor
public class RequestHeaderInterceptor implements HandlerInterceptor {

    public static final String USER_INFO_ISSUE_REASON = "Obtaining user info issue";
    public static final String NOT_VALID_TOKEN_PROVIDED_MESSAGE = "Not valid token provided";

    @Autowired
    private final DpsHeaders dpsHeaders;

    private final JaxRsDpsLog log;

    private final OpenIDProviderConfig provider;
    private final IDTokenValidatorConfig validatorProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (TRUE.equals(validateSwaggerRequest(request))) {
            return true;
        }

        log.info("Intercepted the request. Now validating token..");

        if (dpsHeaders.getAuthorization() == null || dpsHeaders.getAuthorization().isEmpty()) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    "Token is required");
        }

        String memberEmail = getUserInfoFromToken(dpsHeaders.getAuthorization()).getEmailAddress();

        if (memberEmail != null) {
            // If JWT validation succeeds/ can extract user identity
            //       return true = requests moves forward to the core API code
            // Update the request Header to include user identity
            dpsHeaders.put("x-user-id", memberEmail);

            return true;
        } else {
            //If JWT validation fails/ cannot extract user identity
            //return false = response is handled here
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }

        return false;
    }

    private Boolean validateSwaggerRequest(HttpServletRequest request) {
        String endpoint = request.getRequestURI().replace(request.getContextPath(), "");
        return endpoint.startsWith("/swagger") || endpoint.startsWith("/webjars");
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
