UPDATE home_banners
SET action_type = 'BASIC_CERTIFICATION',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE image_url = '/banners/invite-answerer-01.png'
  AND deleted = FALSE;
