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
- `docs/criteria/event/survey-event-registration-verification-criteria.md` — 설문 연동 행사 신청 (Status: Draft, 구현 전, 2026-03-02 1라운드 리뷰)
- `docs/criteria/user/signup/*.md` — 회원가입 세부 (Status: Implemented)
- `docs/criteria/storage/image-presigned-url-verification-criteria.md` — S3 Presigned URL (Status: Draft, 구현 전)
- `docs/criteria/survey/survey-criteria-v1.md` — 설문 (Status: Draft, 구현 전)
- `docs/criteria/event/event-image-integration-verification-criteria.md` — 행사-이미지 연계 (Status: **Approved**, 구현 전, 최종 리뷰 PASS 2026-03-02)

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
6. 교차 도메인 참조 불변조건 번호 오류 (예: 설문 INV-09는 "PUBLISHED+OPEN" 조건인데 교차 문서가 "응답 존재만으로 충분"이라고 기술하며 불일치 발생)
7. 설계 결정 미확정(TBD) 사항이 다른 불변조건에 직접 영향을 주는 경우 (예: HTTP 상태코드 400 vs 409 불일치)
8. 표의 "분류" 컬럼이 미확정 DECISION에 따라 달라지는데 고정값("무효")으로 기재 — DECISION 확정 전에는 "미결"로 표기해야 함
9. 교차 문서 액션 아이템(ACTION REQUIRED)이 대상 문서에 GAP 항목으로 등록되지 않아 추적 불가
10. 구현 전 문서의 산출물(OpenAPI 스펙 등) 미반영 상태를 문서 내에서 명시하지 않아 현재 상태 파악 불가
11. 서비스 레벨 중복 검증만 기술하고 DB 레벨 UNIQUE 제약을 부록 DDL에서도 누락 — 동시성 취약점 (EGRP-INV-01 패턴)
12. DECISION으로 등록되지 않은 미확정 정책이 불변조건 주의사항에 묻혀 있음 ("DECISION 필요"라는 표현이 DECISION 표가 아닌 본문에 산재)
13. 불변조건 본문과 입력 도메인 분할 섹션(3절)에 중복으로 기술된 검증 로직이 불변조건 본문에서는 빠지는 패턴 (예: 수정 시 자기 자신 제외)
14. DECISION 표를 신설하더라도 "권장안" 컬럼으로만 운영하면 불변조건 본문의 "~에 따라 다름: 권장안(A)" 표현이 남아 구현 기준 불명확 — DECISION 표의 컬럼명을 "결정(확정)"으로 바꾸고 불변조건 본문도 확정 언어로 교체해야 함

## 설문 연동 행사 신청 도메인 특이사항 (3라운드 리뷰 반영, 2026-03-02)
- 핵심 설계: Event.surveyId(Long, 약한 참조) → 두 도메인 FK 없이 서비스 레벨에서 참조 무결성 관리
- 3라운드 기준 PASS. Round 1 심각 2건 + 주의 7건 해결, Round 2 잔존 주의 2건 중 일부 미반영.
- 행사 검증 기준서가 "3축 모델(visibility + registrationStatus + eventStatus)"로 업데이트된 상태.
  → SEVT 문서 헤더가 여전히 "3축 연동 모델 — Event 2축 + Survey 2축"으로 기술되어 EVT 문서와 축 표현이 불일치.
  → 교차 매트릭스(2-1-1)에 행사 visibility 축 교차 조건이 누락됨. UNPUBLISHED 행사에서의 신청 가능 여부를 명시하지 않음.
- SEVT-INV-05 정책 A/B와 DECISION-01 미확정 상태 지속. DECISION-01이 권장(A)으로만 표시되고 "확정" 표시 없음.
- SEVT-INV-10에서 UNPUBLISHED+CLOSED가 허용되는 경로(한때 PUBLISHED+OPEN → 비공개 전환) 설명 여전히 미흡.
- 설문 INV-09(PUBLISHED+OPEN에서만 응답 가능)와 SEVT-INV-10(responseStatus != NOT_STARTED이면 신청 가능)은 역할이 다름 — 모순 아님. 두 조건이 적용되는 시점(응답 제출 vs 신청)이 다름.
- HTTP 상태코드: SurveyResponseRequiredException=400, SurveyNotReadyException=400 — 프로젝트 관례(도메인 상태 위반=400) 명시됨.
- 동시성 시나리오: 복수 사용자가 동시에 동일 행사에 신청하면서 동시에 설문 응답을 최초 제출하는 경우(INV-01의 409 경합 → 설문 응답 저장 실패 → 행사 신청도 롤백) 에 대한 명시적 처리 정책이 문서에 없음.
- SEVT-INV-06에서 "이미 설문 응답이 존재하면 surveyAnswers 생략 가능"으로 기술되나, 동시에 surveyAnswers를 포함하면서 이미 응답이 존재하는 경우(중복 응답 시도와 행사 신청 동시) 처리 정책이 명시되지 않음.

## 행사-이미지 연계 도메인 특이사항 (최종 리뷰 PASS 2026-03-02, Status: Approved)
- 핵심 아키텍처: Event.posterImageObjectKey(String, nullable, FK 없음) — surveyId 패턴과 동일한 약한 참조
- 1라운드 FAIL → 2라운드 FAIL → 3라운드 PASS → 최종 보완 PASS.
- DECISION 5건 전체 확정 (모두 A안): 프리픽스 강제 / 소유권 무관 / 빈문자열→null / 단일이미지 / ObjectKey만 반환
- 최종 보완에서 해결된 이슈: WATCH-A(ONGOING Key A→B 변경 케이스), WATCH-B(404/400 HTTP 상태코드 분리), WATCH-C(null 케이스 명시)
- 최종 잔존 주의 이슈 (PASS 유지, 향후 권장):
  - EVT-IMG-INV-02 위반 시 예외 표현이 "적절한 비즈니스 예외"로 모호 — 4-1절과 일치하는 구체 예외명/HTTP코드 기재 필요
  - 7-3절 N-표에 HTTP 상태코드 일부 누락 (N-02, N-03, N-04, N-07)
  - ACTION REQUIRED(EVT-INV-07에 posterImageObjectKey 추가)가 event-verification-criteria.md GAP으로 미등록
- 파일 위치: docs/criteria/event/event-image-integration-verification-criteria.md

## 잘 작성된 패턴 (참조용)
- inquiry-verification-criteria.md: 테스트 클래스명, 테스트 수, 커버 상태까지 구체적으로 기술한 모범 사례
- event-verification-criteria.md: GAP 항목을 기존/신규로 나눠 추적, "해결됨" 상태 업데이트 관례
- 3자 시스템 경계 ASCII 다이어그램 + 단계별 번호와 API 흐름 표 대응 방식 (storage 문서의 강점)

## 행사 그룹(EventGroup) 도메인 특이사항 (2라운드 FAIL, 2026-03-03)
- 핵심 설계: Event.groupId(Long, nullable, FK 없음) — DECISION-01(C) 약한 참조, surveyId 패턴과 동일
- 1라운드 심각 4건 모두 해소: DB 유니크 제약(Generated Column), DECISION-07/08/09 신설, 수정 시 자기 자신 제외 명시, PUT 근거 명시
- 2라운드 심각 1건: DECISION 표 전체가 "권장안" 상태 — 불변조건 본문이 미확정 DECISION 참조하는 구조적 모순 지속
  → "DECISION-07에 따라 다름: 권장안 (A)" 표현이 남아있어 구현자가 확정 여부 판단 불가
  → 수정 방향: "권장안" 컬럼을 "결정(확정)"으로 변경 + 불변조건 본문의 미확정 언어를 확정 언어로 교체
- 2라운드 주의 5건:
  1. SoftDeletableEntity.restore() EventGroup 복원 정책 미언급 (복원 불가 명시 또는 재연결 정책 필요)
  2. eventCount 조회 시 N+1 문제 — 그룹 목록에 COUNT 서브쿼리 또는 DTO Projection 전략 미명시
  3. EVT-INV-07과 groupId 수정 분리 정책 교차 언급 없음
  4. DECISION-07 멱등 응답 시 감사 로그 및 응답 본문 형식 미정의
  5. EventGroupDetailResponse에 updatedBy(최종 수정자) 필드 누락 여부 미결정
- DECISION-10(C) 전체 반환, DECISION-11(B) 인증 필수, DECISION-09(A) PUT 확정 — 모두 권장안으로만 표시
- DB 레벨 조건부 유니크 제약: Generated Column 방식 DDL 상세 기술 (이 프로젝트 첫 사례)
- EGRP-INV-08 @Modifying(flushAutomatically=true, clearAutomatically=true) 주의사항 명시됨 (잘 작성된 부분)

## Presigned URL 도메인 특이사항 (2026-02-26 리뷰)
- 핵심 아키텍처: Frontend → Backend(URL발급) → S3(직접 PUT) → Backend(완료확인)
- UPLOADING 상태가 DB 저장 여부 불분명 (프론트엔드 UI 상태일 가능성)
- 업로드 URL 만료: 5분(STOR-INV-03) vs 15분(상태전이표, 4-2절) 불일치 존재
- "공개/비공개 이미지" 구분 개념이 접근 제어 매트릭스에서 사용되나 도메인 규칙에 미정의
- 완료 알림 멱등성(4-3절 성공 반환) vs 입력 검증(5-4절 COMPLETED Key는 무효) 간 충돌
