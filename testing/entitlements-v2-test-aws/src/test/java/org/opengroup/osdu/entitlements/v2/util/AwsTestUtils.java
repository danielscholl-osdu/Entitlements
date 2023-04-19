// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.entitlements.v2.util;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.google.common.base.Strings;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.opengroup.osdu.core.aws.entitlements.ServicePrincipal;
import org.opengroup.osdu.core.aws.iam.IAMConfig;
import org.opengroup.osdu.core.aws.secrets.SecretsManager;

import java.security.*;
import java.util.Date;

public class AwsTestUtils  {


    String client_credentials_secret;
    String client_credentials_clientid;
    ServicePrincipal sp;
    private String awsOauthCustomScope;

    private final static String COGNITO_NAME = "COGNITO_NAME";
    private final static String REGION = "AWS_REGION";




    private AWSCredentialsProvider amazonAWSCredentials;
    private AWSSimpleSystemsManagement ssmManager;
    String sptoken=null;


    public synchronized String getAccessToken() {
        if(sptoken==null) {
            SecretsManager sm = new SecretsManager();
            String cognitoName = System.getProperty(COGNITO_NAME, System.getenv(COGNITO_NAME));
            String amazonRegion = System.getProperty(REGION, System.getenv(REGION));

            String oauth_token_url = "/osdu/cognito/" + cognitoName + "/oauth/token-uri";
            String oauth_custom_scope = "/osdu/cognito/" + cognitoName + "/oauth/custom-scope";

            String client_credentials_client_id = "/osdu/cognito/" + cognitoName + "/client/client-credentials/id";
            String client_secret_key = "client_credentials_client_secret";
            String client_secret_secretName = "/osdu/cognito/" + cognitoName + "/client-credentials-secret";

            amazonAWSCredentials = IAMConfig.amazonAWSCredentials();
            ssmManager = AWSSimpleSystemsManagementClientBuilder.standard()
                    .withCredentials(amazonAWSCredentials)
                    .withRegion(amazonRegion)
                    .build();

            client_credentials_clientid = getSsmParameter(client_credentials_client_id);

            client_credentials_secret = sm.getSecret(client_secret_secretName, amazonRegion, client_secret_key);

            String tokenUrl = getSsmParameter(oauth_token_url);

            awsOauthCustomScope = getSsmParameter(oauth_custom_scope);

            sp = new ServicePrincipal(amazonRegion, tokenUrl, awsOauthCustomScope);
            sptoken = sp.getServicePrincipalAccessToken(client_credentials_clientid, client_credentials_secret);
        }

        return sptoken;



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
        GetParameterRequest paramRequest = (new GetParameterRequest()).withName(parameterKey).withWithDecryption(true);
        GetParameterResult paramResult = ssmManager.getParameter(paramRequest);
        return paramResult.getParameter().getValue();
    }
}
