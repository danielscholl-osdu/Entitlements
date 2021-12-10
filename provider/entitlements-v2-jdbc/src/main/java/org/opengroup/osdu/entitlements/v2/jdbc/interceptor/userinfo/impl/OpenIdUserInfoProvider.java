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

import static com.nimbusds.openid.connect.sdk.claims.ClaimsSet.AUD_CLAIM_NAME;

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
import java.net.URI;
import java.net.URISyntaxException;
import net.minidev.json.JSONObject;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.config.OpenIDProviderConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.userinfo.IUserInfoProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class OpenIdUserInfoProvider implements IUserInfoProvider {

    public static final String USER_INFO_ISSUE_REASON = "Obtaining user info issue";
    public static final String NOT_VALID_TOKEN_PROVIDED_MESSAGE = "Not valid token provided";
    public static final String TOKEN_INFO_ENDPOINT = "https://www.googleapis.com/oauth2/v1/tokeninfo";

    private final JaxRsDpsLog log;
    private final OpenIDProviderConfig provider;
    private final IDTokenValidator openIdValidator;

    public OpenIdUserInfoProvider(JaxRsDpsLog log, OpenIDProviderConfig provider, @Qualifier("opendIdValidator") IDTokenValidator openIdValidator) {
        this.log = log;
        this.provider = provider;
        this.openIdValidator = openIdValidator;
    }

    @Override
    public UserInfo getUserInfoFromToken(String token) {
        try {
            return getUserInfoFromIDToken(token);
        } catch (BadJOSEException | JOSEException | java.text.ParseException | ParseException e) {

            log.info("id_token parsing failed. Trying access_token...");
            log.info("Original exception: " + e.getMessage());

            return getUserInfoFromAccessToken(token);
        }
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
                JSONObject userInfoResponse = httpResponseUserInfo.getContentAsJSONObject();
                String audience = getAudienceForAccessToken(token);

                userInfoResponse.put(AUD_CLAIM_NAME, audience);
                return new UserInfo(userInfoResponse);
            } else {
                throw unauthorizedException();
            }
        } catch (ParseException | URISyntaxException | IOException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(),
                USER_INFO_ISSUE_REASON,
                NOT_VALID_TOKEN_PROVIDED_MESSAGE, e);
        }
    }

    private String getAudienceForAccessToken(BearerAccessToken token) throws IOException, URISyntaxException, ParseException {
        HTTPResponse tokenInfoResponse = new UserInfoRequest(
            new URI(TOKEN_INFO_ENDPOINT),
            token)
            .toHTTPRequest()
            .send();
        if (tokenInfoResponse.indicatesSuccess()) {
            return tokenInfoResponse.getContentAsJSONObject().getAsString("audience");
        } else {
            throw unauthorizedException();
        }
    }

    private UserInfo getUserInfoFromIDToken(String token) throws java.text.ParseException, BadJOSEException, JOSEException, ParseException {
        String aptToken = token.replace("Bearer ", "");

        IDTokenClaimsSet claims = null;

        JWT jwt = JWTParser.parse(aptToken);
        claims = openIdValidator.validate(jwt, null);
        return UserInfo.parse(claims.toJSONString());
    }

    private AppException unauthorizedException() {
        return new AppException(HttpStatus.UNAUTHORIZED.value(),
            USER_INFO_ISSUE_REASON,
            NOT_VALID_TOKEN_PROVIDED_MESSAGE);
    }
}
