package com.demo.throttle.service.impl;

import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.Semaphore;

@ToString
public class SlaContext {
    @Getter
    private final int rps;
    private final Semaphore semaphore;
    @Getter
    private long lastAccessTimestamp;

    public SlaContext(int rps) {
        this.rps = rps;
        this.semaphore = new Semaphore(rps);
    }

    public boolean canAcquire() {
        return semaphore.availablePermits() > 0;
    }

    public boolean tryAcquire() {
        try {
            return semaphore.tryAcquire();
        } finally {
            lastAccessTimestamp = System.nanoTime();
        }
    }

    public boolean tryAcquire(int permits) {
        try {
            return semaphore.tryAcquire(permits);
        } finally {
            lastAccessTimestamp = System.nanoTime();
        }
    }

    public int getAcquiredPermits() {
        return rps - semaphore.availablePermits();
    }

    public void reset() {
        if (getAcquiredPermits() > 0) {
            semaphore.release(getAcquiredPermits());
        }
    }
}
