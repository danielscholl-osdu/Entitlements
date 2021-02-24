package org.opengroup.osdu.entitlements.v2.azure.model;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@Generated
public class NodeVertex {
    private String id;
    private String label;
    private String type;
    private Map<String, List<Map<String, String>>> properties;

    public String getNodeId() {
        return properties.get(VertexPropertyNames.NODE_ID).get(0).get("value");
    }

    public String getName() {
        return properties.get(VertexPropertyNames.NAME).get(0).get("value");
    }

    public String getDescription() {
        return properties.get(VertexPropertyNames.DESCRIPTION).get(0).get("value");
    }

    public String getDataPartitionId() {
        return properties.get(VertexPropertyNames.DATA_PARTITION_ID).get(0).get("value");
    }

    public Set<String> getAppIds() {
        return Optional.ofNullable(properties.get(VertexPropertyNames.APP_ID))
                .orElseGet(Collections::emptyList).stream()
                .map(appId -> appId.get("value"))
                .collect(Collectors.toSet());
    }
}