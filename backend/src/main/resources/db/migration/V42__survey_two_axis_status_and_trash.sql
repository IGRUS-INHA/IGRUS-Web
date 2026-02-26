-- 설문 2축 상태 모델 전환 및 휴지통 기능 추가
-- 기존 surveys_status (DRAFT/PUBLISHED/CLOSED) → surveys_visibility + surveys_response_status

-- 1) surveys_visibility 컬럼 추가 + 데이터 마이그레이션
ALTER TABLE surveys ADD COLUMN surveys_visibility VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
UPDATE surveys SET surveys_visibility = 'PUBLISHED' WHERE surveys_status = 'PUBLISHED';
UPDATE surveys SET surveys_visibility = 'PUBLISHED' WHERE surveys_status = 'CLOSED';
-- DRAFT는 기본값으로 이미 설정됨

-- 2) surveys_response_status 컬럼 추가 + 데이터 마이그레이션
ALTER TABLE surveys ADD COLUMN surveys_response_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED';
UPDATE surveys SET surveys_response_status = 'OPEN' WHERE surveys_status = 'PUBLISHED';
UPDATE surveys SET surveys_response_status = 'CLOSED' WHERE surveys_status = 'CLOSED';
-- NOT_STARTED는 기본값으로 이미 설정됨 (DRAFT 케이스)

-- 3) surveys_trashed_at 컬럼 추가
ALTER TABLE surveys ADD COLUMN surveys_trashed_at TIMESTAMP NULL;

-- 4) 기존 surveys_status 컬럼 및 인덱스 제거
DROP INDEX idx_surveys_status ON surveys;
ALTER TABLE surveys DROP COLUMN surveys_status;

-- 5) 새 인덱스 추가
CREATE INDEX idx_surveys_visibility ON surveys(surveys_visibility);
CREATE INDEX idx_surveys_response_status ON surveys(surveys_response_status);
CREATE INDEX idx_surveys_trashed_at ON surveys(surveys_trashed_at);
