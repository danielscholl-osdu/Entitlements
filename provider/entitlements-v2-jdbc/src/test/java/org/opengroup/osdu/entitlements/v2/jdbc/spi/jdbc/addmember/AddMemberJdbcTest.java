package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.addmember;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.DATA_PARTITION_ID;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getMemberNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getDataViewersGroupNode;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
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
@SpringBootTest
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@RunWith(SpringRunner.class)
public class AddMemberJdbcTest{

	@MockBean
	protected AuditLogger auditLogger;
	@MockBean
	protected RequestInfo requestInfo;
	@MockBean
	protected JaxRsDpsLog logger;
	@Autowired
	private AddMemberRepoJdbc sut;

	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private GroupRepository groupRepository;

	@Test
	public void should_createAndSetMemberReference_whenInsertAUser_andAddedMemberNodeDoesNotExist() {
		EntityNode memberNode = getMemberNode("member");
		EntityNode groupNode = getDataViewersGroupNode("x");
		AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder()
				.memberNode(memberNode)
				.role(Role.MEMBER)
				.existingParents(new HashSet<>())
				.partitionId(DATA_PARTITION_ID)
				.build();

		GroupInfoEntity group = groupRepository.save(GroupInfoEntity.fromEntityNode(groupNode));

		//when
		sut.addMember(groupNode, addMemberRepoDto);

		//then
		List<MemberInfoEntity> expected = Collections.singletonList(MemberInfoEntity.fromEntityNode(memberNode, addMemberRepoDto.getRole()));
		List<MemberInfoEntity> actual = memberRepository.findMemberByEmailInGroup(group.getId(), addMemberRepoDto.getMemberNode().getNodeId());

		assertEquals(1, actual.size());

		MemberInfoEntity actualMember = actual.get(0);

		assertEquals(memberNode.getNodeId(), actualMember.getEmail());
		assertEquals(addMemberRepoDto.getRole().getValue(), actualMember.getRole());

		verify(auditLogger).addMember(AuditStatus.SUCCESS, groupNode.getNodeId(), memberNode.getNodeId(), addMemberRepoDto.getRole());
	}

	@Test
	public void should_updateReferences_whenInsertAUser_andAddedMemberExist() {
		EntityNode memberNode = EntityNode.builder()
				.nodeId("member@xxx.com")
				.name("member@xxx.com")
				.type(NodeType.USER)
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
		EntityNode firstGroupNode = EntityNode.builder()
				.nodeId("data.x.viewers@dp.domain.com")
				.name("data.x.viewers")
				.type(NodeType.GROUP)
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
		EntityNode secondGroupNode = EntityNode.builder()
				.nodeId("data.y.viewers@dp.domain.com")
				.name("data.y.viewers")
				.type(NodeType.GROUP)
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
		AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder()
				.memberNode(memberNode)
				.role(Role.OWNER)
				.existingParents(new HashSet<>())
				.partitionId(DATA_PARTITION_ID)
				.build();

		GroupInfoEntity firstGroup = groupRepository.save(GroupInfoEntity.fromEntityNode(firstGroupNode));
		GroupInfoEntity secondGroup = groupRepository.save(GroupInfoEntity.fromEntityNode(secondGroupNode));

		sut.addMember(firstGroupNode, addMemberRepoDto);

		//when
		sut.addMember(secondGroupNode, addMemberRepoDto);

		//then
		List<MemberInfoEntity> expected = Collections.singletonList(MemberInfoEntity.fromEntityNode(memberNode, addMemberRepoDto
				.getRole()));
		List<MemberInfoEntity> actual = memberRepository.findMemberByEmailInGroup(secondGroup.getId(), addMemberRepoDto
				.getMemberNode().getNodeId());

		assertEquals(expected.size(), actual.size());

		MemberInfoEntity actualMember = actual.get(0);

		assertEquals(memberNode.getNodeId(), actualMember.getEmail());
		assertEquals(addMemberRepoDto.getRole().getValue(), actualMember.getRole());

		verify(auditLogger).addMember(AuditStatus.SUCCESS, secondGroupNode.getNodeId(), memberNode.getNodeId(), addMemberRepoDto
				.getRole());
	}

	@Test
	public void should_updateReferences_whenInsertAGroupToAnotherGroup() {
		EntityNode firstGroupNode = EntityNode.builder()
				.nodeId("data.x.viewers@dp.domain.com")
				.name("data.x.viewers")
				.type(NodeType.GROUP)
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
		EntityNode secondGroupNode = EntityNode.builder()
				.nodeId("data.y.viewers@dp.domain.com")
				.name("data.y.viewers")
				.type(NodeType.GROUP)
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
		AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder()
				.memberNode(secondGroupNode)
				.role(Role.OWNER)
				.existingParents(new HashSet<>())
				.partitionId(DATA_PARTITION_ID)
				.build();

		GroupInfoEntity firstGroup = groupRepository.save(GroupInfoEntity.fromEntityNode(firstGroupNode));
		GroupInfoEntity secondGroup = groupRepository.save(GroupInfoEntity.fromEntityNode(secondGroupNode));

		//when
		sut.addMember(firstGroupNode, addMemberRepoDto);

		//then
		List<GroupInfoEntity> expected = Collections.singletonList(GroupInfoEntity.fromEntityNode(secondGroupNode));
		List<GroupInfoEntity> actual = groupRepository.findChildByEmail(firstGroup.getId(), addMemberRepoDto.getMemberNode().getNodeId());

		assertEquals(expected.size(), actual.size());

		GroupInfoEntity actualMember = actual.get(0);

		assertEquals(secondGroupNode.getNodeId(), actualMember.getEmail());

		verify(auditLogger).addMember(AuditStatus.SUCCESS, firstGroupNode.getNodeId(), secondGroupNode.getNodeId(), addMemberRepoDto.getRole());
	}
}
