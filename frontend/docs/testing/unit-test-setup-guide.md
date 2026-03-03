# 단위 테스트 도입 가이드 (Vitest + React Testing Library)

## 개요

프론트엔드 단위 테스트 프레임워크 도입을 위한 설정 가이드.
Vite 7 기반 프로젝트이므로 네이티브 통합되는 Vitest를 사용하고, 컴포넌트 테스트를 위해 React Testing Library를 함께 설정한다.

**현재 상태**: 미도입 (이 문서는 도입 시 참고용)

---

## 1. 의존성 설치

```bash
pnpm add -D vitest @testing-library/react @testing-library/jest-dom @testing-library/user-event jsdom
```

| 패키지                        | 용도                                                 |
| ----------------------------- | ---------------------------------------------------- |
| `vitest`                      | 테스트 러너 (Vite 네이티브 통합, Jest 호환 API)      |
| `@testing-library/react`      | 컴포넌트 렌더링 + DOM 쿼리 (`render`, `screen`)      |
| `@testing-library/jest-dom`   | 커스텀 매처 (`toBeInTheDocument`, `toBeDisabled` 등) |
| `@testing-library/user-event` | 사용자 인터랙션 시뮬레이션 (click, type 등)          |
| `jsdom`                       | 브라우저 DOM 환경 시뮬레이션                         |

### 초기 제외 패키지

| 패키지                | 제외 사유                                                          |
| --------------------- | ------------------------------------------------------------------ |
| `msw`                 | API 모킹 — 순수 함수/컴포넌트 테스트에 불필요, 통합 테스트 시 추가 |
| `@vitest/coverage-v8` | 커버리지 — 테스트 충분히 쌓인 후 추가                              |
| `@vitest/ui`          | 브라우저 UI — 선택사항, 필요 시 추가                               |

---

## 2. 설정 파일

### 2.1 `frontend/vitest.config.ts` (신규 생성)

`vite.config.ts`와 분리하는 이유:

- `vite.config.ts`가 `loadEnv` 함수 형태로 복잡함
- Tailwind 플러그인, 프록시 설정 등 테스트에 불필요한 설정 포함
- 관심사 분리

```ts
import path from "path";
import { fileURLToPath } from "url";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react-swc";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  define: {
    // Feature flags — 테스트에서는 기본 true (프로덕션 기본값과 동일)
    __FEATURE_COMMUNITY__: true,
    __FEATURE_SEARCH__: true,
    __FEATURE_PROFILE_EDIT__: true,
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    globals: true, // describe, it, expect 전역 사용
    include: ["src/**/*.test.{ts,tsx}"],
    css: false, // CSS 처리 생략 (테스트 속도 향상)
    restoreMocks: true, // 테스트 간 mock 자동 복원
  },
});
```

### 2.2 `frontend/src/test/setup.ts` (신규 생성)

```ts
import "@testing-library/jest-dom/vitest";
```

jest-dom 매처를 Vitest의 `expect`에 등록. 이후 필요 시 확장:

- `window.matchMedia` mock
- `IntersectionObserver` mock
- MSW 서버 setup/teardown

### 2.3 `frontend/src/test/test-utils.tsx` (신규 생성)

테스트에서 providers를 매번 감싸지 않도록 커스텀 render 함수 제공.

```tsx
import { type ReactElement } from "react";
import { render, type RenderOptions } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";

function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
}

interface CustomRenderOptions extends Omit<RenderOptions, "wrapper"> {
  initialRoute?: string;
}

function renderWithProviders(
  ui: ReactElement,
  options: CustomRenderOptions = {},
): ReturnType<typeof render> {
  const { initialRoute = "/", ...renderOptions } = options;
  const testQueryClient = createTestQueryClient();

  function Wrapper({ children }: { children: React.ReactNode }): ReactElement {
    return (
      <QueryClientProvider client={testQueryClient}>
        <MemoryRouter initialEntries={[initialRoute]}>{children}</MemoryRouter>
      </QueryClientProvider>
    );
  }

  return render(ui, { wrapper: Wrapper, ...renderOptions });
}

export * from "@testing-library/react";
export { default as userEvent } from "@testing-library/user-event";
export { renderWithProviders as render };
```

**사용법:**

```tsx
import { render, screen, userEvent } from "@/test/test-utils";
```

---

## 3. 기존 설정 파일 수정

### 3.1 `frontend/tsconfig.json`

`compilerOptions`에 추가:

```jsonc
"types": ["vitest/globals"]
```

-> `describe`, `it`, `expect` 등을 import 없이 전역 사용 가능하게 함.

### 3.2 `frontend/tsconfig.node.json`

`include`에 추가:

```jsonc
"include": ["vite.config.ts", "eslint.config.ts", "orval.config.ts", "vitest.config.ts"]
```

### 3.3 `frontend/eslint.config.js`

`prettier` config 앞에 테스트 파일 전용 override 추가:

```js
// 테스트 파일
{
  files: ['**/*.test.{ts,tsx}', '**/test/**/*.{ts,tsx}'],
  rules: {
    '@typescript-eslint/explicit-function-return-type': 'off',
    '@typescript-eslint/no-non-null-assertion': 'off',
  },
},
```

### 3.4 `frontend/package.json` scripts

```json
"test": "vitest",
"test:run": "vitest run"
```

| 스크립트        | 용도                     |
| --------------- | ------------------------ |
| `pnpm test`     | watch 모드 (로컬 개발용) |
| `pnpm test:run` | 단일 실행 (CI용)         |

---

## 4. 테스트 파일 구조

**코로케이션 방식** — 소스 파일 옆에 `.test.ts(x)` 배치:

```
src/
  test/                          # 테스트 인프라 (setup, utils)
    setup.ts
    test-utils.tsx
  lib/
    utils.ts
    utils.test.ts                # <- 코로케이션
  utils/
    error.ts
    error.test.ts                # <- 코로케이션
  components/
    ui/
      spinner.tsx
      spinner.test.tsx           # <- 코로케이션
    board/
      Pagination.tsx
      Pagination.test.tsx        # <- 코로케이션
```

---

## 5. 테스트 작성 예시

### 5.1 순수 함수 테스트 — `src/lib/utils.test.ts`

```ts
import { cn } from "@/lib/utils";

describe("cn", () => {
  it("클래스명을 병합한다", () => {
    expect(cn("px-2", "py-1")).toBe("px-2 py-1");
  });

  it("조건부 클래스를 처리한다", () => {
    expect(cn("base", true && "active", false && "hidden")).toBe("base active");
  });

  it("Tailwind 충돌을 해결한다 (마지막 우선)", () => {
    expect(cn("px-2", "px-4")).toBe("px-4");
  });

  it("undefined/null을 무시한다", () => {
    expect(cn("base", undefined, null, "extra")).toBe("base extra");
  });
});
```

### 5.2 에러 헬퍼 테스트 — `src/utils/error.test.ts`

```ts
import { ApiError } from "@/types/error";
import {
  isApiError,
  hasErrorCode,
  getErrorMessage,
  isUnauthorizedError,
} from "@/utils/error";

describe("isApiError", () => {
  it("ApiError 인스턴스를 감지한다", () => {
    const error = new ApiError(400, "TEST", "test");
    expect(isApiError(error)).toBe(true);
  });

  it("일반 Error는 false", () => {
    expect(isApiError(new Error("test"))).toBe(false);
  });
});

describe("getErrorMessage", () => {
  it("매핑된 한국어 메시지를 반환한다", () => {
    const error = new ApiError(403, "BOARD_READ_DENIED", "server msg");
    expect(getErrorMessage(error)).toBe("게시판 읽기 권한이 없습니다.");
  });

  it("매핑 없으면 서버 메시지를 반환한다", () => {
    const error = new ApiError(400, "UNKNOWN", "fieldName: some error");
    expect(getErrorMessage(error)).toBe("some error");
  });
});
```

### 5.3 컴포넌트 테스트 — `src/components/ui/spinner.test.tsx`

```tsx
import { render } from "@/test/test-utils";
import { Spinner } from "@/components/ui/spinner";

describe("Spinner", () => {
  it("기본 medium 사이즈로 렌더링된다", () => {
    const { container } = render(<Spinner />);
    expect(container.firstElementChild).toHaveClass("h-6", "w-6");
  });

  it("small 사이즈를 적용한다", () => {
    const { container } = render(<Spinner size="sm" />);
    expect(container.firstElementChild).toHaveClass("h-4", "w-4");
  });
});
```

### 5.4 인터랙티브 컴포넌트 테스트 — `src/components/board/Pagination.test.tsx`

```tsx
import { render, screen, userEvent } from "@/test/test-utils";
import { Pagination } from "@/components/board/Pagination";

describe("Pagination", () => {
  it("totalPages가 1 이하면 렌더링하지 않는다", () => {
    const { container } = render(
      <Pagination currentPage={1} totalPages={1} onPageChange={vi.fn()} />,
    );
    expect(container.firstChild).toBeNull();
  });

  it("페이지 버튼 클릭 시 onPageChange를 호출한다", async () => {
    const user = userEvent.setup();
    const onPageChange = vi.fn();

    render(
      <Pagination currentPage={1} totalPages={5} onPageChange={onPageChange} />,
    );

    await user.click(screen.getByRole("button", { name: "3" }));
    expect(onPageChange).toHaveBeenCalledWith(3);
  });
});
```

---

## 6. Zustand 스토어 테스트 패턴

Zustand의 `setState`를 직접 호출하여 상태를 제어:

```ts
import { useAuthStore } from "@/stores/authStore";

beforeEach(() => {
  useAuthStore.setState({
    user: undefined,
    accessToken: undefined,
    refreshToken: undefined,
    isAuthenticated: false,
    isHydrated: true, // hydration 대기 생략
  });
  localStorage.removeItem("auth-storage");
  localStorage.removeItem("accessToken");
});
```

---

## 7. 주의사항

### Feature Flags

테스트에서 feature flag off 상태를 테스트하려면:

```ts
vi.stubGlobal("__FEATURE_COMMUNITY__", false);
// 테스트 후 자동 복원 (restoreMocks: true)
```

### exactOptionalPropertyTypes

tsconfig에서 `exactOptionalPropertyTypes: true`가 설정되어 있으므로, mock 데이터에서 optional 필드는 `undefined`를 명시적으로 넣거나 헬퍼 함수를 사용할 것.

### CSS 클래스 단언

`cn()`이 `twMerge`를 사용하므로 클래스 순서가 달라질 수 있음. 전체 문자열 비교 대신 `toHaveClass('px-4')` 사용.

---

## 8. 검증

설정 완료 후 테스트 실행:

```bash
pnpm test:run
```

모든 테스트 통과 확인.

---

## 9. 향후 확장

| 단계      | 내용                                               |
| --------- | -------------------------------------------------- |
| MSW 도입  | API 호출이 포함된 컴포넌트/훅 테스트 시            |
| 커버리지  | `@vitest/coverage-v8` 추가, CI에서 커버리지 리포트 |
| CI 통합   | GitHub Actions에 `pnpm test:run` 추가              |
| 훅 테스트 | `renderHook` 활용 (`useAuth`, `usePagination` 등)  |
