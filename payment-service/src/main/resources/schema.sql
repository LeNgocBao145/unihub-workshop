-- Payments Table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registration_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    gateway VARCHAR(50),
    provider_transaction_id VARCHAR(255) UNIQUE,
    bank_reference_code VARCHAR(255) UNIQUE,
    actual_content TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_registration_id ON payments(registration_id);
CREATE INDEX idx_bank_reference_code ON payments(bank_reference_code);
CREATE INDEX idx_provider_transaction_id ON payments(provider_transaction_id);
CREATE INDEX idx_status ON payments(status);

