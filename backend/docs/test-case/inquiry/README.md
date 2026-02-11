# Inquiry(문의) 테스트 케이스

**작성일**: 2026-02-11
**버전**: 2.0
**관련 검증 기준서**: [inquiry-verification-criteria.md](../../../../docs/criteria/inquiry-verification-criteria.md)

---

## 1. 개요

문의 도메인의 전체 테스트 케이스를 관리하는 인덱스 문서이다. 각 기능 영역별로 별도의 테스트 케이스 문서를 작성하며, 검증 기준서의 8개 불변조건(INQ-INV-01~08)과 6개 테스트 갭(GAP-INQ-01~06)을 해소하는 것을 목표로 한다.

---

## 2. 테스트 케이스 문서 목록

| 문서 | 범위 | 케이스 수 | 우선순위 |
|------|------|----------|---------|
| [guest-inquiry-test-cases.md](./guest-inquiry-test-cases.md) | 비회원 문의 생성, 조회, 알림 | ~25 | P1 |
| [member-inquiry-test-cases.md](./member-inquiry-test-cases.md) | 회원 문의 생성, 내 문의 목록/상세, 알림 | ~20 | P1 |
| [inquiry-admin-test-cases.md](./inquiry-admin-test-cases.md) | 관리자 목록/상세 조회, 상태 변경, 답변, 메모, 삭제 | ~35 | P1 |
| [inquiry-domain-test-cases.md](./inquiry-domain-test-cases.md) | 도메인 엔티티 단위 테스트 (FSM, 첨부파일, 답변, 소유권) | ~30 | P2 |
| [inquiry-security-test-cases.md](./inquiry-security-test-cases.md) | RBAC 컨트롤러 테스트, 소유권 검증, 보안 | ~25 | P2 |

---

## 3. 테스트 케이스 ID 규칙

| 접두사 | 영역 | 예시 |
|--------|------|------|
| INQ-G-xxx | 비회원 문의 (Guest) | INQ-G-001 |
| INQ-M-xxx | 회원 문의 (Member) | INQ-M-001 |
| INQ-A-xxx | 관리자 기능 (Admin) | INQ-A-001 |
| INQ-D-xxx | 도메인 엔티티 (Domain) | INQ-D-001 |
| INQ-SEC-xxx | 보안/RBAC (Security) | INQ-SEC-001 |

번호는 10단위로 그룹핑 (001-009, 010-019 등).

---

## 4. 불변조건 커버리지 매트릭스

| 불변조건 | 설명 | 도메인 테스트 | 서비스 테스트 | 보안 테스트 |
|---------|------|-------------|-------------|------------|
| INQ-INV-01 | 문의번호 유일성 | INQ-D-070~074 | INQ-G-020~022, INQ-M-020~022 | - |
| INQ-INV-02 | 첨부파일 최대 3개 | INQ-D-040~045 | INQ-G-004~006, INQ-M-003~005 | - |
| INQ-INV-03 | 답변 최대 1건 | INQ-D-050~054 | INQ-A-044 | - |
| INQ-INV-04 | Soft delete 필터링 | - | INQ-A-072, INQ-G-045 | INQ-SEC-053 |
| INQ-INV-05 | 비회원 비밀번호 필수 | INQ-D-005~006 | INQ-G-015~016 | INQ-SEC-050 |
| INQ-INV-06 | 회원 사용자 참조 필수 | INQ-D-010~015 | INQ-M-010 | - |
| INQ-INV-07 | COMPLETED 종단 상태 | INQ-D-027~028 | INQ-A-025~026 | - |
| INQ-INV-08 | 답변 시 자동 완료 | INQ-D-029 | INQ-A-041 | - |

---

## 5. 테스트 갭 해소 현황

| Gap ID | 설명 | 해소 테스트 케이스 | 상태 |
|--------|------|------------------|------|
| GAP-INQ-01 | COMPLETED 상태 전이 테스트 부재 | INQ-A-025~026, INQ-D-027~028 | ⬜ 테스트 케이스 정의됨 |
| GAP-INQ-02 | 비회원 이메일 불일치 케이스 | INQ-G-042~043 | ⬜ 테스트 케이스 정의됨 |
| GAP-INQ-03 | 문의번호 충돌 재시도 로직 | INQ-G-020~022, INQ-M-020~022 | ⬜ 테스트 케이스 정의됨 |
| GAP-INQ-04 | 컨트롤러 RBAC 검증 테스트 | INQ-SEC-010~034 | ⬜ 테스트 케이스 정의됨 |
| GAP-INQ-05 | IN_PROGRESS 상태 전이 테스트 | INQ-A-022~024, INQ-D-023~026 | ⬜ 테스트 케이스 정의됨 |
| GAP-INQ-06 | 이메일 알림 실패 영향 미검증 | INQ-G-050~052, INQ-M-060~062, INQ-A-047~048 | ⬜ 테스트 케이스 정의됨 |

---

## 6. 구현 현황 요약

| 테스트 계층 | 구현된 테스트 수 | 주요 테스트 클래스 |
|------------|----------------|------------------|
| 도메인 단위 | 21개 | `InquiryTest`(12), `InquiryNumberGeneratorTest`(5), `InquiryAttachmentTest`(1), `InquiryReplyTest`(2), `InquiryMemoTest`(1) |
| 서비스 통합 | 14개 | `CreateGuestInquiryServiceTest`(2), `CreateMemberInquiryServiceTest`(2), `CreateInquiryReplyServiceTest`(2), `LookupGuestInquiryServiceTest`(3), 기타(5) |
| 컨트롤러 | 0개 | **미구현** (GAP-INQ-04) |

---

## 7. 테스트 실행 방법

```bash
# 전체 테스트 실행
./gradlew test

# Inquiry 관련 테스트만 실행
./gradlew test --tests "igrus.web.inquiry.*"

# 도메인 단위 테스트만 실행
./gradlew test --tests "igrus.web.inquiry.domain.*"

# 서비스 통합 테스트만 실행
./gradlew test --tests "igrus.web.inquiry.service.*"
```

---

## 8. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2024-01-23 | - | 초기 작성 (단일 파일) |
| 2.0 | 2026-02-11 | - | 검증 기준서 기반 전면 재작성, 5개 파일로 분리, 135개 테스트 케이스 정의 |
