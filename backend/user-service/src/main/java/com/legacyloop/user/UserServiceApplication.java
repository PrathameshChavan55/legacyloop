package com.legacyloop.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Accounts, institutions, academic master data, profiles and premium billing. Port 8081. */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.legacyloop.user", "com.legacyloop.common"})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
