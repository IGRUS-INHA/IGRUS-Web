# Auth & Inquiries API Orval 마이그레이션

## 개요

인증(Auth) 시스템과 문의(Inquiries) 시스템을 수동 axios 기반 API에서 Orval 생성 API로 마이그레이션했습니다.

**마이그레이션 날짜**: 2026-02-02

## 마이그레이션 완료 API

### 인증 API

- ✅ 로그인 (`POST /api/v1/auth/password/login`)
- ✅ 회원가입 (`POST /api/v1/auth/password/signup`)
- ✅ 이메일 인증 (`POST /api/v1/auth/password/verify-email`)
- ✅ 인증 코드 재발송 (`POST /api/v1/auth/password/resend-verification`)
- ⏸️ 로그아웃 (`POST /api/v1/auth/password/logout`) - 구현 대기 중
- ⏸️ 토큰 갱신 (`POST /api/v1/auth/password/refresh`) - 구현 대기 중

### 문의 API

- ✅ 내 문의 목록 (`GET /api/v1/inquiries/my`)
- ✅ 문의 작성 (`POST /api/v1/inquiries/member`)

## 주요 변경 사항

### 1. AuthStore 수정

**파일**: [src/stores/authStore.ts](../../src/stores/authStore.ts)

**변경 내용**:

- `login()` 함수 제거 (Mock 데이터 사용 중지)
- `setAuth()` 함수의 `refreshToken` 파라미터를 optional로 변경
- 로그인 로직을 LoginPage로 이동

**Before**:

```typescript
login: async (studentId: string, password: string): Promise<void> => {
  // Mock login
  set({ user: {...}, accessToken: 'mock-access-token', ... });
}

setAuth: (user: User, accessToken: string, refreshToken: string): void => {
  set({ user, accessToken, refreshToken, isAuthenticated: true });
}
```

**After**:

```typescript
setAuth: (user: User, accessToken: string, refreshToken?: string): void => {
  set({
    user,
    accessToken,
    refreshToken: refreshToken || undefined,
    isAuthenticated: true,
  });
};
```

### 2. LoginPage 수정

**파일**: [src/pages/auth/LoginPage.tsx](../../src/pages/auth/LoginPage.tsx)

**변경 내용**:

- `useLogin` Orval hook 사용
- `PasswordLoginResponse` 타입 캐스팅 적용
- 로그인 성공 시 `setAuth()` 호출

**주요 코드**:

```typescript
const loginMutation = useLogin();

const response = await loginMutation.mutateAsync({
  data: { studentId: data.studentId, password: data.password },
});

// Blob 타입 우회 (타입 캐스팅)
const loginData = response.data as unknown as PasswordLoginResponse;

const user = {
  studentId: loginData.studentId,
  name: loginData.name,
  email: "", // PasswordLoginResponse에 email이 없음
  joinedDate: "", // PasswordLoginResponse에 joinedDate가 없음
  role: loginData.role,
};

setAuth(user, loginData.accessToken);
```

### 3. SignupPage 수정

**파일**: [src/pages/auth/SignupPage.tsx](../../src/pages/auth/SignupPage.tsx)

**변경 내용**:

- `useSignup` Orval hook 사용
- 모든 필수 필드 추가 (phoneNumber, department, grade, gender, motivation, privacyConsent)
- Null response 처리 (HTTP 상태 코드 기반)
- 회원가입 성공 시 이메일 인증 페이지로 리다이렉트 (이메일 정보 전달)
- **에러 상태 관리 및 표시**:
  - 필드별 에러 상태 관리
  - 클라이언트 측 유효성 검사
  - 백엔드 에러 메시지 파싱 및 필드 매핑

**주요 코드**:

```typescript
const [errors, setErrors] = useState<{
  studentId?: string;
  email?: string;
  phoneNumber?: string;
  // ... 기타 필드
}>({});

const handleSignup = async (data) => {
  // 에러 초기화
  setErrors({});

  // 클라이언트 측 유효성 검사
  if (data.password !== data.passwordConfirm) {
    setErrors({ passwordConfirm: '비밀번호가 일치하지 않습니다.' });
    return;
  }

  try {
    const response = await signupMutation.mutateAsync({ data });

    // 성공 처리
    navigate('/verify-email', { state: { email: data.email } });
  } catch (error) {
    // 백엔드 에러 파싱
    const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류';
    const newErrors: typeof errors = {};

    if (errorMessage.includes('이미 가입된 학번')) {
      newErrors.studentId = '이미 가입된 학번입니다.';
    }
    if (errorMessage.includes('이미 존재하는 이메일')) {
      newErrors.email = '이미 존재하는 이메일입니다.';
    }
    if (errorMessage.includes('이미 등록된 전화번호')) {
      newErrors.phoneNumber = '이미 등록된 전화번호입니다.';
    }

    setErrors(newErrors);
  }
};

// AuthForm에 errors 전달
<AuthForm errors={errors} />
```

### 4. VerifyEmailPage 추가

**파일**: [src/pages/auth/VerifyEmailPage.tsx](../../src/pages/auth/VerifyEmailPage.tsx)

**새로 생성된 파일**:

- 이메일 인증 페이지 구현
- 6자리 인증 코드 입력 폼
- `useVerifyEmail`, `useResendVerification` Orval hook 사용
- 인증 코드 재발송 기능 (60초 쿨다운)
- 인증 완료 시 로그인 페이지로 리다이렉트

**주요 기능**:

```typescript
// 이메일 인증
const verifyEmailMutation = useVerifyEmail();
const response = await verifyEmailMutation.mutateAsync({
  data: { email, code },
});

// 인증 코드 재발송
const resendVerificationMutation = useResendVerification();
await resendVerificationMutation.mutateAsync({
  data: { email },
});

// 재발송 쿨다운 타이머 (60초)
const [resendCooldown, setResendCooldown] = useState(0);
```

**UI 특징**:

- 6자리 코드 입력 (숫자만, 자동 maxLength 6)
- 큰 글씨 + 중앙 정렬 + 넓은 간격 (tracking-widest)
- SignupPage에서 전달받은 이메일 자동 입력 (변경 불가)
- 재발송 버튼 쿨다운 표시 (예: "인증 코드 재발송 (45초)")
- 로그인 페이지로 이동 링크

### 5. AuthForm 수정

**파일**: [src/components/feature/auth/AuthForm.tsx](../../src/components/feature/auth/AuthForm.tsx)

**변경 내용**:

- 회원가입 필수 필드 추가:
  - 전화번호 (phoneNumber)
  - 학과 (department)
  - 학년 (grade)
  - 성별 (gender - MALE/FEMALE select)
  - 가입 동기 (motivation - textarea)
  - 개인정보 동의 (privacyConsent - checkbox)
- **에러 표시 기능 추가**:
  - `errors` prop 추가 (필드별 에러 메시지)
  - 에러가 있는 필드에 빨간 테두리 표시
  - Input 아래에 에러 메시지 표시

**주요 코드**:

```typescript
interface AuthFormData {
  studentId: string;
  name: string;
  email: string;
  password: string;
  passwordConfirm: string;
  phoneNumber?: string;
  department?: string;
  motivation?: string;
  gender?: 'MALE' | 'FEMALE';
  grade?: number;
  privacyConsent?: boolean;
}

interface AuthFormProps {
  // ... 기존 props
  errors?: {
    studentId?: string;
    name?: string;
    email?: string;
    phoneNumber?: string;
    password?: string;
    passwordConfirm?: string;
    department?: string;
    grade?: string;
    gender?: string;
    motivation?: string;
    privacyConsent?: string;
  };
}

// 에러 표시 예시
<Input
  className={`... ${
    errors.studentId
      ? 'border-red-500 focus:border-red-500'
      : 'focus:border-primary border-border'
  }`}
/>
{errors.studentId && (
  <p className="mt-1 text-sm text-red-500">{errors.studentId}</p>
)}
```

### 6. client.ts 수정

**파일**: [src/api/client.ts](../../src/api/client.ts)

**변경 내용**:

- 이메일 인증 엔드포인트를 public endpoint로 추가
- 자동 토큰 갱신에서 제외 (로그인 전 사용)

**주요 코드**:

```typescript
const isPublicEndpoint =
  url.includes("/auth/password/login") ||
  url.includes("/auth/password/signup") ||
  url.includes("/auth/password/refresh") ||
  url.includes("/auth/password/verify-email") || // 추가
  url.includes("/auth/password/resend-verification"); // 추가
```

### 7. InquiryPage 수정

**파일**: [src/pages/inquiry/InquiryPage.tsx](../../src/pages/inquiry/InquiryPage.tsx)

**변경 내용**:

- Mock 데이터 제거
- `useGetMyInquiries` hook으로 문의 목록 조회
- `useCreateMemberInquiry` hook으로 문의 작성
- 페이지네이션 0-based 변환 (`page: currentPage - 1`)
- 필드명 매핑: UI의 `type` → API의 `type` (TYPE_MAPPING 사용)

**주요 코드**:

```typescript
// 문의 유형 매핑 (UI → API)
const TYPE_MAPPING: Record<string, string> = {
  signup: "JOIN",
  event: "GENERAL",
  report: "BUG_REPORT",
  account: "GENERAL",
  other: "GENERAL",
};

// 문의 목록 조회 (페이지네이션: 0-based)
const {
  data: response,
  isLoading,
  refetch,
} = useGetMyInquiries({
  page: currentPage - 1,
  size: 10,
});

// 문의 작성
const createMutation = useCreateMemberInquiry();

const handleSubmit = async (data) => {
  const apiType = TYPE_MAPPING[data.type] || "GENERAL";

  await createMutation.mutateAsync({
    data: {
      title: data.title,
      content: data.content,
      type: apiType,
    },
  });

  refetch(); // 목록 새로고침
};
```

## 알려진 이슈

### 1. Blob 응답 타입 문제

**증상**: Orval이 생성한 Response 타입이 `Blob`으로 정의됨

**영향 받는 API**:

- `useLogin` (Login API)
- `useSignup` (Signup API)
- `useVerifyEmail` (Email Verification API)
- `useGetMyInquiries` (Inquiries API)
- `useCreateMemberInquiry` (Inquiries API)

**임시 해결 방법**:

```typescript
const loginData = response.data as unknown as PasswordLoginResponse;
```

**장기 해결**: 백엔드 OpenAPI spec 수정 필요

### 2. refreshToken 응답 누락

**증상**: Login 응답에 `refreshToken` 필드가 없음

**추정 원인**: httpOnly cookie로 전달

**대응**:

- `authStore.setAuth()`에서 `refreshToken`을 optional로 처리
- `customFetch`가 `credentials: 'include'`로 설정되어 있어 쿠키는 자동 전송됨

### 3. PasswordLoginResponse 필드 누락

**증상**: `email`, `joinedDate` 필드가 응답에 없음

**대응**: 빈 문자열로 초기화

```typescript
const user = {
  studentId: loginData.studentId,
  name: loginData.name,
  email: "", // 응답에 없음
  joinedDate: "", // 응답에 없음
  role: loginData.role,
};
```

**장기 해결**: 백엔드에서 필드 추가 또는 User 인터페이스 수정 필요

### 4. Signup 응답 Null 처리

**증상**: Signup API가 성공 시 null response body 반환

**대응**: HTTP 상태 코드로 성공 여부 판단

```typescript
// response.data가 null이어도 정상
// HTTP status가 201 Created 또는 200 OK면 성공
console.log("Signup success:", response);
alert("회원가입이 완료되었습니다!");
```

### 5. 이메일 인증 필수

**증상**: 로그인 시 "이메일 인증이 완료되지 않았습니다" 에러 (403 Forbidden)

**인증 플로우**:

1. 회원가입 → 이메일로 6자리 코드 발송
2. VerifyEmailPage에서 코드 입력
3. 인증 완료 후 로그인 가능

**에러 처리**:

```typescript
// LoginPage.tsx
if (errorMessage.includes("이메일 인증")) {
  alert(
    "이메일 인증이 완료되지 않았습니다.\n\n회원가입 시 입력하신 이메일에서 인증 메일을 확인해주세요.",
  );
}
```

### 6. 문의 유형 매핑

**증상**: UI와 API의 문의 유형 값이 다름

**매핑**:
| UI 값 | API 값 |
|--------|---------|
| `signup` | `JOIN` |
| `event` | `GENERAL` |
| `report` | `BUG_REPORT` |
| `account` | `GENERAL` |
| `other` | `GENERAL` |

**대응**: `TYPE_MAPPING` 객체로 변환

## 브라우저 테스트 가이드 (사용자 수행)

### 준비 단계

1. **개발 서버 실행**

   ```bash
   cd frontend
   npm run dev
   ```

2. **브라우저 개발자 도구 열기** (F12)
   - Network 탭 활성화
   - Console 탭 활성화

---

### Test 1: 회원가입 & 이메일 인증

#### 1.1 정상 회원가입

**시나리오**: 새로운 계정 생성

1. 브라우저에서 `http://localhost:5173/signup` 접속
2. 폼 입력:
   - 학번: `12345678`
   - 이름: `홍길동`
   - 이메일: `test@inha.edu`
   - 전화번호: `010-1234-5678`
   - 학과: `컴퓨터공학과`
   - 학년: `3`
   - 성별: `남성` 선택
   - 가입 동기: `프로그래밍에 관심이 많아서 지원합니다`
   - 비밀번호: `Test1234!@`
   - 비밀번호 확인: `Test1234!@`
   - 개인정보 동의: 체크
3. "회원가입" 버튼 클릭
4. **예상 결과**:
   - 회원가입 성공 메시지 + 이메일 인증 안내
   - 이메일 인증 페이지(`/verify-email`)로 리다이렉트
5. **Network 탭 확인**:
   - 요청 URL: `POST .../api/v1/auth/password/signup`
   - Status: 201 Created
   - Request Body에 모든 필드 포함
   - Response Body: null (정상)
6. **Console 탭 확인**:
   - 에러 없음

#### 1.2 이메일 인증

**시나리오**: 이메일로 받은 6자리 코드 입력

1. 회원가입 후 자동으로 `/verify-email` 페이지로 이동
2. 이메일 주소가 자동으로 입력되어 있음 (변경 불가)
3. 이메일에서 받은 6자리 코드 입력 (예: `123456`)
4. "인증 확인" 버튼 클릭
5. **예상 결과**:
   - 이메일 인증 성공 메시지
   - 로그인 페이지로 리다이렉트
6. **Network 탭 확인**:
   - 요청 URL: `POST .../api/v1/auth/password/verify-email`
   - Request Body: `{ email: "test@inha.edu", code: "123456" }`
   - Status: 200 OK

#### 1.3 인증 코드 재발송

**시나리오**: 코드를 받지 못했거나 만료된 경우

1. `/verify-email` 페이지에서 "인증 코드 재발송" 버튼 클릭
2. **예상 결과**:
   - "인증 코드가 재발송되었습니다" 메시지
   - 버튼이 60초 쿨다운 ("인증 코드 재발송 (59초)" 형태로 표시)
   - 새로운 코드가 이메일로 발송됨
3. **Network 탭 확인**:
   - 요청 URL: `POST .../api/v1/auth/password/resend-verification`
   - Request Body: `{ email: "test@inha.edu" }`
   - Status: 200 OK

#### 1.4 중복 학번 에러 처리

**시나리오**: 이미 존재하는 학번으로 가입 시도

1. 위와 동일한 학번으로 다시 가입 시도
2. **예상 결과**:
   - **학번 input에 빨간 테두리 표시**
   - **학번 input 아래에 "이미 가입된 학번입니다." 에러 메시지 표시**
   - Status: 409 Conflict
3. **UI 확인**:
   - 에러가 있는 필드만 빨간 테두리
   - 다른 필드는 정상 테두리 유지
   - 에러 메시지는 해당 input 바로 아래에 빨간색 텍스트로 표시

#### 1.5 중복 이메일 에러 처리

**시나리오**: 이미 사용 중인 이메일로 가입 시도

1. 기존 회원이 사용 중인 이메일로 가입 시도
2. **예상 결과**:
   - **이메일 input에 빨간 테두리**
   - **"이미 존재하는 이메일입니다." 에러 메시지**
   - Status: 409 Conflict

#### 1.6 중복 전화번호 에러 처리

**시나리오**: 이미 등록된 전화번호로 가입 시도

1. 기존 회원의 전화번호로 가입 시도
2. **예상 결과**:
   - **전화번호 input에 빨간 테두리**
   - **"이미 등록된 전화번호입니다." 에러 메시지**
   - Status: 409 Conflict

#### 1.7 비밀번호 불일치 에러

**시나리오**: 비밀번호와 비밀번호 확인이 일치하지 않음

1. 회원가입 폼에서 비밀번호: `Test1234!@`, 비밀번호 확인: `Test1234!` 입력
2. **예상 결과**:
   - **비밀번호 확인 input에 빨간 테두리**
   - **"비밀번호가 일치하지 않습니다." 에러 메시지**
   - 서버 요청 전에 클라이언트 측에서 차단

---

### Test 2: 로그인

#### 2.1 이메일 인증 전 로그인 시도

**시나리오**: 이메일 인증을 하지 않고 로그인 시도

1. 회원가입만 하고 이메일 인증을 건너뛴 상태
2. 로그인 페이지에서 로그인 시도
3. **예상 결과**:
   - "이메일 인증이 완료되지 않았습니다" 에러 메시지
   - Status: 403 Forbidden

#### 2.2 정상 로그인 (이메일 인증 후)

**시나리오**: 이메일 인증 완료 후 로그인

1. 브라우저에서 `http://localhost:5173/login` 접속
2. 폼 입력:
   - 학번: `12345678`
   - 비밀번호: `Test1234!@`
3. "로그인" 버튼 클릭
4. **예상 결과**:
   - 로그인 성공
   - 메인 페이지로 리다이렉트
   - authStore에 user, accessToken 저장됨
5. **Network 탭 확인**:
   - 요청 URL: `POST .../api/v1/auth/password/login`
   - Status: 200 OK
   - Response Body: `{ accessToken: "...", userId: ..., studentId: "...", name: "...", role: "..." }`
   - **주의**: refreshToken이 응답에 없고 Set-Cookie 헤더로 전달될 수 있음
6. **Console 탭 확인**:
   - 에러 없음

#### 2.2 잘못된 비밀번호

**시나리오**: 틀린 비밀번호로 로그인 시도

1. 학번: `12345678`, 비밀번호: `WrongPassword`
2. **예상 결과**:
   - "학번 또는 비밀번호가 일치하지 않습니다" 에러
   - Status: 401 Unauthorized

---

### Test 3: 문의 시스템

#### 3.1 내 문의 목록 조회 (로그인 필수)

**시나리오**: 로그인 후 문의 페이지 접속

1. 로그인 상태에서 `/inquiries` 접속
2. **예상 결과**:
   - 내 문의 목록 표시 (비어있을 수 있음)
   - 로딩 상태 → 데이터 표시
3. **Network 탭 확인**:
   - 요청 URL: `GET .../api/v1/inquiries/my?page=0&size=10`
   - Request Headers에 `Authorization: Bearer <token>` 포함
   - Status: 200 OK

#### 3.2 문의 작성

**시나리오**: 새 문의 작성

1. "새 문의 작성" 탭 클릭
2. 폼 입력:
   - 문의 유형: `가입/입부 문의` 선택
   - 제목: `테스트 문의`
   - 내용: `문의 내용 테스트입니다`
3. "문의 제출" 버튼 클릭
4. **예상 결과**:
   - 문의 작성 성공 메시지
   - "문의 내역 보기" 탭으로 자동 전환
   - 목록에 새 문의 추가됨
5. **Network 탭 확인**:
   - 요청 URL: `POST .../api/v1/inquiries/member`
   - Request Body: `{ title: "...", content: "...", type: "JOIN" }`
   - **중요**: UI의 `signup`이 `JOIN`으로 변환되었는지 확인
   - Status: 201 Created
6. **Console 탭 확인**:
   - React Query 캐시 invalidate 확인 (목록 자동 갱신)

#### 3.3 로그인하지 않은 상태에서 문의 페이지 접근

**시나리오**: 비로그인 상태에서 접근

1. 로그아웃 또는 시크릿 모드에서 `/inquiries` 접속
2. **예상 결과**:
   - 로그인 페이지로 리다이렉트
   - 또는 401 Unauthorized 에러
3. **Network 탭 확인**:
   - Status: 401 Unauthorized

---

## 검증 체크리스트

### TypeScript 컴파일

```bash
cd frontend
npm run build
```

- [x] 빌드 에러 없음

### 기능 체크

**Auth**:

- [ ] 회원가입 (정상, 중복 에러, 모든 필수 필드 입력)
- [ ] **에러 표시 UI**:
  - [ ] 중복 학번: 학번 필드에 빨간 테두리 + 에러 메시지
  - [ ] 중복 이메일: 이메일 필드에 빨간 테두리 + 에러 메시지
  - [ ] 중복 전화번호: 전화번호 필드에 빨간 테두리 + 에러 메시지
  - [ ] 비밀번호 불일치: 비밀번호 확인 필드에 빨간 테두리 + 에러 메시지
- [ ] 이메일 인증 (6자리 코드 입력, 인증 성공)
- [ ] 인증 코드 재발송 (60초 쿨다운)
- [ ] 이메일 인증 전 로그인 시도 (403 에러)
- [ ] 이메일 인증 후 로그인 (정상, 틀린 비밀번호)
- [ ] 로그인 후 인증 필요 페이지 접근 (403 없음)

**Inquiries**:

- [ ] 내 문의 목록 조회 (로그인 필수)
- [ ] 문의 작성 (type 매핑 확인)
- [ ] 비로그인 시 401 에러

### 에러 처리

- [ ] 401 에러 (로그인 필요 시)
- [ ] 409 에러 (중복 학번, 이메일, 전화번호)
- [ ] Blob 타입 캐스팅 정상 작동
- [ ] **필드별 에러 메시지 표시** (input 아래 빨간색 텍스트)
- [ ] **에러 필드 시각적 표시** (빨간 테두리)

### 상태 관리

- [ ] authStore에 user, accessToken 저장
- [ ] Zustand persist로 새로고침 후에도 로그인 유지
- [ ] 로그아웃 시 상태 초기화

---

## 관련 파일

### 수정된 파일

- [src/stores/authStore.ts](../../src/stores/authStore.ts) - setAuth 헬퍼 수정
- [src/pages/auth/LoginPage.tsx](../../src/pages/auth/LoginPage.tsx) - useLogin hook 사용, 이메일 인증 에러 처리
- [src/pages/auth/SignupPage.tsx](../../src/pages/auth/SignupPage.tsx) - useSignup hook 사용, 모든 필드 추가, **에러 상태 관리 및 파싱**, 이메일 인증 페이지로 리다이렉트
- [src/pages/auth/VerifyEmailPage.tsx](../../src/pages/auth/VerifyEmailPage.tsx) - **신규 생성**: 이메일 인증 페이지
- [src/components/feature/auth/AuthForm.tsx](../../src/components/feature/auth/AuthForm.tsx) - 회원가입 필수 필드 추가, **errors prop 및 에러 표시 UI 추가**
- [src/api/client.ts](../../src/api/client.ts) - 이메일 인증 엔드포인트를 public endpoint로 추가
- [src/pages/inquiry/InquiryPage.tsx](../../src/pages/inquiry/InquiryPage.tsx) - useGetMyInquiries, useCreateMemberInquiry hook 사용
- [src/router.tsx](../../src/router.tsx) - `/verify-email` 경로 추가 (이미 존재)

### 참조 파일

**Orval 생성 API**:

- [src/api/model/password-authentication/password-authentication.ts](../../src/api/model/password-authentication/password-authentication.ts) - Auth hooks
- [src/api/model/inquiry/inquiry.ts](../../src/api/model/inquiry/inquiry.ts) - Inquiry hooks

**기존 인프라**:

- [src/api/client.ts](../../src/api/client.ts) - customFetch
- [.env](../../.env) - 환경변수

---

## 다음 단계

### 단기 (즉시 수행)

1. 사용자 브라우저 테스트 수행
2. 테스트 결과에 따라 버그 수정
3. 로그아웃 기능 구현 (`useLogout` hook)
4. 토큰 갱신 기능 구현 (`useRefreshToken` hook)

### 중기 (백엔드 협의 필요)

1. OpenAPI spec 수정하여 Blob 타입 문제 해결
2. PasswordLoginResponse에 `email`, `joinedDate` 필드 추가
3. refreshToken을 응답 body에 포함할지 결정

### 장기 (최적화)

1. 불필요한 파일 삭제:
   - `src/api/auth.ts` (사용되지 않음)
   - `src/api/inquiries.ts` (사용되지 않음)
2. React Query 캐시 전략 최적화
3. 에러 핸들링 개선 (Toast 메시지 사용)

---

## 참고 자료

- [Orval 마이그레이션 플랜](../../../C:/Users/hwang/.claude/plans/scalable-skipping-crown.md)
- [Posts API 마이그레이션 문서](./orval-api-migration.md) - 이전 마이그레이션 사례
