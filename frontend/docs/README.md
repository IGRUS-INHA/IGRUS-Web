# Frontend Docs - 내부 개발 문서

> **📝 문서 목적**: 이 폴더는 프론트엔드 개발자를 위한 **내부 개발 문서**입니다.
> 실제 구현, 아키텍처 설계, 마이그레이션 기록 등 개발에 필요한 상세 정보를 포함합니다.

## 📂 폴더 구조

### `architecture/` - 아키텍처 설계
프론트엔드 아키텍처 관련 다이어그램 및 설계 문서

| 문서 | 설명 |
|------|------|
| [auth-flow-diagram.md](./architecture/auth-flow-diagram.md) | 인증 흐름 다이어그램 |
| [architecture-review.md](./architecture/architecture-review.md) | 아키텍처 리뷰 및 개선 우선순위 |

### `design/` - 디자인 시스템
UI 컴포넌트 및 디자인 규칙

| 문서 | 설명 |
|------|------|
| [component-spec.md](./design/component-spec.md) | 컴포넌트 명세 및 사용 가이드 |
| [design-system.md](./design/design-system.md) | IGRUS 디자인 시스템 (색상, 타이포그래피, 컴포넌트) |

### `migration/` - 마이그레이션 기록
TypeScript 마이그레이션 및 기타 전환 작업 가이드

| 문서 | 설명 |
|------|------|
| [checklist.md](./migration/checklist.md) | 마이그레이션 체크리스트 |
| [phase-plan.md](./migration/phase-plan.md) | 단계별 마이그레이션 계획 |
| [typescript-migration-checklist.md](./migration/typescript-migration-checklist.md) | TypeScript 마이그레이션 상세 체크리스트 |
| [typescript-migration-guide.md](./migration/typescript-migration-guide.md) | TypeScript 마이그레이션 가이드 |
| [auth-inquiries-orval-migration.md](./migration/auth-inquiries-orval-migration.md) | Auth & Inquiries API Orval 마이그레이션 |
| [orval-api-migration.md](./migration/orval-api-migration.md) | Posts API Orval 마이그레이션 |

### `testing/` - E2E 테스트 가이드
Playwright 자동화 테스트 및 수동 브라우저 테스트 시나리오

각 파일은 해당 기능의 **Playwright 자동화 테스트**와 **수동 브라우저 테스트** 시나리오를 포함합니다.

| 문서 | 설명 | 상태 |
|------|------|------|
| [auth-test-guide.md](./testing/auth-test-guide.md) | 인증 (회원가입, 이메일 인증, 로그인) | ✅ |
| [inquiries-test-guide.md](./testing/inquiries-test-guide.md) | 문의 (문의 목록 조회, 문의 작성) | ✅ |
| [posts-test-guide.md](./testing/posts-test-guide.md) | 게시판 (게시글 CRUD, 좋아요) | ✅ |
| events-test-guide.md | 이벤트 | ⏸️ 예정 |
| admin-test-guide.md | 관리자 | ⏸️ 예정 |

**빠른 시작**:
```bash
# Playwright 설치
pnpm add -D @playwright/test
npx playwright install

# 테스트 실행
npx playwright test              # 모든 테스트
npx playwright test --ui         # UI 모드 (디버깅)
npx playwright test --headed     # 브라우저 표시

# 수동 브라우저 테스트
pnpm dev                         # 개발 서버 실행
# 브라우저에서 http://localhost:5173 접속
# 각 기능별 가이드의 시나리오 따라 테스트
```

**테스트 문서 작성 규칙**:
- 새로운 기능 추가/수정 시 해당 기능의 테스트 가이드 파일에 시나리오 추가
- 수동 브라우저 테스트 섹션: 사용자가 직접 수행할 단계별 시나리오
- Playwright 자동화 테스트 섹션: Page Object 패턴 및 테스트 코드 예시
- 각 시나리오는 **예상 결과**, **Network 탭 확인**, **Console 탭 확인** 포함

## 🛠 기술 스택

- React 19
- TypeScript
- Vite 7
- React Router DOM (라우팅)
- Zustand (상태관리)
- Orval + TanStack Query (API 클라이언트)
- Tailwind CSS
- shadcn/ui 컴포넌트

## 📌 문서 작성 가이드

- **내부용 문서**이므로 작업 중인 내용, 미완성 문서도 자유롭게 작성
- 구현 상세, 트러블슈팅, 작업 노트 등 실용적인 정보 우선
- 외부 공개용 문서는 루트의 `docs/` 폴더에 별도 관리
