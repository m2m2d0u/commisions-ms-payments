-- V1: Create commission_rules table

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create commission_rules table
CREATE TABLE commission_rules (
    rule_id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_id         UUID NOT NULL,
    currency            VARCHAR(3) NOT NULL,

    -- Rule conditions
    transfer_type       VARCHAR(20) NOT NULL CHECK (transfer_type IN ('SAME_WALLET', 'CROSS_WALLET', 'INTERNATIONAL')),
    min_transaction     BIGINT,
    max_transaction     BIGINT,
    kyc_level           VARCHAR(20) CHECK (kyc_level IN ('LEVEL_1', 'LEVEL_2', 'LEVEL_3', 'ANY')),

    -- Commission calculation
    percentage          NUMERIC(5,4) NOT NULL CHECK (percentage >= 0 AND percentage <= 1),
    fixed_amount        BIGINT DEFAULT 0,
    min_amount          BIGINT DEFAULT 0 CHECK (min_amount >= 0),
    max_amount          BIGINT CHECK (max_amount IS NULL OR max_amount >= min_amount),

    -- Rule management
    is_active           BOOLEAN DEFAULT TRUE,
    priority            INTEGER DEFAULT 0,
    effective_from      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    effective_to        TIMESTAMP WITH TIME ZONE,

    -- Metadata
    description         TEXT,
    notes               TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by          UUID,

    -- Constraint: One active rule per provider per currency per type per priority
    UNIQUE(provider_id, currency, transfer_type, priority, effective_from)
);

-- Create indexes for commission_rules
CREATE INDEX idx_commission_rules_provider ON commission_rules(provider_id, is_active);
CREATE INDEX idx_commission_rules_currency ON commission_rules(currency);
CREATE INDEX idx_commission_rules_provider_currency ON commission_rules(provider_id, currency, is_active);
CREATE INDEX idx_commission_rules_priority ON commission_rules(priority DESC);
CREATE INDEX idx_commission_rules_effective ON commission_rules(effective_from, effective_to);
CREATE INDEX idx_commission_rules_provider_type ON commission_rules(provider_id, transfer_type, is_active);

-- Add table comment
COMMENT ON TABLE commission_rules IS 'Commission rules for fee calculation per provider and currency';
COMMENT ON COLUMN commission_rules.percentage IS 'Percentage fee (e.g., 0.0050 for 0.5%)';
COMMENT ON COLUMN commission_rules.fixed_amount IS 'Fixed fee amount in XOF/XAF';
COMMENT ON COLUMN commission_rules.min_amount IS 'Minimum commission amount';
COMMENT ON COLUMN commission_rules.max_amount IS 'Maximum commission amount (cap)';
