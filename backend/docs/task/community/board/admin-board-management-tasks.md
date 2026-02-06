# Tasks: 게시판 관리 API (Admin Board Management)

**Input**: GitHub Issue #145 - [Backend] 게시판 관리 API 구현
**Prerequisites**: `tasks.md` (기존 게시판 기능 구현 완료)
**Tech Stack**: Java 21, Spring Boot 4.0.1, Spring Data JPA, MySQL 8.x, Flyway

**Tests**: 테스트 코드 작성 포함 (backend/CLAUDE.md 개발 규칙에 따름)

**Organization**: 요구사항 기반으로 구성하여 독립적 구현 및 테스트 가능

## Format: `[ID] [P?] [REQ] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[REQ]**: 해당 태스크가 속한 요구사항 (REQ1~REQ4)
- 모든 태스크에 정확한 파일 경로 포함

## Path Conventions

- **Backend**: `backend/src/main/java/igrus/web/community/board/`
- **Tests**: `backend/src/test/java/igrus/web/community/board/`
- **Migrations**: `backend/src/main/resources/db/migration/`

## 요구사항 (Issue #145)

| REQ | 요구사항 | 상태 | 비고 |
|-----|---------|------|------|
| REQ0 | 게시판 목록 조회 API | ✅ 완료 | `GET /api/v1/boards` |
| REQ1 | 게시판 생성 API (관리자 전용) | ❌ 미구현 | |
| REQ2 | 게시판 수정 API (관리자 전용) | ❌ 미구현 | |
| REQ3 | 게시판 삭제 API (관리자 전용, 게시글 있으면 삭제 불가) | ❌ 미구현 | |
| REQ4 | 게시판별 권한 설정 (읽기/작성 권한) | ✅ 완료 | `BoardPermission` 엔티티 |
| REQ5 | 게시판별 옵션 설정 (익명 허용, 질문 태그 허용) | ✅ 완료 | Board 엔티티 필드 |

---

## 현재 구현 상태 분석

### 기존 인프라 (활용 가능)

- **Board 엔티티**: `Board.create()` 정적 팩토리 메서드 존재. 단, update 메서드 없음 (불변)
- **BoardPermission 엔티티**: `BoardPermission.create()` 존재. update 메서드 없음
- **BoardRepository**: `findByCode()`, `findAllByOrderByDisplayOrderAsc()`
- **BoardPermissionRepository**: `findByBoardAndRole()`, `findAllByBoard()`
- **BoardCode enum**: `NOTICES`, `GENERAL`, `INSIGHT` — 현재 고정값

### 핵심 설계 결정 사항

1. **BoardCode가 enum**: 새 게시판 생성 시 코드를 동적으로 추가할 수 없음. String으로 변경하거나, enum을 확장해야 함
2. **Board 엔티티 불변**: setter/update 메서드가 없어 수정 API 구현 시 엔티티 변경 필요
3. **게시글 존재 확인**: PostRepository에 `existsByBoard()` / `countByBoard()` 메서드 없음
4. **권한 설정 API**: 게시판 생성/수정 시 권한도 함께 설정할지 별도 API로 분리할지 결정 필요

### 참고 패턴 (기존 Admin 컨트롤러)

```java
// AdminMemberController 패턴:
@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Member Management", description = "...")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
public class AdminMemberController { ... }
```

---

## Phase 1: 사전 작업 (도메인 변경)

**Purpose**: 관리 API 구현에 필요한 도메인 레이어 변경

### 1.1 BoardCode 동적 지원 결정

> **결정 필요**: BoardCode를 enum에서 String으로 변경할지, 아니면 관리자가 enum에 정의된 코드로만 게시판을 생성할 수 있도록 할지.
>
> **권장**: BoardCode를 String으로 변경. 현재 `BoardCode.fromPathVariable()` 등의 메서드를 사용하는 곳이 있어 마이그레이션 필요.
>
> **대안**: enum 유지하되 관리자가 생성 시 미리 정의된 코드만 선택하도록 제한. 향후 확장성은 떨어지지만 안전.

- [ ] T101 [REQ1] BoardCode enum → String 타입 변경 여부 결정 및 리서치
  - 영향 범위: Board.java, BoardController, GetBoardByCodeService, PostController 등
  - enum 유지 시 새 코드 추가는 코드 배포 필요
  - String 변경 시 기존 fromPathVariable 로직 변경 필요

### 1.2 Board 엔티티 수정 메서드 추가

- [ ] T102 [P] [REQ2] Board 엔티티에 update 메서드 추가 in `backend/src/main/java/igrus/web/community/board/domain/Board.java`
  - `updateName(String name)`: 이름 변경
  - `updateDescription(String description)`: 설명 변경
  - `updateOptions(Boolean allowsAnonymous, Boolean allowsQuestionTag)`: 옵션 변경
  - `updateDisplayOrder(Integer displayOrder)`: 표시 순서 변경
  - 또는 `update(String name, String description, Boolean allowsAnonymous, Boolean allowsQuestionTag, Integer displayOrder)`: 일괄 변경

### 1.3 BoardPermission 수정 메서드 추가

- [ ] T103 [P] [REQ2] BoardPermission 엔티티에 update 메서드 추가 in `backend/src/main/java/igrus/web/community/board/domain/BoardPermission.java`
  - `updatePermissions(Boolean canRead, Boolean canWrite)`: 읽기/쓰기 권한 변경

### 1.4 PostRepository 게시글 존재 확인 메서드 추가

- [ ] T104 [P] [REQ3] PostRepository에 게시판별 게시글 존재 확인 메서드 추가 in `backend/src/main/java/igrus/web/community/post/repository/PostRepository.java`
  - `boolean existsByBoardAndDeletedFalse(Board board)`: 삭제되지 않은 게시글 존재 여부 확인

### 1.5 ErrorCode 추가

- [ ] T105 [P] [REQ1/2/3] ErrorCode에 관리 API용 에러 코드 추가 in `backend/src/main/java/igrus/web/common/exception/ErrorCode.java`
  - `BOARD_ALREADY_EXISTS(409, "이미 존재하는 게시판 코드입니다")`
  - `BOARD_HAS_POSTS(400, "게시글이 존재하는 게시판은 삭제할 수 없습니다")`
  - `BOARD_CODE_IMMUTABLE(400, "게시판 코드는 변경할 수 없습니다")`

---

## Phase 2: 예외 클래스 및 DTO 생성

**Purpose**: API 요청/응답 DTO 및 예외 클래스 생성

### 2.1 예외 클래스

- [ ] T201 [P] [REQ1] BoardAlreadyExistsException 구현 in `backend/src/main/java/igrus/web/community/board/exception/BoardAlreadyExistsException.java`
  - CustomBaseException 상속
  - ErrorCode.BOARD_ALREADY_EXISTS 사용

- [ ] T202 [P] [REQ3] BoardHasPostsException 구현 in `backend/src/main/java/igrus/web/community/board/exception/BoardHasPostsException.java`
  - CustomBaseException 상속
  - ErrorCode.BOARD_HAS_POSTS 사용

### 2.2 Request DTO

- [ ] T203 [P] [REQ1] CreateBoardRequest DTO 구현 in `backend/src/main/java/igrus/web/community/board/dto/request/CreateBoardRequest.java`
  - record 형식
  - 필드: code(String, @NotBlank), name(String, @NotBlank @Size(max=50)), description(String, @Size(max=200)), allowsAnonymous(Boolean), allowsQuestionTag(Boolean), displayOrder(Integer)
  - permissions: List<BoardPermissionRequest> (역할별 읽기/쓰기 권한)
  - Jakarta Bean Validation 어노테이션 적용

- [ ] T204 [P] [REQ2] UpdateBoardRequest DTO 구현 in `backend/src/main/java/igrus/web/community/board/dto/request/UpdateBoardRequest.java`
  - record 형식
  - 필드: name(String, @Size(max=50)), description(String, @Size(max=200)), allowsAnonymous(Boolean), allowsQuestionTag(Boolean), displayOrder(Integer)
  - code는 변경 불가이므로 제외
  - null 필드는 변경하지 않는 partial update 방식

- [ ] T205 [P] [REQ1/2] BoardPermissionRequest DTO 구현 in `backend/src/main/java/igrus/web/community/board/dto/request/BoardPermissionRequest.java`
  - record 형식
  - 필드: role(UserRole, @NotNull), canRead(Boolean, @NotNull), canWrite(Boolean, @NotNull)

### 2.3 Response DTO

- [ ] T206 [P] [REQ1/2] AdminBoardDetailResponse DTO 구현 in `backend/src/main/java/igrus/web/community/board/dto/response/AdminBoardDetailResponse.java`
  - record 형식
  - 필드: id, code, name, description, allowsAnonymous, allowsQuestionTag, displayOrder, permissions(List<BoardPermissionResponse>), createdAt, updatedAt
  - `static of(Board, List<BoardPermission>)` 팩토리 메서드

- [ ] T207 [P] [REQ1/2] BoardPermissionResponse DTO 구현 in `backend/src/main/java/igrus/web/community/board/dto/response/BoardPermissionResponse.java`
  - record 형식
  - 필드: role(String), canRead(boolean), canWrite(boolean)
  - `static of(BoardPermission)` 팩토리 메서드

---

## Phase 3: 서비스 레이어 구현

**Purpose**: 비즈니스 로직 구현

### 3.1 게시판 생성 서비스

- [ ] T301 [REQ1] CreateBoardService 구현 in `backend/src/main/java/igrus/web/community/board/service/admin/CreateBoardService.java`
  - `AdminBoardDetailResponse createBoard(CreateBoardRequest request)`
  - 로직:
    1. BoardCode 중복 확인 → BoardAlreadyExistsException
    2. Board.create() 호출
    3. BoardRepository.save()
    4. permissions 설정: 각 role에 대해 BoardPermission.create() 후 저장
    5. AdminBoardDetailResponse 반환
  - @Transactional 적용

### 3.2 게시판 수정 서비스

- [ ] T302 [REQ2] UpdateBoardService 구현 in `backend/src/main/java/igrus/web/community/board/service/admin/UpdateBoardService.java`
  - `AdminBoardDetailResponse updateBoard(String code, UpdateBoardRequest request)`
  - 로직:
    1. Board 조회 → BoardNotFoundException
    2. Board.update() 호출 (null이 아닌 필드만 변경)
    3. AdminBoardDetailResponse 반환
  - @Transactional 적용

### 3.3 게시판 삭제 서비스

- [ ] T303 [REQ3] DeleteBoardService 구현 in `backend/src/main/java/igrus/web/community/board/service/admin/DeleteBoardService.java`
  - `void deleteBoard(String code)`
  - 로직:
    1. Board 조회 → BoardNotFoundException
    2. PostRepository.existsByBoardAndDeletedFalse(board) → BoardHasPostsException
    3. BoardPermissionRepository.findAllByBoard(board) → 전체 삭제
    4. BoardRepository.delete(board)
  - @Transactional 적용

### 3.4 게시판 권한 수정 서비스

- [ ] T304 [REQ2] UpdateBoardPermissionsService 구현 in `backend/src/main/java/igrus/web/community/board/service/admin/UpdateBoardPermissionsService.java`
  - `List<BoardPermissionResponse> updatePermissions(String code, List<BoardPermissionRequest> permissions)`
  - 로직:
    1. Board 조회 → BoardNotFoundException
    2. 기존 권한 조회
    3. 각 role에 대해: 존재하면 update, 없으면 create
    4. 변경된 권한 목록 반환
  - @Transactional 적용

---

## Phase 4: 테스트 작성

**Purpose**: 서비스 단위 테스트 및 컨트롤러 통합 테스트

### 4.1 서비스 단위 테스트

- [ ] T401 [P] [REQ1] CreateBoardServiceTest 구현 in `backend/src/test/java/igrus/web/community/board/service/admin/CreateBoardServiceTest.java`
  - 정상 생성 시 AdminBoardDetailResponse 반환
  - 중복 코드 생성 시 BoardAlreadyExistsException 발생
  - 권한 정보 포함하여 생성 시 BoardPermission도 저장됨

- [ ] T402 [P] [REQ2] UpdateBoardServiceTest 구현 in `backend/src/test/java/igrus/web/community/board/service/admin/UpdateBoardServiceTest.java`
  - 정상 수정 시 변경된 정보 반환
  - 존재하지 않는 게시판 수정 시 BoardNotFoundException 발생
  - null 필드는 기존값 유지 확인

- [ ] T403 [P] [REQ3] DeleteBoardServiceTest 구현 in `backend/src/test/java/igrus/web/community/board/service/admin/DeleteBoardServiceTest.java`
  - 게시글 없는 게시판 삭제 성공
  - 게시글 있는 게시판 삭제 시 BoardHasPostsException 발생
  - 존재하지 않는 게시판 삭제 시 BoardNotFoundException 발생
  - 삭제 시 BoardPermission도 함께 삭제 확인

- [ ] T404 [P] [REQ2] UpdateBoardPermissionsServiceTest 구현 in `backend/src/test/java/igrus/web/community/board/service/admin/UpdateBoardPermissionsServiceTest.java`
  - 기존 권한 수정 성공
  - 새 역할 권한 추가 성공
  - 존재하지 않는 게시판 권한 수정 시 BoardNotFoundException 발생

### 4.2 컨트롤러 통합 테스트

- [ ] T405 [REQ1/2/3] AdminBoardControllerTest 구현 in `backend/src/test/java/igrus/web/community/board/controller/AdminBoardControllerTest.java`
  - POST /api/v1/admin/boards - ADMIN 권한으로 게시판 생성 성공 (201)
  - POST /api/v1/admin/boards - 비인증 사용자 접근 시 401
  - POST /api/v1/admin/boards - MEMBER 권한으로 접근 시 403
  - POST /api/v1/admin/boards - 중복 코드 생성 시 409
  - PATCH /api/v1/admin/boards/{code} - ADMIN 권한으로 수정 성공 (200)
  - PATCH /api/v1/admin/boards/{code} - 존재하지 않는 게시판 수정 시 404
  - DELETE /api/v1/admin/boards/{code} - 게시글 없는 게시판 삭제 성공 (204)
  - DELETE /api/v1/admin/boards/{code} - 게시글 있는 게시판 삭제 시 400
  - PUT /api/v1/admin/boards/{code}/permissions - 권한 수정 성공 (200)

---

## Phase 5: 컨트롤러 구현

**Purpose**: REST API 엔드포인트 구현

- [ ] T501 [REQ1/2/3] AdminBoardController 구현 in `backend/src/main/java/igrus/web/community/board/controller/AdminBoardController.java`
  - 클래스 레벨:
    - `@RequestMapping("/api/v1/admin/boards")`
    - `@PreAuthorize("hasRole('ADMIN')")`
    - `@Tag(name = "Admin Board Management", description = "게시판 관리 API (ADMIN 전용)")`
    - `@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)`
  - 엔드포인트:
    - `POST /api/v1/admin/boards` → createBoard (201 Created)
    - `PATCH /api/v1/admin/boards/{code}` → updateBoard (200 OK)
    - `DELETE /api/v1/admin/boards/{code}` → deleteBoard (204 No Content)
    - `PUT /api/v1/admin/boards/{code}/permissions` → updatePermissions (200 OK)
  - 모든 엔드포인트에 Swagger 어노테이션 (@Operation, @ApiResponses, @Parameter) 적용

---

## Phase 6: Flyway 마이그레이션 (필요 시)

**Purpose**: 스키마 변경이 필요한 경우 마이그레이션 작성

- [ ] T601 [REQ1] BoardCode String 변환 마이그레이션 작성 (T101 결정에 따라)
  - enum 유지 시: SKIP
  - String 변환 시: `V{next}__change_board_code_to_varchar.sql`
    - boards.boards_code 컬럼 타입 변경
    - 기존 데이터 마이그레이션

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (사전 작업)**: 즉시 시작 가능. T101 결정이 전체 방향에 영향
- **Phase 2 (DTO/예외)**: Phase 1 완료 필요 (T105 ErrorCode 추가 후)
- **Phase 3 (서비스)**: Phase 1 + Phase 2 완료 필요
- **Phase 4 (테스트)**: Phase 3와 병렬 가능 (TDD 접근 시 Phase 3 전에 작성)
- **Phase 5 (컨트롤러)**: Phase 3 완료 필요
- **Phase 6 (마이그레이션)**: T101 결정 후 필요 시 Phase 1과 함께 진행

### Within Each Phase - Parallel Opportunities

- Phase 1: T102, T103, T104, T105 병렬 실행 가능
- Phase 2: T201~T207 전부 병렬 실행 가능
- Phase 3: T301~T304 순차 또는 독립적 병렬 가능
- Phase 4: T401~T404 전부 병렬 실행 가능

### Critical Path

```
T101 (결정) → T102/T103 (엔티티 변경) → T301~T304 (서비스) → T501 (컨트롤러)
                T104 (PostRepository) ↗
                T105 (ErrorCode) → T201/T202 (예외) ↗
                                   T203~T207 (DTO) ↗
```

---

## API 설계

### POST /api/v1/admin/boards

게시판 생성

**Request Body:**
```json
{
  "code": "STUDY",
  "name": "스터디",
  "description": "스터디 게시판입니다",
  "allowsAnonymous": false,
  "allowsQuestionTag": true,
  "displayOrder": 4,
  "permissions": [
    { "role": "ASSOCIATE", "canRead": false, "canWrite": false },
    { "role": "MEMBER", "canRead": true, "canWrite": true },
    { "role": "OPERATOR", "canRead": true, "canWrite": true },
    { "role": "ADMIN", "canRead": true, "canWrite": true }
  ]
}
```

**Response:** `201 Created` + AdminBoardDetailResponse

### PATCH /api/v1/admin/boards/{code}

게시판 수정 (partial update)

**Request Body:**
```json
{
  "name": "스터디 게시판",
  "description": "스터디 관련 게시판",
  "allowsAnonymous": true,
  "allowsQuestionTag": false,
  "displayOrder": 3
}
```

**Response:** `200 OK` + AdminBoardDetailResponse

### DELETE /api/v1/admin/boards/{code}

게시판 삭제

**Response:** `204 No Content`
**Error:** `400 Bad Request` (게시글 존재 시)

### PUT /api/v1/admin/boards/{code}/permissions

게시판 권한 일괄 수정

**Request Body:**
```json
[
  { "role": "ASSOCIATE", "canRead": true, "canWrite": false },
  { "role": "MEMBER", "canRead": true, "canWrite": true },
  { "role": "OPERATOR", "canRead": true, "canWrite": true },
  { "role": "ADMIN", "canRead": true, "canWrite": true }
]
```

**Response:** `200 OK` + List<BoardPermissionResponse>

---

## Summary

| Phase | Task Count | Parallel Tasks | 설명 |
|-------|------------|----------------|------|
| Phase 1: 사전 작업 | 5 | 4 | 도메인 변경, ErrorCode 추가 |
| Phase 2: DTO/예외 | 7 | 7 | 모든 DTO 및 예외 클래스 |
| Phase 3: 서비스 | 4 | 4 | CRUD 비즈니스 로직 |
| Phase 4: 테스트 | 5 | 4 | 단위 + 통합 테스트 |
| Phase 5: 컨트롤러 | 1 | 0 | REST 엔드포인트 |
| Phase 6: 마이그레이션 | 1 | 0 | 스키마 변경 (조건부) |
| **Total** | **23** | **19** |  |

## Notes

- T101 (BoardCode 결정)이 전체 구현 방향을 결정하는 핵심 태스크
- 기존 `BoardController`의 사용자용 API는 변경하지 않음 (관리자 API와 분리)
- `AdminMemberController` 패턴을 따라 일관된 관리자 API 구조 유지
- 게시판 삭제는 hard delete (현재 Board 엔티티에 soft delete 없음)
- 테스트 작성 시 기존 `BoardControllerTest` 패턴 참고
