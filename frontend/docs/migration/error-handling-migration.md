# 에러 처리 마이그레이션 가이드

기존 Axios 스타일 에러 처리에서 ApiError 기반 에러 처리로 점진적으로 마이그레이션하는 가이드입니다.

## 목차
- [마이그레이션 개요](#마이그레이션-개요)
- [우선순위](#우선순위)
- [Before/After 코드 예시](#beforeafter-코드-예시)
- [마이그레이션 체크리스트](#마이그레이션-체크리스트)
- [주의사항](#주의사항)

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

**Phase 2 (향후):**
- ⬜ 게시판 페이지 (BoardListPage, PostDetailPage, PostWritePage, PostEditPage)

**Phase 3 (향후):**
- ⬜ 인증 페이지 (LoginPage, SignupPage, ForgotPasswordPage 등)
- ⬜ 행사 페이지 (EventListPage, EventDetailPage, EventWritePage)
- ⬜ 문의 페이지 (InquiryPage)
- ⬜ 마이페이지 (MyPage)
- ⬜ 관리자 페이지 (AdminDashboard 등)

**Phase 4 (향후):**
- ⬜ 댓글 컴포넌트
- ⬜ 기타 공통 컴포넌트

---

## 우선순위

### 1순위: 게시판 도메인 (향후)

**이유:**
- 가장 많이 사용되는 기능
- 다양한 에러 케이스 포함
- 사용자 경험에 직접적 영향

**마이그레이션 대상 파일:**
- frontend/src/pages/board/BoardListPage.tsx
- frontend/src/pages/board/PostDetailPage.tsx
- frontend/src/pages/board/PostWritePage.tsx
- frontend/src/pages/board/PostEditPage.tsx

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

### 3순위: 행사 도메인 (향후)

**이유:**
- 복잡한 에러 케이스 (정원 마감, 신청 기간, 권한 등)
- 행사별 에러 처리 로직 통일 필요

**마이그레이션 대상 파일:**
- frontend/src/pages/event/EventListPage.tsx
- frontend/src/pages/event/EventDetailPage.tsx
- frontend/src/pages/event/EventWritePage.tsx
- frontend/src/components/feature/event/EventCard.tsx

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

## 참고 문서

- [에러 처리 테스트 가이드](../testing/error-handling-test.md)
- [에러 처리 구현 계획](../../plans/crispy-honking-marble.md)
- [CLAUDE.md - 에러 처리 규칙](../../CLAUDE.md)
