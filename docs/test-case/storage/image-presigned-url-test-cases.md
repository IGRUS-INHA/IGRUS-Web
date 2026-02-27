# S3 Presigned URL 이미지 업로드/다운로드 테스트 케이스

## 문서 정보

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-02-26 |
| 검증 기준 문서 | `docs/criteria/storage/image-presigned-url-verification-criteria.md` |
| 대상 기능 | S3 Presigned URL 기반 이미지 업로드/다운로드 (URL 발급, S3 PUT 업로드, 완료 확인, 다운로드 URL, 파일 삭제) |
| 테스트 케이스 수 | 총 62개 |

## 카테고리 요약

| # | 카테고리 | 테스트 케이스 수 | 커버리지 대상 |
|---|---------|:---:|-------------|
| 1 | 도메인 규칙과 불변조건 | 18 | STOR-INV-01 ~ STOR-INV-09 |
| 2 | 상태 모델 전이 | 10 | 정상 전이 6건 + 금지 전이 3건 + 멱등성 1건 |
| 3 | 보안 정책 | 8 | SEC-STOR-01 ~ SEC-STOR-04, URL 보안 |
| 4 | 입력 경계값 | 14 | 파일 크기, 파일명, Content-Type, objectKey |
| 5 | 외부 의존성 실패 | 7 | S3 장애, URL 만료, 네트워크 단절 |
| 6 | 관측 가능성 | 5 | 로그, 메타데이터 감사 이력 |

---

## 1. 도메인 규칙과 불변조건

### STOR-INV-01: 파일 크기 제한

#### TC-001: 최대 허용 크기(10MB) 파일 업로드 URL 요청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자(ASSOCIATE 이상) 로그인 상태, 유효한 액세스 토큰 보유 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청<br>2. 요청 본문에 `fileSize: 10485760` (10MB 정확히) 포함 |
| **입력 데이터** | `{ "fileName": "test.png", "contentType": "image/png", "fileSize": 10485760 }` |
| **기대 결과** | HTTP 200 응답, `presignedUrl`과 `objectKey`가 포함된 JSON 반환 |
| **비고** | STOR-INV-01 경계값 상한 |

#### TC-002: 최대 허용 크기 초과(10MB+1B) 파일 업로드 URL 요청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청<br>2. 요청 본문에 `fileSize: 10485761` (10MB+1B) 포함 |
| **입력 데이터** | `{ "fileName": "large.png", "contentType": "image/png", "fileSize": 10485761 }` |
| **기대 결과** | HTTP 400 응답, 파일 크기 초과 관련 오류 메시지 반환 |
| **비고** | STOR-INV-01 경계값 초과, `@Max(10485760)` 검증 |

#### TC-003: 최소 유효 크기(1B) 파일 업로드 URL 요청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청<br>2. 요청 본문에 `fileSize: 1` 포함 |
| **입력 데이터** | `{ "fileName": "tiny.png", "contentType": "image/png", "fileSize": 1 }` |
| **기대 결과** | HTTP 200 응답, Presigned URL 정상 발급 |
| **비고** | STOR-INV-01 경계값 하한, `@Min(1)` 검증 |

#### TC-004: 파일 크기 0바이트 업로드 URL 요청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청<br>2. 요청 본문에 `fileSize: 0` 포함 |
| **입력 데이터** | `{ "fileName": "empty.png", "contentType": "image/png", "fileSize": 0 }` |
| **기대 결과** | HTTP 400 응답, 파일 크기 오류 메시지 반환 |
| **비고** | 빈 파일은 무효 입력 |

### STOR-INV-02: 허용 파일 타입 제한

#### TC-005: 허용된 4종 파일 타입 각각 업로드 URL 요청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. 허용된 4종 Content-Type 각각에 대해 `POST /api/storage/presigned-url` 요청<br>2. 각 요청에서 정상 응답 확인 |
| **입력 데이터** | Content-Type별 4건: `image/jpeg`, `image/png`, `image/gif`, `image/webp` / 공통: `fileSize: 1024`, `fileName: "test.{ext}"` |
| **기대 결과** | 4건 모두 HTTP 200 응답, 각각 유효한 Presigned URL 발급 |
| **비고** | STOR-INV-02, allowlist 정상 동작 확인. 파라미터화 테스트 권장 |

#### TC-006: 금지된 파일 타입 업로드 URL 요청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. 금지된 Content-Type으로 `POST /api/storage/presigned-url` 요청<br>2. 각 요청에서 거부 응답 확인 |
| **입력 데이터** | Content-Type별 5건: `image/bmp`, `image/svg+xml`, `image/tiff`, `application/pdf`, `text/plain` / 공통: `fileSize: 1024`, `fileName: "test.bmp"` |
| **기대 결과** | 5건 모두 HTTP 400 응답, 허용되지 않은 파일 타입 오류 메시지 |
| **비고** | STOR-INV-02, allowlist 위반 검증 |

### STOR-INV-03: Presigned URL 만료 시간

#### TC-007: 업로드용 Presigned URL 5분 만료 시간 설정 검증

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | S3Client를 Mock으로 대체 |
| **테스트 절차** | 1. 업로드용 Presigned URL 생성 서비스 호출<br>2. S3Client Mock의 `generatePresignedUrl()` 호출 시 전달된 만료 시간 파라미터 검증 |
| **입력 데이터** | 유효한 업로드 요청 |
| **기대 결과** | S3Client에 전달된 만료 시간이 5분(300초)으로 설정됨 |
| **비고** | STOR-INV-03, SDK 호출 파라미터 검증 |

#### TC-008: 다운로드용 Presigned URL 1시간 만료 시간 설정 검증

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | S3Client를 Mock으로 대체, COMPLETED 상태의 파일 메타데이터 존재 |
| **테스트 절차** | 1. 다운로드용 Presigned URL 생성 서비스 호출<br>2. S3Client Mock의 `generatePresignedUrl()` 호출 시 전달된 만료 시간 파라미터 검증 |
| **입력 데이터** | 존재하는 objectKey (COMPLETED 상태) |
| **기대 결과** | S3Client에 전달된 만료 시간이 1시간(3600초)으로 설정됨 |
| **비고** | STOR-INV-03, SDK 호출 파라미터 검증 |

### STOR-INV-04: Object Key 유일성

#### TC-009: 동일 파일명으로 다수 업로드 시 각각 다른 Object Key 생성

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. 동일한 파일명으로 Object Key 생성 로직을 10회 호출<br>2. 생성된 10개의 Object Key가 모두 다른지 확인 |
| **입력 데이터** | `fileName: "photo.png"`, `contentType: "image/png"` 동일 입력 10회 |
| **기대 결과** | 10개의 Object Key가 모두 상이하며, 각각 UUID를 포함 |
| **비고** | STOR-INV-04 |

#### TC-010: Object Key 형식이 규약에 부합하는지 검증

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. Object Key 생성 로직 호출<br>2. 반환된 Key가 `{사용처}/{YYYY}/{MM}/{DD}/{UUID}.{확장자}` 형식인지 정규표현식으로 검증 |
| **입력 데이터** | `contentType: "image/png"`, 사용처: `posts` |
| **기대 결과** | Object Key가 `posts/2026/02/26/{UUID-v4-또는-v7}.png` 형식과 일치 |
| **비고** | STOR-INV-04, 정규표현식: `^(posts|profiles|events)/\d{4}/\d{2}/\d{2}/[0-9a-f-]{36}\.\w+$` |

### STOR-INV-05: 업로드 완료 확인

#### TC-011: S3 HEAD 검증 성공 시 COMPLETED 상태 전이

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | REQUESTED 상태의 파일 메타데이터 DB 저장, S3Client Mock이 HEAD 요청에 200 + 일치하는 Content-Type/Content-Length 반환 설정 |
| **테스트 절차** | 1. `POST /api/storage/confirm` 호출 (유효한 objectKey)<br>2. DB에서 해당 메타데이터의 상태 확인 |
| **입력 데이터** | `{ "objectKey": "posts/2026/02/26/{UUID}.png" }` |
| **기대 결과** | HTTP 200 응답, `{ status: "COMPLETED", objectKey: "..." }` 반환, DB 상태가 COMPLETED로 갱신 |
| **비고** | STOR-INV-05, 정상 완료 흐름 |

#### TC-012: 존재하지 않는 Object Key로 완료 알림 시 실패

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | S3Client Mock이 HEAD 요청에 404 반환 설정 |
| **테스트 절차** | 1. `POST /api/storage/confirm` 호출 (S3에 실제 파일이 없는 objectKey) |
| **입력 데이터** | `{ "objectKey": "posts/2026/02/26/{UUID-없는-파일}.png" }` |
| **기대 결과** | 응답에 `status: "FAILED"`, `reason` 필드 포함, DB 상태가 FAILED로 갱신 |
| **비고** | STOR-INV-05, HEAD 검증 실패 시나리오 |

### STOR-INV-06: 미인증 사용자 업로드 차단

#### TC-013: JWT 없이 업로드 URL 요청 시 401 응답

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증되지 않은 상태 (Authorization 헤더 없음) |
| **테스트 절차** | 1. Authorization 헤더 없이 `POST /api/storage/presigned-url` 요청 |
| **입력 데이터** | `{ "fileName": "test.png", "contentType": "image/png", "fileSize": 1024 }` (헤더에 JWT 없음) |
| **기대 결과** | HTTP 401 Unauthorized 응답 |
| **비고** | STOR-INV-06, Spring Security JWT 인증 필수 |

### STOR-INV-07: Content-Type 일치 검증

#### TC-014: Presigned URL 생성 시 Content-Type 조건 포함 검증

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | S3Client를 Mock으로 대체 |
| **테스트 절차** | 1. `image/png` Content-Type으로 Presigned URL 생성 서비스 호출<br>2. S3Client Mock에 전달된 요청 객체에서 Content-Type 조건 확인 |
| **입력 데이터** | `contentType: "image/png"` |
| **기대 결과** | S3 Presigned URL 생성 요청에 `Content-Type: image/png` 조건이 포함됨 |
| **비고** | STOR-INV-07, Content-Type 위변조 방지 |

### STOR-INV-08: 고아 파일 방지

#### TC-015: REQUESTED 상태 24시간 경과 시 스케줄러가 EXPIRED로 전환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | REQUESTED 상태이고 `createdAt`이 현재 시각 기준 24시간 이전인 메타데이터 DB 레코드 존재 |
| **테스트 절차** | 1. 스케줄러의 만료 처리 메서드를 직접 호출<br>2. 대상 레코드의 상태 확인 |
| **입력 데이터** | DB에 `status=REQUESTED`, `createdAt=NOW()-25시간` 레코드 |
| **기대 결과** | 해당 레코드의 상태가 EXPIRED로 갱신됨 |
| **비고** | STOR-INV-08, 스케줄러 로직 검증 |

#### TC-016: REQUESTED 상태 24시간 미경과 레코드는 EXPIRED로 전환되지 않음

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | REQUESTED 상태이고 `createdAt`이 현재 시각 기준 23시간 59분 전인 메타데이터 DB 레코드 존재 |
| **테스트 절차** | 1. 스케줄러의 만료 처리 메서드를 직접 호출<br>2. 대상 레코드의 상태 확인 |
| **입력 데이터** | DB에 `status=REQUESTED`, `createdAt=NOW()-23시간59분` 레코드 |
| **기대 결과** | 해당 레코드의 상태가 REQUESTED로 유지됨 (변경 없음) |
| **비고** | STOR-INV-08, 24시간 경계값 미달 |

### STOR-INV-09: 파일 삭제 정책

#### TC-017: S3 삭제 성공 후 DB Soft Delete 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | COMPLETED 상태의 파일 메타데이터 DB 존재, S3Client Mock이 삭제 성공 반환, 해당 파일을 참조하는 상위 엔티티 없음, OPERATOR 이상 권한 보유 |
| **테스트 절차** | 1. 파일 삭제 API 호출<br>2. S3 삭제 Mock 호출 확인<br>3. DB에서 해당 메타데이터의 Soft Delete 상태 확인 |
| **입력 데이터** | 삭제 대상 objectKey |
| **기대 결과** | HTTP 200 응답, S3 삭제 Mock이 호출됨, DB 레코드가 Soft Delete 처리됨 (삭제 시각 기록) |
| **비고** | STOR-INV-09, 삭제 순서: S3 먼저 -> DB 후 |

#### TC-018: S3 삭제 실패 시 DB 미변경 및 500 응답

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | COMPLETED 상태의 파일 메타데이터 DB 존재, S3Client Mock이 삭제 시 예외 발생하도록 설정 |
| **테스트 절차** | 1. 파일 삭제 API 호출<br>2. DB에서 해당 메타데이터의 상태 확인 |
| **입력 데이터** | 삭제 대상 objectKey (S3 삭제 실패 시뮬레이션) |
| **기대 결과** | HTTP 500 응답, DB 레코드가 변경되지 않음 (Soft Delete 미처리, 롤백) |
| **비고** | STOR-INV-09, S3 삭제 실패 시 트랜잭션 롤백 |

---

## 2. 상태 모델 전이

### 정상 전이

#### TC-019: 초기 상태 -> REQUESTED 전이 (업로드 URL 요청)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 인증된 사용자, 유효한 파일 타입/크기 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청<br>2. DB에서 생성된 메타데이터 레코드 확인 |
| **입력 데이터** | `{ "fileName": "photo.png", "contentType": "image/png", "fileSize": 5242880 }` |
| **기대 결과** | HTTP 200, DB에 `status=REQUESTED` 레코드 생성, `objectKey`, `uploaderUserId`, `originalFileName`, `contentType`, `fileSize`, `createdAt` 저장 |
| **비고** | 상태 모델 2-3 첫 번째 전이 |

#### TC-020: REQUESTED -> CONFIRMING -> COMPLETED 정상 전이 (완료 확인)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | REQUESTED 상태의 메타데이터 존재, S3Client Mock이 HEAD 성공 반환 |
| **테스트 절차** | 1. `POST /api/storage/confirm` 호출<br>2. 중간 상태(CONFIRMING) 전이 확인 (서비스 로직 내부)<br>3. 최종 상태(COMPLETED) 확인 |
| **입력 데이터** | `{ "objectKey": "{존재하는-REQUESTED-상태-Key}" }` |
| **기대 결과** | DB 상태가 REQUESTED -> CONFIRMING -> COMPLETED로 전이, `completedAt` 기록 |
| **비고** | Happy Path 전체 전이 검증 |

#### TC-021: CONFIRMING -> FAILED 전이 (HEAD 검증 실패)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | REQUESTED 상태의 메타데이터 존재, S3Client Mock이 HEAD 요청에 404 반환 |
| **테스트 절차** | 1. `POST /api/storage/confirm` 호출<br>2. DB에서 상태 확인 |
| **입력 데이터** | `{ "objectKey": "{REQUESTED-상태-Key}" }` |
| **기대 결과** | DB 상태가 FAILED로 갱신, 응답에 `status: "FAILED"`, `reason` 포함 |
| **비고** | S3에 파일이 없는 경우 |

#### TC-022: CONFIRMING -> FAILED 전이 (Content-Type 불일치)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | REQUESTED 상태의 메타데이터 존재 (`contentType: image/png`), S3Client Mock이 HEAD 요청에 200 반환하되 Content-Type이 `image/jpeg`로 불일치 |
| **테스트 절차** | 1. `POST /api/storage/confirm` 호출<br>2. DB에서 상태 확인 |
| **입력 데이터** | `{ "objectKey": "{REQUESTED-상태-Key}" }` |
| **기대 결과** | DB 상태가 FAILED로 갱신, 사유에 Content-Type 불일치 명시 |
| **비고** | STOR-INV-05 사후조건 (Content-Type 일치 검증) |

#### TC-023: CONFIRMING -> FAILED 전이 (Content-Length 불일치)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | REQUESTED 상태의 메타데이터 존재 (`fileSize: 1024`), S3Client Mock이 HEAD 요청에 200 반환하되 Content-Length가 `2048`로 불일치 |
| **테스트 절차** | 1. `POST /api/storage/confirm` 호출<br>2. DB에서 상태 확인 |
| **입력 데이터** | `{ "objectKey": "{REQUESTED-상태-Key}" }` |
| **기대 결과** | DB 상태가 FAILED로 갱신, 사유에 Content-Length 불일치 명시 |
| **비고** | STOR-INV-05 사후조건 (Content-Length 일치 검증) |

#### TC-024: REQUESTED -> EXPIRED 전이 (스케줄러)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | TC-015와 동일 |
| **테스트 절차** | TC-015와 동일 |
| **입력 데이터** | TC-015와 동일 |
| **기대 결과** | TC-015와 동일 |
| **비고** | 상태 모델 전이 관점에서의 EXPIRED 전환. TC-015(불변조건 관점)와 동일 시나리오이나 상태 전이 검증 목적 |

### 금지된 전이

#### TC-025: COMPLETED -> UPLOADING 전이 시도 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | COMPLETED 상태의 메타데이터 존재 |
| **테스트 절차** | 1. COMPLETED 상태인 objectKey로 다시 완료 알림이 아닌, 새 업로드를 시도하는 시나리오 시뮬레이션 (동일 Object Key에 대한 새 Presigned URL 발급 시도) |
| **입력 데이터** | 이미 COMPLETED 상태인 objectKey |
| **기대 결과** | 요청 거부, 완료된 업로드는 재업로드 불가 |
| **비고** | 금지된 전이 검증 |

#### TC-026: FAILED -> COMPLETED 전이 시도 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | FAILED 상태의 메타데이터 존재 |
| **테스트 절차** | 1. FAILED 상태인 objectKey로 `POST /api/storage/confirm` 호출 |
| **입력 데이터** | `{ "objectKey": "{FAILED-상태-Key}" }` |
| **기대 결과** | 요청 거부 (400 또는 409), 실패한 업로드는 새로 시작해야 함 |
| **비고** | 금지된 전이 검증. 검증 기준서 5-4 무효 동치류 참조 |

#### TC-027: EXPIRED -> UPLOADING 전이 시도 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | EXPIRED 상태의 메타데이터 존재 |
| **테스트 절차** | 1. EXPIRED 상태인 objectKey로 `POST /api/storage/confirm` 호출 |
| **입력 데이터** | `{ "objectKey": "{EXPIRED-상태-Key}" }` |
| **기대 결과** | 요청 거부 (400 또는 409), 만료된 URL로는 업로드 불가, 새 URL 필요 |
| **비고** | 금지된 전이 검증. 검증 기준서 5-4 무효 동치류 참조 |

### 멱등성

#### TC-028: 이미 COMPLETED 상태인 objectKey로 완료 알림 재호출 시 멱등 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | COMPLETED 상태의 메타데이터 존재 |
| **테스트 절차** | 1. COMPLETED 상태인 objectKey로 `POST /api/storage/confirm` 호출<br>2. 응답 확인 |
| **입력 데이터** | `{ "objectKey": "{COMPLETED-상태-Key}" }` |
| **기대 결과** | HTTP 200 응답, `{ status: "COMPLETED", objectKey: "..." }` 반환, HEAD 검증 생략, DB 상태 변경 없음 |
| **비고** | 검증 기준서 5-4 멱등성 규칙. 네트워크 재시도 상황 대비 |

---

## 3. 보안 정책

### SEC-STOR-01: 소유권 검증 - 업로드 완료 알림

#### TC-029: 다른 사용자의 업로드 완료 알림 시도 시 403 응답

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 사용자 A가 요청하여 REQUESTED 상태인 메타데이터 존재, 사용자 B로 로그인 |
| **테스트 절차** | 1. 사용자 B의 JWT로 `POST /api/storage/confirm` 호출 (사용자 A의 objectKey 지정) |
| **입력 데이터** | `{ "objectKey": "{사용자A의-objectKey}" }`, Authorization: 사용자 B의 JWT |
| **기대 결과** | HTTP 403 Forbidden 응답 |
| **비고** | SEC-STOR-01, 본인 요청만 완료 알림 가능 |

### SEC-STOR-02: 미인증 사용자 업로드 URL 요청

#### TC-030: 미인증 사용자의 업로드 URL 요청 시 401 응답

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증되지 않은 상태 |
| **테스트 절차** | 1. Authorization 헤더 없이 `POST /api/storage/presigned-url` 요청 |
| **입력 데이터** | `{ "fileName": "test.png", "contentType": "image/png", "fileSize": 1024 }` |
| **기대 결과** | HTTP 401 Unauthorized 응답 |
| **비고** | SEC-STOR-02, TC-013과 동일 시나리오 (보안 관점 재확인) |

### SEC-STOR-03: 존재하지 않는 Object Key 다운로드

#### TC-031: 존재하지 않는 Object Key로 다운로드 URL 요청 시 404 응답

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `GET /api/storage/download-url?objectKey=non-existent-key` 요청 |
| **입력 데이터** | `objectKey=posts/2026/02/26/00000000-0000-0000-0000-000000000000.png` (존재하지 않는 Key) |
| **기대 결과** | HTTP 404 Not Found 응답 |
| **비고** | SEC-STOR-03, DB에 메타데이터가 없는 Key로의 다운로드 시도 |

### SEC-STOR-04: 미인증 사용자 다운로드 URL 요청

#### TC-032: 미인증 사용자의 다운로드 URL 요청 시 401 응답

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증되지 않은 상태 |
| **테스트 절차** | 1. Authorization 헤더 없이 `GET /api/storage/download-url?objectKey={key}` 요청 |
| **입력 데이터** | `objectKey=posts/2026/02/26/{UUID}.png` |
| **기대 결과** | HTTP 401 Unauthorized 응답 |
| **비고** | SEC-STOR-04 |

### URL 보안 정책

#### TC-033: Presigned URL이 HTTPS로 생성되는지 검증

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | S3Client Mock 설정 |
| **테스트 절차** | 1. Presigned URL 생성 서비스 호출<br>2. 반환된 URL이 `https://`로 시작하는지 검증 |
| **입력 데이터** | 유효한 업로드 요청 |
| **기대 결과** | 반환된 presignedUrl이 `https://`로 시작 |
| **비고** | URL 보안 정책, HTTPS 강제 |

#### TC-034: Presigned URL 생성 시 Content-Length 상한 조건 포함 검증

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | S3Client Mock 설정 |
| **테스트 절차** | 1. Presigned URL 생성 서비스 호출<br>2. S3Client에 전달된 요청에 Content-Length 조건(상한)이 포함되었는지 확인 |
| **입력 데이터** | `fileSize: 5242880` (5MB) |
| **기대 결과** | S3 Presigned URL 생성 요청에 Content-Length 상한 조건이 포함됨 |
| **비고** | URL 보안 정책, Content-Length 제한 |

### 역할별 접근 제어

#### TC-035: 파일 메타데이터 삭제 - ASSOCIATE/MEMBER 권한으로 시도 시 403 응답

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | ASSOCIATE 또는 MEMBER 권한으로 로그인, COMPLETED 상태의 파일 메타데이터 존재 |
| **테스트 절차** | 1. ASSOCIATE 권한의 JWT로 파일 삭제 API 호출 |
| **입력 데이터** | 삭제 대상 objectKey |
| **기대 결과** | HTTP 403 Forbidden 응답 |
| **비고** | RBAC 매트릭스: ASSOCIATE/MEMBER는 삭제 권한 없음. OPERATOR 이상만 삭제 가능 |

#### TC-036: 참조 중인 파일 삭제 시도 시 409 응답

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | COMPLETED 상태의 파일 메타데이터 존재, 해당 파일이 게시글(또는 프로필 등) 엔티티에서 참조 중, OPERATOR 이상 권한 보유 |
| **테스트 절차** | 1. 참조 중인 파일의 삭제 API 호출 |
| **입력 데이터** | 참조 중인 objectKey |
| **기대 결과** | HTTP 409 Conflict 응답, 참조 무결성 위반 관련 오류 메시지 |
| **비고** | STOR-INV-09, 참조 무결성 검증 |

---

## 4. 입력 경계값

### 파일 크기 경계값

#### TC-037: 파일 크기 음수 값(-1) 업로드 URL 요청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, `fileSize: -1` |
| **입력 데이터** | `{ "fileName": "test.png", "contentType": "image/png", "fileSize": -1 }` |
| **기대 결과** | HTTP 400 응답 |
| **비고** | 음수 값 검증, `@Min(1)` 위반 |

#### TC-038: 파일 크기 null 업로드 URL 요청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, `fileSize` 필드 누락 |
| **입력 데이터** | `{ "fileName": "test.png", "contentType": "image/png" }` (fileSize 없음) |
| **기대 결과** | HTTP 400 응답, `@NotNull` 위반 메시지 |
| **비고** | null 입력 검증 |

#### TC-039: 일반 소형 파일(1KB) 업로드 URL 요청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 하 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, `fileSize: 1024` |
| **입력 데이터** | `{ "fileName": "small.png", "contentType": "image/png", "fileSize": 1024 }` |
| **기대 결과** | HTTP 200 응답, Presigned URL 정상 발급 |
| **비고** | 유효 동치류 대표값 |

#### TC-040: 대형 파일(50MB) 업로드 URL 요청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, `fileSize: 52428800` |
| **입력 데이터** | `{ "fileName": "huge.png", "contentType": "image/png", "fileSize": 52428800 }` |
| **기대 결과** | HTTP 400 응답, 파일 크기 초과 오류 |
| **비고** | 무효 동치류 대표값 |

### 파일명 경계값

#### TC-041: 최소 길이 파일명(1자) 업로드 URL 요청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, `fileName: "a"` |
| **입력 데이터** | `{ "fileName": "a", "contentType": "image/png", "fileSize": 1024 }` |
| **기대 결과** | HTTP 200 응답, Presigned URL 정상 발급 |
| **비고** | `@Size(max=255)` 하한 경계, 확장자 없어도 Content-Type 기준 판단 |

#### TC-042: 최대 길이 파일명(255자) 업로드 URL 요청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, `fileName`이 정확히 255자 |
| **입력 데이터** | `{ "fileName": "a{254회 반복}.png", "contentType": "image/png", "fileSize": 1024 }` (총 255자) |
| **기대 결과** | HTTP 200 응답, Presigned URL 정상 발급 |
| **비고** | `@Size(max=255)` 상한 경계 |

#### TC-043: 파일명 256자 이상 업로드 URL 요청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, `fileName`이 256자 |
| **입력 데이터** | `{ "fileName": "a{255회 반복}.png", "contentType": "image/png", "fileSize": 1024 }` (총 256자) |
| **기대 결과** | HTTP 400 응답, 파일명 길이 초과 오류 |
| **비고** | `@Size(max=255)` 초과 |

#### TC-044: 빈 문자열 파일명 업로드 URL 요청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, `fileName: ""` |
| **입력 데이터** | `{ "fileName": "", "contentType": "image/png", "fileSize": 1024 }` |
| **기대 결과** | HTTP 400 응답, `@NotBlank` 위반 메시지 |
| **비고** | 빈 문자열 검증 |

#### TC-045: null 파일명 업로드 URL 요청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, `fileName` 필드 누락 |
| **입력 데이터** | `{ "contentType": "image/png", "fileSize": 1024 }` (fileName 없음) |
| **기대 결과** | HTTP 400 응답, `@NotBlank` 위반 메시지 |
| **비고** | null 입력 검증 |

#### TC-046: 한글 파일명 업로드 URL 요청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, 한글 파일명 사용 |
| **입력 데이터** | `{ "fileName": "동아리 사진.png", "contentType": "image/png", "fileSize": 1024 }` |
| **기대 결과** | HTTP 200 응답, Presigned URL 정상 발급, Object Key는 UUID 기반으로 생성됨 (한글 미포함) |
| **비고** | 검증 기준서 5-3, 원본 파일명은 메타데이터에만 저장 |

#### TC-047: 특수문자 포함 파일명 업로드 URL 요청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 하 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, 특수문자 포함 파일명 사용 |
| **입력 데이터** | `{ "fileName": "photo (1).png", "contentType": "image/png", "fileSize": 1024 }` |
| **기대 결과** | HTTP 200 응답, Presigned URL 정상 발급 |
| **비고** | 검증 기준서 5-3, Object Key는 UUID로 대체되므로 특수문자 무관 |

#### TC-048: 공백만 포함된 파일명 업로드 URL 요청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, `fileName: "   .png"` |
| **입력 데이터** | `{ "fileName": "   .png", "contentType": "image/png", "fileSize": 1024 }` |
| **기대 결과** | HTTP 400 응답, `@NotBlank` 위반 메시지 |
| **비고** | 검증 기준서 5-3, 공백만으로 구성된 파일명은 거부 |

### Content-Type 경계값

#### TC-049: null Content-Type 업로드 URL 요청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청, `contentType` 필드 누락 |
| **입력 데이터** | `{ "fileName": "test.png", "fileSize": 1024 }` (contentType 없음) |
| **기대 결과** | HTTP 400 응답, Content-Type 필수 오류 메시지 |
| **비고** | null 입력 검증 |

### objectKey 입력 검증

#### TC-050: 빈 문자열 objectKey로 완료 알림 시 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 인증된 사용자 로그인 상태 |
| **테스트 절차** | 1. `POST /api/storage/confirm` 요청, `objectKey: ""` |
| **입력 데이터** | `{ "objectKey": "" }` |
| **기대 결과** | HTTP 400 응답, `@NotBlank` 위반 메시지 |
| **비고** | 검증 기준서 5-4, 무효 동치류 |

---

## 5. 외부 의존성 실패

### S3 서비스 장애

#### TC-051: Presigned URL 생성 시 S3 SDK 장애 발생 시 500 응답

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | S3Client Mock이 `generatePresignedUrl()` 호출 시 `SdkClientException` 발생하도록 설정 |
| **테스트 절차** | 1. `POST /api/storage/presigned-url` 요청 |
| **입력 데이터** | 유효한 업로드 요청 데이터 |
| **기대 결과** | HTTP 500 Internal Server Error 응답, 에러 로깅 확인 |
| **비고** | 외부 의존성 실패 정책 4-1 |

#### TC-052: 다운로드 URL 생성 시 S3 SDK 장애 발생 시 500 응답

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | COMPLETED 상태의 메타데이터 존재, S3Client Mock이 다운로드 URL 생성 시 예외 발생하도록 설정 |
| **테스트 절차** | 1. `GET /api/storage/download-url?objectKey={key}` 요청 |
| **입력 데이터** | 존재하는 objectKey (COMPLETED 상태) |
| **기대 결과** | HTTP 500 Internal Server Error 응답 |
| **비고** | 외부 의존성 실패 정책 4-1 |

#### TC-053: S3 HEAD 검증 중 S3 장애 발생 시 FAILED 전이

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 서비스 통합 (S3Client Mock) |
| **사전 조건** | REQUESTED 상태의 메타데이터 존재, S3Client Mock이 HEAD 요청 시 예외 발생하도록 설정 |
| **테스트 절차** | 1. `POST /api/storage/confirm` 호출<br>2. DB 상태 확인 |
| **입력 데이터** | `{ "objectKey": "{REQUESTED-상태-Key}" }` |
| **기대 결과** | DB 상태가 FAILED로 갱신, 사유에 S3 장애 명시 |
| **비고** | 외부 의존성 실패 정책 4-1, HEAD 검증 실패 경로 |

### Presigned URL 만료

#### TC-054: 만료된 업로드 URL로 S3 PUT 시 403 응답 (E2E)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | E2E (LocalStack 또는 실제 S3) |
| **사전 조건** | 업로드용 Presigned URL 발급 완료, 5분 이상 경과 |
| **테스트 절차** | 1. Presigned URL 발급<br>2. 5분 이상 대기 (또는 테스트에서 만료된 URL 시뮬레이션)<br>3. 해당 URL로 S3 PUT 요청 |
| **입력 데이터** | 만료된 Presigned URL + 이미지 바이너리 |
| **기대 결과** | S3가 HTTP 403 Forbidden (AccessDenied) 반환 |
| **비고** | STOR-INV-03, E2E 또는 수동 QA 수준 테스트 |

#### TC-055: 만료된 다운로드 URL로 S3 GET 시 403 응답 (E2E)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | E2E (LocalStack 또는 실제 S3) |
| **사전 조건** | 다운로드용 Presigned URL 발급 완료, 1시간 이상 경과 |
| **테스트 절차** | 1. 다운로드 Presigned URL 발급<br>2. 1시간 이상 대기 (또는 테스트에서 만료된 URL 시뮬레이션)<br>3. 해당 URL로 S3 GET 요청 |
| **입력 데이터** | 만료된 다운로드 Presigned URL |
| **기대 결과** | S3가 HTTP 403 Forbidden (AccessDenied) 반환 |
| **비고** | STOR-INV-03, E2E 또는 수동 QA 수준 테스트 |

### 네트워크 단절

#### TC-056: 완료 알림 API 재호출 시 멱등 동작 검증 (네트워크 재시도 대비)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | COMPLETED 상태의 메타데이터 존재 |
| **테스트 절차** | 1. COMPLETED 상태인 objectKey로 `POST /api/storage/confirm`을 연속 3회 호출<br>2. 모든 응답 확인 |
| **입력 데이터** | `{ "objectKey": "{COMPLETED-상태-Key}" }` |
| **기대 결과** | 3회 모두 HTTP 200, `{ status: "COMPLETED" }` 반환, HEAD 검증 생략 |
| **비고** | 외부 의존성 실패 정책 4-3 멱등성, TC-028과 연관 |

#### TC-057: S3 PUT 불일치 Content-Type으로 업로드 시 S3 403 거부 (E2E)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | E2E (LocalStack 또는 실제 S3) |
| **사전 조건** | `image/png` Content-Type으로 Presigned URL 발급 완료 |
| **테스트 절차** | 1. 발급받은 Presigned URL로 S3 PUT 요청 시 `Content-Type: image/jpeg`로 전송 |
| **입력 데이터** | JPEG 이미지 바이너리 + `Content-Type: image/jpeg` 헤더 |
| **기대 결과** | S3가 HTTP 403 Forbidden 반환 (Presigned URL 조건 불일치) |
| **비고** | STOR-INV-07, S3 레벨 Content-Type 강제 검증 |

---

## 6. 관측 가능성

### 로그 검증

#### TC-058: Presigned URL 생성 시 로그에 userId, contentType, fileSize 기록

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 로그 캡처(LogCaptor 등) 설정 |
| **테스트 절차** | 1. Presigned URL 생성 서비스 호출<br>2. 캡처된 로그 메시지 확인 |
| **입력 데이터** | 유효한 업로드 요청 |
| **기대 결과** | 로그에 `userId`, `contentType`, `fileSize` 포함 확인, Presigned URL 자체는 로그에 미포함 |
| **비고** | 관측 가능성 7-1, 7-3 (URL 로깅 금지) |

#### TC-059: 업로드 완료 확인 시 로그에 objectKey, userId 기록

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 로그 캡처 설정, REQUESTED 상태의 메타데이터 존재 |
| **테스트 절차** | 1. 완료 확인 서비스 호출 (HEAD 검증 성공 Mock)<br>2. 캡처된 로그 메시지 확인 |
| **입력 데이터** | 유효한 objectKey |
| **기대 결과** | 로그에 `objectKey`, `userId` 포함 확인 |
| **비고** | 관측 가능성 7-1 |

#### TC-060: Presigned URL 자체가 로그에 기록되지 않는지 검증

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 로그 캡처 설정 |
| **테스트 절차** | 1. Presigned URL 생성 서비스 호출<br>2. 전체 로그 출력에서 Presigned URL 문자열 패턴 검색 |
| **입력 데이터** | 유효한 업로드 요청 |
| **기대 결과** | 로그에 `X-Amz-Signature`, `X-Amz-Credential` 등 서명 관련 문자열이 포함되지 않음 |
| **비고** | 관측 가능성 7-3, 보안 위험 방지. URL에 서명 정보 포함되므로 절대 로깅 금지 |

### 메타데이터 감사 이력

#### TC-061: 업로드 요청 시 메타데이터에 필수 감사 필드 전부 저장

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 인증된 사용자 |
| **테스트 절차** | 1. Presigned URL 생성 요청<br>2. DB에서 생성된 메타데이터 레코드 조회<br>3. 모든 감사 필드 존재 여부 확인 |
| **입력 데이터** | `{ "fileName": "audit-test.png", "contentType": "image/png", "fileSize": 2048 }` |
| **기대 결과** | DB 레코드에 `objectKey`, `uploaderUserId`, `originalFileName`, `contentType`, `fileSize`, `status(=REQUESTED)`, `createdAt` 모두 non-null로 저장됨 |
| **비고** | 관측 가능성 7-2, 감사 이력 완전성 |

#### TC-062: 업로드 완료 시 completedAt 필드 기록 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | REQUESTED 상태의 메타데이터 존재, S3 HEAD 검증 성공 Mock |
| **테스트 절차** | 1. 완료 확인 API 호출<br>2. DB에서 해당 레코드의 `completedAt` 필드 확인 |
| **입력 데이터** | 유효한 objectKey |
| **기대 결과** | `completedAt`이 non-null로 기록되며, `createdAt` 이후 시각임 |
| **비고** | 관측 가능성 7-2, 업로드 지연 시간 분석 용도 |

---

## 커버리지 매핑

### 불변조건 커버리지

| 불변조건 | 커버 테스트 케이스 | 정상 | 비정상/경계값 |
|---------|:---:|:---:|:---:|
| STOR-INV-01 (파일 크기 제한) | TC-001~004, TC-037~040 | 3 | 5 |
| STOR-INV-02 (허용 파일 타입) | TC-005~006, TC-049 | 1 | 2 |
| STOR-INV-03 (URL 만료 시간) | TC-007~008, TC-054~055 | 2 | 2 |
| STOR-INV-04 (Object Key 유일성) | TC-009~010 | 2 | 0 |
| STOR-INV-05 (업로드 완료 확인) | TC-011~012, TC-022~023 | 1 | 3 |
| STOR-INV-06 (미인증 업로드 차단) | TC-013, TC-030 | 0 | 2 |
| STOR-INV-07 (Content-Type 일치) | TC-014, TC-057 | 1 | 1 |
| STOR-INV-08 (고아 파일 방지) | TC-015~016 | 1 | 1 |
| STOR-INV-09 (파일 삭제 정책) | TC-017~018, TC-036 | 1 | 2 |

### 보안 정책 커버리지

| 보안 정책 | 커버 테스트 케이스 |
|---------|:---:|
| SEC-STOR-01 (소유권 검증) | TC-029 |
| SEC-STOR-02 (미인증 업로드) | TC-030 |
| SEC-STOR-03 (미존재 Key 다운로드) | TC-031 |
| SEC-STOR-04 (미인증 다운로드) | TC-032 |

### 상태 전이 커버리지

| 전이 | 커버 테스트 케이스 |
|------|:---:|
| -> REQUESTED | TC-019 |
| REQUESTED -> CONFIRMING -> COMPLETED | TC-020 |
| CONFIRMING -> FAILED (파일 미존재) | TC-021 |
| CONFIRMING -> FAILED (Content-Type 불일치) | TC-022 |
| CONFIRMING -> FAILED (Content-Length 불일치) | TC-023 |
| REQUESTED -> EXPIRED | TC-024 |
| COMPLETED -> UPLOADING (금지) | TC-025 |
| FAILED -> COMPLETED (금지) | TC-026 |
| EXPIRED -> UPLOADING (금지) | TC-027 |
| COMPLETED 재확인 (멱등) | TC-028 |

### 테스트 레벨 분포

| 테스트 레벨 | 테스트 케이스 수 |
|-----------|:---:|
| 단위 테스트 | 4 (TC-003, TC-009, TC-010, TC-041 등 포함) |
| 서비스 통합 테스트 (S3 Mock) | 46 |
| 통합 테스트 (Controller/MockMvc) | 8 |
| E2E 테스트 (LocalStack/실제 S3) | 4 (TC-054, TC-055, TC-057 등) |
