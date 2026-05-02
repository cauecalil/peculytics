CREATE TABLE analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    total_files INT NOT NULL DEFAULT 0,
    total_transactions INT NOT NULL DEFAULT 0,
    processed_batches INT NOT NULL DEFAULT 0,
    total_batches INT NOT NULL DEFAULT 0,
    error_message VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);
