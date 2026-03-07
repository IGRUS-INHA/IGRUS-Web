-- event_registrations 테이블 외부인 지원 변경 (DECISION-01: 단일 테이블)

-- 1. user_id를 nullable로 변경 (외부인은 user 없음)
ALTER TABLE event_registrations MODIFY event_registrations_user_id BIGINT NULL;

-- 2. 외부인 여부 플래그
ALTER TABLE event_registrations ADD COLUMN event_registrations_is_external BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. 외부인 정보 컬럼 (외부인 신청 시에만 사용)
ALTER TABLE event_registrations ADD COLUMN event_registrations_external_name VARCHAR(50) NULL;
ALTER TABLE event_registrations ADD COLUMN event_registrations_external_student_id VARCHAR(20) NULL;
ALTER TABLE event_registrations ADD COLUMN event_registrations_external_phone VARCHAR(20) NULL;
ALTER TABLE event_registrations ADD COLUMN event_registrations_external_department VARCHAR(100) NULL;

-- 기존 UNIQUE 제약(uk_event_registrations_event_user)은 유지
-- MySQL에서 NULL은 UNIQUE 비교에서 무시되므로 외부인 신청(user_id=NULL)은 이 제약에 걸리지 않음
-- DECISION-02: 외부인 studentId/phone에 대한 DB UNIQUE 제약조건 없음 (서비스 레벨만)
