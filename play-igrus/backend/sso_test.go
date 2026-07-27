package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

// 가짜 igrus 백엔드 — sso 프록시가 쿠키를 올바르게 번역하는지 검증용.
func newFakeIgrus(t *testing.T) *httptest.Server {
	t.Helper()
	mux := http.NewServeMux()

	mux.HandleFunc("POST /api/v1/auth/sso/token", func(w http.ResponseWriter, r *http.Request) {
		var in struct{ Code string }
		json.NewDecoder(r.Body).Decode(&in)
		if in.Code != "good-code" {
			w.WriteHeader(http.StatusUnauthorized)
			json.NewEncoder(w).Encode(map[string]string{"message": "유효하지 않거나 만료된 SSO 코드입니다"})
			return
		}
		json.NewEncoder(w).Encode(map[string]any{
			"accessToken": "at-1", "refreshToken": "rt-1",
			"expiresIn": 300000, "refreshExpiresIn": 1209600000,
		})
	})

	mux.HandleFunc("POST /api/v1/auth/password/refresh", func(w http.ResponseWriter, r *http.Request) {
		c, err := r.Cookie(igrusRefreshCookie)
		if err != nil || c.Value != "rt-1" {
			http.SetCookie(w, &http.Cookie{Name: igrusRefreshCookie, Value: "", MaxAge: -1, Path: "/api/v1/auth"})
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
		// 로테이션: 새 refresh 토큰을 igrus 도메인 쿠키로 내림
		http.SetCookie(w, &http.Cookie{Name: igrusRefreshCookie, Value: "rt-2", MaxAge: 1209600, Path: "/api/v1/auth"})
		json.NewEncoder(w).Encode(map[string]any{"accessToken": "at-2", "expiresIn": 300000})
	})

	mux.HandleFunc("POST /api/v1/auth/password/login", func(w http.ResponseWriter, r *http.Request) {
		var in struct{ StudentID, Password string }
		json.NewDecoder(r.Body).Decode(&in)
		if in.Password != "pw" {
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
		http.SetCookie(w, &http.Cookie{Name: igrusRefreshCookie, Value: "rt-login", MaxAge: 1209600, Path: "/api/v1/auth"})
		json.NewEncoder(w).Encode(map[string]any{"accessToken": "at-login"})
	})

	mux.HandleFunc("POST /api/v1/auth/password/logout", func(w http.ResponseWriter, r *http.Request) {
		http.SetCookie(w, &http.Cookie{Name: igrusRefreshCookie, Value: "", MaxAge: -1, Path: "/api/v1/auth"})
		w.WriteHeader(http.StatusOK)
	})

	srv := httptest.NewServer(mux)
	t.Cleanup(srv.Close)
	return srv
}

func playCookie(res *http.Response) *http.Cookie {
	for _, c := range res.Cookies() {
		if c.Name == playRefreshCookie {
			return c
		}
	}
	return nil
}

// state 쿠키를 포함해 exchange 요청을 만든다.
func exchangeRequest(body, cookieState string) *http.Request {
	r := httptest.NewRequest("POST", "/auth/sso/exchange", strings.NewReader(body))
	if cookieState != "" {
		r.AddCookie(&http.Cookie{Name: playSsoStateCookie, Value: cookieState})
	}
	return r
}

func TestSsoStart(t *testing.T) {
	s := &server{}
	w := httptest.NewRecorder()
	s.ssoStart(w, httptest.NewRequest("POST", "/auth/sso/start", nil))
	res := w.Result()
	if res.StatusCode != 200 {
		t.Fatalf("status = %d", res.StatusCode)
	}
	var body struct{ State string }
	json.NewDecoder(res.Body).Decode(&body)
	var stateCookie *http.Cookie
	for _, c := range res.Cookies() {
		if c.Name == playSsoStateCookie {
			stateCookie = c
		}
	}
	if stateCookie == nil || !stateCookie.HttpOnly || stateCookie.Value == "" {
		t.Fatalf("state 쿠키가 잘못 설정됨: %+v", stateCookie)
	}
	if body.State != stateCookie.Value {
		t.Errorf("본문 state(%q) != 쿠키 state(%q)", body.State, stateCookie.Value)
	}
}

func TestSsoExchange(t *testing.T) {
	s := &server{auth: newAuthenticator(newFakeIgrus(t).URL)}

	// 성공: state 일치 → 쿠키 심고 accessToken 만 내린다
	w := httptest.NewRecorder()
	s.ssoExchange(w, exchangeRequest(`{"code":"good-code","state":"st-1"}`, "st-1"))
	res := w.Result()
	if res.StatusCode != 200 {
		t.Fatalf("status = %d", res.StatusCode)
	}
	c := playCookie(res)
	if c == nil || c.Value != "rt-1" || !c.HttpOnly || c.Path != authCookiePath {
		t.Fatalf("refresh 쿠키가 잘못 설정됨: %+v", c)
	}
	var body map[string]any
	json.NewDecoder(res.Body).Decode(&body)
	if body["accessToken"] != "at-1" {
		t.Errorf("accessToken 누락: %v", body)
	}
	if _, leaked := body["refreshToken"]; leaked {
		t.Error("refreshToken 이 응답 본문으로 새면 안 된다")
	}

	// 로그인 CSRF 방어: state 불일치/누락이면 코드가 유효해도 403
	for name, r := range map[string]*http.Request{
		"state 불일치":  exchangeRequest(`{"code":"good-code","state":"st-1"}`, "other"),
		"state 쿠키 없음": exchangeRequest(`{"code":"good-code","state":"st-1"}`, ""),
		"state 필드 없음": exchangeRequest(`{"code":"good-code"}`, "st-1"),
	} {
		w = httptest.NewRecorder()
		s.ssoExchange(w, r)
		if w.Result().StatusCode != 403 {
			t.Errorf("%s status = %d, want 403", name, w.Result().StatusCode)
		}
		if c := playCookie(w.Result()); c != nil {
			t.Errorf("%s 인데 refresh 쿠키가 설정됨", name)
		}
	}

	// 실패: 무효 코드는 상태 중계, refresh 쿠키 없음
	w = httptest.NewRecorder()
	s.ssoExchange(w, exchangeRequest(`{"code":"bad","state":"st-1"}`, "st-1"))
	if w.Result().StatusCode != 401 {
		t.Errorf("무효 코드 status = %d, want 401", w.Result().StatusCode)
	}
	if c := playCookie(w.Result()); c != nil {
		t.Error("무효 코드인데 refresh 쿠키가 설정됨")
	}

	// code 없음 → 400
	w = httptest.NewRecorder()
	s.ssoExchange(w, exchangeRequest(`{}`, "st-1"))
	if w.Result().StatusCode != 400 {
		t.Errorf("code 없음 status = %d, want 400", w.Result().StatusCode)
	}
}

func TestSsoRefresh(t *testing.T) {
	s := &server{auth: newAuthenticator(newFakeIgrus(t).URL)}

	// 쿠키 없음 → igrus 안 가고 401
	w := httptest.NewRecorder()
	s.ssoRefresh(w, httptest.NewRequest("POST", "/auth/refresh", nil))
	if w.Result().StatusCode != 401 {
		t.Fatalf("쿠키 없음 status = %d, want 401", w.Result().StatusCode)
	}

	// 유효 쿠키 → 로테이션된 토큰으로 쿠키 갱신
	w = httptest.NewRecorder()
	r := httptest.NewRequest("POST", "/auth/refresh", nil)
	r.AddCookie(&http.Cookie{Name: playRefreshCookie, Value: "rt-1"})
	s.ssoRefresh(w, r)
	res := w.Result()
	if res.StatusCode != 200 {
		t.Fatalf("status = %d", res.StatusCode)
	}
	if c := playCookie(res); c == nil || c.Value != "rt-2" {
		t.Fatalf("로테이션 쿠키 미반영: %+v", c)
	}

	// 무효 쿠키 → 401 + 쿠키 삭제
	w = httptest.NewRecorder()
	r = httptest.NewRequest("POST", "/auth/refresh", nil)
	r.AddCookie(&http.Cookie{Name: playRefreshCookie, Value: "rt-stale"})
	s.ssoRefresh(w, r)
	res = w.Result()
	if res.StatusCode != 401 {
		t.Fatalf("무효 쿠키 status = %d, want 401", res.StatusCode)
	}
	if c := playCookie(res); c == nil || c.Value != "" || c.MaxAge >= 0 {
		t.Fatalf("무효 세션 쿠키가 삭제되지 않음: %+v", c)
	}
}

func TestSsoLoginAndLogout(t *testing.T) {
	s := &server{auth: newAuthenticator(newFakeIgrus(t).URL)}

	// 로그인 성공 → 퍼스트파티 쿠키
	w := httptest.NewRecorder()
	r := httptest.NewRequest("POST", "/auth/login", strings.NewReader(`{"studentId":"12345678","password":"pw"}`))
	s.ssoLogin(w, r)
	res := w.Result()
	if res.StatusCode != 200 {
		t.Fatalf("status = %d", res.StatusCode)
	}
	if c := playCookie(res); c == nil || c.Value != "rt-login" {
		t.Fatalf("로그인 쿠키 미설정: %+v", c)
	}

	// 로그인 실패 → 상태 중계, 쿠키 없음
	w = httptest.NewRecorder()
	s.ssoLogin(w, httptest.NewRequest("POST", "/auth/login", strings.NewReader(`{"studentId":"1","password":"wrong"}`)))
	if w.Result().StatusCode != 401 {
		t.Errorf("로그인 실패 status = %d, want 401", w.Result().StatusCode)
	}

	// 로그아웃 → 쿠키 삭제 (igrus 세션 종료 중계 포함)
	w = httptest.NewRecorder()
	r = httptest.NewRequest("POST", "/auth/logout", nil)
	r.AddCookie(&http.Cookie{Name: playRefreshCookie, Value: "rt-login"})
	s.ssoLogout(w, r)
	res = w.Result()
	if c := playCookie(res); c == nil || c.Value != "" || c.MaxAge >= 0 {
		t.Fatalf("로그아웃 후 쿠키가 삭제되지 않음: %+v", c)
	}

	// 쿠키 없이 로그아웃해도 200
	w = httptest.NewRecorder()
	s.ssoLogout(w, httptest.NewRequest("POST", "/auth/logout", nil))
	if w.Result().StatusCode != 200 {
		t.Errorf("빈 로그아웃 status = %d, want 200", w.Result().StatusCode)
	}
}

// X-Forwarded-Proto: https 일 때만 Secure 쿠키가 된다 (Caddy 뒤 prod).
func TestPlayCookieSecureFlag(t *testing.T) {
	s := &server{}

	w := httptest.NewRecorder()
	r := httptest.NewRequest("POST", "/auth/refresh", nil)
	r.Header.Set("X-Forwarded-Proto", "https")
	s.setPlayRefreshCookie(w, r, "v", 100)
	if c := playCookie(w.Result()); c == nil || !c.Secure {
		t.Error("https 요청인데 Secure 쿠키가 아님")
	}

	w = httptest.NewRecorder()
	s.setPlayRefreshCookie(w, httptest.NewRequest("POST", "/auth/refresh", nil), "v", 100)
	if c := playCookie(w.Result()); c == nil || c.Secure {
		t.Error("로컬 http 인데 Secure 쿠키면 dev 에서 동작 안 함")
	}
}
