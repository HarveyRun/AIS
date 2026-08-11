ALTER TABLE users
    ADD COLUMN capability_description VARCHAR(240) NULL AFTER avatar_url;

CREATE TABLE discovery_matter_participants (
    matter_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    participation_type VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (matter_id, user_id),
    CONSTRAINT fk_discovery_participant_matter
        FOREIGN KEY (matter_id) REFERENCES discovery_matters(id) ON DELETE CASCADE,
    CONSTRAINT fk_discovery_participant_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_discovery_participation_type
        CHECK (participation_type IN ('PRIMARY', 'SUPPORTING')),
    INDEX idx_discovery_participant_user (user_id, matter_id),
    INDEX idx_discovery_participant_order (matter_id, participation_type, sort_order, user_id)
);

INSERT INTO discovery_matter_participants(matter_id,user_id,participation_type,sort_order)
SELECT r.matter_id,
       rp.user_id,
       CASE WHEN SUM(r.role_type='REQUIRED')>0 THEN 'PRIMARY' ELSE 'SUPPORTING' END,
       MIN(r.sort_order)
FROM discovery_matter_role_people rp
JOIN discovery_matter_roles r ON r.id=rp.role_id
GROUP BY r.matter_id,rp.user_id;
