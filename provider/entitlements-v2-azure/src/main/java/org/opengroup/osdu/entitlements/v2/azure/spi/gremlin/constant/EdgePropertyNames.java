package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class EdgePropertyNames {
    public static final String ROLE = "role";
    public static final String CHILD_EDGE_LB = "child";
    public static final String PARENT_EDGE_LB = "parent";
}
