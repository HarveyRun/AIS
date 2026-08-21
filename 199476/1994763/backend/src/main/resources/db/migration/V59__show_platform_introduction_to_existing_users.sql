UPDATE users
SET platform_intro_required = TRUE
WHERE account_status = 'ACTIVE';
