package com.demo.throttle.controller;

import com.demo.throttle.service.GoodDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DumpController {

    private final GoodDataService dataService;

    @GetMapping("/throttled-data")
    public String getData() {
        return dataService.getRandomData();
    }
}
