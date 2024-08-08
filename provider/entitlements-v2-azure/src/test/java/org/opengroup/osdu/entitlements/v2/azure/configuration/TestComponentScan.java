package org.opengroup.osdu.entitlements.v2.azure.configuration;

import org.opengroup.osdu.azure.filters.LogCustomDimensionFilter;
import org.opengroup.osdu.entitlements.v2.azure.EntitlementsV2Application;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
    value = {
        "org.opengroup.osdu.core",
        "org.opengroup.osdu.azure",
        "org.opengroup.osdu.entitlements.v2"
    },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EntitlementsV2Application.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = LogCustomDimensionFilter.class)
    }
)
public class TestComponentScan {

}
