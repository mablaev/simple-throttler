package com.demo.throttle.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GoodDataService {
    public String getRandomData() {
        return UUID.randomUUID().toString();
    }
}
