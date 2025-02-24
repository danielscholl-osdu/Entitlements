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

package org.opengroup.osdu.entitlements.v2.jdbc.interceptor.userinfo.impl;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.config.EntOpenIDProviderConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.IDTokenValidatorFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.nimbusds.openid.connect.sdk.claims.ClaimsSet.AUD_CLAIM_NAME;

@Slf4j
@Component
@ConditionalOnExpression(value = "'${openid.provider.url}' == 'https://accounts.google.com' && '${authentication-mode}' != 'ISTIO'")
public class GcpUserInfoProvider extends OpenIdUserInfoProvider {

    private static final String USER_INFO_ISSUE_REASON = "Obtaining user info issue";
    private static final String NOT_VALID_TOKEN_PROVIDED_MESSAGE = "Not valid token provided";
    private static final String TOKEN_INFO_ENDPOINT = "https://www.googleapis.com/oauth2/v1/tokeninfo";

    private final EntOpenIDProviderConfig provider;
    private final IRedisCache<String, UserInfo> cache;

    public GcpUserInfoProvider(
        JaxRsDpsLog log,
        IDTokenValidatorFactory idTokenValidatorFactory,
        EntOpenIDProviderConfig provider,
        IRedisCache<String, UserInfo> cache
    ) {
        super(log, idTokenValidatorFactory);
        this.provider = provider;
        this.cache = cache;
    }

    @Override
    public UserInfo getUserInfoFromToken(String token) {
        try {
            return super.getUserInfoFromToken(token);
        } catch (AppException e) {
            log.warn("id_token parsing failed. Original exception: {}.", e.getMessage(), e);
            return getUserInfoFromAccessToken(token);
        }
    }

    private UserInfo getUserInfoFromAccessToken(String accessToken) {
        try {
            BearerAccessToken token = BearerAccessToken.parse(accessToken);
            return sendUserInfoRequest(token);
        } catch (ParseException | URISyntaxException | IOException | AppException e) {
            log.warn("Could not authorize user. Original exception {}", e.getMessage(), e);
            throw new AppException(HttpStatus.UNAUTHORIZED.value(),
                USER_INFO_ISSUE_REASON,
                NOT_VALID_TOKEN_PROVIDED_MESSAGE, e);
        }
    }

    private UserInfo sendUserInfoRequest(BearerAccessToken token)
            throws IOException, ParseException, URISyntaxException {
        String cacheKey = getUserInfoCacheKey(token.getValue());
        UserInfo userInfo = cache.get(cacheKey);

        if (userInfo == null) {
            log.debug("Cached user info for access token not found. Getting new user info");
            HTTPRequest userInfoRequest = new UserInfoRequest(provider.getProviderMetadata().getUserInfoEndpointURI(), token)
                    .toHTTPRequest();
            log.debug("User info request: {}", userInfoRequest);
            HTTPResponse httpResponseUserInfo = userInfoRequest.send();
            log.debug("User info response: {}", httpResponseUserInfo);

            if (httpResponseUserInfo.indicatesSuccess()) {
                JSONObject userInfoJson = httpResponseUserInfo.getContentAsJSONObject();
                JSONObject tokenInfoJson = getTokenInfoForAccessToken(token);
                String audience = tokenInfoJson.getAsString("audience");
                long ttl = tokenInfoJson.getAsNumber("expires_in").longValue() * 1000;

                userInfoJson.put(AUD_CLAIM_NAME, audience);
                userInfo = new UserInfo(userInfoJson);
                cache.put(cacheKey, ttl, userInfo);
                return userInfo;
            } else {
                throw new AppException(HttpStatus.UNAUTHORIZED.value(),
                        USER_INFO_ISSUE_REASON,
                        NOT_VALID_TOKEN_PROVIDED_MESSAGE);
            }
        }
        log.debug("Cached user info value used");
        return userInfo;
    }

    private static String getUserInfoCacheKey(String token) {
        return String.format("entitlements-user-info:%s", token);
    }

    private JSONObject getTokenInfoForAccessToken(BearerAccessToken token)
        throws IOException, URISyntaxException, ParseException {
        HTTPResponse tokenInfoResponse = new UserInfoRequest(new URI(TOKEN_INFO_ENDPOINT), token)
            .toHTTPRequest()
            .send();
        log.debug("Token info response: {}", tokenInfoResponse);

        if (tokenInfoResponse.indicatesSuccess()) {
            return tokenInfoResponse.getContentAsJSONObject();
        } else {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(),
                USER_INFO_ISSUE_REASON,
                NOT_VALID_TOKEN_PROVIDED_MESSAGE);
        }
    }
}
