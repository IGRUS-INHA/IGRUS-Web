-- V6: Rename user_role_history and create board tables
-- user_role_history 테이블명/컬럼명 변경 및 게시판 테이블 생성

-- =====================================================
-- user_role_history → user_role_histories 변경
-- =====================================================
RENAME TABLE user_role_history TO user_role_histories;

-- FK 삭제 (인덱스 삭제 전에 필요)
ALTER TABLE user_role_histories DROP FOREIGN KEY fk_user_role_history_user;

-- 인덱스 삭제
DROP INDEX idx_user_role_history_user_id ON user_role_histories;
DROP INDEX idx_user_role_history_new_role ON user_role_histories;
DROP INDEX idx_user_role_history_created_at ON user_role_histories;
DROP INDEX idx_user_role_history_created_by ON user_role_histories;

-- 컬럼명 변경
ALTER TABLE user_role_histories
    CHANGE COLUMN user_role_history_id user_role_histories_id BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN user_role_history_user_id user_role_histories_user_id BIGINT NOT NULL,
    CHANGE COLUMN user_role_history_previous_role user_role_histories_previous_role VARCHAR(20) NOT NULL,
    CHANGE COLUMN user_role_history_new_role user_role_histories_new_role VARCHAR(20) NOT NULL,
    CHANGE COLUMN user_role_history_reason user_role_histories_reason VARCHAR(255),
    CHANGE COLUMN user_role_history_created_at user_role_histories_created_at DATETIME(6) NOT NULL,
    CHANGE COLUMN user_role_history_updated_at user_role_histories_updated_at DATETIME(6) NOT NULL,
    CHANGE COLUMN user_role_history_created_by user_role_histories_created_by BIGINT,
    CHANGE COLUMN user_role_history_updated_by user_role_histories_updated_by BIGINT;

-- 인덱스 재생성
CREATE INDEX idx_user_role_histories_user_id ON user_role_histories(user_role_histories_user_id);
CREATE INDEX idx_user_role_histories_new_role ON user_role_histories(user_role_histories_new_role);
CREATE INDEX idx_user_role_histories_created_at ON user_role_histories(user_role_histories_created_at);
CREATE INDEX idx_user_role_histories_created_by ON user_role_histories(user_role_histories_created_by);

-- FK 재생성
ALTER TABLE user_role_histories
    ADD CONSTRAINT fk_user_role_histories_user FOREIGN KEY (user_role_histories_user_id) REFERENCES users(users_id) ON DELETE CASCADE;

-- =====================================================
-- 게시판 테이블 생성
-- =====================================================

-- boards 테이블
CREATE TABLE boards (
    boards_id BIGINT NOT NULL AUTO_INCREMENT,
    boards_code VARCHAR(20) NOT NULL UNIQUE,
    boards_name VARCHAR(50) NOT NULL,
    boards_description VARCHAR(200),
    boards_allows_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    boards_allows_question_tag BOOLEAN NOT NULL DEFAULT FALSE,
    boards_display_order INT NOT NULL DEFAULT 0,
    boards_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    boards_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    boards_created_by BIGINT,
    boards_updated_by BIGINT,
    PRIMARY KEY (boards_id),
    INDEX idx_boards_code (boards_code),
    INDEX idx_boards_display_order (boards_display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- board_permissions 테이블
CREATE TABLE board_permissions (
    board_permissions_id BIGINT NOT NULL AUTO_INCREMENT,
    board_permissions_board_id BIGINT NOT NULL,
    board_permissions_role VARCHAR(20) NOT NULL,
    board_permissions_can_read BOOLEAN NOT NULL DEFAULT FALSE,
    board_permissions_can_write BOOLEAN NOT NULL DEFAULT FALSE,
    board_permissions_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    board_permissions_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    board_permissions_created_by BIGINT,
    board_permissions_updated_by BIGINT,
    PRIMARY KEY (board_permissions_id),
    UNIQUE KEY uk_board_permissions_board_role (board_permissions_board_id, board_permissions_role),
    INDEX idx_board_permissions_board_id (board_permissions_board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- FK 별도 추가 (이름 명확화)
ALTER TABLE board_permissions
    ADD CONSTRAINT fk_board_permissions_board FOREIGN KEY (board_permissions_board_id) REFERENCES boards(boards_id) ON DELETE CASCADE;

-- 초기 게시판 데이터 INSERT
INSERT INTO boards (boards_code, boards_name, boards_description, boards_allows_anonymous, boards_allows_question_tag, boards_display_order) VALUES
('NOTICES', '공지사항', '동아리 공지사항을 확인할 수 있습니다.', FALSE, FALSE, 1),
('GENERAL', '자유게시판', '자유롭게 이야기를 나눌 수 있는 공간입니다.', TRUE, TRUE, 2),
('INSIGHT', '정보공유', '유용한 정보를 공유하는 게시판입니다.', FALSE, FALSE, 3);

-- 게시판별 권한 초기 데이터 INSERT
-- NOTICES (공지사항): ASSOCIATE 읽기만(준회원 공개글만), MEMBER 이상 읽기, OPERATOR 이상 쓰기
INSERT INTO board_permissions (board_permissions_board_id, board_permissions_role, board_permissions_can_read, board_permissions_can_write) VALUES
((SELECT boards_id FROM boards WHERE boards_code = 'NOTICES'), 'ASSOCIATE', TRUE, FALSE),
((SELECT boards_id FROM boards WHERE boards_code = 'NOTICES'), 'MEMBER', TRUE, FALSE),
((SELECT boards_id FROM boards WHERE boards_code = 'NOTICES'), 'OPERATOR', TRUE, TRUE),
((SELECT boards_id FROM boards WHERE boards_code = 'NOTICES'), 'ADMIN', TRUE, TRUE);

-- GENERAL (자유게시판): MEMBER 이상 읽기/쓰기
INSERT INTO board_permissions (board_permissions_board_id, board_permissions_role, board_permissions_can_read, board_permissions_can_write) VALUES
((SELECT boards_id FROM boards WHERE boards_code = 'GENERAL'), 'ASSOCIATE', FALSE, FALSE),
((SELECT boards_id FROM boards WHERE boards_code = 'GENERAL'), 'MEMBER', TRUE, TRUE),
((SELECT boards_id FROM boards WHERE boards_code = 'GENERAL'), 'OPERATOR', TRUE, TRUE),
((SELECT boards_id FROM boards WHERE boards_code = 'GENERAL'), 'ADMIN', TRUE, TRUE);

-- INSIGHT (정보공유): MEMBER 이상 읽기/쓰기
INSERT INTO board_permissions (board_permissions_board_id, board_permissions_role, board_permissions_can_read, board_permissions_can_write) VALUES
((SELECT boards_id FROM boards WHERE boards_code = 'INSIGHT'), 'ASSOCIATE', FALSE, FALSE),
((SELECT boards_id FROM boards WHERE boards_code = 'INSIGHT'), 'MEMBER', TRUE, TRUE),
((SELECT boards_id FROM boards WHERE boards_code = 'INSIGHT'), 'OPERATOR', TRUE, TRUE),
((SELECT boards_id FROM boards WHERE boards_code = 'INSIGHT'), 'ADMIN', TRUE, TRUE);
