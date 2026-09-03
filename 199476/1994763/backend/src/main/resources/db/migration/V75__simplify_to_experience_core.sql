SET @add_job_title_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'users'
       AND column_name = 'job_title') = 0,
    'ALTER TABLE users ADD COLUMN job_title VARCHAR(80) NULL AFTER avatar_url',
    'SELECT 1'
);
PREPARE add_job_title_statement FROM @add_job_title_sql;
EXECUTE add_job_title_statement;
DEALLOCATE PREPARE add_job_title_statement;

UPDATE users u
LEFT JOIN (
    SELECT uj.user_id, MIN(j.name) AS job_name
    FROM user_jobs uj
    JOIN jobs j ON j.id = uj.job_id
    WHERE uj.deleted_at IS NULL
      AND j.deleted_at IS NULL
    GROUP BY uj.user_id
) legacy_job ON legacy_job.user_id = u.id
SET u.job_title = legacy_job.job_name
WHERE u.job_title IS NULL;

UPDATE certifications
SET enabled = FALSE,
    deleted_at = COALESCE(deleted_at, CURRENT_TIMESTAMP(6))
WHERE certification_type = 'MAIN_JOB';

UPDATE user_jobs
SET verified = FALSE,
    deleted_at = COALESCE(deleted_at, CURRENT_TIMESTAMP(6));

UPDATE home_banners
SET enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE action_type IN (
    'CONTENT_CONTRIBUTION',
    'MATTER_DISCOVERY',
    'EXPERIENCE_DISCOVERY',
    'RANDOM_DISCOVERY'
)
   OR title LIKE '%邀请%';

UPDATE admin_permissions
SET active = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code LIKE 'CONTENT_CONTRIBUTION_%'
   OR code LIKE 'JOB_%'
   OR code LIKE 'DISCOVERY_%'
   OR code LIKE 'EXPERIENCE_%';

UPDATE admin_permissions
SET active = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code LIKE 'INVITATION_%';

UPDATE invitation_campaign_settings
SET enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP(6);

UPDATE analytics_event_definitions
SET active = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE event_name LIKE 'content_contribution_%'
   OR event_name IN (
       'matter_entry_click',
       'matter_select',
       'experience_entry_click',
       'experience_select',
       'job_filter_select',
       'people_result_view',
       'people_result_empty'
   );

DROP TABLE IF EXISTS content_contribution_rewards;
DROP TABLE IF EXISTS content_contribution_jobs;
DROP TABLE IF EXISTS content_contributions;
