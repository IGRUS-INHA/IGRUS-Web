# TypeScript 마이그레이션 가이드

## 개요

프론트엔드 코드베이스를 JavaScript에서 TypeScript로 마이그레이션하기 위한 가이드 문서입니다.

## 현재 상태

- **총 파일 수**: 97개 (.js/.jsx)
- **총 코드 줄 수**: 약 7,800줄
- **기술 스택**: React 19, Vite 7, Zustand, React Query

### 마이그레이션 완료된 파일

| 파일 | 설명 |
|------|------|
| `tsconfig.json` | TypeScript 설정 (strict 모드) |
| `tsconfig.node.json` | Node 환경 설정 |
| `vite.config.ts` | Vite 설정 |
| `eslint.config.js` | ESLint TypeScript 규칙 추가 |
| `src/types/common.ts` | 공통 타입 (Role, BoardType, Status 등) |
| `src/types/entities.ts` | 엔티티 타입 (User, Post, Event 등) |
| `src/types/api.ts` | API 요청/응답 타입 |
| `src/types/store.ts` | Zustand 스토어 타입 |
| `src/types/index.ts` | 타입 재export |
| `src/stores/authStore.ts` | 인증 스토어 |
| `src/stores/uiStore.ts` | UI 스토어 |
| `src/stores/index.ts` | 스토어 재export |
| `src/api/client.ts` | API 클라이언트 |
| `src/hooks/useAuth.ts` | 인증 훅 |
| `src/hooks/usePermission.ts` | 권한 훅 |
| `src/hooks/queries/usePosts.ts` | 게시글 쿼리 훅 |
| `src/hooks/queries/useEvents.ts` | 행사 쿼리 훅 |

---

## 1. 타입 Import 방법

### 타입 전용 import

```typescript
// 권장: type 키워드 사용
import type { User, Post, Event } from '@/types';
import type { BoardType, Role } from '@/types/common';
import type { ApiResponse, LoginRequest } from '@/types/api';
import type { AuthStore, UIStore } from '@/types/store';
```

### 값과 타입 혼합 import

```typescript
// 값(상수)과 타입을 함께 import
import { ROLES, type Role } from '@/types/common';
import { BOARDS, type BoardType } from '@/types/common';
```

---

## 2. 마이그레이션 순서

### Phase 1: 인프라 (완료)
1. `src/types/` - 타입 정의 ✓
2. `src/api/client.ts` - API 클라이언트 ✓
3. `src/stores/authStore.ts` - 인증 스토어 ✓
4. `src/stores/uiStore.ts` - UI 스토어 ✓

### Phase 2: API 레이어
1. `src/api/auth.js` → `auth.ts`
2. `src/api/posts.js` → `posts.ts`
3. `src/api/events.js` → `events.ts`
4. `src/api/users.js` → `users.ts`
5. `src/api/admin.js` → `admin.ts`
6. `src/api/inquiries.js` → `inquiries.ts`

### Phase 3: 훅 (일부 완료)
1. `src/hooks/useAuth.ts` ✓
2. `src/hooks/usePermission.ts` ✓
3. `src/hooks/useToast.js` → `useToast.ts`
4. `src/hooks/usePagination.js` → `usePagination.ts`
5. `src/hooks/queries/usePosts.ts` ✓
6. `src/hooks/queries/useEvents.ts` ✓
7. `src/hooks/queries/useInquiries.js` → `useInquiries.ts`

### Phase 4: 상수 & 유틸리티
1. `src/constants/permissions.js` → `permissions.ts`
2. `src/constants/board.js` → `board.ts`
3. `src/constants/event.js` → `event.ts`
4. `src/utils/*.js` → `*.ts`

### Phase 5: 컴포넌트
1. `src/components/ui/*.jsx` → `*.tsx`
2. `src/components/common/*.jsx` → `*.tsx`
3. `src/components/feature/**/*.jsx` → `*.tsx`
4. `src/components/board/*.jsx` → `*.tsx`

### Phase 6: 페이지
1. 인증 페이지 (`src/pages/auth/`)
2. 게시판 페이지 (`src/pages/board/`)
3. 행사 페이지 (`src/pages/event/`)
4. 관리자 페이지 (`src/pages/admin/`)
5. 기타 페이지

---

## 3. 코드 컨벤션

### any 사용 금지

```typescript
// 금지
const data: any = response.data;

// 권장: 구체적인 타입 또는 unknown 사용
const data: User = response.data;
const data: unknown = response.data;
```

### 명시적 반환 타입

```typescript
// 금지
function getUser() {
  return { name: 'test' };
}

// 권장: 반환 타입 명시
function getUser(): User {
  return { name: 'test', studentId: '12345678', ... };
}
```

### null 체크

```typescript
// 권장: 옵셔널 체이닝과 nullish coalescing 사용
const role = user?.role ?? null;
```

---

## 4. 패턴 예시

### 4.1 Zustand 스토어 패턴

```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthStore, AuthPersistState } from '@/types/store';
import type { User } from '@/types/entities';

export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      // 상태
      user: null,
      accessToken: null,
      isAuthenticated: false,

      // 액션
      setAuth: (
        user: User,
        accessToken: string,
        refreshToken: string
      ): void => {
        set({ user, accessToken, refreshToken, isAuthenticated: true });
      },

      logout: (): void => {
        set({ user: null, accessToken: null, isAuthenticated: false });
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state): AuthPersistState => ({
        user: state.user,
        accessToken: state.accessToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
```

### 4.2 TanStack Query 패턴

```typescript
import {
  useQuery,
  useMutation,
  useQueryClient,
  type UseQueryResult,
  type UseMutationResult,
} from '@tanstack/react-query';
import type { Post, PostDetail } from '@/types/entities';
import type { BoardType } from '@/types/common';

// 쿼리 키 팩토리 (as const 사용)
export const postKeys = {
  all: ['posts'] as const,
  lists: () => [...postKeys.all, 'list'] as const,
  list: (board: BoardType, filters: object) =>
    [...postKeys.lists(), board, filters] as const,
  detail: (board: BoardType, id: string) =>
    [...postKeys.all, 'detail', board, id] as const,
};

// 쿼리 훅 - 배열 반환 타입 명시
export function usePosts(
  board: BoardType,
  params: object = {}
): UseQueryResult<Post[]> {
  return useQuery({
    queryKey: postKeys.list(board, params),
    queryFn: async (): Promise<Post[]> => {
      const response = await postsApi.getList(board, params);
      return response.data;
    },
    enabled: !!board,
  });
}

// 뮤테이션 훅 (낙관적 업데이트)
interface ToggleLikeContext {
  previousPost: PostDetail | undefined;
  queryKey: readonly unknown[];
}

export function useToggleLike(): UseMutationResult<
  void,
  Error,
  { board: BoardType; postId: string; isLiked: boolean },
  ToggleLikeContext
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ board, postId, isLiked }) => {
      if (isLiked) {
        await postsApi.unlike(board, postId);
      } else {
        await postsApi.like(board, postId);
      }
    },
    onMutate: async ({ board, postId, isLiked }) => {
      const queryKey = postKeys.detail(board, postId);
      await queryClient.cancelQueries({ queryKey });
      const previousPost = queryClient.getQueryData<PostDetail>(queryKey);

      if (previousPost) {
        queryClient.setQueryData<PostDetail>(queryKey, {
          ...previousPost,
          isLiked: !isLiked,
          likes: isLiked ? previousPost.likes - 1 : previousPost.likes + 1,
        });
      }

      return { previousPost, queryKey };
    },
    onError: (_err, _variables, context) => {
      if (context?.previousPost) {
        queryClient.setQueryData(context.queryKey, context.previousPost);
      }
    },
  });
}
```

### 4.3 컴포넌트 Props 패턴

```typescript
import type { ReactNode, ComponentProps } from 'react';
import type { Post } from '@/types/entities';
import type { BoardType } from '@/types/common';

// 기본 Props 인터페이스
interface PostCardProps {
  post: Post;
  board: BoardType;
  onLike?: (postId: string) => void;
}

export function PostCard({ post, board, onLike }: PostCardProps): ReactNode {
  return (
    <div>
      <h2>{post.title}</h2>
      <button onClick={() => onLike?.(post.id)}>좋아요</button>
    </div>
  );
}

// HTML 속성 확장
interface ButtonProps extends ComponentProps<'button'> {
  variant?: 'primary' | 'secondary';
  size?: 'sm' | 'md' | 'lg';
}

export function Button({
  variant = 'primary',
  size = 'md',
  children,
  ...props
}: ButtonProps): ReactNode {
  return (
    <button className={`btn-${variant} btn-${size}`} {...props}>
      {children}
    </button>
  );
}
```

### 4.4 커스텀 훅 패턴

```typescript
import { useState, useCallback } from 'react';

interface UsePaginationReturn {
  page: number;
  limit: number;
  setPage: (page: number) => void;
  setLimit: (limit: number) => void;
  hasNextPage: boolean;
  hasPrevPage: boolean;
}

export function usePagination(
  initialPage = 1,
  initialLimit = 20
): UsePaginationReturn {
  const [page, setPage] = useState(initialPage);
  const [limit, setLimit] = useState(initialLimit);
  const [totalPages, setTotalPages] = useState(0);

  const hasNextPage = page < totalPages;
  const hasPrevPage = page > 1;

  return {
    page,
    limit,
    setPage,
    setLimit,
    hasNextPage,
    hasPrevPage,
  };
}
```

---

## 5. 검증 체크리스트

마이그레이션 후 아래 항목을 확인하세요:

- [ ] TypeScript 컴파일 통과: `npx tsc --noEmit`
- [ ] ESLint 통과: `npm run lint`
- [ ] 빌드 성공: `npm run build`
- [ ] 개발 서버 정상 동작: `npm run dev`
- [ ] `any` 타입 없음
- [ ] 모든 함수에 명시적 반환 타입 있음
- [ ] strict null 체크 통과
- [ ] 기존 로직 변경 없음 (타입만 추가)

---

## 6. 주요 타입 파일 위치

| 파일 | 설명 |
|------|------|
| `src/types/common.ts` | Role, BoardType, Status, Theme, Toast 등 |
| `src/types/entities.ts` | User, Post, Event, Comment, Inquiry |
| `src/types/api.ts` | ApiResponse, Request/Response 타입 |
| `src/types/store.ts` | AuthStore, UIStore 타입 |
| `src/types/index.ts` | 모든 타입 재export |

---

## 7. 자주 발생하는 이슈

### 7.1 Import 경로 문제

```typescript
// 상대 경로 사용 금지
import { User } from '../../../types/entities';

// 절대 경로 (path alias) 사용
import type { User } from '@/types';
```

### 7.2 제네릭 배열 표기

```typescript
// 둘 다 가능하지만 일관성 유지
type UserList = User[];        // 권장
type UserList = Array<User>;   // 가능
```

### 7.3 이벤트 핸들러 타입

```typescript
import type { ChangeEvent, FormEvent } from 'react';

const handleChange = (e: ChangeEvent<HTMLInputElement>): void => {
  setValue(e.target.value);
};

const handleSubmit = (e: FormEvent<HTMLFormElement>): void => {
  e.preventDefault();
  // ...
};
```

---

## 8. Sonnet 작업 프롬프트

### API 레이어 변환

```markdown
# TypeScript 마이그레이션 - API 레이어

## 선행 조건
src/types/ 디렉토리에 타입 정의가 완료되어 있음

## 요청 작업
다음 파일들을 TypeScript로 변환:
- src/api/auth.js → auth.ts
- src/api/posts.js → posts.ts
- src/api/events.js → events.ts
- src/api/users.js → users.ts
- src/api/admin.js → admin.ts
- src/api/inquiries.js → inquiries.ts

## 변환 규칙
- import type { ... } from '@/types' 사용
- 함수 파라미터와 반환 타입 명시
- 기존 export 구조 유지
```

### 컴포넌트 변환

```markdown
# TypeScript 마이그레이션 - 컴포넌트

## 선행 조건
src/types/ 타입 정의 완료

## 요청 작업
src/components/ui/*.jsx → *.tsx 변환

## Props 인터페이스 규칙
- 파일 상단에 Props 인터페이스 정의
- React.FC 사용하지 않음 (직접 타이핑)
- children은 React.ReactNode
- 이벤트 핸들러는 React.MouseEvent 등 사용
```

---

작성일: 2026-01-31
