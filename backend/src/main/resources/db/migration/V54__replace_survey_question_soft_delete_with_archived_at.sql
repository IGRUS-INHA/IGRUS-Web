-- survey_questions, survey_question_options, survey_question_rows 의
-- soft-delete 컬럼을 archived_at 모델로 교체합니다.
--
-- 배경:
--   기존 soft-delete는 "폼에서 빠짐"과 "삭제됨"을 동일 플래그로 표현하여
--   과거 응답이 가리키는 옵션이 폼 조회 시 사라져 미응답으로 보이는 문제가 있었습니다.
--   archived_at 모델로 의미를 분리하면, 응답이 있으면 archive(보존), 없으면 hard delete가 가능합니다.
--   hard delete 시 자식 옵션·행은 FK ON DELETE CASCADE로 함께 정리됩니다.

-- 1. archived_at, archived_by 컬럼 추가
ALTER TABLE survey_questions
    ADD COLUMN survey_questions_archived_at TIMESTAMP(6) NULL,
    ADD COLUMN survey_questions_archived_by BIGINT NULL;

ALTER TABLE survey_question_options
    ADD COLUMN survey_question_options_archived_at TIMESTAMP(6) NULL,
    ADD COLUMN survey_question_options_archived_by BIGINT NULL;

ALTER TABLE survey_question_rows
    ADD COLUMN survey_question_rows_archived_at TIMESTAMP(6) NULL,
    ADD COLUMN survey_question_rows_archived_by BIGINT NULL;

-- 2. 기존 soft-deleted 데이터를 archived로 이전 (timestamp 보존)
UPDATE survey_questions
    SET survey_questions_archived_at = COALESCE(survey_questions_deleted_at, survey_questions_updated_at),
        survey_questions_archived_by = survey_questions_deleted_by
    WHERE survey_questions_deleted = TRUE;

UPDATE survey_question_options
    SET survey_question_options_archived_at = COALESCE(survey_question_options_deleted_at, survey_question_options_updated_at),
        survey_question_options_archived_by = survey_question_options_deleted_by
    WHERE survey_question_options_deleted = TRUE;

UPDATE survey_question_rows
    SET survey_question_rows_archived_at = COALESCE(survey_question_rows_deleted_at, survey_question_rows_updated_at),
        survey_question_rows_archived_by = survey_question_rows_deleted_by
    WHERE survey_question_rows_deleted = TRUE;

-- 3. soft-delete 컬럼 제거
ALTER TABLE survey_questions
    DROP COLUMN survey_questions_deleted,
    DROP COLUMN survey_questions_deleted_at,
    DROP COLUMN survey_questions_deleted_by;

ALTER TABLE survey_question_options
    DROP COLUMN survey_question_options_deleted,
    DROP COLUMN survey_question_options_deleted_at,
    DROP COLUMN survey_question_options_deleted_by;

ALTER TABLE survey_question_rows
    DROP COLUMN survey_question_rows_deleted,
    DROP COLUMN survey_question_rows_deleted_at,
    DROP COLUMN survey_question_rows_deleted_by;

-- 4. hard-delete 시 자식 cascade를 위해 FK 재정의
--    (응답이 없는 질문 hard-delete 시 자식 옵션·행도 함께 삭제됨)
--    survey_answers → survey_questions/options/rows FK는 RESTRICT 유지
--    (hard-delete는 응답 없을 때만 호출되므로 트리거되지 않음)
ALTER TABLE survey_question_options
    DROP FOREIGN KEY fk_survey_question_options_question;
ALTER TABLE survey_question_options
    ADD CONSTRAINT fk_survey_question_options_question
    FOREIGN KEY (survey_question_options_question_id)
    REFERENCES survey_questions(survey_questions_id)
    ON DELETE CASCADE;

ALTER TABLE survey_question_rows
    DROP FOREIGN KEY fk_survey_question_rows_question;
ALTER TABLE survey_question_rows
    ADD CONSTRAINT fk_survey_question_rows_question
    FOREIGN KEY (survey_question_rows_question_id)
    REFERENCES survey_questions(survey_questions_id)
    ON DELETE CASCADE;

-- 5. archived 조회 성능을 위한 인덱스 (NULL이 아닌 archived_at 행 검색)
CREATE INDEX idx_survey_questions_archived_at
    ON survey_questions(survey_questions_archived_at);
CREATE INDEX idx_survey_question_options_archived_at
    ON survey_question_options(survey_question_options_archived_at);
CREATE INDEX idx_survey_question_rows_archived_at
    ON survey_question_rows(survey_question_rows_archived_at);
