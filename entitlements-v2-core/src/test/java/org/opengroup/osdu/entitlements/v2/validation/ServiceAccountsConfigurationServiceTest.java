package org.opengroup.osdu.entitlements.v2.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.util.FileReaderService;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAccountsConfigurationServiceTest {

    private static final String SERVICE_P_ID = "service_principal@xxx.com";

    @Mock
    private AppProperties appProperties;
    @Spy
    private FileReaderService fileReaderService = new FileReaderService();
    @Mock
    private RequestInfo requestInfo;
    @InjectMocks
    private ServiceAccountsConfigurationService serviceAccountsConfigurationService;

    private TenantInfo tenantInfo;

    @Before
    public void setup() throws Exception {
        when(appProperties.getGroupsOfServicePrincipal()).thenReturn("/test_groups_of_service_principal.json");
        tenantInfo = mock(TenantInfo.class);
        when(tenantInfo.getServiceAccount()).thenReturn(SERVICE_P_ID);
        when(requestInfo.getTenantInfo()).thenReturn(tenantInfo);
        Whitebox.invokeMethod(serviceAccountsConfigurationService, "init");
        verify(fileReaderService).readFile("/test_groups_of_service_principal.json");
    }

    @Test
    public void shouldReturnGroupsOfServicePrincipal() {
        Set<String> res = serviceAccountsConfigurationService.getServiceAccountGroups(SERVICE_P_ID);
        assertEquals(new HashSet<>(Arrays.asList("users", "users.data.root")), res);
    }

    @Test
    public void shouldReturnNoGroupsIfGivenUserIsNotServicePrincipal() {
        Set<String> res = serviceAccountsConfigurationService.getServiceAccountGroups("member@xxx.com");
        assertEquals(0, res.size());
    }

    @Test
    public void shouldReturnTrueIfServicePrincipalInBootstrapGroup() {
        EntityNode memberNode = EntityNode.builder().nodeId(SERVICE_P_ID).dataPartitionId("common").build();
        EntityNode groupNode = EntityNode.builder().name("users.data.root").dataPartitionId("common").build();
        boolean res = serviceAccountsConfigurationService.isMemberProtectedServiceAccount(memberNode, groupNode);
        assertTrue(res);
    }

    @Test
    public void shouldReturnFalseIfServiceAccountInBootstrapGroupWithWrongDataPartition() {
        EntityNode memberNode = EntityNode.builder().nodeId(SERVICE_P_ID).dataPartitionId("tenant1").build();
        EntityNode groupNode = EntityNode.builder().name("users.data.root").dataPartitionId("common").build();
        boolean res = serviceAccountsConfigurationService.isMemberProtectedServiceAccount(memberNode, groupNode);
        assertFalse(res);
    }

    @Test
    public void shouldReturnFalseIfServiceAccountIsNotExistingInBootstrapGroup() {
        EntityNode memberNode = EntityNode.builder().nodeId("member@xxx.com").dataPartitionId("common").build();
        EntityNode groupNode = EntityNode.builder().name("users.data.root").dataPartitionId("common").build();
        boolean res = serviceAccountsConfigurationService.isMemberProtectedServiceAccount(memberNode, groupNode);
        assertFalse(res);
    }

    @Test
    public void shouldReturnFalseIfServiceAccountInNonBootstrapGroup() {
        EntityNode memberNode = EntityNode.builder().nodeId("service-project-id@xxx.com").dataPartitionId("common").build();
        EntityNode groupNode = EntityNode.builder().name("users.test").dataPartitionId("common").build();
        boolean res = serviceAccountsConfigurationService.isMemberProtectedServiceAccount(memberNode, groupNode);
        assertFalse(res);
    }
}
