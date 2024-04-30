package org.opengroup.osdu.entitlements.v2.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "partition.feature-flag")
@Configuration
@Data
public class PartitionFeatureFlagConfig {

    private Map<String, Boolean> defaults = new HashMap<>();

}
