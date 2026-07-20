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
	maxTitleLen    = 100
	maxDescLen     = 50 // 설명 최대 50자 (스펙)
	maxBodyLen     = 20000
	maxCategoryLen = 20
	maxURLLen      = 2048
)

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
type projectView struct {
	ID           int64      `json:"id"`
	Title        string     `json:"title"`
	Description  string     `json:"description"`
	Body         string     `json:"body,omitempty"`
	Category     string     `json:"category"`
	Author       string     `json:"author"`
	ThumbnailURL string     `json:"thumbnailUrl,omitempty"`
	BannerURL    string     `json:"bannerUrl,omitempty"`
	RedirectURL  string     `json:"redirectUrl,omitempty"`
	Status       string     `json:"status,omitempty"`
	RejectReason string     `json:"rejectReason,omitempty"`
	ReviewerName string     `json:"reviewerName,omitempty"`
	TotalClicks  int64      `json:"totalClicks"`
	CreatedAt    time.Time  `json:"createdAt"`
	ReviewedAt   *time.Time `json:"reviewedAt,omitempty"`
}

func imageURL(key string) string {
	if key == "" {
		return ""
	}
	return "/images/" + key
}

// listView: 메인 카드용 — 본문/리다이렉트 제외 (payload 절약)
func listView(p row) projectView {
	return projectView{
		ID:           p.ID,
		Title:        p.Title,
		Description:  p.Description,
		Category:     p.Category,
		Author:       strings.TrimSpace(p.Department + " " + p.AuthorName),
		ThumbnailURL: imageURL(p.ThumbnailKey),
		TotalClicks:  p.TotalClicks,
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

// mineView / adminView: 상태·반려사유 포함
func mineView(p row) projectView {
	v := detailView(p)
	v.Status = p.Status
	v.RejectReason = p.RejectReason
	v.ReviewedAt = p.ReviewedAt
	return v
}

func adminView(p row) projectView {
	v := mineView(p)
	v.ReviewerName = p.ReviewerName
	return v
}

func views(items []row, f func(row) projectView) []projectView {
	out := make([]projectView, 0, len(items))
	for _, p := range items {
		out = append(out, f(p))
	}
	return out
}

// GET /api/projects?category= — 승인작만, 인기순. 로그인 없이 볼 수 있다.
func (s *server) listApproved(w http.ResponseWriter, r *http.Request) {
	items, err := listApprovedProjects(s.db, strings.TrimSpace(r.URL.Query().Get("category")))
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "목록을 불러올 수 없습니다")
		return
	}
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
	writeJSON(w, http.StatusOK, detailView(p))
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

// POST /api/projects (multipart: title, description, body, url, category, thumbnail?, banner?)
func (s *server) createProject(w http.ResponseWriter, r *http.Request, p Profile) {
	if err := r.ParseMultipartForm(2 * maxImageBytes); err != nil {
		writeErr(w, http.StatusBadRequest, "폼을 읽을 수 없습니다")
		return
	}

	title := strings.TrimSpace(r.FormValue("title"))
	desc := strings.TrimSpace(r.FormValue("description"))
	body := r.FormValue("body")
	category := strings.TrimSpace(r.FormValue("category"))

	if title == "" || utf8.RuneCountInString(title) > maxTitleLen {
		writeErr(w, http.StatusBadRequest, "제목은 1~100자여야 합니다")
		return
	}
	if desc == "" || utf8.RuneCountInString(desc) > maxDescLen {
		writeErr(w, http.StatusBadRequest, "설명은 1~50자여야 합니다")
		return
	}
	if utf8.RuneCountInString(body) > maxBodyLen {
		writeErr(w, http.StatusBadRequest, "본문이 너무 깁니다")
		return
	}
	// 분류는 자유 문자열 (스펙: enum ㄴㄴ) — 길이만 제한
	if category == "" || utf8.RuneCountInString(category) > maxCategoryLen {
		writeErr(w, http.StatusBadRequest, "분류는 1~20자여야 합니다")
		return
	}
	link, ok := validateURL(r.FormValue("url"))
	if !ok {
		writeErr(w, http.StatusBadRequest, "http:// 또는 https:// 로 시작하는 주소여야 합니다")
		return
	}

	thumb, err := s.saveImage(r, "thumbnail")
	if err != nil {
		writeImageErr(w, err)
		return
	}
	banner, err := s.saveImage(r, "banner")
	if err != nil {
		writeImageErr(w, err)
		return
	}

	id, err := insertProject(s.db, row{
		StudentID:    p.StudentID,
		AuthorName:   p.Name,
		Department:   p.Department,
		Title:        title,
		Description:  desc,
		BodyMD:       body,
		ThumbnailKey: thumb,
		BannerKey:    banner,
		RedirectURL:  link,
		Category:     category,
	})
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "저장에 실패했습니다")
		return
	}
	writeJSON(w, http.StatusCreated, map[string]any{"id": id, "status": "pending"})
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

// GET /api/projects/mine — 내 제출 현황(반려 사유 포함)
func (s *server) listMine(w http.ResponseWriter, r *http.Request, p Profile) {
	items, err := listByStudent(s.db, p.StudentID)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "목록을 불러올 수 없습니다")
		return
	}
	writeJSON(w, http.StatusOK, views(items, mineView))
}

// GET /api/admin/projects?status=pending — 검수 대기열
func (s *server) listForReview(w http.ResponseWriter, r *http.Request, p Profile) {
	status := r.URL.Query().Get("status")
	if status == "" {
		status = "pending"
	}
	if status != "pending" && status != "approved" && status != "rejected" {
		writeErr(w, http.StatusBadRequest, "잘못된 status 입니다")
		return
	}
	items, err := listByStatus(s.db, status)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "목록을 불러올 수 없습니다")
		return
	}
	writeJSON(w, http.StatusOK, views(items, adminView))
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

		if err := review(s.db, id, status, body.Reason, p); err == errNotFound {
			writeErr(w, http.StatusNotFound, "이미 처리되었거나 없는 제출입니다")
			return
		} else if err != nil {
			writeErr(w, http.StatusInternalServerError, "처리에 실패했습니다")
			return
		}
		writeJSON(w, http.StatusOK, map[string]string{"status": status})
	}
}
