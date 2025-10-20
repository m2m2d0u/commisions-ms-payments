package com.payment.commission.dto.request;

import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;
import com.payment.commission.domain.enums.TransferType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for calculating transaction fees
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateFeeRequest {

    @NotNull(message = "{validation.amount.required}")
    @Min(value = 1, message = "{validation.amount.min}")
    private Long amount;

    @NotNull(message = "{validation.currency.required}")
    private Currency currency;

    @NotNull(message = "{validation.transfer.type.required}")
    private TransferType transferType;

    private KYCLevel kycLevel;
}
