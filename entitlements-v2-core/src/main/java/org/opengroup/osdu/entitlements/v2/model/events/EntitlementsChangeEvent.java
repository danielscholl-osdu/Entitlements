package org.opengroup.osdu.entitlements.v2.model.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntitlementsChangeEvent {
    private EntitlementsChangeType kind;
    private String group;
    @Builder.Default
    private String user = "";
    private EntitlementsChangeAction action;
    private String modifiedBy;
    private long modifiedOn;
}
