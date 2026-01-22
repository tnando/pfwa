package com.pfwa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for the Personal Finance Web Application (PFWA).
 */
@SpringBootApplication
@EnableScheduling
public class PfwaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PfwaApplication.class, args);
    }
}
