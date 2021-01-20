package org.opengroup.osdu.entitlements.v2.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;

@RunWith(MockitoJUnitRunner.class)
public class PermissionServiceTest {

    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;
    @Mock
    private RequestInfo requestInfo;
    @Mock
    private RequestInfoUtilService requestInfoUtilService;
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
    public void shouldReturnTrueIfItIsInternalServiceAccount() {
        EntityNode groupNode = EntityNode.builder().nodeId("user.x@dp.domain.com").name("user.x").dataPartitionId("dp").type(NodeType.GROUP).build();
        EntityNode requester = EntityNode.builder().nodeId("datafier@dp.domain.com").name("datafier@dp.domain.com").type(NodeType.USER).dataPartitionId("dp").build();
        Assert.assertTrue(permissionService.hasOwnerPermissionOf(requester, groupNode));
    }

    @Test
    public void shouldReturnTrueIfItIsOwnerOfTheGroup() {
        EntityNode groupNode = EntityNode.builder().nodeId("user.x@dp.domain.com").name("user.x").dataPartitionId("dp").type(NodeType.GROUP).build();
        EntityNode requester = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").type(NodeType.USER).dataPartitionId("dp").build();
        Mockito.when(retrieveGroupRepo.hasDirectChild(Mockito.eq(groupNode), Mockito.any())).thenReturn(true);
        Assert.assertTrue(permissionService.hasOwnerPermissionOf(requester, groupNode));
    }

    @Test
    public void shouldReturnTrueIfGivenGroupIsDataOrUserGroupAndCallerIsNotOwnerOfGroupButCallerBelongsToDataRootGroup() {
        EntityNode groupNode = EntityNode.builder().nodeId("users.y@dp.domain.com").name("users.y").type(NodeType.GROUP).dataPartitionId("dp").build();
        EntityNode rootDataGroupNode = EntityNode.builder().nodeId("users.data.root@dp.domain.com").name("users.data.root").type(NodeType.GROUP).dataPartitionId("dp").build();
        EntityNode requester = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").type(NodeType.USER).dataPartitionId("dp").build();
        Mockito.when(retrieveGroupRepo.hasDirectChild(Mockito.eq(groupNode), Mockito.any())).thenReturn(false);
        Mockito.when(retrieveGroupRepo.hasDirectChild(Mockito.eq(rootDataGroupNode), Mockito.any())).thenReturn(true);
        Mockito.when(retrieveGroupRepo.groupExistenceValidation(rootDataGroupNode.getNodeId(), rootDataGroupNode.getDataPartitionId())).thenReturn(rootDataGroupNode);

        Assert.assertTrue(permissionService.hasOwnerPermissionOf(requester, groupNode));

        groupNode = EntityNode.builder().nodeId("data.y@dp.domain.com").name("data.y").type(NodeType.GROUP).dataPartitionId("dp").build();

        Assert.assertTrue(permissionService.hasOwnerPermissionOf(requester, groupNode));
    }

    @Test
    public void shouldReturnFalseIfItIsNotOwnerOfTheGroup() {
        EntityNode groupNode = EntityNode.builder().nodeId("user.x@dp.domain.com").name("user.x").dataPartitionId("dp").type(NodeType.GROUP).build();
        EntityNode requester = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").type(NodeType.USER).dataPartitionId("dp").build();
        EntityNode rootDataGroupNode = EntityNode.builder().nodeId("users.data.root@dp.domain.com").name("users.data.root").type(NodeType.GROUP).dataPartitionId("dp").build();
        Mockito.when(retrieveGroupRepo.groupExistenceValidation(rootDataGroupNode.getNodeId(), rootDataGroupNode.getDataPartitionId())).thenReturn(rootDataGroupNode);
        Mockito.when(retrieveGroupRepo.hasDirectChild(Mockito.eq(groupNode), Mockito.any())).thenReturn(false);
        Assert.assertFalse(permissionService.hasOwnerPermissionOf(requester, groupNode));
    }
}
