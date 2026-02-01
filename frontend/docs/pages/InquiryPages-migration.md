# Inquiry Pages 마이그레이션

> 완료일: 2026-01-30

## 개요

`frontend/src/components/SupportView.jsx`를 `frontend/src/pages/inquiry/` 디렉토리의 2개 페이지로 마이그레이션했습니다.

## 마이그레이션 상세

### 원본 파일
- `frontend/src/components/SupportView.jsx`

### 대상 파일
1. `frontend/src/pages/inquiry/InquiryPage.jsx` - 비로그인 사용자용 문의 작성 페이지
2. `frontend/src/pages/inquiry/InquiryLookupPage.jsx` - 문의번호/비밀번호로 문의 조회 페이지

### 영향받은 컴포넌트
- `frontend/src/components/feature/inquiry/InquiryForm.jsx` - theme prop 제거, CSS 변수 사용
- `frontend/src/components/feature/inquiry/InquiryListItem.jsx` - theme prop 제거, CSS 변수 사용

## 주요 변경사항

### 1. 구조 변경: 탭 UI → 별도 페이지

**Before (SupportView.jsx)**
```jsx
const SupportView = ({ theme }) => {
  const [view, setView] = useState('form'); // 탭 전환

  return (
    <div>
      {/* 탭 버튼 */}
      <button onClick={() => setView('form')}>New Inquiry</button>
      <button onClick={() => setView('history')}>View History</button>

      {/* 조건부 렌더링 */}
      {view === 'form' ? <Form /> : <History />}
    </div>
  );
};
```

**After (별도 페이지)**
- `/inquiry` - InquiryPage (문의 작성)
- `/inquiry/lookup` - InquiryLookupPage (문의 조회)

### 2. InquiryPage - 문의 작성 페이지

#### Props 제거
```jsx
// Before
const SupportView = ({ theme }) => {
  const isDark = theme === 'dark';
}

// After
export default function InquiryPage() {
  // theme prop 없음
}
```

#### 주요 기능
1. **비로그인 사용자용 문의 작성**
   - 문의 유형, 제목, 내용, 비밀번호 입력
   - React Query `useCreateInquiry()` 훅 사용
   - 제출 중 로딩 상태 표시

2. **제출 성공 시 문의번호 안내**
   ```jsx
   if (submitted && inquiryInfo) {
     return (
       <Card>
         <CheckCircle /> 문의가 접수되었습니다
         <div>문의번호: {inquiryInfo.inquiryNumber}</div>
         <div>비밀번호: {inquiryInfo.password}</div>
         <Button>새 문의 작성</Button>
         <Link to="/inquiry/lookup">문의 조회하기</Link>
       </Card>
     );
   }
   ```

3. **페이지 간 이동**
   - 상단에 "문의 조회 →" 링크
   - 제출 완료 후 "문의 조회하기" 버튼

#### CSS 변수 사용
```jsx
// Before
className={isDark ? 'bg-[#1A1A1A] border-white/10' : 'bg-white border-gray-200'}

// After
className="bg-card border-border"
```

### 3. InquiryLookupPage - 문의 조회 페이지

#### 로그인 상태에 따른 분기 처리 (2026-01-31 업데이트)

**구조**
```jsx
export default function InquiryLookupPage() {
  const { isAuthenticated } = useAuthStore();

  return (
    <div>
      {/* 공통 헤더 */}
      <header>...</header>

      {/* 로그인 상태에 따라 다른 컴포넌트 렌더링 */}
      {isAuthenticated ? <MyInquiriesView /> : <GuestInquiryLookup />}
    </div>
  );
}
```

#### MyInquiriesView - 로그인 회원용
1. **자동 문의 목록 조회**
   ```jsx
   function MyInquiriesView() {
     const { data, isLoading, error } = useMyInquiries();
     const inquiries = data?.data || [];
     // ...
   }
   ```

2. **상태별 UI**
   - 로딩: `<Spinner size="lg" />`
   - 에러: 에러 메시지 Card
   - 빈 목록: "등록된 문의가 없습니다" + 문의 작성 버튼
   - 목록: `InquiryListItem` 컴포넌트로 렌더링

#### GuestInquiryLookup - 비회원용
1. **문의번호 + 비밀번호 조회**
   ```jsx
   const handleLookup = async (e) => {
     e.preventDefault();
     const response = await inquiriesApi.lookup({
       inquiryNumber,
       password,
     });
     setInquiries(Array.isArray(data) ? data : [data]);
   };
   ```

2. **조회 결과 표시**
   - `InquiryListItem` 컴포넌트 사용
   - 로딩 상태: `<Spinner size="lg" />`
   - 에러 상태: 상세 에러 메시지 표시
   - 빈 결과: "조회된 문의가 없습니다" 메시지

3. **에러 처리**
   ```jsx
   catch (err) {
     setError(
       err.response?.status === 404
         ? '문의를 찾을 수 없습니다. 문의번호와 비밀번호를 확인해주세요.'
         : err.response?.status === 401
         ? '비밀번호가 일치하지 않습니다.'
         : '문의 조회 중 오류가 발생했습니다.'
     );
   }
   ```

4. **페이지 간 이동**
   - 상단에 "새 문의 작성 →" 링크

### 4. InquiryForm 컴포넌트 업데이트

#### 변경사항
```jsx
// Before
import { useUIStore } from '@/stores';

export default function InquiryForm({ onSubmit, loading = false }) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  return (
    <Card className={`${isDark ? 'bg-card' : 'bg-card shadow-xl'}`}>
      <select className={`${isDark ? 'bg-white/5' : 'bg-muted'}`} />
    </Card>
  );
}

// After
export default function InquiryForm({ onSubmit, loading = false }) {
  return (
    <Card className="bg-card shadow-xl">
      <select className="bg-input border-border text-foreground" />
    </Card>
  );
}
```

#### CSS 변수 매핑
- `bg-white/5` / `bg-muted` → `bg-input`
- `border-white/10` / `border-gray-200` → `border-border`
- 다크모드 조건부 클래스 제거

### 5. InquiryListItem 컴포넌트 업데이트

#### 변경사항
```jsx
// Before
import { useUIStore } from '@/stores';

export default function InquiryListItem({ inquiry }) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  return (
    <Card className={`${isDark ? 'bg-card' : 'bg-card shadow-sm'}`}>
      {/* ... */}
    </Card>
  );
}

// After
export default function InquiryListItem({ inquiry }) {
  return (
    <Card className="bg-card border-border shadow-sm">
      {/* ... */}
    </Card>
  );
}
```

## 사용된 컴포넌트 및 API

### UI 컴포넌트
- `@/components/ui/card` - Card
- `@/components/ui/button` - Button
- `@/components/ui/input` - Input
- `@/components/ui/spinner` - Spinner

### Feature 컴포넌트
- `@/components/feature/inquiry/InquiryForm` - 문의 작성 폼 (업데이트됨)
- `@/components/feature/inquiry/InquiryListItem` - 문의 목록 아이템 (업데이트됨)

### API & Hooks
- `@/api/inquiries` - inquiriesApi.lookup(), inquiriesApi.create()
- `@/hooks/queries/useInquiries` - useCreateInquiry()
- `react-router-dom` - useNavigate, Link
- `lucide-react` - Send, CheckCircle, Search, AlertCircle

## CSS 변수 매핑

| 이전 (하드코딩) | 이후 (CSS 변수) | 용도 |
|----------------|-----------------|------|
| `bg-white` / `bg-[#1A1A1A]` | `bg-card` | 카드 배경 |
| `text-black` / `text-white` | `text-foreground` | 메인 텍스트 |
| `text-gray-500` / `text-gray-400` | `text-muted-foreground` | 보조 텍스트 |
| `border-gray-100` / `border-white/10` | `border-border` | 테두리 |
| `bg-gray-50` / `bg-white/5` | `bg-input` | 입력창 배경 |
| `border-gray-200` / `border-white/10` | `border-border` / `border-input` | 입력창 테두리 |
| `text-[#03A69E]` | `text-primary` | 브랜드 색상 |
| `bg-[#03A69E]` | `bg-primary` | 브랜드 배경 |
| `bg-[#03A69E]/20` | `bg-primary/20` | 브랜드 배경 (투명도) |
| `text-red-500` | `text-destructive` | 에러/위험 색상 |
| `bg-green-500/20 text-green-500` | `bg-success/20 text-success` | 성공 상태 |
| `bg-orange-500/20 text-orange-500` | `bg-warning/20 text-warning` | 경고 상태 |

## 라우팅 구조

### 기존 (SupportView)
```
App.jsx
└── activeMenu === 'support' → <SupportView theme={theme} />
    ├── view === 'form' → 문의 작성 폼
    └── view === 'history' → 문의 내역
```

### 변경 후 (Pages)
```
router.jsx
├── /inquiry → <InquiryPage />
│   ├── 문의 작성 폼
│   └── 제출 완료 화면
└── /inquiry/lookup → <InquiryLookupPage />
    ├── 조회 폼
    └── 조회 결과
```

## 레이아웃 구조

### InquiryPage
```
max-w-3xl mx-auto
├── Header
│   ├── 제목 & 설명
│   └── "문의 조회 →" 링크
└── Card (문의 작성 폼 또는 제출 완료)
    ├── 문의 유형 (select)
    ├── 제목 (Input)
    ├── 내용 (textarea)
    ├── 비밀번호 (Input)
    └── 제출 버튼 (Button)
```

### InquiryLookupPage
```
max-w-3xl mx-auto
├── Header
│   ├── 제목 & 설명
│   └── "새 문의 작성 →" 링크
├── Card (조회 폼)
│   ├── 문의번호 (Input)
│   ├── 비밀번호 (Input)
│   └── 조회 버튼 (Button)
└── 조회 결과
    ├── 로딩 (Spinner)
    ├── 에러 (Card with AlertCircle)
    ├── 빈 결과 (Card with message)
    └── 결과 목록 (InquiryListItem[])
```

## 마이그레이션 체크리스트

- [x] theme prop 제거
- [x] CSS 변수로 색상 변경
- [x] 탭 UI → 별도 페이지 분리
- [x] Link로 페이지 간 이동
- [x] useCreateInquiry() React Query 훅 사용
- [x] inquiriesApi.lookup() API 호출
- [x] UI 컴포넌트 사용 (Card, Button, Input, Spinner)
- [x] Feature 컴포넌트 업데이트 (InquiryForm, InquiryListItem)
- [x] lucide-react 아이콘 사용
- [x] 에러 처리 및 로딩 상태
- [x] 빌드 성공 확인
- [x] 문서 작성

## 테스트 확인

- [x] 프론트엔드 빌드 성공 (`npm run build`)
- [ ] 문의 작성 기능 동작 확인 (백엔드 연동 필요)
- [ ] 문의 조회 기능 동작 확인 (백엔드 연동 필요)
- [ ] 다크모드/라이트모드 전환 확인
- [ ] 반응형 레이아웃 확인

## 다음 단계

1. 백엔드 API 연동 후 실제 동작 테스트
2. 문의 상세보기 페이지 추가 고려
3. ~~로그인 사용자용 문의 내역 페이지 추가 고려 (MyPage에서)~~ ✅ **완료** (2026-01-31)
4. 원본 SupportView.jsx 파일 삭제 (모든 마이그레이션 완료 후)

## 업데이트 내역

### 2026-01-31: 로그인 상태 기반 분기 처리 추가
- InquiryLookupPage가 로그인 상태 감지
- 로그인 회원: MyInquiriesView 컴포넌트로 자동 문의 목록 표시
- 비회원: GuestInquiryLookup 컴포넌트로 문의번호+비밀번호 입력 방식 유지
- useAuthStore의 isAuthenticated로 상태 판별
- useMyInquiries hook 활용 (이미 구현되어 있었음)
- inquiriesApi.getMyList() API 활용 (이미 구현되어 있었음)
