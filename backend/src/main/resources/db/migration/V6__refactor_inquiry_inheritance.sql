-- Inquiry 엔티티 회원/비회원 분리 리팩토링
-- JPA JOINED 상속 전략 적용

-- 1. inquiries 테이블에 discriminator 컬럼 추가
ALTER TABLE inquiries ADD COLUMN inquiries_author_type VARCHAR(20) NOT NULL DEFAULT 'GUEST';

-- 2. member_inquiries 테이블 생성 (회원 문의)
CREATE TABLE member_inquiries (
    inquiries_id BIGINT PRIMARY KEY,
    member_inquiries_user_id BIGINT NOT NULL,

    CONSTRAINT fk_member_inquiries_inquiry
        FOREIGN KEY (inquiries_id) REFERENCES inquiries(inquiries_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_member_inquiries_user
        FOREIGN KEY (member_inquiries_user_id) REFERENCES users(users_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. guest_inquiries 테이블 생성 (비회원 문의)
CREATE TABLE guest_inquiries (
    inquiries_id BIGINT PRIMARY KEY,
    guest_inquiries_email VARCHAR(255) NOT NULL,
    guest_inquiries_name VARCHAR(50) NOT NULL,
    guest_inquiries_password_hash VARCHAR(255) NOT NULL,

    CONSTRAINT fk_guest_inquiries_inquiry
        FOREIGN KEY (inquiries_id) REFERENCES inquiries(inquiries_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 기존 데이터 마이그레이션

-- 4.1 회원 문의 데이터 마이그레이션
INSERT INTO member_inquiries (inquiries_id, member_inquiries_user_id)
SELECT inquiries_id, inquiries_user_id
FROM inquiries
WHERE inquiries_user_id IS NOT NULL;

-- 4.2 회원 문의 discriminator 업데이트
UPDATE inquiries
SET inquiries_author_type = 'MEMBER'
WHERE inquiries_user_id IS NOT NULL;

-- 4.3 비회원 문의 데이터 마이그레이션
INSERT INTO guest_inquiries (inquiries_id, guest_inquiries_email, guest_inquiries_name, guest_inquiries_password_hash)
SELECT inquiries_id, inquiries_guest_email, inquiries_guest_name, inquiries_password_hash
FROM inquiries
WHERE inquiries_user_id IS NULL AND inquiries_guest_email IS NOT NULL;

-- 5. 부모 테이블에서 중복 컬럼 제거
ALTER TABLE inquiries DROP FOREIGN KEY fk_inquiries_user;
ALTER TABLE inquiries DROP COLUMN inquiries_user_id;
ALTER TABLE inquiries DROP COLUMN inquiries_guest_email;
ALTER TABLE inquiries DROP COLUMN inquiries_guest_name;
ALTER TABLE inquiries DROP COLUMN inquiries_password_hash;

-- 6. 인덱스 생성
CREATE INDEX idx_member_inquiries_user_id ON member_inquiries(member_inquiries_user_id);
CREATE INDEX idx_guest_inquiries_email ON guest_inquiries(guest_inquiries_email);
CREATE INDEX idx_inquiries_author_type ON inquiries(inquiries_author_type);

-- 7. 기존 인덱스 삭제 (더 이상 필요 없음)
DROP INDEX idx_inquiries_user_id ON inquiries;
DROP INDEX idx_inquiries_guest_email ON inquiries;
