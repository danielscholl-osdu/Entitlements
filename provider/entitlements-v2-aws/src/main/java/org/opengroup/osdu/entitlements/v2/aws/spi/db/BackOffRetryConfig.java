// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.entitlements.v2.aws.spi.db;

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
