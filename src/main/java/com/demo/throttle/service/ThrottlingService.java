package com.demo.throttle.service;

import java.util.Optional;

/**
 * Contract describes throttling service.
 */
public interface ThrottlingService extends AutoCloseable {
    /**
     * @param token - user token. User may have multiple tokens.
     * @return true if request is within allowed request per second (RPS) or false otherwise
     */
    boolean isRequestAllowed(Optional<String> token);

    /**
     * @param token user token. User may have multiple tokens.
     * @return true if permit was allocated, otherwise false
     */
    boolean tryAcquireRequestPermit(Optional<String> token);
}
