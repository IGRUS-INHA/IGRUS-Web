# 회원가입 관심 분야/가입 경로 검증 기준서

> **Status**: Draft
> **Last Updated**: 2026-02-11
> **Issue**: [#327 - 회원가입 API에 관심 분야/가입 경로 필드 추가](https://github.com/IGRUS-INHA/IGRUS-Web/issues/327)
> **Scope**: 관심 분야(interests), 기타 관심 분야(customInterest), 가입 경로(joinRoute), 기타 가입 경로(customJoinRoute)
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)

## 목적

이 문서는 회원가입 API에 관심 분야(`interests`)와 가입 경로(`joinRoute`) 필드를 추가할 때 **반드시 지켜져야 하는 규칙**을 명시하여, 구현 및 코드 변경 시 검증 기준으로 사용한다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 변경에 직접 관련된 4개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | interests/joinRoute 필수 조건, OTHER-custom 연동 규칙 |
| 2 | 입력 도메인 분할과 경계값 | 4개 신규 필드의 유효/무효 입력 분류 및 경계값 |
| 3 | 시스템 경계와 책임 분리 | DTO 검증 vs 서비스 검증 vs DB 제약의 책임 분배 |
| 4 | 테스트 전략 | 검증 항목별 테스트 커버리지 계획 |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

### INT-INV-01: 관심 분야 최소 1개 선택 필수

> 회원가입 시 `interests`는 반드시 1개 이상 선택해야 한다.

- **검증 계층**: DTO 레벨 (`@NotNull`, `@Size(min = 1)`)
- **위반 시**: `MethodArgumentNotValidException` → 400 Bad Request
- **관련 코드**: `PasswordSignupRequest.interests`

### INT-INV-02: 가입 경로 필수

> 회원가입 시 `joinRoute`는 반드시 선택해야 한다.

- **검증 계층**: DTO 레벨 (`@NotNull`)
- **위반 시**: `MethodArgumentNotValidException` → 400 Bad Request
- **관련 코드**: `PasswordSignupRequest.joinRoute`

### INT-INV-03: OTHER 선택 시 customInterest 필수

> `interests`에 `OTHER`가 포함된 경우 `customInterest`는 반드시 비어있지 않은 값이어야 한다.

- **검증 계층**: 서비스 레벨 (DTO 검증만으로 표현 불가 - 필드 간 교차 검증)
- **사전조건**: `interests` 리스트에 `Interest.OTHER` 포함
- **사후조건**: `customInterest != null && !customInterest.isBlank()`
- **위반 시**: `InvalidCustomFieldException` → 400 Bad Request
- **관련 코드**: `SignupService.validateOtherFields()`
- **검증 방법**: interests에 OTHER 포함 + customInterest null/빈 문자열 → 예외 발생 assertion

### INT-INV-04: OTHER 선택 시 customJoinRoute 필수

> `joinRoute`가 `OTHER`인 경우 `customJoinRoute`는 반드시 비어있지 않은 값이어야 한다.

- **검증 계층**: 서비스 레벨 (필드 간 교차 검증)
- **사전조건**: `joinRoute == JoinRoute.OTHER`
- **사후조건**: `customJoinRoute != null && !customJoinRoute.isBlank()`
- **위반 시**: `InvalidCustomFieldException` → 400 Bad Request
- **관련 코드**: `SignupService.validateOtherFields()`
- **검증 방법**: joinRoute=OTHER + customJoinRoute null/빈 문자열 → 예외 발생 assertion

### INT-INV-05: 기존 사용자 데이터 호환성

> 기존 사용자(마이그레이션 이전 가입)의 interests/joinRoute는 null이 허용된다.

- **근거**: DB 컬럼은 nullable, NOT NULL 제약은 API 레벨(DTO)에서만 강제
- **사후조건**: 기존 사용자 조회 시 interests=null/빈 배열, joinRoute=null 정상 반환
- **검증 방법**: 마이그레이션 후 기존 사용자 데이터 정상 조회 확인

### INT-INV-06: OTHER가 아닌 경우 custom 필드 무시

> `interests`에 `OTHER`가 없는 경우 `customInterest` 값은 무시되어도 무방하다 (저장은 되나 검증하지 않음). `joinRoute`가 `OTHER`가 아닌 경우도 동일.

- **근거**: 프론트엔드에서 OTHER 해제 시 custom 필드를 비우지 않을 수 있음
- **동작**: custom 값이 전달되더라도 예외 없이 저장
- **검증 방법**: interests=[WEB_FRONTEND] + customInterest="임의값" → 정상 가입 성공

---

## 2. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

입력값은 2단계로 검증된다: **DTO 레벨** (Jakarta Validation) → **서비스 레벨** (교차 검증).

### 2-1. interests (관심 분야)

| 동치류 | 입력 예시 | 기대 결과 | 검증 계층 |
|--------|----------|----------|----------|
| 유효 - 단일 선택 | `["WEB_FRONTEND"]` | 성공 | DTO |
| 유효 - 복수 선택 | `["WEB_FRONTEND", "AI", "CLOUD"]` | 성공 | DTO |
| 유효 - OTHER 포함 + custom 있음 | `["AI", "OTHER"]` + `customInterest="임베디드"` | 성공 | 서비스 |
| 유효 - 전체 선택 (10개) | 모든 Interest 값 | 성공 | DTO |
| 무효 - null | `null` | 400 | DTO (`@NotNull`) |
| 무효 - 빈 배열 | `[]` | 400 | DTO (`@Size(min=1)`) |
| 무효 - 유효하지 않은 enum 값 | `["INVALID_VALUE"]` | 400 | Jackson 역직렬화 |
| 무효 - OTHER 포함 + custom null | `["OTHER"]` + `customInterest=null` | 400 | 서비스 |
| 무효 - OTHER 포함 + custom 빈 문자열 | `["OTHER"]` + `customInterest=""` | 400 | 서비스 |
| 무효 - OTHER 포함 + custom 공백만 | `["OTHER"]` + `customInterest="   "` | 400 | 서비스 |

**경계값**:

| 항목 | 경계 지점 | 기대 결과 |
|------|----------|----------|
| 최소 선택 수 | 0개 (빈 배열) | 400 (min=1 위반) |
| 최소 유효 선택 수 | 1개 | 성공 |
| 최대 enum 종류 수 | 10개 (전체) | 성공 |

### 2-2. customInterest (기타 관심 분야)

| 동치류 | 입력 예시 | 기대 결과 | 검증 계층 |
|--------|----------|----------|----------|
| 유효 - 정상 입력 (OTHER 선택 시) | `"임베디드 시스템"` | 성공 | 서비스 |
| 유효 - 100자 (최대) | 100자 문자열 | 성공 | DTO |
| 유효 - null (OTHER 미선택 시) | `null` | 성공 | - |
| 무효 - 101자 (초과) | 101자 문자열 | 400 | DTO (`@Size(max=100)`) |
| 무효 - null (OTHER 선택 시) | `null` | 400 | 서비스 |
| 무효 - 빈 문자열 (OTHER 선택 시) | `""` | 400 | 서비스 |

**경계값**:

| 항목 | 경계 지점 | 기대 결과 |
|------|----------|----------|
| 최대 길이 | 100자 | 성공 |
| 최대 초과 | 101자 | 400 |

### 2-3. joinRoute (가입 경로)

| 동치류 | 입력 예시 | 기대 결과 | 검증 계층 |
|--------|----------|----------|----------|
| 유효 - 정상 enum | `"EVERYTIME"` | 성공 | DTO |
| 유효 - OTHER + custom 있음 | `"OTHER"` + `customJoinRoute="인스타그램"` | 성공 | 서비스 |
| 무효 - null | `null` | 400 | DTO (`@NotNull`) |
| 무효 - 유효하지 않은 enum 값 | `"INVALID"` | 400 | Jackson 역직렬화 |
| 무효 - OTHER + custom null | `"OTHER"` + `customJoinRoute=null` | 400 | 서비스 |
| 무효 - OTHER + custom 빈 문자열 | `"OTHER"` + `customJoinRoute=""` | 400 | 서비스 |
| 무효 - OTHER + custom 공백만 | `"OTHER"` + `customJoinRoute="   "` | 400 | 서비스 |

### 2-4. customJoinRoute (기타 가입 경로)

| 동치류 | 입력 예시 | 기대 결과 | 검증 계층 |
|--------|----------|----------|----------|
| 유효 - 정상 입력 (OTHER 선택 시) | `"인스타그램 광고"` | 성공 | 서비스 |
| 유효 - 100자 (최대) | 100자 문자열 | 성공 | DTO |
| 유효 - null (OTHER 미선택 시) | `null` | 성공 | - |
| 무효 - 101자 (초과) | 101자 문자열 | 400 | DTO (`@Size(max=100)`) |
| 무효 - null (OTHER 선택 시) | `null` | 400 | 서비스 |
| 무효 - 빈 문자열 (OTHER 선택 시) | `""` | 400 | 서비스 |

**경계값**:

| 항목 | 경계 지점 | 기대 결과 |
|------|----------|----------|
| 최대 길이 | 100자 | 성공 |
| 최대 초과 | 101자 | 400 |

### 2-5. 필드 간 조합 테스트 (Pairwise)

교차 검증이 필요한 핵심 조합:

| # | interests 포함 OTHER | customInterest | joinRoute | customJoinRoute | 기대 결과 |
|---|:---:|:---:|:---:|:---:|:---:|
| 1 | X | null | EVERYTIME | null | 성공 |
| 2 | X | null | OTHER | "인스타" | 성공 |
| 3 | O | "임베디드" | EVERYTIME | null | 성공 |
| 4 | O | "임베디드" | OTHER | "인스타" | 성공 |
| 5 | O | null | EVERYTIME | null | 400 (INT-INV-03 위반) |
| 6 | O | "" | OTHER | "인스타" | 400 (INT-INV-03 위반) |
| 7 | X | null | OTHER | null | 400 (INT-INV-04 위반) |
| 8 | X | null | OTHER | "" | 400 (INT-INV-04 위반) |
| 9 | O | null | OTHER | null | 400 (INT-INV-03, 04 둘 다 위반) |

---

## 3. 시스템 경계와 책임 분리 (System Boundary & SoC)

### 3-1. 검증 책임 분배

```
┌──────────────────────────────────────────────────────┐
│ DTO 레벨 (Jakarta Validation)                         │
│  - @NotNull, @Size(min=1): interests 필수/최소 1개    │
│  - @NotNull: joinRoute 필수                           │
│  - @Size(max=100): customInterest/customJoinRoute 길이│
│  - Jackson: enum 역직렬화 실패 → 400                  │
├──────────────────────────────────────────────────────┤
│ 서비스 레벨 (SignupService)                            │
│  - OTHER + custom 교차 검증 (INT-INV-03, 04)          │
│  - 중복 검증 (학번/이메일/전화번호) ← 기존 로직       │
├──────────────────────────────────────────────────────┤
│ 엔티티 레벨 (User.create)                             │
│  - 학번/이메일/학년 형식 검증 ← 기존 로직             │
│  - interests/joinRoute: 추가 검증 없음 (DTO에서 완료) │
├──────────────────────────────────────────────────────┤
│ DB 레벨 (Flyway Migration)                            │
│  - 4개 컬럼 모두 nullable (기존 데이터 호환)          │
│  - interests: JSON 타입, joinRoute: VARCHAR(30)       │
│  - customInterest/customJoinRoute: VARCHAR(100)       │
└──────────────────────────────────────────────────────┘
```

**설계 결정 근거**:
- DTO vs 서비스 분리: `@NotNull`/`@Size` 같은 단일 필드 검증은 DTO에서, 필드 간 교차 검증(OTHER → custom 필수)은 서비스에서 수행
- DB nullable: 기존 사용자 데이터 호환을 위해 NOT NULL 제약을 DB에 걸지 않음. 이는 `wishes` 컬럼 추가 시(V32) 사용한 동일 패턴

### 3-2. 데이터 저장 구조

| 필드 | DB 컬럼 | 타입 | 저장 방식 |
|------|---------|------|----------|
| `interests` | `users_interests` | JSON | `InterestListConverter` (enum 배열 → JSON 문자열) |
| `customInterest` | `users_custom_interest` | VARCHAR(100) | 단순 문자열 |
| `joinRoute` | `users_join_route` | VARCHAR(30) | `@Enumerated(STRING)` |
| `customJoinRoute` | `users_custom_join_route` | VARCHAR(100) | 단순 문자열 |

**Interest enum 값**:

| Enum | 표시값 |
|------|-------|
| `WEB_FRONTEND` | 웹 (프론트엔드) |
| `WEB_BACKEND` | 웹 (백엔드) |
| `APP` | 앱 |
| `SECURITY` | 해킹/보안 |
| `UI_UX_DESIGN` | 디자인 (UI/UX) |
| `OTHER_DESIGN` | 디자인 (UI/UX 외) |
| `AI` | AI |
| `CLOUD` | Cloud |
| `GAME` | 게임 |
| `OTHER` | 기타 (직접 입력) |

**JoinRoute enum 값**:

| Enum | 표시값 |
|------|-------|
| `EVERYTIME` | 에브리타임 |
| `POSTER` | 포스터 및 현수막 |
| `OT` | OT |
| `REFERRAL` | 지인 소개 |
| `OTHER` | 기타 (직접 입력) |

### 3-3. 관리자 조회 응답

`UserDetailResponse`에 4개 필드가 추가되어 관리자가 가입자의 관심 분야와 가입 경로를 확인할 수 있다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `interests` | `List<Interest>` | 관심 분야 enum 목록 |
| `customInterest` | `String` | 기타 관심 분야 (OTHER 선택 시) |
| `joinRoute` | `JoinRoute` | 가입 경로 |
| `customJoinRoute` | `String` | 기타 가입 경로 (OTHER 선택 시) |

---

## 4. 테스트 전략 (Test Strategy)

### 4-1. 테스트 계층별 검증 항목

#### 단위 테스트 (순수 Java)

| 테스트 대상 | 검증 항목 | 테스트 클래스 |
|------------|----------|-------------|
| `InterestListConverter` | JSON ↔ `List<Interest>` 양방향 변환 | `InterestListConverterTest` (신규) |
| `InterestListConverter` | null/빈 리스트/단일 요소/전체 요소 변환 | `InterestListConverterTest` (신규) |
| `InterestListConverter` | 잘못된 JSON/잘못된 enum 값 → 예외 | `InterestListConverterTest` (신규) |

#### 서비스 통합 테스트 (SignupServiceTest)

| 검증 항목 | 불변조건 | 테스트 메서드 |
|----------|---------|-------------|
| interests에 OTHER 포함 + customInterest null → 예외 | INT-INV-03 | `signup_WithOtherInterestWithoutCustom_ThrowsException` |
| interests에 OTHER 포함 + customInterest 빈 문자열 → 예외 | INT-INV-03 | `signup_WithOtherInterestWithBlankCustom_ThrowsException` |
| interests에 OTHER 포함 + customInterest 공백만 → 예외 | INT-INV-03 | `signup_WithOtherInterestWithWhitespaceCustom_ThrowsException` |
| interests에 OTHER 포함 + customInterest 있으면 성공 | INT-INV-03 | `signup_WithOtherInterestAndCustom_Succeeds` |
| joinRoute=OTHER + customJoinRoute null → 예외 | INT-INV-04 | `signup_WithOtherJoinRouteWithoutCustom_ThrowsException` |
| joinRoute=OTHER + customJoinRoute 빈 문자열 → 예외 | INT-INV-04 | `signup_WithOtherJoinRouteWithBlankCustom_ThrowsException` |
| joinRoute=OTHER + customJoinRoute 공백만 → 예외 | INT-INV-04 | `signup_WithOtherJoinRouteWithWhitespaceCustom_ThrowsException` |
| joinRoute=OTHER + customJoinRoute 있으면 성공 | INT-INV-04 | `signup_WithOtherJoinRouteAndCustom_Succeeds` |
| interests/joinRoute 정상 입력 시 회원가입 성공 | INT-INV-01, 02 | `signup_WithValidInterestsAndJoinRoute_Succeeds` |
| OTHER 아닌 경우 custom 필드 무시 | INT-INV-06 | `signup_WithoutOtherAndWithCustom_Succeeds` |

#### 통합/E2E 테스트 (PasswordSignupIntegrationTest)

| 검증 항목 | 테스트 메서드 |
|----------|-------------|
| 전체 회원가입 플로우에 interests/joinRoute 포함 | 기존 E2E 테스트 업데이트 |
| DB 저장 후 조회 시 interests/joinRoute 정상 반환 | 신규 또는 기존 테스트 확장 |

### 4-2. 테스트-검증 항목 매핑

| 불변조건 | 커버 테스트 | 목표 상태 |
|---------|-----------|----------|
| INT-INV-01 (interests 최소 1개) | DTO 검증 → `PasswordSignupIntegrationTest` | **DTO 테스트로 커버** |
| INT-INV-02 (joinRoute 필수) | DTO 검증 → `PasswordSignupIntegrationTest` | **DTO 테스트로 커버** |
| INT-INV-03 (OTHER → customInterest 필수) | `SignupServiceTest` (3개 케이스) | **서비스 테스트로 커버** |
| INT-INV-04 (OTHER → customJoinRoute 필수) | `SignupServiceTest` (3개 케이스) | **서비스 테스트로 커버** |
| INT-INV-05 (기존 데이터 호환) | 마이그레이션 후 기존 데이터 조회 | **수동 검증** |
| INT-INV-06 (OTHER 아닌 경우 custom 무시) | `SignupServiceTest` (1개 케이스) | **서비스 테스트로 커버** |

---

## 관련 문서

- [회원가입/승인/강등 검증 기준서](../../verification-criteria.md) - 기존 회원가입 검증 기준 (학번/이메일/비밀번호 등)
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC) - 용어 및 개념 참조
