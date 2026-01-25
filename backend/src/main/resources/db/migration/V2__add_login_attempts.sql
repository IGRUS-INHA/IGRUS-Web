-- =====================================================
-- Login Attempts Table
-- Version: V2 - Add Login Attempts for Brute Force Protection
-- Database: MySQL 8.x
-- =====================================================

-- -----------------------------------------------------
-- Table: login_attempts (로그인 시도 기록)
-- -----------------------------------------------------
CREATE TABLE login_attempts (
    login_attempts_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_attempts_student_id VARCHAR(8) NOT NULL,
    login_attempts_attempt_count INT NOT NULL DEFAULT 0,
    login_attempts_last_attempt_at DATETIME(6) NOT NULL,
    login_attempts_locked_until DATETIME(6),
    login_attempts_created_at DATETIME(6) NOT NULL,
    login_attempts_updated_at DATETIME(6) NOT NULL,
    login_attempts_created_by BIGINT,
    login_attempts_updated_by BIGINT,
    CONSTRAINT uk_login_attempts_student_id UNIQUE (login_attempts_student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
