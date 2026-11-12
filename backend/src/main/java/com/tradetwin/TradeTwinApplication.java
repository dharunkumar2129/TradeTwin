package com.tradetwin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TradeTwinApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeTwinApplication.class, args);
    }
}
