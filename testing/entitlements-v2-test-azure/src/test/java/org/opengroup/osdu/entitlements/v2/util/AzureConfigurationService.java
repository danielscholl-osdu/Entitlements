package org.opengroup.osdu.entitlements.v2.util;

import com.google.common.base.Strings;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;

/**
 * This class is getting the values from environment variables
 * to be used in integration tests
 */
public class AzureConfigurationService implements ConfigurationService {
    private static String SERVICE_URL;

    @Override
    public String getTenantId() {
        return "opendes";
    }

    @Override
    public String getUnauthorizedTenantId() {
        return "default-dev-sis-internal-hq";
    }

    @Override
    public synchronized String getServiceUrl() {
        if (Strings.isNullOrEmpty(SERVICE_URL)) {
            String serviceUrl = System.getProperty("HOST_URL", System.getenv("HOST_URL"));
            if (serviceUrl == null || serviceUrl.contains("-null")) {
                serviceUrl = "http://localhost:8080/entitlements/v2";
            }
            SERVICE_URL = serviceUrl;
        }
        return SERVICE_URL;
    }

    @Override
    public String getDomain() {
        return "contoso.com";
    }

    @Override
    public String getIdOfGroup(String groupName) {
        return groupName.toLowerCase() + "@" + getTenantId() + "." + getDomain();
    }
}
