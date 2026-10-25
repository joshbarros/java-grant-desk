package com.grantdesk.clawback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClawbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClawbackApplication.class, args);
    }
}
