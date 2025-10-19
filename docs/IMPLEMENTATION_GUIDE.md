# Commission Service - Complete Implementation Guide

**Service**: payment-commission-service
**Version**: 1.0.0
**Language**: Java 17
**Framework**: Spring Boot 3.2.0
**Build Tool**: Gradle 8.5+
**Purpose**: Transaction fee calculation, commission rule management, revenue tracking, and settlement
**Last Updated**: 2025-10-19

---

## Table of Contents

1. [Overview](#overview)
2. [Service Architecture](#service-architecture)
3. [Technology Stack](#technology-stack)
4. [Project Structure](#project-structure)
5. [Database Schema](#database-schema)
6. [Domain Model](#domain-model)
7. [API Specifications](#api-specifications)
8. [Service Layer](#service-layer)
9. [Repository Layer](#repository-layer)
10. [Event Publishing](#event-publishing)
11. [Security & Authentication](#security--authentication)
12. [Configuration](#configuration)
13. [Testing Strategy](#testing-strategy)
14. [Deployment](#deployment)
15. [Implementation Checklist](#implementation-checklist)

---

## Overview

The **Commission Service** is responsible for calculating and tracking all transaction fees on the platform. It implements BCEAO-compliant fee rules and manages platform revenue.

### Core Responsibilities

- ✅ **Fee Calculation** - Calculate transaction fees based on BCEAO rules
- ✅ **Commission Rules Management** - Manage fee rules per provider and transfer type
- ✅ **Revenue Tracking** - Track platform commission revenue per transaction
- ✅ **Settlement Management** - Track settlement status with providers
- ✅ **Revenue Reporting** - Generate revenue reports by provider, period, currency
- ✅ **Rule Versioning** - Support time-based rule activation/expiration
- ✅ **Multi-Currency Support** - Handle XOF and XAF commissions

### Key Business Rules (BCEAO Compliance)

1. **BCEAO Fee Structure**:
   - Transfers ≤ 5,000 XOF: **FREE** (financial inclusion mandate)
   - Transfers > 5,000 XOF: **100 XOF fixed + 0.5% of amount**, capped at **1,000 XOF**
   - Formula: `min(100 + (amount × 0.005), 1000)`

2. **Commission Types**:
   - **SAME_WALLET**: Same provider (e.g., Orange Money → Orange Money)
   - **CROSS_WALLET**: Different providers (e.g., Orange Money → Wave)
   - **INTERNATIONAL**: Cross-country transfers

3. **Rule Priority System**:
   - Multiple rules can exist for the same provider
   - Higher priority rules are evaluated first
   - First matching rule is applied

4. **Commission Components**:
   - **Percentage Fee**: % of transaction amount
   - **Fixed Fee**: Flat amount added to percentage
   - **Minimum Fee**: Floor value (can't charge less)
   - **Maximum Fee**: Ceiling value (can't charge more)

5. **Settlement Tracking**:
   - Commissions marked as PENDING/COMPLETED
   - Track settlement with providers
   - Generate settlement reports

---

## Service Architecture

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Controller Layer                         │
│  (REST API Endpoints, Request Validation, Error Handling)   │
└─────────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                           │
│   (Fee Calculation, Rule Management, Revenue Tracking)      │
└─────────────────────────────────────────────────────────────┘
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
┌────────▼────────┐ ┌──────▼────────┐ ┌───────▼──────────┐
│ Commission      │ │ Commission    │ │ Rule             │
│ Rule Repo       │ │ Transaction   │ │ Engine           │
│                 │ │ Repo          │ │                  │
└─────────────────┘ └───────────────┘ └──────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL Database                       │
│                    (commission_db)                           │
└─────────────────────────────────────────────────────────────┘
```

### Integration Points

**Outbound (This service calls)**:
- None (stateless calculation service)

**Inbound (Other services call this)**:
- Transaction Service (calculate fees for transactions)
- Admin Service (manage commission rules, view revenue)
- Reporting Service (revenue analytics)

**Event Publishing (Kafka)**:
- `COMMISSION_COLLECTED` - When commission is recorded
- `COMMISSION_REFUNDED` - When transaction is refunded
- `COMMISSION_SETTLED` - When commission is settled with provider

**Event Consumption (Kafka)**:
- `TRANSACTION_COMPLETED` - Record commission for completed transaction
- `TRANSACTION_REVERSED` - Mark commission as refunded

---

## Technology Stack

### Core Dependencies

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.payment'
version = '1.0.0'
sourceCompatibility = '17'

repositories {
    mavenCentral()
    mavenLocal() // For shared libraries
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Shared Libraries (MUST be built first)
    implementation 'com.payment:payment-common-lib:1.0.0'
    implementation 'com.payment:payment-security-lib:1.0.0'
    implementation 'com.payment:payment-kafka-lib:1.0.0'

    // Database
    implementation 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'

    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'

    // Redis (for caching commission rules)
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // Utilities
    implementation 'org.mapstruct:mapstruct:1.5.5.Final'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'

    // API Documentation
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:postgresql:1.19.3'
    testImplementation 'org.testcontainers:kafka:1.19.3'
    testImplementation 'org.springframework.kafka:spring-kafka-test'
    testImplementation 'io.rest-assured:rest-assured:5.3.2'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

---

## Project Structure

```
payment-commission-service/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── payment/
│   │   │           └── commission/
│   │   │               ├── CommissionServiceApplication.java
│   │   │               │
│   │   │               ├── config/
│   │   │               │   ├── SecurityConfig.java
│   │   │               │   ├── KafkaConfig.java
│   │   │               │   ├── RedisConfig.java
│   │   │               │   ├── DatabaseConfig.java
│   │   │               │   └── OpenApiConfig.java
│   │   │               │
│   │   │               ├── controller/
│   │   │               │   ├── CommissionController.java
│   │   │               │   ├── CommissionRuleController.java
│   │   │               │   ├── RevenueReportController.java
│   │   │               │   └── advice/
│   │   │               │       └── GlobalExceptionHandler.java
│   │   │               │
│   │   │               ├── service/
│   │   │               │   ├── CommissionService.java
│   │   │               │   ├── CommissionServiceImpl.java
│   │   │               │   ├── CommissionRuleService.java
│   │   │               │   ├── CommissionRuleServiceImpl.java
│   │   │               │   ├── FeeCalculationEngine.java
│   │   │               │   ├── RevenueTrackingService.java
│   │   │               │   ├── SettlementService.java
│   │   │               │   └── CommissionEventPublisher.java
│   │   │               │
│   │   │               ├── repository/
│   │   │               │   ├── CommissionRuleRepository.java
│   │   │               │   └── CommissionTransactionRepository.java
│   │   │               │
│   │   │               ├── domain/
│   │   │               │   ├── entity/
│   │   │               │   │   ├── CommissionRule.java
│   │   │               │   │   └── CommissionTransaction.java
│   │   │               │   │
│   │   │               │   └── enums/
│   │   │               │       ├── TransferType.java
│   │   │               │       ├── CommissionStatus.java
│   │   │               │       └── SettlementStatus.java
│   │   │               │
│   │   │               ├── dto/
│   │   │               │   ├── request/
│   │   │               │   │   ├── CalculateFeeRequest.java
│   │   │               │   │   ├── CreateRuleRequest.java
│   │   │               │   │   ├── UpdateRuleRequest.java
│   │   │               │   │   └── RevenueReportRequest.java
│   │   │               │   │
│   │   │               │   └── response/
│   │   │               │       ├── FeeCalculationResponse.java
│   │   │               │       ├── CommissionRuleResponse.java
│   │   │               │       ├── RevenueReportResponse.java
│   │   │               │       └── SettlementReportResponse.java
│   │   │               │
│   │   │               ├── event/
│   │   │               │   ├── CommissionCollectedEvent.java
│   │   │               │   ├── CommissionRefundedEvent.java
│   │   │               │   └── CommissionSettledEvent.java
│   │   │               │
│   │   │               ├── listener/
│   │   │               │   └── TransactionEventListener.java
│   │   │               │
│   │   │               ├── mapper/
│   │   │               │   ├── CommissionRuleMapper.java
│   │   │               │   └── CommissionTransactionMapper.java
│   │   │               │
│   │   │               ├── validation/
│   │   │               │   ├── FeeRuleValidator.java
│   │   │               │   └── AmountValidator.java
│   │   │               │
│   │   │               └── exception/
│   │   │                   ├── RuleNotFoundException.java
│   │   │                   ├── InvalidRuleException.java
│   │   │                   └── NoMatchingRuleException.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       │
│   │       ├── db/
│   │       │   └── migration/
│   │       │       ├── V1__create_commission_rules_table.sql
│   │       │       ├── V2__create_commission_transactions_table.sql
│   │       │       ├── V3__insert_default_bceao_rules.sql
│   │       │       └── V4__add_indexes.sql
│   │       │
│   │       └── logback-spring.xml
│   │
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── payment/
│       │           └── commission/
│       │               ├── controller/
│       │               │   ├── CommissionControllerTest.java
│       │               │   └── CommissionRuleControllerTest.java
│       │               │
│       │               ├── service/
│       │               │   ├── CommissionServiceTest.java
│       │               │   ├── FeeCalculationEngineTest.java
│       │               │   └── RevenueTrackingServiceTest.java
│       │               │
│       │               ├── repository/
│       │               │   └── CommissionRuleRepositoryTest.java
│       │               │
│       │               └── integration/
│       │                   ├── FeeCalculationIntegrationTest.java
│       │                   └── RuleManagementIntegrationTest.java
│       │
│       └── resources/
│           ├── application-test.yml
│           └── test-data.sql
│
├── build.gradle
├── settings.gradle
├── gradle.properties
├── Dockerfile
├── docker-compose.yml
└── README.md
```

---

## Database Schema

### Tables

The Commission Service uses two main tables in the `commission_db` database:

#### 1. commission_rules

Stores commission calculation rules per wallet provider and currency.

```sql
CREATE TABLE commission_rules (
    rule_id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_id         UUID NOT NULL REFERENCES wallet_providers(provider_id) ON DELETE CASCADE,
    currency            VARCHAR(3) NOT NULL REFERENCES currencies(currency_code),

    -- Rule conditions
    transfer_type       VARCHAR(20) NOT NULL CHECK (transfer_type IN ('SAME_WALLET', 'CROSS_WALLET', 'INTERNATIONAL')),
    min_transaction     BIGINT,    -- Minimum transaction amount (NULL = no minimum)
    max_transaction     BIGINT,    -- Maximum transaction amount (NULL = no maximum)
    kyc_level           VARCHAR(20) CHECK (kyc_level IN ('LEVEL_1', 'LEVEL_2', 'LEVEL_3', 'ANY')),

    -- Commission calculation
    percentage          NUMERIC(5,4) NOT NULL CHECK (percentage >= 0 AND percentage <= 1), -- e.g., 0.0050 for 0.5%
    fixed_amount        BIGINT DEFAULT 0,       -- Fixed fee in XOF (e.g., 100)
    min_amount          BIGINT DEFAULT 0 CHECK (min_amount >= 0),  -- Min commission (e.g., 50 XOF)
    max_amount          BIGINT CHECK (max_amount IS NULL OR max_amount >= min_amount), -- Max commission (e.g., 1000 XOF)

    -- Rule management
    is_active           BOOLEAN DEFAULT TRUE,
    priority            INTEGER DEFAULT 0,      -- Higher priority evaluated first
    effective_from      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    effective_to        TIMESTAMP WITH TIME ZONE, -- NULL = no expiry

    -- Metadata
    description         TEXT,
    notes               TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by          UUID REFERENCES users(user_id),

    -- Constraint: One active rule per provider per currency per type per priority
    UNIQUE(provider_id, currency, transfer_type, priority, effective_from)
);

-- Indexes
CREATE INDEX idx_commission_rules_provider ON commission_rules(provider_id, is_active);
CREATE INDEX idx_commission_rules_currency ON commission_rules(currency);
CREATE INDEX idx_commission_rules_provider_currency ON commission_rules(provider_id, currency, is_active);
CREATE INDEX idx_commission_rules_priority ON commission_rules(priority DESC);
CREATE INDEX idx_commission_rules_effective ON commission_rules(effective_from, effective_to);
CREATE INDEX idx_commission_rules_provider_type ON commission_rules(provider_id, transfer_type, is_active);
```

#### 2. commission_transactions

Tracks platform commission revenue per transaction.

```sql
CREATE TABLE commission_transactions (
    commission_id       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id      UUID NOT NULL REFERENCES transactions(transaction_id) ON DELETE RESTRICT,
    rule_id             UUID REFERENCES commission_rules(rule_id) ON DELETE SET NULL,
    provider_id         UUID NOT NULL REFERENCES wallet_providers(provider_id) ON DELETE RESTRICT,
    currency            VARCHAR(3) NOT NULL REFERENCES currencies(currency_code),

    -- Commission details
    amount              BIGINT NOT NULL CHECK (amount >= 0), -- Commission amount in XOF
    calculation_basis   JSONB,  -- Store calculation details (percentage, fixed, min, max applied)

    -- Accounting
    status              VARCHAR(20) DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'REFUNDED')),
    settled             BOOLEAN DEFAULT FALSE,
    settlement_date     TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_commission_transactions_transaction ON commission_transactions(transaction_id);
CREATE INDEX idx_commission_transactions_provider ON commission_transactions(provider_id);
CREATE INDEX idx_commission_transactions_currency ON commission_transactions(currency);
CREATE INDEX idx_commission_transactions_created_at ON commission_transactions(created_at DESC);
CREATE INDEX idx_commission_transactions_settled ON commission_transactions(settled, settlement_date);
```

### Database Functions

#### calculate_transfer_fee(amount)

BCEAO-compliant fee calculation function.

```sql
CREATE OR REPLACE FUNCTION calculate_transfer_fee(p_amount BIGINT)
RETURNS BIGINT AS $$
DECLARE
    v_fixed_fee BIGINT := 100;      -- 100 XOF fixed fee
    v_percentage NUMERIC := 0.005;  -- 0.5%
    v_max_fee BIGINT := 1000;       -- Maximum 1,000 XOF
    v_free_threshold BIGINT := 5000; -- FREE for amounts ≤ 5,000 XOF
    v_total_fee BIGINT;
BEGIN
    -- Amounts ≤ 5,000 XOF are FREE (BCEAO financial inclusion)
    IF p_amount <= v_free_threshold THEN
        RETURN 0;
    END IF;

    -- Calculate fee: fixed + percentage
    v_total_fee := v_fixed_fee + FLOOR(p_amount * v_percentage);

    -- Cap at maximum
    IF v_total_fee > v_max_fee THEN
        RETURN v_max_fee;
    END IF;

    RETURN v_total_fee;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_transfer_fee IS 'BCEAO-compliant fee calculation: FREE ≤5000 XOF, else 100 + 0.5% (max 1000)';
```

---

## Domain Model

### Core Entities

#### 1. CommissionRule.java

```java
package com.payment.commission.domain.entity;

import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;
import com.payment.commission.domain.enums.TransferType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "commission_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false, length = 20)
    private TransferType transferType;

    @Column(name = "min_transaction")
    private Long minTransaction;

    @Column(name = "max_transaction")
    private Long maxTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_level", length = 20)
    private KYCLevel kycLevel;

    // Commission calculation parameters
    @Column(name = "percentage", nullable = false, precision = 5, scale = 4)
    private BigDecimal percentage; // e.g., 0.0050 for 0.5%

    @Column(name = "fixed_amount")
    private Long fixedAmount = 0L; // e.g., 100 XOF

    @Column(name = "min_amount")
    private Long minAmount = 0L;

    @Column(name = "max_amount")
    private Long maxAmount;

    // Rule management
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    // Metadata
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (effectiveFrom == null) {
            effectiveFrom = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business methods
    public boolean isEffective() {
        LocalDateTime now = LocalDateTime.now();
        return isActive &&
               !now.isBefore(effectiveFrom) &&
               (effectiveTo == null || now.isBefore(effectiveTo));
    }

    public boolean matches(Long amount, TransferType type, KYCLevel userKycLevel) {
        if (!isEffective() || transferType != type) {
            return false;
        }

        // Check amount range
        if (minTransaction != null && amount < minTransaction) {
            return false;
        }
        if (maxTransaction != null && amount > maxTransaction) {
            return false;
        }

        // Check KYC level
        if (kycLevel != null && kycLevel != KYCLevel.ANY && kycLevel != userKycLevel) {
            return false;
        }

        return true;
    }

    public Long calculateFee(Long amount) {
        // Calculate percentage fee
        BigDecimal percentageFee = BigDecimal.valueOf(amount)
                .multiply(percentage)
                .setScale(0, java.math.RoundingMode.DOWN);

        // Add fixed amount
        long totalFee = percentageFee.longValue() + (fixedAmount != null ? fixedAmount : 0);

        // Apply minimum
        if (minAmount != null && totalFee < minAmount) {
            totalFee = minAmount;
        }

        // Apply maximum
        if (maxAmount != null && totalFee > maxAmount) {
            totalFee = maxAmount;
        }

        return totalFee;
    }
}
```

#### 2. CommissionTransaction.java

```java
package com.payment.commission.domain.entity;

import com.payment.common.enums.Currency;
import com.payment.commission.domain.enums.CommissionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "commission_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "commission_id")
    private UUID commissionId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Column(name = "amount", nullable = false)
    private Long amount; // Commission amount in XOF

    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(name = "calculation_basis", columnDefinition = "jsonb")
    private String calculationBasis;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CommissionStatus status = CommissionStatus.COMPLETED;

    @Column(name = "settled")
    private Boolean settled = false;

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Business methods
    public void markAsSettled() {
        this.settled = true;
        this.settlementDate = LocalDateTime.now();
    }

    public void markAsRefunded() {
        this.status = CommissionStatus.REFUNDED;
    }
}
```

### Enums

#### TransferType.java

```java
package com.payment.commission.domain.enums;

public enum TransferType {
    SAME_WALLET,      // Same provider (Orange → Orange)
    CROSS_WALLET,     // Different providers (Orange → Wave)
    INTERNATIONAL     // Cross-country transfer
}
```

#### CommissionStatus.java

```java
package com.payment.commission.domain.enums;

public enum CommissionStatus {
    PENDING,      // Commission pending
    COMPLETED,    // Commission collected
    REFUNDED      // Commission refunded (transaction reversed)
}
```

---

## API Specifications

### Base URL

```
http://localhost:8086/api/v1/commissions
```

### Authentication

All endpoints require JWT authentication.

```http
Authorization: Bearer <JWT_TOKEN>
```

### API Endpoints

#### 1. Calculate Fee

**POST** `/calculate`

Calculate transaction fee based on active rules.

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "amount": 50000,
  "currency": "XOF",
  "providerId": "660e8400-e29b-41d4-a716-446655440001",
  "transferType": "SAME_WALLET",
  "kycLevel": "LEVEL_2"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "amount": 50000,
    "currency": "XOF",
    "commissionAmount": 350,
    "calculationDetails": {
      "ruleId": "770e8400-e29b-41d4-a716-446655440002",
      "transferType": "SAME_WALLET",
      "percentageFee": 0.005,
      "percentageAmount": 250,
      "fixedAmount": 100,
      "minAmount": 50,
      "maxAmount": 1000,
      "totalBeforeCap": 350,
      "finalAmount": 350
    }
  }
}
```

**BCEAO Example (Amount ≤ 5,000 XOF):**
```json
{
  "amount": 3000,
  "currency": "XOF",
  "providerId": "660e8400-e29b-41d4-a716-446655440001",
  "transferType": "SAME_WALLET"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "amount": 3000,
    "currency": "XOF",
    "commissionAmount": 0,
    "calculationDetails": {
      "reason": "FREE_THRESHOLD",
      "message": "Transactions ≤ 5,000 XOF are free (BCEAO financial inclusion)"
    }
  }
}
```

---

#### 2. Get Commission Rules

**GET** `/rules`

Get all commission rules (paginated, filterable).

**Query Parameters:**
- `providerId` (optional) - Filter by provider
- `currency` (optional) - Filter by currency
- `transferType` (optional) - Filter by transfer type
- `isActive` (optional, default: true) - Filter by active status
- `page` (default: 0)
- `size` (default: 20)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "ruleId": "770e8400-e29b-41d4-a716-446655440002",
        "providerId": "660e8400-e29b-41d4-a716-446655440001",
        "providerName": "Orange Money Senegal",
        "currency": "XOF",
        "transferType": "SAME_WALLET",
        "percentage": 0.005,
        "fixedAmount": 100,
        "minAmount": 50,
        "maxAmount": 1000,
        "isActive": true,
        "priority": 10,
        "effectiveFrom": "2025-01-01T00:00:00Z",
        "effectiveTo": null,
        "description": "Orange Money same wallet: 0.5% + 100 XOF (max 1000)"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

#### 3. Create Commission Rule

**POST** `/rules`

Create a new commission rule (Admin only).

**Request Body:**
```json
{
  "providerId": "660e8400-e29b-41d4-a716-446655440001",
  "currency": "XOF",
  "transferType": "SAME_WALLET",
  "minTransaction": null,
  "maxTransaction": null,
  "kycLevel": "ANY",
  "percentage": 0.005,
  "fixedAmount": 100,
  "minAmount": 50,
  "maxAmount": 1000,
  "priority": 10,
  "effectiveFrom": "2025-01-01T00:00:00Z",
  "effectiveTo": null,
  "description": "Standard BCEAO fee: 0.5% + 100 XOF, max 1000 XOF"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Règle de commission créée avec succès",
  "data": {
    "ruleId": "770e8400-e29b-41d4-a716-446655440002",
    "providerId": "660e8400-e29b-41d4-a716-446655440001",
    "currency": "XOF",
    "transferType": "SAME_WALLET",
    "percentage": 0.005,
    "fixedAmount": 100,
    "minAmount": 50,
    "maxAmount": 1000,
    "isActive": true,
    "createdAt": "2025-10-19T10:00:00Z"
  }
}
```

---

#### 4. Update Commission Rule

**PUT** `/rules/{ruleId}`

Update an existing commission rule (Admin only).

**Request Body:**
```json
{
  "percentage": 0.0075,
  "fixedAmount": 150,
  "maxAmount": 1500,
  "description": "Updated fee structure",
  "isActive": true
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Règle de commission mise à jour avec succès",
  "data": {
    "ruleId": "770e8400-e29b-41d4-a716-446655440002",
    "percentage": 0.0075,
    "fixedAmount": 150,
    "maxAmount": 1500,
    "updatedAt": "2025-10-19T11:00:00Z"
  }
}
```

---

#### 5. Deactivate Commission Rule

**DELETE** `/rules/{ruleId}`

Deactivate a commission rule (soft delete).

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Règle de commission désactivée avec succès"
}
```

---

#### 6. Get Revenue Report

**GET** `/revenue`

Get revenue report by provider, period, currency.

**Query Parameters:**
- `providerId` (optional)
- `currency` (optional)
- `startDate` (required) - ISO 8601 date
- `endDate` (required) - ISO 8601 date
- `groupBy` (optional: PROVIDER, CURRENCY, DAY, MONTH) - default: PROVIDER

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "reportPeriod": {
      "startDate": "2025-10-01",
      "endDate": "2025-10-31"
    },
    "totalRevenue": 2500000,
    "currency": "XOF",
    "transactionCount": 1500,
    "averageCommission": 1666,
    "settledAmount": 2000000,
    "unsettledAmount": 500000,
    "breakdown": [
      {
        "providerId": "660e8400-e29b-41d4-a716-446655440001",
        "providerName": "Orange Money Senegal",
        "revenue": 1500000,
        "transactionCount": 900,
        "averageCommission": 1666,
        "settled": 1200000,
        "unsettled": 300000
      },
      {
        "providerId": "aa0e8400-e29b-41d4-a716-446655440005",
        "providerName": "Wave Senegal",
        "revenue": 1000000,
        "transactionCount": 600,
        "averageCommission": 1666,
        "settled": 800000,
        "unsettled": 200000
      }
    ]
  }
}
```

---

#### 7. Record Commission (Internal)

**POST** `/record`

Record commission for a completed transaction (called internally by Transaction Service or via event).

**Request Body:**
```json
{
  "transactionId": "880e8400-e29b-41d4-a716-446655440003",
  "ruleId": "770e8400-e29b-41d4-a716-446655440002",
  "providerId": "660e8400-e29b-41d4-a716-446655440001",
  "amount": 350,
  "currency": "XOF",
  "calculationBasis": {
    "percentageFee": 0.005,
    "percentageAmount": 250,
    "fixedAmount": 100,
    "finalAmount": 350
  }
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Commission enregistrée avec succès",
  "data": {
    "commissionId": "990e8400-e29b-41d4-a716-446655440004",
    "transactionId": "880e8400-e29b-41d4-a716-446655440003",
    "amount": 350,
    "status": "COMPLETED",
    "createdAt": "2025-10-19T10:30:00Z"
  }
}
```

---

## Service Layer

### Core Services

#### 1. CommissionService

Main service interface.

```java
package com.payment.commission.service;

import com.payment.commission.dto.request.*;
import com.payment.commission.dto.response.*;
import com.payment.commission.domain.enums.TransferType;
import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;

import java.util.UUID;

public interface CommissionService {

    // Fee calculation
    FeeCalculationResponse calculateFee(CalculateFeeRequest request);
    Long calculateFee(Long amount, Currency currency, UUID providerId, TransferType transferType, KYCLevel kycLevel);

    // Commission recording
    void recordCommission(UUID transactionId, UUID ruleId, UUID providerId, Long amount, Currency currency, String calculationBasis);

    // BCEAO-compliant fee calculation
    Long calculateBCEAOFee(Long amount);
}
```

#### 2. FeeCalculationEngine

Core fee calculation logic.

```java
package com.payment.commission.service;

import com.payment.commission.domain.entity.CommissionRule;
import com.payment.commission.domain.enums.TransferType;
import com.payment.commission.repository.CommissionRuleRepository;
import com.payment.commission.exception.NoMatchingRuleException;
import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FeeCalculationEngine {

    private final CommissionRuleRepository commissionRuleRepository;

    private static final Long BCEAO_FREE_THRESHOLD = 5000L; // XOF
    private static final Long BCEAO_FIXED_FEE = 100L;       // XOF
    private static final Double BCEAO_PERCENTAGE = 0.005;   // 0.5%
    private static final Long BCEAO_MAX_FEE = 1000L;        // XOF

    /**
     * Calculate fee using BCEAO rules
     */
    public Long calculateBCEAOFee(Long amount) {
        // Financial inclusion: FREE for amounts ≤ 5,000 XOF
        if (amount <= BCEAO_FREE_THRESHOLD) {
            log.info("Amount {} XOF ≤ {} XOF: FREE (BCEAO financial inclusion)", amount, BCEAO_FREE_THRESHOLD);
            return 0L;
        }

        // Calculate: 100 XOF + 0.5% of amount
        Long percentageFee = Math.round(amount * BCEAO_PERCENTAGE);
        Long totalFee = BCEAO_FIXED_FEE + percentageFee;

        // Cap at 1,000 XOF
        Long finalFee = Math.min(totalFee, BCEAO_MAX_FEE);

        log.info("BCEAO fee for {} XOF: {} (fixed: {}, percentage: {}, capped at: {})",
                 amount, finalFee, BCEAO_FIXED_FEE, percentageFee, BCEAO_MAX_FEE);

        return finalFee;
    }

    /**
     * Calculate fee using custom commission rules
     */
    @Cacheable(value = "commission-calculation", key = "#amount + '-' + #providerId + '-' + #transferType")
    public Long calculateFee(Long amount, Currency currency, UUID providerId, TransferType transferType, KYCLevel kycLevel) {
        log.info("Calculating fee: amount={}, currency={}, provider={}, type={}, kycLevel={}",
                 amount, currency, providerId, transferType, kycLevel);

        // Get active rules for this provider, currency, and transfer type, ordered by priority
        List<CommissionRule> rules = commissionRuleRepository.findActiveRulesByProviderAndCurrencyAndType(
            providerId, currency, transferType
        );

        if (rules.isEmpty()) {
            log.warn("No commission rules found for provider {} and transfer type {}, using BCEAO default",
                     providerId, transferType);
            return calculateBCEAOFee(amount);
        }

        // Find first matching rule
        CommissionRule matchingRule = rules.stream()
                .filter(rule -> rule.matches(amount, transferType, kycLevel))
                .findFirst()
                .orElseThrow(() -> new NoMatchingRuleException(
                    "Aucune règle de commission ne correspond aux critères"
                ));

        Long fee = matchingRule.calculateFee(amount);
        log.info("Fee calculated using rule {}: {} XOF", matchingRule.getRuleId(), fee);

        return fee;
    }

    /**
     * Get matching rule for transaction
     */
    public CommissionRule findMatchingRule(Long amount, Currency currency, UUID providerId,
                                          TransferType transferType, KYCLevel kycLevel) {
        List<CommissionRule> rules = commissionRuleRepository.findActiveRulesByProviderAndCurrencyAndType(
            providerId, currency, transferType
        );

        return rules.stream()
                .filter(rule -> rule.matches(amount, transferType, kycLevel))
                .findFirst()
                .orElse(null);
    }
}
```

#### 3. CommissionServiceImpl

```java
package com.payment.commission.service;

import com.payment.commission.domain.entity.CommissionRule;
import com.payment.commission.domain.entity.CommissionTransaction;
import com.payment.commission.domain.enums.CommissionStatus;
import com.payment.commission.domain.enums.TransferType;
import com.payment.commission.dto.request.CalculateFeeRequest;
import com.payment.commission.dto.response.FeeCalculationResponse;
import com.payment.commission.repository.CommissionTransactionRepository;
import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommissionServiceImpl implements CommissionService {

    private final FeeCalculationEngine feeCalculationEngine;
    private final CommissionTransactionRepository commissionTransactionRepository;
    private final CommissionEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public FeeCalculationResponse calculateFee(CalculateFeeRequest request) {
        log.info("Calculating fee for request: {}", request);

        Long feeAmount = feeCalculationEngine.calculateFee(
            request.getAmount(),
            request.getCurrency(),
            request.getProviderId(),
            request.getTransferType(),
            request.getKycLevel()
        );

        CommissionRule matchingRule = feeCalculationEngine.findMatchingRule(
            request.getAmount(),
            request.getCurrency(),
            request.getProviderId(),
            request.getTransferType(),
            request.getKycLevel()
        );

        return FeeCalculationResponse.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .commissionAmount(feeAmount)
                .ruleId(matchingRule != null ? matchingRule.getRuleId() : null)
                .transferType(request.getTransferType())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Long calculateFee(Long amount, Currency currency, UUID providerId,
                            TransferType transferType, KYCLevel kycLevel) {
        return feeCalculationEngine.calculateFee(amount, currency, providerId, transferType, kycLevel);
    }

    @Override
    public void recordCommission(UUID transactionId, UUID ruleId, UUID providerId,
                                Long amount, Currency currency, String calculationBasis) {
        log.info("Recording commission: transaction={}, amount={} {}", transactionId, amount, currency);

        CommissionTransaction commission = CommissionTransaction.builder()
                .transactionId(transactionId)
                .ruleId(ruleId)
                .providerId(providerId)
                .amount(amount)
                .currency(currency)
                .calculationBasis(calculationBasis)
                .status(CommissionStatus.COMPLETED)
                .settled(false)
                .build();

        commissionTransactionRepository.save(commission);

        // Publish event
        eventPublisher.publishCommissionCollected(commission);

        log.info("Commission recorded successfully: {}", commission.getCommissionId());
    }

    @Override
    @Transactional(readOnly = true)
    public Long calculateBCEAOFee(Long amount) {
        return feeCalculationEngine.calculateBCEAOFee(amount);
    }
}
```

---

## Event Publishing

### Commission Events

#### 1. CommissionCollectedEvent

```java
package com.payment.commission.event;

import com.payment.kafka.event.BaseEvent;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionCollectedEvent extends BaseEvent {
    private UUID commissionId;
    private UUID transactionId;
    private UUID providerId;
    private Long amount;
    private String currency;
    private String calculationBasis;
}
```

#### 2. CommissionEventPublisher

```java
package com.payment.commission.service;

import com.payment.kafka.KafkaTopics;
import com.payment.kafka.publisher.EventPublisher;
import com.payment.commission.domain.entity.CommissionTransaction;
import com.payment.commission.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionEventPublisher {

    private final EventPublisher eventPublisher;

    public void publishCommissionCollected(CommissionTransaction commission) {
        CommissionCollectedEvent event = CommissionCollectedEvent.builder()
                .commissionId(commission.getCommissionId())
                .transactionId(commission.getTransactionId())
                .providerId(commission.getProviderId())
                .amount(commission.getAmount())
                .currency(commission.getCurrency().name())
                .calculationBasis(commission.getCalculationBasis())
                .build();

        eventPublisher.publish(KafkaTopics.COMMISSION_EVENTS, event);
        log.info("Published COMMISSION_COLLECTED event for commission: {}", commission.getCommissionId());
    }
}
```

---

## Configuration

### application.yml

```yaml
spring:
  application:
    name: payment-commission-service

  datasource:
    url: jdbc:postgresql://localhost:5432/commission_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms

server:
  port: 8086

# BCEAO Fee Configuration
commission:
  bceao:
    free-threshold: 5000       # XOF
    fixed-fee: 100             # XOF
    percentage-fee: 0.005      # 0.5%
    max-fee: 1000              # XOF

# Caching
cache:
  commission-rules:
    ttl-seconds: 3600  # 1 hour

# Logging
logging:
  level:
    com.payment.commission: DEBUG
    org.hibernate.SQL: DEBUG
```

---

## Testing Strategy

### Unit Tests

#### FeeCalculationEngineTest.java

```java
@ExtendWith(MockitoExtension.class)
class FeeCalculationEngineTest {

    @Mock
    private CommissionRuleRepository commissionRuleRepository;

    @InjectMocks
    private FeeCalculationEngine feeCalculationEngine;

    @Test
    void testCalculateBCEAOFee_FreeThreshold() {
        // Amount ≤ 5,000 XOF should be FREE
        Long fee = feeCalculationEngine.calculateBCEAOFee(3000L);
        assertThat(fee).isEqualTo(0L);
    }

    @Test
    void testCalculateBCEAOFee_StandardFee() {
        // Amount: 50,000 XOF
        // Expected: 100 + (50000 × 0.005) = 100 + 250 = 350 XOF
        Long fee = feeCalculationEngine.calculateBCEAOFee(50000L);
        assertThat(fee).isEqualTo(350L);
    }

    @Test
    void testCalculateBCEAOFee_MaxCap() {
        // Amount: 500,000 XOF
        // Calculated: 100 + (500000 × 0.005) = 100 + 2500 = 2600 XOF
        // Expected: 1000 XOF (capped)
        Long fee = feeCalculationEngine.calculateBCEAOFee(500000L);
        assertThat(fee).isEqualTo(1000L);
    }
}
```

---

## Implementation Checklist

### Phase 1: Setup (Week 1)
- [ ] Create Git repository
- [ ] Initialize Gradle project with Spring Boot 3.2.0
- [ ] Add dependencies
- [ ] Configure application.yml
- [ ] Create database: `commission_db`

### Phase 2: Database & Domain (Week 1)
- [ ] Create Flyway migrations
- [ ] Implement domain entities
- [ ] Create repositories
- [ ] Test BCEAO fee calculation function

### Phase 3: Core Services (Week 2)
- [ ] Implement FeeCalculationEngine
- [ ] Implement CommissionService
- [ ] Implement CommissionRuleService
- [ ] Write unit tests (>80% coverage)

### Phase 4: API Layer (Week 3)
- [ ] Implement controllers
- [ ] Add Swagger documentation
- [ ] Test all endpoints
- [ ] Add request validation

### Phase 5: Events & Testing (Week 4)
- [ ] Implement event publishing
- [ ] Write integration tests
- [ ] Performance testing
- [ ] Deploy to staging

---

## Summary

This implementation guide provides:

- ✅ **Complete BCEAO fee calculation** - FREE ≤5,000 XOF, 100 + 0.5% (max 1,000 XOF)
- ✅ **Flexible rule engine** - Support multiple commission rules per provider
- ✅ **Revenue tracking** - Track all platform commissions
- ✅ **7 API endpoints** - Calculate, manage rules, track revenue
- ✅ **4-week implementation plan** - Detailed checklist

**Implementation Time**: **4 weeks**
**Team Size**: 1-2 developers

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-19
**Maintained By**: Technical Architecture Team
