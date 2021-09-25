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
package org.opengroup.osdu.entitlements.v2.aws.interceptor;



import org.opengroup.osdu.core.aws.entitlements.Authorizer;
import org.opengroup.osdu.core.aws.entitlements.RequestKeys;
import org.opengroup.osdu.core.aws.lambda.HttpStatusCodes;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;


@Component
public class RequestHeaderInterceptor implements HandlerInterceptor {


    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.environment}")
    private String awsEnvironment;

    @Autowired
    DpsHeaders dpsheaders;

    @Autowired
    private JaxRsDpsLog log;

    Authorizer authorizer;


    @PostConstruct
    public void init() {
        authorizer = new Authorizer(awsRegion, awsEnvironment);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        log.info("Intercepted the request. Now validating token..");

        Map<String,String> headers = dpsheaders.getHeaders();
        log.info(headers.get("jwt_decoded"));
        return true;
//        //validate JWT token here
//        // extract custom attribute from the JWT token using OAuth 2.0 and
//        String memberEmail = validateJwt(headers);
//        if(memberEmail!=null) {
//            // If JWT validation succeeds/ can extract user identity
//            //       return true = requests moves forward to the core API code
//            // Update the request Header to include user identity
//            headers.put("x-user-id", memberEmail);
//
//            return true;
//        }
//        else //If JWT validation fails/ cannot extract user identity
//            //       return false = response is handled here
//            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//            return false;
    }


    public String validateJwt(Map<String, String> headers)
    {


        int httpStatusCode = HttpStatusCodes.UNASSIGNED;
        String memberEmail=null;
        // check for valid JWT
        // authorization header is lowercase in osdu services but standard is uppercase first letter
        String authorizationContents = headers.get(RequestKeys.AUTHORIZATION_HEADER_KEY);
        if(authorizationContents == null){
            authorizationContents = headers.get(RequestKeys.AUTHORIZATION_HEADER_KEY.toLowerCase());
        }
        //no JWT
        if(authorizationContents == null)
        {
            throw  AppException.createForbidden("No JWT token. Access is Forbidden");
        }


        memberEmail = authorizer.validateJWT(authorizationContents);

        return memberEmail;
       }


}
