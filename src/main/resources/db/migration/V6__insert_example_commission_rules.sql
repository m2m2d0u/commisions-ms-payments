-- V6: Insert example commission rules for SAME_WALLET transfers
-- All fees are capped at maximum 5% to comply with regulatory requirements
-- These examples demonstrate various commission structures for testing and reference

-- ============================================================================
-- XOF Currency Examples (West African CFA Franc)
-- ============================================================================

-- Example 1: Pure percentage fee (no fixed component)
-- Small transactions: 1% fee with minimum 50 XOF
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XOF', 'SAME_WALLET', 1000, 10000,
    'ANY', 0.0100, 0, 50, NULL,
    80, 'Example: 1% fee for small transactions (1K-10K XOF)',
    'Pure percentage-based fee with minimum threshold'
);

-- Example 2: Percentage + fixed fee combination
-- Medium transactions: 0.5% + 200 XOF fixed, capped at 1,500 XOF
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XOF', 'SAME_WALLET', 10001, 50000,
    'ANY', 0.0050, 200, 200, 1500,
    70, 'Example: 0.5% + 200 XOF for medium transactions (10K-50K)',
    'Hybrid fee structure with cap'
);

-- Example 3: Low percentage with fixed fee for large transactions
-- 0.3% + 500 XOF, minimum 500 XOF, capped at 3,000 XOF
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XOF', 'SAME_WALLET', 50001, 200000,
    'ANY', 0.0030, 500, 500, 3000,
    60, 'Example: 0.3% + 500 XOF for large transactions (50K-200K)',
    'Lower percentage for high-value transactions'
);

-- Example 4: VIP rate for very large transactions
-- Pure percentage: 0.2%, capped at 5,000 XOF
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XOF', 'SAME_WALLET', 200001, NULL,
    'LEVEL_3', 0.0020, 0, 0, 5000,
    50, 'Example: VIP rate 0.2% for high-value transactions (>200K)',
    'Preferential rate for fully verified users with large transactions'
);

-- Example 5: Pure fixed fee (no percentage)
-- Flat 100 XOF for micro transactions
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XOF', 'SAME_WALLET', 500, 5000,
    'ANY', 0.0000, 100, 100, 100,
    75, 'Example: Flat 100 XOF for micro transactions (500-5K)',
    'Pure fixed fee, no percentage component'
);

-- Example 6: Tiered rate with KYC requirement
-- 0.4% for LEVEL_2 users, medium-high transactions
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XOF', 'SAME_WALLET', 25000, 100000,
    'LEVEL_2', 0.0040, 0, 100, 2500,
    65, 'Example: 0.4% for LEVEL_2 users (25K-100K)',
    'Discount rate for verified users'
);

-- Example 7: High percentage for basic KYC (LEVEL_1)
-- 2% fee, demonstrates higher rate for unverified users
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XOF', 'SAME_WALLET', 5001, 25000,
    'LEVEL_1', 0.0200, 0, 150, 1000,
    85, 'Example: 2% for LEVEL_1 users (5K-25K)',
    'Higher rate for basic verification level'
);

-- Example 8: Maximum 5% rate
-- Demonstrates upper limit, flat 5% with no cap
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XOF', 'SAME_WALLET', 100, 1000,
    'ANY', 0.0500, 0, 5, 50,
    95, 'Example: Maximum 5% for tiny transactions (100-1K)',
    'Upper regulatory limit demonstration'
);

-- ============================================================================
-- XAF Currency Examples (Central African CFA Franc)
-- ============================================================================

-- Example 9: Standard rate for XAF small transactions
-- 1.5% + 50 XAF fixed
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XAF', 'SAME_WALLET', 1000, 20000,
    'ANY', 0.0150, 50, 50, 500,
    80, 'Example: 1.5% + 50 XAF for small transactions',
    'XAF standard small transaction rate'
);

-- Example 10: Pure percentage for XAF medium transactions
-- 0.8% with 1,200 XAF cap
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XAF', 'SAME_WALLET', 20001, 100000,
    'ANY', 0.0080, 0, 100, 1200,
    70, 'Example: 0.8% for XAF medium transactions (20K-100K)',
    'Pure percentage with cap'
);

-- Example 11: VIP XAF rate with KYC LEVEL_3
-- 0.25% for premium users, minimum 200 XAF
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XAF', 'SAME_WALLET', 50000, NULL,
    'LEVEL_3', 0.0025, 0, 200, 4000,
    60, 'Example: VIP 0.25% for XAF high-value (>50K)',
    'Premium rate for fully verified XAF users'
);

-- Example 12: Fixed fee for XAF micro payments
-- Flat 75 XAF
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XAF', 'SAME_WALLET', 500, 3000,
    'ANY', 0.0000, 75, 75, 75,
    90, 'Example: Flat 75 XAF for micro payments (500-3K)',
    'Fixed fee for small XAF transactions'
);

-- Example 13: Hybrid rate for XAF
-- 0.6% + 150 XAF, min 200, max 2,000
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XAF', 'SAME_WALLET', 10000, 75000,
    'LEVEL_2', 0.0060, 150, 200, 2000,
    65, 'Example: 0.6% + 150 XAF for LEVEL_2 XAF users',
    'Hybrid fee with KYC discount'
);

-- Example 14: High rate for LEVEL_1 XAF
-- 3% demonstrating penalty for low verification
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XAF', 'SAME_WALLET', 3001, 15000,
    'LEVEL_1', 0.0300, 0, 100, 800,
    85, 'Example: 3% for LEVEL_1 XAF users (3K-15K)',
    'Higher rate incentivizes KYC completion'
);

-- Example 15: Promotional rate
-- Very low 0.1% for specific tier, no fixed fee
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes, effective_to
) VALUES (
    'XAF', 'SAME_WALLET', 5000, 30000,
    'ANY', 0.0010, 0, 50, 300,
    95, 'Example: Promotional 0.1% rate (5K-30K XAF)',
    'Limited-time promotional rate',
    CURRENT_TIMESTAMP + INTERVAL '90 days'
);

-- Example 16: 4% flat rate for comparison
-- Demonstrates mid-high percentage
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XOF', 'SAME_WALLET', 2000, 8000,
    'ANY', 0.0400, 0, 80, 320,
    78, 'Example: 4% flat rate (2K-8K XOF)',
    'Demonstrates mid-high percentage tier'
);

-- Example 17: Combination with higher fixed component
-- 0.7% + 300 XOF
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes
) VALUES (
    'XOF', 'SAME_WALLET', 15000, 60000,
    'ANY', 0.0070, 300, 300, 2000,
    68, 'Example: 0.7% + 300 XOF (15K-60K)',
    'Higher fixed component example'
);

-- Example 18: Zero fee tier (promotional/special case)
-- Free for specific transaction range with LEVEL_3
INSERT INTO commission_rules (
    currency, transfer_type, min_transaction, max_transaction,
    kyc_level, percentage, fixed_amount, min_amount, max_amount,
    priority, description, notes, effective_to
) VALUES (
    'XOF', 'SAME_WALLET', 10000, 15000,
    'LEVEL_3', 0.0000, 0, 0, 0,
    100, 'Example: Free tier for LEVEL_3 users (10K-15K)',
    'Promotional: free transfers for verified users in this range',
    CURRENT_TIMESTAMP + INTERVAL '60 days'
);

-- Add indexes for efficient querying of example rules
CREATE INDEX IF NOT EXISTS idx_commission_rules_examples ON commission_rules(currency, transfer_type, kyc_level)
WHERE description LIKE 'Example:%';

-- Summary comment
COMMENT ON TABLE commission_rules IS 'Commission rules with flexible fee structures: percentage-based, fixed-fee, or hybrid models with min/max caps';
