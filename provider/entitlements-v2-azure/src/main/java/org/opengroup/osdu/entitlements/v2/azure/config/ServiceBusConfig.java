package org.opengroup.osdu.entitlements.v2.azure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ServiceBusConfig {

    @Value("${azure.entitlements-change.servicebus.topic-name}")
    private String entitlementsChangeServiceBusTopic;
}
