package com.payment.commission.controller;

import com.payment.common.dto.commission.request.CalculateFeeRequest;
import com.payment.common.dto.commission.response.FeeCalculationResponse;
import com.payment.commission.service.CommissionService;
import com.payment.common.i18n.MessageService;
import com.payment.common.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Commission operations
 */
@RestController
@RequestMapping("/api/v1/commissions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Commissions", description = "Commission fee calculation APIs")
public class CommissionController {

    private final CommissionService commissionService;
    private final MessageService messageService;

    /**
     * Calculate transaction fee
     */
    @PostMapping("/calculate")
    @Operation(summary = "Calculate transaction fee", description = "Calculate commission fee for a transaction based on active rules")
    public ResponseEntity<ApiResponse<FeeCalculationResponse>> calculateFee(@Valid @RequestBody CalculateFeeRequest request) {
        log.info("Calculating fee for amount: {} {}", request.getAmount(), request.getCurrency());

        FeeCalculationResponse response = commissionService.calculateFee(request);

        return ResponseEntity.ok(
            ApiResponse.success(messageService.getMessage("success.fee.calculated"), response)
        );
    }

    /**
     * Calculate BCEAO-compliant fee
     */
    @GetMapping("/bceao-fee/{amount}")
    @Operation(summary = "Calculate BCEAO fee", description = "Calculate fee using BCEAO standard rules (FREE ≤5000 XOF, else 100 + 0.5% max 1000)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateBCEAOFee(@PathVariable Long amount) {
        log.info("Calculating BCEAO fee for amount: {}", amount);

        Long fee = commissionService.calculateBCEAOFee(amount);

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("currency", "XOF");
        data.put("commissionAmount", fee);
        data.put("rule", "BCEAO_STANDARD");

        return ResponseEntity.ok(
            ApiResponse.success(messageService.getMessage("success.bceao.fee.calculated"), data)
        );
    }
}
