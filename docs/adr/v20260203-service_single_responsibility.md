# 서비스 레이어 단일 책임 구조로 리팩토링

## 배경

기존 서비스 레이어는 도메인별로 하나의 대형 서비스 클래스가 모든 비즈니스 로직을 담당하는 구조였습니다.

```java
// 기존 구조 예시
@Service
public class InquiryService {
    public InquiryResponse createGuestInquiry(...) { ... }
    public InquiryResponse createMemberInquiry(...) { ... }
    public List<InquiryResponse> getAllInquiries(...) { ... }
    public InquiryDetailResponse getInquiryDetail(...) { ... }
    public void deleteInquiry(...) { ... }
    public void updateInquiryStatus(...) { ... }
    public void createInquiryReply(...) { ... }
    // ... 수십 개의 메서드
}
```

이 구조에서 다음과 같은 문제가 발생했습니다:

- **비대한 클래스**: 하나의 서비스에 10개 이상의 메서드가 집중되어 가독성 저하
- **높은 결합도**: 서비스 간 의존 시 불필요한 메서드까지 함께 주입
- **테스트 부담**: 하나의 서비스를 테스트하기 위해 많은 의존성을 모킹해야 함
- **변경 영향 범위 확대**: 하나의 기능 수정 시 관련 없는 메서드에 영향 가능성

## 선택지

1. **단일 책임 서비스 분리**: 서비스를 기능 단위로 분리하고, 역할별(read/write/support 등) 패키지로 구조화
2. **파사드 패턴**: 기존 대형 서비스를 유지하되, 내부 로직을 위임 서비스로 분리
3. **현행 유지 + 메서드 분리**: 클래스는 유지하고 private 메서드로 내부 로직만 분리

## 결정

- **단일 책임 서비스 분리** 채택

각 서비스 클래스가 하나의 명확한 책임만 갖도록 분리하고, 역할별 패키지로 구조화합니다.

## 결정 이유

### 1. 단일 책임 원칙(SRP) 준수

각 서비스가 하나의 유스케이스만 담당하므로 변경 이유가 하나로 명확해집니다.

```
// 리팩토링 후 구조 예시 - Inquiry
inquiry/service/
├── create/
│   ├── CreateGuestInquiryService.java
│   └── CreateMemberInquiryService.java
├── read/
│   ├── GetAllInquiriesService.java
│   ├── GetInquiryDetailService.java
│   └── GetMyInquiriesService.java
├── manage/
│   ├── CreateInquiryReplyService.java
│   ├── DeleteInquiryService.java
│   └── UpdateInquiryStatusService.java
└── support/
    ├── InquiryFinder.java
    └── InquiryValidator.java
```

### 2. 의존성 최소화

컨트롤러나 다른 서비스에서 필요한 기능만 주입받을 수 있습니다.

```java
// Before: 전체 서비스 주입 (불필요한 의존성 포함)
private final InquiryService inquiryService;

// After: 필요한 기능만 주입
private final GetInquiryDetailService getInquiryDetailService;
private final DeleteInquiryService deleteInquiryService;
```

### 3. 테스트 용이성

서비스 하나에 필요한 의존성이 적어 단위 테스트 작성이 간편합니다.

### 4. 파사드 패턴 대비 이점

파사드 패턴은 외부 인터페이스를 단순화하지만, 내부적으로 추가 위임 레이어가 생깁니다. 단일 책임 서비스 분리는 컨트롤러가 필요한 서비스를 직접 주입받으므로 불필요한 중간 레이어 없이 직관적입니다.

## 적용 범위 및 패키지 구조 규칙

### 역할별 패키지 분류

| 패키지 | 역할 | 포함 대상 |
|--------|------|-----------|
| `read/` | 데이터 조회 | `Get*Service`, 조회 쿼리 |
| `write/` | 데이터 생성/수정/삭제 | `Create*Service`, `Update*Service`, `Delete*Service` |
| `create/` | 생성 전용 (write에서 분리 시) | `Create*Service` |
| `manage/` | 상태 변경, 관리 작업 | `Update*Service`, `Delete*Service` (관리자 기능) |
| `support/` | 공통 유틸리티 | `*Validator`, `*Finder`, `*Helper` |
| `permission/` | 권한 검증 | `Check*Service`, `Can*Service` |

### 클래스 명명 규칙

- 조회: `Get{대상}Service` (예: `GetMyInquiriesService`)
- 생성: `Create{대상}Service` (예: `CreateGuestInquiryService`)
- 수정: `Update{대상}Service` (예: `UpdateInquiryStatusService`)
- 삭제: `Delete{대상}Service` (예: `DeleteInquiryService`)
- 검증: `{대상}Validator` (예: `CommentValidator`)
- 조회 헬퍼: `{대상}Finder` (예: `InquiryFinder`)

### 클래스 구조 패턴

```java
@Service
@RequiredArgsConstructor
@Transactional  // read 서비스는 @Transactional(readOnly = true) 사용
public class Get{Domain}Service {

    private final SomeRepository repository;

    public ResponseDto execute(...) {
        // 단일 비즈니스 로직
    }
}
```

## 적용 대상

| 커밋 | 도메인 | 분리 구조 |
|------|--------|-----------|
| `519f668` | SemesterMember | read / write / support |
| `b738b4f` | Auth | read / write / manage / support |
| `91bfcec` | Community (Board, Post, Comment, Bookmark, CommentLike) | read / write / support / permission |
| `79ede2d` | Inquiry | create / read / manage / support |

## 고려한 대안

### 파사드 패턴

**장점:**
- 기존 컨트롤러 코드 변경 최소화
- 외부 인터페이스 단순 유지

**채택하지 않은 이유:**
- 파사드 클래스가 결국 기존 대형 서비스와 유사한 역할을 하게 됨
- 위임 레이어가 추가되어 코드 추적이 어려워짐
- 컨트롤러에서 직접 필요한 서비스를 주입받는 것이 더 명확함

### 현행 유지 + 메서드 분리

**장점:**
- 리팩토링 비용 없음
- 기존 코드 구조 유지

**채택하지 않은 이유:**
- 클래스 크기 문제를 근본적으로 해결하지 못함
- 의존성 주입 단위가 여전히 대형 서비스 클래스
- 테스트 시 불필요한 의존성 모킹 문제 지속

## 결과

- Auth, Community, Inquiry, SemesterMember 4개 도메인의 서비스 레이어가 단일 책임 구조로 전환됨
- 각 서비스 클래스가 하나의 유스케이스만 담당
- 컨트롤러에서 필요한 서비스만 선택적으로 주입
- 새로운 기능 추가 시 기존 서비스 수정 없이 새로운 서비스 클래스 생성으로 대응 가능 (OCP)

## 후속 조치

- [ ] 신규 도메인 개발 시 동일한 단일 책임 구조 적용
- [ ] 기존 미전환 도메인이 있다면 동일 패턴으로 순차 리팩토링
