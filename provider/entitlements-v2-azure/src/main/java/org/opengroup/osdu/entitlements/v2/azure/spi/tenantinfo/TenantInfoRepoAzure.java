package org.opengroup.osdu.entitlements.v2.azure.spi.tenantinfo;

import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.azure.multitenancy.TenantInfoDoc;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.AzureAppProperties;
import org.opengroup.osdu.entitlements.v2.spi.tenantinfo.TenantInfoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class TenantInfoRepoAzure implements TenantInfoRepo {

    @Autowired
    private CosmosStore cosmosStore;
    @Autowired
    private AzureAppProperties config;


    @Override
    public String getServiceAccountOrServicePrincipal(String partitionId) {
        String cosmosDbName = config.getCosmosDbName();
        String tenantInfoContainer = config.getTenantInfoContainerName();

        TenantInfoDoc tenantInfoDoc = cosmosStore.findItem(partitionId, cosmosDbName, tenantInfoContainer, partitionId, partitionId, TenantInfoDoc.class)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "tenant not found", "the tenant does not exist"));

        return tenantInfoDoc.getServiceprincipalAppId();
    }
}
