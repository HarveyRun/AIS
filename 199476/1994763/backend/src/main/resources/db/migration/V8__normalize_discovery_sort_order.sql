UPDATE discovery_categories c
JOIN (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY main_category ORDER BY sort_order, id) AS position
    FROM discovery_categories
) ranked ON ranked.id = c.id
SET c.sort_order = ranked.position;

UPDATE discovery_matters m
JOIN (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY category_id ORDER BY sort_order, id) AS position
    FROM discovery_matters
) ranked ON ranked.id = m.id
SET m.sort_order = ranked.position;
