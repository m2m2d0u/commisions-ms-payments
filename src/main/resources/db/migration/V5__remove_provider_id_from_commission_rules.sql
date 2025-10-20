-- V5: Remove provider_id from commission_rules and commission_transactions
-- This migration removes provider-specific columns to make commission rules function as a wallet

-- Drop indexes that reference provider_id in commission_rules
DROP INDEX IF EXISTS idx_commission_rules_provider;
DROP INDEX IF EXISTS idx_commission_rules_provider_currency;
DROP INDEX IF EXISTS idx_commission_rules_provider_type;
DROP INDEX IF EXISTS idx_commission_rules_lookup;

-- Drop the unique constraint that includes provider_id
ALTER TABLE commission_rules DROP CONSTRAINT IF EXISTS commission_rules_provider_id_currency_transfer_type_priority_eff_key;

-- Remove provider_id column from commission_rules
ALTER TABLE commission_rules DROP COLUMN IF EXISTS provider_id;

-- Add new unique constraint without provider_id
ALTER TABLE commission_rules ADD CONSTRAINT commission_rules_currency_transfer_type_priority_effective_from_key
    UNIQUE(currency, transfer_type, priority, effective_from);

-- Create new indexes without provider_id
CREATE INDEX idx_commission_rules_currency_active ON commission_rules(currency, is_active);
CREATE INDEX idx_commission_rules_currency_type_active ON commission_rules(currency, transfer_type, is_active);
CREATE INDEX idx_commission_rules_lookup_new ON commission_rules(currency, transfer_type, is_active, priority DESC)
    WHERE is_active = TRUE;

-- Drop indexes that reference provider_id in commission_transactions
DROP INDEX IF EXISTS idx_commission_transactions_revenue;
DROP INDEX IF EXISTS idx_commission_transactions_settlement;

-- Remove provider_id column from commission_transactions
ALTER TABLE commission_transactions DROP COLUMN IF EXISTS provider_id;

-- Create new indexes without provider_id
CREATE INDEX idx_commission_transactions_revenue_new ON commission_transactions(currency, status, created_at)
    WHERE status = 'COMPLETED';

CREATE INDEX idx_commission_transactions_settlement_new ON commission_transactions(settled, settlement_date)
    WHERE settled = FALSE;

-- Update table comment
COMMENT ON TABLE commission_rules IS 'Commission rules for fee calculation - functions as a wallet containing all commission rules';

-- Analyze tables for query planner
ANALYZE commission_rules;
ANALYZE commission_transactions;
