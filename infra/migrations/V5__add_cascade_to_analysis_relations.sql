ALTER TABLE transactions
    DROP CONSTRAINT transactions_analysis_id_fkey;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_analysis_id_fkey
    FOREIGN KEY (analysis_id)
    REFERENCES analyses(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE;

ALTER TABLE transactions
    DROP CONSTRAINT transactions_statement_file_id_fkey;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_statement_file_id_fkey
    FOREIGN KEY (statement_file_id)
    REFERENCES statement_files(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE;

ALTER TABLE statement_files
    DROP CONSTRAINT statement_files_analysis_id_fkey;

ALTER TABLE statement_files
    ADD CONSTRAINT statement_files_analysis_id_fkey
    FOREIGN KEY (analysis_id)
    REFERENCES analyses(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE;