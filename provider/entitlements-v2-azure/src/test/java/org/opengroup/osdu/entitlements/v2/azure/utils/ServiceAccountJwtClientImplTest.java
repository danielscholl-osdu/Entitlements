package org.opengroup.osdu.entitlements.v2.azure.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAccountJwtClientImplTest {

    @InjectMocks
    private ServiceAccountJwtClientImpl serviceAccountJwtClient;

    @Mock
    private ITenantFactory tenantInfoServiceProvider;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private JaxRsDpsLog log;

    @Mock
    private AzureServicePrincipleTokenService tokenService;

    @Test
    public void shouldThrowAppExceptionForInvalidTenant() {
        try {
            String token = serviceAccountJwtClient.getIdToken("test");
            fail("should throw exception");
        } catch (AppException ex) {
            assertEquals(400, ex.getError().getCode());
            assertEquals("Invalid tenant Name", ex.getError().getReason());
            assertEquals("Invalid tenant Name from azure", ex.getError().getMessage());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldReturnIdTokenSuccessfully() {
        String partitionId = "test-partition-id";
        String token = "aaa.bbb.ccc";
        String expectedIdToken = "Bearer " + token;
        TenantInfo mockTenantInfo = mock(TenantInfo.class);
        when(mockTenantInfo.getServiceAccount()).thenReturn("testuser@xyz.com");
        when(tenantInfoServiceProvider.getTenantInfo(partitionId)).thenReturn(mockTenantInfo);
        when(tokenService.getAuthorizationToken()).thenReturn(token);

        String idToken = serviceAccountJwtClient.getIdToken(partitionId);

        assertEquals(expectedIdToken, idToken);
    }

}