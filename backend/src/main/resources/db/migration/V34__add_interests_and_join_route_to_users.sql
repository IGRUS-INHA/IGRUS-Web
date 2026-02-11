-- 관심 분야(interests), 기타 관심 분야, 가입 경로, 기타 가입 경로 컬럼 추가 (모두 nullable, 기존 데이터 호환)
ALTER TABLE users ADD COLUMN users_interests JSON NULL;
ALTER TABLE users ADD COLUMN users_custom_interest VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN users_join_route VARCHAR(30) NULL;
ALTER TABLE users ADD COLUMN users_custom_join_route VARCHAR(100) NULL;
