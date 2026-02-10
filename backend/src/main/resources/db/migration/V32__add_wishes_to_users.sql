-- 가입 목적(wishes) 컬럼 추가 (JSON 배열, nullable)
ALTER TABLE users ADD COLUMN users_wishes JSON NULL;
