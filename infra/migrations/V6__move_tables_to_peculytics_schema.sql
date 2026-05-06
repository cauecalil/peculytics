CREATE SCHEMA IF NOT EXISTS peculytics;

ALTER TABLE IF EXISTS analyses SET SCHEMA peculytics;
ALTER TABLE IF EXISTS statement_files SET SCHEMA peculytics;
ALTER TABLE IF EXISTS transactions SET SCHEMA peculytics;
ALTER TABLE IF EXISTS categorization_rules SET SCHEMA peculytics;
