-- 준회원 승인/거절 결정 테이블 생성 및 PasswordCredential에서 승인 정보 분리

-- 1. 새 테이블 생성
CREATE TABLE associate_decisions (
    associate_decisions_id BIGINT NOT NULL AUTO_INCREMENT,
    associate_decisions_user_id BIGINT NOT NULL,
    associate_decisions_type VARCHAR(20) NOT NULL,
    associate_decisions_reason VARCHAR(255),
    associate_decisions_decided_by BIGINT NOT NULL,
    associate_decisions_decided_at DATETIME(6) NOT NULL,
    associate_decisions_created_at DATETIME(6) NOT NULL,
    associate_decisions_updated_at DATETIME(6) NOT NULL,
    associate_decisions_created_by BIGINT,
    associate_decisions_updated_by BIGINT,
    PRIMARY KEY (associate_decisions_id),
    UNIQUE KEY uk_associate_decisions_user_id (associate_decisions_user_id),
    CONSTRAINT fk_associate_decisions_user FOREIGN KEY (associate_decisions_user_id) REFERENCES users(users_id)
);

-- 2. 기존 승인 데이터 마이그레이션
INSERT INTO associate_decisions (
    associate_decisions_user_id, associate_decisions_type, associate_decisions_decided_by,
    associate_decisions_decided_at, associate_decisions_created_at, associate_decisions_updated_at,
    associate_decisions_created_by, associate_decisions_updated_by
)
SELECT pc.password_credentials_user_id, 'APPROVED', pc.password_credentials_approved_by,
       pc.password_credentials_approved_at, pc.password_credentials_approved_at, pc.password_credentials_approved_at,
       pc.password_credentials_approved_by, pc.password_credentials_approved_by
FROM password_credentials pc
WHERE pc.password_credentials_approved_at IS NOT NULL;

-- 3. 기존 컬럼 제거
ALTER TABLE password_credentials DROP COLUMN password_credentials_approved_at;
ALTER TABLE password_credentials DROP COLUMN password_credentials_approved_by;
