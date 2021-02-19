package org.opengroup.osdu.entitlements.v2.api;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupOperation;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.service.UpdateGroupService;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.opengroup.osdu.entitlements.v2.validation.ApiInputValidation;
import org.opengroup.osdu.entitlements.v2.validation.PartitionHeaderValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
public class UpdateGroupApi {

    private final RequestInfo requestInfo;
    private final UpdateGroupService updateGroupService;
    private final PartitionHeaderValidationService partitionHeaderValidationService;
    private final RequestInfoUtilService requestInfoUtilService;

    @PatchMapping("/groups/{group_email}")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "','" + AppProperties.ADMIN + "','" + AppProperties.USERS + "')")
    public ResponseEntity<UpdateGroupResponseDto> updateGroup(@Valid @PathVariable("group_email") String existingGroupEmail,
                                                              @Valid @RequestBody List<UpdateGroupOperation> updateGroupRequest) {
        performRequestBodyValidation(updateGroupRequest);

        String partitionId = requestInfo.getHeaders().getPartitionId();
        String partitionDomain = requestInfoUtilService.getDomain(partitionId);
        performValidation(existingGroupEmail, partitionId, partitionDomain);

        UpdateGroupOperation renameOperation = getRenameOperation(updateGroupRequest);
        UpdateGroupOperation appIdsOperation = getAppIdsUpdateOperation(updateGroupRequest);

        UpdateGroupServiceDto updateGroupServiceDto = UpdateGroupServiceDto.builder()
                .existingGroupEmail(existingGroupEmail.toLowerCase())
                .partitionId(partitionId)
                .partitionDomain(partitionDomain)
                .requesterId(requestInfoUtilService.getUserId(requestInfo.getHeaders()))
                .renameOperation(renameOperation)
                .appIdsOperation(appIdsOperation)
                .build();

        UpdateGroupResponseDto result = updateGroupService.updateGroup(updateGroupServiceDto);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private void performValidation(String groupEmail, String partitionId, String partitionDomain) {
        partitionHeaderValidationService.validateSinglePartitionProvided(partitionId);
        ApiInputValidation.validateEmailAndBelongsToPartition(groupEmail, partitionDomain);
    }

    private void performRequestBodyValidation(List<UpdateGroupOperation> requestBody) {
        if (requestBody.size() > 2) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Invalid number of operation provided, only rename and appIds update are allowed.");
        }
        for (UpdateGroupOperation operation : requestBody) {
            if (operation.getPath().equalsIgnoreCase("/name") && operation.getValue().size() > 1) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Invalid number of group name provided.");
            }
        }
    }

    private UpdateGroupOperation getRenameOperation(List<UpdateGroupOperation> request) {
        for (UpdateGroupOperation op : request) {
            if (op.getPath().equalsIgnoreCase("/name")) {
                return op;
            }
        }
        return null;
    }

    private UpdateGroupOperation getAppIdsUpdateOperation(List<UpdateGroupOperation> request) {
        for (UpdateGroupOperation op : request) {
            if (op.getPath().equalsIgnoreCase("/appIds")) {
                return op;
            }
        }
        return null;
    }
}
