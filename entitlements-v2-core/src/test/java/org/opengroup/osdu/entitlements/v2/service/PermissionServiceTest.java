package org.opengroup.osdu.entitlements.v2.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.http.HttpStatus;

@RunWith(MockitoJUnitRunner.class)
public class PermissionServiceTest {

    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;
    @Mock
    private RequestInfo requestInfo;
    @Mock
    private RequestInfoUtilService requestInfoUtilService;
    @Mock
    private AuthorizationService authorizationService;
    @InjectMocks
    private PermissionService permissionService;

    @Before
    public void setup() {
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setServiceAccount("datafier@dp.domain.com");
        Mockito.when(requestInfo.getTenantInfo()).thenReturn(tenantInfo);
        Mockito.when(requestInfoUtilService.getDomain("dp")).thenReturn("dp.domain.com");
    }

    @Test
    public void shouldAllowInternalServiceAccountManageMembers() {
        EntityNode requester = EntityNode.builder().nodeId("datafier@dp.domain.com").name("datafier@dp.domain.com").type(NodeType.USER).dataPartitionId("dp").build();
        permissionService.verifyCanManageMembers(requester, null);
    }

    @Test
    public void shouldAllowOpsRoleManageMembers() {
        EntityNode requester = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").type(NodeType.USER).dataPartitionId("dp").build();
        Mockito.when(authorizationService.isCurrentUserAuthorized(null, AppProperties.OPS)).thenReturn(true);
        permissionService.verifyCanManageMembers(requester, null);
    }

    @Test
    public void shouldAllowOwnerOfTheGroupManageMembers() {
        EntityNode groupNode = EntityNode.builder().nodeId("user.x@dp.domain.com").name("user.x").dataPartitionId("dp").type(NodeType.GROUP).build();
        EntityNode requester = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").type(NodeType.USER).dataPartitionId("dp").build();
        Mockito.when(retrieveGroupRepo.hasDirectChild(Mockito.eq(groupNode), Mockito.any())).thenReturn(true);
        permissionService.verifyCanManageMembers(requester, groupNode);
    }

    @Test
    public void shouldAllowGroupIsDataOrUserGroupAndCallerIsNotOwnerOfGroupButCallerBelongsToDataRootGroup() {
        EntityNode groupNode = EntityNode.builder().nodeId("users.y@dp.domain.com").name("users.y").type(NodeType.GROUP).dataPartitionId("dp").build();
        EntityNode requester = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").type(NodeType.USER).dataPartitionId("dp").build();
        Mockito.when(authorizationService.isCurrentUserAuthorized(null, "users.data.root")).thenReturn(true);

        permissionService.verifyCanManageMembers(requester, groupNode);

        groupNode = EntityNode.builder().nodeId("data.y@dp.domain.com").name("data.y").type(NodeType.GROUP).dataPartitionId("dp").build();

        permissionService.verifyCanManageMembers(requester, groupNode);
    }

    @Test
    public void shouldThrow401IfItIsNotOwnerOfTheGroup() {
        EntityNode groupNode = EntityNode.builder().nodeId("user.x@dp.domain.com").name("user.x").dataPartitionId("dp").type(NodeType.GROUP).build();
        EntityNode requester = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").type(NodeType.USER).dataPartitionId("dp").build();
        Mockito.when(authorizationService.isCurrentUserAuthorized(null, "users.data.root")).thenReturn(false);
        Mockito.when(retrieveGroupRepo.hasDirectChild(Mockito.eq(groupNode), Mockito.any())).thenReturn(false);
        try {
            permissionService.verifyCanManageMembers(requester, groupNode);
            Assert.fail("401 exception is expected");
        } catch (AppException appException) {
            Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), appException.getError().getCode());
            Assert.assertEquals(HttpStatus.UNAUTHORIZED.getReasonPhrase(), appException.getError().getReason());
            Assert.assertEquals("Not authorized to manage members", appException.getError().getMessage());
        }
    }
}
