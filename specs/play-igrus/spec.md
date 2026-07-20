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
  `/api/v1/mypage/profile` 로 넘겨 응답을 신뢰한다(introspection, 60초 캐시).
- 운영진 = 프로필 role 이 `OPERATOR` 또는 `ADMIN`.

## 작품(project)

제출 항목:

| 필드 | 제약 | 비고 |
|---|---|---|
| 제목 | 1~100자 | |
| 설명 | 1~50자 | 카드에 노출되는 한 줄 소개 |
| 본문 | ≤20,000자, 마크다운 | 상세 다이얼로그에서 렌더링 |
| 리다이렉트 URL | http/https 만 | javascript:/data: 차단 (XSS 방지) |
| 분류 | 1~20자 자유 문자열 | enum 아님(스펙). 프론트는 게임/앱/웹/기타 제안 |
| 정방형 썸네일 | 선택, PNG/JPEG/WebP ≤4MB | 없으면 분류 색 플레이스홀더 |
| 배너 | 선택, PNG/JPEG/WebP ≤4MB | 상세 상단. 없으면 분류 색 그라데이션 |

- 한 사람이 여러 작품 등록 가능.
- 제출 → `pending` → 운영진 승인(`approved`) / 반려(`rejected`, 사유 선택).
- **승인/반려한 운영진 정보(학번·이름)를 기록**한다.
- 승인/반려는 `pending` 상태에서 1회만 반영 (동시 처리 가드).

## 랭킹 (인기순)

- "이동하기" 클릭 시 `POST /api/projects/{id}/click` → 일별(KST) 버킷 + 총합 집계.
- 점수: `score = Σ(일별 클릭 × 0.5^(경과일 / 7))` — 반감기 7일.
  최근 클릭이 많을수록 상위, 오래된 인기작은 서서히 하락.
- 매번 연산하지 않고 **시작 시 1회 + 매일 04:00 KST 배치**로 재계산해 `projects.score` 에 캐싱.
- 기본 정렬: `score DESC, created_at DESC`.

## API

| 메서드/경로 | 인증 | 설명 |
|---|---|---|
| `GET /api/projects?category=` | 공개 | 승인작 목록 (인기순) |
| `GET /api/projects/{id}` | 공개 | 상세 (배너·본문·리다이렉트 URL 포함) |
| `POST /api/projects/{id}/click` | 공개 | 클릭 집계 (204) |
| `POST /api/projects` | 로그인 | multipart 제출 → pending |
| `GET /api/projects/mine` | 로그인 | 내 제출 현황 (반려 사유 포함) |
| `GET /api/admin/projects?status=` | 운영진 | 검수 목록 (기본 pending) |
| `POST /api/admin/projects/{id}/approve` | 운영진 | 승인 (리뷰어 기록) |
| `POST /api/admin/projects/{id}/reject` | 운영진 | 반려 (`{reason}` 선택) |
| `GET /images/{key}` | 공개 | S3 이미지 프록시 (1년 immutable 캐시) |
| `GET /healthz` | 공개 | 헬스체크 `{"status":"ok"}` |
| `GET /*` | 공개 | SPA 정적 서빙 (없는 경로는 index.html 폴백, /assets/* 는 장기 캐시) |

에러 응답: `{"error": "메시지"}`. OpenAPI codegen 은 미적용(단일 클라이언트) — 이 표가 계약.

## 화면 (모바일 퍼스트)

- `/` 메인: 카드 그리드(모바일 2열), 분류 필터 칩. 카드 탭 → 모바일 풀스크린 시트 /
  데스크톱 다이얼로그(native `<dialog>`): 배너·제목·작성자·마크다운 본문(스크롤)·이동하기.
- `/login`: 학번(8자리)+비밀번호. 회원가입은 www 링크.
- `/submit`: RHF 제출 폼 (설명 50자 카운터, 이미지 미리보기).
- `/my`: 내 제출 상태 (심사중/승인됨/반려됨+사유).
- `/admin`: 운영진 검수 (대기/승인/반려 탭, 승인·반려 버튼).

## 배포/인프라

- 단일 Docker 이미지(`play-igrus/Dockerfile`): node 스테이지에서 프론트 빌드
  (`VITE_IGRUS_API_URL=https://api.igrus.co.kr`, play API 는 같은 origin 상대경로) →
  Go 바이너리 + `/web`(SPA) 를 distroless 에 담음.
- CDK(`infra/cdk/lib/igrus-web-v2-stack.ts`): `play` A레코드(EIP), Caddy 사이트 블록
  (`play.igrus.co.kr → play-api:8080`), compose `play-api` 서비스,
  시크릿 `igrus/play/server/prod`(JSON `{"dsn": "..."}`) grantRead.
  ECR repo(`igrus/play/server`)는 spring repo 와 동일하게 CDK 밖에서 생성·URL 참조.
- CD: `.github/workflows/play-prod-cd.yaml` — `play-v*` 태그 push → Go test → ECR push →
  SSM `igrus-deploy <tag> play-api` → `/healthz` 헬스체크.
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
