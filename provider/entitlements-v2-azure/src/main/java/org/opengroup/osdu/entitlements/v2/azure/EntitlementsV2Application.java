package org.opengroup.osdu.entitlements.v2.azure;

import org.opengroup.osdu.azure.dependencies.AzureOSDUConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@ComponentScan({
        "org.opengroup.osdu.core",
        "org.opengroup.osdu.azure",
        "org.opengroup.osdu.entitlements.v2"
})
@SpringBootApplication
@EnableAsync
public class EntitlementsV2Application {
    public static void main(String[] args) {
        Class<?>[] sources = new Class<?>[]{
                EntitlementsV2Application.class,
                AzureOSDUConfig.class
        };
        SpringApplication.run(sources, args);
    }
}
