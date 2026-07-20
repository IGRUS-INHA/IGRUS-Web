package main

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
	"unicode/utf8"
)

const (
	maxTitleLen = 100
	maxDescLen  = 50 // 설명 최대 50자 (스펙)
	maxBodyLen  = 20000
	maxURLLen   = 2048
)

// 분류는 서버에서 고정 — frontend/src/components/category.ts 의 CATEGORIES 와 동기화
var allowedCategories = map[string]bool{"게임": true, "앱": true}

type server struct {
	db     *sql.DB
	auth   *authenticator
	images imageStore
}

func writeJSON(w http.ResponseWriter, code int, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(code)
	json.NewEncoder(w).Encode(v)
}

func writeErr(w http.ResponseWriter, code int, msg string) {
	writeJSON(w, code, map[string]string{"error": msg})
}

// validateURL 은 http/https 만 통과시킨다.
// 프론트가 이 값을 <a href> 에 넣기 때문에 javascript: 나 data: 를 허용하면
// 승인된 작품 카드가 그대로 XSS 가 된다.
func validateURL(raw string) (string, bool) {
	raw = strings.TrimSpace(raw)
	if len(raw) > maxURLLen {
		return "", false
	}
	u, err := url.Parse(raw)
	if err != nil {
		return "", false
	}
	if u.Scheme != "http" && u.Scheme != "https" {
		return "", false
	}
	if u.Host == "" {
		return "", false
	}
	return raw, true
}

// projectView 는 응답 JSON. 목록/상세/내 제출/관리자에 따라 채우는 필드가 다르다.
// 클릭수는 어떤 응답에도 싣지 않는다 (서버단 제공 절대 금지 — 랭킹 정렬로만 쓰인다).
type projectView struct {
	ID           int64        `json:"id"`
	Title        string       `json:"title"`
	Description  string       `json:"description"`
	Body         string       `json:"body,omitempty"`
	Category     string       `json:"category"`
	Author       string       `json:"author,omitempty"`
	ThumbnailURL string       `json:"thumbnailUrl,omitempty"`
	BannerURL    string       `json:"bannerUrl,omitempty"`
	RedirectURL  string       `json:"redirectUrl,omitempty"`
	Status       string       `json:"status,omitempty"`
	RejectReason string       `json:"rejectReason,omitempty"`
	ReviewerName string       `json:"reviewerName,omitempty"`
	CreatedAt    time.Time    `json:"createdAt"`
	ReviewedAt   *time.Time   `json:"reviewedAt,omitempty"`
	Version      int          `json:"version,omitempty"` // 라이브 버전(내 작품) 또는 해당 버전 번호(검수)
	Update       *projectView `json:"update,omitempty"`  // 심사 대기/반려된 최신 제출 버전
}

func imageURL(key string) string {
	if key == "" {
		return ""
	}
	return "/images/" + key
}

// listView: 메인 카드용 — 본문/리다이렉트 제외 (payload 절약). 클릭수 비공개.
// Author 는 공개 표시 이름 — 공개 응답에서는 resolveAuthors 로 닉네임이 반영된다.
// 학번은 어떤 형태로도 노출하지 않는다.
func listView(p row) projectView {
	return projectView{
		ID:           p.ID,
		Title:        p.Title,
		Description:  p.Description,
		Category:     p.Category,
		Author:       p.AuthorName,
		ThumbnailURL: imageURL(p.ThumbnailKey),
		CreatedAt:    p.CreatedAt,
	}
}

// detailView: 다이얼로그용 — 배너/본문/리다이렉트 포함
func detailView(p row) projectView {
	v := listView(p)
	v.Body = p.BodyMD
	v.BannerURL = imageURL(p.BannerKey)
	v.RedirectURL = p.RedirectURL
	return v
}

// mineView: 상태·반려사유·라이브 버전 포함
func mineView(p row) projectView {
	v := detailView(p)
	v.Status = p.Status
	v.RejectReason = p.RejectReason
	v.ReviewedAt = p.ReviewedAt
	v.Version = p.Version
	return v
}

// versionView: 버전 이력 한 건 — 검수 목록과 내 작품의 update 필드에 실린다.
func versionView(p row, v version) projectView {
	return projectView{
		ID:           p.ID,
		Version:      v.Version,
		Title:        v.Title,
		Description:  v.Description,
		Body:         v.BodyMD,
		Category:     v.Category,
		Author:       p.AuthorName,
		ThumbnailURL: imageURL(v.ThumbnailKey),
		BannerURL:    imageURL(v.BannerKey),
		RedirectURL:  v.RedirectURL,
		Status:       v.Status,
		RejectReason: v.RejectReason,
		ReviewerName: v.ReviewerName,
		CreatedAt:    v.CreatedAt,
		ReviewedAt:   v.ReviewedAt,
	}
}

func views(items []row, f func(row) projectView) []projectView {
	out := make([]projectView, 0, len(items))
	for _, p := range items {
		out = append(out, f(p))
	}
	return out
}

// GET /api/projects?category=&sort=popular|recent — 승인작만, 정렬은 서버가 결정.
func (s *server) listApproved(w http.ResponseWriter, r *http.Request) {
	sort := r.URL.Query().Get("sort")
	if sort == "" {
		sort = "popular"
	}
	if sort != "popular" && sort != "recent" {
		writeErr(w, http.StatusBadRequest, "잘못된 sort 입니다")
		return
	}
	items, err := listApprovedProjects(s.db, strings.TrimSpace(r.URL.Query().Get("category")), sort)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "목록을 불러올 수 없습니다")
		return
	}
	s.resolveAuthors(r.Context(), items) // 닉네임 표시 (서버단 폴백)
	writeJSON(w, http.StatusOK, views(items, listView))
}

// GET /api/projects/{id} — 다이얼로그 상세. 승인작만 공개.
func (s *server) getDetail(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
	if err != nil {
		writeErr(w, http.StatusBadRequest, "잘못된 id 입니다")
		return
	}
	p, err := getProject(s.db, id)
	if err == errNotFound || (err == nil && p.Status != "approved") {
		writeErr(w, http.StatusNotFound, "작품을 찾을 수 없습니다")
		return
	}
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "작품을 불러올 수 없습니다")
		return
	}
	items := []row{p}
	s.resolveAuthors(r.Context(), items) // 닉네임 표시 (서버단 폴백)
	writeJSON(w, http.StatusOK, detailView(items[0]))
}

// POST /api/projects/{id}/click — "이동하기" 클릭 집계. 랭킹 배치의 입력이 된다.
func (s *server) click(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
	if err != nil {
		writeErr(w, http.StatusBadRequest, "잘못된 id 입니다")
		return
	}
	if err := addClick(s.db, id, kstTodayString()); err == errNotFound {
		writeErr(w, http.StatusNotFound, "작품을 찾을 수 없습니다")
		return
	} else if err != nil {
		writeErr(w, http.StatusInternalServerError, "처리에 실패했습니다")
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// parseProjectForm 은 제출/수정 공통의 multipart 필드 검증. 실패 시 에러 응답까지 쓴다.
func parseProjectForm(w http.ResponseWriter, r *http.Request) (row, bool) {
	if err := r.ParseMultipartForm(2 * maxImageBytes); err != nil {
		writeErr(w, http.StatusBadRequest, "폼을 읽을 수 없습니다")
		return row{}, false
	}

	title := strings.TrimSpace(r.FormValue("title"))
	desc := strings.TrimSpace(r.FormValue("description"))
	body := r.FormValue("body")
	category := strings.TrimSpace(r.FormValue("category"))

	if title == "" || utf8.RuneCountInString(title) > maxTitleLen {
		writeErr(w, http.StatusBadRequest, "제목은 1~100자여야 합니다")
		return row{}, false
	}
	if desc == "" || utf8.RuneCountInString(desc) > maxDescLen {
		writeErr(w, http.StatusBadRequest, "설명은 1~50자여야 합니다")
		return row{}, false
	}
	if utf8.RuneCountInString(body) > maxBodyLen {
		writeErr(w, http.StatusBadRequest, "본문이 너무 깁니다")
		return row{}, false
	}
	if !allowedCategories[category] {
		writeErr(w, http.StatusBadRequest, "허용되지 않는 분류입니다")
		return row{}, false
	}
	link, ok := validateURL(r.FormValue("url"))
	if !ok {
		writeErr(w, http.StatusBadRequest, "http:// 또는 https:// 로 시작하는 주소여야 합니다")
		return row{}, false
	}
	return row{Title: title, Description: desc, BodyMD: body, RedirectURL: link, Category: category}, true
}

// saveFormImages 는 썸네일/배너 업로드 처리. 실패 시 에러 응답까지 쓴다.
func (s *server) saveFormImages(w http.ResponseWriter, r *http.Request) (thumb, banner string, ok bool) {
	thumb, err := s.saveImage(r, "thumbnail")
	if err != nil {
		writeImageErr(w, err)
		return "", "", false
	}
	banner, err = s.saveImage(r, "banner")
	if err != nil {
		writeImageErr(w, err)
		return "", "", false
	}
	return thumb, banner, true
}

// POST /api/projects (multipart: title, description, body, url, category, thumbnail, banner?)
func (s *server) createProject(w http.ResponseWriter, r *http.Request, p Profile) {
	fields, ok := parseProjectForm(w, r)
	if !ok {
		return
	}
	thumb, banner, ok := s.saveFormImages(w, r)
	if !ok {
		return
	}
	if thumb == "" {
		writeErr(w, http.StatusBadRequest, "썸네일을 등록해주세요")
		return
	}

	fields.StudentID = p.StudentID
	fields.AuthorName = p.Name
	fields.Department = p.Department
	fields.ThumbnailKey = thumb
	fields.BannerKey = banner
	id, err := insertProject(s.db, fields)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "저장에 실패했습니다")
		return
	}
	writeJSON(w, http.StatusCreated, map[string]any{"id": id, "status": "pending"})
}

// PUT /api/projects/{id} — 본인 작품 수정. 승인작은 수정본으로 쌓여 재승인을 받아야
// 반영되고, 심사중/반려작은 바로 덮어쓰고 다시 심사 대기가 된다.
func (s *server) updateProject(w http.ResponseWriter, r *http.Request, p Profile) {
	id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
	if err != nil {
		writeErr(w, http.StatusBadRequest, "잘못된 id 입니다")
		return
	}
	proj, err := getProject(s.db, id)
	if err == errNotFound {
		writeErr(w, http.StatusNotFound, "작품을 찾을 수 없습니다")
		return
	}
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "작품을 불러올 수 없습니다")
		return
	}
	if proj.StudentID != p.StudentID {
		writeErr(w, http.StatusForbidden, "본인 작품만 수정할 수 있습니다")
		return
	}

	fields, ok := parseProjectForm(w, r)
	if !ok {
		return
	}
	thumb, banner, ok := s.saveFormImages(w, r)
	if !ok {
		return
	}
	latest, err := latestVersion(s.db, id)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "작품을 불러올 수 없습니다")
		return
	}
	// 새 파일이 없으면 사용자가 폼에서 보던 최신 버전의 이미지를 유지.
	// 배너 제거는 미지원 — 교체만 가능.
	if thumb == "" {
		thumb = latest.ThumbnailKey
	}
	if banner == "" {
		banner = latest.BannerKey
	}
	if thumb == "" {
		writeErr(w, http.StatusBadRequest, "썸네일을 등록해주세요")
		return
	}
	fields.ThumbnailKey = thumb
	fields.BannerKey = banner

	v := version{
		ProjectID:    id,
		Version:      latest.Version,
		Title:        fields.Title,
		Description:  fields.Description,
		BodyMD:       fields.BodyMD,
		ThumbnailKey: fields.ThumbnailKey,
		BannerKey:    fields.BannerKey,
		RedirectURL:  fields.RedirectURL,
		Category:     fields.Category,
	}
	if latest.Status == "pending" {
		// 심사 전 재수정 — 같은 버전 번호에 내용만 교체
		err = updatePendingVersion(s.db, v)
	} else {
		// 심사 끝난 버전 뒤에는 새 버전으로 쌓는다 (이력 보존)
		v.Version = latest.Version + 1
		err = insertVersion(s.db, v)
	}
	if err == nil && proj.Version == 0 {
		// 아직 라이브가 없는 프로젝트는 행 자체도 최신 내용으로 (목록/수정 폼이 이 행을 본다)
		err = syncDraftProject(s.db, id, fields)
	}
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "저장에 실패했습니다")
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"id": id, "status": "pending", "version": v.Version})
}

func writeImageErr(w http.ResponseWriter, err error) {
	switch err {
	case errNotImage:
		writeErr(w, http.StatusBadRequest, "이미지는 PNG/JPEG/WebP 파일이어야 합니다")
	case errTooLarge:
		writeErr(w, http.StatusBadRequest, "이미지는 4MB 이하여야 합니다")
	default:
		writeErr(w, http.StatusInternalServerError, "이미지 저장에 실패했습니다")
	}
}

// GET /api/projects/mine — 내 제출 현황. 라이브 버전 번호 + 심사 대기/반려된 최신 버전.
func (s *server) listMine(w http.ResponseWriter, r *http.Request, p Profile) {
	items, err := listByStudent(s.db, p.StudentID)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "목록을 불러올 수 없습니다")
		return
	}
	latest, err := listLatestVersionsByStudent(s.db, p.StudentID)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "목록을 불러올 수 없습니다")
		return
	}
	out := []projectView{}
	for _, it := range items {
		v := mineView(it)
		if lv, ok := latest[it.ID]; ok && lv.Status != "approved" {
			u := versionView(it, lv)
			v.Update = &u
		}
		out = append(out, v)
	}
	writeJSON(w, http.StatusOK, out)
}

// GET /api/admin/projects?status=pending — 검수 목록.
// 버전 이력 그대로: 승인/반려 탭에는 지난 버전들(v1, v2, …)이 전부 남는다.
func (s *server) listForReview(w http.ResponseWriter, r *http.Request, p Profile) {
	status := r.URL.Query().Get("status")
	if status == "" {
		status = "pending"
	}
	if status != "pending" && status != "approved" && status != "rejected" {
		writeErr(w, http.StatusBadRequest, "잘못된 status 입니다")
		return
	}
	items, err := listVersionsByStatus(s.db, status)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "목록을 불러올 수 없습니다")
		return
	}
	out := []projectView{}
	for _, it := range items {
		out = append(out, versionView(it.p, it.v))
	}
	writeJSON(w, http.StatusOK, out)
}

// POST /api/admin/projects/{id}/approve | /reject — 승인 운영진 정보 기록 (스펙)
func (s *server) reviewProject(status string) func(http.ResponseWriter, *http.Request, Profile) {
	return func(w http.ResponseWriter, r *http.Request, p Profile) {
		id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
		if err != nil {
			writeErr(w, http.StatusBadRequest, "잘못된 id 입니다")
			return
		}
		var body struct {
			Reason string `json:"reason"`
		}
		json.NewDecoder(r.Body).Decode(&body) // 사유는 선택

		if err := reviewVersion(s.db, id, status, body.Reason, p); err == errNotFound {
			writeErr(w, http.StatusNotFound, "이미 처리되었거나 없는 제출입니다")
			return
		} else if err != nil {
			writeErr(w, http.StatusInternalServerError, "처리에 실패했습니다")
			return
		}
		writeJSON(w, http.StatusOK, map[string]string{"status": status})
	}
}
