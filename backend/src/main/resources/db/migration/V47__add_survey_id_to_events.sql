-- events 테이블에 설문 연결을 위한 event_survey_id 컬럼 추가
-- nullable FK: 설문 연결은 선택 사항 (SEVT-INV-01)
-- 행사당 최대 1개 설문 연결 (SEVT-INV-02), 설문은 여러 행사에 재사용 가능 (SEVT-INV-03)
ALTER TABLE events ADD COLUMN event_survey_id BIGINT NULL;

ALTER TABLE events ADD CONSTRAINT fk_events_survey
    FOREIGN KEY (event_survey_id) REFERENCES surveys(surveys_id);
