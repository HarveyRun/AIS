-- 实名认证业务退出：保留认证/材料通用表供经历审核使用，历史实名记录停止使用。
UPDATE certification_materials material
JOIN certifications certification ON certification.id = material.certification_id
SET material.deleted_at = COALESCE(material.deleted_at, CURRENT_TIMESTAMP(6))
WHERE certification.certification_type = 'IDENTITY';

UPDATE certifications
SET enabled = FALSE,
    deleted_at = COALESCE(deleted_at, CURRENT_TIMESTAMP(6))
WHERE certification_type = 'IDENTITY';

-- 经历已统一，不再区分公益/变现，也不再保留升级来源和“必选认证项”。
ALTER TABLE certifications
    DROP CHECK chk_certification_unified_experience_type,
    DROP INDEX idx_certifications_experience_business,
    DROP INDEX idx_certifications_upgrade_source,
    DROP COLUMN experience_business_type,
    DROP COLUMN monetization_amount,
    DROP COLUMN legacy_experience_business_type,
    DROP COLUMN legacy_experience_status,
    DROP COLUMN upgrade_source_id,
    DROP COLUMN required_item;

ALTER TABLE first_experience_rewards
    DROP CHECK chk_first_experience_reward_type,
    DROP CHECK chk_first_experience_reward_amount,
    DROP COLUMN experience_business_type;

ALTER TABLE experience_tips
    DROP COLUMN experience_business_type;

-- 询问只保留每小时费用；语音由每次预约/立即通话决定，不再使用方式开关。
ALTER TABLE users
    DROP COLUMN inquiry_price_min,
    DROP COLUMN inquiry_price_max,
    DROP COLUMN accepting_voice_inquiries,
    DROP COLUMN accepting_video_inquiries;

ALTER TABLE inquiries
    DROP COLUMN voice_allowed,
    DROP COLUMN video_allowed;

-- 已退出且无运行时代码的旧业务表。
DROP TABLE IF EXISTS experience_invitation_rewards;
DROP TABLE IF EXISTS experience_invitation_relationships;
DROP TABLE IF EXISTS invitation_submission_guards;
DROP TABLE IF EXISTS user_invitations;
DROP TABLE IF EXISTS invitation_campaign_settings;
DROP TABLE IF EXISTS job_certification_appointments;
DROP TABLE IF EXISTS inquiry_message_reports;
DROP TABLE IF EXISTS inquiry_message_report_cases;

ALTER TABLE withdrawals
    DROP COLUMN bank_card_id;

DROP TABLE IF EXISTS bank_cards;

UPDATE admin_permissions
SET active = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code IN (
    'CERTIFICATION_EDIT',
    'MESSAGE_REPORT_PROCESS',
    'SENSITIVE_ORIGINAL_VIEW',
    'OFFLINE_APPOINTMENT_VIEW',
    'OFFLINE_APPOINTMENT_PROCESS'
)
   OR code LIKE 'INVITATION_%';

UPDATE analytics_event_definitions
SET active = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE event_name IN (
    'identity_submitted',
    'certification_home_view',
    'job_certification_start',
    'job_method_select',
    'invitation_rules_view',
    'invitation_submit',
    'invitation_approved',
    'invitation_rejected',
    'invitation_rewarded'
);

UPDATE home_banners
SET enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE deleted = FALSE
  AND action_type IN (
      'INVITE_EXPERIENCE',
      'INVITE_PUBLIC_EXPERIENCE',
      'INVITE_MONETIZED_EXPERIENCE',
      'BASIC_CERTIFICATION'
  );
