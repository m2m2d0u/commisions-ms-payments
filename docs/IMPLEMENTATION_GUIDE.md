# Commission Service - Implementation Guide

**Service**: payment-commission-service
**Version**: 1.0.0
**Language**: Java 17
**Framework**: Spring Boot 3.2.0
**Build Tool**: Gradle 8.5+
**Purpose**: Transaction fee calculation, commission rule management, revenue tracking, and settlement
**Last Updated**: 2025-10-21

---

## Table of Contents

1. [Overview](#overview)
2. [Service Architecture](#service-architecture)
3. [Core Workflows](#core-workflows)
4. [Business Rules](#business-rules)
5. [Validation Rules](#validation-rules)
6. [API Workflow](#api-workflow)
7. [Event Workflows](#event-workflows)
8. [Implementation Checklist](#implementation-checklist)

---

## Overview

The **Commission Service** is responsible for calculating and tracking all transaction fees on the platform. It implements BCEAO-compliant fee rules and manages platform revenue.

### Core Responsibilities

- ✅ **Fee Calculation** - Calculate transaction fees based on commission rules
- ✅ **Commission Rules Management** - Manage fee rules as a centralized wallet by currency and transfer type
- ✅ **Revenue Tracking** - Track platform commission revenue per transaction
- ✅ **Settlement Management** - Track settlement status
- ✅ **Revenue Reporting** - Generate revenue reports by period and currency
- ✅ **Rule Versioning** - Support time-based rule activation/expiration
- ✅ **Multi-Currency Support** - Handle XOF and XAF commissions

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
│ Commission      │ │ Commission    │ │ Fee              │
│ Rule Repo       │ │ Transaction   │ │ Calculation      │
│                 │ │ Repo          │ │ Engine           │
└─────────────────┘ └───────────────┘ └──────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL Database                       │
│                    (commission_db)                           │
└─────────────────────────────────────────────────────────────┘
```

### Integration Points

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

## Core Workflows

### 1. Fee Calculation Workflow (Rule-Based)

**Trigger**: Transaction Service calls `/api/v1/commissions/calculate`

**Request Requirements**:
- `ruleId` (UUID) - **MANDATORY** - The specific commission rule to use
- `amount` (Long) - Transaction amount
- `currency` (String) - XOF or XAF
- `transferType` (String) - SAME_WALLET, CROSS_WALLET, or INTERNATIONAL
- `kycLevel` (String) - LEVEL_1, LEVEL_2, LEVEL_3, or ANY (optional)

**Workflow Steps**:

1. **Receive Request**
   - Validate request body (ruleId, amount, currency, transferType)
   - Extract parameters

2. **Retrieve Commission Rule**
   - Fetch rule by `ruleId` from database
   - If not found → throw `RuleNotFoundException`

3. **Validate Rule Status**
   - Check if rule `isActive = true`
   - If not active → throw `RuleNotFoundException` with message "Rule is not active"

4. **Validate Rule Effective Date**
   - Check if current date is within `effectiveFrom` and `effectiveTo` range
   - If not effective → throw `RuleNotFoundException` with message "Rule is not effective for current date"

5. **Validate Transaction Amount Range** ⭐ NEW
   - Check if `amount >= rule.minAmount` (if minAmount is set)
   - If below minimum → throw `NoMatchingRuleException` with message and minimum value
   - Check if `amount <= rule.maxAmount` (if maxAmount is set)
   - If above maximum → throw `NoMatchingRuleException` with message and maximum value

6. **Calculate Fee**
   - Apply percentage: `percentageFee = amount × rule.percentage`
   - Add fixed amount: `totalFee = percentageFee + rule.fixedAmount`
   - Apply minimum cap: `if totalFee < rule.minAmount then totalFee = rule.minAmount`
   - Apply maximum cap: `if totalFee > rule.maxAmount then totalFee = rule.maxAmount`

7. **Build Response**
   - Return `FeeCalculationResponse` with:
     - Transaction amount
     - Currency
     - Commission amount
     - Rule ID
     - Transfer type
     - Calculation details (percentage, fixed amount, min/max, etc.)

8. **Return Success Response**
   - HTTP 200 OK with fee calculation details

**Error Scenarios**:
- Missing `ruleId` → HTTP 400 Bad Request
- Rule not found → HTTP 404 Not Found
- Rule not active → HTTP 404 Not Found
- Rule not effective → HTTP 404 Not Found
- Amount below minimum → HTTP 400 Bad Request
- Amount above maximum → HTTP 400 Bad Request
- Invalid currency/transferType → HTTP 400 Bad Request

---

### 2. BCEAO Fee Calculation Workflow (Legacy)

**Trigger**: Called when no custom rules exist (fallback)

**BCEAO Rules**:
1. Amount ≤ 5,000 XOF → **FREE** (financial inclusion mandate)
2. Amount > 5,000 XOF → **100 XOF + 0.5% of amount**, capped at **1,000 XOF**

**Workflow Steps**:

1. **Check Free Threshold**
   - If `amount ≤ 5,000 XOF` → return fee = 0

2. **Calculate Standard Fee**
   - `percentageFee = amount × 0.005` (0.5%)
   - `totalFee = 100 + percentageFee`

3. **Apply Maximum Cap**
   - `finalFee = min(totalFee, 1000)`

4. **Return Fee**
   - Return calculated fee amount

---

### 3. Commission Recording Workflow

**Trigger**: Transaction completed event or direct API call

**Workflow Steps**:

1. **Receive Transaction Details**
   - Transaction ID
   - Rule ID used for calculation
   - Commission amount
   - Currency
   - Calculation basis (JSON with details)

2. **Create Commission Record**
   - Generate commission ID (UUID)
   - Set status = COMPLETED
   - Set settled = false
   - Record timestamp

3. **Save to Database**
   - Insert into `commission_transactions` table

4. **Publish Event**
   - Publish `COMMISSION_COLLECTED` event to Kafka
   - Include commission ID, transaction ID, amount, currency

5. **Log Success**
   - Log commission recorded successfully

---

### 4. Commission Rule Management Workflow

#### Create Rule Workflow

1. **Receive Create Request** (Admin only)
   - Validate all required fields
   - Validate percentage (0 ≤ percentage ≤ 1.0)
   - Validate amounts (min ≥ 0, max > min if both set)

2. **Business Validation**
   - Check effective dates (effectiveTo > effectiveFrom)
   - Validate currency (XOF or XAF)
   - Validate transfer type

3. **Create Rule Entity**
   - Generate rule ID
   - Set timestamps
   - Set created_by (from JWT)

4. **Save to Database**
   - Insert into `commission_rules` table

5. **Return Success**
   - HTTP 201 Created with rule details

#### Update Rule Workflow

1. **Receive Update Request** (Admin only)
   - Rule ID in path
   - Fields to update in body

2. **Retrieve Existing Rule**
   - Fetch by rule ID
   - If not found → HTTP 404

3. **Apply Updates**
   - Update allowed fields only
   - Update `updated_at` timestamp

4. **Validate Updated Rule**
   - Re-validate business rules
   - Check constraints

5. **Save Changes**
   - Update database

6. **Return Success**
   - HTTP 200 OK with updated rule

#### Deactivate Rule Workflow

1. **Receive Deactivate Request** (Admin only)
   - Rule ID in path

2. **Retrieve Rule**
   - Fetch by rule ID
   - If not found → HTTP 404

3. **Soft Delete**
   - Set `isActive = false`
   - Update timestamp

4. **Save Changes**
   - Update database

5. **Return Success**
   - HTTP 200 OK

---

### 5. Revenue Reporting Workflow

**Trigger**: Admin requests revenue report

**Workflow Steps**:

1. **Receive Report Request**
   - Start date (required)
   - End date (required)
   - Currency (optional filter)
   - Group by (CURRENCY, DAY, MONTH)

2. **Validate Date Range**
   - Ensure endDate > startDate
   - Check date range not too large (e.g., max 1 year)

3. **Query Database**
   - Filter by date range
   - Filter by currency if provided
   - Filter by status = COMPLETED
   - Group by requested dimension

4. **Calculate Metrics**
   - Total revenue (sum of amounts)
   - Transaction count
   - Average commission
   - Settled amount
   - Unsettled amount

5. **Build Report Response**
   - Report period
   - Total metrics
   - Breakdown by group

6. **Return Report**
   - HTTP 200 OK with report data

---

### 6. Commission Refund Workflow

**Trigger**: Transaction reversed event

**Workflow Steps**:

1. **Receive Refund Event**
   - Transaction ID to refund

2. **Find Commission Record**
   - Query by transaction ID
   - If not found → log warning and return

3. **Update Commission Status**
   - Set status = REFUNDED
   - Keep original amount for audit

4. **Save Changes**
   - Update database

5. **Publish Event**
   - Publish `COMMISSION_REFUNDED` event to Kafka

6. **Log Success**
   - Log commission refunded

---

## Business Rules

### 1. BCEAO Fee Structure

**Financial Inclusion Rule**:
- Transfers ≤ 5,000 XOF: **FREE**
- Purpose: Promote financial inclusion for small transactions

**Standard Fee Rule**:
- Transfers > 5,000 XOF: **100 XOF fixed + 0.5% of amount**
- Maximum cap: **1,000 XOF**
- Formula: `min(100 + (amount × 0.005), 1000)`

### 2. Commission Types

- **SAME_WALLET**: Same provider (e.g., Orange Money → Orange Money)
- **CROSS_WALLET**: Different providers (e.g., Orange Money → Wave)
- **INTERNATIONAL**: Cross-country transfers

### 3. Rule Priority System

- Multiple rules can exist for the same currency and transfer type
- Higher priority rules are evaluated first
- First matching rule is applied

### 4. Commission Components

- **Percentage Fee**: % of transaction amount (0-100%)
- **Fixed Fee**: Flat amount added to percentage (e.g., 100 XOF)
- **Minimum Fee**: Floor value (can't charge less)
- **Maximum Fee**: Ceiling value (can't charge more)

### 5. Rule Effectiveness

Rules have time-based activation:
- `effectiveFrom`: Start date/time (required)
- `effectiveTo`: End date/time (optional, NULL = no expiry)
- Rules are only applied if current time is within this range

---

## Validation Rules

### Request Validation

#### CalculateFeeRequest
- `ruleId`: Required, must be valid UUID
- `amount`: Required, must be > 0
- `currency`: Required, must be XOF or XAF
- `transferType`: Required, must be SAME_WALLET, CROSS_WALLET, or INTERNATIONAL
- `kycLevel`: Optional, if provided must be LEVEL_1, LEVEL_2, LEVEL_3, or ANY

#### CreateRuleRequest
- `currency`: Required, must be XOF or XAF
- `transferType`: Required
- `percentage`: Required, must be 0 ≤ percentage ≤ 1.0
- `fixedAmount`: Optional, must be ≥ 0 if provided
- `minAmount`: Optional, must be ≥ 0 if provided
- `maxAmount`: Optional, must be > minAmount if both provided
- `minTransaction`: Optional, must be ≥ 0 if provided
- `maxTransaction`: Optional, must be > minTransaction if both provided
- `effectiveFrom`: Required
- `effectiveTo`: Optional, must be > effectiveFrom if provided
- `priority`: Optional, must be ≥ 0
- `description`: Optional, max 500 characters

### Business Validation

#### Rule-Based Fee Calculation Validation (⭐ NEW)

**Step 1: Rule Existence**
- Rule with provided `ruleId` must exist
- Error: "Commission rule not found: {ruleId}"

**Step 2: Rule Active Status**
- `rule.isActive` must be `true`
- Error: "Commission rule is not active: {ruleId}"

**Step 3: Rule Effective Date Range**
- Current timestamp must be ≥ `rule.effectiveFrom`
- Current timestamp must be < `rule.effectiveTo` (if effectiveTo is not NULL)
- Error: "Commission rule is not effective for the current date: {ruleId}"

**Step 4: Transaction Amount Range Validation** ⭐ NEW
- **Minimum Amount Check**:
  - If `rule.minAmount` is not NULL
  - Transaction `amount` must be ≥ `rule.minAmount`
  - Error: "Transaction amount is below the minimum allowed for this rule (Min: {minAmount} {currency})"

- **Maximum Amount Check**:
  - If `rule.maxAmount` is not NULL
  - Transaction `amount` must be ≤ `rule.maxAmount`
  - Error: "Transaction amount exceeds the maximum allowed for this rule (Max: {maxAmount} {currency})"

**Step 5: Fee Calculation**
- After validation passes, calculate fee using rule's formula
- Apply percentage and fixed amounts
- Apply min/max caps on the calculated fee (not on transaction amount)

### Response Validation

#### FeeCalculationResponse
- `amount`: Required, must be > 0
- `currency`: Required
- `commissionAmount`: Required, must be ≥ 0
- `transferType`: Required

### Database Constraints

#### commission_rules table
- `percentage`: CHECK (percentage >= 0 AND percentage <= 1)
- `fixed_amount`: DEFAULT 0
- `min_amount`: DEFAULT 0, CHECK (min_amount >= 0)
- `max_amount`: CHECK (max_amount IS NULL OR max_amount >= min_amount)
- `min_transaction`: CHECK (min_transaction >= 0 OR min_transaction IS NULL)
- `max_transaction`: CHECK (max_transaction >= min_transaction OR max_transaction IS NULL)
- `is_active`: DEFAULT TRUE
- `priority`: DEFAULT 0
- UNIQUE(currency, transfer_type, priority, effective_from)

#### commission_transactions table
- `amount`: NOT NULL, CHECK (amount >= 0)
- `status`: CHECK (status IN ('PENDING', 'COMPLETED', 'REFUNDED'))
- `settled`: DEFAULT FALSE

---

## API Workflow

### 1. Calculate Fee (Rule-Based)

**Endpoint**: `POST /api/v1/commissions/calculate`

**Flow**:
```
Client → Controller → CommissionService → FeeCalculationEngine
                                              ↓
                                    Get Rule by ruleId
                                              ↓
                                    Validate: Rule exists?
                                              ↓
                                    Validate: Rule active?
                                              ↓
                                    Validate: Rule effective?
                                              ↓
                                    Validate: Amount >= minAmount? ⭐
                                              ↓
                                    Validate: Amount <= maxAmount? ⭐
                                              ↓
                                    Calculate fee using rule
                                              ↓
                                    Build FeeCalculationResponse
                                              ↓
Client ← Controller ← CommissionService ← Return response
```

### 2. Get Rules by Currency

**Endpoint**: `GET /api/v1/commissions/rules/currency/{currency}`

**Flow**:
```
Client → Controller → CommissionRuleService → Repository
                                                  ↓
                                    Query rules by currency
                                                  ↓
                                    Map to response DTOs
                                                  ↓
Client ← Controller ← CommissionRuleService ← Return rules
```

### 3. Get Active Rules by Currency

**Endpoint**: `GET /api/v1/commissions/rules/currency/{currency}/active`

**Flow**:
```
Client → Controller → CommissionRuleService → Repository
                                                  ↓
                        Query active rules by currency (isActive=true)
                                                  ↓
                                Filter by effective date range
                                                  ↓
                                    Map to response DTOs
                                                  ↓
Client ← Controller ← CommissionRuleService ← Return active rules
```

### 4. Create Commission Rule

**Endpoint**: `POST /api/v1/commissions/rules`

**Flow**:
```
Admin → Controller → Validate request → CommissionRuleService
                                              ↓
                                    Validate business rules
                                              ↓
                                    Create rule entity
                                              ↓
                                    Save to database
                                              ↓
                                    Map to response DTO
                                              ↓
Admin ← Controller ← Return created rule (201 Created)
```

### 5. Record Commission (Internal)

**Endpoint**: `POST /api/v1/commissions/record`

**Flow**:
```
Transaction Service → Controller → CommissionService
                                        ↓
                            Create CommissionTransaction entity
                                        ↓
                            Save to database
                                        ↓
                            Publish COMMISSION_COLLECTED event
                                        ↓
Transaction Service ← Controller ← Return success
```

### 6. Get Revenue Report

**Endpoint**: `GET /api/v1/commissions/revenue`

**Flow**:
```
Admin → Controller → Validate date range → RevenueTrackingService
                                                    ↓
                                        Query commissions in range
                                                    ↓
                                        Calculate metrics
                                                    ↓
                                        Group by dimension
                                                    ↓
                                        Build report response
                                                    ↓
Admin ← Controller ← Return revenue report
```

---

## Event Workflows

### 1. Commission Collected Event

**Trigger**: Commission recorded in database

**Flow**:
```
CommissionService.recordCommission()
        ↓
Save commission to database
        ↓
CommissionEventPublisher.publishCommissionCollected()
        ↓
Create CommissionCollectedEvent
        ↓
Publish to Kafka topic: COMMISSION_EVENTS
        ↓
[Other services consume event]
```

**Event Payload**:
- `commissionId` (UUID)
- `transactionId` (UUID)
- `amount` (Long)
- `currency` (String)
- `calculationBasis` (JSON String)
- `timestamp` (ISO 8601)

### 2. Commission Refunded Event

**Trigger**: Transaction reversed

**Flow**:
```
TransactionEventListener.onTransactionReversed()
        ↓
Find commission by transactionId
        ↓
Update status to REFUNDED
        ↓
Save to database
        ↓
CommissionEventPublisher.publishCommissionRefunded()
        ↓
Create CommissionRefundedEvent
        ↓
Publish to Kafka topic: COMMISSION_EVENTS
        ↓
[Other services consume event]
```

**Event Payload**:
- `commissionId` (UUID)
- `transactionId` (UUID)
- `originalAmount` (Long)
- `currency` (String)
- `refundedAt` (ISO 8601)

### 3. Transaction Completed Event (Consumed)

**Trigger**: Transaction Service publishes TRANSACTION_COMPLETED

**Flow**:
```
Kafka Consumer receives TRANSACTION_COMPLETED event
        ↓
TransactionEventListener.onTransactionCompleted()
        ↓
Extract transaction details (ID, amount, currency, ruleId)
        ↓
CommissionService.recordCommission()
        ↓
Save commission record
        ↓
Publish COMMISSION_COLLECTED event
```

---

## Implementation Checklist

### Phase 1: Setup (Week 1)
- [ ] Create Git repository
- [ ] Initialize Gradle project with Spring Boot 3.2.0
- [ ] Add dependencies (Spring Data JPA, PostgreSQL, Flyway, Kafka, Redis)
- [ ] Configure application.yml (database, Kafka, Redis)
- [ ] Create database: `commission_db`
- [ ] Set up Docker Compose for local development

### Phase 2: Database & Domain (Week 1)
- [ ] Create Flyway migration V1: commission_rules table
- [ ] Create Flyway migration V2: commission_transactions table
- [ ] Create Flyway migration V3: insert default BCEAO rules
- [ ] Create Flyway migration V4: add indexes
- [ ] Implement CommissionRule entity with business methods
- [ ] Implement CommissionTransaction entity
- [ ] Create enums: TransferType, CommissionStatus
- [ ] Create repositories with custom queries

### Phase 3: Core Services (Week 2)
- [ ] Implement FeeCalculationEngine
  - [ ] calculateBCEAOFee() method
  - [ ] calculateFeeByRuleId() method with all validations ⭐
  - [ ] getRuleById() method
- [ ] Implement CommissionService
  - [ ] calculateFee() method (rule-based)
  - [ ] recordCommission() method
  - [ ] refundCommission() method
- [ ] Implement CommissionRuleService
  - [ ] createRule() method
  - [ ] updateRule() method
  - [ ] deactivateRule() method
  - [ ] getRulesByCurrency() method
  - [ ] getActiveRulesByCurrency() method
- [ ] Write unit tests (>80% coverage)

### Phase 4: API Layer (Week 3)
- [ ] Implement CommissionController
  - [ ] POST /calculate endpoint
  - [ ] POST /record endpoint
- [ ] Implement CommissionRuleController
  - [ ] GET /rules endpoint
  - [ ] GET /rules/currency/{currency} endpoint
  - [ ] GET /rules/currency/{currency}/active endpoint
  - [ ] POST /rules endpoint
  - [ ] PUT /rules/{ruleId} endpoint
  - [ ] DELETE /rules/{ruleId} endpoint
- [ ] Implement RevenueReportController
  - [ ] GET /revenue endpoint
- [ ] Add GlobalExceptionHandler
- [ ] Add Swagger/OpenAPI documentation
- [ ] Add request/response validation
- [ ] Test all endpoints with Postman

### Phase 5: Events & Messaging (Week 3)
- [ ] Implement CommissionEventPublisher
  - [ ] publishCommissionCollected()
  - [ ] publishCommissionRefunded()
- [ ] Implement TransactionEventListener
  - [ ] onTransactionCompleted()
  - [ ] onTransactionReversed()
- [ ] Configure Kafka topics
- [ ] Test event publishing and consumption

### Phase 6: Validation & Security (Week 4)
- [ ] Add Jakarta Bean Validation annotations
- [ ] Implement custom validators
- [ ] Add i18n support (French & English messages) ✅
- [ ] Configure Spring Security
- [ ] Add JWT authentication
- [ ] Add role-based access control (Admin vs User)

### Phase 7: Testing & Deployment (Week 4)
- [ ] Write integration tests with Testcontainers
- [ ] Write API tests with REST Assured
- [ ] Performance testing
- [ ] Load testing (fee calculation under high load)
- [ ] Create Dockerfile
- [ ] Deploy to staging environment
- [ ] Smoke tests in staging
- [ ] Deploy to production

---

## Summary

This implementation guide provides:

- ✅ **Rule-based fee calculation** - Explicit ruleId-based calculation with validations
- ✅ **Amount range validation** - Validate transaction amount against rule min/max ⭐
- ✅ **Flexible rule engine** - Support multiple commission rules per currency/type
- ✅ **Revenue tracking** - Track all platform commissions
- ✅ **7 API endpoints** - Calculate fees, manage rules, track revenue
- ✅ **BCEAO compliance** - FREE ≤5,000 XOF, 100 + 0.5% (max 1,000 XOF) as fallback
- ✅ **4-week implementation plan** - Detailed checklist

**Implementation Time**: **4 weeks**
**Team Size**: 1-2 developers

---

**Document Version**: 2.0.0
**Last Updated**: 2025-10-21
**Maintained By**: Technical Architecture Team

## Changelog

### Version 2.0.0 (2025-10-21)
- **MAJOR CHANGE**: Converted guide to workflow-focused format
- Removed all code snippets and examples
- Added comprehensive validation rules section
- Added transaction amount range validation (minAmount/maxAmount) ⭐
- Enhanced fee calculation workflow with step-by-step validation
- Added detailed API workflow diagrams
- Added event workflow documentation
- Restructured for better focus on business processes
- Added rule-based fee calculation workflow
- Mandatory ruleId in fee calculation requests

### Version 1.1.0 (2025-10-20)
- Removed `providerId` from commission rules - rules now function as a centralized wallet
- Updated database schema to remove `provider_id` column from both tables
- Modified unique constraints and indexes to reflect wallet-based architecture
- Updated all API endpoints and DTOs to remove provider-specific filtering
- Changed revenue reporting to group by currency instead of provider
- Simplified commission rule management to be currency and transfer-type based

### Version 1.0.0 (2025-10-19)
- Initial implementation guide with complete code examples
