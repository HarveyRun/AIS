DELETE existing_relation
FROM user_jobs existing_relation
JOIN (
    SELECT user_id, job_id
    FROM (
        SELECT
            user_id,
            job_id,
            ROW_NUMBER() OVER (
                PARTITION BY user_id
                ORDER BY
                    CASE WHEN certification_id IS NOT NULL THEN 0 ELSE 1 END,
                    certification_id DESC,
                    verified DESC,
                    updated_at DESC,
                    job_id
            ) AS relation_rank
        FROM user_jobs
    ) ranked_relations
    WHERE relation_rank > 1
) duplicate_relation
    ON duplicate_relation.user_id = existing_relation.user_id
   AND duplicate_relation.job_id = existing_relation.job_id;

ALTER TABLE user_jobs
    ADD UNIQUE KEY uk_user_jobs_user (user_id);
