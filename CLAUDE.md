# IGRUS-Web

인하대학교 IGRUS 동아리 웹사이트 프로젝트

## 프로젝트 구조
SOLID원칙 지키기, 성급한 추상화 금지

모노레포 구조를 사용함.

```
IGRUS-Web/
├── backend/     # Spring Boot 백엔드
├── frontend/    # React + Vite 프론트엔드
├── openapi/     # OpenAPI 스펙 (단일진실점 - 프론트/백 공유)
├── docs/        # 프로젝트 문서 (ADR, 기능 명세 등)
├── specs/       # 기능 스펙 문서
└── README.md
```

### 서브 프로젝트별 가이드

각 서브 프로젝트의 상세한 개발 규칙은 해당 디렉토리의 CLAUDE.md 참조:

- `frontend/CLAUDE.md` - 프론트엔드 개발 규칙, TypeScript, React 가이드
- `backend/CLAUDE.md` - 백엔드 개발 규칙, 아키텍처, 테스트 가이드

## 기술 스택

### Backend

- Java 21
- Spring Boot 3.5.9
- Spring Data JPA
- Spring Security
- MySQL 8.x
- Gradle

### Frontend

- React 19
- TypeScript
- Vite 7
- React Router DOM (라우팅)
- Zustand (상태관리)
- Orval + TanStack Query (API 클라이언트)

## 커밋 규칙

### 커밋 메시지 형식

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type

- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `style`: 코드 포맷팅, 세미콜론 누락 등 (코드 변경 없음)
- `refactor`: 코드 리팩토링 (기능 변경 없음)
- `test`: 테스트 코드 추가/수정
- `chore`: 빌드, 설정 파일 수정 등 기타 변경

### Scope (선택사항)

변경된 부분을 명시: `frontend`, `backend`, `api`, `auth` 등

### Subject

- 명령형 현재 시제 사용 (예: "변경한다" 대신 "변경")
- 첫 글자 소문자
- 마침표 없음
- 50자 이내

### 예시

```
feat(frontend): 로그인 페이지 추가
fix(backend): 사용자 인증 토큰 만료 버그 수정
docs: README에 설치 방법 추가
refactor(api): 유저 서비스 코드 정리
```

### 기타

- 커밋과 PR 시, Co-Authored-By 를 제외할 것

### Claude 커밋 프로세스 (필수)

**커밋 전 반드시 사용자에게 커밋 메시지 확인 받을 것**

1. **포맷팅 & 린팅 실행 (필수)** — 프론트엔드 변경 시 `cd frontend && pnpm format && pnpm lint`
2. `git status`, `git diff`로 변경사항 확인
3. 커밋 메시지 초안 작성
4. **사용자에게 커밋 메시지 확인 요청 (필수)**
5. 사용자 승인 후 `git add` 및 `git commit` 실행
6. `git status`로 커밋 완료 확인

**중요:** 사용자 승인 없이 절대 커밋하지 말 것

### Claude PR 프로세스 (필수)

**PR 생성 전 반드시 사용자에게 제목과 본문 확인 받을 것**

1. `git log`, `git diff`로 PR에 포함될 변경사항 파악
2. PR 제목과 본문 초안 작성
3. **사용자에게 PR 제목/본문 확인 요청 (필수)**
4. 사용자 승인 후 `gh pr create` 실행

**중요:** 사용자 승인 없이 절대 PR을 생성하지 말 것

### Claude 이슈 생성 프로세스 (필수)

**이슈 생성 전 반드시 사용자에게 제목과 본문 확인 받을 것**

1. `gh issue list`로 기존 이슈 확인하여 중복 여부 검토
2. 기존 이슈의 양식(제목 태그, 본문 구조)에 맞추어 초안 작성
3. **사용자에게 이슈 제목/본문 확인 요청 (필수)**
4. 사용자 승인 후 `gh issue create` 실행

**중요:** 사용자 승인 없이 절대 이슈를 생성하지 말 것

## 문서화

- 코드 수정 이후에는 항상 문서화를 할 것.
- 이미 작성되어 있는 문서를 찾고, 그 문서를 업데이트 할 것.
- 문서화 과정에서는 speckit을 적절하게 사용할 것.
- 모든 문서는 항상 최신 상태를 유지할 것.
- 연관된 문서의 상태를 잘 반영하고 있는지 체크할 것.
