# 게시글/문의 S3 스토리지 연동 (Post & Inquiry S3 Integration) 검증 기준서

> **Status**: Draft
> **Last Updated**: 2026-03-05
> **Scope**: 게시글(Post) 이미지와 문의(Inquiry) 첨부파일의 S3 스토리지 연동 -- Object Key 유효성 검증, FileReferenceChecker 구현, 필드명 의미 정합성, API 응답 이미지 정보 포함
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)
> **관련 검증 기준서**:
> - [Presigned URL 이미지 업로드/다운로드 검증 기준서](./image-presigned-url-verification-criteria.md) -- S3 기본 기능 (STOR-INV-*)
> - [행사-이미지 연계 검증 기준서](../event/event-image-integration-verification-criteria.md) -- Event 도메인의 동일 패턴 선행 구현 (EVT-IMG-INV-*)
> - [문의 검증 기준서](../inquiry-verification-criteria.md) -- 문의 CRUD 및 상태 모델 (INQ-INV-*)

## 목적

이 문서는 게시글(Post) 도메인과 문의(Inquiry) 도메인에서 S3 스토리지와의 **연계 규칙**을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

S3 Presigned URL 기반 업로드/다운로드 자체의 검증은 `STOR-INV-*`에서, 행사 도메인의 S3 연계는 `EVT-IMG-INV-*`에서 각각 다루므로, 이 문서는 **Post와 Inquiry 두 도메인이 Storage 모듈과 만나는 접점**만 다룬다. 구체적으로:

1. Post 이미지 Object Key 유효성 검증 (FileMetadata COMPLETED 상태 확인)
2. Inquiry 첨부파일 Object Key 유효성 검증 (FileMetadata COMPLETED 상태 확인)
3. PostFileReferenceChecker 구현 (FileReferenceChecker 인터페이스)
4. InquiryFileReferenceChecker 구현 (FileReferenceChecker 인터페이스)
5. 필드명 의미 정합성 (imageUrl/fileUrl이 실제로는 objectKey를 저장하는 문제)
6. API 응답에서 이미지/첨부파일 다운로드 흐름

### 현재 상태 분석

**Post 도메인**:
- `PostImage.imageUrl` (VARCHAR 500)에 실제로는 S3 Object Key를 저장 중 `(현재 구현 일치)` -- `PostImage.java:47-48`
- `CreatePostRequest.imageUrls` / `UpdatePostRequest.imageUrls`: `List<String>`, 최대 5개 `(현재 구현 일치)` -- `CreatePostRequest.java:30-31`
- 프론트엔드가 `useImageUpload` 훅으로 S3에 업로드 후 Object Key를 전송 `(현재 구현 일치)`
- **누락**: PostFileReferenceChecker 미구현, Object Key 유효성 검증 없음

**Inquiry 도메인**:
- `InquiryAttachment.fileUrl` (VARCHAR 500)에 실제로는 S3 Object Key를 저장 중 `(현재 구현 일치)` -- `InquiryAttachment.java:32-33`
- `AttachmentInfo.fileUrl`: `@NotBlank` + URL 패턴 검증 `(현재 구현 -- URL 패턴 검증이 objectKey와 불일치)` -- `AttachmentInfo.java:20-24`
- `AttachmentInfo.fileName`, `AttachmentInfo.fileSize`: 클라이언트에서 전달하지만 FileMetadata에도 동일 정보 존재 `(현재 구현 일치)` -- `AttachmentInfo.java:27-33`
- **누락**: InquiryFileReferenceChecker 미구현, Object Key 유효성 검증 없음

QA Testing 용어 정리 wiki의 10개 영역 중, 이 연계 도메인에 직접 관련된 8개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | Object Key - FileMetadata 참조 정합성, 프리픽스 검증, 참조 무결성 |
| 2 | 상태 모델 | 이미지/첨부파일 연결 가능 조건 (FileMetadata 상태와 교차) |
| 3 | 시스템 경계와 책임 분리 | Post/Inquiry 모듈 ↔ Storage 모듈 간 의존 방향 및 책임 경계 |
| 4 | 입력 도메인 분할과 경계값 | Object Key 필드의 유효/무효 동치류, 이미지/첨부파일 개수 경계 |
| 5 | 권한/보안 정책 | 이미지/첨부파일 접근 권한, 소유권 정책 |
| 6 | 관측 가능성 | 참조 검증 성공/실패 로그, 참조 무결성 차단 로그 |
| 7 | 테스트 전략 | 테스트-검증 항목 매핑, 커버리지 현황 |
| 8 | GAP 분석 | 기존 구현의 수정 필요 사항 |
| 9 | DB 마이그레이션 및 API 스펙 변경 | 필드 리네이밍에 따른 스키마 변경 (구현 가이드) |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

시스템 전체에서 **항상 참이어야 하는 조건**이다. 어떤 코드 변경이든 이 조건을 깨뜨리면 시스템 무결성이 훼손된다.

### 1-1. Post 이미지 불변조건

#### POST-IMG-INV-01: 이미지 Object Key는 COMPLETED 상태 파일만 참조 가능

> 게시글에 연결되는 이미지의 Object Key는 `FileMetadata.status == COMPLETED`이고 `deleted == false`인 것만 허용한다.

- **사전조건**: 게시글 생성/수정 요청 시 `imageUrls` 리스트에 Object Key가 포함됨
- **사후조건**: 모든 Object Key에 대해 FileMetadata가 존재하고, `status == COMPLETED`이며, `deleted == false`
- **위반 시 예외**: `InvalidImageReferenceException` (400 Bad Request) -- 존재하지 않거나 상태가 부적절한 Object Key를 참조하려 할 때 발생. StorageErrorCode에 `INVALID_IMAGE_REFERENCE` 추가 필요.
- **관련 코드**:
  - `CreatePostService.addImages()` (L116-124) -- 현재는 Object Key를 검증 없이 저장 `(현재 구현 -- 검증 없음)`
  - `UpdatePostService.updatePost()` (L82-90) -- 현재는 Object Key를 검증 없이 저장 `(현재 구현 -- 검증 없음)`
  - Object Key 유효성 검증 로직 **(신규 구현 필요)**
- **검증 방법**:
  - COMPLETED 상태의 Object Key로 게시글 생성 시 성공 확인
  - REQUESTED / CONFIRMING / FAILED / EXPIRED 상태의 Object Key로 게시글 생성 시 거부 확인
  - 존재하지 않는 Object Key로 게시글 생성 시 거부 확인
  - Soft Delete된 FileMetadata의 Object Key로 게시글 생성 시 거부 확인 (`@SQLRestriction`에 의해 미조회)

#### POST-IMG-INV-02: Object Key `posts/` 프리픽스 강제 정책 (DECISION-01 확정: A안)

> 게시글에 연결되는 이미지의 Object Key는 `posts/` 프리픽스로 시작해야 한다. 게시글 서비스에서 프리픽스를 **강제 검증**한다.

- **사전조건**: 업로드 URL 생성 시 프론트엔드가 `purpose = "posts"` 지정 → ObjectKeyGenerator가 `posts/` 프리픽스 Key 생성
- **사후조건**: Object Key 형식이 `posts/{YYYY}/{MM}/{DD}/{UUID}.{ext}`
- **위반 시**: 다른 도메인(events, profiles, inquiries)의 이미지를 게시글에 부적절하게 연결하는 것을 방지. 400 Bad Request.
- **관련 코드**: `ObjectKeyGenerator.generate("posts", contentType)` `(현재 구현 일치)`, 게시글 서비스 프리픽스 검증 로직 **(신규 구현 필요)**
- **검증 방법**: `posts/` 프리픽스 Key 허용, `events/` 또는 `inquiries/` 프리픽스 Key 거부 (400 Bad Request)

#### POST-IMG-INV-03: 참조 무결성 -- 게시글이 참조 중인 이미지 삭제 차단

> 게시글의 `PostImage.imageUrl`(실제로는 objectKey)로 참조 중인 파일은 `FileDeleteService`에서 삭제를 거부한다.

- **사전조건**: FileDeleteService가 Object Key로 삭제 요청을 받음
- **사후조건**: PostFileReferenceChecker가 해당 Object Key를 참조하는 게시글이 존재하면 `FileReferenceExistsException` 발생 (`409 Conflict`)
- **위반 시**: 게시글 이미지가 깨진 이미지(broken image) 표시
- **관련 코드**:
  - `FileReferenceChecker` 인터페이스 (L10-18) `(현재 구현 일치)`
  - `FileDeleteService.checkFileReferences()` (L77-83) `(현재 구현 일치)` -- 모든 FileReferenceChecker 순회
  - `PostFileReferenceChecker` **(신규 구현 필요)** -- `FileReferenceChecker` 구현체
- **검증 방법**:
  - 게시글에서 참조 중인 Object Key로 파일 삭제 요청 시 `409 Conflict` 응답 확인
  - 게시글에서 참조하지 않는 Object Key로 파일 삭제 요청 시 정상 삭제 확인
  - Soft Delete된 게시글이 참조하는 Object Key로 파일 삭제 요청 시 삭제 허용 확인 (Soft Delete된 게시글은 참조로 간주하지 않음)

#### POST-IMG-INV-04: 이미지 수정 시 기존 이미지 자동 정리 없음

> 게시글 수정으로 이미지 목록이 변경되더라도, 이전 이미지 파일은 자동으로 삭제되지 않는다.

- **사전조건**: 게시글 수정 시 이미지 목록이 [A, B]에서 [C]로 변경됨
- **사후조건**: 이전 이미지(A, B)의 FileMetadata와 S3 객체는 그대로 유지됨
- **설계 근거**: 이미지가 다른 게시글에서도 참조될 가능성. 고아 이미지 정리는 주기적 스케줄러 또는 수동 운영으로 처리.
- **관련 코드**: `UpdatePostService.updatePost()` (L83) -- `post.clearImages()` 후 새 이미지 추가 `(현재 구현 일치 -- 기존 S3 파일 삭제 없음)`
- **검증 방법**: 게시글 이미지를 [A, B]에서 [C]로 변경한 후, A와 B의 FileMetadata가 COMPLETED 상태로 유지됨을 확인

#### POST-IMG-INV-05: 게시글 삭제(Soft Delete) 시 이미지 자동 삭제 없음

> 게시글이 Soft Delete되더라도, 연결된 이미지 파일은 자동으로 삭제되지 않는다.

- **사전조건**: 게시글이 Soft Delete됨
- **사후조건**: 연결된 이미지의 FileMetadata와 S3 객체는 그대로 유지됨. Soft Delete된 게시글은 참조 무결성 검사에서 제외됨(POST-IMG-INV-03 참조).
- **설계 근거**: Soft Delete된 게시글은 복원 가능성이 있으므로 이미지를 유지.
- **검증 방법**: 이미지가 연결된 게시글을 삭제한 후, FileMetadata 상태가 COMPLETED로 유지됨을 확인

#### POST-IMG-INV-06: 게시글당 이미지 최대 5개 제한 (DECISION-03 확정: A안, 개수 검증 먼저)

> 하나의 게시글에 첨부 가능한 이미지는 최대 5개이다. 개수 검증은 Object Key 유효성 검증보다 **먼저** 수행한다 (DB 조회 전 빠른 실패).

- **검증 계층**: DTO 레벨 (`@Size(max = 5)`) + 엔티티 레벨 (`Post.MAX_IMAGE_COUNT = 5`)
- **위반 시**: DTO 검증 실패 또는 `PostImageLimitExceededException`
- **관련 코드**:
  - `Post.MAX_IMAGE_COUNT = 5` (L57) `(현재 구현 일치)`
  - `Post.addImage()` (L266-267) -- 크기 검증 `(현재 구현 일치)`
  - `CreatePostRequest.imageUrls` (L30-31) -- `@Size(max = 5)` `(현재 구현 일치)`
  - `PostValidator.validateImageCount()` (L12, L18-19) `(현재 구현 일치)`
- **검증 방법**: 5개 이미지 첨부 시 성공, 6개 이미지 첨부 시 거부 확인. 6개 Object Key 중 일부가 무효(non-COMPLETED)인 경우에도 개수 검증이 먼저 동작하여 개수 초과 에러가 반환됨을 확인.

#### POST-IMG-INV-07: 이미지 없는 게시글 허용

> 이미지가 없는 게시글도 유효하다. `imageUrls`가 null 또는 빈 리스트인 경우 이미지 없이 게시글이 생성/수정된다.

- **관련 코드**: `CreatePostRequest` compact constructor (L33-36) -- null을 `List.of()`로 변환 `(현재 구현 일치)`
- **검증 방법**: `imageUrls`를 null 또는 빈 리스트로 게시글 생성/수정 시 성공 확인

### 1-2. Inquiry 첨부파일 불변조건

#### INQ-ATT-INV-01: 첨부파일 Object Key는 COMPLETED 상태 파일만 참조 가능

> 문의에 연결되는 첨부파일의 Object Key (현재 `fileUrl` 필드)는 `FileMetadata.status == COMPLETED`이고 `deleted == false`인 것만 허용한다.

- **사전조건**: 문의 생성 요청 시 `attachments` 리스트에 첨부파일 정보가 포함됨
- **사후조건**: 모든 첨부파일의 Object Key(fileUrl)에 대해 FileMetadata가 존재하고, `status == COMPLETED`이며, `deleted == false`
- **위반 시 예외**: `InvalidImageReferenceException` (400 Bad Request) -- POST-IMG-INV-01과 동일한 예외 사용
- **관련 코드**:
  - `InquiryAttachmentHelper.addAttachments()` (L24-34) -- 현재는 fileUrl을 검증 없이 저장 `(현재 구현 -- 검증 없음)`
  - Object Key 유효성 검증 로직 **(신규 구현 필요)**
- **검증 방법**:
  - COMPLETED 상태의 Object Key로 문의 생성 시 첨부 성공 확인
  - REQUESTED / CONFIRMING / FAILED / EXPIRED 상태의 Object Key로 문의 생성 시 거부 확인
  - 존재하지 않는 Object Key로 문의 생성 시 거부 확인
  - Soft Delete된 FileMetadata의 Object Key로 문의 생성 시 거부 확인 (`@SQLRestriction`에 의해 미조회)

#### INQ-ATT-INV-02: Object Key 프리픽스 강제 정책 (DECISION-01 확정: A안 / DECISION-04 미확정: 프리픽스 값)

> 문의에 연결되는 첨부파일의 Object Key는 **DECISION-04에서 확정될 프리픽스 값**으로 시작해야 한다. 문의 서비스에서 프리픽스를 **강제 검증**한다. 프리픽스 강제 로직 자체는 DECISION-01 확정(A안)에 의해 구현하되, 실제 프리픽스 값은 DECISION-04 확정 후 채운다.

- **사전조건**: 업로드 URL 생성 시 프론트엔드가 `purpose = "{DECISION-04에서 확정될 값}"` 지정 → ObjectKeyGenerator가 해당 프리픽스 Key 생성
- **사후조건**: Object Key 형식이 `{DECISION-04에서 확정될 값}/{YYYY}/{MM}/{DD}/{UUID}.{ext}`
- **위반 시**: 다른 도메인(posts, events)의 이미지를 문의 첨부파일로 부적절하게 연결하는 것을 방지. 400 Bad Request.
- **관련 코드**: 문의 서비스 프리픽스 검증 로직 **(신규 구현 필요)** -- `ObjectKeyValidator.validate(objectKey, "{DECISION-04 확정값}/")` 형태로 호출
- **검증 방법**: `{DECISION-04 확정값}/` 프리픽스 Key 허용, 다른 도메인 프리픽스 Key 거부 (400 Bad Request)
- **[DECISION-04 미확정]**: 프론트엔드에서 현재 사용 중인 purpose 값 확인 후 프리픽스 확정 필요. 확정 시 이 불변조건의 플레이스홀더를 실제 값으로 교체할 것.

#### INQ-ATT-INV-03: 참조 무결성 -- 문의가 참조 중인 첨부파일 삭제 차단

> 문의의 `InquiryAttachment.fileUrl`(실제로는 objectKey)로 참조 중인 파일은 `FileDeleteService`에서 삭제를 거부한다.

- **사전조건**: FileDeleteService가 Object Key로 삭제 요청을 받음
- **사후조건**: InquiryFileReferenceChecker가 해당 Object Key를 참조하는 문의가 존재하면 `FileReferenceExistsException` 발생 (`409 Conflict`)
- **위반 시**: 문의 첨부파일이 깨진 파일(broken file) 표시
- **관련 코드**:
  - `FileReferenceChecker` 인터페이스 (L10-18) `(현재 구현 일치)`
  - `FileDeleteService.checkFileReferences()` (L77-83) `(현재 구현 일치)`
  - `InquiryFileReferenceChecker` **(신규 구현 필요)** -- `FileReferenceChecker` 구현체
- **검증 방법**:
  - 문의에서 참조 중인 Object Key로 파일 삭제 요청 시 `409 Conflict` 응답 확인
  - 문의에서 참조하지 않는 Object Key로 파일 삭제 요청 시 정상 삭제 확인
  - Soft Delete된 문의가 참조하는 Object Key로 파일 삭제 요청 시 삭제 허용 확인 (Soft Delete된 문의는 참조로 간주하지 않음)

#### INQ-ATT-INV-04: 문의 삭제(Soft Delete) 시 첨부파일 자동 삭제 없음

> 문의가 Soft Delete되더라도, 연결된 첨부파일은 자동으로 삭제되지 않는다.

- **사전조건**: 문의가 Soft Delete됨
- **사후조건**: 연결된 첨부파일의 FileMetadata와 S3 객체는 그대로 유지됨. Soft Delete된 문의는 참조 무결성 검사에서 제외됨(INQ-ATT-INV-03 참조).
- **설계 근거**: Soft Delete된 문의는 복원 가능성이 있으므로 첨부파일을 유지.
- **검증 방법**: 첨부파일이 연결된 문의를 삭제한 후, FileMetadata 상태가 COMPLETED로 유지됨을 확인

#### INQ-ATT-INV-05: 문의당 첨부파일 최대 3개 제한 (기존 INQ-INV-02 확장, DECISION-03 확정: A안)

> 하나의 문의에 첨부파일은 최대 3개까지만 가능하다. 이 제한은 기존 INQ-INV-02에서 정의되어 있으며, S3 연동 시에도 동일하게 적용된다. 개수 검증은 Object Key 유효성 검증보다 **먼저** 수행한다 (DB 조회 전 빠른 실패).

- **관련 코드**: `Inquiry.MAX_ATTACHMENTS = 3` (L34) `(현재 구현 일치)`, DTO `@Size(max = 3)` `(현재 구현 일치)`
- **검증 방법**: 3개 첨부 시 성공, 4개 첨부 시 거부. 기존 INQ-INV-02 참조.

---

## 2. 상태 모델 (State Machine & Transitions)

### 2-1. 이미지/첨부파일 연결 가능 조건

게시글과 문의에 이미지/첨부파일을 연결하려면 해당 파일의 FileMetadata 상태가 COMPLETED여야 한다.

```
                          FileMetadata 상태
┌───────────┐   ┌────────────┐   ┌───────────┐
│ REQUESTED │   │ CONFIRMING │   │ COMPLETED │ ◄── 이 상태만 연결 가능
└───────────┘   └────────────┘   └───────────┘
                                       │
                                       │ 게시글/문의 생성·수정 시
                                       │ Object Key 검증
                                       ▼
                               ┌──────────────────┐
                               │  Post / Inquiry   │
                               │  에 연결됨         │
                               └──────────────────┘

┌──────────┐   ┌──────────┐
│  FAILED  │   │ EXPIRED  │ ◄── 이 상태는 연결 거부
└──────────┘   └──────────┘
```

### 2-2. Post 이미지 연결 상태 전이

```
              게시글 생성 (imageUrls=[] 또는 COMPLETED Key 목록)
                                    │
                                    ▼
                         ┌──────────────────┐
                         │  이미지 미연결    │
                         │ (imageUrls=[])   │
                         └────────┬─────────┘
                                  │
                    수정 (imageUrls=[유효한 COMPLETED Key 목록])
                                  │
                                  ▼
                         ┌──────────────────┐
                         │  이미지 연결됨    │
                         │ (imageUrls=[값])  │
                         └────────┬─────────┘
                         │        │         │
          수정(빈 리스트) │  수정(다른 목록)  │  게시글 삭제(Soft Delete)
              │          │        │         │
              ▼          │        ▼         │
         이미지 미연결    │   이미지 변경    │  게시글 삭제됨
         (기존 이미지     │   (기존 이미지   │  (이미지 유지,
          유지)          │    유지)         │   참조 해제)
                         │                  │
                         └──────────────────┘
```

> **이미지 교체 메커니즘**: 게시글 수정 시 `Post.clearImages()`로 기존 `PostImage` 레코드를 모두 **DELETE** (JPA `orphanRemoval=true`)한 후, 새 이미지 목록으로 `PostImage` 레코드를 **INSERT**한다. 기존 레코드의 `imageUrl`을 UPDATE하는 것이 아니다. 단, S3의 실제 파일과 `FileMetadata`는 삭제되지 않는다 (POST-IMG-INV-04, POST-IMG-INV-05).

### 2-3. Inquiry 첨부파일 연결 특성

문의는 생성 시에만 첨부파일을 추가할 수 있으며 수정 기능이 없다 (기존 문의 도메인 특성). 따라서 연결 상태 전이는 단방향이다:

```
              문의 생성 (attachments=null 또는 AttachmentInfo 목록)
                                    │
                         ┌──────────┴──────────┐
                         ▼                      ▼
              ┌──────────────────┐    ┌──────────────────┐
              │  첨부파일 없음   │    │  첨부파일 연결됨  │
              └──────────────────┘    └────────┬─────────┘
                                               │
                                    문의 삭제(Soft Delete)
                                               │
                                               ▼
                                     문의 삭제됨
                                     (첨부파일 유지,
                                      참조 해제)
```

---

## 3. 시스템 경계와 책임 분리 (System Boundary & SoC)

### 3-1. Post 모듈 → Storage 모듈 의존 방향

```
┌───────────────────────────────────────────────────────┐
│                    Post 모듈                           │
│                                                       │
│  PostImage 엔티티                                     │
│  ├── imageUrl: String (실제로는 objectKey)             │
│  │   (Storage 모듈의 Object Key를 문자열로 보관)       │
│  │                                                    │
│  CreatePostService / UpdatePostService                │
│  ├── addImages / 이미지 수정                          │
│  │   └── FileMetadata 존재+COMPLETED 검증  ──────────┐│
│  │                                                   ││
│  PostFileReferenceChecker (신규)                      ││
│  └── isReferenced(objectKey) ──────────────────────┐ ││
│                                                    │ ││
└────────────────────────────────────────────────────┼─┼┘
                                                     │ │
┌────────────────────────────────────────────────────┼─┼┐
│                    Storage 모듈                     │ ││
│                                                    │ ││
│  FileMetadataRepository                            │ ││
│  ├── findByObjectKeyAndDeletedFalse() ◄────────────┘ ││
│  │                                                   ││
│  FileDeleteService                                   ││
│  ├── checkFileReferences()                           ││
│  │   └── FileReferenceChecker.isReferenced() ◄───────┘│
│  │       (PostFileReferenceChecker가 Bean으로 등록)    │
│                                                       │
└───────────────────────────────────────────────────────┘
```

### 3-2. Inquiry 모듈 → Storage 모듈 의존 방향

```
┌───────────────────────────────────────────────────────┐
│                    Inquiry 모듈                        │
│                                                       │
│  InquiryAttachment 엔티티                              │
│  ├── fileUrl: String (실제로는 objectKey)              │
│  ├── fileName: String                                 │
│  ├── fileSize: Long                                   │
│  │                                                    │
│  InquiryAttachmentHelper                              │
│  ├── addAttachments()                                 │
│  │   └── FileMetadata 존재+COMPLETED 검증  ──────────┐│
│  │                                                   ││
│  InquiryFileReferenceChecker (신규)                   ││
│  └── isReferenced(objectKey) ──────────────────────┐ ││
│                                                    │ ││
└────────────────────────────────────────────────────┼─┼┘
                                                     │ │
┌────────────────────────────────────────────────────┼─┼┐
│                    Storage 모듈                     │ ││
│  (Post와 동일 구조)                                  │ ││
│  FileMetadataRepository ◄────────────────────────────┘ ││
│  FileDeleteService                                    ││
│  └── FileReferenceChecker.isReferenced() ◄────────────┘│
└───────────────────────────────────────────────────────┘
```

### 3-3. 각 구성요소 책임

| 구성요소 | 책임 | 하지 않는 것 |
|---------|------|-------------|
| **PostImage 엔티티** | `imageUrl`(objectKey)을 문자열 필드로 보관, displayOrder 관리 | FileMetadata 엔티티에 대한 JPA 관계 없음 (약한 참조) |
| **InquiryAttachment 엔티티** | `fileUrl`(objectKey), `fileName`, `fileSize`를 보관 | FileMetadata 엔티티에 대한 JPA 관계 없음 (약한 참조) |
| **CreatePostService / UpdatePostService** | 이미지 Object Key 검증(COMPLETED 상태 확인), 프리픽스 검증, 게시글 CRUD | 이미지 업로드/다운로드 URL 생성, S3 직접 접근 |
| **InquiryAttachmentHelper** | 첨부파일 Object Key 검증(COMPLETED 상태 확인), 프리픽스 검증 | 파일 업로드/다운로드 URL 생성, S3 직접 접근 |
| **PostFileReferenceChecker** | 해당 Object Key를 참조하는 활성(deleted=false) 게시글이 있는지 확인 | 파일 삭제 결정, S3 접근 |
| **InquiryFileReferenceChecker** | 해당 Object Key를 참조하는 활성(deleted=false) 문의가 있는지 확인 | 파일 삭제 결정, S3 접근 |
| **FileDeleteService** | 참조 무결성 검사 후 파일 삭제 | 어떤 엔티티가 참조하는지 알 필요 없음 (인터페이스로 추상화) |
| **Frontend** | 이미지 업로드 후 COMPLETED Object Key를 요청에 포함 | 이미지 참조 검증 (서버가 수행) |

### 3-4. 약한 참조 설계 (Weak Reference)

> PostImage / InquiryAttachment와 FileMetadata 간에 JPA `@ManyToOne` 또는 FK 제약을 사용하지 않는다.

- **이유**: Soft Delete 호환성. Post/Inquiry와 FileMetadata는 각각 독립적으로 Soft Delete되며, FK가 있으면 삭제 순서 충돌 및 orphan 문제가 발생한다.
- **연결 방식**: 엔티티의 문자열 필드에 FileMetadata의 `objectKey` 값을 저장한다.
- **참조 무결성**: 애플리케이션 레벨에서 `FileReferenceChecker` 인터페이스를 통해 보장한다.
- **선행 패턴**: Event.posterImageObjectKey (String, nullable, FK 없음), Event.surveyId (Long, nullable, FK 없음)와 동일한 약한 참조 패턴.

---

## 4. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 4-1. Post imageUrls 입력값 (게시글 생성/수정)

| 분류 | 값 | 예상 결과 | 검증 |
|------|---|----------|------|
| **유효: null** | `null` | 빈 리스트로 변환 → 이미지 없는 게시글 | POST-IMG-INV-07 |
| **유효: 빈 리스트** | `[]` | 이미지 없는 게시글 | POST-IMG-INV-07 |
| **유효: COMPLETED Key 1개** | `["posts/2026/03/01/{UUID}.png"]` | 이미지 1개 연결 성공 | POST-IMG-INV-01 |
| **유효: COMPLETED Key 5개** | 5개 COMPLETED Key 목록 | 이미지 5개 연결 성공 (최대) | POST-IMG-INV-06 |
| **무효: 6개 Key** | 6개 Key 목록 | 거부 (이미지 최대 5개 초과, DTO @Size 검증) | POST-IMG-INV-06 |
| **무효: 존재하지 않는 Key** | `["posts/2026/03/01/nonexistent.png"]` | 거부 (FileMetadata 미발견) | POST-IMG-INV-01 |
| **무효: Soft Delete된 Key** | `deleted=true`인 FileMetadata의 Object Key | 거부 (@SQLRestriction에 의해 미조회) | POST-IMG-INV-01 |
| **무효: REQUESTED 상태 Key** | 업로드 완료 안 된 Object Key | 거부 (400 Bad Request) | POST-IMG-INV-01 |
| **무효: CONFIRMING 상태 Key** | S3 HEAD 검증 중인 Object Key | 거부 (400 Bad Request) | POST-IMG-INV-01 |
| **무효: FAILED 상태 Key** | 업로드 실패한 Object Key | 거부 (400 Bad Request) | POST-IMG-INV-01 |
| **무효: EXPIRED 상태 Key** | 만료된 Object Key | 거부 (400 Bad Request) | POST-IMG-INV-01 |
| **무효: 다른 도메인 Key** | `["events/2026/03/01/{UUID}.png"]` (COMPLETED) | 거부 (400 Bad Request) -- `posts/` 프리픽스 강제 (DECISION-01 확정: A안) | POST-IMG-INV-02 |
| **무효: 목록 내 일부 무효** | `["posts/valid.png", "posts/invalid.png"]` | 전체 거부 (하나라도 검증 실패 시 전체 요청 거부) | POST-IMG-INV-01 |
| **미결: 중복 Key** | `["posts/same.png", "posts/same.png"]` | 미결 (DECISION-06 확정 후 결정) | DECISION-06 |

### 4-2. Inquiry attachments 입력값 (문의 생성)

| 분류 | 값 | 예상 결과 | 검증 |
|------|---|----------|------|
| **유효: null** | `null` | 첨부파일 없는 문의 | INQ-INV-02 |
| **유효: 빈 리스트** | `[]` | 첨부파일 없는 문의 | INQ-INV-02 |
| **유효: COMPLETED Key 1개** | fileUrl=`inquiries/.../{UUID}.png` | 첨부파일 1개 연결 성공 | INQ-ATT-INV-01 |
| **유효: COMPLETED Key 3개** | 3개 첨부파일 정보 | 첨부파일 3개 연결 성공 (최대) | INQ-ATT-INV-05 |
| **무효: 4개 첨부** | 4개 첨부파일 정보 | 거부 (최대 3개 초과) | INQ-ATT-INV-05 |
| **무효: 존재하지 않는 Key** | fileUrl=`inquiries/.../nonexistent.png` | 거부 (FileMetadata 미발견) | INQ-ATT-INV-01 |
| **무효: Soft Delete된 Key** | `deleted=true`인 FileMetadata의 Object Key | 거부 (@SQLRestriction에 의해 미조회) | INQ-ATT-INV-01 |
| **무효: REQUESTED 상태 Key** | 업로드 완료 안 된 Object Key | 거부 (400 Bad Request) | INQ-ATT-INV-01 |
| **무효: CONFIRMING 상태 Key** | S3 HEAD 검증 중인 Object Key | 거부 (400 Bad Request) | INQ-ATT-INV-01 |
| **무효: FAILED 상태 Key** | 업로드 실패한 Object Key | 거부 (400 Bad Request) | INQ-ATT-INV-01 |
| **무효: EXPIRED 상태 Key** | 만료된 Object Key | 거부 (400 Bad Request) | INQ-ATT-INV-01 |
| **무효: 다른 도메인 Key** | fileUrl=`posts/.../{UUID}.png` (COMPLETED) | 거부 (400 Bad Request) -- 프리픽스 강제 (DECISION-01 확정: A안, DECISION-04 미확정) | INQ-ATT-INV-02 |
| **미결: 중복 Key** | 동일 fileUrl을 가진 AttachmentInfo 2개 | 미결 (DECISION-06 확정 후 결정) | DECISION-06 |

### 4-3. 이미지/첨부파일 개수 경계값

| 도메인 | 테스트 값 | 분류 | 예상 결과 |
|--------|----------|------|----------|
| Post | 0개 | 최소 유효 | 허용 |
| Post | 1개 | 일반 유효 | 허용 |
| Post | 5개 | 최대 허용 경계 | 허용 |
| Post | 6개 | 경계 초과 | 거부 |
| Inquiry | 0개 | 최소 유효 | 허용 |
| Inquiry | 1개 | 일반 유효 | 허용 |
| Inquiry | 3개 | 최대 허용 경계 | 허용 |
| Inquiry | 4개 | 경계 초과 | 거부 |

---

## 5. 권한/보안 정책 (RBAC & Authorization)

### 5-1. Post 이미지 접근 권한

이미지 연결/해제는 게시글 생성/수정의 일부이므로, 게시글 CRUD 권한과 동일하다.

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 이미지 포함 게시글 작성 | 401 | 게시판 권한에 따름 | **O** (일반 게시판) | **O** | **O** |
| 이미지 포함 게시글 수정 | 401 | 게시판 권한에 따름 | **O** (본인 글만) | 본인 또는 ADMIN | **O** |
| 이미지 포함 게시글 조회 | 401 | 게시판 권한에 따름 | **O** | **O** | **O** |
| 이미지 다운로드 URL 요청 (Storage API) | 401 (SEC-STOR-04) | **O** | **O** | **O** | **O** |

> **참고**: 게시글 API는 `@PreAuthorize("isAuthenticated()")` -- 인증된 사용자만 접근 가능. 게시판별 세부 권한은 `CheckWritePermissionService`에서 처리.

### 5-2. Inquiry 첨부파일 접근 권한

문의 생성은 회원/비회원 모두 가능하며, 첨부파일 권한도 이에 따른다.

| 작업 | 비인증(비회원) | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 첨부파일 포함 비회원 문의 생성 | 아래 비회원 제약 참조 | - | - | - | - |
| 첨부파일 포함 회원 문의 생성 | 401 | **O** | **O** | **O** | **O** |
| 첨부파일 포함 문의 조회 (본인) | 비밀번호 인증 (다운로드는 DECISION-07에 따라 결정) | 본인만 | 본인만 | **O** | **O** |
| 첨부파일 다운로드 URL 요청 (Storage API) | 401 (SEC-STOR-04) | **O** | **O** | **O** | **O** |

> **비회원 문의 첨부파일 제약 (STOR-INV-06 + SEC-STOR-04)**:
> STOR-INV-06에 의해 비인증 사용자는 Presigned Upload URL 발급을 요청할 수 없다 (401 Unauthorized). 따라서 **비회원은 S3에 파일을 업로드할 수 없으므로, 비회원 문의에 첨부파일을 포함하는 것 자체가 불가능하다**. 또한 SEC-STOR-04에 의해 비인증 사용자는 다운로드 URL 요청도 불가능하다.
>
> 이에 따라 비회원 문의 첨부파일 기능은 **현재 범위에서 사실상 비활성 상태**이다. DECISION-07에서 비회원 첨부파일 정책 전반을 결정해야 한다.

### 5-3. 다른 사용자 이미지/첨부파일 사용 정책 (DECISION-02 확정: A안, 소유권 무관)

> 이미지 업로드자 소유권 검증을 게시글/문의 연결 시 수행하지 않는다. COMPLETED 상태만 확인하며, 업로드자가 누구인지와 무관하게 연결을 허용한다. EVT-IMG에서 A안으로 확정된 정책과 동일하게 적용한다.

| ID | 검증 항목 | 예상 결과 | 설명 |
|----|----------|----------|------|
| SEC-POST-IMG-01 | 사용자 A가 업로드한 이미지를 사용자 B가 게시글에 연결 시도 | **허용** | Object Key가 COMPLETED 상태이면 연결 가능. 업로드자 신원 미검증. |
| SEC-INQ-ATT-01 | 회원 A가 업로드한 파일을 회원 B가 문의에 첨부 시도 | **허용** | 동일 정책 적용. |

---

## 6. 관측 가능성 (Observability & Audit)

### 6-1. 서비스별 로그 메시지

| 서비스/시점 | 로그 메시지 형식 | 로그 레벨 |
|-----------|-----------|----------|
| 게시글 생성 (이미지 포함) | `게시글 작성 완료 - boardCode: {}, postId: {}, imageCount: {}, objectKeys: [{}]` | INFO |
| 게시글 수정 (이미지 변경) | `게시글 이미지 수정 - postId: {}, 이전: [{}], 이후: [{}]` | INFO |
| 문의 생성 (첨부파일 포함) | `문의 생성 완료 - inquiryNumber: {}, 첨부파일 수: {}, objectKeys: [{}]` | INFO |
| 이미지/첨부파일 참조 검증 실패 | `이미지 참조 검증 실패 - objectKey: {}, 사유: {} (status={} 또는 미존재)` | WARN |
| 참조 무결성 검사 차단 (Post) | `파일 삭제 차단 (게시글 참조) - objectKey: {}, referencing postId: {}` | WARN |
| 참조 무결성 검사 차단 (Inquiry) | `파일 삭제 차단 (문의 참조) - objectKey: {}, referencing inquiryId: {}` | WARN |

### 6-2. 로그 주의사항

- Object Key는 로그에 기록 가능 (보안 위험 없음, Presigned URL과 달리 서명 정보 미포함)
- 이미지 변경 시 이전/이후 Object Key 목록을 모두 로깅하여 변경 추적 가능
- 파일 삭제 차단 시 어떤 게시글/문의가 참조하는지 ID를 함께 로깅

---

## 7. 테스트 전략 (Test Strategy)

### 7-1. 테스트 레벨별 전략

| 테스트 레벨 | 범위 | 의존성 처리 |
|-----------|------|-----------|
| **단위 테스트** | PostFileReferenceChecker 로직, InquiryFileReferenceChecker 로직, Object Key 프리픽스 검증 | Repository Mock |
| **서비스 통합 테스트** | CreatePostService/UpdatePostService의 이미지 참조 검증, InquiryAttachmentHelper의 첨부파일 참조 검증, FileDeleteService의 참조 무결성 | 실제 DB (H2), FileMetadataRepository 실제 사용 |
| **컨트롤러 통합 테스트** | API 요청/응답에 이미지/첨부파일 필드 포함 여부, 에러 응답 | MockMvc + 실제 서비스 |

### 7-2. 검증 항목별 테스트 매핑

| 불변조건 | 테스트 범위 | 상태 |
|---------|-----------|------|
| POST-IMG-INV-01 (COMPLETED 상태 검증) | 통합: 각 FileUploadStatus별 Object Key로 게시글 생성 시 성공/거부 확인 | 미구현 |
| POST-IMG-INV-02 (프리픽스 강제, DECISION-01 확정: A안) | 통합: `posts/` 프리픽스 Key 허용, 다른 프리픽스 Key 거부 (400) 확인 | 미구현 |
| POST-IMG-INV-03 (참조 무결성) | 통합: FileDeleteService에서 참조 중인 Key 삭제 차단 확인 | 미구현 |
| POST-IMG-INV-04 (이미지 변경 시 유지) | 통합: 이미지 변경 후 기존 이미지 FileMetadata 상태 확인 | 미구현 |
| POST-IMG-INV-05 (게시글 삭제 시 유지) | 통합: 게시글 Soft Delete 후 이미지 FileMetadata 상태 확인 | 미구현 |
| POST-IMG-INV-06 (이미지 5개 제한) | 통합: 5개 성공, 6개 거부 확인 | 부분 커버 (S3 검증 미검증) |
| POST-IMG-INV-07 (이미지 없는 게시글) | 통합: null/빈 리스트로 게시글 생성 성공 확인 | 부분 커버 (S3 검증 미검증) |
| INQ-ATT-INV-01 (COMPLETED 상태 검증) | 통합: 각 FileUploadStatus별 Object Key로 문의 생성 시 성공/거부 확인 | 미구현 |
| INQ-ATT-INV-02 (프리픽스 강제, DECISION-01 확정: A안, DECISION-04 미확정) | 통합: DECISION-04 확정 프리픽스 Key 허용, 다른 프리픽스 Key 거부 (400) 확인 | 미구현 |
| INQ-ATT-INV-03 (참조 무결성) | 통합: FileDeleteService에서 참조 중인 Key 삭제 차단 확인 | 미구현 |
| INQ-ATT-INV-04 (문의 삭제 시 유지) | 통합: 문의 Soft Delete 후 첨부파일 FileMetadata 상태 확인 | 미구현 |
| GAP-INQ-S3-01 (URL 패턴 검증 버그) | 단위: Object Key 형식으로 AttachmentInfo 생성 시 validation 통과 확인 | 미구현 |
| GAP-COMMON-S3-01 (fileName/fileSize 출처 정책) | DECISION-05 확정 후 테스트 범위 결정 | 미구현 |
| SEC-POST-IMG-01 (다른 사용자 이미지) | 통합: 사용자 A 업로드 이미지를 사용자 B가 게시글에 연결 성공 확인 (DECISION-02 확정: A안) | 미구현 |
| SEC-INQ-ATT-01 (다른 사용자 첨부파일) | 통합: 사용자 A 업로드 파일을 사용자 B가 문의에 첨부 성공 확인 (DECISION-02 확정: A안) | 미구현 |

### 7-3. 부정 시나리오 (Negative Scenarios)

| # | 시나리오 | 도메인 | 예상 결과 |
|---|---------|--------|----------|
| N-01 | 업로드 미완료(REQUESTED) 이미지를 게시글에 연결 | Post | 거부 (이미지 참조 검증 실패) |
| N-02 | 업로드 실패(FAILED) 이미지를 게시글에 연결 | Post | 거부 |
| N-03 | 만료(EXPIRED) 이미지를 게시글에 연결 | Post | 거부 |
| N-19 | 확인 중(CONFIRMING) 이미지를 게시글에 연결 | Post | 거부 (COMPLETED 아님) |
| N-04 | Soft Delete된 이미지를 게시글에 연결 | Post | 거부 (@SQLRestriction에 의해 미조회) |
| N-05 | 게시글이 참조 중인 이미지를 삭제 | Post | 거부 (409 Conflict) |
| N-06 | 존재하지 않는 Object Key를 게시글에 연결 | Post | 거부 (FileMetadata 미발견) |
| N-07 | 6개 이미지를 게시글에 첨부 | Post | 거부 (최대 5개 초과) |
| N-08 | imageUrls 목록 내 일부만 무효인 경우 | Post | 전체 거부 |
| N-09 | `events/` 프리픽스 Key를 게시글에 연결 | Post | 거부 (프리픽스 검증 실패, 400) |
| N-10 | 업로드 미완료(REQUESTED) 파일을 문의에 첨부 | Inquiry | 거부 (참조 검증 실패) |
| N-11 | 업로드 실패(FAILED) 파일을 문의에 첨부 | Inquiry | 거부 |
| N-12 | Soft Delete된 파일을 문의에 첨부 | Inquiry | 거부 (@SQLRestriction에 의해 미조회) |
| N-17 | 확인 중(CONFIRMING) 파일을 문의에 첨부 | Inquiry | 거부 (COMPLETED 아님) |
| N-18 | 만료(EXPIRED) 파일을 문의에 첨부 | Inquiry | 거부 |
| N-13 | 문의가 참조 중인 첨부파일을 삭제 | Inquiry | 거부 (409 Conflict) |
| N-14 | 4개 첨부파일을 문의에 첨부 | Inquiry | 거부 (최대 3개 초과) |
| N-15 | `posts/` 프리픽스 Key를 문의에 첨부 | Inquiry | 거부 (프리픽스 검증 실패, 400) |
| N-16 | Object Key 형식의 fileUrl이 현재 URL 패턴 검증에 걸림 | Inquiry | 현재: 거부 (Bean Validation 실패) → GAP-INQ-S3-01 수정 후: 허용 |

---

## 8. GAP 분석 (기존 구현의 수정 필요 사항)

### GAP-INQ-S3-01: AttachmentInfo URL 패턴 검증 버그

- **현재 상태**: `AttachmentInfo.fileUrl`에 `@Pattern(regexp = "^https?://...")` URL 정규식 적용 중 -- `AttachmentInfo.java:21-24`
- **문제**: S3 Object Key는 URL 형식이 아니므로 (`inquiries/2026/03/01/{UUID}.png`), 정상적인 Object Key가 Bean Validation에 의해 거부됨
- **심각도**: **높음** -- S3 연동 구현 시 이 검증이 남아있으면 모든 첨부파일 생성이 실패함
- **수정 방향**: URL 패턴 검증(`@Pattern`)을 제거하고, Object Key 형식 검증으로 대체하거나 `@NotBlank`만 유지
- **수정 전 코드**:
  ```java
  // AttachmentInfo.java:20-25 (현재 -- 부적합)
  @NotBlank(message = "파일 URL은 필수입니다")
  @Pattern(
          regexp = "^https?://[\\w.-]+(?:\\.[\\w.-]+)+[\\w.,@?^=%&:/~+#-]*$",
          message = "유효한 URL 형식이어야 합니다"
  )
  private String fileUrl;
  ```
- **수정 후 코드** (DECISION-08에 따라 필드명 변경 가능):
  ```java
  @NotBlank(message = "파일 Object Key는 필수입니다")
  private String fileUrl; // 또는 objectKey (DECISION-08)
  ```

### GAP-COMMON-S3-01: fileName/fileSize 정보 출처 미확정 (DECISION-05 미확정)

- **현재 상태**: 문의 첨부파일의 `fileName`과 `fileSize`는 클라이언트에서 전달받지만, 동일 정보가 `FileMetadata`에도 존재한다 -- `AttachmentInfo.java:27-33`, `FileMetadata.java`
- **문제**: 클라이언트 전달값과 서버(FileMetadata)의 정보가 불일치할 수 있으며, 클라이언트 위변조 가능성이 존재한다
- **심각도**: **중간** -- 기능 동작에 치명적이지는 않으나, 데이터 정합성에 영향
- **수정 방향**: DECISION-05에서 결정. 선택지:
  - A) 클라이언트 전달값 사용 (현재 방식 유지)
  - B) FileMetadata에서 조회하여 사용 (서버 권위적) -- **권장** (클라이언트 위변조 방지, 중복 전달 불필요)
  - C) 클라이언트 전달값과 FileMetadata 값 일치 검증
- **영향 범위**: 섹션 3-2, DECISION-05

### GAP-INQ-S3-02: 비회원 문의 첨부파일 업로드 불가능

- **현재 상태**: STOR-INV-06에 의해 비인증 사용자는 Presigned Upload URL 발급 불가 (401 Unauthorized)
- **문제**: 비회원은 S3에 파일을 업로드할 수 없으므로, 비회원 문의에 첨부파일을 포함하는 것 자체가 현재 아키텍처에서 불가능
- **심각도**: **높음** -- 비회원 문의 첨부파일 기능이 사실상 비활성 상태
- **수정 방향**: DECISION-07에서 비회원 첨부파일 정책 전반을 결정. 아래 선택지 중 택 1:
  - A) 비회원 전용 업로드/다운로드 엔드포인트 생성 (인증 없이 접근 가능, rate limiting 필수)
  - B) 비회원 문의에서 첨부파일 기능 제거 (텍스트 문의만 허용)
  - C) 비회원 문의 생성 시 임시 토큰 발급하여 업로드/다운로드 허용

---

## 9. DB 마이그레이션 및 API 스펙 변경 (신규 구현 필요)

### 9-1. 현재 DB 스키마 현황

| 테이블 | 컬럼명 | 타입 | 제약조건 | 비고 |
|--------|--------|------|---------|------|
| `post_images` | `post_images_image_url` | `VARCHAR(500)` | `NOT NULL` | 실제로는 S3 Object Key 저장 |
| `inquiry_attachments` | `inquiry_attachments_file_url` | `VARCHAR(500)` | `NOT NULL` | 실제로는 S3 Object Key 저장 |
| `inquiry_attachments` | `inquiry_attachments_file_name` | `VARCHAR(255)` | `NOT NULL` | 클라이언트 전달 값 |
| `inquiry_attachments` | `inquiry_attachments_file_size` | `BIGINT` | `NOT NULL` | 클라이언트 전달 값 |

### 9-2. Flyway 마이그레이션 (DECISION-08에 따름)

> DECISION-08이 확정되면 해당 방향에 맞게 마이그레이션 스크립트를 작성한다.

**DECISION-08 A안 (리네이밍 수행) 선택 시**:

```sql
-- V{N}__rename_object_key_columns.sql

-- PostImage: imageUrl → objectKey
ALTER TABLE post_images RENAME COLUMN post_images_image_url TO post_images_object_key;

-- InquiryAttachment: fileUrl → objectKey
ALTER TABLE inquiry_attachments RENAME COLUMN inquiry_attachments_file_url TO inquiry_attachments_object_key;
```

**DECISION-08 B안 (현재 필드명 유지) 선택 시**:

- DB 마이그레이션 불필요
- 코드 내 주석으로 실제 의미 명시

**DECISION-08 C안 (엔티티/서비스만 변경, API 하위호환) 선택 시**:

```sql
-- DB는 A안과 동일하게 리네이밍
ALTER TABLE post_images RENAME COLUMN post_images_image_url TO post_images_object_key;
ALTER TABLE inquiry_attachments RENAME COLUMN inquiry_attachments_file_url TO inquiry_attachments_object_key;
-- OpenAPI 스펙은 기존 필드명 유지 (DTO에서 @Column으로 매핑)
```

### 9-3. OpenAPI 스펙 변경

DECISION-08에 따라 API 스펙 변경 범위가 달라진다.

**A안 선택 시 변경 대상 스키마**:

| 스키마 | 변경 내용 | 비고 |
|--------|---------|------|
| `CreatePostRequest` | `imageUrls` → `imageObjectKeys` | 요청 필드명 변경 |
| `UpdatePostRequest` | `imageUrls` → `imageObjectKeys` | 요청 필드명 변경 |
| `PostDetailResponse` | `imageUrls` → `imageObjectKeys` | 응답 필드명 변경 |
| `PostListResponse` | `imageUrls` → `imageObjectKeys` (포함되는 경우) | 응답 필드명 변경 |
| `AttachmentInfo` | `fileUrl` → `objectKey` | 요청 필드명 변경 |
| `InquiryDetailResponse` | 첨부파일 필드의 `fileUrl` → `objectKey` | 응답 필드명 변경 |

**B안/C안 선택 시**: OpenAPI 스펙 변경 불필요 또는 최소

**DECISION-08과 무관한 필수 변경**: GAP-INQ-S3-01에 의해 `AttachmentInfo.fileUrl`의 `@Pattern(regexp = "^https?://...")` URL 패턴 검증이 제거되므로, OpenAPI 스펙에서 해당 필드의 `pattern` 제약도 함께 제거해야 한다. 이 변경은 DECISION-08 선택지와 무관하게 S3 연동 시 반드시 수행해야 하는 필수 변경이다.

### 9-4. 필드명 의미 정합성 현황

현재 필드명과 실제 저장 값 사이에 의미적 불일치가 있다.

| 엔티티 | 현재 필드명 | 실제 저장 값 | 문제 | DECISION |
|--------|-----------|------------|------|----------|
| `PostImage` | `imageUrl` | S3 Object Key (예: `posts/2026/03/01/{UUID}.png`) | "URL"이 아닌 Object Key를 저장 | DECISION-08 |
| `InquiryAttachment` | `fileUrl` | S3 Object Key (예: `inquiries/2026/03/01/{UUID}.png`) | "URL"이 아닌 Object Key를 저장 | DECISION-08 |
| `CreatePostRequest` | `imageUrls` | S3 Object Key 목록 | "URL"이 아닌 Object Key 목록 | DECISION-08 |
| `AttachmentInfo` | `fileUrl` | S3 Object Key | "URL"이 아닌 Object Key | DECISION-08 |

### 9-5. PostFileReferenceChecker 구현

```java
// 신규 구현 필요
@Component
@RequiredArgsConstructor
public class PostFileReferenceChecker implements FileReferenceChecker {

    private final PostImageRepository postImageRepository;

    @Override
    public boolean isReferenced(String objectKey) {
        // Soft Delete되지 않은 게시글 중 해당 Object Key를 imageUrl로 참조하는 것이 있는지 확인
        return postImageRepository.existsByImageUrlAndPostDeletedFalse(objectKey);
    }
}
```

- `PostImageRepository`에 `existsByImageUrlAndPostDeletedFalse(String imageUrl)` 쿼리 메서드 추가 필요
- **주의**: PostImage는 `BaseEntity`를 상속하여 Soft Delete 필드가 없으므로, 부모인 `Post`의 `deleted` 필드를 기준으로 필터링해야 한다. JPQL 또는 `@Query`로 조인 쿼리 필요:
  ```java
  @Query("SELECT CASE WHEN COUNT(pi) > 0 THEN true ELSE false END FROM PostImage pi WHERE pi.imageUrl = :objectKey AND pi.post.deleted = false")
  boolean existsByImageUrlAndPostNotDeleted(@Param("objectKey") String objectKey);
  ```

### 9-6. InquiryFileReferenceChecker 구현

```java
// 신규 구현 필요
@Component
@RequiredArgsConstructor
public class InquiryFileReferenceChecker implements FileReferenceChecker {

    private final InquiryAttachmentRepository inquiryAttachmentRepository;

    @Override
    public boolean isReferenced(String objectKey) {
        // Soft Delete되지 않은 문의 중 해당 Object Key를 fileUrl로 참조하는 것이 있는지 확인
        return inquiryAttachmentRepository.existsByFileUrlAndInquiryDeletedFalse(objectKey);
    }
}
```

- `InquiryAttachmentRepository`에 유사한 JPQL 쿼리 추가 필요
- **주의**: Inquiry는 `@SQLRestriction("inquiries_deleted = false")` (L20)가 적용되어 있으므로, 일반 JPA 조회에서는 Soft Delete된 문의의 첨부파일이 자동으로 제외된다. 하지만 `InquiryAttachment`에서 `inquiry`를 조인할 때 이 필터가 적용되는지 확인 필요. 확실하게 하려면 명시적 JPQL 사용:
  ```java
  @Query("SELECT CASE WHEN COUNT(ia) > 0 THEN true ELSE false END FROM InquiryAttachment ia WHERE ia.fileUrl = :objectKey AND ia.inquiry.deleted = false")
  boolean existsByFileUrlAndInquiryNotDeleted(@Param("objectKey") String objectKey);
  ```

### 9-7. Object Key 유효성 검증 공통 로직

Post와 Inquiry 모두 동일한 검증 패턴이 필요하므로, 공통 유틸리티 또는 서비스로 추출 가능:

```java
// 공통 검증 로직 (안)
@Component
@RequiredArgsConstructor
public class ObjectKeyValidator {

    private final FileMetadataRepository fileMetadataRepository;

    /**
     * Object Key가 유효한 COMPLETED 파일을 참조하는지 검증한다.
     * @param objectKey 검증 대상 Object Key
     * @param expectedPrefix 예상 프리픽스 (예: "posts/", "inquiries/")
     * @throws InvalidImageReferenceException Object Key가 유효하지 않은 경우
     */
    public void validate(String objectKey, String expectedPrefix) {
        // 1. 프리픽스 검증 (DECISION-01 확정: A안)
        // 2. FileMetadata 조회 (deleted=false)
        // 3. status == COMPLETED 확인
    }
}
```

---

## 설계 결정 사항 (DECISION)

### 확정된 DECISION

| ID | 항목 | 선택지 | 결정 | 영향 범위 |
|----|------|-------|------|----------|
| DECISION-01 | Object Key 프리픽스 검증 여부 | A) 각 도메인에서 해당 프리픽스 강제 (Post→`posts/`, Inquiry→`inquiries/`) B) 프리픽스 무관하게 COMPLETED Key면 허용 | **확정: A** -- 프리픽스 강제. EVT-IMG-INV-03 동일 패턴 적용. | POST-IMG-INV-02, INQ-ATT-INV-02, 섹션 4-1, 4-2, 7-2 |
| DECISION-02 | 이미지/첨부파일 업로드자 소유권 검증 | A) 소유권 무관 (COMPLETED만 확인) B) 업로드자 == 요청자 검증 | **확정: A** -- 소유권 무관. EVT-IMG DECISION-02 동일 패턴 적용. | SEC-POST-IMG-01, SEC-INQ-ATT-01, 섹션 5-3 |
| DECISION-03 | 검증 순서: 개수 검증 vs Object Key 유효성 검증 | A) 개수 검증 먼저 (빠른 실패) B) Object Key 유효성 검증 먼저 | **확정: A** -- 개수 검증 먼저 (DB 조회 전 빠른 실패). | POST-IMG-INV-06, INQ-ATT-INV-05, 섹션 4-1, 4-2 |

### 미확정 DECISION

| ID | 항목 | 선택지 | 현재 상태 | 권장안 | 영향 범위 |
|----|------|-------|----------|-------|----------|
| DECISION-04 | Inquiry 첨부파일 프리픽스 값 | A) `inquiries/` 프리픽스 사용 B) 프론트엔드에서 현재 사용 중인 purpose 값 확인 후 결정 | **미정** -- 프론트엔드 코드 확인 필요 | 프론트엔드 확인 후 결정 | INQ-ATT-INV-02, 섹션 4-2 |
| DECISION-05 | Inquiry 첨부파일 fileName/fileSize 출처 | A) 클라이언트 전달값 유지 (현재 방식) B) FileMetadata에서 조회 (서버 권위적) C) 클라이언트 전달값과 FileMetadata 일치 검증 | **미정** | **B안 권장** (서버 권위적, 클라이언트 위변조 방지) | GAP-COMMON-S3-01, 섹션 3-2 |
| DECISION-06 | Post imageUrls / Inquiry attachments 내 중복 Object Key 허용 여부 | A) 중복 허용 B) 중복 거부 (400 Bad Request) | **미정** | **A안 권장** (같은 이미지를 여러 위치에 표시하는 유스케이스) | 섹션 4-1, 4-2 |
| DECISION-07 | 비회원 문의 첨부파일 정책 전반 | A) 비회원 전용 업로드/다운로드 엔드포인트 생성 B) 비회원 첨부파일 기능 제거 C) 비회원 문의 생성 시 임시 토큰 발급 | **미정** -- STOR-INV-06에 의해 비회원은 업로드 자체 불가 (GAP-INQ-S3-02 참조) | 업로드부터 결정 필요 | 섹션 5-2, GAP-INQ-S3-02, N-16 |
| DECISION-08 | 필드명 리네이밍 (imageUrl → objectKey, fileUrl → objectKey) | A) 리네이밍 수행 (DB + 엔티티 + DTO + API 일괄 변경) B) 현재 필드명 유지 (주석으로 실제 의미 명시) C) 엔티티/서비스만 변경, API 스펙은 하위호환 유지 | **미정** | **B안 권장** (최소 변경, 현재 S3 연동 기능 구현에 집중) | 섹션 9, OpenAPI 스펙, Flyway 마이그레이션, 프론트엔드 |

---

## 관련 문서

- [Presigned URL 이미지 업로드/다운로드 검증 기준서](./image-presigned-url-verification-criteria.md) -- S3 기본 기능 (STOR-INV-*)
- [행사-이미지 연계 검증 기준서](../event/event-image-integration-verification-criteria.md) -- Event 도메인의 동일 패턴 선행 구현 (EVT-IMG-INV-*)
- [문의 검증 기준서](../inquiry-verification-criteria.md) -- 문의 CRUD 및 상태 모델 (INQ-INV-*)
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)
