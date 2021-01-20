package org.opengroup.osdu.entitlements.v2.azure.model;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Generated
public class NodeVertex {
    private String id;
    private String label;
    private String type;
    private Map<String, List<Map<String, String>>> properties;

    public String getNodeId() {
        return properties.get("nodeId").get(0).get("value");
    }

    public String getName() {
        return properties.get("name").get(0).get("value");
    }

    public String getDescription() {
        return properties.get("description").get(0).get("value");
    }

    public String getDataPartitionId() {
        return properties.get("dataPartitionId").get(0).get("value");
    }

    public String getAppIds() {
        return properties.get("appIds").get(0).get("value");
    }
}