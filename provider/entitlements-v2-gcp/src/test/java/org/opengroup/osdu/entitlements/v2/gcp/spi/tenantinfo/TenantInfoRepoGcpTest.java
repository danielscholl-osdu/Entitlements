package org.opengroup.osdu.entitlements.v2.gcp.spi.tenantinfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantInfoRepoGcpTest {

    @Mock
    private ITenantFactory tenantInfoFactory;
    @Mock
    private TenantInfo tenantInfo;

    @InjectMocks
    private TenantInfoRepoGcp sut;

    @Before
    public void setup() {
        when(tenantInfo.getServiceAccount()).thenReturn("serviceaccount");
    }

    @Test
    public void shouldGetServiceAccountWhenTenantInfoGotFromFactory() {
        when(tenantInfoFactory.getTenantInfo("dp")).thenReturn(tenantInfo);

        String result = sut.getServiceAccountOrServicePrincipal("dp");
        assertEquals("serviceaccount", result);
    }

    @Test(expected = AppException.class)
    public void shouldThrowExceptionWhenGettingTenantInfoThrowsException() {
        doThrow(AppException.class)
                .when(tenantInfoFactory)
                .getTenantInfo(anyString());

        sut.getServiceAccountOrServicePrincipal("dp");
    }
}
