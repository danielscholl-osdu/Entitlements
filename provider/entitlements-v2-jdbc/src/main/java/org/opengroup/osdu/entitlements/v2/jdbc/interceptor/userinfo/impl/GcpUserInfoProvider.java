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

import static com.nimbusds.openid.connect.sdk.claims.ClaimsSet.AUD_CLAIM_NAME;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.config.EntOpenIDProviderConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(value = "openid.provider.url", havingValue = "https://accounts.google.com")
public class GcpUserInfoProvider extends OpenIdUserInfoProvider {

    private static final String USER_INFO_ISSUE_REASON = "Obtaining user info issue";
    private static final String NOT_VALID_TOKEN_PROVIDED_MESSAGE = "Not valid token provided";
    private static final String TOKEN_INFO_ENDPOINT = "https://www.googleapis.com/oauth2/v1/tokeninfo";

    private final EntOpenIDProviderConfig provider;

    public GcpUserInfoProvider(
        JaxRsDpsLog log,
        Map<String, IDTokenValidator> openIdValidators,
        EntOpenIDProviderConfig provider
    ) {
        super(log, openIdValidators);
        this.provider = provider;
    }

    @Override
    public UserInfo getUserInfoFromToken(String token) {
        try {
            return super.getUserInfoFromToken(token);
        } catch (AppException e) {
            return getUserInfoFromAccessToken(token);
        }
    }

    private UserInfo getUserInfoFromAccessToken(String accessToken) {
        try {
            BearerAccessToken token = BearerAccessToken.parse(accessToken);
            return sendUserInfoRequest(token);
        } catch (ParseException | URISyntaxException | IOException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(),
                USER_INFO_ISSUE_REASON,
                NOT_VALID_TOKEN_PROVIDED_MESSAGE, e);
        }
    }

    private UserInfo sendUserInfoRequest(BearerAccessToken token)
        throws IOException, ParseException, URISyntaxException {
        HTTPResponse httpResponseUserInfo =
            new UserInfoRequest(provider.getProviderMetadata().getUserInfoEndpointURI(), token)
                .toHTTPRequest()
                .send();

        if (httpResponseUserInfo.indicatesSuccess()) {
            JSONObject userInfoResponse = httpResponseUserInfo.getContentAsJSONObject();
            String audience = getAudienceForAccessToken(token);

            userInfoResponse.put(AUD_CLAIM_NAME, audience);
            return new UserInfo(userInfoResponse);
        } else {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(),
                USER_INFO_ISSUE_REASON,
                NOT_VALID_TOKEN_PROVIDED_MESSAGE);
        }
    }

    private String getAudienceForAccessToken(BearerAccessToken token)
        throws IOException, URISyntaxException, ParseException {
        HTTPResponse tokenInfoResponse = new UserInfoRequest(new URI(TOKEN_INFO_ENDPOINT), token)
            .toHTTPRequest()
            .send();

        String audience = null;
        if (tokenInfoResponse.indicatesSuccess()) {
            audience = tokenInfoResponse.getContentAsJSONObject().getAsString("audience");
        }
        if (Objects.nonNull(audience) && this.getOpenIdValidators().containsKey(audience)) {
            return audience;
        } else {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(),
                USER_INFO_ISSUE_REASON,
                NOT_VALID_TOKEN_PROVIDED_MESSAGE);
        }
    }
}
