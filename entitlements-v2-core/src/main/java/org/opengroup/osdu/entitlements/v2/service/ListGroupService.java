package org.opengroup.osdu.entitlements.v2.service;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListGroupService {
    private final JaxRsDpsLog log;
    private final AuditLogger auditLogger;
    private final RequestInfo requestInfo;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final IGroupCacheService groupCacheService;

    public Set<ParentReference> getGroups(ListGroupServiceDto listGroupServiceDto) {
        log.info(String.format("ListGroupService#run timestamp: %d", System.currentTimeMillis()));
        String requesterId = listGroupServiceDto.getRequesterId();
        log.info(String.format("requested by %s", requesterId));
        Set<ParentReference> groups = new HashSet<>();
        listGroupServiceDto.getPartitionIds().forEach(partitionId ->
                groups.addAll(groupCacheService.getFromPartitionCache(requesterId, partitionId)));
        log.info(String.format("ListGroupService#run cache look up done timestamp: %d", System.currentTimeMillis()));
        try {
            String serviceAccount = requestInfo.getTenantInfo().getServiceAccount();
            // TODO: Uncomment when AppId filter is optimized. The current logic is RU expensive,
            //  so we temporarily disable for now. US https://dev.azure.com/slb-swt/data-at-rest/_workitems/edit/599488
//            if (serviceAccount.equalsIgnoreCase(requesterId) || Strings.isNullOrEmpty(listGroupServiceDto.getAppId())) {
                auditLogger.listGroup(AuditStatus.SUCCESS, fetchParentIds(groups));
                return groups;
//            } else {
//                return filterGroupsByAppId(groups, listGroupServiceDto);
//            }
        } catch (Exception e) {
            auditLogger.listGroup(AuditStatus.FAILURE, new ArrayList<>());
            throw e;
        }
    }

    private Set<ParentReference> filterGroupsByAppId(Set<ParentReference> groups,
                                                     ListGroupServiceDto listGroupServiceDto) {
        String appId = listGroupServiceDto.getAppId();
        log.info(String.format("Filtering groups based on caller's appId: %s", appId));
        Set<ParentReference> accessibleGroups = new HashSet<>();
        listGroupServiceDto.getPartitionIds().forEach(partitionId ->
                accessibleGroups.addAll(retrieveGroupRepo.filterParentsByAppID(groups, partitionId, appId)));
        auditLogger.listGroup(AuditStatus.SUCCESS, fetchParentIds(accessibleGroups));
        log.info(String.format(
                "ListGroupService#run cache app id filter done timestamp: %d", System.currentTimeMillis()));
        return accessibleGroups;
    }

    private List<String> fetchParentIds(Set<ParentReference> groups) {
        return groups.stream()
                .map(ParentReference::getId)
                .collect(Collectors.toList());
    }
}
