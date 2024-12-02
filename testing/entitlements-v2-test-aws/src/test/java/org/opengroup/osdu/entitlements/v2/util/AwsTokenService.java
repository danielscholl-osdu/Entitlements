/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.entitlements.v2.util;

import com.google.common.base.Strings;
import org.opengroup.osdu.core.aws.cognito.AWSCognitoClient;
import org.opengroup.osdu.core.aws.entitlements.ServicePrincipal;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;


public class AwsTokenService implements TokenService {
    private static volatile AwsTokenService instance;

    private final AWSCognitoClient cognitoClient;
    ServicePrincipal sp;

    private static String TOKEN;
    private static String NO_ACC_TOKEN;
    private static final String SERVICE_PRINCIPAL_EMAIL =  System.getProperty("SERVICE_PRINCIPAL_EMAIL", System.getenv("SERVICE_PRINCIPAL_EMAIL"));
    public static final String NO_ACCESS_USER = System.getProperty("AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS", System.getenv("AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS"));

    // Private constructor to prevent direct instantiation
    private AwsTokenService() {
        this.cognitoClient = new AWSCognitoClient();
        this.sp = new ServicePrincipal();
    }

    // Public method to get the singleton instance
    public static AwsTokenService getInstance() {
        if (instance == null) {
            synchronized (AwsTokenService.class) {
                if (instance == null) {
                    instance = new AwsTokenService();
                }
            }
        }
        return instance;
    }
    /**
     * Returns token of a service principal
     */
    @Override
    public synchronized Token getToken()  {
        if (Strings.isNullOrEmpty(TOKEN)) {
            String sptoken = sp.getServicePrincipalAccessToken();
            sptoken = sptoken.replace("Bearer ","");
            TOKEN = sptoken;
        }
        return Token.builder()
                .value(TOKEN)
                .userId(SERVICE_PRINCIPAL_EMAIL)
                .build();
    }

    @Override
    public synchronized Token getNoAccToken() {
        if (Strings.isNullOrEmpty(NO_ACC_TOKEN)) {
            String noAccessToken  = cognitoClient.getTokenForUserWithNoAccess();
            noAccessToken = noAccessToken.replace("Bearer ","");
            NO_ACC_TOKEN = noAccessToken;
        }
        return Token.builder()
                .value(NO_ACC_TOKEN)
                .userId(NO_ACCESS_USER)
                .build();
    }

}
