# 준회원 승인/거절 정보 분리 및 AssociateDecision 엔티티 도입

## 배경

기존에 준회원 승인 정보(`approvedAt`, `approvedBy`)는 `PasswordCredential` 엔티티에 저장되었다. 준회원 거절 기능(Issue #257)을 추가하면서, 승인/거절 정보를 어디에 저장할지 결정이 필요했다.

문제점:
- `PasswordCredential`은 비밀번호 인증 자격증명을 관리하는 엔티티로, 승인/거절 결정 정보를 함께 관리하면 단일 책임 원칙에 위배됨
- 거절 정보(거절 사유, 거절자, 거절일)를 `PasswordCredential`에 추가하면 엔티티가 비대해짐
- 향후 다른 인증 방식(OAuth 등)이 추가될 경우, 승인/거절 정보가 특정 인증 방식에 종속됨

## 선택지

1. **PasswordCredential에 거절 필드 추가**: `rejectedAt`, `rejectedBy`, `rejectionReason` 필드를 기존 엔티티에 추가
2. **별도 엔티티로 분리**: 승인/거절 정보를 관리하는 `AssociateDecision` 엔티티 신규 생성, 기존 `approvedAt`/`approvedBy`도 이관

## 결정

- **별도 엔티티(AssociateDecision)로 분리** 채택

## 결정 이유

- **단일 책임 원칙**: `PasswordCredential`은 비밀번호 인증만, `AssociateDecision`은 준회원 결정만 담당
- **통합 관리**: 승인과 거절을 하나의 엔티티에서 `AssociateDecisionType`(APPROVED/REJECTED)으로 구분하여 관리
- **확장성**: 향후 인증 방식 추가 시 준회원 결정 로직은 독립적으로 유지
- **감사 이력 명확화**: 결정자, 결정일, 사유를 하나의 엔티티에서 일관되게 관리

## 적용 범위

- 준회원 승인/거절 프로세스 전체
- `PasswordCredential` → `AssociateDecision`으로 승인 정보 이관
- 승인 대기 준회원 조회: `NOT EXISTS (SELECT 1 FROM AssociateDecision)` 서브쿼리로 필터링

## 결과

### 새 엔티티: `AssociateDecision`
- `associate_decisions` 테이블
- User와 OneToOne 관계 (unique 제약)
- 정적 팩토리 메서드: `approve(User, Long)`, `reject(User, Long, String)`
- `AssociateDecisionType` enum: APPROVED, REJECTED

### 마이그레이션: Flyway V24
- `associate_decisions` 테이블 생성
- 기존 `password_credentials.approved_at/approved_by` 데이터를 `associate_decisions`로 이관
- `password_credentials`에서 `approved_at`, `approved_by` 컬럼 제거

### 영향받는 서비스
- `ApproveAssociateService`: `PasswordCredentialRepository` → `AssociateDecisionRepository`
- `BulkApproveAssociatesService`: 동일 변경
- `GetPendingAssociatesService`: `UserRepository.findByRole()` → `AssociateDecisionRepository.findPendingAssociates()`
- 신규: `RejectAssociateService`, `BulkRejectAssociatesService`, `GetRejectedAssociatesService`

## 후속 조치

- [x] `AssociateDecision` 엔티티 및 리포지토리 생성
- [x] Flyway V24 마이그레이션 작성
- [x] `PasswordCredential`에서 승인 필드 제거
- [x] 기존 승인 서비스 리팩토링
- [x] 거절 서비스 구현 (개별/일괄)
- [x] 거절 목록 조회 서비스 구현
- [x] 컨트롤러 엔드포인트 추가
- [x] 테스트 작성 및 기존 테스트 보정
