-- =====================================================
-- V5: 익명화 지원을 위한 users 테이블 컬럼 수정
-- =====================================================

-- studentId 길이를 20으로 확장 (DELETED_123 형태 지원)
ALTER TABLE users MODIFY COLUMN users_student_id VARCHAR(20) NOT NULL;

-- phoneNumber, department, motivation을 nullable로 변경 (익명화 시 null 지원)
ALTER TABLE users MODIFY COLUMN users_phone_number VARCHAR(20) NULL;
ALTER TABLE users MODIFY COLUMN users_department VARCHAR(50) NULL;
ALTER TABLE users MODIFY COLUMN users_motivation TEXT NULL;
