package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class VertexPropertyNames {
    public static final String NODE_ID = "nodeId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String DATA_PARTITION_ID = "dataPartitionId";
    public static final String APP_IDS = "appIds";
}
