-- V6: Board tables creation
-- 게시판 및 게시판 권한 테이블 생성

-- boards 테이블
CREATE TABLE boards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    allows_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    allows_question_tag BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by BIGINT,
    updated_by BIGINT,
    PRIMARY KEY (id),
    INDEX idx_boards_code (code),
    INDEX idx_boards_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- board_permissions 테이블
CREATE TABLE board_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    board_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    can_read BOOLEAN NOT NULL DEFAULT FALSE,
    can_write BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_board_permissions_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    UNIQUE KEY uk_board_permissions_board_role (board_id, role),
    INDEX idx_board_permissions_board_id (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 초기 게시판 데이터 INSERT
INSERT INTO boards (code, name, description, allows_anonymous, allows_question_tag, display_order) VALUES
('notices', '공지사항', '동아리 공지사항을 확인할 수 있습니다.', FALSE, FALSE, 1),
('general', '자유게시판', '자유롭게 이야기를 나눌 수 있는 공간입니다.', TRUE, TRUE, 2),
('insight', '정보공유', '유용한 정보를 공유하는 게시판입니다.', FALSE, FALSE, 3);

-- 게시판별 권한 초기 데이터 INSERT
-- notices (공지사항): ASSOCIATE 읽기만(준회원 공개글만), MEMBER 이상 읽기, OPERATOR 이상 쓰기
INSERT INTO board_permissions (board_id, role, can_read, can_write) VALUES
((SELECT id FROM boards WHERE code = 'notices'), 'ASSOCIATE', TRUE, FALSE),
((SELECT id FROM boards WHERE code = 'notices'), 'MEMBER', TRUE, FALSE),
((SELECT id FROM boards WHERE code = 'notices'), 'OPERATOR', TRUE, TRUE),
((SELECT id FROM boards WHERE code = 'notices'), 'ADMIN', TRUE, TRUE);

-- general (자유게시판): MEMBER 이상 읽기/쓰기
INSERT INTO board_permissions (board_id, role, can_read, can_write) VALUES
((SELECT id FROM boards WHERE code = 'general'), 'ASSOCIATE', FALSE, FALSE),
((SELECT id FROM boards WHERE code = 'general'), 'MEMBER', TRUE, TRUE),
((SELECT id FROM boards WHERE code = 'general'), 'OPERATOR', TRUE, TRUE),
((SELECT id FROM boards WHERE code = 'general'), 'ADMIN', TRUE, TRUE);

-- insight (정보공유): MEMBER 이상 읽기/쓰기
INSERT INTO board_permissions (board_id, role, can_read, can_write) VALUES
((SELECT id FROM boards WHERE code = 'insight'), 'ASSOCIATE', FALSE, FALSE),
((SELECT id FROM boards WHERE code = 'insight'), 'MEMBER', TRUE, TRUE),
((SELECT id FROM boards WHERE code = 'insight'), 'OPERATOR', TRUE, TRUE),
((SELECT id FROM boards WHERE code = 'insight'), 'ADMIN', TRUE, TRUE);
