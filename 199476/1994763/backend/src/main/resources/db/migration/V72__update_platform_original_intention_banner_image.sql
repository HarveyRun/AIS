UPDATE home_banners
SET image_url = '/banners/platform-original-intention-v2.png'
WHERE image_url = '/banners/platform-original-intention.png'
  AND deleted = FALSE;
