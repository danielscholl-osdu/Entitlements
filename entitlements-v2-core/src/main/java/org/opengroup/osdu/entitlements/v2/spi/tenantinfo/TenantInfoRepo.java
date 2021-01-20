package org.opengroup.osdu.entitlements.v2.spi.tenantinfo;

public interface TenantInfoRepo {
    String getServiceAccountOrServicePrincipal(String partitionId);
}
