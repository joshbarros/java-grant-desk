package com.grantdesk.eligibility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EligibilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(EligibilityApplication.class, args);
    }
}
