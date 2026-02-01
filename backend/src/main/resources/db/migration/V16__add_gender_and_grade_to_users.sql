-- 성별 컬럼 추가 (기존 데이터는 'MALE'로 설정)
ALTER TABLE users ADD COLUMN users_gender VARCHAR(10) NOT NULL DEFAULT 'MALE';

-- 학년 컬럼 추가 (기존 데이터는 1로 설정)
ALTER TABLE users ADD COLUMN users_grade INT NOT NULL DEFAULT 1;

-- 기본값 제거 (애플리케이션 레벨에서 필수값 검증)
ALTER TABLE users ALTER COLUMN users_gender DROP DEFAULT;
ALTER TABLE users ALTER COLUMN users_grade DROP DEFAULT;
