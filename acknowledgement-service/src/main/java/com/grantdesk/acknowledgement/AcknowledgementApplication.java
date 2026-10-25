package com.grantdesk.acknowledgement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AcknowledgementApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcknowledgementApplication.class, args);
    }
}
