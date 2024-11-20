package org.opengroup.osdu.entitlements.v2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.util.CommonConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.TokenTestUtils;

@Disabled
public class TenantInitTest extends AcceptanceBaseTest {

    public TenantInitTest() {
        super(new CommonConfigurationService());
    }

    @BeforeEach
    @Override
    public void setupTest() throws Exception {
        this.testUtils = new TokenTestUtils();
    }

    @AfterEach
    @Override
    public void tearTestDown() throws Exception {
        this.testUtils = null;
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

        entitlementsV2Service.provisionGroupsForNewTenant(testUtils.getToken());
        entitlementsV2Service.provisionGroupsForNewTenant(testUtils.getToken());
    }
}
