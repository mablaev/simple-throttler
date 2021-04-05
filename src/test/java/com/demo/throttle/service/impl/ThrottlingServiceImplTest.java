package com.demo.throttle.service.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.demo.throttle.service.SlaService.SLA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ThrottlingServiceImplTest {
    private static final int GUEST_RPS = 33;
    public static final String NON_EXISTING_TOKEN = "non-existing-token";

    private SlaServiceImpl slaService;
    private ThrottlingServiceImpl throttlingService;

    @Before
    public void setUp() {
        slaService = spy(new SlaServiceImpl(Executors.newFixedThreadPool(3)));
        throttlingService = spy(new ThrottlingServiceImpl(slaService, GUEST_RPS, 100));
    }

    @After
    public void tearDown() throws Exception {
        throttlingService.close();
    }

    @Test
    public void emptyTokenShouldBeLimitedByGuestRps() {
        acquireAndAssert(null, GUEST_RPS);

        boolean requestAllowed = throttlingService.isRequestAllowed(Optional.empty());

        assertThat(requestAllowed).isFalse();
        verify(slaService, never()).getSlaByToken(anyString());
    }

    @Test
    public void nonAuthorizedUserShouldBeLimitedByGuestRps() {
        acquireAndAssert(NON_EXISTING_TOKEN, GUEST_RPS);

        boolean requestAllowed = throttlingService.isRequestAllowed(Optional.of(NON_EXISTING_TOKEN));

        assertThat(requestAllowed).isFalse();
        verify(slaService, times(GUEST_RPS + 1)).getSlaByToken(eq(NON_EXISTING_TOKEN));
    }

    @Test
    public void useGuestRpsIfSlaIsNotRetrievedYet() {
        acquireAndAssert("token-1", GUEST_RPS);

        boolean requestAllowed = throttlingService.isRequestAllowed(Optional.of("token-1"));

        assertThat(requestAllowed).isFalse();

        verify(slaService, times(GUEST_RPS + 1)).getSlaByToken(eq("token-1"));
    }

    @Test
    public void userCanAcquireAccordingToHisTokens() {
        SLA testUserSla = SLA.of("user-1", 2);

        when(slaService.getSlaByToken(eq("token-1")))
                .thenReturn(CompletableFuture.completedFuture(testUserSla));
        when(slaService.getSlaByToken(eq("token-2")))
                .thenReturn(CompletableFuture.completedFuture(testUserSla));

        assertThat(throttlingService.tryAcquireRequestPermit(Optional.of("token-1"))).isTrue();
        assertThat(throttlingService.tryAcquireRequestPermit(Optional.of("token-2"))).isTrue();

        assertThat(throttlingService.isRequestAllowed(Optional.of("token-1"))).isFalse();
        assertThat(throttlingService.isRequestAllowed(Optional.of("token-2"))).isFalse();

        assertThat(throttlingService.tryAcquireRequestPermit(Optional.of("token-1"))).isFalse();
        assertThat(throttlingService.tryAcquireRequestPermit(Optional.of("token-2"))).isFalse();
    }

    @Test
    public void requestIsNotAllowedWhenRpsDecreased() {
        acquireAndAssert(NON_EXISTING_TOKEN, GUEST_RPS);

        SLA newGuestSla = SLA.of(ThrottlingServiceImpl.GUEST_USER, GUEST_RPS - 5);

        when(slaService.getSlaByToken(eq(NON_EXISTING_TOKEN)))
                .thenReturn(CompletableFuture.completedFuture(newGuestSla));

        boolean requestAllowed = throttlingService.isRequestAllowed(Optional.of(NON_EXISTING_TOKEN));

        assertThat(requestAllowed).isFalse();
    }

    @Test
    public void requestIsAllowedWhenRpsIncreased() {
        acquireAndAssert(NON_EXISTING_TOKEN, GUEST_RPS);

        SLA newGuestSla = SLA.of(ThrottlingServiceImpl.GUEST_USER, GUEST_RPS + 1);

        when(slaService.getSlaByToken(eq(NON_EXISTING_TOKEN)))
                .thenReturn(CompletableFuture.completedFuture(newGuestSla));

        boolean requestAllowed = throttlingService.isRequestAllowed(Optional.of(NON_EXISTING_TOKEN));

        assertThat(requestAllowed).isTrue();
    }

    @Test
    public void assertUserRps() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            //exhausts permits for guest
            acquireAndAssert(NON_EXISTING_TOKEN, GUEST_RPS);
            assertThat(throttlingService.isRequestAllowed(Optional.of(NON_EXISTING_TOKEN))).isFalse();

            TimeUnit.MILLISECONDS.sleep(1500);

            assertThat(throttlingService.isRequestAllowed(Optional.of(NON_EXISTING_TOKEN))).isTrue();
        }
    }

    private void acquireAndAssert(String token, int times) {
        for (int i = 0; i < times; i++) {
            assertThat(throttlingService.tryAcquireRequestPermit(Optional.ofNullable(token))).isTrue();
        }
    }
}