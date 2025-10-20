package com.payment.commission.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.payment.common.enums.Currency;
import com.payment.commission.domain.enums.TransferType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for fee calculation
 * Contains the calculated commission fee and related details
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeeCalculationResponse {

    /**
     * Transaction amount
     */
    @NotNull(message = "{validation.response.amount.required}")
    @Min(value = 1, message = "{validation.response.amount.min}")
    private Long amount;

    /**
     * Transaction currency
     */
    @NotNull(message = "{validation.response.currency.required}")
    private Currency currency;

    /**
     * Calculated commission amount
     */
    @NotNull(message = "{validation.response.commission.amount.required}")
    @Min(value = 0, message = "{validation.response.commission.amount.min}")
    private Long commissionAmount;

    /**
     * ID of the rule used for calculation
     * May be null for BCEAO default calculations
     */
    private UUID ruleId;

    /**
     * Transfer type
     */
    @NotNull(message = "{validation.response.transfer.type.required}")
    private TransferType transferType;

    /**
     * Additional calculation details
     */
    private Map<String, Object> calculationDetails;
}
