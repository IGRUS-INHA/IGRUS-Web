# 검증 기준 리뷰어 에이전트 메모리

## 프로젝트 개요
- 도메인: 인하대학교 IGRUS 동아리 웹사이트
- 기술 스택: Spring Boot 4 / Java 21 / React 19 / MySQL 8
- 역할 체계: ASSOCIATE < MEMBER < OPERATOR < ADMIN

## 검증 기준서 위치 및 현황
- `docs/criteria/verification-criteria.md` — 회원가입/승인/강등 (Status: Draft)
- `docs/criteria/inquiry-verification-criteria.md` — 문의 (Status: Review Completed, 가장 완성도 높음)
- `docs/criteria/event/event-verification-criteria.md` — 행사 (Status: Draft)
- `docs/criteria/event/event-registration-verification-criteria.md` — 행사 신청 (Status: Draft)
- `docs/criteria/user/signup/*.md` — 회원가입 세부 (Status: Implemented)
- `docs/criteria/storage/image-presigned-url-verification-criteria.md` — S3 Presigned URL (Status: Draft, 구현 전)

## QA Wiki 10개 영역 (문서에서 사용하는 매핑 관례)
문서 섹션 번호와 Wiki 실제 번호가 다름. 아래는 문서가 사용하는 관례:
1. 도메인 규칙과 불변조건 (Wiki #2)
2. 상태 모델 (Wiki #6)
3. 시스템 경계와 책임 분리 (Wiki #3)
4. 외부 의존성 실패 정책 (Wiki #4)
5. 입력 도메인 분할과 경계값 (Wiki #5)
6. 권한/보안 정책 (Wiki #7)
7. 관측 가능성 (Wiki #8)
8. 테스트 전략 (Wiki #9)

## 문서 형식 패턴 (기존 검증 기준서 기준)
- 헤더: Status, Last Updated, Scope, Reference (QA Wiki 링크)
- 불변조건 ID: `{도메인접두어}-INV-{번호}` (예: INV-01, EVT-INV-01, INQ-INV-01, STOR-INV-01)
- 보안 체크리스트 ID: `SEC-{도메인접두어}-{번호}` (예: SEC-01, SEC-EVT-01, SEC-INQ-01, SEC-STOR-01)
- 각 불변조건: 인용문(> 으로 시작) + 사전조건, 사후조건, 위반 시, 검증 방법, 관련 코드
- 상태 전이 테이블: 전이, 트리거, 사전조건, 사후조건 (필수 4열)
- 금지된 전이 테이블: 시도, 예상 결과, 이유 (필수 3열)
- 완성도가 높은 문서는 "관련 코드" 필드에 실제 파일명:라인번호 기재
- GAP 항목: `GAP-{도메인접두어}-{번호}` ID, 내용, 심각도(높음/중간/낮음), 상태

## 반복적으로 발견되는 문제 패턴
1. 동일 개념의 수치가 문서 내 여러 위치에서 불일치 (예: 만료 시간 5분 vs 15분)
2. 권한 매트릭스에 나오는 개념(예: "공개/비공개 이미지")이 도메인 규칙 섹션에 미정의
3. 상태 다이어그램의 상태와 감사 이력 필드 목록 불일치
4. 멱등성 관련 정책이 섹션 간 충돌 (외부 의존성 섹션 vs 입력 검증 섹션)
5. 구현 전 문서에서 테스트 전략이 "구현 후 작성" 수준에 머묾
6. `(현재 구현 일치)`와 `(구현 예정)` 레이블이 같은 항목에 혼재 또는 잘못 표기되는 패턴 (event-verification-criteria.md EVT-INV-11, EVT-INV-14에서 발견)
7. 비연동 케이스 열거가 불완전하여 구현자가 추론 부담을 지는 패턴 (EVT-INV-20: COMPLETED/CANCELED 행사 unpublish 케이스 누락)
8. 검증 방식이 "A 또는 B"로 미확정 기술되어 예외 타입 불일치 유발 가능 (EVT 4-3절 publish/unpublish 권한 검증 방식)
9. 이력 엔티티의 nullable 필드 컬럼 스키마 미명시 패턴 (EventStatusChangeHistory.reason nullable 여부 미정)

## 잘 작성된 패턴 (참조용)
- inquiry-verification-criteria.md: 테스트 클래스명, 테스트 수, 커버 상태까지 구체적으로 기술한 모범 사례
- event-verification-criteria.md: GAP 항목을 기존/신규로 나눠 추적, "해결됨" 상태 업데이트 관례
- 3자 시스템 경계 ASCII 다이어그램 + 단계별 번호와 API 흐름 표 대응 방식 (storage 문서의 강점)

## Presigned URL 도메인 특이사항 (2026-02-26 리뷰)
- 핵심 아키텍처: Frontend → Backend(URL발급) → S3(직접 PUT) → Backend(완료확인)
- UPLOADING 상태가 DB 저장 여부 불분명 (프론트엔드 UI 상태일 가능성)
- 업로드 URL 만료: 5분(STOR-INV-03) vs 15분(상태전이표, 4-2절) 불일치 존재
- "공개/비공개 이미지" 구분 개념이 접근 제어 매트릭스에서 사용되나 도메인 규칙에 미정의
- 완료 알림 멱등성(4-3절 성공 반환) vs 입력 검증(5-4절 COMPLETED Key는 무효) 간 충돌
