package org.opengroup.osdu.entitlements.v2.acceptance;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.UpdateGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

import java.util.HashSet;
import java.util.Set;

public abstract class TenantInitTest extends AcceptanceBaseTest {

    public TenantInitTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
    }

    @Override
    protected RequestData getRequestDataForUnauthorizedTest() {
        Token noAccessToken = tokenService.getNoDataAccessToken();
        return RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath("tenant-provisioning")
                .token(noAccessToken.getValue())
                .build();
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
