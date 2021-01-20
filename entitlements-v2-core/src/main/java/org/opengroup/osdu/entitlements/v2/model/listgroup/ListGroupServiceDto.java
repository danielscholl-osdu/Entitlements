package org.opengroup.osdu.entitlements.v2.model.listgroup;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;

import java.util.List;

@Data
@Generated
@Builder
public class ListGroupServiceDto {
    private String requesterId;
    private String appId;
    private List<String> partitionIds;
}
