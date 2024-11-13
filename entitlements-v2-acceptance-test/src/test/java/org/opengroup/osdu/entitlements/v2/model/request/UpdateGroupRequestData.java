package org.opengroup.osdu.entitlements.v2.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupRequestData {
    private String op;
    private String path;
    private List<String> value;
}
