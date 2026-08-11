package com.shixianwen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ShixianwenApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShixianwenApplication.class, args);
    }
}
