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
	// 승인작 수정본 — 승인 전까지 라이브 버전(projects)은 그대로 유지된다.
	// 프로젝트당 수정본 1개(PK)만: 재수정하면 덮어쓴다.
	`CREATE TABLE IF NOT EXISTS project_revisions (
	  project_id    BIGINT        PRIMARY KEY,
	  title         VARCHAR(100)  NOT NULL,
	  description   VARCHAR(50)   NOT NULL DEFAULT '',
	  body_md       MEDIUMTEXT    NOT NULL,
	  thumbnail_key VARCHAR(255)  NOT NULL DEFAULT '',
	  banner_key    VARCHAR(255)  NOT NULL DEFAULT '',
	  redirect_url  VARCHAR(2048) NOT NULL,
	  category      VARCHAR(20)   NOT NULL,
	  status        VARCHAR(10)   NOT NULL DEFAULT 'pending',
	  reject_reason VARCHAR(500)  NOT NULL DEFAULT '',
	  created_at    DATETIME(3)   NOT NULL
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

// updateProjectContent 는 pending/rejected 작품의 직접 수정 — 심사 대기 상태로 되돌린다.
func updateProjectContent(db *sql.DB, id int64, p row) error {
	_, err := db.Exec(
		`UPDATE projects SET title = ?, description = ?, body_md = ?, thumbnail_key = ?,
		 banner_key = ?, redirect_url = ?, category = ?, status = 'pending', reject_reason = ''
		 WHERE id = ?`,
		p.Title, p.Description, p.BodyMD, p.ThumbnailKey, p.BannerKey, p.RedirectURL, p.Category, id)
	return err
}

// ── 수정본 (project_revisions) ─────────────────────────────────────────

type revision struct {
	ProjectID    int64
	Title        string
	Description  string
	BodyMD       string
	ThumbnailKey string
	BannerKey    string
	RedirectURL  string
	Category     string
	Status       string // pending | rejected
	RejectReason string
	CreatedAt    time.Time
}

const revCols = `project_id, title, description, body_md, thumbnail_key, banner_key,
	redirect_url, category, status, reject_reason, created_at`

func scanRevisions(rows *sql.Rows) (map[int64]revision, error) {
	defer rows.Close()
	out := map[int64]revision{}
	for rows.Next() {
		var v revision
		err := rows.Scan(&v.ProjectID, &v.Title, &v.Description, &v.BodyMD, &v.ThumbnailKey,
			&v.BannerKey, &v.RedirectURL, &v.Category, &v.Status, &v.RejectReason, &v.CreatedAt)
		if err != nil {
			return nil, err
		}
		out[v.ProjectID] = v
	}
	return out, rows.Err()
}

// upsertRevision — 재수정하면 이전 수정본(반려 포함)을 덮어쓰고 다시 심사 대기가 된다.
func upsertRevision(db *sql.DB, rev revision) error {
	_, err := db.Exec(
		`REPLACE INTO project_revisions
		 (project_id, title, description, body_md, thumbnail_key, banner_key,
		  redirect_url, category, status, reject_reason, created_at)
		 VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', '', ?)`,
		rev.ProjectID, rev.Title, rev.Description, rev.BodyMD, rev.ThumbnailKey,
		rev.BannerKey, rev.RedirectURL, rev.Category, time.Now().UTC())
	return err
}

func getRevision(db *sql.DB, projectID int64) (revision, error) {
	rows, err := db.Query(`SELECT `+revCols+` FROM project_revisions WHERE project_id = ?`, projectID)
	if err != nil {
		return revision{}, err
	}
	revs, err := scanRevisions(rows)
	if err != nil {
		return revision{}, err
	}
	rev, ok := revs[projectID]
	if !ok {
		return revision{}, errNotFound
	}
	return rev, nil
}

func listRevisionsByStudent(db *sql.DB, studentID string) (map[int64]revision, error) {
	rows, err := db.Query(
		`SELECT `+revCols+` FROM project_revisions
		 WHERE project_id IN (SELECT id FROM projects WHERE student_id = ?)`, studentID)
	if err != nil {
		return nil, err
	}
	return scanRevisions(rows)
}

// listPendingRevisionProjects 는 검수 대기열용 — 수정본이 대기 중인 프로젝트 행 + 수정본.
func listPendingRevisionProjects(db *sql.DB) ([]row, map[int64]revision, error) {
	revRows, err := db.Query(`SELECT ` + revCols + ` FROM project_revisions WHERE status = 'pending'`)
	if err != nil {
		return nil, nil, err
	}
	revs, err := scanRevisions(revRows)
	if err != nil {
		return nil, nil, err
	}
	rows, err := db.Query(`SELECT ` + selectCols + ` FROM projects
		WHERE id IN (SELECT project_id FROM project_revisions WHERE status = 'pending')
		ORDER BY created_at DESC`)
	if err != nil {
		return nil, nil, err
	}
	items, err := scanRows(rows)
	if err != nil {
		return nil, nil, err
	}
	return items, revs, nil
}

// applyRevision 은 수정본 승인 — 라이브 버전에 반영 후 수정본을 지운다.
// FOR UPDATE + status 조건으로 동시 승인을 한 번만 반영한다.
func applyRevision(db *sql.DB, id int64, reviewer Profile) error {
	tx, err := db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()
	rows, err := tx.Query(
		`SELECT `+revCols+` FROM project_revisions WHERE project_id = ? AND status = 'pending' FOR UPDATE`, id)
	if err != nil {
		return err
	}
	revs, err := scanRevisions(rows)
	if err != nil {
		return err
	}
	rev, ok := revs[id]
	if !ok {
		return errNotFound
	}
	_, err = tx.Exec(
		`UPDATE projects SET title = ?, description = ?, body_md = ?, thumbnail_key = ?,
		 banner_key = ?, redirect_url = ?, category = ?, reviewer_student_id = ?,
		 reviewer_name = ?, reviewed_at = ? WHERE id = ?`,
		rev.Title, rev.Description, rev.BodyMD, rev.ThumbnailKey, rev.BannerKey,
		rev.RedirectURL, rev.Category, reviewer.StudentID, reviewer.Name, time.Now().UTC(), id)
	if err != nil {
		return err
	}
	if _, err := tx.Exec(`DELETE FROM project_revisions WHERE project_id = ?`, id); err != nil {
		return err
	}
	return tx.Commit()
}

// rejectRevision — 라이브 버전은 유지, 수정본만 반려 표시(작성자가 사유를 본다).
func rejectRevision(db *sql.DB, id int64, reason string) error {
	res, err := db.Exec(
		`UPDATE project_revisions SET status = 'rejected', reject_reason = ?
		 WHERE project_id = ? AND status = 'pending'`, reason, id)
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
