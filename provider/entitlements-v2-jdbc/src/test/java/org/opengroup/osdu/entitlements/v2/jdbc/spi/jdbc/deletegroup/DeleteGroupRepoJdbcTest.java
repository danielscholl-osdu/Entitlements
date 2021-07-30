package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.deletegroup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.*;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DeleteGroupRepoJdbcTest {

    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private RequestInfoUtilService requestInfoUtilService;
    @Autowired
    private DeleteGroupRepoJdbc sut;
    @MockBean
    private GroupRepository groupRepository;
    @MockBean
    private MemberRepository memberRepository;

    @Test
    public void shouldDeleteGroupAndPreserveParents() {
        EntityNode groupToDelete = getUsersGroupNode("x");

        EntityNode childGroup1 = getDataGroupNode("x");
        EntityNode childGroup2 = getDataGroupNode("y");
        EntityNode childGroup3 = getDataGroupNode("z");

        GroupInfoEntity savedChild1 = GroupInfoEntity.fromEntityNode(childGroup1);
        GroupInfoEntity savedChild2 = GroupInfoEntity.fromEntityNode(childGroup2);
        GroupInfoEntity savedChild3 = GroupInfoEntity.fromEntityNode(childGroup3);
        GroupInfoEntity savedGroupToDelete = GroupInfoEntity.fromEntityNode(groupToDelete);

        groupRepository.addChildGroupById(savedGroupToDelete.getId(), savedChild1.getId());
        groupRepository.addChildGroupById(savedGroupToDelete.getId(), savedChild2.getId());
        groupRepository.addChildGroupById(savedGroupToDelete.getId(), savedChild3.getId());

        when(groupRepository.findDirectParents(anyList())).thenReturn(Collections.emptyList());
        when(groupRepository.findByEmail(any())).thenReturn(Collections.singletonList(savedGroupToDelete));

        //when
        sut.deleteGroup(groupToDelete);
        when(groupRepository.findByEmail(any())).thenReturn(Collections.emptyList());

        //then
        assertTrue(groupRepository.findByEmail(groupToDelete.getNodeId()).isEmpty());

        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedChild1.getId())).isEmpty());
        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedChild2.getId())).isEmpty());
        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedChild3.getId())).isEmpty());

        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, groupToDelete.getNodeId());
    }

    @Test
    public void shouldDeleteGroupAndPreserveChildren() {
        EntityNode groupToDelete = getUsersGroupNode("x");

        EntityNode parentGroup1 = getDataGroupNode("x");
        EntityNode parentGroup2 = getDataGroupNode("y");
        EntityNode parentGroup3 = getDataGroupNode("z");

        GroupInfoEntity savedParent1 = GroupInfoEntity.fromEntityNode(parentGroup1);
        GroupInfoEntity savedParent2 = GroupInfoEntity.fromEntityNode(parentGroup2);
        GroupInfoEntity savedParent3 = GroupInfoEntity.fromEntityNode(parentGroup3);
        GroupInfoEntity savedGroupToDelete = GroupInfoEntity.fromEntityNode(groupToDelete);

        when(groupRepository.findDirectParents(anyList())).thenReturn(Collections.emptyList());
        when(groupRepository.findByEmail(any())).thenReturn(Collections.singletonList(savedGroupToDelete));

        //when
        sut.deleteGroup(groupToDelete);
        when(groupRepository.findByEmail(any())).thenReturn(Collections.emptyList());

        //then
        assertTrue(groupRepository.findByEmail(groupToDelete.getNodeId()).isEmpty());

        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedParent1.getId())).isEmpty());
        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedParent2.getId())).isEmpty());
        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedParent3.getId())).isEmpty());

        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, groupToDelete.getNodeId());
    }

    @Test(expected = AppException.class)
    public void shouldReturnIfTheGivenGroupIsNotFoundWhenDeleteGroup() throws Exception{
        EntityNode groupNode = getCommonGroup("newgroup");

        sut.deleteGroup(groupNode);

        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, groupNode.getNodeId());

    }

}
