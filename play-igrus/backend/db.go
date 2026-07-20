package main

import (
	"database/sql"
	"errors"
	"strings"
	"time"

	_ "github.com/go-sql-driver/mysql"
)

var errUnauthorized = errors.New("unauthorized")
var errNotFound = errors.New("not found")

// ponytail: 임베디드 idempotent 스키마. 프로젝트 수백 건 규모라 golang-migrate 는
// 과하다. 스키마 변경이 잦아지면 그때 마이그레이션 도구 도입.
var schema = []string{
	`CREATE TABLE IF NOT EXISTS projects (
	  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
	  student_id          VARCHAR(8)    NOT NULL,
	  author_name         VARCHAR(50)   NOT NULL,
	  department          VARCHAR(50)   NOT NULL DEFAULT '',
	  title               VARCHAR(100)  NOT NULL,
	  description         VARCHAR(50)   NOT NULL DEFAULT '',
	  body_md             MEDIUMTEXT    NOT NULL,
	  thumbnail_key       VARCHAR(255)  NOT NULL DEFAULT '',
	  banner_key          VARCHAR(255)  NOT NULL DEFAULT '',
	  redirect_url        VARCHAR(2048) NOT NULL,
	  category            VARCHAR(20)   NOT NULL,
	  status              VARCHAR(10)   NOT NULL DEFAULT 'pending',
	  reject_reason       VARCHAR(500)  NOT NULL DEFAULT '',
	  reviewer_student_id VARCHAR(8)    NOT NULL DEFAULT '',
	  reviewer_name       VARCHAR(50)   NOT NULL DEFAULT '',
	  reviewed_at         DATETIME(3)   NULL,
	  created_at          DATETIME(3)   NOT NULL,
	  total_clicks        BIGINT        NOT NULL DEFAULT 0,
	  score               DOUBLE        NOT NULL DEFAULT 0,
	  INDEX idx_projects_status_score (status, score DESC),
	  INDEX idx_projects_student (student_id)
	) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`,
	`CREATE TABLE IF NOT EXISTS project_clicks_daily (
	  project_id BIGINT NOT NULL,
	  click_date DATE   NOT NULL,
	  clicks     BIGINT NOT NULL DEFAULT 0,
	  PRIMARY KEY (project_id, click_date)
	) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`,
}

// row 는 projects 테이블 한 행. JSON 응답 형태는 projects.go 의 뷰 변환이 결정한다.
type row struct {
	ID                int64
	StudentID         string
	AuthorName        string
	Department        string
	Title             string
	Description       string
	BodyMD            string
	ThumbnailKey      string
	BannerKey         string
	RedirectURL       string
	Category          string
	Status            string
	RejectReason      string
	ReviewerStudentID string
	ReviewerName      string
	ReviewedAt        *time.Time
	CreatedAt         time.Time
	TotalClicks       int64
	Score             float64
}

func openDB(dsn string) (*sql.DB, error) {
	// parseTime 없이는 DATETIME 스캔이 전부 실패한다 — 설정 실수를 부팅 시점에 잡는다.
	if !strings.Contains(dsn, "parseTime=true") {
		return nil, errors.New("DSN 에 parseTime=true 가 필요합니다")
	}
	db, err := sql.Open("mysql", dsn)
	if err != nil {
		return nil, err
	}
	db.SetMaxOpenConns(5)
	db.SetConnMaxLifetime(5 * time.Minute)
	if err := db.Ping(); err != nil {
		return nil, err
	}
	for _, stmt := range schema {
		if _, err := db.Exec(stmt); err != nil {
			return nil, err
		}
	}
	return db, nil
}

const selectCols = `id, student_id, author_name, department, title, description, body_md,
	thumbnail_key, banner_key, redirect_url, category, status, reject_reason,
	reviewer_student_id, reviewer_name, reviewed_at, created_at, total_clicks, score`

func scanRows(rows *sql.Rows) ([]row, error) {
	defer rows.Close()
	// nil 이 아닌 빈 슬라이스로 시작해야 JSON 이 null 대신 [] 로 나간다.
	out := []row{}
	for rows.Next() {
		var p row
		var reviewed sql.NullTime
		err := rows.Scan(&p.ID, &p.StudentID, &p.AuthorName, &p.Department, &p.Title,
			&p.Description, &p.BodyMD, &p.ThumbnailKey, &p.BannerKey, &p.RedirectURL,
			&p.Category, &p.Status, &p.RejectReason, &p.ReviewerStudentID, &p.ReviewerName,
			&reviewed, &p.CreatedAt, &p.TotalClicks, &p.Score)
		if err != nil {
			return nil, err
		}
		if reviewed.Valid {
			t := reviewed.Time
			p.ReviewedAt = &t
		}
		out = append(out, p)
	}
	return out, rows.Err()
}

func insertProject(db *sql.DB, p row) (int64, error) {
	res, err := db.Exec(
		`INSERT INTO projects
		 (student_id, author_name, department, title, description, body_md,
		  thumbnail_key, banner_key, redirect_url, category, created_at)
		 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
		p.StudentID, p.AuthorName, p.Department, p.Title, p.Description, p.BodyMD,
		p.ThumbnailKey, p.BannerKey, p.RedirectURL, p.Category, time.Now().UTC())
	if err != nil {
		return 0, err
	}
	return res.LastInsertId()
}

// listApprovedProjects 는 인기순(score DESC), 동점이면 최신순.
func listApprovedProjects(db *sql.DB, category string) ([]row, error) {
	q := `SELECT ` + selectCols + ` FROM projects WHERE status = 'approved'`
	args := []any{}
	if category != "" {
		q += ` AND category = ?`
		args = append(args, category)
	}
	q += ` ORDER BY score DESC, created_at DESC`
	rows, err := db.Query(q, args...)
	if err != nil {
		return nil, err
	}
	return scanRows(rows)
}

func listByStatus(db *sql.DB, status string) ([]row, error) {
	rows, err := db.Query(
		`SELECT `+selectCols+` FROM projects WHERE status = ? ORDER BY created_at DESC`, status)
	if err != nil {
		return nil, err
	}
	return scanRows(rows)
}

func listByStudent(db *sql.DB, studentID string) ([]row, error) {
	rows, err := db.Query(
		`SELECT `+selectCols+` FROM projects WHERE student_id = ? ORDER BY created_at DESC`, studentID)
	if err != nil {
		return nil, err
	}
	return scanRows(rows)
}

func getProject(db *sql.DB, id int64) (row, error) {
	rows, err := db.Query(`SELECT `+selectCols+` FROM projects WHERE id = ?`, id)
	if err != nil {
		return row{}, err
	}
	items, err := scanRows(rows)
	if err != nil {
		return row{}, err
	}
	if len(items) == 0 {
		return row{}, errNotFound
	}
	return items[0], nil
}

// review 는 pending 상태인 건만 승인/거절한다.
// WHERE 에 status='pending' 을 넣어 두 운영진이 동시에 눌러도 한 번만 반영되게 한다.
// 승인한 운영진 정보를 함께 기록한다. (스펙)
func review(db *sql.DB, id int64, status, reason string, reviewer Profile) error {
	res, err := db.Exec(
		`UPDATE projects SET status = ?, reject_reason = ?, reviewer_student_id = ?,
		 reviewer_name = ?, reviewed_at = ? WHERE id = ? AND status = 'pending'`,
		status, reason, reviewer.StudentID, reviewer.Name, time.Now().UTC(), id)
	if err != nil {
		return err
	}
	n, err := res.RowsAffected()
	if err != nil {
		return err
	}
	if n == 0 {
		return errNotFound
	}
	return nil
}

// addClick 은 승인된 작품의 클릭을 총합 + 일별(KST 날짜) 버킷에 기록한다.
func addClick(db *sql.DB, id int64, date string) error {
	res, err := db.Exec(
		`UPDATE projects SET total_clicks = total_clicks + 1 WHERE id = ? AND status = 'approved'`, id)
	if err != nil {
		return err
	}
	n, err := res.RowsAffected()
	if err != nil {
		return err
	}
	if n == 0 {
		return errNotFound
	}
	_, err = db.Exec(
		`INSERT INTO project_clicks_daily (project_id, click_date, clicks) VALUES (?, ?, 1)
		 ON DUPLICATE KEY UPDATE clicks = clicks + 1`, id, date)
	return err
}
