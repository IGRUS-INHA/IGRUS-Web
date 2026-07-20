package main

import (
	"database/sql"
	"log"
	"net/http"
	"strings"
)

// project.igrus.co.kr 기존 작품 이관용 임시 엔드포인트.
// igrus 본 DB(users 테이블) DSN 이 설정된 경우에만 열린다 — IGRUS_DSN env
// 또는 play 시크릿의 igrus_dsn 키. 이관이 끝나면 제거해 엔드포인트를 닫는다.
// DSN 이 잘못돼도 서버는 죽이지 않는다 — 이관 기능만 스킵.
func registerMigrate(mux *http.ServeMux, s *server, dsn string) {
	if dsn == "" {
		return
	}
	idb, err := sql.Open("mysql", dsn)
	if err == nil {
		err = idb.Ping()
	}
	if err != nil {
		log.Printf("igrus DB 연결 실패 — 이관 엔드포인트 비활성: %v", err)
		return
	}
	mux.HandleFunc("POST /api/admin/migrate", s.auth.requireStaff(s.migrateProject(idb)))
	log.Print("⚠ 이관 엔드포인트 열림: POST /api/admin/migrate (끝나면 igrus DSN 제거)")
}

// resolveMigrateUser 는 이름(+선택적 학번)으로 igrus users 에서 유저를 찾는다.
// 학번 없이 이름이 여러 명이면 후보 학번 목록과 함께 실패를 알린다.
func resolveMigrateUser(igrus *sql.DB, name, studentID string) (string, []string, error) {
	if studentID != "" {
		var n int
		err := igrus.QueryRow(
			`SELECT COUNT(*) FROM users
			 WHERE users_student_id = ? AND users_name = ? AND users_deleted = 0`,
			studentID, name).Scan(&n)
		if err != nil {
			return "", nil, err
		}
		if n == 0 {
			return "", nil, errNotFound
		}
		return studentID, nil, nil
	}
	rows, err := igrus.Query(
		`SELECT users_student_id FROM users WHERE users_name = ? AND users_deleted = 0`, name)
	if err != nil {
		return "", nil, err
	}
	defer rows.Close()
	ids := []string{}
	for rows.Next() {
		var id string
		if err := rows.Scan(&id); err != nil {
			return "", nil, err
		}
		ids = append(ids, id)
	}
	if err := rows.Err(); err != nil {
		return "", nil, err
	}
	switch len(ids) {
	case 0:
		return "", nil, errNotFound
	case 1:
		return ids[0], nil, nil
	default:
		return "", ids, nil // 동명이인 — 호출측이 학번을 지정해야 한다
	}
}

// POST /api/admin/migrate (multipart)
// 필드: name(실명), nickname, department, studentId(선택 — 동명이인일 때만 필수),
//       title, description, body, url, category, thumbnail(파일)
// 동작: igrus users 닉네임 갱신 → 썸네일 S3 업로드 → 프로젝트 등록 + 즉시 승인.
// 같은 학번+제목이 이미 있으면 프로젝트 등록은 건너뛴다 (재실행 안전).
func (s *server) migrateProject(igrus *sql.DB) func(http.ResponseWriter, *http.Request, Profile) {
	return func(w http.ResponseWriter, r *http.Request, admin Profile) {
		fields, ok := parseProjectForm(w, r)
		if !ok {
			return
		}
		name := strings.TrimSpace(r.FormValue("name"))
		nickname := strings.TrimSpace(r.FormValue("nickname"))
		if name == "" || nickname == "" {
			writeErr(w, http.StatusBadRequest, "name 과 nickname 이 필요합니다")
			return
		}

		studentID, candidates, err := resolveMigrateUser(igrus, name, strings.TrimSpace(r.FormValue("studentId")))
		if err == errNotFound {
			writeErr(w, http.StatusNotFound, "가입된 유저를 찾을 수 없습니다: "+name)
			return
		}
		if err != nil {
			writeErr(w, http.StatusInternalServerError, "유저 조회에 실패했습니다: "+err.Error())
			return
		}
		if candidates != nil {
			writeJSON(w, http.StatusConflict, map[string]any{
				"error":      "동명이인이 있습니다 — studentId 를 지정해주세요: " + name,
				"candidates": candidates,
			})
			return
		}

		if _, err := igrus.Exec(
			`UPDATE users SET users_nickname = ? WHERE users_student_id = ?`,
			nickname, studentID); err != nil {
			writeErr(w, http.StatusInternalServerError, "닉네임 변경에 실패했습니다: "+err.Error())
			return
		}

		// 재실행 안전: 같은 학번+제목이면 등록 생략 (닉네임 갱신은 이미 반영됨)
		var existing int64
		err = s.db.QueryRow(
			`SELECT id FROM projects WHERE student_id = ? AND title = ? LIMIT 1`,
			studentID, fields.Title).Scan(&existing)
		if err == nil {
			writeJSON(w, http.StatusOK, map[string]any{"id": existing, "studentId": studentID, "skipped": true})
			return
		}
		if err != sql.ErrNoRows {
			writeErr(w, http.StatusInternalServerError, "중복 확인에 실패했습니다")
			return
		}

		thumb, err := s.saveImage(r, "thumbnail")
		if err != nil {
			writeImageErr(w, err)
			return
		}
		if thumb == "" {
			writeErr(w, http.StatusBadRequest, "썸네일을 등록해주세요")
			return
		}

		fields.StudentID = studentID
		fields.AuthorName = name
		fields.Department = strings.TrimSpace(r.FormValue("department"))
		fields.ThumbnailKey = thumb
		fields.BannerKey = thumb // 배너도 썸네일과 같은 이미지 사용 (이관 스펙)
		id, err := insertProject(s.db, fields)
		if err != nil {
			writeErr(w, http.StatusInternalServerError, "저장에 실패했습니다")
			return
		}
		if err := reviewVersion(s.db, id, "approved", "", admin); err != nil {
			writeErr(w, http.StatusInternalServerError, "승인 처리에 실패했습니다")
			return
		}
		writeJSON(w, http.StatusCreated, map[string]any{"id": id, "studentId": studentID, "status": "approved"})
	}
}
