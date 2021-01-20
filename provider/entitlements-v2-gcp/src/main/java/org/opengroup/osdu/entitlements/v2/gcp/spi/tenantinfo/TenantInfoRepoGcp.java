package org.opengroup.osdu.entitlements.v2.gcp.spi.tenantinfo;

import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.entitlements.v2.spi.tenantinfo.TenantInfoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TenantInfoRepoGcp implements TenantInfoRepo {

    @Autowired
    private ITenantFactory tenantInfoFactory;

    @Override
    public String getServiceAccountOrServicePrincipal(String partitionId) {
        TenantInfo tenantInfo = this.tenantInfoFactory.getTenantInfo(partitionId);
        return tenantInfo.getServiceAccount();
    }
}
