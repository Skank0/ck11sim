package com.ntcees;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ApplicationCK11sim {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ApplicationCK11sim.class);
        app.run(args);
    }
}