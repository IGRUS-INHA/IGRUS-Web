-- -----------------------------------------------------
-- V3: refresh_tokens_token 컬럼 크기 확장
-- JWT 토큰이 기존 VARCHAR(255)보다 길어서 Data truncation 에러 발생
-- -----------------------------------------------------

-- 기존 UNIQUE 제약 조건 제거
ALTER TABLE refresh_tokens DROP INDEX uk_refresh_tokens_token;

-- 컬럼 크기 확장 (255 -> 2048)
ALTER TABLE refresh_tokens MODIFY COLUMN refresh_tokens_token VARCHAR(2048) NOT NULL;

-- UNIQUE 제약 조건 재생성 (prefix 길이 지정)
ALTER TABLE refresh_tokens ADD CONSTRAINT uk_refresh_tokens_token UNIQUE (refresh_tokens_token(255));