package org.opengroup.osdu.entitlements.v2;

import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.Token;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.util.AnthosConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.OpenIDTokenProvider;

public class TenantInitTest extends AcceptanceBaseTest {

    public TenantInitTest() {
        super(new AnthosConfigurationService(), new OpenIDTokenProvider());
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        return RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath("tenant-provisioning")
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
