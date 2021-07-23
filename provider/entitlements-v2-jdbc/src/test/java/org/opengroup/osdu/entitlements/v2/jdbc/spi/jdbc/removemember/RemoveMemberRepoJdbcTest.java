package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.removemember;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.DATA_PARTITION_ID;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getCommonGroup;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getDataViewersGroupNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getMemberNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getRequesterNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getUsersGroupNode;

import java.util.Collections;
import java.util.List;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.addmember.AddMemberRepoJdbc;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit4.SpringRunner;

@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:db/create-db-script.sql")
@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:db/drop-db-script.sql")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@SpringBootTest
@RunWith(SpringRunner.class)
public class RemoveMemberRepoJdbcTest {

    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private JaxRsDpsLog log;

    @Autowired
    private RemoveMemberRepoJdbc sut;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private AddMemberRepoJdbc addMemberRepoJdbc;

    @Test
    public void should_updateReference_WhenRemoveAUser_fromAGroup() {
        EntityNode requesterNode = getRequesterNode();
        EntityNode memberNode = getMemberNode("member");
        EntityNode groupNode = getDataViewersGroupNode("x");

        GroupInfoEntity savedGroup = groupRepository.save(GroupInfoEntity.fromEntityNode(groupNode));

        addMemberRepoJdbc.addMember(groupNode, AddMemberRepoDto.builder()
                .memberNode(memberNode)
                .partitionId(memberNode.getDataPartitionId())
                .role(Role.MEMBER)
                .build());

        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .requesterId(requesterNode.getNodeId())
                .partitionId(DATA_PARTITION_ID)
                .childrenReference(ChildrenReference.createChildrenReference(memberNode, Role.MEMBER))
                .build();

        //when
        sut.removeMember(groupNode, memberNode, removeMemberServiceDto);

        //then
        assertTrue(memberRepository.findMembersByGroup(savedGroup.getId()).isEmpty());
        assertTrue(memberRepository.findByEmail(memberNode.getNodeId()).isEmpty());

        verify(auditLogger).removeMember(AuditStatus.SUCCESS, groupNode.getNodeId(), memberNode.getNodeId(), requesterNode.getNodeId());
    }

    @Test
    public void should_updateReferences_whenRemovingAGroupFromAnotherGroup() {
        EntityNode requesterNode = getRequesterNode();
        EntityNode groupNode = getUsersGroupNode("x");
        EntityNode groupToRemoveNode = getUsersGroupNode("y");

        GroupInfoEntity savedGroup = groupRepository.save(GroupInfoEntity.fromEntityNode(groupNode));
        GroupInfoEntity savedGroupToRemove = groupRepository.save(GroupInfoEntity.fromEntityNode(groupToRemoveNode));

        groupRepository.addChildGroupById(savedGroup.getId(), savedGroupToRemove.getId());

        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .requesterId(requesterNode.getNodeId())
                .partitionId(DATA_PARTITION_ID)
                .childrenReference(ChildrenReference.createChildrenReference(groupToRemoveNode, Role.MEMBER))
                .build();

        //when
        sut.removeMember(groupNode, groupToRemoveNode, removeMemberServiceDto);

        //then
        assertTrue(groupRepository.findChildByEmail(savedGroup.getId(), groupToRemoveNode.getNodeId()).isEmpty());
        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedGroupToRemove.getId())).isEmpty());

        verify(auditLogger).removeMember(AuditStatus.SUCCESS, groupNode.getNodeId(), groupToRemoveNode.getNodeId(), requesterNode.getNodeId());
    }
}
