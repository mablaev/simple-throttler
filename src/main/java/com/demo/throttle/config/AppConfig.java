package com.demo.throttle.config;

import com.demo.throttle.service.SlaService;
import com.demo.throttle.service.ThrottlingService;
import com.demo.throttle.service.impl.ThrottlingServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableCaching
@ConfigurationPropertiesScan("com.demo.throttle")
@RequiredArgsConstructor
public class AppConfig {

    private final AppProperties properties;

    @Bean
    public ThrottlingService throttlingService(SlaService slaService) {
        return new ThrottlingServiceImpl(slaService, properties.getGuestRps(), properties.getPoolAutoReleaseTimeOut());
    }

    @Bean
    @Qualifier("slaRetrieveExecutor")
    public Executor slaRetrieveExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(properties.getSlaRetrieverPoolSize());
        return executor;
    }
}
