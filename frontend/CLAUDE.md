# Frontend CLAUDE.md

## 프로젝트 개요

IGRUS Web 프론트엔드 - React + Vite 기반 SPA

---

# 1. 프로젝트 운영 원칙

## 1.1 Claude 작업 지침

* 이 프로젝트는 2년 이상 유지보수 대상이므로 대충 코드 작성 금지
* 귀찮아서 만든 잘못된 코드 1개가 나중에 여러 배로 비용 증가

## 1.2 공통 금지 사항 / 실행 규칙

* **절대로 개발 서버 실행 금지** (`pnpm dev` 등 금지)
* **패키지 매니저는 pnpm 고정**
* **코드 포맷팅/타입 체크 명령 실행 금지** (`pnpm biome`, `pnpm tsc --noEmit` 등 금지 - VSCode에서 자동 처리)

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

# 3. React + Vite 규칙

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

# 4. 포맷팅 & 개발 도구

## 4.1 Biome (포맷팅 & 린팅)

* **VSCode 익스텐션 설치 필수**: `biomejs.biome`
* 프로젝트 루트에 `biome.json` 설정 파일 사용
* 저장 시 자동 포맷팅 권장

## 4.2 Orval (API 클라이언트 생성)

* 백엔드 OpenAPI 스펙 기반으로 API 클라이언트 자동 생성
* `orval.config.ts`에 설정
* 백엔드 API 변경 시 Orval 재생성 필요

```bash
pnpm orval
```

## 4.3 환경 변수

* Vite 환경 변수는 `VITE_` 접두사 사용
* `.env.local` 파일에 로컬 설정 (gitignore됨)

```
VITE_API_URL=http://localhost:8080
```

---

# 5. 라이브러리 사용 가이드

## 5.1 Orval + React Query 사용법

Orval은 백엔드 OpenAPI 스펙을 읽어서 React Query 훅을 자동 생성해줍니다.

### 5.1.1 API 클라이언트 생성

```bash
# 백엔드 서버가 실행 중이어야 함
pnpm orval
```

실행하면 `src/api/` 폴더에 훅들이 생성됩니다.

### 5.1.2 데이터 조회 (useQuery)

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

### 5.1.3 데이터 변경 (useMutation)

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

### 5.1.4 조건부 fetch (enabled)

```tsx
// userId가 있을 때만 fetch
const { data } = useGetUser(userId, {
  query: {
    enabled: !!userId, // userId가 있을 때만 실행
  },
});
```

### 5.1.5 자주 쓰는 상태값

| 상태 | 설명 |
|------|------|
| `isLoading` | 첫 로딩 중 (데이터 없음) |
| `isFetching` | 백그라운드 refetch 중 (데이터 있을 수 있음) |
| `isError` | 에러 발생 |
| `isSuccess` | 성공 |
| `data` | 응답 데이터 |
| `error` | 에러 객체 |

---

## 5.2 Zustand 사용법

Zustand는 간단한 전역 상태 관리 라이브러리입니다.

### 5.2.1 Store 생성

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

### 5.2.2 Store 사용

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

### 5.2.3 다이얼로그 상태 관리 (실전 예시)

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

## 5.3 Zod + React Hook Form 사용법

Zod로 폼 검증 스키마를 정의하고, React Hook Form과 연동합니다.

### 5.3.1 기본 사용법

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

### 5.3.2 자주 쓰는 Zod 검증

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

### 5.3.3 폼 기본값 설정

```tsx
const { register } = useForm<EditUserForm>({
  resolver: zodResolver(editUserSchema),
  defaultValues: {
    name: user.name,
    email: user.email,
  },
});
```
