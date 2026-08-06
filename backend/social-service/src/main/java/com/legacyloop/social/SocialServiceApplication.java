package com.legacyloop.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The feed, the alumni network, chat and notifications. Port 8083.
 *
 * <p>This is the MongoDB service. Posts, conversations and notifications are documents that are
 * read whole and never joined, which is the case for a document store; accounts and jobs are
 * relational and stay in MySQL.
 */
@SpringBootApplication(scanBasePackages = {"com.legacyloop.social", "com.legacyloop.common"})
public class SocialServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialServiceApplication.class, args);
    }
}
