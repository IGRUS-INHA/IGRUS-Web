-- 사용자 테이블에 임시 학번 여부 플래그 추가
ALTER TABLE users ADD COLUMN users_has_temporary_student_id BOOLEAN NOT NULL DEFAULT FALSE;

-- 임시 학번 시퀀스 테이블 (연도별 순번 관리)
CREATE TABLE temp_student_id_sequences (
    temp_student_id_sequences_year INT NOT NULL PRIMARY KEY,
    temp_student_id_sequences_next_value INT NOT NULL DEFAULT 1
);
