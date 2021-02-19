package org.opengroup.osdu.entitlements.v2.api;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.GroupDto;
import org.opengroup.osdu.entitlements.v2.service.CreateGroupService;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class CreateGroupApi {

    private final CreateGroupService createGroupService;
    private final RequestInfoUtilService requestInfoUtilService;
    private final RequestInfo requestInfo;

    @PostMapping("/groups")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "', '" + AppProperties.ADMIN + "')")
    public ResponseEntity<GroupDto> createGroup(@Valid @RequestBody CreateGroupDto groupInfoDto) {
        String dataPartitionId = requestInfo.getHeaders().getPartitionId();
        String partitionDomain = requestInfoUtilService.getDomain(dataPartitionId);
        EntityNode inputGroupNode = CreateGroupDto.createGroupNode(groupInfoDto, partitionDomain, dataPartitionId);
        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                .requesterId(requestInfoUtilService.getUserId(requestInfo.getHeaders()))
                .partitionDomain(partitionDomain)
                .partitionId(dataPartitionId).build();
        EntityNode outputGroupNode = createGroupService.run(inputGroupNode, createGroupServiceDto);
        GroupDto output = GroupDto.createFromEntityNode(outputGroupNode);
        return new ResponseEntity<>(output, HttpStatus.CREATED);
    }
}
