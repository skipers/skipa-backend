package com.skipers.skipa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SkipaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkipaBackendApplication.class, args);
    }
}
