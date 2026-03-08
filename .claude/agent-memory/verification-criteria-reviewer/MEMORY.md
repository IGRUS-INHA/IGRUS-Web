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
15. 신규 기능의 취소/삭제 엔드포인트가 OpenAPI 스펙 및 문서의 엔드포인트 분리 표 양쪽에서 동시에 누락되는 패턴 — 특히 "관리자만 취소 가능" 정책처럼 기존 회원 본인 취소와 다른 권한 구조일 때 별도 엔드포인트 또는 기존 엔드포인트 권한 변경 명세가 빠지기 쉬움
16. 서비스 레벨 중복 검증 + DB UNIQUE 제약을 모두 명시하면서도 두 계층 사이 경합 시 예외 변환 정책(DataIntegrityViolationException → 비즈니스 예외)을 기술하지 않는 패턴

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

## 행사/설문 기능 보완 도메인 특이사항 (2026-03-08, 2라운드 리뷰 PASS)
- 파일 위치: docs/criteria/event/event-survey-improvements-verification-criteria.md
- 범위: 6개 Phase — Phase 1(allowExternal 버그), Phase 2(응답 수), Phase 3(관리자 응답 목록), Phase 4(응답 삭제), Phase 5(외부인 통계), Phase 6(코드 중복)
- 1라운드 심각 3건 모두 해소:
  1. CRIT-01 해소: EVTSRV-027/034에서 204 No Content로 확정, EVTSRV-044에서 검증 절차 4번에 204 명시
  2. CRIT-02 해소: EVTSRV-029/034에서 SurveyClosedException + 409 Conflict로 확정
  3. CRIT-03 해소: EVTSRV-043(관리자 응답 목록), EVTSRV-044(응답 삭제) 추가
- 2라운드 잔존 주의 이슈 (PASS 유지, 향후 권장):
  - WARN-01 잔존: EVTSRV-030 4단계에 "승인된 신청이었던 경우"라는 조건이 있으나, APPROVED 외 다른 상태(WAITING, PENDING) 시 currentCount 변화 없음을 검증하는 별도 케이스 없음
  - WARN-02 잔존: 행사 신청 취소 상태 확인 API(`GET /api/v1/events/{eventId}/registrations/me` 등)가 EVTSRV-030 검증 절차에 미명시
  - WARN-03 해소(부분): EVTSRV-039 비고에 "구현 정책에 따름"으로 언급하나 DECISION 표 부재 — 미결 정책임을 표기하는 수준은 개선됨
  - WARN-04 잔존: Phase 5 권한 검증 항목이 여전히 없음 — 통계 API `GET /api/v1/admin/surveys/{surveyId}/statistics` 접근 권한 RBAC 표 없음
  - WARN-05 잔존: EVTSRV-028 비고에 404 반환은 기술했으나 설계 의도(정보 은닉) 명시 없음
  - WARN-06 잔존: EVTSRV-021/043에서 "응답자 정보"가 여전히 모호 — userId, userName 필드명 명시 없음
- 검증 기준 문서 내 OpenAPI 스펙 미등록 상태: surveys.yaml에 DELETE /surveys/{surveyId}/responses 및 GET /admin/surveys/{surveyId}/responses 모두 미등록 — 구현 전이므로 검증 항목(EVTSRV-043, EVTSRV-044)이 이를 검증함
- 2라운드 추가 발견 주의 이슈:
  - WARN-07: EVTSRV-030에서 설문 응답 삭제 후 responseCount 감소 여부 검증 항목이 EVTSRV-018 경계값 표에 없음 (EVTSRV-018의 마지막 행이 "삭제 후 카운트 감소"를 다루긴 하지만 삭제 API 실행 시나리오로 연결되지 않음)
  - WARN-08: EVTSRV-043 검증 절차가 스펙 등록 확인에만 초점 — 응답 스키마(SurveyAdminResponseListItem 등 신규 스키마명)가 surveys.yaml에 정의되어야 함을 명시하지 않음

## 행사 그룹(EventGroup) 도메인 특이사항 (3라운드 PASS, 2026-03-03)
- 핵심 설계: Event.groupId(Long, nullable, FK 없음) — DECISION-01(C) 약한 참조, surveyId 패턴과 동일
- 1라운드 심각 4건 모두 해소, 2라운드 심각 1건 해소, 3라운드 PASS.
- 3라운드 해소: DECISION 표 전체 "확정안" 전환 완료. 11개 DECISION 모두 "(X) 확정:" 형식으로 기술. 불변조건 본문의 미확정 언어 교체 완료.
- 3라운드 잔존 주의 4건 (PASS 유지, 향후 권장):
  1. SoftDeletableEntity.restore() 복원 불가 근거 미명시 — DECISION-03(A)에 의해 groupId=null 변경 후 복원 시 행사 재연결 불가이므로 복원 불가 명시 필요
  2. eventCount N+1 문제 대응 전략(COUNT 서브쿼리 또는 DTO Projection) 미명시
  3. EVT-INV-07(행사 수정 API)에서 groupId 수정이 불가함을 EGRP 문서에서 교차 언급하지 않음
  4. DECISION-07 멱등 성공(200 OK) 시 응답 본문 및 서비스 로그 형식 미정의
- 참고 4건: updatedBy 필드 미결정, collation 표기 불일치(utf8mb4_unicode_ci vs 0900_ai_ci), 상세조회에 eventCount 미포함 의도 미명시, 행사 제거 응답 200 OK 근거 미명시
- DECISION-10(C) 전체 반환, DECISION-11(B) 인증 필수, DECISION-09(A) PUT — 모두 확정됨
- DB 레벨 조건부 유니크 제약: Generated Column 방식 DDL 상세 기술 (이 프로젝트 첫 사례)
- EGRP-INV-08 @Modifying(flushAutomatically=true, clearAutomatically=true) 주의사항 명시됨 (잘 작성된 부분)
- 파일 위치: docs/criteria/event/event-group-verification-criteria.md

## Presigned URL 도메인 특이사항 (2026-02-26 리뷰)
- 핵심 아키텍처: Frontend → Backend(URL발급) → S3(직접 PUT) → Backend(완료확인)
- UPLOADING 상태가 DB 저장 여부 불분명 (프론트엔드 UI 상태일 가능성)
- 업로드 URL 만료: 5분(STOR-INV-03) vs 15분(상태전이표, 4-2절) 불일치 존재
- "공개/비공개 이미지" 구분 개념이 접근 제어 매트릭스에서 사용되나 도메인 규칙에 미정의
- 완료 알림 멱등성(4-3절 성공 반환) vs 입력 검증(5-4절 COMPLETED Key는 무효) 간 충돌

## 외부인 행사 신청 도메인 특이사항 (2026-03-06, 3라운드 리뷰 FAIL)
- 파일 위치: docs/criteria/event/external-event-registration-verification-criteria.md
- 핵심 설계: Event.allowExternal(Boolean, NOT NULL DEFAULT FALSE) + 외부인 전용 엔드포인트(security: []) + 기존 EventRegistration 테이블 확장(DECISION-01 확정 A)
- 역할 계층: 외부인(비인증) < ASSOCIATE < MEMBER < OPERATOR < ADMIN — 준회원이 allowExternal=true 행사에서 조건부 허용(REG-INV-04 변경)
- DECISION 확정 현황 (3라운드 기준): 01(A-단일테이블), 03(A-관리자만취소), 05(기본값false), 08(A-기존API확장) — 4건 확정. 02, 04, 06, 07 — 4건 미확정(권장안).
- 1라운드 ~ 2라운드 해결 이슈:
  - EXT-INV-02/03 DB UNIQUE 경합 → DataIntegrityViolationException → 409 변환 정책 명시
  - Section 3-3-1 신설, DECISION-08 신규 등록(확정), GAP-EXT-03/05/06 등록
  - DECISION-01, 03 확정 전환, EXT-INV-04/09 등 불변조건 본문 확정 언어 교체
- 3라운드 잔존 심각 이슈 (FAIL 원인):
  - OpenAPI 스펙(`openapi/openapi.yaml` + `openapi/paths/events.yaml`)에 `POST /api/v1/registrations/{registrationId}/cancel` 경로 미등록. DECISION-08 확정 후에도 GAP-EXT-03 미해결. approve/reject/revert 패턴과 동일하게 스펙 추가 필요.
- 3라운드 잔존 주의 이슈:
  1. EXT-INV-01 allowExternal=false → 400 근거(프로젝트 관례) 미명시
  2. 4-1절 surveyAnswers 유효/무효 컬럼이 "DECISION-04에 따라 다름" — "미결"로 통일 필요
  3. Section 2-1 선발제 금지된 전이 표 — WAITING | cancel() (외부인 본인) 결과에 "401 Unauthorized" 미명시
  4. GAP-EXT-06 내용에 DECISION-08 확정 반영 누락
  5. EXT-INV-08 EVT-INV-18 번호 정확성 미검증
- 잘 작성된 부분: DECISION-01/03/08 확정 전환 및 불변조건 본문 일관 교체, EXT-INV-02/03 경합 정책 연동(불변조건→중복표→로그→동시성테스트), Section 3-3-1 취소 엔드포인트 상세 표

## 게시글/문의 S3 연계 도메인 특이사항 (2026-03-05, 3라운드 리뷰 PASS)
- 파일 위치: docs/criteria/storage/post-inquiry-s3-integration-verification-criteria.md
- 핵심 아키텍처: PostImage.imageUrl(String, FK없음) / InquiryAttachment.fileUrl(String, FK없음) — EVT-IMG 패턴과 동일한 약한 참조
- 1라운드 FAIL → 2라운드 FAIL → 3라운드 PASS.
- 3라운드에서 해결된 이슈:
  1. COMMON-S3-INV-01 완전 삭제 — 불변조건 1-3절에서 제거됨 (DECISION-05 항목 본문 부연/GAP에 흡수)
  2. INQ-ATT-INV-02 플레이스홀더 처리 확정 — 헤더에 "DECISION-04 미확정: 프리픽스 값" 명시, 본문 전체가 플레이스홀더 구조로 교체
  3. 4-1/4-2절 중복 Key 행이 "미결 (DECISION-06 확정 후 결정)"으로 수정
  4. 4-2절 Inquiry 입력값 표에 CONFIRMING 상태 Key 행 추가 (N-17로도 대응)
  5. 2-2절에 이미지 교체 메커니즘 주석 추가 (clearImages → orphanRemoval DELETE 후 INSERT)
  6. 9-3절에 @Pattern 제거와 OpenAPI pattern 제거 연동 필수 변경 명시
- 3라운드 잔존 주의 이슈 (PASS 유지, 향후 권장):
  1. 7-3절 N-표 일부 행(N-02, N-03, N-04, N-07, N-11, N-18 등)에 HTTP 상태코드 누락
  2. DECISION-04 영향 범위 표에 "4-2절 다른 도메인 Key 행, N-15" 미등재 — 반복 패턴
  3. 9-5절 구현 스니펫 메서드명 불일치 (existsByImageUrlAndPostDeletedFalse vs existsByImageUrlAndPostNotDeleted) 잔존 (주석에서 실제 JPQL 쿼리 메서드명과 다름)
  4. 2-3절 문의 CONFIRMING 시나리오 (문의 생성 중 파일 상태 변경) 명시 없음 — 단방향 상태전이 특성상 첨부 불가 경우만 간단히 언급
- DECISION 현황: 확정 3건(01~03), 미확정 5건(04~08)
- 특이사항: COMMON-S3-INV-01 완전 삭제로 1-3절 소제목 없이 1-1(Post), 1-2(Inquiry) 두 절로만 구성 — 향후 공통 불변조건 신설 시 1-3절 번호 재사용 가능
