# 회원가입 관심 분야/가입 경로 테스트 케이스

**작성일**: 2026-02-11
**버전**: 1.1
**관련 스펙**: [interests-join-route-verification-criteria.md](../../../../docs/criteria/user/signup/interests-join-route-verification-criteria.md)
**우선순위**: P1

---

## 1. 개요

회원가입 API에 추가되는 관심 분야(`interests`), 기타 관심 분야(`customInterest`), 가입 경로(`joinRoute`), 기타 가입 경로(`customJoinRoute`) 4개 필드에 대한 테스트 케이스이다. DTO 레벨 단일 필드 검증, 서비스 레벨 교차 검증, 필드 간 조합 테스트, 기존 데이터 호환성을 포함한다.

**대상 엔드포인트**:
- `POST /api/v1/auth/password/signup` - 회원가입

**대상 서비스/클래스**:
- `SignupService` - 교차 검증 (`validateOtherFields()`)
- `InterestListConverter` - JSON ↔ `List<Interest>` 변환
- `PasswordSignupRequest` - DTO 검증

---

## 2. 테스트 케이스

### 2.1 InterestListConverter 단위 테스트

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SINT-001 | null → JSON 변환 | - | `convertToDatabaseColumn(null)` 호출 | null 반환 | ✅ |
| SINT-002 | 빈 리스트 → JSON 변환 | - | `convertToDatabaseColumn([])` 호출 | null 반환 | ✅ |
| SINT-003 | 단일 요소 → JSON 변환 | - | `convertToDatabaseColumn([WEB_FRONTEND])` 호출 | `"[\"WEB_FRONTEND\"]"` 반환 | ✅ |
| SINT-004 | 복수 요소 → JSON 변환 | - | `convertToDatabaseColumn([WEB_FRONTEND, AI, CLOUD])` 호출 | `"[\"WEB_FRONTEND\",\"AI\",\"CLOUD\"]"` 반환 | ✅ |
| SINT-005 | 전체 요소(10개) → JSON 변환 | - | 모든 Interest enum 값 리스트 변환 | 10개 값 포함 JSON 배열 반환 | ✅ |
| SINT-006 | null JSON → 리스트 변환 | - | `convertToEntityAttribute(null)` 호출 | 빈 리스트 반환 | ✅ |
| SINT-007 | 유효 JSON → 리스트 변환 | - | `convertToEntityAttribute("[\"AI\",\"GAME\"]")` 호출 | `[AI, GAME]` 리스트 반환 | ✅ |
| SINT-008 | 잘못된 JSON 형식 → 예외 | - | `convertToEntityAttribute("not-json")` 호출 | 예외 발생 | ✅ |
| SINT-009 | 잘못된 enum 값 포함 JSON → 예외 | - | `convertToEntityAttribute("[\"INVALID_VALUE\"]")` 호출 | 예외 발생 | ✅ |

### 2.2 관심 분야 입력 검증 (DTO 레벨)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SINT-010 | interests null → 400 | 유효한 회원가입 정보 | interests=null로 회원가입 요청 | 400 Bad Request, @NotNull 위반 (INT-INV-01) | ⬜ |
| SINT-011 | interests 빈 배열 → 400 | 유효한 회원가입 정보 | interests=[]로 회원가입 요청 | 400 Bad Request, @Size(min=1) 위반 (INT-INV-01) | ⬜ |
| SINT-012 | interests 단일 선택 → 성공 | 유효한 회원가입 정보 | interests=[WEB_FRONTEND]로 회원가입 요청 | 성공, 경계값(최소 유효 선택 수=1) | ⬜ |
| SINT-013 | interests 복수 선택 → 성공 | 유효한 회원가입 정보 | interests=[WEB_FRONTEND, AI, CLOUD]로 회원가입 요청 | 성공 | ⬜ |
| SINT-014 | interests 전체 선택(10개) → 성공 | 유효한 회원가입 정보 | 모든 Interest enum 값 선택 | 성공, 경계값(최대 enum 종류 수=10) | ⬜ |
| SINT-015 | interests 유효하지 않은 enum 값 → 400 | 유효한 회원가입 정보 | interests=["INVALID_VALUE"]로 요청 | 400 Bad Request, Jackson 역직렬화 실패 | ⬜ |

### 2.3 가입 경로 입력 검증 (DTO 레벨)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SINT-020 | joinRoute null → 400 | 유효한 회원가입 정보 | joinRoute=null로 회원가입 요청 | 400 Bad Request, @NotNull 위반 (INT-INV-02) | ⬜ |
| SINT-021 | joinRoute 유효 enum → 성공 | 유효한 회원가입 정보 | joinRoute=EVERYTIME으로 회원가입 요청 | 성공 | ⬜ |
| SINT-022 | joinRoute 유효하지 않은 enum 값 → 400 | 유효한 회원가입 정보 | joinRoute="INVALID"로 요청 | 400 Bad Request, Jackson 역직렬화 실패 | ⬜ |
| SINT-023 | joinRoute 각 enum 값별 성공 | 유효한 회원가입 정보 | joinRoute를 EVERYTIME, POSTER, OT, REFERRAL로 각각 요청 | 각 값으로 정상 가입 성공 | ⬜ |

### 2.4 OTHER 교차 검증 - 관심 분야 (서비스 레벨, INT-INV-03)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SINT-030 | OTHER 포함 + customInterest null → 예외 | 유효한 회원가입 정보 | interests=[AI, OTHER], customInterest=null로 요청 | 400 Bad Request, InvalidCustomFieldException (INT-INV-03) | ✅ |
| SINT-031 | OTHER 포함 + customInterest 빈 문자열 → 예외 | 유효한 회원가입 정보 | interests=[OTHER], customInterest=""로 요청 | 400 Bad Request, InvalidCustomFieldException (INT-INV-03) | ✅ |
| SINT-032 | OTHER 포함 + customInterest 공백만 → 예외 | 유효한 회원가입 정보 | interests=[OTHER], customInterest="   "로 요청 | 400 Bad Request, InvalidCustomFieldException (INT-INV-03) | ✅ |
| SINT-033 | OTHER 포함 + customInterest 유효 → 성공 | 유효한 회원가입 정보 | interests=[AI, OTHER], customInterest="임베디드 시스템"로 요청 | 성공 (INT-INV-03 충족) | ✅ |
| SINT-034 | OTHER 단독 선택 + customInterest 유효 → 성공 | 유효한 회원가입 정보 | interests=[OTHER], customInterest="로보틱스"로 요청 | 성공 | ✅ |
| SINT-035 | OTHER 미포함 + customInterest 있음 → 성공 (무시) | 유효한 회원가입 정보 | interests=[WEB_FRONTEND], customInterest="임의값"로 요청 | 성공, custom 값 저장되나 검증 안 함 (INT-INV-06) | ✅ |

### 2.5 OTHER 교차 검증 - 가입 경로 (서비스 레벨, INT-INV-04)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SINT-040 | OTHER + customJoinRoute null → 예외 | 유효한 회원가입 정보 | joinRoute=OTHER, customJoinRoute=null로 요청 | 400 Bad Request, InvalidCustomFieldException (INT-INV-04) | ✅ |
| SINT-041 | OTHER + customJoinRoute 빈 문자열 → 예외 | 유효한 회원가입 정보 | joinRoute=OTHER, customJoinRoute=""로 요청 | 400 Bad Request, InvalidCustomFieldException (INT-INV-04) | ✅ |
| SINT-042 | OTHER + customJoinRoute 공백만 → 예외 | 유효한 회원가입 정보 | joinRoute=OTHER, customJoinRoute="   "로 요청 | 400 Bad Request, InvalidCustomFieldException (INT-INV-04) | ✅ |
| SINT-043 | OTHER + customJoinRoute 유효 → 성공 | 유효한 회원가입 정보 | joinRoute=OTHER, customJoinRoute="인스타그램 광고"로 요청 | 성공 (INT-INV-04 충족) | ✅ |
| SINT-044 | OTHER 아닌 경우 + customJoinRoute 있음 → 성공 (무시) | 유효한 회원가입 정보 | joinRoute=EVERYTIME, customJoinRoute="임의값"로 요청 | 성공, custom 값 저장되나 검증 안 함 (INT-INV-06) | ✅ |

### 2.6 custom 필드 길이 검증 (DTO 레벨)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SINT-050 | customInterest 100자 (최대, 경계값) → 성공 | interests에 OTHER 포함 | customInterest=100자 문자열로 요청 | 성공 | ⬜ |
| SINT-051 | customInterest 101자 (초과, 경계값) → 400 | interests에 OTHER 포함 | customInterest=101자 문자열로 요청 | 400 Bad Request, @Size(max=100) 위반 | ⬜ |
| SINT-052 | customJoinRoute 100자 (최대, 경계값) → 성공 | joinRoute=OTHER | customJoinRoute=100자 문자열로 요청 | 성공 | ⬜ |
| SINT-053 | customJoinRoute 101자 (초과, 경계값) → 400 | joinRoute=OTHER | customJoinRoute=101자 문자열로 요청 | 400 Bad Request, @Size(max=100) 위반 | ⬜ |

### 2.7 필드 간 조합 테스트 (Pairwise, 서비스 레벨)

검증 기준서 2-5절의 핵심 조합을 검증한다.

| ID | 테스트 케이스 | interests OTHER 포함 | customInterest | joinRoute | customJoinRoute | 예상 결과 | 상태 |
|----|-------------|:---:|:---:|:---:|:---:|----------|------|
| SINT-060 | 조합 1: 둘 다 OTHER 아님 + custom 없음 | X | null | EVERYTIME | null | 성공 | ✅ |
| SINT-061 | 조합 2: interests만 OTHER 아님 + joinRoute=OTHER 성공 | X | null | OTHER | "인스타" | 성공 | ✅ |
| SINT-062 | 조합 3: interests만 OTHER + joinRoute OTHER 아님 성공 | O | "임베디드" | EVERYTIME | null | 성공 | ✅ |
| SINT-063 | 조합 4: 둘 다 OTHER + 둘 다 custom 있음 성공 | O | "임베디드" | OTHER | "인스타" | 성공 | ✅ |
| SINT-064 | 조합 5: interests OTHER + custom 없음 → 실패 | O | null | EVERYTIME | null | 400 (INT-INV-03 위반) | ✅ |
| SINT-065 | 조합 6: interests OTHER + custom 빈 문자열 → 실패 | O | "" | OTHER | "인스타" | 400 (INT-INV-03 위반) | ✅ |
| SINT-066 | 조합 7: joinRoute OTHER + custom 없음 → 실패 | X | null | OTHER | null | 400 (INT-INV-04 위반) | ✅ |
| SINT-067 | 조합 8: joinRoute OTHER + custom 빈 문자열 → 실패 | X | null | OTHER | "" | 400 (INT-INV-04 위반) | ✅ |
| SINT-068 | 조합 9: 둘 다 OTHER + 둘 다 custom 없음 → 실패 | O | null | OTHER | null | 400 (INT-INV-03, 04 둘 다 위반) | ✅ |

### 2.8 기존 데이터 호환성 (INT-INV-05)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SINT-070 | 기존 사용자 interests=null 조회 | 마이그레이션 전 가입 사용자 (interests 컬럼 null) | 사용자 상세 조회 API 호출 | 200 OK, interests=null 또는 빈 배열 정상 반환 | ⬜ |
| SINT-071 | 기존 사용자 joinRoute=null 조회 | 마이그레이션 전 가입 사용자 (joinRoute 컬럼 null) | 사용자 상세 조회 API 호출 | 200 OK, joinRoute=null 정상 반환 | ⬜ |

### 2.9 관리자 조회 응답

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SINT-080 | UserDetailResponse에 interests 포함 | interests=[WEB_FRONTEND, AI]로 가입한 사용자 | 관리자 사용자 상세 조회 | 응답에 interests=[WEB_FRONTEND, AI] 포함 | ⬜ |
| SINT-081 | UserDetailResponse에 customInterest 포함 | interests에 OTHER 포함 + customInterest="임베디드"로 가입한 사용자 | 관리자 사용자 상세 조회 | 응답에 customInterest="임베디드" 포함 | ⬜ |
| SINT-082 | UserDetailResponse에 joinRoute 포함 | joinRoute=EVERYTIME으로 가입한 사용자 | 관리자 사용자 상세 조회 | 응답에 joinRoute=EVERYTIME 포함 | ⬜ |
| SINT-083 | UserDetailResponse에 customJoinRoute 포함 | joinRoute=OTHER + customJoinRoute="인스타그램"으로 가입한 사용자 | 관리자 사용자 상세 조회 | 응답에 customJoinRoute="인스타그램" 포함 | ⬜ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| INT-INV-01 | interests 최소 1개 선택 필수 | SINT-010~012 |
| INT-INV-02 | joinRoute 필수 | SINT-020~022 |
| INT-INV-03 | OTHER → customInterest 필수 | SINT-030~034, SINT-064~065, SINT-068 |
| INT-INV-04 | OTHER → customJoinRoute 필수 | SINT-040~043, SINT-066~068 |
| INT-INV-05 | 기존 사용자 데이터 호환 | SINT-070~071 |
| INT-INV-06 | OTHER 아닌 경우 custom 무시 | SINT-035, SINT-044, SINT-060~062 |

---

## 4. 구현된 테스트 클래스

### 4.1 단위 테스트 (InterestListConverter)
- **파일**: `backend/src/test/java/igrus/web/common/converter/InterestListConverterTest.java` (신규)
- **테스트 범위**: SINT-001~009
- **테스트 수**: 9개 (전체 구현 완료)

### 4.2 서비스 통합 테스트 (SignupService)
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/service/signup/SignupServiceTest.java` (기존 확장)
- **테스트 범위**: SINT-030~035, SINT-040~044
- **테스트 수**: 11개 (교차 검증 6 + 5, 전체 구현 완료)
- **추가된 @Nested 클래스**:
  - `SignupInterestOtherValidationTest` (SINT-030~035)
  - `SignupJoinRouteOtherValidationTest` (SINT-040~044)
  - `SignupInterestsAndJoinRouteTest` (interests/joinRoute 정상 저장 확인)

### 4.3 통합/E2E 테스트 (PasswordSignupIntegrationTest)
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/integration/PasswordSignupIntegrationTest.java` (기존 확장)
- **테스트 범위**: SINT-060~068
- **테스트 수**: 9개 (Pairwise 조합 테스트, 전체 구현 완료)
- **추가된 @Nested 클래스**: `SignupPairwiseCombinationTest`

### 4.4 미구현 테스트
- DTO 입력 검증 (SINT-010~015, SINT-020~023): Jakarta Validation 어노테이션으로 보장, 별도 컨트롤러 테스트 미작성
- custom 필드 길이 검증 (SINT-050~053): Jakarta Validation `@Size(max=100)` 어노테이션으로 보장, 별도 테스트 미작성
- 기존 데이터 호환성 (SINT-070~071): DB 컬럼 nullable 설정으로 보장, 수동 검증 대상
- 관리자 조회 응답 (SINT-080~083): `UserDetailResponse.from(User)` 매핑 구현 완료, 관리자 API 통합 테스트는 별도 진행 필요

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-11 | - | 검증 기준서 기반 최초 작성 |
| 1.1 | 2026-02-11 | - | 구현 완료: SINT-001~009, SINT-030~035, SINT-040~044, SINT-060~068 상태 업데이트, 테스트 클래스 정보 반영 |
