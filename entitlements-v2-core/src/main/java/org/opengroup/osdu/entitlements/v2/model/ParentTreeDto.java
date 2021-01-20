package org.opengroup.osdu.entitlements.v2.model;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;

import java.util.Set;

@Data
@Builder
@Generated
public class ParentTreeDto {
    private Set<ParentReference> parentReferences;
    private int maxDepth;
}
