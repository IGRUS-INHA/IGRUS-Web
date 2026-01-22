# IGRUS-Web

인하대학교 IGRUS 동아리 웹사이트 프로젝트

## 프로젝트 구조

모노레포 구조를 사용함.

```
IGRUS-Web/
├── backend/     # Spring Boot 백엔드
├── frontend/    # React + Vite 프론트엔드
├── docs/        # 프로젝트 문서 (ADR, 기능 명세 등)
├── specs/       # 기능 스펙 문서
└── README.md
```

### 서브 프로젝트별 가이드

각 서브 프로젝트의 상세한 개발 규칙은 해당 디렉토리의 CLAUDE.md 참조:
- `backend/CLAUDE.md` - 백엔드 개발 규칙, 아키텍처, 테스트 가이드

## 기술 스택

### Backend
- Java 21
- Spring Boot 4.0.1
- Spring Data JPA
- Spring Security
- MySQL 8.x
- Gradle

### Frontend
- React 19
- TypeScript
- Vite 7

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
