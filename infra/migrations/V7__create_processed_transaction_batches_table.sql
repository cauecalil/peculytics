CREATE TABLE peculytics.processed_transaction_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id UUID NOT NULL REFERENCES peculytics.analyses(id) ON DELETE CASCADE,
    statement_file_id UUID NOT NULL REFERENCES peculytics.statement_files(id) ON DELETE CASCADE,
    batch_number INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_processed_transaction_batches_batch UNIQUE (analysis_id, statement_file_id, batch_number)
);
