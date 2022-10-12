package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.listmember;

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
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.*;

@SpringBootTest(classes = SpiJdbcTestConfig.class)
@RunWith(SpringRunner.class)
public class ListMemberRepoJdbcTest {
    @MockBean
    protected AuditLogger auditLogger;

    @Autowired
    private ListMemberRepoJdbc sut;

    @MockBean
    private GroupRepository groupRepository;
    @MockBean
    private MemberRepository memberRepository;
    @MockBean
    private JdbcTemplateRunner jdbcTemplateRunner;

    @Test
    public void should_returnDirectMemberNodes() throws Exception {
        EntityNode groupToAccess = getDataViewersGroupNode("x");

        EntityNode member = getMemberNode("member");
        EntityNode childGroup1 = getCommonGroup("g1");
        EntityNode childGroup2 = getCommonGroup("g2");
        EntityNode childGroup3 = getCommonGroup("g3");

        GroupInfoEntity savedGroupToAccess = GroupInfoEntity.fromEntityNode(groupToAccess);
        GroupInfoEntity savedChild1 = GroupInfoEntity.fromEntityNode(childGroup1);
        GroupInfoEntity savedChild2 = GroupInfoEntity.fromEntityNode(childGroup2);
        GroupInfoEntity savedChild3 = GroupInfoEntity.fromEntityNode(childGroup3);

        ListMemberServiceDto listMemberServiceDto = ListMemberServiceDto.builder()
                .groupId(groupToAccess.getNodeId())
                .partitionId(DATA_PARTITION_ID).build();

        when(groupRepository.findDirectChildren(anyList())).thenReturn(Arrays.asList(savedChild1, savedChild3));
        when(groupRepository.findByEmail(any())).thenReturn(Collections.singletonList(savedGroupToAccess));
        when(memberRepository.findMembersByGroup(any())).thenReturn(Collections.singletonList(MemberInfoEntity.fromEntityNode(member, Role.OWNER)));


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
