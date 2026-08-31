ALTER TABLE home_banners
    MODIFY COLUMN action_type VARCHAR(40) NOT NULL DEFAULT 'NONE';

UPDATE home_banners
SET action_type = 'NONE'
WHERE action_type = 'RANDOM_DISCOVERY';
