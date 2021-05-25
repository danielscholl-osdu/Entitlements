package org.opengroup.osdu.entitlements.v2.gcp.service;

import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequestScope
@Component
public class VmGroupCache {
    private Map<String, Set<ParentReference>> groupMap = new HashMap<>();

    public Set<ParentReference> getGroupCache(String requesterId) {
        return this.groupMap.get(requesterId);
    }

    public void addGroupCache(String requesterId, Set<ParentReference> parents) {
        this.groupMap.put(requesterId, parents);
    }

    public void deleteGroupCache(String requesterId) {
        this.groupMap.remove(requesterId);
    }
}
