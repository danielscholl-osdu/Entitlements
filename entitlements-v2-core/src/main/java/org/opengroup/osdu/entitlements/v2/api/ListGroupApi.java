package org.opengroup.osdu.entitlements.v2.api;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.service.ListGroupService;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.opengroup.osdu.entitlements.v2.validation.PartitionHeaderValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ListGroupApi {

    private final JaxRsDpsLog log;
    private final RequestInfo requestInfo;
    private final ListGroupService listGroupService;
    private final RequestInfoUtilService requestInfoUtilService;
    private final PartitionHeaderValidationService partitionHeaderValidationService;

    @GetMapping("/groups")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "', '" + AppProperties.ADMIN + "', '" + AppProperties.USERS + "')")
    public ResponseEntity<ListGroupResponseDto> listGroups() {
        DpsHeaders dpsHeaders = requestInfo.getHeaders();
        List<String> partitionIdList = requestInfoUtilService.getPartitionIdList(dpsHeaders);
        partitionHeaderValidationService.validateIfSpecialListGroupPartitionIsProvided(partitionIdList);
        String userId = requestInfoUtilService.getUserId(dpsHeaders);
        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto.builder()
                .requesterId(userId)
                .appId(requestInfoUtilService.getAppId(dpsHeaders))
                .partitionIds(partitionIdList)
                .build();
        ListGroupResponseDto body = ListGroupResponseDto.builder()
                .groups(new ArrayList<>(listGroupService.getGroups(listGroupServiceDto)))
                .desId(userId)
                .memberEmail(userId)
                .build();
        log.debug(String.format("ListGroupResponseDto#create done timestamp: %d", System.currentTimeMillis()));
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

}
