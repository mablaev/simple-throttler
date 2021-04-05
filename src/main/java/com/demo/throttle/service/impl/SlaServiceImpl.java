package com.demo.throttle.service.impl;

import com.demo.throttle.service.SlaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SlaServiceImpl implements SlaService {
    private static final int MIN_SLEEP_TIME = 200;
    private static final Map<String, SLA> slaDb;

    static {
        SLA user1Sla = SLA.of("user1", 3);
        SLA user2Sla = SLA.of("user2", 5);
        slaDb = Map.of(
                "token-1", user1Sla,
                "token-2", user1Sla,
                "token-3", user2Sla,
                "token-4", user2Sla,
                "token-5", user2Sla
        );
    }

    private final Executor executor;

    public SlaServiceImpl(@Qualifier("slaRetrieveExecutor") Executor executor) {
        this.executor = executor;
    }

    @Override
    @Cacheable(value = "sla-cache", sync = true)
    public CompletableFuture<SLA> getSlaByToken(String token) {
        return CompletableFuture.supplyAsync(() -> retrieveSla(token), executor);
    }

    private SLA retrieveSla(String token) {
        try {
            return slaDb.getOrDefault(token, SLA.EMPTY);
        } finally {
            delay();
        }
    }

    private void delay() {
        int sleepTime = MIN_SLEEP_TIME + new Random().nextInt(100);
        try {
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
            log.error("Interruption signal was received", e);
            Thread.currentThread().interrupt();
        }
    }
}
