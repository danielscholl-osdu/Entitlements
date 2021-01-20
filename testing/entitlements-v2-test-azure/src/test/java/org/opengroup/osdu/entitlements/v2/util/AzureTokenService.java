package org.opengroup.osdu.entitlements.v2.util;

import com.google.common.base.Strings;
import org.opengroup.osdu.azure.util.AzureServicePrincipal;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

public class AzureTokenService implements TokenService {
    private static final String INTEGRATION_TESTER = "integration_tester_entitlements@desid.com";
    private static final String CLIENT_ID =
            System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
    private static final String CLIENT_SECRET =
            System.getProperty("AZURE_TESTER_SERVICEPRINCIPAL_SECRET",
                    System.getenv("AZURE_TESTER_SERVICEPRINCIPAL_SECRET"));
    private static final String TENANT_ID =
            System.getProperty("AZURE_AD_TENANT_ID", System.getenv("AZURE_AD_TENANT_ID"));
    private static final String APP_RESOURCE_ID =
            System.getProperty("AZURE_AD_APP_RESOURCE_ID", System.getenv("AZURE_AD_APP_RESOURCE_ID"));
    private static final String NO_DATA_ACCESS_CLIENT_ID =
            System.getProperty("NO_DATA_ACCESS_TESTER", System.getenv("NO_DATA_ACCESS_TESTER"));
    private static final String NO_DATA_ACCESS_CLIENT_SECRET =
            System.getProperty("NO_DATA_ACCESS_TESTER_SERVICEPRINCIPAL_SECRET",
                    System.getenv("NO_DATA_ACCESS_TESTER_SERVICEPRINCIPAL_SECRET"));
    private static String TOKEN;
    private static String NODATAACEESSTOKEN;

    @Override
    public synchronized Token getToken() {
        if (Strings.isNullOrEmpty(TOKEN)) {
            TOKEN = retrieveToken(CLIENT_ID, CLIENT_SECRET);
        }
        return Token.builder()
                .value(TOKEN)
                .userId(INTEGRATION_TESTER)
                .build();
    }

    @Override
    public synchronized Token getNoDataAccessToken() {
        if (Strings.isNullOrEmpty(NODATAACEESSTOKEN)) {
            NODATAACEESSTOKEN = retrieveToken(NO_DATA_ACCESS_CLIENT_ID, NO_DATA_ACCESS_CLIENT_SECRET);
        }
        return Token.builder()
                .value(NODATAACEESSTOKEN)
                .userId(NO_DATA_ACCESS_CLIENT_ID)
                .build();    }

    private String retrieveToken(String clientId, String clientSecret) {
        try {
            return new AzureServicePrincipal().getIdToken(clientId, clientSecret, TENANT_ID, APP_RESOURCE_ID);
        } catch (Exception e) {
            throw new RuntimeException("Cannot retrieve token", e);
        }
    }
}
