package org.opengroup.osdu.entitlements.v2.api;

import io.swagger.annotations.ApiParam;
import javax.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupOnBehalfOfServiceDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupsOfPartitionDto;
import org.opengroup.osdu.entitlements.v2.service.ListGroupOnBehalfOfService;
import org.opengroup.osdu.entitlements.v2.validation.PartitionHeaderValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class ListGroupOnBehalfOfApi {

    private static final String INVALID_FILTER_ERROR_MESSAGE = "Invalid filter";
    private final RequestInfo requestInfo;
    private final ListGroupOnBehalfOfService listGroupOnBehalfOfService;
    private final PartitionHeaderValidationService partitionHeaderValidationService;

    @GetMapping("/members/{member_email}/groups")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "', '" + AppProperties.ADMIN + "')")
    public ResponseEntity<ListGroupResponseDto> listGroupsOnBehalfOf(@PathVariable("member_email") String memberId,
        @ApiParam(name = "type", allowableValues = "NONE,DATA,USER,SERVICE", defaultValue = "NONE")
        @RequestParam(name = "type") String type,
        @RequestParam(name = "appid", required = false) String appId) {

        memberId = memberId.toLowerCase();
        String partitionId = requestInfo.getHeaders().getPartitionId();
        GroupType groupType = getTypeParameterCaseInsensitive(type);
        partitionHeaderValidationService.validateSinglePartitionProvided(partitionId);
        ListGroupOnBehalfOfServiceDto listGroupOnBehalfOfServiceDto = ListGroupOnBehalfOfServiceDto.builder()
                .memberId(memberId)
                .groupType(groupType)
                .partitionId(partitionId)
                .appId(appId)
                .build();

        ListGroupResponseDto responseDto = listGroupOnBehalfOfService.getGroupsOnBehalfOfMember(listGroupOnBehalfOfServiceDto);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @GetMapping("/groups/all")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "', '" + AppProperties.ADMIN + "')")
    public ResponseEntity<ListGroupsOfPartitionDto> listAllPartitionGroups(
        @ApiParam(name = "type", allowableValues = "NONE,DATA,USER,SERVICE", defaultValue = "NONE")
        @RequestParam(name = "type") String type,
        @RequestParam(name = "cursor", required = false) String cursor,
        @ApiParam(name = "limit", defaultValue = "100")
        @RequestParam(name = "limit", required = false, defaultValue = "100") @Min(1) Integer limit
    ) {
        String partitionId = requestInfo.getHeaders().getPartitionId();
        ListGroupsOfPartitionDto groupsInPartition = listGroupOnBehalfOfService.getGroupsInPartition(partitionId, getTypeParameterCaseInsensitive(type), cursor, limit);
        return new ResponseEntity<>(groupsInPartition, HttpStatus.OK);
    }

    private GroupType getTypeParameterCaseInsensitive(String type) {
        if (type == null || type.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), INVALID_FILTER_ERROR_MESSAGE);
        } else {
            try {
                return GroupType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), INVALID_FILTER_ERROR_MESSAGE);
            }
        }
    }
}

