/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.entitlements.v2.util;

import java.io.IOException;

import org.opengroup.osdu.core.ibm.util.KeyCloakProvider;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IBMTokenService implements TokenService {

    private static String TOKEN;
    private static final String SERVICE_PRINCIPAL =  System.getProperty("SERVICE_PRINCIPAL", System.getenv("SERVICE_PRINCIPAL"));
    private static final String SERVICE_PRINCIPAL_PASSWORD =  System.getProperty("SERVICE_PRINCIPAL_PASSWORD", System.getenv("SERVICE_PRINCIPAL_PASSWORD"));
    /**
     * Returns token of a service principal
     */
    @Override
    public synchronized Token getToken()  {
    	try {
			if (Strings.isNullOrEmpty(TOKEN)) {
				TOKEN = KeyCloakProvider.getToken(SERVICE_PRINCIPAL, SERVICE_PRINCIPAL_PASSWORD);
	          
				} 
			} catch (IOException e) {
			log.error("Service principal token genration failed"+e.getMessage());
			e.printStackTrace();
		}
        
        return Token.builder()
                .value(TOKEN)
                .userId(SERVICE_PRINCIPAL)
                .build();
    }


}
