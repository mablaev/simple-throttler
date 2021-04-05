package com.demo.throttle.service.impl;

import com.demo.throttle.service.SlaService;
import com.demo.throttle.service.ThrottlingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static com.demo.throttle.service.SlaService.SLA;

@Slf4j
public class ThrottlingServiceImpl implements ThrottlingService {
    public static final String GUEST_USER = "guest";

    private final SlaService slaService;
    private final SLA guestSla;
    private final ConcurrentMap<String, SlaContext> slaContextPool;
    private final int autoReleaseTimeout;
    private final ScheduledExecutorService scheduledExecutorService;

    public ThrottlingServiceImpl(SlaService slaService, int guestRps, int autoReleaseTimeout) {
        this.slaService = slaService;
        this.autoReleaseTimeout = autoReleaseTimeout;
        this.slaContextPool = new ConcurrentHashMap<>();
        this.guestSla = SLA.of(GUEST_USER, guestRps);

        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new HouseKeeperJob(this), 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public boolean isRequestAllowed(Optional<String> optionalToken) {
        log.debug("isRequestAllowed: {}", optionalToken);
        SlaContext slaContext = getSlaContext(optionalToken);
        return slaContext.canAcquire();
    }

    @Override
    public boolean tryAcquireRequestPermit(Optional<String> optionalToken) {
        log.debug("tryAcquireRequestPermit: {}", optionalToken);
        SlaContext slaContext = getSlaContext(optionalToken);
        return slaContext.tryAcquire();
    }

    private SlaContext getSlaContext(Optional<String> optionalToken) {
        SLA sla = getSlaByToken(optionalToken);
        return slaContextPool.compute(sla.getUser(), slaContextComputation(sla));
    }

    private BiFunction<String, SlaContext, SlaContext> slaContextComputation(SLA sla) {
        return (user, existingContext) -> isContextNewOrChanged(sla, existingContext)
                ? newSlaContext(sla, existingContext) : existingContext;
    }

    private boolean isContextNewOrChanged(SLA sla, SlaContext existingContext) {
        return existingContext == null || sla.getRps() != existingContext.getRps();
    }

    private SlaContext newSlaContext(SLA sla, SlaContext existingContext) {
        SlaContext newSlaContext = new SlaContext(sla.getRps());

        if (existingContext != null) {
            int needToAcquire = Math.min(newSlaContext.getRps(), existingContext.getAcquiredPermits());
            if (needToAcquire > 0) {
                existingContext.reset();
                newSlaContext.tryAcquire(needToAcquire);
            }
        }

        log.debug("New slaContext was created {}", newSlaContext);
        return newSlaContext;
    }

    private SLA getSlaByToken(Optional<String> token) {
        return token.map(slaService::getSlaByToken)
                .filter(CompletableFuture::isDone)
                .map(CompletableFuture::join)
                .filter(Predicate.not(SLA.EMPTY::equals))
                .orElse(guestSla);
    }

    @Override
    public void close() throws Exception {
        if (!scheduledExecutorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            scheduledExecutorService.shutdownNow();
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    private static class HouseKeeperJob implements Runnable {

        private final ThrottlingServiceImpl throttlingService;

        @Override
        public void run() {
//            slaContextPool.values().removeIf(this::contextExpired);
//            slaContextPool.values().forEach(SlaContext::reset);
            log.debug("Pool house-keeping is started...");
            Set<String> users = Set.copyOf(throttlingService.slaContextPool.keySet());
            log.debug("Number of users to revisit is '{}'", users.size());
            users.forEach(user -> throttlingService.slaContextPool.compute(user, this::invalidate));
            log.debug("house-keeping is completed.");
        }

        /**
         * @param user       name
         * @param slaContext current sla context
         * @return if returns null, then according to the Map.compute, it will remove the (k,v) from Map.
         */
        private SlaContext invalidate(String user, SlaContext slaContext) {
            if (contextExpired(slaContext)) {
                log.debug("Context is expired for user='{}', it will be removed", user);
                return null;
            } else {
                slaContext.reset();
                return slaContext;
            }
        }

        private boolean contextExpired(SlaContext slaContext) {
            long elapsedNanos = System.nanoTime() - slaContext.getLastAccessTimestamp();
            long elapseSeconds = TimeUnit.SECONDS.convert(elapsedNanos, TimeUnit.NANOSECONDS);
            return elapseSeconds > throttlingService.autoReleaseTimeout;
        }
    }
}
