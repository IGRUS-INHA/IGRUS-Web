CREATE TABLE event_attachment (
    event_attachment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    file_metadata_id BIGINT NOT NULL,
    is_thumbnail BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL,

    CONSTRAINT fk_event_attachment_event
        FOREIGN KEY (event_id) REFERENCES events(event_id),
    CONSTRAINT fk_event_attachment_file_metadata
        FOREIGN KEY (file_metadata_id) REFERENCES file_metadata(file_metadata_id),
    CONSTRAINT uk_event_attachment_event_file
        UNIQUE (event_id, file_metadata_id),

    INDEX idx_event_attachment_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
