package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.creategroup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.SpiJdbcTestConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.*;

@SpringBootTest(classes = SpiJdbcTestConfig.class)
@RunWith(SpringRunner.class)
public class CreateGroupJdbcTest {
    @MockBean
    protected AuditLogger auditLogger;

    @Autowired
    private CreateGroupRepoJdbc sut;

    @MockBean
    private MemberRepository memberRepository;
    @MockBean
    private GroupRepository groupRepository;
    @MockBean
    private JdbcTemplateRunner jdbcTemplateRunner;

    @Test
    public void should_updateReference_whenCreateGroup_andNotAddDataRootGroup() {
        EntityNode groupNode = getUsersGroupNode("x");
        EntityNode requesterNode = getRequesterNode();
        CreateGroupRepoDto createGroupRepoDto = CreateGroupRepoDto.builder()
                .requesterNode(requesterNode)
                .dataRootGroupNode(null)
                .addDataRootGroup(false)
                .partitionId(DATA_PARTITION_ID).build();

        GroupInfoEntity groupEntity = GroupInfoEntity.fromEntityNode(groupNode);

        when(groupRepository.save(groupEntity)).thenReturn(groupEntity);
        when(groupRepository.findByEmail(any())).thenReturn(Collections.singletonList(groupEntity));
        when(memberRepository.findMembersByGroup(any())).thenReturn(Collections.singletonList(MemberInfoEntity.fromEntityNode(requesterNode, Role.OWNER)));

        //when
        sut.createGroup(groupNode, createGroupRepoDto);

        //then
        List<GroupInfoEntity> actual = groupRepository.findByEmail(groupNode.getNodeId());

        assertEquals(1, actual.size());

        GroupInfoEntity actualGroup = actual.get(0);

        assertEquals(groupNode.getNodeId(), actualGroup.getEmail());
        assertEquals(groupNode.getName(), actualGroup.getName());
        assertEquals(groupNode.getDataPartitionId(), actualGroup.getPartitionId());

        List<MemberInfoEntity> actualGroupOwners = memberRepository.findMembersByGroup(actualGroup.getId());

        assertEquals(1, actualGroupOwners.size());

        MemberInfoEntity actualOwner = actualGroupOwners.get(0);

        assertEquals(requesterNode.getNodeId(), actualOwner.getEmail());
        assertEquals(Role.OWNER.getValue(), actualOwner.getRole());

        verify(auditLogger).createGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
    }

    @Test
    public void should_updateReference_whenCreateGroup_andAddDataRootGroup() {
        EntityNode groupNode = getUsersGroupNode("x");
        EntityNode requesterNode = getRequesterNode();
        EntityNode dataRootGroupNode = getDataRootGroupNode();

        CreateGroupRepoDto createChildGroupRepoDto = CreateGroupRepoDto.builder()
                .requesterNode(requesterNode)
                .dataRootGroupNode(dataRootGroupNode)
                .addDataRootGroup(true)
                .partitionId(DATA_PARTITION_ID).build();


        GroupInfoEntity groupEntity = GroupInfoEntity.fromEntityNode(groupNode);
        when(groupRepository.findByEmail(any())).thenReturn(Collections.singletonList(groupEntity));
        when(groupRepository.save(groupEntity)).thenReturn(groupEntity);
        when(groupRepository.findDirectParents(anyList())).thenReturn(Collections.singletonList(GroupInfoEntity.fromEntityNode(dataRootGroupNode)));
        when(memberRepository.findMembersByGroup(any())).thenReturn(Collections.singletonList(MemberInfoEntity.fromEntityNode(requesterNode, Role.OWNER)));

        //when
        sut.createGroup(groupNode, createChildGroupRepoDto);

        //then
        List<GroupInfoEntity> actual = groupRepository.findByEmail(groupNode.getNodeId());

        assertEquals(1, actual.size());

        GroupInfoEntity actualGroup = actual.get(0);

        assertEquals(groupNode.getNodeId(), actualGroup.getEmail());
        assertEquals(groupNode.getName(), actualGroup.getName());
        assertEquals(groupNode.getDataPartitionId(), actualGroup.getPartitionId());

        List<GroupInfoEntity> actualParents = groupRepository.findDirectParents(Collections.singletonList(actualGroup.getId()));

        assertEquals(1, actualParents.size());

        GroupInfoEntity actualRootGroup = actualParents.get(0);

        assertEquals(dataRootGroupNode.getNodeId(), actualRootGroup.getEmail());
        assertEquals(dataRootGroupNode.getName(), actualRootGroup.getName());
        assertEquals(dataRootGroupNode.getDataPartitionId(), actualRootGroup.getPartitionId());

        List<MemberInfoEntity> actualGroupOwners = memberRepository.findMembersByGroup(actualGroup.getId());

        assertEquals(1, actualGroupOwners.size());

        MemberInfoEntity actualOwner = actualGroupOwners.get(0);

        assertEquals(requesterNode.getNodeId(), actualOwner.getEmail());
        assertEquals(Role.OWNER.getValue(), actualOwner.getRole());

        verify(auditLogger).createGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
    }
}
