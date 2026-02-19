# 프론트엔드 미완성 작업 목록

**작성일**: 2026-02-05
**최종 업데이트**: 2026-02-19
**남은 작업**: 3개 (부분 완료 1개, 미완료 2개)

> **2026-02-19 세션 완료**: EventWritePage, EventEditPage 디자인 리뉴얼 완료 (한국어 라벨, 단일 카드 레이아웃, sticky 상단바). `registrationStartAt` 하드코딩 제거 — 신청 가능 기간(시작~마감) UI 추가. `alert()` → Swal 전환은 별도 P2 태스크로 유지.

---

## 목차

- [1. 차단형 다이얼로그 통일]
- [2. 반응형 레이아웃 글씨 줄바꿈 개선]
- [3. E2E 테스트 환경 구축-playwright]
- [남은 작업 요약](#남은-작업-요약)
- [체크리스트](#체크리스트)

---

## P2 - Medium

### 1. 차단형 다이얼로그 통일 (`alert()` → `Swal.fire()`) 🔧 (부분 완료)

**상태**: 일부 페이지만 SweetAlert2 전환 완료 / `alert()` 11개 파일 잔존

**현재 상태**

차단형 다이얼로그(사용자 확인/입력이 필요한 알림)가 2가지 혼재:

- `Swal.fire()`: LoginPage, ForgotPasswordPage, ChangePasswordPage (3개 파일) ← 전환 완료
- `alert()`: PostDetailPage, PostWritePage, PostEditPage, SignupPage, InquiryPage, EventWritePage, EventEditPage, EventDetailPage, VerifyEmailPage, CommentSection, CommentItem (11개 파일)

> **참고**: 비차단 알림(성공/실패 피드백)은 `useToast()` / `addToast()`로 이미 통일되어 있음 (관리자 탭 5개, ProtectedRoute, useComments). Toast는 별도 시스템으로 유지.

**남은 작업**

- `alert()` 호출 11개 파일을 `Swal.fire()`로 교체
- 필요 시 `confirm()` → `Swal.fire({ showCancelButton: true })` 교체

**작업 규모**: 🟡 중 (3시간)

**관련 파일**

- `alert()` 사용 파일 11개 (위 목록 참조)

---

### 2. 반응형 레이아웃 글씨 줄바꿈 개선

**현재 문제**

- 모바일/태블릿에서 긴 텍스트가 레이아웃을 깨뜨림
- 테이블 셀에서 텍스트 오버플로우
- 한글 단어 중간에서 끊김

**작업 규모**: 🟡 중 (2시간)

**세부 작업 항목**

1. 전역 CSS 유틸리티 추가 (`frontend/src/index.css`)
   - `word-break-keep` (한글 줄바꿈 최적화)
   - `word-break-all` (영문 줄바꿈)
   - `truncate`, `line-clamp-2` (말줄임)

2. 적용 대상 컴포넌트
   - 게시글 제목: `.truncate` 또는 `.line-clamp-2`
   - 게시글/댓글/행사 내용: `.word-break-keep`
   - 테이블 셀 (긴 텍스트): `.truncate` + 툴팁

3. 반응형 테스트
   - 모바일 (375px, 390px)
   - 태블릿 (768px, 1024px)
   - 데스크톱 (1280px, 1920px)

**관련 파일**

- `frontend/src/index.css`
- `frontend/src/pages/board/BoardListPage.tsx`
- `frontend/src/pages/board/PostDetailPage.tsx`
- `frontend/src/pages/event/EventListPage.tsx`
- `frontend/src/pages/admin/*.tsx` (테이블 있는 모든 페이지)

---

## P3 - Low

### 3. E2E 테스트 환경 구축 (Playwright)

**목표**

- Playwright로 E2E 테스트 자동화 기반 마련
- CI/CD 파이프라인 통합
- 주요 사용자 플로우 테스트 커버리지 확보

**작업 규모**: 🔴 대 (6시간)

**세부 작업 항목**

1. Playwright 설치 및 설정 (`playwright.config.ts`)
2. 테스트 디렉토리 구조 생성 (`frontend/tests/e2e/`)
3. 우선 테스트 구현 (회원가입, 로그인, 게시글 작성)
4. 테스트 헬퍼 함수 작성
5. CI/CD 통합 (GitHub Actions)
6. `package.json` 스크립트 추가

**관련 파일**

- `playwright.config.ts` (신규)
- `frontend/tests/e2e/**/*.spec.ts` (신규)
- `.github/workflows/e2e-test.yml` (신규)
- `package.json` (스크립트 추가)

---

## 남은 작업 요약

| 작업                                | 우선순위 | 예상 시간  |
| ----------------------------------- | -------- | ---------- |
| 차단형 다이얼로그 통일 (alert→Swal) | P2       | 3시간      |
| 반응형 글씨 줄바꿈                  | P2       | 2시간      |
| E2E 테스트 환경 구축                | P3       | 6시간      |
| **총계**                            |          | **11시간** |

---

## 체크리스트

- [~] 차단형 다이얼로그 통일 (부분 완료 - 3개 파일 Swal 전환, alert() 11개 파일 잔존)
- [ ] 반응형 글씨 줄바꿈
- [ ] E2E 테스트 환경

---

**마지막 업데이트**: 2026-02-19 (EventWritePage/EditPage 디자인 리뉴얼 완료)
