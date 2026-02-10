# React Hook Form 마이그레이션 가이드

폼이 존재하지만 React Hook Form(RHF)이 적용되지 않은 파일을 정리한 문서입니다.
RHF 마이그레이션 시 참고하세요.

---

## 현황

### RHF 적용 완료 (6개)

| 파일 | 경로 |
|------|------|
| PostEditPage.tsx | `pages/board/` |
| PostWritePage.tsx | `pages/board/` |
| WithdrawPage.tsx | `pages/mypage/` |
| ChangePasswordPage.tsx | `pages/mypage/` |
| EventWritePage.tsx | `pages/event/` |
| EventEditPage.tsx | `pages/event/` |

### RHF 미적용 (15개)

`<form>`, `onSubmit`, `handleSubmit` 등 폼 관련 코드가 있지만 `useForm`을 사용하지 않는 파일입니다.

| 파일 | 경로 | 우선순위 |
|------|------|----------|
| SignupPage.tsx | `pages/auth/` | 높음 |
| LoginPage.tsx | `pages/auth/` | 높음 |
| AuthForm.tsx | `components/feature/auth/` | 높음 |
| VerifyEmailPage.tsx | `pages/auth/` | 높음 |
| ForgotPasswordPage.tsx | `pages/auth/` | 높음 |
| ResetPasswordPage.tsx | `pages/auth/` | 높음 |
| InquiryPage.tsx | `pages/inquiry/` | 높음 |
| InquiryForm.tsx | `components/feature/inquiry/` | 높음 |
| CommentSection.tsx | `components/feature/comment/` | 보통 |
| CommentItem.tsx | `components/feature/comment/` | 보통 |
| CommentInput.tsx | `components/feature/comment/` | 보통 |
| ReportModal.tsx | `components/board/` | 보통 |
| SearchBar.tsx | `components/board/` | 낮음 |
| UsersTab.tsx | `pages/admin/tabs/` | 낮음 |
| LoginHistoryTab.tsx | `pages/admin/tabs/` | 낮음 |

---

## 우선순위 기준

- **높음**: 입력 필드가 많고, 유효성 검증이 필요한 폼 (인증, 문의)
- **보통**: 단일 입력이지만 UX 개선 여지가 있는 폼 (댓글, 신고)
- **낮음**: 검색/필터 용도로 RHF 도입 실익이 적은 폼

---

## 마이그레이션 순서 (권장)

### 1단계: 인증 관련 (6개)

SignupPage, LoginPage, AuthForm, VerifyEmailPage, ForgotPasswordPage, ResetPasswordPage

- 입력 필드가 가장 많고 유효성 검증 로직이 복잡함
- AuthForm은 LoginPage와 SignupPage에서 공유하는 공통 컴포넌트이므로 먼저 처리

### 2단계: 문의 폼 (2개)

InquiryPage, InquiryForm

- 다수의 입력 필드 + 유효성 검증 필요

### 3단계: 댓글 및 신고 (4개)

CommentSection, CommentItem, CommentInput, ReportModal

- 단일 textarea 위주이지만 에러 핸들링 통일에 유리

### 4단계: 검색/어드민 (3개) - 선택

SearchBar, UsersTab, LoginHistoryTab

- 단순 필터/검색이므로 필요시에만 적용

---

## 마이그레이션 패턴

기존 RHF 적용 파일을 참고하여 아래 패턴을 따릅니다.

### Before (useState 기반)

```typescript
const [title, setTitle] = useState('');
const [content, setContent] = useState('');
const [error, setError] = useState('');

const handleSubmit = (e: React.FormEvent) => {
  e.preventDefault();
  if (!title) {
    setError('제목을 입력하세요');
    return;
  }
  // submit logic
};
```

### After (RHF 기반)

```typescript
import { useForm } from 'react-hook-form';

interface FormData {
  title: string;
  content: string;
}

const { register, handleSubmit, formState: { errors } } = useForm<FormData>();

const onSubmit = (data: FormData) => {
  // submit logic
};
```

### 체크리스트

- [ ] `useState`로 관리하던 폼 상태를 `useForm`으로 전환
- [ ] 수동 유효성 검증을 `register` 옵션(`required`, `pattern` 등)으로 교체
- [ ] 에러 메시지를 `formState.errors`로 통일
- [ ] `e.preventDefault()` 제거 (RHF `handleSubmit`이 처리)
- [ ] 필요시 `useFormContext`로 하위 컴포넌트에 폼 상태 전달
