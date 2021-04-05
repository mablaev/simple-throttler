package com.demo.throttle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
public class ThrottleApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThrottleApplication.class, args);
	}

}
