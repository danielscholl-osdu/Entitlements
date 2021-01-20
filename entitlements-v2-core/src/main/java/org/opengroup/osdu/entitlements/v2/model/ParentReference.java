package org.opengroup.osdu.entitlements.v2.model;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

/**
 * We use 2 json library annotation in this class for performance optimization
 * @ConfiledJson @JsonAttribute annotations are from dsljson library and they are used for conversion when storing and retrieving from reference db,
 * because it outperforms other json libraries
 * @JsonProperty @JsonIgnore annotations are from jackson and spring boot will use it to serialize the response, we use this class as the output of
 * List group API in order to save the object recreation cost. And the response should only contain email (we rename id to email when serializing),
 * name and description fields
 */
@Data
@CompiledJson
@Generated
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentReference {
    @JsonProperty("email")
    private String id;
    private String name;
    private String description;
    @JsonIgnore
    private String dataPartitionId;

    @JsonAttribute(index = 3)
    public String getId() {
        return id;
    }

    @JsonAttribute(index = 1)
    public String getName() {
        return name;
    }

    @JsonAttribute(index = 2)
    public String getDescription() {
        return description;
    }

    @JsonAttribute(index = 4)
    public String getDataPartitionId() {
        return dataPartitionId;
    }

    public static ParentReference createParentReference(EntityNode groupNode) {
        return ParentReference.builder()
                .id(groupNode.getNodeId())
                .name(groupNode.getName())
                .description(groupNode.getDescription())
                .dataPartitionId(groupNode.getDataPartitionId())
                .build();
    }

    @JsonIgnore
    public boolean isRootUserGroup() {
        return "users".equalsIgnoreCase(name);
    }

    @JsonIgnore
    public boolean isDataGroup() {
        return name.toLowerCase().startsWith("data.");
    }

    /*
    sharing user group is not considered as users group, we should not add the root user group as member when creating them
     */
    @JsonIgnore
    public boolean isUserGroup() {
        String lowercaseGroupName = name.toLowerCase();
        return (lowercaseGroupName.startsWith("users.") || lowercaseGroupName.startsWith("user.") || isRootUserGroup()) &&
                !lowercaseGroupName.startsWith("users.sharing_");
    }

    @JsonIgnore
    public boolean isServiceGroup() {
        return name.toLowerCase().startsWith("service.");
    }

    public boolean isMatchGroupType(GroupType type) {
        return GroupType.DATA.equals(type) ? isDataGroup() : GroupType.USER.equals(type) ? isUserGroup() : GroupType.SERVICE.equals(type) && isServiceGroup();
    }
}
