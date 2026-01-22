-- IGRUS Web 초기 스키마 생성

-- 1. positions 테이블 (직책)
CREATE TABLE positions (
    positions_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    positions_name VARCHAR(20) NOT NULL UNIQUE,
    positions_image_url VARCHAR(255),
    positions_display_order INT,
    positions_created_at DATETIME(6) NOT NULL,
    positions_updated_at DATETIME(6) NOT NULL,
    positions_created_by BIGINT,
    positions_updated_by BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. users 테이블 (사용자 기본정보)
CREATE TABLE users (
    users_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    users_student_id VARCHAR(8) NOT NULL UNIQUE,
    users_name VARCHAR(50) NOT NULL,
    users_email VARCHAR(255) NOT NULL UNIQUE,
    users_phone_number VARCHAR(20) NOT NULL UNIQUE,
    users_department VARCHAR(50) NOT NULL,
    users_motivation TEXT NOT NULL,
    users_role VARCHAR(20) NOT NULL,
    users_position_id BIGINT,
    users_created_at DATETIME(6) NOT NULL,
    users_updated_at DATETIME(6) NOT NULL,
    users_created_by BIGINT,
    users_updated_by BIGINT,

    CONSTRAINT fk_users_position
        FOREIGN KEY (users_position_id) REFERENCES positions(positions_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. password_credentials 테이블 (패스워드 기반 인증 정보)
CREATE TABLE password_credentials (
    password_credentials_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    password_credentials_user_id BIGINT NOT NULL UNIQUE,
    password_credentials_password_hash VARCHAR(255) NOT NULL,
    password_credentials_status VARCHAR(20) NOT NULL,
    password_credentials_approved_at DATETIME(6),
    password_credentials_approved_by BIGINT,
    password_credentials_created_at DATETIME(6) NOT NULL,
    password_credentials_updated_at DATETIME(6) NOT NULL,
    password_credentials_created_by BIGINT,
    password_credentials_updated_by BIGINT,

    CONSTRAINT fk_password_credentials_user
        FOREIGN KEY (password_credentials_user_id) REFERENCES users(users_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. user_role_history 테이블 (사용자 역할 변경 이력)
CREATE TABLE user_role_history (
    user_role_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_role_history_user_id BIGINT NOT NULL,
    user_role_history_previous_role VARCHAR(20) NOT NULL,
    user_role_history_new_role VARCHAR(20) NOT NULL,
    user_role_history_reason VARCHAR(255),
    user_role_history_created_at DATETIME(6) NOT NULL,
    user_role_history_updated_at DATETIME(6) NOT NULL,
    user_role_history_created_by BIGINT,
    user_role_history_updated_by BIGINT,

    CONSTRAINT fk_user_role_history_user
        FOREIGN KEY (user_role_history_user_id) REFERENCES users(users_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 생성
CREATE INDEX idx_users_role ON users(users_role);
CREATE INDEX idx_user_role_history_user_id ON user_role_history(user_role_history_user_id);
CREATE INDEX idx_user_role_history_new_role ON user_role_history(user_role_history_new_role);
CREATE INDEX idx_user_role_history_created_at ON user_role_history(user_role_history_created_at);
CREATE INDEX idx_user_role_history_created_by ON user_role_history(user_role_history_created_by);