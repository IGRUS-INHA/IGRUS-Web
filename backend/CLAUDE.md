# Backend CLAUDE.md

## 프로젝트 개요

IGRUS Web 백엔드 - Spring Boot 기반 REST API 서버

인하대학교 IT 동아리 IGRUS의 웹 사이트 백엔드

## 사용 기술

| 분류 | 기술 | 버전 |
|------|-----|------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.9 |
| ORM | Spring Data JPA | - |
| Security | Spring Security + OAuth2 | - |
| Database | MySQL | 8.x |
| Migration | Flyway | - |
| JWT | JJWT | 0.12.3 |
| API Docs | SpringDoc OpenAPI (Swagger) | 2.8.14 |
| Cloud | AWS | - |
| Build | Gradle | - |
| Test | JUnit 5, H2 (in-memory) | - |

## 개발 규칙

### 1. 계층 구조 (Layered Architecture)

```
Controller → Service → Repository → Entity
     ↓           ↓
    DTO        Domain
```

- **Controller**: HTTP 요청/응답 처리만 담당, 비즈니스 로직 금지
- **Service**: 비즈니스 로직 담당, 트랜잭션 관리
- **Repository**: 데이터 접근만 담당
- **DTO**: 계층 간 데이터 전송, Entity 직접 노출 금지

### 2. 코드 컨벤션

#### 네이밍 규칙
- **클래스**: PascalCase (UserService, PostController)
- **메서드/변수**: camelCase (findByEmail, userId)
- **상수**: UPPER_SNAKE_CASE (MAX_PAGE_SIZE)
- **패키지**: lowercase (user, post, security)

#### DTO 네이밍
- 요청: `{Action}{Domain}Request` (CreatePostRequest)
- 응답: `{Domain}{Action}Response` (PostDetailResponse)
- 내부 전송: `{Domain}Dto` (UserDto)

#### 메서드 네이밍
- 조회 단건: `get{Domain}` / `find{Domain}By{Field}`
- 조회 목록: `get{Domain}List` / `find{Domain}sBy{Field}`
- 생성: `create{Domain}`
- 수정: `update{Domain}`
- 삭제: `delete{Domain}`

### 3. 예외 처리

#### ErrorCode 사용 필수
```java
// Good
throw new UserNotFoundException(ErrorCode.USER_NOT_FOUND);

// Bad - 직접 메시지 작성 금지
throw new RuntimeException("사용자를 찾을 수 없습니다");
```

#### 커스텀 예외 생성 규칙
- `CustomBaseException` 상속
- 도메인별 패키지에 위치 (`exception/custom/{domain}/`)
- ErrorCode와 1:1 매핑

#### 비즈니스 예외 정의 원칙
- 적합한 예외가 없으면 새로 정의 (RuntimeException 직접 사용 금지)

### 4. API 설계

#### RESTful 원칙 준수
```
GET    /api/users/{id}      # 단건 조회
GET    /api/users           # 목록 조회
POST   /api/users           # 생성
PUT    /api/users/{id}      # 전체 수정
PATCH  /api/users/{id}      # 부분 수정
DELETE /api/users/{id}      # 삭제
```

### API 버저닝 적용
```
/api/v1/~~~
/api/v2/~~~
...
```

#### 응답 형식 통일
- 성공: HTTP 2xx + 데이터
- 실패: HTTP 4xx/5xx + ErrorResponse

### 5. 데이터베이스

#### Entity 규칙
- `BaseEntity` 상속 (createdAt, updatedAt 자동 관리)
- `@Entity` 클래스에 `@NoArgsConstructor(access = PROTECTED)` 필수
- 연관관계 설정 시 지연 로딩(`FetchType.LAZY`) 기본 사용

#### 쿼리 작성
- 단순 쿼리: Spring Data JPA 메서드 쿼리
- 복잡 쿼리: `@Query` 또는 QueryDSL
- N+1 문제 주의: `@EntityGraph` 또는 fetch join 활용

#### 도메인 엔티티 변경 시 그에 맞는 flyway 마이그레이션 파일 생성하기

### 6. 보안

#### 인증/인가
- Spring Security + JWT 사용
- 민감 정보 로깅 금지 (비밀번호, 토큰 등)
- CORS 설정 명시적 관리

#### 입력 검증
- `@Valid` + Bean Validation 필수 사용
- SQL Injection, XSS 방지

### 7. 트랜잭션

```java
@Transactional
@Service
public class UserService {

    // 조회 메서드: readOnly로 최적화
    @Transactional(readOnly = true)
    public User getUser(Long id) { ... }

    // 변경 메서드: 클래스 레벨 @Transactional 상속
    public User createUser(CreateUserRequest request) { ... }
}
```

- Service 클래스 레벨에 `@Transactional` 설정 (기본 트랜잭션 보장)
- 조회 메서드에만 `@Transactional(readOnly = true)` 명시
- 변경 메서드는 클래스 레벨 트랜잭션 상속

### 8. 테스트

#### 테스트 구조
```
src/test/java/
├── unit/           # 단위 테스트
├── integration/    # 통합 테스트
└── acceptance/     # 인수 테스트
```

#### 테스트 네이밍
```java
@DisplayName("사용자 조회 - 존재하는 사용자 ID로 조회 시 사용자 정보 반환")
@Test
void getUser_WithValidId_ReturnsUser() { ... }
```

#### 테스트 접근법: 고전파 + 런던파 혼용

| 구분 | 고전파 (Classical) | 런던파 (Mockist) |
|------|-------------------|-----------------|
| 단위 정의 | 행위(behavior) 단위 | 클래스 단위 |
| 협력 객체 | 실제 객체 사용 | Mock으로 대체 |
| 장점 | 리팩토링 내성 높음 | 실패 원인 파악 명확 |
| 단점 | 원인 파악 어려움 | 구현에 결합됨 |

**상황별 권장 방식:**
- 도메인 로직: 고전파 (실제 객체 사용)
- 외부 의존성 (DB, API): Mock 사용
- 비결정적 요소 (시간, 랜덤): Mock/Stub 사용

```java
@Test
void 주문_생성_시_재고가_차감되고_결제가_처리된다() {
    // 외부 의존성만 Mock
    PaymentGateway paymentGateway = mock(PaymentGateway.class);

    // 도메인 객체는 실제 사용 (고전파)
    Product product = new Product("상품A", 10);
    Order order = new Order(product, 3);
    OrderService service = new OrderService(productRepo, paymentGateway);

    service.createOrder(order);

    // 상태 검증 (고전파)
    assertThat(product.getStock()).isEqualTo(7);
    // 상호작용 검증 (런던파)
    verify(paymentGateway).process(any());
}
```

#### 테스트 코드에 @Transactional 사용 금지

#### 변화에 강건한 테스트를 작성할 것

#### 테스트 가치가 높은 코드만 작성할 것

#### 새로운 기능을 추가하면 관련 테스트를 꼭 작성할 것

#### 로직을 수정하면 테스트도 적절하게 수정할 것

#### 엣지 케이스가 없는지 꼼꼼하게 테스트 할 것

#### 항상 마지막에는 모든 테스트를 실행하면서 문제가 없는지 체크할 것

### 9. 로깅

```java
// 클래스 레벨에 @Slf4j 사용
@Slf4j
@Service
public class UserService {
    public void createUser(...) {
        log.info("사용자 생성 요청: email={}", request.getEmail());
        // 민감 정보는 로깅하지 않음
    }
}
```

- DEBUG: 개발 시 디버깅 정보
- INFO: 주요 비즈니스 로직 흐름
- WARN: 잠재적 문제
- ERROR: 예외 발생, 중요한 에러 발생

### 10. 성능 고려사항

- 페이징 처리: `Pageable` + `Page<T>` 사용
- 캐싱: Redis 활용 (`@Cacheable`, `@CacheEvict`)
- 비동기 처리: `@Async` + CompletableFuture

### 11. 문서화

#### 문서화 & 개발 워크 플로우
1. 기능 구현 전 docs 폴더에서 스펙 문서가 존재하는지 확인한다.
2. 만약 존재한다면 그 문서를 사용하거나 업데이트 하고, 존재하지 않는다면 새로운 문서를 작성한다.
3. 스펙 문서를 기반으로 테스트 케이스를 뽑아내서 docs/backend/test-case 폴더에 md 파일로 저장한다. 최대한 꼼꼼하게 테스트 케이스를 뽑아낸다. 효과적인 QA 기법들을 사용한다.
4. 테스트 케이스를 기반으로 구현을 시작한다.
5. 구현 중에 테스트 케이스가 추가로 더 필요하다고 생각하면 테스트 케이스 문서를 보충한다.
5. 테스트 케이스에 대한 테스트 코드를 작성한다. 이 과정에서 구현에 문제가 있다면 고친다.
6. 테스트 코드를 실행해 문제가 없는지 확인한다.

#### Swagger
- Swagger/OpenAPI 3.0 사용
- Controller 메서드에 `@Operation`, `@ApiResponse` 어노테이션
    - 컨트롤러에는 꼭 Swagger/OpenAPI 관련 어노테이션을 붙여서 문서화 할 것
- 새로운 기능 구현, 기능 수정 이후에는 Swagger 관련 코드를 업데이트 해서 문서화 상태를 항상 최신으로 유지할 것

### 12. SOLID 원칙 준수
- SRP(Single Responsibility Principle): 단일 책임 원칙
- OCP(Open Closed Principle): 개방 폐쇄 원칙
- LSP(Liskov Substitution Principle): 리스코프 치환 원칙
- ISP(Interface Segregation Principle): 인터페이스 분리 원칙
- DIP(Dependency Inversion Principle): 의존 역전 원칙

### 13. 코드 리뷰
- 구현과 테스트가 끝난 후에는 꼭 코드 리뷰를 진행할 것.
- 로직이 이상한 부분은 없는지, 성능 상의 문제가 될 부분은 없는지, 빠뜨린 로직이 없는지 등 최대한 꼼꼼하게 체크할 것.

### 14. 꼼꼼한 요구사항 명세
- 불명확한 요구사항이 모두 명확해질 때까지 요구사항의 상세화 작업을 진행할 것.
- 불명확한 부분은 사용자에게 물어볼 것. 스스로 판단하지 말 것.

### 15. 구현 전에 반드시 아래를 산출:
- Acceptance Criteria (성공/실패 케이스)
- Edge cases (null/빈값/경계값/동시성/멱등성)
- 권한 모델(누가 무엇을 할 수 있는지)
- 데이터 제약(unique, length, FK, soft delete 여부)

### 16. 프론트엔드와 백엔드 주소가 서로 다름을 고려
- 프론트엔드와 백엔드 주소가 서로 다르기 때문에 생길 수 있는 문제를 방지할 것.
- CORS 에러 방지, 쿠키 설정 등을 유의할 것.

### 17. Flyway 스크립트의 버전을 체크할 것
- 커밋하기 전에 이전 커밋 기록을 보고 스크립트 버전이 충돌되지 않는지 체크할 것.
- 최신 스크립트의 버전은 항상 이전 버전보다 커야 함.
- 이전에 커밋한 스크립트는 수정하면 안 됨. 새로운 버전의 스크립트를 만들어서 작업해야 함. 이를 항상 체크.

### 18. 시간 클래스는 Instant 로 통일
- 시간을 나타내는 클래스는 Instant 클래스만 사용할 것.
- 다른 클래스는 사용하지 말 것.
