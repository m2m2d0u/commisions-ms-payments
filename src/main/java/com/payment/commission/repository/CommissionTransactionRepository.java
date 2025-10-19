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
     * Find all commissions for a provider
     */
    List<CommissionTransaction> findByProviderId(UUID providerId);

    /**
     * Find commissions by provider and currency
     */
    List<CommissionTransaction> findByProviderIdAndCurrency(UUID providerId, Currency currency);

    /**
     * Find commissions by status
     */
    List<CommissionTransaction> findByStatus(CommissionStatus status);

    /**
     * Find unsettled commissions
     */
    List<CommissionTransaction> findBySettledFalse();

    /**
     * Find unsettled commissions for a provider
     */
    List<CommissionTransaction> findByProviderIdAndSettledFalse(UUID providerId);

    /**
     * Calculate total revenue for a provider within a date range
     */
    @Query("SELECT SUM(ct.amount) FROM CommissionTransaction ct " +
           "WHERE ct.providerId = :providerId " +
           "AND ct.status = 'COMPLETED' " +
           "AND ct.createdAt BETWEEN :startDate AND :endDate")
    Long calculateTotalRevenueByProviderAndDateRange(
            @Param("providerId") UUID providerId,
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
     * Count transactions for a provider within a date range
     */
    @Query("SELECT COUNT(ct) FROM CommissionTransaction ct " +
           "WHERE ct.providerId = :providerId " +
           "AND ct.status = 'COMPLETED' " +
           "AND ct.createdAt BETWEEN :startDate AND :endDate")
    Long countTransactionsByProviderAndDateRange(
            @Param("providerId") UUID providerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find commissions created within a date range
     */
    List<CommissionTransaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find commissions by provider and date range
     */
    @Query("SELECT ct FROM CommissionTransaction ct " +
           "WHERE ct.providerId = :providerId " +
           "AND ct.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ct.createdAt DESC")
    List<CommissionTransaction> findByProviderIdAndDateRange(
            @Param("providerId") UUID providerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
