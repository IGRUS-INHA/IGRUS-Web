package main

import (
	"context"
	"encoding/json"
	"net/http"
	"sync"
	"time"
)

// 이 서버는 JWT 를 직접 검증하지 않는다.
// igrus-web 의 JWT 는 HMAC 대칭키라서, 시크릿을 여기로 가져오면
// 이 서버가 igrus-web 전체의 토큰을 위조할 수 있게 된다.
// 대신 유저가 보낸 Authorization 헤더를 그대로 기존 백엔드의
// /api/v1/mypage/profile 에 넘기고 그 응답을 신뢰한다. (토큰 introspection)

type Profile struct {
	StudentID  string `json:"studentId"`
	Name       string `json:"name"`
	Department string `json:"department"`
	Role       string `json:"role"` // ASSOCIATE | MEMBER | OPERATOR | ADMIN
}

func (p Profile) IsStaff() bool {
	return p.Role == "OPERATOR" || p.Role == "ADMIN"
}

// 토큰 → 신원(학번·이름·role)은 짧게 캐시한다. 이건 화면에 보이는 표시 데이터가
// 아니라서 캐시해도 "고쳤는데 안 바뀐다" 류의 버그가 생기지 않는다. 반대로 캐시가
// 없으면 로그인 필요 요청마다 igrus 로 왕복이 한 번씩 붙고, play 의 가용성이
// igrus 에 그대로 묶인다. 권한 회수가 최대 60초 늦게 반영되는 건 감수한다.
//
// (닉네임/자기소개/링크 같은 공개 프로필은 캐시하지 않는다 — profiles.go 참고)
// ponytail: 프로세스 메모리 캐시. 서버를 여러 대로 늘리면 캐시가 인스턴스마다
// 따로 놀지만, TTL 이 60초라 실질 문제는 없다. 문제가 되면 Redis 로 교체.
type authCache struct {
	mu      sync.Mutex
	entries map[string]cacheEntry
}

type cacheEntry struct {
	profile Profile
	expires time.Time
}

const authTTL = 60 * time.Second

func newAuthCache() *authCache {
	return &authCache{entries: map[string]cacheEntry{}}
}

func (c *authCache) get(token string) (Profile, bool) {
	c.mu.Lock()
	defer c.mu.Unlock()
	e, ok := c.entries[token]
	if !ok || time.Now().After(e.expires) {
		delete(c.entries, token)
		return Profile{}, false
	}
	return e.profile, true
}

func (c *authCache) put(token string, p Profile) {
	c.mu.Lock()
	defer c.mu.Unlock()
	// 만료된 항목 청소 — 토큰 종류가 많지 않아 전체 순회로 충분하다.
	now := time.Now()
	for k, e := range c.entries {
		if now.After(e.expires) {
			delete(c.entries, k)
		}
	}
	c.entries[token] = cacheEntry{profile: p, expires: now.Add(authTTL)}
}

type authenticator struct {
	apiBase string
	cache   *authCache
	client  *http.Client
}

func newAuthenticator(apiBase string) *authenticator {
	return &authenticator{
		apiBase: apiBase,
		cache:   newAuthCache(),
		client:  &http.Client{Timeout: 5 * time.Second},
	}
}

func (a *authenticator) lookup(ctx context.Context, token string) (Profile, error) {
	if p, ok := a.cache.get(token); ok {
		return p, nil
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, a.apiBase+"/api/v1/mypage/profile", nil)
	if err != nil {
		return Profile{}, err
	}
	req.Header.Set("Authorization", token)

	resp, err := a.client.Do(req)
	if err != nil {
		return Profile{}, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return Profile{}, errUnauthorized
	}

	var p Profile
	if err := json.NewDecoder(resp.Body).Decode(&p); err != nil {
		return Profile{}, err
	}
	a.cache.put(token, p)
	return p, nil
}

// requireLogin 은 로그인한 유저만 통과시킨다.
func (a *authenticator) requireLogin(next func(http.ResponseWriter, *http.Request, Profile)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		token := r.Header.Get("Authorization")
		if token == "" {
			writeErr(w, http.StatusUnauthorized, "로그인이 필요합니다")
			return
		}
		p, err := a.lookup(r.Context(), token)
		if err != nil {
			// 기존 백엔드가 죽었을 때와 토큰이 틀렸을 때를 구분한다.
			// 502 로 내려야 프론트가 "로그아웃됨" 으로 오해하지 않는다.
			if err == errUnauthorized {
				writeErr(w, http.StatusUnauthorized, "로그인이 필요합니다")
			} else {
				writeErr(w, http.StatusBadGateway, "인증 서버에 연결할 수 없습니다")
			}
			return
		}
		next(w, r, p)
	}
}

// requireStaff 는 운영진(OPERATOR/ADMIN)만 통과시킨다.
func (a *authenticator) requireStaff(next func(http.ResponseWriter, *http.Request, Profile)) http.HandlerFunc {
	return a.requireLogin(func(w http.ResponseWriter, r *http.Request, p Profile) {
		if !p.IsStaff() {
			writeErr(w, http.StatusForbidden, "운영진만 접근할 수 있습니다")
			return
		}
		next(w, r, p)
	})
}
