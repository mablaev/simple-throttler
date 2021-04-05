package com.demo.throttle.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "throttling")
public class AppProperties {
    int guestRps;
    int slaRetrieverPoolSize;
    int poolAutoReleaseTimeOut;
}
