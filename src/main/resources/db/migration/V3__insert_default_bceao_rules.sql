-- V3: Insert default BCEAO-compliant commission rules

-- Note: This inserts sample BCEAO rules
-- In production, these should be inserted via API or admin interface with actual provider IDs

-- BCEAO Standard Rule for SAME_WALLET transfers
-- Free for amounts <= 5,000 XOF
-- 100 XOF + 0.5% for amounts > 5,000 XOF, capped at 1,000 XOF
INSERT INTO commission_rules (
    provider_id,
    currency,
    transfer_type,
    min_transaction,
    max_transaction,
    kyc_level,
    percentage,
    fixed_amount,
    min_amount,
    max_amount,
    is_active,
    priority,
    effective_from,
    description
) VALUES
-- Rule 1: Free transactions <= 5,000 XOF (Financial Inclusion)
(
    '00000000-0000-0000-0000-000000000001'::UUID,  -- Placeholder provider ID
    'XOF',
    'SAME_WALLET',
    NULL,
    5000,
    'ANY',
    0.0000,
    0,
    0,
    0,
    TRUE,
    100,
    CURRENT_TIMESTAMP,
    'BCEAO: Gratuit pour transferts <= 5,000 XOF (inclusion financière)'
),
-- Rule 2: Standard fee for transactions > 5,000 XOF
(
    '00000000-0000-0000-0000-000000000001'::UUID,
    'XOF',
    'SAME_WALLET',
    5001,
    NULL,
    'ANY',
    0.0050,  -- 0.5%
    100,     -- 100 XOF fixed
    100,     -- Min 100 XOF
    1000,    -- Max 1,000 XOF
    TRUE,
    90,
    CURRENT_TIMESTAMP,
    'BCEAO: 100 XOF + 0.5%, plafonné à 1,000 XOF'
);

-- Add comment
COMMENT ON TABLE commission_rules IS 'Default BCEAO rules inserted. Replace provider_id with actual provider UUIDs in production.';
