-- AssociateDecision OneToMany 전환: UNIQUE 제약 제거 + active 컬럼 추가

-- 1. user_id UNIQUE 제약 제거 (한 유저에 여러 결정 기록 허용)
ALTER TABLE associate_decisions DROP INDEX uk_associate_decisions_user_id;

-- 2. active 컬럼 추가 (현재 유효한 결정 표시용)
ALTER TABLE associate_decisions ADD COLUMN associate_decisions_active BOOLEAN NOT NULL DEFAULT TRUE;

-- 3. active 컬럼 인덱스 추가 (조회 성능)
CREATE INDEX idx_associate_decisions_active ON associate_decisions (associate_decisions_user_id, associate_decisions_active);
