package main

import (
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestSlackNotify(t *testing.T) {
	got := make(chan string, 1)
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		b, _ := io.ReadAll(r.Body)
		got <- string(b)
	}))
	defer ts.Close()

	(&slackNotifier{url: ts.URL}).notify("테스트 알림")

	select {
	case body := <-got:
		var m map[string]string
		if err := json.Unmarshal([]byte(body), &m); err != nil || m["text"] != "테스트 알림" {
			t.Fatalf("잘못된 페이로드: %s", body)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("알림 요청이 오지 않았습니다")
	}
}

func TestSlackNotifyNoop(t *testing.T) {
	// nil 수신자와 빈 URL 은 패닉 없이 no-op 이어야 한다 (테스트/로컬 환경)
	var n *slackNotifier
	n.notify("x")
	(&slackNotifier{}).notify("x")
}
