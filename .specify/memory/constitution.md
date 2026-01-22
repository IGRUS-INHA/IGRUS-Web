<!--
SYNC IMPACT REPORT
==================
Version change: 1.0.0 → 1.1.0 (MINOR - documentation management section added)
Modified principles: N/A
Added sections:
  - 문서 관리 (Documentation Management) under 기술 표준
  - Updated 프로젝트 구조 to include docs subfolders
Removed sections: N/A
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ (no changes needed - uses specs/ for feature docs)
  - .specify/templates/spec-template.md ✅ (no changes needed)
  - .specify/templates/tasks-template.md ✅ (docs/ reference compatible)
Follow-up TODOs: None
-->

# IGRUS-Web Constitution

## Core Principles

### I. 테스트 우선 개발 (Test-First Development)

모든 기능 구현은 테스트 작성부터 시작한다.

- 테스트 코드 작성 → 테스트 실패 확인 → 구현 → 테스트 통과 (Red-Green-Refactor) 사이클 준수
- 단위 테스트, 통합 테스트, 계약 테스트를 구분하여 작성
- 테스트 없는 기능 PR은 머지 불가
- 테스트 커버리지는 핵심 비즈니스 로직에 집중

**근거**: 버그 조기 발견, 리팩토링 안정성 확보, 코드 품질 보장

### II. 코드 품질 (Code Quality)

깨끗하고 읽기 쉬운 코드를 작성한다.

- 함수와 클래스는 단일 책임 원칙(SRP) 준수
- 변수, 함수, 클래스명은 의도를 명확히 드러내도록 작성
- 중복 코드 최소화 (DRY 원칙)
- 주석은 "왜(Why)"를 설명할 때만 사용, 코드 자체가 "무엇(What)"을 설명
- 린터(ESLint, Checkstyle) 경고 0개 유지

**근거**: 유지보수성 향상, 협업 효율성 증가, 기술 부채 방지

### III. 타입 안전성 (Type Safety)

정적 타입 시스템을 최대한 활용한다.

- **Frontend**: TypeScript strict 모드 사용, `any` 타입 사용 금지
- **Backend**: Java 타입 시스템 활용, null 처리는 Optional 사용
- API 요청/응답 타입은 공유 스키마로 정의
- 런타임 타입 검증은 시스템 경계(API 입력, 외부 데이터)에서만 수행

**근거**: 컴파일 타임 오류 검출, IDE 자동완성 지원, 리팩토링 안정성

### IV. API 계약 우선 (API Contract First)

프론트엔드-백엔드 통신은 명확한 계약에 기반한다.

- API 엔드포인트 변경 전 스펙 문서 업데이트 필수
- RESTful 원칙 준수: 리소스 기반 URL, 적절한 HTTP 메서드 사용
- 에러 응답 형식 표준화
- 버전 관리가 필요한 Breaking change는 사전 공지

**근거**: 프론트엔드-백엔드 독립 개발 가능, 통합 오류 감소

### V. 코드 리뷰 (Code Review)

모든 코드 변경은 리뷰를 거친다.

- PR당 최소 1명의 승인 필요
- 리뷰어는 기능, 테스트, 보안, 성능 측면에서 검토
- 피드백은 구체적이고 건설적으로 작성
- 리뷰 요청 후 24시간 내 최초 리뷰 완료 권장

**근거**: 지식 공유, 버그 조기 발견, 코드 일관성 유지

### VI. 보안 우선 (Security First)

보안은 개발 전 과정에 적용한다.

- 민감 정보(API 키, 비밀번호)는 환경변수로 관리, 코드에 하드코딩 금지
- 사용자 입력은 항상 검증 및 이스케이프 처리
- OWASP Top 10 취약점 방지 (XSS, SQL Injection, CSRF 등)
- 인증/인가 로직은 표준 라이브러리 사용

**근거**: 사용자 데이터 보호, 서비스 안정성, 법적 준수

### VII. 단순성 (Simplicity)

필요한 것만 구현한다.

- YAGNI (You Aren't Gonna Need It): 현재 요구사항만 구현
- 추상화는 3번 반복 후 도입
- 외부 라이브러리 도입 전 필요성 검토
- 복잡한 구조보다 명확한 구조 선호

**근거**: 개발 속도 향상, 유지보수 부담 감소, 기술 부채 방지

## 기술 표준

### 기술 스택

| 구분 | 기술 | 버전 요구사항 |
|------|------|---------------|
| Frontend | React, TypeScript | Node.js LTS |
| Backend | Spring Boot, Java | JDK 17+ |
| 빌드 도구 | npm (Frontend), Gradle (Backend) | - |

### 프로젝트 구조

```
IGRUS-Web/
├── backend/          # Spring Boot 백엔드
│   └── docs/         # 백엔드 관련 문서
├── frontend/         # React 프론트엔드
│   └── docs/         # 프론트엔드 관련 문서
├── docs/             # 프로젝트 공통 문서
│   └── feature/      # 기능별 문서
└── specs/            # 기능 명세 및 구현 계획
```

### 문서 관리 (Documentation Management)

프로젝트 문서는 체계적으로 관리한다.

- **공통 문서**: `docs/` 폴더에서 관리
  - PRD, 아키텍처, 공통 가이드 등 프로젝트 전반에 관한 문서
- **백엔드 문서**: `backend/docs/` 폴더에서 관리
  - API 문서, 데이터베이스 스키마, 백엔드 아키텍처 등
- **프론트엔드 문서**: `frontend/docs/` 폴더에서 관리
  - 컴포넌트 가이드, 스타일 가이드, 프론트엔드 아키텍처 등
- **기능 명세**: `specs/` 폴더에서 관리 (speckit 도구 사용)

**근거**: 문서 위치 일관성, 관심사 분리, 유지보수 용이성

### 코드 스타일

- **Frontend**: ESLint + Prettier 설정 준수
- **Backend**: Google Java Style Guide 준수
- 들여쓰기: 공백 2칸 (Frontend), 공백 4칸 (Backend)

## 개발 워크플로우

### 브랜치 전략

- `main`: 프로덕션 배포 브랜치
- `dev`: 개발 통합 브랜치
- `feature/*`: 기능 개발 브랜치
- `fix/*`: 버그 수정 브랜치

### 커밋 규칙

```
<type>(<scope>): <subject>
```

- **type**: feat, fix, docs, style, refactor, test, chore
- **scope**: frontend, backend, api, auth 등
- **subject**: 명령형 현재 시제, 50자 이내

### PR 프로세스

1. 기능 브랜치에서 작업
2. 테스트 통과 확인
3. PR 생성 및 리뷰 요청
4. 최소 1명 승인 후 머지
5. 머지 후 브랜치 삭제

## Governance

### 헌법 적용

이 헌법은 IGRUS-Web 프로젝트의 모든 코드 변경에 적용된다.

- 모든 PR 리뷰 시 헌법 원칙 준수 여부 확인
- 원칙 위반 시 머지 불가
- 예외 상황은 문서화하고 팀 합의 필요

### 헌법 수정

- 수정 제안은 PR을 통해 진행
- 팀원 과반수 동의 필요
- 수정 시 버전 번호 업데이트
- 모든 수정 이력은 Git 히스토리로 추적

### 버전 관리

- MAJOR: 원칙 삭제 또는 근본적 변경
- MINOR: 새 원칙 추가 또는 섹션 확장
- PATCH: 문구 수정, 오타 수정, 명확화

**Version**: 1.1.0 | **Ratified**: 2026-01-22 | **Last Amended**: 2026-01-22
