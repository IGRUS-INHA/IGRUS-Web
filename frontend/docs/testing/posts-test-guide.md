# Posts (게시판) 테스트 가이드 (Playwright)

## 개요

이 문서는 게시판(Posts) 시스템의 E2E 테스트를 Playwright로 작성할 때 참고할 수 있는 가이드입니다.

**테스트 대상 기능**:

- 게시글 목록 조회 (페이지네이션)
- 게시글 상세 조회
- 게시글 작성 (로그인 필수)
- 게시글 수정/삭제
- 좋아요 토글
- 검색 및 정렬

**주요 플로우**:
게시글 목록 조회 → 상세 보기 → 좋아요 → 게시글 작성

---

## Playwright 자동화 테스트

### Page Objects

#### BoardListPage

**파일**: `e2e/pages/BoardListPage.ts`

```typescript
import { Page, Locator } from "@playwright/test";

export class BoardListPage {
  readonly page: Page;
  readonly boardType: string;

  // Post List
  readonly postItems: Locator;
  readonly postTitles: Locator;
  readonly emptyStateMessage: Locator;

  // Pagination
  readonly paginationContainer: Locator;
  readonly pageButtons: Locator;
  readonly nextButton: Locator;
  readonly prevButton: Locator;

  // Search & Sort
  readonly searchInput: Locator;
  readonly searchButton: Locator;
  readonly sortSelect: Locator;

  // Actions
  readonly writeButton: Locator;

  constructor(page: Page, boardType: "notices" | "free" | "qna" = "notices") {
    this.page = page;
    this.boardType = boardType;

    // Post List
    this.postItems = page.locator('[data-testid="post-item"]');
    this.postTitles = page.locator('[data-testid="post-title"]');
    this.emptyStateMessage = page.getByText(/게시글이 없습니다/);

    // Pagination
    this.paginationContainer = page.locator('[data-testid="pagination"]');
    this.pageButtons = this.paginationContainer.locator("button");
    this.nextButton = page.getByRole("button", { name: /다음|Next/ });
    this.prevButton = page.getByRole("button", { name: /이전|Previous/ });

    // Search & Sort
    this.searchInput = page.getByPlaceholder(/검색/);
    this.searchButton = page.getByRole("button", { name: /검색/ });
    this.sortSelect = page.locator('select[name="sort"]');

    // Actions
    this.writeButton = page.getByRole("button", { name: /글쓰기|작성/ });
  }

  async goto(page: number = 0) {
    const url =
      page > 0
        ? `/board/${this.boardType}?page=${page}`
        : `/board/${this.boardType}`;
    await this.page.goto(url);
  }

  async getPostCount() {
    return await this.postItems.count();
  }

  async clickPost(index: number) {
    await this.postItems.nth(index).click();
  }

  async clickWriteButton() {
    await this.writeButton.click();
  }

  async search(keyword: string) {
    await this.searchInput.fill(keyword);
    await this.searchButton.click();
  }

  async sortBy(sortOption: string) {
    await this.sortSelect.selectOption(sortOption);
  }

  async goToPage(pageNumber: number) {
    await this.pageButtons.filter({ hasText: String(pageNumber) }).click();
  }

  async goToNextPage() {
    await this.nextButton.click();
  }

  async goToPrevPage() {
    await this.prevButton.click();
  }
}
```

#### PostDetailPage

**파일**: `e2e/pages/PostDetailPage.ts`

```typescript
import { Page, Locator } from "@playwright/test";

export class PostDetailPage {
  readonly page: Page;

  // Post Content
  readonly postTitle: Locator;
  readonly postContent: Locator;
  readonly postAuthor: Locator;
  readonly postDate: Locator;

  // Actions
  readonly likeButton: Locator;
  readonly likeCount: Locator;
  readonly editButton: Locator;
  readonly deleteButton: Locator;
  readonly backButton: Locator;

  // Comments (if implemented)
  readonly commentInput: Locator;
  readonly commentSubmitButton: Locator;
  readonly commentItems: Locator;

  constructor(page: Page) {
    this.page = page;

    // Post Content
    this.postTitle = page.locator('[data-testid="post-title"]');
    this.postContent = page.locator('[data-testid="post-content"]');
    this.postAuthor = page.locator('[data-testid="post-author"]');
    this.postDate = page.locator('[data-testid="post-date"]');

    // Actions
    this.likeButton = page.getByRole("button", { name: /좋아요/ });
    this.likeCount = page.locator('[data-testid="like-count"]');
    this.editButton = page.getByRole("button", { name: /수정/ });
    this.deleteButton = page.getByRole("button", { name: /삭제/ });
    this.backButton = page.getByRole("button", { name: /목록|뒤로/ });

    // Comments
    this.commentInput = page.getByPlaceholder(/댓글을 입력/);
    this.commentSubmitButton = page.getByRole("button", { name: /댓글 작성/ });
    this.commentItems = page.locator('[data-testid="comment-item"]');
  }

  async goto(boardType: string, postId: number) {
    await this.page.goto(`/board/${boardType}/${postId}`);
  }

  async toggleLike() {
    await this.likeButton.click();
  }

  async getLikeCount() {
    const text = await this.likeCount.textContent();
    return parseInt(text || "0", 10);
  }

  async clickEdit() {
    await this.editButton.click();
  }

  async clickDelete() {
    await this.deleteButton.click();
  }

  async goBack() {
    await this.backButton.click();
  }

  async addComment(content: string) {
    await this.commentInput.fill(content);
    await this.commentSubmitButton.click();
  }

  async getCommentCount() {
    return await this.commentItems.count();
  }
}
```

#### PostWritePage

**파일**: `e2e/pages/PostWritePage.ts`

```typescript
import { Page, Locator } from "@playwright/test";

export class PostWritePage {
  readonly page: Page;

  readonly titleInput: Locator;
  readonly contentTextarea: Locator;
  readonly submitButton: Locator;
  readonly cancelButton: Locator;

  constructor(page: Page) {
    this.page = page;

    this.titleInput = page.getByPlaceholder(/제목/);
    this.contentTextarea = page.getByPlaceholder(/내용/);
    this.submitButton = page.getByRole("button", { name: /작성|등록/ });
    this.cancelButton = page.getByRole("button", { name: /취소/ });
  }

  async goto(boardType: string) {
    await this.page.goto(`/board/${boardType}/write`);
  }

  async fillPost(data: { title: string; content: string }) {
    await this.titleInput.fill(data.title);
    await this.contentTextarea.fill(data.content);
  }

  async submit() {
    await this.submitButton.click();
  }

  async cancel() {
    await this.cancelButton.click();
  }
}
```

### 테스트 유틸리티

#### 테스트 데이터 생성

**파일**: `e2e/utils/testData.ts` (추가)

```typescript
/**
 * 고유한 테스트 게시글 데이터 생성
 */
export function generateUniquePost() {
  const timestamp = Date.now();

  return {
    title: `테스트 게시글 ${timestamp}`,
    content: `테스트 게시글 내용입니다.\n작성 시간: ${new Date(timestamp).toISOString()}`,
  };
}
```

### 테스트 케이스

#### 1. 게시글 목록 조회 테스트

**파일**: `e2e/posts/post-list.spec.ts`

```typescript
import { test, expect } from "@playwright/test";
import { BoardListPage } from "../pages/BoardListPage";
import { waitForApiResponse } from "../utils/apiHelpers";

test.describe("게시글 목록 조회", () => {
  test("공지사항 게시판 접속", async ({ page }) => {
    const boardListPage = new BoardListPage(page, "notices");
    await boardListPage.goto();

    // API 응답 대기
    const response = await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/boards/notices/posts") &&
        response.status() === 200,
    );

    // 응답 확인
    const responseBody = await response.json();
    expect(responseBody).toHaveProperty("content");
    expect(responseBody).toHaveProperty("totalElements");

    // 게시글 목록 표시 확인
    const postCount = await boardListPage.getPostCount();
    expect(postCount).toBeGreaterThanOrEqual(0);
  });

  test("페이지네이션", async ({ page }) => {
    const boardListPage = new BoardListPage(page, "notices");
    await boardListPage.goto();

    // 첫 페이지 로드 대기
    await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/boards/notices/posts?page=0") &&
        response.status() === 200,
    );

    // 2페이지로 이동
    const nextPagePromise = page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/boards/notices/posts?page=1") &&
        response.status() === 200,
    );

    await boardListPage.goToNextPage();
    await nextPagePromise;

    // URL에 page 파라미터 확인
    expect(page.url()).toContain("page=1");
  });

  test("검색", async ({ page }) => {
    const boardListPage = new BoardListPage(page, "notices");
    await boardListPage.goto();

    // 검색어 입력 및 검색
    const searchPromise = page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/boards/notices/posts") &&
        response.url().includes("search=테스트") &&
        response.status() === 200,
    );

    await boardListPage.search("테스트");
    await searchPromise;

    // URL에 search 파라미터 확인
    expect(page.url()).toContain("search=테스트");
  });

  test("정렬", async ({ page }) => {
    const boardListPage = new BoardListPage(page, "notices");
    await boardListPage.goto();

    // 정렬 옵션 변경
    const sortPromise = page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/boards/notices/posts") &&
        response.url().includes("sort=likes") &&
        response.status() === 200,
    );

    await boardListPage.sortBy("likes");
    await sortPromise;

    // URL에 sort 파라미터 확인
    expect(page.url()).toContain("sort=likes");
  });
});
```

#### 2. 게시글 상세 조회 테스트

**파일**: `e2e/posts/post-detail.spec.ts`

```typescript
import { test, expect } from "@playwright/test";
import { BoardListPage } from "../pages/BoardListPage";
import { PostDetailPage } from "../pages/PostDetailPage";

test.describe("게시글 상세 조회", () => {
  test("게시글 클릭하여 상세 페이지 이동", async ({ page }) => {
    const boardListPage = new BoardListPage(page, "notices");
    await boardListPage.goto();

    // 목록 로드 대기
    await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/boards/notices/posts") &&
        response.status() === 200,
    );

    // 첫 번째 게시글 클릭
    const detailPromise = page.waitForResponse(
      (response) =>
        response.url().match(/\/api\/v1\/boards\/notices\/posts\/\d+$/) &&
        response.status() === 200,
    );

    await boardListPage.clickPost(0);
    const response = await detailPromise;

    // 응답 확인
    const responseBody = await response.json();
    expect(responseBody).toHaveProperty("id");
    expect(responseBody).toHaveProperty("title");
    expect(responseBody).toHaveProperty("content");
    expect(responseBody).toHaveProperty("author");

    // 상세 페이지 요소 확인
    const postDetailPage = new PostDetailPage(page);
    await expect(postDetailPage.postTitle).toBeVisible();
    await expect(postDetailPage.postContent).toBeVisible();
    await expect(postDetailPage.postAuthor).toBeVisible();
  });

  test("좋아요 토글", async ({ page }) => {
    const boardListPage = new BoardListPage(page, "notices");
    const postDetailPage = new PostDetailPage(page);

    await boardListPage.goto();

    // 게시글 상세 페이지로 이동
    await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/boards/notices/posts") &&
        response.status() === 200,
    );
    await boardListPage.clickPost(0);

    // 상세 페이지 로드 대기
    await page.waitForResponse(
      (response) =>
        response.url().match(/\/api\/v1\/boards\/notices\/posts\/\d+$/) &&
        response.status() === 200,
    );

    // 현재 좋아요 수 확인
    const initialLikeCount = await postDetailPage.getLikeCount();

    // 좋아요 버튼 클릭
    const likePromise = page.waitForResponse(
      (response) =>
        response.url().match(/\/api\/v1\/posts\/\d+\/likes$/) &&
        response.status() === 200,
    );

    await postDetailPage.toggleLike();
    await likePromise;

    // 좋아요 수 변경 확인
    const updatedLikeCount = await postDetailPage.getLikeCount();
    expect(updatedLikeCount).not.toBe(initialLikeCount);
  });
});
```

#### 3. 게시글 작성 테스트

**파일**: `e2e/posts/post-create.spec.ts`

```typescript
import { test, expect } from "@playwright/test";
import { LoginPage } from "../pages/LoginPage";
import { BoardListPage } from "../pages/BoardListPage";
import { PostWritePage } from "../pages/PostWritePage";
import { PostDetailPage } from "../pages/PostDetailPage";
import { EXISTING_USER, generateUniquePost } from "../utils/testData";
import { waitForApiResponse } from "../utils/apiHelpers";

test.describe("게시글 작성", () => {
  test.beforeEach(async ({ page }) => {
    // 로그인 필수
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login(EXISTING_USER.studentId, EXISTING_USER.password);
    await waitForApiResponse(page, "/api/v1/auth/password/login", 200);
  });

  test("새 게시글 작성", async ({ page }) => {
    const boardListPage = new BoardListPage(page, "notices");
    const postWritePage = new PostWritePage(page);
    const postDetailPage = new PostDetailPage(page);
    const postData = generateUniquePost();

    // 게시판으로 이동
    await boardListPage.goto();

    // 글쓰기 버튼 클릭
    await boardListPage.clickWriteButton();

    // 글쓰기 페이지로 이동 확인
    await expect(page).toHaveURL(/\/board\/notices\/write/);

    // 폼 작성
    await postWritePage.fillPost(postData);

    // 제출
    const createPromise = waitForApiResponse(
      page,
      "/api/v1/boards/notices/posts",
      201,
    );
    await postWritePage.submit();

    // API 응답 확인
    const response = await createPromise;
    const responseBody = await response.json();
    expect(responseBody).toHaveProperty("id");

    // 상세 페이지로 리다이렉트 확인
    await expect(page).toHaveURL(/\/board\/notices\/\d+/);

    // 작성한 게시글 내용 확인
    const displayedTitle = await postDetailPage.postTitle.textContent();
    expect(displayedTitle).toContain(postData.title);
  });

  test("로그인하지 않고 글쓰기 시도", async ({ page }) => {
    // 로그아웃 (beforeEach에서 로그인한 상태를 해제)
    await page.evaluate(() => {
      localStorage.clear();
    });

    const boardListPage = new BoardListPage(page, "notices");
    await boardListPage.goto();

    // 글쓰기 버튼 클릭
    await boardListPage.clickWriteButton();

    // 로그인 페이지로 리다이렉트
    await expect(page).toHaveURL(/\/login/);
  });
});
```

#### 4. 게시글 수정/삭제 테스트

**파일**: `e2e/posts/post-edit-delete.spec.ts`

```typescript
import { test, expect } from "@playwright/test";
import { LoginPage } from "../pages/LoginPage";
import { PostDetailPage } from "../pages/PostDetailPage";
import { PostWritePage } from "../pages/PostWritePage";
import { EXISTING_USER } from "../utils/testData";
import { waitForApiResponse } from "../utils/apiHelpers";

test.describe("게시글 수정/삭제", () => {
  test.beforeEach(async ({ page }) => {
    // 로그인 필수
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login(EXISTING_USER.studentId, EXISTING_USER.password);
    await waitForApiResponse(page, "/api/v1/auth/password/login", 200);
  });

  test("내가 작성한 게시글 수정", async ({ page }) => {
    const postDetailPage = new PostDetailPage(page);
    const postWritePage = new PostWritePage(page);

    // 내가 작성한 게시글로 이동 (postId는 실제 테스트 데이터에 맞게 조정)
    await postDetailPage.goto("notices", 123);

    // 수정 버튼 표시 확인 (내가 작성한 게시글만 표시됨)
    await expect(postDetailPage.editButton).toBeVisible();

    // 수정 버튼 클릭
    await postDetailPage.clickEdit();

    // 수정 페이지로 이동 확인
    await expect(page).toHaveURL(/\/board\/notices\/123\/edit/);

    // 수정 폼 작성
    await postWritePage.titleInput.fill("수정된 제목");

    // 제출
    const updatePromise = waitForApiResponse(
      page,
      "/api/v1/boards/notices/posts/123",
      200,
    );
    await postWritePage.submit();
    await updatePromise;

    // 상세 페이지로 리다이렉트
    await expect(page).toHaveURL(/\/board\/notices\/123/);

    // 수정된 내용 확인
    const updatedTitle = await postDetailPage.postTitle.textContent();
    expect(updatedTitle).toContain("수정된 제목");
  });

  test("내가 작성한 게시글 삭제", async ({ page }) => {
    const postDetailPage = new PostDetailPage(page);

    // 내가 작성한 게시글로 이동
    await postDetailPage.goto("notices", 123);

    // 삭제 버튼 표시 확인
    await expect(postDetailPage.deleteButton).toBeVisible();

    // 삭제 확인 대화상자 처리
    page.on("dialog", async (dialog) => {
      expect(dialog.message()).toContain("삭제하시겠습니까");
      await dialog.accept();
    });

    // 삭제 버튼 클릭
    const deletePromise = waitForApiResponse(
      page,
      "/api/v1/boards/notices/posts/123",
      200,
    );
    await postDetailPage.clickDelete();
    await deletePromise;

    // 목록 페이지로 리다이렉트
    await expect(page).toHaveURL(/\/board\/notices$/);
  });
});
```

---

## 수동 브라우저 테스트

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

### Test 4: 게시판 (Posts)

#### 4.1 게시글 목록 조회

**시나리오**: 공지사항 게시판 접속

1. 브라우저에서 `/board/notices` 접속
2. **예상 결과**:
   - 게시글 목록 표시
   - 제목, 작성자, 작성일, 조회수 등 표시
3. **Network 탭 확인**:
   - URL: `GET .../api/v1/boards/notices/posts?page=0&size=20`
   - Status: 200 OK
   - Response Body:
     ```json
     {
       "content": [
         {
           "id": 1,
           "title": "공지사항 제목",
           "author": "작성자",
           "createdAt": "2024-01-01T00:00:00",
           "viewCount": 100,
           "likeCount": 10
         }
       ],
       "totalElements": 50,
       "totalPages": 3,
       "number": 0
     }
     ```

#### 4.2 페이지네이션

**시나리오**: 페이지 이동

1. 2페이지 클릭
2. **예상 결과**:
   - URL에 `page=1` 포함 (0-based)
   - 다른 게시글 목록 표시
3. **Network 탭 확인**:
   - URL: `GET .../api/v1/boards/notices/posts?page=1&size=20`
   - Status: 200 OK

#### 4.3 게시글 상세 조회

**시나리오**: 게시글 클릭

1. 게시글 클릭
2. **예상 결과**:
   - 제목, 내용, 작성자, 날짜 표시
   - 조회수 증가
   - 좋아요 버튼 표시
3. **Network 탭 확인**:
   - URL: `GET .../api/v1/boards/notices/posts/{postId}`
   - Status: 200 OK
   - Response Body:
     ```json
     {
       "id": 1,
       "title": "게시글 제목",
       "content": "게시글 내용",
       "author": {
         "id": 123,
         "name": "작성자",
         "studentId": "12345678"
       },
       "createdAt": "2024-01-01T00:00:00",
       "viewCount": 101,
       "likeCount": 10,
       "isLiked": false
     }
     ```

#### 4.3.1 게시글 상세 페이지 - 더 메뉴 팝업 테스트

**시나리오**: 더 메뉴 팝업의 디자인 시스템 토큰 적용 확인

1. `/board/general/:postId` 접속
2. 상단 우측 더 메뉴 버튼 (점 3개 아이콘) 클릭
3. **예상 결과**:
   - 드롭다운 메뉴 표시
   - **라이트 모드**: 밝은 배경 (`#FFFFFF`)
   - **다크 모드**: 어두운 배경 (`#343A40`, gray-7)
   - "신고하기", "작성자 차단" 메뉴 항목 표시
4. 다크 모드 토글 버튼 클릭
5. **예상 결과**:
   - 배경색이 자연스럽게 전환됨
   - 하드코딩된 색상 없음
   - 디자인 시스템의 `popover` 토큰 색상 적용됨
6. **Console 탭 확인**:
   - 에러 없음
7. **개발자 도구 Elements 탭 확인**:
   - 드롭다운 요소에 `bg-popover` 클래스 적용 확인
   - `bg-[#252525]` 같은 하드코딩된 클래스 없어야 함

#### 4.4 좋아요 토글

**시나리오**: 좋아요 버튼 클릭

1. 게시글 상세 페이지에서 좋아요 버튼 클릭
2. **예상 결과**:
   - 좋아요 수 증가/감소
   - 버튼 색상 변경 (빨간색 ↔ 회색)
   - `isLiked` 상태 변경
3. **Network 탭 확인**:
   - URL: `POST .../api/v1/posts/{postId}/likes`
   - Request Headers: `Authorization: Bearer <token>` (로그인 필요)
   - Status: 200 OK
   - Response Body:
     ```json
     {
       "isLiked": true,
       "likeCount": 11
     }
     ```
4. **Console 탭 확인**:
   - React Query 캐시 업데이트 확인

#### 4.5 게시글 작성

**시나리오**: 새 게시글 작성 (로그인 필수)

1. "글쓰기" 버튼 클릭
2. **예상 결과**:
   - 글쓰기 페이지로 이동 (`/board/notices/write`)
   - 로그인하지 않았으면 로그인 페이지로 리다이렉트
3. 제목, 내용 입력 후 "작성" 버튼 클릭
4. **예상 결과**:
   - 작성 성공 메시지
   - 상세 페이지로 리다이렉트
5. **Network 탭 확인**:
   - URL: `POST .../api/v1/boards/notices/posts`
   - Request Headers: `Authorization: Bearer <token>`
   - Request Body:
     ```json
     {
       "title": "새 게시글 제목",
       "content": "새 게시글 내용"
     }
     ```
   - Status: 201 Created
   - Response Body:
     ```json
     {
       "id": 123,
       "title": "새 게시글 제목",
       "content": "새 게시글 내용",
       "author": {...},
       "createdAt": "2024-01-01T00:00:00"
     }
     ```

#### 4.6 게시글 수정

**시나리오**: 내가 작성한 게시글 수정

1. 내가 작성한 게시글 상세 페이지에서 "수정" 버튼 클릭
2. **예상 결과**:
   - 수정 페이지로 이동
   - 기존 내용이 폼에 자동 입력됨
3. 내용 수정 후 "수정" 버튼 클릭
4. **예상 결과**:
   - 수정 성공 메시지
   - 상세 페이지로 리다이렉트
5. **Network 탭 확인**:
   - URL: `PUT .../api/v1/boards/notices/posts/{postId}`
   - Status: 200 OK

#### 4.7 게시글 삭제

**시나리오**: 내가 작성한 게시글 삭제

1. 내가 작성한 게시글 상세 페이지에서 "삭제" 버튼 클릭
2. **예상 결과**:
   - 삭제 확인 대화상자 표시
3. "확인" 클릭
4. **예상 결과**:
   - 삭제 성공 메시지
   - 목록 페이지로 리다이렉트
5. **Network 탭 확인**:
   - URL: `DELETE .../api/v1/boards/notices/posts/{postId}`
   - Status: 200 OK 또는 204 No Content

#### 4.8 검색

**시나리오**: 게시글 검색

1. 검색창에 키워드 입력 (예: "테스트")
2. 검색 버튼 클릭
3. **예상 결과**:
   - URL에 `search=테스트` 포함
   - 검색 결과 게시글 목록 표시
4. **Network 탭 확인**:
   - URL: `GET .../api/v1/boards/notices/posts?search=테스트&page=0&size=20`
   - Status: 200 OK

#### 4.9 정렬

**시나리오**: 정렬 옵션 변경

1. 정렬 선택 박스에서 "좋아요순" 선택
2. **예상 결과**:
   - URL에 `sort=likes` 포함
   - 좋아요 수가 많은 게시글부터 표시
3. **Network 탭 확인**:
   - URL: `GET .../api/v1/boards/notices/posts?sort=likes,desc&page=0&size=20`
   - Status: 200 OK

---

## 테스트 실행 방법

### 모든 게시판 테스트 실행

```bash
npx playwright test e2e/posts
```

### 특정 테스트 파일 실행

```bash
npx playwright test e2e/posts/post-create.spec.ts
```

### UI 모드로 실행 (디버깅)

```bash
npx playwright test e2e/posts --ui
```

### 헤드풀 모드 (브라우저 표시)

```bash
npx playwright test e2e/posts --headed
```

---

## 주의사항

### 1. 게시글 작성/수정/삭제 권한

게시글 작성, 수정, 삭제는 로그인이 필요하며, 수정/삭제는 본인이 작성한 게시글만 가능:

```typescript
// 수정/삭제 버튼은 본인 게시글에만 표시
test("본인이 작성하지 않은 게시글", async ({ page }) => {
  const postDetailPage = new PostDetailPage(page);
  await postDetailPage.goto("notices", 456); // 다른 사람이 작성한 게시글

  // 수정/삭제 버튼이 표시되지 않아야 함
  await expect(postDetailPage.editButton).not.toBeVisible();
  await expect(postDetailPage.deleteButton).not.toBeVisible();
});
```

### 2. 좋아요 인증

좋아요 기능은 로그인한 사용자만 사용 가능:

```typescript
// 비로그인 상태에서 좋아요 클릭 시
await postDetailPage.toggleLike();

// 401 Unauthorized 또는 로그인 페이지로 리다이렉트
await page.waitForResponse(
  (response) =>
    response.url().includes("/api/v1/posts/") && response.status() === 401,
);
```

### 3. 페이지네이션 0-based

API의 페이지 번호는 0부터 시작하지만, UI에서는 1부터 표시:

- UI: 1페이지 → API: `page=0`
- UI: 2페이지 → API: `page=1`

### 4. 조회수 증가

게시글 상세 조회 시 조회수가 자동으로 증가하므로, 테스트 시 주의:

```typescript
// 같은 게시글을 여러 번 조회하면 조회수가 계속 증가
// 테스트 환경에서는 조회수 체크를 엄격하게 하지 않거나,
// 특정 조건에서만 조회수 증가하도록 백엔드 구현 필요
```

### 5. 테스트 데이터 정리

테스트로 작성한 게시글이 계속 누적되지 않도록 정리:

```typescript
test.afterAll(async ({ request }) => {
  // 테스트 게시글 삭제 (백엔드에 삭제 API 필요)
  await request.delete("/api/v1/test/posts/cleanup");
});
```

---

## 참고 자료

- [Playwright 공식 문서](https://playwright.dev/)
- [Page Object Model 가이드](https://playwright.dev/docs/pom)
- [게시판 API 명세](../../specs/board-api-spec.md)
