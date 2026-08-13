ALTER TABLE discovery_matter_jobs
    DROP CHECK chk_matter_job_type,
    DROP INDEX idx_matter_job_order,
    DROP COLUMN participation_type,
    ADD INDEX idx_matter_job_order (matter_id, sort_order, job_id);
