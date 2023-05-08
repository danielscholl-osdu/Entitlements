package org.opengroup.osdu.entitlements.v2.azure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class PublisherConfig {

    @Value("${azure.publisher.batchsize}")
    private String pubSubBatchSize;
}
