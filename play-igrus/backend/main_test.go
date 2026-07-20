package main

import (
	"bytes"
	"image"
	"image/png"
	"math"
	"mime/multipart"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"
)

// validateURL 이 뚫리면 승인된 작품 카드가 그대로 XSS 가 된다.
func TestValidateURL(t *testing.T) {
	bad := []string{
		"javascript:alert(1)",
		"data:text/html,<script>alert(1)</script>",
		"JavaScript:alert(1)",
		"file:///etc/passwd",
		"http://",
		"",
		"그냥글자",
	}
	for _, s := range bad {
		if _, ok := validateURL(s); ok {
			t.Errorf("통과하면 안 되는 URL 이 통과함: %q", s)
		}
	}

	good := []string{
		"https://igrus.co.kr/game",
		"http://example.com",
		"  https://example.com/a?b=1  ", // 앞뒤 공백은 잘라내고 통과
	}
	for _, s := range good {
		if _, ok := validateURL(s); !ok {
			t.Errorf("통과해야 하는 URL 이 막힘: %q", s)
		}
	}
}

// 랭킹 공식: score = Σ(일별 클릭 × 0.5^(경과일/7))
func TestDecay(t *testing.T) {
	cases := []struct {
		daysAgo float64
		want    float64
	}{
		{0, 1},    // 오늘 클릭은 그대로
		{7, 0.5},  // 반감기
		{14, 0.25},
	}
	for _, c := range cases {
		if got := decay(c.daysAgo); math.Abs(got-c.want) > 1e-9 {
			t.Errorf("decay(%v) = %v, want %v", c.daysAgo, got, c.want)
		}
	}
}

// 만료된 캐시 항목을 계속 쓰면 탈퇴/정지된 유저가 TTL 이후에도 통과한다.
func TestAuthCacheExpires(t *testing.T) {
	c := newAuthCache()
	c.entries["tok"] = cacheEntry{
		profile: Profile{Name: "홍길동"},
		expires: time.Now().Add(-time.Second), // 이미 만료
	}
	if _, ok := c.get("tok"); ok {
		t.Error("만료된 캐시가 통과함")
	}

	c.put("tok2", Profile{Name: "김철수", Role: "OPERATOR"})
	p, ok := c.get("tok2")
	if !ok {
		t.Fatal("방금 넣은 캐시를 못 찾음")
	}
	if !p.IsStaff() {
		t.Error("OPERATOR 가 운영진으로 인식되지 않음")
	}
	if (Profile{Role: "MEMBER"}).IsStaff() {
		t.Error("MEMBER 가 운영진으로 인식됨")
	}
}

// 이미지 키 형식 밖의 경로(../ 등)는 절대 서빙되면 안 된다.
func TestImageKeyPattern(t *testing.T) {
	if !keyPattern.MatchString("0123456789abcdef0123456789abcdef.png") {
		t.Error("정상 키가 막힘")
	}
	for _, k := range []string{"../etc/passwd", "a.png", "0123456789abcdef0123456789abcdef.svg", "0123456789ABCDEF0123456789abcdef.png"} {
		if keyPattern.MatchString(k) {
			t.Errorf("비정상 키가 통과함: %q", k)
		}
	}
}

// saveImage: 진짜 이미지가 아니면 거부, PNG 는 저장 후 키 반환.
func TestSaveImage(t *testing.T) {
	dir := t.TempDir()
	s := &server{images: dirStore{dir: dir}}

	// 가짜 PNG (확장자만 png 인 텍스트)
	var buf bytes.Buffer
	mw := multipart.NewWriter(&buf)
	fw, _ := mw.CreateFormFile("thumbnail", "fake.png")
	fw.Write([]byte("not an image"))
	mw.Close()
	r := httptest.NewRequest("POST", "/api/projects", &buf)
	r.Header.Set("Content-Type", mw.FormDataContentType())
	if _, err := s.saveImage(r, "thumbnail"); err != errNotImage {
		t.Errorf("가짜 이미지가 거부되지 않음: %v", err)
	}

	// 진짜 PNG
	var img bytes.Buffer
	png.Encode(&img, image.NewRGBA(image.Rect(0, 0, 2, 2)))
	buf.Reset()
	mw = multipart.NewWriter(&buf)
	fw, _ = mw.CreateFormFile("thumbnail", "real.png")
	fw.Write(img.Bytes())
	mw.Close()
	r = httptest.NewRequest("POST", "/api/projects", &buf)
	r.Header.Set("Content-Type", mw.FormDataContentType())
	key, err := s.saveImage(r, "thumbnail")
	if err != nil {
		t.Fatalf("진짜 PNG 저장 실패: %v", err)
	}
	if !keyPattern.MatchString(key) {
		t.Errorf("생성된 키가 패턴에 안 맞음: %q", key)
	}

	// 필드 없음 → 에러 아님, 빈 키 (썸네일 선택)
	buf.Reset()
	mw = multipart.NewWriter(&buf)
	mw.WriteField("title", "x")
	mw.Close()
	r = httptest.NewRequest("POST", "/api/projects", &buf)
	r.Header.Set("Content-Type", mw.FormDataContentType())
	if key, err := s.saveImage(r, "thumbnail"); err != nil || key != "" {
		t.Errorf("필드 없음이 에러가 됨: key=%q err=%v", key, err)
	}
}

// SPA 서빙: 실제 파일은 그대로, 라우터 경로는 index.html 폴백, assets 는 장기 캐시.
func TestSpaHandler(t *testing.T) {
	dir := t.TempDir()
	os.WriteFile(dir+"/index.html", []byte("<html>app</html>"), 0o644)
	os.Mkdir(dir+"/assets", 0o755)
	os.WriteFile(dir+"/assets/main.js", []byte("js"), 0o644)
	h := spaHandler(dir)

	get := func(path string) *httptest.ResponseRecorder {
		w := httptest.NewRecorder()
		h.ServeHTTP(w, httptest.NewRequest("GET", path, nil))
		return w
	}

	if w := get("/assets/main.js"); w.Body.String() != "js" ||
		w.Header().Get("Cache-Control") != "public, max-age=31536000, immutable" {
		t.Errorf("assets 서빙/캐시 헤더가 틀림: %q %q", w.Body.String(), w.Header().Get("Cache-Control"))
	}
	// 라우터 경로(/my)와 루트 → index.html 폴백
	for _, p := range []string{"/", "/my", "/admin", "/no/such/route"} {
		if w := get(p); w.Code != 200 || w.Body.String() != "<html>app</html>" {
			t.Errorf("%s 가 index.html 폴백이 아님: code=%d", p, w.Code)
		}
	}
	// 경로 조작이 dir 밖으로 못 나가는지 — ServeFile 의 .. 차단(400) 또는 폴백, 어느 쪽이든
	// 밖의 파일 내용만 아니면 안전하다.
	if w := get("/../main_test.go"); strings.Contains(w.Body.String(), "package main") {
		t.Error("경로 조작이 dir 밖 파일에 접근함")
	}
}

// DB 통합 테스트 — 로컬 MySQL 이 있을 때만 실행.
// 예) TEST_DSN='root:root@tcp(127.0.0.1:3306)/play_igrus_test?parseTime=true' go test ./...
func TestReviewAndRankingWithMySQL(t *testing.T) {
	dsn := os.Getenv("TEST_DSN")
	if dsn == "" {
		t.Skip("TEST_DSN 미설정 — MySQL 통합 테스트 생략")
	}
	db, err := openDB(dsn)
	if err != nil {
		t.Fatal(err)
	}
	// defer db.Close() 를 쓰면 Cleanup 이 닫힌 DB 에 DELETE 를 날리게 된다 (Cleanup 은 defer 이후 실행)
	t.Cleanup(func() {
		db.Exec(`DELETE FROM project_clicks_daily`)
		db.Exec(`DELETE FROM projects`)
		db.Close()
	})
	// 이전 실행이 비정상 종료로 남긴 행 제거 (건수 단언이 흔들리지 않게)
	db.Exec(`DELETE FROM project_clicks_daily`)
	db.Exec(`DELETE FROM projects`)

	id, err := insertProject(db, row{
		StudentID: "12201234", AuthorName: "홍길동", Department: "컴퓨터공학과",
		Title: "테스트", Description: "설명", BodyMD: "# 본문",
		Category: "게임", RedirectURL: "https://example.com",
	})
	if err != nil {
		t.Fatal(err)
	}

	// 승인 전에는 클릭 불가
	if err := addClick(db, id, kstTodayString()); err != errNotFound {
		t.Errorf("승인 전 클릭이 막히지 않음: %v", err)
	}

	// 운영진 둘이 동시에 눌러도 한 번만 반영
	reviewer := Profile{StudentID: "12180001", Name: "운영진"}
	if err := review(db, id, "approved", "", reviewer); err != nil {
		t.Fatalf("첫 승인이 실패함: %v", err)
	}
	if err := review(db, id, "rejected", "마음 바뀜", reviewer); err != errNotFound {
		t.Fatalf("두 번째 처리가 막히지 않음: %v", err)
	}

	items, err := listApprovedProjects(db, "")
	if err != nil {
		t.Fatal(err)
	}
	if len(items) != 1 || items[0].ReviewerName != "운영진" {
		t.Fatalf("승인 목록/리뷰어 기록이 틀림: %+v", items)
	}

	// 클릭 2번 → 오늘자 score 2
	if err := addClick(db, id, kstTodayString()); err != nil {
		t.Fatal(err)
	}
	if err := addClick(db, id, kstTodayString()); err != nil {
		t.Fatal(err)
	}
	if err := recomputeScores(db, kstToday()); err != nil {
		t.Fatal(err)
	}
	p, err := getProject(db, id)
	if err != nil {
		t.Fatal(err)
	}
	if math.Abs(p.Score-2) > 1e-9 || p.TotalClicks != 2 {
		t.Errorf("score=%v totalClicks=%d, want 2/2", p.Score, p.TotalClicks)
	}
}
