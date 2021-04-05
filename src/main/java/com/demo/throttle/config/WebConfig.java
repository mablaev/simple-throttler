package com.demo.throttle.config;

import com.demo.throttle.controller.support.ThrottlingFilter;
import com.demo.throttle.service.ThrottlingService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {
    @Bean
    public FilterRegistrationBean<ThrottlingFilter> apiFilter(ThrottlingService throttlingService) {
        FilterRegistrationBean<ThrottlingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ThrottlingFilter(throttlingService));
        return registration;
    }
}
