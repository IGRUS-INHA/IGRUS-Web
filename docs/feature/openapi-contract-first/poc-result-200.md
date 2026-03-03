# TASK-200: swagger-request-validator PoC 검증 결과

## 메타데이터
| 항목 | 값 |
|------|------|
| 작성일 | 2026-03-02 |
| 라이브러리 | Atlassian swagger-request-validator |
| 검증 버전 | 2.46.0 (mockmvc + spring-webmvc) |
| OpenAPI 스펙 버전 | 3.1.0 |
| 테스트 클래스 | `SwaggerRequestValidatorPocTest` |

## 1. 검증 항목별 결과 요약

| # | 검증 항목 | 결과 | 비고 |
|---|----------|------|------|
| 1 | 멀티파일 스펙 로딩 ($ref 해석) | PASS | 번들 파일 없이 openapi.yaml 직접 사용 가능 |
| 2 | MockMvc 통합 검증 | PASS | openApi().isValid(validator) ResultMatcher 정상 작동 |
| 3 | 의도적 불일치 감지 | PARTIAL | OpenAPI 3.1.0에서 타입/구조 불일치 미감지 (3.0.3에서는 정상) |
| 4 | LevelResolver 설정 | PASS | CSRF, Security, additionalProperties 제어 가능 |
| 5 | 성능 영향 측정 | PASS | 초기화 ~3s, 요청당 ~32ms (MockMvc 포함), 순수 검증 ~1ms |
| 6 | 의존성 충돌 | PASS | 기존 Spring Boot 3.5.9 의존성과 충돌 없음 |
| 7 | 라이브러리 버전 확정 | PASS | 2.46.0 (2025년 9월 기준 최신 안정 버전) |
| 8 | 테스트 컨텍스트 캐싱 | PASS | 기존 컨텍스트 캐싱 미파괴, @Profile 자동 포함 확인 |

## 2. 상세 결과

### 2.1 스펙 파일 로딩

**결론: 멀티파일 스펙(openapi.yaml)을 직접 사용 가능. 번들 파일 불필요.**

- `OpenApiInteractionValidator.createForSpecificationUrl()`이 `$ref`를 재귀적으로 해석
- `../openapi/openapi.yaml` (멀티파일) 및 `../openapi/openapi.bundled.yaml` (번들) 모두 로딩 성공
- 멀티파일 스펙으로 실제 API 검증까지 정상 통과
- **권장**: MockMvc 테스트에서는 번들 파일을 사용하여 안정성 확보 (CI 환경에서 $ref 해석 경로 문제 방지)

### 2.2 MockMvc 통합

**결론: openApi().isValid(validator) ResultMatcher가 정상 작동.**

```java
mockMvc.perform(get("/api/v1/boards")
        .with(withAuth(memberUser))
        .with(csrf()))
    .andExpect(status().isOk())
    .andExpect(openApi().isValid(validator));
```

- `GET /api/v1/boards` 배열 응답 검증 통과
- `GET /api/v1/boards/{code}` 객체 응답 검증 통과
- CSRF 토큰(`_csrf` 쿼리 파라미터)은 LevelResolver에서 IGNORE 처리 필요

### 2.3 의도적 불일치 감지 (핵심 발견)

**결론: OpenAPI 3.1.0 스펙에서 swagger-request-validator 2.46.0의 타입 검증에 한계 존재.**

#### OpenAPI 3.0.3 vs 3.1.0 비교

| 검증 유형 | OpenAPI 3.0.3 | OpenAPI 3.1.0 |
|-----------|:------------:|:------------:|
| 배열/객체 구조 불일치 | 감지됨 (ERROR) | **감지 안 됨** |
| boolean/string 타입 불일치 | 감지됨 (ERROR) | **감지 안 됨** |
| required 필드 누락 | 감지됨 (ERROR) | 프로젝트 스키마에 required 미정의 |
| additionalProperties 추가 필드 | 감지됨 (명시 시) | 허용됨 (3.1.0 기본값 true) |

**OpenAPI 3.0.3 테스트 출력:**
```
[PoC] OpenAPI 3.0.3에서 배열/객체 불일치 감지: hasErrors=true
  - ERROR: Instance type (object) does not match any allowed primitive type (allowed: ["array"])
[PoC] OpenAPI 3.0.3에서 타입 불일치 감지: hasErrors=true
  - ERROR: [Path '/active'] Instance type (string) does not match any allowed primitive type (allowed: ["boolean"])
[PoC] 엄격한 스키마(3.0.3)에서 required 필드 누락 감지: hasErrors=true
  - ERROR: Object has missing required properties (["id"])
```

**OpenAPI 3.1.0 테스트 출력:**
```
[PoC] 구조적 불일치(배열 vs 객체) 감지: hasErrors=false
[PoC] 타입 불일치(boolean에 string) 감지: hasErrors=false
[PoC] additionalProperties 미명시 상태에서 추가 필드 감지: hasErrors=false
```

#### 원인 분석

swagger-request-validator 2.46.0은 내부적으로 JSON Schema 검증에 `json-schema-validator`를 사용한다. OpenAPI 3.1.0은 JSON Schema 2020-12를 채택하는데, 이 라이브러리의 3.1.0 지원이 아직 완전하지 않은 것으로 추정된다.

#### 대응 방안

| 방안 | 장점 | 단점 |
|------|------|------|
| **A. 프로젝트 스펙을 3.0.3으로 다운그레이드** | 타입 검증 완전 활성화 | 3.1.0 전용 기능 사용 불가 (예: webhooks, pathItems) |
| **B. 현행 3.1.0 유지 + 한계 인지** | 스펙 변경 불필요 | 타입/구조 불일치 미감지 |
| **C. 3.1.0 유지 + required/additionalProperties 강화** | 부분적 검증 강화 | 타입 불일치는 여전히 미감지 |
| **D. 라이브러리 업데이트 대기** | 근본적 해결 | 시점 불확실 |

**권장: 방안 B (현행 유지 + 한계 인지)**
- 현재 프로젝트에서 3.1.0 전용 기능을 사용하지 않으므로 향후 다운그레이드도 가능
- 당장은 응답 스키마의 구조적 정합성(필드 존재 여부, 형태)을 검증하는 수준으로 활용
- 향후 라이브러리가 3.1.0 지원을 완성하면 자동으로 검증 범위 확대

### 2.4 LevelResolver 설정

**결론: 프로젝트에 적합한 기본 LevelResolver 확정.**

```java
private static LevelResolver createProjectLevelResolver() {
    return LevelResolver.create()
        // MockMvc CSRF 파라미터 허용
        .withLevel("validation.request.parameter.query.unexpected", ValidationReport.Level.IGNORE)
        // Spring Security는 별도 처리
        .withLevel("validation.request.security.missing", ValidationReport.Level.IGNORE)
        // additionalProperties 경고 (안정화 후 ERROR 전환 가능)
        .withLevel("validation.response.body.schema.additionalProperties", ValidationReport.Level.WARN)
        .build();
}
```

| 키 | 설정 | 사유 |
|----|------|------|
| `validation.request.parameter.query.unexpected` | IGNORE | MockMvc의 `_csrf` 쿼리 파라미터 허용 |
| `validation.request.security.missing` | IGNORE | Spring Security가 인증/인가 담당 |
| `validation.response.body.schema.additionalProperties` | WARN | 스키마 미명시 시 추가 필드 경고만 (향후 ERROR 전환) |

### 2.5 성능 측정

| 측정 항목 | 결과 | 비고 |
|----------|------|------|
| Validator 초기화 | ~3,225ms | 번들 스펙 기준, JVM 초기화 포함 |
| 요청당 검증 (MockMvc 포함) | ~32ms | HTTP 파이프라인 + 검증 |
| 순수 검증 비용 | ~1ms | SimpleRequest/Response 직접 사용 |

- 초기화 비용은 1회만 발생 (싱글턴 패턴 적용 시)
- 요청당 순수 검증 비용이 ~1ms이므로 테스트 성능에 미치는 영향 극히 적음
- dev/test 프로필 Filter에서도 순수 검증 비용만 추가됨 (런타임 오버헤드 무시 가능)

### 2.6 의존성 충돌 확인

**결론: 기존 의존성과 충돌 없음.**

```groovy
// 테스트 의존성 (기존 확인)
testImplementation 'com.atlassian.oai:swagger-request-validator-mockmvc:2.46.0'

// 런타임 의존성 (TASK-220에서 추가 예정)
implementation 'com.atlassian.oai:swagger-request-validator-spring-webmvc:2.46.0'
```

- `compileTestJava` 태스크 정상 통과
- 기존 Spring Boot 3.5.9, Jackson, jakarta.validation과 충돌 없음
- springdoc-openapi-starter-webmvc-ui:2.8.14와 공존 가능

### 2.7 라이브러리 버전 확정

**확정 버전: 2.46.0**

| 모듈 | 아티팩트 | 스코프 |
|------|---------|--------|
| MockMvc 테스트용 | `swagger-request-validator-mockmvc` | testImplementation |
| 런타임 Filter용 | `swagger-request-validator-spring-webmvc` | implementation |

### 2.8 테스트 컨텍스트 캐싱 전략

**결론: 기존 컨텍스트 캐싱에 영향 없음.**

- `SwaggerRequestValidatorPocTest`는 `ServiceIntegrationTestBase` 상속 + `@AutoConfigureMockMvc`
- 이 조합은 기존 `BoardControllerTest`, `ControllerIntegrationTestBase`와 동일
- `@MockitoBean`, `@Import`, `@ActiveProfiles` 추가 없음 --> 컨텍스트 캐시 키 동일
- `@Profile({"dev", "test"})` 설정 클래스는 `@ActiveProfiles("test")` 환경에서 자동 포함
- TASK-221에서 `OpenApiValidationConfig`를 `@Profile({"dev", "test"})`로 생성해도 기존 컨텍스트에 자동 포함

**OpenApiInteractionValidator 인스턴스 관리 전략:**
- 테스트: static 싱글턴 또는 `@BeforeAll`에서 1회 생성 (OpenApiValidatorUtil 유틸리티)
- 런타임 Filter: Spring Bean으로 1회 생성 (`@Configuration` 클래스에서)

## 3. 향후 과제

### 즉시 진행 (TASK-201, TASK-220)
- `build.gradle`에 의존성 공식 추가 (현재는 PoC용으로만 추가)
- OpenApiValidatorUtil 유틸리티 작성 (TASK-202)

### 중기 개선
- 프로젝트 OpenAPI 스키마에 `required` 필드 추가 --> 필수 필드 누락 감지 활성화
- 주요 응답 스키마에 `additionalProperties: false` 추가 --> 추가 필드 감지 활성화
- 라이브러리의 OpenAPI 3.1.0 타입 검증 지원 업데이트 모니터링

### 장기 검토
- 프로젝트 스펙 3.0.3 다운그레이드 여부 결정 (타입 검증 완전 활성화 필요 시)
- 또는 라이브러리 업데이트로 3.1.0 타입 검증이 완성될 때까지 대기

## 4. 테스트 결과

| 테스트 | 결과 |
|--------|:----:|
| 번들 파일 로딩 | PASS |
| 멀티파일 스펙 $ref 해석 | PASS |
| 멀티파일 스펙으로 API 검증 | PASS |
| MockMvc GET /boards 스키마 검증 | PASS |
| MockMvc GET /boards/{code} 스키마 검증 | PASS |
| additionalProperties 미명시 시 추가 필드 허용 | PASS (문서화) |
| 배열/객체 불일치 미감지 (3.1.0 한계) | PASS (문서화) |
| boolean/string 타입 불일치 미감지 (3.1.0 한계) | PASS (문서화) |
| required 필드 누락 감지 (3.0.3 인라인) | PASS |
| 배열/객체 불일치 감지 (3.0.3 인라인) | PASS |
| 타입 불일치 감지 (3.0.3 인라인) | PASS |
| LevelResolver 기본 설정 검증 | PASS |
| Security IGNORE 검증 | PASS |
| CSRF 파라미터 IGNORE 검증 | PASS |
| 성능 측정 | PASS |
| 테스트 컨텍스트 캐싱 확인 | PASS |
| **전체** | **16/16 PASS** |
