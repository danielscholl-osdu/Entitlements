package org.opengroup.osdu.entitlements.v2.azure.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

@Data
@Builder
@Generated
@NoArgsConstructor
@AllArgsConstructor
public class RemoveEdgeDto {
    private String fromNodeId;
    private String fromDataPartitionId;
    private String toNodeId;
    private String toDataPartitionId;
    private String edgeLabel;
}
