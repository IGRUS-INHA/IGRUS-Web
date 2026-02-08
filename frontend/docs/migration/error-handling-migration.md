# 에러 처리 마이그레이션 가이드

기존 Axios 스타일 에러 처리에서 ApiError 기반 에러 처리로 점진적으로 마이그레이션하는 가이드입니다.

## 목차
- [마이그레이션 개요](#마이그레이션-개요)
- [우선순위](#우선순위)
- [Before/After 코드 예시](#beforeafter-코드-예시)
- [마이그레이션 체크리스트](#마이그레이션-체크리스트)
- [주의사항](#주의사항)
- [글로벌 에러 핸들러 마이그레이션](#글로벌-에러-핸들러-마이그레이션)

---

## 마이그레이션 개요

### 왜 점진적으로 마이그레이션하나요?

1. **리스크 최소화**: 한 번에 모든 파일을 수정하면 예상치 못한 버그 발생 가능
2. **테스트 용이성**: 도메인별로 수정하고 테스트하여 안정성 확보
3. **개발 효율성**: 새 기능 개발과 병행하여 점진적으로 개선
4. **하위 호환성**: 기존 동작을 유지하면서 새로운 방식 도입

### 마이그레이션 전략

**Phase 1 (완료):**
- ✅ types/error.ts 생성 (ApiError 클래스)
- ✅ utils/error.ts 리팩토링 (45개 헬퍼 함수, 148개 에러 코드 매핑)
- ✅ client.ts 수정 (ApiError 사용, default 코드 생성)

**Phase 2 (완료):**
- ✅ BoardListPage: `(error as any).response?.status` → `isBoardReadDenied() || isForbiddenError()`
- ✅ PostDetailPage: `error.message?.includes('403')` → `isForbiddenError()`, `isNotFoundError()`
- ✅ PostWritePage: 메시지 기반 체크 → `isForbiddenError()`, `isBoardWriteDenied()`, `isUnauthorizedError()`
- ✅ PostEditPage: 메시지 기반 체크 → `isForbiddenError()`, `isUnauthorizedError()`, `isNotFoundError()`
- ✅ CommentSection: `error?.code` 직접 접근 → `isForbiddenError()`, `hasErrorCode()`
- ✅ CommentItem: `error?.message` 직접 접근 → `getErrorMessage()`

**Phase 3 (진행 중):**
- ⬜ 인증 페이지 (LoginPage, SignupPage, ForgotPasswordPage 등)
- ✅ EventDetailPage: query `(error as any).response?.status` → `isForbiddenError() || isEventAccessDenied()`, mutation별 에러 코드 분기 추가
- ✅ EventWritePage: 에러 무시 → `isForbiddenError() || isEventOperatorRequired()`, `getErrorMessage()`
- ✅ EventEditPage: query 403/404 분리, mutation `isForbiddenError() || isEventOperatorRequired()`
- ✅ EventListPage: `error` 구조분해 추가, `isForbiddenError()` 403 처리
- ⬜ 문의 페이지 (InquiryPage)
- ⬜ 마이페이지 (MyPage)
- ⬜ 관리자 페이지 (AdminDashboard 등)

**Phase 4 (향후):**
- ⬜ 기타 공통 컴포넌트

**Phase 5 (향후):**
- ⬜ 글로벌 에러 핸들러 도입 (QueryClient `defaultOptions.mutations.onError`)

---

## 우선순위

### 1순위: 게시판 도메인 (완료)

**이유:**
- 가장 많이 사용되는 기능
- 다양한 에러 케이스 포함
- 사용자 경험에 직접적 영향

**마이그레이션 완료 파일:**
- ✅ frontend/src/pages/board/BoardListPage.tsx
- ✅ frontend/src/pages/board/PostDetailPage.tsx
- ✅ frontend/src/pages/board/PostWritePage.tsx
- ✅ frontend/src/pages/board/PostEditPage.tsx
- ✅ frontend/src/components/feature/comment/CommentSection.tsx
- ✅ frontend/src/components/feature/comment/CommentItem.tsx

### 2순위: 인증 도메인 (향후)

**이유:**
- 사용자 접근 첫 단계
- 명확한 에러 코드 (INVALID_CREDENTIALS, EMAIL_NOT_VERIFIED 등)
- 테스트 용이

**마이그레이션 대상 파일:**
- frontend/src/pages/auth/LoginPage.tsx
- frontend/src/pages/auth/SignupPage.tsx
- frontend/src/pages/auth/ForgotPasswordPage.tsx
- frontend/src/pages/auth/ResetPasswordPage.tsx
- frontend/src/pages/auth/VerifyEmailPage.tsx
- frontend/src/components/feature/auth/AuthForm.tsx

### 3순위: 행사 도메인 (완료)

**이유:**
- 복잡한 에러 케이스 (정원 마감, 신청 기간, 권한 등)
- 행사별 에러 처리 로직 통일 필요

**마이그레이션 완료 파일:**
- ✅ frontend/src/pages/event/EventDetailPage.tsx
- ✅ frontend/src/pages/event/EventWritePage.tsx
- ✅ frontend/src/pages/event/EventEditPage.tsx
- ✅ frontend/src/pages/event/EventListPage.tsx

### 4순위: 기타 도메인 (향후)

**마이그레이션 대상:**
- 문의 페이지 (InquiryPage)
- 마이페이지 (MyPage)
- 관리자 페이지 (AdminDashboard)

### 5순위: 컴포넌트 (향후)

**마이그레이션 대상:**
- 댓글 컴포넌트 (CommentSection, CommentActions 등)
- 공통 컴포넌트 (에러 바운더리 등)

---

## Before/After 코드 예시

### 예시 1: 에러 체크 로직 (게시판 권한)

**Before:**
```typescript
// ❌ 타입 안정성 낮음, 중복 코드
const isForbidden = error && (
  (error as any).code === 'BOARD_READ_DENIED' ||
  (error as Error).message?.includes('권한이 없습니다')
);
```

**After:**
```typescript
// ✅ 타입 안정성 높음, 재사용 가능
import { isBoardReadDenied } from '@/utils/error';

const isForbidden = isBoardReadDenied(error);
```

### 예시 2: 에러 처리 콜백 (게시글 작성)

**Before:**
```typescript
// ❌ any 타입, 메시지 기반 체크
onError: (error: any) => {
  let errorMessage = '게시글 작성에 실패했습니다.';

  if (error.message) {
    errorMessage = error.message;
  }

  if (error.message?.includes('403') || error.message?.includes('권한')) {
    errorMessage = '❌ 권한이 없습니다.\n\n로그인 후 다시 시도하거나,\n게시판 작성 권한을 확인해주세요.';
  }

  if (error.message?.includes('401') || error.message?.includes('인증')) {
    errorMessage = '❌ 로그인이 필요합니다.';
    alert(errorMessage);
    navigate('/login');
    return;
  }

  alert(errorMessage);
}
```

**After:**
```typescript
// ✅ unknown 타입, 헬퍼 함수 사용
import { isForbiddenError, isUnauthorizedError, getErrorMessage } from '@/utils/error';

onError: (error: unknown) => {
  let errorMessage = '게시글 작성에 실패했습니다.';

  if (isForbiddenError(error)) {
    errorMessage = '❌ 권한이 없습니다.\n\n로그인 후 다시 시도하거나,\n게시판 작성 권한을 확인해주세요.';
  } else if (isUnauthorizedError(error)) {
    errorMessage = '❌ 로그인이 필요합니다.';
    alert(errorMessage);
    navigate('/login');
    return;
  } else {
    errorMessage = getErrorMessage(error);
  }

  alert(errorMessage);
}
```

### 예시 3: 에러 처리 콜백 (게시글 삭제)

**Before:**
```typescript
// ❌ any 타입, 메시지 포함 여부 체크
onError: (error: any) => {
  let errorMessage = '게시글 삭제에 실패했습니다.';
  if (error.message?.includes('403')) errorMessage = '삭제 권한이 없습니다.';
  else if (error.message?.includes('404')) errorMessage = '게시글을 찾을 수 없습니다.';
  alert(errorMessage);
}
```

**After:**
```typescript
// ✅ unknown 타입, HTTP 상태 기반 체크
import { isForbiddenError, isNotFoundError, getErrorMessage } from '@/utils/error';

onError: (error: unknown) => {
  let errorMessage = '게시글 삭제에 실패했습니다.';

  if (isForbiddenError(error)) {
    errorMessage = '삭제 권한이 없습니다.';
  } else if (isNotFoundError(error)) {
    errorMessage = '게시글을 찾을 수 없습니다.';
  } else {
    errorMessage = getErrorMessage(error);
  }

  alert(errorMessage);
}
```

### 예시 4: 로그인 에러 처리 (향후 마이그레이션)

**Before:**
```typescript
// LoginPage.tsx (현재 상태 - 향후 마이그레이션 필요)
onError: (error: any) => {
  if (error.message?.includes('INVALID_CREDENTIALS')) {
    setError('학번 또는 비밀번호가 올바르지 않습니다.');
  } else if (error.message?.includes('EMAIL_NOT_VERIFIED')) {
    setError('이메일 인증이 필요합니다.');
  } else if (error.message?.includes('ACCOUNT_SUSPENDED')) {
    setError('정지된 계정입니다.');
  } else {
    setError('로그인에 실패했습니다.');
  }
}
```

**After (향후):**
```typescript
// ✅ 헬퍼 함수 사용
import {
  isInvalidCredentials,
  isEmailNotVerified,
  isAccountSuspended,
  getErrorMessage,
} from '@/utils/error';

onError: (error: unknown) => {
  if (isInvalidCredentials(error)) {
    setError('학번 또는 비밀번호가 올바르지 않습니다.');
  } else if (isEmailNotVerified(error)) {
    setError('이메일 인증이 필요합니다.');
  } else if (isAccountSuspended(error)) {
    setError('정지된 계정입니다.');
  } else {
    setError(getErrorMessage(error));
  }
}
```

---

## 마이그레이션 체크리스트

### 1. 파일 선택

**마이그레이션 대상 파일 선택 기준:**
- [ ] `error.message?.includes()` 패턴 사용
- [ ] `(error as any)` 타입 단언 사용
- [ ] `(error as Error)` 타입 단언 사용
- [ ] 에러 처리 로직이 중복됨

**검색 명령:**
```bash
# 마이그레이션 대상 파일 찾기
cd frontend/src
grep -r "error\.message.*includes" . --include="*.tsx" --include="*.ts"
grep -r "error as any" . --include="*.tsx" --include="*.ts"
grep -r "error as Error" . --include="*.tsx" --include="*.ts"
```

### 2. 파일 수정

**단계별 작업:**
1. [ ] Import 추가
   ```typescript
   import { isForbiddenError, isNotFoundError, getErrorMessage } from '@/utils/error';
   ```

2. [ ] 에러 타입 변경
   ```typescript
   // Before
   onError: (error: any) => {

   // After
   onError: (error: unknown) => {
   ```

3. [ ] 에러 체크 로직 교체
   ```typescript
   // Before
   if (error.message?.includes('403'))

   // After
   if (isForbiddenError(error))
   ```

4. [ ] 에러 메시지 추출
   ```typescript
   // Before
   const message = error.message || '오류가 발생했습니다.';

   // After
   const message = getErrorMessage(error);
   ```

### 3. 테스트

**확인 사항:**
- [ ] TypeScript 컴파일 에러 없음
- [ ] 기존 동작 유지 (에러 메시지 동일)
- [ ] UI 동작 동일 (로그아웃, 리다이렉트 등)
- [ ] 수동 테스트 (주요 에러 케이스)

### 4. 커밋

**커밋 메시지 형식:**
```
refactor(frontend): 에러 처리 마이그레이션 - [도메인명]

- [PageName]: 에러 체크 로직을 헬퍼 함수로 교체
- any 타입 제거, ApiError 기반 타입 안정성 확보
```

**예시:**
```
refactor(frontend): 에러 처리 마이그레이션 - 인증

- LoginPage: 에러 체크 로직을 헬퍼 함수로 교체
- SignupPage: any 타입 제거, ApiError 기반 타입 안정성 확보
```

---

## 주의사항

### 1. 기존 동작 유지

**절대 변경하면 안 되는 것:**
- ❌ 에러 메시지 내용 변경
- ❌ 로그아웃 시점 변경
- ❌ 리다이렉트 동작 변경
- ❌ UI 표시 방식 변경

**변경 가능한 것:**
- ✅ 에러 체크 방식 (메시지 → 헬퍼 함수)
- ✅ 타입 (any → unknown)
- ✅ 타입 단언 제거

### 2. 하위 호환성

**ApiError는 Error를 상속하므로 하위 호환성 유지:**
```typescript
// 기존 코드는 그대로 동작
if (error instanceof Error) {
  console.log(error.message);
}

// ApiError도 Error이므로 위 체크 통과
```

### 3. 점진적 마이그레이션

**규칙:**
- 새 기능 개발 시: 무조건 새 방식 사용
- 버그 수정 시: 해당 파일 마이그레이션
- 리팩토링 세션: 도메인별로 일괄 마이그레이션

**우선순위:**
1. 새 기능 개발
2. 버그 수정
3. 리팩토링

### 4. 에러 로깅

**마이그레이션 후 에러 로깅 추가 권장:**
```typescript
onError: (error: unknown) => {
  // 디버깅용 에러 정보 로깅
  console.error('Error occurred:', getErrorInfo(error));

  // 사용자에게 표시할 메시지
  const message = getErrorMessage(error);
  alert(message);
}
```

### 5. 테스트 필수

**마이그레이션 후 반드시 테스트:**
- TypeScript 컴파일
- 빌드 테스트
- 수동 주요 시나리오 테스트
- 회귀 테스트 (기존 기능 동작 확인)

---

## 도메인별 마이그레이션 가이드

### 인증 도메인 (향후)

**사용할 헬퍼 함수:**
- `isInvalidCredentials(error)` - 잘못된 인증 정보
- `isEmailNotVerified(error)` - 이메일 미인증
- `isAccountSuspended(error)` - 계정 정지
- `isAccountWithdrawn(error)` - 계정 탈퇴
- `isAccountLocked(error)` - 계정 잠김
- `isTokenExpired(error)` - 토큰 만료

**마이그레이션 대상 파일:**
1. LoginPage.tsx - 로그인 에러 처리
2. SignupPage.tsx - 회원가입 에러 처리
3. ForgotPasswordPage.tsx - 비밀번호 찾기 에러
4. ResetPasswordPage.tsx - 비밀번호 재설정 에러
5. VerifyEmailPage.tsx - 이메일 인증 에러

### 행사 도메인 (향후)

**사용할 헬퍼 함수:**
- `isEventNotFound(error)` - 행사 없음
- `isEventAccessDenied(error)` - 행사 접근 권한 없음
- `isEventAlreadyRegistered(error)` - 이미 신청한 행사
- `isEventCapacityFull(error)` - 행사 정원 마감
- `isEventRegistrationClosed(error)` - 신청 기간 종료
- `isEventOperatorRequired(error)` - 행사 운영자 권한 필요

**마이그레이션 대상 파일:**
1. EventListPage.tsx - 행사 목록
2. EventDetailPage.tsx - 행사 상세
3. EventWritePage.tsx - 행사 작성
4. EventCard.tsx - 행사 카드 컴포넌트

### 문의 도메인 (향후)

**사용할 헬퍼 함수:**
- `hasErrorCode(error, 'INQUIRY_NOT_FOUND')` - 문의 없음
- `hasErrorCode(error, 'INQUIRY_ACCESS_DENIED')` - 문의 접근 권한 없음
- `hasErrorCode(error, 'INQUIRY_ALREADY_REPLIED')` - 이미 답변된 문의

**마이그레이션 대상 파일:**
1. InquiryPage.tsx - 문의하기

---

## FAQ

### Q1: 기존 Axios 스타일 함수는 언제 제거하나요?

A: 모든 마이그레이션이 완료된 후 제거합니다. 현재는 하위 호환성 유지를 위해 남겨둡니다.

### Q2: 새 기능 개발 시 어떤 방식을 사용해야 하나요?

A: **무조건 새 방식(ApiError, 헬퍼 함수)을 사용**해야 합니다. 기존 방식은 사용하지 마세요.

### Q3: 마이그레이션하지 않은 파일에서 버그가 발생하면?

A: 버그 수정과 함께 해당 파일을 마이그레이션합니다.

### Q4: 어떤 순서로 마이그레이션하나요?

A: 우선순위 섹션을 참고하세요. 게시판(완료) → 인증 → 행사 → 기타 순서입니다.

### Q5: 마이그레이션 후 성능 영향은?

A: 없습니다. 헬퍼 함수는 단순 타입 체크만 수행하므로 성능 영향 미미합니다.

---

## 글로벌 에러 핸들러 마이그레이션

### 배경: 현재 에러 처리 아키텍처

현재 에러 처리는 3개 레이어로 구성되어 있으나, Layer 2가 비어 있어 각 페이지에서 공통 에러까지 직접 처리하고 있다.

```
┌─────────────────────────────────────────────────────────┐
│ Layer 1: customFetch (api/client.ts)          [완료]     │
│ - HTTP 응답 → ApiError 변환                              │
│ - 401 → 토큰 갱신 시도 → 실패 시 자동 로그아웃            │
│ - 403/4xx/5xx → ApiError throw                          │
└──────────────────────┬──────────────────────────────────┘
                       │ ApiError throw
                       ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 2: QueryClient 글로벌 핸들러 (lib/queryClient.ts)  │
│ - 현재: 설정 없음 (비어 있음)                   [미구현]  │
│ - 목표: 공통 에러 (500 서버 오류 등) 일괄 처리            │
└──────────────────────┬──────────────────────────────────┘
                       │ 페이지 고유 에러만 전달
                       ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 3: 페이지 컴포넌트 onError                [현재]    │
│ - 페이지 맥락에 맞는 에러 메시지 표시                     │
│ - 예: BOARD_READ_DENIED → "정회원 승인 후 이용 가능"      │
│ - 예: 403 in PostEditPage → "수정 권한이 없습니다"        │
└─────────────────────────────────────────────────────────┘
```

### 문제점

- 401 인증 에러: `client.ts`에서 이미 토큰 갱신/로그아웃을 처리하지만, PostWritePage/PostEditPage에서 중복으로 `isUnauthorizedError` 체크 + `/login` 리다이렉트를 수행
- 500 서버 에러: 각 페이지 `onError`에서 개별적으로 처리하거나, 아예 처리하지 않음
- 동일한 에러 분기 코드가 여러 페이지에 반복됨

### 목표

Layer 2에 글로벌 핸들러를 추가하여 **공통 에러는 한 곳에서 처리**하고, **페이지별 onError에는 해당 페이지 고유 에러만** 남긴다.

### 구현 계획

#### 1단계: QueryClient에 글로벌 mutation onError 추가

**수정 파일:** `frontend/src/lib/queryClient.ts`

```typescript
import { QueryClient } from '@tanstack/react-query';
import { isServerError, getErrorMessage } from '@/utils/error';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      gcTime: 1000 * 60 * 30,
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
      onError: (error: unknown) => {
        // 5xx 서버 에러는 글로벌에서 일괄 처리
        if (isServerError(error)) {
          // TODO: toast 시스템 도입 후 toast.error()로 교체
          console.error('[Global Error]', getErrorMessage(error));
        }
      },
    },
  },
} as const);
```

**주의:** QueryClient의 글로벌 `onError`는 mutation의 개별 `onError`보다 **후순위로 실행**된다. 개별 `onError`에서 처리한 에러도 글로벌 `onError`에 도달하므로, 글로벌에서는 개별 핸들러가 처리하지 않는 공통 에러만 처리해야 한다.

#### 2단계: 페이지별 onError에서 공통 로직 제거

글로벌 핸들러가 처리하는 에러(500 등)를 각 페이지 `onError`에서 제거한다.

**제거 대상 패턴:**
- 서버 에러 메시지 표시 (글로벌에서 처리)

**유지 대상 패턴 (페이지 고유 에러):**
- `isForbiddenError` → 페이지마다 다른 메시지 필요
- `isNotFoundError` → 페이지마다 다른 리다이렉트/메시지 필요
- `isBoardWriteDenied` → 게시판별 권한 안내 메시지

#### 3단계: 401 중복 처리 정리

현재 `client.ts`에서 401 시 토큰 갱신 → 실패 시 `handleLogout()` (→ `/login` 리다이렉트)을 수행한다.
따라서 **페이지별 `isUnauthorizedError` 체크 + `/login` 리다이렉트는 중복**이며, 글로벌 핸들러 도입 후 제거할 수 있다.

**제거 대상 파일:**
- PostWritePage.tsx: `isUnauthorizedError` 분기 제거
- PostEditPage.tsx: `isUnauthorizedError` 분기 제거

### 글로벌 핸들러 도입 전제 조건

1. **Phase 2~4 마이그레이션 완료**: 모든 페이지가 ApiError 기반으로 전환되어야 에러 흐름이 일관됨
2. **토스트 시스템 도입**: 글로벌 에러를 `alert()` 대신 토스트로 표시하는 것이 UX상 적합
3. **에러 바운더리 검토**: Query 에러를 React Error Boundary로 처리하는 방식도 함께 검토

### 최종 목표 구조

```
┌─────────────────────────────────────────────────────────┐
│ Layer 1: customFetch (api/client.ts)                     │
│ - HTTP → ApiError 변환                                   │
│ - 401 토큰 갱신 / 로그아웃                                │
└──────────────────────┬──────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 2: QueryClient 글로벌 핸들러                        │
│ - 500: 서버 오류 토스트                                   │
│ - 429: 요청 제한 토스트                                   │
│ - 기타 공통 에러 로깅                                     │
└──────────────────────┬──────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 3: 페이지 컴포넌트 onError                          │
│ - 403: 페이지 맥락에 맞는 권한 안내                       │
│ - 404: 페이지별 "찾을 수 없음" 처리                       │
│ - 409: 페이지별 충돌 안내                                 │
└─────────────────────────────────────────────────────────┘
```

---

## 참고 문서

- [에러 처리 테스트 가이드](../testing/error-handling-test.md)
- [에러 처리 구현 계획](../../plans/crispy-honking-marble.md)
- [CLAUDE.md - 에러 처리 규칙](../../CLAUDE.md)
