-- =====================================================
-- IGRUS Web Database Schema
-- Version: V1 - Initial Schema
-- Database: MySQL 8.x
-- =====================================================

-- =====================================================
-- Level 0: 의존성 없는 테이블
-- =====================================================

-- -----------------------------------------------------
-- Table: positions (직책)
-- -----------------------------------------------------
CREATE TABLE positions (
    positions_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    positions_name VARCHAR(20) NOT NULL,
    positions_image_url VARCHAR(255),
    positions_display_order INT,
    -- BaseEntity 감사 컬럼
    positions_created_at DATETIME(6) NOT NULL,
    positions_updated_at DATETIME(6) NOT NULL,
    positions_created_by BIGINT,
    positions_updated_by BIGINT,
    -- SoftDeletableEntity 삭제 컬럼
    positions_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    positions_deleted_at DATETIME(6),
    positions_deleted_by BIGINT,

    CONSTRAINT uk_positions_name UNIQUE (positions_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: email_verifications (이메일 인증)
-- -----------------------------------------------------
CREATE TABLE email_verifications (
    email_verifications_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email_verifications_email VARCHAR(255) NOT NULL,
    email_verifications_code VARCHAR(6) NOT NULL,
    email_verifications_attempts INT NOT NULL DEFAULT 0,
    email_verifications_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verifications_expires_at DATETIME(6) NOT NULL,
    -- BaseEntity 감사 컬럼
    email_verifications_created_at DATETIME(6) NOT NULL,
    email_verifications_updated_at DATETIME(6) NOT NULL,
    email_verifications_created_by BIGINT,
    email_verifications_updated_by BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_email_verifications_email ON email_verifications(email_verifications_email);

-- =====================================================
-- Level 1: positions 의존 테이블
-- =====================================================

-- -----------------------------------------------------
-- Table: users (사용자)
-- -----------------------------------------------------
CREATE TABLE users (
    users_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    users_student_id VARCHAR(8) NOT NULL,
    users_name VARCHAR(50) NOT NULL,
    users_email VARCHAR(255) NOT NULL,
    users_phone_number VARCHAR(20) NOT NULL,
    users_department VARCHAR(50) NOT NULL,
    users_motivation TEXT NOT NULL,
    users_role VARCHAR(20) NOT NULL,
    users_status VARCHAR(20) NOT NULL,
    -- BaseEntity 감사 컬럼
    users_created_at DATETIME(6) NOT NULL,
    users_updated_at DATETIME(6) NOT NULL,
    users_created_by BIGINT,
    users_updated_by BIGINT,
    -- SoftDeletableEntity 삭제 컬럼
    users_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    users_deleted_at DATETIME(6),
    users_deleted_by BIGINT,

    CONSTRAINT uk_users_student_id UNIQUE (users_student_id),
    CONSTRAINT uk_users_email UNIQUE (users_email),
    CONSTRAINT uk_users_phone_number UNIQUE (users_phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_role ON users(users_role);
CREATE INDEX idx_users_status ON users(users_status);

-- =====================================================
-- Level 2: users 의존 테이블
-- =====================================================

-- -----------------------------------------------------
-- Table: user_positions (User-Position 중간 테이블)
-- -----------------------------------------------------
CREATE TABLE user_positions (
    user_positions_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_positions_user_id BIGINT NOT NULL,
    user_positions_position_id BIGINT NOT NULL,
    user_positions_assigned_at DATETIME(6) NOT NULL,
    -- BaseEntity 감사 컬럼
    user_positions_created_at DATETIME(6) NOT NULL,
    user_positions_updated_at DATETIME(6) NOT NULL,
    user_positions_created_by BIGINT,
    user_positions_updated_by BIGINT,

    CONSTRAINT uk_user_positions_user_position
        UNIQUE (user_positions_user_id, user_positions_position_id),
    CONSTRAINT fk_user_positions_user
        FOREIGN KEY (user_positions_user_id) REFERENCES users(users_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_positions_position
        FOREIGN KEY (user_positions_position_id) REFERENCES positions(positions_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: user_suspensions (사용자 정지 이력)
-- -----------------------------------------------------
CREATE TABLE user_suspensions (
    user_suspensions_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_suspensions_user_id BIGINT NOT NULL,
    user_suspensions_reason VARCHAR(255) NOT NULL,
    user_suspensions_suspended_at DATETIME(6) NOT NULL,
    user_suspensions_suspended_until DATETIME(6) NOT NULL,
    user_suspensions_suspended_by BIGINT NOT NULL,
    user_suspensions_lifted_at DATETIME(6),
    user_suspensions_lifted_by BIGINT,
    -- BaseEntity 감사 컬럼
    user_suspensions_created_at DATETIME(6) NOT NULL,
    user_suspensions_updated_at DATETIME(6) NOT NULL,
    user_suspensions_created_by BIGINT,
    user_suspensions_updated_by BIGINT,

    CONSTRAINT fk_user_suspensions_user
        FOREIGN KEY (user_suspensions_user_id) REFERENCES users(users_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 엔티티에 정의된 인덱스
CREATE INDEX idx_user_suspensions_user_id ON user_suspensions(user_suspensions_user_id);
CREATE INDEX idx_user_suspensions_suspended_at ON user_suspensions(user_suspensions_suspended_at);
CREATE INDEX idx_user_suspensions_suspended_until ON user_suspensions(user_suspensions_suspended_until);
CREATE INDEX idx_user_suspensions_lifted_at ON user_suspensions(user_suspensions_lifted_at);

-- -----------------------------------------------------
-- Table: user_role_history (사용자 역할 변경 이력)
-- -----------------------------------------------------
CREATE TABLE user_role_history (
    user_role_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_role_history_user_id BIGINT NOT NULL,
    user_role_history_previous_role VARCHAR(20) NOT NULL,
    user_role_history_new_role VARCHAR(20) NOT NULL,
    user_role_history_reason VARCHAR(255),
    -- BaseEntity 감사 컬럼
    user_role_history_created_at DATETIME(6) NOT NULL,
    user_role_history_updated_at DATETIME(6) NOT NULL,
    user_role_history_created_by BIGINT,
    user_role_history_updated_by BIGINT,

    CONSTRAINT fk_user_role_history_user
        FOREIGN KEY (user_role_history_user_id) REFERENCES users(users_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 엔티티에 정의된 인덱스
CREATE INDEX idx_user_role_history_user_id ON user_role_history(user_role_history_user_id);
CREATE INDEX idx_user_role_history_new_role ON user_role_history(user_role_history_new_role);
CREATE INDEX idx_user_role_history_created_at ON user_role_history(user_role_history_created_at);
CREATE INDEX idx_user_role_history_created_by ON user_role_history(user_role_history_created_by);

-- -----------------------------------------------------
-- Table: refresh_tokens (리프레시 토큰)
-- -----------------------------------------------------
CREATE TABLE refresh_tokens (
    refresh_tokens_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refresh_tokens_user_id BIGINT NOT NULL,
    refresh_tokens_token VARCHAR(255) NOT NULL,
    refresh_tokens_expires_at DATETIME(6) NOT NULL,
    refresh_tokens_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    -- BaseEntity 감사 컬럼
    refresh_tokens_created_at DATETIME(6) NOT NULL,
    refresh_tokens_updated_at DATETIME(6) NOT NULL,
    refresh_tokens_created_by BIGINT,
    refresh_tokens_updated_by BIGINT,

    CONSTRAINT uk_refresh_tokens_token UNIQUE (refresh_tokens_token),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (refresh_tokens_user_id) REFERENCES users(users_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(refresh_tokens_user_id);

-- -----------------------------------------------------
-- Table: privacy_consents (개인정보 동의)
-- -----------------------------------------------------
CREATE TABLE privacy_consents (
    privacy_consents_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    privacy_consents_user_id BIGINT NOT NULL,
    privacy_consents_consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    privacy_consents_consent_date DATETIME(6),
    privacy_consents_policy_version VARCHAR(255) NOT NULL,
    -- BaseEntity 감사 컬럼
    privacy_consents_created_at DATETIME(6) NOT NULL,
    privacy_consents_updated_at DATETIME(6) NOT NULL,
    privacy_consents_created_by BIGINT,
    privacy_consents_updated_by BIGINT,

    CONSTRAINT fk_privacy_consents_user
        FOREIGN KEY (privacy_consents_user_id) REFERENCES users(users_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_privacy_consents_user_id ON privacy_consents(privacy_consents_user_id);

-- -----------------------------------------------------
-- Table: password_credentials (비밀번호 인증 정보)
-- -----------------------------------------------------
CREATE TABLE password_credentials (
    password_credentials_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    password_credentials_user_id BIGINT NOT NULL,
    password_credentials_password_hash VARCHAR(255) NOT NULL,
    password_credentials_status VARCHAR(20) NOT NULL,
    password_credentials_approved_at DATETIME(6),
    password_credentials_approved_by BIGINT,
    -- BaseEntity 감사 컬럼
    password_credentials_created_at DATETIME(6) NOT NULL,
    password_credentials_updated_at DATETIME(6) NOT NULL,
    password_credentials_created_by BIGINT,
    password_credentials_updated_by BIGINT,
    -- SoftDeletableEntity 삭제 컬럼
    password_credentials_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    password_credentials_deleted_at DATETIME(6),
    password_credentials_deleted_by BIGINT,

    CONSTRAINT uk_password_credentials_user_id UNIQUE (password_credentials_user_id),
    CONSTRAINT fk_password_credentials_user
        FOREIGN KEY (password_credentials_user_id) REFERENCES users(users_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: password_reset_tokens (비밀번호 재설정 토큰)
-- -----------------------------------------------------
CREATE TABLE password_reset_tokens (
    password_reset_tokens_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    password_reset_tokens_user_id BIGINT NOT NULL,
    password_reset_tokens_token VARCHAR(255) NOT NULL,
    password_reset_tokens_expires_at DATETIME(6) NOT NULL,
    password_reset_tokens_used BOOLEAN NOT NULL DEFAULT FALSE,
    -- BaseEntity 감사 컬럼
    password_reset_tokens_created_at DATETIME(6) NOT NULL,
    password_reset_tokens_updated_at DATETIME(6) NOT NULL,
    password_reset_tokens_created_by BIGINT,
    password_reset_tokens_updated_by BIGINT,

    CONSTRAINT uk_password_reset_tokens_token UNIQUE (password_reset_tokens_token),
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (password_reset_tokens_user_id) REFERENCES users(users_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(password_reset_tokens_user_id);

-- -----------------------------------------------------
-- Table: inquiries (문의 - JOINED 상속 부모 테이블)
-- -----------------------------------------------------
CREATE TABLE inquiries (
    inquiries_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiries_author_type VARCHAR(20) NOT NULL,
    inquiries_inquiry_number VARCHAR(20) NOT NULL,
    inquiries_type VARCHAR(20) NOT NULL,
    inquiries_status VARCHAR(20) NOT NULL,
    inquiries_title VARCHAR(100) NOT NULL,
    inquiries_content TEXT NOT NULL,
    -- BaseEntity 감사 컬럼
    inquiries_created_at DATETIME(6) NOT NULL,
    inquiries_updated_at DATETIME(6) NOT NULL,
    inquiries_created_by BIGINT,
    inquiries_updated_by BIGINT,
    -- SoftDeletableEntity 삭제 컬럼
    inquiries_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    inquiries_deleted_at DATETIME(6),
    inquiries_deleted_by BIGINT,

    CONSTRAINT uk_inquiries_inquiry_number UNIQUE (inquiries_inquiry_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_inquiries_type ON inquiries(inquiries_type);
CREATE INDEX idx_inquiries_status ON inquiries(inquiries_status);
CREATE INDEX idx_inquiries_author_type ON inquiries(inquiries_author_type);

-- =====================================================
-- Level 3: inquiries + users 의존 테이블
-- =====================================================

-- -----------------------------------------------------
-- Table: member_inquiries (회원 문의 - JOINED 상속 자식 테이블)
-- -----------------------------------------------------
CREATE TABLE member_inquiries (
    inquiries_id BIGINT PRIMARY KEY,
    member_inquiries_user_id BIGINT NOT NULL,

    CONSTRAINT fk_member_inquiries_inquiry
        FOREIGN KEY (inquiries_id) REFERENCES inquiries(inquiries_id) ON DELETE CASCADE,
    CONSTRAINT fk_member_inquiries_user
        FOREIGN KEY (member_inquiries_user_id) REFERENCES users(users_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_member_inquiries_user_id ON member_inquiries(member_inquiries_user_id);

-- -----------------------------------------------------
-- Table: guest_inquiries (비회원 문의 - JOINED 상속 자식 테이블)
-- -----------------------------------------------------
CREATE TABLE guest_inquiries (
    inquiries_id BIGINT PRIMARY KEY,
    guest_inquiries_email VARCHAR(255) NOT NULL,
    guest_inquiries_name VARCHAR(50) NOT NULL,
    guest_inquiries_password_hash VARCHAR(255) NOT NULL,

    CONSTRAINT fk_guest_inquiries_inquiry
        FOREIGN KEY (inquiries_id) REFERENCES inquiries(inquiries_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: inquiry_attachments (문의 첨부파일)
-- -----------------------------------------------------
CREATE TABLE inquiry_attachments (
    inquiry_attachments_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_attachments_inquiry_id BIGINT NOT NULL,
    inquiry_attachments_file_url VARCHAR(500) NOT NULL,
    inquiry_attachments_file_name VARCHAR(255) NOT NULL,
    inquiry_attachments_file_size BIGINT NOT NULL,
    -- BaseEntity 감사 컬럼
    inquiry_attachments_created_at DATETIME(6) NOT NULL,
    inquiry_attachments_updated_at DATETIME(6) NOT NULL,
    inquiry_attachments_created_by BIGINT,
    inquiry_attachments_updated_by BIGINT,

    CONSTRAINT fk_inquiry_attachments_inquiry
        FOREIGN KEY (inquiry_attachments_inquiry_id) REFERENCES inquiries(inquiries_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_inquiry_attachments_inquiry_id ON inquiry_attachments(inquiry_attachments_inquiry_id);

-- -----------------------------------------------------
-- Table: inquiry_replies (문의 답변)
-- -----------------------------------------------------
CREATE TABLE inquiry_replies (
    inquiry_replies_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_replies_inquiry_id BIGINT NOT NULL,
    inquiry_replies_content TEXT NOT NULL,
    inquiry_replies_replied_by BIGINT NOT NULL,
    -- BaseEntity 감사 컬럼
    inquiry_replies_created_at DATETIME(6) NOT NULL,
    inquiry_replies_updated_at DATETIME(6) NOT NULL,
    inquiry_replies_created_by BIGINT,
    inquiry_replies_updated_by BIGINT,

    CONSTRAINT uk_inquiry_replies_inquiry_id UNIQUE (inquiry_replies_inquiry_id),
    CONSTRAINT fk_inquiry_replies_inquiry
        FOREIGN KEY (inquiry_replies_inquiry_id) REFERENCES inquiries(inquiries_id) ON DELETE CASCADE,
    CONSTRAINT fk_inquiry_replies_replied_by
        FOREIGN KEY (inquiry_replies_replied_by) REFERENCES users(users_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: inquiry_memos (문의 메모)
-- -----------------------------------------------------
CREATE TABLE inquiry_memos (
    inquiry_memos_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_memos_inquiry_id BIGINT NOT NULL,
    inquiry_memos_content TEXT NOT NULL,
    inquiry_memos_written_by BIGINT NOT NULL,
    -- BaseEntity 감사 컬럼
    inquiry_memos_created_at DATETIME(6) NOT NULL,
    inquiry_memos_updated_at DATETIME(6) NOT NULL,
    inquiry_memos_created_by BIGINT,
    inquiry_memos_updated_by BIGINT,

    CONSTRAINT fk_inquiry_memos_inquiry
        FOREIGN KEY (inquiry_memos_inquiry_id) REFERENCES inquiries(inquiries_id) ON DELETE CASCADE,
    CONSTRAINT fk_inquiry_memos_written_by
        FOREIGN KEY (inquiry_memos_written_by) REFERENCES users(users_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_inquiry_memos_inquiry_id ON inquiry_memos(inquiry_memos_inquiry_id);
