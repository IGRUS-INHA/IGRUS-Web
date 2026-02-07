# 프론트엔드 컴포넌트 명세

> 최종 업데이트: 2026-02-02

## 개요

IGRUS 웹사이트 프론트엔드의 공통 컴포넌트 구조와 명세를 정리한 문서입니다.

---

## 레이아웃 컴포넌트

### 1. Layout

**파일 경로:** `components/common/Layout.tsx` (TypeScript ✓)

**용도:** 일반 사용자 페이지 기본 레이아웃 (2026-02-02 업데이트)

**주요 기능:**
- ✅ **Sidebar 통합** (mass-club-portal 디자인 기반)
  - Desktop: 항상 표시 (sticky 포지션)
  - Mobile/Tablet: 토글 가능 (fixed 포지션, backdrop 포함)
  - 햄버거 메뉴 버튼으로 열고 닫기
- ✅ **Header 통합**
  - 상단 고정 헤더
  - 모바일: 햄버거 메뉴 토글 버튼 포함
  - 네비게이션 메뉴 (공지사항, 자유게시판, 정보공유, 행사)
  - 로그인/회원가입 또는 사용자 정보 표시
- ✅ **React Router Outlet**
  - 하위 페이지 렌더링
  - 중앙 정렬, 최대 너비 제한 (max-w-7xl)

**레이아웃 구조:**
```
┌─────────────────────────────────────┐
│ Sidebar (좌측 고정)                  │
│  - 로고 (IGRUS)                     │
│  - 메인 네비게이션                   │
│    • 홈                             │
│    • 게시판                         │
│    • 행사                           │
│    • 문의                           │
│  - 테마 토글                        │
│  - 마이페이지/로그인                 │
│  - 관리자 (권한 있을 시)             │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ Header (상단 고정)                   │
│  - 햄버거 메뉴 (모바일)              │
│  - 네비게이션 (Desktop)              │
│  - 로그인/회원가입 (우측)            │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ Main Content                        │
│  - Outlet (페이지 내용)              │
└─────────────────────────────────────┘
```

**디자인 특징:**
- Flexbox 레이아웃 (수평: Sidebar + Main)
- Sidebar
  - Desktop (lg+): `sticky top-0`, 항상 표시
  - Mobile/Tablet: `fixed`, backdrop 포함, 토글 가능
  - 다크모드 지원
- Header
  - 상단 고정
  - 햄버거 메뉴 버튼 (모바일, `lg:hidden`)
  - 네비게이션 메뉴 (Desktop, `hidden md:flex`)
- Main Content
  - `flex-1` (나머지 공간 차지)
  - `overflow-auto` (스크롤 가능)
  - 중앙 정렬 컨테이너 (`max-w-7xl mx-auto`)

**사용 예시:**
```jsx
<Route element={<Layout />}>
  <Route path="/" element={<HomePage />} />
  <Route path="/board/:boardType" element={<BoardListPage />} />
</Route>
```

**의존성:**
- `react-router-dom`: `Outlet`
- `@/stores`: `useUIStore` (사이드바 상태 관리)
- `@/components/common`: `Sidebar`, `Header`

---

### 2. AdminLayout

**파일 경로:** `components/common/AdminLayout.tsx` (TypeScript ✓)

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

**파일 경로:** `components/common/Sidebar.tsx` (TypeScript ✓)

**용도:** 메인 애플리케이션 전역 사이드바 (mass-club-portal 디자인 기반)

**주요 기능:**
- **메인 네비게이션**
  - 홈 (`/`)
  - 게시판 (`/board/notices`)
  - 행사 (`/events`)
  - 문의 (`/inquiry`)
- **사용자 메뉴**
  - 로그인 여부에 따라 다른 UI
    - 비로그인: 로그인 링크 (`/login`)
    - 로그인: 마이페이지 (`/mypage`)
  - 관리자 권한 있을 시: 관리자 페이지 (`/admin`)
- **테마 토글**
  - 다크/라이트 모드 전환
  - 아이콘: Sun (다크모드), Moon (라이트모드)
- **모바일 반응형**
  - Desktop (lg+): sticky, 항상 표시
  - Mobile/Tablet: fixed, backdrop 포함, 토글 가능

**Props:**
```typescript
interface SidebarProps {
  isOpen: boolean;      // 사이드바 열림 상태
  onClose?: () => void; // 닫기 핸들러
}
```

**디자인 특징:**
- 활성 메뉴 아이템 좌측에 primary 색상 강조선 (`w-1 h-6 bg-primary rounded-r-full`)
- hover 시 아이콘 색상 primary로 변경
- 모바일에서는 backdrop과 함께 오버레이 형태로 표시
- 로고: IGRUS + Code 아이콘
- 닫기 버튼 (X): 모바일/태블릿에서만 표시 (`lg:hidden`)

**상태 관리:**
- `useUIStore`의 `theme`, `toggleTheme` 사용
- `useAuthStore`의 `user`, `isAuthenticated` 사용
- 사이드바 열림/닫힘 상태는 부모 컴포넌트(Layout)에서 관리

---

## UI 컴포넌트

### 1. Button

**파일 경로:** `components/ui/button.tsx` (TypeScript ✓)

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

**파일 경로:** `components/ui/input.tsx` (TypeScript ✓)

**특징:**
- IGRUS 디자인 시스템 스타일 적용
- 포커스 시 primary 색상 border
- placeholder 색상 자동 조정

---

### 3. Card

**파일 경로:** `components/ui/card.tsx` (TypeScript ✓)

**구성:**
- `Card`: 카드 컨테이너
- `CardHeader`: 카드 헤더
- `CardTitle`: 카드 제목
- `CardDescription`: 카드 설명
- `CardContent`: 카드 본문
- `CardFooter`: 카드 푸터

---

### 4. Spinner

**파일 경로:** `components/ui/spinner.tsx` (TypeScript ✓)

**용도:** 로딩 상태 표시

---

### 5. Toast

**파일 경로:** `components/ui/toast.tsx` (TypeScript ✓)

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
- `Header.tsx`: 헤더 (2026-02-02 업데이트)
- `Footer.jsx`: 푸터
- `Layout.tsx`: 기본 레이아웃 (2026-02-02 업데이트)
- `AdminLayout.tsx`: 관리자 레이아웃
- `Sidebar.tsx`: 사이드바 (2026-02-02 업데이트)
- `SearchBar.tsx`: 검색바 (전역 검색)
- `ProtectedRoute.jsx`: 권한 보호 라우트

### Header

**파일 경로:** `components/common/Header.tsx` (TypeScript ✓)

**용도:** 상단 고정 헤더 (2026-02-02 업데이트)

**주요 기능:**
- **모바일 메뉴 토글**
  - 햄버거 메뉴 버튼 (`Menu` 아이콘)
  - Desktop에서는 숨김 (`lg:hidden`)
  - 클릭 시 `useUIStore().toggleSidebar()` 호출
- **로고**
  - IGRUS 텍스트
  - 모바일에서만 표시 (`lg:hidden`)
- **네비게이션 메뉴** (Desktop)
  - 공지사항 (`/board/notices`)
  - 자유게시판 (`/board/general`)
  - 정보공유 (`/board/insight`)
  - 행사 (`/events`)
  - Desktop에서만 표시 (`hidden md:flex`)
- **사용자 액션**
  - 로그인 상태:
    - 사용자 이름 + 역할 표시 (`sm:inline`으로 반응형)
    - 마이페이지 버튼
  - 비로그인 상태:
    - 로그인 버튼
    - 회원가입 버튼 (`hidden sm:inline`으로 반응형)

**디자인 특징:**
- 상단 고정: `border-b bg-background`
- 컨테이너: `container mx-auto px-4 h-16`
- Flexbox 레이아웃: `flex items-center justify-between`
- 반응형 표시/숨김
  - 햄버거 메뉴: 모바일에서만 (`lg:hidden`)
  - 네비게이션: Desktop에서만 (`hidden md:flex`)
  - 사용자 이름: 작은 화면에서 숨김 (`hidden sm:inline`)
  - 회원가입 버튼: 작은 화면에서 숨김 (`hidden sm:inline`)

**상태 관리:**
- `useAuthStore`의 `user`, `isAuthenticated` 사용
- `useUIStore`의 `toggleSidebar` 사용

**의존성:**
- `react-router-dom`: `Link`
- `@/stores`: `useAuthStore`, `useUIStore`
- `@/components/ui/button`: `Button`
- `@/constants`: `ROLE_LABELS`
- `lucide-react`: `Menu` 아이콘

### SearchBar

**파일 경로:** `components/common/SearchBar.tsx` (TypeScript ✓)

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
