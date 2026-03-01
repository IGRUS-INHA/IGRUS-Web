# 관리자 준회원 승인 기능 테스트

## 개요

이 문서는 관리자 페이지의 준회원 승인 기능(`/admin/associates`)의 테스트 시나리오를 설명합니다.

## 구현 내용

### 1. 준회원 목록 조회

- **목적**: 승인 대기 중인 준회원 목록을 테이블 형태로 표시
- **표시 항목**: 학번, 이름, 학과, 가입동기, 신청일
- **파일**: `frontend/src/pages/admin/AdminAssociates.tsx`

### 2. 개별 승인

- **목적**: 특정 준회원을 정회원으로 승인
- **API**: `POST /api/v1/admin/associates/{id}/approve`
- **현재**: Mock 모드 (데이터에서 직접 제거)

### 3. 일괄 승인

- **목적**: 체크박스로 선택한 여러 준회원을 한 번에 승인
- **API**: `POST /api/v1/admin/associates/approve-batch`
- **현재**: Mock 모드

### 4. 거절 (미구현)

- **상태**: UI만 배치, disabled 처리
- **사유**: 백엔드 API 미존재
- **표시**: "추후 지원 예정" tooltip

---

## 테스트 환경 설정

### 사전 준비

```bash
cd frontend
pnpm dev
```

브라우저에서 http://localhost:5173 접속

### Mock 모드

현재 `USE_MOCK = true` 설정으로, 23개의 mock 데이터가 포함되어 있습니다.
백엔드 연동 시 `AdminAssociates.tsx`에서 `USE_MOCK = false`로 변경하고 주석 처리된 API 호출 코드를 활성화합니다.

---

## 테스트 시나리오

### 1. 페이지 접근 및 권한 체크

#### 테스트 1-1: ADMIN 역할로 접근

**목적**: ADMIN 사용자가 정상적으로 페이지에 접근할 수 있는지 확인

**절차**:

1. ADMIN 역할 계정으로 로그인
2. `/admin/associates` 접속

**기대 결과**:

- 준회원 관리 페이지가 정상 렌더링
- "준회원 관리" 제목과 "대기 23명" 배지 표시
- 23개의 mock 데이터가 테이블에 표시 (첫 페이지 20개)

#### 테스트 1-2: OPERATOR 역할로 접근

**목적**: ADMIN 미만 역할은 접근 차단되는지 확인

**절차**:

1. OPERATOR 역할 계정으로 로그인
2. `/admin/associates` 접속

**기대 결과**:

- 홈페이지로 리다이렉트
- 권한 부족 토스트 메시지 표시

---

### 2. 목록 표시

#### 테스트 2-1: 테이블 데이터 확인

**목적**: 준회원 정보가 올바르게 표시되는지 확인

**절차**:

1. `/admin/associates` 접속
2. 테이블 내용 확인

**기대 결과**:

- 학번(20240001 등), 이름, 학과, 가입동기, 신청일(2025.03.15 형식) 표시
- 가입동기는 긴 텍스트일 경우 1줄로 줄임 (line-clamp)
- 각 행에 "승인"/"거절" 버튼 표시

#### 테스트 2-2: 반응형 레이아웃

**목적**: 모바일/태블릿 화면에서 테이블이 적절히 표시되는지 확인

**절차**:

1. 브라우저 너비를 줄여가며 확인

**기대 결과**:

- md 미만: 학과 컬럼 숨김
- lg 미만: 가입동기 컬럼 추가 숨김
- 학번, 이름, 신청일, 액션은 항상 표시
- 테이블 가로 스크롤 지원

---

### 3. 체크박스 선택

#### 테스트 3-1: 개별 선택

**목적**: 체크박스로 개별 행 선택이 동작하는지 확인

**절차**:

1. 임의의 행의 체크박스 클릭
2. 다른 행의 체크박스도 클릭

**기대 결과**:

- 선택된 행 배경색 변경 (bg-primary/5)
- 상단에 액션 바 표시: "N명 선택됨" + "선택 승인"/"선택 거절" 버튼
- 체크 해제 시 선택 수 감소

#### 테스트 3-2: 전체 선택

**목적**: 헤더 체크박스로 현재 페이지 전체 선택/해제

**절차**:

1. 테이블 헤더의 체크박스 클릭
2. 다시 클릭

**기대 결과**:

- 첫 클릭: 현재 페이지 모든 행 선택 (최대 20개)
- 두 번째 클릭: 전체 선택 해제
- 액션 바의 선택 수가 정확히 반영

---

### 4. 개별 승인

#### 테스트 4-1: 승인 확인 후 진행

**목적**: 개별 승인이 정상 동작하는지 확인

**절차**:

1. 첫 번째 행의 "승인" 버튼 클릭
2. 확인 다이얼로그에서 "확인" 클릭

**기대 결과**:

- "김민수 님을 정회원으로 승인하시겠습니까?" 확인 다이얼로그
- 확인 후 "김민수 님이 정회원으로 승인되었습니다." alert
- 해당 행이 테이블에서 제거
- 대기 인원 수 배지 감소 (23명 → 22명)

#### 테스트 4-2: 승인 취소

**목적**: 확인 다이얼로그에서 취소 시 아무 변경 없는지 확인

**절차**:

1. "승인" 버튼 클릭
2. 확인 다이얼로그에서 "취소" 클릭

**기대 결과**:

- 테이블 변경 없음
- 대기 인원 수 유지

---

### 5. 일괄 승인

#### 테스트 5-1: 여러 명 일괄 승인

**목적**: 체크박스로 선택한 여러 준회원을 한 번에 승인

**절차**:

1. 3개 행 체크박스 선택
2. 액션 바의 "선택 승인" 버튼 클릭
3. 확인 다이얼로그에서 "확인" 클릭

**기대 결과**:

- "3명을 정회원으로 승인하시겠습니까?" 확인 다이얼로그
- 확인 후 "3명이 정회원으로 승인되었습니다." alert
- 선택된 3개 행 테이블에서 제거
- 대기 인원 수 감소
- 체크박스 선택 상태 초기화
- 액션 바 사라짐

#### 테스트 5-2: 전체 선택 후 일괄 승인

**목적**: 전체 선택 후 일괄 승인 동작 확인

**절차**:

1. 헤더 체크박스로 전체 선택
2. "선택 승인" 클릭 → 확인

**기대 결과**:

- 현재 페이지 전체 (최대 20명) 승인
- 2페이지 데이터가 있었다면 1페이지로 표시 변경

---

### 6. 거절 버튼

#### 테스트 6-1: 개별 거절 버튼 비활성

**목적**: 거절 버튼이 disabled 상태인지 확인

**절차**:

1. 행의 "거절" 버튼 위에 마우스 올리기

**기대 결과**:

- 버튼 클릭 불가 (disabled)
- "추후 지원 예정" tooltip 표시

#### 테스트 6-2: 일괄 거절 버튼 비활성

**목적**: 선택 거절 버튼이 disabled 상태인지 확인

**절차**:

1. 체크박스로 행 선택
2. 액션 바의 "선택 거절" 버튼 확인

**기대 결과**:

- 버튼 클릭 불가 (disabled)
- "추후 지원 예정" tooltip 표시

---

### 7. 페이지네이션

#### 테스트 7-1: 페이지 이동

**목적**: 20건 초과 시 페이지네이션 동작 확인

**절차**:

1. 초기 상태에서 23개 mock 데이터 확인
2. 페이지네이션의 "2" 버튼 클릭

**기대 결과**:

- 1페이지: 20개 행 표시
- 2페이지: 3개 행 표시
- 페이지 번호 하이라이트 변경

#### 테스트 7-2: 페이지 이동 시 선택 초기화

**목적**: 다른 페이지로 이동 시 체크박스 선택이 초기화되는지 확인

**절차**:

1. 1페이지에서 여러 행 선택
2. 2페이지로 이동
3. 다시 1페이지로 이동

**기대 결과**:

- 페이지 이동 시 선택 상태 초기화
- 액션 바 사라짐

---

### 8. 빈 상태

#### 테스트 8-1: 모든 준회원 승인 후

**목적**: 대기자가 없을 때 빈 상태 메시지 표시 확인

**절차**:

1. 전체 선택 → 일괄 승인 반복하여 모든 mock 데이터 승인

**기대 결과**:

- "승인 대기 중인 준회원이 없습니다." 메시지
- Users 아이콘 표시
- 대기 인원 배지 "대기 0명"
- 페이지네이션 숨김

---

## 빌드 검증

```bash
cd frontend
pnpm tsc --noEmit 2>&1 | grep "AdminAssociates"
```

AdminAssociates.tsx 관련 에러가 없어야 합니다.

---

## 백엔드 연동 전환 가이드

`AdminAssociates.tsx`에서 다음 변경 필요:

1. `USE_MOCK = false`로 변경
2. 주석 처리된 import 활성화:
   ```typescript
   import {
     useGetPendingAssociates,
     useApproveAssociate,
     useApproveBulk,
   } from "@/api/model/admin-associate-approval/admin-associate-approval";
   import { useQueryClient } from "@tanstack/react-query";
   import { getGetPendingAssociatesQueryKey } from "@/api/model/admin-associate-approval/admin-associate-approval";
   ```
3. 주석 처리된 API 훅 호출 코드 활성화
4. Mock 데이터 및 `mockData` state 제거
5. `isLoading`, `isError` 를 실제 query 상태로 교체

### Blob 응답 해결 (백엔드)

백엔드 Swagger 어노테이션에 `schema` 추가 필요:

```java
@ApiResponse(responseCode = "200", description = "조회 성공",
    content = @Content(schema = @Schema(implementation = AssociateInfoPageResponse.class)))
```

수정 후 `pnpm orval`로 프론트엔드 API 코드 재생성.

---

## 관련 파일

| 파일                                                                 | 역할                                          |
| -------------------------------------------------------------------- | --------------------------------------------- |
| `src/pages/admin/AdminAssociates.tsx`                                | 준회원 관리 페이지                            |
| `src/api/model/admin-associate-approval/admin-associate-approval.ts` | API 훅 (Orval 생성)                           |
| `src/api/model/models/associateInfoResponse.ts`                      | 준회원 정보 타입                              |
| `src/api/model/models/associateInfoPageResponse.ts`                  | 페이징 응답 타입                              |
| `src/api/model/models/bulkApprovalRequest.ts`                        | 일괄 승인 요청 타입                           |
| `src/api/model/models/bulkApprovalResultResponse.ts`                 | 일괄 승인 결과 타입                           |
| `src/components/board/Pagination.tsx`                                | 페이지네이션 컴포넌트                         |
| `src/constants/permissions.ts`                                       | 권한 상수 (`canApproveAssociate`)             |
| `src/router.tsx`                                                     | 라우트 설정 (`/admin/associates`, ADMIN 권한) |
