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
	  version             INT           NOT NULL DEFAULT 0,
	  hidden              TINYINT(1)    NOT NULL DEFAULT 0,
	  INDEX idx_projects_status_score (status, score DESC),
	  INDEX idx_projects_student (student_id)
	) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`,
	`CREATE TABLE IF NOT EXISTS project_clicks_daily (
	  project_id BIGINT NOT NULL,
	  click_date DATE   NOT NULL,
	  clicks     BIGINT NOT NULL DEFAULT 0,
	  PRIMARY KEY (project_id, click_date)
	) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`,
	// 제출 1건 = 버전 1개 (v1 = 최초 제출). append-only — 승인/반려 이력이 그대로 남는다.
	// projects 행은 라이브 스냅샷(승인작) 또는 최신 제출 내용(미승인작), version 은 라이브 버전 번호(0=미승인).
	`CREATE TABLE IF NOT EXISTS project_versions (
	  project_id          BIGINT        NOT NULL,
	  version             INT           NOT NULL,
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
	  PRIMARY KEY (project_id, version),
	  INDEX idx_versions_status (status)
	) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`,
	// 프로필 사진 — 닉네임/자기소개/링크는 igrus users 테이블이지만, 이미지 저장은
	// 여기(S3 play/ prefix)에 이미 있으므로 키만 학번에 매달아 둔다.
	`CREATE TABLE IF NOT EXISTS profile_avatars (
	  student_id VARCHAR(8)   NOT NULL PRIMARY KEY,
	  image_key  VARCHAR(255) NOT NULL,
	  updated_at DATETIME(3)  NOT NULL
	) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`,
}

// getAvatarKey 는 없으면 "" 를 준다 (미설정은 에러가 아니다).
func getAvatarKey(db *sql.DB, studentID string) (string, error) {
	var key string
	err := db.QueryRow(`SELECT image_key FROM profile_avatars WHERE student_id = ?`, studentID).Scan(&key)
	if err == sql.ErrNoRows {
		return "", nil
	}
	return key, err
}

func setAvatarKey(db *sql.DB, studentID, key string) error {
	_, err := db.Exec(
		`INSERT INTO profile_avatars (student_id, image_key, updated_at) VALUES (?, ?, ?)
		 ON DUPLICATE KEY UPDATE image_key = VALUES(image_key), updated_at = VALUES(updated_at)`,
		studentID, key, time.Now().UTC())
	return err
}

func deleteAvatarKey(db *sql.DB, studentID string) error {
	_, err := db.Exec(`DELETE FROM profile_avatars WHERE student_id = ?`, studentID)
	return err
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
	Version           int  // 라이브 버전 번호 (0 = 아직 승인된 버전 없음)
	Hidden            bool // 작성자가 공개를 내린 상태 — 심사 상태(status)와 별개 축
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
	if err := migrate(db); err != nil {
		return nil, err
	}
	return db, nil
}

// migrate 는 구버전 스키마에서 넘어오는 idempotent 이관 — 매 부팅마다 실행해도 안전하다.
func migrate(db *sql.DB) error {
	// ① 기존 projects 테이블에 없는 컬럼 추가 (MySQL 은 ADD COLUMN IF NOT EXISTS 미지원)
	var n int
	for col, ddl := range map[string]string{
		"version": `ALTER TABLE projects ADD COLUMN version INT NOT NULL DEFAULT 0`,
		"hidden":  `ALTER TABLE projects ADD COLUMN hidden TINYINT(1) NOT NULL DEFAULT 0`,
	} {
		db.QueryRow(`SELECT COUNT(*) FROM information_schema.columns
			WHERE table_schema = DATABASE() AND table_name = 'projects' AND column_name = ?`, col).Scan(&n)
		if n == 0 {
			if _, err := db.Exec(ddl); err != nil {
				return err
			}
		}
	}
	// ② 버전 이력이 없는 기존 프로젝트는 현재 내용을 v1 로 백필
	if _, err := db.Exec(`INSERT INTO project_versions
		(project_id, version, title, description, body_md, thumbnail_key, banner_key,
		 redirect_url, category, status, reject_reason, reviewer_student_id, reviewer_name,
		 reviewed_at, created_at)
		SELECT id, 1, title, description, body_md, thumbnail_key, banner_key,
		 redirect_url, category, status, reject_reason, reviewer_student_id, reviewer_name,
		 reviewed_at, created_at
		FROM projects p
		WHERE NOT EXISTS (SELECT 1 FROM project_versions v WHERE v.project_id = p.id)`); err != nil {
		return err
	}
	if _, err := db.Exec(
		`UPDATE projects SET version = 1 WHERE status = 'approved' AND version = 0`); err != nil {
		return err
	}
	// ③ 구 단일 수정본 테이블(project_revisions)이 남아 있으면 다음 버전으로 이관 후 제거
	db.QueryRow(`SELECT COUNT(*) FROM information_schema.tables
		WHERE table_schema = DATABASE() AND table_name = 'project_revisions'`).Scan(&n)
	if n > 0 {
		if _, err := db.Exec(`INSERT INTO project_versions
			(project_id, version, title, description, body_md, thumbnail_key, banner_key,
			 redirect_url, category, status, reject_reason, created_at)
			SELECT r.project_id,
			 (SELECT COALESCE(MAX(v.version), 0) + 1 FROM project_versions v WHERE v.project_id = r.project_id),
			 r.title, r.description, r.body_md, r.thumbnail_key, r.banner_key,
			 r.redirect_url, r.category, r.status, r.reject_reason, r.created_at
			FROM project_revisions r`); err != nil {
			return err
		}
		if _, err := db.Exec(`DROP TABLE project_revisions`); err != nil {
			return err
		}
	}
	return nil
}

const selectCols = `id, student_id, author_name, department, title, description, body_md,
	thumbnail_key, banner_key, redirect_url, category, status, reject_reason,
	reviewer_student_id, reviewer_name, reviewed_at, created_at, total_clicks, score, version, hidden`

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
			&reviewed, &p.CreatedAt, &p.TotalClicks, &p.Score, &p.Version, &p.Hidden)
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

// insertProject 는 프로젝트 행 + v1 버전을 함께 만든다.
func insertProject(db *sql.DB, p row) (int64, error) {
	tx, err := db.Begin()
	if err != nil {
		return 0, err
	}
	defer tx.Rollback()
	now := time.Now().UTC()
	res, err := tx.Exec(
		`INSERT INTO projects
		 (student_id, author_name, department, title, description, body_md,
		  thumbnail_key, banner_key, redirect_url, category, created_at)
		 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
		p.StudentID, p.AuthorName, p.Department, p.Title, p.Description, p.BodyMD,
		p.ThumbnailKey, p.BannerKey, p.RedirectURL, p.Category, now)
	if err != nil {
		return 0, err
	}
	id, err := res.LastInsertId()
	if err != nil {
		return 0, err
	}
	if _, err := tx.Exec(
		`INSERT INTO project_versions
		 (project_id, version, title, description, body_md, thumbnail_key, banner_key,
		  redirect_url, category, created_at)
		 VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?)`,
		id, p.Title, p.Description, p.BodyMD, p.ThumbnailKey, p.BannerKey,
		p.RedirectURL, p.Category, now); err != nil {
		return 0, err
	}
	return id, tx.Commit()
}

// listApprovedProjects — 승인·공개 작품 중 작성자가 살아있는 것만 반환한다.
// igrus users 와 INNER JOIN 이라, 탈퇴(soft/hard)·학번 변경으로 작성자가 사라진 작품은
// 애초에 쿼리에서 안 나온다 (행은 지우지 않고 노출만 차단 — 복구되면 다시 보인다).
// 작성자 표시 이름(닉네임 폴백)도 같은 조인에서 채운다. sort 는 핸들러에서 검증됨.
func listApprovedProjects(db *sql.DB, igrusDB, category, sort string) ([]row, error) {
	usersTable := "users"
	if igrusDB != "" {
		usersTable = igrusDB + ".users"
	}
	q := `SELECT ` + prefixed(selectCols, "p.") + `,
	         COALESCE(NULLIF(u.users_nickname, ''), u.users_name)
	      FROM projects p
	      JOIN ` + usersTable + ` u
	        ON u.users_student_id = p.student_id AND u.users_deleted = 0
	      WHERE p.status = 'approved' AND p.hidden = 0`
	args := []any{}
	if category != "" {
		q += ` AND p.category = ?`
		args = append(args, category)
	}
	if sort == "recent" {
		q += ` ORDER BY p.created_at DESC`
	} else {
		q += ` ORDER BY p.score DESC, p.created_at DESC`
	}
	rows, err := db.Query(q, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []row{}
	for rows.Next() {
		var p row
		var reviewed sql.NullTime
		var displayName string
		if err := rows.Scan(&p.ID, &p.StudentID, &p.AuthorName, &p.Department, &p.Title,
			&p.Description, &p.BodyMD, &p.ThumbnailKey, &p.BannerKey, &p.RedirectURL,
			&p.Category, &p.Status, &p.RejectReason, &p.ReviewerStudentID, &p.ReviewerName,
			&reviewed, &p.CreatedAt, &p.TotalClicks, &p.Score, &p.Version, &p.Hidden,
			&displayName); err != nil {
			return nil, err
		}
		if reviewed.Valid {
			t := reviewed.Time
			p.ReviewedAt = &t
		}
		p.AuthorName = displayName // 스냅샷 대신 현재 표시 이름(닉네임 폴백)
		out = append(out, p)
	}
	return out, rows.Err()
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

// ── 버전 (project_versions) ────────────────────────────────────────────

type version struct {
	ProjectID    int64
	Version      int
	Title        string
	Description  string
	BodyMD       string
	ThumbnailKey string
	BannerKey    string
	RedirectURL  string
	Category     string
	Status       string // pending | approved | rejected
	RejectReason string
	ReviewerName string
	ReviewedAt   *time.Time
	CreatedAt    time.Time
}

const verCols = `project_id, version, title, description, body_md, thumbnail_key, banner_key,
	redirect_url, category, status, reject_reason, reviewer_name, reviewed_at, created_at`

func scanVersions(rows *sql.Rows) ([]version, error) {
	defer rows.Close()
	out := []version{}
	for rows.Next() {
		var v version
		var reviewed sql.NullTime
		err := rows.Scan(&v.ProjectID, &v.Version, &v.Title, &v.Description, &v.BodyMD,
			&v.ThumbnailKey, &v.BannerKey, &v.RedirectURL, &v.Category, &v.Status,
			&v.RejectReason, &v.ReviewerName, &reviewed, &v.CreatedAt)
		if err != nil {
			return nil, err
		}
		if reviewed.Valid {
			t := reviewed.Time
			v.ReviewedAt = &t
		}
		out = append(out, v)
	}
	return out, rows.Err()
}

// latestVersion 은 가장 최근 제출 버전 (없으면 errNotFound — 정상 데이터엔 항상 v1 이 있다).
func latestVersion(db *sql.DB, projectID int64) (version, error) {
	rows, err := db.Query(
		`SELECT `+verCols+` FROM project_versions WHERE project_id = ?
		 ORDER BY version DESC LIMIT 1`, projectID)
	if err != nil {
		return version{}, err
	}
	items, err := scanVersions(rows)
	if err != nil {
		return version{}, err
	}
	if len(items) == 0 {
		return version{}, errNotFound
	}
	return items[0], nil
}

// updatePendingVersion 은 아직 심사 전인 버전의 내용 교체 — 버전 번호는 그대로.
func updatePendingVersion(db *sql.DB, v version) error {
	res, err := db.Exec(
		`UPDATE project_versions SET title = ?, description = ?, body_md = ?, thumbnail_key = ?,
		 banner_key = ?, redirect_url = ?, category = ?, created_at = ?
		 WHERE project_id = ? AND version = ? AND status = 'pending'`,
		v.Title, v.Description, v.BodyMD, v.ThumbnailKey, v.BannerKey, v.RedirectURL,
		v.Category, time.Now().UTC(), v.ProjectID, v.Version)
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

func insertVersion(db *sql.DB, v version) error {
	_, err := db.Exec(
		`INSERT INTO project_versions
		 (project_id, version, title, description, body_md, thumbnail_key, banner_key,
		  redirect_url, category, created_at)
		 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
		v.ProjectID, v.Version, v.Title, v.Description, v.BodyMD, v.ThumbnailKey,
		v.BannerKey, v.RedirectURL, v.Category, time.Now().UTC())
	return err
}

// syncDraftProject 는 아직 한 번도 승인 안 된 프로젝트의 행을 최신 제출 내용으로 동기화한다.
// (승인작의 라이브 행은 reviewVersion 의 승인 경로에서만 바뀐다)
func syncDraftProject(db *sql.DB, id int64, p row) error {
	_, err := db.Exec(
		`UPDATE projects SET title = ?, description = ?, body_md = ?, thumbnail_key = ?,
		 banner_key = ?, redirect_url = ?, category = ?, status = 'pending', reject_reason = ''
		 WHERE id = ? AND version = 0`,
		p.Title, p.Description, p.BodyMD, p.ThumbnailKey, p.BannerKey, p.RedirectURL, p.Category, id)
	return err
}

// reviewVersion 은 대기 중인 버전을 승인/반려한다. 이력 행은 지우지 않는다.
// FOR UPDATE + status 조건으로 두 운영진이 동시에 눌러도 한 번만 반영된다.
// 승인: 라이브(projects)에 반영(클릭수·score 유지). 반려: 라이브 유지(미승인작은 프로젝트도 반려).
func reviewVersion(db *sql.DB, projectID int64, status, reason string, reviewer Profile) error {
	tx, err := db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()
	rows, err := tx.Query(
		`SELECT `+verCols+` FROM project_versions
		 WHERE project_id = ? AND status = 'pending' FOR UPDATE`, projectID)
	if err != nil {
		return err
	}
	items, err := scanVersions(rows)
	if err != nil {
		return err
	}
	if len(items) == 0 {
		return errNotFound
	}
	v := items[0]
	now := time.Now().UTC()
	if _, err := tx.Exec(
		`UPDATE project_versions SET status = ?, reject_reason = ?, reviewer_student_id = ?,
		 reviewer_name = ?, reviewed_at = ? WHERE project_id = ? AND version = ?`,
		status, reason, reviewer.StudentID, reviewer.Name, now, projectID, v.Version); err != nil {
		return err
	}
	if status == "approved" {
		_, err = tx.Exec(
			`UPDATE projects SET title = ?, description = ?, body_md = ?, thumbnail_key = ?,
			 banner_key = ?, redirect_url = ?, category = ?, status = 'approved', version = ?,
			 reject_reason = '', reviewer_student_id = ?, reviewer_name = ?, reviewed_at = ?
			 WHERE id = ?`,
			v.Title, v.Description, v.BodyMD, v.ThumbnailKey, v.BannerKey, v.RedirectURL,
			v.Category, v.Version, reviewer.StudentID, reviewer.Name, now, projectID)
	} else {
		// 라이브 버전이 없는(미승인) 프로젝트만 프로젝트 자체를 반려 상태로
		_, err = tx.Exec(
			`UPDATE projects SET status = 'rejected', reject_reason = ?, reviewer_student_id = ?,
			 reviewer_name = ?, reviewed_at = ? WHERE id = ? AND version = 0`,
			reason, reviewer.StudentID, reviewer.Name, now, projectID)
	}
	if err != nil {
		return err
	}
	return tx.Commit()
}

// versionItem 은 검수 목록의 한 건 — 버전 내용 + 소속 프로젝트(작성자 등).
type versionItem struct {
	p row
	v version
}

// listVersionsByStatus 는 검수 탭용 — 해당 상태의 모든 버전 이력 (v1, v2, … 전부 남는다).
func listVersionsByStatus(db *sql.DB, status string) ([]versionItem, error) {
	rows, err := db.Query(
		`SELECT `+prefixed(selectCols, "p.")+`, `+prefixed(verCols, "v.")+`
		 FROM project_versions v JOIN projects p ON p.id = v.project_id
		 WHERE v.status = ? ORDER BY v.created_at DESC`, status)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []versionItem{}
	for rows.Next() {
		var p row
		var v version
		var pReviewed, vReviewed sql.NullTime
		err := rows.Scan(&p.ID, &p.StudentID, &p.AuthorName, &p.Department, &p.Title,
			&p.Description, &p.BodyMD, &p.ThumbnailKey, &p.BannerKey, &p.RedirectURL,
			&p.Category, &p.Status, &p.RejectReason, &p.ReviewerStudentID, &p.ReviewerName,
			&pReviewed, &p.CreatedAt, &p.TotalClicks, &p.Score, &p.Version, &p.Hidden,
			&v.ProjectID, &v.Version, &v.Title, &v.Description, &v.BodyMD,
			&v.ThumbnailKey, &v.BannerKey, &v.RedirectURL, &v.Category, &v.Status,
			&v.RejectReason, &v.ReviewerName, &vReviewed, &v.CreatedAt)
		if err != nil {
			return nil, err
		}
		if vReviewed.Valid {
			t := vReviewed.Time
			v.ReviewedAt = &t
		}
		out = append(out, versionItem{p: p, v: v})
	}
	return out, rows.Err()
}

// prefixed 는 "a, b" → "p.a, p.b" — JOIN 쿼리에서 컬럼 목록 재사용용.
func prefixed(cols, prefix string) string {
	parts := strings.Split(cols, ",")
	for i, c := range parts {
		parts[i] = prefix + strings.TrimSpace(c)
	}
	return strings.Join(parts, ", ")
}

// listLatestVersionsByStudent 는 내 작품 화면용 — 프로젝트별 최신 버전.
func listLatestVersionsByStudent(db *sql.DB, studentID string) (map[int64]version, error) {
	rows, err := db.Query(
		`SELECT `+verCols+` FROM project_versions
		 WHERE project_id IN (SELECT id FROM projects WHERE student_id = ?)`, studentID)
	if err != nil {
		return nil, err
	}
	items, err := scanVersions(rows)
	if err != nil {
		return nil, err
	}
	latest := map[int64]version{}
	for _, v := range items {
		if v.Version > latest[v.ProjectID].Version {
			latest[v.ProjectID] = v
		}
	}
	return latest, nil
}

// setProjectHidden 은 공개/비공개 토글 — 소유·상태 검증은 핸들러가 한다.
func setProjectHidden(db *sql.DB, id int64, hidden bool) error {
	_, err := db.Exec(`UPDATE projects SET hidden = ? WHERE id = ?`, hidden, id)
	return err
}

// addClick 은 승인된 작품의 클릭을 총합 + 일별(KST 날짜) 버킷에 기록한다.
func addClick(db *sql.DB, id int64, date string) error {
	res, err := db.Exec(
		`UPDATE projects SET total_clicks = total_clicks + 1
		 WHERE id = ? AND status = 'approved' AND hidden = 0`, id)
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
