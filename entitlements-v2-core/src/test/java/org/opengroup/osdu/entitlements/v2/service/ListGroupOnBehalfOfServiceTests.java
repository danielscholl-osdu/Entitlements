package org.opengroup.osdu.entitlements.v2.service;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupOnBehalfOfServiceDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ListGroupOnBehalfOfServiceTests {

    @MockBean
    private JaxRsDpsLog logger;

    @MockBean
    private ListGroupService listGroupService;

    @Autowired
    ListGroupOnBehalfOfService service;

    @Test
    public void should_getAllGroupsWhenGivenMemberEmailExists() {
        ListGroupOnBehalfOfServiceDto listGroupOnBehalfOfServiceDto = ListGroupOnBehalfOfServiceDto.builder()
                .memberId("user@xxx.com")
                .groupType(GroupType.NONE)
                .partitionId("dp")
                .build();

        List<String> partitionIds = new ArrayList<>();
        partitionIds.add("dp");
        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto.builder()
                .requesterId("user@xxx.com").partitionIds(partitionIds).build();

        when(listGroupService.getGroups(listGroupServiceDto)).thenReturn(new HashSet<>(Arrays.asList(
                ParentReference.builder().id("g1@dp.domain.com").name("g1").dataPartitionId("dp").build(),
                ParentReference.builder().id("g2@dp.domain.com").name("g2").dataPartitionId("dp").build(),
                ParentReference.builder().id("g3@dp.domain.com").name("g3").dataPartitionId("dp").build(),
                ParentReference.builder().id("g4@dp.domain.com").name("g4").dataPartitionId("dp").build())
        ));

        ListGroupResponseDto retGroups = service.getGroupsOnBehalfOfMember(listGroupOnBehalfOfServiceDto);
        assertEquals(4, retGroups.getGroups().size());
    }

    @Test
    public void should_getEmptyGroupListWhenNoGroupsExistForMember() {
        ListGroupOnBehalfOfServiceDto listGroupOnBehalfOfServiceDto = ListGroupOnBehalfOfServiceDto.builder()
                .memberId("user@xxx.com")
                .groupType(GroupType.NONE)
                .partitionId("dp")
                .build();

        List<String> partitionIds = new ArrayList<>();
        partitionIds.add("dp");
        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto.builder()
                .requesterId("user@xxx.com").partitionIds(partitionIds).build();

        when(listGroupService.getGroups(listGroupServiceDto)).thenReturn(new HashSet<>());

        ListGroupResponseDto retGroups = service.getGroupsOnBehalfOfMember(listGroupOnBehalfOfServiceDto);
        assertEquals(0, retGroups.getGroups().size());
    }

    @Test
    public void shouldReturnOnlyDataGroupsWhenGivenGroupTypeIsData(){
        ListGroupOnBehalfOfServiceDto listGroupOnBehalfOfServiceDto = ListGroupOnBehalfOfServiceDto.builder()
                .memberId("user@xxx.com")
                .groupType(GroupType.DATA)
                .partitionId("dp")
                .build();

        Set<ParentReference> output = new HashSet<>();
        output.add(ParentReference.builder().name("viewers").id("viewers@dp.domain.com")
                .description("").dataPartitionId("dp").build());
        output.add(ParentReference.builder().name("data.x").id("data.x@dp.domain.com")
                .description("a data group").dataPartitionId("dp").build());
        output.add(ParentReference.builder().name("users.x").id("users.x@dp.domain.com")
                .description("a user group").dataPartitionId("dp").build());
        output.add(ParentReference.builder().name("service.x").id("service.x@dp.domain.com")
                .description("a service group").dataPartitionId("dp").build());
        List<String> partitionIds = new ArrayList<>();
        partitionIds.add("dp");

        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto.builder()
                .requesterId("user@xxx.com").partitionIds(partitionIds).build();
        when(listGroupService.getGroups(listGroupServiceDto)).thenReturn(output);

        ListGroupResponseDto retGroups = service.getGroupsOnBehalfOfMember(listGroupOnBehalfOfServiceDto);
        assertEquals(1, retGroups.getGroups().size());
    }

    @Test
    public void shouldReturnOnlyServiceGroupsWhenGivenGroupTypeIsService(){
        ListGroupOnBehalfOfServiceDto listGroupOnBehalfOfServiceDto = ListGroupOnBehalfOfServiceDto.builder()
                .memberId("user@xxx.com")
                .groupType(GroupType.SERVICE)
                .partitionId("dp")
                .build();

        Set<ParentReference> output = new HashSet<>();
        output.add(ParentReference.builder().name("viewers").id("viewers@dp.domain.com")
                .description("").dataPartitionId("dp").build());
        output.add(ParentReference.builder().name("service.x").id("service.x@dp.domain.com")
                .description("a service group").dataPartitionId("dp").build());
        output.add(ParentReference.builder().name("service.y").id("service.y@dp.domain.com")
                .description("a service group").dataPartitionId("dp").build());
        output.add(ParentReference.builder().name("service.z").id("service.z@dp.domain.com")
                .description("a service group").dataPartitionId("dp").build());
        List<String> partitionIds = new ArrayList<>();
        partitionIds.add("dp");

        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto.builder()
                .requesterId("user@xxx.com").partitionIds(partitionIds).build();
        when(listGroupService.getGroups(listGroupServiceDto)).thenReturn(output);

        ListGroupResponseDto retGroups = service.getGroupsOnBehalfOfMember(listGroupOnBehalfOfServiceDto);
        assertEquals(3, retGroups.getGroups().size());
    }

    @Test
    public void shouldReturnOnlyUserGroupsWhenGivenGroupTypeIsUser(){
        ListGroupOnBehalfOfServiceDto listGroupOnBehalfOfServiceDto = ListGroupOnBehalfOfServiceDto.builder()
                .memberId("user@xxx.com")
                .groupType(GroupType.USER)
                .partitionId("dp")
                .build();

        Set<ParentReference> output = new HashSet<>();
        output.add(ParentReference.builder().name("viewers").id("viewers@dp.domain.com")
                .description("").dataPartitionId("dp").build());
        output.add(ParentReference.builder().name("data.x").id("data.x@dp.domain.com")
                .description("a data group").dataPartitionId("dp").build());
        output.add(ParentReference.builder().name("users.x").id("users.x@dp.domain.com")
                .description("a user group").dataPartitionId("dp").build());
        output.add(ParentReference.builder().name("users.y").id("users.y@dp.domain.com")
                .description("a user group").dataPartitionId("dp").build());
        List<String> partitionIds = new ArrayList<>();
        partitionIds.add("dp");

        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto.builder()
                .requesterId("user@xxx.com").partitionIds(partitionIds).build();
        when(listGroupService.getGroups(listGroupServiceDto)).thenReturn(output);

        ListGroupResponseDto retGroups = service.getGroupsOnBehalfOfMember(listGroupOnBehalfOfServiceDto);
        assertEquals(2, retGroups.getGroups().size());
    }

    }
