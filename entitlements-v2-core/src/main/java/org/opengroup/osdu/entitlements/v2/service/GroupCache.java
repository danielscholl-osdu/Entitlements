package org.opengroup.osdu.entitlements.v2.service;

import org.opengroup.osdu.entitlements.v2.model.ParentReference;

import java.util.Set;

public interface GroupCache {
    Set<ParentReference> getGroupCache(String requesterId);
    void addGroupCache(String requesterId, Set<ParentReference> parents);
}
