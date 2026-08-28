ALTER TABLE jobs
    ADD COLUMN main_work VARCHAR(500) NULL AFTER description,
    ADD COLUMN can_help_with VARCHAR(500) NULL AFTER main_work,
    ADD COLUMN not_responsible_for VARCHAR(500) NULL AFTER can_help_with;

ALTER TABLE discovery_matter_jobs
    ADD COLUMN role_description VARCHAR(500) NULL AFTER sort_order;
