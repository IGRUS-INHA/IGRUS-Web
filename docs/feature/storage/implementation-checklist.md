# S3 Presigned URL 이미지 업로드/다운로드 구현 체크리스트

## 메타데이터
| 항목 | 값 |
|------|---|
| 생성일 | 2026-02-26 |
| 최종 업데이트 | 2026-02-27 |
| 검증 기준 문서 | `c:\dev\IGRUS-Web\docs\criteria\storage\image-presigned-url-verification-criteria.md` |
| 테스트 케이스 문서 | `c:\dev\IGRUS-Web\docs\test-case\storage\image-presigned-url-test-cases.md` |
| 작업 계획 문서 | `c:\dev\IGRUS-Web\docs\feature\storage\image-presigned-url-task-plan.md` |
| 전체 상태 | IN_PROGRESS |

## 작업 진행 현황

### 그룹 1: Foundation Layer
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-001 | AWS S3 SDK 의존성 + S3Client/S3Presigner Bean 설정 | DONE | - | `backend/src/main/java/igrus/web/storage/config/S3Config.java` | S3 의존성은 build.gradle에 기존 존재, application.yml 프로퍼티도 기존 존재. S3Presigner Bean만 수동 등록 |
| TASK-003 | Flyway V45 - file_metadata 테이블 생성 | DONE | - | `backend/src/main/resources/db/migration/V45__create_file_metadata_table.sql` | V41~V44가 survey에 사용되어 V45로 생성 |
| TASK-005 | ObjectKeyGenerator 유틸리티 | DONE | - | `backend/src/main/java/igrus/web/storage/domain/ObjectKeyGenerator.java` | Clock Bean 주입, UUID v4 사용 |
| TASK-007 | StorageErrorCode + 커스텀 예외 클래스 | DONE | - | `backend/src/main/java/igrus/web/storage/exception/StorageErrorCode.java` 외 8개 예외 클래스 | CommunityErrorCode 패턴 따름 |
| TASK-004 | FileMetadata 엔티티 + FileUploadStatus enum | DONE | - | `backend/src/main/java/igrus/web/storage/domain/FileMetadata.java`, `backend/src/main/java/igrus/web/storage/domain/FileUploadStatus.java` | SoftDeletableEntity 상속, 상태 전이 도메인 메서드 포함 |
| TASK-006 | FileMetadataRepository | DONE | - | `backend/src/main/java/igrus/web/storage/repository/FileMetadataRepository.java` | JpaRepository 상속 |

**그룹 상태**: PASS
**리뷰 이력**: Round 1 code-reviewer FAIL (IllegalArgumentException, V41 주석 오류) → Round 2 수정 후 PASS

---

### 그룹 2: DTOs + All Services
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-014 | 요청/응답 DTO (records + Bean Validation) | DONE | - | `backend/src/main/java/igrus/web/storage/dto/CreatePresignedUrlRequest.java`, `CreatePresignedUrlResponse.java`, `ConfirmUploadRequest.java`, `ConfirmUploadResponse.java`, `DownloadUrlResponse.java` | Java record + Bean Validation |
| TASK-008 | PresignedUrlService - 업로드용 URL 생성 | DONE | - | `backend/src/main/java/igrus/web/storage/service/PresignedUrlService.java` | Content-Type allowlist 검증, 5분 만료, Content-Length 조건 포함, URL 로깅 금지 |
| TASK-009 | UploadConfirmService - 업로드 완료 확인 | DONE | - | `backend/src/main/java/igrus/web/storage/service/UploadConfirmService.java` | S3 HEAD 검증, 소유권 검증, COMPLETED 멱등 반환, FAILED/EXPIRED 거부 |
| TASK-010 | DownloadUrlService - 다운로드 URL 생성 | DONE | - | `backend/src/main/java/igrus/web/storage/service/DownloadUrlService.java` | COMPLETED 상태만 허용, 1시간 만료 |
| TASK-011 | FileDeleteService - 파일 삭제 | DONE | - | `backend/src/main/java/igrus/web/storage/service/FileDeleteService.java`, `backend/src/main/java/igrus/web/storage/service/FileReferenceChecker.java` | S3 삭제 -> DB Soft Delete 순서 보장, FileReferenceChecker 전략 패턴 |
| TASK-012 | FileExpirationScheduler - 고아 파일 만료 | DONE | - | `backend/src/main/java/igrus/web/storage/scheduler/FileExpirationScheduler.java`, `backend/src/main/java/igrus/web/storage/service/FileExpirationService.java` | SuspensionAutoLiftScheduler 패턴 참조, 매시 정각 실행 |

**그룹 상태**: PASS
**리뷰 이력**: Round 1 spec-reviewer PASS, code-reviewer FAIL (try-catch 범위, S3 에러 노출) → Round 2 수정 후 code-reviewer PASS

---

### 그룹 3: API Layer
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-013 | StorageController - 4개 엔드포인트 | DONE | PASS | `backend/src/main/java/igrus/web/storage/controller/StorageController.java` | DELETE를 @RequestParam으로 변경 (objectKey 슬래시 이슈 해결) |
| TASK-015 | Spring Security 경로 설정 | DONE | PASS | `backend/src/main/java/igrus/web/security/config/SecurityPaths.java`, `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java` | STORAGE_API 상수 추가, DELETE만 OPERATOR/ADMIN 제한 |

**그룹 상태**: PASS
**리뷰 이력**: Round 1 spec-reviewer FAIL (DELETE objectKey 슬래시 이슈) → Round 2 수정 후 spec-reviewer PASS, code-reviewer PASS

---

### 그룹 4: Unit + Service Integration Tests
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-016 | 단위 테스트 (ObjectKeyGenerator, FileMetadata, DTO) | DONE | - | `backend/src/test/java/igrus/web/storage/domain/ObjectKeyGeneratorTest.java`, `FileMetadataTest.java`, `backend/src/test/java/igrus/web/storage/dto/CreatePresignedUrlRequestTest.java`, `ConfirmUploadRequestTest.java` | TC-003, TC-009, TC-010 등 커버 |
| TASK-017 | PresignedUrlService 통합 테스트 | DONE | - | `backend/src/test/java/igrus/web/storage/service/PresignedUrlServiceIntegrationTest.java` | TC-007, TC-014, TC-019, TC-033, TC-034, TC-051, TC-058, TC-060, TC-061 |
| TASK-018 | UploadConfirmService 통합 테스트 | DONE | - | `backend/src/test/java/igrus/web/storage/service/UploadConfirmServiceIntegrationTest.java` | TC-011, TC-012, TC-020~TC-029, TC-053, TC-056, TC-059, TC-062 |
| TASK-019 | DownloadUrlService 통합 테스트 | DONE | - | `backend/src/test/java/igrus/web/storage/service/DownloadUrlServiceIntegrationTest.java` | TC-008, TC-031, TC-052 |
| TASK-020 | FileDeleteService 통합 테스트 | DONE | - | `backend/src/test/java/igrus/web/storage/service/FileDeleteServiceIntegrationTest.java` | TC-017, TC-018, TC-036. TestFileReferenceChecker 내부 @TestConfiguration 사용 |
| TASK-021 | Scheduler 통합 테스트 | DONE | - | `backend/src/test/java/igrus/web/storage/service/FileExpirationServiceIntegrationTest.java` | TC-015, TC-016, TC-024. 네이티브 SQL로 createdAt 오버라이드 |

**그룹 상태**: PASS
**리뷰 이력**: Round 1 spec-reviewer PASS, code-reviewer PASS

---

### 그룹 5: Controller Integration Tests
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-022 | Controller 통합 테스트 (인증/인가 + 경계값) | DONE | - | `backend/src/test/java/igrus/web/storage/integration/StorageControllerIntegrationTest.java` | TC-029(소유권 403), TC-031(미존재 ObjectKey 404) 추가 |

**그룹 상태**: PASS
**리뷰 이력**: Round 1 spec-reviewer FAIL (TC-003 누락) → TC-003 추가 + 미사용 필드 제거 → code-reviewer PASS

---

## 검증 기준 충족 현황
| ID | 설명 | 관련 TASK | 상태 |
|----|------|----------|:----:|
| STOR-INV-01 | 파일 크기 제한 (최대 10MB) | TASK-008, TASK-014, TASK-016, TASK-017, TASK-022 | ⬜ |
| STOR-INV-02 | 허용 파일 타입 제한 (jpeg/png/gif/webp) | TASK-008, TASK-014, TASK-016, TASK-022 | ⬜ |
| STOR-INV-03 | Presigned URL 만료 시간 (업로드 5분, 다운로드 1시간) | TASK-008, TASK-010, TASK-017, TASK-019 | ⬜ |
| STOR-INV-04 | Object Key 유일성 (UUID 기반) | TASK-005, TASK-008, TASK-016, TASK-017 | ⬜ |
| STOR-INV-05 | 업로드 완료 확인 (S3 HEAD 검증) | TASK-009, TASK-018 | ⬜ |
| STOR-INV-06 | 미인증 사용자 업로드 차단 | TASK-015, TASK-022 | ⬜ |
| STOR-INV-07 | Content-Type 일치 검증 | TASK-008, TASK-009, TASK-017, TASK-018 | ⬜ |
| STOR-INV-08 | 고아 파일 방지 (24시간 후 EXPIRED) | TASK-012, TASK-021 | ⬜ |
| STOR-INV-09 | 파일 삭제 정책 (S3 먼저 → DB Soft Delete) | TASK-011, TASK-020 | ⬜ |

## 테스트 케이스 통과 현황
| TC-ID | 설명 | 관련 TASK | 상태 |
|-------|------|----------|:----:|
| TC-001 | 최대 허용 크기(10MB) 업로드 URL 요청 성공 | TASK-022 | ✅ |
| TC-002 | 최대 허용 크기 초과(10MB+1B) 거부 | TASK-022 | ✅ |
| TC-003 | 최소 유효 크기(1B) 업로드 URL 요청 성공 | TASK-016 | ✅ |
| TC-004 | 파일 크기 0바이트 거부 | TASK-022 | ✅ |
| TC-005 | 허용된 4종 파일 타입 성공 | TASK-022 | ✅ |
| TC-006 | 금지된 파일 타입 거부 | TASK-022 | ✅ |
| TC-007 | 업로드용 URL 5분 만료 설정 검증 | TASK-017 | ✅ |
| TC-008 | 다운로드용 URL 1시간 만료 설정 검증 | TASK-019 | ✅ |
| TC-009 | 동일 파일명 다수 업로드 시 유일 Object Key | TASK-016 | ✅ |
| TC-010 | Object Key 형식 규약 검증 | TASK-016 | ✅ |
| TC-011 | S3 HEAD 성공 시 COMPLETED 전이 | TASK-018 | ✅ |
| TC-012 | 존재하지 않는 Object Key 완료 알림 실패 | TASK-018 | ✅ |
| TC-013 | JWT 없이 업로드 URL 요청 시 401 | TASK-022 | ✅ |
| TC-014 | Presigned URL Content-Type 조건 포함 검증 | TASK-017 | ✅ |
| TC-015 | REQUESTED 24시간 경과 → EXPIRED | TASK-021 | ✅ |
| TC-016 | REQUESTED 24시간 미경과 유지 | TASK-021 | ✅ |
| TC-017 | S3 삭제 성공 후 DB Soft Delete | TASK-020 | ✅ |
| TC-018 | S3 삭제 실패 시 DB 미변경 500 | TASK-020 | ✅ |
| TC-019 | → REQUESTED 전이 | TASK-017 | ✅ |
| TC-020 | REQUESTED → CONFIRMING → COMPLETED | TASK-018 | ✅ |
| TC-021 | CONFIRMING → FAILED (파일 미존재) | TASK-018 | ✅ |
| TC-022 | CONFIRMING → FAILED (Content-Type 불일치) | TASK-018 | ✅ |
| TC-023 | CONFIRMING → FAILED (Content-Length 불일치) | TASK-018 | ✅ |
| TC-024 | REQUESTED → EXPIRED (스케줄러) | TASK-021 | ✅ |
| TC-025 | COMPLETED → UPLOADING 전이 거부 | TASK-018 | ✅ |
| TC-026 | FAILED → COMPLETED 전이 거부 | TASK-018 | ✅ |
| TC-027 | EXPIRED → UPLOADING 전이 거부 | TASK-018 | ✅ |
| TC-028 | COMPLETED 상태 멱등 성공 | TASK-018 | ✅ |
| TC-029 | 다른 사용자 업로드 완료 알림 403 | TASK-018, TASK-022 | ✅ |
| TC-030 | 미인증 업로드 URL 요청 401 | TASK-022 | ✅ |
| TC-031 | 미존재 Object Key 다운로드 404 | TASK-019, TASK-022 | ✅ |
| TC-032 | 미인증 다운로드 URL 요청 401 | TASK-022 | ✅ |
| TC-033 | Presigned URL HTTPS 검증 | TASK-017 | ✅ |
| TC-034 | Content-Length 상한 조건 포함 검증 | TASK-017 | ✅ |
| TC-035 | ASSOCIATE/MEMBER 삭제 시 403 | TASK-022 | ✅ |
| TC-036 | 참조 중인 파일 삭제 409 | TASK-020 | ✅ |
| TC-037 | 파일 크기 음수(-1) 거부 | TASK-022 | ✅ |
| TC-038 | 파일 크기 null 거부 | TASK-022 | ✅ |
| TC-039 | 소형 파일(1KB) 성공 | TASK-022 | ✅ |
| TC-040 | 대형 파일(50MB) 거부 | TASK-022 | ✅ |
| TC-041 | 최소 길이 파일명(1자) 성공 | TASK-022 | ✅ |
| TC-042 | 최대 길이 파일명(255자) 성공 | TASK-022 | ✅ |
| TC-043 | 파일명 256자 이상 거부 | TASK-022 | ✅ |
| TC-044 | 빈 문자열 파일명 거부 | TASK-022 | ✅ |
| TC-045 | null 파일명 거부 | TASK-022 | ✅ |
| TC-046 | 한글 파일명 성공 | TASK-022 | ✅ |
| TC-047 | 특수문자 포함 파일명 성공 | TASK-022 | ✅ |
| TC-048 | 공백만 파일명 거부 | TASK-022 | ✅ |
| TC-049 | null Content-Type 거부 | TASK-022 | ✅ |
| TC-050 | 빈 objectKey 완료 알림 거부 | TASK-022 | ✅ |
| TC-051 | S3 SDK 장애 시 500 (업로드 URL) | TASK-017 | ✅ |
| TC-052 | S3 SDK 장애 시 500 (다운로드 URL) | TASK-019 | ✅ |
| TC-053 | S3 HEAD 장애 시 FAILED | TASK-018 | ✅ |
| TC-054 | 만료된 업로드 URL 403 (E2E) | - | ⬜ |
| TC-055 | 만료된 다운로드 URL 403 (E2E) | - | ⬜ |
| TC-056 | 완료 알림 멱등 3회 호출 | TASK-018 | ✅ |
| TC-057 | Content-Type 불일치 S3 403 (E2E) | - | ⬜ |
| TC-058 | 로그에 userId/contentType/fileSize 기록 | TASK-017 | ✅ |
| TC-059 | 완료 확인 로그에 objectKey/userId 기록 | TASK-018 | ✅ |
| TC-060 | Presigned URL 로그 미기록 검증 | TASK-017 | ✅ |
| TC-061 | 메타데이터 감사 필드 전부 저장 | TASK-017 | ✅ |
| TC-062 | completedAt 필드 기록 확인 | TASK-018 | ✅ |

## 이슈 로그
| # | 라운드 | 그룹 | 심각도 | 리뷰어 | 설명 | 해결 |
|---|--------|------|--------|--------|------|------|
