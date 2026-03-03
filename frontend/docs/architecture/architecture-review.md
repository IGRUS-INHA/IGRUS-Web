# 프론트엔드 아키텍처 리뷰

> **작성일**: 2025-02-10
> **대상**: `frontend/src/` 전체 코드베이스
> **목적**: 현재 아키텍처의 강점과 약점을 객관적으로 파악하고, 개선 우선순위를 정리

---

## 부문별 평가

### 1. 자기 규칙 준수 — 45/100

**CLAUDE.md에 정의한 규칙을 실제 코드가 따르지 않는 곳이 다수 존재.**
문서가 많을수록 괴리가 눈에 띄므로, 규칙 위반은 규칙이 없는 것보다 더 나쁜 인상을 준다.

| 규칙 (CLAUDE.md)                                  | 위반 위치                               | 내용                                                |
| ------------------------------------------------- | --------------------------------------- | --------------------------------------------------- |
| 폼은 React Hook Form + Zod                        | `components/feature/auth/LoginForm.tsx` | `useState`로 폼 수동 관리                           |
| 클래스 합치기는 무조건 `cn()`, 템플릿 리터럴 금지 | `components/common/Header.tsx:76`       | 백틱 템플릿 리터럴로 className 조합                 |
| `as` 최소화, IO 경계에서 1회만                    | `pages/board/PostDetailPage.tsx`        | `as string` 3회, `as PostDetailResponse`, `as Node` |
| 내부에 `null` 유입 금지                           | `types/entities.ts:136`                 | `myRegistration?: EventRegistration \| null`        |
| 아이콘은 lucide-react                             | `pages/HomePage.tsx:294`                | 이모지 `👍`, `💬` 직접 사용                         |
| `as` 금지                                         | `pages/auth/SignupPage.tsx:145`         | `undefined as unknown as true`                      |

**개선 방향:**

- LoginForm에 React Hook Form + Zod 적용 (migration/react-hook-form-migration.md 참고)
- Header.tsx의 템플릿 리터럴을 `cn()`으로 교체
- PostDetailPage의 `as` 단언을 타입 가드 또는 제네릭으로 대체
- 규칙 위반을 정기적으로 감사하는 프로세스 도입

---

### 2. 컴포넌트 설계 — 55/100

**갓 컴포넌트 문제가 심각. 비즈니스 로직과 UI가 분리되지 않음.**

| 파일                             | 줄 수 | 문제                                                   |
| -------------------------------- | ----- | ------------------------------------------------------ |
| `pages/auth/SignupPage.tsx`      | 680+  | 4단계 스텝 폼 전체가 한 파일                           |
| `pages/board/PostDetailPage.tsx` | 408   | 좋아요/북마크/삭제/신고/댓글 스크롤 핸들러 전부 인라인 |
| `pages/mypage/MyPage.tsx`        | 352   | 4개 탭 콘텐츠가 모두 한 파일에 인라인                  |
| `pages/HomePage.tsx`             | 305   | 하드코딩 목 데이터 + 히어로 + 카드 리스트              |

**CLAUDE.md에 "store가 비대해지지 않게 커스텀 훅으로 로직 분리"라고 했지만, 가장 복잡한 페이지들에서 훅 추출이 전혀 없음.**

**개선 방향:**

```
# PostDetailPage 분해 예시
hooks/queries/usePostDetail.ts    → API 호출 + 데이터 변환
hooks/usePostActions.ts           → 좋아요/북마크/삭제/신고 핸들러
pages/board/PostDetailPage.tsx    → UI 렌더링만 담당
```

```
# MyPage 분해 예시
pages/mypage/tabs/PostsTab.tsx
pages/mypage/tabs/LikesTab.tsx
pages/mypage/tabs/ScrapsTab.tsx
pages/mypage/tabs/EventsTab.tsx
pages/mypage/MyPage.tsx           → 탭 전환 + 프로필 헤더만
```

---

### 3. 타입 안전성 — 65/100

**tsconfig 설정은 최상급이나, 실제 코드에서 `as` 남발로 무력화됨.**

tsconfig.json 강점:

- `strict: true` + 모든 strict 옵션 개별 활성화
- `noUncheckedIndexedAccess: true` (배열 인덱싱 시 `undefined` 강제)
- `exactOptionalPropertyTypes: true` (optional 프로퍼티 엄격 처리)

실제 코드 문제:

```tsx
// PostDetailPage.tsx — as 남발
const post = response?.data as PostDetailResponse | undefined;
boardType as string  // 3회
event.target as Node

// SignupPage.tsx — 이중 단언
privacyConsent: undefined as unknown as true,
termsConsent: undefined as unknown as true,
```

**개선 방향:**

- Orval 생성 타입을 활용하여 `as` 없이 타입 추론되도록 커스텀 훅에서 처리
- `useParams`의 반환값을 검증하는 유틸 함수 도입 (`as string` 대신)
- Zod 스키마의 `defaultValues`는 타입 호환되는 값 사용 (`undefined as unknown as true` 제거)

---

### 4. 일관성 — 45/100

**프로젝트 전체에서 가장 약한 부분.**

#### 알림 시스템이 3가지 혼재

| 방식                    | 사용 위치                                    | 비고                   |
| ----------------------- | -------------------------------------------- | ---------------------- |
| `alert()`               | PostDetailPage, SignupPage, PostWritePage 등 | 네이티브 브라우저      |
| `Swal.fire()`           | MyPage 로그아웃                              | SweetAlert2 라이브러리 |
| `toast.success/error()` | useComments 훅                               | 자체 구현 토스트       |

Toast 시스템을 직접 만들어놓고 `alert()`를 쓰고 있다.

#### isDark 삼항 지옥

디자인 시스템에서 시맨틱 토큰(`bg-background`, `text-foreground`, `bg-card`)을 정의했으면, 컴포넌트에서 `isDark`를 체크할 이유가 없다. 시맨틱 토큰이 다크모드를 처리해야 하는데, 실제로는 토큰을 무시하고 삼항으로 직접 분기:

```tsx
// PostDetailPage.tsx:236 — 양쪽이 완전히 동일한 코드
isDark
  ? "text-muted-foreground hover:text-foreground"
  : "text-muted-foreground hover:text-foreground";

// 이런 패턴이 거의 모든 페이지에 존재
isDark ? "bg-white/5 border-border" : "bg-muted border-border";
isDark ? "bg-card border-border" : "bg-card border-border shadow-sm";
```

#### 쿼리 키 관리 방식 혼재

```tsx
// useComments.ts — Orval 생성 함수 사용 (올바름)
queryKey: getGetCommentsQueryKey(variables.postId);

// PostDetailPage.tsx — 문자열 하드코딩 (위험)
queryKey: [`/api/v1/boards/${boardType}/posts/${post.postId}`];
queryKey: [`/api/v1/posts/${post.postId}/bookmarks/status`];
```

**개선 방향:**

1. `alert()` 전부 제거 → Toast 통일. SweetAlert2도 제거 또는 Toast로 대체
2. isDark 삼항 제거 → 시맨틱 토큰 활용. `dark:shadow-none` 등 Tailwind dark variant 사용
3. 쿼리 키 하드코딩 → Orval 생성 함수(`getXxxQueryKey`)로 통일

---

### 5. 상태 관리 — 72/100

**구조 자체는 괜찮으나 세부 구현에 문제.**

강점:

- `authStore` / `uiStore` 관심사 분리
- 타입을 `types/store.ts`에서 `AuthState + AuthActions = AuthStore`로 합성
- Zustand persist + `partialize`로 민감 정보 제외
- `onFinishHydration` + `hasHydrated` 이중 체크로 race condition 방지
- `useAuth()` 파사드 훅으로 store 직접 접근 방지

문제:

- `uiStore`에 `modalContent: ReactNode` 저장 — ReactNode는 직렬화 불가. `partialize`에서 제외하고 있지만 설계적 안티패턴
- 토스트 ID를 `Date.now()`로 생성 — 같은 ms에 2개 토스트 추가 시 ID 충돌 가능
- CLAUDE.md에 "다이얼로그는 props 없이 Zustand store에서 관리"라고 했지만, LoginForm은 props 기반

**개선 방향:**

- 토스트 ID를 카운터 또는 `crypto.randomUUID()`로 변경
- 모달 콘텐츠는 store에 저장하지 말고, 모달 타입/데이터만 저장

---

### 6. API 레이어 — 75/100

강점:

- Orval `tags-split` 모드로 도메인별 디렉토리 자동 분리
- `customFetch`를 Orval mutator로 주입하여 인증/에러/토큰 갱신 중앙 집중화
- QueryClient 설정 최적화 (`staleTime: 5분`, `gcTime: 30분`, `retry: 1`)
- 커스텀 query 훅 레이어 (`hooks/queries/`)로 캐시 무효화 캡슐화

문제:

- `customFetch` 반환에서 `as T` 타입 단언
- 쿼리 키 하드코딩 (위 일관성 항목 참조)
- `hooks/queries/useComments.ts`에서 Orval 키 함수를 쓰면서, `PostDetailPage.tsx`에서는 하드코딩 — 같은 기능인데 방식이 다름

---

### 7. 인증 아키텍처 — 78/100

강점:

- 토큰 갱신 싱글턴 패턴 (`ensureRefresh`) — 동시 다발 401에서 중복 갱신 방지
- 선제적 토큰 갱신 (만료 60초 전)
- 비즈니스 로직 에러 vs 토큰 에러 구분 (`tokenErrorCodes` Set)
- ProtectedRoute의 `minRole` 기반 계층적 권한 가드

문제:

- `window.location.href = '/login'` 하드 리다이렉트 — React Router 네비게이션을 벗어남, 앱 상태 전체 소실
- Access Token을 localStorage에 저장 — XSS 취약 (refreshToken은 httpOnly 쿠키로 올바르게 관리)

---

### 8. 에러 처리 — 80/100

강점:

- `ApiError` 클래스로 백엔드 ErrorResponse와 1:1 매핑
- 148개 에러 코드별 한국어 메시지 매핑
- 45개 타입 안전 헬퍼 함수 (`isBoardReadDenied()` 등)
- 레이어별 에러 처리 패턴 문서화 (Hook → Component → Mutation)

문제:

- 에러 처리 헬퍼가 게시판 페이지에만 적용 완료, 인증/행사/문의 페이지는 미적용 (마이그레이션 중)
- `onError`에서 `alert()` vs `toast.error()` 혼용

---

### 9. 디자인 시스템 — 60/100

강점:

- 체계적 디자인 토큰 (8단계 브랜드/그레이, 시맨틱 컬러, spacing, radius)
- 다크모드 시맨틱 토큰 정의
- `typo-*` 유틸리티로 tailwind-merge 충돌 해결
- `@media (prefers-reduced-motion: reduce)` 접근성 고려

문제:

- **`index.css` 748줄 단일 파일** — SweetAlert, 마크다운 에디터, 히어로 섹션 스타일 혼재
- **시맨틱 토큰을 정의해놓고 컴포넌트에서 `isDark ?` 삼항으로 우회** — 디자인 시스템의 존재 의미 반감
- 일부 컬러 하드코딩 (`rgba(3, 166, 158, 0.5)` 등 raw 값)
- UI 프리미티브가 5개뿐 (`button`, `card`, `input`, `spinner`, `toast`) — Select, Checkbox, Modal 등 부재

---

### 10. 성능 최적화 — 40/100

- **Code Splitting 없음**: `React.lazy()` 한 줄도 없음. 31개 페이지 전부 정적 import
- **Error Boundary 없음**: 렌더링 에러 시 앱 전체 흰 화면
- **메모이제이션 미적용**: `useMemo`/`useCallback` 거의 미사용
- **이미지 최적화 없음**: `<img>` 직접 사용, lazy loading 미적용
- **Vite 번들 최적화 설정 없음**: `manualChunks`, `rollupOptions` 미설정

---

### 11. 라우팅 — 60/100

강점:

- 단일 `router.tsx`에서 전체 라우트 파악 가능
- `ProtectedRoute`의 `minRole` prop으로 선언적 권한 가드
- 논리적 라우트 그룹핑 (공개, 인증, 게시판, 행사, 마이페이지, 관리자)

문제:

- **lazy loading 없음** — 31개 페이지 전부 초기 번들에 포함
- Feature flag(`__FEATURE_COMMUNITY__`, `__FEATURE_SEARCH__`)가 라우트에 미적용
- 라우트가 단일 파일에 전부 나열 — 도메인별 분리 미흡

---

### 12. 기타 문제

#### 명령형 DOM 접근

```tsx
// PostDetailPage.tsx:193-199
const handleCommentClick = () => {
  const input = document.getElementById("comment-input") as HTMLInputElement;
  if (input) {
    input.scrollIntoView({ behavior: "smooth", block: "center" });
    setTimeout(() => {
      input.focus();
    }, 300);
  }
};
```

React에서 `useRef`로 해야 할 일을 `document.getElementById` + `setTimeout`으로 처리.

#### HomePage 하드코딩 목 데이터

```tsx
const FEATURED_POSTS: Post[] = [
  { id: "1", title: "2024 봄학기 신입회원 모집", ... },
  // ...
];
```

API가 있는 프로젝트에서 메인 페이지가 하드코딩 데이터를 렌더링. "2024 봄학기"가 고정.

#### components/board 디렉토리 위치

`components/board/` (`SortSelect`, `Pagination`)가 `components/feature/board/` 밖에 독립 존재. 범용이면 `ui/`에, board 전용이면 `feature/board/`에 있어야 함.

---

## 부문별 점수 요약

| 부문           | 점수       | 등급   |
| -------------- | ---------- | ------ |
| 자기 규칙 준수 | 45/100     | F      |
| 컴포넌트 설계  | 55/100     | D      |
| 타입 안전성    | 65/100     | C+     |
| **일관성**     | **45/100** | **F**  |
| 상태 관리      | 72/100     | B-     |
| API 레이어     | 75/100     | B      |
| 인증 아키텍처  | 78/100     | B+     |
| 에러 처리      | 80/100     | A-     |
| 디자인 시스템  | 60/100     | C      |
| 성능 최적화    | 40/100     | F      |
| 라우팅         | 60/100     | C      |
| **종합**       | **62/100** | **D+** |

---

## 개선 우선순위

우선순위는 **노력 대비 효과**가 큰 순서.

### P0 — 즉시 (각 30분 이내)

1. **router.tsx에 React.lazy() 적용**
   - 10분이면 끝나는 작업. 성능 부문 인상이 크게 달라짐

   ```tsx
   const BoardListPage = lazy(() => import("@/pages/board/BoardListPage"));
   ```

2. **Layout에 ErrorBoundary 추가**
   - Outlet을 감싸는 것만으로 충분

3. **Header.tsx 템플릿 리터럴을 cn()으로 교체**
   - 자기 규칙 위반 중 가장 쉽게 고칠 수 있는 것

### P1 — 단기 (각 1-2시간)

4. **alert() 전부 제거, Toast로 통일**
   - SweetAlert2도 제거하거나 Toast로 대체
   - 알림 시스템 하나로 통일

5. **isDark 삼항 제거**
   - 시맨틱 토큰이 다크모드를 처리하게 하고, 필요한 경우 `dark:` variant 사용
   - 동일 코드 분기 (PostDetailPage:236) 같은 무의미한 삼항부터 제거

6. **쿼리 키 하드코딩을 Orval 생성 함수로 교체**
   - `getGetCommentsQueryKey` 패턴을 PostDetailPage 등에도 적용

### P2 — 중기 (각 반나절)

7. **갓 컴포넌트 분해**
   - PostDetailPage → `usePostActions` 훅 추출
   - MyPage → 탭별 컴포넌트 분리
   - SignupPage → 스텝별 컴포넌트 분리

8. **LoginForm에 React Hook Form + Zod 적용**
   - 이미 SignupPage에서 패턴이 있으므로 따라하면 됨

9. **index.css 분할**
   - `base.css`, `typography.css`, `vendor/sweetalert.css`, `vendor/mdeditor.css`, `hero.css` 등으로 분리

### P3 — 장기

10. **UI 프리미티브 확충** (Select, Checkbox, Modal, Dialog, Dropdown)
11. **HomePage API 연동** (하드코딩 목 데이터 제거)
12. **PostDetailPage의 `document.getElementById`를 `useRef`로 교체**

---

## 강점 정리 (유지할 것)

- 에러 처리 유틸 (`utils/error.ts`): 148개 에러 코드 매핑, 45개 타입 안전 헬퍼
- 토큰 갱신 싱글턴 + 선제 갱신 + 비즈니스/토큰 에러 분리 (`api/client.ts`)
- tsconfig.json의 엄격한 설정 (`noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`)
- 디자인 토큰 체계 (브랜드 8단계, 그레이 8단계, 시맨틱 컬러)
- Zustand store 타입 분리 (`types/store.ts`)
- 권한 시스템 (`constants/permissions.ts`): 계층적 모델 + 도메인별 함수
- `typo-*` 접두사로 tailwind-merge 충돌 해결
