package com.demo.throttle.service;

import lombok.Value;

import java.util.concurrent.CompletableFuture;

public interface SlaService {
    /**
     * @param token of the user. can be null;
     * @return if null then returns SLA.EMPTY, otherwise corresponding value for a user to which token belongs.
     */
    CompletableFuture<SLA> getSlaByToken(String token);

    @Value(staticConstructor = "of")
    class SLA {
        public static final SLA EMPTY = of(null, 0);

        String user;
        int rps;
    }
}
