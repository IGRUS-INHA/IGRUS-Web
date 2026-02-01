# AdminUsers 페이지

> 작성일: 2026-01-30

## 개요

회원 정보를 조회하고 관리하는 관리자 페이지입니다. 검색, 필터링, 페이지네이션 기능을 제공하며 회원 정보를 CSV로 내보낼 수 있습니다.

## 파일 정보

- **경로**: `frontend/src/pages/admin/AdminUsers.jsx`
- **라우트**: `/admin/users`
- **권한**: OPERATOR 이상
- **레이아웃**: AdminLayout (사이드바 포함)

## 기능

### 1. 검색 기능

- 회원 이름 또는 학번으로 실시간 검색
- Search 아이콘이 포함된 입력 필드
- 검색 시 자동으로 첫 페이지로 이동

### 2. 필터 옵션

#### 상태 필터
- 모든 상태 (기본값)
- 활성 (Active)
- 정지 (Suspended)

#### 권한 필터
- 모든 권한 (기본값)
- 관리자 (Admin)
- 회원 (Member)

### 3. 회원 목록 표시

UserTable 컴포넌트를 사용하여 다음 정보를 표시합니다:
- 학번 (studentId)
- 이름 (name)
- 상태 (status) - 배지 형태로 표시
- 권한 (role)
- 작업 (수정 버튼)

### 4. 페이지네이션

- 페이지당 5명 표시
- 이전/다음 버튼
- 페이지 번호 버튼 (현재 페이지 강조)
- 필터 변경 시 자동으로 첫 페이지로 이동

### 5. 회원 수정

- 각 회원 행의 "수정" 버튼 클릭
- handleEdit 함수 호출
- 현재는 alert로 알림 (TODO: 모달 구현 예정)

### 6. CSV 내보내기

- "CSV 내보내기" 버튼 클릭
- 현재 필터링된 회원 목록을 CSV로 다운로드
- UTF-8 BOM 포함 (한글 지원)
- 파일명: `회원목록_YYYY-MM-DD.csv`
- 내보내는 열: 학번, 이름, 상태, 권한

## 컴포넌트 구조

```
AdminUsers
├── 페이지 헤더
│   ├── 제목 (h1)
│   └── 설명 (p)
└── 메인 컨테이너 (카드)
    ├── 검색 및 필터 섹션
    │   ├── 검색 입력 (Search 아이콘)
    │   ├── 상태 필터 (select)
    │   └── 권한 필터 (select)
    ├── 결과 정보 (총 N명의 회원)
    ├── UserTable 컴포넌트
    └── 페이지네이션
        ├── 이전 버튼
        ├── 페이지 번호 버튼들
        └── 다음 버튼
```

## 사용된 컴포넌트 및 라이브러리

### UI 컴포넌트
- `@/components/feature/admin/UserTable` - 회원 테이블 컴포넌트
- `@/components/ui/input` - 검색 입력 필드
- `@/components/ui/button` - 페이지네이션 버튼

### 라이브러리
- `lucide-react` - Search 아이콘
- `react` - useState 훅

## 상태 관리

### Local State (useState)

| 상태 | 초기값 | 설명 |
|------|--------|------|
| searchQuery | '' | 검색어 |
| statusFilter | 'all' | 상태 필터 |
| roleFilter | 'all' | 권한 필터 |
| currentPage | 1 | 현재 페이지 번호 |

### 샘플 데이터

```javascript
const sampleUsers = [
  { id: '1', studentId: '20230001', name: 'Kim Min-su', status: 'Active', role: 'Member' },
  { id: '2', studentId: '20210542', name: 'Lee Ha-na', status: 'Active', role: 'Admin' },
  { id: '3', studentId: '20240122', name: 'Park Jun-ho', status: 'Suspended', role: 'Member' },
  { id: '4', studentId: '20220315', name: 'Choi Young-ji', status: 'Active', role: 'Member' },
  { id: '5', studentId: '20230789', name: 'Jung Seo-jun', status: 'Active', role: 'Member' },
  { id: '6', studentId: '20210100', name: 'Kang Mi-young', status: 'Suspended', role: 'Member' },
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

### handleEdit
```javascript
const handleEdit = (user) => {
  console.log('Edit user:', user);
  alert(`편집 기능 구현 예정: ${user.name}`);
};
```
- 회원 정보 수정 핸들러
- TODO: 모달 구현 예정

### handleExport
```javascript
const handleExport = () => {
  // CSV 생성 및 다운로드
  const headers = ['학번', '이름', '상태', '권한'];
  const rows = filteredUsers.map((user) => [
    user.studentId,
    user.name,
    user.status === 'Active' ? '활성' : '정지',
    user.role,
  ]);

  const csvContent = [
    headers.join(','),
    ...rows.map((row) => row.join(',')),
  ].join('\n');

  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' });
  // ... 다운로드 로직
};
```
- 현재 필터링된 목록을 CSV로 변환
- UTF-8 BOM 포함 (\ufeff)
- 파일명에 날짜 포함

## CSS 및 디자인 토큰

### 색상 변수
- `bg-card` - 카드 배경
- `border-border` - 테두리
- `bg-background` - 선택 요소 배경
- `bg-accent` - 호버 상태
- `text-muted-foreground` - 보조 텍스트
- `text-primary` - 브랜드 색상

### 간격 토큰
- `space-y-s6` - 메인 컨테이너 세로 간격
- `space-y-s2` - 헤더 세로 간격
- `gap-s4` - 검색/필터 간격
- `gap-s3` - 필터 옵션 간격
- `gap-s2` - 페이지네이션 간격
- `p-s6` - 카드 패딩
- `mb-s5` - 하단 마진 (큼)
- `mb-s4` - 하단 마진 (중간)
- `mt-s5` - 상단 마진

### 타이포그래피 토큰
- `text-h1` - 페이지 제목
- `text-b2` - 본문 텍스트
- `text-c1` - 캡션 텍스트
- `text-sm` - 작은 텍스트

### 라운드 토큰
- `rounded-[2.5rem]` - 카드 모서리
- `rounded-r3` - 버튼/입력 필드 모서리
- `rounded-r2` - 배지 모서리

### 애니메이션
- `animate-in fade-in duration-300` - 페이지 진입 애니메이션
- `transition-colors` - 선택 요소 색상 전환
- `transition-all` - 버튼 전환

## 반응형 디자인

### 모바일 (기본)
- 검색/필터 섹션: 세로 배치 (flex-col)
- 필터 옵션: 가로 배치 유지

### 데스크탑 (lg 이상)
- 검색/필터 섹션: 가로 배치 (flex-row)
- 검색 입력: flex-1로 확장

## 원본 마이그레이션 정보

### 원본 컴포넌트
- `frontend/src/components/AdminView.jsx` - 'users' 탭 부분

### 주요 변경사항
1. **컴포넌트 분리**: AdminView에서 독립된 페이지로 분리
2. **UserTable 사용**: 테이블 로직을 재사용 가능한 컴포넌트로 분리
3. **검색 기능 추가**: 이름/학번 실시간 검색
4. **필터 기능 추가**: 상태/권한 필터
5. **페이지네이션 추가**: 5개씩 페이지 분할
6. **CSV 내보내기 추가**: 필터링된 목록 다운로드
7. **Props 제거**: theme prop 제거, CSS 변수 사용
8. **페이지 헤더 추가**: 제목과 설명 추가

### 변경 전후 비교

#### Before (AdminView.jsx - users 탭)
```jsx
<table className="w-full text-left">
  <thead>
    <tr className="text-xs text-gray-500 uppercase tracking-widest border-b border-white/5">
      <th className="pb-4 font-bold">Student ID</th>
      <th className="pb-4 font-bold">Name</th>
      <th className="pb-4 font-bold">Status</th>
      <th className="pb-4 font-bold">Role</th>
      <th className="pb-4 font-bold text-right">Actions</th>
    </tr>
  </thead>
  <tbody className="divide-y divide-gray-100 dark:divide-white/5">
    {[...].map((user, i) => (
      <tr key={i} className="group">
        <td className="py-4 text-sm font-medium">{user.id}</td>
        <td className="py-4 text-sm font-bold">{user.name}</td>
        <td className="py-4">
          <span className={`px-2 py-1 rounded-md text-[10px] font-bold ${
            user.status === 'Active' ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'
          }`}>
            {user.status}
          </span>
        </td>
        <td className="py-4 text-sm text-gray-500">{user.role}</td>
        <td className="py-4 text-right">
          <button className="text-[#03A69E] hover:underline text-xs font-bold">Edit</button>
        </td>
      </tr>
    ))}
  </tbody>
</table>
```

#### After (AdminUsers.jsx)
```jsx
<div className="space-y-s6 animate-in fade-in duration-300">
  {/* Page Header */}
  <div className="space-y-s2">
    <h1 className="text-h1">회원 관리</h1>
    <p className="text-b2 text-muted-foreground">
      동아리 회원 정보를 관리하고, 회원 상태 및 권한을 변경할 수 있습니다.
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
          placeholder="이름 또는 학번으로 검색..."
          value={searchQuery}
          onChange={(e) => {
            setSearchQuery(e.target.value);
            setCurrentPage(1);
          }}
          className="pl-10 rounded-r3"
        />
      </div>

      {/* Filter Options */}
      <div className="flex gap-s3">
        <select value={statusFilter} onChange={...}>...</select>
        <select value={roleFilter} onChange={...}>...</select>
      </div>
    </div>

    {/* User Table */}
    <UserTable users={paginatedUsers} title="" onEdit={handleEdit} onExport={handleExport} />

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

- [ ] 실제 API 연동하여 회원 데이터 가져오기
- [ ] 회원 수정 모달 구현
- [ ] 회원 삭제 기능 추가
- [ ] 무한 스크롤 또는 서버 사이드 페이지네이션 고려
- [ ] 로딩 상태 처리
- [ ] 에러 상태 처리
- [ ] 정렬 기능 추가 (학번, 이름, 상태 등)
- [ ] 일괄 작업 기능 (선택한 회원들에게 일괄 작업)
- [ ] 고급 검색 기능 (기수, 학과 등)
- [ ] Excel 내보내기 옵션 추가

## 관련 문서

- [AdminDashboard](./admin-dashboard.md)
- [UserTable 컴포넌트](../component-spec.md)
- [마이그레이션 상태](../migration-status.md)
- [페이지 디자인 현황](../page-design-status.md)
- [디자인 시스템](../design-system.md)
