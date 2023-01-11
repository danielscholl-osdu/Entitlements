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

package org.opengroup.osdu.entitlements.v2.util;

import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

public class JdbcTokenService implements TokenService {

    private static String token;

    @Override
    public Token getToken() {
        String authMode = System.getProperty("AUTH_MODE", System.getenv("AUTH_MODE"));
        if ("IAP".equals(authMode)) {
            return getIapToken();
        } else {
            return getServiceAccToken();
        }
    }

    @Nullable
    private Token getServiceAccToken() {
        Token testerToken = null;
        if (Strings.isNullOrEmpty(token)) {
            String serviceAccountFile = System
                .getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
            try {
                GoogleServiceAccount testerAccount = new GoogleServiceAccount(serviceAccountFile);

                testerToken = Token.builder()
                    .value(testerAccount.getAuthToken())
                    .userId(testerAccount.getEmail())
                    .build();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return testerToken;
    }

    private Token getIapToken() {
        String url = System.getProperty("IAP_URL", System.getenv("IAP_URL"));
        String iapClientId = getIapClientId(url);
        ServiceAccountCredentials serviceAccountCredentials = getIdTokenProvider();
        IdTokenCredentials idTokenCredentials = getIdTokenCredentials(serviceAccountCredentials, iapClientId);
        try {
            idTokenCredentials.refreshIfExpired();
        } catch (IOException e) {
            throw new RuntimeException("Unable to refresh IAP token", e);
        }
        return Token.builder()
            .value(idTokenCredentials.getIdToken().getTokenValue())
            .userId(serviceAccountCredentials.getClientEmail())
            .build();
    }

    public String getIapClientId(String url) {
        try {
            Document doc = Jsoup.connect(url).get();

            String redirectLocation = doc.location();
            List<NameValuePair> queryParameters = URLEncodedUtils.parse(new URI(redirectLocation), StandardCharsets.UTF_8.displayName());

            return queryParameters.stream()
                .filter(pair -> "client_id".equals(pair.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                    String.format("No client_id found in redirect response from IAP - %s", url))).getValue();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Exception during get Google IAP client id", e);
        }
    }

    private IdTokenCredentials getIdTokenCredentials(ServiceAccountCredentials serviceAccountCredentials,
        String iapClientId) {
        return IdTokenCredentials.newBuilder()
            .setIdTokenProvider(serviceAccountCredentials)
            .setTargetAudience(iapClientId)
            .build();
    }

    private ServiceAccountCredentials getIdTokenProvider() {
        String serviceAccountFile = System
            .getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
        Decoder decoder = Base64.getDecoder();
        byte[] decode = decoder.decode(serviceAccountFile);
        InputStream targetStream = new ByteArrayInputStream(decode);
        try {
            ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(targetStream);
            return credentials;
        } catch (IOException e) {
            throw new RuntimeException("Unable get credentials from INTEGRATION_TESTER variable", e);
        }
    }

}
