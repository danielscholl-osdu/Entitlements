/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.util;

import com.google.common.base.Strings;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;


public class IBMConfigurationService implements ConfigurationService {
    private static String SERVICE_URL;

    @Override
    public String getTenantId() {
        String tenant = System.getProperty("TENANT_NAME", System.getenv("TENANT_NAME"));
        return tenant;
    }

    @Override
    public synchronized String getServiceUrl() {
        if (Strings.isNullOrEmpty(SERVICE_URL)) {
            String serviceUrl = System.getProperty("ENTITLEMENT_V2_URL", System.getenv("ENTITLEMENT_V2_URL"));
            if (serviceUrl == null || serviceUrl.contains("-null")) {
                serviceUrl = "http://localhost:8080/api/entitlements/v2/";
            }
            SERVICE_URL = serviceUrl;
        }
        return SERVICE_URL;
    }

    @Override
    public String getDomain() {
        String domain = System.getProperty("ENTITLEMENTS_V2_DOMAIN", System.getenv("ENTITLEMENTS_V2_DOMAIN"));
        if (Strings.isNullOrEmpty(domain)) {
            domain = "example.com";
        }
        return domain;
    }

    @Override
    public String getIdOfGroup(String groupName) {
        return groupName.toLowerCase() + "@" + getTenantId() + "." + getDomain();
    }
}
