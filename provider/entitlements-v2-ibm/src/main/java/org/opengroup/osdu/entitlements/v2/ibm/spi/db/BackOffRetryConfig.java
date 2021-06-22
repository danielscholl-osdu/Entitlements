/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.db;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.function.Predicate;

@Configuration
@ComponentScan
public class BackOffRetryConfig {

    @Bean
    public Retry createRetryClient() {
        return Retry.of("ContractEntitlementsV2", createRetryConfig());
    }

    private RetryConfig createRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofRandomized(500, 0.2))
                .retryOnException(shouldRetry())
                .build();
    }

    private Predicate<Throwable> shouldRetry() {
        return p ->
        {
            if (p instanceof AppException) {
                AppException e = ((AppException) p);
                return e.getError().getCode() == HttpStatus.SC_LOCKED;
            }
            return false;
        };
    }
}
