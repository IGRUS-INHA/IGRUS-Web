# AdminScraps 페이지

> 작성일: 2026-01-30

## 개요

스크랩 데이터를 관리하는 관리자 페이지입니다. 사용자들이 스크랩한 게시글 목록을 확인하고, 검색/필터링하며, 필요시 삭제할 수 있는 기능을 제공합니다.

**최종 업데이트**: 2026-01-30 (AdminView 탭에서 독립 페이지로 마이그레이션 완료)

## 파일 정보

- **경로**: `frontend/src/pages/admin/AdminScraps.jsx`
- **라우트**: `/admin/scraps`
- **권한**: OPERATOR 이상
- **레이아웃**: AdminLayout (사이드바 포함)

## 기능

### 1. 통계 카드 섹션

스크랩 관련 주요 지표를 3개의 카드로 표시합니다.

| 통계 항목 | 아이콘 | 색상 | 설명 |
|----------|--------|------|------|
| 총 스크랩 | Bookmark | primary | 전체 스크랩 수 |
| 가장 많은 게시판 | TrendingUp | orange-500 | 스크랩이 가장 많은 게시판 (예: 자유게시판) |
| 최근 스크랩 | Clock | blue-500 | 최근 24시간 내 스크랩 수 |

### 2. 스크랩 목록

#### 검색 및 필터 기능
- **검색**: 게시글 제목 또는 작성자 이메일로 검색
- **게시판 필터**: 전체 / 공지사항 / 자유게시판 / 질문게시판 / 스터디게시판
- 실시간 필터링 (입력/선택 시 즉시 반영)
- 검색/필터 변경 시 페이지가 자동으로 1페이지로 리셋됨

#### 스크랩 카드
각 스크랩 항목은 Card 컴포넌트로 표시되며 다음 정보를 포함:
- **게시판 타입 배지**: primary 색상, 대문자 표시 (예: "FREE")
- **게시글 제목**: line-clamp-1로 한 줄 표시
- **메타 정보**:
  - 작성자 이메일
  - 스크랩한 사용자 이메일
  - 스크랩 날짜
- **바로가기 버튼**: 해당 게시글 상세 페이지로 이동

#### 페이지네이션
- 페이지당 5개 항목 표시
- 이전/다음 버튼 및 페이지 번호 버튼 제공 (최대 5개 페이지 번호 표시)
- 현재 페이지 정보 표시 (예: "총 10개의 스크랩 | 페이지 1 / 2")
- 총 페이지가 1페이지일 경우 페이지네이션 숨김

### 3. 상태 처리

#### Empty State
스크랩이 없거나 검색/필터 결과가 없을 때:
- 중앙 정렬된 메시지
- "문의가 없습니다." 텍스트 (text-muted-foreground)

#### 바로가기 기능
- 각 스크랩 카드의 "바로가기" 버튼 클릭 시
- 해당 게시글 상세 페이지로 이동: `/board/{boardType}/{postId}`
- Link 컴포넌트 사용

## 컴포넌트 구조

```
AdminScraps
├── Header Section
│   ├── h1: "스크랩 관리"
│   └── p: 설명
├── Stats Cards (grid, 1열 → 3열 반응형)
│   ├── Card (총 스크랩)
│   ├── Card (가장 많은 게시판)
│   └── Card (최근 스크랩)
└── Main Content Card
    ├── Search & Filter Section
    │   ├── Search Input (Search 아이콘)
    │   └── Board Filter (select)
    ├── Result Info (총 N개의 스크랩 | 페이지 X / Y)
    ├── Scrap Cards (조건부)
    │   ├── Empty State (조건부)
    │   └── Card (각 스크랩)
    │       ├── Board Badge
    │       ├── Post Title
    │       ├── Meta Info (작성자, 스크랩한 사람, 날짜)
    │       └── View Button
    └── Pagination (페이지 > 1일 때)
        ├── Previous Button
        ├── Page Number Buttons
        └── Next Button
```

## 사용된 컴포넌트 및 라이브러리

### UI 컴포넌트
- `@/components/ui/card` - Card
- `@/components/ui/button` - Button (바로가기, 페이지네이션)
- `@/components/ui/input` - Input (검색)

### 라이브러리
- `lucide-react` - 아이콘
  - Search (검색 아이콘)
  - Bookmark (총 스크랩 아이콘)
  - TrendingUp (가장 많은 게시판 아이콘)
  - Clock (최근 스크랩 아이콘)
- `react-router-dom` - Link (바로가기 링크)

### 상태 관리
- `useState` - 스크랩 목록, 검색어, 게시판 필터, 현재 페이지 관리

## CSS 및 디자인 토큰

### 색상 변수
- `bg-card` - 카드 배경
- `bg-background` - 페이지 배경
- `border-border` - 테두리
- `text-foreground` - 메인 텍스트
- `text-muted-foreground` - 보조 텍스트
- `text-primary` - 브랜드 색상 (총 스크랩, 게시판 배지, 바로가기 버튼)
- `bg-primary/20` - 게시판 배지 배경
- `text-orange-500` - 가장 많은 게시판 아이콘
- `text-blue-500` - 최근 스크랩 아이콘
- `hover:border-primary/30` - 스크랩 카드 호버 테두리

### 간격 토큰
- `space-y-s6` - 메인 섹션 간격
- `space-y-s2` - 헤더 섹션 간격
- `gap-s4` - 그리드 아이템 간격, 검색/필터 간격, 스크랩 카드 내부 간격
- `gap-s3` - 메타 정보 간격, 페이지네이션 간격
- `gap-s2` - 버튼 간격
- `p-s6` - 카드 패딩
- `p-s5` - 통계 카드 패딩

### 타이포그래피 토큰
- `text-h1` - 페이지 제목
- `text-h2` - 통계 숫자
- `text-b1` - 게시글 제목
- `text-b2` - 설명 텍스트
- `text-c1` - 메타 정보

### 애니메이션
- `animate-in fade-in duration-300` - 페이지 진입 애니메이션
- `transition-all` - 스크랩 카드 호버 트랜지션

## 반응형 디자인

### 모바일 (기본)
- 통계 카드: 1열 그리드
- 검색/필터: 세로 레이아웃 (flex-col)
- 스크랩 카드: 전체 너비

### 태블릿 이상 (md 이상)
- 통계 카드: 3열 그리드

### 데스크탑 (lg 이상)
- 검색/필터: 가로 레이아웃 (flex-row)

## 샘플 데이터

```javascript
const sampleScraps = [
  {
    id: 1,
    postId: 'post-1',
    postTitle: '첫 번째 게시글 제목입니다',
    board: 'FREE',
    author: 'user1@test.com',
    scrappedBy: 'user5@test.com',
    scrappedAt: '2026-01-29 14:30',
  },
  {
    id: 2,
    postId: 'post-2',
    postTitle: '두 번째 게시글 제목',
    board: 'NOTICE',
    author: 'admin@test.com',
    scrappedBy: 'user3@test.com',
    scrappedAt: '2026-01-29 10:15',
  },
  {
    id: 3,
    postId: 'post-3',
    postTitle: '질문 있습니다',
    board: 'QUESTION',
    author: 'user2@test.com',
    scrappedBy: 'user4@test.com',
    scrappedAt: '2026-01-28 16:45',
  },
  {
    id: 4,
    postId: 'post-4',
    postTitle: '스터디 모집합니다',
    board: 'STUDY',
    author: 'user6@test.com',
    scrappedBy: 'user1@test.com',
    scrappedAt: '2026-01-28 09:20',
  },
  {
    id: 5,
    postId: 'post-5',
    postTitle: '안녕하세요 반갑습니다',
    board: 'FREE',
    author: 'user7@test.com',
    scrappedBy: 'user2@test.com',
    scrappedAt: '2026-01-27 18:00',
  },
  {
    id: 6,
    postId: 'post-6',
    postTitle: '중요 공지사항',
    board: 'NOTICE',
    author: 'admin@test.com',
    scrappedBy: 'user8@test.com',
    scrappedAt: '2026-01-27 11:30',
  },
];
```

## TODO

- [ ] 실제 API 연동하여 스크랩 데이터 가져오기
- [x] 페이지네이션 추가 (완료 - 2026-01-30)
- [x] 게시판별 필터 추가 (완료 - 2026-01-30)
- [x] 스크랩된 게시글로 이동하는 링크 추가 (완료 - 2026-01-30)
- [x] 통계 카드 추가 (완료 - 2026-01-30)
- [ ] 정렬 기능 (스크랩 수 / 최신순)
- [ ] 기간별 필터 추가 (최근 7일 / 30일 / 전체)
- [ ] 로딩 상태 처리
- [ ] 에러 상태 처리
- [ ] 스크랩 삭제 기능
- [ ] 일괄 삭제 기능 (체크박스)
- [ ] CSV 내보내기 기능
- [ ] 사용자별 스크랩 통계
- [ ] 인기 게시글 Top 10

## 구현 상태

- **기본 구조**: ✅ 완료 (2026-01-30)
- **통계 카드**: ✅ 완료 (2026-01-30)
- **검색 기능**: ✅ 완료 (2026-01-30)
- **필터 기능**: ✅ 완료 (2026-01-30)
- **페이지네이션**: ✅ 완료 (2026-01-30)
- **바로가기 링크**: ✅ 완료 (2026-01-30)
- **AdminLayout 적용**: ✅ 완료 (2026-01-30)
- **API 연동**: ⏳ 대기 중

## 관련 문서

- [관리자 대시보드](./admin-dashboard.md)
- [페이지 디자인 현황](../page-design-status.md)
- [디자인 시스템](../design-system.md)
