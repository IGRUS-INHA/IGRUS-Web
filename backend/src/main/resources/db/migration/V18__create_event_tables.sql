-- 행사 테이블
CREATE TABLE events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_user_id BIGINT NOT NULL,
    event_title VARCHAR(100) NOT NULL,
    event_description TEXT NOT NULL,
    event_location VARCHAR(200) NOT NULL,
    event_start_at TIMESTAMP NOT NULL,
    event_end_at TIMESTAMP NOT NULL,
    event_registration_start_at TIMESTAMP NOT NULL,
    event_registration_end_at TIMESTAMP NOT NULL,
    event_capacity INT NOT NULL,
    event_current_count INT NOT NULL DEFAULT 0,
    event_version BIGINT DEFAULT 0,
    event_status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    event_close_reason VARCHAR(20),
    event_registration_type VARCHAR(20) NOT NULL,
    event_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    event_created_by BIGINT,
    event_updated_by BIGINT,
    CONSTRAINT fk_events_user FOREIGN KEY (event_user_id) REFERENCES users(users_id)
);

-- 행사 신청 테이블
CREATE TABLE event_registrations (
    event_registrations_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_registrations_event_id BIGINT NOT NULL,
    event_registrations_user_id BIGINT NOT NULL,
    event_registrations_registered_at TIMESTAMP NOT NULL,
    event_registrations_status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    event_registrations_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_registrations_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    event_registrations_created_by BIGINT,
    event_registrations_updated_by BIGINT,
    CONSTRAINT fk_event_registrations_event FOREIGN KEY (event_registrations_event_id) REFERENCES events(event_id),
    CONSTRAINT fk_event_registrations_user FOREIGN KEY (event_registrations_user_id) REFERENCES users(users_id),
    CONSTRAINT uk_event_registrations_event_user UNIQUE (event_registrations_event_id, event_registrations_user_id)
);

-- 인덱스
CREATE INDEX idx_events_status ON events(event_status);
CREATE INDEX idx_events_registration_end_at ON events(event_registration_end_at);
CREATE INDEX idx_event_registrations_user_id ON event_registrations(event_registrations_user_id);
CREATE INDEX idx_event_registrations_status ON event_registrations(event_registrations_status);
