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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.InitiateAuthResult;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.opengroup.osdu.core.aws.entitlements.ServicePrincipal;
import org.opengroup.osdu.core.aws.iam.IAMConfig;
import org.opengroup.osdu.core.aws.secrets.SecretsManager;

import java.security.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AwsTestUtils  {

    private static final String COGNITO_NAME = System.getProperty("COGNITO_NAME", System.getenv("COGNITO_NAME"));
    private static final String REGION = System.getProperty("AWS_REGION", System.getenv("AWS_REGION"));
    private static final String AUTH_FLOW = System.getProperty("AWS_COGNITO_AUTH_FLOW", System.getenv("AWS_COGNITO_AUTH_FLOW"));
    public static final String NO_ACCESS_USER = System.getProperty("AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS", System.getenv("AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS"));
    private static final String NO_ACCESS_USER_PASSWORD = System.getProperty("AWS_COGNITO_AUTH_PARAMS_PASSWORD", System.getenv("AWS_COGNITO_AUTH_PARAMS_PASSWORD"));

    private final AWSSimpleSystemsManagement ssmManager;
    private final SecretsManager sm;
    String sptoken=null;

    public AwsTestUtils() {
        AWSCredentialsProvider amazonAWSCredentials = IAMConfig.amazonAWSCredentials();
        this.ssmManager = AWSSimpleSystemsManagementClientBuilder.standard()
                .withCredentials(amazonAWSCredentials)
                .withRegion(REGION)
                .build();
        this.sm = new SecretsManager();
    }

    public synchronized String getAccessToken() {
        if(sptoken==null) {
            String clientCredentialsClientId = getSsmParameter("/osdu/cognito/" + COGNITO_NAME + "/client/client-credentials/id");
            String clientCredentialsSecret = sm.getSecret("/osdu/cognito/" + COGNITO_NAME + "/client-credentials-secret", REGION, "client_credentials_client_secret");
            String tokenUrl = getSsmParameter("/osdu/cognito/" + COGNITO_NAME + "/oauth/token-uri");
            String awsOauthCustomScope = getSsmParameter("/osdu/cognito/" + COGNITO_NAME + "/oauth/custom-scope");

            ServicePrincipal sp = new ServicePrincipal(REGION, tokenUrl, awsOauthCustomScope);
            sptoken = sp.getServicePrincipalAccessToken(clientCredentialsClientId, clientCredentialsSecret);
        }
        return sptoken;
    }

    public synchronized String getNoAccessToken() {
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", NO_ACCESS_USER);
        authParameters.put("PASSWORD", NO_ACCESS_USER_PASSWORD);

        AWSCognitoIdentityProvider provider = AWSCognitoBuilder.generateCognitoClient();
        InitiateAuthRequest request = new InitiateAuthRequest();
        request.setClientId(getClientId());
        request.setAuthFlow(AUTH_FLOW);
        request.setAuthParameters(authParameters);

        InitiateAuthResult result = provider.initiateAuth(request);
        return result.getAuthenticationResult().getAccessToken();
    }

    private String getClientId() {
        String clientId = "/osdu/cognito/" + COGNITO_NAME + "/client/id";
        return getSsmParameter(clientId);
    }

    private static String createInvalidToken(String username) {

        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(2048);
                
            KeyPair kp = keyGenerator.genKeyPair();
            PublicKey publicKey = (PublicKey) kp.getPublic();
            PrivateKey privateKey = (PrivateKey) kp.getPrivate();
            
            
            String token = Jwts.builder()
                    .setSubject(username)
                    .setExpiration(new Date())                
                    .setIssuer("info@example.com")                    
                    // RS256 with privateKey
                    .signWith(SignatureAlgorithm.RS256, privateKey)
                    .compact();
                    
            return token;
        }
        catch (NoSuchAlgorithmException ex) {            
            return null;
        }
    }

    private String getSsmParameter(String parameterKey) {
        if (parameterKey == null || parameterKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter key cannot be null or empty");
        }

        try {
            GetParameterRequest paramRequest = new GetParameterRequest()
                    .withName(parameterKey)
                    .withWithDecryption(true);
            GetParameterResult paramResult = ssmManager.getParameter(paramRequest);
            String value = paramResult.getParameter().getValue();

            if (value == null || value.trim().isEmpty()) {
                throw new IllegalStateException("Retrieved parameter value is null or empty for key: " + parameterKey);
            }

            return value;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve SSM parameter: " + parameterKey, e);
        }
    }

}
