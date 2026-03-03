# OpenAPI 스펙 Redocly 번들링 도입

## 배경

프로젝트의 OpenAPI 스펙은 멀티파일 구조(`openapi/openapi.yaml` → `paths/*.yaml` → `schemas/*.yaml`)로 관리된다. 프론트엔드는 Orval을 사용하여 이 스펙에서 TypeScript 타입과 React Query 훅을 자동 생성한다.

**문제**: Orval이 생성하는 TypeScript 타입 이름이 백엔드 스키마 이름과 불일치했다.
- 백엔드(SpringDoc): `EventDetailResponse`
- Orval 생성: `CloseEvent200`, `CloseEvent200EventStatus` (operation명 + HTTP 상태코드 기반)

**원인**: Orval 내부의 `@scalar/json-magic/bundle`이 외부 파일 `$ref`를 모두 인라인 처리하여, `components/schemas`에 등록된 스키마도 `$ref` 연결이 끊어졌다. Orval은 `$ref` 문자열의 마지막 경로 세그먼트에서 타입 이름을 추출하므로, `$ref`가 사라지면 operation명 기반으로 이름을 생성한다.

## 결정

- `@redocly/cli`를 도입하여 멀티파일 스펙을 단일 번들 파일로 변환
- Orval은 번들된 파일을 입력으로 사용

## 이유

### 시도한 접근 (실패)

1. **`openapi.yaml`에 `components/schemas`만 추가**: path 파일의 `$ref`가 여전히 외부 파일을 가리키므로, Orval 내부 번들러가 인라인 처리하여 효과 없음
2. **path 파일의 `$ref`를 `../openapi.yaml#/components/schemas/Name`으로 변경**: Orval 내부 번들러가 이를 `#/x-ext/<hash>/components/schemas/Name`으로 네임스페이싱하여 경로가 깨짐

### Redocly 번들링이 동작하는 이유

- `redocly bundle`은 모든 외부 `$ref`를 해석하여 스키마를 `components/schemas` 아래에 배치
- 번들 결과에서 모든 `$ref`가 `#/components/schemas/Name` 형식의 내부 참조로 변환
- Orval은 단일 파일 내의 `#/components/schemas/` 참조를 정상적으로 인식하여 스키마 이름 기반 타입 생성

## 변경 사항

| 파일 | 변경 |
|------|------|
| `openapi/openapi.yaml` | `components/schemas`에 모든 스키마 `$ref` 포워딩 등록 (~130개) |
| `frontend/package.json` | `@redocly/cli` devDependency 추가, `openapi:bundle` 스크립트 추가, `api:generate` 스크립트 업데이트 |
| `frontend/orval.config.ts` | 입력 파일을 `openapi.bundled.yaml`로 변경 |
| `.gitignore` | `openapi/openapi.bundled.yaml` 추가 |

## 결과

- Orval 생성 타입이 스키마 이름과 일치 (`EventDetailResponse`, `SurveyDetailResponse` 등)
- operation 기반 중복 타입 제거 (모델 파일 수 ~449 → 262)
- API 변경 시 워크플로우: `pnpm api:generate` (번들링 + Orval 코드 생성)
- 번들 파일(`openapi.bundled.yaml`)은 생성 산출물이므로 gitignore 처리
