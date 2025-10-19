-- V4: Additional performance indexes and database functions

-- Create database function for BCEAO fee calculation
CREATE OR REPLACE FUNCTION calculate_transfer_fee(p_amount BIGINT)
RETURNS BIGINT AS $$
DECLARE
    v_fixed_fee BIGINT := 100;      -- 100 XOF fixed fee
    v_percentage NUMERIC := 0.005;  -- 0.5%
    v_max_fee BIGINT := 1000;       -- Maximum 1,000 XOF
    v_free_threshold BIGINT := 5000; -- FREE for amounts <= 5,000 XOF
    v_total_fee BIGINT;
BEGIN
    -- Amounts <= 5,000 XOF are FREE (BCEAO financial inclusion)
    IF p_amount <= v_free_threshold THEN
        RETURN 0;
    END IF;

    -- Calculate fee: fixed + percentage
    v_total_fee := v_fixed_fee + FLOOR(p_amount * v_percentage);

    -- Cap at maximum
    IF v_total_fee > v_max_fee THEN
        RETURN v_max_fee;
    END IF;

    RETURN v_total_fee;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_transfer_fee IS 'BCEAO-compliant fee calculation: FREE <=5000 XOF, else 100 + 0.5% (max 1000)';

-- Additional composite indexes for complex queries
-- Note: Removed CURRENT_TIMESTAMP from WHERE clause as PostgreSQL requires immutable functions in index predicates
CREATE INDEX idx_commission_rules_lookup ON commission_rules(provider_id, currency, transfer_type, is_active, priority DESC)
    WHERE is_active = TRUE;

CREATE INDEX idx_commission_transactions_revenue ON commission_transactions(provider_id, currency, status, created_at)
    WHERE status = 'COMPLETED';

CREATE INDEX idx_commission_transactions_settlement ON commission_transactions(provider_id, settled, settlement_date)
    WHERE settled = FALSE;

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Add trigger to commission_rules
CREATE TRIGGER update_commission_rules_updated_at
    BEFORE UPDATE ON commission_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add statistics for query planner
ANALYZE commission_rules;
ANALYZE commission_transactions;
