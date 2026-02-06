-- V20: 리프레시 토큰 로테이션 지원 필드 추가
-- tokenFamily: 같은 로그인 세션에서 파생된 토큰 체인을 그룹화
-- replacedByToken: 로테이션 시 어떤 새 토큰으로 교체되었는지 추적
-- revokedAt: 폐기 시점 기록 (Grace Period 판단용)

ALTER TABLE refresh_tokens
    ADD COLUMN refresh_tokens_token_family VARCHAR(36) NOT NULL DEFAULT '' AFTER refresh_tokens_revoked;

ALTER TABLE refresh_tokens
    ADD COLUMN refresh_tokens_replaced_by_token VARCHAR(2048) NULL AFTER refresh_tokens_token_family;

ALTER TABLE refresh_tokens
    ADD COLUMN refresh_tokens_revoked_at DATETIME(6) NULL AFTER refresh_tokens_replaced_by_token;

-- 기존 토큰에 고유한 token_family 값 부여
UPDATE refresh_tokens SET refresh_tokens_token_family = UUID() WHERE refresh_tokens_token_family = '';

-- token_family 기반 조회를 위한 인덱스
CREATE INDEX idx_refresh_tokens_token_family ON refresh_tokens(refresh_tokens_token_family);
