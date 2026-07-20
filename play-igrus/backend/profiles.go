package main

import (
	"context"
	"database/sql"
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
)

// 공개 프로필 (닉네임/자기소개/링크) — igrus users 테이블에서 직접 읽는다.
// play 와 igrus 는 같은 RDS 인스턴스라, HTTP 왕복 대신 크로스 스키마 조회 한 번으로
// 끝낸다 (play DB 유저에 igrus_web.users 의 필요한 컬럼만 SELECT 권한을 부여해 둔다).
// 캐시하지 않는다 — 요청 하나 안에서만 살아서 프로필 수정이 곧바로 반영된다.

type profileLink struct {
	Label string `json:"label"`
	URL   string `json:"url"`
}

type publicProfile struct {
	DisplayName  string        `json:"displayName"`
	Introduction string        `json:"introduction"`
	Links        []profileLink `json:"links"`
}

// lookupProfiles 는 학번들의 공개 프로필을 한 번의 쿼리로 읽는다.
// 닉네임 폴백(닉네임 없으면 이름)은 여기서 SQL 로 적용한다 — 실명은 절대 노출하지 않고,
// 폴백 결과(DisplayName)만 응답에 싣는다. 탈퇴 회원(users_deleted)은 제외.
func (s *server) lookupProfiles(ctx context.Context, studentIDs []string) (map[string]publicProfile, error) {
	out := map[string]publicProfile{}
	if len(studentIDs) == 0 {
		return out, nil
	}
	// igrusDB 비어 있으면 현재 스키마의 users (테스트용) — prod 는 "igrus_web".
	usersTable := "users"
	if s.igrusDB != "" {
		usersTable = s.igrusDB + ".users"
	}
	q := `SELECT users_student_id,
	         COALESCE(NULLIF(users_nickname, ''), users_name),
	         COALESCE(users_introduction, ''), users_links
	      FROM ` + usersTable + `
	      WHERE users_deleted = 0 AND users_student_id IN (?` + strings.Repeat(",?", len(studentIDs)-1) + `)`
	args := make([]any, len(studentIDs))
	for i, id := range studentIDs {
		args[i] = id
	}
	rows, err := s.db.QueryContext(ctx, q, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	for rows.Next() {
		var sid, display, intro string
		var linksJSON sql.NullString
		if err := rows.Scan(&sid, &display, &intro, &linksJSON); err != nil {
			return nil, err
		}
		p := publicProfile{DisplayName: display, Introduction: intro, Links: []profileLink{}}
		if linksJSON.Valid && linksJSON.String != "" {
			// 깨진 JSON 이어도 링크만 비운다 — 이름/소개는 살린다
			_ = json.Unmarshal([]byte(linksJSON.String), &p.Links)
			if p.Links == nil {
				p.Links = []profileLink{}
			}
		}
		out[sid] = p
	}
	return out, rows.Err()
}

// ── 내 프로필 사진 ────────────────────────────────────────────────────
// 닉네임/자기소개/링크는 igrus 백엔드가 주인이고, 사진만 여기서 관리한다.

// GET /api/profile/avatar
func (s *server) getMyAvatar(w http.ResponseWriter, r *http.Request, me Profile) {
	key, err := getAvatarKey(s.db, me.StudentID)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "프로필 사진을 불러올 수 없습니다")
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"avatarUrl": imageURL(key)})
}

// PUT /api/profile/avatar — multipart, 필드명 image.
// 이전 이미지는 S3 에 남긴다 (다른 곳에서 참조 중일 수 있고, 용량이 미미하다).
func (s *server) putMyAvatar(w http.ResponseWriter, r *http.Request, me Profile) {
	key, err := s.saveImage(r, "image")
	switch {
	case err == errTooLarge:
		writeErr(w, http.StatusBadRequest, "이미지는 4MB 이하여야 합니다")
		return
	case err == errNotImage:
		writeErr(w, http.StatusBadRequest, "PNG, JPG, WebP 이미지만 올릴 수 있습니다")
		return
	case err != nil:
		writeErr(w, http.StatusBadRequest, "이미지를 읽을 수 없습니다")
		return
	case key == "": // saveImage 는 파일이 없으면 빈 키를 준다 — 여기선 필수다
		writeErr(w, http.StatusBadRequest, "이미지를 선택해 주세요")
		return
	}
	if err := setAvatarKey(s.db, me.StudentID, key); err != nil {
		writeErr(w, http.StatusInternalServerError, "프로필 사진을 저장할 수 없습니다")
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"avatarUrl": imageURL(key)})
}

// DELETE /api/profile/avatar
func (s *server) deleteMyAvatar(w http.ResponseWriter, r *http.Request, me Profile) {
	if err := deleteAvatarKey(s.db, me.StudentID); err != nil {
		writeErr(w, http.StatusInternalServerError, "프로필 사진을 지울 수 없습니다")
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// GET /api/projects/{id}/author — 작성자 프로필 다이얼로그용.
// 학번은 절대 응답에 싣지 않는다 (프로젝트 id 로만 조회).
func (s *server) getAuthor(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
	if err != nil {
		writeErr(w, http.StatusBadRequest, "잘못된 id 입니다")
		return
	}
	p, err := getProject(s.db, id)
	if err == errNotFound || (err == nil && (p.Status != "approved" || p.Hidden)) {
		writeErr(w, http.StatusNotFound, "작품을 찾을 수 없습니다")
		return
	}
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "작성자를 불러올 수 없습니다")
		return
	}

	// 작성자가 탈퇴/삭제(또는 학번 변경)면 부산물도 지워진 것처럼 404.
	profs, err := s.lookupProfiles(r.Context(), []string{p.StudentID})
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "작성자를 불러올 수 없습니다")
		return
	}
	prof, ok := profs[p.StudentID]
	if !ok {
		writeErr(w, http.StatusNotFound, "작품을 찾을 수 없습니다")
		return
	}

	out := struct {
		publicProfile
		AvatarURL string        `json:"avatarUrl,omitempty"`
		Projects  []projectView `json:"projects"`
	}{
		publicProfile: publicProfile{
			DisplayName:  prof.DisplayName,
			Introduction: prof.Introduction,
			Links:        prof.Links,
		},
		Projects: []projectView{},
	}
	if key, err := getAvatarKey(s.db, p.StudentID); err == nil {
		out.AvatarURL = imageURL(key)
	}
	// 이 작성자의 다른 승인작 — 승인작만 공개 (심사중/반려/작성자 비공개 제외)
	if mine, err := listByStudent(s.db, p.StudentID); err == nil {
		for _, it := range mine {
			if it.Status == "approved" && !it.Hidden {
				out.Projects = append(out.Projects, listView(it))
			}
		}
	}
	writeJSON(w, http.StatusOK, out)
}
