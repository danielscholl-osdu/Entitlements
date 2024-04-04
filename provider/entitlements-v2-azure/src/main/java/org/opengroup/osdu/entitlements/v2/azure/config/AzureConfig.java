package org.opengroup.osdu.entitlements.v2.azure.config;

import com.microsoft.applicationinsights.TelemetryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class AzureConfig {

    @Bean
    public TelemetryClient telemetryClient() {
        return new TelemetryClient();
    }

    @Bean
    public Duration slowIndicatorLoggingThreshold() {
        return Duration.ofSeconds(5);
    }
}
