package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.renamegroup;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.DATA_PARTITION_ID;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getDataGroupNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getDataRootGroupNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getMemberNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getUsersGroupNode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.addmember.AddMemberRepoJdbc;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
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
public class RenameGroupRepoJdbcTest {

    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private JaxRsDpsLog logger;

    @Autowired
    private RenameGroupRepoJdbc sut;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private AddMemberRepoJdbc addMemberRepoJdbc;

    @Test
    public void shouldRenameGroupSuccessfully() {
        final String newGroupName = "users.y";
        final String newGroupId = "users.y@dp.domain.com";

        EntityNode initialGroupNode = getUsersGroupNode("x");

        EntityNode parent1Node = getDataGroupNode("x");
        EntityNode parent2Node = getDataGroupNode("y");
        EntityNode parent3Node = getDataGroupNode("z");

        EntityNode childOwnerNode = getMemberNode("owner");
        EntityNode childMemberNode = getMemberNode("member");
        EntityNode childGroupNode = getDataGroupNode("w");

        EntityNode dataRootGroupNode = getDataRootGroupNode();

        GroupInfoEntity savedDataRootGroup = groupRepository.save(GroupInfoEntity.fromEntityNode(dataRootGroupNode));
        GroupInfoEntity savedInitialGroup = groupRepository.save(GroupInfoEntity.fromEntityNode(initialGroupNode));

        GroupInfoEntity savedParent1 = groupRepository.save(GroupInfoEntity.fromEntityNode(parent1Node));
        GroupInfoEntity savedParent2 = groupRepository.save(GroupInfoEntity.fromEntityNode(parent2Node));
        GroupInfoEntity savedParent3 = groupRepository.save(GroupInfoEntity.fromEntityNode(parent3Node));

        GroupInfoEntity savedChildGroup = groupRepository.save(GroupInfoEntity.fromEntityNode(childGroupNode));

        addMemberRepoJdbc.addMember(initialGroupNode, AddMemberRepoDto.builder()
                .memberNode(childMemberNode)
                .role(Role.MEMBER)
                .partitionId(DATA_PARTITION_ID)
                .build());

        addMemberRepoJdbc.addMember(initialGroupNode, AddMemberRepoDto.builder()
                .memberNode(childOwnerNode)
                .role(Role.OWNER)
                .partitionId(DATA_PARTITION_ID)
                .build());

        groupRepository.addChildGroupById(savedInitialGroup.getId(), savedChildGroup.getId());
        groupRepository.addChildGroupById(savedParent1.getId(), savedInitialGroup.getId());
        groupRepository.addChildGroupById(savedParent2.getId(), savedInitialGroup.getId());
        groupRepository.addChildGroupById(savedParent3.getId(), savedInitialGroup.getId());

        //when
        sut.run(initialGroupNode, newGroupName);

        //then

        List<GroupInfoEntity> actualGroupsByNewEmail = groupRepository.findByEmail(newGroupId);

        assertEquals(1, actualGroupsByNewEmail.size());

        GroupInfoEntity actualGroup = actualGroupsByNewEmail.get(0);

        assertEquals(newGroupId, actualGroup.getEmail());
        assertEquals(newGroupName, actualGroup.getName());

        List<String> childrenGroupEmails = groupRepository.findDirectChildren(
                Collections.singletonList(actualGroup.getId())).stream()
                    .map(GroupInfoEntity::getEmail)
                    .collect(Collectors.toList());

        assertEquals(1, childrenGroupEmails.size());
        assertTrue(childrenGroupEmails.contains(childGroupNode.getNodeId()));

        List<MemberInfoEntity> childrenUsers = memberRepository.findMembersByGroup(actualGroup.getId());

        assertEquals(2, childrenUsers.size());

        Optional<MemberInfoEntity> actualMember = childrenUsers.stream()
                .filter(m -> Role.MEMBER.getValue().equals(m.getRole()))
                .findFirst();

        Optional<MemberInfoEntity> actualOwner = childrenUsers.stream()
                .filter(m -> Role.OWNER.getValue().equals(m.getRole()))
                .findFirst();

        assertTrue(actualMember.isPresent());
        assertEquals(childMemberNode.getNodeId(), actualMember.get().getEmail());

        assertTrue(actualOwner.isPresent());
        assertEquals(childOwnerNode.getNodeId(), actualOwner.get().getEmail());

        List<String> actualParentNames = groupRepository
                .findDirectParents(Collections.singletonList(actualGroup.getId()))
                .stream()
                .map(GroupInfoEntity::getEmail)
                .collect(Collectors.toList());

        assertEquals(3, actualParentNames.size());

        assertTrue(actualParentNames.contains(parent1Node.getNodeId()));
        assertTrue(actualParentNames.contains(parent2Node.getNodeId()));
        assertTrue(actualParentNames.contains(parent3Node.getNodeId()));

        verify(auditLogger).updateGroup(AuditStatus.SUCCESS, initialGroupNode.getNodeId());
    }
}
