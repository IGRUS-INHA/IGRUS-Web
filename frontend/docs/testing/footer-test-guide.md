# Footer 테스트 가이드 (Playwright)

## 개요

이 문서는 Footer 컴포넌트의 E2E 테스트를 Playwright로 작성할 때 참고할 수 있는 가이드입니다.

**테스트 대상 기능**:
- 조건부 푸터 표시 (특정 페이지에만 표시)
- 내부 링크 네비게이션 (게시판, 행사, 문의)
- 법적 페이지 링크 (개인정보처리방침, 이용약관)
- SNS 외부 링크 (인스타그램, 페이스북)
- 반응형 레이아웃 (모바일/데스크톱)
- 다크모드 지원

**주요 플로우**:
페이지 이동 → 푸터 표시 확인 → 링크 클릭 → 테마 전환 → 반응형 확인

---

## Playwright 자동화 테스트

### Component Object

#### FooterComponent

**파일**: `e2e/components/FooterComponent.ts`

```typescript
import { Page, Locator } from '@playwright/test';

export class FooterComponent {
  readonly page: Page;

  // Footer container
  readonly footer: Locator;

  // Sections
  readonly clubInfoSection: Locator;
  readonly quickLinksSection: Locator;
  readonly contactSection: Locator;
  readonly legalSection: Locator;

  // Club info
  readonly clubName: Locator;
  readonly clubFullName: Locator;
  readonly addressText: Locator;

  // Quick links
  readonly boardLink: Locator;
  readonly eventsLink: Locator;
  readonly inquiryLink: Locator;

  // Contact info
  readonly contactEmail: Locator;
  readonly contactPhone: Locator;

  // SNS links
  readonly instagramLink: Locator;
  readonly facebookLink: Locator;

  // Legal links
  readonly privacyPolicyLink: Locator;
  readonly termsOfServiceLink: Locator;

  // Copyright
  readonly copyrightText: Locator;

  constructor(page: Page) {
    this.page = page;

    // Footer container
    this.footer = page.locator('footer');

    // Sections (adjust selectors based on actual implementation)
    this.clubInfoSection = this.footer.locator('[data-testid="club-info"]');
    this.quickLinksSection = this.footer.locator('[data-testid="quick-links"]');
    this.contactSection = this.footer.locator('[data-testid="contact-info"]');
    this.legalSection = this.footer.locator('[data-testid="legal-links"]');

    // Club info
    this.clubName = this.footer.getByText('IGRUS');
    this.clubFullName = this.footer.getByText('인하대학교 웹 개발 동아리');
    this.addressText = this.footer.locator('text=/인천광역시|인하대학교/');

    // Quick links
    this.boardLink = this.footer.getByRole('link', { name: '게시판' });
    this.eventsLink = this.footer.getByRole('link', { name: '행사' });
    this.inquiryLink = this.footer.getByRole('link', { name: '문의' });

    // Contact info
    this.contactEmail = this.footer.locator('a[href^="mailto:"]');
    this.contactPhone = this.footer.locator('a[href^="tel:"]');

    // SNS links
    this.instagramLink = this.footer.locator('a[href*="instagram"]');
    this.facebookLink = this.footer.locator('a[href*="facebook"]');

    // Legal links
    this.privacyPolicyLink = this.footer.getByRole('link', { name: '개인정보처리방침' });
    this.termsOfServiceLink = this.footer.getByRole('link', { name: '이용약관' });

    // Copyright
    this.copyrightText = this.footer.getByText(/© \d{4} IGRUS/);
  }

  async isVisible(): Promise<boolean> {
    return await this.footer.isVisible();
  }

  async isHidden(): Promise<boolean> {
    const count = await this.footer.count();
    return count === 0;
  }

  async clickQuickLink(linkName: 'board' | 'events' | 'inquiry') {
    switch (linkName) {
      case 'board':
        await this.boardLink.click();
        break;
      case 'events':
        await this.eventsLink.click();
        break;
      case 'inquiry':
        await this.inquiryLink.click();
        break;
    }
  }

  async clickLegalLink(linkName: 'privacy' | 'terms') {
    switch (linkName) {
      case 'privacy':
        await this.privacyPolicyLink.click();
        break;
      case 'terms':
        await this.termsOfServiceLink.click();
        break;
    }
  }

  async clickSNSLink(platform: 'instagram' | 'facebook') {
    switch (platform) {
      case 'instagram':
        await this.instagramLink.click();
        break;
      case 'facebook':
        await this.facebookLink.click();
        break;
    }
  }

  async verifyDarkMode(isDark: boolean) {
    const bgColor = await this.footer.evaluate((el) => {
      return window.getComputedStyle(el).backgroundColor;
    });

    // Dark mode background should be dark (adjust expected values as needed)
    if (isDark) {
      // Example: rgb(26, 26, 26) for dark mode
      return bgColor.includes('26, 26, 26') || bgColor.includes('0, 0, 0');
    } else {
      // Light mode background should be light
      return bgColor.includes('249, 250, 251') || bgColor.includes('255, 255, 255');
    }
  }
}
```

---

## 테스트 케이스

### 1. 조건부 푸터 표시 테스트

**파일**: `e2e/tests/footer-display.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { FooterComponent } from '../components/FooterComponent';

test.describe('Footer conditional display', () => {
  test('should display footer on home page', async ({ page }) => {
    await page.goto('/');
    const footer = new FooterComponent(page);

    expect(await footer.isVisible()).toBe(true);
  });

  test('should display footer on board pages', async ({ page }) => {
    await page.goto('/board/notices');
    const footer = new FooterComponent(page);

    expect(await footer.isVisible()).toBe(true);
  });

  test('should display footer on events page', async ({ page }) => {
    await page.goto('/events');
    const footer = new FooterComponent(page);

    expect(await footer.isVisible()).toBe(true);
  });

  test('should display footer on inquiry page', async ({ page }) => {
    await page.goto('/inquiry');
    const footer = new FooterComponent(page);

    expect(await footer.isVisible()).toBe(true);
  });

  test('should NOT display footer on login page', async ({ page }) => {
    await page.goto('/login');
    const footer = new FooterComponent(page);

    expect(await footer.isHidden()).toBe(true);
  });

  test('should NOT display footer on signup page', async ({ page }) => {
    await page.goto('/signup');
    const footer = new FooterComponent(page);

    expect(await footer.isHidden()).toBe(true);
  });

  test('should NOT display footer on admin pages', async ({ page }) => {
    // Note: This test requires authentication
    // Add login logic here if needed

    await page.goto('/admin');
    const footer = new FooterComponent(page);

    // Footer should be hidden on admin pages
    expect(await footer.isHidden()).toBe(true);
  });
});
```

---

### 2. 내부 링크 네비게이션 테스트

**파일**: `e2e/tests/footer-navigation.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { FooterComponent } from '../components/FooterComponent';

test.describe('Footer internal navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should navigate to board page when clicking board link', async ({ page }) => {
    const footer = new FooterComponent(page);

    await footer.clickQuickLink('board');
    await page.waitForURL(/\/board\/notices/);

    expect(page.url()).toContain('/board/notices');
  });

  test('should navigate to events page when clicking events link', async ({ page }) => {
    const footer = new FooterComponent(page);

    await footer.clickQuickLink('events');
    await page.waitForURL(/\/events/);

    expect(page.url()).toContain('/events');
  });

  test('should navigate to inquiry page when clicking inquiry link', async ({ page }) => {
    const footer = new FooterComponent(page);

    await footer.clickQuickLink('inquiry');
    await page.waitForURL(/\/inquiry/);

    expect(page.url()).toContain('/inquiry');
  });

  test('should navigate to privacy policy page', async ({ page }) => {
    const footer = new FooterComponent(page);

    await footer.clickLegalLink('privacy');
    await page.waitForURL(/\/privacy/);

    expect(page.url()).toContain('/privacy');
    expect(await page.getByText('개인정보처리방침').first()).toBeVisible();
  });

  test('should navigate to terms of service page', async ({ page }) => {
    const footer = new FooterComponent(page);

    await footer.clickLegalLink('terms');
    await page.waitForURL(/\/terms/);

    expect(page.url()).toContain('/terms');
    expect(await page.getByText('이용약관').first()).toBeVisible();
  });
});
```

---

### 3. SNS 외부 링크 테스트

**파일**: `e2e/tests/footer-sns-links.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { FooterComponent } from '../components/FooterComponent';

test.describe('Footer SNS external links', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('Instagram link should open in new tab', async ({ page, context }) => {
    const footer = new FooterComponent(page);

    // Listen for new page (new tab)
    const pagePromise = context.waitForEvent('page');

    await footer.instagramLink.click();

    const newPage = await pagePromise;
    await newPage.waitForLoadState();

    expect(newPage.url()).toContain('instagram.com');
  });

  test('Facebook link should open in new tab', async ({ page, context }) => {
    const footer = new FooterComponent(page);

    // Listen for new page (new tab)
    const pagePromise = context.waitForEvent('page');

    await footer.facebookLink.click();

    const newPage = await pagePromise;
    await newPage.waitForLoadState();

    expect(newPage.url()).toContain('facebook.com');
  });

  test('SNS links should have proper attributes', async ({ page }) => {
    const footer = new FooterComponent(page);

    // Check Instagram link attributes
    const instagramTarget = await footer.instagramLink.getAttribute('target');
    const instagramRel = await footer.instagramLink.getAttribute('rel');

    expect(instagramTarget).toBe('_blank');
    expect(instagramRel).toBe('noopener noreferrer');

    // Check Facebook link attributes
    const facebookTarget = await footer.facebookLink.getAttribute('target');
    const facebookRel = await footer.facebookLink.getAttribute('rel');

    expect(facebookTarget).toBe('_blank');
    expect(facebookRel).toBe('noopener noreferrer');
  });
});
```

---

### 4. 반응형 레이아웃 테스트

**파일**: `e2e/tests/footer-responsive.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { FooterComponent } from '../components/FooterComponent';

test.describe('Footer responsive layout', () => {
  test('should display 3-column grid on desktop', async ({ page }) => {
    // Set desktop viewport
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/');

    const footer = new FooterComponent(page);

    // Check if sections are displayed side by side (3 columns)
    const clubInfoBox = await footer.clubInfoSection.boundingBox();
    const contactBox = await footer.contactSection.boundingBox();

    // On desktop, sections should be on same horizontal level
    expect(clubInfoBox?.y).toBeCloseTo(contactBox?.y || 0, 50);
  });

  test('should display stacked layout on mobile', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');

    const footer = new FooterComponent(page);

    // Check if sections are stacked vertically
    const clubInfoBox = await footer.clubInfoSection.boundingBox();
    const contactBox = await footer.contactSection.boundingBox();

    // On mobile, contact section should be below club info
    expect(contactBox?.y || 0).toBeGreaterThan(clubInfoBox?.y || 0);
  });

  test('should be scrollable on mobile with long content', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');

    const footer = new FooterComponent(page);

    // Footer should be visible and all content accessible
    expect(await footer.isVisible()).toBe(true);
    expect(await footer.copyrightText.isVisible()).toBe(true);
  });
});
```

---

### 5. 다크모드 전환 테스트

**파일**: `e2e/tests/footer-dark-mode.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { FooterComponent } from '../components/FooterComponent';

test.describe('Footer dark mode support', () => {
  test('should apply dark mode styles when theme is dark', async ({ page }) => {
    await page.goto('/');

    // Toggle dark mode (adjust selector based on your theme toggle button)
    const themeToggle = page.locator('[aria-label="Toggle theme"]');
    await themeToggle.click();

    // Wait for theme transition
    await page.waitForTimeout(300);

    const footer = new FooterComponent(page);

    // Verify dark mode background
    const isDarkMode = await footer.verifyDarkMode(true);
    expect(isDarkMode).toBe(true);
  });

  test('should apply light mode styles when theme is light', async ({ page }) => {
    await page.goto('/');

    const footer = new FooterComponent(page);

    // Verify light mode background (default)
    const isLightMode = await footer.verifyDarkMode(false);
    expect(isLightMode).toBe(true);
  });

  test('should persist theme preference across page navigation', async ({ page }) => {
    await page.goto('/');

    // Enable dark mode
    const themeToggle = page.locator('[aria-label="Toggle theme"]');
    await themeToggle.click();
    await page.waitForTimeout(300);

    // Navigate to another page
    await page.goto('/events');

    const footer = new FooterComponent(page);

    // Dark mode should still be applied
    const isDarkMode = await footer.verifyDarkMode(true);
    expect(isDarkMode).toBe(true);
  });
});
```

---

## 수동 테스트 체크리스트

### 시각적 검증

**푸터 표시**:
- [ ] 홈페이지(`/`)에서 푸터가 페이지 하단에 표시됨
- [ ] 게시판 페이지(`/board/*`)에서 푸터 표시됨
- [ ] 행사 페이지(`/events`)에서 푸터 표시됨
- [ ] 문의 페이지(`/inquiry`)에서 푸터 표시됨
- [ ] 로그인/회원가입 페이지에서 푸터 숨겨짐
- [ ] 관리자 페이지에서 푸터 숨겨짐
- [ ] 마이페이지에서 푸터 숨겨짐

**레이아웃**:
- [ ] 데스크톱(>= 1024px)에서 3열 그리드로 표시됨
- [ ] 모바일(< 1024px)에서 1열 스택 레이아웃으로 표시됨
- [ ] 콘텐츠가 짧은 페이지에서도 푸터가 하단에 고정됨
- [ ] 콘텐츠가 긴 페이지에서 스크롤 하단에 푸터가 나타남
- [ ] 사이드바(z-50)가 푸터(z-10) 위에 올바르게 표시됨

**내용**:
- [ ] 동아리 이름, 전체 이름이 올바르게 표시됨
- [ ] 동아리 방 주소가 표시됨
- [ ] 이메일, 전화번호가 올바른 형식으로 표시됨
- [ ] SNS 아이콘(인스타그램, 페이스북)이 표시됨
- [ ] 바로가기 링크(게시판, 행사, 문의)가 표시됨
- [ ] 법적 링크(개인정보처리방침, 이용약관)가 표시됨
- [ ] 저작권 문구가 올바른 연도로 표시됨

**다크모드**:
- [ ] 라이트 모드에서 배경이 밝은 회색(`bg-gray-50`)
- [ ] 다크 모드에서 배경이 어두운 회색(`bg-[#1A1A1A]`)
- [ ] 라이트 모드에서 텍스트 색상이 읽기 좋음
- [ ] 다크 모드에서 텍스트 색상이 읽기 좋음
- [ ] 테마 전환 시 푸터 색상이 부드럽게 변경됨

---

### 기능 검증

**내부 링크 네비게이션**:
- [ ] "게시판" 클릭 시 `/board/notices`로 이동
- [ ] "행사" 클릭 시 `/events`로 이동
- [ ] "문의" 클릭 시 `/inquiry`로 이동
- [ ] "개인정보처리방침" 클릭 시 `/privacy`로 이동
- [ ] "이용약관" 클릭 시 `/terms`로 이동

**외부 링크**:
- [ ] 인스타그램 아이콘 클릭 시 새 탭에서 인스타그램 페이지 열림
- [ ] 페이스북 아이콘 클릭 시 새 탭에서 페이스북 페이지 열림
- [ ] 이메일 주소 클릭 시 메일 앱이 열림 (mailto:)
- [ ] 전화번호 클릭 시 전화 앱이 열림 (tel:) - 모바일

**반응형**:
- [ ] 브라우저 크기를 조절하면 레이아웃이 부드럽게 전환됨
- [ ] 1024px 브레이크포인트에서 3열 → 1열로 전환됨
- [ ] 모바일에서 모든 텍스트가 읽기 좋은 크기로 표시됨
- [ ] 터치 타겟(링크, 아이콘)이 충분히 큼 (최소 44x44px)

---

### 접근성 검증

**키보드 네비게이션**:
- [ ] Tab 키로 모든 링크에 접근 가능
- [ ] 포커스 표시가 명확함
- [ ] Enter 키로 링크 활성화 가능
- [ ] 링크 순서가 논리적임

**스크린 리더**:
- [ ] 푸터 랜드마크(`<footer>`)가 올바르게 인식됨
- [ ] 모든 링크에 적절한 텍스트가 있음
- [ ] 아이콘 링크에 aria-label 또는 대체 텍스트가 있음
- [ ] 섹션 제목이 의미론적으로 올바름

**색상 대비**:
- [ ] 라이트 모드에서 텍스트와 배경의 대비가 WCAG AA 기준 충족 (최소 4.5:1)
- [ ] 다크 모드에서 텍스트와 배경의 대비가 WCAG AA 기준 충족
- [ ] 링크 색상이 주변 텍스트와 구분됨
- [ ] 호버/포커스 상태가 명확함

---

### z-index 레이어링 검증

**데스크톱**:
- [ ] 사이드바(z-50)가 푸터(z-10) 위에 표시됨
- [ ] 푸터의 왼쪽 부분이 사이드바에 가려짐 (정상 동작)
- [ ] Header(z-30/40)가 푸터 위에 올바르게 표시됨
- [ ] 모달이나 드롭다운이 푸터 위에 올바르게 표시됨

**모바일**:
- [ ] 사이드바 오버레이(z-50)가 푸터 위에 표시됨
- [ ] 백드롭(z-40)이 푸터를 가림
- [ ] 사이드바 닫을 때 푸터가 다시 보임

---

## 성능 검증

**로딩**:
- [ ] 푸터가 페이지 로드 시 빠르게 렌더링됨
- [ ] 아이콘(Lucide-react)이 빠르게 로드됨
- [ ] 레이아웃 시프트(CLS)가 발생하지 않음

**메모리**:
- [ ] 페이지 전환 시 메모리 누수가 없음
- [ ] 테마 전환 시 성능 저하가 없음

---

## 주의사항

### 데이터 업데이트
- 실제 동아리 정보로 업데이트되었는지 확인
  - 주소, 이메일, 전화번호
  - SNS URL (인스타그램, 페이스북)

### 브라우저 호환성
- Chrome, Firefox, Safari, Edge에서 테스트
- 모바일 브라우저(iOS Safari, Chrome Mobile)에서 테스트

### 법적 페이지 내용
- 개인정보처리방침과 이용약관 내용이 법적으로 적절한지 검토 필요
- 업데이트 날짜가 올바른지 확인

---

## 자동화 테스트 실행 방법

```bash
# 모든 Footer 테스트 실행
npx playwright test e2e/tests/footer-*.spec.ts

# 특정 테스트만 실행
npx playwright test e2e/tests/footer-display.spec.ts

# UI 모드로 실행 (디버깅용)
npx playwright test --ui

# 헤드리스 모드로 실행
npx playwright test --headed
```

---

## 문제 해결

### 푸터가 표시되지 않는 경우
1. `shouldShowFooter` 함수의 경로 매칭 로직 확인
2. Layout 컴포넌트에서 조건부 렌더링 로직 확인
3. CSS z-index 충돌 확인

### 링크가 작동하지 않는 경우
1. 라우터 설정 확인 (`/privacy`, `/terms`)
2. 상수 파일의 경로 확인 (`FOOTER_QUICK_LINKS`, `FOOTER_LEGAL_LINKS`)
3. 외부 링크의 URL 형식 확인

### 다크모드가 적용되지 않는 경우
1. `useUIStore`의 theme 상태 확인
2. Tailwind CSS의 dark mode 설정 확인
3. 조건부 클래스명이 올바르게 적용되는지 확인

---

## 관련 문서

- [Auth 테스트 가이드](./auth-test-guide.md)
- [Posts 테스트 가이드](./posts-test-guide.md)
- [Inquiries 테스트 가이드](./inquiries-test-guide.md)
