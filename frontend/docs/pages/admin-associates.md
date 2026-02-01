# AdminAssociates 페이지

> 작성일: 2026-01-30

## 개요

준회원(가입 신청자) 승인/거절 관리 페이지입니다. 관리자가 가입 신청자 목록을 확인하고 승인 또는 거절할 수 있습니다.

## 파일 정보

- **경로**: `frontend/src/pages/admin/AdminAssociates.jsx`
- **라우트**: `/admin/associates`
- **권한**: ADMIN
- **레이아웃**: AdminLayout (사이드바 포함)

## 기능

### 1. 페이지 헤더

- 제목: "준회원 승인"
- 설명: "가입 신청자를 검토하고 승인 또는 거절할 수 있습니다."

### 2. 신청자 목록

각 신청자는 카드 형태로 표시되며 다음 정보를 포함합니다.

| 필드 | 설명 | 표시 형식 |
|------|------|----------|
| 이름 | 신청자 이름 | 굵은 글씨 |
| 학번 | 신청자 학번 | 괄호 안에 회색으로 표시 |
| 신청일 | 가입 신청 날짜 | "신청일: YYYY-MM-DD" |
| 자기소개 | 신청자가 작성한 자기소개 | 일반 텍스트 |

### 3. 액션 버튼

각 카드 우측에 2개의 아이콘 버튼이 배치됩니다.

| 버튼 | 아이콘 | 색상 | 기능 |
|------|--------|------|------|
| 승인 | CheckCircle | green-500 | 신청자를 정회원으로 승인 |
| 거절 | XCircle | red-500 | 신청 거절 |

### 4. Empty State

신청자가 없을 때 표시되는 빈 상태 화면:
- CheckCircle 아이콘 (48px, 투명도 20%)
- "현재 승인 대기 중인 신청자가 없습니다." 메시지

## 컴포넌트 구조

```
AdminAssociates
├── 페이지 헤더
│   ├── h1 (제목)
│   └── p (설명)
└── 신청자 목록 (조건부 렌더링)
    ├── 신청자 있음
    │   └── Card (각 신청자)
    │       ├── 정보 영역
    │       │   ├── 이름 + 학번
    │       │   ├── 신청일
    │       │   └── 자기소개
    │       └── 액션 버튼 영역
    │           ├── 승인 버튼
    │           └── 거절 버튼
    └── 신청자 없음 (Empty State)
        └── Card (빈 상태 메시지)
```

## 사용된 컴포넌트 및 라이브러리

### UI 컴포넌트
- `@/components/ui/card` - Card (신청자 카드 및 빈 상태 카드)
- `@/components/ui/button` - Button (액션 버튼)

### 라이브러리
- `lucide-react` - 아이콘
  - CheckCircle (승인 버튼, 빈 상태)
  - XCircle (거절 버튼)

### 상태 관리
- `useState` - 신청자 목록 상태 관리

## CSS 및 디자인 토큰

### 색상 변수
- `text-muted-foreground` - 보조 텍스트 (학번, 신청일)
- `text-foreground/80` - 자기소개 텍스트
- `text-green-500` - 승인 버튼
- `hover:bg-green-500/10` - 승인 버튼 호버 배경
- `text-red-500` - 거절 버튼
- `hover:bg-red-500/10` - 거절 버튼 호버 배경

### 간격 토큰
- `space-y-6` - 메인 컨테이너 세로 간격
- `space-y-4` - 카드 간 간격
- `mb-2` - 제목 하단 마진
- `mt-1` - 신청일 상단 마진
- `mt-2` - 자기소개 상단 마진
- `ml-4` - 버튼 영역 좌측 마진
- `gap-2` - 버튼 간 간격

### 타이포그래피
- `text-2xl` - 페이지 제목
- `text-sm` - 페이지 설명, 자기소개
- `text-xs` - 학번, 신청일
- `font-bold` - 신청자 이름
- `font-normal` - 학번

### 애니메이션
- `animate-in fade-in duration-300` - 페이지 진입 애니메이션
- `transition-all hover:shadow-md` - 카드 호버 효과

## 샘플 데이터

```javascript
const sampleAssociates = [
  { id: '20249999', name: 'New Student 1', date: '2024-05-25', intro: 'Hi, I love coding!' },
  { id: '20248888', name: 'New Student 2', date: '2024-05-24', intro: 'Interested in UI/UX.' },
];
```

## 이벤트 핸들러

### handleApprove(associateId)
- **기능**: 신청자를 정회원으로 승인
- **파라미터**: `associateId` - 신청자 학번
- **동작**:
  1. 콘솔에 로그 출력
  2. 목록에서 해당 신청자 제거 (임시 구현)
- **TODO**: 승인 API 호출 구현 필요

### handleReject(associateId)
- **기능**: 신청 거절
- **파라미터**: `associateId` - 신청자 학번
- **동작**:
  1. 콘솔에 로그 출력
  2. 목록에서 해당 신청자 제거 (임시 구현)
- **TODO**: 거절 API 호출 구현 필요

## 원본 마이그레이션 정보

### 원본 컴포넌트
- `frontend/src/components/AdminView.jsx` - 'approvals' 탭 부분

### 주요 변경사항
1. **Props 제거**: theme prop 제거, CSS 변수 사용
2. **컴포넌트 사용**: Card, Button 컴포넌트 활용
3. **상태 관리**: useState로 신청자 목록 관리
4. **페이지 구조**: 헤더 섹션 추가 (제목 + 설명)
5. **Empty State**: 빈 상태 처리 추가
6. **이벤트 핸들러**: 승인/거절 함수 분리

### 변경 전후 비교

#### Before (AdminView.jsx)
```jsx
<div className={`p-6 rounded-2xl border flex items-center justify-between ${
  isDark ? 'bg-white/5 border-white/5' : 'bg-gray-50 border-gray-100'
}`}>
  <div>
    <h4 className="font-bold">{app.name} <span className="text-gray-500 text-xs font-normal">({app.id})</span></h4>
    <p className="text-xs text-gray-500 mt-1">{app.intro}</p>
  </div>
  <div className="flex gap-2">
    <button className="p-2 text-green-500 hover:bg-green-500/10 rounded-lg transition"><CheckCircle size={20} /></button>
    <button className="p-2 text-red-500 hover:bg-red-500/10 rounded-lg transition"><XCircle size={20} /></button>
  </div>
</div>
```

#### After (AdminAssociates.jsx)
```jsx
<Card
  key={associate.id}
  className="p-6 flex items-center justify-between transition-all hover:shadow-md"
>
  <div className="flex-1">
    <h4 className="font-bold">
      {associate.name}{' '}
      <span className="text-muted-foreground text-xs font-normal">
        ({associate.id})
      </span>
    </h4>
    <p className="text-xs text-muted-foreground mt-1">
      신청일: {associate.date}
    </p>
    <p className="text-sm text-foreground/80 mt-2">
      {associate.intro}
    </p>
  </div>

  <div className="flex gap-2 ml-4">
    <Button
      size="icon"
      variant="ghost"
      className="text-green-500 hover:bg-green-500/10 hover:text-green-500"
      onClick={() => handleApprove(associate.id)}
      title="승인"
    >
      <CheckCircle size={20} />
    </Button>
    <Button
      size="icon"
      variant="ghost"
      className="text-red-500 hover:bg-red-500/10 hover:text-red-500"
      onClick={() => handleReject(associate.id)}
      title="거절"
    >
      <XCircle size={20} />
    </Button>
  </div>
</Card>
```

## TODO

- [ ] 실제 API 연동
  - [ ] GET `/api/admin/associates` - 신청자 목록 조회
  - [ ] POST `/api/admin/associates/:id/approve` - 승인 처리
  - [ ] POST `/api/admin/associates/:id/reject` - 거절 처리
- [ ] 로딩 상태 처리 (스피너)
- [ ] 에러 상태 처리 (에러 메시지)
- [ ] 승인/거절 확인 모달 추가
- [ ] 거절 시 사유 입력 기능
- [ ] 상세보기 모달 추가 (전체 자기소개, 연락처 등)
- [ ] 일괄 승인/거절 기능 (체크박스)
- [ ] 토스트 알림 추가 (승인/거절 완료 시)
- [ ] 페이지네이션 추가 (신청자가 많을 경우)
- [ ] 검색/필터 기능 (이름, 학번, 신청일 기준)

## 관련 문서

- [관리자 대시보드](./admin-dashboard.md)
- [마이그레이션 상태](../migration-status.md)
- [페이지 디자인 현황](../page-design-status.md)
- [디자인 시스템](../design-system.md)
