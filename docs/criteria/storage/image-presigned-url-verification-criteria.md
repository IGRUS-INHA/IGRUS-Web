# Presigned URL 이미지 업로드/다운로드 검증 기준서

> **Status**: Draft
> **Last Updated**: 2026-02-26
> **Scope**: Presigned URL 발급, 이미지 업로드(PUT), 이미지 다운로드(GET), 파일 메타데이터 관리, 업로드 완료 확인
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)

## 목적

이 문서는 S3 Presigned URL 기반 이미지 업로드/다운로드 기능에서 **반드시 지켜져야 하는 규칙**을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

프론트엔드에서 S3로 직접 파일을 업로드/다운로드하는 Presigned URL 방식을 채택한다. 백엔드는 URL 발급과 메타데이터 관리만 담당하고, 실제 바이너리 전송은 프론트엔드 ↔ S3 간 직접 이루어진다.

**사용처**: 게시글 이미지, 프로필 이미지, 행사 이미지 등 범용

QA Testing 용어 정리 wiki의 10개 영역 중, 이 도메인에 직접 관련된 8개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | 파일 크기/타입 제한, URL 만료 정책, Object Key 유일성 |
| 2 | 상태 모델 | 업로드 라이프사이클 (REQUESTED → CONFIRMING → COMPLETED / FAILED / EXPIRED) |
| 3 | 시스템 경계와 책임 분리 | Frontend ↔ Backend ↔ S3 3자 경계, 각 책임 범위 |
| 4 | 외부 의존성 실패 정책 | S3 서비스 장애, Presigned URL 만료, 네트워크 단절 |
| 5 | 입력 도메인 분할과 경계값 | 파일 크기/타입/파일명 경계값 |
| 6 | 권한/보안 정책 | RBAC 업로드/다운로드 권한, URL 보안, Content-Type 검증 |
| 7 | 관측 가능성 | 업로드 요청/완료 로깅, 감사 이력 |
| 8 | 테스트 전략 | 단위/통합 테스트, S3 모킹 전략 |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

시스템 전체에서 **항상 참이어야 하는 조건**이다. 어떤 코드 변경이든 이 조건을 깨뜨리면 시스템 무결성이 훼손된다.

### STOR-INV-01: 파일 크기 제한

> 업로드 파일의 크기는 최대 **10MB (10,485,760 bytes)**를 초과할 수 없다.

- **검증 계층**: Backend (Presigned URL 생성 시 Content-Length 조건) + Frontend (업로드 전 클라이언트 검증)
- **위반 시**: Backend — Presigned URL 생성 거부, Frontend — 업로드 차단 및 사용자 안내
- **검증 방법**: 10MB 파일 업로드 성공, 10MB+1B 파일 업로드 거부 확인

### STOR-INV-02: 허용 파일 타입 제한

> 업로드 가능한 파일 타입은 `image/jpeg`, `image/png`, `image/gif`, `image/webp`만 허용한다.

- **검증 계층**: Backend (Presigned URL 생성 시 Content-Type 조건) + Frontend (파일 선택 시 accept 속성)
- **위반 시**: Backend — Presigned URL 생성 거부, Frontend — 파일 선택 차단
- **검증 방법**: 허용 4종 각각 업로드 성공, 금지 타입(bmp, svg, tiff, exe 등) 업로드 거부 확인

### STOR-INV-03: Presigned URL 만료 시간

> 업로드용 Presigned URL은 발급 후 **5분** 이내에만 유효하다.
> 다운로드용 Presigned URL은 발급 후 **1시간** 이내에만 유효하다.

- **사전조건**: 백엔드에서 URL 생성 시 만료 시간 설정
- **위반 시**: S3가 `403 Forbidden` (AccessDenied) 반환
- **검증 방법**: 만료 시간 경과 후 URL 사용 시 403 응답 확인

### STOR-INV-04: Object Key 유일성

> S3 Object Key는 시스템 전체에서 유일해야 한다. UUID 기반으로 생성한다.

- **형식**: `{사용처}/{YYYY}/{MM}/{DD}/{UUID}.{확장자}`
  - 예: `posts/2026/02/25/550e8400-e29b-41d4-a716-446655440000.png`
  - 예: `profiles/2026/02/25/6ba7b810-9dad-11d1-80b4-00c04fd430c8.jpeg`
  - 예: `events/2026/02/25/6ba7b811-9dad-11d1-80b4-00c04fd430c8.webp`
- **사전조건**: UUID v4 또는 v7 사용
- **위반 시**: 기존 파일 덮어쓰기 (UUID 충돌 확률은 실질적으로 0)
- **검증 방법**: 동일 파일명으로 다수 업로드 시 각각 다른 Object Key 생성 확인

### STOR-INV-05: 업로드 완료 확인

> 프론트엔드의 업로드 완료 알림을 받은 후, 백엔드는 S3 HEAD 요청으로 파일 존재 여부를 검증한다.

- **사전조건**: 프론트엔드가 S3 PUT 성공 후 백엔드에 완료 API 호출
- **사후조건**: S3에 해당 Object Key의 파일이 존재하고, Content-Type과 Content-Length가 요청과 일치
- **위반 시**: 업로드 실패 처리 (메타데이터 저장 거부)
- **검증 방법**: 존재하지 않는 Object Key로 완료 알림 시 실패 응답 확인

### STOR-INV-06: 미인증 사용자 업로드 차단

> Presigned URL 발급(업로드용)은 인증된 사용자만 요청할 수 있다.

- **검증 계층**: Spring Security (JWT 인증 필수)
- **위반 시**: `401 Unauthorized`
- **검증 방법**: JWT 없이 업로드 URL 요청 시 401 응답 확인

### STOR-INV-07: Content-Type 일치 검증

> Presigned URL 생성 시 지정한 Content-Type과 실제 업로드 시 Content-Type이 일치해야 한다.

- **검증 계층**: S3 Presigned URL 조건(Condition)으로 강제
- **위반 시**: S3가 `403 Forbidden` 반환
- **검증 방법**: Content-Type `image/png`으로 URL 발급 후 `image/jpeg`로 업로드 시도 시 403 확인

### STOR-INV-08: 고아 파일 방지

> 업로드 완료 확인 없이 방치된 S3 객체는 **24시간** 후 정리되어야 한다.

- **S3 정리**: S3 Lifecycle Rule로 `uploads/` 프리픽스 내 미완료 객체 24시간 후 자동 삭제
- **DB 정리**: 스케줄러가 REQUESTED 상태로 24시간 이상 경과한 메타데이터를 EXPIRED로 전환
  - 스케줄러 실행 주기: 매 1시간
  - 대상: `status = REQUESTED AND createdAt < NOW() - INTERVAL 24 HOUR`
- **검증 방법**: 업로드 완료 알림 없이 24시간 경과 후 해당 S3 객체 삭제 및 DB 상태 EXPIRED 확인

### STOR-INV-09: 파일 삭제 정책

> 파일 삭제 시 S3 객체 삭제를 먼저 수행하고, 성공 후 DB 메타데이터를 Soft Delete 처리한다.

- **삭제 순서**: S3 객체 삭제 → DB Soft Delete (순서 보장 필수)
- **S3 삭제 실패 시**: `500 Internal Server Error` 반환, DB 변경 없음 (롤백)
- **참조 무결성**: 상위 엔티티(게시글, 프로필 등)에서 참조 중인 파일은 삭제를 거부하고 `409 Conflict` 반환
- **검증 방법**:
  - S3 삭제 성공 후 DB Soft Delete 확인
  - S3 삭제 실패 시 DB 미변경 및 500 응답 확인
  - 참조 중인 파일 삭제 시도 시 409 응답 확인

---

## 2. 상태 모델 (State Machine & Transitions)

### 2-1. 업로드 라이프사이클

```
                          [프론트엔드 전용]
┌───────────┐   URL 발급    ┌────────────┐   S3 PUT 성공   ┌────────────┐   HEAD 검증 성공   ┌───────────┐
│ REQUESTED │ ───────────> │ UPLOADING  │ ──────────────> │ CONFIRMING │ ────────────────> │ COMPLETED │
└───────────┘              └─────┬──────┘                 └─────┬──────┘                  └───────────┘
                                 │                              │
                                 │ S3 PUT 실패                   │ HEAD 검증 실패
                                 ▼                              ▼
                           ┌──────────┐                   ┌──────────┐
                           │  FAILED  │                   │  FAILED  │
                           └──────────┘                   └──────────┘
                                 │
                                 │ URL 만료 (5분)
                                 ▼
                           ┌──────────┐
                           │ EXPIRED  │
                           └──────────┘
```

> **참고**: UPLOADING은 프론트엔드 UI 전용 상태이며 백엔드 DB에는 저장하지 않는다.
> DB에 저장되는 상태는 **REQUESTED / CONFIRMING / COMPLETED / FAILED / EXPIRED** 5가지이다.
> 프론트엔드에서 S3 PUT을 시작하면 UI에서만 UPLOADING으로 표시하고, 백엔드 DB 상태는 REQUESTED가 유지된다.
> 프론트엔드가 완료 알림 API를 호출하면 백엔드에서 REQUESTED → CONFIRMING으로 전이한 뒤 HEAD 검증을 수행한다.

### 2-2. 상태 정의

| 상태 | 저장 위치 | 설명 |
|------|----------|------|
| **REQUESTED** | Backend DB | 사용자가 업로드 URL을 요청하여 Presigned URL이 발급된 상태. 메타데이터(Object Key, 파일명, 타입, 크기)가 DB에 저장되었으나 아직 실제 파일 업로드는 시작되지 않음 |
| **UPLOADING** | Frontend UI 전용 | 프론트엔드가 S3에 PUT 요청을 보내 파일 전송 중인 상태. 백엔드 DB에는 반영되지 않으며, DB 상태는 REQUESTED가 유지됨 |
| **CONFIRMING** | Backend DB | 프론트엔드가 업로드 완료 알림 API를 호출하여 백엔드가 S3 HEAD 요청으로 파일 존재 여부와 메타데이터 일치를 검증 중인 상태 |
| **COMPLETED** | Backend DB | S3 HEAD 검증이 성공하여 파일이 정상적으로 업로드 완료된 상태. 다운로드 URL 발급이 가능함 |
| **FAILED** | Backend DB / Frontend UI | 업로드 또는 검증이 실패한 상태. S3 PUT 실패는 프론트엔드 UI에서만 표시되고, S3 HEAD 검증 실패는 백엔드 DB에 기록됨. 새로운 업로드를 시작해야 함 |
| **EXPIRED** | Backend DB | Presigned URL 만료 시간(5분)이 경과하여 더 이상 업로드할 수 없는 상태. 스케줄러에 의해 REQUESTED에서 자동 전환됨. 새로운 URL을 재요청해야 함 |

### 2-3. 상태 전이

| 전이 | 트리거 | 사전조건 | 사후조건 |
|------|--------|---------|---------|
| → REQUESTED | 사용자가 업로드 URL 요청 | 인증된 사용자, 유효한 파일 타입/크기 | Presigned URL 발급, 메타데이터(Object Key, 파일명, 타입, 크기) DB 저장 (status=REQUESTED) |
| REQUESTED → UPLOADING | 프론트엔드가 S3 PUT 시작 | 유효한 Presigned URL | **프론트엔드 UI 전용 상태 변경** (백엔드 DB 미반영, REQUESTED 유지) |
| UPLOADING → CONFIRMING | S3 PUT 성공 (HTTP 200) | 프론트엔드가 완료 API 호출 | 백엔드가 DB 상태를 REQUESTED → CONFIRMING으로 갱신, S3 HEAD 요청 시작 |
| CONFIRMING → COMPLETED | S3 HEAD 검증 성공 | Content-Type, Content-Length 일치 | 메타데이터 상태 COMPLETED로 갱신, 다운로드 가능 |
| UPLOADING → FAILED | S3 PUT 실패 | - | **프론트엔드 UI 전용** — 사용자에게 실패 안내, 재시도 유도 (백엔드 DB 미반영) |
| CONFIRMING → FAILED | S3 HEAD 검증 실패 | 파일 미존재 또는 타입/크기 불일치 | 메타데이터 상태 FAILED로 갱신 |
| REQUESTED → EXPIRED | Presigned URL 만료 (5분) | 업로드 미완료 (스케줄러에 의한 전환, STOR-INV-08 참조) | 재요청 필요 |

**금지된 전이 (Invalid Transition)**:

| 시도 | 예상 결과 | 이유 |
|------|----------|------|
| COMPLETED → UPLOADING | 거부 | 완료된 업로드는 재업로드 불가 |
| FAILED → COMPLETED | 거부 | 실패한 업로드는 새로 시작해야 함 |
| EXPIRED → UPLOADING | 거부 | 만료된 URL로는 업로드 불가, 새 URL 필요 |

---

## 3. 시스템 경계와 책임 분리 (System Boundary & SoC)

### 3-1. 3자 시스템 경계

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                              신뢰 경계 (Trust Boundary)                         │
│                                                                                  │
│  ┌──────────────┐          ┌──────────────┐                                      │
│  │   Frontend   │ ──(1)──> │   Backend    │                                      │
│  │              │ <──(3)── │              │ ──(2)──> S3 SDK (Presigned URL 생성)  │
│  │              │          │              │ ──(6)──> S3 SDK (HEAD 검증)           │
│  └──────┬───────┘          └──────────────┘                                      │
│         │                                                                        │
│         │ (4) Presigned PUT                                                      │
│         │ (5) 완료 알림 → Backend                                                │
│         ▼                                                                        │
│  ┌──────────────┐                                                                │
│  │     S3       │  (외부 의존성)                                                  │
│  └──────────────┘                                                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 3-2. 각 구성요소 책임

| 구성요소 | 책임 | 하지 않는 것 |
|---------|------|-------------|
| **Frontend** | 파일 선택 UI, 클라이언트 사전 검증(크기/타입), S3 직접 PUT 업로드, 업로드 진행률 표시, 완료 알림 API 호출 | 파일 바이너리를 Backend로 전송하지 않음 |
| **Backend** | 인증/인가 검증, Presigned URL 생성, 파일 메타데이터 DB 관리, S3 HEAD 검증, 다운로드 URL 발급 | 파일 바이너리를 중계하지 않음, S3 파일 직접 읽기 없음 |
| **S3** | 파일 저장, Presigned URL 조건 검증 (Content-Type, Content-Length), Lifecycle Rule 적용 | 비즈니스 로직 처리 없음 |

### 3-3. API 흐름 (업로드)

| 단계 | 요청 | 응답 | 오류 시 |
|------|------|------|---------|
| (1) URL 요청 | `POST /api/storage/presigned-url` (fileName, contentType, fileSize) | `{ presignedUrl, objectKey }` | 401, 400 (파일 타입/크기 불량) |
| (2) URL 생성 | Backend → S3 SDK `generatePresignedUrl()` | Presigned PUT URL | S3 SDK 오류 → 500 |
| (3) URL 반환 | Backend → Frontend | JSON 응답 | - |
| (4) S3 PUT | Frontend → S3 `PUT presignedUrl` (binary body) | HTTP 200 | 403 (만료/조건불일치), 네트워크 오류 |
| (5) 완료 알림 | `POST /api/storage/confirm` (objectKey) | 성공: `{ status: "COMPLETED", objectKey }`, 멱등 재호출(이미 COMPLETED): `{ status: "COMPLETED", objectKey }` | HEAD 검증 실패: `{ status: "FAILED", objectKey, reason }` |
| (6) HEAD 검증 | Backend → S3 `HEAD objectKey` | 200 + 메타데이터 | 404 → FAILED |

### 3-4. API 흐름 (다운로드)

| 단계 | 요청 | 응답 | 오류 시 |
|------|------|------|---------|
| (1) URL 요청 | `GET /api/storage/download-url?objectKey={key}` | `{ presignedUrl }` | 401 (미인증), 404 (파일 없음) |
| (2) URL 생성 | Backend → S3 SDK `generatePresignedUrl()` | Presigned GET URL | S3 SDK 오류 → 500 |
| (3) 이미지 로드 | Frontend → S3 GET (img src 또는 fetch) | 이미지 바이너리 | 403 (만료) |

---

## 4. 외부 의존성 실패 정책 (External Dependency Failure Policy)

### 4-1. S3 서비스 장애

| 항목 | 정책 |
|------|------|
| Presigned URL 생성 실패 | 사용자에게 `500 Internal Server Error` 반환, 에러 로깅 |
| S3 PUT 업로드 실패 | 프론트엔드에서 재시도 UI 제공 (최대 3회) |
| S3 HEAD 검증 실패 | 메타데이터 상태 FAILED로 갱신, 사용자에게 재업로드 안내 |
| S3 GET 다운로드 실패 | 이미지 플레이스홀더 표시, 재요청 유도 |
| 재시도 전략 | 프론트엔드: 지수 백오프 (1s, 2s, 4s), 백엔드: 재시도 없음 (Presigned URL 재발급으로 대체) |

### 4-2. Presigned URL 만료

| 항목 | 정책 |
|------|------|
| 업로드 URL 만료 (5분) | 프론트엔드가 새 URL 재요청 후 재업로드 |
| 다운로드 URL 만료 (1시간) | 프론트엔드가 새 URL 재요청 후 재로드 |
| 만료 감지 | S3 `403 AccessDenied` 응답을 프론트엔드가 감지하여 자동 재요청 |

### 4-3. 네트워크 단절

| 항목 | 정책 |
|------|------|
| 업로드 중 단절 | 프론트엔드에서 감지 후 재시도 UI 표시 |
| 완료 알림 실패 | 프론트엔드에서 재시도 (Presigned URL이 아직 유효한 경우) |
| 멱등성 | 완료 알림 API는 멱등 (동일 objectKey로 재호출 시 이미 COMPLETED면 성공 반환) |

---

## 5. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 5-1. 업로드 URL 요청 입력값

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 | 검증 |
|------|-----------|-----------|--------|------|
| `fileName` | 1~255자 문자열 (확장자 불검증 -- Content-Type으로 대체) | null, 빈 문자열, 256자 이상 | 1자 (최소), 255자 (최대), 256자 (초과) | `@NotBlank`, `@Size(max=255)` |
| `contentType` | `image/jpeg`, `image/png`, `image/gif`, `image/webp` | null, `image/bmp`, `image/svg+xml`, `application/pdf`, `text/plain` | - | 허용 목록(allowlist) 검증 |
| `fileSize` | 1 ~ 10,485,760 bytes | null, 0, 음수, 10,485,761 이상 | 1 (최소), 10,485,760 (최대), 10,485,761 (초과) | `@NotNull`, `@Min(1)`, `@Max(10485760)` |

### 5-2. 파일 크기 경계값 상세

| 테스트 값 | 분류 | 예상 결과 |
|----------|------|----------|
| 0 bytes | 빈 파일 (무효) | 거부 |
| 1 byte | 최소 유효 크기 | 허용 |
| 1 KB (1,024 bytes) | 일반 소형 | 허용 |
| 5 MB (5,242,880 bytes) | 일반 중형 | 허용 |
| 10 MB (10,485,760 bytes) | 최대 허용 경계 | 허용 |
| 10 MB + 1 byte (10,485,761 bytes) | 경계 초과 | 거부 |
| 50 MB | 대형 (무효) | 거부 |

### 5-3. 파일명 특수 케이스

| 테스트 케이스 | 파일명 예시 | 예상 결과 |
|-------------|-----------|----------|
| 한글 파일명 | `사진.png` | 허용 (Object Key는 UUID로 대체) |
| 특수문자 포함 | `photo (1).png` | 허용 (Object Key는 UUID로 대체) |
| 공백만 | `   .png` | 거부 (`@NotBlank` 검증) |
| 매우 긴 이름 | 256자 이상 | 거부 (`@Size(max=255)`) |
| 확장자 없음 | `photo` | 허용 (Content-Type 기준으로 판단, 확장자 불검증) |
| 이중 확장자 | `photo.png.exe` | 허용 (Content-Type 기준으로 판단, 확장자 불검증) |

### 5-4. 업로드 완료 확인 입력값

| 필드 | 유효 동치류 | 무효 동치류 | 검증 |
|------|-----------|-----------|------|
| `objectKey` | 존재하는 Object Key (REQUESTED 또는 COMPLETED 상태) | null, 빈 문자열, 존재하지 않는 Key, FAILED 상태인 Key, EXPIRED 상태인 Key | `@NotBlank` + DB 조회 |

> **멱등성 규칙**: REQUESTED 상태의 Key는 HEAD 검증 후 COMPLETED로 전이한다. 이미 COMPLETED 상태인 Key로 재호출 시에는 HEAD 검증을 생략하고 즉시 성공(`200 OK`)을 반환한다. FAILED/EXPIRED 상태인 Key는 완료 알림 대상이 아니므로 무효 동치류에 해당한다.

---

## 6. 권한/보안 정책 (RBAC & Authorization)

### 6-1. 역할별 접근 제어 매트릭스

> **현재 버전 접근 정책**: 모든 이미지에 동일한 접근 제어를 적용한다. 인증된 사용자(ASSOCIATE 이상)는 모든 이미지 다운로드 URL을 요청할 수 있다. 비인증 사용자는 다운로드할 수 없다.

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 업로드 URL 요청 | 401 | **O** | **O** | **O** | **O** |
| 업로드 완료 알림 | 401 | **O** (본인 요청만) | **O** (본인 요청만) | **O** (본인 요청만) | **O** |
| 다운로드 URL 요청 | 401 | **O** | **O** | **O** | **O** |
| 파일 메타데이터 삭제 | 401 | 403 | 403 | **O** (본인 것 또는 관리 대상) | **O** |

### 6-2. 소유권 검증 (Ownership Verification)

| ID | 검증 항목 | 예상 결과 |
|----|----------|----------|
| SEC-STOR-01 | 다른 사용자의 업로드 완료 알림 시도 | `403 Forbidden` |
| SEC-STOR-02 | 미인증 사용자의 업로드 URL 요청 | `401 Unauthorized` |
| SEC-STOR-03 | 존재하지 않는 Object Key로 다운로드 URL 요청 | `404 Not Found` |
| SEC-STOR-04 | 미인증 사용자의 다운로드 URL 요청 | `401 Unauthorized` |

### 6-3. URL 보안

| 항목 | 정책 |
|------|------|
| Presigned URL 노출 | URL은 요청자에게만 반환, 서버 로그에 URL 기록 금지 |
| HTTPS 강제 | Presigned URL은 항상 HTTPS |
| CORS 설정 | S3 버킷 CORS: 프론트엔드 도메인만 허용 |
| Content-Type 강제 | Presigned URL 생성 시 Content-Type 조건 포함, 위변조 방지 |
| Content-Length 제한 | Presigned URL 생성 시 Content-Length 상한 조건 포함 |

---

## 7. 관측 가능성 (Observability & Audit)

### 7-1. 서비스별 로그 메시지

| 서비스 | 시작/완료 로그 | 실패 로그 |
|--------|-------------|---------|
| Presigned URL 생성 | `Presigned URL 생성: userId, contentType, fileSize` | `Presigned URL 생성 실패: userId, error` |
| 업로드 완료 확인 | `업로드 완료 확인: objectKey, userId` | `업로드 검증 실패: objectKey, reason` |
| 다운로드 URL 생성 | `다운로드 URL 생성: objectKey, userId` | `다운로드 URL 생성 실패: objectKey, error` |
| 파일 삭제 | `파일 삭제: objectKey, deletedBy` | `파일 삭제 실패: objectKey, error` |

### 7-2. 메타데이터 감사 이력

| 필드 | 저장 내용 | 용도 |
|------|---------|------|
| objectKey | S3 Object Key | 파일 식별 |
| uploaderUserId | 업로드 요청자 | 소유권 추적 |
| originalFileName | 원본 파일명 | 사용자 안내 |
| contentType | MIME 타입 | 파일 종류 확인 |
| fileSize | 파일 크기 (bytes) | 용량 관리 |
| status | REQUESTED / CONFIRMING / COMPLETED / FAILED / EXPIRED | 상태 추적 |
| createdAt | 생성 시각 | 감사 이력 |
| completedAt | 업로드 완료 시각 | 지연 시간 분석 |

### 7-3. 로그 주의사항

- **Presigned URL 자체를 로그에 기록하지 않음** (URL에 서명 정보 포함, 보안 위험)
- Object Key, userId, contentType, fileSize만 로깅

---

## 8. 테스트 전략 (Test Strategy)

### 8-1. 테스트 레벨별 전략

| 테스트 레벨 | 범위 | S3 처리 방식 |
|-----------|------|-------------|
| **단위 테스트** | 파일 검증 로직 (크기/타입), Object Key 생성, 상태 전이 | S3 의존성 없음 |
| **서비스 통합 테스트** | Presigned URL 생성, 메타데이터 CRUD, 완료 확인 로직 | S3Client **Mock** (Mockito) |
| **E2E 테스트** (선택) | 전체 업로드/다운로드 흐름 | LocalStack 또는 실제 S3 테스트 버킷 |

### 8-2. 검증 항목별 테스트 매핑 (구현 후 작성)

| 불변조건 | 테스트 범위 | 상태 |
|---------|-----------|------|
| STOR-INV-01 (파일 크기 제한) | 단위: 경계값 검증, 통합: DTO 검증 | 미구현 |
| STOR-INV-02 (허용 파일 타입) | 단위: allowlist 검증, 통합: DTO 검증 | 미구현 |
| STOR-INV-03 (URL 만료 시간) | 통합: SDK 호출 파라미터 검증 | 미구현 |
| STOR-INV-04 (Object Key 유일성) | 단위: UUID 기반 Key 생성 검증 | 미구현 |
| STOR-INV-05 (업로드 완료 확인) | 통합: S3 HEAD Mock 성공/실패 시나리오 | 미구현 |
| STOR-INV-06 (미인증 업로드 차단) | 통합: MockMvc 401 검증 | 미구현 |
| STOR-INV-07 (Content-Type 일치) | 통합: SDK 호출 파라미터에 Content-Type 조건 포함 확인 | 미구현 |
| STOR-INV-08 (고아 파일 방지) | 수동: S3 Lifecycle Rule 설정 확인 + 스케줄러 EXPIRED 전환 검증 | 미구현 |
| STOR-INV-09 (파일 삭제 정책) | 통합: S3 삭제 Mock 성공/실패 시나리오, 참조 무결성 검증 | 미구현 |

### 8-3. S3 테스트 더블 전략

| 접근법 | 장점 | 단점 | 적용 |
|--------|------|------|------|
| **Mockito Mock** | 빠름, 결정적 | S3 실제 동작과 차이 가능 | 서비스 통합 테스트 (기본) |
| **LocalStack** | S3 API 호환, 실제에 가까움 | Docker 필요, CI 설정 복잡 | E2E 테스트 (선택) |
| **실제 S3 테스트 버킷** | 가장 정확 | 비용, 네트워크 의존, 느림 | 수동 QA (선택) |

---

## 관련 문서

- [회원가입/승인/강등 검증 기준서](../verification-criteria.md) - 동일 형식의 기존 검증 기준서
- [행사 검증 기준서](../event/event-verification-criteria.md) - 상태 모델 참조
- [문의 검증 기준서](../inquiry-verification-criteria.md) - 외부 의존성 실패 정책 참조
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC) - 용어 및 개념 참조
