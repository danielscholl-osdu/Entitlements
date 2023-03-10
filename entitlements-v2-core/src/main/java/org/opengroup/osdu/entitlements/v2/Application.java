package org.opengroup.osdu.entitlements.v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@PropertySource("classpath:swagger.properties")
public class Application  {
    public static void main(String[] args) {
        SpringApplication.run(new Class[] { Application.class} , args);
    }
}
