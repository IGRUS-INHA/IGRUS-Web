# AdminDashboard 페이지

> 작성일: 2026-01-30

## 개요

관리자 대시보드 페이지로, 웹사이트의 주요 통계를 한눈에 보여주고 각 관리 메뉴로 빠르게 이동할 수 있는 허브 역할을 합니다.

## 파일 정보

- **경로**: `frontend/src/pages/admin/AdminDashboard.jsx`
- **라우트**: `/admin`
- **권한**: OPERATOR 이상
- **레이아웃**: AdminLayout (사이드바 포함)

## 기능

### 1. 통계 카드 섹션

4개의 StatCard 컴포넌트를 사용하여 주요 지표를 표시합니다.

| 통계 항목 | 아이콘 | 색상 | 설명 |
|----------|--------|------|------|
| Visitors | BarChart2 | blue-500 | 방문자 수 (2.4k) |
| Active Members | Users | primary | 활성 회원 수 (450) |
| Pending Inquiries | AlertCircle | orange-500 | 대기중인 문의 (8) |
| Today's Posts | FileText | purple-500 | 오늘 작성된 게시글 (24) |

### 2. 네비게이션 카드 섹션

4개의 관리 메뉴로 이동할 수 있는 카드를 제공합니다.

| 메뉴 | 아이콘 | 링크 | 설명 |
|------|--------|------|------|
| 회원 관리 | Users | `/admin/users` | 회원 정보 조회 및 관리 |
| 가입 승인 | UserCheck | `/admin/associates` | 신규 회원 가입 승인 관리 |
| 문의 관리 | MessageSquare | `/admin/inquiries` | 회원 문의사항 답변 및 관리 |
| 스크랩 관리 | Layers | `/admin/scraps` | 게시글 스크랩 통계 확인 |

## 컴포넌트 구조

```
AdminDashboard
├── 통계 카드 섹션 (grid, 1열 → 4열 반응형)
│   ├── StatCard (Visitors)
│   ├── StatCard (Active Members)
│   ├── StatCard (Pending Inquiries)
│   └── StatCard (Today's Posts)
└── 네비게이션 카드 섹션 (grid, 1열 → 2열 반응형)
    ├── Link 카드 (회원 관리)
    ├── Link 카드 (가입 승인)
    ├── Link 카드 (문의 관리)
    └── Link 카드 (스크랩 관리)
```

## 사용된 컴포넌트 및 라이브러리

### UI 컴포넌트
- `@/components/feature/admin/StatCard` - 통계 카드 컴포넌트

### 라이브러리
- `react-router-dom` - Link (네비게이션)
- `lucide-react` - 아이콘
  - BarChart2, Users, AlertCircle, FileText (통계 카드)
  - UserCheck, MessageSquare, Layers (네비게이션 카드)

### 상태 관리
- `@/stores` - useUIStore (테마 관리)

## CSS 및 디자인 토큰

### 색상 변수
- `bg-card` - 카드 배경
- `border-border` - 테두리
- `text-muted-foreground` - 보조 텍스트
- `text-primary` - 브랜드 색상

### 간격 토큰
- `space-y-s8` - 메인 컨테이너 세로 간격
- `gap-s6` - 그리드 아이템 간격
- `p-s6` - 카드 패딩
- `mb-s4`, `mb-s2` - 하단 마진

### 타이포그래피 토큰
- `text-h3` - 카드 제목
- `text-body` - 본문 텍스트

### 애니메이션
- `animate-in fade-in duration-300` - 페이지 진입 애니메이션
- `hover:scale-[1.02]` - 네비게이션 카드 호버 효과

## 반응형 디자인

### 모바일 (기본)
- 통계 카드: 1열 그리드
- 네비게이션 카드: 1열 그리드

### 태블릿 이상 (md 이상)
- 통계 카드: 4열 그리드
- 네비게이션 카드: 2열 그리드

## 원본 마이그레이션 정보

### 원본 컴포넌트
- `frontend/src/components/AdminView.jsx` - renderStats() 함수 부분

### 주요 변경사항
1. **Props 제거**: theme prop 제거, useUIStore() 사용
2. **컴포넌트화**: 통계 카드를 StatCard 컴포넌트로 분리
3. **네비게이션 추가**: 관리 메뉴 카드 추가 (원본에 없던 기능)
4. **CSS 변수**: 하드코딩된 색상/간격을 디자인 토큰으로 변경
5. **라우팅**: Link 컴포넌트 사용

### 변경 전후 비교

#### Before (AdminView.jsx)
```jsx
<div className={`p-8 rounded-[2.5rem] border ${
  isDark ? 'bg-[#1A1A1A] border-white/5' : 'bg-white border-gray-100 shadow-sm'
}`}>
  <div className={`${stat.color} mb-4`}>{stat.icon}</div>
  <p className="text-gray-500 text-xs font-bold uppercase tracking-widest mb-1">
    {stat.label}
  </p>
  <h3 className="text-3xl font-bold">{stat.value}</h3>
</div>
```

#### After (AdminDashboard.jsx)
```jsx
<StatCard
  label="Visitors"
  value="2.4k"
  icon={<BarChart2 size={24} />}
  colorClass="text-blue-500"
/>
```

## TODO

- [ ] 실제 API 연동하여 통계 데이터 가져오기
- [ ] 실시간 업데이트 (WebSocket 또는 폴링)
- [ ] 로딩 상태 처리
- [ ] 에러 상태 처리
- [ ] 통계 기간 선택 기능 추가 (오늘/이번 주/이번 달)
- [ ] 차트 추가 (선택적)

## 관련 문서

- [마이그레이션 상태](../migration-status.md)
- [페이지 디자인 현황](../page-design-status.md)
- [디자인 시스템](../design-system.md)
