package org.opengroup.osdu.entitlements.v2.azure.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@Generated
@NoArgsConstructor
@AllArgsConstructor
public class AddEdgeDto {
    private String fromNodeId;
    private String toNodeId;
    /**
     * data partition id of a node, from where the edge goes
     */
    private String dpOfFromNodeId;
    /**
     * data partition id of a node, to where the edge goes
     */
    private String dpOfToNodeId;
    private String edgeLabel;
    private Map<String, String> edgeProperties;
}
