package com.minsu.mockstocklive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MockStockLiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockStockLiveApplication.class, args);
    }

}
