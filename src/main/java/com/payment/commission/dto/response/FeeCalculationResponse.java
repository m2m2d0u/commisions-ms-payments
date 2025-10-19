package com.payment.commission.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.payment.common.enums.Currency;
import com.payment.commission.domain.enums.TransferType;
import lombok.*;

import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for fee calculation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeeCalculationResponse {

    private Long amount;
    private Currency currency;
    private Long commissionAmount;
    private UUID ruleId;
    private TransferType transferType;
    private Map<String, Object> calculationDetails;
}
