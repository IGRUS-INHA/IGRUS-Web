-- AssociateDecision OneToMany 전환: UNIQUE 제약 제거 + active 컬럼 추가
-- 기존 데이터는 유저당 최대 1행 (V27 UNIQUE 제약) 이므로 DEFAULT TRUE 적용이 안전함

-- 1. active 컬럼 추가 (현재 유효한 결정 표시용)
ALTER TABLE associate_decisions ADD COLUMN associate_decisions_active BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. 복합 인덱스 추가 (user_id가 선두 컬럼이므로 FK 참조 인덱스로도 사용 가능)
CREATE INDEX idx_associate_decisions_active ON associate_decisions (associate_decisions_user_id, associate_decisions_active);

-- 3. UNIQUE 제약 제거 (대체 인덱스가 있으므로 FK 제약 유지됨)
ALTER TABLE associate_decisions DROP INDEX uk_associate_decisions_user_id;
