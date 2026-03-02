# Auth 테스트 가이드 (Playwright)

## 개요

이 문서는 인증(Auth) 시스템의 E2E 테스트를 Playwright로 작성할 때 참고할 수 있는 가이드입니다.

**테스트 대상 기능**:

- 회원가입
- 이메일 인증 (인증 코드 발송 및 검증)
- 로그인
- 중복 데이터 에러 처리 (학번, 이메일, 전화번호)
- 클라이언트 측 유효성 검사 (비밀번호 불일치 등)

**주요 플로우**:
회원가입 → 이메일 인증 → 로그인

---

## Playwright 자동화 테스트

### Page Objects

#### SignupPage

**파일**: `e2e/pages/SignupPage.ts`

```typescript
import { Page, Locator } from "@playwright/test";

export class SignupPage {
  readonly page: Page;

  // Form inputs
  readonly studentIdInput: Locator;
  readonly nameInput: Locator;
  readonly emailInput: Locator;
  readonly phoneNumberInput: Locator;
  readonly departmentInput: Locator;
  readonly gradeInput: Locator;
  readonly genderSelect: Locator;
  readonly motivationTextarea: Locator;
  readonly passwordInput: Locator;
  readonly passwordConfirmInput: Locator;
  readonly privacyConsentCheckbox: Locator;

  // Buttons
  readonly signupButton: Locator;

  // Error messages
  readonly studentIdError: Locator;
  readonly emailError: Locator;
  readonly phoneNumberError: Locator;
  readonly passwordConfirmError: Locator;

  constructor(page: Page) {
    this.page = page;

    // Form inputs
    this.studentIdInput = page.getByPlaceholder("학번 (8자리)");
    this.nameInput = page.getByPlaceholder("이름");
    this.emailInput = page.getByPlaceholder("이메일");
    this.phoneNumberInput = page.getByPlaceholder(
      "전화번호 (예: 010-1234-5678)",
    );
    this.departmentInput = page.getByPlaceholder("학과 (예: 컴퓨터공학과)");
    this.gradeInput = page.getByPlaceholder("학년 (1~4)");
    this.genderSelect = page.locator('select[name="gender"]');
    this.motivationTextarea =
      page.getByPlaceholder("동아리 가입 동기를 작성해주세요");
    this.passwordInput = page.locator('input[name="password"]').first();
    this.passwordConfirmInput = page.locator('input[name="passwordConfirm"]');
    this.privacyConsentCheckbox = page.locator('input[name="privacyConsent"]');

    // Buttons
    this.signupButton = page.getByRole("button", { name: "회원가입" });

    // Error messages (필드 바로 아래에 표시되는 에러 메시지)
    this.studentIdError = page
      .locator('input[name="studentId"]')
      .locator("..")
      .locator("p.text-red-500");
    this.emailError = page
      .locator('input[name="email"]')
      .locator("..")
      .locator("p.text-red-500");
    this.phoneNumberError = page
      .locator('input[name="phoneNumber"]')
      .locator("..")
      .locator("p.text-red-500");
    this.passwordConfirmError = page
      .locator('input[name="passwordConfirm"]')
      .locator("..")
      .locator("p.text-red-500");
  }

  async goto() {
    await this.page.goto("/signup");
  }

  async fillForm(data: {
    studentId: string;
    name: string;
    email: string;
    phoneNumber: string;
    department: string;
    grade: string;
    gender: "MALE" | "FEMALE";
    motivation: string;
    password: string;
    passwordConfirm: string;
  }) {
    await this.studentIdInput.fill(data.studentId);
    await this.nameInput.fill(data.name);
    await this.emailInput.fill(data.email);
    await this.phoneNumberInput.fill(data.phoneNumber);
    await this.departmentInput.fill(data.department);
    await this.gradeInput.fill(data.grade);
    await this.genderSelect.selectOption(data.gender);
    await this.motivationTextarea.fill(data.motivation);
    await this.passwordInput.fill(data.password);
    await this.passwordConfirmInput.fill(data.passwordConfirm);
    await this.privacyConsentCheckbox.check();
  }

  async submit() {
    await this.signupButton.click();
  }

  async hasErrorBorder(input: Locator) {
    const className = await input.getAttribute("class");
    return className?.includes("border-red-500") ?? false;
  }

  async getErrorMessage(errorLocator: Locator) {
    return await errorLocator.textContent();
  }
}
```

#### VerifyEmailPage

**파일**: `e2e/pages/VerifyEmailPage.ts`

```typescript
import { Page, Locator } from "@playwright/test";

export class VerifyEmailPage {
  readonly page: Page;

  readonly emailInput: Locator;
  readonly codeInput: Locator;
  readonly verifyButton: Locator;
  readonly resendButton: Locator;

  constructor(page: Page) {
    this.page = page;

    this.emailInput = page.getByPlaceholder(/이메일/);
    this.codeInput = page.getByPlaceholder(/6자리 인증 코드/);
    this.verifyButton = page.getByRole("button", { name: "인증 확인" });
    this.resendButton = page.getByRole("button", { name: /인증 코드 재발송/ });
  }

  async goto() {
    await this.page.goto("/verify-email");
  }

  async fillCode(code: string) {
    await this.codeInput.fill(code);
  }

  async verify() {
    await this.verifyButton.click();
  }

  async resendCode() {
    await this.resendButton.click();
  }

  async isResendButtonDisabled() {
    return await this.resendButton.isDisabled();
  }

  async getResendButtonText() {
    return await this.resendButton.textContent();
  }
}
```

#### LoginPage

**파일**: `e2e/pages/LoginPage.ts`

```typescript
import { Page, Locator } from "@playwright/test";

export class LoginPage {
  readonly page: Page;

  readonly studentIdInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;

  constructor(page: Page) {
    this.page = page;

    this.studentIdInput = page.getByPlaceholder("학번 (8자리)");
    this.passwordInput = page.getByPlaceholder(/비밀번호/);
    this.loginButton = page.getByRole("button", { name: "로그인" });
  }

  async goto() {
    await this.page.goto("/login");
  }

  async login(studentId: string, password: string) {
    await this.studentIdInput.fill(studentId);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }
}
```

### 테스트 유틸리티

#### 테스트 데이터 생성

**파일**: `e2e/utils/testData.ts`

```typescript
/**
 * 고유한 테스트 사용자 데이터 생성
 * (각 테스트마다 다른 데이터를 사용하여 중복 에러 방지)
 */
export function generateUniqueUser() {
  const timestamp = Date.now();
  const random = Math.floor(Math.random() * 1000);

  return {
    studentId: `${timestamp.toString().slice(-8)}`,
    name: `테스트${random}`,
    email: `test${timestamp}@inha.edu`,
    phoneNumber: `010-${String(timestamp).slice(-8, -4)}-${String(timestamp).slice(-4)}`,
    department: "컴퓨터공학과",
    grade: "3",
    gender: "MALE" as const,
    motivation: "프로그래밍에 관심이 많아서 지원합니다",
    password: "Test1234!@",
    passwordConfirm: "Test1234!@",
  };
}

/**
 * 이미 존재하는 사용자 데이터 (중복 테스트용)
 */
export const EXISTING_USER = {
  studentId: "12345678",
  email: "existing@inha.edu",
  phoneNumber: "010-1234-5678",
  password: "Test1234!@",
};
```

#### API 응답 대기

**파일**: `e2e/utils/apiHelpers.ts`

```typescript
import { Page, expect } from "@playwright/test";

/**
 * API 응답 대기 및 검증
 */
export async function waitForApiResponse(
  page: Page,
  url: string,
  expectedStatus: number,
) {
  const response = await page.waitForResponse(
    (response) =>
      response.url().includes(url) && response.status() === expectedStatus,
  );
  return response;
}

/**
 * API 요청 본문 캡처
 */
export async function captureRequestBody(page: Page, url: string) {
  const request = await page.waitForRequest((request) =>
    request.url().includes(url),
  );
  return request.postDataJSON();
}
```

### 테스트 케이스

#### 1. 정상 회원가입 플로우

**파일**: `e2e/auth/signup.spec.ts`

```typescript
import { test, expect } from "@playwright/test";
import { SignupPage } from "../pages/SignupPage";
import { VerifyEmailPage } from "../pages/VerifyEmailPage";
import { LoginPage } from "../pages/LoginPage";
import { generateUniqueUser } from "../utils/testData";
import { waitForApiResponse } from "../utils/apiHelpers";

test.describe("회원가입", () => {
  test("정상 회원가입 → 이메일 인증 → 로그인", async ({ page }) => {
    const signupPage = new SignupPage(page);
    const verifyEmailPage = new VerifyEmailPage(page);
    const loginPage = new LoginPage(page);

    const userData = generateUniqueUser();

    // 1. 회원가입 페이지 이동
    await signupPage.goto();

    // 2. 폼 작성
    await signupPage.fillForm(userData);

    // 3. 회원가입 API 호출 대기
    const signupPromise = waitForApiResponse(
      page,
      "/api/v1/auth/password/signup",
      201,
    );

    // 4. 제출
    await signupPage.submit();

    // 5. API 응답 확인
    await signupPromise;

    // 6. 알림 확인 (회원가입 성공 메시지)
    page.on("dialog", async (dialog) => {
      expect(dialog.message()).toContain("회원가입이 완료되었습니다");
      await dialog.accept();
    });

    // 7. 이메일 인증 페이지로 리다이렉트 확인
    await expect(page).toHaveURL("/verify-email");

    // 8. 이메일이 자동 입력되어 있는지 확인
    await expect(verifyEmailPage.emailInput).toHaveValue(userData.email);
    await expect(verifyEmailPage.emailInput).toBeDisabled();

    // 9. 인증 코드 입력 (실제 이메일에서 가져와야 함 - 테스트 환경에서는 mock)
    // TODO: 테스트 환경에서 이메일 확인 방법 구현 필요
    const mockVerificationCode = "123456";
    await verifyEmailPage.fillCode(mockVerificationCode);

    // 10. 인증 확인
    const verifyPromise = waitForApiResponse(
      page,
      "/api/v1/auth/password/verify-email",
      200,
    );
    await verifyEmailPage.verify();
    await verifyPromise;

    // 11. 로그인 페이지로 리다이렉트
    await expect(page).toHaveURL("/login");

    // 12. 로그인
    await loginPage.login(userData.studentId, userData.password);

    // 13. 로그인 성공 확인 (메인 페이지로 리다이렉트)
    await waitForApiResponse(page, "/api/v1/auth/password/login", 200);
    await expect(page).toHaveURL("/");
  });
});
```

#### 2. 중복 에러 테스트

**파일**: `e2e/auth/signup-errors.spec.ts`

```typescript
import { test, expect } from "@playwright/test";
import { SignupPage } from "../pages/SignupPage";
import { generateUniqueUser, EXISTING_USER } from "../utils/testData";

test.describe("회원가입 에러 처리", () => {
  test("중복 학번 에러", async ({ page }) => {
    const signupPage = new SignupPage(page);
    await signupPage.goto();

    const userData = generateUniqueUser();
    await signupPage.fillForm({
      ...userData,
      studentId: EXISTING_USER.studentId, // 이미 존재하는 학번
    });

    await signupPage.submit();

    // API 응답 대기 (409 Conflict)
    await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/auth/password/signup") &&
        response.status() === 409,
    );

    // 에러 메시지 확인
    await expect(signupPage.studentIdError).toBeVisible();
    await expect(signupPage.studentIdError).toHaveText(
      "이미 가입된 학번입니다.",
    );

    // 빨간 테두리 확인
    const hasErrorBorder = await signupPage.hasErrorBorder(
      signupPage.studentIdInput,
    );
    expect(hasErrorBorder).toBe(true);
  });

  test("중복 이메일 에러", async ({ page }) => {
    const signupPage = new SignupPage(page);
    await signupPage.goto();

    const userData = generateUniqueUser();
    await signupPage.fillForm({
      ...userData,
      email: EXISTING_USER.email, // 이미 존재하는 이메일
    });

    await signupPage.submit();

    await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/auth/password/signup") &&
        response.status() === 409,
    );

    await expect(signupPage.emailError).toBeVisible();
    await expect(signupPage.emailError).toHaveText(
      "이미 존재하는 이메일입니다.",
    );
  });

  test("중복 전화번호 에러", async ({ page }) => {
    const signupPage = new SignupPage(page);
    await signupPage.goto();

    const userData = generateUniqueUser();
    await signupPage.fillForm({
      ...userData,
      phoneNumber: EXISTING_USER.phoneNumber, // 이미 존재하는 전화번호
    });

    await signupPage.submit();

    await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/auth/password/signup") &&
        response.status() === 409,
    );

    await expect(signupPage.phoneNumberError).toBeVisible();
    await expect(signupPage.phoneNumberError).toHaveText(
      "이미 등록된 전화번호입니다.",
    );
  });

  test("비밀번호 불일치 에러 (클라이언트 측)", async ({ page }) => {
    const signupPage = new SignupPage(page);
    await signupPage.goto();

    const userData = generateUniqueUser();
    await signupPage.fillForm({
      ...userData,
      passwordConfirm: "DifferentPassword123!", // 다른 비밀번호
    });

    await signupPage.submit();

    // 클라이언트 측 유효성 검사이므로 API 요청이 발생하지 않아야 함
    // 대신 에러 메시지가 즉시 표시됨
    await expect(signupPage.passwordConfirmError).toBeVisible();
    await expect(signupPage.passwordConfirmError).toHaveText(
      "비밀번호가 일치하지 않습니다.",
    );

    // 빨간 테두리 확인
    const hasErrorBorder = await signupPage.hasErrorBorder(
      signupPage.passwordConfirmInput,
    );
    expect(hasErrorBorder).toBe(true);
  });
});
```

#### 3. 이메일 인증 테스트

**파일**: `e2e/auth/verify-email.spec.ts`

```typescript
import { test, expect } from "@playwright/test";
import { VerifyEmailPage } from "../pages/VerifyEmailPage";

test.describe("이메일 인증", () => {
  test("인증 코드 재발송 쿨다운", async ({ page }) => {
    const verifyEmailPage = new VerifyEmailPage(page);
    await verifyEmailPage.goto();

    // 이메일 입력
    await verifyEmailPage.emailInput.fill("test@inha.edu");

    // 재발송 버튼 클릭
    await verifyEmailPage.resendCode();

    // API 응답 대기
    await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/auth/password/resend-verification") &&
        response.status() === 200,
    );

    // 쿨다운 시작 확인 (버튼이 비활성화되어야 함)
    const isDisabled = await verifyEmailPage.isResendButtonDisabled();
    expect(isDisabled).toBe(true);

    // 버튼 텍스트에 남은 시간 표시 확인
    const buttonText = await verifyEmailPage.getResendButtonText();
    expect(buttonText).toMatch(/인증 코드 재발송 \(\d+초\)/);

    // 1초 대기 후 카운트다운 확인
    await page.waitForTimeout(1000);
    const updatedButtonText = await verifyEmailPage.getResendButtonText();
    expect(updatedButtonText).toMatch(/인증 코드 재발송 \(5[0-9]초\)/);
  });

  test("잘못된 인증 코드", async ({ page }) => {
    const verifyEmailPage = new VerifyEmailPage(page);
    await verifyEmailPage.goto();

    await verifyEmailPage.emailInput.fill("test@inha.edu");
    await verifyEmailPage.fillCode("000000"); // 잘못된 코드

    // 알림 대기 (인증 실패 메시지)
    page.on("dialog", async (dialog) => {
      expect(dialog.message()).toContain("인증에 실패");
      await dialog.accept();
    });

    await verifyEmailPage.verify();

    // API 응답 대기 (400 또는 401)
    await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/auth/password/verify-email") &&
        (response.status() === 400 || response.status() === 401),
    );
  });
});
```

#### 4. 로그인 테스트

**파일**: `e2e/auth/login.spec.ts`

```typescript
import { test, expect } from "@playwright/test";
import { LoginPage } from "../pages/LoginPage";
import { EXISTING_USER } from "../utils/testData";

test.describe("로그인", () => {
  test("이메일 인증 전 로그인 시도", async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // 회원가입은 했지만 이메일 인증을 하지 않은 계정
    await loginPage.login("99999999", "Test1234!@");

    // 403 Forbidden 응답 대기
    await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/auth/password/login") &&
        response.status() === 403,
    );

    // 알림 확인
    page.on("dialog", async (dialog) => {
      expect(dialog.message()).toContain("이메일 인증이 완료되지 않았습니다");
      await dialog.accept();
    });
  });

  test("정상 로그인", async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    await loginPage.login(EXISTING_USER.studentId, EXISTING_USER.password);

    // 로그인 성공 응답 대기
    const response = await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/auth/password/login") &&
        response.status() === 200,
    );

    // 응답 본문 확인
    const responseBody = await response.json();
    expect(responseBody).toHaveProperty("accessToken");
    expect(responseBody).toHaveProperty("studentId");

    // 메인 페이지로 리다이렉트 확인
    await expect(page).toHaveURL("/");

    // LocalStorage에 인증 정보 저장 확인
    const authState = await page.evaluate(() => {
      return localStorage.getItem("auth-storage");
    });
    expect(authState).toBeTruthy();
  });

  test("잘못된 비밀번호", async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    await loginPage.login(EXISTING_USER.studentId, "WrongPassword123!");

    // 401 Unauthorized 응답 대기
    await page.waitForResponse(
      (response) =>
        response.url().includes("/api/v1/auth/password/login") &&
        response.status() === 401,
    );

    // 알림 확인
    page.on("dialog", async (dialog) => {
      expect(dialog.message()).toContain(
        "학번 또는 비밀번호가 일치하지 않습니다",
      );
      await dialog.accept();
    });
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

### Test 1: 회원가입 & 이메일 인증

#### 1.1 정상 회원가입

**시나리오**: 새로운 계정 생성

1. 브라우저에서 `http://localhost:5173/signup` 접속
2. 폼 입력:
   - 학번: `12345678`
   - 이름: `홍길동`
   - 이메일: `test@inha.edu`
   - 전화번호: `010-1234-5678`
   - 학과: `컴퓨터공학과`
   - 학년: `3`
   - 성별: `남성` 선택
   - 가입 동기: `프로그래밍에 관심이 많아서 지원합니다`
   - 비밀번호: `Test1234!@`
   - 비밀번호 확인: `Test1234!@`
   - 개인정보 동의: 체크
3. "회원가입" 버튼 클릭
4. **예상 결과**:
   - 회원가입 성공 메시지 + 이메일 인증 안내
   - 이메일 인증 페이지(`/verify-email`)로 리다이렉트
5. **Network 탭 확인**:
   - 요청 URL: `POST .../api/v1/auth/password/signup`
   - Status: 201 Created
   - Request Body에 모든 필드 포함
   - Response Body: null (정상)
6. **Console 탭 확인**:
   - 에러 없음

#### 1.2 이메일 인증

**시나리오**: 이메일로 받은 6자리 코드 입력

1. 회원가입 후 자동으로 `/verify-email` 페이지로 이동
2. 이메일 주소가 자동으로 입력되어 있음 (변경 불가)
3. 이메일에서 받은 6자리 코드 입력 (예: `123456`)
4. "인증 확인" 버튼 클릭
5. **예상 결과**:
   - 이메일 인증 성공 메시지
   - 로그인 페이지로 리다이렉트
6. **Network 탭 확인**:
   - 요청 URL: `POST .../api/v1/auth/password/verify-email`
   - Request Body: `{ email: "test@inha.edu", code: "123456" }`
   - Status: 200 OK

#### 1.3 인증 코드 재발송

**시나리오**: 코드를 받지 못했거나 만료된 경우

1. `/verify-email` 페이지에서 "인증 코드 재발송" 버튼 클릭
2. **예상 결과**:
   - "인증 코드가 재발송되었습니다" 메시지
   - 버튼이 60초 쿨다운 ("인증 코드 재발송 (59초)" 형태로 표시)
   - 새로운 코드가 이메일로 발송됨
3. **Network 탭 확인**:
   - 요청 URL: `POST .../api/v1/auth/password/resend-verification`
   - Request Body: `{ email: "test@inha.edu" }`
   - Status: 200 OK

#### 1.4 중복 학번 에러 처리

**시나리오**: 이미 존재하는 학번으로 가입 시도

1. 위와 동일한 학번으로 다시 가입 시도
2. **예상 결과**:
   - **학번 input에 빨간 테두리 표시**
   - **학번 input 아래에 "이미 가입된 학번입니다." 에러 메시지 표시**
   - Status: 409 Conflict
3. **UI 확인**:
   - 에러가 있는 필드만 빨간 테두리
   - 다른 필드는 정상 테두리 유지
   - 에러 메시지는 해당 input 바로 아래에 빨간색 텍스트로 표시

#### 1.5 중복 이메일 에러 처리

**시나리오**: 이미 사용 중인 이메일로 가입 시도

1. 기존 회원이 사용 중인 이메일로 가입 시도
2. **예상 결과**:
   - **이메일 input에 빨간 테두리**
   - **"이미 존재하는 이메일입니다." 에러 메시지**
   - Status: 409 Conflict

#### 1.6 중복 전화번호 에러 처리

**시나리오**: 이미 등록된 전화번호로 가입 시도

1. 기존 회원의 전화번호로 가입 시도
2. **예상 결과**:
   - **전화번호 input에 빨간 테두리**
   - **"이미 등록된 전화번호입니다." 에러 메시지**
   - Status: 409 Conflict

#### 1.7 비밀번호 불일치 에러

**시나리오**: 비밀번호와 비밀번호 확인이 일치하지 않음

1. 회원가입 폼에서 비밀번호: `Test1234!@`, 비밀번호 확인: `Test1234!` 입력
2. **예상 결과**:
   - **비밀번호 확인 input에 빨간 테두리**
   - **"비밀번호가 일치하지 않습니다." 에러 메시지**
   - 서버 요청 전에 클라이언트 측에서 차단

---

### Test 2: 로그인

#### 2.1 이메일 인증 전 로그인 시도

**시나리오**: 이메일 인증을 하지 않고 로그인 시도

1. 회원가입만 하고 이메일 인증을 건너뛴 상태
2. 로그인 페이지에서 로그인 시도
3. **예상 결과**:
   - "이메일 인증이 완료되지 않았습니다" 에러 메시지
   - Status: 403 Forbidden

#### 2.2 정상 로그인 (이메일 인증 후)

**시나리오**: 이메일 인증 완료 후 로그인

1. 브라우저에서 `http://localhost:5173/login` 접속
2. 폼 입력:
   - 학번: `12345678`
   - 비밀번호: `Test1234!@`
3. "로그인" 버튼 클릭
4. **예상 결과**:
   - 로그인 성공
   - 메인 페이지로 리다이렉트
   - authStore에 user, accessToken 저장됨
5. **Network 탭 확인**:
   - 요청 URL: `POST .../api/v1/auth/password/login`
   - Status: 200 OK
   - Response Body: `{ accessToken: "...", userId: ..., studentId: "...", name: "...", role: "..." }`
   - **주의**: refreshToken이 응답에 없고 Set-Cookie 헤더로 전달될 수 있음
6. **Console 탭 확인**:
   - 에러 없음

#### 2.3 잘못된 비밀번호

**시나리오**: 틀린 비밀번호로 로그인 시도

1. 학번: `12345678`, 비밀번호: `WrongPassword`
2. **예상 결과**:
   - "학번 또는 비밀번호가 일치하지 않습니다" 에러
   - Status: 401 Unauthorized

---

## 테스트 실행 방법

### 모든 테스트 실행

```bash
npx playwright test
```

### 특정 테스트 파일 실행

```bash
npx playwright test e2e/auth/signup.spec.ts
```

### UI 모드로 실행 (디버깅)

```bash
npx playwright test --ui
```

### 특정 브라우저에서만 실행

```bash
npx playwright test --project=chromium
```

### 헤드풀 모드 (브라우저 표시)

```bash
npx playwright test --headed
```

---

## 주의사항

### 1. 이메일 인증 코드 처리

실제 이메일을 확인할 수 없는 테스트 환경에서는 다음 방법을 고려:

**Option A: 테스트 전용 API 엔드포인트**

```typescript
// 백엔드에 테스트 전용 엔드포인트 추가 (개발 환경에만)
// GET /api/v1/test/verification-code?email=test@inha.edu
const response = await page.request.get(
  "/api/v1/test/verification-code?email=test@inha.edu",
);
const { code } = await response.json();
await verifyEmailPage.fillCode(code);
```

**Option B: 테스트용 고정 코드**

```typescript
// 백엔드에서 특정 이메일 패턴에 대해 고정 코드 사용
// 예: test-*@inha.edu 이메일은 항상 "123456" 코드 사용
await verifyEmailPage.fillCode("123456");
```

### 2. 중복 데이터 문제

각 테스트마다 고유한 데이터를 사용하거나, 테스트 후 데이터를 정리:

```typescript
test.afterEach(async ({ request }) => {
  // 테스트 계정 삭제 (백엔드에 삭제 API 필요)
  await request.delete("/api/v1/test/users/cleanup");
});
```

### 3. 병렬 실행 문제

병렬 테스트 실행 시 중복 에러가 발생할 수 있으므로, 고유 데이터 생성기 사용:

```typescript
// generateUniqueUser()는 timestamp + random을 사용하여 충돌 방지
const userData = generateUniqueUser();
```

---

## 실시간 중복 체크 테스트 (Issue #338)

회원가입 페이지에서 학번과 이메일 입력 시 실시간 중복 검증 API가 호출되고 UI 피드백이 표시되는지 테스트합니다.

### 시나리오 1: 학번 중복 실시간 검증

**목적**: 학번 입력 필드에서 포커스를 벗어날 때 중복 체크 API가 호출되고 결과가 표시되는지 확인

**테스트 단계**:

1. 회원가입 페이지로 이동
2. 학번 입력 필드에 8자리 숫자 입력 (예: "12345678")
3. 다른 필드로 포커스 이동 (blur 이벤트 발생)
4. **예상 결과**:
   - Network 탭에서 `GET /api/v1/auth/password/check-student-id?studentId=12345678` 요청 확인
   - 입력 필드 오른쪽에 스피너 아이콘 표시 (로딩 중)
   - 사용 가능한 학번인 경우: 초록색 체크 아이콘 + "사용 가능한 학번입니다." 텍스트 표시
   - 중복된 학번인 경우: 빨간색 에러 메시지 "이미 가입된 학번입니다." 표시
5. 학번을 수정하면 이전 체크 결과가 리셋되는지 확인

**Playwright 코드 예시**:

```typescript
test("학번 중복 실시간 검증 - 사용 가능", async ({ page }) => {
  const signupPage = new SignupPage(page);
  await page.goto("/signup");

  // 학번 입력 및 blur
  const studentId = generateUniqueUser().studentId;
  await signupPage.studentIdInput.fill(studentId);
  await signupPage.nameInput.click(); // 포커스 이동

  // API 호출 대기
  const response = await waitForApiResponse(
    page,
    "/api/v1/auth/password/check-student-id",
    200,
  );
  expect(response.status()).toBe(200);

  // 로딩 스피너 확인
  const spinner = page.locator(".animate-spin");
  await expect(spinner).toBeVisible();
  await expect(spinner).toBeHidden({ timeout: 3000 });

  // 성공 피드백 확인
  const successIcon = page
    .locator('[class*="text-green-600"]')
    .filter({ has: page.locator("svg") });
  await expect(successIcon).toBeVisible();
  await expect(page.getByText("사용 가능한 학번입니다.")).toBeVisible();
});

test("학번 중복 실시간 검증 - 중복", async ({ page }) => {
  const signupPage = new SignupPage(page);
  await page.goto("/signup");

  // 이미 존재하는 학번 입력
  await signupPage.studentIdInput.fill("12345678"); // 기존 사용자 학번
  await signupPage.nameInput.click();

  // API 호출 대기 (409 Conflict)
  const response = await waitForApiResponse(
    page,
    "/api/v1/auth/password/check-student-id",
    409,
  );
  expect(response.status()).toBe(409);

  // 에러 메시지 확인
  await expect(page.getByText("이미 가입된 학번입니다.")).toBeVisible();

  // 성공 아이콘이 표시되지 않음
  const successIcon = page.locator('[class*="text-green-600"]');
  await expect(successIcon).toBeHidden();
});

test("학번 수정 시 체크 결과 리셋", async ({ page }) => {
  const signupPage = new SignupPage(page);
  await page.goto("/signup");

  // 첫 번째 체크
  await signupPage.studentIdInput.fill("12345678");
  await signupPage.nameInput.click();
  await waitForApiResponse(page, "/api/v1/auth/password/check-student-id", 200);

  // 학번 수정
  await signupPage.studentIdInput.fill("87654321");

  // 이전 체크 결과가 사라졌는지 확인
  await expect(page.getByText("사용 가능한 학번입니다.")).toBeHidden();
  const successIcon = page.locator('[class*="text-green-600"]');
  await expect(successIcon).toBeHidden();
});
```

### 시나리오 2: 이메일 중복 실시간 검증

**목적**: 이메일 입력 필드에서 포커스를 벗어날 때 또는 도메인 변경 시 중복 체크 API가 호출되는지 확인

**테스트 단계**:

1. 회원가입 페이지의 Step 1 (연락처) 진행
2. 이메일 로컬파트 입력 (예: "testuser")
3. 도메인 선택 (예: "inha.edu")
4. 다른 필드로 포커스 이동
5. **예상 결과**:
   - Network 탭에서 `GET /api/v1/auth/password/check-email?email=testuser@inha.edu` 요청 확인
   - "확인 중..." 로딩 텍스트 표시
   - 사용 가능: "사용 가능한 이메일입니다." 초록색 텍스트
   - 중복: "이미 사용 중인 이메일입니다." 빨간색 에러

**도메인 변경 시 재검증**:

1. 이메일 로컬파트 입력 후 `inha.edu` 선택 → 체크 완료
2. 도메인을 `gmail.com`으로 변경
3. **예상 결과**:
   - 이전 체크 결과 리셋
   - 새로운 이메일(`testuser@gmail.com`)로 자동 재검증 API 호출

**Playwright 코드 예시**:

```typescript
test("이메일 중복 실시간 검증 - 사용 가능", async ({ page }) => {
  const signupPage = new SignupPage(page);
  await page.goto("/signup");

  // Step 0 완료
  await signupPage.fillBasicInfo(generateUniqueUser());
  await signupPage.nextButton.click();

  // 이메일 입력
  const email = `test${Date.now()}@inha.edu`;
  const [local, domain] = email.split("@");

  await signupPage.emailLocalInput.fill(local);
  // 도메인 선택은 기본값 inha.edu 사용
  await signupPage.phoneNumberInput.click(); // 포커스 이동

  // API 호출 대기
  const response = await waitForApiResponse(
    page,
    "/api/v1/auth/password/check-email",
    200,
  );
  expect(response.url()).toContain(`email=${encodeURIComponent(email)}`);

  // 로딩 표시 확인
  await expect(page.getByText("확인 중...")).toBeVisible();
  await expect(page.getByText("확인 중...")).toBeHidden({ timeout: 3000 });

  // 성공 피드백 확인
  await expect(page.getByText("사용 가능한 이메일입니다.")).toBeVisible();
});

test("이메일 도메인 변경 시 재검증", async ({ page }) => {
  const signupPage = new SignupPage(page);
  await page.goto("/signup");

  // Step 0 완료
  await signupPage.fillBasicInfo(generateUniqueUser());
  await signupPage.nextButton.click();

  // 이메일 로컬파트 입력
  await signupPage.emailLocalInput.fill("testuser");
  await signupPage.phoneNumberInput.click(); // blur → inha.edu로 체크

  // 첫 번째 API 호출 대기
  await waitForApiResponse(page, "/api/v1/auth/password/check-email", 200);
  await expect(page.getByText("사용 가능한 이메일입니다.")).toBeVisible();

  // 도메인 변경
  await signupPage.emailDomainSelect.selectOption("gmail.com");

  // 두 번째 API 호출 대기 (새로운 이메일)
  const response = await waitForApiResponse(
    page,
    "/api/v1/auth/password/check-email",
    200,
  );
  expect(response.url()).toContain("email=testuser%40gmail.com");

  // 이전 메시지가 사라지고 새로 체크됨
  await expect(page.getByText("확인 중...")).toBeVisible();
  await expect(page.getByText("확인 중...")).toBeHidden({ timeout: 3000 });
});

test("커스텀 도메인 입력 시 이메일 검증", async ({ page }) => {
  const signupPage = new SignupPage(page);
  await page.goto("/signup");

  await signupPage.fillBasicInfo(generateUniqueUser());
  await signupPage.nextButton.click();

  // 이메일 로컬파트 입력
  await signupPage.emailLocalInput.fill("testuser");

  // 커스텀 도메인 선택
  await signupPage.emailDomainSelect.selectOption("custom");

  // 커스텀 도메인 입력 필드가 나타남
  const customDomainInput = page.locator('input[name="customDomain"]');
  await expect(customDomainInput).toBeVisible();

  // 커스텀 도메인 입력 및 blur
  await customDomainInput.fill("example.com");
  await signupPage.phoneNumberInput.click();

  // API 호출 확인
  const response = await waitForApiResponse(
    page,
    "/api/v1/auth/password/check-email",
    200,
  );
  expect(response.url()).toContain("email=testuser%40example.com");

  await expect(page.getByText("사용 가능한 이메일입니다.")).toBeVisible();
});
```

### 시나리오 3: 스텝 이동 시 중복 차단

**목적**: 중복된 학번/이메일이 있을 때 다음 스텝으로 이동이 차단되는지 확인

**테스트 단계**:

1. Step 0에서 중복된 학번 입력
2. "다음" 버튼 클릭
3. **예상 결과**:
   - Step 0에 머물러 있음 (Step 1로 이동 안 됨)
   - 학번 필드에 에러 메시지 표시
   - 중복 체크가 완료되지 않은 경우 자동으로 체크 트리거

**Playwright 코드 예시**:

```typescript
test("중복 학번으로 다음 스텝 이동 차단", async ({ page }) => {
  const signupPage = new SignupPage(page);
  await page.goto("/signup");

  // 중복된 학번으로 기본 정보 입력
  const userData = generateUniqueUser();
  userData.studentId = "12345678"; // 기존 사용자 학번

  await signupPage.fillBasicInfo(userData);

  // 중복 체크 완료 대기
  await waitForApiResponse(page, "/api/v1/auth/password/check-student-id", 409);

  // "다음" 버튼 클릭
  await signupPage.nextButton.click();

  // Step 0에 머물러 있는지 확인
  await expect(page.getByText("기본 정보")).toHaveClass(/text-primary/);
  await expect(signupPage.studentIdInput).toBeVisible();

  // 에러 메시지 표시 확인
  await expect(page.getByText("이미 가입된 학번입니다.")).toBeVisible();
});

test("중복 체크 미완료 시 다음 스텝 이동 트리거", async ({ page }) => {
  const signupPage = new SignupPage(page);
  await page.goto("/signup");

  const userData = generateUniqueUser();

  // 학번만 입력하고 blur 하지 않음 (체크 안 됨)
  await signupPage.studentIdInput.fill(userData.studentId);
  await signupPage.nameInput.fill(userData.name);
  // ... 나머지 필드 입력

  // "다음" 버튼 클릭 → 체크 트리거
  await signupPage.nextButton.click();

  // API 호출이 자동으로 발생하는지 확인
  const response = await waitForApiResponse(
    page,
    "/api/v1/auth/password/check-student-id",
    200,
  );
  expect(response.status()).toBe(200);

  // 체크 완료 후 자동으로 다음 스텝 이동
  await expect(page.getByText("연락처")).toHaveClass(/text-primary/, {
    timeout: 3000,
  });
});
```

### Manual 브라우저 테스트

자동화 테스트 외에도 다음 항목을 수동으로 확인:

1. **로딩 스피너 애니메이션**: 스피너가 부드럽게 회전하는지 확인
2. **성공 아이콘 색상**: 초록색 체크 아이콘이 명확히 보이는지
3. **에러 메시지 가독성**: 빨간색 텍스트가 읽기 쉬운지
4. **포커스 관리**: blur 이벤트가 자연스럽게 동작하는지
5. **네트워크 탭**: API 요청/응답이 올바른 형식인지 확인
6. **Console 탭**: 에러 없이 동작하는지

---

## 참고 자료

- [Playwright 공식 문서](https://playwright.dev/)
- [Page Object Model 가이드](https://playwright.dev/docs/pom)
- [Auth 마이그레이션 문서](../migration/auth-inquiries-orval-migration.md)
- [Issue #338: 회원가입 이메일/학번 검증 API 연결](https://github.com/Hoon-Inha-IGRUS/IGRUS-Web/issues/338)
