-- Run this once against job-email-agent.db to add support for web-scraped job tracking.
-- e.g.: sqlite3 job-email-agent.db < migrations/001_create_processed_jobs.sql
--
-- (spring.jpa.hibernate.ddl-auto=none, so tables are created manually - matches the
--  existing style of the processed_emails table.)

CREATE TABLE IF NOT EXISTS processed_jobs (
    id integer,
    source varchar(255),
    external_id varchar(255),
    title varchar(500),
    company varchar(255),
    url varchar(1000),
    match_level varchar(255) check (match_level in ('HIGH','MEDIUM','LOW','NONE')),
    matching_score integer not null,
    draft_created boolean not null default 0,
    processed_at timestamp,
    primary key (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_processed_jobs_source_external_id
    ON processed_jobs (source, external_id);
