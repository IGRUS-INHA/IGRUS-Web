-- V22: 리프레시 토큰 낙관적 잠금(Optimistic Lock) 지원
-- 동시 요청에 의한 중복 토큰 로테이션 방지를 위한 version 컬럼 추가

ALTER TABLE refresh_tokens
    ADD COLUMN refresh_tokens_version BIGINT NOT NULL DEFAULT 0;
