package org.opengroup.osdu.entitlements.v2.aws.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.context.ContextConfiguration;

@Import({MongoProperties.class, MongoConfigTest.class,
        EntitlementsContext.class
})
@ComponentScan(basePackageClasses = {ConversionService.class})
@ContextConfiguration(classes = EntitlementsTestConfig.class)
public class EntitlementsTestConfig {

    @Bean
    public ConversionService conversionService(ApplicationContext ctx) {
        DefaultConversionService conversionService = new DefaultConversionService();
        for (Converter<?, ?> converter : ctx.getBeansOfType(Converter.class).values()) {
            conversionService.addConverter(converter);
        }
        return conversionService;
    }
}
