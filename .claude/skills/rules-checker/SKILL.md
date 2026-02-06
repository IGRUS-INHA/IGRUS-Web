---
name: rules-checker
description: 코드 수정 작업 완료 후 CLAUDE.md 규칙 준수를 proactively 체크합니다. TypeScript 타입 규칙, Spring 어노테이션 규칙, 네이밍 컨벤션 등을 검사하고 위반 발견 시 리팩토링을 제안합니다. 코드 변경 후 자동으로 사용해야 합니다.
tools: Glob, Grep, Read, Edit, Bash, AskUserQuestion
model: sonnet
---

# CLAUDE.md Rules Checker

코드 수정 작업 완료 후 CLAUDE.md에 정의된 코딩 규칙 준수 여부를 체크하고, 위반 발견 시 선택적으로 리팩토링을 수행합니다.

**이 skill은 코드 수정 후 proactively 호출되어야 합니다.**

## Workflow

### Phase 1: 변경 파일 감지

변경된 파일 목록을 추출합니다:

```bash
git diff --name-only HEAD
```

파일 타입별 분류:
- `frontend/**/*.ts, *.tsx` → Frontend 규칙 체크
- `backend/**/*.java` → Backend 규칙 체크

변경된 파일이 없으면 체크를 스킵합니다.

### Phase 2: 규칙 체크

#### Frontend 규칙 (frontend/CLAUDE.md 참조)

| 규칙 | Grep 패턴 | 심각도 |
|------|----------|--------|
| `any` 타입 금지 | `:\s*any\b` | Error |
| `unknown` 타입 금지 | `:\s*unknown\b` | Error |
| `never` 타입 금지 | `:\s*never\b` | Error |
| Non-null assertion 금지 | `!\.\w+`, `!\[` | Error |
| `\| null` 금지 | `\|\s*null\b` | Warning |
| `as` 최소화 | `\bas\s+[A-Z]\w+` | Warning |

**Frontend 체크 방법:**

```bash
# any 타입 사용 체크
grep -n ":\s*any\b" <file>

# Non-null assertion 체크
grep -n "!\." <file>

# | null 사용 체크
grep -n "|\s*null\b" <file>
```

#### Backend 규칙 (backend/CLAUDE.md 참조)

| 규칙 | 체크 방법 | 심각도 |
|------|----------|--------|
| Service 클래스 `@Transactional` 필수 | 클래스에 어노테이션 확인 | Error |
| `RuntimeException` 직접 사용 금지 | `throw new RuntimeException` | Error |
| Controller `@Operation` 필수 | 메서드에 어노테이션 확인 | Warning |
| Controller `@ApiResponse` 필수 | 메서드에 어노테이션 확인 | Warning |
| 테스트 클래스 `@Transactional` 금지 | 테스트 파일에서 확인 | Error |
| Instant 시간 클래스 강제 | `LocalDateTime`, `LocalDate` 사용 확인 | Warning |

**Backend 체크 방법:**

```bash
# RuntimeException 직접 사용 체크
grep -n "throw new RuntimeException" <file>

# LocalDateTime 사용 체크 (Instant 대신)
grep -n "LocalDateTime\|LocalDate\|LocalTime" <file>

# @Transactional 확인 (Service 클래스)
grep -n "@Transactional" <file>

# @Operation 확인 (Controller 클래스)
grep -n "@Operation" <file>
```

### Phase 3: 리포트 생성

체크 결과를 Markdown 테이블로 출력:

```markdown
## CLAUDE.md 규칙 체크 결과

### 위반 요약
| 파일 | 규칙 | 라인 | 심각도 | 자동 수정 |
|------|------|------|--------|----------|
| src/stores/authStore.ts | `| null` 사용 | 19 | Warning | 가능 |
| PostController.java | `@Operation` 누락 | 45 | Warning | 가능 |

### 위반 없음
위반 사항이 없으면: "모든 규칙을 준수하고 있습니다."
```

### Phase 4: 선택적 리팩토링

위반 사항이 있고 자동 수정이 가능한 경우, 사용자에게 질문:

**자동 수정 가능한 규칙:**
- `| null` → `| undefined` 변환
- Service 클래스에 `@Transactional` 추가
- Controller 메서드에 `@Operation`, `@ApiResponse` 템플릿 추가
- 테스트 클래스에서 `@Transactional` 제거

**선택지 제시:**
1. 모든 자동 수정 적용
2. 특정 항목만 선택
3. 리팩토링 건너뛰기

### Phase 5: 수정 적용

사용자가 승인한 항목만 Edit 도구로 수정합니다.

수정 후 결과 보고:

```markdown
## 리팩토링 완료

| 파일 | 변경 내용 | 상태 |
|------|----------|------|
| src/stores/authStore.ts | `| null` → `| undefined` | 완료 |
| PostController.java | `@Operation` 추가 | 완료 |
```

## 참조 파일

체크에 필요한 규칙 상세:
- **Frontend 규칙**: `frontend/CLAUDE.md`
- **Backend 규칙**: `backend/CLAUDE.md`
- **공통 규칙**: `CLAUDE.md` (루트)

## 체크 스킵 조건

다음 경우 체크를 스킵:
- 변경된 파일이 없는 경우
- 문서 파일(`.md`)만 변경된 경우
- 설정 파일만 변경된 경우 (`.json`, `.yaml`, `.yml`)

## 주의사항

- 자동 수정은 사용자 확인 후에만 적용
- Error 심각도 위반은 반드시 수정 권고
- Warning 심각도 위반은 선택적 수정 제안
