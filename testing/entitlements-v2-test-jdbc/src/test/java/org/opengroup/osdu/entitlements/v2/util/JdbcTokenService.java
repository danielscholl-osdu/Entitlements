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

import com.google.common.base.Strings;
import java.io.IOException;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

public class JdbcTokenService implements TokenService {

    private static String token;

    @Override
    public Token getToken() {
        Token testerToken = null;
        if (Strings.isNullOrEmpty(token)) {
            String serviceAccountFile = System
                    .getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
            String audience = System.getProperty("INTEGRATION_TEST_AUDIENCE",
                    System.getenv("INTEGRATION_TEST_AUDIENCE"));
            if (Strings.isNullOrEmpty(audience)) {
                audience = "245464679631-ktfdfpl147m1mjpbutl00b3cmffissgq.apps.googleusercontent.com";
            }
            try {
                GoogleServiceAccount testerAccount = new GoogleServiceAccount(serviceAccountFile);

                testerToken = Token.builder()
                        .value(testerAccount.getAuthToken(audience))
                        .userId(testerAccount.getEmail())
                        .build();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return testerToken;
    }

}
