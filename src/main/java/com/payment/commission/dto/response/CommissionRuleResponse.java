package com.payment.commission.dto.response;

import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;
import com.payment.commission.domain.enums.TransferType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for commission rule
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRuleResponse {

    private UUID ruleId;
    private UUID providerId;
    private String providerName;
    private Currency currency;
    private TransferType transferType;
    private Long minTransaction;
    private Long maxTransaction;
    private KYCLevel kycLevel;
    private BigDecimal percentage;
    private Long fixedAmount;
    private Long minAmount;
    private Long maxAmount;
    private Boolean isActive;
    private Integer priority;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String description;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
