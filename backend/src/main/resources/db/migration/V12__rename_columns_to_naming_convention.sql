-- V12: Rename columns to follow naming convention (table_name_column_name)
-- 테이블명_컬럼명 네이밍 컨벤션 적용

-- =====================================================
-- 1. user_role_history 테이블명 변경 (복수형)
-- =====================================================
RENAME TABLE user_role_history TO user_role_histories;

-- user_role_histories 컬럼명 변경
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

-- 인덱스 재생성 (user_role_histories)
DROP INDEX idx_user_role_history_user_id ON user_role_histories;
DROP INDEX idx_user_role_history_new_role ON user_role_histories;
DROP INDEX idx_user_role_history_created_at ON user_role_histories;
DROP INDEX idx_user_role_history_created_by ON user_role_histories;

CREATE INDEX idx_user_role_histories_user_id ON user_role_histories(user_role_histories_user_id);
CREATE INDEX idx_user_role_histories_new_role ON user_role_histories(user_role_histories_new_role);
CREATE INDEX idx_user_role_histories_created_at ON user_role_histories(user_role_histories_created_at);
CREATE INDEX idx_user_role_histories_created_by ON user_role_histories(user_role_histories_created_by);

-- =====================================================
-- 2. boards 테이블 컬럼명 변경
-- =====================================================
ALTER TABLE boards
    CHANGE COLUMN id boards_id BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN code boards_code VARCHAR(20) NOT NULL,
    CHANGE COLUMN name boards_name VARCHAR(50) NOT NULL,
    CHANGE COLUMN description boards_description VARCHAR(200),
    CHANGE COLUMN allows_anonymous boards_allows_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    CHANGE COLUMN allows_question_tag boards_allows_question_tag BOOLEAN NOT NULL DEFAULT FALSE,
    CHANGE COLUMN display_order boards_display_order INT NOT NULL DEFAULT 0,
    CHANGE COLUMN created_at boards_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHANGE COLUMN updated_at boards_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CHANGE COLUMN created_by boards_created_by BIGINT,
    CHANGE COLUMN updated_by boards_updated_by BIGINT;

-- 인덱스 재생성 (boards)
DROP INDEX idx_boards_code ON boards;
DROP INDEX idx_boards_display_order ON boards;

CREATE INDEX idx_boards_code ON boards(boards_code);
CREATE INDEX idx_boards_display_order ON boards(boards_display_order);

-- =====================================================
-- 3. board_permissions 테이블 컬럼명 변경
-- =====================================================

-- FK 삭제
ALTER TABLE board_permissions DROP FOREIGN KEY fk_board_permissions_board;

-- UK 삭제
ALTER TABLE board_permissions DROP INDEX uk_board_permissions_board_role;

-- 컬럼명 변경
ALTER TABLE board_permissions
    CHANGE COLUMN id board_permissions_id BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN board_id board_permissions_board_id BIGINT NOT NULL,
    CHANGE COLUMN role board_permissions_role VARCHAR(20) NOT NULL,
    CHANGE COLUMN can_read board_permissions_can_read BOOLEAN NOT NULL DEFAULT FALSE,
    CHANGE COLUMN can_write board_permissions_can_write BOOLEAN NOT NULL DEFAULT FALSE,
    CHANGE COLUMN created_at board_permissions_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- 신규 컬럼 추가 (BaseEntity 상속에 따른)
ALTER TABLE board_permissions
    ADD COLUMN board_permissions_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER board_permissions_created_at,
    ADD COLUMN board_permissions_created_by BIGINT AFTER board_permissions_updated_at,
    ADD COLUMN board_permissions_updated_by BIGINT AFTER board_permissions_created_by;

-- FK 재생성
ALTER TABLE board_permissions
    ADD CONSTRAINT fk_board_permissions_board FOREIGN KEY (board_permissions_board_id) REFERENCES boards(boards_id) ON DELETE CASCADE;

-- UK 재생성
ALTER TABLE board_permissions
    ADD CONSTRAINT uk_board_permissions_board_role UNIQUE (board_permissions_board_id, board_permissions_role);

-- 인덱스 재생성
DROP INDEX idx_board_permissions_board_id ON board_permissions;
CREATE INDEX idx_board_permissions_board_id ON board_permissions(board_permissions_board_id);

-- =====================================================
-- 4. posts 테이블 컬럼명 변경
-- =====================================================

-- FK 삭제
ALTER TABLE posts DROP FOREIGN KEY fk_posts_board;
ALTER TABLE posts DROP FOREIGN KEY fk_posts_author;

-- 컬럼명 변경
ALTER TABLE posts
    CHANGE COLUMN id posts_id BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN board_id posts_board_id BIGINT NOT NULL,
    CHANGE COLUMN author_id posts_author_id BIGINT NOT NULL,
    CHANGE COLUMN title posts_title VARCHAR(100) NOT NULL,
    CHANGE COLUMN content posts_content TEXT NOT NULL,
    CHANGE COLUMN view_count posts_view_count INT NOT NULL DEFAULT 0,
    CHANGE COLUMN version posts_version BIGINT,
    CHANGE COLUMN is_anonymous posts_is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    CHANGE COLUMN is_question posts_is_question BOOLEAN NOT NULL DEFAULT FALSE,
    CHANGE COLUMN is_visible_to_associate posts_is_visible_to_associate BOOLEAN NOT NULL DEFAULT FALSE,
    CHANGE COLUMN deleted posts_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CHANGE COLUMN deleted_at posts_deleted_at TIMESTAMP(6),
    CHANGE COLUMN deleted_by posts_deleted_by BIGINT,
    CHANGE COLUMN created_at posts_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHANGE COLUMN updated_at posts_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CHANGE COLUMN created_by posts_created_by BIGINT,
    CHANGE COLUMN updated_by posts_updated_by BIGINT;

-- FK 재생성
ALTER TABLE posts
    ADD CONSTRAINT fk_posts_board FOREIGN KEY (posts_board_id) REFERENCES boards(boards_id),
    ADD CONSTRAINT fk_posts_author FOREIGN KEY (posts_author_id) REFERENCES users(users_id);

-- 인덱스 재생성
DROP INDEX idx_posts_board_id ON posts;
DROP INDEX idx_posts_author_id ON posts;
DROP INDEX idx_posts_created_at ON posts;
DROP INDEX idx_posts_board_question ON posts;
DROP INDEX idx_posts_board_deleted ON posts;

CREATE INDEX idx_posts_board_id ON posts(posts_board_id);
CREATE INDEX idx_posts_author_id ON posts(posts_author_id);
CREATE INDEX idx_posts_created_at ON posts(posts_created_at);
CREATE INDEX idx_posts_board_question ON posts(posts_board_id, posts_is_question);
CREATE INDEX idx_posts_board_deleted ON posts(posts_board_id, posts_deleted);

-- =====================================================
-- 5. post_images 테이블 컬럼명 변경
-- =====================================================

-- FK 삭제
ALTER TABLE post_images DROP FOREIGN KEY fk_post_images_post;

-- 컬럼명 변경
ALTER TABLE post_images
    CHANGE COLUMN id post_images_id BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN post_id post_images_post_id BIGINT NOT NULL,
    CHANGE COLUMN image_url post_images_image_url VARCHAR(500) NOT NULL,
    CHANGE COLUMN display_order post_images_display_order INT NOT NULL DEFAULT 0,
    CHANGE COLUMN created_at post_images_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHANGE COLUMN updated_at post_images_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CHANGE COLUMN created_by post_images_created_by BIGINT,
    CHANGE COLUMN updated_by post_images_updated_by BIGINT;

-- FK 재생성
ALTER TABLE post_images
    ADD CONSTRAINT fk_post_images_post FOREIGN KEY (post_images_post_id) REFERENCES posts(posts_id) ON DELETE CASCADE;

-- 인덱스 재생성
DROP INDEX idx_post_images_post_id ON post_images;
CREATE INDEX idx_post_images_post_id ON post_images(post_images_post_id);

-- =====================================================
-- 6. post_views 테이블 컬럼명 변경
-- =====================================================

-- FK 삭제
ALTER TABLE post_views DROP FOREIGN KEY fk_post_views_post;
ALTER TABLE post_views DROP FOREIGN KEY fk_post_views_viewer;

-- 컬럼명 변경
ALTER TABLE post_views
    CHANGE COLUMN id post_views_id BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN post_id post_views_post_id BIGINT NOT NULL,
    CHANGE COLUMN viewer_id post_views_viewer_id BIGINT NOT NULL,
    CHANGE COLUMN viewed_at post_views_viewed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- 신규 컬럼 추가 (BaseEntity 상속에 따른)
ALTER TABLE post_views
    ADD COLUMN post_views_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER post_views_viewed_at,
    ADD COLUMN post_views_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER post_views_created_at,
    ADD COLUMN post_views_created_by BIGINT AFTER post_views_updated_at,
    ADD COLUMN post_views_updated_by BIGINT AFTER post_views_created_by;

-- FK 재생성
ALTER TABLE post_views
    ADD CONSTRAINT fk_post_views_post FOREIGN KEY (post_views_post_id) REFERENCES posts(posts_id),
    ADD CONSTRAINT fk_post_views_viewer FOREIGN KEY (post_views_viewer_id) REFERENCES users(users_id);

-- 인덱스 재생성
DROP INDEX idx_post_views_post_id ON post_views;
DROP INDEX idx_post_views_viewer_id ON post_views;
DROP INDEX idx_post_views_viewed_at ON post_views;

CREATE INDEX idx_post_views_post_id ON post_views(post_views_post_id);
CREATE INDEX idx_post_views_viewer_id ON post_views(post_views_viewer_id);
CREATE INDEX idx_post_views_viewed_at ON post_views(post_views_viewed_at);

-- =====================================================
-- 7. comments 테이블 컬럼명 변경
-- =====================================================

-- FK 삭제
ALTER TABLE comments DROP FOREIGN KEY fk_comment_post;
ALTER TABLE comments DROP FOREIGN KEY fk_comment_parent;
ALTER TABLE comments DROP FOREIGN KEY fk_comment_author;

-- 컬럼명 변경
ALTER TABLE comments
    CHANGE COLUMN id comments_id BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN post_id comments_post_id BIGINT NOT NULL,
    CHANGE COLUMN parent_comment_id comments_parent_comment_id BIGINT NULL,
    CHANGE COLUMN author_id comments_author_id BIGINT NOT NULL,
    CHANGE COLUMN content comments_content VARCHAR(500) NOT NULL,
    CHANGE COLUMN is_anonymous comments_is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    CHANGE COLUMN deleted comments_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CHANGE COLUMN deleted_at comments_deleted_at TIMESTAMP(6) NULL,
    CHANGE COLUMN deleted_by comments_deleted_by BIGINT NULL,
    CHANGE COLUMN created_at comments_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHANGE COLUMN updated_at comments_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CHANGE COLUMN created_by comments_created_by BIGINT NULL,
    CHANGE COLUMN updated_by comments_updated_by BIGINT NULL;

-- FK 재생성
ALTER TABLE comments
    ADD CONSTRAINT fk_comments_post FOREIGN KEY (comments_post_id) REFERENCES posts(posts_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comments_parent FOREIGN KEY (comments_parent_comment_id) REFERENCES comments(comments_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comments_author FOREIGN KEY (comments_author_id) REFERENCES users(users_id);

-- 인덱스 재생성
DROP INDEX idx_comment_post ON comments;
DROP INDEX idx_comment_parent ON comments;
DROP INDEX idx_comment_author ON comments;

CREATE INDEX idx_comments_post ON comments(comments_post_id);
CREATE INDEX idx_comments_parent ON comments(comments_parent_comment_id);
CREATE INDEX idx_comments_author ON comments(comments_author_id);

-- =====================================================
-- 8. comment_likes 테이블 컬럼명 변경
-- =====================================================

-- FK 삭제
ALTER TABLE comment_likes DROP FOREIGN KEY fk_comment_like_comment;
ALTER TABLE comment_likes DROP FOREIGN KEY fk_comment_like_user;

-- UK 삭제
ALTER TABLE comment_likes DROP INDEX uk_comment_like;

-- 컬럼명 변경
ALTER TABLE comment_likes
    CHANGE COLUMN id comment_likes_id BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN comment_id comment_likes_comment_id BIGINT NOT NULL,
    CHANGE COLUMN user_id comment_likes_user_id BIGINT NOT NULL,
    CHANGE COLUMN created_at comment_likes_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHANGE COLUMN updated_at comment_likes_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CHANGE COLUMN created_by comment_likes_created_by BIGINT NULL,
    CHANGE COLUMN updated_by comment_likes_updated_by BIGINT NULL;

-- FK 재생성
ALTER TABLE comment_likes
    ADD CONSTRAINT fk_comment_likes_comment FOREIGN KEY (comment_likes_comment_id) REFERENCES comments(comments_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comment_likes_user FOREIGN KEY (comment_likes_user_id) REFERENCES users(users_id);

-- UK 재생성
ALTER TABLE comment_likes
    ADD CONSTRAINT uk_comment_likes_comment_user UNIQUE (comment_likes_comment_id, comment_likes_user_id);

-- 인덱스 재생성
DROP INDEX idx_comment_like_user ON comment_likes;
CREATE INDEX idx_comment_likes_user ON comment_likes(comment_likes_user_id);

-- =====================================================
-- 9. comment_reports 테이블 컬럼명 변경
-- =====================================================

-- FK 삭제
ALTER TABLE comment_reports DROP FOREIGN KEY fk_comment_report_comment;
ALTER TABLE comment_reports DROP FOREIGN KEY fk_comment_report_reporter;
ALTER TABLE comment_reports DROP FOREIGN KEY fk_comment_report_resolver;

-- 컬럼명 변경
ALTER TABLE comment_reports
    CHANGE COLUMN id comment_reports_id BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN comment_id comment_reports_comment_id BIGINT NOT NULL,
    CHANGE COLUMN reporter_id comment_reports_reporter_id BIGINT NOT NULL,
    CHANGE COLUMN reason comment_reports_reason TEXT NOT NULL,
    CHANGE COLUMN status comment_reports_status ENUM('PENDING', 'RESOLVED', 'DISMISSED') NOT NULL DEFAULT 'PENDING',
    CHANGE COLUMN resolved_at comment_reports_resolved_at TIMESTAMP(6) NULL,
    CHANGE COLUMN resolved_by comment_reports_resolved_by BIGINT NULL,
    CHANGE COLUMN created_at comment_reports_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHANGE COLUMN updated_at comment_reports_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CHANGE COLUMN created_by comment_reports_created_by BIGINT NULL,
    CHANGE COLUMN updated_by comment_reports_updated_by BIGINT NULL;

-- FK 재생성
ALTER TABLE comment_reports
    ADD CONSTRAINT fk_comment_reports_comment FOREIGN KEY (comment_reports_comment_id) REFERENCES comments(comments_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comment_reports_reporter FOREIGN KEY (comment_reports_reporter_id) REFERENCES users(users_id),
    ADD CONSTRAINT fk_comment_reports_resolver FOREIGN KEY (comment_reports_resolved_by) REFERENCES users(users_id);

-- 인덱스 재생성
DROP INDEX idx_comment_report_status ON comment_reports;
CREATE INDEX idx_comment_reports_status ON comment_reports(comment_reports_status);
