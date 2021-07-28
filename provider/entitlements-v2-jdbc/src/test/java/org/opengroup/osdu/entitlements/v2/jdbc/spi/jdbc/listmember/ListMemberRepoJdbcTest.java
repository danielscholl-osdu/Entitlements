package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.listmember;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.DATA_PARTITION_ID;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getCommonGroup;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getDataViewersGroupNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getMemberNode;

import java.util.List;
import java.util.stream.Collectors;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
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
public class ListMemberRepoJdbcTest {
    @MockBean
    protected AuditLogger auditLogger;
    @MockBean
    protected RequestInfo requestInfo;
    @MockBean
    protected JaxRsDpsLog logger;

    @Autowired
    private ListMemberRepoJdbc sut;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private JdbcTemplateRunner jdbcTemplateRunner;

    @Test
    public void should_returnDirectMemberNodes() throws Exception {
        EntityNode groupToAccess = getDataViewersGroupNode("x");

        EntityNode member = getMemberNode("member");
        EntityNode childGroup1 = getCommonGroup("g1");
        EntityNode childGroup2 = getCommonGroup("g2");
        EntityNode childGroup3 = getCommonGroup("g3");

        GroupInfoEntity savedGroupToAccess = groupRepository.save(GroupInfoEntity.fromEntityNode(groupToAccess));
        GroupInfoEntity savedChild1 = groupRepository.save(GroupInfoEntity.fromEntityNode(childGroup1));
        GroupInfoEntity savedChild2 = groupRepository.save(GroupInfoEntity.fromEntityNode(childGroup2));
        GroupInfoEntity savedChild3 = groupRepository.save(GroupInfoEntity.fromEntityNode(childGroup3));

        Long savedMemberId = jdbcTemplateRunner.saveMemberInfoEntity(
                MemberInfoEntity.fromEntityNode(member, Role.OWNER)
        );

        groupRepository.addMemberById(savedGroupToAccess.getId(), savedMemberId, Role.OWNER.getValue());

        groupRepository.addChildGroupById(savedGroupToAccess.getId(), savedChild1.getId());
        groupRepository.addChildGroupById(savedGroupToAccess.getId(), savedChild3.getId());

        groupRepository.addChildGroupById(savedChild1.getId(), savedChild2.getId());

        ListMemberServiceDto listMemberServiceDto = ListMemberServiceDto.builder()
                .groupId(groupToAccess.getNodeId())
                .partitionId(DATA_PARTITION_ID).build();


        //when
        List<ChildrenReference> members = sut.run(listMemberServiceDto);

        //then
        assertEquals(3, members.size());

        List<String> memberIds = members.stream()
                .map(ChildrenReference::getId)
                .collect(Collectors.toList());

        assertTrue(memberIds.contains(member.getNodeId()));
        assertTrue(memberIds.contains(childGroup1.getNodeId()));
        assertTrue(memberIds.contains(childGroup3.getNodeId()));

        assertFalse(memberIds.contains(childGroup2.getNodeId()));

        verify(auditLogger).listMember(AuditStatus.SUCCESS, listMemberServiceDto.getGroupId());
    }

}
