# 회원가입 관심 분야/가입 경로 테스트 케이스

**작성일**: 2026-02-11
**버전**: 1.0
**관련 검증 기준서**: [interests-join-route-verification-criteria.md](../../../../docs/criteria/user/signup/interests-join-route-verification-criteria.md)

---

## 1. 개요

회원가입 시 추가되는 관심 분야(`interests`), 기타 관심 분야(`customInterest`), 가입 경로(`joinRoute`), 기타 가입 경로(`customJoinRoute`) 4개 필드에 대한 테스트 케이스를 관리하는 인덱스 문서이다. 검증 기준서의 6개 불변조건(INT-INV-01~06)을 해소하는 것을 목표로 한다.

---

## 2. 테스트 케이스 문서 목록

| 문서 | 범위 | 케이스 수 | 우선순위 |
|------|------|----------|---------|
| [interests-join-route-test-cases.md](./interests-join-route-test-cases.md) | 단위 테스트(Converter), DTO 입력 검증, 교차 검증, 조합 테스트, 데이터 호환성, 관리자 조회 | ~40 | P1 |

---

## 3. 테스트 케이스 ID 규칙

| 접두사 | 영역 | 예시 |
|--------|------|------|
| SINT-0xx | InterestListConverter 단위 테스트 | SINT-001 |
| SINT-01x | 관심 분야 입력 검증 (DTO) | SINT-010 |
| SINT-02x | 가입 경로 입력 검증 (DTO) | SINT-020 |
| SINT-03x | OTHER 교차 검증 - 관심 분야 (서비스) | SINT-030 |
| SINT-04x | OTHER 교차 검증 - 가입 경로 (서비스) | SINT-040 |
| SINT-05x | custom 필드 길이 검증 (DTO) | SINT-050 |
| SINT-06x | 필드 간 조합 테스트 (Pairwise) | SINT-060 |
| SINT-07x | 기존 데이터 호환성 | SINT-070 |
| SINT-08x | 관리자 조회 응답 | SINT-080 |

번호는 10단위로 그룹핑 (001-009, 010-019 등).

---

## 4. 불변조건 커버리지 매트릭스

| 불변조건 | 설명 | 단위 테스트 | 서비스 테스트 | 통합 테스트 |
|---------|------|-----------|-------------|-----------|
| INT-INV-01 | interests 최소 1개 선택 필수 | - | - | SINT-010~012 |
| INT-INV-02 | joinRoute 필수 | - | - | SINT-020~022 |
| INT-INV-03 | OTHER → customInterest 필수 | - | SINT-030~034 | SINT-063~066 |
| INT-INV-04 | OTHER → customJoinRoute 필수 | - | SINT-040~044 | SINT-067~068 |
| INT-INV-05 | 기존 사용자 null 허용 | - | - | SINT-070~071 |
| INT-INV-06 | OTHER 아닌 경우 custom 무시 | - | SINT-035 | SINT-060~062 |

---

## 5. 테스트 실행 방법

```bash
# 전체 테스트 실행
./gradlew test

# 회원가입 관련 테스트만 실행
./gradlew test --tests "igrus.web.security.auth.password.*"

# Converter 단위 테스트만 실행
./gradlew test --tests "igrus.web.user.domain.InterestListConverterTest"

# SignupService 통합 테스트만 실행
./gradlew test --tests "igrus.web.security.auth.password.service.signup.SignupServiceTest"
```

---

## 6. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-11 | - | 검증 기준서 기반 최초 작성 |
