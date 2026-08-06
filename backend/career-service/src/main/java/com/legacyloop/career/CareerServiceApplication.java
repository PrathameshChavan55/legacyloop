package com.legacyloop.career;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Jobs, applications, referrals, resumes, AI and analytics. Port 8082. */
@SpringBootApplication(scanBasePackages = {"com.legacyloop.career", "com.legacyloop.common"})
public class CareerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerServiceApplication.class, args);
    }
}
