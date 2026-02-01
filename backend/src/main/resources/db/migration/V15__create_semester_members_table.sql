CREATE TABLE semester_members (
    semester_members_id BIGINT NOT NULL AUTO_INCREMENT,
    semester_members_user_id BIGINT NOT NULL,
    semester_members_year INT NOT NULL,
    semester_members_semester INT NOT NULL,
    semester_members_role VARCHAR(20) NOT NULL,
    semester_members_created_at DATETIME(6) NOT NULL,
    semester_members_updated_at DATETIME(6) NOT NULL,
    semester_members_created_by BIGINT,
    semester_members_updated_by BIGINT,
    PRIMARY KEY (semester_members_id),
    CONSTRAINT fk_semester_members_user FOREIGN KEY (semester_members_user_id) REFERENCES users(users_id),
    CONSTRAINT uk_semester_members_user_year_semester UNIQUE (semester_members_user_id, semester_members_year, semester_members_semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_semester_members_year_semester ON semester_members (semester_members_year, semester_members_semester);
