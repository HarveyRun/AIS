INSERT INTO home_banners (
    display_mode,
    image_url,
    sort_order,
    enabled
)
SELECT
    'IMAGE_ONLY',
    '/banners/invite-answerer-01.png',
    0,
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM home_banners
    WHERE image_url = '/banners/invite-answerer-01.png'
      AND deleted = FALSE
);
