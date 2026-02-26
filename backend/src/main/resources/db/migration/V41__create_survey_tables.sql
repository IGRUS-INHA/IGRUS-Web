-- 설문 테이블
CREATE TABLE surveys (
    surveys_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    surveys_title VARCHAR(100) NOT NULL,
    surveys_description VARCHAR(500),
    surveys_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    surveys_access_level VARCHAR(20) NOT NULL,
    surveys_deadline TIMESTAMP NULL,
    surveys_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    surveys_deleted_at TIMESTAMP NULL,
    surveys_deleted_by BIGINT,
    surveys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    surveys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    surveys_created_by BIGINT,
    surveys_updated_by BIGINT
);

-- 설문 질문 테이블
CREATE TABLE survey_questions (
    survey_questions_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_questions_survey_id BIGINT NOT NULL,
    survey_questions_type VARCHAR(30) NOT NULL,
    survey_questions_title VARCHAR(200) NOT NULL,
    survey_questions_description VARCHAR(500),
    survey_questions_required BOOLEAN NOT NULL DEFAULT FALSE,
    survey_questions_display_order INT NOT NULL,
    survey_questions_scale_min INT,
    survey_questions_scale_max INT,
    survey_questions_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    survey_questions_deleted_at TIMESTAMP NULL,
    survey_questions_deleted_by BIGINT,
    survey_questions_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    survey_questions_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    survey_questions_created_by BIGINT,
    survey_questions_updated_by BIGINT,
    CONSTRAINT fk_survey_questions_survey FOREIGN KEY (survey_questions_survey_id) REFERENCES surveys(surveys_id)
);

-- 설문 질문 선택지 테이블
CREATE TABLE survey_question_options (
    survey_question_options_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_question_options_question_id BIGINT NOT NULL,
    survey_question_options_text VARCHAR(200) NOT NULL,
    survey_question_options_display_order INT NOT NULL,
    survey_question_options_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    survey_question_options_deleted_at TIMESTAMP NULL,
    survey_question_options_deleted_by BIGINT,
    survey_question_options_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    survey_question_options_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    survey_question_options_created_by BIGINT,
    survey_question_options_updated_by BIGINT,
    CONSTRAINT fk_survey_question_options_question FOREIGN KEY (survey_question_options_question_id) REFERENCES survey_questions(survey_questions_id)
);

-- 설문 그리드 행 테이블
CREATE TABLE survey_question_rows (
    survey_question_rows_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_question_rows_question_id BIGINT NOT NULL,
    survey_question_rows_label VARCHAR(200) NOT NULL,
    survey_question_rows_display_order INT NOT NULL,
    survey_question_rows_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    survey_question_rows_deleted_at TIMESTAMP NULL,
    survey_question_rows_deleted_by BIGINT,
    survey_question_rows_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    survey_question_rows_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    survey_question_rows_created_by BIGINT,
    survey_question_rows_updated_by BIGINT,
    CONSTRAINT fk_survey_question_rows_question FOREIGN KEY (survey_question_rows_question_id) REFERENCES survey_questions(survey_questions_id)
);

-- 설문 응답 테이블
CREATE TABLE survey_responses (
    survey_responses_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_responses_survey_id BIGINT NOT NULL,
    survey_responses_user_id BIGINT,
    survey_responses_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    survey_responses_deleted_at TIMESTAMP NULL,
    survey_responses_deleted_by BIGINT,
    survey_responses_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    survey_responses_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    survey_responses_created_by BIGINT,
    survey_responses_updated_by BIGINT,
    CONSTRAINT fk_survey_responses_survey FOREIGN KEY (survey_responses_survey_id) REFERENCES surveys(surveys_id),
    CONSTRAINT fk_survey_responses_user FOREIGN KEY (survey_responses_user_id) REFERENCES users(users_id),
    CONSTRAINT uk_survey_responses_survey_user UNIQUE (survey_responses_survey_id, survey_responses_user_id)
);

-- 설문 답변 테이블
CREATE TABLE survey_answers (
    survey_answers_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_answers_response_id BIGINT NOT NULL,
    survey_answers_question_id BIGINT NOT NULL,
    survey_answers_option_id BIGINT,
    survey_answers_row_id BIGINT,
    survey_answers_text_value TEXT,
    survey_answers_numeric_value INT,
    survey_answers_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    survey_answers_deleted_at TIMESTAMP NULL,
    survey_answers_deleted_by BIGINT,
    survey_answers_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    survey_answers_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    survey_answers_created_by BIGINT,
    survey_answers_updated_by BIGINT,
    CONSTRAINT fk_survey_answers_response FOREIGN KEY (survey_answers_response_id) REFERENCES survey_responses(survey_responses_id),
    CONSTRAINT fk_survey_answers_question FOREIGN KEY (survey_answers_question_id) REFERENCES survey_questions(survey_questions_id),
    CONSTRAINT fk_survey_answers_option FOREIGN KEY (survey_answers_option_id) REFERENCES survey_question_options(survey_question_options_id),
    CONSTRAINT fk_survey_answers_row FOREIGN KEY (survey_answers_row_id) REFERENCES survey_question_rows(survey_question_rows_id)
);

-- 인덱스
CREATE INDEX idx_surveys_status ON surveys(surveys_status);
CREATE INDEX idx_surveys_deadline ON surveys(surveys_deadline);
CREATE INDEX idx_survey_questions_survey_id ON survey_questions(survey_questions_survey_id);
CREATE INDEX idx_survey_question_options_question_id ON survey_question_options(survey_question_options_question_id);
CREATE INDEX idx_survey_question_rows_question_id ON survey_question_rows(survey_question_rows_question_id);
CREATE INDEX idx_survey_responses_survey_id ON survey_responses(survey_responses_survey_id);
CREATE INDEX idx_survey_responses_user_id ON survey_responses(survey_responses_user_id);
CREATE INDEX idx_survey_answers_response_id ON survey_answers(survey_answers_response_id);
CREATE INDEX idx_survey_answers_question_id ON survey_answers(survey_answers_question_id);
