package org.opengroup.osdu.entitlements.v2.acceptance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    private String value;
    private String userId;
}
