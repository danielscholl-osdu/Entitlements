package org.opengroup.osdu.entitlements.v2.azure.model;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;

import java.util.Map;

@Data
@Builder
@Generated
public class NodeEdge {
    private String id;
    private String label;
    private String type;
    private String inVLabel;
    private String outVLabel;
    private String inV;
    private String outV;
    private Map<String, String> properties;

    public String getRole() {
        return properties.get("role");
    }
}