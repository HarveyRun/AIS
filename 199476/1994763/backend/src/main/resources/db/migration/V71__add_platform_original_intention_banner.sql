INSERT INTO home_banners (
    display_mode,
    image_url,
    action_type,
    sort_order,
    enabled
)
SELECT
    'IMAGE_ONLY',
    '/banners/platform-original-intention.png',
    'PLATFORM_INTRODUCTION',
    3,
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM home_banners
    WHERE image_url = '/banners/platform-original-intention.png'
      AND deleted = FALSE
);
