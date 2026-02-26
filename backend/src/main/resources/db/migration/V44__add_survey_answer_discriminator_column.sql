-- STI(Single Table Inheritance) discriminator 컬럼 추가
-- 답변 유형별 서브클래스 구분: TEXT, OPTION, NUMERIC, GRID
ALTER TABLE survey_answers
    ADD COLUMN survey_answers_answer_type VARCHAR(10) NOT NULL DEFAULT 'TEXT';

-- 기존 데이터가 있을 경우 유형 자동 분류
UPDATE survey_answers SET survey_answers_answer_type = 'NUMERIC'
WHERE survey_answers_numeric_value IS NOT NULL;

UPDATE survey_answers SET survey_answers_answer_type = 'GRID'
WHERE survey_answers_row_id IS NOT NULL;

UPDATE survey_answers SET survey_answers_answer_type = 'OPTION'
WHERE survey_answers_option_id IS NOT NULL AND survey_answers_row_id IS NULL;

-- 기본값 제거 (JPA가 discriminator 값을 자동 설정)
ALTER TABLE survey_answers ALTER COLUMN survey_answers_answer_type DROP DEFAULT;

-- discriminator 인덱스
CREATE INDEX idx_survey_answers_answer_type ON survey_answers(survey_answers_answer_type);
