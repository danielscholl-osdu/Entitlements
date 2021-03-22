package org.opengroup.osdu.entitlements.v2.model;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@Generated
@NoArgsConstructor
public class ParentReferences {
    private Set<ParentReference> parentReferencesOfUser;
}