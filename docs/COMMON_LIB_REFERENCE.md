# Payment Common Library - Complete Code Reference

**Library**: payment-common-lib
**Version**: 1.0.0
**Language**: Java 17
**Purpose**: Shared utilities, DTOs, exceptions, and constants for BCEAO-compliant payment system
**Last Updated**: 2025-10-18

---

## Table of Contents

1. [Overview](#overview)
2. [Package Structure](#package-structure)
3. [Core Value Objects](#core-value-objects)
4. [Enumerations](#enumerations)
5. [Data Transfer Objects (DTOs)](#data-transfer-objects-dtos)
6. [Exceptions](#exceptions)
7. [Utility Classes](#utility-classes)
8. [Validation Annotations](#validation-annotations)
9. [Constants](#constants)
10. [Resource Files](#resource-files)
11. [Complete Code Listings](#complete-code-listings)

---

## Overview

The `payment-common-lib` is the **foundational library** for the BCEAO-compliant payment system. It provides:

- ✅ **Currency handling** - XOF/XAF (no decimals)
- ✅ **Money value object** - Immutable, type-safe money operations
- ✅ **Phone number utilities** - Validation for 14 WAEMU/CEMAC countries
- ✅ **Common exceptions** - French error messages (BCEAO requirement)
- ✅ **Standard DTOs** - API responses, error responses, pagination
- ✅ **Business enums** - Currency, Country, KYC levels, Transaction types
- ✅ **Validation annotations** - Custom validators for phone numbers, amounts, currencies

### Key Design Principles

1. **Immutability** - All value objects are immutable (Money, DTOs)
2. **Type Safety** - Use enums instead of strings for known values
3. **French-First** - All user-facing messages in French
4. **Zero Decimals** - XOF/XAF amounts are whole numbers (long, not BigDecimal)
5. **BCEAO Compliance** - Fee calculation, KYC limits, transaction rules built-in

---

## Package Structure

```
com.payment.common/
│
├── dto/                          # Data Transfer Objects
│   ├── Money.java               ⭐ Immutable money value object
│   ├── ApiResponse.java          Standard API response wrapper
│   ├── ErrorResponse.java        Error response with details
│   ├── PageResponse.java         Paginated response
│   └── ValidationError.java      Field-level validation errors
│
├── enums/                        # Enumerations
│   ├── Currency.java            ⭐ XOF, XAF
│   ├── Country.java             ⭐ 14 WAEMU/CEMAC countries
│   ├── KYCLevel.java            ⭐ LEVEL_1, LEVEL_2, LEVEL_3
│   ├── TransactionType.java      PAYIN, PAYOUT, TRANSFER, etc.
│   ├── TransactionStatus.java    PENDING, COMPLETED, FAILED, etc.
│   ├── UserStatus.java           ACTIVE, SUSPENDED, FROZEN, etc.
│   ├── AccountStatus.java        ACTIVE, FROZEN, CLOSED
│   └── ErrorCode.java           ⭐ Standardized error codes (French)
│
├── exception/                    # Custom Exceptions
│   ├── BaseException.java       ⭐ Base for all custom exceptions
│   ├── ValidationException.java
│   ├── ResourceNotFoundException.java
│   ├── BusinessRuleException.java
│   ├── InsufficientBalanceException.java
│   ├── LimitExceededException.java
│   └── DuplicateResourceException.java
│
├── util/                         # Utility Classes
│   ├── MoneyUtils.java          ⭐ Fee calculations, conversions
│   ├── PhoneNumberUtils.java    ⭐ Phone validation & formatting
│   ├── DateUtils.java            Date/time utilities
│   ├── ValidationUtils.java      Common validation logic
│   ├── IdempotencyKeyGenerator.java  Generate unique keys
│   └── FormatUtils.java          Formatting utilities
│
├── constant/                     # Constants
│   ├── RegexPatterns.java        Regex patterns for validation
│   ├── DateTimeFormats.java      Date/time format constants
│   ├── TransactionLimits.java    BCEAO transaction limits
│   └── Messages.java             Common message keys
│
└── annotation/                   # Custom Annotations
    ├── ValidPhoneNumber.java     Phone number validation
    ├── ValidCurrency.java        Currency validation
    ├── ValidAmount.java          Amount validation
    └── ValidPhoneNumberValidator.java  Validator implementation
```

⭐ = Critical classes

---

## Core Value Objects

### 1. Money.java

**Package**: `com.payment.common.dto`
**Purpose**: Immutable value object for handling XOF/XAF currency amounts

#### Design Decisions

- **Uses `long` not `BigDecimal`** - XOF/XAF have NO decimal places
- **Immutable** - All operations return new Money instances
- **Type-safe** - Currency is enforced at compile time
- **Arithmetic operations** - add(), subtract(), multiply()
- **Comparison operations** - isGreaterThan(), isLessThan(), isZero()
- **French formatting** - format() returns "5 000 XOF" (French number format)

#### Class Definition

```java
package com.payment.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.payment.common.enums.Currency;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
public final class Money implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long amount;        // Amount in minor units (whole number)
    private final Currency currency;  // XOF or XAF

    @JsonCreator
    public Money(
            @JsonProperty("amount") long amount,
            @JsonProperty("currency") Currency currency) {

        if (currency == null) {
            throw new IllegalArgumentException("La devise ne peut pas être null");
        }

        if (amount < 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être négatif");
        }

        this.amount = amount;
        this.currency = currency;
    }

    // Factory methods
    public static Money ofXOF(long amount) {
        return new Money(amount, Currency.XOF);
    }

    public static Money ofXAF(long amount) {
        return new Money(amount, Currency.XAF);
    }

    public static Money zero(Currency currency) {
        return new Money(0L, currency);
    }

    // Arithmetic operations
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Impossible d'additionner des devises différentes: "
                + this.currency + " et " + other.currency
            );
        }
        return new Money(this.amount + other.amount, this.currency);
    }

    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Impossible de soustraire des devises différentes: "
                + this.currency + " et " + other.currency
            );
        }

        long result = this.amount - other.amount;
        if (result < 0) {
            throw new IllegalArgumentException("Le résultat ne peut pas être négatif");
        }

        return new Money(result, this.currency);
    }

    public Money multiply(long factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("Le facteur ne peut pas être négatif");
        }
        return new Money(this.amount * factor, this.currency);
    }

    public Money divide(long divisor) {
        if (divisor <= 0) {
            throw new IllegalArgumentException("Le diviseur doit être positif");
        }
        return new Money(this.amount / divisor, this.currency);
    }

    // Comparison operations
    public boolean isGreaterThan(Money other) {
        ensureSameCurrency(other);
        return this.amount > other.amount;
    }

    public boolean isLessThan(Money other) {
        ensureSameCurrency(other);
        return this.amount < other.amount;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        ensureSameCurrency(other);
        return this.amount >= other.amount;
    }

    public boolean isLessThanOrEqual(Money other) {
        ensureSameCurrency(other);
        return this.amount <= other.amount;
    }

    public boolean isZero() {
        return this.amount == 0;
    }

    public boolean isPositive() {
        return this.amount > 0;
    }

    public boolean isNegative() {
        return this.amount < 0;
    }

    // Formatting
    public String format() {
        return String.format("%,d %s", amount, currency)
            .replace(',', ' ');  // French format uses spaces
    }

    public String formatWithSymbol() {
        return format() + " FCFA";  // CFA Franc symbol
    }

    private void ensureSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Les devises doivent être identiques: "
                + this.currency + " et " + other.currency
            );
        }
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
```

#### Usage Examples

```java
// Creating Money instances
Money amount1 = Money.ofXOF(5000L);           // 5,000 XOF
Money amount2 = new Money(10000L, Currency.XOF);  // 10,000 XOF
Money zero = Money.zero(Currency.XOF);        // 0 XOF

// Arithmetic operations
Money total = amount1.add(amount2);           // 15,000 XOF
Money difference = amount2.subtract(amount1); // 5,000 XOF
Money doubled = amount1.multiply(2);          // 10,000 XOF
Money half = amount2.divide(2);               // 5,000 XOF

// Comparisons
boolean isGreater = amount2.isGreaterThan(amount1);  // true
boolean isZero = zero.isZero();                      // true

// Formatting
String formatted = amount1.format();          // "5 000 XOF"
String withSymbol = amount1.formatWithSymbol(); // "5 000 XOF FCFA"

// JSON serialization
// {"amount": 5000, "currency": "XOF"}
```

#### Validation Rules

1. **Amount cannot be negative** - Throws IllegalArgumentException
2. **Currency cannot be null** - Throws IllegalArgumentException
3. **Same currency required for operations** - Cannot add XOF + XAF
4. **Subtraction cannot result in negative** - Throws exception
5. **Division by zero or negative** - Throws exception

#### Testing Checklist

- [ ] Create Money with valid XOF amount
- [ ] Create Money with valid XAF amount
- [ ] Reject negative amounts
- [ ] Reject null currency
- [ ] Add two Money objects (same currency)
- [ ] Reject addition of different currencies
- [ ] Subtract two Money objects
- [ ] Reject subtraction resulting in negative
- [ ] Multiply by positive factor
- [ ] Divide by positive divisor
- [ ] Compare amounts (greater/less than)
- [ ] Format for French display
- [ ] JSON serialization/deserialization

---

## Enumerations

### 1. Currency.java

**Package**: `com.payment.common.enums`
**Purpose**: Supported currencies (XOF, XAF)

```java
package com.payment.common.enums;

import lombok.Getter;

/**
 * Supported currencies in the payment system.
 *
 * V1: XOF only (West African CFA Franc)
 * V2: XOF + XAF (Central African CFA Franc)
 *
 * CRITICAL: Both XOF and XAF have NO decimal places.
 * Exchange rate: 1 XOF = 1 XAF (both pegged to EUR at 655.957)
 */
@Getter
public enum Currency {

    XOF("XOF", "Franc CFA (BCEAO)", 0, "fr-FR", "Franc CFA de la BCEAO"),
    XAF("XAF", "Franc CFA (BEAC)", 0, "fr-FR", "Franc CFA de la BEAC");

    private final String code;              // ISO 4217 code
    private final String displayName;       // Short display name
    private final int decimalPlaces;        // 0 for XOF/XAF
    private final String locale;            // Locale for formatting
    private final String fullName;          // Full official name

    Currency(String code, String displayName, int decimalPlaces, String locale, String fullName) {
        this.code = code;
        this.displayName = displayName;
        this.decimalPlaces = decimalPlaces;
        this.locale = locale;
        this.fullName = fullName;
    }

    /**
     * Get Currency from ISO code
     */
    public static Currency fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Le code devise ne peut pas être null");
        }

        for (Currency currency : values()) {
            if (currency.code.equalsIgnoreCase(code)) {
                return currency;
            }
        }
        throw new IllegalArgumentException("Devise non supportée: " + code);
    }

    /**
     * Check if currency has decimals
     */
    public boolean hasDecimals() {
        return decimalPlaces > 0;
    }

    /**
     * Check if currency is XOF
     */
    public boolean isXOF() {
        return this == XOF;
    }

    /**
     * Check if currency is XAF
     */
    public boolean isXAF() {
        return this == XAF;
    }

    /**
     * Get currency symbol (both use FCFA)
     */
    public String getSymbol() {
        return "FCFA";
    }

    @Override
    public String toString() {
        return code;
    }
}
```

#### Currency Specifications

| Currency | ISO Code | Decimal Places | Exchange Rate | Regulatory Body | Countries |
|----------|----------|----------------|---------------|-----------------|-----------|
| **XOF** | XOF | 0 | 1 XOF = 1 XAF = 0.00152449 EUR | BCEAO | 8 WAEMU countries |
| **XAF** | XAF | 0 | 1 XAF = 1 XOF = 0.00152449 EUR | BEAC | 6 CEMAC countries |

**Key Facts**:
- Both currencies are **pegged to EUR** at 655.957 FCFA = 1 EUR
- Exchange rate between XOF and XAF is **fixed at 1:1**
- Symbol for both is **FCFA** (Franc de la Communauté Financière Africaine)
- No decimal places - always whole numbers

#### Usage Examples

```java
// Get currency by code
Currency xof = Currency.fromCode("XOF");
Currency xaf = Currency.fromCode("XAF");

// Check properties
boolean hasDecimals = xof.hasDecimals();  // false
String symbol = xof.getSymbol();          // "FCFA"
int decimals = xof.getDecimalPlaces();    // 0

// Type checking
boolean isXOF = xof.isXOF();              // true
boolean isXAF = xof.isXAF();              // false

// Display names
String code = xof.getCode();              // "XOF"
String name = xof.getDisplayName();       // "Franc CFA (BCEAO)"
String fullName = xof.getFullName();      // "Franc CFA de la BCEAO"
```

---

### 2. Country.java

**Package**: `com.payment.common.enums`
**Purpose**: Supported countries in WAEMU and CEMAC regions

```java
package com.payment.common.enums;

import lombok.Getter;

/**
 * Supported countries in WAEMU (8) and CEMAC (6) regions.
 *
 * WAEMU (Union Économique et Monétaire Ouest-Africaine):
 * - Currency: XOF
 * - Regulator: BCEAO (Banque Centrale des États de l'Afrique de l'Ouest)
 *
 * CEMAC (Communauté Économique et Monétaire de l'Afrique Centrale):
 * - Currency: XAF
 * - Regulator: BEAC (Banque des États de l'Afrique Centrale)
 */
@Getter
public enum Country {

    // WAEMU Countries (BCEAO - XOF)
    SENEGAL("SN", "Sénégal", "+221", Currency.XOF, "Dakar", "WAEMU"),
    IVORY_COAST("CI", "Côte d'Ivoire", "+225", Currency.XOF, "Abidjan", "WAEMU"),
    MALI("ML", "Mali", "+223", Currency.XOF, "Bamako", "WAEMU"),
    BURKINA_FASO("BF", "Burkina Faso", "+226", Currency.XOF, "Ouagadougou", "WAEMU"),
    BENIN("BJ", "Bénin", "+229", Currency.XOF, "Porto-Novo", "WAEMU"),
    TOGO("TG", "Togo", "+228", Currency.XOF, "Lomé", "WAEMU"),
    NIGER("NE", "Niger", "+227", Currency.XOF, "Niamey", "WAEMU"),
    GUINEA_BISSAU("GW", "Guinée-Bissau", "+245", Currency.XOF, "Bissau", "WAEMU"),

    // CEMAC Countries (BEAC - XAF)
    CAMEROON("CM", "Cameroun", "+237", Currency.XAF, "Yaoundé", "CEMAC"),
    GABON("GA", "Gabon", "+241", Currency.XAF, "Libreville", "CEMAC"),
    CONGO("CG", "Congo", "+242", Currency.XAF, "Brazzaville", "CEMAC"),
    CENTRAL_AFRICAN_REPUBLIC("CF", "République Centrafricaine", "+236", Currency.XAF, "Bangui", "CEMAC"),
    CHAD("TD", "Tchad", "+235", Currency.XAF, "N'Djamena", "CEMAC"),
    EQUATORIAL_GUINEA("GQ", "Guinée Équatoriale", "+240", Currency.XAF, "Malabo", "CEMAC");

    private final String code;           // ISO 3166-1 alpha-2
    private final String displayName;    // French name
    private final String dialCode;       // Phone country code
    private final Currency currency;     // XOF or XAF
    private final String capital;        // Capital city
    private final String region;         // WAEMU or CEMAC

    Country(String code, String displayName, String dialCode, Currency currency, String capital, String region) {
        this.code = code;
        this.displayName = displayName;
        this.dialCode = dialCode;
        this.currency = currency;
        this.capital = capital;
        this.region = region;
    }

    /**
     * Get Country from ISO code (e.g., "SN" -> SENEGAL)
     */
    public static Country fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Le code pays ne peut pas être null");
        }

        for (Country country : values()) {
            if (country.code.equalsIgnoreCase(code)) {
                return country;
            }
        }
        throw new IllegalArgumentException("Pays non supporté: " + code);
    }

    /**
     * Get Country from dial code (e.g., "+221" -> SENEGAL)
     */
    public static Country fromDialCode(String dialCode) {
        if (dialCode == null) {
            throw new IllegalArgumentException("Le code téléphonique ne peut pas être null");
        }

        // Normalize dial code (remove spaces, ensure +)
        String normalized = dialCode.trim();
        if (!normalized.startsWith("+")) {
            normalized = "+" + normalized;
        }

        for (Country country : values()) {
            if (country.dialCode.equals(normalized)) {
                return country;
            }
        }
        throw new IllegalArgumentException("Pays non trouvé pour le code: " + dialCode);
    }

    /**
     * Get all WAEMU countries
     */
    public static Country[] getWAEMUCountries() {
        return new Country[]{
            SENEGAL, IVORY_COAST, MALI, BURKINA_FASO,
            BENIN, TOGO, NIGER, GUINEA_BISSAU
        };
    }

    /**
     * Get all CEMAC countries
     */
    public static Country[] getCEMACCountries() {
        return new Country[]{
            CAMEROON, GABON, CONGO, CENTRAL_AFRICAN_REPUBLIC,
            CHAD, EQUATORIAL_GUINEA
        };
    }

    /**
     * Check if country uses XOF
     */
    public boolean usesXOF() {
        return currency == Currency.XOF;
    }

    /**
     * Check if country uses XAF
     */
    public boolean usesXAF() {
        return currency == Currency.XAF;
    }

    /**
     * Check if country is in WAEMU region
     */
    public boolean isWAEMU() {
        return "WAEMU".equals(region);
    }

    /**
     * Check if country is in CEMAC region
     */
    public boolean isCEMAC() {
        return "CEMAC".equals(region);
    }

    /**
     * Get regulator (BCEAO or BEAC)
     */
    public String getRegulator() {
        return isWAEMU() ? "BCEAO" : "BEAC";
    }

    @Override
    public String toString() {
        return displayName;
    }
}
```

#### Country Details

**WAEMU Countries (8)**:

| Country | Code | Dial | Capital | Population | Currency |
|---------|------|------|---------|------------|----------|
| Sénégal | SN | +221 | Dakar | 17M | XOF |
| Côte d'Ivoire | CI | +225 | Abidjan | 27M | XOF |
| Mali | ML | +223 | Bamako | 21M | XOF |
| Burkina Faso | BF | +226 | Ouagadougou | 22M | XOF |
| Bénin | BJ | +229 | Porto-Novo | 13M | XOF |
| Togo | TG | +228 | Lomé | 8M | XOF |
| Niger | NE | +227 | Niamey | 25M | XOF |
| Guinée-Bissau | GW | +245 | Bissau | 2M | XOF |

**CEMAC Countries (6)**:

| Country | Code | Dial | Capital | Population | Currency |
|---------|------|------|---------|------------|----------|
| Cameroun | CM | +237 | Yaoundé | 27M | XAF |
| Gabon | GA | +241 | Libreville | 2M | XAF |
| Congo | CG | +242 | Brazzaville | 6M | XAF |
| République Centrafricaine | CF | +236 | Bangui | 5M | XAF |
| Tchad | TD | +235 | N'Djamena | 17M | XAF |
| Guinée Équatoriale | GQ | +240 | Malabo | 1M | XAF |

#### Usage Examples

```java
// Get country by code
Country senegal = Country.fromCode("SN");
Country cameroon = Country.fromCode("CM");

// Get country by dial code
Country country = Country.fromDialCode("+221");  // SENEGAL

// Get all countries by region
Country[] waemu = Country.getWAEMUCountries();  // 8 countries
Country[] cemac = Country.getCEMACCountries();  // 6 countries

// Check properties
boolean usesXOF = senegal.usesXOF();           // true
boolean isWAEMU = senegal.isWAEMU();           // true
String regulator = senegal.getRegulator();     // "BCEAO"
String capital = senegal.getCapital();         // "Dakar"

// Get dial code for validation
String dialCode = senegal.getDialCode();       // "+221"
Currency currency = senegal.getCurrency();     // Currency.XOF
```

---

### 3. KYCLevel.java

**Package**: `com.payment.common.enums`
**Purpose**: KYC levels with transaction limits (BCEAO requirement)

```java
package com.payment.common.enums;

import com.payment.common.dto.Money;
import lombok.Getter;

/**
 * KYC levels with transaction limits (amounts in XOF).
 *
 * BCEAO Requirement: 3-tier KYC system with different limits.
 *
 * LEVEL_1: Basic KYC (name, phone, birth date)
 * LEVEL_2: Enhanced KYC (ID document, address, proof of address)
 * LEVEL_3: Full KYC (enhanced + business documents for businesses)
 */
@Getter
public enum KYCLevel {

    LEVEL_1(
        1,
        "Niveau 1",
        "KYC basique",
        50_000L,      // Single transaction limit (XOF)
        100_000L,     // Daily limit (XOF)
        500_000L,     // Monthly limit (XOF)
        false         // Requires annual renewal
    ),

    LEVEL_2(
        2,
        "Niveau 2",
        "KYC amélioré",
        250_000L,     // Single transaction limit (XOF)
        500_000L,     // Daily limit (XOF)
        5_000_000L,   // Monthly limit (XOF)
        false         // Requires annual renewal
    ),

    LEVEL_3(
        3,
        "Niveau 3",
        "KYC complet",
        1_000_000L,   // Single transaction limit (XOF)
        2_000_000L,   // Daily limit (XOF)
        20_000_000L,  // Monthly limit (XOF)
        false         // Requires annual renewal
    );

    private final int level;
    private final String displayName;
    private final String description;
    private final long maxTransactionAmount;  // XOF
    private final long maxDailyAmount;        // XOF
    private final long maxMonthlyAmount;      // XOF
    private final boolean requiresRenewal;

    KYCLevel(int level, String displayName, String description,
             long maxTransactionAmount, long maxDailyAmount,
             long maxMonthlyAmount, boolean requiresRenewal) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
        this.maxTransactionAmount = maxTransactionAmount;
        this.maxDailyAmount = maxDailyAmount;
        this.maxMonthlyAmount = maxMonthlyAmount;
        this.requiresRenewal = requiresRenewal;
    }

    /**
     * Get KYC level from integer (1, 2, or 3)
     */
    public static KYCLevel fromLevel(int level) {
        for (KYCLevel kycLevel : values()) {
            if (kycLevel.level == level) {
                return kycLevel;
            }
        }
        throw new IllegalArgumentException("Niveau KYC invalide: " + level);
    }

    /**
     * Check if level can process transaction amount
     */
    public boolean canProcessAmount(long amount) {
        return amount <= maxTransactionAmount;
    }

    /**
     * Check if level can process transaction amount (Money object)
     */
    public boolean canProcessAmount(Money amount) {
        if (amount == null) {
            return false;
        }
        return canProcessAmount(amount.getAmount());
    }

    /**
     * Check if daily limit allows amount
     */
    public boolean isDailyLimitSufficient(long currentDailyTotal, long newAmount) {
        return (currentDailyTotal + newAmount) <= maxDailyAmount;
    }

    /**
     * Check if monthly limit allows amount
     */
    public boolean isMonthlyLimitSufficient(long currentMonthlyTotal, long newAmount) {
        return (currentMonthlyTotal + newAmount) <= maxMonthlyAmount;
    }

    /**
     * Get max transaction amount as Money
     */
    public Money getMaxTransactionAmountAsMoney(Currency currency) {
        return new Money(maxTransactionAmount, currency);
    }

    /**
     * Get max daily amount as Money
     */
    public Money getMaxDailyAmountAsMoney(Currency currency) {
        return new Money(maxDailyAmount, currency);
    }

    /**
     * Get max monthly amount as Money
     */
    public Money getMaxMonthlyAmountAsMoney(Currency currency) {
        return new Money(maxMonthlyAmount, currency);
    }

    /**
     * Check if this level is higher than another
     */
    public boolean isHigherThan(KYCLevel other) {
        return this.level > other.level;
    }

    /**
     * Check if this level is lower than another
     */
    public boolean isLowerThan(KYCLevel other) {
        return this.level < other.level;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
```

#### KYC Level Comparison Table

| Feature | LEVEL_1 | LEVEL_2 | LEVEL_3 |
|---------|---------|---------|---------|
| **Required Documents** | Name, Phone, DOB | ID, Address, Proof | Enhanced + Business docs |
| **Single Transaction** | 50,000 XOF | 250,000 XOF | 1,000,000 XOF |
| **Daily Limit** | 100,000 XOF | 500,000 XOF | 2,000,000 XOF |
| **Monthly Limit** | 500,000 XOF | 5,000,000 XOF | 20,000,000 XOF |
| **Annual Renewal** | Yes | Yes | Yes |
| **Verification Time** | Instant | 24-48 hours | 3-5 days |
| **Target Users** | Individual | Individual | Business/High-value |

#### Usage Examples

```java
// Get KYC level
KYCLevel level1 = KYCLevel.LEVEL_1;
KYCLevel level2 = KYCLevel.fromLevel(2);

// Check transaction limits
boolean canProcess = level1.canProcessAmount(40_000L);  // true (< 50,000)
boolean canProcessMoney = level1.canProcessAmount(Money.ofXOF(40_000L));  // true

// Check daily limits
long currentDaily = 80_000L;
long newTransaction = 30_000L;
boolean withinDaily = level1.isDailyLimitSufficient(currentDaily, newTransaction);
// false (80,000 + 30,000 = 110,000 > 100,000)

// Check monthly limits
long currentMonthly = 400_000L;
boolean withinMonthly = level1.isMonthlyLimitSufficient(currentMonthly, 50_000L);
// true (400,000 + 50,000 = 450,000 < 500,000)

// Get limits as Money objects
Money maxTransaction = level1.getMaxTransactionAmountAsMoney(Currency.XOF);
// Money(50000, XOF)

// Compare levels
boolean isHigher = level2.isHigherThan(level1);  // true
boolean isLower = level1.isLowerThan(level2);    // true
```

---

### 4. TransactionType.java

**Package**: `com.payment.common.enums`

```java
package com.payment.common.enums;

import lombok.Getter;

/**
 * Types of transactions in the payment system.
 */
@Getter
public enum TransactionType {

    PAYIN("PAYIN", "Dépôt", "Deposit from mobile money to platform"),
    PAYOUT("PAYOUT", "Retrait", "Withdrawal from platform to mobile money"),
    TRANSFER("TRANSFER", "Transfert", "Transfer between platform accounts"),
    REFUND("REFUND", "Remboursement", "Refund to customer"),
    COMMISSION("COMMISSION", "Commission", "Commission payment"),
    REVERSAL("REVERSAL", "Annulation", "Transaction reversal");

    private final String code;
    private final String displayName;  // French
    private final String description;   // English (internal)

    TransactionType(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public static TransactionType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Le code transaction ne peut pas être null");
        }

        for (TransactionType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type de transaction invalide: " + code);
    }

    public boolean isPayin() {
        return this == PAYIN;
    }

    public boolean isPayout() {
        return this == PAYOUT;
    }

    public boolean isTransfer() {
        return this == TRANSFER;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
```

---

### 5. TransactionStatus.java

**Package**: `com.payment.common.enums`

```java
package com.payment.common.enums;

import lombok.Getter;

/**
 * Transaction status lifecycle.
 */
@Getter
public enum TransactionStatus {

    PENDING("PENDING", "En attente", "Transaction initiated, awaiting processing"),
    PROCESSING("PROCESSING", "En cours", "Transaction being processed"),
    COMPLETED("COMPLETED", "Terminée", "Transaction successfully completed"),
    FAILED("FAILED", "Échouée", "Transaction failed"),
    CANCELLED("CANCELLED", "Annulée", "Transaction cancelled by user"),
    REVERSED("REVERSED", "Inversée", "Transaction reversed"),
    EXPIRED("EXPIRED", "Expirée", "Transaction expired");

    private final String code;
    private final String displayName;  // French
    private final String description;   // English (internal)

    TransactionStatus(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public static TransactionStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Le code statut ne peut pas être null");
        }

        for (TransactionStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Statut invalide: " + code);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED ||
               this == CANCELLED || this == REVERSED || this == EXPIRED;
    }

    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public boolean isPending() {
        return this == PENDING || this == PROCESSING;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
```

---

### 6. ErrorCode.java

**Package**: `com.payment.common.enums`

```java
package com.payment.common.enums;

import lombok.Getter;

/**
 * Standard error codes for the payment system.
 * All error messages are in French (BCEAO requirement).
 *
 * Error Code Format: ERR_XXXX
 * - 1000-1999: Validation Errors
 * - 2000-2999: Resource Not Found Errors
 * - 3000-3999: Business Rule Violations
 * - 4000-4999: Duplicate Resource Errors
 * - 5000-5999: System Errors
 * - 6000-6999: External Service Errors
 */
@Getter
public enum ErrorCode {

    // Validation Errors (1000-1999)
    VALIDATION_ERROR("ERR_1000", "Erreur de validation"),
    INVALID_PHONE_NUMBER("ERR_1001", "Numéro de téléphone invalide"),
    INVALID_AMOUNT("ERR_1002", "Montant invalide"),
    INVALID_CURRENCY("ERR_1003", "Devise invalide"),
    MISSING_REQUIRED_FIELD("ERR_1004", "Champ obligatoire manquant"),
    INVALID_EMAIL("ERR_1005", "Adresse email invalide"),
    INVALID_DATE("ERR_1006", "Date invalide"),
    INVALID_COUNTRY("ERR_1007", "Pays invalide"),
    INVALID_KYC_LEVEL("ERR_1008", "Niveau KYC invalide"),

    // Resource Errors (2000-2999)
    RESOURCE_NOT_FOUND("ERR_2000", "Ressource non trouvée"),
    USER_NOT_FOUND("ERR_2001", "Utilisateur non trouvé"),
    ACCOUNT_NOT_FOUND("ERR_2002", "Compte non trouvé"),
    TRANSACTION_NOT_FOUND("ERR_2003", "Transaction non trouvée"),
    PSP_NOT_FOUND("ERR_2004", "Fournisseur de paiement non trouvé"),

    // Business Rule Errors (3000-3999)
    BUSINESS_RULE_VIOLATION("ERR_3000", "Violation de règle métier"),
    INSUFFICIENT_BALANCE("ERR_3001", "Solde insuffisant"),
    DAILY_LIMIT_EXCEEDED("ERR_3002", "Limite quotidienne dépassée"),
    MONTHLY_LIMIT_EXCEEDED("ERR_3003", "Limite mensuelle dépassée"),
    TRANSACTION_LIMIT_EXCEEDED("ERR_3004", "Limite de transaction dépassée"),
    ACCOUNT_FROZEN("ERR_3005", "Compte gelé"),
    KYC_UPGRADE_REQUIRED("ERR_3006", "Mise à niveau KYC requise"),
    KYC_EXPIRED("ERR_3007", "KYC expiré, renouvellement requis"),
    ACCOUNT_NOT_ACTIVE("ERR_3008", "Compte non actif"),
    TRANSACTION_NOT_ALLOWED("ERR_3009", "Transaction non autorisée"),

    // Duplicate Errors (4000-4999)
    DUPLICATE_RESOURCE("ERR_4000", "Ressource déjà existante"),
    DUPLICATE_PHONE_NUMBER("ERR_4001", "Numéro de téléphone déjà utilisé"),
    DUPLICATE_EMAIL("ERR_4002", "Email déjà utilisé"),
    DUPLICATE_TRANSACTION("ERR_4003", "Transaction en double détectée"),
    DUPLICATE_IDEMPOTENCY_KEY("ERR_4004", "Clé d'idempotence déjà utilisée"),

    // System Errors (5000-5999)
    INTERNAL_SERVER_ERROR("ERR_5000", "Erreur interne du serveur"),
    SERVICE_UNAVAILABLE("ERR_5001", "Service temporairement indisponible"),
    DATABASE_ERROR("ERR_5002", "Erreur de base de données"),
    CONFIGURATION_ERROR("ERR_5003", "Erreur de configuration"),

    // External Service Errors (6000-6999)
    PSP_ERROR("ERR_6000", "Erreur du fournisseur de paiement"),
    PSP_TIMEOUT("ERR_6001", "Délai d'attente du fournisseur dépassé"),
    PSP_INSUFFICIENT_BALANCE("ERR_6002", "Solde insuffisant chez le fournisseur"),
    PSP_INVALID_CREDENTIALS("ERR_6003", "Identifiants PSP invalides"),
    PSP_SERVICE_UNAVAILABLE("ERR_6004", "Service PSP indisponible"),
    WEBHOOK_VALIDATION_FAILED("ERR_6005", "Validation du webhook échouée");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ErrorCode fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Le code erreur ne peut pas être null");
        }

        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equalsIgnoreCase(code)) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("Code erreur invalide: " + code);
    }

    public boolean isValidationError() {
        return code.startsWith("ERR_1");
    }

    public boolean isResourceError() {
        return code.startsWith("ERR_2");
    }

    public boolean isBusinessRuleError() {
        return code.startsWith("ERR_3");
    }

    public boolean isDuplicateError() {
        return code.startsWith("ERR_4");
    }

    public boolean isSystemError() {
        return code.startsWith("ERR_5");
    }

    public boolean isExternalServiceError() {
        return code.startsWith("ERR_6");
    }

    @Override
    public String toString() {
        return code + ": " + message;
    }
}
```

---

## Data Transfer Objects (DTOs)

### 1. ApiResponse.java

**Package**: `com.payment.common.dto`
**Purpose**: Standard wrapper for all API responses

```java
package com.payment.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard API response wrapper for all endpoints.
 *
 * Success Example:
 * {
 *   "success": true,
 *   "message": "Transaction créée avec succès",
 *   "data": { "transactionId": "123", "amount": 5000 },
 *   "timestamp": "2025-10-18T10:30:00Z"
 * }
 *
 * Error Example:
 * {
 *   "success": false,
 *   "message": "Solde insuffisant",
 *   "data": null,
 *   "timestamp": "2025-10-18T10:30:00Z"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Create success response with data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .build();
    }

    /**
     * Create success response without data
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .build();
    }

    /**
     * Create error response with message only
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .build();
    }

    /**
     * Create error response with data (e.g., validation errors)
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .data(data)
            .build();
    }
}
```

#### Usage Examples

```java
// Success with data
ApiResponse<TransactionDTO> response = ApiResponse.success(
    "Transaction créée avec succès",
    transactionDTO
);

// Success without data
ApiResponse<Void> response = ApiResponse.success("Opération réussie");

// Error without data
ApiResponse<Void> response = ApiResponse.error("Solde insuffisant");

// Error with validation details
ApiResponse<List<ValidationError>> response = ApiResponse.error(
    "Erreur de validation",
    validationErrors
);
```

---

### 2. ErrorResponse.java

**Package**: `com.payment.common.dto`

```java
package com.payment.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Detailed error response for API exceptions.
 *
 * Example:
 * {
 *   "errorCode": "ERR_3001",
 *   "message": "Solde insuffisant",
 *   "details": "Votre solde actuel est de 1000 XOF",
 *   "path": "/api/v1/transactions",
 *   "timestamp": "2025-10-18T10:30:00Z",
 *   "validationErrors": []
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String errorCode;         // ERR_XXXX
    private String message;           // French user message
    private String details;           // Additional details
    private String path;              // Request path
    private Instant timestamp;

    @Builder.Default
    private List<ValidationError> validationErrors = List.of();

    /**
     * Create error response from error code
     */
    public static ErrorResponse of(String errorCode, String message, String path) {
        return ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message)
            .path(path)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create error response with validation errors
     */
    public static ErrorResponse withValidationErrors(
            String errorCode,
            String message,
            String path,
            List<ValidationError> validationErrors) {

        return ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message)
            .path(path)
            .validationErrors(validationErrors)
            .timestamp(Instant.now())
            .build();
    }
}
```

---

### 3. ValidationError.java

**Package**: `com.payment.common.dto`

```java
package com.payment.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Field-level validation error.
 *
 * Example:
 * {
 *   "field": "phoneNumber",
 *   "rejectedValue": "1234",
 *   "message": "Numéro de téléphone invalide"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {

    private String field;           // Field name
    private Object rejectedValue;   // Invalid value
    private String message;         // French error message

    public static ValidationError of(String field, Object rejectedValue, String message) {
        return ValidationError.builder()
            .field(field)
            .rejectedValue(rejectedValue)
            .message(message)
            .build();
    }
}
```

---

### 4. PageResponse.java

**Package**: `com.payment.common.dto`

```java
package com.payment.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response wrapper.
 *
 * Example:
 * {
 *   "content": [...],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 150,
 *   "totalPages": 8,
 *   "first": true,
 *   "last": false
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> of(
            List<T> content,
            int page,
            int size,
            long totalElements) {

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<T>builder()
            .content(content)
            .page(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .first(page == 0)
            .last(page == totalPages - 1)
            .build();
    }
}
```

---

### 5. QRCodePayInRequest.java

**Package**: `com.payment.common.dto`
**Purpose**: Request DTO for QR Code Pay-In with failureUrl and successUrl

**Added**: v1.1.0 (QR Code & OTP Pay-In feature)

```java
package com.payment.common.dto;

import com.payment.common.enums.Currency;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for QR Code Pay-In
 * Extends standard pay-in with failureUrl and successUrl for PSP redirect
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QRCodePayInRequest {

    @NotNull(message = "L'ID du compte est obligatoire")
    private UUID accountId;

    @NotNull(message = "L'ID du fournisseur est obligatoire")
    private UUID providerId;

    @NotNull(message = "L'ID de l'abonnement est obligatoire")
    private UUID subscriptionId;

    @NotNull(message = "Le montant est obligatoire")
    @Min(value = 1, message = "Le montant doit être supérieur à 0")
    private Long amount;

    @NotNull(message = "La devise est obligatoire")
    private Currency currency;

    @NotBlank(message = "L'URL d'échec est obligatoire")
    @Pattern(regexp = "^https?://.*", message = "L'URL d'échec doit être une URL valide")
    private String failureUrl;

    @NotBlank(message = "L'URL de succès est obligatoire")
    @Pattern(regexp = "^https?://.*", message = "L'URL de succès doit être une URL valide")
    private String successUrl;

    private String description;

    private Map<String, Object> metadata;
}
```

#### Usage Example

```java
QRCodePayInRequest request = QRCodePayInRequest.builder()
        .accountId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        .providerId(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"))
        .subscriptionId(UUID.fromString("770e8400-e29b-41d4-a716-446655440002"))
        .amount(10000L)
        .currency(Currency.XOF)
        .failureUrl("https://client.com/payment/failure")
        .successUrl("https://client.com/payment/success")
        .description("Recharge via QR Code")
        .build();
```

---

### 6. QRCodePayInResponse.java

**Package**: `com.payment.common.dto`
**Purpose**: Response DTO for QR Code Pay-In including paymentUrl

**Added**: v1.1.0 (QR Code & OTP Pay-In feature)

```java
package com.payment.common.dto;

import com.payment.common.enums.Currency;
import com.payment.common.enums.TransactionStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for QR Code Pay-In
 * Includes paymentUrl field for QR code display
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QRCodePayInResponse {

    private UUID transactionId;

    private String transactionType; // PAY_IN

    private TransactionStatus status; // PENDING, PROCESSING

    private Long amount;

    private Long commissionAmount;

    private Long netAmount;

    private Currency currency;

    private String pspReference;

    private String paymentUrl; // URL to display as QR code

    private LocalDateTime createdAt;

    private String description;
}
```

#### Usage Example

```java
QRCodePayInResponse response = QRCodePayInResponse.builder()
        .transactionId(UUID.randomUUID())
        .transactionType("PAY_IN")
        .status(TransactionStatus.PROCESSING)
        .amount(10000L)
        .commissionAmount(150L)
        .netAmount(9850L)
        .currency(Currency.XOF)
        .pspReference("OM-REF-67890")
        .paymentUrl("https://pay.orange-money.com/qr/abc123def456")
        .createdAt(LocalDateTime.now())
        .description("Recharge via QR Code")
        .build();
```

---

### 7. OTPPayInRequest.java

**Package**: `com.payment.common.dto`
**Purpose**: Request DTO for OTP Pay-In with OTP field

**Added**: v1.1.0 (QR Code & OTP Pay-In feature)

```java
package com.payment.common.dto;

import com.payment.common.enums.Currency;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for OTP Pay-In
 * Extends standard pay-in with OTP field
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OTPPayInRequest {

    @NotNull(message = "L'ID du compte est obligatoire")
    private UUID accountId;

    @NotNull(message = "L'ID du fournisseur est obligatoire")
    private UUID providerId;

    @NotNull(message = "L'ID de l'abonnement est obligatoire")
    private UUID subscriptionId;

    @NotNull(message = "Le montant est obligatoire")
    @Min(value = 1, message = "Le montant doit être supérieur à 0")
    private Long amount;

    @NotNull(message = "La devise est obligatoire")
    private Currency currency;

    @NotBlank(message = "L'OTP est obligatoire")
    @Size(min = 4, max = 10, message = "L'OTP doit contenir entre 4 et 10 caractères")
    private String otp;

    private String description;

    private Map<String, Object> metadata;
}
```

#### Usage Example

```java
OTPPayInRequest request = OTPPayInRequest.builder()
        .accountId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        .providerId(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"))
        .subscriptionId(UUID.fromString("770e8400-e29b-41d4-a716-446655440002"))
        .amount(10000L)
        .currency(Currency.XOF)
        .otp("123456")
        .description("Recharge via OTP")
        .build();
```

**Security Note**: Never log the actual OTP value in application logs.

---

### 8. OTPPayInResponse.java

**Package**: `com.payment.common.dto`
**Purpose**: Response DTO for OTP Pay-In (standard response, no paymentUrl)

**Added**: v1.1.0 (QR Code & OTP Pay-In feature)

```java
package com.payment.common.dto;

import com.payment.common.enums.Currency;
import com.payment.common.enums.TransactionStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for OTP Pay-In
 * Standard response (same as regular pay-in, no paymentUrl)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OTPPayInResponse {

    private UUID transactionId;

    private String transactionType; // PAY_IN

    private TransactionStatus status; // PENDING, PROCESSING

    private Long amount;

    private Long commissionAmount;

    private Long netAmount;

    private Currency currency;

    private String pspReference;

    private LocalDateTime createdAt;

    private String description;
}
```

#### Usage Example

```java
OTPPayInResponse response = OTPPayInResponse.builder()
        .transactionId(UUID.randomUUID())
        .transactionType("PAY_IN")
        .status(TransactionStatus.PROCESSING)
        .amount(10000L)
        .commissionAmount(150L)
        .netAmount(9850L)
        .currency(Currency.XOF)
        .pspReference("OM-REF-99999")
        .createdAt(LocalDateTime.now())
        .description("Recharge via OTP")
        .build();
```

---

## Exceptions

### BaseException.java

**Package**: `com.payment.common.exception`

```java
package com.payment.common.exception;

import com.payment.common.enums.ErrorCode;
import lombok.Getter;

/**
 * Base exception for all custom exceptions in the system.
 * All user-facing messages MUST be in French (BCEAO requirement).
 */
@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String userMessage;  // French message for end users

    public BaseException(ErrorCode errorCode, String userMessage) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public BaseException(ErrorCode errorCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public String getErrorCodeString() {
        return errorCode.getCode();
    }
}
```

### Other Exception Classes

```java
// ValidationException.java
public class ValidationException extends BaseException {
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}

// ResourceNotFoundException.java
public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String resource, String identifier) {
        super(
            ErrorCode.RESOURCE_NOT_FOUND,
            String.format("%s non trouvé: %s", resource, identifier)
        );
    }
}

// BusinessRuleException.java
public class BusinessRuleException extends BaseException {
    public BusinessRuleException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

// InsufficientBalanceException.java
public class InsufficientBalanceException extends BusinessRuleException {
    public InsufficientBalanceException(Money available, Money required) {
        super(
            ErrorCode.INSUFFICIENT_BALANCE,
            String.format(
                "Solde insuffisant. Disponible: %s, Requis: %s",
                available.format(),
                required.format()
            )
        );
    }
}

// LimitExceededException.java
public class LimitExceededException extends BusinessRuleException {
    public LimitExceededException(String limitType, Money limit, Money attempted) {
        super(
            ErrorCode.TRANSACTION_LIMIT_EXCEEDED,
            String.format(
                "Limite %s dépassée. Limite: %s, Tentative: %s",
                limitType,
                limit.format(),
                attempted.format()
            )
        );
    }
}

// DuplicateResourceException.java
public class DuplicateResourceException extends BaseException {
    public DuplicateResourceException(String resource, String identifier) {
        super(
            ErrorCode.DUPLICATE_RESOURCE,
            String.format("%s déjà existant: %s", resource, identifier)
        );
    }
}
```

---

## Utility Classes

### 1. MoneyUtils.java

**Package**: `com.payment.common.util`
**Purpose**: Money calculations and BCEAO fee logic

```java
package com.payment.common.util;

import com.payment.common.dto.Money;
import com.payment.common.enums.Currency;

/**
 * Utility class for Money operations and calculations.
 *
 * BCEAO Fee Rules:
 * - Transfers <= 5,000 XOF: FREE
 * - Transfers > 5,000 XOF: 100 XOF + 0.5%, capped at 1,000 XOF
 */
public final class MoneyUtils {

    private static final long FREE_TRANSFER_THRESHOLD = 5000L;  // XOF
    private static final long BASE_FEE = 100L;                  // XOF
    private static final double FEE_PERCENTAGE = 0.005;         // 0.5%
    private static final long MAX_FEE = 1000L;                  // XOF

    private MoneyUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Calculate transfer fee according to BCEAO rules.
     *
     * Rules:
     * - Amount <= 5,000 XOF: 0 XOF (FREE)
     * - Amount > 5,000 XOF: 100 XOF + 0.5%, capped at 1,000 XOF
     *
     * @param amount Transfer amount
     * @return Fee amount
     */
    public static Money calculateTransferFee(Money amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Le montant ne peut pas être null");
        }

        // Free for amounts <= 5,000 XOF
        if (amount.getAmount() <= FREE_TRANSFER_THRESHOLD) {
            return new Money(0L, amount.getCurrency());
        }

        // Calculate: 100 + 0.5%
        long variableFee = (long) (amount.getAmount() * FEE_PERCENTAGE);
        long totalFee = BASE_FEE + variableFee;

        // Cap at 1,000 XOF
        totalFee = Math.min(totalFee, MAX_FEE);

        return new Money(totalFee, amount.getCurrency());
    }

    /**
     * Calculate percentage of amount
     */
    public static Money calculatePercentage(Money amount, double percentage) {
        if (amount == null) {
            throw new IllegalArgumentException("Le montant ne peut pas être null");
        }

        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Le pourcentage doit être entre 0 et 100");
        }

        long result = (long) (amount.getAmount() * (percentage / 100.0));
        return new Money(result, amount.getCurrency());
    }

    /**
     * Convert XOF to XAF (1:1 exchange rate)
     */
    public static Money convertXOFtoXAF(Money xofAmount) {
        if (xofAmount.getCurrency() != Currency.XOF) {
            throw new IllegalArgumentException("Le montant doit être en XOF");
        }
        return new Money(xofAmount.getAmount(), Currency.XAF);
    }

    /**
     * Convert XAF to XOF (1:1 exchange rate)
     */
    public static Money convertXAFtoXOF(Money xafAmount) {
        if (xafAmount.getCurrency() != Currency.XAF) {
            throw new IllegalArgumentException("Le montant doit être en XAF");
        }
        return new Money(xafAmount.getAmount(), Currency.XOF);
    }

    /**
     * Parse amount from string (no decimals allowed for XOF/XAF)
     */
    public static Money parseAmount(String amountStr, Currency currency) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Le montant ne peut pas être vide");
        }

        // Remove spaces (French number format)
        amountStr = amountStr.trim().replace(" ", "").replace(",", "");

        try {
            long amount = Long.parseLong(amountStr);
            return new Money(amount, currency);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Format de montant invalide: " + amountStr, e);
        }
    }

    /**
     * Format amount for display (French format with spaces)
     * Example: 5000 -> "5 000"
     */
    public static String formatAmount(long amount) {
        return String.format("%,d", amount).replace(',', ' ');
    }

    /**
     * Format amount with currency
     * Example: 5000, XOF -> "5 000 XOF"
     */
    public static String formatAmountWithCurrency(long amount, Currency currency) {
        return formatAmount(amount) + " " + currency.getCode();
    }

    /**
     * Check if amount is within KYC limit
     */
    public static boolean isWithinLimit(Money amount, Money limit) {
        if (amount == null || limit == null) {
            return false;
        }

        if (!amount.getCurrency().equals(limit.getCurrency())) {
            throw new IllegalArgumentException("Les devises doivent être identiques");
        }

        return amount.isLessThanOrEqual(limit);
    }

    /**
     * Calculate total debit (amount + fee)
     */
    public static Money calculateTotalDebit(Money amount, Money fee) {
        if (amount == null || fee == null) {
            throw new IllegalArgumentException("Le montant et les frais ne peuvent pas être null");
        }

        return amount.add(fee);
    }

    /**
     * Min of two Money objects
     */
    public static Money min(Money a, Money b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Les montants ne peuvent pas être null");
        }

        if (!a.getCurrency().equals(b.getCurrency())) {
            throw new IllegalArgumentException("Les devises doivent être identiques");
        }

        return a.isLessThan(b) ? a : b;
    }

    /**
     * Max of two Money objects
     */
    public static Money max(Money a, Money b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Les montants ne peuvent pas être null");
        }

        if (!a.getCurrency().equals(b.getCurrency())) {
            throw new IllegalArgumentException("Les devises doivent être identiques");
        }

        return a.isGreaterThan(b) ? a : b;
    }
}
```

#### Fee Calculation Examples

```java
// Free transfer (≤ 5,000 XOF)
Money amount1 = Money.ofXOF(5_000L);
Money fee1 = MoneyUtils.calculateTransferFee(amount1);
// fee1 = 0 XOF

// Small transfer (5,001 - 180,000 XOF)
Money amount2 = Money.ofXOF(10_000L);
Money fee2 = MoneyUtils.calculateTransferFee(amount2);
// fee2 = 150 XOF (100 + 10,000 * 0.5%)

// Large transfer (capped at 1,000 XOF)
Money amount3 = Money.ofXOF(1_000_000L);
Money fee3 = MoneyUtils.calculateTransferFee(amount3);
// fee3 = 1,000 XOF (capped)
```

---

### 2. PhoneNumberUtils.java

**Package**: `com.payment.common.util`
**Purpose**: Phone number validation and formatting for WAEMU/CEMAC

```java
package com.payment.common.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.payment.common.enums.Country;

/**
 * Utility class for phone number validation and formatting.
 *
 * Supports WAEMU/CEMAC countries:
 * - Senegal (SN): +221
 * - Ivory Coast (CI): +225
 * - Mali (ML): +223
 * - Burkina Faso (BF): +226
 * - Benin (BJ): +229
 * - Togo (TG): +228
 * - Niger (NE): +227
 * - Guinea-Bissau (GW): +245
 * - Cameroon (CM): +237
 * - Gabon (GA): +241
 * - Congo (CG): +242
 * - Central African Republic (CF): +236
 * - Chad (TD): +235
 * - Equatorial Guinea (GQ): +240
 */
public final class PhoneNumberUtils {

    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    private PhoneNumberUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validate phone number for a given country
     *
     * @param phoneNumber Phone number (with or without country code)
     * @param country Country enum
     * @return true if valid
     */
    public static boolean isValidPhoneNumber(String phoneNumber, Country country) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        if (country == null) {
            return false;
        }

        try {
            PhoneNumber parsedNumber = phoneUtil.parse(phoneNumber, country.getCode());
            return phoneUtil.isValidNumberForRegion(parsedNumber, country.getCode());
        } catch (NumberParseException e) {
            return false;
        }
    }

    /**
     * Validate phone number (auto-detect country)
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        try {
            PhoneNumber parsedNumber = phoneUtil.parse(phoneNumber, null);
            return phoneUtil.isValidNumber(parsedNumber);
        } catch (NumberParseException e) {
            return false;
        }
    }

    /**
     * Format phone number to E164 format (+221771234567)
     */
    public static String formatToE164(String phoneNumber, Country country) {
        if (!isValidPhoneNumber(phoneNumber, country)) {
            throw new IllegalArgumentException("Numéro de téléphone invalide: " + phoneNumber);
        }

        try {
            PhoneNumber parsedNumber = phoneUtil.parse(phoneNumber, country.getCode());
            return phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Impossible de formater le numéro: " + phoneNumber, e);
        }
    }

    /**
     * Format phone number to international format (+221 77 123 45 67)
     */
    public static String formatToInternational(String phoneNumber, Country country) {
        if (!isValidPhoneNumber(phoneNumber, country)) {
            throw new IllegalArgumentException("Numéro de téléphone invalide: " + phoneNumber);
        }

        try {
            PhoneNumber parsedNumber = phoneUtil.parse(phoneNumber, country.getCode());
            return phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Impossible de formater le numéro: " + phoneNumber, e);
        }
    }

    /**
     * Format phone number to national format (77 123 45 67)
     */
    public static String formatToNational(String phoneNumber, Country country) {
        if (!isValidPhoneNumber(phoneNumber, country)) {
            throw new IllegalArgumentException("Numéro de téléphone invalide: " + phoneNumber);
        }

        try {
            PhoneNumber parsedNumber = phoneUtil.parse(phoneNumber, country.getCode());
            return phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Impossible de formater le numéro: " + phoneNumber, e);
        }
    }

    /**
     * Extract country code from phone number (+221 -> SN)
     */
    public static String extractCountryCode(String phoneNumber) {
        try {
            PhoneNumber parsedNumber = phoneUtil.parse(phoneNumber, null);
            String regionCode = phoneUtil.getRegionCodeForNumber(parsedNumber);
            return regionCode != null ? regionCode : "UNKNOWN";
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Numéro de téléphone invalide: " + phoneNumber, e);
        }
    }

    /**
     * Extract Country enum from phone number
     */
    public static Country extractCountry(String phoneNumber) {
        String countryCode = extractCountryCode(phoneNumber);

        try {
            return Country.fromCode(countryCode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Pays non supporté: " + countryCode);
        }
    }

    /**
     * Normalize phone number (remove spaces, dashes, parentheses)
     */
    public static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
    }

    /**
     * Check if phone number is mobile (vs landline)
     */
    public static boolean isMobileNumber(String phoneNumber, Country country) {
        if (!isValidPhoneNumber(phoneNumber, country)) {
            return false;
        }

        try {
            PhoneNumber parsedNumber = phoneUtil.parse(phoneNumber, country.getCode());
            PhoneNumberUtil.PhoneNumberType type = phoneUtil.getNumberType(parsedNumber);
            return type == PhoneNumberUtil.PhoneNumberType.MOBILE ||
                   type == PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE;
        } catch (NumberParseException e) {
            return false;
        }
    }

    /**
     * Validate and format phone number in one call
     */
    public static String validateAndFormat(String phoneNumber, Country country) {
        if (!isValidPhoneNumber(phoneNumber, country)) {
            throw new IllegalArgumentException(
                "Numéro de téléphone invalide pour " + country.getDisplayName() + ": " + phoneNumber
            );
        }

        return formatToE164(phoneNumber, country);
    }
}
```

#### Phone Number Examples

```java
// Validate phone number
boolean valid = PhoneNumberUtils.isValidPhoneNumber("771234567", Country.SENEGAL);  // true
boolean valid2 = PhoneNumberUtils.isValidPhoneNumber("+221771234567", Country.SENEGAL);  // true

// Format to E164 (storage format)
String e164 = PhoneNumberUtils.formatToE164("77 123 45 67", Country.SENEGAL);
// "+221771234567"

// Format to international (display format)
String intl = PhoneNumberUtils.formatToInternational("+221771234567", Country.SENEGAL);
// "+221 77 123 45 67"

// Format to national
String national = PhoneNumberUtils.formatToNational("+221771234567", Country.SENEGAL);
// "77 123 45 67"

// Extract country
Country country = PhoneNumberUtils.extractCountry("+221771234567");
// Country.SENEGAL

// Check if mobile
boolean isMobile = PhoneNumberUtils.isMobileNumber("+221771234567", Country.SENEGAL);
// true

// Validate and format in one call
String formatted = PhoneNumberUtils.validateAndFormat("771234567", Country.SENEGAL);
// "+221771234567"
```

---

### 3. DateUtils.java

**Package**: `com.payment.common.util`

```java
package com.payment.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Utility class for date and time operations.
 */
public final class DateUtils {

    public static final ZoneId DAKAR_ZONE = ZoneId.of("Africa/Dakar");  // GMT+0
    public static final ZoneId YAOUNDE_ZONE = ZoneId.of("Africa/Douala");  // GMT+1

    private DateUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Get current instant
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Get current date in Dakar timezone
     */
    public static LocalDate today() {
        return LocalDate.now(DAKAR_ZONE);
    }

    /**
     * Get current date/time in Dakar timezone
     */
    public static LocalDateTime nowInDakar() {
        return LocalDateTime.now(DAKAR_ZONE);
    }

    /**
     * Get current date/time in Yaoundé timezone
     */
    public static LocalDateTime nowInYaounde() {
        return LocalDateTime.now(YAOUNDE_ZONE);
    }

    /**
     * Get start of day (00:00:00)
     */
    public static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(DAKAR_ZONE).toInstant();
    }

    /**
     * Get end of day (23:59:59.999)
     */
    public static Instant endOfDay(LocalDate date) {
        return date.atTime(23, 59, 59, 999_999_999).atZone(DAKAR_ZONE).toInstant();
    }

    /**
     * Get start of month
     */
    public static Instant startOfMonth(YearMonth yearMonth) {
        LocalDate firstDay = yearMonth.atDay(1);
        return startOfDay(firstDay);
    }

    /**
     * Get end of month
     */
    public static Instant endOfMonth(YearMonth yearMonth) {
        LocalDate lastDay = yearMonth.atEndOfMonth();
        return endOfDay(lastDay);
    }

    /**
     * Add days to instant
     */
    public static Instant addDays(Instant instant, long days) {
        return instant.plus(days, ChronoUnit.DAYS);
    }

    /**
     * Add months to instant
     */
    public static Instant addMonths(Instant instant, long months) {
        return LocalDateTime.ofInstant(instant, DAKAR_ZONE)
            .plusMonths(months)
            .atZone(DAKAR_ZONE)
            .toInstant();
    }

    /**
     * Add years to instant
     */
    public static Instant addYears(Instant instant, long years) {
        return LocalDateTime.ofInstant(instant, DAKAR_ZONE)
            .plusYears(years)
            .atZone(DAKAR_ZONE)
            .toInstant();
    }

    /**
     * Calculate days between two instants
     */
    public static long daysBetween(Instant start, Instant end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Check if instant is in the past
     */
    public static boolean isPast(Instant instant) {
        return instant.isBefore(Instant.now());
    }

    /**
     * Check if instant is in the future
     */
    public static boolean isFuture(Instant instant) {
        return instant.isAfter(Instant.now());
    }

    /**
     * Check if instant is today
     */
    public static boolean isToday(Instant instant) {
        LocalDate date = LocalDateTime.ofInstant(instant, DAKAR_ZONE).toLocalDate();
        return date.equals(today());
    }

    /**
     * Format instant to French date format (18/10/2025)
     */
    public static String formatFrenchDate(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(DAKAR_ZONE);
        return formatter.format(instant);
    }

    /**
     * Format instant to French date/time format (18/10/2025 14:30)
     */
    public static String formatFrenchDateTime(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(DAKAR_ZONE);
        return formatter.format(instant);
    }

    /**
     * Parse French date format (18/10/2025)
     */
    public static LocalDate parseFrenchDate(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return LocalDate.parse(dateStr, formatter);
    }

    /**
     * Get age from birth date
     */
    public static int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, today()).getYears();
    }

    /**
     * Check if user is adult (>= 18 years)
     */
    public static boolean isAdult(LocalDate birthDate) {
        return calculateAge(birthDate) >= 18;
    }

    /**
     * Get KYC expiration date (1 year from now)
     */
    public static Instant getKYCExpirationDate() {
        return addYears(now(), 1);
    }
}
```

---

### 4. ValidationUtils.java

**Package**: `com.payment.common.util`

```java
package com.payment.common.util;

import java.util.regex.Pattern;

/**
 * Common validation utilities.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    );

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Check if string is null or empty
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if string is not null and not empty
     */
    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (isNullOrEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate UUID format
     */
    public static boolean isValidUUID(String uuid) {
        if (isNullOrEmpty(uuid)) {
            return false;
        }
        return UUID_PATTERN.matcher(uuid.toLowerCase()).matches();
    }

    /**
     * Validate amount (positive, non-zero)
     */
    public static boolean isValidAmount(Long amount) {
        return amount != null && amount > 0;
    }

    /**
     * Require non-null
     */
    public static <T> T requireNonNull(T obj, String paramName) {
        if (obj == null) {
            throw new IllegalArgumentException(paramName + " ne peut pas être null");
        }
        return obj;
    }

    /**
     * Require non-empty string
     */
    public static String requireNonEmpty(String str, String paramName) {
        if (isNullOrEmpty(str)) {
            throw new IllegalArgumentException(paramName + " ne peut pas être vide");
        }
        return str;
    }

    /**
     * Require positive amount
     */
    public static long requirePositive(long amount, String paramName) {
        if (amount <= 0) {
            throw new IllegalArgumentException(paramName + " doit être positif");
        }
        return amount;
    }

    /**
     * Require non-negative amount
     */
    public static long requireNonNegative(long amount, String paramName) {
        if (amount < 0) {
            throw new IllegalArgumentException(paramName + " ne peut pas être négatif");
        }
        return amount;
    }
}
```

---

### 5. IdempotencyKeyGenerator.java

**Package**: `com.payment.common.util`

```java
package com.payment.common.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Generate unique idempotency keys for transactions.
 */
public final class IdempotencyKeyGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private IdempotencyKeyGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generate UUID-based idempotency key
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate secure random key (Base64 encoded)
     */
    public static String generateSecureKey(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate key with prefix (e.g., "TXN_abc123")
     */
    public static String generateWithPrefix(String prefix) {
        return prefix + "_" + generateSecureKey(16);
    }

    /**
     * Generate transaction idempotency key
     */
    public static String generateTransactionKey() {
        return generateWithPrefix("TXN");
    }
}
```

---

### 6. FormatUtils.java

**Package**: `com.payment.common.util`

```java
package com.payment.common.util;

/**
 * Formatting utilities.
 */
public final class FormatUtils {

    private FormatUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Mask phone number (keep first 3 and last 2 digits)
     * Example: +221771234567 -> +221****67
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 5) {
            return phoneNumber;
        }

        String prefix = phoneNumber.substring(0, Math.min(4, phoneNumber.length()));
        String suffix = phoneNumber.substring(Math.max(0, phoneNumber.length() - 2));
        int maskLength = phoneNumber.length() - prefix.length() - suffix.length();

        return prefix + "*".repeat(maskLength) + suffix;
    }

    /**
     * Mask email (keep first 2 chars and domain)
     * Example: john.doe@example.com -> jo******@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        if (parts[0].length() <= 2) {
            return parts[0] + "@" + parts[1];
        }

        String prefix = parts[0].substring(0, 2);
        return prefix + "****@" + parts[1];
    }

    /**
     * Truncate string to max length
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * Capitalize first letter
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
```

---

## Validation Annotations

### @ValidPhoneNumber

**Package**: `com.payment.common.annotation`

```java
package com.payment.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates phone number format for WAEMU/CEMAC countries.
 *
 * Usage:
 * @ValidPhoneNumber(country = "SN")
 * private String phoneNumber;
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPhoneNumberValidator.class)
@Documented
public @interface ValidPhoneNumber {

    String message() default "Numéro de téléphone invalide";

    String country() default "";  // ISO code (e.g., "SN")

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

### ValidPhoneNumberValidator

```java
package com.payment.common.annotation;

import com.payment.common.enums.Country;
import com.payment.common.util.PhoneNumberUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for @ValidPhoneNumber annotation.
 */
public class ValidPhoneNumberValidator
        implements ConstraintValidator<ValidPhoneNumber, String> {

    private String countryCode;

    @Override
    public void initialize(ValidPhoneNumber annotation) {
        this.countryCode = annotation.country();
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // If country specified, validate for that country
        if (countryCode != null && !countryCode.isEmpty()) {
            try {
                Country country = Country.fromCode(countryCode);
                return PhoneNumberUtils.isValidPhoneNumber(phoneNumber, country);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        // Otherwise, validate with auto-detect
        return PhoneNumberUtils.isValidPhoneNumber(phoneNumber);
    }
}
```

### @ValidCurrency

```java
package com.payment.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates currency code (XOF or XAF).
 *
 * Usage:
 * @ValidCurrency
 * private String currency;
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCurrencyValidator.class)
@Documented
public @interface ValidCurrency {

    String message() default "Devise invalide";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

### ValidCurrencyValidator

```java
package com.payment.common.annotation;

import com.payment.common.enums.Currency;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for @ValidCurrency annotation.
 */
public class ValidCurrencyValidator
        implements ConstraintValidator<ValidCurrency, String> {

    @Override
    public boolean isValid(String currencyCode, ConstraintValidatorContext context) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return false;
        }

        try {
            Currency.fromCode(currencyCode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
```

### @ValidAmount

```java
package com.payment.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates amount (positive, non-zero).
 *
 * Usage:
 * @ValidAmount
 * private Long amount;
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidAmountValidator.class)
@Documented
public @interface ValidAmount {

    String message() default "Montant invalide";

    long min() default 1;

    long max() default Long.MAX_VALUE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

### ValidAmountValidator

```java
package com.payment.common.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for @ValidAmount annotation.
 */
public class ValidAmountValidator
        implements ConstraintValidator<ValidAmount, Long> {

    private long min;
    private long max;

    @Override
    public void initialize(ValidAmount annotation) {
        this.min = annotation.min();
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(Long amount, ConstraintValidatorContext context) {
        if (amount == null) {
            return false;
        }

        return amount >= min && amount <= max;
    }
}
```

---

## Constants

### RegexPatterns.java

**Package**: `com.payment.common.constant`

```java
package com.payment.common.constant;

/**
 * Regex patterns for validation.
 */
public final class RegexPatterns {

    private RegexPatterns() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String UUID = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
    public static final String PHONE_INTERNATIONAL = "^\\+[1-9]\\d{1,14}$";
    public static final String ALPHA_NUMERIC = "^[a-zA-Z0-9]+$";
    public static final String ALPHA_ONLY = "^[a-zA-Z]+$";
    public static final String NUMERIC_ONLY = "^[0-9]+$";
}
```

### DateTimeFormats.java

```java
package com.payment.common.constant;

/**
 * Date/time format constants.
 */
public final class DateTimeFormats {

    private DateTimeFormats() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String FRENCH_DATE = "dd/MM/yyyy";
    public static final String FRENCH_DATETIME = "dd/MM/yyyy HH:mm";
    public static final String FRENCH_DATETIME_SECONDS = "dd/MM/yyyy HH:mm:ss";
    public static final String ISO_DATE = "yyyy-MM-dd";
    public static final String ISO_DATETIME = "yyyy-MM-dd'T'HH:mm:ss'Z'";
}
```

### TransactionLimits.java

```java
package com.payment.common.constant;

/**
 * BCEAO transaction limits (amounts in XOF).
 */
public final class TransactionLimits {

    private TransactionLimits() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Free transfer threshold
    public static final long FREE_TRANSFER_THRESHOLD = 5_000L;  // XOF

    // Fee constants
    public static final long BASE_FEE = 100L;                   // XOF
    public static final double FEE_PERCENTAGE = 0.005;          // 0.5%
    public static final long MAX_FEE = 1_000L;                  // XOF

    // KYC Level 1 limits
    public static final long LEVEL_1_TRANSACTION = 50_000L;     // XOF
    public static final long LEVEL_1_DAILY = 100_000L;          // XOF
    public static final long LEVEL_1_MONTHLY = 500_000L;        // XOF

    // KYC Level 2 limits
    public static final long LEVEL_2_TRANSACTION = 250_000L;    // XOF
    public static final long LEVEL_2_DAILY = 500_000L;          // XOF
    public static final long LEVEL_2_MONTHLY = 5_000_000L;      // XOF

    // KYC Level 3 limits
    public static final long LEVEL_3_TRANSACTION = 1_000_000L;  // XOF
    public static final long LEVEL_3_DAILY = 2_000_000L;        // XOF
    public static final long LEVEL_3_MONTHLY = 20_000_000L;     // XOF

    // Minimum age
    public static final int MINIMUM_AGE = 18;                   // years
}
```

### Messages.java

```java
package com.payment.common.constant;

/**
 * Common message keys (for i18n).
 */
public final class Messages {

    private Messages() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String VALIDATION_ERROR = "validation.error";
    public static final String RESOURCE_NOT_FOUND = "resource.not.found";
    public static final String INSUFFICIENT_BALANCE = "insufficient.balance";
    public static final String LIMIT_EXCEEDED = "limit.exceeded";
}
```

---

## Resource Files

### messages_fr.properties

**File**: `src/main/resources/messages_fr.properties`

```properties
# Success messages
success=Opération réussie
transaction.success=Transaction créée avec succès
user.created=Utilisateur créé avec succès

# Error messages
error=Une erreur est survenue
validation.error=Erreur de validation
resource.not.found=Ressource non trouvée
insufficient.balance=Solde insuffisant
limit.exceeded=Limite dépassée

# Validation messages
invalid.phone.number=Numéro de téléphone invalide
invalid.email=Adresse email invalide
invalid.amount=Montant invalide
invalid.currency=Devise invalide
required.field=Champ obligatoire

# Business rule messages
kyc.upgrade.required=Mise à niveau KYC requise
account.frozen=Compte gelé
daily.limit.exceeded=Limite quotidienne dépassée
monthly.limit.exceeded=Limite mensuelle dépassée
```

### messages_en.properties

**File**: `src/main/resources/messages_en.properties`

```properties
# Success messages (internal use)
success=Operation successful
transaction.success=Transaction created successfully
user.created=User created successfully

# Error messages
error=An error occurred
validation.error=Validation error
resource.not.found=Resource not found
insufficient.balance=Insufficient balance
limit.exceeded=Limit exceeded
```

---

## Testing Guide

### Unit Test Example - MoneyUtilsTest.java

```java
package com.payment.common.util;

import com.payment.common.dto.Money;
import com.payment.common.enums.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MoneyUtils Tests")
class MoneyUtilsTest {

    @Test
    @DisplayName("Calculate fee - Free for amounts <= 5,000 XOF")
    void calculateTransferFee_freeForSmallAmounts() {
        Money amount = Money.ofXOF(5000L);
        Money fee = MoneyUtils.calculateTransferFee(amount);

        assertThat(fee.getAmount()).isEqualTo(0L);
        assertThat(fee.getCurrency()).isEqualTo(Currency.XOF);
    }

    @Test
    @DisplayName("Calculate fee - 100 + 0.5% for amounts > 5,000 XOF")
    void calculateTransferFee_chargesForLargeAmounts() {
        // 10,000 XOF -> 100 + 0.5% = 100 + 50 = 150 XOF
        Money amount = Money.ofXOF(10_000L);
        Money fee = MoneyUtils.calculateTransferFee(amount);

        assertThat(fee.getAmount()).isEqualTo(150L);
    }

    @Test
    @DisplayName("Calculate fee - Capped at 1,000 XOF")
    void calculateTransferFee_cappedAt1000XOF() {
        Money amount = Money.ofXOF(1_000_000L);
        Money fee = MoneyUtils.calculateTransferFee(amount);

        assertThat(fee.getAmount()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Parse amount - Valid XOF amount")
    void parseAmount_validXOFAmount() {
        Money money = MoneyUtils.parseAmount("5000", Currency.XOF);

        assertThat(money.getAmount()).isEqualTo(5000L);
        assertThat(money.getCurrency()).isEqualTo(Currency.XOF);
    }

    @Test
    @DisplayName("Parse amount - French format with spaces")
    void parseAmount_withSpaces_frenchFormat() {
        Money money = MoneyUtils.parseAmount("5 000", Currency.XOF);

        assertThat(money.getAmount()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("Convert XOF to XAF - 1:1 exchange rate")
    void convertXOFtoXAF_fixedExchangeRate() {
        Money xof = Money.ofXOF(10_000L);
        Money xaf = MoneyUtils.convertXOFtoXAF(xof);

        assertThat(xaf.getAmount()).isEqualTo(10_000L);
        assertThat(xaf.getCurrency()).isEqualTo(Currency.XAF);
    }

    @Test
    @DisplayName("Calculate percentage - 10% of 10,000 XOF")
    void calculatePercentage_tenPercent() {
        Money amount = Money.ofXOF(10_000L);
        Money percentage = MoneyUtils.calculatePercentage(amount, 10.0);

        assertThat(percentage.getAmount()).isEqualTo(1_000L);
    }
}
```

---

## Complete Implementation Checklist

### Phase 1: Core Value Objects (Week 1)
- [ ] Implement `Money.java` with all operations
- [ ] Write unit tests for `Money` (15+ test cases)
- [ ] Implement `Currency` enum
- [ ] Implement `Country` enum
- [ ] Implement `KYCLevel` enum with limits

### Phase 2: DTOs (Week 1)
- [ ] Implement `ApiResponse.java`
- [ ] Implement `ErrorResponse.java`
- [ ] Implement `ValidationError.java`
- [ ] Implement `PageResponse.java`

### Phase 3: Enumerations (Week 1)
- [ ] Implement `TransactionType` enum
- [ ] Implement `TransactionStatus` enum
- [ ] Implement `UserStatus` enum
- [ ] Implement `AccountStatus` enum
- [ ] Implement `ErrorCode` enum (30+ error codes)

### Phase 4: Exceptions (Week 2)
- [ ] Implement `BaseException`
- [ ] Implement `ValidationException`
- [ ] Implement `ResourceNotFoundException`
- [ ] Implement `BusinessRuleException`
- [ ] Implement `InsufficientBalanceException`
- [ ] Implement `LimitExceededException`
- [ ] Implement `DuplicateResourceException`

### Phase 5: Utilities (Week 2)
- [ ] Implement `MoneyUtils` with fee calculation
- [ ] Write unit tests for `MoneyUtils` (20+ test cases)
- [ ] Implement `PhoneNumberUtils`
- [ ] Write unit tests for `PhoneNumberUtils` (14 countries)
- [ ] Implement `DateUtils`
- [ ] Implement `ValidationUtils`
- [ ] Implement `IdempotencyKeyGenerator`
- [ ] Implement `FormatUtils`

### Phase 6: Validation Annotations (Week 3)
- [ ] Implement `@ValidPhoneNumber` annotation
- [ ] Implement `ValidPhoneNumberValidator`
- [ ] Implement `@ValidCurrency` annotation
- [ ] Implement `ValidCurrencyValidator`
- [ ] Implement `@ValidAmount` annotation
- [ ] Implement `ValidAmountValidator`

### Phase 7: Constants (Week 3)
- [ ] Implement `RegexPatterns`
- [ ] Implement `DateTimeFormats`
- [ ] Implement `TransactionLimits`
- [ ] Implement `Messages`

### Phase 8: Resources (Week 3)
- [ ] Create `messages_fr.properties`
- [ ] Create `messages_en.properties`

### Phase 9: Testing & Quality (Week 4)
- [ ] Achieve 80%+ test coverage
- [ ] Add integration tests
- [ ] Code review
- [ ] Documentation review
- [ ] Performance testing

### Phase 10: Publishing (Week 4)
- [ ] Build library: `./gradlew build`
- [ ] Run all tests: `./gradlew test`
- [ ] Publish to local Maven: `./gradlew publishToMavenLocal`
- [ ] Create README.md
- [ ] Tag version 1.0.0
- [ ] Publish to artifact repository

---

## Summary

This document provides **complete specifications** for all Java code in `payment-common-lib`:

- ✅ **15+ Classes** fully specified with implementation details
- ✅ **10+ Enumerations** for type safety
- ✅ **7+ Utility classes** with BCEAO compliance
- ✅ **3+ Validation annotations** for bean validation
- ✅ **XOF/XAF handling** - No decimals, whole numbers only
- ✅ **French language** - All user-facing messages
- ✅ **14 WAEMU/CEMAC countries** - Phone validation
- ✅ **3-tier KYC system** - Transaction limits
- ✅ **Fee calculation** - BCEAO rules (free ≤ 5,000 XOF, capped at 1,000 XOF)

**Total Lines of Code**: ~3,500 lines
**Estimated Implementation Time**: 3-4 weeks
**Test Coverage Target**: 80%+

---

**Ready for implementation!** 🚀
