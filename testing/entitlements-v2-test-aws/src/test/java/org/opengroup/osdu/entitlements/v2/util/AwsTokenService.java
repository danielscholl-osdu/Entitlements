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

import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

public class AwsTokenService implements TokenService {


    AwsTestUtils utils = new AwsTestUtils();
    private static String TOKEN;
    private static final String SERVICE_PRINCIPAL_EMAIL =  System.getProperty("SERVICE_PRINCIPAL_EMAIL", System.getenv("SERVICE_PRINCIPAL_EMAIL"));
    /**
     * Returns token of a service principal
     */
    @Override
    public synchronized Token getToken()  {
        if (Strings.isNullOrEmpty(TOKEN)) {
            String sptoken  = utils.getAccessToken();
            sptoken = sptoken.replace("Bearer ","");
            TOKEN = sptoken;
        }
        return Token.builder()
                .value(TOKEN)
                .userId(SERVICE_PRINCIPAL_EMAIL)
                .build();
    }


}
