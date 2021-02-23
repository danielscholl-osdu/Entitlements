package org.opengroup.osdu.entitlements.v2.di;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.util.FileReaderService;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class KeySvcAccBeanConfigurationTests {

    @Mock
    private AppProperties appProperties;
    @Mock
    private FileReaderService fileReaderService;
    @Mock
    private RequestInfo requestInfo;
    @Mock
    private JaxRsDpsLog log;
    @InjectMocks
    private KeySvcAccBeanConfiguration sut;

    private TenantInfo tenantInfo;

    private final String SERVICE_PRINCIPAL = "{\n" +
            "  \"users\": [\n" +
            "    {\n" +
            "      \"email\": \"SERVICE_PRINCIPAL\",\n" +
            "      \"role\": \"OWNER\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"ownersOf\": [\n" +
            "    {\n" +
            "      \"groupName\": \"users.data.root\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"groupName\": \"users\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Before
    public void setup() throws Exception {
        when(appProperties.getGroupsOfServicePrincipal()).thenReturn("groups_of_service_principal.json");
        prepareFileReaderForUsersTesting();
        tenantInfo = Mockito.mock(TenantInfo.class);
        when(requestInfo.getTenantInfo()).thenReturn(tenantInfo);
        Whitebox.invokeMethod(sut, "init");
    }

    @Test
    public void shouldReturnTrue_IfServicePrincipalAccount() {
        when(tenantInfo.getServiceAccount()).thenReturn("datafier@xxx.iam.gserviceaccount.com");
        boolean res = sut.isKeyServiceAccount("datafier@xxx.iam.gserviceaccount.com");
        assertTrue(res);
    }

    @Test
    public void shouldReturnFalse_IfDNonKeySvcAccount() {
        when(tenantInfo.getServiceAccount()).thenReturn("datafier@xxx.iam.gserviceaccount.com");
        boolean res = sut.isKeyServiceAccount("member@xxx.com");
        assertFalse(res);
    }

    @Test
    public void shouldReturnGroups_ifGivenDatafierAcc() {
        when(tenantInfo.getServiceAccount()).thenReturn("datafier@xxx.iam.gserviceaccount.com");
        Set<String> res = sut.getServiceAccountGroups("datafier@xxx.iam.gserviceaccount.com");
        assertEquals(2, res.size());
        assertTrue(res.contains("users"));
        assertTrue(res.contains("users.data.root"));
    }

    @Test
    public void shouldReturnEmptyGroupSet_ifGivenEmailIsNotKeySvcAcc() {
        when(tenantInfo.getServiceAccount()).thenReturn("datafier@xxx.iam.gserviceaccount.com");
        Set<String> res = sut.getServiceAccountGroups("member@xxx.com");
        assertEquals(0, res.size());
    }

    @Test
    public void shouldReturnTrue_ifKeySvcAccInBootstrapGroup() {
        when(tenantInfo.getServiceAccount()).thenReturn("datafier@xxx.iam.gserviceaccount.com");
        boolean res = sut.isKeySvcAccountInBootstrapGroup("users.data.root", "datafier@xxx.iam.gserviceaccount.com");
        assertTrue(res);
    }

    @Test
    public void shouldReturnFalse_ifGivenNonKeySvcAccInBootstrapGroup() {
        when(tenantInfo.getServiceAccount()).thenReturn("datafier@xxx.iam.gserviceaccount.com");
        boolean res = sut.isKeySvcAccountInBootstrapGroup("users.data.root", "member@xxx.iam.gserviceaccount.com");
        assertFalse(res);
    }

    @Test
    public void shouldReturnFalse_ifGivenKeySvcAccInNonBootstrapGroup() {
        when(tenantInfo.getServiceAccount()).thenReturn("datafier@xxx.iam.gserviceaccount.com");
        boolean res = sut.isKeySvcAccountInBootstrapGroup("users.test", "service-project-id@appspot.gserviceaccount.com");
        assertFalse(res);
    }

    private void prepareFileReaderForUsersTesting() {
        when(fileReaderService.readFile("groups_of_service_principal.json")).thenReturn(SERVICE_PRINCIPAL);
    }
}