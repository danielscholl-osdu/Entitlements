package org.opengroup.osdu.entitlements.v2.azure.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.Role;

@Data
@Builder
@Generated
@NoArgsConstructor
@AllArgsConstructor
public class AddEdgeDto {
    private String parentNodeId;
    private String childNodeId;
    private Role roleOfChild;
    /**
     * data partition id of child node
     */
    private String dpOfChild;
}
