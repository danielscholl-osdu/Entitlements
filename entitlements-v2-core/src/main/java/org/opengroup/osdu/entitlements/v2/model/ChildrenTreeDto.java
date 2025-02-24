package org.opengroup.osdu.entitlements.v2.model;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;

import java.util.List;

@Data
@Builder
@Generated
public class ChildrenTreeDto {
    private List<String> childrenUserIds;
    private int maxDepth;
}
