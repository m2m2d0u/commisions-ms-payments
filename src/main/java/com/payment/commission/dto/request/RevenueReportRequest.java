package com.payment.commission.dto.request;

import com.payment.common.enums.Currency;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * Request DTO for revenue report
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportRequest {

    private Currency currency;

    @NotNull(message = "{validation.start.date.required}")
    private LocalDate startDate;

    @NotNull(message = "{validation.end.date.required}")
    private LocalDate endDate;

    private String groupBy; // CURRENCY, DAY, MONTH
}
