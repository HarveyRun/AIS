ALTER TABLE experience_categories
    ADD COLUMN recommendation_group BOOLEAN NOT NULL DEFAULT FALSE AFTER parent_id,
    ADD COLUMN target_category_id BIGINT NULL AFTER recommendation_group,
    ADD CONSTRAINT fk_experience_category_target FOREIGN KEY (target_category_id) REFERENCES experience_categories(id);

UPDATE experience_categories
SET recommendation_group = TRUE
WHERE parent_id IS NULL AND name = '常见推荐' AND deleted_at IS NULL;

UPDATE experience_categories recommendation
JOIN experience_categories recommendation_parent ON recommendation_parent.id = recommendation.parent_id
JOIN experience_categories target ON target.name = recommendation.name AND target.deleted_at IS NULL
JOIN experience_categories target_parent ON target_parent.id = target.parent_id
SET recommendation.target_category_id = target.id
WHERE recommendation_parent.recommendation_group = TRUE
  AND recommendation.deleted_at IS NULL
  AND target_parent.name = CASE recommendation.name
      WHEN '买房卖房' THEN '住房家居'
      WHEN '邻里纠纷' THEN '法律维权'
      WHEN '买车用车' THEN '生活方式'
      WHEN '看病就医' THEN '健康医疗'
      WHEN '副业' THEN '副业创业'
      WHEN '劳动仲裁' THEN '法律维权'
      ELSE '' END;

UPDATE certifications certification
JOIN experience_categories recommendation ON recommendation.id = certification.experience_category_id
SET certification.experience_category_id = recommendation.target_category_id
WHERE recommendation.target_category_id IS NOT NULL;
