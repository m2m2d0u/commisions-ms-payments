-- V2: Create commission_transactions table

CREATE TABLE commission_transactions (
    commission_id       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id      UUID NOT NULL,
    rule_id             UUID REFERENCES commission_rules(rule_id) ON DELETE SET NULL,
    provider_id         UUID NOT NULL,
    currency            VARCHAR(3) NOT NULL,

    -- Commission details
    amount              BIGINT NOT NULL CHECK (amount >= 0),
    calculation_basis   JSONB,

    -- Accounting
    status              VARCHAR(20) DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'REFUNDED')),
    settled             BOOLEAN DEFAULT FALSE,
    settlement_date     TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for commission_transactions
CREATE INDEX idx_commission_transactions_transaction ON commission_transactions(transaction_id);
CREATE INDEX idx_commission_transactions_provider ON commission_transactions(provider_id);
CREATE INDEX idx_commission_transactions_currency ON commission_transactions(currency);
CREATE INDEX idx_commission_transactions_created_at ON commission_transactions(created_at DESC);
CREATE INDEX idx_commission_transactions_settled ON commission_transactions(settled, settlement_date);
CREATE INDEX idx_commission_transactions_status ON commission_transactions(status);

-- Add table comment
COMMENT ON TABLE commission_transactions IS 'Platform commission revenue tracking per transaction';
COMMENT ON COLUMN commission_transactions.calculation_basis IS 'JSON object storing calculation details (percentage, fixed, min, max applied)';
COMMENT ON COLUMN commission_transactions.amount IS 'Commission amount in XOF/XAF';
