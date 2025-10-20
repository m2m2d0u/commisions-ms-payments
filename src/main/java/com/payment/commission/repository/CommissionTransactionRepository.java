package com.payment.commission.repository;

import com.payment.commission.domain.entity.CommissionTransaction;
import com.payment.commission.domain.enums.CommissionStatus;
import com.payment.common.enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Commission Transactions
 */
@Repository
public interface CommissionTransactionRepository extends JpaRepository<CommissionTransaction, UUID> {

    /**
     * Find commission by transaction ID
     */
    Optional<CommissionTransaction> findByTransactionId(UUID transactionId);

    /**
     * Find commissions by currency
     */
    List<CommissionTransaction> findByCurrency(Currency currency);

    /**
     * Find commissions by status
     */
    List<CommissionTransaction> findByStatus(CommissionStatus status);

    /**
     * Find unsettled commissions
     */
    List<CommissionTransaction> findBySettledFalse();

    /**
     * Calculate total revenue within a date range
     */
    @Query("SELECT SUM(ct.amount) FROM CommissionTransaction ct " +
           "WHERE ct.status = 'COMPLETED' " +
           "AND ct.createdAt BETWEEN :startDate AND :endDate")
    Long calculateTotalRevenueByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Calculate total revenue by currency within a date range
     */
    @Query("SELECT SUM(ct.amount) FROM CommissionTransaction ct " +
           "WHERE ct.currency = :currency " +
           "AND ct.status = 'COMPLETED' " +
           "AND ct.createdAt BETWEEN :startDate AND :endDate")
    Long calculateTotalRevenueByCurrencyAndDateRange(
            @Param("currency") Currency currency,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count transactions within a date range
     */
    @Query("SELECT COUNT(ct) FROM CommissionTransaction ct " +
           "WHERE ct.status = 'COMPLETED' " +
           "AND ct.createdAt BETWEEN :startDate AND :endDate")
    Long countTransactionsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find commissions created within a date range
     */
    List<CommissionTransaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
