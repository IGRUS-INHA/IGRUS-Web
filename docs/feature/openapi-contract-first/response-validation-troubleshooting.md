# OpenAPI 응답 검증 실패 트러블슈팅 가이드

## 메타데이터
| 항목 | 값 |
|------|------|
| 작성일 | 2026-03-02 |
| 관련 TASK | TASK-231 |
| 라이브러리 | Atlassian swagger-request-validator 2.46.0 |
| 스펙 버전 | OpenAPI 3.1.0 |

## 목차
1. [검증 메커니즘 개요](#1-검증-메커니즘-개요)
2. [검증 실패 메시지 해석](#2-검증-실패-메시지-해석)
3. [일반적인 불일치 유형과 해결 방법](#3-일반적인-불일치-유형과-해결-방법)
4. [수정 방향 결정 기준](#4-수정-방향-결정-기준)
5. [OpenAPI 3.1.0 타입 검증 한계](#5-openapi-310-타입-검증-한계)
6. [프로젝트 설정 참조](#6-프로젝트-설정-참조)
7. [FAQ](#7-faq)

---

## 1. 검증 메커니즘 개요

이 프로젝트에는 두 가지 OpenAPI 응답 검증 메커니즘이 존재한다.

### 1.1 MockMvc ResultMatcher (통합 테스트)

```java
mockMvc.perform(get("/api/v1/boards"))
    .andExpect(status().isOk())
    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
```

- **동작 방식**: `OpenApiValidatorUtil.matchesOpenApiSpec()`이 응답 JSON을 OpenAPI 스펙과 비교한다
- **실패 시**: 테스트가 `AssertionError`로 실패한다 (CI에서 빌드 실패)
- **대상**: 2xx 성공 응답만 검증한다. 4xx/5xx 에러 응답과 204 No Content는 검증 대상이 아니다
- **파일 위치**: `backend/src/test/java/igrus/web/common/OpenApiValidatorUtil.java`

### 1.2 OpenApiValidationFilter (런타임 검증)

- **동작 방식**: Servlet Filter가 모든 API 요청/응답을 가로채어 스키마와 비교한다
- **실패 시**: 예외를 던지지 않고 WARN 로그를 출력한다. 응답은 정상 반환된다
- **활성화 조건**: `@Profile({"dev", "test"})` + `openapi.validation.filter.enabled=true`
- **현재 상태**: `application-test.yml`에서 `openapi.validation.filter.enabled=false`로 비활성화 (마이그레이션 완료 후 활성화 예정)
- **파일 위치**: `backend/src/main/java/igrus/web/common/config/OpenApiValidationConfig.java`

---

## 2. 검증 실패 메시지 해석

### 2.1 MockMvc ResultMatcher 실패 메시지

테스트에서 `matchesOpenApiSpec()`이 실패하면 다음과 같은 메시지가 출력된다.

```
java.lang.AssertionError: Validation failed.
[ERROR] /api/v1/boards/{code} Response status=200 does not match...
  - [ERROR] Object has missing required properties (["id","code","name"])
  - [WARN] Object instance has properties which are not allowed by the schema: ["extraField"]
```

**메시지 구조**:
- `[ERROR]` / `[WARN]`: 검증 레벨. ERROR는 테스트를 실패시키고, WARN은 경고만 남긴다
- `/api/v1/boards/{code}`: 검증이 실패한 API 경로
- `Response status=200`: 검증 대상 HTTP 상태 코드
- 하위 메시지: 구체적인 스키마 불일치 내용

### 2.2 LoggingValidationReportHandler 로그 메시지

Filter 활성화 상태에서 스키마 불일치가 발생하면 다음 로그가 출력된다.

```
WARN  [OpenAPI 응답 검증 실패] URI=/api/v1/boards/general, 메시지=
  - [ERROR] Object has missing required properties (["id"])
  - [WARN] Object instance has properties which are not allowed by the schema: ["extraField"]
```

```
WARN  [OpenAPI 요청 검증 실패] URI=/api/v1/boards/general/posts, 메시지=
  - [ERROR] Object has missing required properties (["title","content"])
```

**로그 형식**:
- `[OpenAPI 요청 검증 실패]`: 요청 본문이 스키마와 불일치
- `[OpenAPI 요청 검증 경고]`: 요청에 WARN 레벨 불일치 존재
- `[OpenAPI 응답 검증 실패]`: 응답 본문이 스키마와 불일치
- `[OpenAPI 응답 검증 경고]`: 응답에 WARN 레벨 불일치 존재

### 2.3 주요 에러 메시지 유형

| 메시지 패턴 | 의미 |
|------------|------|
| `Object has missing required properties (["field1","field2"])` | 스키마에서 `required`로 정의된 필드가 응답 JSON에 없음 |
| `Instance type (X) does not match any allowed primitive type (allowed: ["Y"])` | 필드 타입 불일치 (예: 스키마는 `integer`인데 응답은 `string`) |
| `Instance value ("VALUE") not found in enum` | enum 값 불일치 |
| `Object instance has properties which are not allowed by the schema` | `additionalProperties: false`인 스키마에 정의되지 않은 필드가 존재 |
| `A]Instance failed to match at least one required schema among N` | `oneOf`/`anyOf` 스키마 불일치 |

---

## 3. 일반적인 불일치 유형과 해결 방법

### 3.1 필수 필드 누락

**증상**:
```
Object has missing required properties (["id","name"])
```

**원인**: OpenAPI 스키마에서 `required: [id, name]`으로 정의된 필드가 컨트롤러 응답에 포함되지 않음

**진단 순서**:
1. OpenAPI 스키마(`openapi/schemas/*.yaml`)에서 해당 스키마의 `required` 필드 확인
2. 컨트롤러가 반환하는 DTO(generated model)의 필드 매핑 확인
3. 서비스 레이어에서 해당 필드에 값이 설정되는지 확인

**해결 방법**:
- 필드가 실제로 항상 존재해야 하는 경우: 컨트롤러/서비스에서 필드 매핑 수정
- 필드가 선택적(nullable)인 경우: 스키마에서 `required`에서 제거하거나 `nullable: true` 추가

**실제 사례** (이슈 #5):
```yaml
# 문제: UserDetailResponse.joinRoute가 스키마에서 required이지만 실제로는 null일 수 있음
# 해결: nullable: true 추가 필요
UserDetailResponse:
  properties:
    joinRoute:
      type: string
      nullable: true  # <-- 이 속성 추가
```

### 3.2 타입 불일치

**증상**:
```
Instance type (string) does not match any allowed primitive type (allowed: ["integer"])
```

**원인**: 스키마에서 정의한 타입과 실제 직렬화된 JSON 타입이 다름

**흔한 경우**:

| 스키마 타입 | Java 타입 | 직렬화 결과 | 불일치 여부 |
|-----------|----------|------------|:--------:|
| `integer` (format: int64) | `Long` | `12345` | 일치 |
| `string` (format: date-time) | `Instant` | `"2026-03-02T12:00:00Z"` | 일치 |
| `integer` | `String` | `"12345"` | 불일치 |
| `boolean` | `Boolean` | `true` | 일치 |
| `number` | `Integer` | `123` | 확인 필요 |

**Instant 직렬화 확인**:
이 프로젝트는 `java.time.Instant`를 사용하며, Jackson의 `JavaTimeModule`이 ISO-8601 형식(`"2026-03-02T12:00:00Z"`)으로 직렬화한다. OpenAPI 스키마에서 `format: date-time`으로 정의하면 일치한다.

```yaml
# 올바른 Instant 스키마 정의
createdAt:
  type: string
  format: date-time
```

**해결 방법**:
- 스키마의 타입 정의를 Java 직렬화 결과와 일치시킨다
- 또는 컨트롤러에서 올바른 타입으로 변환하여 반환한다

### 3.3 enum 값 불일치

**증상**:
```
Instance value ("ACTIVE_MEMBER") not found in enum (possible values: ["ASSOCIATE","MEMBER","OPERATOR","ADMIN"])
```

**원인**: Java enum의 `name()` 또는 `@JsonValue`가 반환하는 값이 OpenAPI 스키마의 enum 목록에 포함되지 않음

**진단 순서**:
1. Java enum 클래스에서 직렬화 방식 확인 (`@JsonValue` 사용 여부)
2. OpenAPI 스키마의 `enum` 목록 확인
3. 양쪽 값 목록 비교

**해결 방법**:
- Java enum 값이 정확하다면: 스키마의 `enum` 목록에 누락된 값 추가
- 스키마가 정확하다면: Java enum 또는 직렬화 설정 수정

```yaml
# 스키마의 enum 목록
role:
  type: string
  enum:
    - ASSOCIATE
    - MEMBER
    - OPERATOR
    - ADMIN
```

### 3.4 추가 필드 (additionalProperties)

**증상**:
```
Object instance has properties which are not allowed by the schema: ["extraField"]
```

**원인**: 응답 JSON에 스키마에 정의되지 않은 필드가 포함됨

**현재 설정**: `additionalProperties`는 WARN 레벨로 설정되어 있어, MockMvc ResultMatcher에서는 테스트를 실패시키지 않고 경고만 남긴다.

```java
// OpenApiValidatorFactory.createProjectLevelResolver()
.withLevel("validation.response.body.schema.additionalProperties", ValidationReport.Level.WARN)
```

**진단 순서**:
1. 스키마에 정의된 `properties` 목록 확인
2. 컨트롤러가 반환하는 DTO의 필드 목록 확인
3. DTO에 있지만 스키마에 없는 필드 식별

**해결 방법**:
- 필드가 실제로 필요하다면: 스키마의 `properties`에 해당 필드 추가
- 필드가 불필요하다면: DTO 매핑에서 해당 필드를 제외하거나 `@JsonIgnore` 추가
- 스키마에 명시적으로 추가 필드를 허용하려면:
  ```yaml
  ResponseSchema:
    type: object
    additionalProperties: true  # 명시적 허용
  ```

### 3.5 페이지네이션 응답 (Generic Page)

**증상**:
```
Instance type (object) does not match any allowed primitive type (allowed: ["array"])
```
또는 `content` 배열의 아이템 타입이 `object`로만 정의되어 검증이 느슨해지는 경우

**원인**: Spring Data JPA의 `Page` 응답을 Generic `Page` 스키마로 정의할 때, `content` 배열의 아이템 타입이 구체적으로 정의되지 않은 경우

**올바른 스키마 정의 예시**:
```yaml
# 구체적인 타입으로 content를 정의
PageLoginHistoryResponse:
  type: object
  properties:
    content:
      type: array
      items:
        $ref: '#/components/schemas/LoginHistoryResponse'  # 구체적 타입
    totalPages:
      type: integer
    totalElements:
      type: integer
      format: int64
    # ... 나머지 Page 필드
```

**피해야 할 정의**:
```yaml
# content가 object[]로 정의되면 내부 타입 검증 불가
PageResponse:
  type: object
  properties:
    content:
      type: array
      items:
        type: object  # 너무 느슨 -- 어떤 객체든 통과
```

**해결 방법**:
- 각 페이지네이션 응답 스키마에서 `content` 배열의 `items`를 구체적인 스키마로 정의한다
- 이것이 런타임 응답 검증 도입의 핵심 동기 중 하나이다

### 3.6 nullable 필드 처리

**증상**:
```
Instance type (null) does not match any allowed primitive type (allowed: ["string"])
```

**원인**: 응답에 `null` 값이 포함되었지만, 스키마에서 해당 필드가 nullable로 정의되지 않음

**해결 방법**:
```yaml
# OpenAPI 3.1.0에서 nullable 정의
fieldName:
  type:
    - string
    - "null"

# 또는 (3.0 호환)
fieldName:
  type: string
  nullable: true
```

---

## 4. 수정 방향 결정 기준

응답 검증이 실패하면 **스펙을 수정**할 것인가, **컨트롤러 응답을 수정**할 것인가를 결정해야 한다. 다음 기준을 따른다.

### 스펙을 수정해야 하는 경우

| 상황 | 예시 |
|------|------|
| 스펙이 실제 비즈니스 요구사항을 반영하지 못하는 경우 | 필드가 nullable인데 `required`로 정의됨 |
| 프론트엔드가 이미 현재 응답 형식에 맞춰 구현된 경우 | 스펙 변경 시 프론트엔드 재작업 필요 없음 |
| 스키마 정의가 너무 느슨하여 검증 효과가 없는 경우 | `content: object[]`를 구체적 타입으로 변경 |
| enum 값이 추가된 경우 | 새로운 상태값이 Java에 추가되었으나 스펙에 반영 안 됨 |

**스펙 수정 워크플로우**:
1. `openapi/schemas/*.yaml` 또는 `openapi/paths/*.yaml` 수정
2. `./gradlew openApiGenerate`로 인터페이스/모델 재생성
3. 컨트롤러가 새 인터페이스에 맞게 동작하는지 확인
4. `./gradlew test`로 전체 테스트 통과 확인
5. `pnpm api:generate`로 프론트엔드 API 클라이언트 재생성

### 컨트롤러 응답을 수정해야 하는 경우

| 상황 | 예시 |
|------|------|
| 스펙이 API 계약으로서 올바르게 정의된 경우 | 스펙에 `required: [id]`가 맞지만 컨트롤러가 id를 안 넣음 |
| 컨트롤러의 DTO 매핑에 버그가 있는 경우 | 필드명 오타, 잘못된 타입 변환 |
| 스키마에 없는 필드를 불필요하게 반환하는 경우 | 내부 디버그 필드가 응답에 노출됨 |

### 판단이 어려운 경우

1. 프론트엔드 개발자와 협의하여 API 계약을 확정한다
2. 스펙을 먼저 수정하고, 컨트롤러를 스펙에 맞춘다 (Contract-First 원칙)
3. PR 리뷰에서 스펙 변경 사유를 명시한다

---

## 5. OpenAPI 3.1.0 타입 검증 한계

TASK-200 PoC에서 발견된 중요한 한계사항이다.

### 5.1 감지되지 않는 불일치

swagger-request-validator 2.46.0은 OpenAPI 3.1.0 스펙에서 **다음 유형의 불일치를 감지하지 못한다**:

| 불일치 유형 | 예시 | 감지 여부 (3.1.0) | 감지 여부 (3.0.3) |
|-----------|------|:-----------------:|:-----------------:|
| 배열 vs 객체 | 스키마: `array`, 응답: `{}` | 감지 안 됨 | 감지됨 |
| boolean vs string | 스키마: `boolean`, 응답: `"true"` | 감지 안 됨 | 감지됨 |
| integer vs string | 스키마: `integer`, 응답: `"123"` | 감지 안 됨 | 감지됨 |
| required 필드 누락 | 스키마: `required: [id]`, 응답: `{}` | 프로젝트 스키마에 required 미정의 시 미감지 | 감지됨 |
| additionalProperties | 스키마 미명시 시 추가 필드 | 허용됨 (3.1.0 기본값 true) | 설정에 따라 감지 |

### 5.2 원인

swagger-request-validator 2.46.0 내부의 JSON Schema 검증 엔진이 OpenAPI 3.1.0 (JSON Schema 2020-12)을 아직 완전히 지원하지 않는 것으로 추정된다.

### 5.3 대응 방안

현재 프로젝트에서는 **방안 B (현행 3.1.0 유지 + 한계 인지)**를 채택하였다.

- 응답 스키마의 구조적 정합성(필드 존재 여부, 형태)은 검증할 수 있다
- 타입 수준의 정밀 검증은 컴파일 타임(openapi-generator의 인터페이스 `implements`)에 의존한다
- 향후 라이브러리가 3.1.0 지원을 완성하면 자동으로 검증 범위가 확대된다

### 5.4 검증 효과가 있는 항목

한계에도 불구하고, 현재 검증으로 잡을 수 있는 문제:

- 응답 JSON의 구조가 스키마와 완전히 다른 경우 (필드 이름 오타 등)
- `required` 필드가 명시된 스키마에서 필수 필드 누락
- `additionalProperties: false`가 명시된 스키마에서 추가 필드 존재
- enum 값 목록 불일치 (스키마에 `enum`이 명시된 경우)
- 배열/객체 혼동 (스키마에 명시적으로 정의된 경우)

---

## 6. 프로젝트 설정 참조

### 6.1 OpenApiValidatorFactory

**파일**: `backend/src/main/java/igrus/web/common/config/OpenApiValidatorFactory.java`

모든 검증 설정의 **단일 진실점(Single Source of Truth)**이다. 런타임 Filter와 테스트 유틸리티 양쪽에서 이 팩토리를 참조한다.

**LevelResolver 설정**:

| 키 | 레벨 | 사유 |
|----|------|------|
| `validation.request.parameter.query.unexpected` | IGNORE | MockMvc의 `_csrf` 쿼리 파라미터 허용 |
| `validation.request.security.missing` | IGNORE | Spring Security가 인증/인가 담당 |
| `validation.response.body.schema.additionalProperties` | WARN | 스키마 미명시 시 추가 필드 경고만 (향후 ERROR 전환 가능) |

**스펙 파일 경로**: `../openapi/openapi.yaml` (backend/ 디렉토리 기준 상대 경로)

### 6.2 OpenApiValidatorUtil (테스트용)

**파일**: `backend/src/test/java/igrus/web/common/OpenApiValidatorUtil.java`

**사용법**:
```java
import igrus.web.common.OpenApiValidatorUtil;

// 2xx 성공 응답에서만 사용
mockMvc.perform(get("/api/v1/boards"))
    .andExpect(status().isOk())
    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
```

**적용 규칙**:
- 2xx 성공 응답 테스트에만 추가한다
- 4xx/5xx 에러 응답 테스트에는 추가하지 않는다 (에러 응답 스키마 검증은 불필요)
- 204 No Content 응답에는 추가하지 않는다 (응답 본문이 없음)

### 6.3 OpenApiValidationConfig (런타임 Filter)

**파일**: `backend/src/main/java/igrus/web/common/config/OpenApiValidationConfig.java`

**활성화 조건**:
1. `@Profile({"dev", "test"})`: dev 또는 test 프로필에서만 Bean 등록
2. `@ConditionalOnProperty(name = "openapi.validation.filter.enabled", havingValue = "true")`: 프로퍼티 값이 `true`일 때만 활성화

**현재 상태**:
- `application-test.yml`: `openapi.validation.filter.enabled: false` (마이그레이션 완료 후 `true`로 변경)
- prod: `@Profile` 조건 불충족으로 Bean 미등록

**LoggingValidationReportHandler 동작**:
- 검증 실패(ERROR) 시: `WARN [OpenAPI 응답 검증 실패] URI=..., 메시지=...` 로그 출력
- 검증 경고(WARN만) 시: `WARN [OpenAPI 응답 검증 경고] URI=..., 메시지=...` 로그 출력
- 응답은 정상 반환 (예외를 던지지 않음)

### 6.4 검증 레벨 변경 방법

검증 레벨을 변경하려면 `OpenApiValidatorFactory.createProjectLevelResolver()`를 수정한다. 양쪽(테스트 + 런타임)에 동시 적용된다.

```java
// 예: additionalProperties를 ERROR로 격상
.withLevel("validation.response.body.schema.additionalProperties", ValidationReport.Level.ERROR)
```

**사용 가능한 레벨**:
- `IGNORE`: 완전 무시 (검증 건너뜀)
- `INFO`: 정보성 메시지 (테스트 통과)
- `WARN`: 경고 (테스트 통과, 로그 출력)
- `ERROR`: 에러 (테스트 실패)

---

## 7. FAQ

### Q1. 테스트에서 `matchesOpenApiSpec()` 추가 후 실패합니다. 어떻게 해야 하나요?

**A**: 다음 순서로 진단한다.

1. 에러 메시지에서 불일치 유형을 확인한다 (3절 참조)
2. OpenAPI 스키마(`openapi/schemas/*.yaml`)와 실제 응답 JSON을 비교한다
3. 수정 방향을 결정한다 (4절 참조)
4. 스펙 또는 컨트롤러를 수정한다
5. `./gradlew test`로 전체 테스트 통과를 확인한다

### Q2. 스펙 파일을 찾을 수 없다는 에러가 발생합니다.

**A**: `OpenApiValidatorFactory`는 `../openapi/openapi.yaml` 경로로 스펙 파일을 찾는다. Gradle 테스트는 `backend/` 디렉토리에서 실행되므로, 프로젝트 루트의 `openapi/` 디렉토리에 파일이 존재해야 한다.

```
에러: IllegalStateException: OpenAPI 스펙 파일을 찾을 수 없습니다:
  /absolute/path/openapi/openapi.yaml
```

확인 사항:
- `openapi/openapi.yaml` 파일이 존재하는지
- IDE에서 테스트 실행 시 working directory가 `backend/`인지

### Q3. CI에서는 통과하는데 로컬에서 실패합니다 (또는 반대).

**A**: working directory 차이일 가능성이 높다.

- CI: `working-directory: backend`로 명시되어 있어 `../openapi/openapi.yaml` 경로가 정상 해석
- 로컬 IDE: 프로젝트 루트를 working directory로 사용할 수 있어 경로가 달라질 수 있음

IntelliJ IDEA에서 Gradle 테스트 실행 시 working directory를 확인한다:
`Run > Edit Configurations > Working directory: $MODULE_WORKING_DIR$`

### Q4. 새 API를 추가했는데 응답 검증에 실패합니다.

**A**: Contract-First 워크플로우를 따라야 한다.

1. `openapi/` 스펙에 새 API의 path와 response 스키마를 먼저 정의한다
2. `./gradlew openApiGenerate`로 인터페이스/모델을 재생성한다
3. 컨트롤러에서 생성된 인터페이스를 `implements`한다
4. 응답 DTO가 생성된 모델과 일치하도록 매핑한다
5. 테스트에서 `matchesOpenApiSpec()`을 추가하여 검증한다

### Q5. `additionalProperties` 경고가 계속 나옵니다. 무시해도 되나요?

**A**: 현재 설정에서는 WARN 레벨이므로 테스트는 통과한다. 하지만 불필요한 필드가 응답에 포함되고 있다는 의미이므로:

- 불필요한 필드라면: DTO 매핑에서 제거
- 필요한 필드라면: OpenAPI 스키마의 `properties`에 추가
- 향후 `additionalProperties`를 ERROR로 격상할 예정이므로, 가능하면 조기에 해결하는 것이 좋다

### Q6. Filter 로그에 `[OpenAPI 응답 검증 경고]`가 나타납니다.

**A**: LoggingValidationReportHandler가 WARN 레벨의 불일치를 감지한 것이다. 응답은 정상 반환되지만, 스키마 불일치가 존재한다는 의미이므로 원인을 파악하여 해결하는 것을 권장한다.

### Q7. 한 테스트에서만 `matchesOpenApiSpec()`이 실패하고, 같은 API의 다른 테스트는 통과합니다.

**A**: 테스트 데이터에 따라 응답이 달라질 수 있다. 예를 들어:

- nullable 필드가 null인 경우 vs 값이 있는 경우
- enum 값이 다른 경우
- 빈 배열 vs 요소가 있는 배열

실패하는 테스트의 실제 응답 JSON을 `.andDo(print())`로 확인하고, 스키마와 비교한다.
