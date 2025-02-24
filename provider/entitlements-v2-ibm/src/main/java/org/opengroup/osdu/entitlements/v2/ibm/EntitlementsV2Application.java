/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

@ComponentScan({
        "org.opengroup.osdu.core",
        "org.opengroup.osdu.ibm",
        "org.opengroup.osdu.entitlements.v2"
})
@SpringBootApplication
@EnableAsync
@PropertySource("classpath:swagger.properties")
public class EntitlementsV2Application {
    public static void main(String[] args) {
        Class<?>[] sources = new Class<?>[]{
                EntitlementsV2Application.class

        };
        SpringApplication.run(sources, args);
    }
}
