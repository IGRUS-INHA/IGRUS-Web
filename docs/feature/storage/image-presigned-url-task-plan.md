# S3 Presigned URL 이미지 업로드/다운로드 작업 계획

## 개요

- **기능 설명**: S3 Presigned URL 기반 이미지 업로드/다운로드 기능을 완전 신규 구현한다. 프론트엔드에서 S3로 직접 파일을 업로드/다운로드하는 Presigned URL 방식을 채택하며, 백엔드는 URL 발급과 메타데이터 관리를 담당한다.
- **관련 문서**
  - 검증 기준서: [`docs/criteria/storage/image-presigned-url-verification-criteria.md`](../../criteria/storage/image-presigned-url-verification-criteria.md)
  - 테스트 케이스: [`docs/test-case/storage/image-presigned-url-test-cases.md`](../../test-case/storage/image-presigned-url-test-cases.md)
- **작성일**: 2026-02-26
- **사용처**: 게시글 이미지, 프로필 이미지, 행사 이미지 등 범용
- **기술 스택 참고**: Backend -- Java 21 + Spring Boot 3.5.9 + Spring Data JPA + MySQL, Frontend -- React 19 + TypeScript + Vite 7 + Zustand + TanStack Query

---

## 작업 목록

### 1. 인프라 및 프로젝트 설정

#### TASK-001: AWS S3 SDK 의존성 추가 및 S3 클라이언트 설정

- **작업명**: AWS S3 SDK 의존성 추가 및 S3Client Bean 설정
- **설명**: `build.gradle`에 AWS S3 SDK(spring-cloud-aws-starter-s3 또는 software.amazon.awssdk:s3) 의존성을 추가하고, `S3Client` 또는 `S3Presigner` Bean을 등록하는 설정 클래스를 작성한다. 프로필별(dev/staging/prod) S3 버킷 이름, 리전 등의 프로퍼티를 `application.yml`에 정의한다.
- **관련 검증 기준**: STOR-INV-03 (URL 만료 시간 -- S3 Presigner 설정), 6-3 URL 보안 (HTTPS 강제)
- **관련 테스트 케이스**: TC-033 (HTTPS 검증)
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-002: S3 버킷 CORS 및 Lifecycle Rule 설정 문서화

- **작업명**: S3 버킷 CORS 설정 및 Lifecycle Rule 적용 가이드 작성
- **설명**: S3 버킷에 프론트엔드 도메인만 허용하는 CORS 정책을 설정하고, `uploads/` 프리픽스 내 미완료 객체를 24시간 후 자동 삭제하는 S3 Lifecycle Rule을 설정한다. 인프라 설정이므로 IaC(CloudFormation/Terraform) 또는 AWS 콘솔 가이드로 문서화한다.
- **관련 검증 기준**: STOR-INV-08 (고아 파일 방지 -- S3 Lifecycle), 6-3 URL 보안 (CORS)
- **관련 테스트 케이스**: 없음 (인프라 설정, 수동 QA)
- **선행 작업**: 없음
- **구현 범위**: backend (인프라/DevOps)
- **예상 난이도**: 중

#### TASK-003: Flyway 마이그레이션 -- 파일 메타데이터 테이블 생성

- **작업명**: `file_metadata` 테이블 Flyway 마이그레이션 스크립트 작성
- **설명**: 파일 메타데이터를 저장하는 `file_metadata` 테이블을 생성하는 Flyway 마이그레이션 스크립트를 작성한다. 기존 마이그레이션 최신 버전(V40) 이후 번호를 사용한다.
  - **컬럼명 규칙 (필수)**: 프로젝트 컬럼명 규칙은 `{table_name}_{column_name}` 형식이다. `BaseEntity`/`SoftDeletableEntity`가 자동 관리하는 필드들의 Flyway 컬럼명이 JPA 네이밍 전략과 반드시 일치해야 한다. 기존 마이그레이션(예: `comments_created_at`, `comments_deleted`)을 참고하여 `file_metadata` 테이블은 다음과 같이 작성한다.
  - **도메인 고유 컬럼**: `file_metadata_id` (PK), `file_metadata_object_key` (unique), `file_metadata_uploader_user_id` (FK -> users), `file_metadata_original_file_name`, `file_metadata_content_type`, `file_metadata_file_size`, `file_metadata_status` (enum: REQUESTED/CONFIRMING/COMPLETED/FAILED/EXPIRED), `file_metadata_completed_at`
  - **BaseEntity 관리 컬럼**: `file_metadata_created_at`, `file_metadata_updated_at`, `file_metadata_created_by`, `file_metadata_updated_by`
  - **SoftDeletableEntity 관리 컬럼**: `file_metadata_deleted` (boolean), `file_metadata_deleted_at`, `file_metadata_deleted_by`
- **관련 검증 기준**: STOR-INV-04 (Object Key 유일성 -- unique 제약), 7-2 메타데이터 감사 이력
- **관련 테스트 케이스**: TC-061 (감사 필드 저장), TC-062 (completedAt 기록)
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 2. 백엔드 도메인 계층

#### TASK-004: FileMetadata 엔티티 및 FileUploadStatus 열거형 구현

- **작업명**: FileMetadata JPA 엔티티 및 FileUploadStatus enum 구현
- **설명**: `igrus.web.storage.domain` 패키지에 `FileMetadata` 엔티티를 구현한다. `SoftDeletableEntity`를 상속하여 Soft Delete를 지원한다. `FileUploadStatus` enum(REQUESTED, CONFIRMING, COMPLETED, FAILED, EXPIRED)을 정의한다. 상태 전이 검증 로직을 엔티티 도메인 메서드로 구현한다 (예: `confirm()`, `complete()`, `fail()`, `expire()`). 금지된 전이(COMPLETED->UPLOADING, FAILED->COMPLETED, EXPIRED->UPLOADING) 시도 시 예외를 발생시킨다.
- **관련 검증 기준**: 2-1 상태 모델 전체, 2-2 상태 정의, 2-3 상태 전이 (금지된 전이 포함)
- **관련 테스트 케이스**: TC-019~TC-028 (상태 전이 전체), TC-061 (감사 필드), TC-062 (completedAt)
- **선행 작업**: TASK-003
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-005: ObjectKeyGenerator 유틸리티 구현

- **작업명**: UUID 기반 S3 Object Key 생성 유틸리티 구현
- **설명**: `igrus.web.storage.domain` 패키지에 `ObjectKeyGenerator` 클래스를 구현한다. Object Key 형식은 `{사용처}/{YYYY}/{MM}/{DD}/{UUID}.{확장자}` 이며, UUID v4 또는 v7을 사용한다. Content-Type에서 확장자를 추출하는 로직을 포함한다. 사용처(posts/profiles/events)는 파라미터로 받는다.
- **관련 검증 기준**: STOR-INV-04 (Object Key 유일성, UUID 기반 형식)
- **관련 테스트 케이스**: TC-009 (유일성), TC-010 (형식 규약)
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-006: FileMetadataRepository 구현

- **작업명**: FileMetadata JPA Repository 인터페이스 구현
- **설명**: `igrus.web.storage.repository` 패키지에 `FileMetadataRepository`를 구현한다. 필요 메서드: `findByObjectKey(String objectKey)`, `findByObjectKeyAndDeletedFalse(String objectKey)`, `findByStatusAndCreatedAtBefore(FileUploadStatus status, Instant threshold)` (스케줄러용), `existsByObjectKeyAndDeletedFalse(String objectKey)`.
- **관련 검증 기준**: STOR-INV-05 (완료 확인 -- DB 조회), STOR-INV-08 (고아 파일 방지 -- 만료 대상 조회)
- **관련 테스트 케이스**: TC-015, TC-016 (스케줄러 조회), TC-011, TC-012 (완료 확인 DB 조회)
- **선행 작업**: TASK-004
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 3. 백엔드 에러 처리

#### TASK-007: Storage 도메인 ErrorCode 및 커스텀 예외 구현

- **작업명**: StorageErrorCode 및 커스텀 예외 클래스 구현
- **설명**: `igrus.web.storage.exception` 패키지에 `StorageErrorCode` enum과 관련 커스텀 예외 클래스를 구현한다. 필요한 에러 코드 및 예외: 파일 크기 초과, 허용되지 않은 Content-Type, 파일 메타데이터 미존재, 잘못된 상태 전이, 소유권 검증 실패, S3 작업 실패, 참조 무결성 위반 등. 모든 예외는 `CustomBaseException`을 상속한다.
- **관련 검증 기준**: STOR-INV-01~09 전체 위반 시 에러 코드, SEC-STOR-01~04
- **관련 테스트 케이스**: TC-002 (400), TC-006 (400), TC-012 (FAILED), TC-013 (401), TC-017~018 (500), TC-029 (403), TC-031 (404), TC-036 (409) 등
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 4. 백엔드 서비스 계층 -- 업로드

#### TASK-008: PresignedUrlService -- 업로드용 Presigned URL 생성

- **작업명**: 업로드용 Presigned URL 생성 서비스 구현
- **설명**: `igrus.web.storage.service` 패키지에 Presigned URL 생성 서비스를 구현한다. 입력 검증(파일 크기 1~10,485,760, Content-Type allowlist), S3 Presigner를 통한 PUT URL 생성 (만료 5분, Content-Type 조건, Content-Length 상한 조건 포함), FileMetadata DB 저장 (status=REQUESTED), 로깅 (userId, contentType, fileSize 기록, URL 자체는 로깅 금지) 처리를 포함한다.
- **관련 검증 기준**: STOR-INV-01 (크기 제한), STOR-INV-02 (타입 제한), STOR-INV-03 (5분 만료), STOR-INV-04 (Object Key 생성), STOR-INV-07 (Content-Type 조건), 6-3 URL 보안 (Content-Length 제한), 7-1 로그, 7-3 로그 주의사항
- **관련 테스트 케이스**: TC-001~006 (크기/타입 검증), TC-007 (5분 만료), TC-009~010 (Object Key), TC-014 (Content-Type 조건), TC-019 (REQUESTED 전이), TC-033 (HTTPS), TC-034 (Content-Length), TC-051 (S3 장애), TC-058 (로그), TC-060 (URL 로깅 금지), TC-061 (감사 필드)
- **선행 작업**: TASK-001, TASK-004, TASK-005, TASK-006, TASK-007
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-009: UploadConfirmService -- 업로드 완료 확인

- **작업명**: 업로드 완료 확인 서비스 구현 (S3 HEAD 검증 포함)
- **설명**: 업로드 완료 확인 서비스를 구현한다. objectKey로 DB 조회, 소유권 검증 (요청자 == uploaderUserId), 상태 검증 (REQUESTED만 허용, COMPLETED면 멱등 성공 반환, FAILED/EXPIRED면 거부), REQUESTED -> CONFIRMING 전이, S3 HEAD 요청으로 파일 존재 여부/Content-Type/Content-Length 일치 검증, 성공 시 CONFIRMING -> COMPLETED (completedAt 기록), 실패 시 CONFIRMING -> FAILED (사유 기록), 로깅 처리를 포함한다.
- **관련 검증 기준**: STOR-INV-05 (업로드 완료 확인 -- HEAD 검증), STOR-INV-07 (Content-Type 일치), SEC-STOR-01 (소유권 검증), 2-3 상태 전이 (REQUESTED->CONFIRMING->COMPLETED/FAILED), 4-3 멱등성, 7-1 로그
- **관련 테스트 케이스**: TC-011~012 (HEAD 성공/실패), TC-020~023 (상태 전이), TC-025~027 (금지된 전이), TC-028 (멱등), TC-029 (소유권), TC-053 (S3 HEAD 장애), TC-056 (멱등 재호출), TC-059 (로그), TC-062 (completedAt)
- **선행 작업**: TASK-001, TASK-004, TASK-006, TASK-007
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-010: DownloadUrlService -- 다운로드용 Presigned URL 생성

- **작업명**: 다운로드용 Presigned URL 생성 서비스 구현
- **설명**: 다운로드 URL 생성 서비스를 구현한다. objectKey로 DB 조회 (COMPLETED 상태, 미삭제), 미존재 시 404, S3 Presigner를 통한 GET URL 생성 (만료 1시간), 로깅 처리를 포함한다.
- **관련 검증 기준**: STOR-INV-03 (1시간 만료), SEC-STOR-03 (미존재 Key 404), SEC-STOR-04 (미인증 401), 3-4 다운로드 API 흐름, 7-1 로그
- **관련 테스트 케이스**: TC-008 (1시간 만료), TC-031 (404), TC-032 (401), TC-052 (S3 장애)
- **선행 작업**: TASK-001, TASK-004, TASK-006, TASK-007
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 5. 백엔드 서비스 계층 -- 삭제 및 스케줄러

#### TASK-011: FileDeleteService -- 파일 삭제

- **작업명**: 파일 삭제 서비스 구현 (S3 삭제 -> DB Soft Delete)
- **설명**: 파일 삭제 서비스를 구현한다. OPERATOR 이상 권한 검증, 참조 무결성 체크 (상위 엔티티에서 참조 중이면 409 Conflict), S3 객체 삭제를 먼저 수행하고 성공 후 DB Soft Delete (순서 보장 필수), S3 삭제 실패 시 500 반환 및 DB 롤백, 로깅 처리를 포함한다. 참조 무결성 체크를 위한 확장 포인트(전략 패턴 또는 이벤트)를 설계한다.
- **관련 검증 기준**: STOR-INV-09 (파일 삭제 정책 -- 순서, 롤백, 참조 무결성), 6-1 RBAC (OPERATOR 이상만 삭제), 7-1 로그
- **관련 테스트 케이스**: TC-017 (삭제 성공), TC-018 (S3 실패 시 롤백), TC-035 (권한 부족 403), TC-036 (참조 중 409)
- **선행 작업**: TASK-001, TASK-004, TASK-006, TASK-007
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-012: FileExpirationScheduler -- 고아 파일 만료 처리

- **작업명**: REQUESTED 상태 24시간 경과 메타데이터 EXPIRED 전환 스케줄러 구현
- **설명**: `@Scheduled`를 사용하여 매 1시간마다 실행되는 스케줄러를 구현한다. `status=REQUESTED AND createdAt < NOW() - 24시간`인 레코드를 조회하여 EXPIRED로 전환한다. 대량 처리 시 페이징 또는 배치 업데이트를 고려한다.
- **관련 검증 기준**: STOR-INV-08 (고아 파일 방지 -- 스케줄러 EXPIRED 전환)
- **관련 테스트 케이스**: TC-015 (24시간 경과 EXPIRED), TC-016 (24시간 미경과 유지), TC-024 (REQUESTED->EXPIRED 전이)
- **선행 작업**: TASK-004, TASK-006
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 6. 백엔드 API 계층

#### TASK-013: StorageController -- API 엔드포인트 구현

- **작업명**: Storage REST API 컨트롤러 구현
- **설명**: `igrus.web.storage.controller` 패키지에 다음 4개 엔드포인트를 구현한다. Swagger/OpenAPI 어노테이션을 포함하며, Orval 호환성을 보장한다. 모든 엔드포인트는 인증 필수 (`@SecurityRequirement`).
  1. `POST /api/v1/storage/presigned-url` -- 업로드용 Presigned URL 요청
  2. `POST /api/v1/storage/confirm` -- 업로드 완료 확인
  3. `GET /api/v1/storage/download-url` -- 다운로드용 Presigned URL 요청
  4. `DELETE /api/v1/storage/{objectKey}` -- 파일 삭제 (OPERATOR 이상)
- **관련 검증 기준**: 3-3 API 흐름 (업로드), 3-4 API 흐름 (다운로드), STOR-INV-06 (인증 필수)
- **관련 테스트 케이스**: TC-013 (401), TC-030 (401), TC-032 (401) 등 Controller 레벨 TC 전체
- **선행 작업**: TASK-008, TASK-009, TASK-010, TASK-011
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-014: 요청/응답 DTO 구현

- **작업명**: Storage API 요청/응답 DTO 구현
- **설명**: 다음 DTO를 `igrus.web.storage.dto` 패키지에 구현한다. Bean Validation 어노테이션을 포함한다.
  - `CreatePresignedUrlRequest`: `fileName` (@NotBlank, @Size(max=255)), `contentType` (@NotNull, allowlist 커스텀 검증), `fileSize` (@NotNull, @Min(1), @Max(10485760)), `purpose` (사용처: posts/profiles/events)
  - `CreatePresignedUrlResponse`: `presignedUrl`, `objectKey`
  - `ConfirmUploadRequest`: `objectKey` (@NotBlank)
  - `ConfirmUploadResponse`: `status`, `objectKey`, `reason` (실패 시)
  - `DownloadUrlResponse`: `presignedUrl`
- **관련 검증 기준**: STOR-INV-01 (크기 검증 어노테이션), STOR-INV-02 (타입 검증), 5-1 입력 도메인 분할
- **관련 테스트 케이스**: TC-001~006 (입력 검증), TC-037~050 (경계값 전체)
- **선행 작업**: TASK-007
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-015: Spring Security 경로 설정 -- Storage API

- **작업명**: Spring Security에 Storage API 경로 인증/인가 규칙 추가
- **설명**: 기존 `SecurityPaths` 및 Security 설정 클래스에 Storage API 경로를 추가한다. 모든 Storage 엔드포인트는 인증 필수 (ASSOCIATE 이상). 삭제 엔드포인트는 OPERATOR 이상으로 제한한다.
- **관련 검증 기준**: STOR-INV-06 (미인증 차단), 6-1 RBAC 매트릭스, SEC-STOR-02 (미인증 업로드 401), SEC-STOR-04 (미인증 다운로드 401)
- **관련 테스트 케이스**: TC-013 (401), TC-030 (401), TC-032 (401), TC-035 (403)
- **선행 작업**: TASK-013
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 7. 백엔드 테스트

#### TASK-016: 단위 테스트 -- 도메인 로직

- **작업명**: ObjectKeyGenerator, FileMetadata 상태 전이, 입력 검증 단위 테스트 작성
- **설명**: 다음 단위 테스트를 작성한다.
  - ObjectKeyGenerator: 유일성 (10회 호출 결과 중복 없음), 형식 규약 정규표현식 검증, Content-Type별 확장자 매핑
  - FileMetadata 상태 전이: 정상 전이 전체, 금지된 전이 시 예외 발생
  - DTO 검증: 파일 크기 경계값(0, 1, 10MB, 10MB+1), Content-Type allowlist/blocklist
- **관련 검증 기준**: STOR-INV-01, STOR-INV-02, STOR-INV-04, 2-3 상태 전이
- **관련 테스트 케이스**: TC-003, TC-009, TC-010 (단위), TC-025~027 (금지된 전이)
- **선행 작업**: TASK-004, TASK-005, TASK-014
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-017: 서비스 통합 테스트 -- Presigned URL 생성

- **작업명**: PresignedUrlService 통합 테스트 작성 (S3Client Mock)
- **설명**: S3Client/S3Presigner를 Mockito Mock으로 대체하여 다음을 검증하는 통합 테스트를 작성한다.
  - 업로드 URL 생성 시 만료 시간 5분 설정 검증
  - Content-Type 조건 포함 검증
  - Content-Length 상한 조건 포함 검증
  - HTTPS URL 생성 검증
  - 유효 입력 시 DB 메타데이터 REQUESTED 상태 저장 확인
  - 감사 필드 (objectKey, uploaderUserId, originalFileName, contentType, fileSize, createdAt) 전부 non-null 저장 확인
  - 로그에 userId/contentType/fileSize 포함, Presigned URL 미포함 확인
  - S3 SDK 장애 시 500 에러 및 에러 로깅 확인
- **관련 검증 기준**: STOR-INV-01~04, STOR-INV-07, 4-1 S3 장애, 6-3 URL 보안, 7-1/7-3 로그
- **관련 테스트 케이스**: TC-007, TC-014, TC-019, TC-033, TC-034, TC-051, TC-058, TC-060, TC-061
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-018: 서비스 통합 테스트 -- 업로드 완료 확인

- **작업명**: UploadConfirmService 통합 테스트 작성 (S3Client Mock)
- **설명**: S3Client Mock을 활용하여 다음을 검증하는 통합 테스트를 작성한다.
  - HEAD 성공 시 REQUESTED -> CONFIRMING -> COMPLETED 전이 및 completedAt 기록
  - HEAD 실패 (404) 시 CONFIRMING -> FAILED 전이 및 사유 기록
  - HEAD 성공이나 Content-Type 불일치 시 FAILED 전이
  - HEAD 성공이나 Content-Length 불일치 시 FAILED 전이
  - COMPLETED 상태에서 재호출 시 멱등 성공 (HEAD 생략)
  - FAILED/EXPIRED 상태에서 호출 시 거부
  - 다른 사용자의 objectKey로 호출 시 403
  - 존재하지 않는 objectKey로 호출 시 에러
  - S3 HEAD 중 장애 시 FAILED 전이
  - 연속 3회 멱등 호출 성공
  - 로그 검증
- **관련 검증 기준**: STOR-INV-05, STOR-INV-07, SEC-STOR-01, 2-3 상태 전이 전체, 4-3 멱등성
- **관련 테스트 케이스**: TC-011~012, TC-020~028, TC-029, TC-050, TC-053, TC-056, TC-059, TC-062
- **선행 작업**: TASK-009
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-019: 서비스 통합 테스트 -- 다운로드 URL 생성

- **작업명**: DownloadUrlService 통합 테스트 작성 (S3Client Mock)
- **설명**: 다음을 검증하는 통합 테스트를 작성한다.
  - 다운로드 URL 생성 시 만료 시간 1시간 설정 검증
  - 존재하지 않는 objectKey 시 404
  - S3 SDK 장애 시 500 에러
- **관련 검증 기준**: STOR-INV-03, SEC-STOR-03, 4-1 S3 장애
- **관련 테스트 케이스**: TC-008, TC-031, TC-052
- **선행 작업**: TASK-010
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-020: 서비스 통합 테스트 -- 파일 삭제

- **작업명**: FileDeleteService 통합 테스트 작성 (S3Client Mock)
- **설명**: 다음을 검증하는 통합 테스트를 작성한다.
  - S3 삭제 성공 후 DB Soft Delete 확인
  - S3 삭제 실패 시 DB 미변경 및 500 응답
  - 참조 중인 파일 삭제 시도 시 409 응답
- **관련 검증 기준**: STOR-INV-09
- **관련 테스트 케이스**: TC-017, TC-018, TC-036
- **선행 작업**: TASK-011
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-021: 서비스 통합 테스트 -- 스케줄러

- **작업명**: FileExpirationScheduler 통합 테스트 작성
- **설명**: 다음을 검증하는 통합 테스트를 작성한다.
  - REQUESTED 상태 24시간 경과 레코드 -> EXPIRED 전환
  - REQUESTED 상태 24시간 미경과 레코드 -> 유지 (변경 없음)
  - COMPLETED/FAILED 상태 레코드는 스케줄러 대상에서 제외
- **관련 검증 기준**: STOR-INV-08
- **관련 테스트 케이스**: TC-015, TC-016, TC-024
- **선행 작업**: TASK-012
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-022: Controller 통합 테스트 -- 인증/인가 및 입력 검증

- **작업명**: StorageController MockMvc 통합 테스트 작성
- **설명**: MockMvc를 사용하여 다음을 검증하는 Controller 통합 테스트를 작성한다.
  - JWT 없이 업로드 URL 요청 시 401
  - JWT 없이 다운로드 URL 요청 시 401
  - JWT 없이 완료 알림 시 401
  - ASSOCIATE/MEMBER 권한으로 삭제 시 403
  - 파일 크기 경계값: 0B (400), 1B (200), 10MB (200), 10MB+1B (400), 음수 (400), null (400), 50MB (400)
  - 파일명 경계값: 1자 (200), 255자 (200), 256자 (400), 빈 문자열 (400), null (400), 한글 (200), 특수문자 (200), 공백만 (400)
  - Content-Type: 허용 4종 각각 (200), 금지 5종 각각 (400), null (400)
  - objectKey: 빈 문자열 (400)
  - 소형 파일 1KB (200)
- **관련 검증 기준**: STOR-INV-01, STOR-INV-02, STOR-INV-06, SEC-STOR-01~04, 5-1~5-4 입력 도메인
- **관련 테스트 케이스**: TC-001~006, TC-013, TC-030, TC-032, TC-035, TC-037~050
- **선행 작업**: TASK-013, TASK-015
- **구현 범위**: backend
- **예상 난이도**: 상

---

### 8. 프론트엔드 구현

#### TASK-023: Orval API 클라이언트 생성 -- Storage API

- **작업명**: Orval을 통한 Storage API 클라이언트 모델 및 훅 자동 생성
- **설명**: 백엔드 Swagger 문서 기반으로 Orval을 실행하여 Storage API의 TypeScript 모델과 TanStack Query 훅을 자동 생성한다. 생성되는 항목: `CreatePresignedUrlRequest`, `CreatePresignedUrlResponse`, `ConfirmUploadRequest`, `ConfirmUploadResponse`, `DownloadUrlResponse` 및 관련 API 함수/훅.
- **관련 검증 기준**: 3-2 시스템 경계 (Frontend 책임)
- **관련 테스트 케이스**: 없음 (자동 생성)
- **선행 작업**: TASK-013 (백엔드 API 완성 후)
- **구현 범위**: frontend
- **예상 난이도**: 하

#### TASK-024: useImageUpload 커스텀 훅 구현

- **작업명**: 이미지 업로드 전체 흐름을 관리하는 커스텀 훅 구현
- **설명**: `useImageUpload` 커스텀 훅을 구현한다. 다음 기능을 포함한다.
  1. 클라이언트 사전 검증: 파일 크기(최대 10MB), Content-Type(image/jpeg|png|gif|webp) 검증
  2. 백엔드에 Presigned URL 요청 (`POST /api/v1/storage/presigned-url`)
  3. S3로 직접 PUT 업로드 (XMLHttpRequest 또는 fetch, 진행률 추적)
  4. 업로드 완료 후 백엔드에 완료 알림 (`POST /api/v1/storage/confirm`)
  5. 상태 관리: IDLE -> VALIDATING -> REQUESTING_URL -> UPLOADING (진행률) -> CONFIRMING -> COMPLETED / FAILED
  6. 에러 처리: S3 PUT 실패 시 재시도 (지수 백오프, 최대 3회), 403 (만료) 시 새 URL 재요청
  7. 완료 알림 실패 시 재시도 (URL 유효 기간 내)
- **관련 검증 기준**: STOR-INV-01 (클라이언트 검증), STOR-INV-02 (클라이언트 검증), 2-1 상태 모델 (UPLOADING은 Frontend UI 전용), 3-2 Frontend 책임 전체, 4-1 재시도 (지수 백오프), 4-2 만료 감지, 4-3 네트워크 단절
- **관련 테스트 케이스**: TC-054 (만료 URL 403), TC-057 (Content-Type 불일치 403)
- **선행 작업**: TASK-023
- **구현 범위**: frontend
- **예상 난이도**: 상

#### TASK-025: useImageDownload 커스텀 훅 구현

- **작업명**: 이미지 다운로드 URL 관리 커스텀 훅 구현
- **설명**: `useImageDownload` 커스텀 훅을 구현한다. objectKey를 받아 다운로드 Presigned URL을 요청하고 캐싱한다. URL 만료 (1시간) 시 자동 재요청 로직을 포함한다. S3 GET 403 (만료) 감지 시 새 URL 자동 요청. 실패 시 이미지 플레이스홀더 표시.
- **관련 검증 기준**: 3-4 다운로드 API 흐름, 4-1 S3 GET 실패 정책, 4-2 다운로드 URL 만료 정책
- **관련 테스트 케이스**: TC-055 (만료 다운로드 URL 403)
- **선행 작업**: TASK-023
- **구현 범위**: frontend
- **예상 난이도**: 중

#### TASK-026: ImageUploader 공통 컴포넌트 구현

- **작업명**: 이미지 업로드 UI 공통 컴포넌트 구현
- **설명**: 재사용 가능한 `ImageUploader` 컴포넌트를 구현한다. 파일 선택 (`accept="image/jpeg,image/png,image/gif,image/webp"`), 파일 드래그 앤 드롭 지원, 업로드 진행률 표시 (프로그레스 바), 업로드 성공/실패 상태 UI, 실패 시 재시도 버튼, 이미지 미리보기 기능을 포함한다. `useImageUpload` 훅과 연동한다.
- **관련 검증 기준**: STOR-INV-02 (파일 선택 시 accept 속성), 3-2 Frontend 책임 (파일 선택 UI, 진행률 표시), 4-1 재시도 UI
- **관련 테스트 케이스**: 없음 (UI 컴포넌트)
- **선행 작업**: TASK-024
- **구현 범위**: frontend
- **예상 난이도**: 중

---

### 9. E2E 테스트 (선택)

#### TASK-027: E2E 테스트 -- 전체 업로드/다운로드 흐름

- **작업명**: LocalStack 기반 E2E 테스트 (선택사항)
- **설명**: LocalStack 또는 실제 S3 테스트 버킷을 사용하여 전체 업로드/다운로드 흐름을 E2E로 검증한다. 만료된 Presigned URL 사용 시 S3 403 응답 확인, Content-Type 불일치 시 S3 403 응답 확인을 포함한다. Docker 및 LocalStack 설정이 필요하므로 선택적으로 진행한다.
- **관련 검증 기준**: STOR-INV-03 (URL 만료), STOR-INV-07 (Content-Type 일치), 8-1 E2E 테스트 레벨
- **관련 테스트 케이스**: TC-054 (만료 업로드 URL 403), TC-055 (만료 다운로드 URL 403), TC-057 (Content-Type 불일치 403)
- **선행 작업**: TASK-001, TASK-008~010, TASK-013
- **구현 범위**: both
- **예상 난이도**: 상

---

## 작업 순서 및 의존성

### 의존성 그래프

```
TASK-001 (S3 SDK 설정) ──────────┐
                                  │
TASK-002 (S3 버킷 CORS/Lifecycle) │  (인프라, 병렬 가능)
                                  │
TASK-003 (Flyway 마이그레이션) ───┤
                                  │
TASK-005 (ObjectKeyGenerator) ────┤
                                  │
TASK-007 (ErrorCode/예외) ────────┤
                                  │
              ┌───────────────────┘
              │
              ▼
TASK-004 (FileMetadata 엔티티) ──> TASK-006 (Repository)
              │                         │
              │                         ▼
              │                  TASK-012 (스케줄러) ──> TASK-021 (스케줄러 테스트)
              │
              ▼
TASK-014 (DTO) ──> TASK-016 (단위 테스트)
              │
              ▼
TASK-008 (URL 생성 서비스) ──────> TASK-017 (URL 생성 테스트)
              │
TASK-009 (완료 확인 서비스) ─────> TASK-018 (완료 확인 테스트)
              │
TASK-010 (다운로드 서비스) ──────> TASK-019 (다운로드 테스트)
              │
TASK-011 (삭제 서비스) ──────────> TASK-020 (삭제 테스트)
              │
              ▼
TASK-013 (Controller) ──> TASK-015 (Security) ──> TASK-022 (Controller 테스트)
              │
              ▼
TASK-023 (Orval 생성) ──> TASK-024 (useImageUpload) ──> TASK-026 (ImageUploader 컴포넌트)
                     └──> TASK-025 (useImageDownload)
                                    │
                                    ▼
                            TASK-027 (E2E 테스트, 선택)
```

### 권장 실행 순서

**Phase 1 -- 인프라 및 기반 (병렬 가능)**
1. TASK-001: S3 SDK 의존성 및 설정
2. TASK-002: S3 버킷 CORS/Lifecycle 문서화
3. TASK-003: Flyway 마이그레이션
4. TASK-005: ObjectKeyGenerator
5. TASK-007: ErrorCode 및 커스텀 예외

**Phase 2 -- 도메인 계층**
6. TASK-004: FileMetadata 엔티티 + FileUploadStatus enum
7. TASK-006: FileMetadataRepository
8. TASK-014: 요청/응답 DTO

**Phase 3 -- 서비스 계층 (병렬 가능)**
9. TASK-008: 업로드용 Presigned URL 서비스
10. TASK-009: 업로드 완료 확인 서비스
11. TASK-010: 다운로드 URL 서비스
12. TASK-011: 파일 삭제 서비스
13. TASK-012: 만료 스케줄러

**Phase 4 -- API 계층**
14. TASK-013: StorageController
15. TASK-015: Spring Security 경로 설정

**Phase 5 -- 백엔드 테스트 (병렬 가능)**
16. TASK-016: 단위 테스트
17. TASK-017: Presigned URL 생성 통합 테스트
18. TASK-018: 업로드 완료 확인 통합 테스트
19. TASK-019: 다운로드 URL 통합 테스트
20. TASK-020: 파일 삭제 통합 테스트
21. TASK-021: 스케줄러 통합 테스트
22. TASK-022: Controller 통합 테스트

**Phase 6 -- 프론트엔드 (백엔드 API 완성 후)**
23. TASK-023: Orval API 클라이언트 생성
24. TASK-024: useImageUpload 훅
25. TASK-025: useImageDownload 훅
26. TASK-026: ImageUploader 컴포넌트

**Phase 7 -- E2E (선택)**
27. TASK-027: E2E 테스트

---

## 구현 시 주의사항

### 기술적 고려사항

1. **AWS S3 SDK 선택**: `spring-cloud-aws-starter-s3`(io.awspring.cloud)를 사용할지 순수 `software.amazon.awssdk:s3`를 사용할지 결정이 필요하다. 이미 `spring-cloud-aws-starter-secrets-manager`가 build.gradle에 포함되어 있으므로 `spring-cloud-aws-starter-s3`를 사용하는 것이 일관성 면에서 유리하다.

2. **S3 Presigner와 S3Client 분리**: Presigned URL 생성에는 `S3Presigner`, HEAD/DELETE 요청에는 `S3Client`가 각각 필요하다. 두 Bean을 모두 등록해야 한다.

3. **시간 클래스**: 프로젝트 규칙에 따라 모든 시간 필드는 `Instant`로 통일한다. `completedAt`, `createdAt` 등 모두 `Instant` 사용.

4. **Soft Delete 패턴**: 기존 `SoftDeletableEntity`를 상속하여 일관된 Soft Delete 패턴을 유지한다. `deleted`, `deletedAt`, `deletedBy` 필드가 자동으로 포함된다.

5. **Flyway 마이그레이션 버전**: 현재 최신이 V40이므로 V41 이상 번호를 사용한다. 커밋 전에 다른 브랜치의 마이그레이션과 버전 충돌이 없는지 반드시 확인한다.

6. **트랜잭션 관리 (TASK-011 파일 삭제)**: S3 삭제 -> DB Soft Delete 순서에서, S3 삭제는 트랜잭션 외부에서 수행하고 성공 후 트랜잭션 내에서 DB 업데이트를 수행하거나, `@Transactional` 내부에서 S3 삭제 후 예외 발생 시 롤백되도록 구현한다. S3 삭제가 외부 호출이므로 트랜잭션 경계 설계에 신중해야 한다.

7. **Presigned URL 로깅 금지**: 검증 기준서 7-3에 따라 Presigned URL 자체 (X-Amz-Signature, X-Amz-Credential 포함)를 절대 로그에 기록하지 않아야 한다. Object Key, userId, contentType, fileSize만 로깅한다.

8. **Content-Type allowlist 검증**: Bean Validation 커스텀 어노테이션 또는 서비스 레이어에서 allowlist 검증을 구현한다. 허용: `image/jpeg`, `image/png`, `image/gif`, `image/webp`.

### 잠재적 위험 요소

1. **S3 SDK 버전 호환성**: Spring Boot 3.5.9와 AWS SDK v2의 호환성을 확인해야 한다. `spring-cloud-aws`의 버전이 Spring Boot 버전과 맞는지 검증 필요.

2. **Presigned URL Content-Length 조건**: AWS S3 Presigned PUT URL에서 Content-Length 상한을 조건으로 설정하는 방식이 SDK 버전에 따라 다를 수 있다. `PutObjectPresignRequest`의 `contentLengthRange` 또는 Policy 조건 활용 여부를 사전 조사해야 한다.

3. **참조 무결성 체크 확장성**: 현재는 게시글 이미지(PostImage)만 참조하지만, 향후 프로필/행사 이미지도 참조할 수 있다. 확장 가능한 참조 체크 메커니즘(전략 패턴 또는 Spring의 `List<FileReferenceChecker>` 주입)을 설계해야 한다.

4. **스케줄러 동시 실행**: 다중 인스턴스 환경에서 스케줄러가 동시에 실행될 수 있다. 현재 단일 인스턴스 기준으로 구현하되, 향후 분산 락(ShedLock 등)이 필요할 수 있음을 인지한다.

5. **프론트엔드 S3 CORS**: S3 버킷의 CORS 설정이 프론트엔드 도메인(localhost 포함 개발 환경)을 정확히 허용해야 한다. 개발/스테이징/운영 환경별로 다른 도메인을 설정해야 한다.

### 기존 코드와의 통합 포인트

1. **패키지 구조**: `igrus.web.storage` 패키지를 새로 생성한다. 기존 패키지 네이밍 컨벤션(`igrus.web.{도메인}`)을 따른다.

2. **BaseEntity/SoftDeletableEntity**: `igrus.web.common.domain` 패키지의 기존 엔티티 기본 클래스를 상속한다.

3. **CustomBaseException/ErrorCode**: `igrus.web.common.exception` 패키지의 기존 예외 체계를 따른다.

4. **GlobalExceptionHandler**: 새로운 Storage 예외가 기존 `GlobalExceptionHandler`에서 처리되는지 확인한다. 필요 시 핸들러를 추가한다.

5. **Spring Security**: 기존 `SecurityPaths`, `ApiSecurityConfig`에 Storage API 경로를 추가한다.

6. **PostImage 엔티티**: 기존 `igrus.web.community.post.domain.PostImage`가 이미지 URL을 관리하고 있다. 향후 이 엔티티가 `FileMetadata`의 `objectKey`를 참조하도록 통합해야 할 수 있다 (이 작업 계획의 범위 외).

7. **Swagger/Orval**: 새 API의 Swagger 어노테이션이 기존 `SwaggerConfig`와 호환되어야 하며, Orval 자동 생성에 문제가 없어야 한다. 백엔드 CLAUDE.md의 `@ApiResponse` 규칙을 반드시 준수한다.

---

## 완료 기준

### 검증 기준 충족 체크리스트

| 검증 기준 | 매핑 작업 | 충족 여부 |
|----------|----------|:---:|
| STOR-INV-01 (파일 크기 제한) | TASK-008, TASK-014, TASK-016, TASK-017, TASK-022 | [ ] |
| STOR-INV-02 (허용 파일 타입) | TASK-008, TASK-014, TASK-016, TASK-022 | [ ] |
| STOR-INV-03 (URL 만료 시간) | TASK-008, TASK-010, TASK-017, TASK-019, TASK-027 | [ ] |
| STOR-INV-04 (Object Key 유일성) | TASK-005, TASK-008, TASK-016, TASK-017 | [ ] |
| STOR-INV-05 (업로드 완료 확인) | TASK-009, TASK-018 | [ ] |
| STOR-INV-06 (미인증 차단) | TASK-015, TASK-022 | [ ] |
| STOR-INV-07 (Content-Type 일치) | TASK-008, TASK-009, TASK-017, TASK-018, TASK-027 | [ ] |
| STOR-INV-08 (고아 파일 방지) | TASK-002, TASK-012, TASK-021 | [ ] |
| STOR-INV-09 (파일 삭제 정책) | TASK-011, TASK-020 | [ ] |
| 상태 모델 (2-1~2-3) | TASK-004, TASK-009, TASK-016, TASK-018 | [ ] |
| 시스템 경계 (3-1~3-4) | TASK-008~011, TASK-013, TASK-024~026 | [ ] |
| 외부 의존성 실패 (4-1~4-3) | TASK-008~010, TASK-017~019, TASK-024 | [ ] |
| 입력 도메인 (5-1~5-4) | TASK-014, TASK-016, TASK-022 | [ ] |
| SEC-STOR-01 (소유권 검증) | TASK-009, TASK-018 | [ ] |
| SEC-STOR-02 (미인증 업로드) | TASK-015, TASK-022 | [ ] |
| SEC-STOR-03 (미존재 Key 다운로드) | TASK-010, TASK-019 | [ ] |
| SEC-STOR-04 (미인증 다운로드) | TASK-015, TASK-022 | [ ] |
| URL 보안 (6-3) | TASK-001, TASK-002, TASK-008, TASK-017 | [ ] |
| 관측 가능성 (7-1~7-3) | TASK-008~011, TASK-017~018 | [ ] |

### 테스트 케이스 통과 체크리스트

| 카테고리 | TC 번호 | 매핑 작업 | 통과 여부 |
|---------|--------|----------|:---:|
| **도메인 규칙** | TC-001 | TASK-022 | [ ] |
| | TC-002 | TASK-022 | [ ] |
| | TC-003 | TASK-016 | [ ] |
| | TC-004 | TASK-022 | [ ] |
| | TC-005 | TASK-022 | [ ] |
| | TC-006 | TASK-022 | [ ] |
| | TC-007 | TASK-017 | [ ] |
| | TC-008 | TASK-019 | [ ] |
| | TC-009 | TASK-016 | [ ] |
| | TC-010 | TASK-016 | [ ] |
| | TC-011 | TASK-018 | [ ] |
| | TC-012 | TASK-018 | [ ] |
| | TC-013 | TASK-022 | [ ] |
| | TC-014 | TASK-017 | [ ] |
| | TC-015 | TASK-021 | [ ] |
| | TC-016 | TASK-021 | [ ] |
| | TC-017 | TASK-020 | [ ] |
| | TC-018 | TASK-020 | [ ] |
| **상태 모델** | TC-019 | TASK-017 | [ ] |
| | TC-020 | TASK-018 | [ ] |
| | TC-021 | TASK-018 | [ ] |
| | TC-022 | TASK-018 | [ ] |
| | TC-023 | TASK-018 | [ ] |
| | TC-024 | TASK-021 | [ ] |
| | TC-025 | TASK-018 | [ ] |
| | TC-026 | TASK-018 | [ ] |
| | TC-027 | TASK-018 | [ ] |
| | TC-028 | TASK-018 | [ ] |
| **보안 정책** | TC-029 | TASK-018, TASK-022 | [ ] |
| | TC-030 | TASK-022 | [ ] |
| | TC-031 | TASK-019, TASK-022 | [ ] |
| | TC-032 | TASK-022 | [ ] |
| | TC-033 | TASK-017 | [ ] |
| | TC-034 | TASK-017 | [ ] |
| | TC-035 | TASK-022 | [ ] |
| | TC-036 | TASK-020 | [ ] |
| **입력 경계값** | TC-037 | TASK-022 | [ ] |
| | TC-038 | TASK-022 | [ ] |
| | TC-039 | TASK-022 | [ ] |
| | TC-040 | TASK-022 | [ ] |
| | TC-041 | TASK-022 | [ ] |
| | TC-042 | TASK-022 | [ ] |
| | TC-043 | TASK-022 | [ ] |
| | TC-044 | TASK-022 | [ ] |
| | TC-045 | TASK-022 | [ ] |
| | TC-046 | TASK-022 | [ ] |
| | TC-047 | TASK-022 | [ ] |
| | TC-048 | TASK-022 | [ ] |
| | TC-049 | TASK-022 | [ ] |
| | TC-050 | TASK-022 | [ ] |
| **외부 의존성** | TC-051 | TASK-017 | [ ] |
| | TC-052 | TASK-019 | [ ] |
| | TC-053 | TASK-018 | [ ] |
| | TC-054 | TASK-027 | [ ] |
| | TC-055 | TASK-027 | [ ] |
| | TC-056 | TASK-018 | [ ] |
| | TC-057 | TASK-027 | [ ] |
| **관측 가능성** | TC-058 | TASK-017 | [ ] |
| | TC-059 | TASK-018 | [ ] |
| | TC-060 | TASK-017 | [ ] |
| | TC-061 | TASK-017 | [ ] |
| | TC-062 | TASK-018 | [ ] |

### 확인이 필요한 사항

1. **AWS S3 SDK 구체 라이브러리 선택**: `spring-cloud-aws-starter-s3` vs 순수 `software.amazon.awssdk:s3` -- 기존에 `spring-cloud-aws-starter-secrets-manager`를 사용 중이므로 전자가 유리하나, 팀 합의가 필요하다.
2. **Presigned PUT URL Content-Length 조건 설정 방법**: AWS SDK v2에서 Content-Length 범위 조건(content-length-range)을 Presigned PUT URL에 적용하는 구체적 방법을 사전 조사해야 한다. POST Policy 방식과 다를 수 있다.
3. **참조 무결성 체크 대상 엔티티**: 현재 `PostImage`만 대상인지, 다른 엔티티(프로필, 행사)도 포함하는지 확인이 필요하다.
4. **사용처(purpose) 파라미터 값 목록**: 현재 `posts`, `profiles`, `events` 3종으로 확정인지, 추가 사용처가 있는지 확인이 필요하다.
5. **프론트엔드 S3 직접 업로드 시 CORS 허용 도메인**: 개발환경(`localhost:5173`), 스테이징, 운영 도메인 목록 확정이 필요하다.
