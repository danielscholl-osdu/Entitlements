package org.opengroup.osdu.entitlements.v2.gcp.di;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;
import org.threeten.bp.Duration;

@Configuration
@ComponentScan
public class DataStoreBeanConfiguration {

    private static final RetrySettings RETRY_SETTINGS = RetrySettings.newBuilder()
            .setMaxAttempts(6)
            .setInitialRetryDelay(Duration.ofSeconds(1))
            .setMaxRetryDelay(Duration.ofSeconds(10))
            .setRetryDelayMultiplier(2.0)
            .setTotalTimeout(Duration.ofSeconds(50))
            .setInitialRpcTimeout(Duration.ofSeconds(50))
            .setRpcTimeoutMultiplier(1.1)
            .setMaxRpcTimeout(Duration.ofSeconds(50))
            .build();

    @Bean
    @RequestScope
    public Datastore getPartitionDatastore(final TenantInfo tenantInfo) {
        return DatastoreOptions.newBuilder()
                .setRetrySettings(RETRY_SETTINGS)
                .setCredentials(new DatastoreCredential(tenantInfo))
                .setProjectId(tenantInfo.getProjectId())
                .build()
                .getService();
    }
}
