package com.legacyloop.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Premium plans, Razorpay orders, webhook verification and subscription lifecycle.
 *
 * <p>Note what this service does NOT do: it never writes to the auth database. It publishes
 * payment.captured and auth-service flips is_premium, because that flag has to be minted
 * into the JWT by whoever owns the token.</p>
 */
@EnableScheduling
@EnableDiscoveryClient
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@SpringBootApplication(scanBasePackages = {"com.legacyloop.payment", "com.legacyloop.common"})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
