# TASK-230: CI OpenAPI 응답 검증 테스트 자동 실행 확인 결과

## 메타데이터
| 항목 | 값 |
|------|------|
| 작성일 | 2026-03-02 |
| CI 워크플로우 파일 | `.github/workflows/backend-ci.yaml` |
| 관련 검증 기준 | CR-205 |

## 1. 확인 결과 요약

| # | 확인 항목 | 결과 | 비고 |
|---|----------|:----:|------|
| 1 | 스펙 파일 접근 | PASS | checkout@v4가 전체 저장소 체크아웃, 상대 경로 정상 해석 |
| 2 | 번들 파일 필요 여부 | 불필요 | $ref 자동 해석으로 멀티파일 스펙 직접 사용 가능 |
| 3 | openapi/ 변경 감지 | 이미 포함 | check-changes 단계에서 `openapi/` 감지 조건 존재 |
| 4 | 테스트 자동 실행 | PASS | `./gradlew test`로 응답 검증 테스트 자동 포함 |

**결론: CI 워크플로우 수정 불필요. 기존 설정으로 CR-205 충족.**

## 2. 상세 분석

### 2.1 스펙 파일 접근

CI 환경에서 `openapi/openapi.yaml` 파일에 접근 가능한지 확인하였다.

- `actions/checkout@v4` (49행)가 전체 저장소를 체크아웃하므로, `openapi/` 디렉토리가 CI 런너에 존재한다
- `working-directory: backend` 설정 (44~45행)으로 인해 Gradle 명령이 `backend/` 디렉토리에서 실행된다
- `OpenApiValidatorFactory.SPEC_FILE_PATH`가 `../openapi/openapi.yaml`로 정의되어 있어, `backend/` 기준 상대 경로로 스펙 파일에 정상 접근한다

```
CI 런너 디렉토리 구조:
$GITHUB_WORKSPACE/
├── backend/          <-- working-directory
│   └── (gradlew)
├── openapi/
│   └── openapi.yaml  <-- ../openapi/openapi.yaml로 접근
└── ...
```

### 2.2 번들 파일 필요 여부

TASK-200 PoC 결과에서 확인된 바와 같이:

- `OpenApiInteractionValidator.createForSpecificationUrl()`이 `$ref`를 재귀적으로 해석한다
- 멀티파일 구조(`openapi.yaml` -> `paths/*.yaml` -> `schemas/*.yaml`)를 번들 없이 직접 사용 가능하다
- CI에 `redocly bundle` 단계 추가는 **불필요**하다

### 2.3 openapi/ 변경 감지

`backend-ci.yaml`의 `check-changes` job에서 이미 `openapi/` 디렉토리 변경을 감지하고 있다.

```javascript
// backend-ci.yaml 28~30행
const backendChanged = files.some(f =>
  (f.filename.startsWith('backend/') && !f.filename.startsWith('backend/docs/')) ||
   f.filename.startsWith('openapi/') ||  // <-- openapi/ 변경 감지 포함
   f.filename === '.github/workflows/backend-ci.yaml' ||
   f.filename === '.github/workflows/flyway-validate/validate-flyway-files.sh'
);
```

이 조건에 의해:
- `openapi/schemas/boards.yaml` 등 스키마 파일 변경 시 backend CI가 자동 트리거된다
- `openapi/paths/posts.yaml` 등 경로 파일 변경 시에도 트리거된다
- 스펙 변경으로 인한 응답 스키마 불일치를 CI에서 사전 탐지할 수 있다

### 2.4 테스트 자동 실행

`build-and-test` job의 `./gradlew test` 단계 (74행)에서 모든 JUnit 테스트가 실행된다. TASK-210~213에서 추가된 OpenAPI 응답 검증 테스트(`matchesOpenApiSpec()`)는 일반 JUnit 테스트이므로 별도 설정 없이 자동 포함된다.

테스트 결과는 `Publish Test Report` 단계 (79~82행)에서 GitHub PR에 게시된다.

### 2.5 테스트 실행 시간 영향

PoC(TASK-200) 성능 측정 결과:
- Validator 초기화: ~3초 (JVM + 스펙 파일 파싱, 1회)
- 요청당 순수 검증: ~1ms

영향 분석:
- `OpenApiValidatorUtil`이 싱글턴 패턴으로 Validator를 1회만 초기화하므로, 전체 테스트 스위트에서 초기화 비용은 ~3초만 추가된다
- 응답 검증이 추가된 각 테스트의 실행 시간 증가는 ~1ms로 무시 가능하다
- **전체 CI 실행 시간 증가: 약 3~5초 (초기화 1회 + 검증 N회)**

## 3. 결론

현재 CI 구성이 OpenAPI 응답 검증 테스트를 자동으로 실행하는 데 필요한 모든 조건을 이미 충족하고 있다.

| 조건 | 현재 상태 | 추가 작업 |
|------|:--------:|:---------:|
| 전체 저장소 체크아웃 | 충족 | 없음 |
| 스펙 파일 경로 접근 | 충족 | 없음 |
| openapi/ 변경 감지 | 충족 | 없음 |
| 테스트 자동 포함 | 충족 | 없음 |
| 번들 파일 생성 | 불필요 | 없음 |
