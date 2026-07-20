# Play IGRUS 스펙

> 원본 아이디어 메모(`play-server/playIgrus.md`)를 구현 확정안으로 정리한 문서.

## 개요

IGRUS 가입자(동아리 비회원 포함 — 인하대생 누구나)가 본인의 작품(앱/게임/웹 등)을 올리고,
운영진 승인 후 메인 화면에 전시되는 서비스.

- `play.igrus.co.kr` 하나로 SPA+API 서빙 — Go 서버(`play-igrus/backend`)가 프론트 빌드
  산출물(`play-igrus/frontend` → `/web`)을 정적 서빙. 같은 origin 이라 CORS 불필요.
  기존 EC2+Caddy 호스트에 단일 컨테이너 추가.
- DB: 기존 prod RDS(`igrus-web-mysql-rds-v2`)의 별도 database `play_igrus`
- 이미지: 기존 S3 버킷(`igrus-web-file-storage-bucket-v2`)의 `play/` prefix
- 80% 모바일 접속 → 모바일 퍼스트 UI

## 인증 (SSO)

- `*.igrus.co.kr` 공유 refresh 쿠키(`Domain=igrus.co.kr`, httpOnly)로 www 와 세션 공유.
  - play 에서 로그인 → www 도 로그인됨 (www 는 hydration 시 silent refresh 부트스트랩,
    `frontend/src/stores/authStore.ts`의 `onHydrated` → `restoreSessionFromCookie`)
  - www 에서 로그인 → play 는 앱 시작 시 `bootstrapAuth()` 로 복원
- play 백엔드는 **JWT 를 직접 검증하지 않는다.** HMAC 시크릿을 공유하면 play 서버가
  igrus-web 전체 토큰을 위조할 수 있기 때문. 대신 `Authorization` 헤더를 기존 백엔드
  `/api/v1/mypage/profile` 로 넘겨 응답을 신뢰한다(introspection).
- **캐시 정책** — 세 축이 서로 다르다:
  - 인증(토큰→신원): 프로세스 메모리 60초 캐시. 표시 데이터가 아니라 신원이라 캐시해도
    "고쳤는데 안 바뀐다"가 없고, 없으면 로그인 요청마다 igrus 왕복이 붙는다.
    권한 회수는 최대 60초 늦게 반영(감수). igrus 직결(`http://app:8080`)이라 왕복 자체는 싸다.
  - 공개 프로필(닉네임/소개/링크): **캐시 안 함.** DB 조인이라 수정이 곧바로 반영된다.
    (이전엔 5분 캐시라 프로필을 고쳐도 메인에 옛 값이 남는 버그가 있었다.)
  - 랭킹(score): `ranking.go` 가 `score` 컬럼에 미리 계산 — 매 요청 재계산하면 비싼 유일한 값.
- 운영진 = 프로필 role 이 `OPERATOR` 또는 `ADMIN`.

## 작품(project)

제출 항목:

| 필드 | 제약 | 비고 |
|---|---|---|
| 제목 | 1~100자 | |
| 설명 | 1~50자 | 카드에 노출되는 한 줄 소개 |
| 본문 | ≤20,000자, 마크다운 | 상세 다이얼로그에서 렌더링 |
| 리다이렉트 URL | http/https 만 | javascript:/data: 차단 (XSS 방지) |
| 분류 | 서버 고정 목록 | 현재 **게임 / 앱** (`backend/projects.go` `allowedCategories`) |
| 정방형 썸네일 | **필수**, PNG/JPEG/WebP ≤4MB | 카드 이미지 |
| 배너 | 선택, PNG/JPEG/WebP ≤4MB | 상세 상단. 없으면 분류 색 그라데이션 |

- 한 사람이 여러 작품 등록 가능.
- 제출 → `pending` → 운영진 승인(`approved`) / 반려(`rejected`, 사유 선택).
- **승인/반려한 운영진 정보(학번·이름)를 기록**한다.
- 승인/반려는 `pending` 상태에서 1회만 반영 (동시 처리 가드).
- 작성자 표시는 **닉네임** (igrus `users` 테이블). 닉네임이 없으면 이름.
  play·igrus 는 **같은 RDS 인스턴스**라, play 가 `igrus_web.users` 를 크로스 스키마로
  직접 조회한다 (HTTP 왕복 없음, 목록 전체를 학번 `IN (...)` 한 쿼리로). 닉네임 폴백
  (`COALESCE(NULLIF(nickname,''), name)`)·탈퇴 제외(`users_deleted`)를 SQL 로 적용하고,
  표시 이름만 응답에 싣는다(실명 비노출). 학번·입학년도는 비공개.
  이름의 진실점은 이 조인뿐 — 조회 실패나 탈퇴 회원이면 이름은 빈 값(스냅샷 폴백 없음).
  조회가 실패해도 목록 자체는 항상 뜬다.
  - play DB 유저에 `igrus_web.users` 의 **필요한 컬럼만** SELECT 권한 부여
    (student_id·name·nickname·introduction·links·deleted). 이메일·전화·비밀번호 해시는
    권한 밖 — play 침해 시 회원 PII 유출을 막는 마지막 경계다.
  - 스키마명은 `IGRUS_DB_NAME` env (기본 `igrus_web`).
- 작성자 탭(카드/상세) → **개발자 프로필 다이얼로그** (커피챗·포트폴리오 용도):
  프로필 사진·표시 이름·출시작 수·자기소개·링크 칩, 그 아래 출시작 목록
  (썸네일 + 작품명 + 분류 뱃지 + 출시일, 탭 → 해당 작품 다이얼로그).
  승인작만 노출 (`GET /api/projects/{id}/author` — 학번 비노출).
- **클릭수는 어떤 API 응답에도 싣지 않는다** (본인·운영진 포함 서버단 제공 금지) —
  랭킹 정렬 입력으로만 쓰인다.

### 버전 (수정·재승인)

- 제출 1건 = 버전 1개 (`project_versions`, v1 = 최초 제출) — **append-only 이력**.
  승인/반려된 버전은 지워지지 않고 검수 탭(승인/반려)에 v1, v2, … 전부 남는다.
- `projects` 행 = 라이브 스냅샷. `projects.version` = 라이브 버전 번호 (0 = 미승인).
- 본인 작품은 `/my` 에서 수정 가능 (`PUT /api/projects/{id}`).
  - 최신 버전이 심사 전(`pending`)이면 그 버전의 내용만 교체 (버전 번호 유지).
  - 심사가 끝난 버전 뒤에는 `v(N+1)` 로 새 버전이 쌓인다 → 재승인 필요.
  - **승인작**: 새 버전이 승인될 때까지 라이브(기존 버전) 유지. 승인 시 라이브 반영
    (클릭수·score 유지), 반려 시 이력에만 기록. 배너 제거는 미지원(교체만).
  - **미승인작**(심사중/반려): 프로젝트 행도 최신 내용으로 동기화되고 다시 심사 대기.
- `/my` 는 라이브 버전(vN 뱃지)과 심사중/반려된 수정 버전을 함께 보여준다.
- 구 스키마 이관은 부팅 시 idempotent migrate: `projects.version`·`hidden` 컬럼 추가,
  기존 행 v1 백필, 구 `project_revisions` → 다음 버전 이관 후 테이블 제거.

### 프로필 사진

- 닉네임·자기소개·링크의 주인은 igrus `users` 테이블이지만, **사진만 play 가 관리**한다
  (이미지 저장 파이프라인(S3 `play/` prefix + `/images/{key}` 프록시)이 여기 있어서).
- play DB `profile_avatars(student_id PK, image_key, updated_at)` — 학번 1개당 1장.
- 교체 시 이전 이미지는 S3 에 남긴다 (키가 랜덤이라 재사용되지 않고, 용량이 미미).

### 공개/비공개 (작성자 토글)

- 작성자가 본인 **승인작**을 공개 목록에서 내리거나 다시 올릴 수 있다
  (`PUT /api/projects/{id}/visibility`, body `{"hidden": true|false}`).
- `projects.hidden` — 심사 상태(status)와 **별개 축**. 숨겨도 승인 상태·클릭수·score 는
  유지되고, 재공개는 재심사 없이 즉시 반영된다.
- 숨김 중에는 메인 목록·상세·작성자 다이얼로그의 승인작 목록에서 제외되고 클릭 집계도
  차단된다 (score 오염 방지). 랭킹 배치는 그대로 돈다.
- `/my` 에서는 계속 보이며 "비공개" 뱃지 + 숨기기/공개하기 버튼(확인 다이얼로그) 표시.

## 랭킹 (인기순)

- "이동하기" 클릭 시 `POST /api/projects/{id}/click` → 일별(KST) 버킷 + 총합 집계.
- 점수: `score = Σ(일별 클릭 × 0.5^(경과일 / 7))` — 반감기 7일.
  최근 클릭이 많을수록 상위, 오래된 인기작은 서서히 하락.
- 매번 연산하지 않고 **시작 시 1회 + 매일 04:00 KST 배치**로 재계산해 `projects.score` 에 캐싱.
- 정렬은 **서버가 한다** (`?sort=popular|recent`) — popular(기본): `score DESC,
  created_at DESC`, recent: `created_at DESC`. 메인에 인기순/최신순 필터 칩.

## API

| 메서드/경로 | 인증 | 설명 |
|---|---|---|
| `GET /api/projects?category=&sort=` | 공개 | 승인작 목록 (sort: popular 기본 / recent) |
| `GET /api/projects/{id}` | 공개 | 상세 (배너·본문·리다이렉트 URL 포함) |
| `GET /api/projects/{id}/author` | 공개 | 작성자 공개 프로필 (사진·표시 이름·자기소개·링크·승인작 목록, 학번 비노출) |
| `POST /api/projects/{id}/click` | 공개 | 클릭 집계 (204) |
| `POST /api/projects` | 로그인 | multipart 제출 → pending |
| `PUT /api/projects/{id}` | 로그인(본인) | 수정 — 승인작은 수정본 대기, 그 외 즉시 반영 후 pending |
| `PUT /api/projects/{id}/visibility` | 로그인(본인) | 승인작 공개/비공개 토글 (`{"hidden": bool}`) |
| `GET /api/projects/mine` | 로그인 | 내 제출 현황 (반려 사유·수정본 상태·`hidden` 포함) |
| `GET /api/profile/avatar` | 로그인 | 내 프로필 사진 URL (`{"avatarUrl": ""}` = 미설정) |
| `PUT /api/profile/avatar` | 로그인 | 프로필 사진 업로드 (multipart `image`, 4MB·PNG/JPG/WebP) |
| `DELETE /api/profile/avatar` | 로그인 | 프로필 사진 삭제 (204) |
| `GET /api/admin/projects?status=` | 운영진 | 검수 목록 — 버전 이력 단위 (기본 pending) |
| `POST /api/admin/projects/{id}/approve` | 운영진 | 승인 — 신규는 공개, 수정 요청은 라이브 반영 |
| `POST /api/admin/projects/{id}/reject` | 운영진 | 반려 (`{reason}` 선택) — 수정 요청은 라이브 유지 |
| `GET /images/{key}` | 공개 | S3 이미지 프록시 (1년 immutable 캐시) |
| `GET /healthz` | 공개 | 헬스체크 `{"status":"ok"}` |
| `GET /*` | 공개 | SPA 정적 서빙 (없는 경로는 index.html 폴백, /assets/* 는 장기 캐시) |

에러 응답: `{"error": "메시지"}`. OpenAPI codegen 은 미적용(단일 클라이언트) — 이 표가 계약.

## 화면 (모바일 퍼스트)

- `/` 메인: 카드 그리드(모바일 2열), 인기순/최신순 정렬 칩 (분류 필터 없음 — API 의
  `?category=` 는 유지). 카드에 분류 뱃지(앱/게임) + 작성자(탭 → 프로필 다이얼로그).
  카드 탭 → 모바일 풀스크린 시트 /
  데스크톱 다이얼로그(native `<dialog>`): 배너·제목·작성자(탭 → 프로필)·마크다운
  본문(스크롤)·이동하기. 배너는 썸네일 위에 로드 완료 후 페이드인(플리커 방지).
- `/login`: 학번(8자리)+비밀번호. 회원가입은 www 링크.
- `/submit`: RHF 제출 폼 (설명 50자 카운터, 이미지 드래그 앤 드롭·미리보기, 필수 뱃지).
- `/edit/{id}`: 제출 폼 재사용 — 대기 중 수정본이 있으면 그 내용을 프리필.
- `/profile`: 내 정보 —
  - 학번·이름: **읽기 전용** (동아리 가입 정보. 틀리면 운영진 문의 안내).
  - 프로필 사진: 드롭존으로 고르면 즉시 업로드 (저장 버튼과 무관), 지우기 버튼.
    play DB `profile_avatars` 에 학번→이미지 키로 저장 (이미지는 S3 `play/` prefix).
  - 닉네임/자기소개/링크 +·🗑: igrus `users` 테이블에 저장
    (`PATCH /api/v1/mypage/profile` 직접 호출, 저장 버튼).
- `/my`: 내 제출 상태 (심사중/승인됨/반려됨+사유, 수정 심사중/수정 반려 표시, 수정 버튼,
  승인작 숨기기/공개하기 버튼 + 비공개 뱃지).
- 헤더: 출시하기(+운영진 검수)와 프로필 아바타(스켈레톤) — 아바타 탭 →
  내 정보 / 내 작품 / 로그아웃 메뉴 (내 작품·로그아웃 버튼은 메뉴로 이동).
- `/admin`: 운영진 검수 (대기/승인/반려 탭, 승인·반려 버튼).

## 배포/인프라

- 단일 Docker 이미지(`play-igrus/Dockerfile`): node 스테이지에서 프론트 빌드
  (`VITE_IGRUS_API_URL=https://api.igrus.co.kr`, play API 는 같은 origin 상대경로) →
  Go 바이너리 + `/web`(SPA) 를 distroless 에 담음.
- CDK(`infra/cdk/lib/igrus-web-v2-stack.ts`): `play` A레코드(EIP), Caddy 사이트 블록
  (`play.igrus.co.kr → play-api:8080`), compose `play-api` 서비스,
  시크릿 `igrus/play/server/prod`(JSON `{"dsn": "..."}`) grantRead.
  ECR repo(`igrus/play/server`)는 spring repo 와 동일하게 CDK 밖에서 생성·URL 참조.
- CD: `.github/workflows/play-prod-cd.yaml` — `main` push(`play-igrus/**` 변경 시) →
  Go test → ECR push(`play-<sha>`) → SSM `igrus-deploy <tag> play-api` → `/healthz` 헬스체크.
- 수동 작업 절차: `docs/infra/ec2-migration-runbook.md` 의 play 섹션 참조.

## 로컬 개발

```bash
# 백엔드 (로컬 MySQL 필요, S3 미설정 시 ./data/images 디스크 저장)
cd play-igrus/backend
DSN='root:root@tcp(127.0.0.1:3306)/play_igrus?parseTime=true' go run .

# 프론트 (vite proxy: /api→localhost:8080, /igrus-api→staging api)
cd play-igrus/frontend
pnpm dev

# 테스트
go test ./...                      # 단위
TEST_DSN='...' go test ./...       # MySQL 통합 포함
```
