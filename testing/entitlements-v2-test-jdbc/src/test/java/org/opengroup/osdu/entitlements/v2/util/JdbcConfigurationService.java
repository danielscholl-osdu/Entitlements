/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.util;

import com.google.common.base.Strings;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;

/**
 * This class is getting the values from environment variables
 * to be used in integration tests
 */
public class JdbcConfigurationService implements ConfigurationService {
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
        String groupId = System.getProperty("GROUP_ID", System.getenv("GROUP_ID"));
        if (Strings.isNullOrEmpty(groupId)) {
            groupId = "group";
        }
        return groupId;
    }

    @Override
    public String getIdOfGroup(String groupName) {
        return groupName.toLowerCase() + "@" + getTenantId() + "." + getDomain();
    }
}
