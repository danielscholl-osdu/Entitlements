package org.opengroup.osdu.entitlements.v2.acceptance.util;

public interface ConfigurationService {

    String getTenantId();

    String getServiceUrl();

    String getDomain();

    String getIdOfGroup(String groupName);
}
