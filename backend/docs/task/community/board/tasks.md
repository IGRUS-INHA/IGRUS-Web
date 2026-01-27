# Tasks: 게시판 (Board) 백엔드 구현

**Input**: `docs/feature/community/board-spec.md`
**Prerequisites**: board-spec.md (기능 명세서)
**Tech Stack**: Java 21, Spring Boot 3.5.9, Spring Data JPA, MySQL 8.x, Flyway

**Tests**: 테스트 코드 작성 포함 (backend/CLAUDE.md 개발 규칙에 따름)

**Organization**: User Story 기반으로 구성하여 독립적 구현 및 테스트 가능

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 태스크가 속한 User Story (US1, US2, US3)
- 모든 태스크에 정확한 파일 경로 포함

## Path Conventions

- **Backend**: `backend/src/main/java/igrus/web/board/`
- **Tests**: `backend/src/test/java/igrus/web/board/`
- **Migrations**: `backend/src/main/resources/db/migration/`

---

## Phase 1: Setup (프로젝트 구조 설정)

**Purpose**: 게시판 도메인의 기본 패키지 구조 및 Flyway 마이그레이션 설정

- [x] T001 기존 Board, Post, Attachment 등 주석 처리된 도메인 클래스 정리 (삭제 또는 백업) in `backend/src/main/java/igrus/web/board/domain/`
- [x] T002 [P] 게시판 도메인 패키지 구조 생성: `board/domain/`, `board/repository/`, `board/service/`, `board/controller/`, `board/dto/`, `board/exception/`
- [x] T003 [P] Flyway 마이그레이션 버전 확인 및 V6 번호 예약 확인

---

## Phase 2: Foundational (핵심 인프라 - 모든 User Story 선행 조건)

**Purpose**: 모든 User Story가 의존하는 핵심 엔티티 및 인프라 구축

**CRITICAL**: 이 페이즈가 완료되어야 User Story 작업 시작 가능

### 2.1 데이터베이스 스키마

- [x] T004 Flyway 마이그레이션 V6__create_board_tables.sql 작성 in `backend/src/main/resources/db/migration/V6__create_board_tables.sql`
  - boards 테이블: id, code(unique), name, description, allows_anonymous, allows_question_tag, display_order, created_at, updated_at
  - board_permissions 테이블: id, board_id(FK), role(ENUM), can_read, can_write, created_at
  - 초기 데이터: notices, general, insight 게시판 INSERT
  - 게시판별 권한 초기 데이터 INSERT (spec 기반)

### 2.2 핵심 도메인 엔티티

- [x] T005 [P] Board 엔티티 구현 in `backend/src/main/java/igrus/web/board/domain/Board.java`
  - BaseEntity 상속
  - 필드: code, name, description, allowsAnonymous, allowsQuestionTag, displayOrder
  - 정적 팩토리 메서드, 불변성 보장

- [x] T006 [P] BoardPermission 엔티티 구현 in `backend/src/main/java/igrus/web/board/domain/BoardPermission.java`
  - BaseEntity 상속
  - 필드: board(ManyToOne), role(UserRole), canRead, canWrite
  - 복합 유니크 제약: (board_id, role)

- [x] T007 [P] BoardCode enum 구현 in `backend/src/main/java/igrus/web/board/domain/BoardCode.java`
  - NOTICES("notices"), GENERAL("general"), INSIGHT("insight")

### 2.3 Repository

- [x] T008 [P] BoardRepository 인터페이스 구현 in `backend/src/main/java/igrus/web/board/repository/BoardRepository.java`
  - findByCode(String code): Optional<Board>
  - findAllByOrderByDisplayOrderAsc(): List<Board>

- [x] T009 [P] BoardPermissionRepository 인터페이스 구현 in `backend/src/main/java/igrus/web/board/repository/BoardPermissionRepository.java`
  - findByBoardAndRole(Board board, UserRole role): Optional<BoardPermission>
  - findAllByBoard(Board board): List<BoardPermission>

### 2.4 예외 클래스

- [x] T010 [P] BoardNotFoundException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/BoardNotFoundException.java`
- [x] T011 [P] BoardAccessDeniedException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/BoardAccessDeniedException.java`
- [x] T012 [P] ErrorCode에 게시판 관련 에러 코드 추가 in `backend/src/main/java/igrus/web/common/exception/ErrorCode.java`
  - BOARD_NOT_FOUND, BOARD_ACCESS_DENIED, BOARD_READ_DENIED, BOARD_WRITE_DENIED

### 2.5 권한 검증 서비스

- [x] T013 BoardPermissionService 구현 in `backend/src/main/java/igrus/web/board/service/BoardPermissionService.java`
  - canRead(Board board, UserRole role): boolean
  - canWrite(Board board, UserRole role): boolean
  - checkReadPermission(Board board, UserRole role): void (예외 발생)
  - checkWritePermission(Board board, UserRole role): void (예외 발생)

**Checkpoint**: Foundation 완료 - User Story 구현 시작 가능

---

## Phase 3: User Story 1 - 게시판 목록 조회 (Priority: P1) MVP

**Goal**: 정회원이 커뮤니티에 접근하여 게시판 목록을 확인하고 원하는 게시판을 선택할 수 있다

**Independent Test**: 로그인한 회원이 게시판 목록 API 호출 시 권한에 맞는 게시판 목록이 반환되면 통과

### Acceptance Criteria

1. 정회원 로그인 상태에서 커뮤니티 메뉴 접근 시 공지사항, 자유게시판, 정보공유 게시판 목록 표시
2. 준회원 로그인 상태에서 접근 시 읽기 권한이 있는 게시판만 표시
3. 각 게시판의 이름, 설명, 접근 가능 여부 정보 포함

### Tests for User Story 1

- [x] T014 [P] [US1] BoardService 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/service/BoardServiceTest.java`
  - 정회원이 게시판 목록 조회 시 3개 게시판 반환
  - 준회원이 게시판 목록 조회 시 읽기 권한 있는 게시판만 반환
  - 존재하지 않는 게시판 코드 조회 시 BoardNotFoundException 발생

- [x] T015 [P] [US1] BoardController 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/controller/BoardControllerTest.java`
  - GET /api/v1/boards - 인증된 사용자가 게시판 목록 조회 성공
  - GET /api/v1/boards - 비인증 사용자가 접근 시 401 Unauthorized
  - GET /api/v1/boards/{code} - 유효한 게시판 코드로 상세 조회 성공
  - GET /api/v1/boards/{code} - 존재하지 않는 코드로 조회 시 404 Not Found

### Implementation for User Story 1

- [x] T016 [US1] BoardService 구현 in `backend/src/main/java/igrus/web/board/service/BoardService.java`
  - getBoardList(UserRole role): List<BoardListResponse>
  - getBoardByCode(String code): BoardDetailResponse
  - getBoardEntity(String code): Board (내부용)

- [x] T017 [P] [US1] BoardListResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/BoardListResponse.java`
  - 필드: code, name, description, canRead, canWrite

- [x] T018 [P] [US1] BoardDetailResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/BoardDetailResponse.java`
  - 필드: code, name, description, allowsAnonymous, allowsQuestionTag, canRead, canWrite

- [x] T019 [US1] BoardController 구현 in `backend/src/main/java/igrus/web/board/controller/BoardController.java`
  - GET /api/v1/boards - 게시판 목록 조회
  - GET /api/v1/boards/{code} - 게시판 상세 조회
  - Swagger 어노테이션 (@Operation, @ApiResponse) 추가

- [x] T020 [US1] SecurityConfig에 게시판 API 경로 권한 설정 추가 in `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java`
  - /api/v1/boards/** - 인증 필요 (ASSOCIATE 이상) - 기존 anyRequest().authenticated() 설정으로 처리됨

**Checkpoint**: User Story 1 완료 - 게시판 목록 조회 기능 독립적으로 테스트 가능

---

## Phase 4: User Story 2 - 게시판 권한 관리 (Priority: P1)

**Goal**: 시스템이 회원 등급에 따라 게시판별 읽기/쓰기 권한을 자동으로 관리한다

**Independent Test**: 각 회원 등급별로 게시판 접근 시 정의된 권한이 올바르게 적용되는지 확인

### Acceptance Criteria

1. 준회원이 공지사항 접근 시 읽기만 가능 (준회원 공개 글만)
2. 준회원이 자유게시판/정보공유 접근 시 접근 거부
3. 정회원이 모든 게시판 읽기 가능, 공지사항 쓰기 불가
4. OPERATOR 이상이 공지사항 쓰기 가능

### Tests for User Story 2

- [x] T021 [P] [US2] BoardPermissionService 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/service/BoardPermissionServiceTest.java`
  - 준회원이 공지사항 읽기 권한 확인 - true
  - 준회원이 자유게시판 읽기 권한 확인 - false
  - 정회원이 공지사항 쓰기 권한 확인 - false
  - OPERATOR가 공지사항 쓰기 권한 확인 - true
  - 권한 없는 접근 시 BoardAccessDeniedException 발생

- [x] T022 [P] [US2] 권한 검증 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/integration/BoardPermissionIntegrationTest.java`
  - 준회원 토큰으로 자유게시판 접근 시 403 Forbidden
  - 정회원 토큰으로 모든 게시판 접근 성공
  - OPERATOR 토큰으로 공지사항 쓰기 권한 확인

### Implementation for User Story 2

- [x] T023 [US2] BoardPermissionService 권한 검증 로직 강화 in `backend/src/main/java/igrus/web/board/service/BoardPermissionService.java`
  - 게시판별 역할 권한 매트릭스 적용
  - 예외 메시지에 필요한 권한 정보 포함

- [x] T024 [P] [US2] BoardWriteDeniedException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/BoardWriteDeniedException.java`
- [x] T025 [P] [US2] BoardReadDeniedException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/BoardReadDeniedException.java`

- [x] T026 [US2] BoardController에 권한 검증 응답 처리 추가 in `backend/src/main/java/igrus/web/board/controller/BoardController.java`
  - 권한 부족 시 적절한 HTTP 상태 코드 및 메시지 반환
  - 준회원 접근 제한 안내 메시지 ("정회원 승인 후 이용 가능합니다")

- [x] T027 [US2] GlobalExceptionHandler에 게시판 예외 핸들러 추가 in `backend/src/main/java/igrus/web/common/exception/GlobalExceptionHandler.java`
  - BoardNotFoundException → 404 Not Found (CustomBaseException 핸들러로 처리)
  - BoardAccessDeniedException → 403 Forbidden (CustomBaseException 핸들러로 처리)
  - BoardReadDeniedException → 403 Forbidden (CustomBaseException 핸들러로 처리)
  - BoardWriteDeniedException → 403 Forbidden (CustomBaseException 핸들러로 처리)

**Checkpoint**: User Story 2 완료 - 권한 관리 기능 독립적으로 테스트 가능

---

## Phase 5: User Story 3 - 공지사항 준회원 공개 설정 (Priority: P2)

**Goal**: 운영자가 공지사항 작성 시 준회원에게 공개 여부를 선택하여 중요 공지를 준회원에게도 전달한다

**Independent Test**: 준회원 공개 옵션이 선택된 공지사항만 준회원에게 표시되는지 확인

### Acceptance Criteria

1. OPERATOR가 공지사항 작성 시 "준회원에게 공개" 옵션 선택 가능
2. 준회원 공개로 설정된 공지는 준회원에게 표시
3. 준회원 공개 미설정 공지는 준회원 목록에서 제외

### Tests for User Story 3

- [x] T028 [SKIPPED] [US3] BoardVisibilityService 테스트 - PostService에서 구현됨
  - 별도 서비스 생성 불필요: PostService가 준회원 필터링 완벽 구현
  - PostServiceTest에서 준회원 공개 로직 테스트 포함 (T044)

### Implementation for User Story 3

- [x] T029 [SKIPPED] [US3] BoardVisibilityService - PostService에서 구현됨
  - PostService.getPostList()에서 준회원 필터링 구현 (line 281-299)
  - PostService.getPostDetail()에서 접근 권한 검증 구현 (line 325-331)

- [x] T030 [US3] Post 준회원 공개 필터링 구현 확인 완료
  - PostService.getPostList(): 준회원이 공지사항 조회 시 isVisibleToAssociate=true 게시글만 반환
  - PostService.getPostDetail(): 준회원이 비공개 공지사항 접근 시 PostNotFoundException 발생
  - PostRepository: findVisibleToAssociateByBoard(), searchVisibleToAssociateByTitleOrContent() 메서드 구현

**Checkpoint**: User Story 3 완료 - 준회원 공개 설정 기능 PostService에서 완전 구현됨

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 전체 기능에 걸친 개선 및 품질 향상

- [x] T031 [DEFERRED] Rate Limiting 설정 - API Gateway/nginx 레벨에서 처리 권장
  - 사용자 레벨 제한: PostRateLimitService로 시간당 20회 제한 이미 적용됨
  - IP 기반 글로벌 제한: API Gateway 또는 nginx에서 처리하는 것이 일반적

- [x] T032 [P] Swagger API 문서 검토 완료 in `backend/src/main/java/igrus/web/community/board/controller/BoardController.java`
  - @Operation, @ApiResponse 어노테이션 적용됨
  - 요청/응답 스키마 문서화 완료

- [x] T033 코드 리뷰 및 리팩토링 완료
  - SOLID 원칙 준수 확인
  - N+1 쿼리 문제: @EntityGraph로 해결됨 (PostRepository)
  - 보안 취약점 점검 완료

- [x] T034 전체 테스트 실행 및 커버리지 확인 (2026-01-27)
  - 모든 단위 테스트 통과 확인 ✅
  - 모든 통합 테스트 통과 확인 ✅
  - Board/Post 관련 167개 테스트 전체 통과

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 의존성 없음 - 즉시 시작 가능
- **Phase 2 (Foundational)**: Phase 1 완료 필요 - 모든 User Story 블로킹
- **Phase 3 (US1)**: Phase 2 완료 필요
- **Phase 4 (US2)**: Phase 2 완료 필요, Phase 3와 병렬 가능
- **Phase 5 (US3)**: Phase 2 완료 필요, Phase 3/4와 병렬 가능 (Post 연계 필요)
- **Phase 6 (Polish)**: 모든 User Story 완료 필요

### User Story Dependencies

- **User Story 1 (P1)**: Phase 2 완료 후 시작 - 독립적
- **User Story 2 (P1)**: Phase 2 완료 후 시작 - 독립적, US1과 병렬 가능
- **User Story 3 (P2)**: Phase 2 완료 후 시작 - Post 기능과 연계 필요

### Within Each User Story

- 테스트 작성 후 실패 확인 → 구현 → 테스트 통과 순서
- DTO → Service → Controller 순서
- 핵심 기능 → 부가 기능 순서

### Parallel Opportunities

- Phase 2의 T005, T006, T007 병렬 실행 가능
- Phase 2의 T008, T009 병렬 실행 가능
- Phase 2의 T010, T011, T012 병렬 실행 가능
- Phase 3의 T014, T015 테스트 병렬 작성 가능
- Phase 3의 T017, T018 DTO 병렬 구현 가능
- Phase 4의 T021, T022 테스트 병렬 작성 가능
- Phase 4의 T024, T025 예외 클래스 병렬 구현 가능

---

## Parallel Example: Phase 2 Foundation

```bash
# 엔티티 병렬 구현
Task: "Board 엔티티 구현 in backend/.../Board.java"
Task: "BoardPermission 엔티티 구현 in backend/.../BoardPermission.java"
Task: "BoardCode enum 구현 in backend/.../BoardCode.java"

# Repository 병렬 구현
Task: "BoardRepository 인터페이스 구현 in backend/.../BoardRepository.java"
Task: "BoardPermissionRepository 인터페이스 구현 in backend/.../BoardPermissionRepository.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup 완료
2. Phase 2: Foundational 완료 (CRITICAL)
3. Phase 3: User Story 1 완료
4. **STOP and VALIDATE**: 게시판 목록 조회 기능 독립 테스트
5. 배포/데모 가능

### Incremental Delivery

1. Setup + Foundational → Foundation 완료
2. User Story 1 → 테스트 → 배포/데모 (MVP!)
3. User Story 2 → 테스트 → 배포/데모
4. User Story 3 → Post 연계 후 테스트 → 배포/데모
5. 각 스토리가 이전 스토리를 깨뜨리지 않고 가치 추가

### Related Specs

이 태스크는 Board(게시판) 기능만 다룹니다. 다음 관련 기능은 별도 태스크로 구현:
- `post-spec.md` - 게시글 CRUD, 익명 게시글, 질문 태그
- `comment-spec.md` - 댓글/대댓글 기능
- `like-bookmark-spec.md` - 좋아요/북마크 기능

---

## Notes

- [P] 태스크 = 다른 파일, 의존성 없음
- [Story] 라벨로 User Story 추적 가능
- 각 User Story는 독립적으로 완료 및 테스트 가능
- 테스트 실패 확인 후 구현 시작
- 태스크 또는 논리적 그룹 완료 후 커밋
- Checkpoint에서 스토리 독립 검증 가능
- 모호한 태스크, 같은 파일 충돌, 독립성 깨는 크로스-스토리 의존성 지양

---

## Summary

| Phase | Task Count | Parallel Tasks |
|-------|------------|----------------|
| Phase 1: Setup | 3 | 2 |
| Phase 2: Foundational | 10 | 8 |
| Phase 3: User Story 1 | 7 | 4 |
| Phase 4: User Story 2 | 7 | 4 |
| Phase 5: User Story 3 | 3 | 1 |
| Phase 6: Polish | 4 | 2 |
| **Total** | **34** | **21** |

**MVP Scope**: Phase 1 + Phase 2 + Phase 3 (User Story 1) = 20 tasks
**Independent Test per Story**: 각 User Story별 독립 테스트 가능