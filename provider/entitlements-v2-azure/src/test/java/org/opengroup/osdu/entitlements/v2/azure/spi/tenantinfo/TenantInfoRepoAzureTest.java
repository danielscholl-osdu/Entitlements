package org.opengroup.osdu.entitlements.v2.azure.spi.tenantinfo;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.azure.multitenancy.TenantInfoDoc;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.AzureAppProperties;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantInfoRepoAzureTest {

    @Mock
    private CosmosStore cosmosStore;
    @Mock
    private AzureAppProperties config;
    @Mock
    private TenantInfoDoc tenantInfoDoc;

    @InjectMocks
    private TenantInfoRepoAzure sut;

    @Before
    public void setup() {
        when(config.getCosmosDbName()).thenReturn("cosmosDbName");
        when(config.getTenantInfoContainerName()).thenReturn("tenantInfoContanerName");
    }

    @Test
    public void shouldReturnServicePrincipalWhenGettingTenantInfoDocFromCosmosDb() {
        when(cosmosStore.findItem(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(tenantInfoDoc));
        when(tenantInfoDoc.getServiceprincipalAppId()).thenReturn("service-principal");

        String result = sut.getServiceAccountOrServicePrincipal("dp");
        assertEquals("service-principal", result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenTenantInfoDocNotFoundFromCosmosDb() {
        when(cosmosStore.findItem(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        try {
            sut.getServiceAccountOrServicePrincipal("dp");
        } catch (AppException apx) {
            assertEquals(HttpStatus.SC_NOT_FOUND, apx.getError().getCode());
        } catch (Exception ex) {
            fail("should not go here");
        }
    }

    @Test(expected = AppException.class)
    public void shouldThrowExceptionWhenCosmosDbThrowsException() {
        doThrow(AppException.class)
                .when(cosmosStore)
                .findItem(anyString(), any(), any(), any(), any(), any());

        sut.getServiceAccountOrServicePrincipal("dp");
    }

}
