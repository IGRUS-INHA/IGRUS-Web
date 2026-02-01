# MyPage 페이지 설계

> 최종 업데이트: 2026-01-30

## 개요

사용자의 프로필 정보와 활동 내역을 확인할 수 있는 마이페이지입니다.

## 파일 정보

- **파일 경로**: `frontend/src/pages/mypage/MyPage.jsx`
- **라우트**: `/mypage`
- **권한**: 로그인 필요 (모든 회원)

## 주요 기능

### 1. 프로필 정보
- 프로필 이미지 (기본: User 아이콘)
- 사용자 이름
- 역할 배지 (준회원/정회원/운영진/관리자)
- 학번
- 이메일
- 가입일

### 2. 활동 탭
4가지 탭으로 활동 내역 분류:
- **게시글**: 작성한 게시글 목록
- **좋아요**: 좋아요한 게시글 목록
- **스크랩**: 스크랩한 게시글 목록
- **행사**: 신청한 행사 목록

### 3. 계정 관리
- 비밀번호 변경 버튼
- 로그아웃 버튼

## 상태 관리

### 전역 상태 (Zustand)
```javascript
import { useAuthStore } from '@/stores/authStore';

const { user, logout } = useAuthStore();
```

### 로컬 상태
```javascript
const [activeTab, setActiveTab] = useState('posts');
```

## 라우팅

### 네비게이션
```javascript
import { useNavigate } from 'react-router-dom';

const navigate = useNavigate();

// 로그아웃 후 홈으로 이동
const handleLogout = () => {
  logout();
  navigate('/');
};

// 비로그인 사용자 리다이렉트
if (!user) {
  navigate('/auth/login');
  return null;
}
```

## UI 컴포넌트

### 사용 컴포넌트
```javascript
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
```

### 아이콘
```javascript
import {
  User, Mail, Calendar, Edit3, Shield,
  Award, Layers, Bookmark, Heart, Lock, LogOut
} from 'lucide-react';
```

## 디자인 시스템

### CSS 변수 사용
- `bg-background`: 배경색
- `text-foreground`: 기본 텍스트색
- `bg-card`: 카드 배경색
- `text-muted-foreground`: 보조 텍스트색
- `bg-primary`: 브랜드 색상 배경
- `text-primary`: 브랜드 색상 텍스트
- `border-border`: 테두리 색상

### 반응형 디자인
- `md:flex-row`: 모바일은 세로, 데스크톱은 가로 배치
- `md:text-left`: 모바일은 중앙, 데스크톱은 좌측 정렬
- `md:justify-start`: 모바일은 중앙, 데스크톱은 좌측 정렬

## 레이아웃 구조

```jsx
<div className="space-y-8 animate-in fade-in duration-300">
  {/* 프로필 헤더 */}
  <Card className="p-10 rounded-[3rem]">
    {/* 프로필 이미지 + 정보 */}
  </Card>

  {/* 활동 탭 */}
  <div className="flex gap-4 overflow-x-auto">
    {/* 탭 버튼들 */}
  </div>

  {/* 콘텐츠 영역 */}
  <Card className="p-8 rounded-[2.5rem] min-h-[400px]">
    {/* 활성 탭에 따른 콘텐츠 */}
  </Card>
</div>
```

## 마이그레이션 변경사항

### 제거된 항목
- ✅ `theme` prop 제거 (CSS 변수로 대체)
- ✅ `onLogout` prop 제거 (내부에서 authStore 사용)
- ✅ `user` prop 제거 (authStore에서 직접 가져오기)
- ✅ 다크모드 조건문 제거 (`isDark ? ... : ...`)

### 추가된 항목
- ✅ `useNavigate` 훅 사용
- ✅ `useAuthStore` 훅 사용
- ✅ 비로그인 사용자 처리 로직
- ✅ Button, Card 컴포넌트 사용
- ✅ ROLE_LABELS 상수 사용

### 디자인 변경
- ✅ 하드코딩된 색상 → CSS 변수
  - `#03A69E` → `text-primary` / `bg-primary`
  - `#1A1A1A` → `bg-card`
  - `gray-500` → `text-muted-foreground`
- ✅ 테마 조건부 클래스 → 단일 시맨틱 클래스
  - Before: `${isDark ? 'bg-[#1A1A1A]' : 'bg-white'}`
  - After: `bg-card`

## TODO

### 향후 개선사항
- [ ] API 연동 (실제 활동 데이터 가져오기)
- [ ] 프로필 이미지 업로드 기능
- [ ] 비밀번호 변경 모달 구현
- [ ] 페이지네이션 (활동 목록)
- [ ] 무한 스크롤 옵션
- [ ] 활동 통계 그래프
- [ ] 프로필 수정 기능

### 데이터 구조 (예상)
```typescript
interface User {
  studentId: string;
  name: string;
  email: string;
  role: 'ASSOCIATE' | 'MEMBER' | 'OPERATOR' | 'ADMIN';
  joinedDate: string; // YYYY-MM-DD
  postCount?: number;
  likeCount?: number;
}

interface Activity {
  id: number;
  board: string;
  date: string;
  title: string;
}

interface Event {
  id: number;
  title: string;
  status: string;
  date?: string;
  location?: string;
}
```

## 관련 파일

### 컴포넌트
- `frontend/src/components/ui/button.jsx`
- `frontend/src/components/ui/card.jsx`
- `frontend/src/components/feature/mypage/ProfileHeader.jsx` (참고용)
- `frontend/src/components/feature/mypage/ActivityList.jsx` (참고용)
- `frontend/src/components/feature/mypage/AppliedEventList.jsx` (참고용)

### 상태 관리
- `frontend/src/stores/authStore.js`

### 상수
- `frontend/src/constants/index.js` (ROLE_LABELS)

### 스타일
- `frontend/src/index.css` (디자인 시스템)

## 참고사항

1. **권한 체크**: 비로그인 사용자는 자동으로 로그인 페이지로 리다이렉트
2. **Mock 데이터**: 현재는 하드코딩된 데이터 사용 (향후 API 연동 필요)
3. **탭 카운트**: 임시 숫자 사용 (실제로는 user 객체에서 가져와야 함)
4. **한글화**: 모든 텍스트 한글로 변경 (기존 영문 → 한글)
