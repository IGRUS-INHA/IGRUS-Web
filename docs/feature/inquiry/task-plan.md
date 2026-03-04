# 문의 답변 없이 상태 변경 불가 로직 및 상태 즉시 반영 처리 작업 계획

## 개요

- **기능 설명**: 문의(Inquiry) 도메인에서 답변이 없는 상태에서 상태 변경을 차단하는 비즈니스 로직을 추가하고, 프론트엔드에서 상태 변경 후 상세 페이지에 즉시 반영되지 않는 버그를 수정한다.
- **관련 문서**
  - 검증 기준서: [`docs/criteria/inquiry-verification-criteria.md`](../../criteria/inquiry-verification-criteria.md)
  - 관련 이슈: [#333 문의 답변 없이 상태 변경 불가 로직 및 상태 즉시 반영 처리](https://github.com/IGRUS-INHA/IGRUS-Web/issues/333)
- **작성일**: 2026-03-03
- **최종 수정일**: 2026-03-03

---

## 현재 문제 분석

### 백엔드 문제

`UpdateInquiryStatusService.updateInquiryStatus()`는 답변 존재 여부를 검증하지 않고 `inquiry.changeStatus(request.getStatus())`만 호출한다. 따라서 답변이 없는 문의도 `PENDING -> IN_PROGRESS -> COMPLETED` 등의 상태 변경이 가능하다.

**설계 결정 — 답변 없이 허용/차단할 전이 범위**:

검증 기준 INQ-INV-08에 의하면 "답변 작성 시 문의 상태가 자동으로 COMPLETED로 전이된다". 이는 COMPLETED 상태가 답변 완료와 연동되어야 함을 의미한다. 따라서:

| 전이 | 답변 없이 허용 | 근거 |
|------|:---:|------|
| PENDING -> IN_PROGRESS | **O** | 처리 시작은 답변 이전에 가능 (상태 모델 2-1) |
| IN_PROGRESS -> PENDING | **O** | 되돌리기는 답변과 무관 (상태 모델 2-1) |
| PENDING -> COMPLETED | **X** | 답변 없이 완료 처리는 INQ-INV-08 위반 |
| IN_PROGRESS -> COMPLETED | **X** | 답변 없이 완료 처리는 INQ-INV-08 위반 |
| 동일 상태 -> 동일 상태 | **O** | 멱등성 보장 (상태 모델 2-1) |

즉, **답변 없이 COMPLETED로 수동 전이하는 것만 차단**한다.

### 프론트엔드 문제

`InquiriesTab.tsx`의 상태 변경 mutation(`useUpdateInquiryStatus`) 성공 시 `invalidate()`가 목록 쿼리(`/api/v1/inquiries`)만 무효화하고, 상세 조회 쿼리(`/api/v1/inquiries/${selectedId}`)를 무효화하지 않는다. 그 결과 상세 패널의 상태 값이 갱신되지 않는다.

---

## 작업 목록

### 1. 백엔드 -- 답변 없는 문의 COMPLETED 전이 차단

#### TASK-001: InquiryErrorCode에 새로운 에러코드 추가

- **작업 ID**: TASK-001
- **작업명**: `INQUIRY_REPLY_REQUIRED_FOR_COMPLETION` 에러코드 추가
- **설명**: `InquiryErrorCode` enum에 답변 없이 COMPLETED 전이를 시도할 때 반환할 에러코드를 추가한다.
  - 코드명: `INQUIRY_REPLY_REQUIRED_FOR_COMPLETION`
  - HTTP 상태: `400` (Bad Request)
  - 메시지: `"답변이 없는 문의는 완료 상태로 변경할 수 없습니다"`
- **수정 파일**: `backend/src/main/java/igrus/web/inquiry/exception/InquiryErrorCode.java`
- **관련 검증 기준**: INQ-INV-08 (답변 작성 시 상태 자동 완료)
- **관련 테스트 케이스**: 없음 (테스트 케이스 문서 미작성)
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-002: 답변 필수 검증 커스텀 예외 클래스 생성

- **작업 ID**: TASK-002
- **작업명**: `InquiryReplyRequiredForCompletionException` 예외 클래스 생성
- **설명**: 기존 프로젝트 패턴에 따라 `CustomBaseException`을 상속하는 커스텀 예외 클래스를 생성한다.
  - 패키지: `igrus.web.inquiry.exception`
  - `InquiryErrorCode.INQUIRY_REPLY_REQUIRED_FOR_COMPLETION`을 사용
  - 기존 `InquiryAlreadyRepliedException` 등의 패턴을 준수
- **수정 파일**: `backend/src/main/java/igrus/web/inquiry/exception/InquiryReplyRequiredForCompletionException.java` (신규)
- **관련 검증 기준**: INQ-INV-08
- **관련 테스트 케이스**: 없음
- **선행 작업**: TASK-001
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-003: UpdateInquiryStatusService에 답변 존재 여부 검증 로직 추가

- **작업 ID**: TASK-003
- **작업명**: 답변 없는 문의의 COMPLETED 전이 차단 로직 구현
- **설명**: `UpdateInquiryStatusService.updateInquiryStatus()` 메서드에서 다음 검증 로직을 추가한다:
  1. 요청 상태가 `COMPLETED`인 경우에만 검증 수행
  2. 현재 상태가 이미 `COMPLETED`인 경우 (멱등성 전이)는 검증 건너뜀
  3. `inquiry.hasReply()`가 `false`이면 `InquiryReplyRequiredForCompletionException` 발생
  - 검증은 FSM 전이 검증(`changeStatus()`) 이전에 수행하여, 유효하지 않은 전이와 답변 누락을 명확히 구분
  - **주의**: `Inquiry.reply`는 `@OneToOne(mappedBy)` lazy 관계이므로 `hasReply()` 호출 시 추가 쿼리가 발생할 수 있음. `findById`로 조회한 엔티티에서 `reply` 접근이 프록시 초기화를 트리거하므로 성능 이슈는 무시할 수준
- **수정 파일**: `backend/src/main/java/igrus/web/inquiry/service/manage/UpdateInquiryStatusService.java`
- **관련 검증 기준**: INQ-INV-08 (답변 작성 시 상태 자동 완료), 상태 모델 2-1 (PENDING/IN_PROGRESS -> COMPLETED 전이)
- **관련 테스트 케이스**: 없음
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 중

**검증 위치 결정 근거 — 서비스 레벨 선택 이유**:

답변 존재 여부 검증은 다음 세 위치 중 **서비스 레벨**이 가장 적합하다:

| 검증 위치 | 장점 | 단점 | 채택 여부 |
|-----------|------|------|:---------:|
| **도메인 엔티티** (`Inquiry.changeStatus()`) | 도메인 규칙을 엔티티 내부에 캡슐화 | `changeStatus()`는 `CreateInquiryReplyService`에서도 `inquiry.complete()` -> `changeStatus(COMPLETED)`로 호출되며, 이 경우 답변은 직전에 설정되므로 hasReply()가 항상 true이지만 불필요한 검증이 매번 수행됨. 또한 `changeStatus()`가 이미 FSM 전이 검증이라는 단일 책임을 갖고 있어 답변 검증까지 추가하면 SRP 위반 | X |
| **서비스 레벨** (`UpdateInquiryStatusService`) | "관리자가 수동으로 상태를 변경"하는 유스케이스에만 검증 적용. `CreateInquiryReplyService`는 자체적으로 답변 설정 후 `complete()`를 호출하므로 검증이 불필요한 경로를 자연스럽게 우회. 기존 `changeStatus()`의 단일 책임 보존 | 엔티티가 아닌 서비스에 비즈니스 규칙이 위치하여 다른 서비스에서 우회 가능 (현재는 `UpdateInquiryStatusService`만이 수동 상태 변경의 유일한 진입점) | **O** |
| **컨트롤러 레벨** | 가장 상위에서 차단 | 서비스 계층에서 직접 호출 시 검증이 우회됨. 컨트롤러는 입력 변환과 HTTP 응답 매핑에 집중해야 하므로 비즈니스 검증을 두기에 부적합 | X |

---

### 2. OpenAPI 스펙 -- 400 에러 응답 정의 추가

#### TASK-008: updateInquiryStatus 엔드포인트에 400 응답 스키마 추가

- **작업 ID**: TASK-008
- **작업명**: OpenAPI 스펙의 `updateInquiryStatus` 엔드포인트에 400 응답 정의 추가
- **설명**: `openapi/paths/inquiries.yaml`의 `inquiriesByIdStatus` 경로에 400 응답을 추가한다. 현재 이 엔드포인트에는 200, 401, 403, 404 응답만 정의되어 있으나, 다음 두 가지 에러가 HTTP 400을 반환하므로 스펙에 반영이 필요하다:
  1. 기존 `INVALID_STATUS_TRANSITION` — 허용되지 않는 상태 전이 시도 (예: COMPLETED -> PENDING)
  2. 신규 `INQUIRY_REPLY_REQUIRED_FOR_COMPLETION` — 답변 없이 COMPLETED 전이 시도
  - 기존 프로젝트의 400 응답 정의 패턴을 따른다. `openapi/paths/admin.yaml`에서 확인되는 패턴: `'400': description: {에러 사유 나열}` 형식
  - 추가할 내용:
    ```yaml
    '400':
      description: 잘못된 상태 전이 (유효하지 않은 전이 / 답변 없이 완료 처리 시도)
    ```
  - 스펙 변경 후 Orval 재생성이 필요하므로 TASK-004, TASK-005보다 선행되어야 한다
- **수정 파일**: `openapi/paths/inquiries.yaml`
- **관련 검증 기준**: INQ-INV-07 (COMPLETED 종단 상태), INQ-INV-08 (답변 필수)
- **관련 테스트 케이스**: 없음
- **선행 작업**: TASK-001
- **구현 범위**: both (OpenAPI 스펙 + Orval 재생성)
- **예상 난이도**: 하

---

### 3. 프론트엔드 -- 상태 변경 후 상세 페이지 즉시 반영

#### TASK-004: InquiriesTab 상태 변경 mutation의 캐시 무효화 범위 확장

- **작업 ID**: TASK-004
- **작업명**: 상태 변경 성공 시 상세 조회 쿼리 캐시 무효화 추가
- **설명**: `InquiriesTab.tsx`에서 `useUpdateInquiryStatus` mutation의 `onSuccess` 콜백에 상세 조회 쿼리 캐시 무효화를 추가한다.
  - 현재 코드 (95-105행): `onSuccess`에서 `invalidate()`만 호출 -> 목록 쿼리(`["/api/v1/inquiries"]`)와 대시보드 쿼리(`["/api/v1/admin/dashboard"]`)만 무효화
  - 수정 후: `onSuccess`에서 `selectedId`가 있으면 추가로 상세 쿼리도 무효화
  - **정확한 queryKey 패턴**: `` [`/api/v1/inquiries/${selectedId}`] `` (배열의 단일 문자열 요소)
    - 이 패턴은 91행의 `useGetInquiryDetail(selectedId!)` 훅이 내부적으로 사용하는 queryKey와 동일
    - `createReply`(113-116행), `updateReply`(128-131행), `createMemo`(144-147행) mutation의 `onSuccess`에서 이미 동일한 패턴으로 상세 쿼리를 무효화하고 있으므로, 동일한 코드를 적용
  - 수정 코드:
    ```typescript
    onSuccess: () => {
      addToast({ type: "success", message: "상태 변경 완료" });
      invalidate();
      if (selectedId)
        queryClient.invalidateQueries({
          queryKey: [`/api/v1/inquiries/${selectedId}`],
        });
    },
    ```
- **수정 파일**: `frontend/src/pages/admin/tabs/InquiriesTab.tsx`
- **관련 검증 기준**: 해당 없음 (프론트엔드 버그 수정)
- **관련 테스트 케이스**: 없음
- **선행 작업**: TASK-008 (OpenAPI 스펙 변경 및 Orval 재생성 완료 후)
- **구현 범위**: frontend
- **예상 난이도**: 하

#### TASK-005: InquiriesTab 상태 변경 실패 시 에러 메시지 구체화

- **작업 ID**: TASK-005
- **작업명**: 상태 변경 실패 시 서버 에러 메시지를 토스트에 표시
- **설명**: `useUpdateInquiryStatus` mutation의 `onError` 콜백에서 서버로부터 전달된 에러 메시지(예: "답변이 없는 문의는 완료 상태로 변경할 수 없습니다")를 파싱하여 토스트에 표시한다.
  - 현재 코드 (101-103행): `onError`에서 고정 메시지 "상태 변경 실패"만 표시
  - 수정 후: 에러 응답의 `message` 필드를 추출하여 표시. 추출 실패 시 기존 고정 메시지를 fallback으로 사용
  - 에러 응답 구조 (OpenAPI `ErrorResponse` 스키마 기준): `{ status: number, code: string, message: string, timestamp: string }`
  - Axios 에러에서 메시지 추출: `error.response?.data?.message ?? "상태 변경 실패"`
  - 이를 통해 사용자는 "답변이 없는 문의는 완료 상태로 변경할 수 없습니다"와 같은 구체적인 사유를 확인 가능
- **수정 파일**: `frontend/src/pages/admin/tabs/InquiriesTab.tsx`
- **관련 검증 기준**: 해당 없음 (UX 개선)
- **관련 테스트 케이스**: 없음
- **선행 작업**: TASK-003 (백엔드 에러 응답 구현 완료 후), TASK-008 (OpenAPI 스펙 반영)
- **구현 범위**: frontend
- **예상 난이도**: 하

---

### 4. 테스트 코드 작성

#### TASK-006: 답변 없는 문의의 COMPLETED 전이 차단 통합 테스트 및 기존 테스트 수정

- **작업 ID**: TASK-006
- **작업명**: `UpdateInquiryStatusService` 통합 테스트에 답변 필수 검증 케이스 추가 및 기존 테스트 수정
- **설명**: `UpdateInquiryStatusServiceTest` 클래스에 다음 작업을 수행한다:

  **A. 기존 테스트 수정 (5건)**:

  TASK-003 적용 후 답변 없이 COMPLETED 전이를 수행하는 기존 테스트가 실패한다. 각 테스트의 구체적인 수정 방향:

  | 기존 테스트 ID | 현재 동작 | 수정 방향 | 상세 |
  |---------------|----------|----------|------|
  | INQ-A-021 (`updateStatus_PendingToCompleted_Success`) | 답변 없이 PENDING -> COMPLETED 성공 | **테스트 의도를 "답변 있는 문의의 수동 COMPLETED 전이 성공"으로 변경** | `CreateInquiryReplyService.createReply()`로 답변 작성 -> 자동 COMPLETED -> 이미 COMPLETED이므로 추가적으로 PENDING -> COMPLETED 직접 테스트는 의미 없음. 대신 답변 있는 IN_PROGRESS 상태 문의에서 COMPLETED 전이 성공을 검증하도록 변경. `@Autowired CreateInquiryReplyService` 추가 필요 |
  | INQ-A-023 (`updateStatus_InProgressToCompleted_Success`) | 답변 없이 IN_PROGRESS -> COMPLETED 성공 | **답변 작성 후 COMPLETED 전이로 변경** | 먼저 PENDING -> IN_PROGRESS 전이 후, `CreateInquiryReplyService.createReply()`로 답변 작성 (이때 자동 COMPLETED). 그러나 답변 작성 시 자동 complete()가 호출되어 이미 COMPLETED 상태가 되므로, 사실상 멱등 전이 테스트가 됨. 대안: (1) 답변은 작성하되 `complete()`를 호출하기 전 상태를 직접 변경하는 것은 서비스 레벨에서 불가. (2) 따라서 이 테스트는 "답변이 있는 문의에 대해 COMPLETED 수동 전이 시 멱등성 유지"를 검증하도록 명칭/의도를 변경 |
  | INQ-A-025 (`updateStatus_CompletedToPending_ThrowsException`) | setup에서 답변 없이 `changeStatus(COMPLETED)` 호출 후 COMPLETED -> PENDING 금지 전이 검증 | **setup을 답변 작성 방식으로 변경** | 현재 `changeStatus(createResponse.getId(), InquiryStatus.COMPLETED)`로 답변 없이 COMPLETED에 도달하는데, TASK-003 이후 이 호출 자체가 `InquiryReplyRequiredForCompletionException`으로 차단됨. `CreateInquiryReplyService.createReply()`를 호출하여 답변 작성 -> 자동 COMPLETED 도달 후, COMPLETED -> PENDING 전이 시 `InvalidStatusTransitionException` 발생을 검증하도록 변경 |
  | INQ-A-026 (`updateStatus_CompletedToInProgress_ThrowsException`) | setup에서 답변 없이 `changeStatus(COMPLETED)` 호출 후 COMPLETED -> IN_PROGRESS 금지 전이 검증 | **setup을 답변 작성 방식으로 변경** | INQ-A-025와 동일한 문제. `CreateInquiryReplyService.createReply()`를 호출하여 답변 작성 -> 자동 COMPLETED 도달 후, COMPLETED -> IN_PROGRESS 전이 시 `InvalidStatusTransitionException` 발생을 검증하도록 변경 |
  | INQ-A-027 (`updateStatus_CompletedToCompleted_Idempotent`) | 답변 없이 COMPLETED 도달 후 멱등 전이 | **답변 작성 -> 자동 COMPLETED -> 멱등 COMPLETED 전이로 시나리오 변경** | (1) 문의 생성 (2) `CreateInquiryReplyService.createReply()`로 답변 작성 -> 자동 COMPLETED (3) COMPLETED 수동 전이 -> 멱등 성공 확인. `@Autowired CreateInquiryReplyService` 사용 |

  **참고**: INQ-A-025/026은 `ForbiddenTransitionTest` 중첩 클래스 내에 위치하며, 테스트의 검증 대상(COMPLETED에서 다른 상태로의 전이 차단)은 변경되지 않는다. setup 방식만 답변 없이 직접 `changeStatus(COMPLETED)`를 호출하는 것에서 `CreateInquiryReplyService.createReply()`를 통해 답변을 작성하여 자동으로 COMPLETED에 도달하는 방식으로 변경된다.

  **B. 신규 테스트 추가 (5건)**:

  1. **답변 없이 PENDING -> COMPLETED 시도 시 예외 발생**: 답변이 없는 PENDING 상태 문의에 COMPLETED 전이 요청 시 `InquiryReplyRequiredForCompletionException` 발생 확인
  2. **답변 없이 IN_PROGRESS -> COMPLETED 시도 시 예외 발생**: 답변이 없는 IN_PROGRESS 상태 문의에 COMPLETED 전이 요청 시 동일 예외 발생 확인
  3. **답변 있는 문의의 COMPLETED 전이 성공**: 답변이 작성된 문의에서 수동 COMPLETED 전이가 정상 동작하는지 확인 (이 경우 `CreateInquiryReplyService`가 이미 complete()를 호출하므로 실질적으로 COMPLETED -> COMPLETED 멱등성 전이가 됨)
  4. **답변 없이 PENDING -> IN_PROGRESS 전이는 정상 동작**: 답변 유무와 무관하게 IN_PROGRESS 전이는 허용됨을 확인 (기존 테스트 INQ-A-020이 이미 커버하지만, 명시적 문서화 차원)
  5. **답변 없이 COMPLETED -> COMPLETED 멱등성 전이는 정상 동작**: 이미 COMPLETED 상태인 문의에 동일 상태 전이는 답변 검증을 건너뜀 (TASK-003의 멱등성 전이 예외 조건 검증). 단, 이 테스트는 답변 작성 -> 자동 COMPLETED -> 멱등 전이 시나리오로 구성 (답변 없이 COMPLETED에 도달하는 것 자체가 차단되므로)

  - 기존 `InquiryTestFixture`의 `createGuestInquiryRequest()`, `updateStatusRequest()`, `createReplyRequest()` 활용
  - 답변 작성을 위해 `CreateInquiryReplyService`를 `@Autowired`로 주입
- **수정 파일**: `backend/src/test/java/igrus/web/inquiry/service/manage/UpdateInquiryStatusServiceTest.java`
- **관련 검증 기준**: INQ-INV-07 (COMPLETED 종단 상태), INQ-INV-08 (답변 작성 시 상태 자동 완료), 상태 모델 2-1
- **관련 테스트 케이스**: 없음 (신규 테스트 케이스)
- **선행 작업**: TASK-003
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-007: Inquiry 도메인 단위 테스트에 hasReply 검증 보강 (선택)

- **작업 ID**: TASK-007
- **작업명**: `InquiryTest`에 `hasReply()` 상태에 따른 동작 검증 추가
- **설명**: `InquiryTest` 클래스에 다음 단위 테스트를 추가한다. 이 작업은 기존 테스트가 `hasReply()`의 기본적인 동작을 간접적으로 커버하고 있으므로 선택 사항이다:
  1. **reply 미설정 시 hasReply()는 false**: 신규 생성된 문의의 `hasReply()`가 false를 반환하는지 확인
  2. **reply 설정 후 hasReply()는 true**: `setReply()` 호출 후 `hasReply()`가 true를 반환하는지 확인
- **수정 파일**: `backend/src/test/java/igrus/web/inquiry/domain/InquiryTest.java`
- **관련 검증 기준**: INQ-INV-03 (문의당 답변 최대 1건), INQ-INV-08
- **관련 테스트 케이스**: 없음
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 5. 문서화 -- 검증 기준서 및 관련 문서 업데이트

#### TASK-009: 검증 기준서 INQ-INV-08 섹션 업데이트

- **작업 ID**: TASK-009
- **작업명**: 검증 기준서의 INQ-INV-08에 답변 필수 규칙의 역방향 제약 반영
- **설명**: `docs/criteria/inquiry-verification-criteria.md`의 INQ-INV-08 섹션을 업데이트하여, 답변 작성 시 자동 완료뿐만 아니라 "답변 없이 COMPLETED로 수동 전이 시 차단"이라는 역방향 제약을 명시한다. 현재 INQ-INV-08은 "답변 작성 시 상태가 자동으로 COMPLETED로 전이된다"만 기술하고 있으나, 이번 작업으로 다음 내용이 추가되어야 한다:
  - **사전조건 추가**: COMPLETED로의 수동 상태 전이 시 답변이 존재해야 함 (`inquiry.hasReply() == true`)
  - **위반 시**: `InquiryReplyRequiredForCompletionException` (HTTP 400)
  - **관련 코드**: `UpdateInquiryStatusService` (검증 로직 위치)
  - **검증 방법**: 답변 없는 문의에서 수동 COMPLETED 전이 시 400 에러 반환 확인
- **수정 파일**: `docs/criteria/inquiry-verification-criteria.md`
- **관련 검증 기준**: INQ-INV-08
- **선행 작업**: TASK-003
- **구현 범위**: docs
- **예상 난이도**: 하

#### TASK-010: 검증 기준서 상태 모델 2-1 섹션 업데이트

- **작업 ID**: TASK-010
- **작업명**: 상태 전이 테이블에 답변 필수 사전조건 반영
- **설명**: `docs/criteria/inquiry-verification-criteria.md`의 상태 모델 2-1 섹션의 전이 테이블을 업데이트한다. 현재 `PENDING -> COMPLETED`와 `IN_PROGRESS -> COMPLETED` 전이의 사전조건에 "OPERATOR/ADMIN 권한"만 명시되어 있으나, **"답변 존재 (수동 전이 시)"** 사전조건을 추가해야 한다:
  - `PENDING -> COMPLETED` 행: 사전조건에 `+ 답변 존재 (수동 전이 시)` 추가
  - `IN_PROGRESS -> COMPLETED` 행: 사전조건에 `+ 답변 존재 (수동 전이 시)` 추가
  - 금지된 전이 테이블은 변경 불필요 (COMPLETED -> 다른 상태 금지는 기존과 동일)
- **수정 파일**: `docs/criteria/inquiry-verification-criteria.md`
- **관련 검증 기준**: 상태 모델 2-1
- **선행 작업**: TASK-009
- **구현 범위**: docs
- **예상 난이도**: 하

#### TASK-011: 검증 기준서 테스트-검증 항목 매핑 업데이트

- **작업 ID**: TASK-011
- **작업명**: 8-2 섹션의 불변조건/상태 전이 커버리지 테이블 업데이트
- **설명**: `docs/criteria/inquiry-verification-criteria.md`의 8-2 섹션을 업데이트하여, TASK-006에서 추가/수정된 테스트 케이스를 커버리지 매핑에 반영한다:
  - **불변조건 커버리지 테이블**:
    - INQ-INV-08 행: 기존 "커버됨" 유지, 추가 커버 테스트로 TASK-006의 신규 테스트 (답변 없이 COMPLETED 차단) 기재
  - **상태 전이 커버리지 테이블**:
    - `PENDING -> COMPLETED` 행: TASK-006 수정 테스트 반영 (답변 후 전이)
    - `IN_PROGRESS -> COMPLETED` 행: TASK-006 수정 테스트 반영
  - `UpdateInquiryStatusServiceTest` 테스트 수 업데이트: 7개 -> 12개 (기존 7 + 신규 5)
- **수정 파일**: `docs/criteria/inquiry-verification-criteria.md`
- **관련 검증 기준**: 8. 테스트 전략
- **선행 작업**: TASK-006
- **구현 범위**: docs
- **예상 난이도**: 하

---

## 작업 순서 및 의존성

```
TASK-001 (에러코드 추가)
    |
    ├──────────────────────────────────┐
    v                                  v
TASK-002 (커스텀 예외 클래스)     TASK-008 (OpenAPI 스펙 400 응답 추가)
    |                                  |
    v                                  v
TASK-003 (서비스 로직 수정)      ┌─────┴─────┐
    |                            |           |
    ├───────────────┐     TASK-004       TASK-005
    |               |     (FE 캐시)     (FE 에러 메시지)
    v               v          [TASK-003 + TASK-008 이후]
TASK-006         TASK-009
(통합 테스트)    (INQ-INV-08 문서)
                    |
                    v
                TASK-010
                (상태 모델 문서)
    |
    v
TASK-011
(테스트 매핑 문서)
    [TASK-006 이후]

TASK-007 (도메인 단위 테스트) [독립, 선택]
```

### 권장 실행 순서

1. **1차 (병렬 가능)**: TASK-001, TASK-007
2. **2차 (병렬 가능)**: TASK-002, TASK-008 (TASK-001 완료 후)
3. **3차**: TASK-003 (TASK-002 완료 후)
4. **4차 (병렬 가능)**: TASK-004, TASK-005, TASK-006, TASK-009 (TASK-003 + TASK-008 완료 후)
5. **5차**: TASK-010 (TASK-009 완료 후)
6. **6차**: TASK-011 (TASK-006 완료 후)

**핵심 선후 관계**:
- TASK-008 (OpenAPI 스펙)은 TASK-004, TASK-005 (프론트엔드)보다 **반드시 먼저** 완료되어야 한다. Orval이 스펙에서 API 클라이언트를 자동 생성하므로, 스펙에 400 응답이 정의되지 않은 상태에서 프론트엔드 작업을 시작하면 타입 불일치가 발생할 수 있다.
- TASK-006 (테스트)은 TASK-003 (서비스 로직) 완료 후에만 실행 가능하다.
- TASK-009 ~ TASK-011 (문서화)은 구현이 확정된 후 진행한다.

---

## 구현 시 주의사항

### 기술적 고려사항

1. **Lazy Loading 주의**: `Inquiry.reply`는 `@OneToOne(mappedBy = "inquiry")`로 선언되어 있어, JPA에서 기본적으로 EAGER fetch될 수 있다 (OneToOne의 non-owning side). `hasReply()` 호출 시 추가 쿼리가 발생하더라도 성능에 큰 영향은 없으나, `findById`로 조회한 시점에 이미 reply가 초기화되었을 가능성이 있다.

2. **멱등성 전이 처리**: `InquiryStatus.canTransitionTo()`에서 동일 상태 전이 (`COMPLETED -> COMPLETED`)를 허용하고 있으므로, TASK-003에서 이 경우를 답변 검증에서 제외해야 한다. 그렇지 않으면 답변이 작성된 후 COMPLETED 상태의 문의에 멱등성 전이가 차단되는 부작용이 발생한다.

3. **검증 순서**: TASK-003에서 답변 존재 여부 검증은 `changeStatus()` (FSM 전이 검증) **이전**에 수행한다. 이렇게 하면:
   - `COMPLETED -> COMPLETED` (멱등) 시 답변 검증을 건너뛰는 조건을 현재 상태 기준으로 판단 가능
   - COMPLETED에서 다른 상태로의 전이는 FSM에서 차단되므로 답변 검증과 무관

4. **프론트엔드 캐시 무효화**: `invalidateQueries`는 비동기적으로 refetch를 트리거한다. 상세 조회 쿼리 키는 기존 패턴(`` [`/api/v1/inquiries/${selectedId}`] ``)과 동일하게 사용한다.

### 잠재적 위험 요소

1. **기존 테스트 호환성**: TASK-003 로직 추가로 기존 테스트 5건(INQ-A-021, 023, 025, 026, 027)이 실패한다. INQ-A-021, 023, 027은 답변 없이 COMPLETED로의 전이를 수행하므로 `InquiryReplyRequiredForCompletionException`이 발생하고, INQ-A-025, 026은 setup에서 답변 없이 `changeStatus(COMPLETED)`를 호출하여 COMPLETED 상태에 도달하려 하므로 동일한 예외가 발생한다.

   **완화 방안**:
   - TASK-003과 TASK-006을 동일 PR에 포함시켜, 서비스 로직 변경과 테스트 수정을 함께 커밋한다. 이렇게 하면 CI에서 중간 상태의 빌드 실패가 발생하지 않는다.
   - 기존 테스트 수정은 단순히 `CreateInquiryReplyService.createReply()`를 호출하여 답변을 먼저 작성하는 것이므로 로직 변경이 아닌 시나리오 보강이다.
   - 수정 전 기존 테스트의 의도("COMPLETED 전이 성공" 및 "COMPLETED에서의 금지 전이")는 각각 신규 테스트의 "답변 있는 문의의 COMPLETED 전이 성공" 케이스와 수정된 INQ-A-025/026으로 보존된다.

2. **OpenAPI 스펙 변경 영향**: 400 응답 추가는 기존 프론트엔드 코드에 breaking change를 유발하지 않는다. 다만 Orval 재생성 시 타입 정의가 변경될 수 있으므로, TASK-008 완료 후 프론트엔드 빌드를 확인해야 한다.

3. **`InquiryDetailPage.tsx` (회원 문의 상세 페이지)**: 이 페이지는 관리자 상태 변경 기능이 없으므로 이번 변경에 영향 없음.

### 기존 코드와의 통합 포인트

| 통합 포인트 | 파일 | 설명 |
|------------|------|------|
| `UpdateInquiryStatusService` | `backend/.../service/manage/UpdateInquiryStatusService.java` | 핵심 수정 대상. 검증 로직 추가 |
| `InquiryErrorCode` | `backend/.../exception/InquiryErrorCode.java` | 에러코드 추가 |
| `Inquiry.hasReply()` | `backend/.../domain/Inquiry.java:115-117` | 기존 메서드 재활용 (수정 불필요) |
| `InquiriesTab.tsx` | `frontend/src/pages/admin/tabs/InquiriesTab.tsx` | 캐시 무효화 + 에러 메시지 개선 |
| `UpdateInquiryStatusServiceTest` | `backend/.../service/manage/UpdateInquiryStatusServiceTest.java` | 기존 테스트 수정 + 신규 테스트 추가 |
| `openapi/paths/inquiries.yaml` | OpenAPI 스펙 | 400 응답 정의 추가 |
| `docs/criteria/inquiry-verification-criteria.md` | 검증 기준서 | INQ-INV-08, 상태 모델 2-1, 테스트 매핑 업데이트 |

---

## 완료 기준

### 검증 기준 충족 여부 체크리스트

- [ ] INQ-INV-07: COMPLETED 상태에서 다른 상태로의 전이 차단 (기존 구현 유지, 영향 없음)
- [ ] INQ-INV-08: 답변 작성 시 상태 자동 완료 (기존 구현 유지, 신규 검증으로 역방향 보강 -- 답변 없이 COMPLETED 수동 전이 차단)
- [ ] 상태 모델 2-1: PENDING <-> IN_PROGRESS 양방향 전이는 답변 유무와 무관하게 허용
- [ ] 상태 모델 2-1: 동일 상태 멱등성 전이는 답변 유무와 무관하게 허용

### 기능 검증 체크리스트

- [ ] 답변 없는 문의에서 PENDING -> COMPLETED 시도 시 400 에러 반환 (INQUIRY_REPLY_REQUIRED_FOR_COMPLETION)
- [ ] 답변 없는 문의에서 IN_PROGRESS -> COMPLETED 시도 시 400 에러 반환
- [ ] 답변 없는 문의에서 PENDING -> IN_PROGRESS 전이 정상 동작
- [ ] 답변 없는 문의에서 IN_PROGRESS -> PENDING 전이 정상 동작
- [ ] 답변 있는 문의에서 COMPLETED 전이 정상 동작
- [ ] COMPLETED -> COMPLETED 멱등성 전이 정상 동작
- [ ] 프론트엔드: 상태 변경 후 상세 패널에서 상태가 즉시 반영
- [ ] 프론트엔드: 답변 없이 COMPLETED 변경 시도 시 구체적인 에러 메시지 토스트 표시
- [ ] 기존 테스트 전체 통과 (수정 포함)
- [ ] 신규 테스트 전체 통과

### OpenAPI 스펙 체크리스트

- [ ] `updateInquiryStatus` 엔드포인트에 400 응답 정의 존재
- [ ] Orval 재생성 후 프론트엔드 빌드 성공

### 문서화 체크리스트

- [ ] 검증 기준서 INQ-INV-08 섹션에 답변 필수 사전조건 및 위반 시 예외 추가 (TASK-009)
- [ ] 검증 기준서 상태 모델 2-1 전이 테이블에 답변 필수 사전조건 반영 (TASK-010)
- [ ] 검증 기준서 8-2 테스트-검증 항목 매핑에 신규/수정 테스트 반영 (TASK-011)

### 확인이 필요한 사항

- [ ] `InquiryDetailPage.tsx` (회원 문의 상세 페이지)에서도 상태 반영 이슈가 있는지 확인 (현재 이 페이지는 관리자 상태 변경 기능이 없으므로 영향 없을 것으로 예상)
