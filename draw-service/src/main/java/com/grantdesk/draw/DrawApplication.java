package com.grantdesk.draw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DrawApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrawApplication.class, args);
    }
}
