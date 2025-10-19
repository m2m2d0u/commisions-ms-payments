package com.payment.commission.dto.response;

import com.payment.common.enums.Currency;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for revenue report
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportResponse {

    private ReportPeriod reportPeriod;
    private Long totalRevenue;
    private Currency currency;
    private Long transactionCount;
    private Long averageCommission;
    private Long settledAmount;
    private Long unsettledAmount;
    private List<RevenueBreakdown> breakdown;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportPeriod {
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueBreakdown {
        private String providerId;
        private String providerName;
        private Long revenue;
        private Long transactionCount;
        private Long averageCommission;
        private Long settled;
        private Long unsettled;
    }
}
