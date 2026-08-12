ALTER TABLE users ALTER COLUMN accepting_inquiries SET DEFAULT FALSE;

UPDATE users
SET accepting_inquiries=FALSE
WHERE answerer_status<>'APPROVED';
