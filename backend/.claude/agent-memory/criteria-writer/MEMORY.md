# Criteria Writer Agent Memory

## QA 위키 용어 매핑
- 10개 영역: 품질 목표, 도메인 규칙/불변조건, 시스템 경계/책임 분리, 외부 의존성 장애 정책, 입력 도메인 분할/경계값, 상태 모델, 권한/보안 정책, 관측 가능성, 테스트 전략, 테스트 효과 검증
- 위키에는 명시적 우선순위/심각도 분류가 없음 -- 프로젝트 자체 분류 사용

## 기존 검증 기준서 형식 (docs/criteria/)
- 헤더: Status, Last Updated, Scope, Reference, 관련 검증 기준서
- 목적 섹션: 적용할 위키 영역 테이블 포함
- 불변조건 ID 패턴: `{도메인}-INV-{번호}` (예: EVT-IMG-INV-01, EVT-ATT-INV-01)
- 보안 ID 패턴: `SEC-{도메인}-{번호}` (예: SEC-EVT-ATT-01)
- 각 불변조건: 설명(인용), 사전조건, 사후조건, 위반 시, 관련 코드, 검증 방법
- 섹션 순서: 불변조건 -> 상태 모델 -> 시스템 경계 -> 입력 분할/경계값 -> 권한/보안 -> 관측 가능성 -> 테스트 전략 -> DB/API 변경 -> 설계 결정(필요시) -> 관련 문서

## 폴더 구조
- `docs/criteria/event/` -- 행사 관련 검증 기준서
- `docs/criteria/storage/` -- 스토리지 관련 검증 기준서
- `docs/criteria/survey/` -- 설문 관련 검증 기준서
- `docs/criteria/user/` -- 사용자/회원 관련 검증 기준서

## 공통 패턴
- FileReferenceChecker: 파일 참조 무결성 검사 인터페이스, Bean 등록으로 자동 참조 검사
- Soft Delete 시 파일 유지, 영구 삭제 시 연쇄 정리 패턴
- 행사 3축 상태 모델: visibility / registrationStatus / eventStatus
- Flyway 최신 버전: V47 (2026-03-05 기준)
- StorageErrorCode: FILE_REFERENCE_EXISTS(409), FILE_OWNERSHIP_MISMATCH(403)
