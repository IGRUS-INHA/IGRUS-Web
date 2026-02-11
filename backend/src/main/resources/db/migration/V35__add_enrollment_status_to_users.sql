-- 재학 상태(enrollment_status) 컬럼 추가 (nullable, 기존 데이터 호환)
ALTER TABLE users ADD COLUMN users_enrollment_status VARCHAR(30) NULL;
