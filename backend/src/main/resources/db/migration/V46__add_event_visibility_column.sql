-- event_visibility 컬럼 추가
-- 기존 행사 데이터는 이미 공개된 상태이므로 DEFAULT 'PUBLISHED'로 설정
-- 신규 행사 생성 시에는 JPA Event.create()에서 UNPUBLISHED로 명시적 설정
ALTER TABLE events
    ADD COLUMN event_visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';
