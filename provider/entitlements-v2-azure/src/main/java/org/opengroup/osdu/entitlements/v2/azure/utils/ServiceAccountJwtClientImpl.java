package org.opengroup.osdu.entitlements.v2.azure.utils;

import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@RequiredArgsConstructor
public class ServiceAccountJwtClientImpl implements IServiceAccountJwtClient {
    private final ITenantFactory tenantInfoServiceProvider;
    private final DpsHeaders dpsHeaders;
    private final JaxRsDpsLog log;
    private final AzureServicePrincipleTokenService tokenService;

    @Override
    public String getIdToken(String partitionId){

        TenantInfo tenant = this.tenantInfoServiceProvider.getTenantInfo(partitionId);
        if (tenant == null) {
            this.log.error("Invalid tenant name receiving from azure");
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid tenant Name", "Invalid tenant Name from azure");
        }

        this.dpsHeaders.put(DpsHeaders.USER_EMAIL, tenant.getServiceAccount());

        return "Bearer " + this.tokenService.getAuthorizationToken();
    }
}
