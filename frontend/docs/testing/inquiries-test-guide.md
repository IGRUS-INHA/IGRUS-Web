# Inquiries (문의) 테스트 가이드 (Playwright)

## 개요

이 문서는 문의 시스템의 E2E 테스트를 Playwright로 작성할 때 참고할 수 있는 가이드입니다.

**테스트 대상 기능**:
- 문의 목록 조회 (내 문의 내역)
- 문의 작성
- 인증 상태에 따른 접근 제어 (로그인 필수)

**주요 플로우**:
로그인 → 문의 페이지 접근 → 문의 작성 → 문의 목록 확인

---

## Playwright 자동화 테스트

### Page Objects

#### InquiriesPage

**파일**: `e2e/pages/InquiriesPage.ts`

```typescript
import { Page, Locator } from '@playwright/test';

export class InquiriesPage {
  readonly page: Page;

  // Tabs
  readonly myInquiriesTab: Locator;
  readonly newInquiryTab: Locator;

  // Inquiry List
  readonly inquiryListItems: Locator;
  readonly emptyStateMessage: Locator;

  // Inquiry Form
  readonly typeSelect: Locator;
  readonly titleInput: Locator;
  readonly contentTextarea: Locator;
  readonly submitButton: Locator;

  constructor(page: Page) {
    this.page = page;

    // Tabs
    this.myInquiriesTab = page.getByRole('tab', { name: /문의 내역 보기|내 문의/ });
    this.newInquiryTab = page.getByRole('tab', { name: /새 문의 작성|문의하기/ });

    // Inquiry List
    this.inquiryListItems = page.locator('[data-testid="inquiry-item"]');
    this.emptyStateMessage = page.getByText(/문의 내역이 없습니다/);

    // Inquiry Form
    this.typeSelect = page.locator('select[name="type"]');
    this.titleInput = page.getByPlaceholder(/제목/);
    this.contentTextarea = page.getByPlaceholder(/문의 내용/);
    this.submitButton = page.getByRole('button', { name: /문의 제출|제출/ });
  }

  async goto() {
    await this.page.goto('/inquiries');
  }

  async switchToMyInquiriesTab() {
    await this.myInquiriesTab.click();
  }

  async switchToNewInquiryTab() {
    await this.newInquiryTab.click();
  }

  async fillInquiryForm(data: {
    type: string;
    title: string;
    content: string;
  }) {
    await this.typeSelect.selectOption(data.type);
    await this.titleInput.fill(data.title);
    await this.contentTextarea.fill(data.content);
  }

  async submitInquiry() {
    await this.submitButton.click();
  }

  async getInquiryCount() {
    return await this.inquiryListItems.count();
  }

  async getFirstInquiryTitle() {
    const firstItem = this.inquiryListItems.first();
    return await firstItem.locator('[data-testid="inquiry-title"]').textContent();
  }
}
```

### 테스트 유틸리티

#### 테스트 데이터 생성

**파일**: `e2e/utils/testData.ts` (추가)

```typescript
/**
 * 고유한 테스트 문의 데이터 생성
 */
export function generateUniqueInquiry() {
  const timestamp = Date.now();

  return {
    type: 'JOIN', // 가입/입부 문의
    title: `테스트 문의 ${timestamp}`,
    content: `테스트 문의 내용입니다. (${timestamp})`,
  };
}

/**
 * 문의 유형 매핑 (UI → API)
 */
export const INQUIRY_TYPE_MAP = {
  'signup': 'JOIN',
  'activity': 'ACTIVITY',
  'study': 'STUDY',
  'project': 'PROJECT',
  'general': 'GENERAL',
} as const;
```

### 테스트 케이스

#### 1. 문의 목록 조회 테스트

**파일**: `e2e/inquiries/inquiry-list.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { InquiriesPage } from '../pages/InquiriesPage';
import { LoginPage } from '../pages/LoginPage';
import { EXISTING_USER } from '../utils/testData';
import { waitForApiResponse } from '../utils/apiHelpers';

test.describe('문의 목록 조회', () => {
  test.beforeEach(async ({ page }) => {
    // 로그인 필수
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login(EXISTING_USER.studentId, EXISTING_USER.password);
    await waitForApiResponse(page, '/api/v1/auth/password/login', 200);
  });

  test('내 문의 목록 조회', async ({ page }) => {
    const inquiriesPage = new InquiriesPage(page);
    await inquiriesPage.goto();

    // API 응답 대기
    const response = await page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/inquiries/my') &&
        response.status() === 200
    );

    // 응답 확인
    const responseBody = await response.json();
    expect(responseBody).toHaveProperty('content'); // Page 객체의 content 배열
    expect(responseBody).toHaveProperty('totalElements');

    // Authorization 헤더 확인
    const request = response.request();
    const headers = request.headers();
    expect(headers['authorization']).toBeTruthy();
    expect(headers['authorization']).toContain('Bearer ');
  });

  test('빈 문의 목록 표시', async ({ page }) => {
    const inquiriesPage = new InquiriesPage(page);
    await inquiriesPage.goto();

    await page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/inquiries/my') &&
        response.status() === 200
    );

    // 문의가 없을 경우 빈 상태 메시지 표시
    const count = await inquiriesPage.getInquiryCount();
    if (count === 0) {
      await expect(inquiriesPage.emptyStateMessage).toBeVisible();
    }
  });
});
```

#### 2. 문의 작성 테스트

**파일**: `e2e/inquiries/inquiry-create.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { InquiriesPage } from '../pages/InquiriesPage';
import { LoginPage } from '../pages/LoginPage';
import { EXISTING_USER, generateUniqueInquiry } from '../utils/testData';
import { waitForApiResponse } from '../utils/apiHelpers';

test.describe('문의 작성', () => {
  test.beforeEach(async ({ page }) => {
    // 로그인 필수
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login(EXISTING_USER.studentId, EXISTING_USER.password);
    await waitForApiResponse(page, '/api/v1/auth/password/login', 200);
  });

  test('새 문의 작성', async ({ page }) => {
    const inquiriesPage = new InquiriesPage(page);
    const inquiryData = generateUniqueInquiry();

    await inquiriesPage.goto();

    // "새 문의 작성" 탭으로 전환
    await inquiriesPage.switchToNewInquiryTab();

    // 폼 작성
    await inquiriesPage.fillInquiryForm(inquiryData);

    // 제출 시 API 요청 대기
    const createPromise = waitForApiResponse(page, '/api/v1/inquiries/member', 201);

    await inquiriesPage.submitInquiry();

    // API 응답 확인
    const response = await createPromise;
    const responseBody = await response.json();
    expect(responseBody).toHaveProperty('id');

    // 성공 메시지 확인
    page.on('dialog', async (dialog) => {
      expect(dialog.message()).toContain('문의가 작성되었습니다');
      await dialog.accept();
    });

    // "내 문의" 탭으로 자동 전환 확인
    await expect(inquiriesPage.myInquiriesTab).toHaveAttribute('aria-selected', 'true');

    // 목록 갱신 확인 (React Query invalidate)
    await page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/inquiries/my') &&
        response.status() === 200
    );

    // 새로 작성한 문의가 목록에 있는지 확인
    const firstInquiryTitle = await inquiriesPage.getFirstInquiryTitle();
    expect(firstInquiryTitle).toContain(inquiryData.title);
  });

  test('문의 유형 선택 확인', async ({ page }) => {
    const inquiriesPage = new InquiriesPage(page);
    await inquiriesPage.goto();
    await inquiriesPage.switchToNewInquiryTab();

    // 문의 유형 선택
    await inquiriesPage.typeSelect.selectOption('JOIN');

    // 선택된 값 확인
    const selectedValue = await inquiriesPage.typeSelect.inputValue();
    expect(selectedValue).toBe('JOIN');
  });
});
```

#### 3. 인증 테스트

**파일**: `e2e/inquiries/inquiry-auth.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { InquiriesPage } from '../pages/InquiriesPage';

test.describe('문의 페이지 접근 제어', () => {
  test('로그인하지 않은 상태에서 접근 시도', async ({ page }) => {
    const inquiriesPage = new InquiriesPage(page);

    // 로그인 없이 접근
    await inquiriesPage.goto();

    // 401 Unauthorized 또는 403 Forbidden 응답 대기
    await page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/inquiries/my') &&
        (response.status() === 401 || response.status() === 403)
    );

    // 로그인 페이지로 리다이렉트되는지 확인
    await expect(page).toHaveURL(/\/login/);
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

3. **로그인 먼저 수행**
   - 문의 기능은 로그인 필수이므로 먼저 로그인 필요

---

### Test 3: 문의 시스템

#### 3.1 내 문의 목록 조회 (로그인 필수)
**시나리오**: 로그인 후 문의 페이지 접속

1. 로그인 상태에서 `/inquiries` 접속
2. **예상 결과**:
   - 내 문의 목록 표시 (비어있을 수 있음)
   - 로딩 상태 → 데이터 표시
3. **Network 탭 확인**:
   - 요청 URL: `GET .../api/v1/inquiries/my?page=0&size=10`
   - Request Headers에 `Authorization: Bearer <token>` 포함
   - Status: 200 OK
   - Response Body:
     ```json
     {
       "content": [...],
       "pageable": {...},
       "totalElements": 0,
       "totalPages": 0
     }
     ```

#### 3.2 문의 작성
**시나리오**: 새 문의 작성

1. "새 문의 작성" 탭 클릭
2. 폼 입력:
   - 문의 유형: `가입/입부 문의` 선택
   - 제목: `테스트 문의`
   - 내용: `문의 내용 테스트입니다`
3. "문의 제출" 버튼 클릭
4. **예상 결과**:
   - 문의 작성 성공 메시지
   - "문의 내역 보기" 탭으로 자동 전환
   - 목록에 새 문의 추가됨
5. **Network 탭 확인**:
   - 요청 URL: `POST .../api/v1/inquiries/member`
   - Request Headers: `Authorization: Bearer <token>`
   - Request Body:
     ```json
     {
       "title": "테스트 문의",
       "content": "문의 내용 테스트입니다",
       "type": "JOIN"
     }
     ```
   - **중요**: UI의 `signup` 또는 다른 값이 `JOIN`으로 변환되었는지 확인
   - Status: 201 Created
   - Response Body:
     ```json
     {
       "id": 123,
       "title": "테스트 문의",
       "content": "문의 내용 테스트입니다",
       "type": "JOIN",
       "status": "PENDING",
       "createdAt": "2024-01-01T00:00:00"
     }
     ```
6. **Console 탭 확인**:
   - React Query 캐시 invalidate 확인 (목록 자동 갱신)
   - `queryClient.invalidateQueries(['inquiries'])` 로그 확인

#### 3.3 문의 목록 새로고침
**시나리오**: 문의 작성 후 자동 새로고침

1. 문의 작성 완료 후
2. **예상 결과**:
   - 자동으로 문의 목록 갱신 (React Query invalidation)
   - 새로 작성한 문의가 목록 최상단에 표시
3. **Network 탭 확인**:
   - 요청 URL: `GET .../api/v1/inquiries/my?page=0&size=10`
   - Status: 200 OK
   - 문의 작성 직후 자동으로 목록 조회 API 호출됨

#### 3.4 로그인하지 않은 상태에서 문의 페이지 접근
**시나리오**: 비로그인 상태에서 접근

1. 로그아웃 또는 시크릿 모드에서 `/inquiries` 접속
2. **예상 결과**:
   - 로그인 페이지로 리다이렉트
   - 또는 401 Unauthorized 에러
3. **Network 탭 확인**:
   - 요청 URL: `GET .../api/v1/inquiries/my?page=0&size=10`
   - Request Headers: `Authorization` 헤더 없음
   - Status: 401 Unauthorized
4. **Console 탭 확인**:
   - `ProtectedRoute` 또는 axios interceptor에서 리다이렉트 처리 로그

#### 3.5 페이지네이션
**시나리오**: 문의가 많을 경우 페이지 이동

1. 문의 목록에서 페이지네이션 버튼 클릭 (2페이지)
2. **예상 결과**:
   - 다음 페이지 문의 목록 표시
   - URL에 `page=1` 쿼리 파라미터 추가
3. **Network 탭 확인**:
   - 요청 URL: `GET .../api/v1/inquiries/my?page=1&size=10`
   - Status: 200 OK

#### 3.6 문의 상세 조회 (있는 경우)
**시나리오**: 문의 항목 클릭

1. 문의 목록에서 문의 항목 클릭
2. **예상 결과**:
   - 문의 상세 페이지로 이동
   - 제목, 내용, 작성일, 답변 상태 표시
3. **Network 탭 확인**:
   - 요청 URL: `GET .../api/v1/inquiries/{inquiryId}`
   - Status: 200 OK

---

## 테스트 실행 방법

### 모든 문의 테스트 실행

```bash
npx playwright test e2e/inquiries
```

### 특정 테스트 파일 실행

```bash
npx playwright test e2e/inquiries/inquiry-create.spec.ts
```

### UI 모드로 실행 (디버깅)

```bash
npx playwright test e2e/inquiries --ui
```

### 헤드풀 모드 (브라우저 표시)

```bash
npx playwright test e2e/inquiries --headed
```

---

## 주의사항

### 1. 인증 토큰 관리

문의 API는 모두 인증이 필요하므로 테스트 시 주의:

```typescript
test.beforeEach(async ({ page }) => {
  // 매 테스트 전에 로그인 수행
  const loginPage = new LoginPage(page);
  await loginPage.goto();
  await loginPage.login(EXISTING_USER.studentId, EXISTING_USER.password);
  await waitForApiResponse(page, '/api/v1/auth/password/login', 200);
});
```

### 2. React Query 캐시 무효화

문의 작성 후 목록이 자동 갱신되는지 확인:

```typescript
// 문의 작성 후
await inquiriesPage.submitInquiry();

// 목록 API가 다시 호출되는지 확인
await page.waitForResponse(
  (response) =>
    response.url().includes('/api/v1/inquiries/my') &&
    response.status() === 200
);
```

### 3. 문의 유형 매핑

UI에서 선택한 문의 유형이 API로 전송될 때 올바르게 변환되는지 확인:

- UI: "가입/입부 문의" → API: `"JOIN"`
- UI: "활동 문의" → API: `"ACTIVITY"`
- UI: "스터디 문의" → API: `"STUDY"`

Request Body를 확인하여 올바른 enum 값이 전송되는지 검증.

### 4. 테스트 데이터 정리

테스트로 작성한 문의가 계속 누적되지 않도록 정리:

```typescript
test.afterAll(async ({ request }) => {
  // 테스트 문의 삭제 (백엔드에 삭제 API 필요)
  await request.delete('/api/v1/test/inquiries/cleanup');
});
```

---

## 참고 자료

- [Playwright 공식 문서](https://playwright.dev/)
- [Page Object Model 가이드](https://playwright.dev/docs/pom)
- [Auth & Inquiries 마이그레이션 문서](../migration/auth-inquiries-orval-migration.md)
