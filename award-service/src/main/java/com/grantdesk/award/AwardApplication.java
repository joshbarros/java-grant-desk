package com.grantdesk.award;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AwardApplication {

    public static void main(String[] args) {
        SpringApplication.run(AwardApplication.class, args);
    }
}
