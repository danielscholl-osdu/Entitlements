package org.opengroup.osdu.entitlements.v2.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestData {
    private String relativePath;
    private String method;
    private String body;
    private String dataPartitionId;
    private String token;
    private String url;
    @Default
    private Map<String, String> additionalHeaders = new HashMap<>();
    @Default
    private Map<String, String> queryParams = new HashMap<>();
}
