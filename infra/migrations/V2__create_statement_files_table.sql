CREATE TABLE statement_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id UUID NOT NULL REFERENCES analyses(id),
    title VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    total_transactions INT NOT NULL DEFAULT 0,
    processed_batches INT NOT NULL DEFAULT 0,
    total_batches INT NOT NULL DEFAULT 0,
    parser_name VARCHAR(255),
    error_message VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);
