# OpenAPI 런타임 응답 검증 작업 계획

## 개요

- **기능 설명**: 백엔드 API 응답이 OpenAPI 스펙에 정의된 스키마와 일치하는지를 런타임에 검증하는 메커니즘을 도입한다. 현재 `openapi-generator`를 통한 컴파일 타임 검증(인터페이스 `implements`)과 `useBeanValidation`을 통한 요청 검증은 이루어지고 있으나, **응답(output)이 스키마와 일치하는지는 검증하지 않는다.** 실제로 Generic `Page` 스키마에서 `List<Object>` content가 반환되는 등, 컴파일 타임만으로는 잡히지 않는 스펙 불일치가 존재한다.
- **도입 범위**:
  1. **통합 테스트 응답 스키마 검증** -- MockMvc 기반 통합 테스트에서 API 응답이 OpenAPI 스키마에 부합하는지 자동 검증
  2. **개발 환경 런타임 검증** -- dev/test 프로필에서 Servlet Filter를 통해 모든 응답을 스키마 검증 (프로덕션에서는 비활성화)
  3. **CI 자동 검증** -- 기존 CI 파이프라인에서 통합 테스트 실행 시 응답 검증이 자동으로 수행
- **관련 문서**
  - 기존 마이그레이션 작업 계획: [`task-plan.md`](./task-plan.md) (TASK-010~111)
  - 마이그레이션 가이드: [`migration-guide.md`](./migration-guide.md)
  - 구현 체크리스트: [`implementation-checklist.md`](./implementation-checklist.md)
  - 백엔드 개발 규칙: [`backend/CLAUDE.md`](../../../backend/CLAUDE.md)
  - 테스트 규칙: [`backend/src/test/CLAUDE.md`](../../../backend/src/test/CLAUDE.md)
  - ADR: [`docs/adr/v20260301-openapi_redocly_bundling.md`](../../adr/v20260301-openapi_redocly_bundling.md)
- **작성일**: 2026-03-02
- **최종 수정일**: 2026-03-02 (리뷰 피드백 R1 반영)
- **기술 스택**: Java 21 + Spring Boot 3.5.9 + openapi-generator 7.12.0 + Atlassian swagger-request-validator

---

## 라이브러리 선택 배경

### 후보 라이브러리 비교

| 라이브러리 | 장점 | 단점 | 적합성 |
|-----------|------|------|--------|
| **Atlassian swagger-request-validator** | Spring MockMvc 전용 모듈 제공, Servlet Filter 모듈 제공, OpenAPI 3.0/3.1 지원, 활발한 유지보수 (2025.08 최신 릴리스) | 추가 의존성, 멀티파일 스펙 처리 확인 필요 | **채택 후보** |
| openapi4j | 경량, 독립적 | 2021년 이후 유지보수 중단 | 부적합 |
| 커스텀 ResultMatcher | 추가 의존성 없음 | JSON Schema 검증 직접 구현 필요, 유지보수 부담 | 부적합 |

### 채택 라이브러리: Atlassian swagger-request-validator

**이유**:
1. `swagger-request-validator-mockmvc` 모듈이 Spring MockMvc와 직접 통합되어, 기존 통합 테스트에 `openApi().isValid(validator)` 한 줄 추가로 응답 검증 가능
2. `swagger-request-validator-spring-webmvc` 모듈이 `OpenApiValidationFilter` (Servlet Filter) + `OpenApiValidationInterceptor`를 제공하여, 개발 환경 런타임 검증 구현이 간단
3. Spring Boot 3 / Jakarta EE / JDK 17+ 호환 확인됨
4. OpenAPI 3.0 스펙 파싱, allOf/oneOf/anyOf, 추가 필드 제어 등 세밀한 검증 옵션 제공

**버전**: 2.46.0 이상 (TASK-200 PoC에서 최종 호환 버전 확정)

### 스펙 파일 처리 전략

현재 프로젝트의 OpenAPI 스펙은 멀티파일 구조(`openapi/openapi.yaml` -> `paths/*.yaml` -> `schemas/*.yaml`)이다. swagger-request-validator는 단일 파일 또는 URL 기반 스펙 로딩을 기본으로 하므로, 다음 전략을 사용한다:

- **테스트**: `openapi/openapi.yaml` 경로를 직접 지정하되, `ParseOptions.resolve(true)` 설정으로 `$ref` 자동 해석 시도. 실패 시 `redocly bundle`로 생성한 번들 파일 사용
- **개발 환경 Filter**: classpath에 번들 파일을 배치하거나, `ParseOptions.resolve(true)`로 멀티파일 해석

---

## 작업 목록

### 0. PoC 검증

#### TASK-200: swagger-request-validator PoC 검증

- **작업 ID**: TASK-200
- **작업명**: Atlassian swagger-request-validator 라이브러리 PoC 검증
- **설명**: swagger-request-validator가 현재 프로젝트 환경에서 정상 동작하는지 검증한다. 구체적으로 다음 항목을 확인한다:
  1. **스펙 파일 로딩**: 멀티파일 구조(`openapi/openapi.yaml`)를 `OpenApiInteractionValidator`가 `$ref` 해석하여 정상 로딩하는지 확인. 실패 시 번들 파일(`openapi.bundled.yaml`)로 대안 검증
  2. **MockMvc 통합**: 기존 `BoardControllerTest`에 `openApi().isValid(validator)` ResultMatcher를 추가하여, 정상 응답(200)에 대한 스키마 검증이 통과하는지 확인
  3. **의도적 불일치 감지**: 응답 JSON에 스키마에 없는 필드를 추가하거나, 필수 필드를 누락시켰을 때 검증이 실패하는지 확인
  4. **LevelResolver 설정**: `additionalProperties` 경고 레벨, 응답 전용 검증 설정 등 프로젝트에 적합한 옵션 확정
  5. **성능 영향**: 테스트 실행 시간 증가분 측정 (validator 초기화 비용, 요청당 검증 비용)
  6. **의존성 충돌**: 기존 `build.gradle` 의존성과의 충돌 여부 확인 (jackson, swagger-parser 등)
  7. **라이브러리 버전 확정**: 2.46.0을 기준으로 호환성을 검증하고, 문제 발생 시 적합한 버전을 확정
  8. **테스트 컨텍스트 캐싱 전략 검증**: 아래 "테스트 컨텍스트 캐싱 전략" 절에서 확정된 기본 전략이 실제로 동작하는지 검증 (상세 항목은 해당 절 참조)
- **PoC 산출물**: 검증 결과 문서, 확정된 라이브러리 버전, `OpenApiInteractionValidator` 설정 코드, 테스트 컨텍스트 캐싱 전략 검증 결과
- **관련 검증 기준**: 없음 (인프라 준비 단계)
- **관련 테스트 케이스**: 없음
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 1. 통합 테스트 인프라 구축

#### TASK-201: build.gradle에 swagger-request-validator 테스트 의존성 추가

- **작업 ID**: TASK-201
- **작업명**: build.gradle에 swagger-request-validator-mockmvc 테스트 의존성 추가
- **설명**: TASK-200 PoC 결과를 반영하여, `build.gradle`에 다음 테스트 의존성을 추가한다:
  ```groovy
  testImplementation 'com.atlassian.oai:swagger-request-validator-mockmvc:<PoC 확정 버전>'
  ```
  이 의존성은 `swagger-request-validator-core`를 transitive로 포함한다. 의존성 추가 후 `./gradlew dependencies`로 충돌이 없는지 확인하고, `./gradlew compileJava compileTestJava`가 성공하는지 검증한다.
- **관련 검증 기준**: 없음 (인프라 준비)
- **관련 테스트 케이스**: 없음
- **선행 작업**: TASK-200
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-202: OpenApiValidator 테스트 유틸리티 클래스 작성

- **작업 ID**: TASK-202
- **작업명**: MockMvc 통합 테스트용 OpenApiInteractionValidator 팩토리 유틸리티 작성
- **설명**: 모든 컨트롤러 통합 테스트에서 공통으로 사용할 `OpenApiInteractionValidator` 인스턴스를 생성/관리하는 유틸리티를 작성한다. 핵심 요구사항:
  1. **싱글턴 패턴**: validator 초기화 비용이 높으므로, 스펙 파일 파싱은 한 번만 수행하고 모든 테스트에서 재사용
  2. **스펙 파일 경로**: `openapi/openapi.yaml` (프로젝트 루트 기준 상대 경로) 또는 TASK-200에서 확정된 경로
  3. **LevelResolver 설정**: TASK-200에서 확정된 검증 레벨 (예: `additionalProperties` 무시 여부)
  4. **ParseOptions 설정**: `resolve(true)`, `resolveCombinators(true)` 등 TASK-200에서 확정된 옵션
  5. **ResultMatcher 헬퍼**: `openApi().isValid(validator)`를 간결하게 호출할 수 있는 정적 메서드 제공
  - 위치: `src/test/java/igrus/web/common/OpenApiValidatorUtil.java`
  - 사용 예시:
    ```java
    mockMvc.perform(get("/api/v1/boards"))
        .andExpect(status().isOk())
        .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    ```
- **관련 검증 기준**: 없음 (인프라 준비)
- **관련 테스트 케이스**: 없음
- **선행 작업**: TASK-201
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 2. 기존 통합 테스트에 응답 검증 적용

#### TASK-210: 커뮤니티 도메인 컨트롤러 테스트에 OpenAPI 응답 검증 추가

- **작업 ID**: TASK-210
- **작업명**: 커뮤니티 도메인 통합 테스트(Board, Post, Comment, Like, Bookmark, CommentReport)에 OpenAPI 응답 스키마 검증 추가
- **설명**: 기존 컨트롤러 통합 테스트의 각 MockMvc 호출에 `OpenApiValidatorUtil.matchesOpenApiSpec()` ResultMatcher를 추가한다. 대상 테스트 파일:
  - `BoardControllerTest.java` (5개 테스트)
  - `PostControllerTest.java`
  - `CommentControllerTest.java`
  - `PostLikeControllerTest.java`
  - `CommentLikeControllerTest.java`
  - `BookmarkControllerTest.java`
  - `CommentReportControllerTest.java`
  - 검증 실패 시 응답 스키마 불일치를 식별하고, OpenAPI 스펙 또는 컨트롤러 응답을 수정하여 일치시킨다.
- **관련 검증 기준**: CR-201 (통합 테스트 응답 검증)
- **관련 테스트 케이스**: TC-210-01 ~ TC-210-07
- **선행 작업**: TASK-202, 해당 컨트롤러 마이그레이션 완료 (TASK-010~016)
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-211: Admin 도메인 컨트롤러 테스트에 OpenAPI 응답 검증 추가

- **작업 ID**: TASK-211
- **작업명**: Admin 도메인 통합 테스트(Dashboard, User, Member, LoginHistory, StatusHistory)에 OpenAPI 응답 스키마 검증 추가
- **설명**: Admin 도메인 기존 컨트롤러 통합 테스트에 응답 검증을 추가한다. 대상 테스트 파일:
  - `AdminDashboardControllerTest.java`
  - `AdminUserControllerTest.java`
  - `AdminMemberControllerTest.java`
  - `AdminLoginHistoryControllerTest.java`
  - `AdminAccountStatusChangeHistoryControllerTest.java`
  - **특히 주의**: `AdminLoginHistoryController`와 `AdminAccountStatusChangeHistoryController`는 Generic `Page` 스키마를 사용하며, 현재 `List<Object>` content가 반환될 수 있어 스키마 불일치가 예상된다. 이 불일치를 검증으로 탐지하고 수정한다.
- **관련 검증 기준**: CR-201
- **관련 테스트 케이스**: TC-211-01 ~ TC-211-05
- **선행 작업**: TASK-202, 해당 컨트롤러 마이그레이션 완료 (TASK-030~035)
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-212: 행사 도메인 컨트롤러 테스트에 OpenAPI 응답 검증 추가

- **작업 ID**: TASK-212
- **작업명**: 행사 도메인 통합 테스트(Event, EventRegistration)에 OpenAPI 응답 스키마 검증 추가
- **설명**: 행사 도메인 기존 컨트롤러 통합 테스트에 응답 검증을 추가한다. 대상 테스트 파일:
  - `EventControllerIntegrationTest.java`
  - `EventRegistrationControllerIntegrationTest.java`
- **관련 검증 기준**: CR-201
- **관련 테스트 케이스**: TC-212-01 ~ TC-212-02
- **선행 작업**: TASK-202, 해당 컨트롤러 마이그레이션 완료 (TASK-040~041)
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-213: 테스트 미존재 컨트롤러에 대한 최소 응답 검증 테스트 작성

- **작업 ID**: TASK-213
- **작업명**: 통합 테스트가 없는 컨트롤러에 대한 최소한의 OpenAPI 응답 검증 스모크 테스트 작성
- **설명**: 현재 통합 테스트가 존재하지 않는 컨트롤러(15개)에 대해, 주요 엔드포인트의 응답이 OpenAPI 스키마와 일치하는지 확인하는 최소한의 스모크 테스트를 작성한다. 각 컨트롤러당 1~2개의 대표적인 성공 응답 시나리오만 검증한다.
  - 대상 컨트롤러 및 선행 마이그레이션 TASK 매핑 (아래 매핑 테이블 참조):
    - `PinnedPostController` (4개 엔드포인트)
    - `PasswordAuthController` (16개 엔드포인트 -- 로그인/회원가입 등 핵심 시나리오만)
    - `AdminInquiryController` (7개 엔드포인트)
    - `GuestInquiryController` (2개 엔드포인트)
    - `MemberInquiryController` (3개 엔드포인트)
    - `MyPageController` (12개 엔드포인트 -- 프로필 조회 등 핵심만)
    - `SurveyController` (13개 엔드포인트 -- 목록/상세 조회 등)
    - `SurveyQuestionController` (4개 엔드포인트)
    - `SurveyQuestionOptionController` (4개 엔드포인트)
    - `SurveyQuestionRowController` (4개 엔드포인트)
    - `SurveyResponseController` (3개 엔드포인트)
    - `SurveyAnonymousResponseController` (1개 엔드포인트)
    - `PrivacyConsentController` (5개 엔드포인트)
    - `SemesterMemberController` (2개 엔드포인트)
    - `AdminSemesterMemberController` (3개 엔드포인트)
  - 테스트 구조: `ControllerIntegrationTestBase` 상속 (컨텍스트 캐싱 규칙 준수)
  - **우선순위 주의**: 이 작업은 해당 컨트롤러의 Contract-First 마이그레이션이 완료된 이후에 진행한다. 마이그레이션이 아직 진행 중인 컨트롤러는 먼저 마이그레이션을 완료한 뒤 검증 테스트를 작성한다.

##### 컨트롤러-선행 마이그레이션 TASK 매핑 테이블

| # | 컨트롤러 | 선행 마이그레이션 TASK | 마이그레이션 Phase |
|---|---------|---------------------|:--:|
| 1 | `PinnedPostController` | TASK-015 | Phase 3 |
| 2 | `PasswordAuthController` | TASK-020 | Phase 6 |
| 3 | `AdminInquiryController` | TASK-034 | Phase 5 |
| 4 | `GuestInquiryController` | TASK-050 | Phase 5 |
| 5 | `MemberInquiryController` | TASK-050 | Phase 5 |
| 6 | `MyPageController` | TASK-060 | Phase 6 |
| 7 | `SurveyController` | TASK-070 | Phase 6 |
| 8 | `SurveyQuestionController` | TASK-071 | Phase 4 |
| 9 | `SurveyQuestionOptionController` | TASK-071 | Phase 4 |
| 10 | `SurveyQuestionRowController` | TASK-071 | Phase 4 |
| 11 | `SurveyResponseController` | TASK-072 | Phase 3 |
| 12 | `SurveyAnonymousResponseController` | TASK-072 | Phase 3 |
| 13 | `PrivacyConsentController` | TASK-080 | Phase 2 |
| 14 | `SemesterMemberController` | TASK-081 | Phase 2 |
| 15 | `AdminSemesterMemberController` | TASK-035 | Phase 2 |

> **부분 적용 전략**: Phase 2~3 완료 시점에서 해당 컨트롤러(#11~15)의 스모크 테스트를 먼저 작성하고, Phase 4~6 완료 시점에서 나머지 컨트롤러(#1~10)의 스모크 테스트를 추가할 수 있다.

- **관련 검증 기준**: CR-201, CR-202
- **관련 테스트 케이스**: TC-213-01 ~ TC-213-15
- **선행 작업**: TASK-202, 각 컨트롤러의 마이그레이션 완료 (위 매핑 테이블 참조)
- **구현 범위**: backend
- **예상 난이도**: 상 (대상 컨트롤러 수가 많고, 테스트 데이터 세팅 필요)

---

### 3. 개발 환경 런타임 검증

#### TASK-220: swagger-request-validator-spring-webmvc 런타임 의존성 추가

- **작업 ID**: TASK-220
- **작업명**: build.gradle에 swagger-request-validator-spring-webmvc 런타임 의존성 추가
- **설명**: 개발 환경에서 Servlet Filter 기반 응답 검증을 위해 다음 의존성을 추가한다:
  ```groovy
  implementation 'com.atlassian.oai:swagger-request-validator-spring-webmvc:<PoC 확정 버전>'
  ```

  ##### 의존성 스코프 결정: `implementation` (근거 및 리스크 분석)

  **`implementation`을 선택한 근거**:
  1. `OpenApiValidationConfig` 설정 클래스가 `OpenApiValidationFilter`, `OpenApiValidationInterceptor` 등 라이브러리 타입을 직접 참조하여 Bean을 생성한다. **컴파일 타임에 해당 타입이 classpath에 있어야** 하므로 `testImplementation`으로는 메인 소스 컴파일이 불가능하다.
  2. `runtimeOnly`도 컴파일 타임 참조가 불가하므로 부적합하다.

  **프로덕션 포함 리스크 분석**:
  | 항목 | 영향 |
  |------|------|
  | JAR 크기 | swagger-request-validator-spring-webmvc 및 transitive 의존성이 포함되어 JAR 크기가 수 MB 증가 |
  | 런타임 성능 | `@Profile({"dev", "test"})` 조건으로 prod에서는 Bean이 **등록되지 않으므로** 런타임 성능 영향 제로 |
  | 클래스 로딩 | 설정 클래스에 `@Profile`이 붙어 있으므로 prod에서 Spring이 해당 클래스를 Bean으로 등록하지 않음. 클래스 자체는 classpath에 존재하나 인스턴스화되지 않음 |
  | 보안 | 검증 라이브러리이므로 보안 위험 없음. API 엔드포인트 노출 없음 |
  | 의존성 충돌 | swagger-parser, jackson 등 transitive 의존성이 기존 것과 충돌 가능. TASK-200 PoC에서 확인 |

  **대안 검토 및 기각 사유**:
  - `testImplementation`으로 변경: `OpenApiValidationConfig` 설정 클래스를 `src/test/`로 이동해야 하지만, dev 프로필(로컬 개발 환경)에서도 Filter가 필요하므로 test 전용 배치는 부적합
  - Gradle `developmentOnly` 스코프: Spring Boot DevTools 전용 스코프로, `bootJar` 빌드 시 제외되어 dev 서버 배포 시 누락됨

  **결론**: JAR 크기 수 MB 증가 외에는 실질적 리스크가 없으므로 `implementation` 유지. 향후 모듈 분리 시 재검토 가능.

- **관련 검증 기준**: CR-203
- **관련 테스트 케이스**: 없음
- **선행 작업**: TASK-200
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-221: 개발/테스트 프로필 전용 OpenApiValidationFilter 설정

- **작업 ID**: TASK-221
- **작업명**: dev/test 프로필에서만 활성화되는 OpenApiValidationFilter + OpenApiValidationInterceptor 설정 클래스 작성
- **설명**: `@Profile({"dev", "test"})` 조건으로 활성화되는 설정 클래스를 작성하여, 모든 API 응답이 OpenAPI 스키마에 부합하는지 런타임에 검증한다.
  - **구현 내용**:
    1. `OpenApiValidationFilter` Bean 등록 -- 요청/응답 검증 활성화
    2. `OpenApiValidationInterceptor` Bean 등록 -- `WebMvcConfigurer`를 통해 인터셉터 등록
    3. 스펙 파일 경로: `classpath:` 또는 파일시스템 경로 (TASK-200에서 확정)
    4. LevelResolver 설정: 테스트용과 동일하거나 더 관대한 레벨
  - **위치**: `igrus.web.common.config.OpenApiValidationConfig`
  - **검증 실패 시 동작**:
    - 개발 환경(dev): 응답은 정상 반환하되, WARN 레벨 로그로 스키마 불일치 상세 메시지 출력
    - 테스트 환경(test): 검증 실패 시 500 에러 반환 또는 로그 기록 (PoC에서 동작 방식 확정)
  - **프로덕션(prod) 보장**: `@Profile({"dev", "test"})`이므로 prod 프로필에서는 Bean이 등록되지 않음. 성능 영향 제로.
  - **스펙 파일 제공**: 개발 환경에서 번들 파일(`openapi.bundled.yaml`)을 classpath에 포함시키거나, 파일시스템 경로로 직접 참조하는 방법 중 하나를 선택 (TASK-200 PoC에서 확정)
- **관련 검증 기준**: CR-203, CR-204
- **관련 테스트 케이스**: TC-221-01 ~ TC-221-03
- **선행 작업**: TASK-220, TASK-200
- **구현 범위**: backend
- **예상 난이도**: 중

##### 테스트 컨텍스트 캐싱 전략 (확정)

`@Profile({"dev", "test"})` Filter가 활성화되면, `@ActiveProfiles("test")`를 사용하는 기존 모든 통합 테스트에 Filter가 자동 적용된다. 이로 인해 마이그레이션 미완료 컨트롤러의 테스트에서 응답 검증 실패가 발생할 수 있다.

**기본 전략: "마이그레이션 완전 완료 후 Filter 활성화"**

| 단계 | Filter 상태 | 검증 방식 | 시점 |
|------|:----------:|----------|------|
| **마이그레이션 진행 중** | 비활성화 | MockMvc ResultMatcher 방식만 사용 (`OpenApiValidatorUtil.matchesOpenApiSpec()`) | TASK-210~213 |
| **마이그레이션 완전 완료 후** | 활성화 | Filter + ResultMatcher 병행 | TASK-090 이후 |

**이 전략을 선택한 근거**:
1. `backend/src/test/CLAUDE.md` 3절의 "서브클래스에서 `@ActiveProfiles` 추가 금지" 규칙을 위반하지 않음. 별도 프로필(`test-validation`)을 만들면 이 규칙과 충돌하고 컨텍스트 캐싱이 깨짐
2. URL 패턴 기반 선택적 검증은 마이그레이션 진행 상황에 따라 패턴을 계속 업데이트해야 하므로 유지보수 부담이 큼
3. MockMvc ResultMatcher 방식은 개별 테스트 메서드에 명시적으로 추가하므로, 마이그레이션 완료된 컨트롤러 테스트에만 선택적으로 적용 가능

**TASK-200 PoC에서 검증할 컨텍스트 캐싱 관련 항목**:
1. `@Profile({"dev", "test"})` 설정 클래스가 `@ActiveProfiles("test")`인 기존 `ControllerIntegrationTestBase` 컨텍스트에 자동 포함되는지 확인
2. Filter가 자동 포함될 경우, 마이그레이션 미완료 컨트롤러 테스트에서 실제 검증 실패가 발생하는지 확인
3. 검증 실패 시 테스트가 실패하는지(500 에러) 또는 WARN 로그만 남기는지 -- `LevelResolver` 설정으로 제어 가능한지 확인
4. 필요 시 Filter를 비활성화할 수 있는 `@ConditionalOnProperty` 스위치 추가 방안 검토 (예: `openapi.validation.filter.enabled=false`를 `application-test.yml`에 설정)

**기각된 대안**:
- ~~별도 프로필(`test-validation`) 사용~~: 컨텍스트 캐싱 규칙 위반
- ~~URL 패턴 제한 검증~~: 마이그레이션 진행에 따른 패턴 유지보수 부담
- ~~`@MockitoBean`으로 Filter 비활성화~~: 컨텍스트 캐싱 규칙 위반

#### TASK-222: OpenApiValidationFilter 동작 검증 통합 테스트 작성

- **작업 ID**: TASK-222
- **작업명**: OpenApiValidationFilter가 dev/test 프로필에서만 활성화되고, 응답 스키마 불일치를 감지하는지 검증하는 통합 테스트 작성
- **설명**: Filter 설정이 올바르게 동작하는지 확인하는 통합 테스트를 작성한다:
  1. test 프로필에서 Filter Bean이 등록되는지 확인
  2. 정상 API 호출 시 검증이 통과하는지 확인
  3. 검증 실패 시 로그에 경고가 출력되는지 또는 적절한 에러가 반환되는지 확인
  - **컨텍스트 캐싱 준수**: 이 테스트는 `ControllerIntegrationTestBase`를 상속하여 기존 테스트 컨텍스트를 재사용한다. `@ActiveProfiles` 추가, `@MockitoBean` 사용, `@Import` 추가를 하지 않는다.
- **관련 검증 기준**: CR-203, CR-204
- **관련 테스트 케이스**: TC-222-01 ~ TC-222-02
- **선행 작업**: TASK-221
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 4. CI 파이프라인 연동

#### TASK-230: CI에서 OpenAPI 응답 검증 테스트 자동 실행 확인

- **작업 ID**: TASK-230
- **작업명**: CI 파이프라인에서 OpenAPI 응답 검증 통합 테스트가 자동 실행되는지 확인 및 필요 시 조정
- **설명**: 현재 backend CI(`backend-ci.yaml`)는 `./gradlew test`를 실행하므로, TASK-210~213에서 추가/수정한 통합 테스트가 자동으로 CI에서 실행된다. 다만 다음 사항을 확인하고 필요 시 조정한다:
  1. **스펙 파일 접근**: CI 환경에서 `openapi/openapi.yaml` 파일에 접근 가능한지 확인 (현재 checkout@v4로 전체 저장소를 클론하므로 가능할 것으로 예상)
  2. **번들 파일 필요 시**: 테스트가 번들 파일을 요구하는 경우, CI에 `redocly bundle` 단계 추가 필요 여부 확인
  3. **테스트 실행 시간**: 응답 검증 추가로 인한 CI 실행 시간 증가분 모니터링
  4. **openapi/ 변경 감지**: TASK-110(기존 작업 계획)에서 `openapi/` 변경 시 backend CI 트리거를 추가하면, 스펙 변경 시에도 응답 검증 테스트가 자동 실행되어 스펙-응답 불일치를 사전 탐지
- **관련 검증 기준**: CR-205
- **관련 테스트 케이스**: 없음
- **선행 작업**: TASK-210 (최소 1개 이상의 응답 검증 테스트 존재)
- **구현 범위**: devops
- **예상 난이도**: 하

#### TASK-231: 스펙 불일치 발견 시 대응 가이드 문서화

- **작업 ID**: TASK-231
- **작업명**: OpenAPI 응답 검증 실패 시 원인 파악 및 수정 가이드 문서 작성
- **설명**: 개발자가 응답 검증 실패를 만났을 때 참조할 트러블슈팅 가이드를 작성한다:
  1. **검증 실패 메시지 해석 방법**: swagger-request-validator의 `ValidationReport` 메시지 형식과 주요 에러 유형 설명
  2. **일반적인 불일치 유형과 해결 방법**:
     - 필수 필드 누락: 스키마의 `required` 확인 vs 컨트롤러 응답 매핑 점검
     - 타입 불일치: `integer` vs `string`, `Instant` 직렬화 형식 등
     - enum 값 불일치: 스키마 enum 정의 vs Java enum 직렬화
     - 추가 필드: `additionalProperties: false` 설정과 DTO 매핑 점검
     - 페이지네이션 응답: Generic `Page` 스키마의 `content` 배열 타입 정합성
  3. **수정 방향 결정 기준**: 스펙을 수정할 것인가, 컨트롤러 응답을 수정할 것인가의 판단 기준
  - **위치**: `docs/feature/openapi-contract-first/response-validation-troubleshooting.md`
- **관련 검증 기준**: 없음 (문서)
- **관련 테스트 케이스**: 없음
- **선행 작업**: TASK-210 (실제 검증 실패 사례 수집 후)
- **구현 범위**: docs
- **예상 난이도**: 하

---

## 작업 순서 및 의존성

### 의존성 다이어그램

```
TASK-200 (PoC 검증)
    |
    +---> TASK-201 (테스트 의존성 추가)
    |         |
    |         +---> TASK-202 (테스트 유틸리티)
    |                   |
    |                   +---> TASK-210 (커뮤니티 테스트 검증) ----+
    |                   |                                        |
    |                   +---> TASK-211 (Admin 테스트 검증) ------+
    |                   |                                        |
    |                   +---> TASK-212 (행사 테스트 검증) --------+---> TASK-231 (가이드 문서)
    |                   |                                        |
    |                   +---> TASK-213 (미존재 테스트 작성) ------+
    |                                                            |
    +---> TASK-220 (런타임 의존성 추가)                          +---> TASK-230 (CI 확인)
              |
              +---> TASK-221 (Filter 설정)
                        |
                        +---> TASK-222 (Filter 검증 테스트)
```

### 기존 마이그레이션 작업과의 의존성

```
기존 TASK-010~016 (커뮤니티 마이그레이션)  --+---> TASK-210 (커뮤니티 검증)
기존 TASK-030~035 (Admin 마이그레이션)      --+---> TASK-211 (Admin 검증)
기존 TASK-040~041 (행사 마이그레이션)       --+---> TASK-212 (행사 검증)
기존 TASK-020~081 (전체 마이그레이션)       --+---> TASK-213 (미존재 테스트 작성)
기존 TASK-110  (CI 확장)                    --+---> TASK-230 (CI 확인)
```

**핵심**: TASK-210~213의 응답 검증 테스트는 해당 컨트롤러의 Contract-First 마이그레이션이 완료된 후에 수행해야 한다. 마이그레이션 전 컨트롤러는 수동 Swagger 어노테이션이 남아있고, 생성된 모델 DTO가 아닌 내부 DTO를 반환하므로 스키마 불일치가 당연히 발생한다.

### 권장 실행 순서 (병렬 가능 그룹)

| 순서 | 작업 | 병렬 가능 여부 | 비고 |
|------|------|:------------:|------|
| **Phase A** | TASK-200 (PoC 검증) | 단독 | 라이브러리 버전 확정, 컨텍스트 캐싱 전략 검증 포함. 마이그레이션 작업과 독립적으로 선행 가능 |
| **Phase B** | TASK-201 (테스트 의존성) + TASK-220 (런타임 의존성) | 2개 병렬 | 둘 다 build.gradle 수정이지만 scope이 다름 (test vs implementation) |
| **Phase C** | TASK-202 (테스트 유틸리티) + TASK-221 (Filter 설정) | 2개 병렬 | 서로 독립적 |
| **Phase D** | TASK-210~212 (기존 테스트 검증) + TASK-222 (Filter 검증) | 4개 병렬 | 각각 해당 컨트롤러 마이그레이션 완료 필요 |
| **Phase E** | TASK-213 (미존재 테스트 작성) | 단독 | 전체 마이그레이션 완료 후 또는 부분적으로 진행 (매핑 테이블 참조) |
| **Phase F** | TASK-230 (CI 확인) + TASK-231 (가이드 문서) | 2개 병렬 | Phase D 이후 |

**마이그레이션과의 병렬 실행 전략**:
- Phase A(PoC)는 마이그레이션 Phase 0~2와 동시에 진행 가능
- Phase B~C는 마이그레이션 Phase 3~4와 동시에 진행 가능
- Phase D의 TASK-210은 마이그레이션 Phase 4 이후, TASK-211은 Phase 4~5 이후
- Phase E의 TASK-213은 마이그레이션 Phase 7(모든 컨트롤러 완료) 이후가 이상적이나, 부분 적용도 가능 (TASK-213 매핑 테이블 참조)

---

## 검증 기준 (Acceptance Criteria)

| ID | 설명 | 관련 TASK |
|----|------|----------|
| CR-201 | 기존 통합 테스트가 있는 모든 컨트롤러(14개)에서 OpenAPI 응답 스키마 검증이 추가되고 통과한다 | TASK-210~212 |
| CR-202 | 통합 테스트가 없었던 컨트롤러(15개)에 대해 최소 1개 이상의 응답 검증 스모크 테스트가 존재한다 | TASK-213 |
| CR-203 | dev/test 프로필에서 OpenApiValidationFilter가 활성화되어 모든 API 응답을 스키마 검증한다 | TASK-221, TASK-222 |
| CR-204 | prod 프로필에서 OpenApiValidationFilter가 비활성화되어 성능에 영향이 없다 | TASK-221 |
| CR-205 | CI에서 `./gradlew test` 실행 시 응답 검증 테스트가 자동으로 포함되어 실행된다 | TASK-230 |
| CR-206 | 응답 검증 실패 시 원인과 수정 방향을 판단할 수 있는 가이드 문서가 존재한다 | TASK-231 |

### 컨트롤러 수 정합성 (task-plan.md 기준)

| 항목 | 수량 | 출처 |
|------|:----:|------|
| 마이그레이션 대상 컨트롤러 (전체) | 29개 | task-plan.md 통계 요약 |
| 기존 통합 테스트 보유 컨트롤러 | 14개 | migration-guide.md 테스트 파일 목록 |
| 기존 통합 테스트 미보유 컨트롤러 | 15개 | 29 - 14 = 15 |
| 마이그레이션 완료 레퍼런스 (Health, Storage) | 2개 | migration-guide.md (런타임 검증 대상 아님) |

> **참고**: "29개 컨트롤러"는 task-plan.md의 마이그레이션 대상 컨트롤러를 의미한다. 이미 마이그레이션이 완료된 `HealthController`와 `StorageController`(2개)는 레퍼런스로 별도 관리되며, 이 작업 계획의 검증 대상에는 포함되지 않는다. 필요 시 TASK-210~213 완료 후 해당 2개 컨트롤러의 검증 테스트를 별도로 추가할 수 있다.

### 테스트 보유 컨트롤러 상세 목록 (14개)

| # | 컨트롤러 | 테스트 파일 | 도메인 그룹 | 검증 TASK |
|---|---------|-----------|:---:|:---:|
| 1 | `BoardController` | `BoardControllerTest.java` | 커뮤니티 | TASK-210 |
| 2 | `PostController` | `PostControllerTest.java` | 커뮤니티 | TASK-210 |
| 3 | `CommentController` | `CommentControllerTest.java` | 커뮤니티 | TASK-210 |
| 4 | `PostLikeController` | `PostLikeControllerTest.java` | 커뮤니티 | TASK-210 |
| 5 | `CommentLikeController` | `CommentLikeControllerTest.java` | 커뮤니티 | TASK-210 |
| 6 | `BookmarkController` | `BookmarkControllerTest.java` | 커뮤니티 | TASK-210 |
| 7 | `CommentReportController` | `CommentReportControllerTest.java` | 커뮤니티 | TASK-210 |
| 8 | `AdminDashboardController` | `AdminDashboardControllerTest.java` | Admin | TASK-211 |
| 9 | `AdminUserController` | `AdminUserControllerTest.java` | Admin | TASK-211 |
| 10 | `AdminMemberController` | `AdminMemberControllerTest.java` | Admin | TASK-211 |
| 11 | `AdminLoginHistoryController` | `AdminLoginHistoryControllerTest.java` | Admin | TASK-211 |
| 12 | `AdminAccountStatusChangeHistoryController` | `AdminAccountStatusChangeHistoryControllerTest.java` | Admin | TASK-211 |
| 13 | `EventController` | `EventControllerIntegrationTest.java` | 행사 | TASK-212 |
| 14 | `EventRegistrationController` | `EventRegistrationControllerIntegrationTest.java` | 행사 | TASK-212 |

---

## 테스트 케이스

### TC-210: 커뮤니티 도메인 응답 검증

| TC ID | 테스트 설명 | 검증 내용 |
|-------|-----------|----------|
| TC-210-01 | BoardController GET /boards 응답 스키마 검증 | 배열 응답의 각 요소가 BoardListResponse 스키마와 일치 |
| TC-210-02 | BoardController GET /boards/{code} 응답 스키마 검증 | 단건 응답이 BoardDetailResponse 스키마와 일치 |
| TC-210-03 | PostController GET /posts 응답 스키마 검증 | 페이지네이션 응답이 PostListPageResponse 스키마와 일치 |
| TC-210-04 | CommentController GET /posts/{id}/comments 응답 스키마 검증 | 댓글 목록 응답이 CommentListResponse 스키마와 일치 |
| TC-210-05 | PostLikeController POST /posts/{id}/like 응답 스키마 검증 | 토글 응답이 PostLikeToggleResponse 스키마와 일치 |
| TC-210-06 | BookmarkController POST /posts/{id}/bookmark 응답 스키마 검증 | 토글 응답이 BookmarkToggleResponse 스키마와 일치 |
| TC-210-07 | CommentReportController POST /comments/{id}/reports 응답 스키마 검증 | 신고 생성 응답이 CommentReportResponse 스키마와 일치 |

### TC-211: Admin 도메인 응답 검증

| TC ID | 테스트 설명 | 검증 내용 |
|-------|-----------|----------|
| TC-211-01 | AdminDashboardController GET /admin/dashboard 응답 스키마 검증 | DashboardStatsResponse 스키마 일치 |
| TC-211-02 | AdminUserController GET /admin/users 응답 스키마 검증 | UserListPageResponse 스키마 일치, 특히 Page content 타입 |
| TC-211-03 | AdminMemberController GET /admin/associates/pending 응답 스키마 검증 | AssociateInfoPageResponse 스키마 일치 |
| TC-211-04 | AdminLoginHistoryController GET /admin/login-histories 응답 스키마 검증 | PageLoginHistoryResponse 스키마 일치 (Generic Page 검증 핵심) |
| TC-211-05 | AdminAccountStatusChangeHistoryController 응답 스키마 검증 | PageAccountStatusChangeHistoryResponse 스키마 일치 (Generic Page 검증 핵심) |

### TC-212: 행사 도메인 응답 검증

| TC ID | 테스트 설명 | 검증 내용 |
|-------|-----------|----------|
| TC-212-01 | EventController GET /events 응답 스키마 검증 | 행사 목록 응답 스키마 일치 |
| TC-212-02 | EventRegistrationController GET /events/{id}/registrations 응답 스키마 검증 | 신청자 목록 페이지네이션 응답 스키마 일치 |

### TC-213: 미존재 테스트 대상 스모크 검증 (대표 케이스)

| TC ID | 테스트 설명 | 검증 내용 |
|-------|-----------|----------|
| TC-213-01 | PinnedPostController GET 응답 스키마 검증 | 고정 게시글 목록 스키마 일치 |
| TC-213-02 | PasswordAuthController POST /login 응답 스키마 검증 | 로그인 성공 응답 스키마 일치 |
| TC-213-03 | MyPageController GET /me 응답 스키마 검증 | 프로필 응답 스키마 일치 |
| TC-213-04~15 | 나머지 컨트롤러 대표 엔드포인트 응답 스키마 검증 | 각 스키마 일치 |

### TC-221: Filter 동작 검증

| TC ID | 테스트 설명 | 검증 내용 |
|-------|-----------|----------|
| TC-221-01 | test 프로필에서 OpenApiValidationFilter Bean 등록 확인 | ApplicationContext에 Filter Bean 존재 |
| TC-221-02 | 정상 API 호출 시 검증 통과 확인 | 응답 200 정상 반환 |
| TC-221-03 | 스키마 불일치 시 로그 경고 또는 에러 반환 확인 | 검증 실패 감지 동작 |

> **참고: TC-221-04(prod 프로필 Filter 미등록 검증) 삭제 사유**
>
> 기존 TC-221-04는 "프로덕션 프로필에서 Filter가 비활성화되는지 검증"하는 테스트 케이스였다. 그러나 이 테스트는 `@ActiveProfiles("prod")` 설정이 필요하며, 이는 `backend/src/test/CLAUDE.md` 3절의 "서브클래스에서 `@ActiveProfiles` 추가 금지" 규칙을 위반한다. 또한 prod 프로필 전용 테스트 컨텍스트가 별도로 생성되어 전체 테스트 실행 시간이 증가한다.
>
> **대체 검증 방안**: `@Profile({"dev", "test"})` 어노테이션이 설정 클래스에 선언되어 있으므로, Spring Framework의 프로필 메커니즘에 의해 prod에서 Bean이 등록되지 않음은 프레임워크 수준에서 보장된다. 별도의 통합 테스트로 검증하는 것보다 코드 리뷰에서 `@Profile` 어노테이션 존재를 확인하는 것이 더 적절하다. CR-204(prod 비활성화 보장)의 검증은 설정 클래스의 `@Profile` 어노테이션 존재 + 코드 리뷰로 충족한다.

### TC-222: Filter 통합 검증

| TC ID | 테스트 설명 | 검증 내용 |
|-------|-----------|----------|
| TC-222-01 | Filter 활성화 상태에서 정상 요청/응답 흐름 검증 | 기존 API 동작에 영향 없음 확인 |
| TC-222-02 | Filter 활성화 상태에서 다수 엔드포인트 순차 호출 | 모든 응답이 검증을 통과 |

> **참고: TC-222-03(prod 성능 기준선 확인) 삭제 사유**: TC-221-04와 동일한 사유로 prod 프로필 전용 테스트를 삭제하였다. Filter 오버헤드 제로 확인은 `@Profile` 어노테이션에 의해 Bean 미등록으로 보장된다.

---

## 구현 시 주의사항

### 기술적 고려사항

1. **멀티파일 스펙 해석**: `openapi/openapi.yaml`은 `$ref`로 `paths/*.yaml`, `schemas/*.yaml`을 참조하는 멀티파일 구조이다. `OpenApiInteractionValidator.createForSpecificationUrl()`의 `ParseOptions.resolve(true)` 설정으로 자동 해석이 가능한지 TASK-200에서 반드시 검증해야 한다. 실패 시 `redocly bundle`로 번들 파일을 생성하여 사용한다.

2. **테스트 컨텍스트 캐싱**: 기존 프로젝트는 `@ActiveProfiles("test")` + `@Import({TestPasswordEncoderConfig, TestExternalServiceConfig})`로 통일된 테스트 컨텍스트를 사용한다 (`backend/src/test/CLAUDE.md` 참조). `OpenApiValidationFilter`를 test 프로필에서 활성화하면 **모든 기존 통합 테스트에 Filter가 적용된다.** 이에 대한 확정 전략은 TASK-221의 "테스트 컨텍스트 캐싱 전략" 절을 참조한다.

3. **`additionalProperties` 처리**: openapi-generator로 생성된 Java 모델은 기본적으로 `additionalProperties`를 허용하지 않지만, 실제 응답 JSON에 스키마에 정의되지 않은 필드가 포함될 수 있다. `LevelResolver`에서 이 검증 레벨을 적절히 설정해야 한다. 초기에는 `WARN`으로 시작하고, 안정화 후 `ERROR`로 올리는 것을 권장한다.

4. **Instant 직렬화 형식**: 프로젝트의 시간 타입은 `java.time.Instant`로 통일되어 있고, OpenAPI 스펙에서는 `format: date-time`으로 정의된다. Jackson의 `JavaTimeModule`이 `Instant`를 ISO-8601 형식(`2026-03-02T12:00:00Z`)으로 직렬화하는지, 숫자(epoch milliseconds)로 직렬화하는지에 따라 검증 결과가 달라질 수 있다. `objectMapper` 설정을 확인한다.

5. **Generic Page 스키마**: `AdminLoginHistoryController`, `AdminAccountStatusChangeHistoryController` 등이 사용하는 Generic `Page` 응답 스키마에서 `content` 배열의 아이템 타입이 `object`로 정의되어 있으면, 실제 응답의 구체적인 타입을 검증할 수 없다. 스키마를 구체적인 타입으로 수정하거나, 검증 레벨을 조정해야 한다. **이것이 런타임 응답 검증 도입의 핵심 동기이므로, 스키마 수정이 바람직하다.**

6. **Validator 초기화 비용**: `OpenApiInteractionValidator` 초기화 시 OpenAPI 스펙 파일 전체를 파싱하므로 수백 ms ~ 수 초가 소요될 수 있다. 테스트에서는 `static` 필드로 한 번만 초기화하고, Filter에서는 Bean 초기화 시 한 번만 수행하여 비용을 최소화한다.

### 잠재적 위험 요소

1. **마이그레이션 미완료 상태에서의 혼란**: 29개 컨트롤러 중 일부만 마이그레이션된 상태에서 Filter를 활성화하면, 미마이그레이션 컨트롤러의 모든 테스트가 실패한다. **확정 전략**: 마이그레이션 완전 완료 후 Filter 활성화 (TASK-221 "테스트 컨텍스트 캐싱 전략" 참조).

2. **의존성 충돌**: swagger-request-validator는 내부적으로 `swagger-parser`, `jackson` 등을 사용한다. 기존 `build.gradle`의 `openapitools:jackson-databind-nullable` 등과 버전 충돌이 발생할 수 있다. TASK-200에서 반드시 확인한다.

3. **CI 실행 시간 증가**: 응답 검증이 추가되면 각 테스트의 실행 시간이 소폭 증가한다. validator 초기화가 병목인 경우, 전체 CI 시간이 10~30초 정도 늘어날 수 있다. TASK-200에서 측정한다.

4. **false positive**: 스키마 정의와 실제 구현 사이의 미묘한 차이(예: nullable 필드 처리, empty array vs null 등)로 인해 불필요한 검증 실패가 발생할 수 있다. `LevelResolver` 튜닝이 중요하다.

### 기존 코드와의 통합 포인트

1. **테스트 베이스 클래스**: `ControllerIntegrationTestBase` 패턴을 그대로 사용한다. `OpenApiValidatorUtil`은 별도 유틸리티로 제공하되, 베이스 클래스에 통합하지 않는다 (기존 베이스 클래스 수정 최소화).

2. **build.gradle**: 기존 `openApiGenerate` 태스크와 `sourceSets` 설정에 영향을 주지 않는다. 순수하게 의존성 추가만 수행한다.

3. **CI 파이프라인**: 기존 `backend-ci.yaml`의 `./gradlew test` 단계에서 자연스럽게 실행된다. 별도 CI 단계 추가 불필요.

4. **OpenAPI 스펙**: 스펙 자체는 수정하지 않는 것이 원칙이나, 응답 검증을 통해 발견된 스키마 결함(예: Generic Page의 `content: object[]`)은 스펙을 수정하여 해결한다.

5. **기존 마이그레이션 작업 (TASK-010~111)**: 런타임 검증 작업은 마이그레이션 작업과 독립적으로 진행할 수 있으나, 실제 검증 테스트 적용은 해당 컨트롤러의 마이그레이션 완료 후에 수행한다.

---

## 완료 기준

### 응답 검증 인프라 체크리스트

- [ ] swagger-request-validator-mockmvc 테스트 의존성이 build.gradle에 추가됨 (TASK-201)
- [ ] swagger-request-validator-spring-webmvc 런타임 의존성이 build.gradle에 추가됨 (TASK-220)
- [ ] OpenApiValidatorUtil 테스트 유틸리티가 작성되어 모든 테스트에서 사용 가능 (TASK-202)
- [ ] dev/test 프로필 전용 OpenApiValidationFilter 설정이 존재함 (TASK-221)
- [ ] Filter 활성화 상태에서 정상 동작 확인 (TASK-222)

### 통합 테스트 응답 검증 체크리스트

- [ ] BoardControllerTest에 응답 스키마 검증 추가됨 (TASK-210)
- [ ] PostControllerTest에 응답 스키마 검증 추가됨 (TASK-210)
- [ ] CommentControllerTest에 응답 스키마 검증 추가됨 (TASK-210)
- [ ] PostLikeControllerTest에 응답 스키마 검증 추가됨 (TASK-210)
- [ ] CommentLikeControllerTest에 응답 스키마 검증 추가됨 (TASK-210)
- [ ] BookmarkControllerTest에 응답 스키마 검증 추가됨 (TASK-210)
- [ ] CommentReportControllerTest에 응답 스키마 검증 추가됨 (TASK-210)
- [ ] AdminDashboardControllerTest에 응답 스키마 검증 추가됨 (TASK-211)
- [ ] AdminUserControllerTest에 응답 스키마 검증 추가됨 (TASK-211)
- [ ] AdminMemberControllerTest에 응답 스키마 검증 추가됨 (TASK-211)
- [ ] AdminLoginHistoryControllerTest에 응답 스키마 검증 추가됨 (TASK-211)
- [ ] AdminAccountStatusChangeHistoryControllerTest에 응답 스키마 검증 추가됨 (TASK-211)
- [ ] EventControllerIntegrationTest에 응답 스키마 검증 추가됨 (TASK-212)
- [ ] EventRegistrationControllerIntegrationTest에 응답 스키마 검증 추가됨 (TASK-212)
- [ ] 통합 테스트 미존재 컨트롤러 15개에 대해 스모크 테스트 존재 (TASK-213)

### CI/문서 체크리스트

- [ ] CI에서 `./gradlew test` 실행 시 응답 검증 테스트가 자동 포함됨 (TASK-230)
- [ ] 응답 검증 실패 트러블슈팅 가이드가 존재함 (TASK-231)
- [ ] `./gradlew clean build` 성공 (의존성 충돌 없음)
- [ ] `./gradlew test` 전체 테스트 통과

### 통계 요약

| 항목 | 수량 |
|------|------|
| 전체 작업 항목 | 12개 |
| PoC 작업 | 1개 |
| 인프라 구축 작업 | 4개 (의존성 2 + 유틸리티 1 + Filter 설정 1) |
| 기존 테스트 검증 추가 작업 | 3개 (도메인별) |
| 신규 스모크 테스트 작성 | 1개 (15개 컨트롤러 대상) |
| CI/문서 작업 | 2개 |
| Filter 검증 테스트 | 1개 |
| 예상 Phase | 6 Phase |

---

## 리뷰 피드백 반영 이력

| # | 심각도 | 피드백 요약 | 해결 내용 |
|---|:---:|------------|----------|
| 1 | 필수 | TASK-220의 `implementation` 스코프 리스크 분석 부재 | TASK-220에 "의존성 스코프 결정" 절 추가. `implementation` 유지 근거, 프로덕션 포함 리스크 분석표, 대안 검토 및 기각 사유를 명시 |
| 2 | 필수 | TASK-221의 테스트 컨텍스트 캐싱 전략 미확정 | TASK-221에 "테스트 컨텍스트 캐싱 전략 (확정)" 절 추가. "마이그레이션 완전 완료 후 Filter 활성화"를 기본 전략으로 확정. 선택 근거, PoC 검증 항목, 기각된 대안을 명시. TASK-200에 컨텍스트 캐싱 검증 항목 추가 |
| 3 | 권장 | TASK-211에서 AdminMemberControllerTest 누락 | TASK-211 대상 테스트 목록에 `AdminMemberControllerTest.java` 추가, TC-211-03 추가. "테스트 보유 컨트롤러 상세 목록" 테이블에도 반영 |
| 4 | 권장 | TASK-213 선행 작업 매핑 테이블 부재 | TASK-213에 "컨트롤러-선행 마이그레이션 TASK 매핑 테이블" (15행) 추가. 부분 적용 전략 안내 포함 |
| 5 | 권장 | 라이브러리 버전 PoC 전 하드코딩 | 버전 표기를 "2.46.0 이상 (PoC에서 최종 호환 버전 확정)"으로 변경. TASK-201/220의 `build.gradle` 예시에서 버전을 `<PoC 확정 버전>`으로 변경. TASK-200에 버전 확정 항목 추가 |
| 6 | 권장 | 컨트롤러 수 정합성 확인 필요 | "컨트롤러 수 정합성" 절 추가. task-plan.md 기준 29개, 테스트 보유 14개, 미보유 15개의 출처와 계산 근거를 명시. "테스트 보유 컨트롤러 상세 목록" 테이블(14행) 추가 |
| 7 | 권장 | TC-221-04 prod 프로필 테스트 실현 가능성 | TC-221-04 삭제. 삭제 사유와 대체 검증 방안을 TC-221 절 하단에 명시. TC-222-03도 동일 사유로 삭제. TASK-222의 테스트 케이스 번호를 TC-222-01~02로 조정. 완료 기준 체크리스트에서 "prod 프로필에서 Filter 비활성화 테스트 확인" 항목을 "Filter 활성화 상태에서 정상 동작 확인"으로 변경 |
