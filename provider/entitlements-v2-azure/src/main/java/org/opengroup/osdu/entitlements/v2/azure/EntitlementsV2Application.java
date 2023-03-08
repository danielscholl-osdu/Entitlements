package org.opengroup.osdu.entitlements.v2.azure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({
        "org.opengroup.osdu.core",
        "org.opengroup.osdu.azure",
        "org.opengroup.osdu.entitlements.v2"
})
@SpringBootApplication
public class EntitlementsV2Application {
    public static void main(String[] args) {
        Class<?>[] sources = new Class<?>[] { EntitlementsV2Application.class };
        SpringApplication.run(sources, args);
    }
}
