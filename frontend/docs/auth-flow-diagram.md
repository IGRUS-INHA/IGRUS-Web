# Auth 페이지 사용자 플로우

> 완료일: 2026-01-30

## 페이지 구조

```
frontend/src/pages/auth/
├── LoginPage.jsx          (108 lines)
├── SignupPage.jsx         (191 lines)
├── VerifyEmailPage.jsx    (158 lines)
├── ForgotPasswordPage.jsx (124 lines)
├── ResetPasswordPage.jsx  (132 lines)
└── index.js               (exports)
```

## 사용자 플로우 다이어그램

### 1. 회원가입 플로우

```
┌─────────────────┐
│                 │
│   SignupPage    │  /signup
│  (회원가입)      │
│                 │
└────────┬────────┘
         │
         │ 1. 학번, 이름, 이메일, 비밀번호 입력
         │ 2. 약관 동의
         │ 3. authApi.signup()
         │
         ▼
┌─────────────────┐
│ VerifyEmailPage │  /verify-email?email={email}
│  (이메일 인증)   │
│                 │
└────────┬────────┘
         │
         │ 1. 6자리 코드 입력
         │ 2. 타이머 확인 (5분)
         │ 3. authApi.verifyEmail()
         │
         ▼
┌─────────────────┐
│   LoginPage     │  /login?verified=true
│    (로그인)      │
│                 │
└────────┬────────┘
         │
         │ authStore.login()
         │
         ▼
┌─────────────────┐
│    HomePage     │  /
│                 │
└─────────────────┘
```

### 2. 로그인 플로우

```
┌─────────────────┐
│   LoginPage     │  /login
│    (로그인)      │
│                 │
└────────┬────────┘
         │
         │ 1. 학번, 비밀번호 입력
         │ 2. authStore.login()
         │ 3. 로그인 성공
         │
         ▼
┌─────────────────┐
│    HomePage     │  /
│                 │
└─────────────────┘
```

### 3. 비밀번호 재설정 플로우

```
┌─────────────────┐
│   LoginPage     │  /login
│                 │
│  "Forgot Pass?" │
└────────┬────────┘
         │
         │ Link 클릭
         │
         ▼
┌─────────────────┐
│ForgotPasswordPg │  /forgot-password
│(재설정 요청)     │
│                 │
└────────┬────────┘
         │
         │ 1. 학번 입력
         │ 2. authApi.requestPasswordReset()
         │ 3. 성공 화면 표시
         │
         ▼
┌─────────────────┐
│   이메일 확인    │
│                 │
│  "링크 클릭"    │
└────────┬────────┘
         │
         │ 이메일 링크 클릭
         │
         ▼
┌─────────────────┐
│ResetPasswordPg  │  /reset-password?token={token}
│(새 비밀번호)     │
│                 │
└────────┬────────┘
         │
         │ 1. 새 비밀번호 입력
         │ 2. 비밀번호 확인
         │ 3. authApi.resetPassword()
         │
         ▼
┌─────────────────┐
│   LoginPage     │  /login
│    (로그인)      │
│                 │
└─────────────────┘
```

## 페이지 간 네비게이션 맵

```
                    ┌─────────────────┐
                    │    HomePage     │
                    │       /         │
                    └────────┬────────┘
                             │
                ┌────────────┴────────────┐
                │                         │
      ┌─────────▼────────┐     ┌─────────▼────────┐
      │   LoginPage      │     │   SignupPage     │
      │     /login       │◄────┤     /signup      │
      └─────┬────────────┘     └─────┬────────────┘
            │                        │
            │                        │
            │                        ▼
            │              ┌─────────────────┐
            │              │ VerifyEmailPage │
            │              │ /verify-email   │
            │              └────────┬────────┘
            │                       │
            │◄──────────────────────┘
            │
            │
      ┌─────▼────────────┐
      │ForgotPasswordPage│
      │ /forgot-password │
      └─────┬────────────┘
            │
            │ (이메일 링크)
            │
      ┌─────▼────────────┐
      │ResetPasswordPage │
      │ /reset-password  │
      └─────┬────────────┘
            │
            │
      ┌─────▼────────────┐
      │   LoginPage      │
      │     /login       │
      └──────────────────┘
```

## 페이지별 라우팅 연결

### LoginPage (`/login`)

**나가는 링크:**
- `/signup` - "Don't have an account? Sign up"
- `/forgot-password` - "Forgot Password?"

**성공 시 이동:**
- `/` - 로그인 성공 후 홈으로

### SignupPage (`/signup`)

**나가는 링크:**
- `/login` - "Already have an account? Log in"

**성공 시 이동:**
- `/verify-email?email={email}` - 회원가입 완료 후 이메일 인증

### VerifyEmailPage (`/verify-email`)

**들어오는 쿼리:**
- `?email={email}` - 인증할 이메일 주소

**나가는 링크:**
- `/signup` - "Back" 버튼

**성공 시 이동:**
- `/login?verified=true` - 인증 완료 후 로그인

### ForgotPasswordPage (`/forgot-password`)

**나가는 링크:**
- `/login` - "Back" 버튼 또는 "Back to Login"

**성공 시:**
- 페이지 내에서 성공 화면 표시
- 이메일로 리셋 링크 발송

### ResetPasswordPage (`/reset-password`)

**들어오는 쿼리:**
- `?token={token}` - 비밀번호 재설정 토큰

**나가는 링크:**
- `/login` - "Back" 버튼

**성공 시 이동:**
- `/login` - 비밀번호 변경 완료 후 로그인

## 상태 관리 흐름

### 전역 상태 (Zustand - authStore)

```jsx
// frontend/src/stores/authStore.js
{
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,

  login: async (studentId, password) => {
    // API 호출
    // 상태 업데이트
    set({ user, accessToken, refreshToken, isAuthenticated: true });
  },

  logout: () => {
    set({ user: null, accessToken: null, isAuthenticated: false });
  },
}
```

**사용 페이지:**
- LoginPage: `login()` 액션 호출

### 로컬 상태 (useState)

각 페이지가 자신의 폼 상태를 관리:

```jsx
// LoginPage
const [studentId, setStudentId] = useState('');
const [password, setPassword] = useState('');
const [loading, setLoading] = useState(false);
const [error, setError] = useState('');

// SignupPage
const [form, setForm] = useState({ studentId, name, email, password, passwordConfirm });
const [agreedToTerms, setAgreedToTerms] = useState(false);

// VerifyEmailPage
const [verificationCode, setVerificationCode] = useState('');
const [timeLeft, setTimeLeft] = useState(300);

// ForgotPasswordPage
const [studentId, setStudentId] = useState('');
const [success, setSuccess] = useState(false);

// ResetPasswordPage
const [password, setPassword] = useState('');
const [passwordConfirm, setPasswordConfirm] = useState('');
```

## API 호출 시퀀스

### 회원가입 플로우

```
User                SignupPage           API Server          VerifyEmailPage
  │                     │                     │                     │
  ├─1.폼입력─────────►│                     │                     │
  │                     │                     │                     │
  ├─2.제출클릭────────►│                     │                     │
  │                     ├─3.POST /auth/signup►│                     │
  │                     │◄─4.success──────────┤                     │
  │                     │                     │                     │
  │◄─5.navigate────────┤                     │                     │
  │   (/verify-email?email=...)              │                     │
  │                     │                     │                     │
  ├─6.코드입력─────────┼─────────────────────┼────────────────────►│
  │                     │                     │                     │
  ├─7.제출클릭─────────┼─────────────────────┼────────────────────►│
  │                     │                     │◄─8.POST /auth/      │
  │                     │                     │   signup/verify     │
  │                     │                     ├─9.success──────────►│
  │◄─10.navigate───────┼─────────────────────┼─────────────────────┤
      (/login?verified=true)                 │                     │
```

### 로그인 플로우

```
User                LoginPage            authStore           API Server
  │                     │                     │                  │
  ├─1.학번/비번입력────►│                     │                  │
  │                     │                     │                  │
  ├─2.로그인클릭───────►│                     │                  │
  │                     ├─3.login()──────────►│                  │
  │                     │                     ├─4.POST /auth/────►│
  │                     │                     │    login          │
  │                     │                     │◄─5.tokens─────────┤
  │                     │◄─6.success─────────┤                  │
  │◄─7.navigate────────┤  (isAuthenticated=true)                │
      (/)               │                     │                  │
```

### 비밀번호 재설정 플로우

```
User          ForgotPwPage    API Server    Email       ResetPwPage
  │                │              │            │              │
  ├─1.학번입력────►│              │            │              │
  │                ├─2.POST /auth/►            │              │
  │                │  reset-request│            │              │
  │                │◄─3.success────┤            │              │
  │                │              ├─4.send mail►│              │
  │◄─5.성공화면────┤              │            │              │
  │                │              │            │              │
  ├─6.이메일확인───┼──────────────┼────────────►│              │
  │                │              │            │              │
  ├─7.링크클릭─────┼──────────────┼────────────┴─────────────►│
  │   (/reset-password?token=...)              │              │
  │                │              │            │              │
  ├─8.새비번입력───┼──────────────┼────────────┼─────────────►│
  │                │              │◄─9.POST /auth/            │
  │                │              │   password/reset          │
  │                │              ├─10.success────────────────►│
  │◄─11.navigate───┼──────────────┼────────────┼──────────────┤
      (/login)      │              │            │              │
```

## 에러 처리 패턴

각 페이지는 동일한 에러 처리 패턴을 사용합니다:

```jsx
const [error, setError] = useState('');

const handleSubmit = async (e) => {
  e.preventDefault();
  setError(''); // 에러 초기화

  try {
    // 클라이언트 측 유효성 검사
    if (!title.trim()) {
      setError('Title is required');
      return;
    }

    // API 호출
    await authApi.someMethod();

    // 성공 시 페이지 이동
    navigate('/next-page');
  } catch (err) {
    // 서버 에러 처리
    setError(err.response?.data?.message || 'Operation failed');
  }
};

return (
  <form onSubmit={handleSubmit}>
    {error && (
      <div className="bg-destructive/10 border border-destructive/20 text-destructive text-sm rounded-r3 p-3">
        {error}
      </div>
    )}
    {/* ... */}
  </form>
);
```

## 로딩 상태 패턴

모든 폼 제출 시 로딩 상태를 표시합니다:

```jsx
const [loading, setLoading] = useState(false);

const handleSubmit = async (e) => {
  e.preventDefault();
  setLoading(true); // 로딩 시작

  try {
    await authApi.someMethod();
    navigate('/next-page');
  } catch (err) {
    setError(err.message);
  } finally {
    setLoading(false); // 로딩 종료
  }
};

return (
  <Button type="submit" disabled={loading}>
    {loading ? 'Loading...' : 'Submit'}
  </Button>
);
```

## 접근성 고려사항

### 키보드 네비게이션

모든 폼 요소가 Tab 키로 순차적으로 접근 가능:

```
Tab 순서:
1. 첫 번째 입력 필드
2. 두 번째 입력 필드
3. ...
4. 제출 버튼
5. 링크 (회원가입, 비밀번호 찾기 등)
```

### 폼 검증

HTML5 폼 검증 사용:

```jsx
<Input
  type="email"
  required
  minLength={8}
  maxLength={8}
  pattern="[0-9]{8}"
/>
```

### 에러 메시지

명확하고 구체적인 에러 메시지:

- "비밀번호가 일치하지 않습니다."
- "약관에 동의해주세요."
- "비밀번호는 최소 8자 이상이어야 합니다."
- "인증 코드는 6자리 숫자입니다."

## 모바일 반응형

모든 페이지가 모바일 친화적:

```jsx
<div className="min-h-screen flex items-center justify-center bg-background p-4">
  <div className="max-w-md w-full">
    {/* 모바일에서도 읽기 쉬운 크기 */}
  </div>
</div>
```

- 터치 친화적 버튼 크기 (py-4)
- 여백 확보 (p-4)
- 최대 너비 제한 (max-w-md)
- 반응형 텍스트 크기

## 애니메이션

부드러운 페이지 전환:

```jsx
<div className="animate-in slide-in-from-bottom-8 duration-500">
  {/* 아래에서 위로 슬라이드 인 */}
</div>
```

- 0.5초 지속
- 2rem (8 * 0.25rem) 아래에서 시작
- fade-in 효과 포함

## 보안 고려사항

### 1. 토큰 처리
- 쿼리 파라미터로 토큰 전달 (ResetPasswordPage)
- 이메일로 전달된 토큰 사용

### 2. 비밀번호 요구사항
- 최소 8자
- 대소문자 + 숫자 + 특수문자 권장

### 3. 타이머 제한
- 이메일 인증 코드 5분 제한
- 만료 후 재발송 가능

### 4. 에러 메시지
- 보안을 위해 일반적인 메시지 사용
- 예: "로그인에 실패했습니다" (학번/비밀번호 구분 없음)

## 결론

AuthView를 5개의 독립적인 페이지로 분리하여 다음을 달성했습니다:

1. **명확한 사용자 플로우**: 각 단계가 독립된 페이지
2. **유지보수성**: 페이지별 독립적 수정 가능
3. **확장성**: 새로운 인증 방식 추가 용이
4. **일관성**: 공통 레이아웃 패턴 사용
5. **사용자 경험**: 타이머, 재발송, 약관 동의 등 세부 기능

모든 페이지가 React Router와 통합되어 SPA의 이점을 최대한 활용합니다.
