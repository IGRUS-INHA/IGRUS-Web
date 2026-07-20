package main

import (
	"database/sql"
	"log"
	"math"
	"time"
	_ "time/tzdata" // 컨테이너에 tzdata 없어도 Asia/Seoul 로드 가능
)

var kst = mustKST()

func mustKST() *time.Location {
	loc, err := time.LoadLocation("Asia/Seoul")
	if err != nil {
		panic(err)
	}
	return loc
}

// 인기 점수: score = Σ(일별 클릭 × 0.5^(경과일 / 반감기)).
// 최근 클릭일수록 가치가 높고, 7일 지나면 절반이 된다.
const halfLifeDays = 7.0

func decay(daysAgo float64) float64 {
	return math.Pow(0.5, daysAgo/halfLifeDays)
}

// kstToday 는 KST 기준 오늘 날짜를 UTC 자정 time 으로 돌려준다.
// click_date(DATE) 는 parseTime 스캔 시 UTC 자정으로 오므로 이 기준과 뺄셈하면 정수 일수가 된다.
func kstToday() time.Time {
	y, m, d := time.Now().In(kst).Date()
	return time.Date(y, m, d, 0, 0, 0, 0, time.UTC)
}

func kstTodayString() string {
	return time.Now().In(kst).Format("2006-01-02")
}

// recomputeScores 는 전체 일별 클릭을 읽어 모든 프로젝트의 score 를 재계산한다.
// ponytail: 전체 스캔 + 전체 UPDATE — 프로젝트 수백 건 규모에 충분. 느려지면 증분 계산.
func recomputeScores(db *sql.DB, today time.Time) error {
	rows, err := db.Query(`SELECT project_id, click_date, clicks FROM project_clicks_daily`)
	if err != nil {
		return err
	}
	defer rows.Close()

	scores := map[int64]float64{}
	for rows.Next() {
		var id, clicks int64
		var date time.Time
		if err := rows.Scan(&id, &date, &clicks); err != nil {
			return err
		}
		daysAgo := today.Sub(date).Hours() / 24
		if daysAgo < 0 {
			daysAgo = 0
		}
		scores[id] += float64(clicks) * decay(daysAgo)
	}
	if err := rows.Err(); err != nil {
		return err
	}

	tx, err := db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()
	if _, err := tx.Exec(`UPDATE projects SET score = 0 WHERE score <> 0`); err != nil {
		return err
	}
	for id, s := range scores {
		if _, err := tx.Exec(`UPDATE projects SET score = ? WHERE id = ?`, s, id); err != nil {
			return err
		}
	}
	return tx.Commit()
}

// startRankingLoop 은 시작 시 1회 + 매일 04:00 KST 에 점수를 재계산한다.
func startRankingLoop(db *sql.DB) {
	go func() {
		for {
			if err := recomputeScores(db, kstToday()); err != nil {
				log.Printf("랭킹 재계산 실패: %v", err)
			}
			time.Sleep(untilNextRun())
		}
	}()
}

func untilNextRun() time.Duration {
	now := time.Now().In(kst)
	next := time.Date(now.Year(), now.Month(), now.Day(), 4, 0, 0, 0, kst)
	if !next.After(now) {
		next = next.Add(24 * time.Hour)
	}
	return next.Sub(now)
}
