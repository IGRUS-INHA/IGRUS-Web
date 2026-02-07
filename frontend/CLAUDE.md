# Frontend CLAUDE.md

## 프로젝트 개요

IGRUS Web 프론트엔드 - React + Vite 기반 SPA

---

# 1. 프로젝트 운영 원칙

## 1.1 Claude 작업 지침

* 이 프로젝트는 2년 이상 유지보수 대상이므로 대충 코드 작성 금지
* 귀찮아서 만든 잘못된 코드 1개가 나중에 여러 배로 비용 증가
* **작업 완료 후 어떤 작업을 어떻게 처리했는지 항상 설명할 것**

## 1.2 공통 금지 사항 / 실행 규칙

* **절대로 개발 서버 실행 금지** (`pnpm dev` 등 금지)
* **패키지 매니저는 pnpm 고정**
* **코드 포맷팅/타입 체크 명령 실행 금지** (`pnpm format`, `pnpm lint`, `pnpm tsc --noEmit` 등 금지 - VSCode에서 자동 처리)
* **절대로 백엔드 파일을 수정하지 않는다. 읽기만 한다.** (backend/ 디렉토리 내의 모든 파일은 읽기 전용)

## 1.3 Git 커밋 규칙

* **커밋 전 반드시 사용자에게 커밋 메시지 확인 받을 것**
* 커밋 메시지를 작성한 후, 사용자에게 보여주고 승인을 받은 후에만 커밋 실행
* 사용자 승인 없이 절대 커밋하지 말 것

**커밋 프로세스:**
1. `git status`, `git diff`로 변경사항 확인
2. 커밋 메시지 초안 작성
3. **사용자에게 커밋 메시지 확인 요청 (필수)**
4. 사용자 승인 후 `git add` 및 `git commit` 실행
5. `git status`로 커밋 완료 확인

---

# 2. TypeScript 규칙

## 2.1 기본 원칙

* **경계(IO)에서 타입 확정** → 내부에는 "깨끗한 타입"만 전달
* 내부에 **`null` 유입 금지 (`undefined`만 허용)**
* 규칙을 깨면 **Exceptions 섹션에 케이스/이유/대체안 기록**

## 2.2 타입 금지 규칙

### 2.2.1 모호한 타입 금지

* ❌ `any`, `unknown`, `never` 금지

### 2.2.2 Non-null assertion 금지

* ❌ `foo!.bar` 금지

### 2.2.3 `| null` 금지 (내부 기준)

* ❌ `foo: string | null` 금지
* ✅ `foo?: string` 사용
* ✅ API 응답의 `null`은 **반드시 `undefined`로 변환 후** 내부로 전달

**예시 (API null → undefined)**

```ts
// API 응답 변환
const user = {
  id: response.id,
  name: response.name,
  nickname: response.nickname ?? undefined, // null → undefined
};
```

### 2.2.4 `as` 최소화

* ❌ 내부 로직에서 `as` 남발 금지
* ✅ 불가피하면 **IO 경계에서 1회만** 허용

## 2.3 IO 경계 규칙

* 내부 API 호출은 **Orval이 타입 처리** → 별도 검증 불필요
* 외부 API 호출 시 타입 정의 후 사용

**예시 (외부 API fetch)**

```ts
interface Oembed {
  title: string;
  author_name?: string;
}

export async function fetchOembed(url: string): Promise<Oembed> {
  const res = await fetch(`https://www.youtube.com/oembed?url=${encodeURIComponent(url)}&format=json`);
  return res.json() as Promise<Oembed>;
}
```

## 2.4 타입 선언(명시) 규칙

### 2.4.1 추론 가능하면 생략

```ts
const getConfig = () => {
  return { foo: "a", bar: "b" };
};
```

### 2.4.2 커스텀 훅 반환 타입은 명시 권장

```ts
function useUser(id: string): { user: User | undefined; isLoading: boolean } {
  // ...
}
```

## 2.5 Exceptions (예외 기록 규격)

* 금지 규칙을 깰 경우 아래 형식으로 기록

  * **규칙:**
  * **이유:**
  * **범위:**
  * **대체안:**

---

# 3. 에러 처리 규칙

## 3.1 기본 원칙

* **무조건 ApiError 클래스 사용** - 모든 API 에러는 ApiError 인스턴스
* **헬퍼 함수로 에러 체크** - `utils/error.ts`의 헬퍼 함수 사용
* **`error.message.includes()` 패턴 금지** - 메시지 기반 체크 금지
* **`(error as any)` 타입 단언 금지** - 타입 가드 사용

## 3.2 에러 타입 (ApiError)

```typescript
// types/error.ts
export class ApiError extends Error {
  public readonly status: number;   // HTTP 상태 코드
  public readonly code: string;      // 백엔드 에러 코드
  public readonly timestamp?: string;
}
```

**특징:**
* client.ts에서 모든 API 에러를 ApiError로 통일
* `code` 필드가 없으면 `HTTP_{status}` 형태로 자동 생성 (예: `HTTP_403`)
* 백엔드 ErrorResponse와 1:1 매핑

## 3.3 에러 체크 방법

### 3.3.1 헬퍼 함수 사용 (필수)

```typescript
import { isForbiddenError, isNotFoundError, getErrorMessage } from '@/utils/error';

// ✅ 올바른 방법
onError: (error: unknown) => {
  if (isForbiddenError(error)) {
    alert('권한이 없습니다.');
  } else if (isNotFoundError(error)) {
    alert('게시글을 찾을 수 없습니다.');
  } else {
    alert(getErrorMessage(error));
  }
}

// ❌ 금지된 방법
onError: (error: any) => {
  if (error.message?.includes('403')) {  // 메시지 기반 체크 금지
    alert('권한이 없습니다.');
  }
}
```

### 3.3.2 주요 헬퍼 함수

**HTTP 상태 기반:**
* `isUnauthorizedError(error)` - 401 (인증 필요)
* `isForbiddenError(error)` - 403 (권한 없음)
* `isNotFoundError(error)` - 404 (리소스 없음)
* `isConflictError(error)` - 409 (중복/충돌)
* `isRateLimitError(error)` - 429 (요청 초과)
* `isServerError(error)` - 5xx (서버 오류)

**게시판 관련:**
* `isBoardReadDenied(error)` - 게시판 읽기 권한 없음
* `isBoardWriteDenied(error)` - 게시판 쓰기 권한 없음
* `isPostNotFound(error)` - 게시글 없음
* `isPostAccessDenied(error)` - 게시글 접근 권한 없음
* `isPostDeleted(error)` - 삭제된 게시글

**댓글 관련:**
* `isCommentNotFound(error)` - 댓글 없음
* `isCommentAccessDenied(error)` - 댓글 접근 권한 없음
* `isCommentContentEmpty(error)` - 댓글 내용 비어있음
* `isCommentContentTooLong(error)` - 댓글 길이 초과

**인증 관련:**
* `isInvalidCredentials(error)` - 잘못된 인증 정보
* `isEmailNotVerified(error)` - 이메일 미인증
* `isAccountSuspended(error)` - 계정 정지
* `isAccountWithdrawn(error)` - 계정 탈퇴
* `isTokenExpired(error)` - 토큰 만료

**행사 관련:**
* `isEventNotFound(error)` - 행사 없음
* `isEventAlreadyRegistered(error)` - 이미 신청한 행사
* `isEventCapacityFull(error)` - 행사 정원 마감
* `isEventRegistrationClosed(error)` - 신청 기간 종료

**기타:**
* `getErrorMessage(error)` - 사용자 친화적 메시지 추출
* `getErrorCode(error)` - 에러 코드 추출
* `hasErrorCode(error, code)` - 특정 에러 코드 확인

전체 목록은 `utils/error.ts` 참조 (45개 함수)

## 3.4 에러 처리 예시

### 3.4.1 게시판 목록 조회

```typescript
import { isBoardReadDenied } from '@/utils/error';

const { data, error } = useGetPostList(...);
const isForbidden = isBoardReadDenied(error);

if (isForbidden) {
  return <div>정회원 승인 후 게시판 이용이 가능합니다.</div>;
}
```

### 3.4.2 게시글 삭제

```typescript
import { isForbiddenError, isNotFoundError, getErrorMessage } from '@/utils/error';

const { mutate } = useDeletePost({
  mutation: {
    onError: (error: unknown) => {
      if (isForbiddenError(error)) {
        alert('삭제 권한이 없습니다.');
      } else if (isNotFoundError(error)) {
        alert('게시글을 찾을 수 없습니다.');
      } else {
        alert(getErrorMessage(error));
      }
    },
  },
});
```

### 3.4.3 게시글 작성

```typescript
import { isForbiddenError, isUnauthorizedError, getErrorMessage } from '@/utils/error';

const { mutate } = useCreatePost({
  mutation: {
    onError: (error: unknown) => {
      if (isForbiddenError(error)) {
        alert('권한이 없습니다.\n정회원 이상만 작성 가능합니다.');
      } else if (isUnauthorizedError(error)) {
        alert('로그인이 필요합니다.');
        navigate('/login');
      } else {
        alert(getErrorMessage(error));
      }
    },
  },
});
```

## 3.5 마이그레이션 (점진적 개선)

**현재 상태 (Phase 2 완료):**
* ✅ types/error.ts - ApiError 클래스
* ✅ utils/error.ts - 45개 헬퍼 함수, 148개 에러 코드 매핑
* ✅ client.ts - ApiError 사용, default 코드 생성
* ✅ 게시판 페이지 (BoardListPage, PostDetailPage, PostWritePage, PostEditPage)

**향후 마이그레이션 대상:**
* ⬜ 인증 페이지 (LoginPage, SignupPage 등)
* ⬜ 행사 페이지 (EventListPage, EventDetailPage 등)
* ⬜ 문의 페이지 (InquiryPage)
* ⬜ 기타 페이지 및 컴포넌트

**마이그레이션 규칙:**
* 새 기능 개발 시: **무조건 새 방식(ApiError, 헬퍼 함수) 사용**
* 버그 수정 시: 해당 파일 마이그레이션
* 리팩토링 세션: 도메인별로 일괄 마이그레이션

**자세한 마이그레이션 가이드:**
* [docs/migration/error-handling-migration.md](docs/migration/error-handling-migration.md)
* [docs/testing/error-handling-test.md](docs/testing/error-handling-test.md)

## 3.6 금지 패턴

```typescript
// ❌ 금지 1: any 타입
onError: (error: any) => { ... }

// ❌ 금지 2: 메시지 기반 체크
if (error.message?.includes('403')) { ... }

// ❌ 금지 3: 타입 단언
if ((error as any).code === 'BOARD_READ_DENIED') { ... }

// ❌ 금지 4: Error 타입 단언
if ((error as Error).message?.includes('권한')) { ... }

// ✅ 올바른 방법: 헬퍼 함수 사용
if (isForbiddenError(error)) { ... }
if (isBoardReadDenied(error)) { ... }
```

## 3.7 레이어별 에러 처리 패턴 (관심사의 분리)

에러 처리는 **레이어별 책임**에 따라 다르게 구현합니다:

### **Data Hook Layer** - Graceful Degradation

**목적**: API 실패 시에도 앱이 완전히 깨지지 않도록 폴백 데이터 제공

```typescript
// ✅ 올바른 예: useBoards.ts
export function useBoardList() {
  const { data, error, isLoading } = useGetBoardList();
  const { user } = useAuthStore();

  if (error || !data) {
    // 폴백 데이터로 최소 기능 제공
    return {
      boards: FALLBACK_BOARDS.map(board => ({
        ...board,
        canRead: canViewBoard(user?.role, board.code),
        canWrite: canWriteBoard(user?.role, board.code),
      })),
      isLoading: false,
      error,  // ← 에러 객체는 반환하여 상위에서 처리 가능
    };
  }

  return {
    boards: data.data?.map(transformBoard) ?? [],
    isLoading,
    error: undefined,
  };
}
```

### **Component Layer** - User Feedback

**목적**: Hook이 제공한 에러 정보를 바탕으로 사용자에게 적절한 피드백 표시

```typescript
// ✅ 올바른 예: BoardListPage.tsx
export default function BoardListPage() {
  const { boards, error } = useBoardList();
  const isForbidden = isBoardReadDenied(error);  // ← Hook이 반환한 error 활용

  if (isForbidden) {
    return (
      <div>
        <p>권한이 없습니다</p>
        <p>정회원 이상만 조회 가능합니다</p>
      </div>
    );
  }

  return <div>{boards.map(board => ...)}</div>;
}
```

### **Mutation onError** - Immediate Feedback

**목적**: 사용자 액션(작성, 수정, 삭제 등)의 실패를 즉시 알림

```typescript
// ✅ 올바른 예: PostDetailPage.tsx
const { mutate: deletePost } = useDeletePost({
  mutation: {
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
    },
  },
});
```

### 레이어별 책임 정리

| 레이어 | 책임 | 예시 |
|--------|------|------|
| **Hook** | Graceful Degradation (복원력) | `useBoards` - 폴백 데이터 제공 |
| **Component** | UI State (에러 상태 표시) | `BoardListPage` - 권한 없음 안내 |
| **Mutation** | Immediate Feedback (즉시 피드백) | `onError` - alert, navigate |

**핵심 원칙:**
- Hook은 에러를 throw하지 않고 `error` 객체로 반환
- Component는 Hook의 `error`를 받아서 UI 표현
- Mutation은 `onError`에서 사용자에게 즉시 피드백

---

# 4. React + Vite 규칙

## 3.1 React/프론트 공통 규칙

### 3.1.1 상태 관리: Zustand

* 상태 관리는 Zustand로 통일
* 전역 상태는 “진짜 전역이어야 하는 것만”
* 모든 것을 전역 store에 넣기 금지

### 3.1.2 폼: React Hook Form + Zod

* 폼 상태 관리: React Hook Form
* 폼 검증 스키마: Zod (@hookform/resolvers)
* 입력 타입은 `z.infer<typeof Schema>`로 도출

```tsx
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
});

type LoginForm = z.infer<typeof loginSchema>;

const { register, handleSubmit } = useForm<LoginForm>({
  resolver: zodResolver(loginSchema),
});
```

### 3.1.3 내부 API fetch: Orval 강제

* 내부 API 호출은 **무조건 Orval**
* 내부 API에 `fetch` 직접 사용 금지
* `fetch`는 외부 API에서만 허용

### 3.1.4 Tailwind + cn() 규칙

* Tailwind 사용
* 클래스 합치기/조건 분기: 무조건 `cn()`
* 템플릿 리터럴 조합 금지

```tsx
<div className={cn(
  "px-3 py-2 rounded-md",
  isActive && "font-semibold",
  disabled ? "opacity-50 pointer-events-none" : "hover:bg-muted"
)} />
```

### 3.1.5 다이얼로그 패턴

* 다이얼로그는 **props 없이** 사용: `<SongEditDialog />`
* open 여부는 **Zustand store에서 관리**
* ❌ props로 open/onClose 전달 금지

```tsx
// ✅ 올바른 사용
<SongEditDialog />

// ❌ 금지
<SongEditDialog open={isOpen} onClose={handleClose} song={song} />
```

### 3.1.6 Store & Custom Hook 분리

* store가 비대해지지 않게 **커스텀 훅으로 로직 분리**
* store는 **상태만** 보관, 복잡한 로직은 **훅에서 처리**

### 3.1.7 인증: Orval 단에서 통합 관리

* 인증 로직(토큰 갱신, 헤더 주입 등)은 **Orval custom instance에서 처리**
* 컴포넌트/훅에서 인증 관련 코드 직접 작성 금지
* ❌ 개별 API 호출마다 토큰 처리 금지

## 3.2 디자인 규칙

* 아이콘: lucide-react 사용
* 이미지: `<img>` 태그 사용 (Vite 환경)
* 정적 이미지(SVG 등): import 후 src에 사용
* 클릭 이벤트: **무조건 `<button type="button">` 사용**
* 클릭 가능 요소: **무조건 `cursor-pointer` 적용**

```tsx
// 정적 이미지 사용법
import spotifyIcon from "@/assets/icons/spotify.svg";

<img src={spotifyIcon} alt="Spotify" width={44} height={44} />

// 버튼
<button type="button" className="cursor-pointer">
  클릭
</button>
```

## 3.3 기술스택

* React 19 + Vite 7
* TypeScript
* Tanstack Query + Orval (OpenAPI 기반 hook 생성)
* Tailwind CSS + cn()
* Zustand
* React Hook Form + Zod (폼 검증용)

---

# 5. 포맷팅 & 개발 도구

## 5.1 ESLint + Prettier (린팅 & 포맷팅)

* **VSCode 익스텐션 설치 필수**: `dbaeumer.vscode-eslint`, `esbenp.prettier-vscode`
* ESLint: 코드 품질 검사 (`eslint.config.js`)
* Prettier: 코드 포맷팅 (`.prettierrc`)
* 저장 시 자동 포맷팅 권장

```bash
# 수동 실행 (필요 시)
pnpm lint     # ESLint 검사
pnpm format   # Prettier 포맷팅
```

## 5.2 Orval (API 클라이언트 생성)

* 백엔드 OpenAPI 스펙 기반으로 API 클라이언트 자동 생성
* `orval.config.ts`에 설정
* 백엔드 API 변경 시 Orval 재생성 필요

```bash
pnpm orval
```

## 5.3 환경 변수

* Vite 환경 변수는 `VITE_` 접두사 사용
* `.env.local` 파일에 로컬 설정 (gitignore됨)

```
VITE_API_URL=http://localhost:8080
```

---

# 6. 라이브러리 사용 가이드

## 6.1 Orval + React Query 사용법

Orval은 백엔드 OpenAPI 스펙을 읽어서 React Query 훅을 자동 생성해줍니다.

### 6.1.1 API 클라이언트 생성

```bash
# 백엔드 서버가 실행 중이어야 함
pnpm orval
```

실행하면 `src/api/` 폴더에 훅들이 생성됩니다.

### 6.1.2 데이터 조회 (useQuery)

```tsx
import { useGetUsers } from "@/api";

function UserList() {
  const { data, isLoading, error } = useGetUsers();

  if (isLoading) return <div>로딩 중...</div>;
  if (error) return <div>에러 발생: {error.message}</div>;

  return (
    <ul>
      {data?.map((user) => (
        <li key={user.id}>{user.name}</li>
      ))}
    </ul>
  );
}
```

### 6.1.3 데이터 변경 (useMutation)

```tsx
import { useCreateUser } from "@/api";
import { useQueryClient } from "@tanstack/react-query";

function CreateUserForm() {
  const queryClient = useQueryClient();

  const { mutate, isPending } = useCreateUser({
    mutation: {
      onSuccess: () => {
        // 성공 시 유저 목록 새로고침
        queryClient.invalidateQueries({ queryKey: ["getUsers"] });
        alert("생성 완료!");
      },
      onError: (error) => {
        alert(`에러: ${error.message}`);
      },
    },
  });

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    mutate({
      data: {
        name: formData.get("name") as string,
        email: formData.get("email") as string,
      },
    });
  };

  return (
    <form onSubmit={handleSubmit}>
      <input name="name" placeholder="이름" />
      <input name="email" placeholder="이메일" />
      <button type="submit" disabled={isPending}>
        {isPending ? "생성 중..." : "생성"}
      </button>
    </form>
  );
}
```

### 6.1.4 조건부 fetch (enabled)

```tsx
// userId가 있을 때만 fetch
const { data } = useGetUser(userId, {
  query: {
    enabled: !!userId, // userId가 있을 때만 실행
  },
});
```

### 6.1.5 자주 쓰는 상태값

| 상태 | 설명 |
|------|------|
| `isLoading` | 첫 로딩 중 (데이터 없음) |
| `isFetching` | 백그라운드 refetch 중 (데이터 있을 수 있음) |
| `isError` | 에러 발생 |
| `isSuccess` | 성공 |
| `data` | 응답 데이터 |
| `error` | 에러 객체 |

---

## 6.2 Zustand 사용법

Zustand는 간단한 전역 상태 관리 라이브러리입니다.

### 6.2.1 Store 생성

```ts
// src/stores/useAuthStore.ts
import { create } from "zustand";

interface AuthState {
  user: User | undefined;
  isLoggedIn: boolean;
  login: (user: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: undefined,
  isLoggedIn: false,
  login: (user) => set({ user, isLoggedIn: true }),
  logout: () => set({ user: undefined, isLoggedIn: false }),
}));
```

### 6.2.2 Store 사용

```tsx
import { useAuthStore } from "@/stores/useAuthStore";

function Header() {
  // 필요한 것만 가져오기 (성능 최적화)
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);

  return (
    <header>
      {user ? (
        <>
          <span>{user.name}님</span>
          <button type="button" onClick={logout}>로그아웃</button>
        </>
      ) : (
        <span>로그인 필요</span>
      )}
    </header>
  );
}
```

### 6.2.3 다이얼로그 상태 관리 (실전 예시)

```ts
// src/stores/useDialogStore.ts
import { create } from "zustand";

interface DialogState {
  // 유저 수정 다이얼로그
  editUserDialog: {
    isOpen: boolean;
    userId: string | undefined;
  };
  openEditUserDialog: (userId: string) => void;
  closeEditUserDialog: () => void;
}

export const useDialogStore = create<DialogState>((set) => ({
  editUserDialog: {
    isOpen: false,
    userId: undefined,
  },
  openEditUserDialog: (userId) =>
    set({ editUserDialog: { isOpen: true, userId } }),
  closeEditUserDialog: () =>
    set({ editUserDialog: { isOpen: false, userId: undefined } }),
}));
```

```tsx
// 다이얼로그 열기
const openEditUserDialog = useDialogStore((s) => s.openEditUserDialog);
<button onClick={() => openEditUserDialog(user.id)}>수정</button>

// 다이얼로그 컴포넌트
function EditUserDialog() {
  const { isOpen, userId } = useDialogStore((s) => s.editUserDialog);
  const close = useDialogStore((s) => s.closeEditUserDialog);

  if (!isOpen) return null;

  return (
    <dialog open>
      <h2>유저 수정: {userId}</h2>
      <button onClick={close}>닫기</button>
    </dialog>
  );
}
```

---

## 6.3 Zod + React Hook Form 사용법

Zod로 폼 검증 스키마를 정의하고, React Hook Form과 연동합니다.

### 6.3.1 기본 사용법

```tsx
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";

// 1. 스키마 정의
const signupSchema = z.object({
  email: z.string().email("올바른 이메일을 입력하세요"),
  password: z.string().min(8, "비밀번호는 8자 이상이어야 합니다"),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: "비밀번호가 일치하지 않습니다",
  path: ["confirmPassword"],
});

// 2. 타입 추출
type SignupForm = z.infer<typeof signupSchema>;

// 3. 폼 컴포넌트
function SignupForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignupForm>({
    resolver: zodResolver(signupSchema),
  });

  const onSubmit = (data: SignupForm) => {
    console.log(data);
    // API 호출
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <div>
        <input {...register("email")} placeholder="이메일" />
        {errors.email && <span>{errors.email.message}</span>}
      </div>

      <div>
        <input {...register("password")} type="password" placeholder="비밀번호" />
        {errors.password && <span>{errors.password.message}</span>}
      </div>

      <div>
        <input {...register("confirmPassword")} type="password" placeholder="비밀번호 확인" />
        {errors.confirmPassword && <span>{errors.confirmPassword.message}</span>}
      </div>

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "처리 중..." : "가입"}
      </button>
    </form>
  );
}
```

### 6.3.2 자주 쓰는 Zod 검증

```ts
import { z } from "zod";

// 필수 문자열
z.string().min(1, "필수 입력입니다")

// 이메일
z.string().email("올바른 이메일을 입력하세요")

// 숫자
z.number().min(0, "0 이상이어야 합니다")

// 선택적 필드
z.string().optional()

// 배열
z.array(z.string()).min(1, "최소 1개 선택")

// enum
z.enum(["admin", "user", "guest"])

// 커스텀 검증
z.string().refine((val) => val.startsWith("@"), {
  message: "@로 시작해야 합니다",
})
```

### 6.3.3 폼 기본값 설정

```tsx
const { register } = useForm<EditUserForm>({
  resolver: zodResolver(editUserSchema),
  defaultValues: {
    name: user.name,
    email: user.email,
  },
});
```

---

# 7. 테스트 가이드

## 7.1 테스트 원칙

* **모든 사용자 테스트 시나리오는 반드시 문서화**
* **테스트 문서는 항상 최신 상태 유지**
* **기능 추가/수정 시 테스트 시나리오도 함께 업데이트**

## 7.2 테스트 문서 위치

### 중앙 테스트 폴더
**[docs/testing/](docs/testing/)** - 모든 E2E 테스트 가이드

테스트 폴더 전체 개요는 **[docs/README.md](docs/README.md#testing---e2e-테스트-가이드)** 참고

각 파일은 해당 기능의 **Playwright 자동화 테스트**와 **수동 브라우저 테스트** 시나리오를 포함합니다:

- **[auth-test-guide.md](docs/testing/auth-test-guide.md)** - 인증 (회원가입, 이메일 인증, 로그인)
- **[inquiries-test-guide.md](docs/testing/inquiries-test-guide.md)** - 문의 (문의 목록 조회, 문의 작성)
- **[posts-test-guide.md](docs/testing/posts-test-guide.md)** - 게시판 (게시글 CRUD, 좋아요)
- **events-test-guide.md** (예정) - 이벤트
- **admin-test-guide.md** (예정) - 관리자

### 기능별 마이그레이션 문서
- [docs/migration/auth-inquiries-orval-migration.md](docs/migration/auth-inquiries-orval-migration.md) - Auth & Inquiries API 마이그레이션
- [docs/migration/orval-api-migration.md](docs/migration/orval-api-migration.md) - Posts API 마이그레이션

## 7.3 테스트 시나리오 작성 규칙

새로운 기능을 추가하거나 기존 기능을 수정할 때는 반드시 다음 작업을 수행:

1. **해당 기능의 테스트 가이드 파일에 테스트 시나리오 추가**
   - Auth 기능: [auth-test-guide.md](docs/testing/auth-test-guide.md)
   - Inquiries 기능: [inquiries-test-guide.md](docs/testing/inquiries-test-guide.md)
   - Posts 기능: [posts-test-guide.md](docs/testing/posts-test-guide.md)
   - 새로운 기능: 새 파일 생성 (예: events-test-guide.md)
   - 수동 브라우저 테스트 섹션에 시나리오 추가
   - Playwright 자동화 테스트 섹션에 예제 코드 추가

2. **테스트 시나리오 구성 요소**:
   - 시나리오 설명
   - 테스트 단계 (1, 2, 3...)
   - 예상 결과
   - Network 탭 확인 사항
   - Console 탭 확인 사항

3. **예시**:
   ```markdown
   #### X.X 새로운 기능 테스트
   **시나리오**: 기능 설명

   1. 사용자 액션 1
   2. 사용자 액션 2
   3. **예상 결과**:
      - UI 변화
      - 상태 변화
   4. **Network 탭 확인**:
      - URL: `POST .../api/v1/...`
      - Status: 200 OK
   5. **Console 탭 확인**:
      - 에러 없음
   ```

## 7.4 Playwright 설치 및 실행

### 7.4.1 설치

```bash
pnpm add -D @playwright/test
npx playwright install
```

### 7.4.2 테스트 실행

```bash
# 모든 테스트 실행
npx playwright test

# 특정 테스트 파일
npx playwright test e2e/auth/signup.spec.ts

# UI 모드 (디버깅)
npx playwright test --ui

# 헤드풀 모드 (브라우저 표시)
npx playwright test --headed
```

## 7.5 테스트 커버리지

현재 테스트 문서에 포함된 기능:

- ✅ **Auth**: 회원가입, 이메일 인증, 로그인, 에러 처리
- ✅ **Inquiries**: 문의 목록 조회, 문의 작성
- ✅ **Posts**: 게시글 목록/상세 조회, 좋아요, 작성
- ⏸️ **Events**: 문서화 예정
- ⏸️ **Admin**: 문서화 예정

## 7.6 테스트 업데이트 체크리스트

기능 수정 시 다음 항목을 확인:

- [ ] 해당 기능의 테스트 가이드 파일에 수동 테스트 시나리오 추가/수정
  - Auth: [auth-test-guide.md](docs/testing/auth-test-guide.md)
  - Inquiries: [inquiries-test-guide.md](docs/testing/inquiries-test-guide.md)
  - Posts: [posts-test-guide.md](docs/testing/posts-test-guide.md)
- [ ] Playwright 자동화 테스트 코드 예시 추가/수정
- [ ] 관련 마이그레이션 문서 업데이트 (있는 경우)
- [ ] 변경된 API endpoint 문서화
- [ ] 에러 케이스 추가 (있는 경우)
