---
name: pre-commit-review
description: 커밋 전 코드 리뷰를 수행합니다. CLAUDE.md 규칙 준수, 버그 탐지, 보안 이슈, 코드 품질을 검사하고 커밋 가능 여부를 판정합니다. 읽기 전용으로 동작하며 코드를 수정하지 않습니다.
tools: Glob, Grep, Read, Bash
model: opus
---

# Pre-Commit Code Review

커밋 전 모든 미커밋 변경사항을 리뷰하여 이슈를 보고하고 커밋 가능 여부를 판정합니다.

**이 skill은 읽기 전용입니다. 코드를 수정하지 않습니다.**

## Workflow

### Phase 1: 변경사항 수집

staged와 unstaged 변경을 모두 수집합니다.

```bash
# 변경된 파일 목록 (staged + unstaged)
git diff --name-only HEAD

# 변경 내용 전체 (diff)
git diff HEAD

# staged만 따로 보기 (커밋 대상 파악용)
git diff --cached --name-only
```

파일 타입별 분류:
- `frontend/**/*.ts, *.tsx` → Frontend 리뷰
- `backend/**/*.java` → Backend 리뷰
- 기타 (`*.md`, `*.json`, `*.yaml`, `*.sql` 등) → 기본 리뷰

**스킵 조건**: 변경된 코드 파일이 없으면 (문서/설정만 변경) 리뷰를 스킵하고 "커밋 가능"으로 판정합니다.

### Phase 2: 규칙 로드

프로젝트의 CLAUDE.md 파일들을 읽어 검사 기준을 동적으로 로드합니다.

**읽어야 할 파일:**
- `CLAUDE.md` (루트 - 공통 규칙)
- `backend/CLAUDE.md` (Backend 규칙)
- `frontend/CLAUDE.md` (Frontend 규칙)

읽은 CLAUDE.md의 규칙들이 이후 Phase에서 검사 기준이 됩니다. CLAUDE.md가 업데이트되면 자동으로 최신 규칙이 반영됩니다.

### Phase 3: 변경 내용 분석

모든 변경 파일에 대해 diff를 분석합니다. 변경 파일을 Read 도구로 읽어 전체 컨텍스트를 파악합니다.

**추가된 라인(+)과 삭제된 라인(-) 모두 검사합니다.**

- **추가된 라인(+)**: 새로 작성된 코드가 CLAUDE.md 규칙을 준수하는지, 버그가 없는지 검사
- **삭제된 라인(-)**: 필수적인 코드가 실수로 삭제되지 않았는지 검사

### Phase 4: CLAUDE.md 규칙 준수 검사

Phase 2에서 로드한 CLAUDE.md 규칙들을 기준으로 변경 코드를 검사합니다.

#### 4.1 추가 라인(+) 검사

CLAUDE.md에 정의된 모든 코딩 규칙(네이밍, 어노테이션, 타입 사용, 아키텍처 패턴 등)에 대해 추가된 코드가 준수하는지 검사합니다.

**심각도 기준:**
- **Error**: CLAUDE.md에서 "금지", "필수", "사용하지 말 것" 등으로 강하게 명시된 규칙 위반
- **Warning**: CLAUDE.md에서 "권장", "최소화", "주의" 등으로 명시된 규칙 위반

#### 4.2 삭제 라인(-) 검사

삭제된 코드에 다음과 같은 필수 요소가 포함되어 있는지 검사합니다. 삭제된 요소가 동일 파일 내에서 다른 형태로 대체(리팩토링)된 경우는 이슈로 보고하지 않습니다. **반드시 현재 파일의 전체 내용을 Read로 확인하여 대체 여부를 판단합니다.**

**Backend 삭제 검사:**
- 필수 어노테이션: `@Transactional`, `@Override`, `@Valid`, `@NotNull`, `@NotBlank` 등
- 보안 어노테이션: `@PreAuthorize`, `@Secured`, `@SecurityRequirement`, `@RolesAllowed`
- 검증 로직: null 체크, 입력 검증, 파라미터 바인딩
- 에러 핸들링: try-catch, `@ExceptionHandler`, 커스텀 예외 throw
- Swagger 문서: `@Operation`, `@ApiResponse`, `@Schema`

**Frontend 삭제 검사:**
- 에러 핸들링: `onError`, `catch`, 에러 헬퍼 함수 호출
- 접근성: `aria-*`, `role`, `alt` 속성
- 권한 체크: `isForbidden`, `isUnauthorized` 등 권한 관련 조건문
- 폼/입력 검증: Zod 스키마, 검증 로직
- 타입 정의: interface, type 정의 (다른 곳에서 참조 중인 경우)

**심각도 기준:**
- **Error**: 보안/인가 관련 코드 삭제, 필수 어노테이션 삭제
- **Warning**: 검증/에러 핸들링/문서 관련 코드 삭제

### Phase 5: 버그 탐지

모든 변경 파일의 추가/삭제 라인을 분석하여 잠재적 버그를 검사합니다.

**추가 라인 버그:**
- 널 참조, 오프바이원, 리소스 누수, 로직 오류
- 동시성 문제 (race condition, 공유 상태)
- JPA/Hibernate (N+1 쿼리, 지연 로딩, detached entity)
- React (useEffect 의존성 누락, 무한 렌더링, stale closure)
- 상태 관리 (Zustand store 직접 변이)

**삭제 라인 버그:**
- 필수 로직 제거 (null 체크, 범위 검증, 경계 조건 처리)
- 부작용 제거 (이벤트 리스너 해제, 리소스 정리, 캐시 무효화)
- 의존 코드 고아화 (삭제된 함수/메서드를 다른 곳에서 여전히 호출)

**판정 기준: confidence score 70 이상인 이슈만 보고합니다.**

### Phase 6: 보안 검사

OWASP Top 10 기준으로 모든 변경 파일의 보안 이슈를 검사합니다.

**추가 라인:**
- Broken Access Control, Injection, XSS
- 하드코딩된 credential/비밀키
- 보안 설정 오류 (CORS, 디버그 모드)
- 민감 정보 로깅

**삭제 라인:**
- 인증/인가 로직 제거
- 입력 검증/XSS 필터 제거
- 보안 헤더/CORS 설정 제거
- 암호화 로직 제거

### Phase 7: 코드 품질 검사

**실질적 품질 문제만 보고합니다. 스타일, 네이밍 선호도, 사소한 개선 등 nitpick은 하지 않습니다.**

- 과도한 중첩 (4단계 이상)
- 동일 로직 반복 (3회 이상)
- 빈 catch 블록 (에러 무시)
- 제네릭 raw type 사용 (Java)
- 미사용 import

### Phase 8: 리포트 생성

모든 검사 결과를 종합하여 다음 형식으로 리포트를 출력합니다.

```markdown
---

## 커밋 전 코드 리뷰 결과

### 변경 요약
| 구분 | 파일 수 | 추가 | 삭제 |
|------|---------|------|------|
| Backend | {N} | +{lines} | -{lines} |
| Frontend | {N} | +{lines} | -{lines} |
| 기타 | {N} | +{lines} | -{lines} |

변경 파일:
- `path/to/file1.java` (수정)
- `path/to/file2.tsx` (신규)
- ...

---

### 발견된 이슈

#### [Error] 규칙 위반 (수정 필수)
| # | 파일 | 라인 | 유형 | 설명 |
|---|------|------|------|------|
| 1 | `SomeService.java` | 25 | 추가 | `throw new RuntimeException` 직접 사용 (backend/CLAUDE.md §3) |
| 2 | `Controller.java` | - | 삭제 | `@Transactional` 어노테이션이 대체 없이 삭제됨 |

#### [Warning] 권장 수정
| # | 파일 | 라인 | 유형 | 설명 |
|---|------|------|------|------|
| 1 | `Controller.java` | 42 | 추가 | `@Operation` 누락 (backend/CLAUDE.md §11) |
| 2 | `Page.tsx` | - | 삭제 | `onError` 에러 핸들링이 제거됨 |

#### [Bug] 잠재적 버그
| # | 파일 | 라인 | 유형 | 설명 | 신뢰도 |
|---|------|------|------|------|--------|
| 1 | `Service.java` | 88 | 추가 | `user.getName()` 호출 시 user가 null 가능 | 85% |
| 2 | `Hook.tsx` | - | 삭제 | cleanup 함수가 삭제되어 메모리 누수 가능 | 90% |

#### [Security] 보안 이슈
| # | 파일 | 라인 | 유형 | 설명 |
|---|------|------|------|------|
| (해당 사항 없으면 "보안 이슈가 발견되지 않았습니다." 표시) |

#### [Quality] 코드 품질
| # | 파일 | 라인 | 유형 | 설명 |
|---|------|------|------|------|
| (해당 사항 없으면 이 섹션 생략) |

---

### 판정

{아래 둘 중 하나}

#### 커밋 가능
Error 수준 이슈가 없고, Bug/Security 이슈가 없습니다.
Warning이 있는 경우에도 커밋은 가능하지만 향후 개선을 권장합니다.

#### 수정 후 커밋 권장
다음 이슈를 해결한 후 커밋하는 것을 권장합니다:
1. {이슈 요약 1}
2. {이슈 요약 2}

---
```

### 판정 기준

| 조건 | 판정 |
|------|------|
| Error 이슈 0개 AND Bug 이슈 0개 AND Security 이슈 0개 | **커밋 가능** |
| Warning만 있음 (Error/Bug/Security 없음) | **커밋 가능** (경고 포함) |
| Error 이슈 1개 이상 | **수정 후 커밋 권장** |
| Bug 이슈 1개 이상 (confidence >= 70) | **수정 후 커밋 권장** |
| Security 이슈 1개 이상 | **수정 후 커밋 권장** |

## 주의사항

- 이 skill은 **읽기 전용**입니다. 코드를 수정하거나 파일을 생성하지 않습니다.
- **모든 변경 파일**을 검사합니다. 파일 수에 따른 제한 없이 전수 검사합니다.
- **오탐 방지**를 위해 confidence score 70 미만의 버그 이슈는 보고하지 않습니다.
- 삭제 라인 검사 시 **리팩토링으로 대체된 경우**(동일 기능이 다른 형태로 존재)는 이슈로 보고하지 않습니다. 반드시 현재 파일의 전체 내용을 Read로 확인하여 대체 여부를 판단합니다.
- 스타일, 네이밍 선호도, 사소한 개선 등 **nitpick은 하지 않습니다**.
- Orval 자동 생성 파일(`frontend/src/api/`)은 검사에서 제외합니다.
- 테스트 코드는 Service `@Transactional` 필수 규칙에서 제외하되, 테스트 `@Transactional` 금지 규칙은 적용합니다.
- 이슈 설명에 **위반한 CLAUDE.md 규칙의 출처**(파일명 및 섹션)를 명시합니다.
