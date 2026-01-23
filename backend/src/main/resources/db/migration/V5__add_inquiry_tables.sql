-- 문의(Inquiry) 기능 테이블 생성
-- PRD V2 기반 문의 도메인

-- 1. inquiries 테이블 (문의 기본 정보)
CREATE TABLE inquiries (
    inquiries_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiries_inquiry_number VARCHAR(20) NOT NULL UNIQUE,
    inquiries_type VARCHAR(20) NOT NULL,
    inquiries_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    inquiries_title VARCHAR(100) NOT NULL,
    inquiries_content TEXT NOT NULL,
    inquiries_guest_email VARCHAR(255),
    inquiries_guest_name VARCHAR(50),
    inquiries_password_hash VARCHAR(255),
    inquiries_user_id BIGINT,
    inquiries_created_at DATETIME(6) NOT NULL,
    inquiries_updated_at DATETIME(6) NOT NULL,
    inquiries_created_by BIGINT,
    inquiries_updated_by BIGINT,
    inquiries_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    inquiries_deleted_at DATETIME(6),
    inquiries_deleted_by BIGINT,

    CONSTRAINT fk_inquiries_user
        FOREIGN KEY (inquiries_user_id) REFERENCES users(users_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. inquiry_attachments 테이블 (첨부파일)
CREATE TABLE inquiry_attachments (
    inquiry_attachments_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_attachments_inquiry_id BIGINT NOT NULL,
    inquiry_attachments_file_url VARCHAR(500) NOT NULL,
    inquiry_attachments_file_name VARCHAR(255) NOT NULL,
    inquiry_attachments_file_size BIGINT NOT NULL,
    inquiry_attachments_created_at DATETIME(6) NOT NULL,
    inquiry_attachments_updated_at DATETIME(6) NOT NULL,
    inquiry_attachments_created_by BIGINT,
    inquiry_attachments_updated_by BIGINT,

    CONSTRAINT fk_inquiry_attachments_inquiry
        FOREIGN KEY (inquiry_attachments_inquiry_id) REFERENCES inquiries(inquiries_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. inquiry_replies 테이블 (답변, 1:1)
CREATE TABLE inquiry_replies (
    inquiry_replies_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_replies_inquiry_id BIGINT NOT NULL UNIQUE,
    inquiry_replies_content TEXT NOT NULL,
    inquiry_replies_replied_by BIGINT NOT NULL,
    inquiry_replies_created_at DATETIME(6) NOT NULL,
    inquiry_replies_updated_at DATETIME(6) NOT NULL,
    inquiry_replies_created_by BIGINT,
    inquiry_replies_updated_by BIGINT,

    CONSTRAINT fk_inquiry_replies_inquiry
        FOREIGN KEY (inquiry_replies_inquiry_id) REFERENCES inquiries(inquiries_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_inquiry_replies_user
        FOREIGN KEY (inquiry_replies_replied_by) REFERENCES users(users_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. inquiry_memos 테이블 (내부 메모, 비공개)
CREATE TABLE inquiry_memos (
    inquiry_memos_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_memos_inquiry_id BIGINT NOT NULL,
    inquiry_memos_content TEXT NOT NULL,
    inquiry_memos_written_by BIGINT NOT NULL,
    inquiry_memos_created_at DATETIME(6) NOT NULL,
    inquiry_memos_updated_at DATETIME(6) NOT NULL,
    inquiry_memos_created_by BIGINT,
    inquiry_memos_updated_by BIGINT,

    CONSTRAINT fk_inquiry_memos_inquiry
        FOREIGN KEY (inquiry_memos_inquiry_id) REFERENCES inquiries(inquiries_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_inquiry_memos_user
        FOREIGN KEY (inquiry_memos_written_by) REFERENCES users(users_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 생성
CREATE INDEX idx_inquiries_type ON inquiries(inquiries_type);
CREATE INDEX idx_inquiries_status ON inquiries(inquiries_status);
CREATE INDEX idx_inquiries_user_id ON inquiries(inquiries_user_id);
CREATE INDEX idx_inquiries_guest_email ON inquiries(inquiries_guest_email);
CREATE INDEX idx_inquiries_created_at ON inquiries(inquiries_created_at);
CREATE INDEX idx_inquiries_deleted ON inquiries(inquiries_deleted);
CREATE INDEX idx_inquiry_attachments_inquiry_id ON inquiry_attachments(inquiry_attachments_inquiry_id);
CREATE INDEX idx_inquiry_replies_inquiry_id ON inquiry_replies(inquiry_replies_inquiry_id);
CREATE INDEX idx_inquiry_memos_inquiry_id ON inquiry_memos(inquiry_memos_inquiry_id);
