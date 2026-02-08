# Board 페이지 기능 마이그레이션 가이드

병합 시 Remote 버전을 채택하면서 HEAD에만 있던 기능들을 정리한 문서입니다.
향후 개별적으로 도입할 때 참고하세요.

---

## 1. 서버 기반 게시판 권한 체크

### 설명
`useBoardList()`, `useBoardByCode()` 훅을 통해 백엔드 API에서 게시판별 `canRead`, `canWrite` 권한을 받아 처리합니다. 현재 Remote 버전은 클라이언트 상수(`BOARD_CATEGORIES`)로 하드코딩되어 있습니다.

### 관련 훅 (이미 구현됨)
- `frontend/src/hooks/useBoards.ts` - `useBoardList()`, `useBoardByCode()`, `useCurrentBoard()`

### 적용 대상
- **BoardListPage.tsx**: 게시판 탭 목록을 `boards.map()`으로 렌더링 + `board.canRead`로 접근 제어 + `board.canWrite`로 글쓰기 버튼 비활성화
- **PostWritePage.tsx / PostEditPage.tsx**: `useCurrentBoard()`로 `allowsAnonymous`, `allowsQuestionTag` 등 서버 설정 반영

### Before (현재 Remote)
```typescript
// 하드코딩된 상수 사용
import { BOARD_CATEGORIES, POST_OPTIONS } from '@/constants/board';
const categories = BOARD_CATEGORIES[validBoardType] || [];
```

### After (향후)
```typescript
// 서버 API 기반
import { useBoardList, useBoardByCode } from '@/hooks/useBoards';
const { boards } = useBoardList();
const { board } = useBoardByCode(validBoardType);

// 권한 체크
if (!board.canRead) { navigate('/'); }
<Button disabled={!board.canWrite}>글쓰기</Button>
```

---

## 2. 토스트 알림 (권한 거부)

### 설명
`useUIStore`의 `addToast`를 사용하여 권한 부족 시 토스트 메시지를 표시하고 리다이렉트합니다.

### 적용 대상
- **BoardListPage.tsx**: 게시판 접근 권한이 없을 때 토스트 + 홈으로 리다이렉트

### After (향후)
```typescript
import { useUIStore } from '@/stores';
const { addToast } = useUIStore();

useEffect(() => {
  if (!boardLoading && !board.canRead) {
    addToast({
      type: 'warning',
      title: '접근 권한 부족',
      message: '게시판 조회 권한이 없습니다.',
      duration: 5000,
    });
    navigate('/', { replace: true });
  }
}, [boardLoading, board.canRead, addToast, navigate]);
```

---

## 3. 에러 헬퍼 함수 적용

### 설명
`@/utils/error`의 헬퍼 함수(`isForbiddenError`, `isNotFoundError`, `getErrorMessage`)를 사용하여 타입 안전한 에러 처리를 합니다. 현재 Remote 버전은 `error.message?.includes('403')` 패턴을 사용합니다.

### 적용 대상
- **PostDetailPage.tsx**: 삭제 에러 처리
- **PostWritePage.tsx**: 작성 에러 처리
- **PostEditPage.tsx**: 수정 에러 처리

### 상세 가이드
`frontend/docs/migration/error-handling-migration.md` 참조

---

## 4. PostDetailResponse 타입 캐스팅

### 설명
Orval 생성 타입의 nullable 필드를 안전하게 처리하기 위해 `PostDetailResponse`로 캐스팅합니다.

### 적용 대상
- **PostDetailPage.tsx**

### Before (현재 Remote)
```typescript
const post = response?.data;
```

### After (향후)
```typescript
import type { PostDetailResponse } from '@/api/model/models';
const post = response?.data as PostDetailResponse | undefined;
```

---

## 5. formatTime 유틸 함수

### 설명
게시글 작성 시간을 상대적 형식("방금 전", "5분 전", "2시간 전", "3일 전")으로 표시하는 함수입니다. Remote 버전에는 이 함수가 없어 원시 날짜가 표시됩니다.

### 적용 대상
- **PostDetailPage.tsx**: 게시글 작성 시간 표시

### 구현
```typescript
const formatTime = (createdAt?: string) => {
  if (!createdAt) return '';
  const date = new Date(createdAt);
  const now = new Date();
  const diff = Math.floor((now.getTime() - date.getTime()) / 1000 / 60);

  if (diff < 1) return '방금 전';
  if (diff < 60) return `${diff}분 전`;
  if (diff < 1440) return `${Math.floor(diff / 60)}시간 전`;
  if (diff < 10080) return `${Math.floor(diff / 1440)}일 전`;

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
};
```

> **권장**: 이 함수는 `@/utils/date.ts`로 추출하여 공용 유틸로 사용하세요.

---

## 6. 종합 쿼리 무효화 (Invalidation)

### 설명
좋아요/북마크 토글 후 상세 페이지뿐 아니라 목록 페이지 쿼리도 함께 무효화하여, 뒤로가기 시 카운트가 즉시 반영되도록 합니다.

### 적용 대상
- **PostDetailPage.tsx**: handleLike, handleScrap

### Before (현재 Remote)
```typescript
// 상세 쿼리만 갱신
onSuccess: () => {
  void queryClient.invalidateQueries({
    queryKey: [`/api/v1/boards/${boardType}/posts/${post.postId}`],
  });
}
```

### After (향후)
```typescript
// 상세 + 목록 + 북마크 상태 모두 갱신
onSuccess: async () => {
  await queryClient.invalidateQueries({
    queryKey: [`/api/v1/posts/${post.postId}/bookmarks/status`],
  });
  await queryClient.invalidateQueries({
    queryKey: [`/api/v1/boards/${boardType}/posts/${post.postId}`],
  });
  await queryClient.invalidateQueries({
    queryKey: [`/api/v1/boards/${boardType}/posts`],
  });
}
```

---

## 7. 커스텀 Tailwind 토큰

### 설명
프로젝트 디자인 시스템에서 정의한 커스텀 spacing/radius 토큰(`s1`~`s8`, `r1`~`r4`)이 HEAD에서 사용되었습니다. Remote는 표준 Tailwind 값을 사용합니다.

### 매핑 테이블

| 커스텀 토큰 | 표준 Tailwind | 실제 값 |
|------------|--------------|--------|
| `px-s4` | `px-4` | 1rem |
| `py-s2` | `py-2` | 0.5rem |
| `mt-s2` | `mt-2` | 0.5rem |
| `rounded-r4` | `rounded-[2.5rem]` | 2.5rem |
| `rounded-r3` | `rounded-2xl` | 1.5rem |

### 적용 대상
- 모든 board 페이지의 버튼, 카드, 메뉴 등

> **참고**: 커스텀 토큰은 `frontend/src/index.css`에 정의되어 있습니다. 디자인 시스템 통일 시 일괄 적용하세요.

---

## 마이그레이션 우선순위

1. **formatTime 함수** - UX에 직접 영향, 독립적으로 추가 가능
2. **에러 헬퍼 함수** - 타입 안전성 확보, `error-handling-migration.md` 참조
3. **종합 쿼리 무효화** - 사용자 경험 개선
4. **서버 기반 권한 체크** - 훅 이미 구현됨, 페이지에 적용만 하면 됨
5. **토스트 알림** - 권한 체크와 함께 적용
6. **타입 캐스팅** - 타입 안전성
7. **커스텀 토큰** - 디자인 시스템 통일 시 일괄 적용
