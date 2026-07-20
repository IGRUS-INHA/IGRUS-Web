package main

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"
	"sync"
	"time"
)

// 공개 프로필 (닉네임/자기소개/링크) — igrus 백엔드 public-profile 프록시.
// 닉네임 폴백은 igrus 서버가 한다: 닉네임이 있으면 displayName 에 닉네임만 오고
// 실명은 응답에 없다. 여기서는 조회 실패 시에만 제출 당시 이름 스냅샷으로 폴백.

type profileLink struct {
	Label string `json:"label"`
	URL   string `json:"url"`
}

type publicProfile struct {
	DisplayName  string        `json:"displayName"`
	Introduction string        `json:"introduction"`
	Links        []profileLink `json:"links"`
}

// ponytail: 프로세스 메모리 5분 캐시 (authCache 와 같은 이유) — 문제가 되면 Redis.
const profileTTL = 5 * time.Minute

type profileCache struct {
	mu      sync.Mutex
	entries map[string]profileEntry
}

type profileEntry struct {
	profile publicProfile
	ok      bool // 404 등 실패도 캐시해서 igrus 를 반복 호출하지 않는다
	expires time.Time
}

func newProfileCache() *profileCache {
	return &profileCache{entries: map[string]profileEntry{}}
}

func (c *profileCache) get(studentID string) (profileEntry, bool) {
	c.mu.Lock()
	defer c.mu.Unlock()
	e, ok := c.entries[studentID]
	if !ok || time.Now().After(e.expires) {
		delete(c.entries, studentID)
		return profileEntry{}, false
	}
	return e, true
}

func (c *profileCache) put(studentID string, e profileEntry) {
	c.mu.Lock()
	defer c.mu.Unlock()
	now := time.Now()
	for k, old := range c.entries {
		if now.After(old.expires) {
			delete(c.entries, k)
		}
	}
	e.expires = now.Add(profileTTL)
	c.entries[studentID] = e
}

// publicProfile 은 학번으로 공개 프로필을 조회한다 (인증 불필요 엔드포인트).
func (a *authenticator) publicProfile(ctx context.Context, studentID string) (publicProfile, bool) {
	if e, ok := a.profiles.get(studentID); ok {
		return e.profile, e.ok
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodGet,
		a.apiBase+"/api/v1/users/"+studentID+"/public-profile", nil)
	if err != nil {
		return publicProfile{}, false
	}
	resp, err := a.client.Do(req)
	if err != nil {
		// 네트워크 실패는 캐시하지 않는다 — igrus 가 복구되면 바로 다시 시도
		return publicProfile{}, false
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		a.profiles.put(studentID, profileEntry{ok: false})
		return publicProfile{}, false
	}
	var p publicProfile
	if err := json.NewDecoder(resp.Body).Decode(&p); err != nil {
		return publicProfile{}, false
	}
	a.profiles.put(studentID, profileEntry{profile: p, ok: true})
	return p, true
}

// resolveAuthors 는 목록의 작성자 이름을 공개 프로필의 표시 이름(닉네임 폴백 적용)으로
// 교체한다. 조회 실패 시 제출 당시 이름 스냅샷 유지.
// ponytail: 작성자 수가 적어 순차 조회 + 캐시로 충분 — 느려지면 병렬화.
func (s *server) resolveAuthors(ctx context.Context, items []row) {
	for i := range items {
		if prof, ok := s.auth.publicProfile(ctx, items[i].StudentID); ok && prof.DisplayName != "" {
			items[i].AuthorName = prof.DisplayName
		}
	}
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

	out := struct {
		publicProfile
		Projects []projectView `json:"projects"`
	}{publicProfile{DisplayName: p.AuthorName, Links: []profileLink{}}, []projectView{}}
	if prof, ok := s.auth.publicProfile(r.Context(), p.StudentID); ok {
		if prof.DisplayName != "" {
			out.DisplayName = prof.DisplayName
		}
		out.Introduction = prof.Introduction
		if prof.Links != nil {
			out.Links = prof.Links
		}
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
