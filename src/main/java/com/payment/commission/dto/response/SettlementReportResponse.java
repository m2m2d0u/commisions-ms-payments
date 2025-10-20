package com.payment.commission.dto.response;

import com.payment.common.enums.Currency;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for settlement report
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementReportResponse {

    private Currency currency;
    private Long totalUnsettled;
    private Long totalSettled;
    private Long transactionCount;
    private List<SettlementItem> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SettlementItem {
        private UUID commissionId;
        private UUID transactionId;
        private Long amount;
        private Boolean settled;
        private LocalDateTime settlementDate;
        private LocalDateTime createdAt;
    }
}
