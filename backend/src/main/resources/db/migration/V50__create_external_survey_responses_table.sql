-- 외부인 설문 응답 테이블 신규 생성 (DECISION-04: 옵션 B - 별도 테이블)
CREATE TABLE external_survey_responses (
    external_survey_responses_id BIGINT NOT NULL AUTO_INCREMENT,
    external_survey_responses_survey_id BIGINT NOT NULL,
    external_survey_responses_registration_id BIGINT NOT NULL,
    external_survey_responses_student_id VARCHAR(20) NOT NULL,
    external_survey_responses_answers JSON NOT NULL,
    external_survey_responses_created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (external_survey_responses_id),
    CONSTRAINT fk_ext_survey_resp_survey FOREIGN KEY (external_survey_responses_survey_id) REFERENCES surveys(surveys_id),
    CONSTRAINT fk_ext_survey_resp_registration FOREIGN KEY (external_survey_responses_registration_id) REFERENCES event_registrations(event_registrations_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
