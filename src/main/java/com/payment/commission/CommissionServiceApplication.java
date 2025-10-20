package com.payment.commission;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Payment Commission Service - Main Application
 *
 * This service handles:
 * - BCEAO-compliant fee calculation
 * - Commission rule management
 * - Revenue tracking and reporting
 * - Settlement management
 *
 * @version 1.0.0
 * @author Payment System Team
 */
@SpringBootApplication(scanBasePackages = {"com.payment.commission", "com.payment.common", "com.payment.security", "com.payment.kafka"})
@EnableCaching
@EnableJpaAuditing
public class CommissionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommissionServiceApplication.class, args);
    }
}
