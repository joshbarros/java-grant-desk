package com.grantdesk.grantee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GranteeApplication {

    public static void main(String[] args) {
        SpringApplication.run(GranteeApplication.class, args);
    }
}
