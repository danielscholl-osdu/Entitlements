package org.opengroup.osdu.entitlements.v2.azure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan({
        "org.opengroup.osdu.core",
        "org.opengroup.osdu.azure",
        "org.opengroup.osdu.entitlements.v2"
})
@SpringBootApplication
@EnableAsync
@EnableScheduling
@PropertySource("classpath:swagger.properties")
public class EntitlementsV2Application {
    public static void main(String[] args) {
        Class<?>[] sources = new Class<?>[] { EntitlementsV2Application.class };
        SpringApplication.run(sources, args);
    }
}
