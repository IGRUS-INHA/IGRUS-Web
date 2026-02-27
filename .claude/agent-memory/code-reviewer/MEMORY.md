# Code Reviewer Agent Memory

## Flyway Migration Patterns
- V41~V44: survey 관련, V45: file_metadata 테이블
- 버전 변경 시 파일 내부 주석도 반드시 갱신 필요 (Round 2에서 발견)
- 체크리스트 문서의 파일 경로도 동시에 갱신 필요

## Project Exception Pattern
- 모든 커스텀 예외: `CustomBaseException` 상속
- 도메인별 ErrorCode enum: `{Domain}ErrorCode implements ErrorCode`
- 기본 생성자 + 상세 메시지 생성자 제공
- cause를 받는 생성자는 외부 시스템 래핑용 (예: S3OperationFailedException)

## LocalDate Usage Convention
- CLAUDE.md는 "Instant만 사용" 규칙이지만, 날짜 추출 목적의 LocalDate 사용은 프로젝트 전반에서 허용됨
- TempStudentIdGeneratorService, GetDashboardStatsService, ObjectKeyGenerator 등에서 동일 패턴
- 시간 *저장*용이 아닌 날짜 기반 계산/경로생성 용도

## Common Review Pitfalls
- Javadoc @throws 설명이 코드 수정 후 갱신되지 않는 경우 빈번
- Flyway 버전 변경 시 파일 내부 주석 + 체크리스트 문서 동시 갱신 누락 주의
- catch(Exception) 블록이 S3 호출뿐 아니라 DB 저장 로직까지 감싸는 패턴 -> 잘못된 에러 변환 유발
- 외부 SDK 에러 메시지(e.getMessage())를 API 응답에 그대로 노출하는 패턴 -> Information Disclosure 보안 취약점
