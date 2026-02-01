# AdminInquiries 페이지

> 작성일: 2026-01-30

## 개요

회원들의 문의사항을 확인하고 답변하는 관리자 페이지입니다. 검색, 필터링, 페이지네이션 기능을 제공하며 카드 형태로 문의 목록을 표시합니다.

## 파일 정보

- **경로**: `frontend/src/pages/admin/AdminInquiries.jsx`
- **라우트**: `/admin/inquiries`
- **권한**: OPERATOR 이상
- **레이아웃**: AdminLayout (사이드바 포함)

## 기능

### 1. 검색 기능

- 문의 제목 또는 작성자 이메일로 실시간 검색
- Search 아이콘이 포함된 입력 필드
- 검색 시 자동으로 첫 페이지로 이동

### 2. 필터 옵션

#### 상태 필터
- 모든 상태 (기본값)
- 답변 대기 (Pending)
- 답변 완료 (Answered)

### 3. 문의 목록 표시

Card 컴포넌트를 사용하여 다음 정보를 카드 형태로 표시합니다:
- 상태 표시 (dot + 텍스트)
  - Pending: 주황색 dot + "답변 대기"
  - Answered: 초록색 dot + "답변 완료"
- 문의 제목 (title)
- 작성자 이메일 (author)
- 작성 날짜 (date)
- 답변 버튼

### 4. 페이지네이션

- 페이지당 5개 표시
- 이전/다음 버튼
- 페이지 번호 버튼 (현재 페이지 강조)
- 필터 변경 시 자동으로 첫 페이지로 이동

### 5. 답변하기

- 각 문의 카드의 "답변하기" 또는 "답변 보기" 버튼 클릭
- handleReply 함수 호출
- Pending 상태: "답변하기" 버튼
- Answered 상태: "답변 보기" 버튼
- 현재는 alert로 알림 (TODO: 모달 구현 예정)

## 컴포넌트 구조

```
AdminInquiries
├── 페이지 헤더
│   ├── 제목 (h1)
│   └── 설명 (p)
└── 메인 컨테이너 (카드)
    ├── 검색 및 필터 섹션
    │   ├── 검색 입력 (Search 아이콘)
    │   └── 상태 필터 (select)
    ├── 결과 정보 (총 N개의 문의)
    ├── 문의 카드 목록
    │   └── 각 카드
    │       ├── 상태 배지 (dot + 텍스트)
    │       ├── 제목 (h4)
    │       ├── 작성자 및 날짜 (p)
    │       └── 답변 버튼
    └── 페이지네이션
        ├── 이전 버튼
        ├── 페이지 번호 버튼들
        └── 다음 버튼
```

## 사용된 컴포넌트 및 라이브러리

### UI 컴포넌트
- `@/components/ui/card` - 문의 카드 컴포넌트
- `@/components/ui/button` - 답변 버튼 및 페이지네이션 버튼
- `@/components/ui/input` - 검색 입력 필드

### 라이브러리
- `lucide-react` - Search 아이콘
- `react` - useState 훅

## 상태 관리

### Local State (useState)

| 상태 | 초기값 | 설명 |
|------|--------|------|
| searchQuery | '' | 검색어 |
| statusFilter | 'all' | 상태 필터 |
| currentPage | 1 | 현재 페이지 번호 |

### 샘플 데이터

```javascript
const sampleInquiries = [
  { id: '1', title: 'Question about dues', author: 'member@test.com', date: '2024-05-23', status: 'Pending' },
  { id: '2', title: 'Room reservation issue', author: 'active@test.com', date: '2024-05-22', status: 'Answered' },
  { id: '3', title: 'Event participation question', author: 'student@test.com', date: '2024-05-21', status: 'Pending' },
  { id: '4', title: 'Website feedback', author: 'user@test.com', date: '2024-05-20', status: 'Answered' },
];
```

## 주요 함수

### handleFilterChange
```javascript
const handleFilterChange = (setter) => (value) => {
  setter(value);
  setCurrentPage(1);
};
```
- 필터 변경 시 상태 업데이트 및 첫 페이지로 이동
- 고차 함수 패턴 사용

### handleReply
```javascript
const handleReply = (inquiry) => {
  console.log('Reply to inquiry:', inquiry);
  alert(`답변 기능 구현 예정: ${inquiry.title}`);
};
```
- 문의 답변 핸들러
- TODO: 모달 구현 예정

## CSS 및 디자인 토큰

### 색상 변수
- `bg-card` - 카드 배경
- `border-border` - 테두리
- `bg-background` - 선택 요소 배경
- `bg-accent` - 호버 상태
- `text-muted-foreground` - 보조 텍스트
- `bg-orange-500` - Pending 상태 dot
- `bg-green-500` - Answered 상태 dot

### 간격 토큰
- `space-y-s6` - 메인 컨테이너 세로 간격
- `space-y-s2` - 헤더 세로 간격
- `space-y-s4` - 카드 목록 세로 간격
- `gap-s4` - 검색/필터 간격
- `gap-s3` - 페이지네이션 간격
- `gap-s2` - 상태 배지 내부 간격
- `p-s6` - 카드 패딩
- `mb-s5` - 하단 마진 (큼)
- `mb-s4` - 하단 마진 (중간)
- `mb-s1` - 하단 마진 (작음)
- `mt-s5` - 상단 마진

### 타이포그래피 토큰
- `text-h1` - 페이지 제목
- `text-b1` - 카드 제목
- `text-b2` - 본문 텍스트
- `text-c1` - 캡션 텍스트

### 라운드 토큰
- `rounded-[2.5rem]` - 메인 컨테이너 모서리
- `rounded-r4` - 문의 카드 모서리
- `rounded-r3` - 버튼/입력 필드 모서리
- `rounded-full` - 상태 dot (원형)

### 애니메이션
- `animate-in fade-in duration-300` - 페이지 진입 애니메이션
- `transition-colors` - 선택 요소 색상 전환

## 반응형 디자인

### 모바일 (기본)
- 검색/필터 섹션: 세로 배치 (flex-col)
- 카드 레이아웃: 세로 쌓기

### 데스크탑 (lg 이상)
- 검색/필터 섹션: 가로 배치 (flex-row)
- 검색 입력: flex-1로 확장
- 카드 내부: 가로 배치 유지

## 원본 마이그레이션 정보

### 원본 컴포넌트
- `frontend/src/components/AdminView.jsx` - 'inquiries' 탭 부분

### 주요 변경사항
1. **컴포넌트 분리**: AdminView에서 독립된 페이지로 분리
2. **Card 컴포넌트 사용**: 재사용 가능한 Card UI 컴포넌트 활용
3. **검색 기능 추가**: 제목/작성자 실시간 검색
4. **필터 기능 추가**: 상태 필터
5. **페이지네이션 추가**: 5개씩 페이지 분할
6. **Props 제거**: theme prop 제거, CSS 변수 사용
7. **페이지 헤더 추가**: 제목과 설명 추가
8. **Button 컴포넌트 사용**: 재사용 가능한 Button UI 컴포넌트 활용
9. **상태 텍스트 한글화**: "Pending" → "답변 대기", "Answered" → "답변 완료"

### 변경 전후 비교

#### Before (AdminView.jsx - inquiries 탭)
```jsx
<div className="space-y-4">
  {[...].map((inq, i) => (
    <div key={i} className={`p-6 rounded-2xl border flex items-center justify-between ${
      isDark ? 'bg-white/5 border-white/5' : 'bg-gray-50 border-gray-100'
    }`}>
      <div>
        <div className="flex items-center gap-2 mb-1">
          <span className={`w-2 h-2 rounded-full ${inq.status === 'Pending' ? 'bg-orange-500' : 'bg-green-500'}`} />
          <span className="text-xs text-gray-500 font-bold uppercase">{inq.status}</span>
        </div>
        <h4 className="font-bold">{inq.title}</h4>
        <p className="text-xs text-gray-500 mt-1">From: {inq.author} • {inq.date}</p>
      </div>
      <button className="px-4 py-2 text-xs font-bold bg-[#03A69E] text-white rounded-lg">Reply</button>
    </div>
  ))}
</div>
```

#### After (AdminInquiries.jsx)
```jsx
<div className="space-y-s6 animate-in fade-in duration-300">
  {/* Page Header */}
  <div className="space-y-s2">
    <h1 className="text-h1">문의 관리</h1>
    <p className="text-b2 text-muted-foreground">
      회원들의 문의사항을 확인하고 답변할 수 있습니다.
    </p>
  </div>

  {/* Search and Filter Section */}
  <div className="p-s6 rounded-[2.5rem] border bg-card border-border shadow-sm">
    <div className="flex flex-col lg:flex-row gap-s4 mb-s5">
      {/* Search Input */}
      <div className="relative flex-1">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
        <Input
          type="text"
          placeholder="제목 또는 작성자로 검색..."
          value={searchQuery}
          onChange={(e) => {
            setSearchQuery(e.target.value);
            setCurrentPage(1);
          }}
          className="pl-10 rounded-r3"
        />
      </div>

      {/* Status Filter */}
      <select value={statusFilter} onChange={...}>...</select>
    </div>

    {/* Inquiry Cards */}
    <div className="space-y-s4">
      {paginatedInquiries.map((inquiry) => (
        <Card key={inquiry.id} className="p-s6 rounded-r4 border bg-white/5 dark:bg-white/5 border-border">
          <div className="flex items-center justify-between">
            <div className="flex-1">
              {/* Status Badge */}
              <div className="flex items-center gap-s2 mb-s1">
                <span className={`w-2 h-2 rounded-full ${
                  inquiry.status === 'Pending' ? 'bg-orange-500' : 'bg-green-500'
                }`} />
                <span className="text-c1 text-muted-foreground font-bold uppercase">
                  {inquiry.status === 'Pending' ? '답변 대기' : '답변 완료'}
                </span>
              </div>

              {/* Title */}
              <h4 className="font-bold text-b1 mb-s1">{inquiry.title}</h4>

              {/* Author and Date */}
              <p className="text-c1 text-muted-foreground">
                From: {inquiry.author} • {inquiry.date}
              </p>
            </div>

            {/* Reply Button */}
            <Button size="sm" onClick={() => handleReply(inquiry)} className="rounded-r3 font-bold">
              {inquiry.status === 'Pending' ? '답변하기' : '답변 보기'}
            </Button>
          </div>
        </Card>
      ))}
    </div>

    {/* Pagination */}
    {totalPages > 1 && (
      <div className="flex justify-center items-center gap-s3 mt-s5">
        <Button variant="outline" size="sm" onClick={...}>이전</Button>
        {/* Page number buttons */}
        <Button variant="outline" size="sm" onClick={...}>다음</Button>
      </div>
    )}
  </div>
</div>
```

## TODO

- [ ] 실제 API 연동하여 문의 데이터 가져오기
- [ ] 답변 작성 모달 구현
- [ ] 답변 보기 모달 구현
- [ ] 문의 상세 내용 표시
- [ ] 무한 스크롤 또는 서버 사이드 페이지네이션 고려
- [ ] 로딩 상태 처리
- [ ] 에러 상태 처리
- [ ] 정렬 기능 추가 (날짜, 상태 등)
- [ ] 일괄 답변 기능
- [ ] 문의 삭제 기능
- [ ] 답변 이메일 자동 발송 기능
- [ ] 문의 통계 정보 표시

## 관련 문서

- [AdminDashboard](./admin-dashboard.md)
- [AdminUsers](./admin-users.md)
- [Card 컴포넌트](../component-spec.md)
- [마이그레이션 상태](../migration-status.md)
- [페이지 디자인 현황](../page-design-status.md)
- [디자인 시스템](../design-system.md)
