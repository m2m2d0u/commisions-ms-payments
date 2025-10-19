package com.payment.commission.dto.request;

import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;
import com.payment.commission.domain.enums.TransferType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for creating a commission rule
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRuleRequest {

    @NotNull(message = "{validation.provider.required}")
    private UUID providerId;

    @NotNull(message = "{validation.currency.required}")
    private Currency currency;

    @NotNull(message = "{validation.transfer.type.required}")
    private TransferType transferType;

    private Long minTransaction;

    private Long maxTransaction;

    private KYCLevel kycLevel;

    @NotNull(message = "{validation.percentage.required}")
    @DecimalMin(value = "0.0", message = "{validation.percentage.min}")
    @DecimalMax(value = "1.0", message = "{validation.percentage.max}")
    private BigDecimal percentage;

    @Min(value = 0, message = "{validation.fixed.amount.min}")
    private Long fixedAmount;

    @Min(value = 0, message = "{validation.min.amount.min}")
    private Long minAmount;

    private Long maxAmount;

    @Min(value = 0, message = "{validation.priority.min}")
    private Integer priority;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    @Size(max = 500, message = "{validation.description.max}")
    private String description;

    private String notes;
}
