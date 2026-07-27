package main

import (
	"bytes"
	"encoding/json"
	"log"
	"net/http"
	"time"
)

// slackNotifier 는 Incoming Webhook 으로 채널에 알림을 보낸다.
// URL 이 비어 있으면(로컬 개발 등) no-op.
type slackNotifier struct {
	url string
}

// notify 는 비동기 발송 — 실패해도 사용자 요청은 실패시키지 않고 로그만 남긴다.
func (n *slackNotifier) notify(text string) {
	if n == nil || n.url == "" {
		return
	}
	go func() {
		body, _ := json.Marshal(map[string]string{"text": text})
		client := &http.Client{Timeout: 10 * time.Second}
		resp, err := client.Post(n.url, "application/json", bytes.NewReader(body))
		if err != nil {
			log.Printf("slack 알림 실패: %v", err)
			return
		}
		defer resp.Body.Close()
		if resp.StatusCode != http.StatusOK {
			log.Printf("slack 알림 실패: status %d", resp.StatusCode)
		}
	}()
}
