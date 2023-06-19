package org.opengroup.osdu.entitlements.v2.model.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.core.common.model.status.Message;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntitlementsChangeEvent implements Message {
    private EntitlementsChangeType kind;
    private String group;
    @Builder.Default
    private String user = "";
    @Builder.Default
    private String updatedGroupEmail = "";
    private EntitlementsChangeAction action;
    private String modifiedBy;
    private long modifiedOn;
}
