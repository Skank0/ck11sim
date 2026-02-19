package com.ntcees;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class ApplicationCK11sim {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ApplicationCK11sim.class);
        app.run(args);
    }
}