/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.interceptor;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class RequestHeaderInterceptor implements HandlerInterceptor {

    @Autowired
    DpsHeaders dpsheaders;

    @Autowired
    private JaxRsDpsLog log;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        log.debug("Intercepted the request in Handler for extracting user/service principal.");
        Map<String,String> headers = dpsheaders.getHeaders();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt princ = (Jwt) authentication.getPrincipal();
        String memberEmail = princ.getClaimAsString("email");
        if(memberEmail!=null) {
            headers.put("x-user-id", memberEmail);
            return true;
        }
        else {
        	response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }


    	

}
