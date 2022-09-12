package org.opengroup.osdu.entitlements.v2.acceptance;

import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

public abstract class TenantInitTest extends AcceptanceBaseTest {

    public TenantInitTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        return RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath("tenant-provisioning")
                .body("{}")
                .build();
    }

    /**
     * Executing provisioning request two times to ensure the request is idempotent
     */
    @Test
    public void shouldSuccessfullyProvisionGroupsForNewTenant() throws Exception {
        Token token = tokenService.getToken();

        entitlementsV2Service.provisionGroupsForNewTenant(token.getValue());

        entitlementsV2Service.provisionGroupsForNewTenant(token.getValue());
    }
}
