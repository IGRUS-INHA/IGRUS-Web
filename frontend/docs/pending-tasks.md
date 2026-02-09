# 프론트엔드 미완성 작업 목록

**작성일**: 2026-02-05
**최종 업데이트**: 2026-02-09
**상태**: Draft
**총 작업 수**: 12개 (완료 5개, 부분 완료 2개, 미완료 5개)

---

## 목차

- [P0 - Critical (긴급)](#p0---critical-긴급)
- [P1 - High (높음)](#p1---high-높음)
- [P2 - Medium (중간)](#p2---medium-중간)
- [P3 - Low (낮음)](#p3---low-낮음)
- [작업 순서 추천](#작업-순서-추천)

---

## P0 - Critical (긴급)

**총 2개 작업 (부분 완료 1개) | 예상 시간: 1.5시간**

### 1. 백엔드 에러 메시지 파싱 및 표시 개선 🔧 (부분 완료)

**현재 문제**

- "학번은 8자리 숫자여야합니다" 클라이언트 검증 오류가 "이미 가입된 학번입니다" 백엔드 에러로 덮어씌워짐
- 사용자가 실제 오류 원인을 파악하기 어려움

**작업 규모**: 🟢 소 (1시간)

**세부 작업 항목**

1. 백엔드 에러 응답 구조 분석 (ErrorCode, message 필드 확인)
2. `axiosClient.ts`에서 에러 인터셉터 개선
   - `response.data.errorCode` 또는 `response.data.message` 파싱
   - 에러 코드별 메시지 매핑 객체 생성
3. `SignupPage.tsx`에서 에러 처리 로직 수정
   - 클라이언트 검증 오류 우선 표시
   - 백엔드 에러는 서버 응답 메시지 그대로 표시
4. 테스트 시나리오:
   - 이미 가입된 학번으로 시도 → "이미 가입된 학번입니다"
   - 7자리 학번 입력 → "학번은 8자리 숫자여야합니다"
   - 잘못된 형식 입력 → 각각의 정확한 에러 메시지

**관련 파일**

- `frontend/src/api/axiosClient.ts`
- `frontend/src/pages/auth/SignupPage.tsx`
- `frontend/src/pages/auth/LoginPage.tsx`

---

### 2. 비밀번호 확인 필드 유효성 검사 개선

**현재 문제**

- 비밀번호 확인 input에서 포커스를 잃었다가 다시 클릭해도 일치 여부 확인이 안 됨
- onBlur 이벤트만 처리되어 재입력 시 검증 누락

**작업 규모**: 🟢 소 (30분)

**세부 작업 항목**

1. `SignupPage.tsx` 비밀번호 확인 필드 수정
2. `onBlur` 이벤트에서 비밀번호 일치 여부 검증
3. `onChange` 이벤트에서도 실시간 검증 추가
4. 불일치 시 즉시 에러 메시지 표시
5. 일치 시 성공 피드백 표시 (선택)

**관련 파일**

- `frontend/src/pages/auth/SignupPage.tsx`

---

## P1 - High (높음)

**총 5개 작업 (완료 4개, 미완료 1개) | 예상 시간: 6.7시간**

### 3. 이메일 미인증 사용자 처리 플로우 개선 ✅

**완료일**: 2026-02-09

**구현 내용**

1. 로그인 실패 시 `USER_NOT_VERIFIED` 에러 코드 감지 처리
2. `LoginPage.tsx` - 미인증 에러 시 인증 페이지로 리다이렉트
3. `SignupPage.tsx` - 이미 가입된 학번이지만 미인증인 경우 인증 페이지로 리다이렉트
4. `VerifyEmailPage.tsx` - state로 전달받은 정보 활용하여 안내 메시지 표시

**관련 파일**

- `frontend/src/pages/auth/LoginPage.tsx`
- `frontend/src/pages/auth/SignupPage.tsx`
- `frontend/src/pages/auth/VerifyEmailPage.tsx`
- `frontend/src/api/auth.ts`

---

### 4. 인증 코드 유효시간 10분으로 변경

**현재 문제**

- 인증 코드 유효시간이 명확하지 않거나 백엔드와 불일치
- 사용자가 남은 시간을 알 수 없음

**작업 규모**: 🟢 소 (1시간)

**세부 작업 항목**

1. 백엔드 확인
   - 이메일 인증 코드 유효시간 설정 확인 (현재 값)
   - 필요시 백엔드 설정 변경 요청 (10분으로)
2. `VerifyEmailPage.tsx` 수정
   - 타이머 UI 추가: "남은 시간: 9:30" 형식
   - 10분(600초) 카운트다운 구현
   - `useEffect`로 1초마다 감소
   - 만료 시 재발송 버튼 활성화 및 안내 메시지
3. 재발송 버튼 클릭 시 타이머 리셋
4. 만료된 코드 입력 시 명확한 에러 메시지

**관련 파일**

- `frontend/src/pages/auth/VerifyEmailPage.tsx`
- `backend/.../EmailVerificationService.java` (확인 필요)

---

### 5. 특정 페이지에서 헤더 제목 숨김 ✅

**완료일**: 2026-02-09

**구현 내용**

1. `Header.tsx`에서 `useLocation()` 훅으로 현재 경로 확인
2. 숨김 대상 경로 배열 정의 및 조건부 렌더링 적용
3. 로그인, 홈, 비밀번호 찾기, 이메일 인증 페이지에서 헤더 제목 숨김

**관련 파일**

- `frontend/src/components/common/Header.tsx`

---

### 6. 검색 가능 페이지에만 헤더 검색창 표시 ✅

**완료일**: 2026-02-05

**구현 내용**

1. `Header.tsx` 수정 완료
   - 검색 가능 경로 확인: `/board/*`, `/events`
   - `useLocation()`으로 현재 경로 매칭하여 조건부 렌더링
   - 검색어 입력 시 쿼리 파라미터 업데이트 (`?search=검색어`)
2. `SearchBar.tsx` 기능 구현
   - Enter 키로 검색 실행
   - X 버튼으로 검색어 초기화
   - 검색어 상태를 props로 제어
3. `BoardListPage.tsx` 수정
   - URL 쿼리 파라미터(`?search=`)에서 검색어 읽어서 API 요청에 포함
   - 검색어 변경 시 첫 페이지로 리셋
4. `EventListPage.tsx` 수정
   - URL 쿼리 파라미터에서 검색어 읽어서 필터링
   - Mock 데이터의 경우 클라이언트 사이드 필터링 (제목/설명)
   - 검색 결과 없을 때 안내 메시지 표시

**작동 방식**

- 헤더 검색창에 검색어 입력 후 Enter → URL에 `?search=검색어` 추가
- 각 페이지에서 쿼리 파라미터를 읽어서 자동으로 검색 실행
- 검색어 초기화 시 쿼리 파라미터 제거

**상세 구현 설명**

#### 1. URL 쿼리 파라미터를 사용한 검색

헤더의 검색창에서 검색어를 입력하면 URL에 `?search=검색어` 형태로 추가됩니다.

예시:
- `/board/notices` → 검색 후 → `/board/notices?search=React`
- `/events` → 검색 후 → `/events?search=세미나`

#### 2. Header.tsx의 동작

```tsx
// 1. URL에서 검색어 읽기
const [searchParams] = useSearchParams();
const searchKeyword = searchParams.get('search') || '';

// 2. 검색 실행 시 URL 업데이트
const handleSearch = (keyword: string) => {
  const newParams = new URLSearchParams(searchParams);
  newParams.set('search', keyword); // search 파라미터 추가
  navigate(`${location.pathname}?${newParams.toString()}`);
};
```

**흐름:**
1. 사용자가 검색창에 "React" 입력 후 Enter
2. URL이 `/board/notices?search=React`로 변경
3. 페이지는 리로드되지 않고 URL만 업데이트

#### 3. BoardListPage의 동작

```tsx
// 1. URL에서 검색어 읽기
const [searchParams] = useSearchParams();
const searchKeyword = searchParams.get('search');

// 2. 검색어가 있으면 API 요청에 포함
const { data: response } = useGetPostList(validBoardType, {
  ...(searchKeyword && { keyword: searchKeyword }), // 조건부로 포함
  page: currentPage - 1,
  size: 20,
});
```

**흐름:**
1. URL이 `/board/notices?search=React`로 변경됨
2. `searchParams.get('search')`로 "React" 읽기
3. API 호출 시 `keyword: "React"` 포함
4. 백엔드가 "React"를 포함한 게시글 반환

#### 4. EventListPage의 동작

```tsx
// 1. URL에서 검색어 읽기
const searchKeyword = searchParams.get('search');

// 2. useEvents에 keyword 전달 (실제 API용)
const { data: events } = useEvents(
  searchKeyword ? { keyword: searchKeyword } : {}
);

// 3. Mock 데이터는 클라이언트에서 필터링
const filteredEvents = useMemo(() => {
  if (!events || !searchKeyword) return events;
  return events.filter(event =>
    event.title.toLowerCase().includes(searchKeyword.toLowerCase())
  );
}, [events, searchKeyword]);
```

**흐름:**
1. URL이 `/events?search=세미나`로 변경됨
2. "세미나" 키워드로 이벤트 필터링
3. 제목이나 설명에 "세미나"가 포함된 항목만 표시

#### 5. 이 방식을 사용한 이유

**장점:**
1. **URL 공유 가능**: 검색 결과를 URL로 공유 가능 (`/board/notices?search=React`)
2. **브라우저 히스토리**: 뒤로가기/앞으로가기 버튼으로 검색 이전/이후 상태로 이동
3. **북마크 가능**: 특정 검색 결과를 북마크 가능
4. **새로고침 유지**: 페이지 새로고침해도 검색 상태 유지
5. **단일 진실 공급원**: URL이 검색 상태의 유일한 출처

**비교:**
- ❌ **useState만 사용**: 새로고침하면 검색 상태 소실, URL 공유 불가
- ✅ **URL 쿼리 파라미터**: 위의 모든 장점 활용

#### 6. 사용자 경험 예시

1. 사용자가 게시판 페이지에서 헤더 검색창에 "TypeScript" 입력
2. URL이 `/board/general?search=TypeScript`로 변경
3. 게시판이 자동으로 "TypeScript" 키워드로 검색된 결과 표시
4. 사용자가 이 URL을 복사해서 친구에게 공유
5. 친구가 링크를 열면 동일한 검색 결과 확인 가능
6. 뒤로가기 버튼으로 검색 이전 상태로 돌아갈 수 있음
7. 새로고침해도 검색 결과 유지

**관련 파일**

- `frontend/src/components/common/Header.tsx`
- `frontend/src/components/common/SearchBar.tsx`
- `frontend/src/pages/board/BoardListPage.tsx`
- `frontend/src/pages/event/EventListPage.tsx`

---

### 7. 히어로 섹션 디자인 구현 ✅

**완료일**: 2026-02-09

**구현 내용**

1. 홈페이지에 히어로 섹션 추가 (배경 그라데이션, 헤드라인, CTA 버튼)
2. 애니메이션 효과 적용 (fade-in, scroll reveal)
3. 반응형 레이아웃 구현 (모바일/태블릿/데스크톱)

**관련 파일**

- `frontend/src/pages/HomePage.tsx`
- `frontend/src/components/home/` (HeroSection 컴포넌트)

---

## P2 - Medium (중간)

**총 4개 작업 (완료 1개, 부분 완료 1개, 미완료 2개) | 예상 시간: 13.2시간**

### 8. 커스텀 Alert/Confirm 컴포넌트 구현 🔧 (부분 완료)

**상태**: 디자인 구현 완료 / 기존 alert·confirm 교체는 부분적

**완료 항목**

1. 공용 컴포넌트 생성 완료 (Alert, Confirm, Toast)
2. Zustand 스토어 전역 상태 관리 구현 완료
3. 디자인 (모달 오버레이, 아이콘, 버튼, 애니메이션) 구현 완료

**남은 작업**

- 기존 `alert()`, `confirm()` 호출을 `useDialogStore()`로 전체 교체
  - 전역 검색으로 남은 사용처 찾아서 교체 필요

**관련 파일**

- `frontend/src/components/common/Alert.tsx`
- `frontend/src/components/common/Confirm.tsx`
- `frontend/src/stores/dialogStore.ts`
- 모든 페이지에서 기존 alert/confirm 교체 (진행 중)

---

### 9. 관리자 화면 및 마이페이지 레이아웃 구성 ✅

**완료일**: 2026-02-09

**구현 내용**

1. 관리자 대시보드 - API 연결 및 통계 카드 실시간 데이터 표시
2. 준회원 승인 페이지 - 목록 조회, 개별/일괄 승인 기능
3. 회원 관리 페이지 - 목록 조회, 검색 필터, 권한 변경, 계정 정지/해제
4. 문의 관리 페이지 - 목록 조회, 필터링, 상태 변경, 답변 작성
5. 마이페이지 - 프로필 표시, 4개 탭 API 연결, 프로필 수정, 비밀번호 변경

**관련 파일**

- `frontend/src/pages/admin/AdminDashboard.tsx`
- `frontend/src/pages/admin/AdminAssociates.tsx`
- `frontend/src/pages/admin/AdminUsers.tsx`
- `frontend/src/pages/admin/AdminInquiries.tsx`
- `frontend/src/pages/mypage/MyPage.tsx`
- `frontend/src/api/admin.ts`
- `frontend/src/api/users.ts`

---

### 10. 반응형 레이아웃 글씨 줄바꿈 개선

**현재 문제**

- 모바일/태블릿에서 긴 텍스트가 레이아웃을 깨뜨림
- 테이블 셀에서 텍스트 오버플로우
- 한글 단어 중간에서 끊김

**작업 규모**: 🟡 중 (2시간)

**세부 작업 항목**

1. 전역 CSS 추가 (`frontend/src/index.css`)

   ```css
   /* 한글 줄바꿈 최적화 */
   .word-break-keep {
     word-break: keep-all;
     overflow-wrap: break-word;
   }

   /* 영문 줄바꿈 */
   .word-break-all {
     word-break: break-all;
   }

   /* 말줄임 (1줄) */
   .truncate {
     overflow: hidden;
     text-overflow: ellipsis;
     white-space: nowrap;
   }

   /* 말줄임 (여러 줄) */
   .line-clamp-2 {
     display: -webkit-box;
     -webkit-line-clamp: 2;
     -webkit-box-orient: vertical;
     overflow: hidden;
   }
   ```

2. 적용 대상 컴포넌트
   - 게시글 제목: `.truncate` 또는 `.line-clamp-2`
   - 게시글 내용: `.word-break-keep`
   - 댓글 내용: `.word-break-keep`
   - 행사 설명: `.word-break-keep`
   - 테이블 셀 (긴 텍스트): `.truncate` + 툴팁

3. 테이블 최적화

   ```tsx
   <td className="max-w-[200px] truncate" title={fullText}>
     {text}
   </td>
   ```

4. 반응형 테스트
   - 모바일 (375px, 390px)
   - 태블릿 (768px, 1024px)
   - 데스크톱 (1280px, 1920px)

**관련 파일**

- `frontend/src/index.css`
- `frontend/src/pages/board/BoardListPage.tsx`
- `frontend/src/pages/board/PostDetailPage.tsx`
- `frontend/src/pages/event/EventListPage.tsx`
- `frontend/src/pages/admin/*.tsx` (테이블 있는 모든 페이지)

---

### 11. 페이지 하단 라우터 디버그 정보 제거

**현재 문제**

- 왼쪽 하단에 개발용 라우터 정보가 프로덕션에도 표시됨
- React Router DevTools 또는 디버그 컴포넌트가 남아있음

**작업 규모**: 🟢 소 (10분)

**세부 작업 항목**

1. `router.tsx` 또는 `App.tsx` 확인
2. React Router DevTools 찾기
   ```tsx
   import { ReactRouterDevtools } from "react-router-devtools";
   ```
3. 조건부 렌더링으로 변경
   ```tsx
   {
     process.env.NODE_ENV === "development" && <ReactRouterDevtools />;
   }
   ```
4. 또는 완전히 제거 (운영 환경에서 불필요한 경우)
5. `.env.development`와 `.env.production` 확인
6. 빌드 후 프로덕션 모드에서 테스트

**관련 파일**

- `frontend/src/router.tsx`
- `frontend/src/App.tsx`
- `frontend/.env.development`
- `frontend/.env.production`

---

## P3 - Low (낮음)

**총 1개 작업 | 예상 시간: 6시간**

### 12. E2E 테스트 환경 구축 (Playwright)

**목표**

- Playwright로 E2E 테스트 자동화 기반 마련
- CI/CD 파이프라인 통합
- 주요 사용자 플로우 테스트 커버리지 확보

**작업 규모**: 🔴 대 (6시간)

**세부 작업 항목**

1. Playwright 설치

   ```bash
   cd frontend
   npm install -D @playwright/test
   npx playwright install
   ```

2. 설정 파일 생성 (`playwright.config.ts`)

   ```ts
   import { defineConfig } from "@playwright/test";

   export default defineConfig({
     testDir: "./tests/e2e",
     fullyParallel: true,
     forbidOnly: !!process.env.CI,
     retries: process.env.CI ? 2 : 0,
     workers: process.env.CI ? 1 : undefined,
     reporter: "html",
     use: {
       baseURL: "http://localhost:5173",
       trace: "on-first-retry",
     },
     webServer: {
       command: "npm run dev",
       url: "http://localhost:5173",
       reuseExistingServer: !process.env.CI,
     },
   });
   ```

3. 테스트 디렉토리 생성

   ```
   frontend/tests/e2e/
   ├── auth/
   │   ├── signup.spec.ts
   │   ├── login.spec.ts
   │   └── password-reset.spec.ts
   ├── board/
   │   ├── post-create.spec.ts
   │   └── post-read.spec.ts
   └── fixtures/
       └── test-data.ts
   ```

4. 우선 구현 테스트

   **signup.spec.ts** (회원가입 플로우)

   ```ts
   test("회원가입 성공 시나리오", async ({ page }) => {
     await page.goto("/signup");
     await page.fill('[name="studentId"]', "12345678");
     await page.fill('[name="name"]', "홍길동");
     // ... 나머지 필드
     await page.click('button[type="submit"]');
     await expect(page).toHaveURL("/verify-email");
   });
   ```

   **login.spec.ts** (로그인 플로우)

   ```ts
   test("로그인 성공 시나리오", async ({ page }) => {
     await page.goto("/login");
     await page.fill('[name="studentId"]', "12345678");
     await page.fill('[name="password"]', "Password123!");
     await page.click('button[type="submit"]');
     await expect(page).toHaveURL("/");
   });
   ```

   **post-create.spec.ts** (게시글 작성)

   ```ts
   test("게시글 작성 성공", async ({ page }) => {
     // 로그인 먼저
     await page.goto("/board/GENERAL/write");
     await page.fill('[name="title"]', "테스트 게시글");
     await page.fill('[name="content"]', "내용입니다");
     await page.click('button[type="submit"]');
     await expect(page.locator("text=테스트 게시글")).toBeVisible();
   });
   ```

5. 테스트 헬퍼 함수 작성

   ```ts
   // tests/e2e/helpers/auth.ts
   export async function login(page, studentId, password) {
     await page.goto("/login");
     await page.fill('[name="studentId"]', studentId);
     await page.fill('[name="password"]', password);
     await page.click('button[type="submit"]');
     await page.waitForURL("/");
   }
   ```

6. CI/CD 통합 (GitHub Actions)

   ```yaml
   # .github/workflows/e2e-test.yml
   name: E2E Tests

   on: [push, pull_request]

   jobs:
     test:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v3
         - uses: actions/setup-node@v3
           with:
             node-version: "20"
         - run: npm ci
         - run: npx playwright install --with-deps
         - run: npm run test:e2e
         - uses: actions/upload-artifact@v3
           if: always()
           with:
             name: playwright-report
             path: playwright-report/
   ```

7. package.json 스크립트 추가
   ```json
   {
     "scripts": {
       "test:e2e": "playwright test",
       "test:e2e:ui": "playwright test --ui",
       "test:e2e:debug": "playwright test --debug"
     }
   }
   ```

**관련 파일**

- `playwright.config.ts` (신규)
- `frontend/tests/e2e/**/*.spec.ts` (신규)
- `frontend/tests/e2e/helpers/*.ts` (신규)
- `.github/workflows/e2e-test.yml` (신규)
- `package.json` (스크립트 추가)

---

## 작업 순서 추천

### 1주차: Critical + High 핵심 (P0 + P1 일부)

**목표**: 사용자 경험 저해 요소 제거, 핵심 플로우 완성

- [ ] Task 1: 백엔드 에러 메시지 파싱 (1h)
- [ ] Task 2: 비밀번호 확인 검증 (30m)
- [ ] Task 3: 이메일 미인증 처리 (2h)
- [ ] Task 4: 인증 코드 타이머 (1h)

**예상 총 시간**: 4.5시간

---

### 2주차: UI 개선 + 관리 페이지 (P1 나머지 + P2 일부)

**목표**: UI 완성도 향상, 관리자 기능 구현

- [ ] Task 5: 헤더 조건부 렌더링 (20m)
- [ ] Task 6: 검색창 조건부 표시 (20m)
- [ ] Task 7: 히어로 섹션 디자인 (3h)
- [ ] Task 9-1: 관리자 대시보드 API 연결 (1h)
- [ ] Task 9-2: 준회원 승인 페이지 (2h)

**예상 총 시간**: 6.7시간

---

### 3주차: 관리 페이지 완성 + 마이페이지 (P2 계속)

**목표**: 관리자 및 사용자 페이지 완성

- [ ] Task 9-3: 회원 관리 페이지 (3h)
- [ ] Task 9-4: 문의 관리 페이지 (1.5h)
- [ ] Task 9-5: 마이페이지 완성 (30m)
- [ ] Task 8: 커스텀 Alert/Confirm (3h)

**예상 총 시간**: 8시간

---

### 4주차: 완성도 향상 + 테스트 (P2 나머지 + P3)

**목표**: 반응형 개선, 품질 보증

- [ ] Task 10: 반응형 글씨 줄바꿈 (2h)
- [ ] Task 11: 라우터 디버그 제거 (10m)
- [ ] Task 12: Playwright 환경 구축 (6h)

**예상 총 시간**: 8.2시간

---

## 총 예상 작업 시간

| 우선순위      | 작업 수  | 예상 시간    |
| ------------- | -------- | ------------ |
| P0 (Critical) | 2개      | 1.5시간      |
| P1 (High)     | 5개      | 6.7시간      |
| P2 (Medium)   | 4개      | 13.2시간     |
| P3 (Low)      | 1개      | 6시간        |
| **총계**      | **12개** | **27.4시간** |

---

## 체크리스트

### Phase 1: Critical (즉시 착수)

- [~] 백엔드 에러 메시지 파싱 (부분 완료)
- [ ] 비밀번호 확인 검증

### Phase 2: Core Features (1-2주)

- [x] 이메일 미인증 처리
- [ ] 인증 코드 타이머
- [x] 헤더 조건부 렌더링
- [x] 검색창 조건부 표시
- [x] 히어로 섹션

### Phase 3: Admin & User Pages (2-3주)

- [x] 관리자 대시보드
- [x] 준회원 승인
- [x] 회원 관리
- [x] 문의 관리
- [x] 마이페이지
- [~] 커스텀 Alert/Confirm (부분 완료 - 디자인 완료, 적용 진행 중)

### Phase 4: Polish & Testing (4주)

- [ ] 반응형 개선
- [ ] 디버그 정보 제거
- [ ] E2E 테스트 환경

---

**마지막 업데이트**: 2026-02-09 (Task 3, 5, 7, 9 완료 / Task 1, 8 부분 완료)
