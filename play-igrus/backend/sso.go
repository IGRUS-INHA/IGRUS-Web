package main

import (
	"bytes"
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"io"
	"net/http"
)

// igrus 인증 프록시.
//
// inhaplay.com 은 igrus.co.kr 과 등록 도메인이 달라 Domain=igrus.co.kr refresh
// 쿠키를 공유할 수 없다. 그래서 로그인/갱신/로그아웃을 이 서버가 igrus 에
// 중계하고, igrus 가 내려주는 refresh 토큰을 이 서버 도메인의 퍼스트파티 쿠키
// (playRefreshToken)로 다시 심는다. play.igrus.co.kr 에서도 같은 경로를 쓴다.
//
// SSO 핸드오프: www 에 로그인돼 있으면 프론트가 igrus 의
// /api/v1/auth/sso/authorize 로 top-level 리다이렉트해 일회용 코드를 받아오고,
// 이 서버의 /auth/sso/exchange 가 코드를 토큰으로 교환해 세션을 만든다.

const (
	igrusRefreshCookie = "refreshToken"     // 스프링 CookieUtil 의 쿠키 이름
	playRefreshCookie  = "playRefreshToken" // 이 서버가 자기 도메인에 심는 쿠키
	playSsoStateCookie = "playSsoState"     // SSO 핸드오프 CSRF 바인딩용 state
	authCookiePath     = "/auth"
)

func (s *server) setPlayRefreshCookie(w http.ResponseWriter, r *http.Request, value string, maxAge int) {
	c := &http.Cookie{
		Name:     playRefreshCookie,
		Value:    value,
		Path:     authCookiePath,
		HttpOnly: true,
		SameSite: http.SameSiteLaxMode,
		// Caddy 뒤에서는 X-Forwarded-Proto 로 https 여부를 판단 (로컬 dev 는 http)
		Secure: r.Header.Get("X-Forwarded-Proto") == "https",
		MaxAge: maxAge,
	}
	if value == "" || maxAge < 0 {
		c.Value = ""
		c.MaxAge = -1
	}
	http.SetCookie(w, c)
}

// forwardAuth 는 igrus 인증 엔드포인트로 요청을 중계하고, 응답의 refresh
// Set-Cookie 를 이 서버의 퍼스트파티 쿠키로 바꿔 심은 뒤 상태/본문을 그대로 내린다.
func (s *server) forwardAuth(w http.ResponseWriter, r *http.Request, igrusPath string, body io.Reader) {
	req, err := http.NewRequestWithContext(r.Context(), http.MethodPost, s.auth.apiBase+igrusPath, body)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "요청 생성 실패")
		return
	}
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	if c, cookieErr := r.Cookie(playRefreshCookie); cookieErr == nil && c.Value != "" {
		req.AddCookie(&http.Cookie{Name: igrusRefreshCookie, Value: c.Value})
	}
	// 로그인 이력(IP/UA)용 원본 정보 전달
	req.Header.Set("User-Agent", r.UserAgent())
	if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
		req.Header.Set("X-Forwarded-For", xff)
	}

	resp, err := s.auth.client.Do(req)
	if err != nil {
		writeErr(w, http.StatusBadGateway, "인증 서버에 연결할 수 없습니다")
		return
	}
	defer resp.Body.Close()

	// igrus 가 refresh 쿠키를 새로 내렸으면(로테이션/삭제 포함) 우리 쿠키로 반영
	for _, c := range resp.Cookies() {
		if c.Name == igrusRefreshCookie {
			s.setPlayRefreshCookie(w, r, c.Value, c.MaxAge)
		}
	}
	// 401 이면 세션이 죽은 것 — 들고 있던 쿠키도 정리
	if resp.StatusCode == http.StatusUnauthorized {
		s.setPlayRefreshCookie(w, r, "", -1)
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(resp.StatusCode)
	io.Copy(w, resp.Body) //nolint:errcheck — 중계 실패는 클라이언트 연결 문제
}

// POST /auth/login — igrus 로그인 중계. 성공 시 refresh 쿠키를 이 도메인에 심는다.
func (s *server) ssoLogin(w http.ResponseWriter, r *http.Request) {
	s.forwardAuth(w, r, "/api/v1/auth/password/login", r.Body)
}

// POST /auth/refresh — 퍼스트파티 refresh 쿠키로 igrus 토큰 갱신 중계.
func (s *server) ssoRefresh(w http.ResponseWriter, r *http.Request) {
	if c, err := r.Cookie(playRefreshCookie); err != nil || c.Value == "" {
		writeErr(w, http.StatusUnauthorized, "로그인이 필요합니다")
		return
	}
	s.forwardAuth(w, r, "/api/v1/auth/password/refresh", nil)
}

// POST /auth/logout — igrus 세션(이 서버가 발급받은 토큰 패밀리만) 종료 + 쿠키 삭제.
// www 세션은 별도 토큰 패밀리라 영향 없다.
func (s *server) ssoLogout(w http.ResponseWriter, r *http.Request) {
	if c, err := r.Cookie(playRefreshCookie); err == nil && c.Value != "" {
		s.forwardAuth(w, r, "/api/v1/auth/password/logout", nil)
		s.setPlayRefreshCookie(w, r, "", -1)
		return
	}
	s.setPlayRefreshCookie(w, r, "", -1)
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

// POST /auth/sso/start — SSO 핸드오프 시작. 랜덤 state 를 HttpOnly 쿠키와
// 응답 본문 양쪽에 내린다. exchange 가 둘의 일치를 요구하므로, 이 브라우저가
// 직접 시작한 핸드오프가 아니면(공격자가 보낸 sso_code 링크 등) 교환이 거부된다.
func (s *server) ssoStart(w http.ResponseWriter, r *http.Request) {
	buf := make([]byte, 16)
	if _, err := rand.Read(buf); err != nil {
		writeErr(w, http.StatusInternalServerError, "state 생성 실패")
		return
	}
	state := hex.EncodeToString(buf)
	http.SetCookie(w, &http.Cookie{
		Name:     playSsoStateCookie,
		Value:    state,
		Path:     authCookiePath,
		HttpOnly: true,
		SameSite: http.SameSiteLaxMode,
		Secure:   r.Header.Get("X-Forwarded-Proto") == "https",
		MaxAge:   600, // 핸드오프 왕복이면 충분
	})
	writeJSON(w, http.StatusOK, map[string]string{"state": state})
}

// POST /auth/sso/exchange {code, state} — SSO 일회용 코드를 igrus 토큰으로
// 교환하고 refresh 토큰을 퍼스트파티 쿠키로 심는다. 응답은 accessToken 만 내린다.
// state 는 /auth/sso/start 가 심은 쿠키와 일치해야 한다 (로그인 CSRF 방지).
func (s *server) ssoExchange(w http.ResponseWriter, r *http.Request) {
	var in struct {
		Code  string `json:"code"`
		State string `json:"state"`
	}
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil || in.Code == "" {
		writeErr(w, http.StatusBadRequest, "code 가 필요합니다")
		return
	}

	stateCookie, err := r.Cookie(playSsoStateCookie)
	if in.State == "" || err != nil || stateCookie.Value != in.State {
		writeErr(w, http.StatusForbidden, "SSO state 가 일치하지 않습니다")
		return
	}
	// state 는 1회용 — 검증 즉시 폐기
	http.SetCookie(w, &http.Cookie{Name: playSsoStateCookie, Value: "", Path: authCookiePath, HttpOnly: true, MaxAge: -1})

	payload, _ := json.Marshal(map[string]string{"code": in.Code})
	req, err := http.NewRequestWithContext(r.Context(), http.MethodPost,
		s.auth.apiBase+"/api/v1/auth/sso/token", bytes.NewReader(payload))
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "요청 생성 실패")
		return
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := s.auth.client.Do(req)
	if err != nil {
		writeErr(w, http.StatusBadGateway, "인증 서버에 연결할 수 없습니다")
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(resp.StatusCode)
		io.Copy(w, resp.Body) //nolint:errcheck
		return
	}

	var out struct {
		AccessToken      string `json:"accessToken"`
		RefreshToken     string `json:"refreshToken"`
		ExpiresIn        int64  `json:"expiresIn"`
		RefreshExpiresIn int64  `json:"refreshExpiresIn"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&out); err != nil || out.RefreshToken == "" {
		writeErr(w, http.StatusBadGateway, "인증 서버 응답이 올바르지 않습니다")
		return
	}

	s.setPlayRefreshCookie(w, r, out.RefreshToken, int(out.RefreshExpiresIn/1000))
	writeJSON(w, http.StatusOK, map[string]any{
		"accessToken": out.AccessToken,
		"expiresIn":   out.ExpiresIn,
	})
}
