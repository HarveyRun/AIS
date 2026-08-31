UPDATE home_banners
SET display_mode = 'IMAGE_ONLY',
    label_text = NULL,
    title = NULL,
    description = NULL,
    image_url = '/banners/content-contribution.png',
    action_type = 'CONTENT_CONTRIBUTION',
    sort_order = 1,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE action_type = 'CONTENT_CONTRIBUTION'
  AND deleted = FALSE;
