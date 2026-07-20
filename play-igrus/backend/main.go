package main

import (
	"cmp"
	"context"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"
)

func main() {
	ctx := context.Background()
	var (
		port    = cmp.Or(os.Getenv("PORT"), "8080")
		apiBase = cmp.Or(os.Getenv("IGRUS_API"), "https://api.igrus.co.kr")
		dataDir = cmp.Or(os.Getenv("DATA_DIR"), "./data")
		dsn     = os.Getenv("DSN")
	)

	if name := os.Getenv("PLAY_SECRET_NAME"); name != "" {
		var err error
		dsn, err = loadDSNFromSecrets(ctx, name)
		if err != nil {
			log.Fatalf("시크릿 로드 실패: %v", err)
		}
	}
	if dsn == "" {
		log.Fatal("DSN 환경변수 또는 PLAY_SECRET_NAME 이 필요합니다")
	}

	db, err := openDB(dsn)
	if err != nil {
		log.Fatalf("DB 열기 실패: %v", err)
	}
	defer db.Close()

	images, err := newImageStore(ctx, dataDir)
	if err != nil {
		log.Fatalf("이미지 스토어 초기화 실패: %v", err)
	}

	s := &server{db: db, auth: newAuthenticator(apiBase), images: images}
	startRankingLoop(db)

	mux := http.NewServeMux()

	// 공개
	mux.HandleFunc("GET /api/projects", s.listApproved)
	mux.HandleFunc("GET /api/projects/{id}", s.getDetail)
	mux.HandleFunc("POST /api/projects/{id}/click", s.click)
	mux.HandleFunc("GET /images/{key}", s.serveImage)
	mux.HandleFunc("GET /healthz", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
	})

	// SPA 정적 서빙 (프론트 빌드 산출물 — Docker 이미지에 /web 로 포함).
	// 로컬 개발은 vite dev 가 UI 를 서빙하므로 디렉토리 없으면 등록 생략.
	staticDir := cmp.Or(os.Getenv("STATIC_DIR"), "./web")
	if st, err := os.Stat(staticDir); err == nil && st.IsDir() {
		mux.Handle("GET /", spaHandler(staticDir))
	}

	// 로그인 필요
	mux.HandleFunc("POST /api/projects", s.auth.requireLogin(s.createProject))
	mux.HandleFunc("PUT /api/projects/{id}", s.auth.requireLogin(s.updateProject))
	mux.HandleFunc("GET /api/projects/mine", s.auth.requireLogin(s.listMine))

	// 운영진 전용
	mux.HandleFunc("GET /api/admin/projects", s.auth.requireStaff(s.listForReview))
	mux.HandleFunc("POST /api/admin/projects/{id}/approve", s.auth.requireStaff(s.reviewProject("approved")))
	mux.HandleFunc("POST /api/admin/projects/{id}/reject", s.auth.requireStaff(s.reviewProject("rejected")))

	srv := &http.Server{
		Addr:              ":" + port,
		Handler:           cors(mux),
		ReadHeaderTimeout: 10 * time.Second,
	}
	log.Printf("play-igrus 시작 :%s (인증 위임 → %s)", port, apiBase)
	log.Fatal(srv.ListenAndServe())
}

// spaHandler 는 존재하는 파일은 그대로, 그 외 경로(/my, /admin 등 라우터 경로)는
// index.html 로 폴백한다. 해시 붙은 /assets/* 는 1년 immutable 캐시.
func spaHandler(dir string) http.Handler {
	files := http.FileServer(http.Dir(dir))
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		path := filepath.Join(dir, filepath.Clean("/"+r.URL.Path))
		if st, err := os.Stat(path); err == nil && !st.IsDir() {
			if strings.HasPrefix(r.URL.Path, "/assets/") {
				w.Header().Set("Cache-Control", "public, max-age=31536000, immutable")
			}
			files.ServeHTTP(w, r)
			return
		}
		http.ServeFile(w, r, filepath.Join(dir, "index.html"))
	})
}

// cors 는 로컬 vite dev(다른 origin)용. prod 는 SPA 를 같은 origin 에서 서빙하므로 사실상 무동작.
func cors(next http.Handler) http.Handler {
	allowed := cmp.Or(os.Getenv("CORS_ORIGIN"), "https://play.igrus.co.kr")
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		origin := r.Header.Get("Origin")
		if origin == allowed ||
			strings.HasPrefix(origin, "http://localhost:") ||
			strings.HasPrefix(origin, "http://127.0.0.1:") {
			w.Header().Set("Access-Control-Allow-Origin", origin)
			w.Header().Set("Vary", "Origin")
			w.Header().Set("Access-Control-Allow-Headers", "Authorization, Content-Type")
			w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS")
			w.Header().Set("Access-Control-Max-Age", "3600")
		}
		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}
