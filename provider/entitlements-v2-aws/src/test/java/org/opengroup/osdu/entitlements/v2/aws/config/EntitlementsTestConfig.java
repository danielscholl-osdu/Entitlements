package org.opengroup.osdu.entitlements.v2.aws.config;

import org.opengroup.osdu.core.aws.mongodb.helper.BasicMongoDBHelper;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.MongoConfig;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.MongoPropertiesReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mongodb.core.MongoTemplate;

@TestConfiguration
@ComponentScan(
        basePackages = {"org.opengroup.osdu"},
        basePackageClasses = {ConversionService.class})
public class EntitlementsTestConfig {

    @MockBean
    MongoPropertiesReader mongoProperties;
    @MockBean
    MongoConfig mongoConfig;

    @Bean
    public ConversionService conversionService(ApplicationContext ctx) {
        DefaultConversionService conversionService = new DefaultConversionService();
        for (Converter<?, ?> converter : ctx.getBeansOfType(Converter.class).values()) {
            conversionService.addConverter(converter);
        }
        return conversionService;
    }

    @Bean
    @Autowired
    public BasicMongoDBHelper mongoHelper(MongoTemplate mongoTemplate) {
        return new BasicMongoDBHelper(mongoTemplate);
    }
}
