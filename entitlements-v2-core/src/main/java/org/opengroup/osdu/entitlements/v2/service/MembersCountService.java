package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.memberscount.MembersCountRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MembersCountService {
    private static final String NOT_AUTHORIZED_MESSAGE = "Not authorized to manage members";
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final MembersCountRepo membersCountRepo;
    private final JaxRsDpsLog log;
    private final PermissionService permissionService;
    private final AuthorizationService authorizationService;
    private final RequestInfo requestInfo;

    public MembersCountResponseDto getMembersCount(MembersCountServiceDto membersCountServiceDto) {
        log.debug(String.format("requested by %s", membersCountServiceDto.getRequesterId()));
        EntityNode groupNode = retrieveGroupRepo.groupExistenceValidation(membersCountServiceDto.getGroupId(), membersCountServiceDto.getPartitionId());
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester(membersCountServiceDto.getRequesterId(), membersCountServiceDto.getPartitionId());
        if (!permissionService.hasOwnerPermissionOf(requesterNode, groupNode) && !isCallerHasAdminPermissions()) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), NOT_AUTHORIZED_MESSAGE);
        }
        return membersCountRepo.getMembersCount(membersCountServiceDto);
    }

    private boolean isCallerHasAdminPermissions() {
        try {
            return authorizationService.isCurrentUserAuthorized(requestInfo.getHeaders(), AppProperties.ADMIN);
        } catch (AppException e) {
            log.warning("Caller does not have ADMIN permissions", e);
            return false;
        }
    }
}
