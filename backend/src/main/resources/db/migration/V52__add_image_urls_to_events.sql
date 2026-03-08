-- event_images 테이블 생성
-- 행사당 최대 5개 이미지 저장 (post_images와 동일한 구조)
CREATE TABLE event_images (
    event_images_id             BIGINT      NOT NULL AUTO_INCREMENT,
    event_images_event_id       BIGINT      NOT NULL,
    event_images_image_url      VARCHAR(500) NOT NULL,
    event_images_display_order  INT         NOT NULL DEFAULT 0,
    event_images_created_at     DATETIME(6) NOT NULL,
    event_images_updated_at     DATETIME(6) NOT NULL,
    event_images_created_by     BIGINT,
    event_images_updated_by     BIGINT,
    PRIMARY KEY (event_images_id),
    CONSTRAINT fk_event_images_event
        FOREIGN KEY (event_images_event_id) REFERENCES events (event_id) ON DELETE CASCADE
);

CREATE INDEX idx_event_images_event_id ON event_images (event_images_event_id);
