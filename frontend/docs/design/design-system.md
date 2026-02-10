# IGRUS Design System

## 색상 (Colors)

### Brand Colors
메인 브랜드 컬러 `#03A69E` 기반 8단계 스케일

| 단계 | HEX | 용도 |
|------|-----|------|
| L1 | `#E6F6F5` | 연한 배경, 호버 상태 |
| L2 | `#B3E5E2` | 강조 배경 |
| L3 | `#66CBC5` | 다크모드 primary |
| L4 | `#03A69E` | **메인 브랜드 컬러** |
| L5 | `#03958E` | 호버 상태 |
| L6 | `#027A75` | 액티브 상태 |
| L7 | `#015F5B` | 강조 텍스트 |
| L8 | `#013E3B` | 가장 어두운 브랜드 |

### Gray Scale
중립 색상 8단계 스케일

| 단계 | HEX | 용도 |
|------|-----|------|
| G1 | `#F8F9FA` | 전체 배경색, 아주 연한 구분선 |
| G2 | `#E9ECEF` | 비활성 버튼 배경, 입력창 배경 |
| G3 | `#DEE2E6` | 일반적인 구분선(Divider), 테두리 |
| G4 | `#ADB5BD` | 비활성 텍스트, 아이콘 (Placeholder) |
| G5 | `#6C757D` | 보조 설명 텍스트, 메타 정보 |
| G6 | `#495057` | 일반 본문 텍스트 (Body Text) |
| G7 | `#343A40` | 강조 텍스트, 서브 타이틀 |
| G8 | `#212529` | 메인 타이틀, 가장 어두운 텍스트 |

### State Colors
상태 표시용 색상

| 이름 | HEX | 용도 |
|------|-----|------|
| Success | `#28A745` | 성공, 완료 |
| Warning | `#FFC107` | 경고, 주의 |
| Destructive | `#DC3545` | 오류, 삭제 |
| Info | `#17A2B8` | 정보, 알림 |

### Semantic Tokens
의미 기반 토큰 (자동 다크모드 대응)

| 토큰 | 라이트 모드 | 다크 모드 | 용도 |
|------|-------------|-----------|------|
| `background` | `#FFFFFF` | G8 | 페이지 배경 |
| `foreground` | G8 | G1 | 메인 텍스트 |
| `card` | `#FFFFFF` | G7 | 카드 배경 |
| `muted` | G1 | G7 | 연한 배경 |
| `muted-foreground` | G5 | G4 | 보조 텍스트 |
| `border` | G3 | G6 | 테두리 |
| `input` | G2 | G7 | 입력창 배경 |
| `primary` | L4 | L3 | 주요 액션 |
| `accent` | L1 | L7 | 강조 배경 |

---

## 타이포그래피 (Typography)

폰트: **Inter**

| 클래스 | 굵기 | 크기 | 행간 | 용도 |
|--------|------|------|------|------|
| `.typo-h1` / `h1` | Bold (700) | 42px | 1.2 | 메인 타이틀 |
| `.typo-h2` / `h2` | Bold (700) | 28px | 1.3 | 섹션 타이틀 |
| `.typo-h3` / `h3` | SemiBold (600) | 22px | 1.3 | 카드 타이틀 |
| `.typo-b1` | Regular (400) | 16px | 1.5 | 본문 |
| `.typo-b2` | Regular (400) | 14px | 1.5 | 작은 본문 |
| `.typo-label` | Medium (500) | 13px | auto | 라벨 |
| `.typo-c1` | Medium (500) | 12px | auto | 캡션 |
| `.typo-c2` | Regular (400) | 10px | auto | 작은 캡션 |

---

## 간격 (Spacing)

8px 기반 스케일

| 토큰 | 값 | 용도 |
|------|-----|------|
| `s1` | 4px | 아이콘-텍스트 간격 |
| `s2` | 8px | 작은 요소 간격 |
| `s3` | 12px | 버튼 내부 패딩 |
| `s4` | 16px | 카드 내부 패딩 |
| `s5` | 24px | 섹션 간격 |
| `s6` | 32px | 큰 섹션 간격 |
| `s7` | 48px | 페이지 섹션 |
| `s8` | 56px | 큰 페이지 섹션 |

```jsx
// 사용 예시
<div className="p-s4 gap-s3 mb-s5">
```

---

## 라운딩 (Border Radius)

| 토큰 | 값 | 용도 |
|------|-----|------|
| `r1` | 4px | 작은 버튼, 태그 |
| `r2` | 8px | 일반 버튼, 입력창 |
| `r3` | 12px | 카드 |
| `r4` | 16px | 큰 카드, 모달 |
| `full` | 100px | 원형 버튼, 아바타 |

```jsx
// 사용 예시
<button className="rounded-r2">버튼</button>
<div className="rounded-r4">카드</div>
<div className="rounded-full">아바타</div>
```

---

## 사용법

### Tailwind 클래스

```jsx
// 색상
<p className="text-foreground">메인 텍스트</p>
<p className="text-muted-foreground">보조 텍스트</p>
<p className="text-primary">브랜드 컬러 텍스트</p>
<div className="bg-card border-border">카드</div>

// 직접 그레이 지정
<p className="text-gray-5">G5 색상</p>
<div className="bg-gray-2">G2 배경</div>

// 직접 브랜드 지정
<div className="bg-brand-l1">L1 배경</div>
<p className="text-brand-l6">L6 텍스트</p>

// 타이포그래피
<h1 className="typo-h1">제목</h1>
<p className="typo-b1">본문</p>
<span className="typo-c1 text-muted-foreground">캡션</span>

// 간격
<div className="p-s4 gap-s3 mb-s5">콘텐츠</div>

// 라운딩
<button className="rounded-r2 px-s4 py-s2">버튼</button>
```

### 다크 모드

`useUIStore`의 `theme` 상태로 관리되며, `<html>` 태그에 `.dark` 클래스 토글로 작동.

```jsx
const { theme, toggleTheme } = useUIStore();
// theme: 'light' | 'dark'
```

시맨틱 토큰 사용 시 자동으로 다크모드 대응됨.
