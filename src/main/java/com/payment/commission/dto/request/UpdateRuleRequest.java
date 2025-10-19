package com.payment.commission.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for updating a commission rule
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRuleRequest {

    @DecimalMin(value = "0.0", message = "{validation.percentage.min}")
    @DecimalMax(value = "1.0", message = "{validation.percentage.max}")
    private BigDecimal percentage;

    @Min(value = 0, message = "{validation.fixed.amount.min}")
    private Long fixedAmount;

    @Min(value = 0, message = "{validation.min.amount.min}")
    private Long minAmount;

    private Long maxAmount;

    private Boolean isActive;

    @Min(value = 0, message = "{validation.priority.min}")
    private Integer priority;

    private LocalDateTime effectiveTo;

    @Size(max = 500, message = "{validation.description.max}")
    private String description;

    private String notes;
}
