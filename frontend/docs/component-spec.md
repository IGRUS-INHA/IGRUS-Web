# 프론트엔드 컴포넌트 명세

> 최종 업데이트: 2026-01-31

## 개요

IGRUS 웹사이트 프론트엔드의 공통 컴포넌트 구조와 명세를 정리한 문서입니다.

---

## 레이아웃 컴포넌트

### 1. Layout

**파일 경로:** `components/common/Layout.jsx`

**용도:** 일반 사용자 페이지 기본 레이아웃

**주요 기능:**
- React Router의 `Outlet`을 통한 하위 페이지 렌더링
- Sticky Header (모든 화면 크기에서 표시)
  - 좌측: 햄버거 메뉴(모바일만) + 동적 페이지 타이틀
  - 우측: 검색바(sm 이상) + Sign In 버튼(비로그인 시)
- Sidebar (모바일에서 토글 가능)
- 페이지별 동적 타이틀 표시 (location.pathname 기반)

**페이지 타이틀 매핑:**
- `/` → "Home"
- `/admin/*` → "Admin"
- `/board/*` → "Community"
- `/event/*` → "Events"
- `/inquiry/*` → "Inquiry"
- `/mypage/*` → "My Page"
- `/auth/*` → "Authentication"
- 기타 → "IGRUS"

**디자인 특징:**
- Sticky header: `sticky top-0 z-40`
- Backdrop blur 효과: `backdrop-blur-md`
- 반투명 배경: `bg-background/80`
- 검색바는 sm 이상에서만 표시 (`hidden sm:block`)
- Sign In 버튼은 비로그인 시에만 표시 (`isAuthenticated === false`)

**사용 예시:**
```jsx
<Route element={<Layout />}>
  <Route path="/" element={<HomePage />} />
  <Route path="/board/:boardType" element={<BoardListPage />} />
</Route>
```

**의존성:**
- `react-router-dom`: `Outlet`, `useLocation`, `useNavigate`
- `@/stores`: `useAuthStore`
- `@/components/common`: `Sidebar`, `SearchBar`
- `lucide-react`: `Menu` 아이콘

---

### 2. AdminLayout

**파일 경로:** `components/common/AdminLayout.jsx`

**용도:** 관리자 페이지 전용 레이아웃

**주요 기능:**
- 관리자 전용 사이드바 네비게이션
- React Router의 `Outlet`을 통한 하위 페이지 렌더링
- 관리자 정보 표시
- 모바일 반응형 사이드바 (토글 가능)

**네비게이션 메뉴:**
- 대시보드 (`/admin`)
- 회원 관리 (`/admin/users`)
- 준회원 관리 (`/admin/associates`)
- 문의 관리 (`/admin/inquiries`)
- 스크랩 관리 (`/admin/scraps`)

**디자인 특징:**
- IGRUS 디자인 시스템 준수
  - Border radius: `rounded-r3` (12px)
  - Primary color: `#03A69E`
  - CSS 변수 활용 (`bg-background`, `text-foreground`, `border-border` 등)
- 현재 활성 경로 하이라이트 (`useLocation()`)
- 사이드바 하단 액션
  - 메인으로 이동
  - 로그아웃

**사용 예시:**
```jsx
<Route element={<AdminLayout />}>
  <Route path="/admin" element={<AdminDashboard />} />
  <Route path="/admin/users" element={<AdminUsers />} />
  <Route path="/admin/associates" element={<AdminAssociates />} />
  <Route path="/admin/inquiries" element={<AdminInquiries />} />
  <Route path="/admin/scraps" element={<AdminScraps />} />
</Route>
```

**의존성:**
- `react-router-dom`: `Link`, `Outlet`, `useLocation`
- `@/stores`: `useAuthStore`, `useUIStore`
- `lucide-react`: 아이콘 컴포넌트

**권한 확인:**
- 사용자 정보는 `useAuthStore`에서 가져옴
- 관리자 페이지 접근은 `ProtectedRoute`와 함께 사용 권장

---

### 3. Sidebar

**파일 경로:** `components/common/Sidebar.jsx`

**용도:** 메인 애플리케이션 전역 사이드바

**주요 기능:**
- 메인 네비게이션 (홈, 게시판, 행사, 문의)
- 사용자 메뉴 (마이페이지, 관리자)
- 테마 토글 (다크/라이트 모드)
- 로그인/로그아웃
- 모바일 반응형 (backdrop과 함께 사용)

**디자인 특징:**
- 활성 메뉴 아이템 좌측에 primary 색상 강조선
- hover 시 아이콘 색상 변경
- 모바일에서는 backdrop과 함께 오버레이 형태로 표시

---

## UI 컴포넌트

### 1. Button

**파일 경로:** `components/ui/button.jsx`

**Variants:**
- `default`: Primary 버튼
- `secondary`: Secondary 버튼
- `outline`: 아웃라인 버튼
- `ghost`: 배경 없는 버튼
- `destructive`: 삭제/취소 등 위험한 동작

**Sizes:**
- `sm`: 작은 버튼
- `md`: 중간 버튼 (기본)
- `lg`: 큰 버튼

---

### 2. Input

**파일 경로:** `components/ui/input.jsx`

**특징:**
- IGRUS 디자인 시스템 스타일 적용
- 포커스 시 primary 색상 border
- placeholder 색상 자동 조정

---

### 3. Card

**파일 경로:** `components/ui/card.jsx`

**구성:**
- `Card`: 카드 컨테이너
- `CardHeader`: 카드 헤더
- `CardTitle`: 카드 제목
- `CardDescription`: 카드 설명
- `CardContent`: 카드 본문
- `CardFooter`: 카드 푸터

---

### 4. Spinner

**파일 경로:** `components/ui/spinner.jsx`

**용도:** 로딩 상태 표시

---

### 5. Toast

**파일 경로:** `components/ui/toast.jsx`

**용도:** 알림 메시지 표시

---

## Feature 컴포넌트

### Admin

**디렉토리:** `components/feature/admin/`

**컴포넌트:**
- `StatCard.jsx`: 통계 카드 (대시보드용)
- `UserTable.jsx`: 사용자 테이블 (회원 관리용)

---

### Auth

**디렉토리:** `components/feature/auth/`

**컴포넌트:**
- `AuthForm.jsx`: 로그인/회원가입 폼

---

### Board

**디렉토리:** `components/feature/board/`

**컴포넌트:**
- `PostCard.jsx`: 게시글 카드
- `PostListItem.jsx`: 게시글 리스트 아이템

---

### Event

**디렉토리:** `components/feature/event/`

**컴포넌트:**
- `EventCard.jsx`: 행사 카드

---

### Inquiry

**디렉토리:** `components/feature/inquiry/`

**컴포넌트:**
- `InquiryForm.jsx`: 문의 폼
- `InquiryListItem.jsx`: 문의 리스트 아이템

---

### MyPage

**디렉토리:** `components/feature/mypage/`

**컴포넌트:**
- `ProfileHeader.jsx`: 프로필 헤더
- `ActivityList.jsx`: 활동 내역 리스트
- `AppliedEventList.jsx`: 신청한 행사 리스트

---

## Board 유틸리티 컴포넌트

**디렉토리:** `components/board/`

**컴포넌트:**
- `Pagination.jsx`: 페이지네이션
- `SearchBar.jsx`: 검색바
- `SortSelect.jsx`: 정렬 선택
- `ReportModal.jsx`: 신고 모달

---

## 공통 컴포넌트

**디렉토리:** `components/common/`

**컴포넌트:**
- `Header.jsx`: 헤더
- `Footer.jsx`: 푸터
- `Layout.jsx`: 기본 레이아웃
- `AdminLayout.jsx`: 관리자 레이아웃
- `Sidebar.jsx`: 사이드바
- `SearchBar.jsx`: 검색바 (전역 검색)
- `ProtectedRoute.jsx`: 권한 보호 라우트

### SearchBar

**파일 경로:** `components/common/SearchBar.jsx`

**용도:** 전역 검색 UI (추후 검색 기능 연동 예정)

**주요 기능:**
- 좌측 Search 아이콘 표시
- Rounded-full 스타일
- 포커스 시 아이콘 색상 변경 (primary)
- 반응형 너비 조정
  - 기본: `w-40` (160px)
  - lg 이상: `lg:w-64` (256px)
  - 포커스 시 lg 이상: `focus:lg:w-80` (320px)

**디자인 특징:**
- Border: `border-border`
- 배경: `bg-background`
- 텍스트: `text-foreground`
- 포커스 border: `focus:border-primary/50`

**Props:**
- `className`: 추가 CSS 클래스 (선택사항)

**사용 예시:**
```jsx
import SearchBar from '@/components/common/SearchBar';

<SearchBar className="hidden sm:block" />
```

---

## 컴포넌트 임포트 규칙

### Alias 사용

```javascript
// UI 컴포넌트
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle } from '@/components/ui/card';

// 공통 컴포넌트
import { Layout, AdminLayout, ProtectedRoute } from '@/components/common';

// Store
import { useAuthStore, useUIStore } from '@/stores';
```

### 상대 경로 사용 금지

```javascript
// ❌ Bad
import Button from '../../components/ui/button';

// ✅ Good
import { Button } from '@/components/ui/button';
```

---

## 스타일링 가이드

### CSS 변수 사용

```jsx
// ✅ 권장
<div className="bg-background text-foreground border-border">

// ❌ 비권장 (하드코딩)
<div className="bg-white text-black border-gray-200">
```

### Border Radius

```jsx
// r1 = 4px, r2 = 8px, r3 = 12px, r4 = 16px, full = 100px
<div className="rounded-r3"> // 12px
<div className="rounded-r4"> // 16px
<div className="rounded-full"> // 100px
```

### Spacing

```jsx
// s1 = 4px, s2 = 8px, s3 = 12px, s4 = 16px, s5 = 24px, s6 = 32px, s7 = 48px, s8 = 56px
<div className="p-s5"> // padding: 24px
<div className="gap-s3"> // gap: 12px
```

---

## 관련 문서

- [디자인 시스템](./design-system.md)
- [페이지 디자인 현황](./page-design-status.md)
- [마이그레이션 가이드](./view-to-pages-migration.md)
