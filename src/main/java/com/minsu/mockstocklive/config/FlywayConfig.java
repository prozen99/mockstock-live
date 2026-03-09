package com.minsu.mockstocklive.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor flywayEntityManagerDependencyPostProcessor() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }

            String[] existingDependencies = beanFactory.getBeanDefinition("entityManagerFactory").getDependsOn();
            String[] dependencies = existingDependencies == null
                    ? new String[]{"flyway"}
                    : Stream.concat(Arrays.stream(existingDependencies), Stream.of("flyway"))
                    .distinct()
                    .toArray(String[]::new);

            beanFactory.getBeanDefinition("entityManagerFactory").setDependsOn(dependencies);
        };
    }
}
